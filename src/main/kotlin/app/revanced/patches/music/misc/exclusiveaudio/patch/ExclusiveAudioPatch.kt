package app.revanced.patches.music.misc.exclusiveaudio.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.music.misc.exclusiveaudio.fingerprints.MusicBrowserServiceFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.fix.decoding.patch.DecodingPatch
import app.revanced.util.bytecode.getStringIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Patch
@Name("Exclusive audio playback")
@Description("Enables the option to play music without video.")
@DependsOn([DecodingPatch::class])
@MusicCompatibility
@Version("0.0.1")
class ExclusiveAudioPatch : BytecodePatch(
    listOf(MusicBrowserServiceFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        MusicBrowserServiceFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getStringIndex("MBS: Return empty root for client: %s, isFullMediaBrowserEnabled: %b, is client browsable: %b, isRedAccount: %b")

                for (index in targetIndex downTo 0) {
                    if (getInstruction(index).opcode != Opcode.INVOKE_VIRTUAL) continue

                    val targetReference = getInstruction<ReferenceInstruction>(index).reference

                    if (!targetReference.toString().endsWith("()Z")) continue

                    with(
                        context
                            .toMethodWalker(it.method)
                            .nextMethod(index, true)
                            .getMethod() as MutableMethod
                    ) {
                        addInstructions(
                            0, """
                                const/4 v0, 0x1
                                return v0
                                """
                        )
                    }
                    break
                }
            }
        } ?: return MusicBrowserServiceFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
