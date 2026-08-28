package com.serotonin.cebikes.client.block

import com.serotonin.cebikes.block.BikeRackBlock
import com.serotonin.cebikes.block.BikeRackBlockEntity
import net.minecraft.util.Identifier
import software.bernie.geckolib.model.GeoModel

class BikeRackGeoModel : GeoModel<BikeRackBlockEntity>() {
    override fun getModelResource(animatable: BikeRackBlockEntity) =
        if (animatable.cachedState.get(BikeRackBlock.WALL_MOUNTED))
            Identifier.of("cebikes", "geo/bike_rack_wall.geo.json")
        else
            Identifier.of("cebikes", "geo/bike_rack.geo.json")

    override fun getTextureResource(animatable: BikeRackBlockEntity) =
        if (animatable.cachedState.get(BikeRackBlock.WALL_MOUNTED))
            Identifier.of("cebikes", "textures/block/bike_rack_wall.png")
        else
            Identifier.of("cebikes", "textures/block/bike_rack.png")

    override fun getAnimationResource(animatable: BikeRackBlockEntity) =
        Identifier.of("cebikes", "animations/bike_rack.animation.json")
}