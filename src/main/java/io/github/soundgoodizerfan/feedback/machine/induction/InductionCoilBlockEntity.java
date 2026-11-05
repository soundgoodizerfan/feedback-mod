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
import io.github.soundgoodizerfan.feedback.machine.crucible.CrucibleBlockEntity;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.process.Mixture;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An induction furnace coil around a crucible: a changing field heats the metal in the vessel directly, with no flame and no smoke,
 * up to {@link FTuning#INDUCTION_CEILING_TU}.
 *
 * <h2>A fire that is an electrical sink, and picky</h2>
 * It is a {@link HeatSource} facing the vessel, like the arc head, but {@link #heats} is true only while the crucible there holds
 * metal (a held item whose material is a metal, or any molten metal): an empty crucible or bare ceramic is not heated, because there
 * is nothing for the field to couple to. The field follows the power the line delivers through a small thermal mass, so a sag lowers
 * it and cutting the supply lets it die away. It draws current only while there is a charge.
 */
public class InductionCoilBlockEntity extends ElectricNode implements HeatSource {

    private float fieldTu = FTuning.AMBIENT_TU.value();
    private float power;
    private boolean charged;
    private long checkedAt = Long.MIN_VALUE;

    public InductionCoilBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.INDUCTION_COIL.get(), pos, state);
    }

    // --- sink ---------------------------------------------------------------------------------

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return charged ? FTuning.INDUCTION_COIL_AMPS : 0f;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.MV;
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        float full = FTuning.ELECTRIC_MV_VOLTS * FTuning.INDUCTION_COIL_AMPS;
        power = volts > 0f && amps > 0f ? Math.min(1f, volts * amps / full) : 0f;
    }

    // --- the charge ---------------------------------------------------------------------------

    /** Whether the crucible at {@code target} holds metal. */
    private static boolean holdsMetal(Level level, BlockPos target) {
        if (!(level.getBlockEntity(target) instanceof CrucibleBlockEntity vessel))
            return false;
        var materials = MaterialTable.get().materials();
        for (ItemStack held : vessel.getContents())
            if (Induction.isMetal(held))
                return true;
        for (var fluid : vessel.moltenFluids())
            if (Mixture.fluidMaterial(materials, fluid).map(materials::get)
                    .filter(m -> m.kind().filter(k -> k == Material.Kind.METAL).isPresent()).isPresent())
                return true;
        return false;
    }

    public void tickServer() {
        if (level == null)
            return;
        // The metal search walks the material table, so it is looked at a few times a second, not every tick.
        long now = level.getGameTime();
        if (now - checkedAt >= FTuning.INDUCTION_CHARGE_CHECK_TICKS || now < checkedAt) {
            checkedAt = now;
            charged = holdsMetal(level, worldPosition.relative(heatFacing()));
        }
        float ambient = AmbientTable.at(level).value();
        float target = charged ? ambient + (FTuning.INDUCTION_CEILING_TU.value() - ambient) * power : ambient;
        fieldTu = Math.max(ambient, fieldTu + (target - fieldTu) / FTuning.INDUCTION_COIL_MASS.workPerTu());
    }

    // --- HeatSource ---------------------------------------------------------------------------

    @Override
    public Direction heatFacing() {
        return HeatSource.facingOf(getBlockState());
    }

    @Override
    public boolean heats(Level level, BlockPos target) {
        return charged && target.equals(worldPosition.relative(heatFacing()));
    }

    @Override
    public Tu getFireTu() {
        return new Tu(fieldTu);
    }

    // --- persistence --------------------------------------------------------------------------

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
