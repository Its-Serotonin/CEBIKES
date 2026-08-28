package com.serotonin.cebikes

import com.serotonin.cebikes.config.CebikesConfig
import com.serotonin.cebikes.entity.AbstractBikeEntity
import com.serotonin.cebikes.item.ModItemGroups
import com.serotonin.cebikes.network.BikeCustomizePayload
import com.serotonin.cebikes.network.BikeInputPayload
import com.serotonin.cebikes.network.HeadlightTogglePayload
import com.serotonin.cebikes.network.OpenBikeCustomizerPayload
import com.serotonin.cebikes.recipe.CebikesRecipes
import com.serotonin.cebikes.registry.CebikesBlockEntities
import com.serotonin.cebikes.registry.CebikesBlocks
import com.serotonin.cebikes.registry.CebikesEntities
import com.serotonin.cebikes.registry.CebikesItems
import com.serotonin.cebikes.registry.ParticleRegistry
import com.serotonin.cebikes.registry.SoundRegistry
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

class Cebikes : ModInitializer {

    override fun onInitialize() {
        CebikesBlocks.register()
        CebikesBlockEntities.register()
        CebikesEntities.register()
        CebikesItems.register()
        ModItemGroups.registerCebikesItemGroups()
        CebikesRecipes.register()
        CebikesConfig.register()
        SoundRegistry.initCebikesSounds()
        ParticleRegistry.register()

        PayloadTypeRegistry.playC2S().register(BikeInputPayload.ID, BikeInputPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(HeadlightTogglePayload.ID, HeadlightTogglePayload.CODEC)
        PayloadTypeRegistry.playC2S().register(BikeCustomizePayload.ID, BikeCustomizePayload.CODEC)

        PayloadTypeRegistry.playS2C().register(OpenBikeCustomizerPayload.ID, OpenBikeCustomizerPayload.CODEC)

        ServerPlayNetworking.registerGlobalReceiver(BikeInputPayload.ID) { payload, context ->
            context.server().execute {
                val player  = context.player()
                val vehicle = player.vehicle
                if (vehicle is AbstractBikeEntity) {
                    vehicle.updateInput(
                        payload.forward,
                        payload.backward,
                        payload.steerLeft,
                        payload.steerRight,
                        payload.jumpStrength,
                        payload.brake
                    )
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(HeadlightTogglePayload.ID) { _, context ->
            context.server().execute {
                val vehicle = context.player().vehicle
                if (vehicle is AbstractBikeEntity) {
                    vehicle.toggleHeadlight()
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(BikeCustomizePayload.ID) { payload, context ->
            context.server().execute {
                val player = context.player()
                val world  = player.serverWorld
                val entity = world.getEntityById(payload.entityId)
                if (entity is AbstractBikeEntity) {
                    if (entity.firstPassenger == player || entity.squaredDistanceTo(player) < 16.0) {
                        entity.setBoneColors(payload.boneColors)
                        entity.setBellType(payload.bellType)
                    }
                }
            }
        }
    }
}