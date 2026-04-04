package com.serotonin.cebikes.network

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

class HeadlightTogglePayload : CustomPayload {

    companion object {
        val ID = CustomPayload.Id<HeadlightTogglePayload>(Identifier.of("cebikes", "headlight_toggle"))

        val CODEC: PacketCodec<RegistryByteBuf, HeadlightTogglePayload> = PacketCodec.of(
            { _, _ -> },         // nothing to write
            { _ -> HeadlightTogglePayload() }  // nothing to read
        )
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID
}