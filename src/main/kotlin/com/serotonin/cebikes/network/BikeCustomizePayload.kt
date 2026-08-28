package com.serotonin.cebikes.network

import net.minecraft.nbt.NbtCompound
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

data class BikeCustomizePayload(
    val entityId: Int,
    val boneColors: NbtCompound,
    val bellType: Int
) : CustomPayload {

    companion object {
        val ID = CustomPayload.Id<BikeCustomizePayload>(
            Identifier.of("cebikes", "bike_customize")
        )
        val CODEC: PacketCodec<RegistryByteBuf, BikeCustomizePayload> = PacketCodec.of(
            { value, buf ->
                buf.writeInt(value.entityId)
                buf.writeNbt(value.boneColors)
                buf.writeInt(value.bellType)
            },
            { buf ->
                val entityId = buf.readInt()
                val nbt = buf.readNbt() ?: NbtCompound()
                val bellType = buf.readInt()
                BikeCustomizePayload(entityId, nbt, bellType)
            }
        )
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID
}