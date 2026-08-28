package com.serotonin.cebikes.client.item.model

import net.minecraft.util.Identifier
import software.bernie.geckolib.model.GeoModel
import com.serotonin.cebikes.item.BikeItem
import software.bernie.geckolib.animation.AnimationState

class BikeItemGeoModel(
    private val modelId: Identifier,
    private val textureId: Identifier,
    private val animationId: Identifier
) : GeoModel<BikeItem>() {

    override fun getModelResource(animatable: BikeItem) = modelId
    override fun getTextureResource(animatable: BikeItem) = textureId
    override fun getAnimationResource(animatable: BikeItem) = animationId

    override fun setCustomAnimations(animatable: BikeItem, instanceId: Long, animationState: AnimationState<BikeItem>?) {
        getBone("back_wheel").ifPresent  { it.setRotX(0f) }
        getBone("front_wheel").ifPresent { it.setRotX(0f); it.setRotY(0f) }
        getBone("left_pedal").ifPresent  { it.setRotX(0f) }
        getBone("right_pedal").ifPresent { it.setRotX(0f) }
        getBone("handle").ifPresent      { it.setRotY(0f) }
        getBone("machbike").ifPresent    { it.setRotZ(0f) }
        getBone("acrobike").ifPresent    { it.setRotZ(0f) }
    }
}