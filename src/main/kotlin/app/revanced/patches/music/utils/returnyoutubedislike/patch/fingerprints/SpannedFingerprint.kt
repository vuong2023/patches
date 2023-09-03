package app.revanced.patches.music.returnyoutubedislike.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch.Companion.Dislike
import app.revanced.util.bytecode.isWideLiteralExists
import com.android.tools.smali.dexlib2.Opcode

object ChipCloudFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(
        Opcode.NOP,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT, // injection point
        Opcode.RETURN_OBJECT
    ),
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(Dislike) }
)
