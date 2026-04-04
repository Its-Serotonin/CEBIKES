package com.serotonin.cebikes.registry

import com.serotonin.cebikes.block.BikeRackBlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object CebikesBlockEntities {

    val BIKE_RACK: BlockEntityType<BikeRackBlockEntity> = Registry.register(
        Registries.BLOCK_ENTITY_TYPE,
        Identifier.of("cebikes", "bike_rack"),
        BlockEntityType.Builder.create(::BikeRackBlockEntity, CebikesBlocks.BIKE_RACK).build()
    )

    fun register() { /* triggers static initialisation */ }
}