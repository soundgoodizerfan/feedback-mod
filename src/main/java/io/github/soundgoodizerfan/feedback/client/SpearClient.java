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
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
            return hand == InteractionHand.MAIN_HAND && stage(entity) > 0 ? HumanoidModel.ArmPose.valueOf("FEEDBACK_SPEAR") : null;
        }

        @Override
        public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                               float partialTick, float equip, float swing) {
            int s = stage(player);
            if (s == 0)
                return false;
            int side = arm == HumanoidArm.RIGHT ? 1 : -1;
            float t = player.tickCount + partialTick;
            float x = 0.5f, y = -0.5f, z = -0.85f;
            if (s == 2) {
                x += Mth.sin(t * 1.7f) * 0.02f;
                y += Mth.sin(t * 2.3f) * 0.02f;
            } else if (s == 3) {
                y -= 0.3f;
                z += 0.2f;
            }
            pose.translate(side * x, y - 0.6f * equip, z);
            if (s == 3)
                pose.mulPose(Axis.XP.rotationDegrees(25));
            return true;
        }
    }
}
