package com.serotonin.cebikes.client

import com.serotonin.cebikes.client.block.BikeRackBlockEntityRenderer
import com.serotonin.cebikes.client.entity.renderer.BikeEntityRenderer
import com.serotonin.cebikes.client.input.CebikesKeyBindings
import com.serotonin.cebikes.client.network.ClientBikeNetworking
import com.serotonin.cebikes.registry.CebikesBlockEntities
import com.serotonin.cebikes.registry.CebikesEntities
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories

class CebikesClient : ClientModInitializer {

    override fun onInitializeClient() {
        CebikesKeyBindings.register()
        ClientBikeNetworking.register()

        EntityRendererRegistry.register(CebikesEntities.MACH_BIKE) { context ->
            BikeEntityRenderer(context)
        }

        EntityRendererRegistry.register(CebikesEntities.ACRO_BIKE) { context ->
            BikeEntityRenderer(context)
        }

       BlockEntityRendererFactories.register(CebikesBlockEntities.BIKE_RACK, ::BikeRackBlockEntityRenderer)
    }
}