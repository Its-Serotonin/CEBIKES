package com.serotonin.cebikes.item

import net.minecraft.block.Block
import net.minecraft.item.BlockItem
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.client.GeoRenderProvider
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.function.Consumer

class BikeRackItem(block: Block, settings: Settings) : BlockItem(block, settings), GeoItem {

    private val cache = GeckoLibUtil.createInstanceCache(this)

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = cache

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {}

    override fun createGeoRenderer(consumer: Consumer<GeoRenderProvider>) {
        consumer.accept(object : GeoRenderProvider {
            private var renderer: software.bernie.geckolib.renderer.GeoItemRenderer<*>? = null
            override fun getGeoItemRenderer() = renderer ?: rendererProvider!!().also { renderer = it }
        })
    }

    companion object {
        var rendererProvider: (() -> software.bernie.geckolib.renderer.GeoItemRenderer<*>)? = null
    }
}