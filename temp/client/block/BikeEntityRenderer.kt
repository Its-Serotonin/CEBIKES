import com.serotonin.cebikes.client.entity.model.BikeGeoModel
import com.serotonin.cebikes.client.entity.renderer.HeadlightGeoLayer
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

    override fun getGeoModel(): BikeGeoModel<T> {
        val entity = this.animatable ?: return super.getGeoModel() as BikeGeoModel<T>
        return modelCache.getOrPut(entity.id.toLong()) { BikeGeoModel() }
    }

    override fun render(
        entity: T,
        entityYaw: Float,
        partialTick: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int
    ) {
        if (entity.isRemoved) modelCache.remove(entity.id.toLong())
        super.render(entity, entityYaw, partialTick, matrices, vertexConsumers, light)
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
        val frameBones = setOf("frame", "headlight_mount")

        val boneColor = if (bone.name in frameBones) {
            (0xFF shl 24) or animatable.dataTracker.get(AbstractBikeEntity.COLOR_KEY)
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
