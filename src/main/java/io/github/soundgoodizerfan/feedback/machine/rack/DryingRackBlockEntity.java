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
package io.github.soundgoodizerfan.feedback.machine.rack;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.process.Cooking;
import io.github.soundgoodizerfan.feedback.process.FoodState;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A rack that dries food (food spec 8): moisture leaves slowly, faster in warmer air. A fire under it
 * warms the air up to {@link FTuning#RACK_MAX_AIR_TU}, the edge of cooking, never past: it dries, it
 * does not cook. Rain on an open rack stops the drying. Dry food keeps (rot runs with moisture), so a
 * rack is the patient answer to the ice box.
 *
 * <p>shortcut: no renderer, so the food on it is not drawn; the block says what it holds when used.
 */
public class DryingRackBlockEntity extends BlockEntity {

    private final NonNullList<ItemStack> items = NonNullList.withSize(FTuning.RACK_SLOTS, ItemStack.EMPTY);

    public DryingRackBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.DRYING_RACK.get(), pos, state);
    }

    public int count() {
        int n = 0;
        for (ItemStack s : items)
            if (!s.isEmpty())
                n++;
        return n;
    }

    /** Hang one of this food; false if the rack is full. */
    public boolean add(ItemStack one) {
        for (int i = 0; i < items.size(); i++)
            if (items.get(i).isEmpty()) {
                items.set(i, one);
                setChanged();
                return true;
            }
        return false;
    }

    /** Take the last food hung, or empty. */
    public ItemStack take() {
        for (int i = items.size() - 1; i >= 0; i--)
            if (!items.get(i).isEmpty()) {
                ItemStack out = items.get(i);
                items.set(i, ItemStack.EMPTY);
                setChanged();
                return out;
            }
        return ItemStack.EMPTY;
    }

    public void tickServer() {
        if (level == null || level.getGameTime() % 20 != 0 || count() == 0)
            return;
        float room = AmbientTable.at(level).value();
        float air = Math.max(room, Math.min(HeatSource.below(level, worldPosition).value(), FTuning.RACK_MAX_AIR_TU.value()));
        boolean rain = level.isRainingAt(worldPosition.above());
        float dry = rain ? 0f : FTuning.RACK_DRY_PER_TICK * 20f * Mth.clamp(air / FTuning.AMBIENT_TU.value(), 0f, 4f);
        for (ItemStack stack : items) {
            if (stack.isEmpty())
                continue;
            Cooking.findPerishable(level, stack).ifPresent(entry -> {
                FoodState s = Cooking.current(stack, entry, level, air);
                FoodState now = new FoodState(s.cook(), Math.max(0f, s.moisture() - dry * Cooking.cutHeat(stack, entry)), s.pathogen(), s.toxin(), s.charred(),
                        s.rot(), s.stamp(), s.envTu(), s.smoke(), s.salt());
                stack.set(FDataComponents.FOOD_STATE.get(), now);
                Cooking.apply(stack, entry, now);
            });
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    public NonNullList<ItemStack> contents() {
        return items;
    }
}
