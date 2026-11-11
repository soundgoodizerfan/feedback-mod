// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.brigadier.Command;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.GuiStyle;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Dev tool, {@code /gunarm}: the gun in your main hand and its arm(s) hung in the middle of the screen, drawn by
 * the first-person pass itself, so what you see is what the game draws. Drag to orbit, scroll to zoom, sliders
 * nudge the arm of the held gun's shape ({@link GunArms#tune}); Print writes the numbers to chat and the log. Nothing is saved.
 */
public final class GunArmTuneScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.ENAMEL;
    private static final String[] NAMES = {"x px", "y px", "z px", "yaw", "pitch", "roll"};
    private float yaw = 200f, pitch = 15f, zoom = 1f;

    /** The arm figures of the held gun's shape. */
    private static float[] tune() {
        var p = Minecraft.getInstance().player;
        ItemStack stack = p == null ? ItemStack.EMPTY : p.getMainHandItem();
        return stack.getItem() instanceof GunItem ? GunArms.tune(GunView.of(stack)) : new float[6];
    }

    private GunArmTuneScreen() {
        super(Component.literal("Gun arm tuning"));
    }

    @Override
    protected void init() {
        for (int i = 0; i < 6; i++) {
            int k = i, y = 20 + i * 22;
            addRenderableWidget(GuiStyle.slider(SKIN, 10, y + 10, 150, i < 3 ? -16 : -90, i < 3 ? 16 : 90, i < 3 ? 0.25 : 1,
                    () -> tune()[k], v -> tune()[k] = (float) v));
        }
        addRenderableWidget(GuiStyle.button(SKIN, Component.literal("Reset"), b -> java.util.Arrays.fill(tune(), 0f), 10, 160, 72, 20));
        addRenderableWidget(GuiStyle.button(SKIN, Component.literal("Print"), b -> print(), 88, 160, 72, 20));
        addRenderableWidget(GuiStyle.button(SKIN, Component.literal("Front"), b -> { yaw = 180f; pitch = 0f; }, 10, 184, 72, 20));
        addRenderableWidget(GuiStyle.button(SKIN, Component.literal("Side"), b -> { yaw = 90f; pitch = 0f; }, 88, 184, 72, 20));
    }

    private static void print() {
        var p = Minecraft.getInstance().player;
        String shape = p != null && p.getMainHandItem().getItem() instanceof GunItem ? GunArms.shape(GunView.of(p.getMainHandItem())) : "?";
        String s = String.format("gun arm tune [" + shape + "]: x %.2f  y %.2f  z %.2f  yaw %.1f  pitch %.1f  roll %.1f", (Object[]) boxed());
        Feedback.LOGGER.info(s);
        if (Minecraft.getInstance().player != null)
            Minecraft.getInstance().player.sendSystemMessage(Component.literal(s));
    }

    private static Object[] boxed() {
        Object[] o = new Object[6];
        for (int i = 0; i < 6; i++)
            o[i] = tune()[i];
        return o;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xFF30343A);
        super.render(g, mx, my, pt);
        for (int i = 0; i < 6; i++)
            g.drawString(font, NAMES[i] + "  " + String.format("%.2f", tune()[i]), 10, 20 + i * 22, SKIN.ink());
        g.drawString(font, "drag: orbit   scroll: zoom", 10, height - 14, SKIN.dim());
        var mc = Minecraft.getInstance();
        ItemStack stack = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) {
            g.drawCenteredString(font, "Hold a gun in your main hand", width / 2, height / 2, SKIN.warn());
            return;
        }
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(width * 0.6f, height * 0.5f, 400f);
        float s = 260f * zoom;
        pose.scale(s, -s, s);
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        Lighting.setupForFlatItems();
        GunRenderer.renderHeld(stack, GunView.of(stack), GunMotion.hand(false), 1f, pose, g.bufferSource(), 0xF000F0);
        g.flush();
        Lighting.setupFor3DItems();
        pose.popPose();
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (x < 170)
            return super.mouseDragged(x, y, button, dx, dy);
        yaw += (float) dx * 0.7f;
        pitch = Mth.clamp(pitch + (float) dy * 0.7f, -89f, 89f);
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        zoom = Mth.clamp(zoom * (sy > 0 ? 1.1f : 1 / 1.1f), 0.3f, 5f);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
    public static final class Command_ {
        @net.neoforged.bus.api.SubscribeEvent
        public static void register(RegisterClientCommandsEvent e) {
            e.getDispatcher().register(Commands.literal("gunarm").executes(c -> {
                Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(new GunArmTuneScreen()));
                return Command.SINGLE_SUCCESS;
            }));
        }
    }
}
