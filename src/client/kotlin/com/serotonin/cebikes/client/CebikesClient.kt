package com.serotonin.cebikes.client

import com.serotonin.cebikes.client.block.BikeRackBlockEntityRenderer
import com.serotonin.cebikes.client.block.BikeRackItemRenderer
import com.serotonin.cebikes.client.entity.renderer.BikeEntityRenderer
import com.serotonin.cebikes.client.gui.BikeCustomizerScreen
import com.serotonin.cebikes.client.input.CebikesKeyBindings
import com.serotonin.cebikes.client.item.renderer.BikeItemRenderer
import com.serotonin.cebikes.client.network.ClientBikeNetworking
import com.serotonin.cebikes.client.particle.BrakeSmokeParticle
import com.serotonin.cebikes.entity.AbstractBikeEntity
import com.serotonin.cebikes.entity.HeadlightAnchorEntity
import com.serotonin.cebikes.item.BikeItem
import com.serotonin.cebikes.item.BikeRackItem
import com.serotonin.cebikes.item.BikeRendererProvider
import com.serotonin.cebikes.network.OpenBikeCustomizerPayload
import com.serotonin.cebikes.registry.CebikesBlockEntities
import com.serotonin.cebikes.registry.CebikesEntities
import com.serotonin.cebikes.registry.ParticleRegistry
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.particle.ParticleFactory
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories
import net.minecraft.client.render.entity.EntityRenderer
import net.minecraft.util.Identifier
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import kotlin.math.cos
import kotlin.math.sin

class CebikesClient : ClientModInitializer {

    override fun onInitializeClient() {

        BikeItem.rendererProvider = BikeRendererProvider { modelId, textureId, animId ->
            BikeItemRenderer(modelId, textureId, animId)
        }

        BikeRackItem.rendererProvider = { BikeRackItemRenderer() }

        CebikesKeyBindings.register()
        ClientBikeNetworking.register()

        WorldRenderEvents.START.register { context ->
            val client = MinecraftClient.getInstance()
            val world = client.world ?: return@register
            val player = client.player ?: return@register
            val partial = context.tickCounter().getTickDelta(false).toDouble()
            val searchBox = Box.of(player.pos, 128.0, 128.0, 128.0)
            for (bike in world.getEntitiesByClass(AbstractBikeEntity::class.java, searchBox) { it.headlightOn }) {
                val anchorId = bike.dataTracker.get(AbstractBikeEntity.HEADLIGHT_ANCHOR_ID)
                if (anchorId == -1) continue
                val anchor = world.getEntityById(anchorId) as? HeadlightAnchorEntity ?: continue
                val ix = bike.prevX + (bike.x - bike.prevX) * partial
                val iy = bike.prevY + (bike.y - bike.prevY) * partial
                val iz = bike.prevZ + (bike.z - bike.prevZ) * partial
                val iYaw = (bike.prevYaw + MathHelper.wrapDegrees(bike.yaw - bike.prevYaw) * partial) * (Math.PI / 180.0)
                anchor.setPosition(
                    ix - sin(iYaw) * HeadlightAnchorEntity.FORWARD_OFFSET.z,
                    iy + HeadlightAnchorEntity.FORWARD_OFFSET.y,
                    iz + cos(iYaw) * HeadlightAnchorEntity.FORWARD_OFFSET.z
                )
            }
        }

        EntityRendererRegistry.register(CebikesEntities.MACH_BIKE) { context ->
            BikeEntityRenderer(context)
        }

        EntityRendererRegistry.register(CebikesEntities.ACRO_BIKE) { context ->
            BikeEntityRenderer(context)
        }

        EntityRendererRegistry.register(CebikesEntities.HEADLIGHT_ANCHOR) { context ->
            object : EntityRenderer<HeadlightAnchorEntity>(context) {
                override fun getTexture(entity: HeadlightAnchorEntity) =
                    Identifier.of("cebikes", "textures/entity/empty.png")
            }
        }

        BlockEntityRendererFactories.register(CebikesBlockEntities.BIKE_RACK) {
            BikeRackBlockEntityRenderer()
        }

        ParticleFactoryRegistry.getInstance().register(ParticleRegistry.BRAKE_SMOKE) { sprites ->
            ParticleFactory { effect, world, x, y, z, vx, vy, vz ->
                BrakeSmokeParticle(world, x, y, z, vx, vy, vz, effect, sprites)
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(OpenBikeCustomizerPayload.ID) { payload, context ->
            val client = context.client()
            client.execute {
                val world = client.world ?: return@execute
                val entity = world.getEntityById(payload.entityId) as? AbstractBikeEntity ?: return@execute
                client.setScreen(BikeCustomizerScreen(entity))
            }
        }
    }
}