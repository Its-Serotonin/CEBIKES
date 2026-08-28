package com.serotonin.cebikes.client.network

import com.serotonin.cebikes.client.input.CebikesKeyBindings
import com.serotonin.cebikes.client.sound.BikeMovingSound
import com.serotonin.cebikes.entity.AbstractBikeEntity
import com.serotonin.cebikes.entity.AcroBikeEntity
import com.serotonin.cebikes.network.BikeInputPayload
import com.serotonin.cebikes.network.HeadlightTogglePayload
import com.serotonin.cebikes.registry.SoundRegistry
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.client.util.InputUtil

object ClientBikeNetworking {

    private var prevBrakePressed = false
    private var prevBellPressed  = false
    private var movingSound: BikeMovingSound? = null

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val player  = client.player ?: return@register
            val options = client.options
            val brakeKey = KeyBindingHelper.getBoundKeyOf(CebikesKeyBindings.BRAKE)
            val vehicle = player.vehicle

            if (vehicle !is AbstractBikeEntity) {
                movingSound?.let {
                    client.soundManager.stop(it)
                    movingSound = null
                }

                if (brakeKey.category == InputUtil.Type.KEYSYM) {
                    val isPhysicallyPressed = InputUtil.isKeyPressed(client.window.handle, brakeKey.code)
                    val justPressed = isPhysicallyPressed && !prevBrakePressed
                    for (binding in listOf(options.sprintKey, options.sneakKey)) {
                        if (KeyBindingHelper.getBoundKeyOf(binding) == brakeKey) {
                            binding.pressed = isPhysicallyPressed
                            if (justPressed) binding.timesPressed++
                        }
                    }
                    prevBrakePressed = isPhysicallyPressed
                }
                prevBellPressed = false
                return@register
            }

            prevBrakePressed = false

            val speed = vehicle.velocity.horizontalLength()
            val isBackward = options.backKey.isPressed && !options.forwardKey.isPressed
            if (speed > 0.01) {
                if (movingSound == null || movingSound!!.isDone) {
                    movingSound = BikeMovingSound(vehicle, isBackward)
                    client.soundManager.play(movingSound!!)
                }
            } else {
                movingSound?.let {
                    client.soundManager.stop(it)
                    movingSound = null
                }
            }

            val jumpStrength = (vehicle as? AcroBikeEntity)?.consumeClientJumpStrength() ?: 0

            val brake = brakeKey.category == InputUtil.Type.KEYSYM &&
                        InputUtil.isKeyPressed(client.window.handle, brakeKey.code)

            val payload = BikeInputPayload(
                forward      = options.forwardKey.isPressed,
                backward     = options.backKey.isPressed,
                steerLeft    = options.leftKey.isPressed,
                steerRight   = options.rightKey.isPressed,
                jumpStrength = jumpStrength,
                brake        = brake
            )

            ClientPlayNetworking.send(payload)

            if (CebikesKeyBindings.HEADLIGHT.wasPressed()) {
                ClientPlayNetworking.send(HeadlightTogglePayload())
            }

            val bellKey = KeyBindingHelper.getBoundKeyOf(CebikesKeyBindings.BELL)
            val isBellPressed = bellKey.category == InputUtil.Type.KEYSYM &&
                                InputUtil.isKeyPressed(client.window.handle, bellKey.code)
            if (isBellPressed && !prevBellPressed) {
                val bellSound = SoundRegistry.getBellSound(vehicle.getBellType())
                client.soundManager.play(PositionedSoundInstance.master(bellSound, 1f))
            }
            prevBellPressed = isBellPressed
        }
    }
}
