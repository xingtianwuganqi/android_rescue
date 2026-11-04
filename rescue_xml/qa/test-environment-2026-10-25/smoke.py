"""Bounded staging smoke test. Never saves response bodies or account/contact data.
Uses the confirmed test host only. No automatic retries and no business writes.
Analytics checks create a few anonymous synthetic records with random installation IDs.
"""
import concurrent.futures
import datetime as dt
import json
from pathlib import Path
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

BASE='http://test.rxswift.cn/'
OUT=Path(__file__).resolve().parent
COMMON={'appType':'android','appVersion':'1.0.2','androidVersion':'8'}
class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self,*args,**kwargs): return None
OPENER=urllib.request.build_opener(NoRedirect())

def call(name,path,method='GET',form=None,body=None,headers=None):
    assert urllib.parse.urlparse(BASE).hostname=='test.rxswift.cn'
    assert not path.startswith(('http:','https:','//'))
    h={'Accept':'application/json','User-Agent':'Android-QA-staging-smoke/1.3.0'}
    h.update(headers or {})
    data=None
    if form is not None:
        data=urllib.parse.urlencode(form).encode();h['Content-Type']='application/x-www-form-urlencoded'
    if body is not None:
        data=json.dumps(body,ensure_ascii=False,separators=(',',':')).encode();h['Content-Type']='application/json'
    req=urllib.request.Request(BASE+path,data=data,headers=h,method=method)
    start=time.monotonic()
    result={'name':name,'method':method,'path':path,'started_at':dt.datetime.now(dt.timezone.utc).isoformat()}
    parsed=None
    try:
        try: response=OPENER.open(req,timeout=12)
        except urllib.error.HTTPError as error: response=error
        with response:
            result['http']=response.status
            result['content_type']=response.headers.get('Content-Type','')
            result['server_date']=response.headers.get('Date')
            if response.headers.get('Location'):
                location=urllib.parse.urljoin(BASE+path,response.headers['Location'])
                result['redirect_host']=urllib.parse.urlparse(location).hostname
                result['redirect_path']=urllib.parse.urlparse(location).path
            raw=response.read(2*1024*1024+1)
            result['response_bytes']=len(raw)
            try: parsed=json.loads(raw)
            except (ValueError,UnicodeError): result['json']=False
            else:
                result['json']=True
                if isinstance(parsed,dict):
                    result['code']=parsed.get('code')
                    payload=parsed.get('data')
                    if isinstance(payload,dict):
                        result['data_keys']=sorted(payload.keys())
                        result['error_code']=payload.get('error_code')
                        result['status']=payload.get('status')
                    elif isinstance(payload,list):
                        result['item_count']=len(payload)
                        result['item_keys']=sorted(payload[0].keys()) if payload and isinstance(payload[0],dict) else []
    except Exception as error:
        result['transport_error']=type(error).__name__
    result['elapsed_ms']=round((time.monotonic()-start)*1000)
    return result,parsed

def verdict(result,ok,expected):
    result['expected']=expected
    result['pass']=bool(ok)
    print(json.dumps(result,ensure_ascii=False),flush=True)
    return result

def initial(case):
    name,path,method,form,expected=case
    result,data=call(name,path,method,form=form)
    if expected=='auth':
        ok=result.get('json') is True and (result.get('http')==401 or result.get('http')==200 and result.get('code')==401)
    elif expected=='legacy_auth':
        ok=result.get('json') is True and result.get('code')==401 and result.get('http') in (200,401,403)
    elif expected=='unread_zero':
        counts=data.get('data') if isinstance(data,dict) else None
        ok=result.get('http')==200 and result.get('code')==200 and isinstance(counts,dict) and all((counts.get(k) or 0)==0 for k in ('like_unread','collec_unread','com_unread','find_pet_unread'))
    else:
        ok=result.get('http')==200 and result.get('code')==200 and isinstance(data,dict) and isinstance(data.get('data'),list)
    return verdict(result,ok,{'auth':'JSON auth rejection','legacy_auth':'legacy code401 auth rejection (HTTP200/401/403)','unread_zero':'anonymous private counters zero; public system announcements allowed','list':'HTTP200/code200 list'}[expected]),data

def main():
    cases=[
        ('adoption_profile_anonymous','api/v2/adoption-profile/me','GET',None,'auth'),
        ('adoption_mine_anonymous','api/v2/adoption-applications/mine?page=1&size=10','GET',None,'auth'),
        ('adoption_received_anonymous','api/v2/adoptions/mine/applications?page=1&size=10','GET',None,'auth'),
        ('adoption_notifications_anonymous','api/v2/adoption-notifications?page=1&size=10','GET',None,'auth'),
        ('adoption_unread_anonymous','api/v2/adoption-notifications/unread-count','GET',None,'auth'),
        ('find_recommendations_anonymous','api/v3/find-pet/me/recommendations/?page=1&size=20','GET',None,'auth'),
        ('legacy_find_info_anonymous','api/v2/searchpet/info','POST',COMMON,'legacy_auth'),
        ('legacy_unread_anonymous','api/v1/authunreadnum/','POST',COMMON,'unread_zero'),
        ('public_find_list','api/v2/findpet/list','POST',{**COMMON,'page':1,'size':10},'list'),
    ]
    results=[]
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
        for result,data in pool.map(initial,cases): results.append(result)
    for label,path,extra in [('home','api/v1/topiclist/',{}),('local','api/v2/addresstopiclist/',{'address':'上海市'})]:
        result,data=call(label+'_feed_first',path,'POST',form={**COMMON,**extra,'page':1,'size':10,'promotion_feed':'true'})
        meta=data.get('meta') if isinstance(data,dict) else None
        items=data.get('data') if isinstance(data,dict) else None
        valid=result.get('http')==200 and result.get('code')==200 and isinstance(meta,dict) and isinstance(items,list) and meta.get('page')==1 and meta.get('size')==10 and isinstance(meta.get('has_more'),bool) and bool(meta.get('snapshot_id'))
        result['meta_keys']=sorted(meta.keys()) if isinstance(meta,dict) else []
        result['has_more']=meta.get('has_more') if isinstance(meta,dict) else None
        results.append(verdict(result,valid,'HTTP200/code200 list and valid snapshot meta'))
        if not valid: continue
        snapshot=meta['snapshot_id']
        if meta['has_more']:
            next_result,next_data=call(label+'_feed_next',path,'POST',form={**COMMON,**extra,'page':2,'size':10,'promotion_feed':'true','snapshot_id':snapshot})
            next_meta=next_data.get('meta',{}) if isinstance(next_data,dict) else {}
            next_items=next_data.get('data') if isinstance(next_data,dict) else None
            ids={x.get('topic_id') for x in items if isinstance(x,dict)}
            next_ids={x.get('topic_id') for x in next_items if isinstance(x,dict)} if isinstance(next_items,list) else set()
            next_result['snapshot_matches']=next_meta.get('snapshot_id')==snapshot
            next_result['duplicate_topic_ids']=len((ids & next_ids)-{None})
            ok=next_result.get('http')==200 and next_result.get('code')==200 and next_meta.get('page')==2 and next_meta.get('snapshot_id')==snapshot and isinstance(next_items,list) and not ((ids & next_ids)-{None})
            results.append(verdict(next_result,ok,'page2 same snapshot, no duplicate topic IDs'))
        mismatch,_=call(label+'_snapshot_size_mismatch',path,'POST',form={**COMMON,**extra,'page':2,'size':11,'promotion_feed':'true','snapshot_id':snapshot})
        results.append(verdict(mismatch,mismatch.get('http')==409 and mismatch.get('error_code')=='FEED_SNAPSHOT_MISMATCH','HTTP409 FEED_SNAPSHOT_MISMATCH'))
    # Deliberate protocol cases, not automatic retries. Only these anonymous records are written.
    anonymous=str(uuid.uuid4());session=str(uuid.uuid4())
    def event(name='page_view',page='home',properties=None):
        return {'event_id':str(uuid.uuid4()),'event_name':name,'anonymous_id':anonymous,'session_id':session,
            'sequence':1,'occurred_at':dt.datetime.now(dt.timezone.utc).isoformat(),'platform':'android','app_version':'1.3.0','page':page,'properties':properties or {}}
    payload=event()
    analytics_cases=[
        ('analytics_accepted',payload,None,200,'accepted'),
        ('analytics_duplicate',payload,None,200,'duplicate'),
        ('analytics_message_type',event(page='message_list',properties={'message_list_type':'like'}),None,200,'accepted'),
        ('analytics_missing_message_type',event(page='message_list'),None,400,None),
        ('analytics_unknown_field',{**event(),'source':'staging-qa'},None,400,None),
        ('analytics_required_login',event('login_success','login',{'login_method':'password'}),None,401,None),
        ('analytics_invalid_token',event(),{'Authorization':'Bearer invalid-staging-test-token'},401,None),
        ('analytics_expired_time',{**event(),'occurred_at':'2020-01-01T00:00:00Z'},None,400,None),
    ]
    for name,payload,headers,http,status in analytics_cases:
        result,_=call(name,'api/v4/analytics/events/','POST',body=payload,headers=headers)
        result['event_id']=payload['event_id']
        ok=result.get('http')==http and result.get('json') is True and (status is None or result.get('status')==status)
        results.append(verdict(result,ok,f'HTTP{http}'+(' '+status if status else ' validation/auth rejection')))
    report={'base_url':BASE,'run_at':dt.datetime.now(dt.timezone.utc).isoformat(),'scope':'real staging HTTP; no authenticated business writes; no Android UI',
        'tests':len(results),'passed':sum(r['pass'] for r in results),'failed':sum(not r['pass'] for r in results),'results':results}
    OUT.joinpath('smoke-results.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({k:v for k,v in report.items() if k!='results'},ensure_ascii=False),flush=True)
if __name__=='__main__': main()
