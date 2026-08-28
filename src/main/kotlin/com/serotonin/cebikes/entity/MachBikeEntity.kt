package com.serotonin.cebikes.entity

import com.serotonin.cebikes.registry.CebikesItems
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.NbtComponent
import net.minecraft.entity.EntityType
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.world.World

class MachBikeEntity(type: EntityType<*>, world: World) : AbstractBikeEntity(type, world) {

    override val maxForwardSpeed     = 0.90
    override val acceleration        = 0.035
    override val naturalDeceleration = 0.95
    override val baseTurnRate        = 2.0f
    override val maxDriftTurnRate    = 6.4f
    override val defaultColor = 0x1449a0

    override fun getStepHeight(): Float = 0.6f

    override fun createBikeItemStack(): ItemStack {
        val stack = ItemStack(CebikesItems.MACH_BIKE)
        val boneColors = dataTracker.get(BONE_COLORS)
        val nbt = NbtCompound()
        nbt.put("BoneColors", boneColors.copy())
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt))
        return stack
    }
}