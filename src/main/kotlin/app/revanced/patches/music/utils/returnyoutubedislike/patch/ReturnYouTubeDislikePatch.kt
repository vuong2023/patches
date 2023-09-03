package app.revanced.patches.music.utils.returnyoutubedislike.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.shared.fingerprints.returnyoutubedislike.DislikeFingerprint
import app.revanced.patches.shared.fingerprints.returnyoutubedislike.LikeFingerprint
import app.revanced.patches.shared.fingerprints.returnyoutubedislike.RemoveLikeFingerprint
import app.revanced.patches.shared.fingerprints.returnyoutubedislike.TextComponentAtomicReferenceFingerprint
import app.revanced.patches.shared.fingerprints.returnyoutubedislike.TextComponentAtomicReferenceLegacyFingerprint
import app.revanced.patches.shared.fingerprints.returnyoutubedislike.TextComponentConstructorFingerprint
import app.revanced.patches.shared.fingerprints.returnyoutubedislike.TextComponentContextFingerprint
import app.revanced.patches.shared.fingerprints.returnyoutubedislike.TextComponentTmpFingerprint

import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.music.utils.videoid.patch.VideoIdPatch
import app.revanced.util.integrations.Constants.MUSIC_UTILS_PATH

import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.Reference

@Patch
@Name("Return YouTube Dislike")
@Description("Shows the dislike count of videos using the Return YouTube Dislike API.")
@DependsOn(
    [
        SettingsPatch::class,
        VideoIdPatch::class
    ]
)
@MusicCompatibility

class ReturnYouTubeDislikePatch : BytecodePatch(
    listOf(
        DislikeFingerprint,
        LikeFingerprint,
        RemoveLikeFingerprint,
        TextComponentConstructorFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {
        listOf(
            LikeFingerprint.toPatch(Vote.LIKE),
            DislikeFingerprint.toPatch(Vote.DISLIKE),
            RemoveLikeFingerprint.toPatch(Vote.REMOVE_LIKE)
        ).forEach { (fingerprint, vote) ->
            with(fingerprint.result ?: throw fingerprint.exception) {
                mutableMethod.addInstructions(
                    0,
                    """
                    const/4 v0, ${vote.value}
                    invoke-static {v0}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->sendVote(I)V
                    """
                )
            }
        }


        TextComponentConstructorFingerprint.result?.let { parentResult ->

            TextComponentContextFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    val conversionContextIndex = it.scanResult.patternScanResult!!.startIndex
                    conversionContextFieldReference =
                        getInstruction<ReferenceInstruction>(conversionContextIndex).reference
                }
            } ?: throw TextComponentContextFingerprint.exception

            TextComponentTmpFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    val startIndex = it.scanResult.patternScanResult!!.startIndex
                    tmpRegister =
                        getInstruction<FiveRegisterInstruction>(startIndex).registerE
                }
            } ?: throw TextComponentTmpFingerprint.exception


            val textComponentAtomicReferenceResult =
                TextComponentAtomicReferenceFingerprint.also {
                    it.resolve(context, parentResult.classDef)
                }.result
                    ?: TextComponentAtomicReferenceLegacyFingerprint.also {
                        it.resolve(context, parentResult.classDef)
                    }.result
                    ?: throw TextComponentAtomicReferenceLegacyFingerprint.exception

            TextComponentAtomicReferenceFingerprint.also {
                it.resolve(context, parentResult.classDef)
            }.result?.let {
                it.mutableMethod.apply {
                    val startIndex = it.scanResult.patternScanResult!!.startIndex
                    val originalRegisterA =
                        getInstruction<TwoRegisterInstruction>(startIndex + 2).registerA

                    replaceInstruction(
                        startIndex + 2,
                        "move-object v$originalRegisterA, v$tmpRegister"
                    )
                    replaceInstruction(
                        startIndex + 1,
                        "move-result-object v$tmpRegister"
                    )
                }
            }

            textComponentAtomicReferenceResult.let {
                it.mutableMethod.apply {
                    val atomicReferenceStartIndex = it.scanResult.patternScanResult!!.startIndex
                    val insertIndex = it.scanResult.patternScanResult!!.endIndex
                    val moveCharSequenceInstruction =
                        getInstruction<TwoRegisterInstruction>(insertIndex)

                    val atomicReferenceRegister =
                        getInstruction<FiveRegisterInstruction>(atomicReferenceStartIndex).registerC

                    val charSequenceRegister =
                        moveCharSequenceInstruction.registerB

                    addInstructions(
                        insertIndex + 1, """
                            move-object/from16 v$tmpRegister, p0
                            iget-object v$tmpRegister, v$tmpRegister, $conversionContextFieldReference
                            invoke-static {v$tmpRegister, v$atomicReferenceRegister, v$charSequenceRegister}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->onComponentCreated(Ljava/lang/Object;Ljava/util/concurrent/atomic/AtomicReference;Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                            move-result-object v$charSequenceRegister
                            move-object v${moveCharSequenceInstruction.registerA}, v${charSequenceRegister}
                            """
                    )
                    removeInstruction(insertIndex)
                }
            }
        } ?: throw TextComponentConstructorFingerprint.exception


        VideoIdPatch.injectCall("$INTEGRATIONS_RYD_CLASS_DESCRIPTOR->newVideoLoaded(Ljava/lang/String;)V")

        /**
         * Add ReVanced Settings
         */
        SettingsPatch.addReVancedMusicPreference("ryd_settings")
        SettingsPatch.addRYDPreference("revanced_ryd_enable", "true")
        SettingsPatch.addRYDPreference("revanced_ryd_compact_layout", "false")
        SettingsPatch.addRYDPreference("revanced_ryd_dislike_percentage", "false")
    }

    private companion object {
        const val INTEGRATIONS_RYD_CLASS_DESCRIPTOR =
            "$MUSIC_UTILS_PATH/ReturnYouTubeDislikePatch;"

        lateinit var conversionContextFieldReference: Reference
        var tmpRegister: Int = 12
    }

    private fun MethodFingerprint.toPatch(voteKind: Vote) = VotePatch(this, voteKind)

    private data class VotePatch(val fingerprint: MethodFingerprint, val voteKind: Vote)

    private enum class Vote(val value: Int) {
        LIKE(1),
        DISLIKE(-1),
        REMOVE_LIKE(0)
    }
}
