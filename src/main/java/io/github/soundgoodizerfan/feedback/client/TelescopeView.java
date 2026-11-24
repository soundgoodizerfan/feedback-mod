// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.item.ModularTelescope;
import io.github.soundgoodizerfan.feedback.net.SightingPayload;
import io.github.soundgoodizerfan.feedback.space.Sighting;
import io.github.soundgoodizerfan.feedback.space.SkyBodies;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * What looking through a {@link ModularTelescope} does to the view, on the client only: the field of view narrows by the telescope's
 * {@code MAGNIFICATION}, a scope mask frames it (vanilla's own spyglass texture), and the {@code TELESCOPE_BLUR} figure lays a pale haze over
 * it, thicker the softer the image. The server knows nothing of the view.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class TelescopeView {

    private static final ResourceLocation SCOPE = ResourceLocation.withDefaultNamespace("textures/misc/spyglass_scope.png");
    /** The haze at full blur, as a share of opaque. */
    private static final float HAZE_AT_FULL_BLUR = 0.6f;

    /** The planet already reported while the player keeps looking; cleared when they lower the telescope. */
    private static SkyBodies.Body reported;

    private TelescopeView() {
    }

    /** The telescope the player is looking through now, or empty. */
    private static ItemStack looking(LocalPlayer player) {
        if (player == null || !player.isUsingItem())
            return ItemStack.EMPTY;
        ItemStack stack = player.getUseItem();
        return stack.getItem() instanceof ModularTelescope && !ModularStacks.broken(stack) ? stack : ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        ItemStack stack = looking(Minecraft.getInstance().player);
        if (stack.isEmpty())
            return;
        ModularFigures f = ModularStacks.figures(stack);
        float magnification = f == null ? 1f : Math.max(1f, f.get(Fig.MAGNIFICATION));
        event.setNewFovModifier(event.getNewFovModifier() / magnification);
    }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ItemStack stack = looking(mc.player);
        if (stack.isEmpty())
            reported = null;
        if (stack.isEmpty() || !mc.options.getCameraType().isFirstPerson())
            return;
        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth(), h = g.guiHeight();
        int size = Math.min(w, h), x = (w - size) / 2, y = (h - size) / 2;
        g.blit(SCOPE, x, y, 0, 0f, 0f, size, size, size, size);
        g.fill(0, 0, x, h, 0xFF000000);
        g.fill(x + size, 0, w, h, 0xFF000000);
        g.fill(x, 0, x + size, y, 0xFF000000);
        g.fill(x, y + size, x + size, h, 0xFF000000);
        ModularFigures f = ModularStacks.figures(stack);
        float blur = f == null ? 0f : f.get(Fig.TELESCOPE_BLUR);
        int alpha = Math.round(255f * HAZE_AT_FULL_BLUR * Math.max(0f, Math.min(1f, blur)));
        if (alpha > 0)
            g.fill(x, y, x + size, y + size, alpha << 24 | 0xE6E6E6);
        planet(g, mc, x, y, size, f);
    }

    /** A planet the scope resolves is drawn as a lit disc where it stands in the view, and reported to the server once. */
    private static void planet(GuiGraphics g, Minecraft mc, int x, int y, int size, ModularFigures f) {
        LocalPlayer player = mc.player;
        float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
        SkyBodies.Body body = Sighting.resolved(player, 1.0, partial);
        if (body == null)
            return;
        if (body != reported) {
            reported = body;
            PacketDistributor.sendToServer(new SightingPayload(body.id()));
        }
        Vec3 d = SkyBodies.direction(player.level(), body, partial);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z)), pitch = (float) -Math.toDegrees(Math.asin(d.y));
        float magnification = f == null ? 1f : Math.max(1f, f.get(Fig.MAGNIFICATION));
        float perDegree = size / 2f / (mc.options.fov().get() / 2f / magnification);
        int cx = Math.round(x + size / 2f + Mth.wrapDegrees(yaw - Mth.lerp(partial, player.yRotO, player.getYRot())) * perDegree);
        int cy = Math.round(y + size / 2f + (pitch - Mth.lerp(partial, player.xRotO, player.getXRot())) * perDegree);
        int r = Math.max(3, size / 14);
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            // lit from the upper left: the disc darkens toward its lower right
            float lit = 1f - 0.45f * Mth.clamp((dy + half * 0.6f) / (2f * r), 0f, 1f);
            int rgb = body.tint() & 0xFFFFFF;
            int col = 0xFF000000 | Math.round(((rgb >> 16) & 0xFF) * lit) << 16 | Math.round(((rgb >> 8) & 0xFF) * lit) << 8 | Math.round((rgb & 0xFF) * lit);
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, col);
        }
    }
}
