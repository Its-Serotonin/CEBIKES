package com.serotonin.cebikes.block

import net.minecraft.util.StringIdentifiable

enum class BikeRackPart : StringIdentifiable {
    MAIN, EXTENSION;

    override fun asString() = name.lowercase()
}