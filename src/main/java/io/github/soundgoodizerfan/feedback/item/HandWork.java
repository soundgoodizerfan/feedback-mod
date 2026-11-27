// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.machine.vice.ViceBlockEntity;
import io.github.soundgoodizerfan.feedback.modular.item.ModularHandTool;
import io.github.soundgoodizerfan.feedback.process.DeformationTable;
import io.github.soundgoodizerfan.feedback.process.Deforming;
import io.github.soundgoodizerfan.feedback.process.Removing;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Hand work by holding use: a hand tool (hammer, file, drill) in one hand and one workpiece in the other.
 *
 * <p>Holding right click takes the workpiece into the tool ({@code HAND_WORK}) and lands one stroke every
 * {@link ItemTuning#HAND_STROKE_TICKS} ticks, each the same blow a machine lands ({@link Deforming#apply} or
 * {@link Removing#apply}: work per stroke is St over the material's hardness). It runs on while strokes land, past
 * done (a plate under a hammer goes on thinning), and stops when one cannot (steel too hard, an ingot gone cold, a
 * piece with nowhere left to go). Letting go, swapping away or being killed hands the piece back as it is, and costs the
 * tool nothing; the tool wears one point per stroke that landed.
 *
 * <p>The mechanism is read from Create's sandpaper (Creators-of-Create/Create, MIT, mc1.21.1/dev): the in-progress
 * piece rides in a data component on the tool so a release gives it back whole, and the server alone changes the piece.
 * Rejected there: its fixed 32-tick duration and flat durability (here the stroke count and wear come from the
 * material's figures), the {@code LivingEntity} mixin (the particles and noise are sent from the server per stroke),
 * the recipe-type lookup by input stack (the deformation and removal tables answer, by property) and item-entity pickup.
 */
public final class HandWork {

    private HandWork() {
    }

    /** What one stroke of {@code tool} makes of a piece held in the other hand, or null when no stroke could land (nothing is spent). */
    @Nullable
    public static ItemStack stroke(ItemStack tool, ItemStack workpiece, Level level) {
        return stroke(tool, workpiece, level, ItemTuning.HAND_WOBBLE);
    }

    /**
     * What one stroke of {@code tool} makes of {@code workpiece}, or null when no stroke could land (nothing is spent). {@code wobble} is the share
     * of the stroke lost to a piece that shifts: {@link ItemTuning#HAND_WOBBLE} held in the hand, the play of the screw in a vice.
     */
    @Nullable
    public static ItemStack stroke(ItemStack tool, ItemStack workpiece, Level level, float wobble) {
        if (!(tool.getItem() instanceof HandTool handTool) || workpiece.isEmpty() || level == null)
            return null;
        float held = Math.max(0f, 1f - wobble);
        // A hand file or drill removes material: the same cut a machine makes, with the tool's own edge hardness as the gate.
        var op = handTool.getOperation(tool);
        if (op.removesMaterial()) {
            Removing.Cut cut = Removing.apply(workpiece, new io.github.soundgoodizerfan.feedback.core.unit.St(ModularHandTool.cutDrive(tool, op).value() * held),
                    ModularHandTool.cutHardness(tool), op, 1f, 0f, level);
            return cut.landed() ? cut.result() : null;
        }
        // Nowhere to put what the work drives out (a bloom's slag): that is the anvil's shingling, not a swing in the hand.
        var entry = DeformationTable.get().find(workpiece, handTool.getOperation(tool));
        if (entry.isPresent() && !entry.get().byproduct().isEmpty())
            return null;
        Deforming.Blow blow = Deforming.apply(workpiece, new io.github.soundgoodizerfan.feedback.core.unit.St(handTool.getStrength(tool).value() * held),
                level, handTool.getOperation(tool));
        return blow.landed() ? blow.result() : null;
    }

    /** Use: take one piece from the other hand into the tool, if a stroke could land on it. */
    public static InteractionResultHolder<ItemStack> begin(Level level, Player player, InteractionHand hand) {
        ItemStack tool = player.getItemInHand(hand);
        if (!(tool.getItem() instanceof HandTool) || working(tool))
            return InteractionResultHolder.pass(tool);
        // A piece clamped in the vice the player is looking at is worked where it sits: the tool is in one hand and the other stays free.
        ViceBlockEntity vice = viceLookedAt(level, player);
        if (vice != null && vice.clamps() && !vice.getWorkpiece().isEmpty()) {
            if (stroke(tool, vice.getWorkpiece(), level, vice.wobble()) == null)
                return InteractionResultHolder.pass(tool);
            if (!level.isClientSide)
                tool.set(FDataComponents.HAND_VICE, vice.getBlockPos());
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(tool);
        }
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        // Tongs in the other hand hold the piece: it is worked where it sits, between the jaws, and nothing moves.
        if (other.getItem() instanceof TongsItem) {
            ItemStack held = other.get(FDataComponents.TONGS_HELD);
            if (held == null || held.isEmpty() || stroke(tool, held, level) == null)
                return InteractionResultHolder.pass(tool);
            if (!level.isClientSide)
                tool.set(FDataComponents.HAND_TONGS, true);
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(tool);
        }
        if (other.isEmpty() || other.getItem() instanceof HandTool)
            return InteractionResultHolder.pass(tool);
        ItemStack one = other.copyWithCount(1);
        if (stroke(tool, one, level) == null)
            return InteractionResultHolder.pass(tool);
        if (!level.isClientSide) {
            other.shrink(1);
            tool.set(FDataComponents.HAND_WORK, one);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(tool);
    }

    /** Whether a piece is held in {@code tool} for work, or a vice's piece is being worked. */
    public static boolean working(ItemStack tool) {
        return tool.has(FDataComponents.HAND_WORK) || tool.has(FDataComponents.HAND_VICE) || tool.has(FDataComponents.HAND_TONGS);
    }

    /** The tongs in the hand that is not using {@code tool}, or null. */
    @Nullable
    public static ItemStack tongsBeside(Player player) {
        ItemStack other = player.getItemInHand(player.getUsedItemHand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        return other.getItem() instanceof TongsItem ? other : null;
    }

    /** The vice the player's crosshair is on, if any. */
    @Nullable
    private static ViceBlockEntity viceLookedAt(Level level, Player player) {
        HitResult hit = player.pick(player.blockInteractionRange(), 1f, false);
        if (hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK
                && level.getBlockEntity(block.getBlockPos()) instanceof ViceBlockEntity vice)
            return vice;
        return null;
    }

    /** Use time: runs until it cannot land a stroke or is let go. */
    public static int useDuration() {
        return 72000;
    }

    public static UseAnim animation() {
        return UseAnim.BRUSH;
    }

    /** Each use tick: every {@link ItemTuning#HAND_STROKE_TICKS}th lands a stroke, on the server only. */
    public static void tick(Level level, LivingEntity user, ItemStack tool, int remaining) {
        if (level.isClientSide || !(user instanceof Player player) || !working(tool))
            return;
        int used = useDuration() - remaining;
        if (used <= 0 || used % ItemTuning.HAND_STROKE_TICKS != 0)
            return;
        BlockPos vicePos = tool.get(FDataComponents.HAND_VICE);
        ViceBlockEntity vice = null;
        ItemStack piece;
        ItemStack tongs = null;
        float wobble = ItemTuning.HAND_WOBBLE;
        if (vicePos != null) {
            // Worked in the vice: the piece stays clamped; the stroke stops when the jaws are open, emptied or out of reach.
            vice = level.getBlockEntity(vicePos) instanceof ViceBlockEntity v ? v : null;
            if (vice == null || !vice.clamps() || vice.getWorkpiece().isEmpty() || player.blockPosition().distSqr(vicePos) > 36) {
                player.stopUsingItem();
                return;
            }
            piece = vice.getWorkpiece();
            wobble = vice.wobble();
        } else if (tool.has(FDataComponents.HAND_TONGS)) {
            // Worked in the tongs: the stroke stops when they are gone or empty.
            tongs = tongsBeside(player);
            piece = tongs == null ? null : tongs.get(FDataComponents.TONGS_HELD);
            if (piece == null || piece.isEmpty()) {
                player.stopUsingItem();
                return;
            }
        } else
            piece = tool.get(FDataComponents.HAND_WORK);
        ItemStack next = stroke(tool, piece, level, wobble);
        if (next == null) {
            player.stopUsingItem();
            return;
        }
        HandTool handTool = (HandTool) tool.getItem();
        if (vice != null)
            vice.setWorkpiece(next);
        else if (tongs != null)
            tongs.set(FDataComponents.TONGS_HELD, next);
        else
            tool.set(FDataComponents.HAND_WORK, next);
        player.causeFoodExhaustion(ItemTuning.HAND_SWING_EXHAUSTION_PER_ST * Math.max(1f, handTool.getStrength(tool).value())
                * io.github.soundgoodizerfan.feedback.combat.skill.Skills.handWorkShare(player));
        level.playSound(null, player.blockPosition(), handTool.getWorkSound(), SoundSource.PLAYERS, 0.3f, 1.5f + level.random.nextFloat() * 0.2f);
        if (level instanceof ServerLevel server)
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, piece), player.getX() + player.getLookAngle().x * 0.6,
                    player.getEyeY() - 0.3, player.getZ() + player.getLookAngle().z * 0.6, 3, 0.1, 0.1, 0.1, 0.05);
        // The tool survives the stroke one point worse; a tool with no point left goes (a modular one is broken, not gone).
        ItemStack worn = tool.getItem().getCraftingRemainingItem(tool);
        if (worn.isEmpty()) {
            giveBack(player, tool);
            tool.shrink(1);
            player.stopUsingItem();
        } else
            tool.setDamageValue(worn.getDamageValue());
    }

    /** Let go: the piece comes back as it is, the tool unharmed. */
    public static void release(ItemStack tool, LivingEntity user) {
        if (user instanceof Player player && !player.level().isClientSide)
            giveBack(player, tool);
    }

    /** A tool carrying a piece that is not in use (logged out mid-stroke, dropped, swapped away): the piece goes back to its owner. */
    public static void sweep(ItemStack tool, Entity holder) {
        if (holder.level().isClientSide || !(holder instanceof Player player) || !working(tool))
            return;
        if (player.isUsingItem() && player.getUseItem() == tool)
            return;
        giveBack(player, tool);
    }

    private static void giveBack(Player player, ItemStack tool) {
        // A vice's piece never left the vice: letting go only forgets which vice.
        tool.remove(FDataComponents.HAND_VICE);
        tool.remove(FDataComponents.HAND_TONGS);
        ItemStack piece = tool.remove(FDataComponents.HAND_WORK);
        if (piece == null || piece.isEmpty())
            return;
        if (player.getOffhandItem().isEmpty() && player.getMainHandItem() == tool)
            player.setItemInHand(InteractionHand.OFF_HAND, piece);
        else if (player.getMainHandItem().isEmpty() && player.getOffhandItem() == tool)
            player.setItemInHand(InteractionHand.MAIN_HAND, piece);
        else
            player.getInventory().placeItemBackInInventory(piece);
    }
}
