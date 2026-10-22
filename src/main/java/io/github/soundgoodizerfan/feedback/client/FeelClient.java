/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.feel.Feel;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * The client end of {@link Feel}: everything is the view and the speakers, never the simulation.
 *
 * <ul>
 * <li><b>Freeze.</b> The world keeps ticking. For {@code freezeTicks} the local view shows a copy of
 * the frame taken the moment the impact landed, drawn under the HUD so the HUD stays live. No tick-rate
 * change, no partial-tick tampering. (The alternative, pinning the partial tick, would show a 20 Hz
 * stutter, not a stop.)</li>
 * <li><b>Flash.</b> A full-screen colour pulse above everything, fading over {@link FeelTuning#FLASH_MS}.</li>
 * <li><b>Camera.</b> Roll through {@link ViewportEvent.ComputeCameraAngles} and a FOV pop through
 * {@link ComputeFovModifierEvent}, both decaying exponentially. Neither writes the player's rotation,
 * so the crosshair never moves (the gun recoil's rotation write is what not to copy).</li>
 * <li><b>Sound and sparks</b> at the impact point.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class FeelClient {

    private static final RandomSource RANDOM = RandomSource.create();

    /** Ticks of freeze still to show; counted on the client tick, which stops while the game is paused. */
    private static int freezeLeft;
    /** The next freeze frame has to be captured, not just shown. */
    private static boolean capture;
    private static TextureTarget frozen;

    private static long flashStartNs;
    private static float flashStrength;

    private static long kickStartNs;
    private static float rollDeg, fovPop;
    private static float rollSign = 1f;

    private FeelClient() {
    }

    /** Plays {@code feel} for the impact at {@code at}; {@code local} is true for the party it happened to. */
    public static void play(Feel feel, Vec3 at, boolean local) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
            return;
        if (!local && mc.player.position().distanceTo(at) > FeelTuning.BYSTANDER_RANGE)
            return;

        flashStrength = Math.max(flashAlpha(), feel.flash() * (local ? 1f : FeelTuning.BYSTANDER_FLASH));
        flashStartNs = System.nanoTime();

        if (local) {
            if (feel.freezeTicks() > 0) {
                freezeLeft = feel.freezeTicks();
                capture = true;
            }
            kickStartNs = System.nanoTime();
            rollSign = -rollSign;
            rollDeg = feel.rollDegrees() * rollSign;
            fovPop = feel.fovPop();
        }

        SoundEvent sound = (feel.heavy() ? FSounds.FEEL_HEAVY : FSounds.FEEL_LIGHT).get();
        float pitch = (feel.heavy() ? FeelTuning.HEAVY_PITCH : FeelTuning.LIGHT_PITCH)
                + (RANDOM.nextFloat() * 2f - 1f) * FeelTuning.PITCH_SPREAD;
        mc.level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.PLAYERS,
                feel.heavy() ? FeelTuning.HEAVY_VOLUME : FeelTuning.LIGHT_VOLUME, pitch, false);

        float speed = FeelTuning.SPARK_SPEED * (feel.heavy() ? FeelTuning.HEAVY_SPARK_BOOST : 1f);
        for (int i = 0; i < feel.sparks(); i++)
            mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z,
                    (RANDOM.nextFloat() - 0.5f) * 2 * speed, (RANDOM.nextFloat() - 0.2f) * speed,
                    (RANDOM.nextFloat() - 0.5f) * 2 * speed);
    }

    /** exp(-elapsed/tau) since {@code startNs}; 0 if never started. */
    private static float decayOf(long startNs, float tauMs) {
        if (startNs == 0)
            return 0f;
        float ms = (System.nanoTime() - startNs) / 1_000_000f;
        return (float) Math.exp(-ms / tauMs);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (freezeLeft > 0 && !Minecraft.getInstance().isPaused())
            freezeLeft--;
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (rollDeg != 0f)
            event.setRoll(event.getRoll() + rollDeg * decayOf(kickStartNs, FeelTuning.ROLL_DECAY_MS));
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        if (fovPop != 0f && event.getPlayer() == Minecraft.getInstance().player)
            event.setNewFovModifier(event.getNewFovModifier()
                    * (1f - fovPop * decayOf(kickStartNs, FeelTuning.FOV_DECAY_MS)));
    }

    static void register(RegisterGuiLayersEvent event) {
        // Under every layer: the HUD stays live over the held frame.
        event.registerBelowAll(Feedback.id("feel_freeze"), FeelClient::renderFreeze);
        event.registerAboveAll(Feedback.id("feel_flash"), FeelClient::renderFlash);
    }

    private static void renderFreeze(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (freezeLeft <= 0 || mc.level == null)
            return;
        RenderTarget main = mc.getMainRenderTarget();
        int w = main.width, h = main.height;
        g.flush();
        if (frozen == null || frozen.width != w || frozen.height != h) {
            if (frozen != null)
                frozen.destroyBuffers();
            frozen = new TextureTarget(w, h, false, Minecraft.ON_OSX);
            capture = true;
        }
        if (capture) {
            // Copy the frame the world just drew into the held target, then go back to drawing into the main one.
            frozen.bindWrite(true);
            main.blitToScreen(w, h);
            main.bindWrite(true);
            capture = false;
            return; // this frame is already the frame being held
        }
        frozen.blitToScreen(w, h);
    }

    private static void renderFlash(GuiGraphics g, DeltaTracker delta) {
        float alpha = flashAlpha();
        if (alpha > 0f)
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), ((int) (alpha * 255f) << 24) | FeelTuning.FLASH_RGB);
    }

    /** The flash as it stands now: its strength, fading linearly to nothing over {@link FeelTuning#FLASH_MS}. */
    private static float flashAlpha() {
        if (flashStartNs == 0)
            return 0f;
        return Math.max(0f, flashStrength * (1f - (System.nanoTime() - flashStartNs) / 1_000_000f / FeelTuning.FLASH_MS));
    }
}
