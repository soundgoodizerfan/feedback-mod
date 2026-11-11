// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.trait.SpearCharge;
import io.github.soundgoodizerfan.feedback.net.SpearChargePayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The client half of the spear's charge ({@link SpearCharge}): reads the use key, tells the server on
 * each change and every {@link #BEAT} ticks while held, and keeps the stage the pose draws from.
 * The local wielder's stage is counted here from the same stage lengths the server uses; other
 * players' arrive in {@code SpearStagePayload}. Stages: 0 none, 1 engaged, 2 tired, 3 disengaged.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class SpearClient {

    private static final int BEAT = 10;
    /** Longest a remote stage is believed without news, so a wielder who left tracking mid-charge does not stay posed. */
    private static final int REMOTE_LIFE = 200;

    /** Held ticks of the local charge, -1 when not charging. */
    private static int ticks = -1;
    private static int stage;
    private static int beat;
    /** The server believes the key is down. */
    private static boolean sent;
    /** The charge ran its course; no more is sent until the key is let go. */
    private static boolean spent;
    /** The local charge's cumulative stage ends in ticks, from the held spear. */
    private static int[] stageEnds = {40, 70, 100};
    /** What a remote wielder's stages are assumed to last, in ticks, until the next news. */
    private static final float REMOTE_STAGE = 30f;
    /** Entity id to {stage, game time it was told}. */
    private static final Map<Integer, long[]> REMOTE = new HashMap<>();

    private SpearClient() {
    }

    public static void setStage(int entity, int stage) {
        var level = Minecraft.getInstance().level;
        if (stage == 0 || level == null)
            REMOTE.remove(entity);
        else
            REMOTE.put(entity, new long[] {stage, level.getGameTime()});
    }

    /** The stage {@code entity} is drawn in. */
    public static int stage(Entity entity) {
        var mc = Minecraft.getInstance();
        if (entity == mc.player)
            return stage;
        long[] told = REMOTE.get(entity.getId());
        return told == null || entity.level().getGameTime() - told[1] > REMOTE_LIFE ? 0 : (int) told[0];
    }

    /**
     * Where the charge is, for the pose: {raise, tired, fade}, each 0 to 1. The raise brings the spear up to level in
     * the first few ticks; tired grows through the second stage (the arm droops and wobbles); fade grows through the
     * third (the spear falls and trembles) until the charge ends.
     */
    public static float[] phase(Entity entity, float partial) {
        int s = stage(entity);
        if (s == 0)
            return new float[] {0, 0, 0};
        if (entity == Minecraft.getInstance().player) {
            float t = ticks + partial;
            return new float[] {Mth.clamp(t / 6f, 0f, 1f), Mth.clamp((t - stageEnds[0]) / (stageEnds[1] - stageEnds[0]), 0f, 1f),
                    Mth.clamp((t - stageEnds[1]) / (stageEnds[2] - stageEnds[1]), 0f, 1f)};
        }
        long[] told = REMOTE.get(entity.getId());
        float in = told == null ? 0 : entity.level().getGameTime() - told[1] + partial;
        return new float[] {s == 1 ? Mth.clamp(in / 6f, 0f, 1f) : 1f, s == 2 ? Mth.clamp(in / REMOTE_STAGE, 0f, 1f) : s == 3 ? 1f : 0f,
                s == 3 ? Mth.clamp(in / REMOTE_STAGE, 0f, 1f) : 0f};
    }

    /** The backport's stab, a thrust along the look and a short return, for a swing of progress {@code swing}. */
    static void stab(PoseStack pose, float swing) {
        float g = -(Mth.cos(Mth.PI * Mth.clamp(swing / 0.05f, 0f, 1f)) - 1f) / 2f;
        float h = Mth.clamp((swing - 0.05f) / 0.15f, 0f, 1f);
        h *= h;
        float j = Mth.clamp((swing - 0.4f) / 0.6f, 0f, 1f);
        j = j < 0.5f ? (j == 0f ? 0f : (float) Math.pow(2, 20 * j - 10) / 2f) : (j == 1f ? 1f : (float) (2 - Math.pow(2, -20 * j + 10)) / 2f);
        pose.translate(j * 0.1f * (g - h), -0.075f * (g - j), 0.65f * (g - h));
        pose.mulPose(Axis.XP.rotationDegrees(-70f * (g - j)));
        pose.translate(0, 0, -0.25f * (j - h));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ItemStack weapon = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        var params = SpearCharge.of(weapon);
        boolean want = params.isPresent() && mc.screen == null && mc.mouseHandler.isMouseGrabbed()
                && mc.options.keyUse.isDown() && player.isAlive() && !player.isSpectator() && !player.isUsingItem()
                && !ModularStacks.broken(weapon);
        if (!want) {
            if (sent)
                PacketDistributor.sendToServer(new SpearChargePayload(false));
            sent = false;
            spent = false;
            ticks = -1;
            stage = 0;
            return;
        }
        if (spent)
            return;
        if (!sent || ++beat >= BEAT) {
            PacketDistributor.sendToServer(new SpearChargePayload(true));
            sent = true;
            beat = 0;
        }
        ticks++;
        int[] stages = SpearCharge.stages(weapon, params.get());
        stageEnds = stages;
        if (ticks >= stages[2]) {
            spent = true;
            ticks = -1;
            stage = 0;
        } else {
            stage = ticks < stages[0] ? 1 : ticks < stages[1] ? 2 : 3;
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        REMOTE.clear();
        ticks = -1;
        stage = 0;
        sent = false;
        spent = false;
    }

    /** The spear's client item extension: the pose in both views. */
    public static final class Extension implements IClientItemExtensions {
        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
            if (io.github.soundgoodizerfan.feedback.modular.ModularFaces.combat(stack) != io.github.soundgoodizerfan.feedback.modular.ModularType.Family.SPEAR)
                return null;
            return hand == InteractionHand.MAIN_HAND && stage(entity) > 0 ? HumanoidModel.ArmPose.valueOf("FEEDBACK_SPEAR") : null;
        }

        @Override
        public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                               float partialTick, float equip, float swing) {
            // Only a spear's face stabs and braces; the one hafted item is every other tool too.
            if (io.github.soundgoodizerfan.feedback.modular.ModularFaces.combat(stack) != io.github.soundgoodizerfan.feedback.modular.ModularType.Family.SPEAR)
                return false;
            int s = stage(player);
            int side = arm == HumanoidArm.RIGHT ? 1 : -1;
            if (s == 0) {
                // A swing is a stab, not vanilla's arc: the spear goes out along the look and comes back.
                if (swing <= 0)
                    return false;
                pose.translate(side * 0.56f, -0.52f - 0.6f * equip, -0.72f);
                stab(pose, swing);
                return true;
            }
            float[] ph = phase(player, partialTick);
            float t = player.tickCount + partialTick;
            float raise = ph[0] * ph[0] * (3 - 2 * ph[0]), tired = ph[1], fade = ph[2];
            // Braced low at the hip and level while engaged; the arm tires (droops, wobbles), then gives out (falls, shakes).
            float x = 0.5f, y = -0.5f - 0.35f * (1 - raise) - 0.12f * tired - 0.3f * fade, z = -0.85f + 0.2f * fade;
            float wobble = 0.01f + 0.03f * tired + 0.05f * fade;
            x += Mth.sin(t * 1.7f) * wobble;
            y += Mth.sin(t * 2.3f) * wobble + Mth.sin(t * 4.1f) * 0.03f * fade;
            pose.translate(side * x, y - 0.6f * equip, z);
            // The handheld sprite stands tip-up; bracing tips it forward to point along the look, drooping as the arm tires.
            pose.mulPose(Axis.XP.rotationDegrees(30f * (1 - raise) - 75f * raise + 10f * tired + 25f * fade));
            pose.mulPose(Axis.ZP.rotationDegrees(side * Mth.sin(t * 3.1f) * (1f + 5f * tired + 8f * fade)));
            return true;
        }
    }
}
