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
package io.github.soundgoodizerfan.feedback.machine.breaker;

import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricPropagator;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A fuse: a holder for one fine wire, sacrificed to protect what it guards -- the third accepted
 * wear-with-correct-use exception (mechanics 6.4), because a fuse is a consumable by nature. The
 * holder never wears; the wire is spent.
 *
 * <h2>Both its figures are its wire's metal</h2>
 * It is a cable's physics with a thin wire (mechanics 6.4): over its rating the wire warms, and
 * when it passes its metal's melting point it is gone and the holder conducts nothing. The rating
 * is the metal's {@code ampacity} times {@link FTuning#FUSE_AMPS_PER_AMPACITY}; how long an
 * overload is tolerated is how far its melting point sits above the room. Tin melts at 232 Tu and
 * blows fast and low; copper carries more and blows slowly. Which fuse fits a line is read off the
 * material table, and nothing names a fuse.
 * <p>
 * A blown holder is rewired by hand with a fine wire of any metal.
 */
public class FuseBlockEntity extends ElectricNode implements ThermalBody {

    /** The wire's metal, or null when there is none -- placed empty, or blown. */
    @Nullable
    private ResourceLocation wire;
    private float temperature = FTuning.AMBIENT_TU.value();

    public FuseBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.FUSE.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.WIRE;
    }

    @Override
    public boolean conducting() {
        return wire != null;
    }

    @Nullable
    private Material material() {
        return wire == null ? null : MaterialTable.get().materials().get(wire);
    }

    public float getRatingAmps() {
        Material m = material();
        return m == null ? 0f : m.ampacity().orElse(0f) * FTuning.FUSE_AMPS_PER_AMPACITY;
    }

    @Override
    public float dropPerAmp() {
        Material m = material();
        return m == null ? 0f : m.electrical().drop().orElse(0f);
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        Tu ambient = AmbientTable.at(level);
        float over = amps - getRatingAmps();
        if (wire != null && over > 0f)
            Heat.warm(this, FTuning.FUSE_OVERLOAD_WORK_PER_AMP * over);
        Heat.tick(this, ambient, ambient, false);

        Material m = material();
        if (m != null && temperature > m.thermal().meltingPoint().map(Tu::value).orElse(Float.MAX_VALUE))
            blow();
    }

    private void blow() {
        wire = null;
        setChanged();
        if (level != null && !level.isClientSide)
            ElectricPropagator.reconnect(level, worldPosition);
    }

    /** Whether the holder has a wire in it. */
    public boolean isIntact() {
        return wire != null;
    }

    @Nullable
    public ResourceLocation getWire() {
        return wire;
    }

    /** Fits one fine wire from {@code stack} into an empty holder. Returns whether it took one. */
    public boolean rewire(ItemStack stack) {
        if (wire != null || level == null)
            return false;
        Optional<Material.Located> found = Material.find(MaterialTable.get().materials(), stack.getItem());
        if (found.isEmpty() || !found.get().form().equals("fine_wire") || found.get().material().ampacity().isEmpty())
            return false;
        stack.shrink(1);
        wire = found.get().id();
        temperature = AmbientTable.at(level).value();
        setChanged();
        if (!level.isClientSide)
            ElectricPropagator.reconnect(level, worldPosition);
        return true;
    }

    @Override
    public Tu getTemperature() {
        return new Tu(temperature);
    }

    @Override
    public void setTemperature(Tu tu) {
        temperature = tu.value();
    }

    @Override
    public ThermalMass getThermalMass() {
        return FTuning.WIRE_MASS;
    }

    @Override
    public Conductance getLeak() {
        return FTuning.WIRE_LEAK;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (wire != null)
            tag.putString("Wire", wire.toString());
        tag.putFloat("Temperature", temperature);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        wire = tag.contains("Wire") ? ResourceLocation.tryParse(tag.getString("Wire")) : null;
        if (tag.contains("Temperature"))
            temperature = tag.getFloat("Temperature");
    }
}
