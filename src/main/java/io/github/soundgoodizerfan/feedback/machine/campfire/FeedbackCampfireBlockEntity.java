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

import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.FireSpan;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.machine.skillet.SkilletBlock;
import io.github.soundgoodizerfan.feedback.process.Cooking;

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

    public FeedbackCampfireBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    private boolean lit() {
        return CampfireBlock.isLitCampfire(getBlockState());
    }

    @Override
    public Tu getFireTu() {
        return lit() && getBlockState().getBlock() instanceof FeedbackCampfireBlock b ? b.flame() : AmbientTable.at(level);
    }

    @Override
    public FireSpan catchUp(long gap) {
        return lit() ? new FireSpan(gap, getFireTu().value()) : FireSpan.NONE;
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
            getItems().set(i, ItemStack.EMPTY);
            if (!player.getInventory().add(stack))
                player.drop(stack, false);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            setChanged();
            return;
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FeedbackCampfireBlockEntity fire) {
        float target = fire.getFireTu().value();
        boolean changed = false;
        for (int i = 0; i < fire.getItems().size(); i++) {
            ItemStack in = fire.getItems().get(i);
            if (in.isEmpty())
                continue;
            fire.tu[i] = SkilletBlock.warm(fire.tu[i], target);
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        int[] bits = new int[tu.length];
        for (int i = 0; i < bits.length; i++)
            bits[i] = Float.floatToIntBits(tu[i]);
        tag.putIntArray("Tus", bits);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int[] bits = tag.getIntArray("Tus");
        for (int i = 0; i < tu.length && i < bits.length; i++)
            tu[i] = Float.intBitsToFloat(bits[i]);
    }
}
