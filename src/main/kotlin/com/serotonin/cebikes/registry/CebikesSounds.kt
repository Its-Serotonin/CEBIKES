package com.serotonin.cebikes.registry

import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier

object SoundRegistry {

    lateinit var BIKE_FORWARDS: SoundEvent
    lateinit var BIKE_REVERSE: SoundEvent
    lateinit var BIKE_JUMP: SoundEvent
    lateinit var HEADLIGHT_ON: SoundEvent
    lateinit var HEADLIGHT_OFF: SoundEvent

    lateinit var BELL_STANDARD: SoundEvent
    lateinit var BELL_STAB: SoundEvent
    lateinit var BELL_STANDARD2: SoundEvent
    lateinit var BELL_ROTOM: SoundEvent
    lateinit var BELL_PIKA: SoundEvent

    lateinit var GUI_OPEN: SoundEvent
    lateinit var BUTTON_PRESS: SoundEvent
    lateinit var BUTTON_26: SoundEvent
    lateinit var BUTTON_4768: SoundEvent

    fun initCebikesSounds() {

        BIKE_FORWARDS = registerSound("bikes.bike_forwards")
        BIKE_REVERSE = registerSound("bikes.bike_reverse")
        BIKE_JUMP = registerSound("bikes.bike_jump")
        HEADLIGHT_ON = registerSound("bikes.headlight_on")
        HEADLIGHT_OFF = registerSound("bikes.headlight_off")

        BELL_STANDARD = registerSound("bikes.bell_standard")
        BELL_STAB = registerSound("bikes.bell_stab")
        BELL_STANDARD2  = registerSound("bikes.bell_standard2")
        BELL_ROTOM  = registerSound("bikes.bell_rotom")
        BELL_PIKA  = registerSound("bikes.bell_pika")

        BUTTON_PRESS = registerSound("bikes.button_press")
        BUTTON_26 = registerSound("bikes.button26")
        BUTTON_4768 = registerSound("bikes.button4768")

        GUI_OPEN = registerSound("bikes.gui_open")
    }

    fun getBells(): List<Pair<SoundEvent, String>> = listOf(
        BELL_STANDARD to "Standard",
        BELL_STAB to "Stab",
        BELL_STANDARD2  to "Standard 2",
        BELL_ROTOM to "Rotom",
        BELL_PIKA to "Pika",
    )

    fun getBellSound(index: Int): SoundEvent = getBells()[index.coerceIn(0, getBells().size - 1)].first
    fun getBellName(index: Int): String = getBells()[index.coerceIn(0, getBells().size - 1)].second
    val BELL_COUNT get() = getBells().size

    private fun registerSound(id: String): SoundEvent {
        val identifier = Identifier.of("cebikes", id)
        val event = SoundEvent.of(identifier)
        Registry.register(Registries.SOUND_EVENT, identifier, event)
        return event
    }
}