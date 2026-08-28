package com.serotonin.cebikes.client.block

import com.serotonin.cebikes.item.BikeRackItem
import net.minecraft.util.Identifier
import software.bernie.geckolib.model.GeoModel
import software.bernie.geckolib.renderer.GeoItemRenderer

class BikeRackItemGeoModel : GeoModel<BikeRackItem>() {
    @Deprecated("Deprecated in Java")
    override fun getModelResource(animatable: BikeRackItem) =
        Identifier.of("cebikes", "geo/bike_rack.geo.json")
    @Deprecated("Deprecated in Java")
    override fun getTextureResource(animatable: BikeRackItem) =
        Identifier.of("cebikes", "textures/block/bike_rack.png")
    override fun getAnimationResource(animatable: BikeRackItem): Identifier? =
        Identifier.of("cebikes", "animations/bike_rack.animation.json")
}

class BikeRackItemRenderer : GeoItemRenderer<BikeRackItem>(BikeRackItemGeoModel())