package com.serotonin.cebikes.client.entity.renderer

import com.serotonin.cebikes.entity.AbstractBikeEntity
import com.serotonin.cebikes.entity.AcroBikeEntity
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.renderer.GeoRenderer
import software.bernie.geckolib.renderer.layer.GeoRenderLayer

class HeadlightGeoLayer<T : AbstractBikeEntity>(
    renderer: GeoRenderer<T>
) : GeoRenderLayer<T>(renderer) {

    override fun render(
        poseStack: MatrixStack,
        animatable: T,
        bakedModel: BakedGeoModel,
        renderType: RenderLayer?,
        bufferSource: VertexConsumerProvider,
        buffer: net.minecraft.client.render.VertexConsumer?,
        partialTick: Float,
        packedLight: Int,
        packedOverlay: Int
    ) {
        if (!animatable.dataTracker.get(AbstractBikeEntity.HEADLIGHT_ON)) return

        val emissiveTexture = when (animatable) {
            is AcroBikeEntity -> Identifier.of("cebikes", "textures/entity/acrobike_emissive.png")
            else              -> Identifier.of("cebikes", "textures/entity/machbike_emissive.png")
        }

        val emissiveRenderType = RenderLayer.getEyes(emissiveTexture)
        val emissiveBuffer = bufferSource.getBuffer(emissiveRenderType)

        val boneColors = animatable.dataTracker.get(AbstractBikeEntity.BONE_COLORS)
        val headlightArgb = if (boneColors.contains("headlight_light_color")) {
            (0xFF shl 24) or boneColors.getInt("headlight_light_color")
        } else {
            -1
        }

        getRenderer().reRender(
            bakedModel, poseStack, bufferSource, animatable,
            emissiveRenderType, emissiveBuffer,
            partialTick,
            LightmapTextureManager.MAX_LIGHT_COORDINATE,
            packedOverlay,
            headlightArgb
        )
    }
}