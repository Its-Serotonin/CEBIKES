package com.serotonin.cebikes.registry

import com.serotonin.cebikes.block.BikeRackBlock
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Blocks
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object CebikesBlocks {

    val BIKE_RACK: BikeRackBlock = Registry.register(
        Registries.BLOCK,
        Identifier.of("cebikes", "bike_rack"),
        BikeRackBlock(
            AbstractBlock.Settings.copy(Blocks.STONE)
                .strength(3.0f, 6.0f)
                .requiresTool()
                .nonOpaque()
        )
    )

    fun register() { /* triggers static initialisation */ }
}