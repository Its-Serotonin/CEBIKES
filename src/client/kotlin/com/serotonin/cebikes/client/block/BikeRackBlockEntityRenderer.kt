package com.serotonin.cebikes.client.block

import com.serotonin.cebikes.block.BikeRackBlock
import com.serotonin.cebikes.block.BikeRackBlockEntity
import com.serotonin.cebikes.client.entity.model.BikeEntityModel
import com.serotonin.cebikes.entity.AbstractBikeEntity
import com.serotonin.cebikes.registry.CebikesItems
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.NbtComponent
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction
import net.minecraft.util.math.RotationAxis

class BikeRackBlockEntityRenderer(
    ctx: BlockEntityRendererFactory.Context
) : BlockEntityRenderer<BikeRackBlockEntity> {

    private val bikeModel = BikeEntityModel<AbstractBikeEntity>(
        BikeEntityModel.getTexturedModelData().createModel()
    )

    // Use the mach bike texture as a fallback; stored item determines which
    private val machTexture = Identifier.of("cebikes", "textures/entity/machbike.png")
    private val acroTexture = Identifier.of("cebikes", "textures/entity/acrobike.png")

    override fun render(
        entity: BikeRackBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        val bikeStack = entity.getStoredBike()
        if (bikeStack.isEmpty) return

        val state = entity.cachedState
        val facing = state.get(BikeRackBlock.FACING)
        val wallMounted = state.get(BikeRackBlock.WALL_MOUNTED)

        // Pick texture based on which bike item is stored
        val texture = if (bikeStack.isOf(CebikesItems.ACRO_BIKE)) acroTexture else machTexture

        // Read stored colour from the item
        val nbt = bikeStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt()
        val color = if (nbt.contains("BikeColor")) nbt.getInt("BikeColor") else 0xFFFFFF
        val argb = (0xFF shl 24) or color

        matrices.push()

        // Position the bike on the rack
        matrices.translate(0.5, 0.0, 0.5)

        if (wallMounted) {
            // Bike is mounted on wall — raised and flush against the wall
            matrices.translate(0.0, 0.6, 0.0)
            val wallOffset = 0.35
            when (facing) {
                Direction.NORTH -> matrices.translate(0.0, 0.0, wallOffset)
                Direction.SOUTH -> matrices.translate(0.0, 0.0, -wallOffset)
                Direction.WEST  -> matrices.translate(wallOffset, 0.0, 0.0)
                Direction.EAST  -> matrices.translate(-wallOffset, 0.0, 0.0)
                else -> {}
            }
        }

        // Rotate bike to face the rack direction
        val yawDeg = when (facing) {
            Direction.SOUTH -> 0f
            Direction.WEST  -> 90f
            Direction.NORTH -> 180f
            Direction.EAST  -> 270f
            else -> 0f
        }
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawDeg))

        // Lift model so wheels sit on the surface
        matrices.translate(0.0, 5.0 / 16.0, 0.0)

        // Scale: 1 model unit → 1/16 block; flip Y
        val s = 1f / 16f
        matrices.scale(s, -s, s)

        val vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture))
        bikeModel.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, argb)

        matrices.pop()
    }
}