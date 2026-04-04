package com.serotonin.cebikes.client.entity.model

import com.serotonin.cebikes.entity.AbstractBikeEntity
import net.minecraft.client.model.*
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.entity.model.EntityModel
import net.minecraft.client.util.math.MatrixStack
import kotlin.math.PI

/**
 * Simple placeholder bike model.
 *
 * Texture layout (64×64):
 *   Frame (3×3×16):       UV  0,  0  → 38 wide, 19 tall
 *   FrontWheel (2×10×10): UV  0, 19  → 24 wide, 20 tall
 *   RearWheel  (2×10×10): UV 24, 19  → 24 wide, 20 tall
 *   Handlebar (12×2×2):   UV  0, 39  → 28 wide,  4 tall
 *   Seat       (4×2×5):   UV 28, 39  → 18 wide,  7 tall
 *
 * Model space origin = wheel axle height (entity vertical centre).
 * Positive Y = downward (standard MC model convention).
 */
class BikeEntityModel<T : AbstractBikeEntity>(root: ModelPart) : EntityModel<T>() {

    private val frame:      ModelPart = root.getChild("frame")
    private val frontWheel: ModelPart = root.getChild("front_wheel")
    private val rearWheel:  ModelPart = root.getChild("rear_wheel")
    private val handlebar:  ModelPart = root.getChild("handlebar")
    private val seat:       ModelPart = root.getChild("seat")

    companion object {
        fun getTexturedModelData(): TexturedModelData {
            val data = ModelData()
            val root = data.root

            // ── Frame ──────────────────────────────────────────────────────
            // Horizontal tube connecting both wheels; pivot at centre/axle height.
            root.addChild(
                "frame",
                ModelPartBuilder.create()
                    .uv(0, 0)
                    .cuboid(-1.5f, -2f, -8f, 3f, 3f, 16f),
                ModelTransform.NONE
            )

            // ── Front wheel ────────────────────────────────────────────────
            // Rotates around X-axis. Pivot is at the wheel axle.
            root.addChild(
                "front_wheel",
                ModelPartBuilder.create()
                    .uv(0, 19)
                    .cuboid(-1f, -5f, -5f, 2f, 10f, 10f),
                ModelTransform.pivot(0f, 0f, 8f)
            )

            // ── Rear wheel ─────────────────────────────────────────────────
            root.addChild(
                "rear_wheel",
                ModelPartBuilder.create()
                    .uv(24, 19)
                    .cuboid(-1f, -5f, -5f, 2f, 10f, 10f),
                ModelTransform.pivot(0f, 0f, -8f)
            )

            // ── Handlebar ─────────────────────────────────────────────────
            // T-bar above the front fork, offset upward (negative Y = up).
            root.addChild(
                "handlebar",
                ModelPartBuilder.create()
                    .uv(0, 39)
                    .cuboid(-6f, -2f, -1f, 12f, 2f, 2f),
                ModelTransform.pivot(0f, -8f, 7f)
            )

            // ── Seat ──────────────────────────────────────────────────────
            // Saddle, slightly behind centre and above axle height.
            root.addChild(
                "seat",
                ModelPartBuilder.create()
                    .uv(28, 39)
                    .cuboid(-2f, -2f, -2.5f, 4f, 2f, 5f),
                ModelTransform.pivot(0f, -7f, -1f)
            )

            return TexturedModelData.of(data, 64, 64)
        }
    }

    override fun setAngles(
        entity: T,
        limbAngle: Float,
        limbDistance: Float,
        animationProgress: Float,
        headYaw: Float,
        headPitch: Float
    ) {
        val rotation = entity.wheelRotation
        frontWheel.pitch = rotation
        rearWheel.pitch  = rotation

        // Handlebar and front wheel turn with steering
        val steerRad = entity.steerAngle * (PI.toFloat() / 180f)
        handlebar.yaw  = steerRad
        frontWheel.yaw = steerRad * 0.6f
    }

    override fun render(
        matrices: MatrixStack,
        vertices: VertexConsumer,
        light: Int,
        overlay: Int,
        color: Int
    ) {
        frame.render(matrices, vertices, light, overlay, color)
        frontWheel.render(matrices, vertices, light, overlay, color)
        rearWheel.render(matrices, vertices, light, overlay, color)
        handlebar.render(matrices, vertices, light, overlay, color)
        seat.render(matrices, vertices, light, overlay, color)
    }
}