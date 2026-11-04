package com.rescue.flutter_720yun.home.qa

import androidx.arch.core.executor.*
import androidx.lifecycle.ViewModelStore
import com.rescue.flutter_720yun.home.viewmodels.HomeViewModel
import com.rescue.flutter_720yun.home.repository.FeedRepository
import com.rescue.flutter_720yun.network.HomeService
import com.rescue.flutter_720yun.util.*
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Exercises real HomeViewModel paging against HTTP, using the existing QA main dispatcher. */
class HomeFeedAuditTest {
    private lateinit var server: MockWebServer
    private lateinit var vm: HomeViewModel
    private val store=ViewModelStore()
    private var oldUser: com.rescue.flutter_720yun.home.models.UserInfo?=null
    private var oldRevision: Long?=null
    @Before fun setup() {
        ArchTaskExecutor.getInstance().setDelegate(object: TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable)=runnable.run()
            override fun postToMainThread(runnable: Runnable)=runnable.run()
            override fun isMainThread()=true
        })
        oldUser=UserManager.userInfo;oldRevision=UserManager.sessionRevision.value
        UserManager::class.java.getDeclaredField("_userInfo").apply { isAccessible=true }.set(UserManager,null)
        server=MockWebServer().apply { start() }
        val service=Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build().create(HomeService::class.java)
        vm=HomeViewModel(FeedRepository(service),{ emptySet() });store.put("feed",vm)
    }
    @After fun cleanup() {
        store.clear();server.shutdown()
        UserManager::class.java.getDeclaredField("_userInfo").apply { isAccessible=true }.set(UserManager,oldUser)
        androidx.lifecycle.LiveData::class.java.getDeclaredField("mData").apply { isAccessible=true }.set(UserManager.sessionRevision,oldRevision)
        ArchTaskExecutor.getInstance().setDelegate(null)
    }
    private fun reply(page: Int,ids: List<Int>,snapshot: String="old",more: Boolean=true,data: String?=null) {
        val items=data ?: ids.joinToString(",","[","]") { """{"topic_id":$it}""" }
        server.enqueue(MockResponse().setBody("""{"code":200,"data":$items,"meta":{"page":$page,"size":10,"snapshot_id":"$snapshot","has_more":$more}}"""))
    }
    private fun fail(code: Int=503,error: String="RETRY_LATER") {
        server.enqueue(MockResponse().setResponseCode(code).setBody("""{"code":$code,"message":"请重试","data":{"error_code":"$error"}}"""))
    }
    private fun idle() {
        val end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5)
        while(vm.isLoading.value==true && System.nanoTime()<end) Thread.sleep(5)
        assertFalse(vm.isLoading.value==true)
    }
    private fun request(): Map<String,String> {
        val body=server.takeRequest(1,TimeUnit.SECONDS)!!.body.readUtf8()
        return body.split('&').associate { part -> val pieces=part.split('=',limit=2)
            java.net.URLDecoder.decode(pieces[0],"UTF-8") to java.net.URLDecoder.decode(pieces.getOrElse(1){""},"UTF-8") }
    }
    @Test fun failedSameScopeRefreshAfterThreePagesKeepsOldSnapshotAndPageFour() {
        repeat(3) { page -> reply(page+1,listOf(page+1));vm.loadListData(if(page==0) RefreshState.REFRESH else RefreshState.MORE);idle();request() }
        fail();vm.loadListData(RefreshState.REFRESH);idle()
        val refresh=request();assertEquals("1",refresh["page"]);assertNull(refresh["snapshot_id"])
        assertEquals(listOf(1,2,3),vm.models.value!!.map { it.topic_id })
        reply(4,listOf(1,4),more=false);vm.loadListData(RefreshState.MORE);idle()
        val append=request();assertEquals("old",append["snapshot_id"]);assertEquals("4",append["page"])
        assertEquals(listOf(1,2,3,4),vm.models.value!!.map { it.topic_id });assertTrue(vm.isLastPage.value==true)
    }
    @Test fun invalidRefreshDataDoesNotCommitMetaAndSuccessfulEmptyBatchCanContinue() {
        reply(1,listOf(1));vm.loadListData(RefreshState.REFRESH);idle();request()
        reply(1,emptyList(),"bad",data="{}");vm.loadListData(RefreshState.REFRESH);idle();request()
        reply(2,listOf(2));vm.loadListData(RefreshState.MORE);idle();assertEquals("old",request()["snapshot_id"])
        reply(1,emptyList(),"new");vm.loadListData(RefreshState.REFRESH);idle();request();assertTrue(vm.models.value!!.isEmpty())
        reply(2,listOf(1),"new",false);vm.loadListData(RefreshState.MORE);idle();val req=request()
        assertEquals("2",req["page"]);assertEquals("new",req["snapshot_id"]);assertEquals(listOf(1),vm.models.value!!.map { it.topic_id })
    }
    @Test fun failedCitySwitchNeverAppendsTheOldCity() {
        reply(1,listOf(1));vm.localListNetworking("上海市",RefreshState.REFRESH);idle();request()
        fail();vm.localListNetworking("北京市",RefreshState.REFRESH);idle();request();assertTrue(vm.models.value!!.isEmpty())
        reply(1,listOf(8),"beijing");vm.localListNetworking("北京市",RefreshState.MORE);idle();val req=request()
        assertEquals("1",req["page"]);assertNull(req["snapshot_id"]);assertEquals("北京市",req["address"])
    }
    @Test fun expiredSnapshotIsInvalidatedBeforeAutomaticFirstPageRetry() {
        reply(1,listOf(1));vm.loadListData(RefreshState.REFRESH);idle();request()
        fail(409,"FEED_SNAPSHOT_EXPIRED");reply(1,listOf(9),"renewed",false)
        vm.loadListData(RefreshState.MORE);idle()
        assertEquals("old",request()["snapshot_id"])
        val fresh=request();assertEquals("1",fresh["page"]);assertNull(fresh["snapshot_id"])
        assertEquals(listOf(9),vm.models.value!!.map { it.topic_id })
    }
    @Test fun identityRevisionInvalidatesOldBatchImmediately() {
        reply(1,listOf(1));vm.loadListData(RefreshState.REFRESH);idle();request()
        UserManager.sessionRevision.value=(UserManager.sessionRevision.value ?: 0)+1
        assertTrue(vm.models.value!!.isEmpty())
        reply(1,listOf(9),"another",false);vm.loadListData(RefreshState.MORE);idle()
        val fresh=request();assertEquals("1",fresh["page"]);assertNull(fresh["snapshot_id"])
    }
}
