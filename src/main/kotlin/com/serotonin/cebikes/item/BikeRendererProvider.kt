package com.serotonin.cebikes.item

import net.minecraft.util.Identifier

fun interface BikeRendererProvider {
    fun createRenderer(modelId: Identifier, textureId: Identifier, animId: Identifier): Any
}