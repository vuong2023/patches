package app.revanced.patches.music.returnyoutubedislike.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch.Companion.Dislike
import app.revanced.util.bytecode.isWideLiteralExists
import com.android.tools.smali.dexlib2.Opcode

object SpannedFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(
        Opcode.PACKED_SWITCH,
        Opcode.GOTO_16
    ),
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(Dislike) }
)
