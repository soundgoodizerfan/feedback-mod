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
package io.github.soundgoodizerfan.feedback.machine.induction;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A coil that heats one metal workpiece laid in it, quickly and cleanly, to the bottom of its forging window plus
 * {@link FTuning#PREHEAT_WINDOW_MARGIN_TU} (never above its working ceiling or {@link FTuning#INDUCTION_CEILING_TU}). It heats the held
 * stack's {@link ItemHeat} stamp while powered and does nothing else: unpowered, the piece cools in the slot at the item's own rate.
 * Only a metal couples to the field. A cold-workable metal (no minimum temperature) has nothing to preheat to.
 */
public class PreheatCoilBlockEntity extends ElectricNode {

    private ItemStack held = ItemStack.EMPTY;
    /** The Tu the held piece is heated to, or 0 when it is not a metal with a forging window. Looked up when laid in. */
    private float goal;
    private boolean goalKnown;
    private float power;

    public PreheatCoilBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.PREHEAT_COIL.get(), pos, state);
    }

    public ItemStack held() {
        return held;
    }

    public void lay(ItemStack stack) {
        if (level != null)
            ItemHeat.settle(stack, level);
        held = stack;
        goalKnown = false;
        setChanged();
    }

    public ItemStack take() {
        ItemStack out = held;
        held = ItemStack.EMPTY;
        goalKnown = false;
        setChanged();
        return out;
    }

    private void findGoal() {
        goalKnown = true;
        goal = 0f;
        if (held.isEmpty())
            return;
        Material.find(MaterialTable.get().materials(), held)
                .filter(found -> found.material().kind().filter(k -> k == Material.Kind.METAL).isPresent())
                .ifPresent(found -> {
                    Material m = found.material();
                    if (m.minTemperature().value() > 0f)
                        goal = Math.min(Math.min(m.minTemperature().value() + FTuning.PREHEAT_WINDOW_MARGIN_TU, m.maxTemperature().value()),
                                FTuning.INDUCTION_CEILING_TU.value());
                });
    }

    private boolean wantsHeat() {
        if (level == null)
            return false;
        if (!goalKnown)
            findGoal();
        return goal > 0f && ItemHeat.get(held, level).value() < goal;
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return wantsHeat() ? FTuning.PREHEAT_AMPS : 0f;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.MV;
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        power = volts > 0f && amps > 0f ? Math.min(1f, volts * amps / (FTuning.ELECTRIC_MV_VOLTS * FTuning.PREHEAT_AMPS)) : 0f;
    }

    public void tickServer() {
        if (level == null || power <= 0f || !wantsHeat())
            return;
        float now = ItemHeat.get(held, level).value();
        ItemHeat.set(held, new Tu(Math.min(goal, now + FTuning.PREHEAT_TU_PER_TICK.tuPerTick() * power)), level);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!held.isEmpty())
            tag.put("Held", held.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        held = tag.contains("Held") ? ItemStack.parseOptional(registries, tag.getCompound("Held")) : ItemStack.EMPTY;
        goalKnown = false;
    }
}
