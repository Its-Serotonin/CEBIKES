package com.serotonin.cebikes.client

import com.serotonin.cebikes.entity.HeadlightAnchorEntity
import com.serotonin.cebikes.registry.CebikesEntities
import dev.lambdaurora.lambdynlights.api.DynamicLightHandler
import dev.lambdaurora.lambdynlights.api.DynamicLightHandlers
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager

class CebikesDynamicLightsInitializer : DynamicLightsInitializer {
    override fun onInitializeDynamicLights(itemLightSourceManager: ItemLightSourceManager) {
        DynamicLightHandlers.registerDynamicLightHandler(
            CebikesEntities.HEADLIGHT_ANCHOR,
            DynamicLightHandler.makeHandler<HeadlightAnchorEntity>(
                { _ -> 15 },
                { _ -> false }
            )
        )
    }
}