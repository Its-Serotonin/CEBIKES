package com.serotonin.cebikes.client.input

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

object CebikesKeyBindings {

    const val CATEGORY = "category.cebikes"

    val BRAKE: KeyBinding = KeyBindingHelper.registerKeyBinding(
        KeyBinding(
            "key.cebikes.brake",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL,
            CATEGORY
        )
    )

    val HEADLIGHT: KeyBinding = KeyBindingHelper.registerKeyBinding(
        KeyBinding(
            "key.cebikes.headlight",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CATEGORY
        )
    )

    fun register() { /* triggers static initialisation */ }
}