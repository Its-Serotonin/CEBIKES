package com.serotonin.cebikes.item

import com.serotonin.cebikes.entity.AbstractBikeEntity
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.NbtComponent
import net.minecraft.entity.EntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.world.RaycastContext
import net.minecraft.world.World

class BikeItem(
    private val entityType: EntityType<out AbstractBikeEntity>,
    settings: Settings
) : Item(settings) {

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

            // Restore colour stored in the item via data components
            val nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt()
            if (nbt.contains("BikeColor")) {
                entity.dataTracker.set(AbstractBikeEntity.COLOR_KEY, nbt.getInt("BikeColor"))
            }

            world.spawnEntity(entity)
        }

        if (!user.abilities.creativeMode) stack.decrement(1)

        return TypedActionResult.success(stack, world.isClient)
    }
}