package com.serotonin.cebikes.client.entity.renderer

import com.serotonin.cebikes.client.entity.model.BikeGeoModel
import com.serotonin.cebikes.entity.AbstractBikeEntity
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.renderer.GeoEntityRenderer

class BikeEntityRenderer<T : AbstractBikeEntity>(
    ctx: EntityRendererFactory.Context
) : GeoEntityRenderer<T>(ctx, BikeGeoModel()) {

    override fun getTexture(entity: T): Identifier =
        (geoModel as BikeGeoModel<T>).getTextureResource(entity)

    init {
        addRenderLayer(HeadlightGeoLayer(this))
    }

    override fun getInstanceId(animatable: T): Long {
        return animatable.id.toLong()
    }

    private val modelCache = HashMap<Long, BikeGeoModel<T>>()

    private var currentEntity: T? = null

    override fun getGeoModel(): BikeGeoModel<T> {
        val entity = currentEntity ?: return super.getGeoModel() as BikeGeoModel<T>
        val model = modelCache.getOrPut(entity.id.toLong()) { BikeGeoModel() }
        return model
    }

    @Suppress("UnstableApiUsage")
    override fun render(
        entity: T,
        entityYaw: Float,
        partialTick: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int
    ) {
        if (entity.isRemoved) modelCache.remove(entity.id.toLong())
        currentEntity = entity
        super.render(entity, entityYaw, partialTick, matrices, vertexConsumers, light)
        currentEntity = null
    }


    override fun renderRecursively(
        poseStack: MatrixStack,
        animatable: T,
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
        val boneColors = animatable.dataTracker.get(AbstractBikeEntity.BONE_COLORS)
        val boneColor = if (boneColors.contains(bone.name)) {
            (0xFF shl 24) or boneColors.getInt(bone.name)
        } else {
            0xFFFFFFFF.toInt()
        }

        super.renderRecursively(
            poseStack, animatable, bone, renderType,
            bufferSource, buffer, isReRender,
            partialTick, packedLight, packedOverlay,
            boneColor
        )
    }

    override fun applyRotations(
        entity: T,
        matrices: MatrixStack,
        ageInTicks: Float,
        rotationYaw: Float,
        partialTick: Float,
        nativeScale: Float
    ) {
        val lerpedYaw = MathHelper.lerpAngleDegrees(partialTick, entity.prevYaw, entity.yaw)
        super.applyRotations(entity, matrices, ageInTicks, lerpedYaw, partialTick, nativeScale)
    }
}
