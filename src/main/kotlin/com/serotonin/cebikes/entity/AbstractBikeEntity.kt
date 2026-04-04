package com.serotonin.cebikes.entity

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.MovementType
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.data.DataTracker
import net.minecraft.entity.data.TrackedData
import net.minecraft.entity.data.TrackedDataHandlerRegistry
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.DyeItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundEvents
import net.minecraft.util.ActionResult
import net.minecraft.util.DyeColor
import net.minecraft.util.Hand
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil
import kotlin.math.*

abstract class AbstractBikeEntity(type: EntityType<*>, world: World) : Entity(type, world), GeoEntity {

    // ── GeckoLib ──────────────────────────────────────────────────────────
    private val geoCache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = geoCache

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
        // No pre-baked animations yet; wheel/handlebar motion is driven procedurally in the renderer.
    }

    // ── Bike characteristics ───────────────────────────────────────────────
    abstract val maxForwardSpeed: Double
    abstract val acceleration: Double
    /** Multiplied against currentSpeed each tick with no throttle input. */
    abstract val naturalDeceleration: Double
    /** Base turn rate in degrees per tick at any speed. */
    abstract val baseTurnRate: Float
    /** Maximum turn rate during a full drift (after holding steer long enough). */
    abstract val maxDriftTurnRate: Float

    // ── Server-side input ──────────────────────────────────────────────────
    var pressingForward  = false
    var pressingBackward = false
    var pressingSteerLeft  = false
    var pressingSteerRight = false
    var pressingJump     = false
    var pressingBrake    = false

    // ── Physics state ──────────────────────────────────────────────────────
    private var wasBraking    = false
    var currentSpeed = 0.0
        private set

    // ── Steering / drift tracking ─────────────────────────────────────────
    /** -1 = left, 0 = none, 1 = right */
    private var steerDirection = 0
    /**
     * How many ticks we've been continuously steering in [steerDirection].
     * Drift bonuses kick in after [DRIFT_THRESHOLD] ticks.
     */
    private var steerDuration = 0

    private var bikeYawInitialized = false

    /** Remembers last non-zero steer direction for brake skid. */
    private var lastNonZeroSteerDirection = 0

    // ── Client-side interpolation ─────────────────────────────────────────
    private var lerpSteps = 0
    private var lerpX = 0.0
    private var lerpY = 0.0
    private var lerpZ = 0.0
    private var lerpYaw = 0.0

    companion object {
        private const val DRIFT_THRESHOLD  = 40   // ticks before drift bonuses appear
        private const val DRIFT_RAMP_TICKS = 30   // ticks over which drift reaches max

        val COLOR_KEY: TrackedData<Int> = DataTracker.registerData(
            AbstractBikeEntity::class.java,
            TrackedDataHandlerRegistry.INTEGER
        )
        val STEER_ANGLE: TrackedData<Float> = DataTracker.registerData(
            AbstractBikeEntity::class.java,
            TrackedDataHandlerRegistry.FLOAT
        )
        val HEADLIGHT_ON: TrackedData<Boolean> = DataTracker.registerData(
            AbstractBikeEntity::class.java,
            TrackedDataHandlerRegistry.BOOLEAN
        )
        const val DEFAULT_COLOR = 0xFFFFFF

        private val DYE_TO_RGB = mapOf(
            DyeColor.WHITE      to 0xF9FFFE,
            DyeColor.ORANGE     to 0xF9801D,
            DyeColor.MAGENTA    to 0xC74EBD,
            DyeColor.LIGHT_BLUE to 0x3AB3DA,
            DyeColor.YELLOW     to 0xFED83D,
            DyeColor.LIME       to 0x80C71F,
            DyeColor.PINK       to 0xF38BAA,
            DyeColor.GRAY       to 0x474F52,
            DyeColor.LIGHT_GRAY to 0x9D9D97,
            DyeColor.CYAN       to 0x169C9C,
            DyeColor.PURPLE     to 0x8932B8,
            DyeColor.BLUE       to 0x3C44AA,
            DyeColor.BROWN      to 0x835432,
            DyeColor.GREEN      to 0x5E7C16,
            DyeColor.RED        to 0xB02E26,
            DyeColor.BLACK      to 0x1D1D21
        )
    }

    /** Client-side accumulated wheel rotation in radians (not synced). */
    var wheelRotation = 0f
        private set

    /** Visual handlebar angle in degrees, synced via DataTracker. */
    val steerAngle: Float
        get() = dataTracker.get(STEER_ANGLE)

    /** Whether the headlight is currently on. */
    val headlightOn: Boolean
        get() = dataTracker.get(HEADLIGHT_ON)

    fun toggleHeadlight() {
        dataTracker.set(HEADLIGHT_ON, !headlightOn)
    }

    // ── DataTracker / NBT ─────────────────────────────────────────────────
    override fun initDataTracker(builder: DataTracker.Builder) {
        builder.add(COLOR_KEY, DEFAULT_COLOR)
        builder.add(STEER_ANGLE, 0f)
        builder.add(HEADLIGHT_ON, false)
    }

    override fun readCustomDataFromNbt(nbt: NbtCompound) {
        val stored = nbt.getInt("BikeColor")
        dataTracker.set(COLOR_KEY, if (stored == 0) DEFAULT_COLOR else stored)
        dataTracker.set(HEADLIGHT_ON, nbt.getBoolean("HeadlightOn"))
        currentSpeed = nbt.getDouble("CurrentSpeed")
        yaw          = nbt.getFloat("BikeYaw")
        headYaw      = yaw
    }

    override fun writeCustomDataToNbt(nbt: NbtCompound) {
        nbt.putInt("BikeColor",    dataTracker.get(COLOR_KEY))
        nbt.putBoolean("HeadlightOn", headlightOn)
        nbt.putDouble("CurrentSpeed", currentSpeed)
        nbt.putFloat("BikeYaw",   yaw)
    }

    // ── Interactability ───────────────────────────────────────────────────
    /** Allows the player to punch/attack to pick up the bike. */
    override fun canHit() = !isRemoved

    // ── Interaction ───────────────────────────────────────────────────────
    override fun interact(player: PlayerEntity, hand: Hand): ActionResult {
        val stack = player.getStackInHand(hand)
        val dye   = stack.item as? DyeItem

        if (dye != null) {
            if (!world.isClient) {
                dataTracker.set(COLOR_KEY, DYE_TO_RGB[dye.color] ?: DEFAULT_COLOR)
                if (!player.abilities.creativeMode) stack.decrement(1)
            }
            return ActionResult.SUCCESS
        }

        if (!hasPassengers() && !player.isSneaking) {
            if (!world.isClient) player.startRiding(this)
            return ActionResult.SUCCESS
        }

        return ActionResult.PASS
    }

    // ── Damage / pick up ──────────────────────────────────────────────────
    override fun damage(source: DamageSource, amount: Float): Boolean {
        if (isRemoved || world.isClient) return false
        removeAllPassengers()
        val ie = ItemEntity(world, x, y + 0.5, z, createBikeItemStack())
        ie.setVelocity(0.0, 0.2, 0.0)
        world.spawnEntity(ie)
        this.remove(RemovalReason.KILLED)
        return true
    }

    abstract fun createBikeItemStack(): ItemStack

    // ── Passenger positioning ─────────────────────────────────────────────
    override fun getPassengerRidingPos(passenger: Entity): Vec3d =
        Vec3d(x, y + 1.60, z - 1)

    override fun canAddPassenger(passenger: Entity) = passengerList.isEmpty()

    override fun getControllingPassenger(): LivingEntity? =
        firstPassenger as? LivingEntity

    // Movement is server-authoritative (via our custom packets), so the client
    // must never think it owns position updates — even though it has a controlling passenger.
    override fun isLogicalSideForUpdatingMovement(): Boolean = !world.isClient

    // GeoEntityRenderer reads bodyYaw, but Entity (not LivingEntity) never updates it.
    override fun getBodyYaw(): Float = yaw

    // ── Client-side interpolation (smooth networked movement) ─────────────
    override fun updateTrackedPositionAndAngles(
        x: Double, y: Double, z: Double,
        yaw: Float, pitch: Float,
        interpolationSteps: Int
    ) {
        lerpX = x
        lerpY = y
        lerpZ = z
        lerpYaw = yaw.toDouble()
        lerpSteps = interpolationSteps + 2
    }

    // ── Input (from server networking) ────────────────────────────────────
    fun updateInput(
        forward: Boolean, backward: Boolean,
        steerLeft: Boolean, steerRight: Boolean,
        jump: Boolean, brake: Boolean
    ) {
        pressingForward    = forward
        pressingBackward   = backward
        pressingSteerLeft  = steerLeft
        pressingSteerRight = steerRight
        pressingJump       = jump
        pressingBrake      = brake
    }

    override fun removePassenger(passenger: Entity) {
        super.removePassenger(passenger)
        pressingForward    = false
        pressingBackward   = false
        pressingSteerLeft  = false
        pressingSteerRight = false
        pressingJump       = false
        pressingBrake      = false
        steerDirection     = 0
        steerDuration      = 0
        lastNonZeroSteerDirection = 0
        bikeYawInitialized = false
    }

    // ── Tick ──────────────────────────────────────────────────────────────
    override fun tick() {
        super.tick()

        if (world.isClient) {
            // Smoothly interpolate toward the server-sent position/yaw
            if (lerpSteps > 0) {
                val d = 1.0 / lerpSteps
                setPosition(
                    this.x + (lerpX - this.x) * d,
                    this.y + (lerpY - this.y) * d,
                    this.z + (lerpZ - this.z) * d
                )
                val yawDiff = MathHelper.wrapDegrees(lerpYaw - this.yaw.toDouble())
                this.yaw     += (yawDiff * d).toFloat()
                this.headYaw  = this.yaw
                this.bodyYaw  = this.yaw
                lerpSteps--
            }

            val speed = velocity.horizontalLength()
            wheelRotation = (wheelRotation + speed.toFloat() * (2f * PI.toFloat() / 2.5f)) % (2f * PI.toFloat())
            return
        }

        if (!isOnGround) velocity = velocity.add(0.0, -0.08, 0.0)

        val rider = firstPassenger as? PlayerEntity
        if (rider != null) {
            handleMovement(rider)
        } else {
            currentSpeed *= 0.85
            if (abs(currentSpeed) < 0.001) currentSpeed = 0.0
            velocity = Vec3d(velocity.x * 0.85, velocity.y, velocity.z * 0.85)
        }

        move(MovementType.SELF, velocity)
        velocityModified = true
    }

    private fun handleMovement(rider: PlayerEntity) {
        // On first tick after mounting, inherit rider's facing direction.
        if (!bikeYawInitialized) {
            yaw    = rider.yaw
            headYaw = yaw
            prevYaw = yaw
            bikeYawInitialized = true
        }

        // ── Steering / drift ───────────────────────────────────────────────
        val rawSteer = when {
            pressingSteerLeft  && !pressingSteerRight -> -1
            pressingSteerRight && !pressingSteerLeft  ->  1
            else                                      ->  0
        }

        // Track how long we've been steering in the same direction.
        if (rawSteer != 0 && rawSteer == steerDirection) {
            steerDuration++
        } else {
            steerDirection = rawSteer
            steerDuration  = if (rawSteer != 0) 1 else 0
        }

        // Remember last steering direction for brake skid
        if (rawSteer != 0) lastNonZeroSteerDirection = rawSteer

        // Drift bonus ramps up after DRIFT_THRESHOLD ticks.
        val driftProgress = if (steerDuration > DRIFT_THRESHOLD)
            min(1f, (steerDuration - DRIFT_THRESHOLD).toFloat() / DRIFT_RAMP_TICKS)
        else 0f

        if (rawSteer != 0 && abs(currentSpeed) > 0.01) {
            val turnRate = baseTurnRate + (maxDriftTurnRate - baseTurnRate) * driftProgress

            // Reverse steering sense when going backward (like reversing a vehicle).
            val speedSign = if (currentSpeed >= 0) 1f else -1f
            yaw     += rawSteer * turnRate * speedSign
            headYaw  = yaw
            prevYaw  = yaw

            // Light smoke from rear tyre during a drift
            if (driftProgress > 0.1f && world is ServerWorld) {
                spawnDriftParticles(world as ServerWorld)
            }
        }

        // ── Handlebar visual angle (synced to client) ──────────────────────
        val handlebarAngle = if (rawSteer != 0) {
            val driftBonus = if (abs(currentSpeed) > 0.01) 10f * driftProgress else 0f
            rawSteer * (15f + driftBonus)
        } else 0f
        dataTracker.set(STEER_ANGLE, handlebarAngle)

        val yawRad = yaw * (PI / 180.0)

        // ── Braking ────────────────────────────────────────────────────────
        if (pressingBrake) {
            val wasMoving = abs(currentSpeed) > 0.05
            currentSpeed *= 0.45
            if (abs(currentSpeed) < 0.02) currentSpeed = 0.0

            // Skid rotation — slide toward the side the bike was already steering
            if (wasMoving) {
                val skidDir = if (steerDirection != 0) steerDirection else lastNonZeroSteerDirection
                if (skidDir != 0) {
                    val skidRate = 4.0f * (abs(currentSpeed) / maxForwardSpeed).toFloat().coerceAtMost(1f)
                    yaw     += skidDir * skidRate
                    headYaw  = yaw
                    prevYaw  = yaw
                }

                if (!wasBraking) playSound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.4f, 0.8f + random.nextFloat() * 0.4f)
                if (world is ServerWorld) spawnBrakeParticles(world as ServerWorld, yawRad)
            }
            wasBraking = true
        } else {
            wasBraking = false
            when {
                pressingForward  -> currentSpeed = min(currentSpeed + acceleration, maxForwardSpeed)
                pressingBackward -> currentSpeed = max(currentSpeed - acceleration, -maxForwardSpeed * 0.25)
                else             -> {
                    currentSpeed *= naturalDeceleration
                    if (abs(currentSpeed) < 0.001) currentSpeed = 0.0
                }
            }
        }

        if (isOnGround) currentSpeed *= 0.96

        velocity = Vec3d(-sin(yawRad) * currentSpeed, velocity.y, cos(yawRad) * currentSpeed)
    }

    // ── Particles ─────────────────────────────────────────────────────────
    private fun spawnBrakeParticles(world: ServerWorld, yawRad: Double) {
        val fwd = Vec3d(-sin(yawRad), 0.0, cos(yawRad))
        for (sign in listOf(0.7, -0.7)) {
            val wx = x + fwd.x * sign
            val wz = z + fwd.z * sign
            repeat(2) {
                world.spawnParticles(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    wx + (random.nextDouble() - 0.5) * 0.3, y + 0.07,
                    wz + (random.nextDouble() - 0.5) * 0.3,
                    1, 0.05, 0.02, 0.05, 0.01
                )
            }
        }
    }

    private fun spawnDriftParticles(world: ServerWorld) {
        val yawRad = yaw * (PI / 180.0)
        val fwd = Vec3d(-sin(yawRad), 0.0, cos(yawRad))
        // Light smoke from the rear wheel
        world.spawnParticles(
            ParticleTypes.SMOKE,
            x - fwd.x * 0.6 + (random.nextDouble() - 0.5) * 0.5,
            y + 0.05,
            z - fwd.z * 0.6 + (random.nextDouble() - 0.5) * 0.5,
            1, 0.02, 0.02, 0.02, 0.005
        )
    }

    // ── Misc ──────────────────────────────────────────────────────────────
    override fun isPushable()   = false
    override fun isCollidable() = !isRemoved
}