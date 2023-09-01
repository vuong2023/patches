package app.revanced.patches.shared.fingerprints.returnyoutubedislike

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object LikeFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("like/like")
)