package com.serotonin.cebikes

import com.serotonin.cebikes.entity.AbstractBikeEntity
import com.serotonin.cebikes.network.BikeInputPayload
import com.serotonin.cebikes.network.HeadlightTogglePayload
import com.serotonin.cebikes.registry.CebikesBlockEntities
import com.serotonin.cebikes.registry.CebikesBlocks
import com.serotonin.cebikes.registry.CebikesEntities
import com.serotonin.cebikes.registry.CebikesItems
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

class Cebikes : ModInitializer {

    override fun onInitialize() {
        CebikesBlocks.register()
        CebikesBlockEntities.register()
        CebikesEntities.register()
        CebikesItems.register()

        // Register C2S packet types
        PayloadTypeRegistry.playC2S().register(BikeInputPayload.ID, BikeInputPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(HeadlightTogglePayload.ID, HeadlightTogglePayload.CODEC)

        // Handle bike input packets from clients
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
                        payload.jump,
                        payload.brake
                    )
                }
            }
        }

        // Handle headlight toggle packets
        ServerPlayNetworking.registerGlobalReceiver(HeadlightTogglePayload.ID) { _, context ->
            context.server().execute {
                val vehicle = context.player().vehicle
                if (vehicle is AbstractBikeEntity) {
                    vehicle.toggleHeadlight()
                }
            }
        }
    }
}