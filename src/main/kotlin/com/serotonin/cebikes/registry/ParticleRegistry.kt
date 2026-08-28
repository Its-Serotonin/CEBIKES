package com.serotonin.cebikes.registry

import com.serotonin.cebikes.particle.BrakeSmokeParticleEffect
import net.minecraft.particle.ParticleType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier


object ParticleRegistry {
    lateinit var BRAKE_SMOKE: ParticleType<BrakeSmokeParticleEffect>

    fun register() {
        BRAKE_SMOKE = Registry.register(
            Registries.PARTICLE_TYPE,
            Identifier.of("cebikes", "brake_smoke"),
            object : ParticleType<BrakeSmokeParticleEffect>(false) {
                override fun getCodec()       = BrakeSmokeParticleEffect.CODEC
                override fun getPacketCodec() = BrakeSmokeParticleEffect.PACKET_CODEC
            }
        )
    }
}