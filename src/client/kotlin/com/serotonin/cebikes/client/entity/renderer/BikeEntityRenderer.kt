package com.serotonin.cebikes.client.entity.renderer

import com.serotonin.cebikes.client.entity.model.BikeGeoModel
import com.serotonin.cebikes.entity.AbstractBikeEntity
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.util.Identifier
import software.bernie.geckolib.renderer.GeoEntityRenderer

class BikeEntityRenderer<T : AbstractBikeEntity>(
    ctx: EntityRendererFactory.Context
) : GeoEntityRenderer<T>(ctx, BikeGeoModel()) {

    override fun getTexture(entity: T): Identifier =
        (geoModel as BikeGeoModel<T>).getTextureResource(entity)
}