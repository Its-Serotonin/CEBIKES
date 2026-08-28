import com.serotonin.cebikes.entity.AbstractBikeEntity
import net.minecraft.client.render.entity.LivingEntityRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.RotationAxis
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(LivingEntityRenderer::class)
abstract class LivingEntityRendererMixin {

    @Inject(method = ["setupTransforms"], at = [At("TAIL")])
    private fun <T : LivingEntity> injectBikeRoll(
        entity: T,
        matrices: MatrixStack,
        animationProgress: Float,
        bodyYaw: Float,
        tickDelta: Float,
        scale: Float,
        ci: CallbackInfo
    ) {
        val bike = entity.vehicle as? AbstractBikeEntity ?: return

        // Vanilla's setupTransforms applied (180 - bodyYaw) degrees around Y,
        // where bodyYaw is already lerped between prevBodyYaw and bodyYaw by tickDelta.
        // Correct this to (180 - lerpedBikeYaw) by applying the delta so the body
        // always faces the bike's heading regardless of where the player looks.
        val lerpedBikeYaw = MathHelper.lerpAngleDegrees(tickDelta, bike.prevYaw, bike.yaw)
        val yawCorrection = bodyYaw - lerpedBikeYaw
        if (yawCorrection != 0f) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawCorrection))
        }

        // Rotate around the bike's roll axis (the entity origin), which is seatHeight
        // blocks below the player's feet. This causes the player model to both lean
        // AND shift laterally/vertically to follow the seat as the bike rolls.
        val roll = bike.smoothedRollRad
        if (roll == 0f) return
        val seatHeight = bike.passengerOffset().y
        matrices.translate(0.0, -seatHeight, 0.0)
        matrices.multiply(RotationAxis.NEGATIVE_Z.rotation(roll))
        matrices.translate(0.0, seatHeight, 0.0)
    }
}