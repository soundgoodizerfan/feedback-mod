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
package io.github.soundgoodizerfan.feedback.machine.column;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Vapour;
import io.github.soundgoodizerfan.feedback.core.multiblock.Multiblocks;
import io.github.soundgoodizerfan.feedback.core.multiblock.Part;
import io.github.soundgoodizerfan.feedback.core.multiblock.RoleRule;
import io.github.soundgoodizerfan.feedback.core.multiblock.Structure;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatedTank;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatedTanks;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.geology.CrudeGrade;
import io.github.soundgoodizerfan.feedback.process.PhaseChange;
import io.github.soundgoodizerfan.feedback.process.PhaseChangeTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The base of the column (spec 6.4): takes graded crude and heat from below (a firebox, lava, a
 * coil, exactly as a boiler does) and sets the column's heat. Each tick the structure calls {@link
 * #run}, which boils the cuts the reboiler has reached and sends each up through the trays: a tray
 * colder than the cut's boiling point holds back its {@code efficiency} share as liquid and lets the
 * rest rise to the next. Trays that cannot take it all (flow, coke) carry it over. What reaches the
 * top is pushed into whatever sits above, or lost to the air as vapour.
 * <p>
 * The profile is a line from the reboiler's temperature at the bottom to the condenser body's temperature at the top; a
 * column with no condenser has a warmer top. Fire the reboiler hotter and every tray's temperature
 * rises with it, so every cut moves heavier: cut drift falls out of the profile, nothing is scripted.
 * The machine names no crude and no product: cuts are the phase-change rows that carry a {@code
 * light_share}, their boiling points and products come from those rows, and the feed's grade sets how
 * much each holds. Coke on the trays comes from running a tray over {@link FTuning#COLUMN_COKE_TU}.
 * shortcut: the grade's light fraction falls as lights are drawn off, but the heavy bottoms stay
 * liquid in the reboiler (cracking them to coke and gas is a reaction-table row, not built).
 * Reflux (none, partial, total; empty hand on the block) trades boil-up for sharper cuts.
 * shortcut: no catch-up across unloaded time, unlike the boiler.
 */
public class ReboilerBlockEntity extends BlockEntity implements Part, ThermalBody, HeatedTanks {

    /** The valve on the return line: how much of the condensed liquid is sent back down (spec 6.4). */
    public enum Reflux {
        NONE(1f, 0f), PARTIAL(FTuning.REFLUX_PARTIAL_THROUGHPUT, FTuning.REFLUX_PARTIAL_SHARPEN),
        TOTAL(FTuning.REFLUX_TOTAL_THROUGHPUT, FTuning.REFLUX_TOTAL_SHARPEN);

        public final float throughput, sharpen;

        Reflux(float throughput, float sharpen) {
            this.throughput = throughput;
            this.sharpen = sharpen;
        }

        public Reflux next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private Reflux reflux = Reflux.NONE;

    public Reflux reflux() {
        return reflux;
    }

    public void cycleReflux() {
        reflux = reflux.next();
        setChanged();
    }

    @Nullable
    private Structure structure;
    private final HeatedTank feed = new HeatedTank(FTuning.REBOILER_FEED_TANK_MB, this::setChanged);
    private float temperature = FTuning.AMBIENT_TU.value();
    private float lastDelta, temperatureLastTick = Float.NaN;
    private final float[] owed = new float[8];

    /** What the reboiler's face shows a pipe: crude in, and steam to clear the trays; nothing out. */
    private final IFluidHandler handler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return feed.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return feed.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!resource.is(FTags.STEAM))
                return feed.fill(resource, action);
            if (temperature < FTuning.COLUMN_COKE_TU && action.execute() && structure != null)
                for (Part p : structure.parts(ColumnStructure.TRAY))
                    if (p instanceof TrayBlockEntity tray)
                        tray.decoke(resource.getAmount() * FTuning.COLUMN_DECOKE_PER_MB);
            return resource.getAmount();
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    public ReboilerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.REBOILER.get(), pos, state);
    }

    /** Whether the table boils {@code stack} in cuts by its grade: a crude, not water. */
    static boolean boilsByGrade(FluidStack stack) {
        return PhaseChangeTable.get().boiling(stack).filter(p -> p.lightShare().isPresent()).isPresent();
    }

    public IFluidHandler getHandler() {
        return handler;
    }

    public HeatedTank getFeed() {
        return feed;
    }

    @Override
    public List<HeatedTank> heatedTanks() {
        return List.of(feed);
    }

    // --- thermal ---------------------------------------------------------------------------

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
        return FTuning.REBOILER_MASS;
    }

    @Override
    public Conductance getLeak() {
        return FTuning.BOILER_LEAK;
    }

    public void tickServer() {
        if (level == null)
            return;
        Tu fireTu = HeatSource.below(level, worldPosition);
        Tu ambient = AmbientTable.at(level);
        Heat.tick(this, fireTu, ambient, fireTu.value() > ambient.value());
        lastDelta = Float.isNaN(temperatureLastTick) ? 0f : temperature - temperatureLastTick;
        temperatureLastTick = temperature;
        feed.relax(getTemperature());
        setChanged();
    }

    // --- the column ------------------------------------------------------------------------

    /** Called once per level tick by the structure, server side. {@code trays} are bottom to top. */
    public void run(Level level, List<TrayBlockEntity> trays, float condenserTu) {
        if (!(level instanceof ServerLevel server) || feed.isEmpty())
            return;
        FluidStack held = feed.getFluid();
        CrudeGrade grade = CrudeGrade.of(held);
        int n = trays.size();
        float ambient = AmbientTable.at(level).value();
        float top = Float.isNaN(condenserTu) ? (temperature + ambient) / 2f : condenserTu;
        float[] t = new float[n];
        for (int i = 0; i < n; i++)
            t[i] = temperature - (temperature - top) * (i + 1f) / (n + 1f);

        // Over-temperature coats the trays whatever is flowing: misuse, never uptime.
        float heaviness = 0.5f + 0.5f * (1f - grade.lightFraction());
        for (int i = 0; i < n; i++)
            trays.get(i).coke(t[i], heaviness);

        List<PhaseChange> cuts = new ArrayList<>();
        for (PhaseChange cut : PhaseChangeTable.get().entries())
            if (cut.lightShare().isPresent() && cut.boils(held))
                cuts.add(cut);
        cuts.sort((a, b) -> Float.compare(a.boil().value(), b.boil().value()));

        float lights = grade.lightFraction();
        for (int c = 0; c < cuts.size() && c < owed.length; c++) {
            PhaseChange cut = cuts.get(c);
            if (temperature < cut.boil().value() || lights <= 0f || feed.isEmpty())
                continue;
            float want = feed.getFluidAmount() * lights * cut.lightShare().get() * FTuning.COLUMN_BOIL_PER_TICK * reflux.throughput + owed[c];
            int boiled = Math.min((int) want, feed.getFluidAmount());
            owed[c] = want - (int) want;
            if (boiled <= 0)
                continue;
            int before = feed.getFluidAmount();
            feed.drain(boiled, IFluidHandler.FluidAction.EXECUTE);
            temperature -= boiled * cut.latent() / FTuning.REBOILER_MASS.workPerTu();
            int left = rise(trays, t, cut, boiled);
            if (left > 0)
                overhead(server, trays, cut, left, temperature);
            // The lights drawn off are no longer in the feed.
            if (!feed.isEmpty() && before > boiled) {
                float remaining = Math.max(0f, (lights * before - boiled) / (before - boiled));
                feed.getFluid().set(FDataComponents.CRUDE_GRADE.get(),
                        new CrudeGrade(grade.density(), grade.sulfur(), remaining));
                lights = remaining;
            }
        }
    }

    /** Sends {@code mb} of a cut up the column from the bottom; returns what got past the top tray. */
    private int rise(List<TrayBlockEntity> trays, float[] t, PhaseChange cut, int mb) {
        FluidStack liquid = new FluidStack(BuiltInRegistries.FLUID.get(cut.liquidFluid()), 1);
        int left = mb;
        for (int i = 0; i < trays.size() && left > 0; i++) {
            if (t[i] > cut.boil().value())
                continue;
            float e = trays.get(i).efficiency();
            int share = Math.round(left * (e + (1f - e) * reflux.sharpen));
            left -= trays.get(i).take(liquid.copyWithAmount(share), t[i]);
        }
        return left;
    }

    /** What rose past the top tray: into a handler above if one takes it, else into the air as vapour. */
    private void overhead(ServerLevel server, List<TrayBlockEntity> trays, PhaseChange cut, int mb, float tu) {
        BlockPos above = trays.isEmpty() ? worldPosition.above() : trays.get(trays.size() - 1).getBlockPos().above();
        FluidStack gas = FluidHeat.stamp(new FluidStack(BuiltInRegistries.FLUID.get(cut.gasFluid()), mb), tu);
        IFluidHandler target = server.getCapability(Capabilities.FluidHandler.BLOCK, above, Direction.DOWN);
        int taken = target == null ? 0 : target.fill(gas, IFluidHandler.FluidAction.EXECUTE);
        if (taken < mb)
            Vapour.source(server, above, 1f, 20, 0, false);
    }

    // --- Part ------------------------------------------------------------------------------

    @Override
    public String role() {
        return ColumnStructure.roleOf(getBlockState(), ColumnStructure.REBOILER);
    }

    @Override
    public boolean connects(Direction face) {
        return face == Direction.UP;
    }

    @Override
    public RoleRule rule() {
        return ColumnStructure.RULE;
    }

    @Nullable
    @Override
    public Structure structure() {
        return structure;
    }

    @Override
    public void setStructure(@Nullable Structure structure) {
        this.structure = structure;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        Multiblocks.load(this);
    }

    @Override
    public void setRemoved() {
        Multiblocks.remove(this);
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Temperature", temperature);
        tag.putInt("Reflux", reflux.ordinal());
        tag.put("Feed", feed.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("Temperature") ? tag.getFloat("Temperature") : FTuning.AMBIENT_TU.value();
        reflux = Reflux.values()[Math.floorMod(tag.getInt("Reflux"), Reflux.values().length)];
        if (tag.contains("Feed"))
            feed.readFromNBT(registries, tag.getCompound("Feed"));
    }
}
