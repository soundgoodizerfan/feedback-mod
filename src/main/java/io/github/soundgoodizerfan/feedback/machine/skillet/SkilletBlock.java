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
package io.github.soundgoodizerfan.feedback.machine.skillet;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.net.SkilletFinishPayload;
import io.github.soundgoodizerfan.feedback.net.SkilletOpenPayload;
import io.github.soundgoodizerfan.feedback.process.Cooking;
import io.github.soundgoodizerfan.feedback.process.FoodState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A pan on a fire (food spec 6). Right-click it holding a food and a minigame opens: the player sets
 * how much of the fire the pan gets, and the food's own heat lags behind the pan's (a control game
 * in the enchanting table's genre). The server replays the heat settings through the same
 * {@link Cooking} ladder a vessel runs, so the screen never decides.
 *
 * <p>shortcut: every food lags by the same {@link FTuning#SKILLET_LAG_TICKS}; make it follow the
 * food's mass when food has one. The pan itself answers a setting at once.
 */
public class SkilletBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 3, 15);

    /** Who has the screen open, and since when: a finished path cannot have taken longer than this. */
    private record Session(BlockPos pos, long time, float fireTu, net.minecraft.world.item.Item item) {
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    public SkilletBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** The pan's heat at a setting from 0 (off the fire) to 1 (full on it). */
    public static float panTu(float fireTu, float ambientTu, float setting) {
        return ambientTu + Math.min(1f, Math.max(0f, setting)) * (fireTu - ambientTu);
    }

    /** One tick of the food's own heat closing on the pan's: the lag the player steers against. Shared by screen and server. */
    public static float warm(float foodTu, float panTu) {
        return foodTu + (panTu - foodTu) / FTuning.SKILLET_LAG_TICKS;
    }

    public static void endSession(UUID player) {
        SESSIONS.remove(player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || Cooking.find(level, held).isEmpty())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;
        float fire = HeatSource.below(level, pos).value();
        if (fire <= FTuning.FOOD_COOK_TU.value()) {
            player.displayClientMessage(Component.translatable("feedback.skillet.cold"), true);
            return ItemInteractionResult.CONSUME;
        }
        SESSIONS.put(player.getUUID(), new Session(pos, level.getGameTime(), fire, held.getItem()));
        PacketDistributor.sendToPlayer((ServerPlayer) player, new SkilletOpenPayload(fire, held.copyWithCount(1)));
        return ItemInteractionResult.CONSUME;
    }

    /**
     * Replay a finished path (server). Refuses, silently and without consuming anything, a path with
     * no open session, a player who has walked away, too many samples, a setting outside 0 to 100, or more
     * ticks than have passed since the screen opened.
     */
    public static void finish(ServerPlayer player, SkilletFinishPayload p) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null)
            return;
        Level level = player.level();
        int n = p.settings().size();
        if (n == 0 || n > FTuning.SKILLET_MAX_SAMPLES || !(level.getBlockState(session.pos()).getBlock() instanceof SkilletBlock)
                || player.distanceToSqr(session.pos().getCenter()) > 64
                || (long) n * FTuning.SKILLET_SAMPLE_TICKS > level.getGameTime() - session.time() + 5
                || level.getGameTime() - session.time() > (long) FTuning.SKILLET_MAX_SAMPLES * FTuning.SKILLET_SAMPLE_TICKS + 200)
            return;
        ItemStack held = player.getMainHandItem();
        if (held.getItem() != session.item() || Cooking.find(level, held).isEmpty())
            return;
        // The fire may have died since the screen opened; never grade against more heat than is there now.
        float fireTu = Math.min(session.fireTu(), HeatSource.below(level, session.pos()).value());
        for (int s : p.settings())
            if (s < 0 || s > 100)
                return;
        float ambient = AmbientTable.at(level).value();
        float food = ambient;
        ItemStack piece = held.copyWithCount(1);
        for (int s : p.settings()) {
            float pan = panTu(fireTu, ambient, s / 100f);
            for (int t = 0; t < FTuning.SKILLET_SAMPLE_TICKS; t++) {
                food = warm(food, pan);
                piece = Cooking.tick(piece, Cooking.find(level, piece).orElseThrow(), new Tu(food), level);
            }
        }
        Cooking.Entry entry = Cooking.find(level, piece).orElseThrow();
        FoodState end = Cooking.state(piece, entry);
        Cooking.apply(piece, entry, end);
        // Show the result at the pan: a sizzle and a puff, black smoke if it burnt.
        if (level instanceof ServerLevel sl) {
            boolean burnt = Cooking.doneness(entry, end) == Cooking.Doneness.CHARRED;
            double x = session.pos().getX() + 0.5, y = session.pos().getY() + 0.25, z = session.pos().getZ() + 0.5;
            sl.playSound(null, session.pos(), SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.4f);
            sl.sendParticles(burnt ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, burnt ? 8 : 3, 0.15, 0.05, 0.15, 0.01);
            sl.sendParticles(ParticleTypes.SMALL_FLAME, x, y, z, 4, 0.2, 0.02, 0.2, 0.01);
        }
        held.shrink(1);
        if (!player.getInventory().add(piece))
            player.drop(piece, false);
    }
}
