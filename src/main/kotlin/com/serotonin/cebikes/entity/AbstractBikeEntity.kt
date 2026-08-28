package com.serotonin.cebikes.entity

import com.serotonin.cebikes.network.OpenBikeCustomizerPayload
import com.serotonin.cebikes.particle.BrakeSmokeParticleEffect
import com.serotonin.cebikes.registry.CebikesEntities
import com.serotonin.cebikes.registry.CebikesItems
import com.serotonin.cebikes.registry.SoundRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
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
import net.minecraft.item.ShearsItem
import net.minecraft.nbt.NbtCompound
import net.minecraft.particle.DustColorTransitionParticleEffect
import net.minecraft.particle.DustParticleEffect
import net.minecraft.particle.ParticleEffect
import net.minecraft.particle.ParticleTypes
import org.joml.Vector3f
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundEvents
import net.minecraft.util.ActionResult
import net.minecraft.util.DyeColor
import net.minecraft.util.Hand
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil
import kotlin.math.*

abstract class AbstractBikeEntity(type: EntityType<*>, world: World) : Entity(type, world), GeoEntity {

    private val geoCache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = geoCache

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
        // No pre-baked animations; wheel/handlebar motion is driven procedurally in the renderer.
    }

    abstract val maxForwardSpeed: Double
    abstract val acceleration: Double
    abstract val naturalDeceleration: Double
    abstract val baseTurnRate: Float
    abstract val maxDriftTurnRate: Float

    var pressingForward  = false
    var pressingBackward = false
    var pressingSteerLeft  = false
    var pressingSteerRight = false
    var pressingBrake    = false
    var smoothedSteerRad = 0f

    private var wasBraking    = false
    var currentSpeed = 0.0
        private set

    private var steerDirection = 0
    private var steerDuration  = 0
    private var bikeYawInitialized = false
    private var lastNonZeroSteerDirection = 0

    private var lerpSteps = 0
    private var lerpX = 0.0
    private var lerpY = 0.0
    private var lerpZ = 0.0
    private var lerpYaw = 0.0

    companion object {
        private const val DRIFT_THRESHOLD  = 25
        private const val DRIFT_RAMP_TICKS = 30

        val BONE_COLORS: TrackedData<NbtCompound> = DataTracker.registerData(
            AbstractBikeEntity::class.java,
            TrackedDataHandlerRegistry.NBT_COMPOUND
        )
        val STEER_ANGLE: TrackedData<Float> = DataTracker.registerData(
            AbstractBikeEntity::class.java,
            TrackedDataHandlerRegistry.FLOAT
        )
        val HEADLIGHT_ON: TrackedData<Boolean> = DataTracker.registerData(
            AbstractBikeEntity::class.java,
            TrackedDataHandlerRegistry.BOOLEAN
        )
        val DRIFT_PROGRESS: TrackedData<Float> = DataTracker.registerData(
            AbstractBikeEntity::class.java,
            TrackedDataHandlerRegistry.FLOAT
        )
        val BELL_TYPE: TrackedData<Int> = DataTracker.registerData(
            AbstractBikeEntity::class.java,
            TrackedDataHandlerRegistry.INTEGER
        )
        val HEADLIGHT_ANCHOR_ID: TrackedData<Int> = DataTracker.registerData(
            AbstractBikeEntity::class.java,
            TrackedDataHandlerRegistry.INTEGER
        )

        val DYE_TO_RGB = mapOf(
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

    var wheelRotation = 0f
        private set

    var smoothedRollRad = 0f

    var isGuiRendering: Boolean = false

    val steerAngle: Float
        get() = dataTracker.get(STEER_ANGLE)

    val headlightOn: Boolean
        get() = dataTracker.get(HEADLIGHT_ON)

    fun toggleHeadlight() {
        val next = !headlightOn
        dataTracker.set(HEADLIGHT_ON, next)
        if (next) {
            playSound(SoundRegistry.HEADLIGHT_ON, 0.5f, 1.0f)
        } else {
            playSound(SoundRegistry.HEADLIGHT_OFF, 0.5f, 1.0f)
        }
    }

    fun getBellType(): Int = dataTracker.get(BELL_TYPE)

    fun setBellType(index: Int) {
        dataTracker.set(BELL_TYPE, index.coerceIn(0, SoundRegistry.BELL_COUNT - 1))
    }

    open val defaultColor: Int = 0xFFFFFF

    fun getBoneColors(): NbtCompound = dataTracker.get(BONE_COLORS)

    fun getBoneColor(boneName: String): Int {
        val nbt = dataTracker.get(BONE_COLORS)
        return if (nbt.contains(boneName)) nbt.getInt(boneName) else 0xFFFFFF
    }

    fun setBoneColors(colors: NbtCompound) {
        dataTracker.set(BONE_COLORS, colors.copy())
    }

    fun setBoneColor(boneName: String, rgb: Int) {
        val colors = dataTracker.get(BONE_COLORS).copy()
        colors.putInt(boneName, rgb)
        dataTracker.set(BONE_COLORS, colors)
    }

    fun applyDefaultColors() {
        val colors = NbtCompound()
        colors.putInt("frame", defaultColor)
        colors.putInt("headlight_mount", defaultColor)
        dataTracker.set(BONE_COLORS, colors)
    }

    override fun initDataTracker(builder: DataTracker.Builder) {
        builder.add(BONE_COLORS, NbtCompound())
        builder.add(STEER_ANGLE, 0f)
        builder.add(HEADLIGHT_ON, false)
        builder.add(DRIFT_PROGRESS, 0f)
        builder.add(BELL_TYPE, 0)
        builder.add(HEADLIGHT_ANCHOR_ID, -1)
    }

    override fun readCustomDataFromNbt(nbt: NbtCompound) {
        val boneColors = when {
            nbt.contains("BoneColors") -> {
                val saved = nbt.getCompound("BoneColors").copy()
                if (!saved.contains("frame")) saved.putInt("frame", defaultColor)
                if (!saved.contains("headlight_mount")) saved.putInt("headlight_mount", defaultColor)
                saved
            }
            nbt.contains("BikeColor") -> {
                val legacy = NbtCompound()
                val c = nbt.getInt("BikeColor")
                legacy.putInt("frame", c)
                legacy.putInt("headlight_mount", c)
                legacy
            }
            else -> {
                val defaults = NbtCompound()
                defaults.putInt("frame", defaultColor)
                defaults.putInt("headlight_mount", defaultColor)
                defaults
            }
        }
        dataTracker.set(BONE_COLORS, boneColors)
        dataTracker.set(HEADLIGHT_ON, nbt.getBoolean("HeadlightOn"))
        dataTracker.set(BELL_TYPE, nbt.getInt("BellType").coerceIn(0, SoundRegistry.BELL_COUNT - 1))
        currentSpeed = nbt.getDouble("CurrentSpeed")
        yaw          = nbt.getFloat("BikeYaw")
        headYaw      = yaw
    }

    override fun writeCustomDataToNbt(nbt: NbtCompound) {
        nbt.put("BoneColors", dataTracker.get(BONE_COLORS).copy())
        nbt.putBoolean("HeadlightOn", headlightOn)
        nbt.putInt("BellType", dataTracker.get(BELL_TYPE))
        nbt.putDouble("CurrentSpeed", currentSpeed)
        nbt.putFloat("BikeYaw", yaw)
    }

    private var headlightAnchor: HeadlightAnchorEntity? = null

    private fun tickHeadlightAnchor() {
        if (world.isClient) return
        val sw = world as ServerWorld

        if (headlightOn) {
            if (headlightAnchor == null || headlightAnchor!!.isRemoved) {
                val anchor = HeadlightAnchorEntity(CebikesEntities.HEADLIGHT_ANCHOR, world)
                updateAnchorPosition(anchor)
                sw.spawnEntity(anchor)
                headlightAnchor = anchor
                dataTracker.set(HEADLIGHT_ANCHOR_ID, anchor.id)
            } else {
                updateAnchorPosition(headlightAnchor!!)
            }
        } else {
            headlightAnchor?.remove(Entity.RemovalReason.DISCARDED)
            headlightAnchor = null
            dataTracker.set(HEADLIGHT_ANCHOR_ID, -1)
        }
    }

    private fun updateAnchorPosition(anchor: HeadlightAnchorEntity) {
        val yawRad = yaw * (PI / 180.0)
        val fwd = Vec3d(-sin(yawRad), 0.0, cos(yawRad))
        anchor.setPosition(
            x + fwd.x * HeadlightAnchorEntity.FORWARD_OFFSET.z,
            y + HeadlightAnchorEntity.FORWARD_OFFSET.y,
            z + fwd.z * HeadlightAnchorEntity.FORWARD_OFFSET.z
        )
    }

    override fun canHit() = !isRemoved

    override fun interact(player: PlayerEntity, hand: Hand): ActionResult {
        val stack = player.getStackInHand(hand)

        if (player.isSneaking && stack.item is CebikesItems.MultibrushItem) {
            if (!world.isClient) {
                ServerPlayNetworking.send(
                    player as ServerPlayerEntity,
                    OpenBikeCustomizerPayload(this.id)
                )
            }
            return ActionResult.SUCCESS
        }

        if (stack.item is ShearsItem && player.isSneaking) {
            if (!world.isClient) {
                applyDefaultColors()
                stack.damage(1, player, LivingEntity.getSlotForHand(hand))
            }
            return ActionResult.SUCCESS
        }

        val dye = stack.item as? DyeItem
        if (dye != null && player.isSneaking) {
            if (!world.isClient) {
                val rgb = DYE_TO_RGB[dye.color] ?: defaultColor
                val colors = dataTracker.get(BONE_COLORS).copy()
                colors.putInt("frame", rgb)
                colors.putInt("headlight_mount", rgb)
                dataTracker.set(BONE_COLORS, colors)
                if (!player.abilities.creativeMode) stack.decrement(1)
            }
            return ActionResult.SUCCESS
        }

        if (!hasPassengers() && !player.isSneaking) {
            if (!world.isClient) {
                player.startRiding(this)
                playSound(SoundRegistry.getBellSound(getBellType()), 0.3f, 1.0f)
            }
            return ActionResult.SUCCESS
        }

        return ActionResult.PASS
    }

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

    override fun remove(reason: RemovalReason) {
        if (!world.isClient) {
            headlightAnchor?.remove(RemovalReason.DISCARDED)
            headlightAnchor = null
        }
        super.remove(reason)
    }

    open fun passengerOffset(): Vec3d = Vec3d(0.0, 1.60, -0.55)

    override fun getPassengerRidingPos(passenger: Entity): Vec3d {
        val off = passengerOffset()
        val yawRad = (yaw * (PI / 180.0)).toFloat()
        val rotated = Vec3d(off.x, 0.0, off.z).rotateY(-yawRad)
        return Vec3d(x + rotated.x, y + off.y, z + rotated.z)
    }

    override fun canAddPassenger(passenger: Entity) = passengerList.isEmpty()

    override fun updatePassengerForDismount(passenger: LivingEntity): Vec3d {
        val yawRad  = yaw * (PI / 180.0)
        val rightX  = cos(yawRad)
        val rightZ  = sin(yawRad)
        val offset  = (width / 2.0 + passenger.width / 2.0 + 0.1)

        for (sign in doubleArrayOf(1.0, -1.0)) {
            val tx = x + rightX * sign * offset
            val tz = z + rightZ * sign * offset
            for (dy in doubleArrayOf(0.0, -0.5, -1.0)) {
                val landY = y + dy
                val passengerBox = Box.of(Vec3d(tx, landY + passenger.height / 2.0, tz),
                                         passenger.width.toDouble(), passenger.height.toDouble(), passenger.width.toDouble())
                if (world.isSpaceEmpty(passenger, passengerBox)) {
                    return Vec3d(tx, landY, tz)
                }
            }
        }
        return Vec3d(x + sin(yawRad) * offset, y, z - cos(yawRad) * offset)
    }

    override fun getControllingPassenger(): LivingEntity? =
        firstPassenger as? LivingEntity

    override fun isLogicalSideForUpdatingMovement(): Boolean = !world.isClient

    override fun getBodyYaw(): Float = yaw

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

    fun updateInput(
        forward: Boolean, backward: Boolean,
        steerLeft: Boolean, steerRight: Boolean,
        jumpStrength: Int, brake: Boolean
    ) {
        pressingForward    = forward
        pressingBackward   = backward
        pressingSteerLeft  = steerLeft
        pressingSteerRight = steerRight
        pressingBrake      = brake
        if (jumpStrength > 0) onJumpInput(jumpStrength)
    }

    open fun onJumpInput(strength: Int) {}

    override fun removePassenger(passenger: Entity) {
        super.removePassenger(passenger)
        pressingForward    = false
        pressingBackward   = false
        pressingSteerLeft  = false
        pressingSteerRight = false
        pressingBrake      = false
        steerDirection     = 0
        steerDuration      = 0
        lastNonZeroSteerDirection = 0
        bikeYawInitialized = false
    }

    override fun tick() {
        super.tick()
        tickHeadlightAnchor()

        if (world.isClient) {
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

            val yawRad = yaw * (PI / 180.0).toFloat()
            val fwd = Vec3d(-sin(yawRad.toDouble()), 0.0, cos(yawRad.toDouble()))
            val signedSpeed = -velocity.dotProduct(fwd).toFloat()
            wheelRotation = (wheelRotation + signedSpeed * (2f * PI.toFloat() / 2.5f)) % (2f * PI.toFloat())

            val anchorId = dataTracker.get(HEADLIGHT_ANCHOR_ID)
            if (anchorId != -1) {
                val anchor = world.getEntityById(anchorId) as? HeadlightAnchorEntity
                anchor?.setPosition(
                    x + fwd.x * HeadlightAnchorEntity.FORWARD_OFFSET.z,
                    y + HeadlightAnchorEntity.FORWARD_OFFSET.y,
                    z + fwd.z * HeadlightAnchorEntity.FORWARD_OFFSET.z
                )
            }
            return
        }

        if (!isOnGround) velocity = velocity.add(0.0, -0.08, 0.0)

        val rider = firstPassenger as? PlayerEntity

        var shouldSpawnBrakeParticles = false
        var brakeYawRad = 0.0

        if (rider != null) {
            handleMovement(rider)
            shouldSpawnBrakeParticles = pressingBrake && abs(currentSpeed) > 0.06
            brakeYawRad = yaw * (PI / 180.0)
        } else {
            currentSpeed *= 0.85
            if (abs(currentSpeed) < 0.001) currentSpeed = 0.0
            velocity = Vec3d(velocity.x * 0.85, velocity.y, velocity.z * 0.85)
        }

        move(MovementType.SELF, velocity)
        velocityModified = true

        if (shouldSpawnBrakeParticles && world is ServerWorld) {
            spawnBrakeParticles(world as ServerWorld, brakeYawRad)
        }
    }

    private fun handleMovement(rider: PlayerEntity) {
        if (!bikeYawInitialized) {
            yaw    = rider.yaw
            headYaw = yaw
            prevYaw = yaw
            bikeYawInitialized = true
        }

        val rawSteer = when {
            pressingSteerLeft  && !pressingSteerRight -> -1
            pressingSteerRight && !pressingSteerLeft  ->  1
            else                                      ->  0
        }

        if (rawSteer != 0 && rawSteer == steerDirection) {
            steerDuration++
        } else {
            steerDirection = rawSteer
            steerDuration  = if (rawSteer != 0) 1 else 0
        }

        if (rawSteer != 0) lastNonZeroSteerDirection = rawSteer

        val driftProgress = if (steerDuration > DRIFT_THRESHOLD)
            min(1f, (steerDuration - DRIFT_THRESHOLD).toFloat() / DRIFT_RAMP_TICKS)
        else 0f
        dataTracker.set(DRIFT_PROGRESS, driftProgress)

        if (rawSteer != 0 && abs(currentSpeed) > 0.01) {
            val turnRate = baseTurnRate + (maxDriftTurnRate - baseTurnRate) * driftProgress
            val speedSign = if (currentSpeed >= 0) 1f else -1f
            yaw     += rawSteer * turnRate * speedSign
            headYaw  = yaw
            prevYaw  = yaw

            if (driftProgress > 0.1f && world is ServerWorld) {
                spawnDriftParticles(world as ServerWorld)
            }
        }

        val handlebarAngle = if (rawSteer != 0) {
            val driftBonus = if (abs(currentSpeed) > 0.01) 10f * driftProgress else 0f
            rawSteer * (15f + driftBonus)
        } else 0f
        dataTracker.set(STEER_ANGLE, handlebarAngle)

        val yawRad = yaw * (PI / 180.0)

        if (pressingBrake) {
            val wasMoving = abs(currentSpeed) > 0.06
            currentSpeed *= 0.86
            if (abs(currentSpeed) < 0.02) currentSpeed = 0.0

            if (wasMoving) {
                val skidDir = if (steerDirection != 0) steerDirection else lastNonZeroSteerDirection
                if (skidDir != 0) {
                    val skidRate = 7.0f * (abs(currentSpeed) / maxForwardSpeed).toFloat().coerceAtMost(1f)
                    yaw     += skidDir * skidRate
                    headYaw  = yaw
                    prevYaw  = yaw
                }
                if (!wasBraking) playSound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.4f, 0.8f + random.nextFloat() * 0.4f)
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

        if (isOnGround) currentSpeed *= 0.97
        velocity = Vec3d(-sin(yawRad) * currentSpeed, velocity.y, cos(yawRad) * currentSpeed)
    }

    protected fun particleEffect(colorKey: String, fallback: ParticleEffect, scale: Float = 1.5f): ParticleEffect {
        val boneColors = dataTracker.get(BONE_COLORS)
        if (!boneColors.contains(colorKey)) return fallback
        val rgb = boneColors.getInt(colorKey)
        return DustParticleEffect(Vector3f(
            ((rgb shr 16) and 0xFF) / 255f,
            ((rgb shr 8)  and 0xFF) / 255f,
            (rgb          and 0xFF) / 255f
        ), scale)
    }

    /*
    protected fun smokyParticleEffect(colorKey: String, fallback: ParticleEffect, scale: Float = 2.0f): ParticleEffect {
        val boneColors = dataTracker.get(BONE_COLORS)
        if (!boneColors.contains(colorKey)) return fallback
        val rgb = boneColors.getInt(colorKey)
        val from = Vector3f(
            ((rgb shr 16) and 0xFF) / 255f,
            ((rgb shr 8)  and 0xFF) / 255f,
            (rgb          and 0xFF) / 255f
        )
        val to = Vector3f(0.55f, 0.55f, 0.55f)   // fades out to smoke grey
        return DustColorTransitionParticleEffect(from, to, scale)
    }*/

    protected fun smokyParticleEffect(colorKey: String, fallback: ParticleEffect, scale: Float = 2.0f): ParticleEffect {
        val boneColors = dataTracker.get(BONE_COLORS)
        if (!boneColors.contains(colorKey)) return fallback
        val rgb = boneColors.getInt(colorKey)
        return BrakeSmokeParticleEffect(
            fromR = ((rgb shr 16) and 0xFF) / 255f,
            fromG = ((rgb shr 8) and 0xFF) / 255f,
            fromB = (rgb and 0xFF) / 255f,
            toR = 0.55f,
            toG = 0.55f,
            toB = 0.55f,
            scale = scale
        )
    }

    protected fun dustyParticleEffect(colorKey: String, fallback: ParticleEffect, scale: Float = 1.5f): ParticleEffect {
        val boneColors = dataTracker.get(BONE_COLORS)
        if (!boneColors.contains(colorKey)) return fallback
        val rgb = boneColors.getInt(colorKey)
        val from = Vector3f(
            ((rgb shr 16) and 0xFF) / 255f,
            ((rgb shr 8)  and 0xFF) / 255f,
            (rgb          and 0xFF) / 255f
        )
        val to = Vector3f(0.55f, 0.55f, 0.55f)
        return DustColorTransitionParticleEffect(from, to, scale)
    }

    private fun spawnBrakeParticles(world: ServerWorld, yawRad: Double) {
        val effect = smokyParticleEffect("particle_brake", ParticleTypes.CAMPFIRE_COSY_SMOKE, 1.0f)
        val fwd = Vec3d(-sin(yawRad), 0.0, cos(yawRad))
        val compensationFactor = -(2.0 + (abs(currentSpeed) / maxForwardSpeed) * 2.0)
        val velOffset = velocity.multiply(compensationFactor)
        for (sign in listOf(0.7, -0.7)) {
            val wx = x + fwd.x * sign + velOffset.x
            val wz = z + fwd.z * sign + velOffset.z
            repeat(2) {
                world.spawnParticles(
                    effect,
                    wx + (random.nextDouble() - 0.5) * 0.3, y + 0.07,
                    wz + (random.nextDouble() - 0.5) * 0.3,
                    1, 0.05, 0.02, 0.05, 0.01
                )
            }
        }
    }

    private fun spawnDriftParticles(world: ServerWorld) {
        val effect = dustyParticleEffect("particle_drift", ParticleTypes.SMOKE, 1.5f)
        val yawRad = yaw * (PI / 180.0)
        val fwd = Vec3d(-sin(yawRad), 0.0, cos(yawRad))
        val velOffset = velocity.multiply(-2.0)

        world.spawnParticles(
            effect,
            x - fwd.x * 0.6 + (random.nextDouble() - 0.5) * 0.5 + velOffset.x,
            y + 0.05,
            z - fwd.z * 0.6 + (random.nextDouble() - 0.5) * 0.5 + velOffset.z,
            1, 0.02, 0.02, 0.02, 0.005
        )
    }

    override fun isPushable()   = false
    override fun isCollidable() = !isRemoved
}
