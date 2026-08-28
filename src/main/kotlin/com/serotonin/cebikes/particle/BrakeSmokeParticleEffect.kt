package com.serotonin.cebikes.particle

import com.mojang.serialization.MapCodec
import com.serotonin.cebikes.registry.ParticleRegistry
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.particle.ParticleEffect
import net.minecraft.particle.ParticleType


class BrakeSmokeParticleEffect(
    val fromR: Float, val fromG: Float, val fromB: Float,
    val toR:   Float, val toG:   Float, val toB:   Float,
    val scale: Float
) : ParticleEffect {

    override fun getType(): ParticleType<*> = ParticleRegistry.BRAKE_SMOKE

    companion object {
        val CODEC: MapCodec<BrakeSmokeParticleEffect> = MapCodec.unit(
            BrakeSmokeParticleEffect(0f, 0f, 0f, 0.55f, 0.55f, 0.55f, 2.0f)
        )

        val PACKET_CODEC: PacketCodec<RegistryByteBuf, BrakeSmokeParticleEffect> =
            PacketCodec.of(
                { value, buf ->
                    buf.writeFloat(value.fromR); buf.writeFloat(value.fromG); buf.writeFloat(value.fromB)
                    buf.writeFloat(value.toR);   buf.writeFloat(value.toG);   buf.writeFloat(value.toB)
                    buf.writeFloat(value.scale)
                },
                { buf ->
                    BrakeSmokeParticleEffect(
                        buf.readFloat(), buf.readFloat(), buf.readFloat(),
                        buf.readFloat(), buf.readFloat(), buf.readFloat(),
                        buf.readFloat()
                    )
                }
            )
    }
}