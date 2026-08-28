package com.serotonin.cebikes.recipe

import com.serotonin.cebikes.item.BikeItem
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.NbtComponent
import net.minecraft.item.ItemStack
import net.minecraft.item.ShearsItem
import net.minecraft.recipe.SpecialCraftingRecipe
import net.minecraft.recipe.book.CraftingRecipeCategory
import net.minecraft.recipe.input.CraftingRecipeInput
import net.minecraft.registry.RegistryWrapper
import net.minecraft.util.collection.DefaultedList
import net.minecraft.world.World

class BikeDyeRemovalRecipe(category: CraftingRecipeCategory) : SpecialCraftingRecipe(category) {

    override fun matches(input: CraftingRecipeInput, world: World): Boolean {
        var hasBike = false
        var hasShears = false
        for (i in 0 until input.size) {
            val stack = input.getStackInSlot(i)
            if (stack.isEmpty) continue
            when {
                stack.item is BikeItem -> hasBike = true
                stack.item is ShearsItem -> hasShears = true
                else -> return false
            }
        }
        return hasBike && hasShears
    }

    override fun craft(input: CraftingRecipeInput, lookup: RegistryWrapper.WrapperLookup): ItemStack {
        var bikeStack = ItemStack.EMPTY
        var shearsStack = ItemStack.EMPTY
        for (i in 0 until input.size) {
            val stack = input.getStackInSlot(i)
            if (stack.isEmpty) continue
            when (stack.item) {
                is BikeItem -> bikeStack = stack.copy()
                is ShearsItem -> shearsStack = stack.copy()
            }
        }
        if (bikeStack.isEmpty || shearsStack.isEmpty) return ItemStack.EMPTY

        val nbt = bikeStack.getOrDefault(DataComponentTypes.CUSTOM_DATA,
            NbtComponent.DEFAULT).copyNbt()
        nbt.remove("BoneColors")
        nbt.remove("BikeColor")  // legacy key
        bikeStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt))

        return bikeStack
    }

    override fun getRemainder(input: CraftingRecipeInput): DefaultedList<ItemStack> {
        val remaining = DefaultedList.ofSize(input.size, ItemStack.EMPTY)
        for (i in 0 until input.size) {
            val stack = input.getStackInSlot(i)
            if (stack.item is ShearsItem) {
                val damaged = stack.copy()
                damaged.damage++
                remaining[i] = if (damaged.damage >= damaged.maxDamage) ItemStack.EMPTY else damaged
            }
        }
        return remaining
    }

    override fun fits(width: Int, height: Int) = width * height >= 2
    override fun getSerializer() = CebikesRecipes.BIKE_UNDYE_SERIALIZER
}