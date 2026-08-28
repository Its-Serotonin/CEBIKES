package com.serotonin.cebikes.item

import com.serotonin.cebikes.entity.AbstractBikeEntity
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.NbtComponent
import net.minecraft.entity.EntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.TypedActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.world.RaycastContext
import net.minecraft.world.World
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.client.GeoRenderProvider
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.renderer.GeoItemRenderer
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.function.Consumer

class BikeItem(
    private val entityType: EntityType<out AbstractBikeEntity>,
    private val modelId: Identifier,
    private val textureId: Identifier,
    private val animationId: Identifier,
    val defaultColor: Int,
    settings: Settings
) : Item(settings), GeoItem {

    override fun getName(stack: ItemStack): Text {
        return Text.translatable(this.translationKey).formatted(Formatting.BLUE)
    }

    private val animatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache? = animatableInstanceCache

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar?) {

    }

    companion object {
        var rendererProvider: BikeRendererProvider? = null
    }

    override fun createGeoRenderer(consumer: Consumer<GeoRenderProvider>) {
        consumer.accept(object : GeoRenderProvider {
            private var renderer: GeoItemRenderer<*>? = null

            override fun getGeoItemRenderer(): GeoItemRenderer<*> {
                if (renderer == null)
                    renderer = rendererProvider!!.createRenderer(modelId, textureId, animationId)
                            as GeoItemRenderer<*>
                return renderer!!
            }
        })
    }

    override fun appendTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltip: MutableList<Text>,
        type: TooltipType
    ) {
        tooltip.add(Text.translatable("tooltip.cebikes.cebike").formatted(Formatting.WHITE))
    }


    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = user.getStackInHand(hand)

        val hit = raycast(world, user, RaycastContext.FluidHandling.NONE)
        if (hit.type != HitResult.Type.BLOCK) return TypedActionResult.pass(stack)

        val blockHit = hit as BlockHitResult
        val pos = blockHit.blockPos.offset(blockHit.side)

        if (!world.isClient) {
            val entity = entityType.create(world) ?: return TypedActionResult.fail(stack)
            entity.refreshPositionAndAngles(
                pos.x + 0.5,
                pos.y.toDouble(),
                pos.z + 0.5,
                user.yaw,
                0f
            )

            val nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt()
            val boneColors = when {
                nbt.contains("BoneColors") -> {
                    val saved = nbt.getCompound("BoneColors").copy()
                    if (!saved.contains("frame")) saved.putInt("frame", entity.defaultColor)
                    if (!saved.contains("headlight_mount")) saved.putInt("headlight_mount", entity.defaultColor)
                    saved
                }
                nbt.contains("BikeColor") -> {
                    val c = nbt.getInt("BikeColor")
                    NbtCompound().also { it.putInt("frame", c); it.putInt("headlight_mount", c) }
                }
                else -> {
                    NbtCompound().also {
                        it.putInt("frame", entity.defaultColor)
                        it.putInt("headlight_mount", entity.defaultColor)
                    }
                }
            }
            entity.dataTracker.set(AbstractBikeEntity.BONE_COLORS, boneColors)
            world.spawnEntity(entity)
        }
            stack.decrement(1)

        return TypedActionResult.success(stack, world.isClient)
    }
}