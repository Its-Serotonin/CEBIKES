package com.serotonin.cebikes.client.gui

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.minecraft.client.MinecraftClient
import net.minecraft.nbt.NbtCompound
import java.io.File

data class BikePreset(
    val name: String,
    val colors: Map<String, Int>,
    val bellType: Int
)

object BikePresetManager {

    const val MAX_PRESETS = 15
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private fun presetsFile(): File =
        File(MinecraftClient.getInstance().runDirectory, "config/cebikes_presets.json")

    fun load(): MutableList<BikePreset> {
        val file = presetsFile()
        if (!file.exists()) return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<BikePreset>>() {}.type
            gson.fromJson(file.readText(), type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun persist(presets: List<BikePreset>) {
        val file = presetsFile()
        file.parentFile?.mkdirs()
        file.writeText(gson.toJson(presets))
    }

    fun savePreset(name: String, colors: NbtCompound, bellType: Int) {
        val presets = load()
        val colorMap = buildMap<String, Int> { for (key in colors.keys) put(key, colors.getInt(key)) }
        val trimmed = name.trim()
        val preset = BikePreset(trimmed, colorMap, bellType)
        val idx = presets.indexOfFirst { it.name.equals(trimmed, ignoreCase = true) }
        if (idx >= 0) presets[idx] = preset
        else {
            if (presets.size >= MAX_PRESETS) presets.removeAt(0)
            presets.add(preset)
        }
        persist(presets)
    }

    fun deletePreset(name: String) {
        val presets = load()
        presets.removeIf { it.name == name }
        persist(presets)
    }
}