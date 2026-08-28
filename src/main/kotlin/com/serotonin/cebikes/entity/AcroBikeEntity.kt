package com.serotonin.cebikes.entity

import com.serotonin.cebikes.registry.CebikesItems
import com.serotonin.cebikes.registry.SoundRegistry
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.NbtComponent
import net.minecraft.entity.EntityType
import net.minecraft.entity.JumpingMount
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import kotlin.math.cos
import kotlin.math.sin

class AcroBikeEntity(type: EntityType<*>, world: World) : AbstractBikeEntity(type, world), JumpingMount {

    override val maxForwardSpeed     = 0.57
    override val acceleration        = 0.017
    override val naturalDeceleration = 0.93
    override val baseTurnRate        = 3.7f
    override val maxDriftTurnRate    = 6.0f
    override val defaultColor = 0xe4473a

    override fun getStepHeight(): Float = 1.1f

    override fun passengerOffset(): Vec3d = Vec3d(0.0, 1.4, -0.54)

    private var pendingJumpStrength = 0

    var clientJumpStrength = 0
        private set

    fun consumeClientJumpStrength(): Int {
        val s = clientJumpStrength
        clientJumpStrength = 0
        return s
    }

    override fun canJump(): Boolean = true

    override fun setJumpStrength(strength: Int) {
        if (world.isClient) clientJumpStrength = strength
    }

    override fun startJumping(height: Int) {}
    override fun stopJumping() {}

    override fun onJumpInput(strength: Int) {
        pendingJumpStrength = strength
    }

    override fun tick() {
        if (!world.isClient && pendingJumpStrength > 0 && isOnGround) {
            val power = 0.3 + 0.5 * (pendingJumpStrength / 100.0)
            velocity = Vec3d(velocity.x, power, velocity.z)
            pendingJumpStrength = 0
            playSound(SoundRegistry.BIKE_JUMP, 0.3f, 0.8f + random.nextFloat() * 0.4f)
            spawnJumpParticles()
        }
        super.tick()
    }

    private fun spawnJumpParticles() {
        val sw = world as? ServerWorld ?: return
        val jumpEffect = particleEffect("particle_jump", ParticleTypes.CLOUD, 2.9f)
        val velOffset = velocity.multiply(-2.0)
        repeat(12) {
            val dx = (random.nextDouble() - 0.5) * 1.2
            val dz = (random.nextDouble() - 0.5) * 1.2
            sw.spawnParticles(jumpEffect,
                x + dx + velOffset.x, y + 0.1, z + dz + velOffset.z,
                2, 0.02, 0.02, 0.0, 0.08)
        }
        val yawRad = yaw * Math.PI / 180.0
        val fwd = Vec3d(-sin(yawRad), 0.0, cos(yawRad))
        for (sign in listOf(1.0, -1.0)) {
            sw.spawnParticles(ParticleTypes.EXPLOSION,
                x + fwd.z * sign * 0.5 + velOffset.x, y,
                z - fwd.x * sign * 0.5 + velOffset.z,
                1, 0.1, 0.0, 0.1, 0.0)
        }
    }

    override fun createBikeItemStack(): ItemStack {
        val stack = ItemStack(CebikesItems.ACRO_BIKE)
        val boneColors = dataTracker.get(BONE_COLORS)
        val nbt = NbtCompound()
        nbt.put("BoneColors", boneColors.copy())
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt))
        return stack
    }
}