package com.rescue.flutter_720yun.adoption.models

object AdoptionOperations {
    fun actions(app: AdoptionApplication, mine: Boolean): List<String> {
        if(app.topic == null || app.topic.unavailable || app.topic.is_delete == 0) return emptyList()
        return if(mine) { if(app.status in listOf("applying", "communicating")) listOf("abandon") else emptyList() }
        else when(app.status) {
            "applying" -> listOf("communicate", "reject")
            "communicating" -> listOf("end") + if(app.topic.is_complete != true) listOf("complete") else emptyList()
            else -> emptyList()
        }
    }
    fun title(action: String) = when(action) {
        "communicate" -> "同意沟通"; "reject" -> "拒绝申请"; "end" -> "结束沟通"
        "complete" -> "完成送养"; else -> "放弃申请"
    }
    fun status(app: AdoptionApplication)=AdoptionLabels.status(app.status,app.result,app.end_reason,app.end_note)
}
