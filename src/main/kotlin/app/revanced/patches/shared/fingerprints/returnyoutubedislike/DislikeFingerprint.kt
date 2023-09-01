package app.revanced.patches.shared.fingerprints.returnyoutubedislike

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object DislikeFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("like/dislike")
)