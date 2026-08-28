package com.serotonin.cebikes.client.gui

import com.mojang.blaze3d.systems.RenderSystem
import com.serotonin.cebikes.entity.AbstractBikeEntity
import com.serotonin.cebikes.entity.AcroBikeEntity
import com.serotonin.cebikes.network.BikeCustomizePayload
import com.serotonin.cebikes.registry.SoundRegistry
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.render.DiffuseLighting
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.RotationAxis
import kotlin.math.*

class BikeCustomizerScreen(
    private val targetEntity: AbstractBikeEntity
) : Screen(Text.translatable("gui.cebikes.customizer.title")) {

    // ── Layout constants ────────────────────────────────────────────────────
    companion object {
        private const val GUI_WIDTH  = 349
        private const val GUI_HEIGHT = 205

        private val BACKGROUND_TEXTURE = Identifier.of(
            "cebikes", "textures/gui/bikecustomizer/customizer_base.png"
        )
        private val BUTTON_TEXTURE        = Identifier.of(
            "cebikes", "textures/gui/bikecustomizer/button.png"
        )
        private val BUTTON_HOVERED_TEXTURE = Identifier.of(
            "cebikes", "textures/gui/bikecustomizer/button_hovered.png"
        )
        private val DEL_BUTTON_TEXTURE = Identifier.of(
            "cebikes", "textures/gui/bikecustomizer/delbutton.png"
        )
        private val DEL_BUTTON_HOVERED_TEXTURE = Identifier.of(
            "cebikes", "textures/gui/bikecustomizer/delbutton_hovered.png"
        )

        // 9-slice button texture dimensions — adjust BUTTON_CORNER if borders look wrong in-game
        private const val BUTTON_TEX_W  = 48
        private const val BUTTON_TEX_H  = 14
        private const val BUTTON_CORNER = 2

        // 3-D model preview area (left panel)
        private const val MODEL_X = 0
        private const val MODEL_Y = 7 // moves top crop up/down , lower = up
        private const val MODEL_W = 181
        private const val MODEL_H = 135 // crops the bottom/makes the window taller, lower = more crop
        private const val MODEL_CENTER_X = MODEL_X + MODEL_W / 2 // 90
       // private const val MODEL_CENTER_Y = MODEL_Y + MODEL_H / 2 + 60    // 102
        private const val MODEL_CENTER_Y = 120
        // Bone-selector list (top-right)
        private const val BONE_LIST_X = 2
        private const val BONE_LIST_Y = 151
        private const val BONE_LIST_W = 182
        private const val BONE_LIST_H = 52
        private const val BONE_ENTRY_H = 13
        private val VISIBLE_BONES = BONE_LIST_H / BONE_ENTRY_H  // 4

        // Square HSV picker (Hue on X, Brightness on Y)
        private const val SQUARE_X = 233
        private const val SQUARE_Y = 23
        private const val SQUARE_W = 96
        private const val SQUARE_H = 96

        // Brightness (Value) slider — vertical, right of square
        private const val V_SLIDER_X = SQUARE_X + SQUARE_W + 4   // 281
        private const val V_SLIDER_Y = SQUARE_Y
        private const val V_SLIDER_W = 8
        private const val V_SLIDER_H = SQUARE_H

        // Saturation slider — horizontal, below square
        private const val S_SLIDER_X = SQUARE_X
        private const val S_SLIDER_Y = SQUARE_Y + SQUARE_H + 4   // 185
        private const val S_SLIDER_W = SQUARE_W
        private const val S_SLIDER_H = 8

        // Colour preview swatch (right column, top)
        private const val PREVIEW_X = 185 // 293
        private const val PREVIEW_Y = SQUARE_Y                       // 86
        private const val PREVIEW_W = 39
        private const val PREVIEW_H = 16

        // Hex code label sits just below the swatch
        private const val HEX_Y = PREVIEW_Y + PREVIEW_H + 3         // 105

        // Buttons (right column, below swatch + hex)
        private const val BTN_X       = PREVIEW_X
        private const val BTN_W       = 39
        private const val BTN_H       = 14
        private const val DONE_Y      = HEX_Y + 12                   // 117
        private const val CANCEL_Y    = DONE_Y    + BTN_H + 4        // 135
        private const val RESET_Y     = CANCEL_Y  + BTN_H + 4        // 153
        private const val RESET_ALL_Y = RESET_Y   + BTN_H + 4        // 171

        // Preset buttons (below the 3D model panel)
        private const val PRESET_BTN_W  = 46
        private const val PRESET_BTN_H  = BTN_H
        private const val SAVE_PRESET_X = 232
        private const val LOAD_PRESET_X = SAVE_PRESET_X + PRESET_BTN_W + 6  // 94
        private const val PRESET_BTN_Y  = MODEL_Y + MODEL_H - 4           // 146

        // Preset overlays — anchored in the right panel so they never overlap the 3D model
        private const val OVERLAY_X    = BONE_LIST_X   // 182, relative to guiLeft
        private const val OVERLAY_W    = 163           // fits the 167-px right panel
        private const val SAVE_OL_Y    = 10            // relative to guiTop
        private const val SAVE_OL_H    = 78
        private const val SAVE_FIELD_Y = 14
        private const val SAVE_FIELD_H = 14
        private const val SAVE_BTN_Y   = 54
        private const val SAVE_BTN_W   = 75            // two 75-px buttons fit with 5-px gap
        private const val LOAD_OL_Y    = 5             // relative to guiTop
        private const val LOAD_OL_H    = 187           // fills most of the right panel
        private const val LOAD_HDR_H   = 14
        private const val LOAD_ROW_H   = 13
        private const val LOAD_VISIBLE = 12

        // ── Bone groups ────────────────────────────────────────────────────
        data class BoneGroup(
            val key: String,
            val displayName: String,
            val bones: List<Pair<String, String>>
        )

        val BONE_GROUPS = listOf(
            BoneGroup("body",    "Body",    listOf(
                "frame"    to "Frame",
            )),
            BoneGroup("seat",    "Seat",    listOf(
                "seat_top" to "Seat"
            )),
            BoneGroup("brakes",  "Brakes",  listOf(
                "rear_brakes"  to "Rear Brakes",
                "front_brakes" to "Front Brakes"
            )),
            BoneGroup("wheels",  "Wheels",  listOf(
                "back_wheel"   to "Back Wheel",
                "back_spokes"  to "Back Spokes",
                "front_wheel"  to "Front Wheel",
                "front_spokes" to "Front Spokes"
            )),
            BoneGroup("handles", "Handles", listOf(
                "handle"       to "Handlebar",
                "left_handle"  to "Left Grip",
                "right_handle" to "Right Grip"
            )),
            BoneGroup("pedals",  "Pedals",  listOf(
                "left_pedal"   to "Left Pedal",
                "right_pedal"  to "Right Pedal"
            )),
            BoneGroup("headlight",  "Headlight",  listOf(
                "headlight"             to "Headlight",
                "headlight_mount"       to "Headlight Mount"
                //"headlight_light_color" to "Headlight Glow"
                //temporarily commenting this out for now
            )),
        )

        // ── Colour math ────────────────────────────────────────────────────

        /** HSV → packed RGB (no alpha). */
        fun hsvToRgb(h: Float, s: Float, v: Float): Int {
            if (s == 0f) {
                val g = (v * 255f).toInt().coerceIn(0, 255)
                return (g shl 16) or (g shl 8) or g
            }
            val h6 = (h / 60f).let { if (it < 0f) it + 6f else it }
            val i  = h6.toInt() % 6
            val f  = h6 - i
            val p  = v * (1f - s)
            val q  = v * (1f - s * f)
            val t  = v * (1f - s * (1f - f))
            val (r, g, b) = when (i) {
                0    -> Triple(v, t, p)
                1    -> Triple(q, v, p)
                2    -> Triple(p, v, t)
                3    -> Triple(p, q, v)
                4    -> Triple(t, p, v)
                else -> Triple(v, p, q)
            }
            return ((r * 255f).toInt().coerceIn(0, 255) shl 16) or
                   ((g * 255f).toInt().coerceIn(0, 255) shl 8)  or
                   (b * 255f).toInt().coerceIn(0, 255)
        }

        /** HSV → ARGB (fully opaque). */
        private fun hsvToArgb(h: Float, s: Float, v: Float) = (0xFF shl 24) or hsvToRgb(h, s, v)

        /** Packed RGB → (hue 0–360, saturation 0–1, value 0–1). */
        fun rgbToHsv(rgb: Int): Triple<Float, Float, Float> {
            val r = ((rgb shr 16) and 0xFF) / 255f
            val g = ((rgb shr 8)  and 0xFF) / 255f
            val b = (rgb          and 0xFF) / 255f
            val mx = maxOf(r, g, b)
            val mn = minOf(r, g, b)
            val d  = mx - mn
            val v  = mx
            val s  = if (mx == 0f) 0f else d / mx
            val h  = when {
                d == 0f -> 0f
                mx == r -> 60f * (((g - b) / d) % 6f).let { if (it < 0f) it + 6f else it }
                mx == g -> 60f * ((b - r) / d + 2f)
                else    -> 60f * ((r - g) / d + 4f)
            }
            return Triple(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
        }
    }

    // ── Screen state ───────────────────────────────────────────────────────
    private var guiLeft = 0
    private var guiTop  = 0

    // 3-D model view controls
    var modelYaw   = 53f
    var modelPitch = 27f
    var modelScale = 50f

    // HSV colour state for the picker
    var currentHue        = 0f
    var currentSaturation = 1f
    var currentValue      = 1f

    // Selected item: either a bone name ("frame") or "group:<key>" ("group:wheels")
    var selectedBone: String? = "group:${BONE_GROUPS.firstOrNull()?.key}"
    var boneListScroll = 0

    private val expandedGroups = mutableSetOf<String>()

    // Full bone group list for this specific bike — adds Particles and Bells groups
    private val effectiveBoneGroups: List<BoneGroup> = run {
        val particleBones = mutableListOf(
            "particle_brake" to "Brake Particles",
            "particle_drift" to "Drift Particles"
        )
        if (targetEntity is AcroBikeEntity) particleBones.add("particle_jump" to "Jump Particles")
        val groups = BONE_GROUPS.toMutableList()
        if (targetEntity is AcroBikeEntity) {
            val seatIndex = groups.indexOfFirst { it.key == "seat" }
            groups.add(seatIndex + 1, BoneGroup("connectors", "Connectors", listOf(
                "seat_connector"  to "Seat Connector",
                "frame_connector" to "Frame Connector"
            )))
        }
        groups +
            BoneGroup("particles", "Particles", particleBones) +
            BoneGroup("bells", "Bells", listOf("bell_type" to "Bell Type"))
    }

    // Bell type selection state (index into SoundRegistry.getBells())
    private var workingBellType: Int = targetEntity.getBellType()

    // Per-bone colour state
    private val originalColors: NbtCompound =
        targetEntity.dataTracker.get(AbstractBikeEntity.BONE_COLORS).copy()
    private val workingColors: NbtCompound = originalColors.copy()

    private enum class DragTarget { NONE, MODEL, COLOR_SQUARE, V_SLIDER, S_SLIDER }
    private var activeDrag = DragTarget.NONE

    private var confirmed  = false   // Done was pressed — payload already sent
    private var cancelled  = false   // Cancel was pressed — revert on close
    private var firstInit  = true

    private enum class OverlayMode { NONE, SAVE, LOAD }
    private var overlayMode = OverlayMode.NONE
    private var saveNameInput = ""
    private var saveNameError = ""
    private var loadedPresets: MutableList<BikePreset> = mutableListOf()
    private var presetListScroll = 0

    // ── Flat list builder ──────────────────────────────────────────────────

    private sealed class BoneListItem {
        data class GroupHeader(val group: BoneGroup) : BoneListItem()
        data class BoneEntry(val group: BoneGroup, val bone: Pair<String, String>) : BoneListItem()
    }

    private fun buildFlatList(): List<BoneListItem> {
        val items = mutableListOf<BoneListItem>()
        for (group in effectiveBoneGroups) {
            items.add(BoneListItem.GroupHeader(group))
            if (group.key in expandedGroups) {
                for (bone in group.bones) items.add(BoneListItem.BoneEntry(group, bone))
            }
        }
        return items
    }

    private fun clampScroll() {
        val maxScroll = maxOf(0, buildFlatList().size - VISIBLE_BONES)
        boneListScroll = boneListScroll.coerceIn(0, maxScroll)
    }

    // ── Screen lifecycle ───────────────────────────────────────────────────

    override fun init() {
        guiLeft = (width  - GUI_WIDTH)  / 2
        guiTop  = (height - GUI_HEIGHT) / 2
        updateHsvFromSelectedBone()
        currentSaturation = 1f  // always open at full saturation and brightness
        currentValue = 1f
        if (firstInit) {
            firstInit = false
            playOpenSound()
        }
    }

    override fun tick() {
        if (targetEntity.isRemoved) {
            confirmed = true
            close()
        }
    }

    override fun shouldPause() = false

    override fun close() {
        when {
            confirmed -> { /* Done already sent the payload — nothing more to do */ }
            cancelled -> targetEntity.dataTracker.set(AbstractBikeEntity.BONE_COLORS, originalColors)
            else -> {
                // Closed without Done or Cancel (Escape, E, entity removed) — auto-save like Done
                confirmed = true
                ClientPlayNetworking.send(BikeCustomizePayload(targetEntity.id, workingColors.copy(), workingBellType))
            }
        }
        super.close()
    }

    // ── Rendering ─────────────────────────────────────────────────────────

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)

        context.drawTexture(
            BACKGROUND_TEXTURE,
            guiLeft, guiTop,
            0f, 0f,
            GUI_WIDTH, GUI_HEIGHT,
            GUI_WIDTH, GUI_HEIGHT
        )

        renderModel(context, delta)
        renderParticlePreview(context)
        renderBoneList(context)
        renderColorSquare(context)
        renderVSlider(context)
        renderSSlider(context)
        renderColorPreview(context)
        renderColorCursor(context)

        // All four right-column buttons share the scale that "Reset All" auto-resolves to,
        // so they look uniform rather than "Done"/"Cancel"/"Reset" being visibly larger.
        val rightBtnScale = textRenderer.getWidth("Reset All").let { rw ->
            if (rw > BTN_W - 4) (BTN_W - 4f) / rw else 1f
        }
        renderButton(context, guiLeft + BTN_X, guiTop + DONE_Y,      BTN_W, BTN_H, "Done",      mouseX, mouseY, rightBtnScale)
        renderButton(context, guiLeft + BTN_X, guiTop + CANCEL_Y,    BTN_W, BTN_H, "Cancel",    mouseX, mouseY, rightBtnScale)
        renderButton(context, guiLeft + BTN_X, guiTop + RESET_Y,     BTN_W, BTN_H, "Reset",     mouseX, mouseY, rightBtnScale)
        renderButton(context, guiLeft + BTN_X, guiTop + RESET_ALL_Y, BTN_W, BTN_H, "Reset All", mouseX, mouseY, rightBtnScale)

        renderButton(context, guiLeft + SAVE_PRESET_X, guiTop + PRESET_BTN_Y, PRESET_BTN_W, PRESET_BTN_H, "Save Preset", mouseX, mouseY)
        renderButton(context, guiLeft + LOAD_PRESET_X, guiTop + PRESET_BTN_Y, PRESET_BTN_W, PRESET_BTN_H, "Load Preset", mouseX, mouseY)

        //context.drawText(textRenderer, "Parts", guiLeft + BONE_LIST_X + 2, guiTop + 5, 0xFFFFFF, true)

        if (overlayMode != OverlayMode.NONE) renderOverlay(context, mouseX, mouseY)
    }

    private fun renderModel(context: DrawContext, delta: Float) {
        if (targetEntity.isRemoved) return

        // Force headlight on when editing the headlight glow or the full lights group
        val showHeadlight = selectedBone == "headlight_light_color" || selectedBone == "group:headlight"
        val savedHeadlight = targetEntity.dataTracker.get(AbstractBikeEntity.HEADLIGHT_ON)
        if (showHeadlight) targetEntity.dataTracker.set(AbstractBikeEntity.HEADLIGHT_ON, true)

        // Tell BikeGeoModel to use rest pose
        targetEntity.isGuiRendering = true

        val mc   = MinecraftClient.getInstance()
        val absX = guiLeft + MODEL_CENTER_X
        val absY = guiTop  + MODEL_CENTER_Y

        context.enableScissor(
            guiLeft + MODEL_X, guiTop + MODEL_Y,
            guiLeft + MODEL_X + MODEL_W, guiTop + MODEL_Y + MODEL_H
        )

        val matrices = context.matrices
        matrices.push()
        matrices.translate(absX.toFloat(), absY.toFloat(), 100f)
        matrices.scale(modelScale, -modelScale, modelScale)
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(modelPitch))

        val savedYaw     = targetEntity.yaw
        val savedPrevYaw = targetEntity.prevYaw
        targetEntity.yaw     = modelYaw
        targetEntity.prevYaw = modelYaw

        DiffuseLighting.disableGuiDepthLighting()
        val dispatcher = mc.entityRenderDispatcher
        dispatcher.setRenderShadows(false)
        val immediate = mc.bufferBuilders.entityVertexConsumers
        RenderSystem.enableDepthTest()
        RenderSystem.runAsFancy {
            dispatcher.render(
                targetEntity,
                0.0, 0.0, 0.0,
                modelYaw, delta,
                matrices, immediate,
                LightmapTextureManager.MAX_LIGHT_COORDINATE
            )
        }
        immediate.draw()
        DiffuseLighting.enableGuiDepthLighting()
        dispatcher.setRenderShadows(true)

        targetEntity.yaw     = savedYaw
        targetEntity.prevYaw = savedPrevYaw
        matrices.pop()
        context.disableScissor()

        // Restore entity flags
        targetEntity.isGuiRendering = false
        if (showHeadlight) targetEntity.dataTracker.set(AbstractBikeEntity.HEADLIGHT_ON, savedHeadlight)
    }

    /**
     * Draws an animated particle preview inside the model area when a particle bone/group
     * is selected. Particles are simple coloured squares that rise and fade, giving the
     * user a live colour preview without needing a real world to spawn particles in.
     */
    private fun renderParticlePreview(context: DrawContext) {
        val sel = selectedBone ?: return

        val colorKeys: List<String> = when {
            sel.startsWith("particle_") -> listOf(sel)
            sel == "group:particles"    -> listOfNotNull(
                "particle_brake",
                "particle_drift",
                if (targetEntity is AcroBikeEntity) "particle_jump" else null
            )
            else -> return
        }

        val cx     = guiLeft + MODEL_CENTER_X
        val floorY = guiTop  + MODEL_CENTER_Y + 26  // approximate wheel-contact level on screen

        // Wall-clock time gives a smooth animation without needing a tick counter
        val tick = (System.currentTimeMillis() / 80L % 24L).toInt()

        context.enableScissor(
            guiLeft + MODEL_X, guiTop + MODEL_Y,
            guiLeft + MODEL_X + MODEL_W, guiTop + MODEL_Y + MODEL_H
        )

        for (colorKey in colorKeys) {
            val rgb = if (workingColors.contains(colorKey)) workingColors.getInt(colorKey) else 0x888888
            when (colorKey) {
                "particle_brake" -> {
                    // Two clusters rising from each wheel contact point
                    for (side in intArrayOf(-22, 22)) {
                        for (i in 0..4) {
                            val age = (i * 5 + tick) % 24
                            val px  = cx + side + (i - 2) * 2
                            val py  = floorY - age * 2
                            val sz  = (3 - age / 8).coerceAtLeast(1)
                            val a   = (200 - age * 16).coerceIn(0, 200)
                            context.fill(px, py, px + sz, py + sz, (a shl 24) or rgb)
                        }
                    }
                }
                "particle_drift" -> {
                    // Central cloud trail rising from behind the bike
                    for (i in 0..6) {
                        val age = (i * 4 + tick) % 24
                        val px  = cx + (i - 3) * 5
                        val py  = floorY - age * 2
                        val sz  = (3 - age / 8).coerceAtLeast(1)
                        val a   = (200 - age * 16).coerceIn(0, 200)
                        context.fill(px, py, px + sz, py + sz, (a shl 24) or rgb)
                    }
                }
                "particle_jump" -> {
                    // Radial burst spreading outward from the base of the bike
                    for (i in 0..11) {
                        val age   = (i * 2 + tick) % 24
                        val angle = i * 30.0 * PI / 180.0
                        val r     = age * 3.5
                        val px    = (cx + cos(angle) * r).toInt()
                        val py    = (floorY - age * 1.5 - abs(sin(angle)) * r * 0.4).toInt()
                        val sz    = (3 - age / 8).coerceAtLeast(1)
                        val a     = (200 - age * 16).coerceIn(0, 200)
                        context.fill(px, py, px + sz, py + sz, (a shl 24) or rgb)
                    }
                }
            }
        }

        context.disableScissor()
    }

    private fun renderBoneList(context: DrawContext) {
        val absX = guiLeft + BONE_LIST_X
        val absY = guiTop  + BONE_LIST_Y

        //context.fill(absX - 1, absY - 1, absX + BONE_LIST_W + 1, absY + BONE_LIST_H + 1, 0x60000000.toInt())

        val flatList = buildFlatList()
        val maxScroll = maxOf(0, flatList.size - VISIBLE_BONES)
        if (boneListScroll > maxScroll) boneListScroll = maxScroll

        val visible = flatList.drop(boneListScroll).take(VISIBLE_BONES)
        for ((idx, item) in visible.withIndex()) {
            val ey = absY + idx * BONE_ENTRY_H
            when (item) {
                is BoneListItem.GroupHeader -> {
                    val groupSelKey = "group:${item.group.key}"
                    val isSelected  = selectedBone == groupSelKey
                    val isExpanded  = item.group.key in expandedGroups

                    // Row background
                    context.fill(absX, ey, absX + BONE_LIST_W, ey + BONE_ENTRY_H,
                        if (isSelected) (0x80 shl 24) or 0xFFFFFF else (0xFF shl 24) or 0x666666)

                    val expandArrow = if (isExpanded) "▼" else "▶"
                    if (item.group.key == "bells") {
                        // No colour swatch for the bells group — text aligns with other group headers
                        context.drawText(textRenderer, "$expandArrow ${item.group.displayName}",
                            absX + 13, ey + 2, if (isSelected) 0xFFFF88 else 0xFFFFFF, false)
                    } else {
                        // Group colour swatch (first bone that has a colour, else white)
                        val swatchRgb = item.group.bones
                            .firstOrNull { workingColors.contains(it.first) }
                            ?.let { workingColors.getInt(it.first) } ?: 0xFFFFFF
                        context.fill(absX + 2, ey + 2, absX + 10, ey + BONE_ENTRY_H - 2, (0xFF shl 24) or swatchRgb)
                        context.drawBorder(absX + 2, ey + 2, 8, BONE_ENTRY_H - 4, (0x404040 shl 24))
                        context.drawText(textRenderer, "$expandArrow ${item.group.displayName}",
                            absX + 13, ey + 2, if (isSelected) 0xFFFF88 else 0xFFFFFF, false)
                    }
                }

                is BoneListItem.BoneEntry -> {
                    val isSelected = selectedBone == item.bone.first
                    if (isSelected) {
                        context.fill(absX, ey, absX + BONE_LIST_W, ey + BONE_ENTRY_H, (0x80 shl 24) or 0xFFFFFF)
                    }

                    if (item.group.key == "bells") {
                        // Arrow selectors: ◀ on left, ▶ on right, bell name in between
                        val arrowColor = if (isSelected) 0xFFFF88 else 0xC8C8C8
                        context.drawText(textRenderer, "◀", absX + 14, ey + 2, arrowColor, false)
                        context.drawText(textRenderer, SoundRegistry.getBellName(workingBellType),
                            absX + 24, ey + 2, arrowColor, false)
                        context.drawText(textRenderer, "▶", absX + BONE_LIST_W - 18, ey + 2, arrowColor, false)
                    } else {
                        // Indented colour swatch
                        val swatch = if (workingColors.contains(item.bone.first))
                            workingColors.getInt(item.bone.first) else 0xFFFFFF
                        context.fill(absX + 14, ey + 2, absX + 22, ey + BONE_ENTRY_H - 2, (0xFF shl 24) or swatch)
                        context.drawBorder(absX + 14, ey + 2, 8, BONE_ENTRY_H - 4, (0x404040 shl 24))
                        context.drawText(textRenderer, item.bone.second,
                            absX + 26, ey + 2, if (isSelected) 0xFFFF88 else 0xDDDDDD, false)
                    }
                }
            }
        }

        // Scroll indicators
        if (boneListScroll > 0) {
            context.drawText(textRenderer, "▲", absX + BONE_LIST_W - 10, absY + 2, 0xC8C8C8, false)
        }
        if (boneListScroll < maxScroll) {
            context.drawText(textRenderer, "▼", absX + BONE_LIST_W - 10, absY + BONE_LIST_H - 9, 0xC8C8C8, false)
        }
    }

    private fun renderColorSquare(context: DrawContext) {
        val absX = guiLeft + SQUARE_X
        val absY = guiTop  + SQUARE_Y

        context.fill(absX - 1, absY - 1, absX + SQUARE_W + 1, absY + SQUARE_H + 1, (0x404040 shl 24))

        // Each column: hue on X, brightness from 1 (top) → 0 (bottom) at current saturation.
        // Divide by SQUARE_W (not SQUARE_W-1) to avoid hue reaching exactly 360°, which
        // causes a yellow fringe on the last column.
        for (x in 0 until SQUARE_W) {
            val hue      = x / SQUARE_W.toFloat() * 360f
            val topColor = hsvToArgb(hue, currentSaturation, 1f)
            context.fillGradient(absX + x, absY, absX + x + 1, absY + SQUARE_H,
                topColor, (0xFF shl 24) or 0x000000)
        }

        //context.drawText(textRenderer, "Hue / Brightness", absX, absY - 9, 0xAAAAAA, false)
    }

    private fun renderVSlider(context: DrawContext) {
        val absX = guiLeft + V_SLIDER_X
        val absY = guiTop  + V_SLIDER_Y

        context.fill(absX - 1, absY - 1, absX + V_SLIDER_W + 1, absY + V_SLIDER_H + 1, (0x404040 shl 24))

        val topColor = hsvToArgb(currentHue, currentSaturation, 1f)
        context.fillGradient(absX, absY, absX + V_SLIDER_W, absY + V_SLIDER_H,
            topColor, (0xFF shl 24) or 0x000000)

        val iy = (absY + (1f - currentValue) * V_SLIDER_H).toInt().coerceIn(absY, absY + V_SLIDER_H - 2)
        context.fill(absX - 2, iy, absX + V_SLIDER_W + 2, iy + 2, (0xFF shl 24) or 0xC8C8C8)
    }

    private fun renderSSlider(context: DrawContext) {
        val absX = guiLeft + S_SLIDER_X
        val absY = guiTop  + S_SLIDER_Y

        context.fill(absX - 1, absY - 1, absX + S_SLIDER_W + 1, absY + S_SLIDER_H + 1, (0x404040 shl 24))

        for (x in 0 until S_SLIDER_W) {
            val sat = x.toFloat() / (S_SLIDER_W - 1).toFloat()
            context.fill(absX + x, absY, absX + x + 1, absY + S_SLIDER_H,
                hsvToArgb(currentHue, sat, currentValue))
        }

        val ix = (absX + currentSaturation * S_SLIDER_W).toInt().coerceIn(absX, absX + S_SLIDER_W - 2)
        context.fill(ix, absY - 2, ix + 2, absY + S_SLIDER_H + 2, (0xFF shl 24) or 0xC8C8C8)

        //context.drawText(textRenderer, "Saturation", absX, absY + S_SLIDER_H + 2, 0xAAAAAA, false)
    }

    private fun renderColorPreview(context: DrawContext) {
        val absX = guiLeft + PREVIEW_X
        val absY = guiTop  + PREVIEW_Y
        val rgb  = hsvToRgb(currentHue, currentSaturation, currentValue)

        // Swatch with white border
        context.fill(absX - 1, absY - 1, absX + PREVIEW_W + 1, absY + PREVIEW_H + 1,
            (0xFF shl 24) or 0xC8C8C8)
        context.fill(absX, absY, absX + PREVIEW_W, absY + PREVIEW_H, (0xFF shl 24) or rgb)

        // Hex code centred below the swatch
        val hex = "#%06X".format(rgb)
        val tw  = textRenderer.getWidth(hex)
        context.drawText(textRenderer, hex,
            absX + (PREVIEW_W - tw) / 2, guiTop + HEX_Y, 0xDDDDDD, false)
    }

    private fun renderColorCursor(context: DrawContext) {
        val absX = guiLeft + SQUARE_X
        val absY = guiTop  + SQUARE_Y
        val cx = (absX + currentHue / 360f * SQUARE_W).toInt().coerceIn(absX, absX + SQUARE_W - 1)
        val cy = (absY + (1f - currentValue) * SQUARE_H).toInt().coerceIn(absY, absY + SQUARE_H - 1)

        // Dark outline cross
        context.fill(cx - 3, cy,     cx + 4, cy + 1, (0xFF shl 24) or 0x404040)
        context.fill(cx,     cy - 3, cx + 1, cy + 4, (0xFF shl 24) or 0x404040)
        // White centre cross
        context.fill(cx - 2, cy,     cx + 3, cy + 1, (0xFF shl 24) or 0xC8C8C8)
        context.fill(cx,     cy - 2, cx + 1, cy + 3, (0xFF shl 24) or 0xC8C8C8)
    }

    /**
     * Draws a button texture using 9-slice scaling so corners stay pixel-perfect
     * regardless of button size. Corners are BUTTON_CORNER px; edges/centre are stretched.
     */
    private fun drawButtonNineSlice(context: DrawContext, texture: Identifier, x: Int, y: Int, w: Int, h: Int) {
        val c  = BUTTON_CORNER
        val tw = BUTTON_TEX_W
        val th = BUTTON_TEX_H
        val cw = tw - 2 * c   // centre width in texture
        val ch = th - 2 * c   // centre height in texture
        val cf = c.toFloat()
        // Corners (1:1, no stretch)
        context.drawTexture(texture, x,       y,       c, c, 0f,          0f,          c,  c,  tw, th)
        context.drawTexture(texture, x+w-c,   y,       c, c, (tw-c).toFloat(), 0f,    c,  c,  tw, th)
        context.drawTexture(texture, x,       y+h-c,   c, c, 0f,  (th-c).toFloat(),   c,  c,  tw, th)
        context.drawTexture(texture, x+w-c,   y+h-c,   c, c, (tw-c).toFloat(), (th-c).toFloat(), c, c, tw, th)
        // Edges (stretch in one axis)
        context.drawTexture(texture, x+c,     y,       w-2*c, c,     cf, 0f,          cw, c,  tw, th)  // top
        context.drawTexture(texture, x+c,     y+h-c,   w-2*c, c,     cf, (th-c).toFloat(), cw, c,  tw, th)  // bottom
        context.drawTexture(texture, x,       y+c,     c,     h-2*c, 0f, cf,          c,  ch, tw, th)  // left
        context.drawTexture(texture, x+w-c,   y+c,     c,     h-2*c, (tw-c).toFloat(), cf, c, ch, tw, th)  // right
        // Centre (stretch both axes)
        context.drawTexture(texture, x+c,     y+c,     w-2*c, h-2*c, cf, cf,          cw, ch, tw, th)
    }

    private fun renderButton(
        context: DrawContext, x: Int, y: Int, w: Int, h: Int,
        label: String, mouseX: Int, mouseY: Int, forcedScale: Float = -1f
    ) {
        val hovered = mouseX in x until x + w && mouseY in y until y + h
        val tex = if (hovered) BUTTON_HOVERED_TEXTURE else BUTTON_TEXTURE
        drawButtonNineSlice(context, tex, x, y, w, h)

        val rawWidth = textRenderer.getWidth(label)
        val scale = when {
            forcedScale >= 0f   -> forcedScale
            rawWidth > w - 4    -> (w - 4f) / rawWidth
            else                -> 1f
        }
        val tx = x + (w - rawWidth * scale) / 2f
        val ty = y + (h - 8f * scale) / 2f

        val matrices = context.matrices
        matrices.push()
        matrices.translate(tx, ty, 0f)
        matrices.scale(scale, scale, 1f)
        context.drawText(textRenderer, label, 0, 0, 0xFFFFFF, false)
        matrices.pop()
    }

    // ── Mouse / keyboard interaction ───────────────────────────────────────

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (overlayMode != OverlayMode.NONE) {
            handleOverlayClick(mouseX.toInt(), mouseY.toInt())
            return true
        }

        val rx = mouseX - guiLeft
        val ry = mouseY - guiTop

        // Model area: start drag-to-rotate
        if (rx >= MODEL_X && rx < MODEL_X + MODEL_W && ry >= MODEL_Y && ry < MODEL_Y + MODEL_H) {
            activeDrag = DragTarget.MODEL
            return true
        }

        // Bone list
        if (rx >= BONE_LIST_X && rx < BONE_LIST_X + BONE_LIST_W &&
            ry >= BONE_LIST_Y  && ry < BONE_LIST_Y  + BONE_LIST_H) {
            val flatList = buildFlatList()
            val idx = boneListScroll + ((ry - BONE_LIST_Y) / BONE_ENTRY_H).toInt()
            if (idx in flatList.indices) {
                when (val item = flatList[idx]) {
                    is BoneListItem.GroupHeader -> {
                        selectedBone = "group:${item.group.key}"
                        // Toggle expansion
                        if (item.group.key in expandedGroups) expandedGroups.remove(item.group.key)
                        else expandedGroups.add(item.group.key)
                        clampScroll()
                        updateHsvFromSelectedBone()
                        playBoneClickSound()
                    }
                    is BoneListItem.BoneEntry -> {
                        selectedBone = item.bone.first
                        if (item.group.key == "bells") {
                            val relX = rx - BONE_LIST_X
                            when {
                                relX >= 14 && relX < 22 -> {
                                    workingBellType = (workingBellType - 1 + SoundRegistry.BELL_COUNT) % SoundRegistry.BELL_COUNT
                                    previewBell(workingBellType)
                                }
                                relX >= BONE_LIST_W - 20 -> {
                                    workingBellType = (workingBellType + 1) % SoundRegistry.BELL_COUNT
                                    previewBell(workingBellType)
                                }
                                else -> previewBell(workingBellType)
                            }
                        } else {
                            updateHsvFromSelectedBone()
                            playBoneClickSound()
                        }
                    }
                }
            }
            return true
        }

        // Colour square
        if (rx >= SQUARE_X && rx < SQUARE_X + SQUARE_W && ry >= SQUARE_Y && ry < SQUARE_Y + SQUARE_H) {
            activeDrag = DragTarget.COLOR_SQUARE
            pickFromSquare(rx - SQUARE_X, ry - SQUARE_Y)
            return true
        }

        // Brightness (V) slider
        if (rx >= V_SLIDER_X && rx < V_SLIDER_X + V_SLIDER_W &&
            ry >= V_SLIDER_Y  && ry < V_SLIDER_Y  + V_SLIDER_H) {
            activeDrag = DragTarget.V_SLIDER
            pickFromVSlider(ry - V_SLIDER_Y)
            return true
        }

        // Saturation (S) slider
        if (rx >= S_SLIDER_X && rx < S_SLIDER_X + S_SLIDER_W &&
            ry >= S_SLIDER_Y  && ry < S_SLIDER_Y  + S_SLIDER_H) {
            activeDrag = DragTarget.S_SLIDER
            pickFromSSlider(rx - S_SLIDER_X)
            return true
        }

        // Buttons
        if (rx >= BTN_X && rx < BTN_X + BTN_W) {
            when {
                ry >= DONE_Y      && ry < DONE_Y      + BTN_H -> { clickDone();     return true }
                ry >= CANCEL_Y    && ry < CANCEL_Y    + BTN_H -> { clickCancel();   return true }
                ry >= RESET_Y     && ry < RESET_Y     + BTN_H -> { clickReset();    return true }
                ry >= RESET_ALL_Y && ry < RESET_ALL_Y + BTN_H -> { clickResetAll(); return true }
            }
        }

        // Preset buttons
        if (ry >= PRESET_BTN_Y && ry < PRESET_BTN_Y + PRESET_BTN_H) {
            when {
                rx >= SAVE_PRESET_X && rx < SAVE_PRESET_X + PRESET_BTN_W -> { clickSavePreset(); return true }
                rx >= LOAD_PRESET_X && rx < LOAD_PRESET_X + PRESET_BTN_W -> { clickLoadPreset(); return true }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (overlayMode != OverlayMode.NONE) return true
        activeDrag = DragTarget.NONE
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int,
                               deltaX: Double, deltaY: Double): Boolean {
        val rx = mouseX - guiLeft
        val ry = mouseY - guiTop
        when (activeDrag) {
            DragTarget.MODEL        -> {
                modelYaw   = (modelYaw + deltaX.toFloat() * 1.5f)
                modelPitch = (modelPitch + deltaY.toFloat() * 0.8f).coerceIn(-50f, 35f)
            }
            DragTarget.COLOR_SQUARE -> pickFromSquare(rx - SQUARE_X, ry - SQUARE_Y)
            DragTarget.V_SLIDER     -> pickFromVSlider(ry - V_SLIDER_Y)
            DragTarget.S_SLIDER     -> pickFromSSlider(rx - S_SLIDER_X)
            DragTarget.NONE         -> {}
        }
        return true
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double,
                                horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (overlayMode == OverlayMode.LOAD) {
            val maxScroll = maxOf(0, loadedPresets.size - LOAD_VISIBLE)
            presetListScroll = (presetListScroll - verticalAmount.toInt()).coerceIn(0, maxScroll)
            return true
        }
        if (overlayMode == OverlayMode.SAVE) return true

        val rx = mouseX - guiLeft
        val ry = mouseY - guiTop

        // Zoom model
        if (rx >= MODEL_X && rx < MODEL_X + MODEL_W && ry >= MODEL_Y && ry < MODEL_Y + MODEL_H) {
            modelScale = (modelScale + verticalAmount.toFloat() * 1.5f).coerceIn(8f, 60f)
            return true
        }

        // Scroll bone list
        if (rx >= BONE_LIST_X && rx < BONE_LIST_X + BONE_LIST_W &&
            ry >= BONE_LIST_Y  && ry < BONE_LIST_Y  + BONE_LIST_H) {
            val maxScroll = maxOf(0, buildFlatList().size - VISIBLE_BONES)
            boneListScroll = (boneListScroll - verticalAmount.toInt()).coerceIn(0, maxScroll)
            return true
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (overlayMode != OverlayMode.NONE) {
            when (keyCode) {
                256 -> { overlayMode = OverlayMode.NONE; saveNameInput = ""; saveNameError = "" } // Escape
                257, 335 -> if (overlayMode == OverlayMode.SAVE) confirmSave()                   // Enter
                259 -> if (overlayMode == OverlayMode.SAVE && saveNameInput.isNotEmpty())        // Backspace
                    saveNameInput = saveNameInput.dropLast(1)
            }
            return true
        }
        // E (inventory key) closes the screen and auto-saves, but only when no overlay is open
        if (MinecraftClient.getInstance().options.inventoryKey.matchesKey(keyCode, scanCode)) {
            close()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        if (overlayMode == OverlayMode.SAVE) {
            if (!chr.isISOControl() && saveNameInput.length < 24) saveNameInput += chr
            return true
        }
        return super.charTyped(chr, modifiers)
    }

    // ── Colour picking helpers ─────────────────────────────────────────────

    private fun pickFromSquare(relX: Double, relY: Double) {
        currentHue   = (relX / SQUARE_W * 360f).toFloat().coerceIn(0f, 359.99f)
        currentValue = (1f - relY / SQUARE_H).toFloat().coerceIn(0f, 1f)
        applyCurrentColor()
    }

    private fun pickFromVSlider(relY: Double) {
        currentValue = (1f - relY / V_SLIDER_H).toFloat().coerceIn(0f, 1f)
        applyCurrentColor()
    }

    private fun pickFromSSlider(relX: Double) {
        currentSaturation = (relX / S_SLIDER_W).toFloat().coerceIn(0f, 1f)
        applyCurrentColor()
    }

    private fun applyCurrentColor() {
        val rgb = hsvToRgb(currentHue, currentSaturation, currentValue)
        val sel = selectedBone ?: return
        if (sel == "bell_type" || sel == "group:bells") return
        if (sel.startsWith("group:")) {
            val groupKey = sel.removePrefix("group:")
            val group = effectiveBoneGroups.find { it.key == groupKey } ?: return
            for ((boneName, _) in group.bones) workingColors.putInt(boneName, rgb)
        } else {
            workingColors.putInt(sel, rgb)
        }
        targetEntity.dataTracker.set(AbstractBikeEntity.BONE_COLORS, workingColors.copy())
    }

    private fun updateHsvFromSelectedBone() {
        val sel = selectedBone ?: return
        if (sel == "bell_type" || sel == "group:bells") return
        val rgb = if (sel.startsWith("group:")) {
            val groupKey  = sel.removePrefix("group:")
            val group     = effectiveBoneGroups.find { it.key == groupKey } ?: return
            val firstBone = group.bones.firstOrNull()?.first ?: return
            if (workingColors.contains(firstBone)) workingColors.getInt(firstBone) else 0xFFFFFF
        } else {
            if (workingColors.contains(sel)) workingColors.getInt(sel) else 0xFFFFFF
        }
        val (h, s, v) = rgbToHsv(rgb)
        currentHue = h; currentSaturation = s; currentValue = v
    }

    private fun previewBell(index: Int) {
        MinecraftClient.getInstance().soundManager.play(
            PositionedSoundInstance.master(SoundRegistry.getBellSound(index), 1f)
        )
    }

    private fun playOpenSound() {
        MinecraftClient.getInstance().soundManager.play(
            PositionedSoundInstance.master(SoundRegistry.GUI_OPEN, 1f, 1f)
        )
    }

    private fun playClickSound() {
        MinecraftClient.getInstance().soundManager.play(
            PositionedSoundInstance.master(SoundRegistry.BUTTON_26, 1f, 1f)
        )
    }

    private fun playPresetClickSound() {
        MinecraftClient.getInstance().soundManager.play(
            PositionedSoundInstance.master(SoundRegistry.BUTTON_4768, 1f, 1f)
        )
    }

    private fun playBoneClickSound() {
        MinecraftClient.getInstance().soundManager.play(
            PositionedSoundInstance.master(SoundRegistry.BUTTON_PRESS, 1f, 1f)
        )
    }

    // ── Button actions ─────────────────────────────────────────────────────

    private fun clickDone() {
        playClickSound()
        confirmed = true
        ClientPlayNetworking.send(BikeCustomizePayload(targetEntity.id, workingColors.copy(), workingBellType))
        close()
    }

    private fun clickCancel() {
        playClickSound()
        cancelled = true
        close()
    }

    private fun clickReset() {
        playClickSound()
        val sel = selectedBone ?: return
        if (sel == "bell_type" || sel == "group:bells") {
            workingBellType = 0
            return
        }
        if (sel.startsWith("group:")) {
            val groupKey = sel.removePrefix("group:")
            val group    = effectiveBoneGroups.find { it.key == groupKey } ?: return
            for ((boneName, _) in group.bones) workingColors.remove(boneName)
        } else {
            workingColors.remove(sel)
        }
        updateHsvFromSelectedBone()
        targetEntity.dataTracker.set(AbstractBikeEntity.BONE_COLORS, workingColors.copy())
    }

    private fun clickResetAll() {
        playClickSound()
        workingColors.keys.toList().forEach { workingColors.remove(it) }
        // Restore the bike's default frame colour so it doesn't fall back to plain white
        workingColors.putInt("frame", targetEntity.defaultColor)
        workingColors.putInt("headlight_mount", targetEntity.defaultColor)
        workingBellType = 0
        updateHsvFromSelectedBone()
        targetEntity.dataTracker.set(AbstractBikeEntity.BONE_COLORS, workingColors.copy())
    }

    // ── Preset buttons ─────────────────────────────────────────────────────

    private fun clickSavePreset() {
        playPresetClickSound()
        loadedPresets = BikePresetManager.load()
        saveNameInput = ""
        saveNameError = ""
        overlayMode = OverlayMode.SAVE
    }

    private fun clickLoadPreset() {
        playPresetClickSound()
        loadedPresets = BikePresetManager.load()
        presetListScroll = 0
        overlayMode = OverlayMode.LOAD
    }

    private fun confirmSave() {
        val name = saveNameInput.trim()
        if (name.isEmpty()) { saveNameError = "Name cannot be empty"; return }
        BikePresetManager.savePreset(name, workingColors, workingBellType)
        playPresetClickSound()
        overlayMode = OverlayMode.NONE
        saveNameInput = ""
        saveNameError = ""
    }

    private fun handleOverlayClick(mx: Int, my: Int) {
        when (overlayMode) {
            OverlayMode.SAVE -> {
                val ox      = (width  - OVERLAY_W) / 2
                val oy      = (height - SAVE_OL_H) / 2
                val saveX   = ox + 4
                val cancelX = ox + OVERLAY_W - 4 - SAVE_BTN_W
                val btnY    = oy + SAVE_BTN_Y
                when {
                    mx in saveX   until saveX   + SAVE_BTN_W && my in btnY until btnY + PRESET_BTN_H -> confirmSave()
                    mx in cancelX until cancelX + SAVE_BTN_W && my in btnY until btnY + PRESET_BTN_H -> {
                        overlayMode = OverlayMode.NONE
                        saveNameInput = ""
                        saveNameError = ""
                        playPresetClickSound()
                    }
                }
            }
            OverlayMode.LOAD -> {
                val ox    = (width  - OVERLAY_W) / 2
                val oy    = (height - LOAD_OL_H) / 2
                val listY = oy + LOAD_HDR_H

                // Close button at bottom
                val closeBtnY = oy + LOAD_HDR_H + LOAD_VISIBLE * LOAD_ROW_H + 1
                val closeBtnX = ox + (OVERLAY_W - 90) / 2
                if (mx in closeBtnX until closeBtnX + 90 && my in closeBtnY until closeBtnY + PRESET_BTN_H) {
                    overlayMode = OverlayMode.NONE; playPresetClickSound(); return
                }

                // Preset rows — only process clicks inside the overlay box
                if (mx !in ox until ox + OVERLAY_W || my !in oy until oy + LOAD_OL_H) return
                for ((i, preset) in loadedPresets.drop(presetListScroll).take(LOAD_VISIBLE).withIndex()) {
                    val ry = listY + i * LOAD_ROW_H
                    if (my !in ry until ry + LOAD_ROW_H) continue
                    val delBtnX = ox + OVERLAY_W - 13
                    if (mx in delBtnX until delBtnX + 12) {
                        BikePresetManager.deletePreset(preset.name)
                        loadedPresets = BikePresetManager.load()
                        presetListScroll = presetListScroll.coerceIn(0, maxOf(0, loadedPresets.size - LOAD_VISIBLE))
                    } else {
                        for ((key, value) in preset.colors) workingColors.putInt(key, value)
                        workingBellType = preset.bellType
                        targetEntity.dataTracker.set(AbstractBikeEntity.BONE_COLORS, workingColors.copy())
                        updateHsvFromSelectedBone()
                        overlayMode = OverlayMode.NONE
                    }
                    playPresetClickSound()
                    return
                }
            }
            OverlayMode.NONE -> {}
        }
    }

    // ── Overlay rendering ──────────────────────────────────────────────────

    private fun renderOverlay(context: DrawContext, mouseX: Int, mouseY: Int) {
        val matrices = context.matrices
        matrices.push()
        matrices.translate(0f, 0f, 200f)  // above the 3D model's Z=100 so overlay is always in front
        RenderSystem.disableDepthTest()
        context.fill(0, 0, width, height, 0x80000000.toInt())
        when (overlayMode) {
            OverlayMode.SAVE -> renderSaveOverlay(context, mouseX, mouseY)
            OverlayMode.LOAD -> renderLoadOverlay(context, mouseX, mouseY)
            OverlayMode.NONE -> {}
        }
        RenderSystem.enableDepthTest()
        matrices.pop()
    }

    private fun renderSaveOverlay(context: DrawContext, mouseX: Int, mouseY: Int) {
        val ox = (width  - OVERLAY_W) / 2
        val oy = (height - SAVE_OL_H) / 2

        context.fill(ox - 1, oy - 1, ox + OVERLAY_W + 1, oy + SAVE_OL_H + 1, (0xFF shl 24) or 0xC8C8C8)
        context.fill(ox, oy, ox + OVERLAY_W, oy + SAVE_OL_H, (0xFF shl 24) or 0x2D2D2D)

        context.drawText(textRenderer, "Save Preset", ox + 4, oy + 3, 0xFFFFFF, true)

        // Text input field
        val fieldX = ox + 4
        val fieldY = oy + SAVE_FIELD_Y
        val fieldW = OVERLAY_W - 8
        context.fill(fieldX - 1, fieldY - 1, fieldX + fieldW + 1, fieldY + SAVE_FIELD_H + 1, (0xFF shl 24) or 0xC8C8C8)
        context.fill(fieldX, fieldY, fieldX + fieldW, fieldY + SAVE_FIELD_H, (0xFF shl 24) or 0x111111)
        val cursor = if ((System.currentTimeMillis() / 500) % 2 == 0L) "|" else " "
        context.drawText(textRenderer, saveNameInput + cursor, fieldX + 3, fieldY + 3, 0xFFFFFF, false)
        val countStr = "${saveNameInput.length}/24"
        context.drawText(textRenderer, countStr,
            fieldX + fieldW - textRenderer.getWidth(countStr) - 3, fieldY + 3, 0x888888, false)

        // Hint / error line
        when {
            saveNameError.isNotEmpty() ->
                context.drawText(textRenderer, saveNameError, ox + 4, oy + 31, 0xFF4444, false)
            loadedPresets.any { it.name.equals(saveNameInput.trim(), ignoreCase = true) } ->
                context.drawText(textRenderer, "Will overwrite existing preset", ox + 4, oy + 31, 0xFFAA44, false)
            saveNameInput.isNotEmpty() && loadedPresets.size >= BikePresetManager.MAX_PRESETS ->
                context.drawText(textRenderer, "At limit — oldest will be replaced", ox + 4, oy + 31, 0xFFAA44, false)
        }

        renderButton(context, ox + 4,                           oy + SAVE_BTN_Y, SAVE_BTN_W, PRESET_BTN_H, "Save",   mouseX, mouseY)
        renderButton(context, ox + OVERLAY_W - 4 - SAVE_BTN_W, oy + SAVE_BTN_Y, SAVE_BTN_W, PRESET_BTN_H, "Cancel", mouseX, mouseY)
    }

    private fun renderLoadOverlay(context: DrawContext, mouseX: Int, mouseY: Int) {
        val ox = (width  - OVERLAY_W) / 2
        val oy = (height - LOAD_OL_H) / 2

        context.fill(ox - 1, oy - 1, ox + OVERLAY_W + 1, oy + LOAD_OL_H + 1, (0xFF shl 24) or 0xC8C8C8)
        context.fill(ox, oy, ox + OVERLAY_W, oy + LOAD_OL_H, (0xFF shl 24) or 0x2D2D2D)

        context.drawText(textRenderer, "Load Preset", ox + 4, oy + 3, 0xFFFFFF, true)

        val listY = oy + LOAD_HDR_H
        if (loadedPresets.isEmpty()) {
            context.drawText(textRenderer, "No presets saved.", ox + 4, listY + 4, 0x888888, false)
        } else {
            val maxScroll = maxOf(0, loadedPresets.size - LOAD_VISIBLE)
            if (presetListScroll > 0)
                context.drawText(textRenderer, "▲", ox + OVERLAY_W / 2 + 55, listY + 2, 0xAAAAAA, false)
            if (presetListScroll < maxScroll)
                context.drawText(textRenderer, "▼", ox + OVERLAY_W / 2 + 55,
                    listY + LOAD_VISIBLE * LOAD_ROW_H - 9, 0xAAAAAA, false)

            for ((i, preset) in loadedPresets.drop(presetListScroll).take(LOAD_VISIBLE).withIndex()) {
                val ry = listY + i * LOAD_ROW_H
                if (mouseX in ox until ox + OVERLAY_W - 16 && mouseY in ry until ry + LOAD_ROW_H)
                    context.fill(ox, ry, ox + OVERLAY_W - 16, ry + LOAD_ROW_H, (0x40 shl 24) or 0xFFFFFF)

                context.drawText(textRenderer,
                    textRenderer.trimToWidth(preset.name, OVERLAY_W - 26),
                    ox + 4, ry + 2, 0xDDDDDD, false)

                val delBtnX = ox + OVERLAY_W - 13
                val delBtnY = ry + (LOAD_ROW_H - 10) / 2
                val delHovered = mouseX in delBtnX until delBtnX + 12 && mouseY in ry until ry + LOAD_ROW_H
                val delTex = if (delHovered) DEL_BUTTON_HOVERED_TEXTURE else DEL_BUTTON_TEXTURE
                context.drawTexture(delTex, delBtnX - 1, delBtnY, 0f, 0f, 12, 11, 12, 11)
                //context.drawText(textRenderer, "×", delBtnX + 3, delBtnY + 1, 0xFFFFFF, false)
            }
        }

        val closeBtnY = oy + LOAD_HDR_H + LOAD_VISIBLE * LOAD_ROW_H + 1
        renderButton(context, ox + (OVERLAY_W - 90) / 2, closeBtnY, 90, PRESET_BTN_H, "Close", mouseX, mouseY)
    }
}