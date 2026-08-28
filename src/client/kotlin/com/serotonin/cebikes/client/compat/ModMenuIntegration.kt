package com.serotonin.cebikes.client.compat

import com.serotonin.cebikes.config.CebikesConfig
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

class ModMenuIntegration : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent -> buildConfigScreen(parent) }
    }

    private fun buildConfigScreen(parent: Screen): Screen {
        val builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.literal("CEBikes Config"))
            .setSavingRunnable { CebikesConfig.save() }

        val category = builder.getOrCreateCategory(Text.literal("Bike Rack"))
        val entryBuilder = builder.entryBuilder()

        category.addEntry(
            entryBuilder.startBooleanToggle(
                Text.literal("Show wall bike racks"),
                CebikesConfig.visibleBikeRack
            )
                .setDefaultValue(true)
                .setTooltip(Text.literal("Toggles visibility for the wall mounted bike racks"))
                .setSaveConsumer { newValue -> CebikesConfig.visibleBikeRack = newValue }
                .build()
        )

        return builder.build()
    }
}