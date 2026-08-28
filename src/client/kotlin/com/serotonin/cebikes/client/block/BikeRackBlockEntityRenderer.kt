package com.serotonin.cebikes.client.block


import com.serotonin.cebikes.block.BikeRackBlock
import com.serotonin.cebikes.block.BikeRackBlockEntity
import com.serotonin.cebikes.block.BikeRackPart
import com.serotonin.cebikes.config.CebikesConfig
import com.serotonin.cebikes.item.BikeItem
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.model.json.ModelTransformationMode
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Direction
import net.minecraft.util.math.RotationAxis
import software.bernie.geckolib.animatable.client.GeoRenderProvider
import software.bernie.geckolib.renderer.GeoBlockRenderer
import software.bernie.geckolib.renderer.GeoItemRenderer

class BikeRackBlockEntityRenderer(
) : GeoBlockRenderer<BikeRackBlockEntity>(BikeRackGeoModel()) {

    override fun render(
        entity: BikeRackBlockEntity,
        partialTick: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        packedLight: Int,
        packedOverlay: Int
    ) {
        // Only render from the main block, not the extension
        if (entity.cachedState.get(BikeRackBlock.PART) != BikeRackPart.MAIN) return

        val onWall = entity.cachedState.get(BikeRackBlock.WALL_MOUNTED)
        val state = entity.cachedState
        val facing = state.get(BikeRackBlock.FACING)



        if (onWall) {
            if (CebikesConfig.visibleBikeRack) {
                super.render(entity, partialTick, matrices, vertexConsumers, packedLight, packedOverlay)
            }
        } else {
            super.render(entity, partialTick, matrices, vertexConsumers, packedLight, packedOverlay)
        }


        val bikeStack = entity.getStoredBike()
        if (bikeStack.isEmpty) return
        val item = bikeStack.item as? BikeItem ?: return
        val renderer = GeoRenderProvider.of(item).geoItemRenderer ?: return

        matrices.push()
        matrices.translate(0.5, 0.0, 0.5)


        //bikes rotation on the ground
        val yawDeg = when (facing) {
            Direction.NORTH -> 270f
            Direction.SOUTH -> 90f
            Direction.WEST  -> 0f
            Direction.EAST  -> 180f
            else -> 0f
        }
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawDeg))
        if (onWall) {
            //bikes rendering on the wall
            matrices.translate(-0.40, -0.5, -0.5)
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(0f))
        } else {
            //bikes pos on ground after rotation
            matrices.translate(-1.05, -0.5, 0.20)
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-22.5f))
        }

        val scale = 1.0f
        matrices.scale(scale, scale, scale)

        val geoModel = (renderer as? GeoItemRenderer<*>)?.geoModel
        geoModel?.getBone("back_wheel")?.ifPresent  { it.setRotX(0f) }
        geoModel?.getBone("front_wheel")?.ifPresent { it.setRotX(0f); it.setRotY(0f) }
        geoModel?.getBone("left_pedal")?.ifPresent  { it.setRotX(0f) }
        geoModel?.getBone("right_pedal")?.ifPresent { it.setRotX(0f) }
        geoModel?.getBone("handle")?.ifPresent      { it.setRotY(0f) }
        geoModel?.getBone("machbike")?.ifPresent    { it.setRotZ(0f) }
        geoModel?.getBone("acrobike")?.ifPresent    { it.setRotZ(0f) }

        renderer.render(
            bikeStack,
            ModelTransformationMode.FIXED,
            matrices,
            vertexConsumers,
            packedLight,
            packedOverlay
        )

        matrices.pop()
    }
}