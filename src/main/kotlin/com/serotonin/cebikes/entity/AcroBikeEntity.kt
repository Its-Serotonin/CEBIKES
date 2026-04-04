package com.serotonin.cebikes.entity

import com.serotonin.cebikes.registry.CebikesItems
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.NbtComponent
import net.minecraft.entity.EntityType
import net.minecraft.entity.JumpingMount
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

class AcroBikeEntity(type: EntityType<*>, world: World) : AbstractBikeEntity(type, world), JumpingMount {

    override val maxForwardSpeed     = 0.55
    override val acceleration        = 0.018
    override val naturalDeceleration = 0.90
    override val baseTurnRate        = 3.7f
    override val maxDriftTurnRate    = 7.7f

    /** Raised step height — can climb up full 1-block-tall walls. */
    override fun getStepHeight(): Float = 1.1f

    // ── Charged jump (horse-style bar) ────────────────────────────────────
    private var pendingJumpStrength = 0

    override fun canJump(): Boolean = true

    override fun setJumpStrength(strength: Int) {
        pendingJumpStrength = strength
    }

    override fun startJumping(height: Int) {}
    override fun stopJumping() {}

    override fun tick() {
        // Apply before super.tick() so the same tick's move() carries the jump upward.
        // handleMovement preserves velocity.y, so the power survives into move().
        if (!world.isClient && pendingJumpStrength > 0 && isOnGround) {
            val power = 0.3 + 0.5 * (pendingJumpStrength / 100.0)
            velocity = Vec3d(velocity.x, power, velocity.z)
            pendingJumpStrength = 0
        }
        super.tick()
    }

    override fun createBikeItemStack(): ItemStack {
        val stack = ItemStack(CebikesItems.ACRO_BIKE)
        val color = dataTracker.get(COLOR_KEY)
        if (color != DEFAULT_COLOR) {
            val nbt = NbtCompound()
            nbt.putInt("BikeColor", color)
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt))
        }
        return stack
    }
}