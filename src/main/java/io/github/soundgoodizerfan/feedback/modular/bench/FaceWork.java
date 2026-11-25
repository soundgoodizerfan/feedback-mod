// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.bench;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.engineering.ToolAbilities;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;

import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.fitting.Fitting;
import io.github.soundgoodizerfan.feedback.fitting.SidedFitting;
import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * What the pry bar's face and a prospector's pick head do to the block under a right click. Both read the head's baked figures and
 * the block's own properties, never an id:
 * <ul>
 *   <li><b>Pry</b> ({@code pry_head}): a block of the {@code feedback:fastened} tag (held on by nails, pins or a seal) comes off whole,
 *       as the item it is, if the bar's toughness carries the load the block's hardness sets ({@link ModularTuning#PRY_TOUGHNESS_PER_HARDNESS}).
 *       A bar that cannot carry it bends: a point of wear, nothing lifted. The bar wears by the load it carried. It also pries a
 *       fitting off a fittable holder (a sensor on the face, else the last upgrade), as the crowbar it replaced did.</li>
 *   <li><b>Prospect</b> (a {@code pickaxe_head} shaped as the {@code prospector} variant, which also digs and sounds): the block's material is found by its item; each metal it {@code bears} throws glints of
 *       that metal's own colour off the struck face, more where more ingots a piece yields. A block that bears nothing puffs grey dust.</li>
 * </ul>
 */
public final class FaceWork {

    private FaceWork() {
    }

    /** Blocks held on by fastening: nailed, pinned or sealed in. */
    public static final TagKey<Block> FASTENED = TagKey.create(Registries.BLOCK, Feedback.id("fastened"));

    /** The result for a face's kind on this click, or PASS when the tool has no such live face. */
    public static InteractionResult use(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        var pry = ToolAbilities.face(stack, "pry_head");
        if (pry != null)
            return pry(context, stack, pry);
        var pick = ToolAbilities.face(stack, "pickaxe_head");
        if (pick != null && "prospector".equals(pick.part().variant()))
            return prospect(context, stack, pick);
        return InteractionResult.PASS;
    }

    private static InteractionResult pry(UseOnContext context, ItemStack stack, ToolAbilities.Face face) {
        var level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        InteractionResult fitting = prise(context, stack);
        if (fitting != InteractionResult.PASS)
            return fitting;
        BlockState state = level.getBlockState(pos);
        if (!state.is(FASTENED))
            return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server))
            return InteractionResult.sidedSuccess(true);
        var figures = ModularStacks.figures(stack);
        float lever = face.ratio("length");
        float carries = figures == null ? 0f : figures.get(Fig.EDGE_TOUGHNESS) * lever;
        float load = Math.max(0.1f, state.getDestroySpeed(level, pos)) * ModularTuning.PRY_TOUGHNESS_PER_HARDNESS;
        Player player = context.getPlayer();
        int wear = Math.max(1, Math.round(load / ModularTuning.PRY_LOAD_PER_WEAR));
        if (carries < load) {
            level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.6f, 0.6f);
            hurt(stack, player, context, 1);
            return InteractionResult.CONSUME;
        }
        ItemStack lifted = new ItemStack(state.getBlock());
        level.destroyBlock(pos, false, player);
        if (!lifted.isEmpty())
            Block.popResource(server, pos, lifted);
        level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8f, 0.5f);
        hurt(stack, player, context, wear);
        return InteractionResult.CONSUME;
    }

    /**
     * Pries a {@link Fitting} off a {@link Fittable}: the sensor on the clicked face if there is one, otherwise the last upgrade fitted.
     * The fitting drops as its item, as when the holder is broken; a holder that needs the upgrade refuses.
     */
    private static InteractionResult prise(UseOnContext context, ItemStack stack) {
        var level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof Fittable fittable))
            return InteractionResult.PASS;
        Direction side = context.getClickedFace();
        SidedFitting sided = fittable.getSidedFitting(side);
        List<UpgradeFitting> upgrades = fittable.getUpgrades();
        if (sided == null && upgrades.isEmpty())
            return InteractionResult.PASS;
        if (level.isClientSide)
            return InteractionResult.sidedSuccess(true);
        Fitting removed;
        if (sided != null) {
            removed = sided;
            removed.onRemoved();
            fittable.setSidedFitting(side, null);
        } else {
            UpgradeFitting last = upgrades.get(upgrades.size() - 1);
            if (!fittable.canRemove(last)) {
                if (context.getPlayer() != null)
                    context.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.translatable("feedback.fitting.in_use"), true);
                return InteractionResult.CONSUME;
            }
            removed = last;
            removed.onRemoved();
            fittable.removeUpgrade(last);
        }
        Block.popResource(level, context.getClickedPos().relative(side), removed.getPickItem());
        level.playSound(null, context.getClickedPos(), SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.6f, 0.7f);
        hurt(stack, context.getPlayer(), context, 1);
        return InteractionResult.CONSUME;
    }

    private static InteractionResult prospect(UseOnContext context, ItemStack stack, ToolAbilities.Face face) {
        var level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level instanceof ServerLevel server))
            return InteractionResult.sidedSuccess(true);
        Map<ResourceLocation, Material> materials = MaterialIngredient.materialsNow();
        var found = Material.find(materials, new ItemStack(level.getBlockState(pos).getBlock()));
        Vec3 spot = Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(context.getClickedFace().getNormal()).scale(0.52));
        var bears = found.map(f -> f.material().bears()).orElse(Map.of());
        if (bears.isEmpty()) {
            server.sendParticles(ParticleTypes.POOF, spot.x, spot.y, spot.z, 3, 0.15, 0.15, 0.15, 0.01);
            level.playSound(null, pos, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 0.5f, 0.8f);
        } else {
            float point = face.ratio("point");
            float richest = 0f;
            for (var e : bears.entrySet()) {
                var metal = materials.get(e.getKey());
                int rgb = metal == null ? 0xFFFFFF : metal.color().orElse(0xFFFFFF);
                int glints = Math.min(ModularTuning.PROSPECT_MAX_GLINTS, Math.max(1, Math.round(e.getValue() * ModularTuning.PROSPECT_GLINTS_PER_INGOT * point)));
                server.sendParticles(new DustParticleOptions(new org.joml.Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), 1f),
                        spot.x, spot.y, spot.z, glints, 0.22, 0.22, 0.22, 0.0);
                server.sendParticles(ParticleTypes.END_ROD, spot.x, spot.y, spot.z, 1 + glints / 3, 0.18, 0.18, 0.18, 0.01);
                richest = Math.max(richest, e.getValue());
            }
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, 0.7f + Math.min(0.9f, richest * 0.2f));
        }
        float point = face.ratio("point");
        hurt(stack, context.getPlayer(), context, Math.max(1, Math.round(point * point)));
        return InteractionResult.CONSUME;
    }

    private static void hurt(ItemStack stack, Player player, UseOnContext context, int amount) {
        if (player != null)
            stack.hurtAndBreak(amount, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(context.getHand()));
    }
}
