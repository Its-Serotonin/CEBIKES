package com.serotonin.cebikes.client.entity.model

import com.serotonin.cebikes.entity.AbstractBikeEntity
import com.serotonin.cebikes.entity.AcroBikeEntity
import net.minecraft.util.Identifier
import software.bernie.geckolib.model.GeoModel

class BikeGeoModel<T : AbstractBikeEntity> : GeoModel<T>() {

    override fun getModelResource(entity: T): Identifier =
        when (entity) {
            is AcroBikeEntity -> Identifier.of("cebikes", "geo/acrobike.geo.json")
            else              -> Identifier.of("cebikes", "geo/machbike.geo.json")
        }

    override fun getTextureResource(entity: T): Identifier =
        when (entity) {
            is AcroBikeEntity -> Identifier.of("cebikes", "textures/entity/acrobike.png")
            else              -> Identifier.of("cebikes", "textures/entity/machbike.png")
        }

    override fun getAnimationResource(entity: T): Identifier =
        Identifier.of("cebikes", "animations/bike.animation.json")
}