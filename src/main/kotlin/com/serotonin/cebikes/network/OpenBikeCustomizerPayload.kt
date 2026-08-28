package com.serotonin.cebikes.network

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

data class OpenBikeCustomizerPayload(val entityId: Int) : CustomPayload {

    companion object {
        val ID = CustomPayload.Id<OpenBikeCustomizerPayload>(
            Identifier.of("cebikes", "open_bike_customizer")
        )
        val CODEC: PacketCodec<RegistryByteBuf, OpenBikeCustomizerPayload> = PacketCodec.of(
            { value, buf -> buf.writeInt(value.entityId) },
            { buf -> OpenBikeCustomizerPayload(buf.readInt()) }
        )
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID
}