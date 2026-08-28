package com.serotonin.cebikes.client.particle

import com.serotonin.cebikes.particle.BrakeSmokeParticleEffect
import net.minecraft.client.particle.ParticleTextureSheet
import net.minecraft.client.particle.SpriteBillboardParticle
import net.minecraft.client.particle.SpriteProvider
import net.minecraft.client.world.ClientWorld

class BrakeSmokeParticle(
    world: ClientWorld,
    x: Double, y: Double, z: Double,
    vx: Double, vy: Double, vz: Double,
    private val effect: BrakeSmokeParticleEffect,
    sprites: SpriteProvider
) : SpriteBillboardParticle(world, x, y, z, vx, vy, vz) {

    init {
        setSprite(sprites.getSprite(random))
        red  = effect.fromR
        green = effect.fromG
        blue  = effect.fromB
        alpha = 0.9f
        scale      = effect.scale * (0.25f + random.nextFloat() * 0.3f)
        maxAge     = 35 + random.nextInt(20)
        this.velocityX = vx
        this.velocityY = 0.02 + random.nextDouble() * 0.02
        this.velocityZ = vz
        gravityStrength = -0.015f
    }

    override fun getType(): ParticleTextureSheet = ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT

    override fun tick() {
        super.tick()
        val t = age.toFloat() / maxAge.toFloat()
        red   = lerp(t, effect.fromR, effect.toR)
        green = lerp(t, effect.fromG, effect.toG)
        blue  = lerp(t, effect.fromB, effect.toB)
        alpha = (1f - t) * 0.85f
        scale     *= 1.012f
    }

    override fun getBrightness(tint: Float) = 0xF000F0

    private fun lerp(t: Float, a: Float, b: Float) = a + t * (b - a)
}