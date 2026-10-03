package com.rescue.flutter_720yun.adoption.models

import java.security.MessageDigest

/** A non-reversible content identity lets a restored confirmation reuse its request key. */
object ApplicationFingerprint {
    fun of(user: Int, topic: Int, body: ApplicationWrite): String = MessageDigest.getInstance("SHA-256")
        .digest("$user:$topic:${body.profile_version}:${body.statement}".toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

object ApplicationAttempt {
    fun key(previousKey: String?, previousFingerprint: String?, fingerprint: String): String =
        previousKey?.takeIf { previousFingerprint == fingerprint } ?: java.util.UUID.randomUUID().toString()
}
