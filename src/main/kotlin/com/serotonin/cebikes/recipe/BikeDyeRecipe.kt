package com.serotonin.cebikes.recipe

import com.serotonin.cebikes.entity.AbstractBikeEntity
import com.serotonin.cebikes.item.BikeItem
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.NbtComponent
import net.minecraft.item.DyeItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.recipe.SpecialCraftingRecipe
import net.minecraft.recipe.book.CraftingRecipeCategory
import net.minecraft.registry.RegistryWrapper
import net.minecraft.world.World
import net.minecraft.recipe.input.CraftingRecipeInput


class BikeDyeRecipe(category: CraftingRecipeCategory) : SpecialCraftingRecipe(category) {

    override fun matches(input: CraftingRecipeInput, world: World): Boolean {
        var hasBike = false
        var hasDye = false
        for (i in 0 until input.size) {
            val stack = input.getStackInSlot(i)
            if (stack.isEmpty) continue
            when {
                stack.item is BikeItem -> hasBike = true
                stack.item is DyeItem -> hasDye = true
                else -> return false
            }
        }
        return hasBike && hasDye
    }

    override fun craft(input: CraftingRecipeInput, lookup: RegistryWrapper.WrapperLookup): ItemStack {
        var bikeStack = ItemStack.EMPTY
        var dyeItem: DyeItem? = null

        for (i in 0 until input.size) {
            val stack = input.getStackInSlot(i)
            if (stack.isEmpty) continue
            when (val item = stack.item) {
                is BikeItem -> bikeStack = stack.copy()
                is DyeItem -> dyeItem = item
            }
        }

        if (bikeStack.isEmpty || dyeItem == null) return ItemStack.EMPTY

        val color = AbstractBikeEntity.DYE_TO_RGB[dyeItem.color] ?: return bikeStack

        val outerNbt = bikeStack.getOrDefault(DataComponentTypes.CUSTOM_DATA,
            NbtComponent.DEFAULT).copyNbt()
        val boneColors: NbtCompound = if (outerNbt.contains("BoneColors"))
            outerNbt.getCompound("BoneColors").copy()
        else NbtCompound()
        boneColors.putInt("frame", color)
        boneColors.putInt("headlight_mount", color)
        outerNbt.put("BoneColors", boneColors)
        bikeStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(outerNbt))

        return bikeStack
    }

    override fun fits(width: Int, height: Int) = width * height >= 2
    override fun getSerializer() = CebikesRecipes.BIKE_DYE_SERIALIZER
}