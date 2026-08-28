package com.serotonin.cebikes.client.sound

import com.serotonin.cebikes.entity.AbstractBikeEntity
import com.serotonin.cebikes.registry.SoundRegistry
import net.minecraft.client.sound.MovingSoundInstance
import net.minecraft.client.sound.SoundInstance
import net.minecraft.sound.SoundCategory

class BikeMovingSound(
    private val bike: AbstractBikeEntity,
    val isReverse: Boolean
) : MovingSoundInstance(
    if (isReverse) SoundRegistry.BIKE_REVERSE
    else SoundRegistry.BIKE_FORWARDS,
    SoundCategory.NEUTRAL,
    SoundInstance.createRandom()
) {
    init {
        repeat = true
        repeatDelay = 0
        volume = 1.2f
        pitch = 1.0f
    }

    override fun tick() {
        if (!bike.isAlive) {
            setDone()
            return
        }
        x = bike.x
        y = bike.y
        z = bike.z

        val speed = bike.velocity.horizontalLength()
        if (speed < 0.01) {
            setDone()
            return
        }
        volume = (speed * 2.0).toFloat().coerceIn(0.1f, 1.0f)
        pitch = (0.5f + speed.toFloat().coerceIn(0.5f, 1.0f))
    }
}