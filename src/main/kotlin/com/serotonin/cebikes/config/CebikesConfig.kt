package com.serotonin.cebikes.config


import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.io.File

object CebikesConfig {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configFile = File(FabricLoader.getInstance().configDir.toFile(), "cebikes.json")

    var visibleBikeRack: Boolean = true

    fun register() {
        if (!configFile.exists()) {
            save()
            return
        }
        try {
            val data = gson.fromJson(configFile.readText(), ConfigData::class.java)
            visibleBikeRack = data?.visibleBikeRack ?: true
        } catch (e: Exception) {
            println("[cebikes] Failed to load config, using defaults: ${e.message}")
        }
    }

    fun save() {
        configFile.writeText(gson.toJson(ConfigData(visibleBikeRack)))
    }

    private data class ConfigData(val visibleBikeRack: Boolean = true)
}