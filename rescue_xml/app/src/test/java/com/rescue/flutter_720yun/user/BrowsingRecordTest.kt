package com.rescue.flutter_720yun.user

import com.google.gson.JsonParser
import com.rescue.flutter_720yun.user.viewmodels.BrowsingRecord
import org.junit.Assert.*
import org.junit.Test

class BrowsingRecordTest {
    private fun parse(json: String)=BrowsingRecord.parse(JsonParser.parseString(json).asJsonObject)
    @Test fun historyUsesNestedRealTopicAndSupportsBothImageShapes() {
        val row=parse("""{"history_id":7,"topic_id":999,"topicInfo":{"topic_id":123,"content":"猫","imgs":["cat.jpg"],"contact_info":"private"}}""")!!
        assertEquals(123,row.topic);assertEquals("cat.jpg",row.image);assertTrue(row.available)
        assertEquals("cat.jpg",parse("""{"topicInfo":{"topic_id":123,"imgs":"cat.jpg,dog.jpg"}}""")!!.image)
        assertNull(parse("""{"topic_id":123,"topicInfo":null}"""))
        assertFalse(parse("""{"topicInfo":{"topic_id":123,"is_delete":0}}""")!!.available)
    }
}
