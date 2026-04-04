package com.serotonin.cebikes.entity

import com.serotonin.cebikes.registry.CebikesItems
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.NbtComponent
import net.minecraft.entity.EntityType
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.world.World

class MachBikeEntity(type: EntityType<*>, world: World) : AbstractBikeEntity(type, world) {

    override val maxForwardSpeed     = 0.90   // ~1.5× faster than the Acro bike
    override val acceleration        = 0.035
    override val naturalDeceleration = 0.94
    override val baseTurnRate        = 1.6f
    override val maxDriftTurnRate    = 5.6f

    /** Normal step height — cannot climb 1-block-tall walls. */
    override fun getStepHeight(): Float = 0.6f

    override fun createBikeItemStack(): ItemStack {
        val stack = ItemStack(CebikesItems.MACH_BIKE)
        val color = dataTracker.get(COLOR_KEY)
        if (color != DEFAULT_COLOR) {
            val nbt = NbtCompound()
            nbt.putInt("BikeColor", color)
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt))
        }
        return stack
    }
}