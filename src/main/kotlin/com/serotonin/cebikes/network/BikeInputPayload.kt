package com.serotonin.cebikes.network

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

data class BikeInputPayload(
    val forward:      Boolean,
    val backward:     Boolean,
    val steerLeft:    Boolean,
    val steerRight:   Boolean,
    val jumpStrength: Int,
    val brake:        Boolean
) : CustomPayload {

    companion object {
        val ID = CustomPayload.Id<BikeInputPayload>(Identifier.of("cebikes", "bike_input"))

        val CODEC: PacketCodec<RegistryByteBuf, BikeInputPayload> = PacketCodec.of(
            { value, buf ->
                buf.writeBoolean(value.forward)
                buf.writeBoolean(value.backward)
                buf.writeBoolean(value.steerLeft)
                buf.writeBoolean(value.steerRight)
                buf.writeInt(value.jumpStrength)
                buf.writeBoolean(value.brake)
            },
            { buf ->
                BikeInputPayload(
                    forward      = buf.readBoolean(),
                    backward     = buf.readBoolean(),
                    steerLeft    = buf.readBoolean(),
                    steerRight   = buf.readBoolean(),
                    jumpStrength = buf.readInt(),
                    brake        = buf.readBoolean()
                )
            }
        )
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID
}