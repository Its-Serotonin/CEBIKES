package com.serotonin.cebikes.recipe

import net.minecraft.recipe.SpecialRecipeSerializer
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object CebikesRecipes {
    val BIKE_DYE_SERIALIZER = Registry.register(
        Registries.RECIPE_SERIALIZER,
        Identifier.of("cebikes", "bike_dye"),
        SpecialRecipeSerializer(::BikeDyeRecipe)
    )

    val BIKE_UNDYE_SERIALIZER = Registry.register(
        Registries.RECIPE_SERIALIZER,
        Identifier.of("cebikes", "bike_undye"),
        SpecialRecipeSerializer(::BikeDyeRemovalRecipe)
    )


    fun register() {}
}