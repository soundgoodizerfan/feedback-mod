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
package io.github.soundgoodizerfan.feedback.machine.campfire;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.FireSpan;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.machine.skillet.SkilletBlock;
import io.github.soundgoodizerfan.feedback.process.Cooking;
import io.github.soundgoodizerfan.feedback.process.ThermalProcess;
import io.github.soundgoodizerfan.feedback.process.ThermalProcessTable;

import java.util.List;

import net.minecraft.core.Direction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The food on a campfire. Extends vanilla's block entity so its four slots, saving, syncing,
 * renderer and drop-on-break all stay; none of vanilla's timed cooking is used. Each slot holds the
 * heat of its own piece, which closes on the flame's (or on ambient when the fire is out) the way
 * food in a pan does, and {@link Cooking#tick} cooks, dries and chars it from that heat.
 * Also the {@link HeatSource} for whatever sits on the fire.
 */
public class FeedbackCampfireBlockEntity extends CampfireBlockEntity implements HeatSource {

    private final float[] tu = new float[4];
    /** Progress of each slot's airless thermal process (TPu), as a vessel keeps it. */
    private final float[] tpu = new float[4];
    /** No air block touches the fire (buried): it runs airless rows, and smoulders cooler. */
    private boolean buried;

    public FeedbackCampfireBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    private boolean lit() {
        return CampfireBlock.isLitCampfire(getBlockState());
    }

    @Override
    public Tu getFireTu() {
        if (!lit() || !(getBlockState().getBlock() instanceof FeedbackCampfireBlock b))
            return AmbientTable.at(level);
        return buried && b.flame().value() > FTuning.FIRE_TU_BURIED_CAMPFIRE.value() ? FTuning.FIRE_TU_BURIED_CAMPFIRE : b.flame();
    }

    @Override
    public FireSpan catchUp(long gap) {
        return lit() ? new FireSpan(gap, getFireTu().value()) : FireSpan.NONE;
    }

    public boolean buried() {
        return buried;
    }

    /** Every face neighbour is solid on the face towards the fire, so no air touches it. */
    private boolean scanBuried() {
        for (Direction d : Direction.values())
            if (!level.getBlockState(worldPosition.relative(d)).isFaceSturdy(level, worldPosition.relative(d), d.getOpposite()))
                return false;
        return true;
    }

    /** Does {@code stack} feed an airless row of its own (logs to charcoal)? */
    static boolean airlessInput(ItemStack stack) {
        return ThermalProcessTable.get().find(List.of(stack), true).filter(ThermalProcess::airless).isPresent();
    }

    public boolean hasFood() {
        return getItems().stream().anyMatch(s -> !s.isEmpty());
    }

    /** Put one of {@code held} on the first free slot, at the heat it already has. */
    boolean put(Player player, ItemStack held) {
        for (int i = 0; i < getItems().size(); i++) {
            if (!getItems().get(i).isEmpty())
                continue;
            tu[i] = ItemHeat.get(held, level).value();
            tpu[i] = 0f;
            placeFood(player, held, 0);
            return true;
        }
        return false;
    }

    /** Hand over the most recently placed piece, carrying its heat. */
    void take(Player player) {
        for (int i = getItems().size() - 1; i >= 0; i--) {
            ItemStack stack = getItems().get(i);
            if (stack.isEmpty())
                continue;
            ItemHeat.set(stack, new Tu(tu[i]), level);
            tpu[i] = 0f;
            getItems().set(i, ItemStack.EMPTY);
            if (!player.getInventory().add(stack))
                player.drop(stack, false);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            setChanged();
            return;
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FeedbackCampfireBlockEntity fire) {
        if (level.getGameTime() % 20 == 0)
            fire.buried = fire.scanBuried();
        float target = fire.getFireTu().value();
        boolean changed = false;
        for (int i = 0; i < fire.getItems().size(); i++) {
            ItemStack in = fire.getItems().get(i);
            if (in.isEmpty())
                continue;
            fire.tu[i] = SkilletBlock.warm(fire.tu[i], target);
            if (fire.buried && fire.advanceAirless(i, in)) {
                changed = true;
                continue;
            }
            var entry = Cooking.findCookable(level, in);
            if (entry.isEmpty())
                continue;
            ItemStack out = Cooking.tick(in, entry.get(), new Tu(fire.tu[i]), level);
            if (out != in) {
                fire.getItems().set(i, out);
                changed = true;
            }
        }
        if (changed) {
            level.sendBlockUpdated(pos, state, state, 3);
            fire.setChanged();
        }
    }

    /** A vessel's advance for one piece, sealed from air; true if the slot changed (finished or spoiled). */
    private boolean advanceAirless(int i, ItemStack in) {
        var found = ThermalProcessTable.get().find(List.of(in), true).filter(ThermalProcess::airless);
        if (found.isEmpty())
            return false;
        ThermalProcess process = found.get();
        Tu now = new Tu(tu[i]);
        if (process.spoilsAt(now)) {
            getItems().set(i, process.spoiled().copy());
            tpu[i] = 0f;
            return true;
        }
        float suit = process.suitability(now);
        if (suit <= 0f) {
            tpu[i] = Math.max(0f, tpu[i] - FTuning.TPU_DECAY_PER_TICK);
            return false;
        }
        tpu[i] += suit;
        if (tpu[i] < process.requiredTpu())
            return false;
        getItems().set(i, process.result().copy());
        tpu[i] = 0f;
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        int[] bits = new int[tu.length];
        for (int i = 0; i < bits.length; i++)
            bits[i] = Float.floatToIntBits(tu[i]);
        tag.putIntArray("Tus", bits);
        int[] progress = new int[tpu.length];
        for (int i = 0; i < progress.length; i++)
            progress[i] = Float.floatToIntBits(tpu[i]);
        tag.putIntArray("Tpus", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int[] bits = tag.getIntArray("Tus");
        for (int i = 0; i < tu.length && i < bits.length; i++)
            tu[i] = Float.intBitsToFloat(bits[i]);
        int[] progress = tag.getIntArray("Tpus");
        for (int i = 0; i < tpu.length && i < progress.length; i++)
            tpu[i] = Float.intBitsToFloat(progress[i]);
    }
}
