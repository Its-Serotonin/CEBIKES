package com.serotonin.cebikes.registry

import com.serotonin.cebikes.item.BikeItem
import com.serotonin.cebikes.item.BikeRackItem
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.item.Item
import net.minecraft.item.ItemGroups
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

object CebikesItems {

    val MACH_BIKE: BikeItem = Registry.register(
        Registries.ITEM,
        Identifier.of("cebikes", "mach_bike"),
        BikeItem(
            CebikesEntities.MACH_BIKE,
            Identifier.of("cebikes", "geo/machbike.geo.json"),
            Identifier.of("cebikes", "textures/entity/machbike.png"),
            Identifier.of("cebikes", "animations/machbike.animation.json"),
            0x1449a0,
            Item.Settings().maxCount(1)
        )
    )

    val ACRO_BIKE: BikeItem = Registry.register(
        Registries.ITEM,
        Identifier.of("cebikes", "acro_bike"),
        BikeItem(
            CebikesEntities.ACRO_BIKE,
            Identifier.of("cebikes", "geo/acrobike.geo.json"),
            Identifier.of("cebikes", "textures/entity/acrobike.png"),
            Identifier.of("cebikes", "animations/acrobike.animation.json"),
            0xe4473a,
            Item.Settings().maxCount(1)
        )
    )

    val BIKE_RACK: BikeRackItem = Registry.register(
        Registries.ITEM,
        Identifier.of("cebikes", "bike_rack"),
        BikeRackItem(CebikesBlocks.BIKE_RACK, Item.Settings().maxCount(64))
    )

    val MACH_CORE: Item? = Registry.register(
        Registries.ITEM,
        Identifier.of("cebikes", "mach_core"),
        BikeCoreItem(Item.Settings().maxCount(16))
    )

    val ACRO_CORE: Item? = Registry.register(
        Registries.ITEM,
        Identifier.of("cebikes", "acro_core"),
        BikeCoreItem(Item.Settings().maxCount(16))
    )

    val MACH_CORE_CENTER: Item? = Registry.register(
        Registries.ITEM,
        Identifier.of("cebikes", "mach_core_center"),
        BikeBuildingItem(Item.Settings().maxCount(64))
    )

    val ACRO_CORE_CENTER: Item? = Registry.register(
        Registries.ITEM,
        Identifier.of("cebikes", "acro_core_center"),
        BikeBuildingItem(Item.Settings().maxCount(64))
    )

    val CORE_TOP_CAP: Item? = Registry.register(
        Registries.ITEM,
        Identifier.of("cebikes", "core_top_cap"),
        BikeBuildingItem(Item.Settings().maxCount(64))
    )

    val CORE_BOTTOM_CAP: Item? = Registry.register(
        Registries.ITEM,
        Identifier.of("cebikes", "core_bottom_cap"),
        BikeBuildingItem(Item.Settings().maxCount(64))
    )

    val RUBBER_INGOT: Item? = Registry.register(
        Registries.ITEM,
        Identifier.of("cebikes", "rubber_ingot"),
        BikeBuildingItem(Item.Settings().maxCount(64))
    )

    val BIKE_FRAME: Item? = Registry.register(
        Registries.ITEM,
        Identifier.of("cebikes", "bike_frame"),
        BikeBuildingItem(Item.Settings().maxCount(64))
    )

    val BIKE_GEARS: Item? = Registry.register(
        Registries.ITEM,
        Identifier.of("cebikes", "bike_gears"),
        BikeBuildingItem(Item.Settings().maxCount(64))
    )

    val BIKE_HANDLES: Item? = Registry.register(
        Registries.ITEM,
        Identifier.of("cebikes", "bike_handles"),
        BikeBuildingItem(Item.Settings().maxCount(64))
    )

    val BIKE_TIRES: Item? = Registry.register(
        Registries.ITEM,
        Identifier.of("cebikes", "bike_tires"),
        BikeBuildingItem(Item.Settings().maxCount(64))
    )

    val MULTIDYE: Item? = Registry.register(
        Registries.ITEM,
        Identifier.of("cebikes", "multidye"),
        MultidyeItem(Item.Settings().maxCount(64))
    )

    val MULTIBRUSH: Item? = Registry.register(
        Registries.ITEM,
        Identifier.of("cebikes", "multibrush"),
        MultibrushItem(Item.Settings().maxCount(1))
    )


    fun register() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register { content ->
            content.add(MACH_BIKE)
            content.add(ACRO_BIKE)
            content.add(BIKE_RACK)
            content.add(MACH_CORE)
            content.add(ACRO_CORE)
            content.add(MACH_CORE_CENTER)
            content.add(ACRO_CORE_CENTER)
            content.add(CORE_TOP_CAP)
            content.add(CORE_BOTTOM_CAP)
            content.add(RUBBER_INGOT)
            content.add(BIKE_FRAME)
            content.add(BIKE_GEARS)
            content.add(BIKE_HANDLES)
            content.add(BIKE_TIRES)
            content.add(MULTIDYE)
            content.add(MULTIBRUSH)
        }
    }

    class BikeCoreItem(settings: Settings) : Item(settings) {

        override fun getName(stack: ItemStack): Text {
            return Text.translatable(this.translationKey).formatted(Formatting.BLUE)
        }
    }

    class BikeBuildingItem(settings: Settings) : Item(settings) {

        override fun getName(stack: ItemStack): Text {
            return Text.translatable(this.translationKey).formatted(Formatting.WHITE)
        }
    }

    class MultidyeItem(settings: Settings) : Item(settings) {

        override fun getName(stack: ItemStack): Text {
            return Text.translatable(this.translationKey).formatted(Formatting.LIGHT_PURPLE)
        }
        override fun appendTooltip(
            stack: ItemStack,
            context: TooltipContext,
            tooltip: MutableList<Text>,
            type: TooltipType
        ) {
            tooltip.add(Text.translatable("tooltip.cebikes.multidye").formatted(Formatting.WHITE))
        }
    }
    class MultibrushItem(settings: Settings) : Item(settings) {

        override fun getName(stack: ItemStack): Text {
            return Text.translatable(this.translationKey).formatted(Formatting.LIGHT_PURPLE)
        }
        override fun appendTooltip(
            stack: ItemStack,
            context: TooltipContext,
            tooltip: MutableList<Text>,
            type: TooltipType
        ) {
            tooltip.add(Text.translatable("tooltip.cebikes.multibrush").formatted(Formatting.WHITE))
        }
    }
}