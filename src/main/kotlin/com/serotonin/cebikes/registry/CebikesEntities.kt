package com.serotonin.cebikes.registry

import com.serotonin.cebikes.entity.AcroBikeEntity
import com.serotonin.cebikes.entity.MachBikeEntity
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object CebikesEntities {

    val MACH_BIKE: EntityType<MachBikeEntity> = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of("cebikes", "mach_bike"),
        EntityType.Builder.create(::MachBikeEntity, SpawnGroup.MISC)
            .dimensions(1.4f, 0.9f)
            .build()
    )

    val ACRO_BIKE: EntityType<AcroBikeEntity> = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of("cebikes", "acro_bike"),
        EntityType.Builder.create(::AcroBikeEntity, SpawnGroup.MISC)
            .dimensions(1.4f, 0.9f)
            .build()
    )

    fun register() { /* triggers static initialisation */ }
}