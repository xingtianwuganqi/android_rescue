package com.rescue.flutter_720yun.adoption.models

/** Mirrors iOS AdoptionApplicationState and the detail button routing order. */
object AdoptionDetailPolicy {
    enum class Route { MANAGE, ENDED, UNAVAILABLE, APPLICATION, APPLY, BLOCKED }
    fun route(state: ApplicationState): Route = when {
        state.can_manage -> Route.MANAGE
        state.workflow_status in listOf("adopted","closed") -> Route.ENDED
        state.workflow_status != "open" -> Route.UNAVAILABLE
        state.application_status in listOf("applying","communicating","ended") -> Route.APPLICATION
        state.can_apply -> Route.APPLY
        else -> Route.BLOCKED
    }
    fun title(state: ApplicationState): String = when {
        state.can_manage -> "管理申请"
        state.workflow_status in listOf("adopted","closed") -> "送养已结束"
        state.workflow_status != "open" -> "状态暂不可用"
        state.application_status == "applying" -> "申请中"
        state.application_status == "communicating" -> if (state.contact_authorized) "查看联系方式" else "沟通中"
        state.application_status == "ended" -> if (state.can_apply && state.application_result == "not_completed") "查看结果 / 重新申请" else "申请已结束"
        state.can_apply -> "申请领养"
        else -> "暂不可申请"
    }
    fun title(app: AdoptionApplication): String = when {
        app.status == "ended" && app.result == "not_completed" && app.end_reason == "owner_ended" && app.end_note == "拒绝沟通申请" -> "被拒绝"
        app.status == "applying" -> "申请中"
        app.status == "communicating" -> "沟通中"
        app.result == "completed" -> "已结束 · 完成送养（送养人报告）"
        else -> "已结束 · 未完成送养"
    }
    fun profileReady(data: ProfileData): Boolean {
        val p = data.profile ?: return false
        if (!data.is_complete || (data.version ?: p.version ?: 0) <= 0 || p.age == null || p.age !in 10..100 ||
            p.residence_city.isNullOrBlank() || p.residence_district.isNullOrBlank() ||
            p.housing_type !in AdoptionLabels.housing || p.employment_status !in AdoptionLabels.employment) return false
        val matcher = java.util.regex.Pattern.compile("\\X").matcher(p.pet_experience.orEmpty())
        var count = 0
        while (matcher.find()) if (++count > 200) return false
        return true
    }
    fun canReapply(state: ApplicationState?, app: AdoptionApplication) = state?.workflow_status == "open" &&
        state.can_apply && app.status == "ended" && app.result == "not_completed"
}
