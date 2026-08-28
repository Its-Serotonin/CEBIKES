package com.serotonin.cebikes.mixin.client

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
        val lerpedBikeYaw = MathHelper.lerpAngleDegrees(tickDelta, bike.prevYaw, bike.yaw)
        val yawCorrection = bodyYaw - lerpedBikeYaw
        if (yawCorrection != 0f) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawCorrection))
        }

        val roll = bike.smoothedRollRad
        if (roll == 0f) return
        val seatHeight = bike.passengerOffset().y
        val driftCorrection = 0.66

        matrices.translate(0.0, -seatHeight * driftCorrection, 0.0)
        matrices.multiply(RotationAxis.NEGATIVE_Z.rotation(roll))
        matrices.translate(0.0, seatHeight * driftCorrection, 0.0)
    }
}