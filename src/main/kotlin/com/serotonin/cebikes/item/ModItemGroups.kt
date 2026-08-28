package com.serotonin.cebikes.item

import com.serotonin.cebikes.registry.CebikesItems
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.text.Text
import net.minecraft.util.Identifier

object ModItemGroups {
    val CEBIKES_ITEM_GROUP: ItemGroup? = Registry.register(
        Registries.ITEM_GROUP,
        Identifier.of("cebikes", "cebikes_items"),
        FabricItemGroup.builder().icon { ItemStack(CebikesItems.MACH_BIKE) }
            .displayName(Text.translatable("itemgroup.cebikes.cebikes_items"))
            .entries { _: ItemGroup.DisplayContext?, entries: ItemGroup.Entries? ->
                entries?.add(CebikesItems.MACH_BIKE)
                entries?.add(CebikesItems.ACRO_BIKE)
                entries?.add(CebikesItems.BIKE_RACK)
                entries?.add(CebikesItems.MACH_CORE)
                entries?.add(CebikesItems.ACRO_CORE)
                entries?.add(CebikesItems.MACH_CORE_CENTER)
                entries?.add(CebikesItems.ACRO_CORE_CENTER)
                entries?.add(CebikesItems.CORE_TOP_CAP)
                entries?.add(CebikesItems.CORE_BOTTOM_CAP)
                entries?.add(CebikesItems.RUBBER_INGOT)
                entries?.add(CebikesItems.BIKE_FRAME)
                entries?.add(CebikesItems.BIKE_GEARS)
                entries?.add(CebikesItems.BIKE_HANDLES)
                entries?.add(CebikesItems.BIKE_TIRES)
                entries?.add(CebikesItems.MULTIDYE)
                entries?.add(CebikesItems.MULTIBRUSH)
            }.build()
    )

    fun registerCebikesItemGroups() {
        println("registering CebikesItemGroups")
    }
}