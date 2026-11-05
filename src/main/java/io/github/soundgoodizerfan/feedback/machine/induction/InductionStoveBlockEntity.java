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
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.machine.skillet.SkilletBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.stockpot.StockpotBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An induction hob: it heats the cookware on top of it when that cookware's material is magnetic (iron and steel yes, copper and
 * aluminium no), up to {@link FTuning#INDUCTION_STOVE_CEILING_TU}. Cooking without fire or smoke: the pan or pot reads it through
 * {@link HeatSource#at} like any fire. It draws current only while magnetic cookware sits on it.
 */
public class InductionStoveBlockEntity extends ElectricNode implements HeatSource {

    private float fieldTu = FTuning.AMBIENT_TU.value();
    private float power;
    private boolean loaded;

    public InductionStoveBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.INDUCTION_STOVE.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return loaded ? FTuning.INDUCTION_STOVE_AMPS : 0f;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.MV;
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        float full = FTuning.ELECTRIC_MV_VOLTS * FTuning.INDUCTION_STOVE_AMPS;
        power = volts > 0f && amps > 0f ? Math.min(1f, volts * amps / full) : 0f;
    }

    /** The material of the cookware at {@code target}, if there is some. */
    private static ResourceLocation cookware(Level level, BlockPos target) {
        var be = level.getBlockEntity(target);
        if (be instanceof SkilletBlockEntity pan)
            return pan.material();
        if (be instanceof StockpotBlockEntity pot)
            return pot.vesselMaterial();
        return null;
    }

    public void tickServer() {
        if (level == null)
            return;
        loaded = Induction.isMagnetic(cookware(level, worldPosition.above()));
        float ambient = AmbientTable.at(level).value();
        float target = loaded ? ambient + (FTuning.INDUCTION_STOVE_CEILING_TU.value() - ambient) * power : ambient;
        fieldTu = Math.max(ambient, fieldTu + (target - fieldTu) / FTuning.INDUCTION_STOVE_MASS.workPerTu());
    }

    @Override
    public boolean heats(Level level, BlockPos target) {
        return target.equals(worldPosition.above()) && Induction.isMagnetic(cookware(level, target));
    }

    @Override
    public Tu getFireTu() {
        return new Tu(fieldTu);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Field", fieldTu);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fieldTu = tag.contains("Field") ? tag.getFloat("Field") : FTuning.AMBIENT_TU.value();
    }
}
