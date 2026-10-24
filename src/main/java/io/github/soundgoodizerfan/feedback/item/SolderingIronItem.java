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
package io.github.soundgoodizerfan.feedback.item;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Fumes;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.TuRate;
import io.github.soundgoodizerfan.feedback.process.Board;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.net.SolderFinishPayload;
import io.github.soundgoodizerfan.feedback.process.Circuit;
import io.github.soundgoodizerfan.feedback.process.Soldering;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The hand route to a good joint (mechanics 6.20, 6.22): attention where the reflow oven is capital.
 *
 * <h2>How it works</h2>
 * Click a fire (any {@link HeatSource}) to heat it; the iron holds the heat by {@link ItemHeat} and
 * cools slowly ({@link FTuning#IRON_COOLING_TU_PER_TICK}). Right-click with a pasted board in the
 * other hand: a soldering screen opens (client, {@code SolderScreen}) and the server notes when and
 * how hot. The player drags the tip along the board's pad row; pace is the skill. The one packet at
 * the end carries the samples and the server replays them through {@link Soldering.Pass}, never
 * trusting a grade from the client. Too fast leaves a pad cold, too slow lifts the trace, a shaky
 * hand bridges; the solder is spent either way and the board is reworked, never refused.
 *
 * <h2>Sense and stop</h2>
 * Sense: the pad colours on the screen and a thermometer on the iron. Stop: lift the button. Toxic
 * solder fumes when the screen opens and when the joint is set.
 *
 * <h2>Wear</h2>
 * None: the iron is used, not worn. Misuse costs boards.
 */
public class SolderingIronItem extends Item implements ItemHeat.Cooling {

    /** When and how hot a drag began, and which hand holds the iron. */
    private record Session(long time, float startTu, InteractionHand hand) {}

    // shortcut: sessions live server side in memory, not on the stack; a restart mid-drag just discards that drag. Upgrade if a drag must survive a relog.
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    public SolderingIronItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public TuRate coolingRate(ItemStack stack) {
        return FTuning.IRON_COOLING_TU_PER_TICK;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !(level.getBlockEntity(context.getClickedPos()) instanceof HeatSource fire))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            float reached = Math.min(FTuning.IRON_MAX_TU, fire.getFireTu().value());
            if (reached > ItemHeat.get(stack, level).value())
                ItemHeat.set(stack, new Tu(reached), level);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static ItemStack boardIn(Player player, InteractionHand hand) {
        return player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack iron = player.getItemInHand(hand);
        Board board = boardIn(player, hand).get(FDataComponents.BOARD.get());
        if (board == null || board.solder().isEmpty())
            return InteractionResultHolder.pass(iron);
        if (level.isClientSide) {
            io.github.soundgoodizerfan.feedback.client.SolderScreen.open(board, ItemHeat.get(iron, level).value());
        } else {
            SESSIONS.put(player.getUUID(), new Session(level.getGameTime(), ItemHeat.get(iron, level).value(), hand));
            Material solder = MaterialTable.get().materials().get(board.solder().get());
            if (solder != null)
                Fumes.release(level, player.position(), Fumes.level(solder));
        }
        return InteractionResultHolder.sidedSuccess(iron, level.isClientSide);
    }

    /**
     * Replay a finished drag (server). Rejects, silently and without consuming anything, a drag with
     * no open session, a changed hand, more samples than pads, out-of-range or repeated pads, a pad
     * worked longer than its cap, or more total dwell than the time since the screen opened.
     */
    public static void finishDrag(ServerPlayer player, SolderFinishPayload p) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null)
            return;
        Level level = player.level();
        ItemStack iron = player.getItemInHand(session.hand());
        ItemStack board = boardIn(player, session.hand());
        Board data = board.get(FDataComponents.BOARD.get());
        if (!(iron.getItem() instanceof SolderingIronItem) || data == null || data.solder().isEmpty())
            return;
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        Material solder = materials.get(data.solder().get());
        java.util.Optional<Soldering.Alloy> alloy = solder == null ? java.util.Optional.empty() : Soldering.alloy(solder);
        int pads = Soldering.pads(data), n = p.pads().size();
        if (alloy.isEmpty() || n > pads || p.dwell().size() != n || p.offset().size() != n)
            return;
        long budget = level.getGameTime() - session.time() + 5;
        long total = 0;
        int last = -1;
        for (int i = 0; i < n; i++) {
            int pad = p.pads().get(i), d = p.dwell().get(i);
            if (pad <= last || pad >= pads || d < 0 || d > FTuning.SOLDER_PAD_MAX_DWELL_TICKS)
                return;
            last = pad;
            total += d;
        }
        if (total > budget)
            return;
        Soldering.Pass pass = new Soldering.Pass(pads, session.startTu(), alloy.get(), Soldering.limit(data, materials));
        for (int i = 0; i < n; i++) {
            // Leaving with the button down is lingering on the last pad, capped.
            int linger = p.held() && i == n - 1 ? FTuning.SOLDER_LEAVE_LINGER_TICKS : 0;
            pass.touch(p.pads().get(i), p.dwell().get(i) + linger);
            if (p.offset().get(i) > FTuning.SOLDER_BRIDGE_OFFSET_PITCHES * 100f)
                pass.bridge(p.pads().get(i));
        }
        if (pass.ironTu() < ItemHeat.get(iron, level).value())
            ItemHeat.set(iron, new Tu(pass.ironTu()), level);
        ItemStack result = Soldering.finish(board, materials, pass);
        Board graded = result.get(FDataComponents.BOARD.get());
        Circuit made = Circuit.of(result);
        player.displayClientMessage(made.live() ? Component.translatable("feedback.board.result", Component.translatable(made.joint().translationKey()))
                : result.has(FDataComponents.CIRCUIT.get()) ? Component.translatable("feedback.circuit.lifted")
                : graded == null ? Component.translatable("feedback.board.lifted")
                : Component.translatable("feedback.board.result", Component.translatable(graded.joint().translationKey())), true);
        Fumes.release(level, player.position(), Fumes.level(solder));
        board.shrink(1);
        if (!result.isEmpty() && !player.getInventory().add(result))
            player.drop(result, false);
    }
}
