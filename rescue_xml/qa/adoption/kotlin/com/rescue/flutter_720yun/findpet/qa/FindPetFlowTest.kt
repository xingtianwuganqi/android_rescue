package com.rescue.flutter_720yun.findpet.qa

import androidx.arch.core.executor.*
import androidx.lifecycle.ViewModelStore
import com.rescue.flutter_720yun.adoption.repository.*
import com.rescue.flutter_720yun.adoption.viewmodels.AdoptionViewModel
import com.rescue.flutter_720yun.findpet.*
import com.rescue.flutter_720yun.home.models.MessageListModel
import com.rescue.flutter_720yun.message.models.MessageBadge
import com.rescue.flutter_720yun.util.UserManager
import kotlinx.coroutines.*
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Real Retrofit + ViewModels; never talks to the deployed server. */
class FindPetFlowTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: FindPetRepository
    private var identity=AdoptionAccount(1,"qa-only",0)
    private var invalidations=0
    private val store=ViewModelStore()
    private val item="""{"recommendation_id":11,"find_id":3,"topic_id":123,"message_id":7,"read":false,"topic_status":"open","topic_available":true,"created_at":"2026-10-14T12:00:00+08:00","imgs":["pet.jpg"],"preview_img":[],"content":"公开介绍","city_name":"上海市"}"""
    private fun page(items: String = item, more: Boolean=false, page: Int=1, state: String="open", unread: Int=1) =
        """{"find_id":3,"state":"$state","items":[$items],"page":$page,"size":20,"total":2,"has_more":$more,"unread_count":$unread}"""
    @Before fun setup() {
        ArchTaskExecutor.getInstance().setDelegate(object: TaskExecutor() {
            override fun executeOnDiskIO(r: Runnable)=r.run()
            override fun postToMainThread(r: Runnable)=r.run()
            override fun isMainThread()=true
        })
        server=MockWebServer().apply { start() }
        val service=Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create(FindPetServices.gson)).build().create(FindPetService::class.java)
        repository=FindPetRepository(service,{identity},{invalidations++})
    }
    @After fun cleanup() { store.clear();server.shutdown();ArchTaskExecutor.getInstance().setDelegate(null) }
    private fun reply(data: String, http: Int=200, code: Int=http) {
        server.enqueue(MockResponse().setResponseCode(http).setHeader("Content-Type","application/json").setBody("""{"code":$code,"message":"qa message","data":$data}"""))
    }
    private fun <T: AdoptionViewModel> track(vm: T): T { store.put(vm.javaClass.name,vm);return vm }
    private fun vm()=track(MyFindPetViewModel(repository))
    private fun editor()=track(PetRequestViewModel(repository))
    private fun idle(vm: AdoptionViewModel) {
        val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5)
        while(vm.busy.value==true && System.nanoTime()<deadline) Thread.sleep(5)
        assertFalse(vm.busy.value==true)
    }
    private fun error(action: suspend ()->Unit): AdoptionError = runBlocking {
        try { action();fail("Expected rejection");error("unreachable") } catch(e: AdoptionError) { e }
    }
    @Test fun recommendationsUseBearerAndNeverAutoRead() = runBlocking {
        reply(page());val data=repository.recommendations()
        val request=server.takeRequest()
        assertEquals("GET",request.method);assertEquals("Bearer qa-only",request.getHeader("Authorization"))
        assertEquals("/api/v3/find-pet/me/recommendations/",request.requestUrl!!.encodedPath)
        assertEquals("20",request.requestUrl!!.queryParameter("size"));assertEquals(1,server.requestCount)
        assertFalse(data.items!!.single().read)
    }
    @Test fun markReadPostsOnlyEmptyJson() = runBlocking {
        reply("""{"recommendation_id":11,"read":true}""")
        repository.read(11)
        val request=server.takeRequest()
        assertEquals("/api/v3/find-pet/recommendations/11/read/",request.path)
        assertEquals("POST",request.method);assertEquals("{}",request.body.readUtf8())
        assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))
    }
    @Test fun http200BusinessRejectionIsNotSuccess() {
        reply("""{"error_code":"ACCOUNT_RESTRICTED"}""",code=403)
        val e=error { repository.recommendations() }
        assertEquals(403,e.status);assertEquals("ACCOUNT_RESTRICTED",e.errorCode);assertEquals(0,invalidations)
    }
    @Test fun active401InvalidatesOnlyCurrentIdentity() {
        reply("""{"error_code":"AUTH_REQUIRED"}""",401)
        assertEquals(401,error { repository.recommendations() }.status);assertEquals(1,invalidations)
    }
    @Test fun oldAccount401CannotInvalidateNewAccount() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"code":401,"data":{"error_code":"AUTH_REQUIRED"}}""").setBodyDelay(200,TimeUnit.MILLISECONDS))
        val job=async(Dispatchers.Default) { try { repository.recommendations();false } catch(e: CancellationException) { true } }
        server.takeRequest(2,TimeUnit.SECONDS)!!
        identity=AdoptionAccount(2,"new-account",1)
        assertTrue(job.await());assertEquals(0,invalidations)
    }
    @Test fun wrongReadTargetAndFalseReadAreRejected() {
        for(data in listOf("""{"recommendation_id":12,"read":true}""","""{"recommendation_id":11,"read":false}""")) {
            reply(data);assertEquals(502,error { repository.read(11) }.status)
        }
    }
    @Test fun readFailureKeepsUnreadAndDoesNotNavigate() {
        val vm=vm();reply(page());vm.load();idle(vm)
        reply("""{"error_code":"TEMPORARILY_UNAVAILABLE"}""",503)
        vm.open(vm.items.value!!.single());idle(vm)
        assertFalse(vm.items.value!!.single().read);assertEquals(1,vm.unread.value)
        assertNull(vm.destination.value)
    }
    @Test fun successfulReadNavigatesWithoutApplyingOrAuthorizing() {
        val vm=vm();reply(page());vm.load();idle(vm)
        reply("""{"recommendation_id":11,"read":true}""");reply(page())
        vm.open(vm.items.value!!.single());idle(vm)
        assertTrue(vm.items.value!!.single().read);assertEquals(0,vm.unread.value)
        assertEquals(123,vm.destination.value?.topicId);assertTrue(vm.destination.value!!.available)
        assertEquals(3,server.requestCount)
    }
    @Test fun unavailableHistoryCanBeReadWithoutOpeningPrivateTopic() {
        val vm=vm();reply(page(item.replace("\"open\"","\"unavailable\"").replace("\"topic_available\":true","\"topic_available\":false")))
        vm.load();idle(vm)
        assertNull(vm.items.value!!.single().image)
        reply("""{"recommendation_id":11,"read":true}""");reply(page(item.replace("\"open\"","\"unavailable\"").replace("\"topic_available\":true","\"topic_available\":false")))
        vm.open(vm.items.value!!.single());idle(vm)
        assertFalse(vm.destination.value!!.available);assertTrue(vm.items.value!!.single().read)
    }
    @Test fun refreshFailurePreservesHistoryAndNextPage() {
        val vm=vm();reply(page(more=true));vm.load();idle(vm)
        reply("{}",503);vm.load();idle(vm)
        assertEquals(1,vm.items.value!!.size);assertEquals(1,vm.page);assertTrue(vm.hasMore)
        reply(page(item.replace(":11,",":12,"),page=2));vm.load(false);idle(vm)
        assertEquals(listOf(11,12),vm.items.value!!.map { it.recommendation_id })
        val calls=List(3) { server.takeRequest() }
        assertEquals(listOf("1","1","2"),calls.map { it.requestUrl!!.queryParameter("page") })
    }
    @Test fun appendedDuplicatesAreMergedByRecommendationId() {
        val vm=vm();reply(page(more=true));vm.load();idle(vm)
        reply(page(page=2));vm.load(false);idle(vm)
        assertEquals(1,vm.items.value!!.size);assertFalse(vm.hasMore)
    }
    @Test fun noRequestAndClosedAreSuccessfulStatesNotErrors() {
        val vm=vm()
        reply(page(items="",state="no_request",unread=0));vm.load();idle(vm)
        assertEquals("no_request",vm.state.value);assertNull(vm.error.value)
        reply(page(state="closed"));vm.load();idle(vm)
        assertEquals("closed",vm.state.value);assertEquals(1,vm.items.value!!.size)
    }
    @Test fun malformedEnvelopeAndNonJsonAreNotEmptySuccess() {
        reply("{}");assertEquals(502,error { repository.recommendations() }.status)
        server.enqueue(MockResponse().setResponseCode(503).setBody("<html>Unavailable</html>"))
        assertEquals(503,error { repository.recommendations() }.status)
    }
    @Test fun infoUsesOriginalBodyTokenAndRecognizesOnlyLegacy207AsAbsent() = runBlocking {
        reply("{}",code=207);assertNull(repository.info())
        val request=server.takeRequest()
        assertEquals("/api/v2/searchpet/info",request.path)
        assertNull(request.getHeader("Authorization"));assertTrue(request.body.readUtf8().contains("token=qa-only"))
        reply("{}",404);assertEquals(404,error { repository.info() }.status)
    }
    @Test fun formSaveUsesOriginalFieldsAndOptionalCityId() = runBlocking {
        reply("{}");repository.save(2,"上海.上海市.浦东新区","真实描述","联系方式","310100")
        val request=server.takeRequest();val body=request.body.readUtf8()
        assertEquals("/api/v2/searchpet/create",request.path)
        assertTrue(request.getHeader("Content-Type")!!.startsWith("application/x-www-form-urlencoded"))
        assertTrue(body.contains("pet_type=2"));assertTrue(body.contains("city_id=310100"));assertTrue(body.contains("contact="))
        assertFalse(body.contains("version"));assertFalse(body.contains("confirmed"))
        assertNull(request.getHeader("Authorization"))
    }
    @Test fun effectiveUsesOriginalOneAndTwo() = runBlocking {
        for(open in listOf(false,true)) {
            reply("{}");repository.setEffective(open)
            val r=server.takeRequest()
            assertEquals("/api/v2/searchpet/effective",r.path)
            assertTrue(r.body.readUtf8().contains("effective="+if(open) "1" else "2"))
        }
    }
    @Test fun editorGetFailureNeverEnablesSave() {
        val vm=editor();reply("{}",503);vm.load();idle(vm)
        assertFalse(vm.loaded)
        vm.address="上海市";vm.description="描述";vm.contact="联系";vm.save();idle(vm)
        assertEquals(1,server.requestCount);assertFalse(vm.saved.value==true)
    }
    @Test fun editorSaveDoesNotCreateAnAdoptionApplication() {
        val vm=editor();reply("{}",code=207);vm.load();idle(vm)
        vm.address="上海.上海市.浦东新区";vm.description="描述";vm.contact="联系"
        reply("{}");reply("""{"id":3,"effective":1,"is_delete":1,"pet_type":1,"address":"上海.上海市.浦东新区","desc":"描述","contact":"联系"}""");vm.save();idle(vm)
        assertTrue(vm.saved.value==true);assertEquals(3,server.requestCount)
        assertEquals("/api/v2/searchpet/info",server.takeRequest().path)
        assertEquals("/api/v2/searchpet/create",server.takeRequest().path)
    }
    @Test fun recommendationUnreadIsIndependentAndAddedOnceToMessageBadge() {
        val rows=listOf(MessageListModel("","",unread=8,category="system"),MessageListModel("","",unread=4,category="my_find_pet"),MessageListModel("","",unread=2,category="like"))
        assertEquals(17,MessageBadge.total(rows,3))
    }
    @Test fun switchingSessionClearsPrivateHistoryAndNavigation() {
        val vm=vm();reply(page());vm.load();idle(vm)
        val old=UserManager.sessionRevision.value ?: 0L
        try {
            UserManager.sessionRevision.value=old+1
            assertTrue(vm.items.value!!.isEmpty());assertNull(vm.state.value);assertNull(vm.unread.value);assertNull(vm.destination.value)
            assertFalse(vm.initialized);assertEquals(0,vm.page)
        } finally { UserManager.sessionRevision.value=old }
    }
    @Test fun originalNotificationsKeepHistoricalTextWithoutRecommendationRouting() = runBlocking {
        reply("""[{"id":1,"content":"旧公告"},{"id":2,"content":"同城推荐","kind":"find_pet_recommendation","recommendation_id":11,"topic_id":123}]""")
        val items=repository.notifications()
        assertEquals("旧公告",items.first().content)
        assertEquals("同城推荐",items.last().content)
        val request=server.takeRequest()
        assertEquals("/api/v1/systemnotification/",request.path)
        assertNull(request.getHeader("Authorization"));assertTrue(request.body.readUtf8().contains("token=qa-only"))
        assertEquals(1,server.requestCount)
    }
    @Test fun unavailableFlagOverridesPublicStatusAndPicture() {
        val rec=FindPetServices.gson.fromJson(item,PetRecommendation::class.java).copy(topic_available=false)
        assertFalse(rec.available);assertNull(rec.image);assertEquals("帖子暂不可用",rec.statusLabel)
    }
    private val publicItem="""{"findId":44,"user_id":9,"effective":1,"is_delete":1,"pet_type":2,"desc":"想领养狗狗","contact":"private-raw-value","liked":false,"collection":false,"likeNum":2,"collectionNum":1,"commNum":3}"""
    private fun publicVm()=com.rescue.flutter_720yun.home.viewmodels.FindPetViewModel(repository,{emptySet()}).also { store.put("public",it) }
    private fun idle(vm: com.rescue.flutter_720yun.home.viewmodels.FindPetViewModel) {
        val end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5)
        while((vm.isLoading.value==true || !vm.operations.value.isNullOrEmpty()) && System.nanoTime()<end) Thread.sleep(5)
        assertFalse(vm.isLoading.value==true);assertTrue(vm.operations.value.isNullOrEmpty())
    }
    @Test fun publicListAllowsAnonymousWithoutBearerOrBodyToken() = runBlocking {
        identity=AdoptionAccount(null,null,0)
        reply("[$publicItem]")
        val items=repository.publicList(1)
        val r=server.takeRequest();val body=r.body.readUtf8()
        assertEquals("/api/v2/findpet/list",r.path);assertNull(r.getHeader("Authorization"))
        assertFalse(body.contains("token="));assertTrue(body.contains("page=1"));assertTrue(body.contains("size=10"))
        assertNull(items.single().contact)
    }
    @Test fun legacyEmptyObjectAndArrayAreSuccessfulButBusinessErrorIsNot() = runBlocking {
        for(body in listOf("{}","[]")) { reply(body);assertTrue(repository.publicList(1).isEmpty()) }
        reply("{}",code=202)
        assertEquals(202,error { repository.publicList(1) }.status)
    }
    @Test fun publicRefreshFailureKeepsSuccessfulPageAndCanAppend() {
        val vm=publicVm()
        reply("[$publicItem]");vm.findPetListNetworking(com.rescue.flutter_720yun.util.RefreshState.REFRESH);idle(vm)
        reply("{}",503);vm.findPetListNetworking(com.rescue.flutter_720yun.util.RefreshState.REFRESH);idle(vm)
        assertEquals(1,vm.page);assertEquals(44,vm.items.value!!.single().findId)
        reply("[${publicItem.replace(":44,",":45,")}]");vm.findPetListNetworking(com.rescue.flutter_720yun.util.RefreshState.MORE);idle(vm)
        assertEquals(listOf(44,45),vm.items.value!!.map { it.findId })
        assertEquals(listOf("1","1","2"),List(3) { server.takeRequest().body.readUtf8() }.map { Regex("page=([0-9]+)").find(it)!!.groupValues[1] })
    }
    @Test fun publicAppendFailureRetriesSamePageAndDeduplicatesIds() {
        val vm=publicVm()
        reply("[$publicItem]");vm.findPetListNetworking(com.rescue.flutter_720yun.util.RefreshState.REFRESH);idle(vm)
        reply("{}",503);vm.findPetListNetworking(com.rescue.flutter_720yun.util.RefreshState.MORE);idle(vm)
        assertEquals(1,vm.page)
        reply("[$publicItem,${publicItem.replace(":44,",":45,")}]");vm.findPetListNetworking(com.rescue.flutter_720yun.util.RefreshState.MORE);idle(vm)
        assertEquals(listOf(44,45),vm.items.value!!.map { it.findId })
        val bodies=List(3) { server.takeRequest().body.readUtf8() }
        assertTrue(bodies[1].contains("page=2"));assertTrue(bodies[2].contains("page=2"))
    }
    @Test fun closedPublicRowsNeverExposeCachedContact() = runBlocking {
        reply("[${publicItem.replace("\"effective\":1","\"effective\":2").dropLast(1)},\"getedcontact\":true,\"contact_info\":\"cached\"}]")
        val row=repository.publicList(1).single()
        assertNull(row.contact);assertNull(row.contact_info);assertFalse(row.getedcontact==true)
    }
    @Test fun publicLikeUpdatesOnlyOnSuccessUsingReturnedState() {
        val vm=publicVm();reply("[$publicItem]");vm.findPetListNetworking(com.rescue.flutter_720yun.util.RefreshState.REFRESH);idle(vm)
        reply("""{"mark":"1","like":1}""");vm.interact(vm.items.value!!.single(),false);idle(vm)
        assertTrue(vm.items.value!!.single().liked==true);assertEquals(3,vm.items.value!!.single().likeNum)
        reply("{}",code=202);vm.interact(vm.items.value!!.single(),false);idle(vm)
        assertTrue(vm.items.value!!.single().liked==true);assertEquals(3,vm.items.value!!.single().likeNum)
        server.takeRequest();val r=server.takeRequest()
        assertEquals("/api/v2/find/action/like",r.path);val body=r.body.readUtf8();assertTrue(body.contains("find_id=44"));assertTrue(body.contains("like_mark=1"))
    }
    @Test fun publicCollectUsesFindIdAndCollectMark() {
        val vm=publicVm();reply("[$publicItem]");vm.findPetListNetworking(com.rescue.flutter_720yun.util.RefreshState.REFRESH);idle(vm)
        reply("""{"mark":"1","collection":1}""");vm.interact(vm.items.value!!.single(),true);idle(vm)
        assertTrue(vm.items.value!!.single().collection==true);assertEquals(2,vm.items.value!!.single().collectionNum)
        server.takeRequest();val r=server.takeRequest();assertEquals("/api/v2/find/action/collect",r.path)
        assertTrue(r.body.readUtf8().contains("collect_mark=1"))
    }
    @Test fun findContactUsesTypeTwoAndActualReturnedContactOnly() {
        val vm=publicVm();reply("[$publicItem]");vm.findPetListNetworking(com.rescue.flutter_720yun.util.RefreshState.REFRESH);idle(vm)
        reply("""{"contact":"authorized-contact"}""");vm.getFindPetContactNetworking(vm.items.value!!.single());idle(vm)
        assertEquals("authorized-contact",vm.copiedContact.value)
        assertNull(vm.items.value!!.single().contact)
        server.takeRequest();val r=server.takeRequest();assertEquals("/api/v1/getcontact/",r.path)
        val body=r.body.readUtf8();assertTrue(body.contains("topic_type=2"));assertTrue(body.contains("topic_id=44"))
    }
    @Test fun deniedContactNeverCopiesPrivateRawContact() {
        val vm=publicVm();reply("[$publicItem]");vm.findPetListNetworking(com.rescue.flutter_720yun.util.RefreshState.REFRESH);idle(vm)
        reply("{}",code=209);vm.getFindPetContactNetworking(vm.items.value!!.single());idle(vm)
        assertNull(vm.copiedContact.value);assertEquals(209,vm.actionError.value?.status)
        assertFalse(vm.items.value!!.single().getedcontact==true)
    }
    @Test fun duplicateLikeTapsMakeOnlyOneWrite() {
        val vm=publicVm();reply("[$publicItem]");vm.findPetListNetworking(com.rescue.flutter_720yun.util.RefreshState.REFRESH);idle(vm)
        server.enqueue(MockResponse().setBody("""{"code":200,"data":{"like":1}}""").setBodyDelay(200,TimeUnit.MILLISECONDS))
        val row=vm.items.value!!.single();vm.interact(row,false);vm.interact(row,false);idle(vm)
        assertEquals(2,server.requestCount);assertEquals(3,vm.items.value!!.single().likeNum)
    }
    @Test fun collectionListUsesLegacyWrappedFindInfo() = runBlocking {
        reply("""[{"collect_id":44,"findInfo":$publicItem}]""")
        assertEquals(44,repository.publicList(1,true).single().findId)
        val r=server.takeRequest();assertEquals("/api/v2/find/collect/list",r.path);assertNull(r.getHeader("Authorization"))
    }
    @Test fun invalidFindIdStopsAllInteractionRequests() {
        assertEquals(400,error { repository.interact(0,false,true) }.status)
        assertEquals(400,error { repository.contact(-1) }.status)
        assertEquals(0,server.requestCount)
    }
    @Test fun notificationRouteResolvesRealTargetFromSecondHistoryPage() {
        val vm=vm()
        reply("""{"recommendation_id":11,"read":true}""")
        reply(page(item.replace(":11,",":12,"),more=true))
        reply(page(page=2))
        vm.open(11,999,true);idle(vm)
        assertEquals(123,vm.destination.value?.topicId)
        assertTrue(vm.destination.value!!.available)
        assertEquals(listOf("POST","GET","GET"),List(3) { server.takeRequest().method })
    }
    @Test fun successfulWriteFollowedByFailedReloadNeverPostsAgain() {
        val vm=editor();reply("{}",code=207);vm.load();idle(vm)
        vm.address="上海.上海市.浦东新区";vm.description="描述";vm.contact="联系"
        reply("{}");reply("{}",503);vm.save();idle(vm)
        assertTrue(vm.writeCommitted.value==true);assertFalse(vm.saved.value==true);assertFalse(vm.loaded)
        vm.save();idle(vm);assertEquals(3,server.requestCount)
        reply("""{"id":3,"effective":1,"is_delete":1}""");vm.load();idle(vm)
        assertTrue(vm.saved.value==true);assertEquals(4,server.requestCount)
        assertEquals(listOf("/api/v2/searchpet/info","/api/v2/searchpet/create","/api/v2/searchpet/info","/api/v2/searchpet/info"),List(4) { server.takeRequest().path })
    }
    @Test fun closingRequeriesActualRequestBeforeReturningSuccess() {
        val vm=editor();reply("""{"id":3,"effective":1,"is_delete":1}""");vm.load();idle(vm)
        reply("{}");reply("""{"id":3,"effective":2,"is_delete":1}""");vm.setEffective(false);idle(vm)
        assertEquals(2,vm.info.value?.effective);assertTrue(vm.saved.value==true)
        assertEquals(3,server.requestCount)
    }
    @Test fun unknownWriteOutcomeQueriesInfoBeforeAnotherConfirmation() {
        val vm=editor();reply("{}",code=207);vm.load();idle(vm)
        vm.address="上海市";vm.description="草稿";vm.contact="联系"
        reply("{}",503);reply("""{"id":3,"effective":1,"is_delete":1}""");vm.save();idle(vm)
        assertFalse(vm.saved.value==true);assertEquals("草稿",vm.description)
        assertTrue(vm.error.value!!.message.contains("结果未知"));assertEquals(3,server.requestCount)
    }
    private fun catalogues(): Pair<List<com.rescue.flutter_720yun.home.models.AddressItem>,List<com.rescue.flutter_720yun.home.models.AddressItem>> {
        val location=java.io.File(System.getProperty("qa.adoption.location"))
        return com.google.gson.Gson().fromJson(location.readText(),Array<com.rescue.flutter_720yun.home.models.AddressItem>::class.java).toList() to
            com.google.gson.Gson().fromJson(java.io.File(location.parentFile,"city.json").readText(),Array<com.rescue.flutter_720yun.home.models.AddressItem>::class.java).toList()
    }
    @Test fun municipalCityIdsUseBothBundledCatalogues() {
        val (provinces,cities)=catalogues()
        for((name,id) in listOf("北京" to "110100","天津" to "120100","上海" to "310100","重庆" to "500100"))
            assertEquals(id,FindPetCities.resolve("$name.${name}市.全市",provinces,cities))
    }
    @Test fun twoDistrictsOfSameCityResolveToSameCityCode() {
        val (provinces,cities)=catalogues()
        assertEquals("310100",FindPetCities.resolve("上海.上海市.浦东新区",provinces,cities))
        assertEquals("310100",FindPetCities.resolve("上海.上海市.黄浦区",provinces,cities))
    }
    @Test fun ambiguousCountyOrMismatchedProvinceOmitsCityId() {
        val (provinces,cities)=catalogues()
        for(address in listOf("东城区","北京市.浦东新区","河南省.杭州市.全市","未知省.未知市.城区")) assertNull(FindPetCities.resolve(address,provinces,cities))
    }
    @Test fun latestUnavailableRecommendationReplacesStalePublicCardBeforeNavigation() {
        val vm=vm();reply(page());vm.load();idle(vm)
        reply("""{"recommendation_id":11,"read":true}""")
        reply(page(item.replace("\"topic_available\":true","\"topic_available\":false")))
        vm.open(vm.items.value!!.single());idle(vm)
        assertFalse(vm.items.value!!.single().available);assertNull(vm.items.value!!.single().image)
        assertFalse(vm.destination.value!!.available)
    }

    @Test fun legacyHttp403Business401InvalidatesCurrentAccount() {
        reply("{}",http=403,code=401)
        val failure=error { repository.info() }
        assertEquals(401,failure.status);assertEquals(401,failure.code);assertEquals(1,invalidations)
    }
    @Test fun legacyHttp200Business401StillInvalidatesCurrentAccount() {
        reply("{}",http=200,code=401)
        assertEquals(401,error { repository.info() }.status);assertEquals(1,invalidations)
    }
    @Test fun legacyHttp401WithoutBusinessJsonStillInvalidatesCurrentAccount() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("<html>Unauthorized</html>"))
        assertEquals(401,error { repository.info() }.status);assertEquals(1,invalidations)
    }
    @Test fun genuineLegacy403DoesNotInvalidateAccount() {
        reply("""{"error_code":"ACCOUNT_RESTRICTED"}""",http=403,code=403)
        val failure=error { repository.info() }
        assertEquals(403,failure.status);assertEquals("ACCOUNT_RESTRICTED",failure.errorCode)
        assertEquals(0,invalidations)
    }
    @Test fun legacyBusiness202DoesNotInvalidateAccount() {
        reply("{}",http=200,code=202)
        assertEquals(202,error { repository.info() }.status);assertEquals(0,invalidations)
    }
    @Test fun v3Http403IsNotNormalizedByLegacyBusiness401Rule() {
        reply("""{"error_code":"ACCOUNT_RESTRICTED"}""",http=403,code=401)
        val failure=error { repository.recommendations() }
        assertEquals(403,failure.status);assertEquals("ACCOUNT_RESTRICTED",failure.errorCode)
        assertEquals(0,invalidations)
    }
    @Test fun oldLegacyHttp403Business401CannotLogOutNewAccount() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"code":401,"data":{}}""")
            .setBodyDelay(200,TimeUnit.MILLISECONDS))
        val result=async(Dispatchers.Default) { try { repository.info();false } catch(e: CancellationException) { true } }
        server.takeRequest(2,TimeUnit.SECONDS)!!;identity=AdoptionAccount(2,"new-account",1)
        assertTrue(result.await());assertEquals(0,invalidations)
    }
    @Test fun originalUnreadUsesBodyTokenAndKeepsSystemTotal() = runBlocking {
        reply("""{"sys_unread":7,"like_unread":2,"collec_unread":1,"com_unread":0}""")
        assertEquals(7,repository.systemUnread().sys_unread)
        val r=server.takeRequest();assertEquals("/api/v1/authunreadnum/",r.path)
        assertNull(r.getHeader("Authorization"));assertTrue(r.body.readUtf8().contains("token=qa-only"))
    }
    @Test fun oldSystemUnread401DoesNotLogOutNewAccount() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"code":401}""").setBodyDelay(200,TimeUnit.MILLISECONDS))
        val result=async(Dispatchers.Default) { try { repository.systemUnread();false } catch(e: CancellationException) { true } }
        server.takeRequest(2,TimeUnit.SECONDS)!!;identity=AdoptionAccount(2,"new-account",1)
        assertTrue(result.await());assertEquals(0,invalidations)
    }

    @Test fun independentFindPetUnreadUsesOriginalBodyToken() = runBlocking {
        reply("""{"sys_unread":0,"like_unread":1,"collec_unread":0,"com_unread":0,"find_pet_unread":1}""")
        val counts=repository.systemUnread()
        assertEquals(0,counts.sys_unread);assertEquals(1,counts.find_pet_unread)
        val rows=listOf(MessageListModel("","",unread=counts.sys_unread,category="system"),
            MessageListModel("","",unread=counts.like_unread,category="like"),
            MessageListModel("","",unread=counts.find_pet_unread,category="my_find_pet"))
        assertEquals(2,MessageBadge.total(rows,null))
        val request=server.takeRequest()
        assertEquals("/api/v1/authunreadnum/",request.path)
        assertTrue(request.body.readUtf8().contains("token=qa-only"))
        assertEquals(1,server.requestCount)
    }
    @Test fun absentOrNullFindPetUnreadIsZeroForOlderBackend() = runBlocking {
        for(field in listOf("",",\"find_pet_unread\":null")) {
            reply("""{"sys_unread":7,"like_unread":2,"collec_unread":1,"com_unread":0$field}""")
            val counts=repository.systemUnread()
            assertEquals(0,counts.find_pet_unread);assertEquals(7,counts.sys_unread)
        }
    }
    @Test fun messageAssociationIsNotUsedForReadOrTopicNavigation() {
        val vm=vm();reply(page());vm.load();idle(vm)
        assertEquals(7,vm.items.value!!.single().message_id)
        reply("""{"recommendation_id":11,"read":true}""");reply(page())
        vm.open(vm.items.value!!.single());idle(vm)
        assertEquals(123,vm.destination.value?.topicId)
        assertEquals("/api/v3/find-pet/me/recommendations/",server.takeRequest().requestUrl!!.encodedPath)
        assertEquals("/api/v3/find-pet/recommendations/11/read/",server.takeRequest().path)
        assertEquals("/api/v3/find-pet/me/recommendations/",server.takeRequest().requestUrl!!.encodedPath)
    }
    @Test fun successfulReadStaysReadWhenTargetRefreshFails() {
        val vm=vm();reply(page());vm.load();idle(vm)
        reply("""{"recommendation_id":11,"read":true}""");reply("{}",503)
        vm.open(vm.items.value!!.single());idle(vm)
        assertTrue(vm.items.value!!.single().read);assertEquals(0,vm.unread.value)
        assertNull(vm.destination.value);assertNotNull(vm.error.value)
    }
    @Test fun repeatedReadDoesNotDecrementUnreadTwice() {
        val vm=vm();reply(page(unread=2));vm.load();idle(vm)
        repeat(2) {
            reply("""{"recommendation_id":11,"read":true}""");reply(page())
            vm.open(vm.items.value!!.single());idle(vm)
        }
        assertEquals(1,vm.unread.value)
    }
    @Test fun systemNotificationReadDoesNotChangeRecommendationState() = runBlocking {
        val vm=vm();reply(page());vm.load();idle(vm)
        val revision=FindPetChanges.revision.value
        reply("""[{"id":1,"content":"旧公告"}]""");repository.notifications()
        assertEquals(revision,FindPetChanges.revision.value)
        assertFalse(vm.items.value!!.single().read);assertEquals(1,vm.unread.value)
        reply("""{"sys_unread":0,"find_pet_unread":1}""")
        assertEquals(1,repository.systemUnread().find_pet_unread)
    }
    @Test fun oldAccountSuccessfulUnreadResponseCannotOverwriteNewAccount() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"code":200,"data":{"sys_unread":8,"find_pet_unread":9}}""").setBodyDelay(200,TimeUnit.MILLISECONDS))
        val result=async(Dispatchers.Default) { try { repository.systemUnread();false } catch(e: CancellationException) { true } }
        server.takeRequest(2,TimeUnit.SECONDS)!!;identity=AdoptionAccount(2,"new-account",1)
        assertTrue(result.await());assertEquals(0,invalidations)
    }

}
