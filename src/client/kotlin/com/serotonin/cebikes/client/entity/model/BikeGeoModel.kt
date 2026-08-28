package com.serotonin.cebikes.client.entity.model

import com.serotonin.cebikes.entity.AbstractBikeEntity
import com.serotonin.cebikes.entity.AcroBikeEntity
import net.minecraft.util.Identifier
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.model.GeoModel
import kotlin.math.PI

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
        when (entity) {
            is AcroBikeEntity -> Identifier.of("cebikes", "animations/acrobike.animation.json")
            else              -> Identifier.of("cebikes", "animations/machbike.animation.json")
        }



    override fun setCustomAnimations(entity: T, instanceId: Long, animationState: AnimationState<T>?) {
        val model = getBakedModel(getModelResource(entity))

        // GUI customiser preview
        if (entity.isGuiRendering) {
            model.getBone("back_wheel").ifPresent   { it.setRotX(0f) }
            model.getBone("front_wheel").ifPresent  { it.setRotX(0f) }
            model.getBone("back_spokes").ifPresent  { it.setRotX(0f) }
            model.getBone("front_spokes").ifPresent { it.setRotX(0f) }
            model.getBone("left_pedal").ifPresent   { it.setRotX(0f) }
            model.getBone("right_pedal").ifPresent  { it.setRotX(PI.toFloat()) }
            model.getBone("handle").ifPresent       { it.setRotY(0f) }
            model.getBone("front_brakes").ifPresent { it.setRotY(0f) }
            model.getBone("machbike").ifPresent     { it.setRotZ(0f) }
            model.getBone("acrobike").ifPresent     { it.setRotZ(0f) }
            model.getBone("connectors").ifPresent   { it.setRotZ(0f) }
            return
        }

        val wheelRot      = entity.wheelRotation
        val steerAngle    = entity.dataTracker.get(AbstractBikeEntity.STEER_ANGLE)
        val targetSteerRad = steerAngle * (PI / 180.0).toFloat()
        entity.smoothedSteerRad += (targetSteerRad - entity.smoothedSteerRad) * 0.15f
        val driftProgress = entity.dataTracker.get(AbstractBikeEntity.DRIFT_PROGRESS)

        model.getBone("back_wheel").ifPresent  { it.setRotX(wheelRot) }
        model.getBone("front_wheel").ifPresent { it.setRotX(wheelRot) }

        model.getBone("left_pedal").ifPresent  { it.setRotX(wheelRot) }
        model.getBone("right_pedal").ifPresent { it.setRotX(wheelRot + PI.toFloat()) }

        model.getBone("handle").ifPresent { it.setRotY(-entity.smoothedSteerRad * 1.5f) }

        model.getBone("front_brakes").ifPresent { it.setRotY(-entity.smoothedSteerRad* 1.5f) }

        val steerSign   = if (steerAngle > 0f) 1f else if (steerAngle < 0f) -1f else 0f
        val targetRoll  = steerSign * (targetSteerRad * 0.05f + driftProgress * 0.21f)

        entity.smoothedRollRad += (targetRoll - entity.smoothedRollRad) * 0.12f

        model.getBone("machbike").ifPresent   { it.setRotZ(-entity.smoothedRollRad) }
        model.getBone("acrobike").ifPresent   { it.setRotZ(-entity.smoothedRollRad) }
        if (entity is AcroBikeEntity) {
            model.getBone("connectors").ifPresent { it.setRotZ(-entity.smoothedRollRad) }
        }
    }
}