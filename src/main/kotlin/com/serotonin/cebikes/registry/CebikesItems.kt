package com.serotonin.cebikes.registry

import com.serotonin.cebikes.item.BikeItem
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroups
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object CebikesItems {

    val MACH_BIKE: BikeItem = Registry.register(
        Registries.ITEM,
        Identifier.of("cebikes", "mach_bike"),
        BikeItem(CebikesEntities.MACH_BIKE, Item.Settings().maxCount(1))
    )

    val ACRO_BIKE: BikeItem = Registry.register(
        Registries.ITEM,
        Identifier.of("cebikes", "acro_bike"),
        BikeItem(CebikesEntities.ACRO_BIKE, Item.Settings().maxCount(1))
    )


    val BIKE_RACK: BlockItem = Registry.register(
        Registries.ITEM,
        Identifier.of("cebikes", "bike_rack"),
        BlockItem(CebikesBlocks.BIKE_RACK, Item.Settings())
    )


    fun register() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register { content ->
            content.add(MACH_BIKE)
            content.add(ACRO_BIKE)
            content.add(BIKE_RACK)
        }
    }
}