package com.serotonin.cebikes.entity

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.data.DataTracker
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

class HeadlightAnchorEntity(type: EntityType<*>, world: World) : Entity(type, world) {

    override fun updateTrackedPositionAndAngles(
        x: Double, y: Double, z: Double,
        yaw: Float, pitch: Float,
        interpolationSteps: Int
    ) {
        // Position is driven entirely by the bike entity's client tick.
    }

    override fun initDataTracker(builder: DataTracker.Builder) {}
    override fun readCustomDataFromNbt(nbt: NbtCompound) {}
    override fun writeCustomDataToNbt(nbt: NbtCompound) {}

    override fun shouldSave() = false
    override fun isPushable() = false
    override fun isCollidable() = false
    override fun canHit() = false
    override fun isSilent() = true

    companion object {
        val FORWARD_OFFSET = Vec3d(0.0, 0.5, 4.0)
    }
}