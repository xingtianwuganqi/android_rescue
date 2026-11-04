package com.rescue.flutter_720yun.review

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.lifecycle.ViewModelStore
import com.rescue.flutter_720yun.adoption.repository.AdoptionAccount
import com.rescue.flutter_720yun.findpet.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Characterizes a requirement violation. A green test proves the current gap, not acceptance. */
class FrontDemandGapProofTest {
    @Test fun read404CurrentlyLeavesUnavailableRecommendationPublic() {
        ArchTaskExecutor.getInstance().setDelegate(object: TaskExecutor() {
            override fun isMainThread()=true
            override fun postToMainThread(runnable: Runnable)=runnable.run()
            override fun executeOnDiskIO(runnable: Runnable)=runnable.run()
        })
        val server=MockWebServer().apply { start() }
        val store=ViewModelStore()
        try {
            val service=Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create(FindPetServices.gson)).build().create(FindPetService::class.java)
            val source=FindPetRepository(service,{ AdoptionAccount(1,"local-review",0) },{ error("must not invalidate") })
            val vm=MyFindPetViewModel(source);store.put("review",vm)
            server.enqueue(MockResponse().setBody("""{"code":200,"data":{"state":"open","page":1,"has_more":false,"unread_count":1,"items":[{"recommendation_id":11,"topic_id":123,"message_id":7,"topic_available":true,"topic_status":"open","read":false,"content":"old public content","imgs":["old.jpg"]}]}}"""))
            fun idle() {
                val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5)
                while(vm.busy.value==true && System.nanoTime()<deadline) Thread.sleep(5)
                assertFalse(vm.busy.value==true)
            }
            vm.load();idle()
            server.enqueue(MockResponse().setResponseCode(404).setBody("""{"code":404,"message":"资源不可用","data":{"error_code":"RESOURCE_UNAVAILABLE"}}"""))
            vm.open(vm.items.value!!.single());idle()
            assertEquals(404,vm.error.value?.status)
            assertNull(vm.destination.value)
            // These retained public fields are the defect; the required placeholder is absent.
            val retained=vm.items.value!!.single()
            assertTrue(retained.available)
            assertEquals("old public content",retained.content)
            assertEquals("old.jpg",retained.image)
            assertFalse(retained.read)
        } finally { store.clear();server.shutdown();ArchTaskExecutor.getInstance().setDelegate(null) }
    }
}
