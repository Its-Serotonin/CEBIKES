package com.serotonin.cebikes.client.network

import com.serotonin.cebikes.client.input.CebikesKeyBindings
import com.serotonin.cebikes.entity.AbstractBikeEntity
import com.serotonin.cebikes.network.BikeInputPayload
import com.serotonin.cebikes.network.HeadlightTogglePayload
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

object ClientBikeNetworking {

    fun register() {
        // The C2S payload type is already registered in the common initialiser (Cebikes.kt).
        // Here we only need to hook the client tick to send the input packet each frame
        // while the local player is riding one of our bikes.
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val player  = client.player ?: return@register
            val options = client.options

            if (player.vehicle !is AbstractBikeEntity) return@register

            val payload = BikeInputPayload(
                forward    = options.forwardKey.isPressed,
                backward   = options.backKey.isPressed,
                steerLeft  = options.leftKey.isPressed,
                steerRight = options.rightKey.isPressed,
                jump       = options.jumpKey.isPressed,
                brake      = CebikesKeyBindings.BRAKE.isPressed
            )

            ClientPlayNetworking.send(payload)

            // Headlight toggle (one-shot on key press, not held)
            if (CebikesKeyBindings.HEADLIGHT.wasPressed()) {
                ClientPlayNetworking.send(HeadlightTogglePayload())
            }
        }
    }
}