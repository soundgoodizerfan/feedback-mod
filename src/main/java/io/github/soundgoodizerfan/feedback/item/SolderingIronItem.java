// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
 * The hand route to a good joint: attention where the reflow oven is capital.
 *
 * <h2>How it works</h2>
 * Click a fire (any {@link HeatSource}) to heat it; the iron holds the heat by {@link ItemHeat} and
 * cools slowly ({@link ItemTuning#IRON_COOLING_TU_PER_TICK}). Right-click with a pasted board in the
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
    private record Session(long time, float startTu, InteractionHand hand, boolean braced, boolean lift) {}

    // shortcut: sessions live server side in memory, not on the stack; a restart mid-drag just discards that drag. Upgrade if a drag must survive a relog.
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    public SolderingIronItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public TuRate coolingRate(ItemStack stack) {
        return ItemTuning.IRON_COOLING_TU_PER_TICK;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !(level.getBlockEntity(context.getClickedPos()) instanceof HeatSource fire))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            float reached = Math.min(ItemTuning.IRON_MAX_TU, fire.getFireTu().value());
            if (reached > ItemHeat.get(stack, level).value())
                ItemHeat.set(stack, new Tu(reached), level);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static ItemStack boardIn(Player player, InteractionHand hand) {
        return player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }

    /** The board a circuit was soldered from, when it still has solder to melt (a finished circuit that remembers its parts). */
    private static Board reworkable(ItemStack held) {
        Circuit circuit = Circuit.of(held);
        Board board = circuit.board().orElse(null);
        return circuit.live() && board != null && board.solder().isPresent() && !board.parts().isEmpty() ? board : null;
    }

    /** The first wick in the pack: lifting a part spends it. */
    private static ItemStack wickIn(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(io.github.soundgoodizerfan.feedback.registry.FItems.WICK.get()))
                return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack iron = player.getItemInHand(hand);
        ItemStack held = boardIn(player, hand);
        Board board = held.get(FDataComponents.BOARD.get());
        boolean lift = false;
        if (board == null || board.solder().isEmpty()) {
            // A finished circuit that remembers its board: the same drag, with a wick, lifts its parts off.
            board = reworkable(held);
            if (board == null)
                return InteractionResultHolder.pass(iron);
            if (wickIn(player).isEmpty()) {
                if (!level.isClientSide)
                    player.displayClientMessage(Component.translatable("feedback.circuit.lift.no_wick"), true);
                return InteractionResultHolder.pass(iron);
            }
            lift = true;
        }
        if (level.isClientSide) {
            io.github.soundgoodizerfan.feedback.client.minigame.SolderScreen.open(board, ItemHeat.get(iron, level).value(), lift);
        } else {
            SESSIONS.put(player.getUUID(), new Session(level.getGameTime(), ItemHeat.get(iron, level).value(), hand, player.isShiftKeyDown(), lift));
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
        Board data = session.lift() ? reworkable(board) : board.get(FDataComponents.BOARD.get());
        if (!(iron.getItem() instanceof SolderingIronItem) || data == null || data.solder().isEmpty())
            return;
        ItemStack wick = session.lift() ? wickIn(player) : ItemStack.EMPTY;
        if (session.lift() && wick.isEmpty())
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
            if (pad <= last || pad >= pads || d < 0 || d > ItemTuning.SOLDER_PAD_MAX_DWELL_TICKS)
                return;
            last = pad;
            total += d;
        }
        if (total > budget)
            return;
        Soldering.Pass pass = new Soldering.Pass(pads, session.startTu(), alloy.get(), Soldering.limit(data, materials));
        for (int i = 0; i < n; i++) {
            // Leaving with the button down is lingering on the last pad, capped.
            int linger = p.held() && i == n - 1 ? ItemTuning.SOLDER_LEAVE_LINGER_TICKS : 0;
            pass.touch(p.pads().get(i), p.dwell().get(i) + linger);
            if (p.offset().get(i) > io.github.soundgoodizerfan.feedback.combat.skill.Skills.solderTolerance(player, session.braced()) * 100f)
                pass.bridge(p.pads().get(i));
        }
        if (pass.ironTu() < ItemHeat.get(iron, level).value())
            ItemHeat.set(iron, new Tu(pass.ironTu()), level);
        if (session.lift()) {
            finishLift(player, board, wick, solder, materials, pass);
            return;
        }
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

    /**
     * The rework drag's result: parts whose two pads were wet come free (one wick use each), the board comes back bare of solder, a part that
     * stayed under its kind's limit comes back as the item that was mounted, a scorched pad lifts the traces and the circuit is gone.
     */
    private static void finishLift(ServerPlayer player, ItemStack held, ItemStack wick, Material solder,
                                   Map<ResourceLocation, Material> materials, Soldering.Pass pass) {
        Level level = player.level();
        Soldering.Lift lift = Soldering.lift(Circuit.of(held), materials, pass);
        Fumes.release(level, player.position(), Fumes.level(solder));
        if (lift.traceLost()) {
            player.displayClientMessage(Component.translatable("feedback.circuit.lifted"), true);
            held.shrink(1);
            give(player, lift.board());
            return;
        }
        if (lift.lifted() == 0) {
            player.displayClientMessage(Component.translatable("feedback.circuit.lift.none"), true);
            return;
        }
        wick.hurtAndBreak(lift.lifted(), (net.minecraft.server.level.ServerLevel) level, player, broken -> {});
        player.displayClientMessage(Component.translatable("feedback.circuit.lift.result", lift.lifted(), lift.parts().size()), true);
        held.shrink(1);
        give(player, lift.board());
        for (ItemStack part : lift.parts())
            give(player, part);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack))
            player.drop(stack, false);
    }
}
