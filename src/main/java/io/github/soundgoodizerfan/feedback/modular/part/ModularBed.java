// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.part;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.item.ModularItem;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;

import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * A bed: a mattress (a cover over fibrous padding in a shell) with an optional frame under it. Built with no frame it is a bedroll, light enough
 * to carry on a pack's lashing. Used on firm ground at night (or in a storm) with nothing hostile near, it skips to morning wherever you are and
 * gives back health by its comfort (the padding's and cover's softness, the mattress's thickness, the frame's lift off the ground), less in
 * cold country by how little its padding keeps the heat in (its warmth). Each night wears it a little.
 * <p>Built with a frame it is placed instead as a two-block bed ({@link ModularBedBlock}) that sets the respawn point like a vanilla bed.
 */
public class ModularBed extends ModularItem {

    /** The mattress and the frame roles' letters in the type file; the mattress's slots are {@code cover}, {@code padding} and {@code shell}. */
    public static final String MATTRESS = "C", FRAME = "F";

    /** Whether {@code stack} is a bed built with no frame: a bedroll, light enough to lash on a pack. */
    public static boolean frameless(ItemStack stack) {
        var design = ModularStacks.design(stack);
        return stack.getItem() instanceof ModularBed && design != null && design.part(FRAME) == null;
    }

    public ModularBed(Properties properties) {
        super(properties);
    }

    /** A framed bed is set down as a two-block bed ({@link ModularBedBlock}) with its design; a bedroll is not placeable. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (frameless(stack))
            return InteractionResult.PASS;
        BlockPlaceContext place = new BlockPlaceContext(context);
        Level level = context.getLevel();
        BlockState state = place.canPlace() ? FModular.PLACED_BED.get().getStateForPlacement(place) : null;
        if (state == null)
            return InteractionResult.FAIL;
        BlockPos pos = place.getClickedPos();
        if (!level.setBlock(pos, state, 26))
            return InteractionResult.FAIL;
        if (!level.isClientSide) {
            state.getBlock().setPlacedBy(level, pos, state, context.getPlayer(), stack);
            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1f, 0.9f);
        }
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild)
            stack.shrink(1);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack roll = player.getItemInHand(hand);
        if (!frameless(roll))
            return InteractionResultHolder.pass(roll);
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp))
            return InteractionResultHolder.pass(roll);
        return rest(sp, roll) ? InteractionResultHolder.success(roll) : InteractionResultHolder.fail(roll);
    }

    /** Beds {@code player} down in {@code roll} (worn a little on success); false with a message when they cannot. */
    public static boolean rest(net.minecraft.server.level.ServerPlayer player, ItemStack roll) {
        if (!(player.level() instanceof ServerLevel server) || ModularStacks.broken(roll))
            return false;
        if (!player.onGround() || !server.dimensionType().natural() || !(server.isNight() || server.isThundering())) {
            player.displayClientMessage(Component.translatable("message.feedback.bed.no_sleep"), true);
            return false;
        }
        AABB near = player.getBoundingBox().inflate(ModularTuning.BED_SAFE_RADIUS, 5, ModularTuning.BED_SAFE_RADIUS);
        if (!server.getEntitiesOfClass(Monster.class, near, m -> m.isPreventingPlayerRest(player)).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.feedback.bed.not_safe"), true);
            return false;
        }
        soothe(player, server, roll);
        // Morning: the next dawn, as a bed would bring it.
        long day = server.getDayTime();
        server.setDayTime(day + 24000L - day % 24000L);
        player.displayClientMessage(Component.translatable("message.feedback.bed.slept"), true);
        return true;
    }

    /** A night's rest in {@code bed}: health by its comfort (less in cold country by its warmth), frost thawed by a warm one, the bed worn a little. */
    public static void soothe(net.minecraft.server.level.ServerPlayer player, ServerLevel server, ItemStack bed) {
        ModularFigures f = ModularStacks.figures(bed);
        // Cold country takes back what the padding does not keep in.
        boolean cold = server.getBiome(player.blockPosition()).value().coldEnoughToSnow(player.blockPosition());
        float comfort = f == null ? 0f : f.get(Fig.COMFORT) * (cold ? f.get(Fig.WARMTH) : 1f);
        comfort = Math.max(0f, comfort + io.github.soundgoodizerfan.feedback.modular.attunement.RestKinds.comfort(bed));
        float warmth = (f == null ? 0f : f.get(Fig.WARMTH)) + io.github.soundgoodizerfan.feedback.modular.attunement.RestKinds.warmth(bed);
        io.github.soundgoodizerfan.feedback.modular.attunement.RestKinds.restore(bed, player,
                player.getMaxHealth() * ModularTuning.BED_HEAL * comfort * io.github.soundgoodizerfan.feedback.modular.attunement.RestKinds.heal(bed, player));
        if (f != null && warmth >= ModularTuning.BED_THAWS_AT)
            player.setTicksFrozen(0);
        player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        io.github.soundgoodizerfan.feedback.modular.attunement.RestKinds.wake(player, server, bed);
        if (!io.github.soundgoodizerfan.feedback.modular.attunement.RestKinds.unworn(bed, player))
            bed.setDamageValue(Math.min(bed.getMaxDamage() - 1, bed.getDamageValue()
                    + io.github.soundgoodizerfan.feedback.modular.attunement.WardKinds.soak(bed, ModularTuning.BED_WEAR)));
        ModularStacks.refresh(bed);
    }
}
