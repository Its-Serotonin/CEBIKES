package com.serotonin.cebikes.block

import com.serotonin.cebikes.registry.CebikesBlockEntities
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.RegistryWrapper
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos

class BikeRackBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(CebikesBlockEntities.BIKE_RACK, pos, state) {

    private var storedBike: ItemStack = ItemStack.EMPTY

    fun hasBike(): Boolean = !storedBike.isEmpty

    fun getStoredBike(): ItemStack = storedBike.copy()

    fun storeBike(stack: ItemStack) {
        storedBike = stack.copyWithCount(1)
        markDirty()
    }

    fun removeBike(): ItemStack {
        val result = storedBike.copy()
        storedBike = ItemStack.EMPTY
        markDirty()
        return result
    }

    // markDirty pushes the update packet immediately to nearby clients in addition
    // to the normal chunk-save flagging.
    override fun markDirty() {
        super.markDirty()
        world?.updateListeners(pos, cachedState, cachedState, Block.NOTIFY_LISTENERS)
    }

    // ── Persistence ───────────────────────────────────────────────────────
    override fun writeNbt(nbt: NbtCompound, registryLookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, registryLookup)
        if (!storedBike.isEmpty) {
            nbt.put("StoredBike", storedBike.encodeAllowEmpty(registryLookup))
        }
    }

    override fun readNbt(nbt: NbtCompound, registryLookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, registryLookup)
        storedBike = if (nbt.contains("StoredBike"))
            ItemStack.fromNbtOrEmpty(registryLookup, nbt.getCompound("StoredBike"))
        else ItemStack.EMPTY
    }

    // ── Client sync packets ───────────────────────────────────────────────
    override fun toUpdatePacket(): Packet<ClientPlayPacketListener> =
        BlockEntityUpdateS2CPacket.create(this)

    override fun toInitialChunkDataNbt(registryLookup: RegistryWrapper.WrapperLookup): NbtCompound =
        createNbt(registryLookup)
}