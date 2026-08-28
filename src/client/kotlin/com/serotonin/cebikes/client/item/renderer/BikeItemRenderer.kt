package com.serotonin.cebikes.client.item.renderer

import com.serotonin.cebikes.client.item.model.BikeItemGeoModel
import com.serotonin.cebikes.item.BikeItem
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.NbtComponent
import net.minecraft.util.Identifier
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.renderer.GeoItemRenderer


class BikeItemRenderer(
    modelId: Identifier,
    textureId: Identifier,
    animationId: Identifier
) : GeoItemRenderer<BikeItem>(
    BikeItemGeoModel(modelId, textureId, animationId)
) {
    override fun renderRecursively(
        poseStack: MatrixStack,
        animatable: BikeItem,
        bone: GeoBone,
        renderType: RenderLayer,
        bufferSource: VertexConsumerProvider,
        buffer: VertexConsumer,
        isReRender: Boolean,
        partialTick: Float,
        packedLight: Int,
        packedOverlay: Int,
        color: Int
    ) {
        val nbt = currentItemStack.getOrDefault(
            DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt()

        val boneColor = when {
            nbt.contains("BoneColors") -> {
                val boneColors = nbt.getCompound("BoneColors")
                if (boneColors.contains(bone.name)) {
                    (0xFF shl 24) or boneColors.getInt(bone.name)
                } else 0xFFFFFFFF.toInt()
            }
            (bone.name == "frame" || bone.name == "headlight_mount") && nbt.contains("BikeColor") -> {
                (0xFF shl 24) or nbt.getInt("BikeColor")
            }
            bone.name == "frame" || bone.name == "headlight_mount" -> {
                (0xFF shl 24) or animatable.defaultColor
            }
            else -> 0xFFFFFFFF.toInt()
        }

        super.renderRecursively(
            poseStack, animatable, bone, renderType,
            bufferSource, buffer, isReRender,
            partialTick, packedLight, packedOverlay,
            boneColor
        )
    }
}