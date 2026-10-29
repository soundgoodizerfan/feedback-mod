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
package io.github.soundgoodizerfan.feedback.vehicle.engine;

import io.github.soundgoodizerfan.feedback.control.Analog;
import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatedTank;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationPropagator;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The design a placed Engine was built to. It comes off the item when placed and goes back on it
 * when broken (the loot table copies the component), so an engine is never re-designed by moving.
 *
 * <h2>It drives the rotation network</h2>
 * A source like the Steam Engine, with one difference: its Su capacity is not a constant. It is
 * the design's torque curve read at the engine's own current speed ({@code docs/feedback_vehicles_spec.md}
 * section 2) -- weak from rest, strongest at the peak RPM -- so a run settles wherever the curve,
 * the load and the friction meet, and a load past the curve stalls it. The network only re-reads
 * capacity when told to, so the tick tells it whenever the curve has moved by {@link #RECALC_SU}.
 * <p>
 * Target speed is the design's peak-torque RPM scaled by the throttle. It burns fuel from its tank
 * (spec 9.1): power scales with the fuel's flame temperature, and the compression ratio decides
 * whether the fuel's ignition point suits it (see {@link Compression}). No heat model yet.
 * <p>
 * <b>Off when placed.</b> With no fuel there is nothing else to make starting it an act, and a
 * source that ran from placement would spin whatever it was set against before the build was
 * finished. Use toggles it; a controller does the same through {@link Switchable}. Off means no
 * capacity at all, and the run coasts down under its own load like any cut-off network. Its
 * throttle ({@link Analog}) takes a number from a controller wherever the engine sits, on a
 * stand as in a car (philosophy 13).
 *
 * <h2>Its weight rides on a box</h2>
 * Sable weighs a block by its state alone, so every Engine block would weigh the same. The block
 * is tagged {@code sable:super_light} (0.25 kpg, 250 Mu), and once it is part of a vehicle it
 * carries the rest of its design's mass on a free box welded at its centre, contacts off -- what
 * plates did before they became cells. Boxes are not saved, so the tick re-makes it.
 * <p>
 * Not weightless, which was tried first: a vehicle of nothing but an engine then weighed zero
 * at the moment of lifting, before the box existed, and Sable discarded the body. The cost of
 * the floor is that an engine designed lighter than 250 Mu weighs 250.
 */
public class EngineBlockEntity extends RotationNode implements Switchable, Analog, DataNode, io.github.soundgoodizerfan.feedback.vehicle.VehiclePart {

    /** Just inside the block, so the box never reaches past what is drawn. */
    private static final double BOX_HALF = 0.45;
    /** What Sable already counts for the block itself: {@code sable:super_light}. */
    private static final double BLOCK_KPG = 0.25;
    /** How far the curve may move, in Su, before the network is told. */
    private static final float RECALC_SU = 1f;

    @Nullable
    private EngineDesign design;
    @Nullable
    private dev.ryanhcode.sable.api.physics.object.box.BoxPhysicsObject box;
    @Nullable
    private dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle weld;

    private final FuelBlend fuel = new FuelBlend(this::setChanged);
    /** Fraction of as-built power left: knock wears it, to a floor. */
    private float condition = 1f;
    /** What the fuel gives this tick, as a share of a reference fuel; 0 when starved or misfiring. */
    private float power;
    private float fuelOwed;
    private boolean running;
    /**
     * How far open, 0 to 1 -- the vehicle's analog input ({@link Analog}, spec section 5,
     * <em>Controls</em>). Shut unless a controller says otherwise, so an engine with no controller
     * idles instead of running flat out. Torque and the speed it reaches both scale with
     * {@code IDLE + (1 - IDLE) x throttle}: shut, a running engine idles rather than dies.
     */
    private float throttle = 0;
    /** Capacity as the network last read it, so the tick knows when the curve has moved. */
    private float reportedSu;
    /** Required by {@link DataNode}; nothing links to an engine, a card targets it. Not saved. */
    private final Set<DataNodeRef> links = new HashSet<>();

    public EngineBlockEntity(BlockPos pos, BlockState state) {
        super(FVehicles.ENGINE_ENTITY.get(), pos, state);
    }

    public FuelBlend getFuel() {
        return fuel;
    }

    public float condition() {
        return condition;
    }

    @Nullable
    public EngineDesign design() {
        return design;
    }

    @Nullable
    private Material material() {
        return design == null ? null : MaterialTable.get().materials().get(design.material());
    }

    // --- rotation -----------------------------------------------------------------------------

    @Override
    public Rpm getGeneratedRpm() {
        Material m = material();
        return running && power > 0f && m != null ? new Rpm(design.peakRpm(m).value() * opening()) : ZERO_RPM;
    }

    /** The torque curve at this engine's own current speed. */
    @Override
    public Su getCapacitySu() {
        Material m = material();
        return running && power > 0f && m != null ? new Su(design.torqueAt(new Rpm(Math.abs(getRpm().value())), m).value() * opening() * power * condition) : NO_SU;
    }

    private float opening() {
        return FTuning.ENGINE_IDLE_FRACTION + (1 - FTuning.ENGINE_IDLE_FRACTION) * throttle;
    }

    @Override
    protected void startSound() {
        io.github.soundgoodizerfan.feedback.client.BlockLoops.engine(this);
    }

    // --- Analog -------------------------------------------------------------------------------

    @Override
    public void setLevel(int channel, float level) {
        float next = Math.clamp(level, 0, 1);
        if (Math.abs(next - throttle) < 1e-3)
            return;
        throttle = next;
        setChanged();
        // The target speed moved with it, not only the curve: recalculate() re-reads capacity but
        // the target is set by a rebuild, so without one throttle changed only the Su.
        if (getLevel() != null && !getLevel().isClientSide)
            RotationPropagator.rebuildFrom(getLevel(), worldPosition);
    }

    @Override
    public String[] channelKeys() {
        return new String[]{"feedback.analog.throttle"};
    }

    // --- Switchable ---------------------------------------------------------------------------

    @Override
    public boolean isEngaged() {
        return running;
    }

    @Override
    public void setEngaged(boolean engaged) {
        if (level == null || level.isClientSide || running == engaged)
            return;
        running = engaged;
        setChanged();
        // Becoming a source or ceasing to be one changes who drives the run: rebuild it, as the
        // Steam Engine does when its steam comes and goes.
        RotationPropagator.rebuildFrom(level, worldPosition);
    }

    // --- the tick -----------------------------------------------------------------------------

    static void serverTick(Level level, BlockPos pos, BlockState state, EngineBlockEntity be) {
        be.burn(level, pos);
        be.followTorqueCurve();
        be.carryMass(level, pos);
    }

    /** Draws fuel for this tick, works out what it gives, and wears the engine if it knocks. */
    private void burn(Level level, BlockPos pos) {
        float before = power;
        Material m = material();
        float flame = fuel.isEmpty() ? 0f : fuel.flame();
        if (!running || design == null || m == null || flame <= 0f) {
            power = 0f;
        } else {
            float factor = flame / FTuning.ENGINE_REFERENCE_FLAME_TU;
            fuelOwed += FTuning.ENGINE_FUEL_MB_PER_TICK_PER_L * design.displacementMb() * opening() / factor;
            int drawn = (int) fuelOwed;
            fuelOwed -= drawn;
            if (drawn > 0)
                fuel.burn(drawn);
            float ignition = fuel.ignition();
            boolean misfire = ignition > design.compression().misfireAbove;
            power = misfire ? 0f : factor;
            if (!misfire && ignition < design.compression().knockBelow) {
                condition = Math.max(FTuning.ENGINE_CONDITION_FLOOR, condition - FTuning.ENGINE_KNOCK_WEAR_PER_TICK * opening());
                setChanged();
                if (level.getGameTime() % 10 == 0)
                    level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ANVIL_HIT,
                            net.minecraft.sounds.SoundSource.BLOCKS, 0.4f, 1.6f);
            }
        }
        // Starting or stopping burning changes the target speed, not only the curve.
        if ((before > 0f) != (power > 0f))
            RotationPropagator.rebuildFrom(level, pos);
    }

    private void followTorqueCurve() {
        float su = getCapacitySu().value();
        if (Math.abs(su - reportedSu) < RECALC_SU)
            return;
        reportedSu = su;
        if (network != null)
            network.recalculate();
    }

    /** Puts the design's mass on the vehicle this engine is part of, once its box is in Sable. */
    private void carryMass(Level level, BlockPos pos) {
        if (design == null || !(level instanceof net.minecraft.server.level.ServerLevel server)
                || !(dev.ryanhcode.sable.companion.SableCompanion.INSTANCE.getContaining(this) instanceof dev.ryanhcode.sable.sublevel.ServerSubLevel body))
            return;
        Material material = material();
        if (material == null)
            return;
        var system = dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem.get(server);
        if (box == null || box.isRemoved()) {
            org.joml.Vector3d centre = new org.joml.Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            var pose = new dev.ryanhcode.sable.companion.math.Pose3d(body.logicalPose().transformPosition(centre, new org.joml.Vector3d()),
                    new org.joml.Quaterniond(body.logicalPose().orientation()), new org.joml.Vector3d(), new org.joml.Vector3d(1, 1, 1));
            box = new dev.ryanhcode.sable.api.physics.object.box.BoxPhysicsObject(pose,
                    new org.joml.Vector3d(BOX_HALF, BOX_HALF, BOX_HALF),
                    Math.max(1e-3, io.github.soundgoodizerfan.feedback.core.unit.Units.kpg(design.mass(material)) - BLOCK_KPG));
            system.addObject(box);
            weld = null;
        }
        if (box.getRuntimeId() == dev.ryanhcode.sable.api.physics.PhysicsPipelineBody.NULL_RUNTIME_ID
                || (weld != null && weld.isValid()))
            return;
        org.joml.Vector3d centre = new org.joml.Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        weld = system.getPipeline().addConstraint(body, box,
                new dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration(centre, new org.joml.Vector3d(), new org.joml.Quaterniond()));
        weld.setContactsEnabled(false); // the box sits inside its own block
    }

    // --- DataNode -----------------------------------------------------------------------------

    @Override
    public boolean acceptsLinks() {
        return false;
    }

    @Override
    public BlockPos getNodePos() {
        return worldPosition;
    }

    @Nullable
    @Override
    public Direction getNodeSide() {
        return null;
    }

    @Override
    public Level getNodeLevel() {
        return level;
    }

    @Override
    public BlockEntity getNodeOwner() {
        return this;
    }

    @Override
    public Set<DataNodeRef> getNodeLinks() {
        return links;
    }

    // --- lifecycle ----------------------------------------------------------------------------

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (weld != null && weld.isValid())
            weld.remove();
        if (box != null && level instanceof net.minecraft.server.level.ServerLevel server && !box.isRemoved())
            dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem.get(server).removeObject(box);
        box = null;
    }

    /** The mass this engine puts on its vehicle, block and box, in kpg, for tests; the block alone until the box exists. */
    public double carriedKpg() {
        return BLOCK_KPG + (box == null || box.isRemoved() ? 0 : box.getMass());
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        design = input.get(FVehicles.ENGINE_DESIGN.get());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (design != null)
            components.set(FVehicles.ENGINE_DESIGN.get(), design);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (design != null)
            EngineDesign.CODEC.encodeStart(NbtOps.INSTANCE, design).ifSuccess(t -> tag.put("Design", t));
        tag.putBoolean("Running", running);
        tag.putFloat("Throttle", throttle);
        tag.putFloat("Condition", condition);
        CompoundTag fuelTag = new CompoundTag();
        fuel.save(fuelTag);
        tag.put("Fuel", fuelTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        design = tag.contains("Design") ? EngineDesign.CODEC.parse(NbtOps.INSTANCE, tag.get("Design")).result().orElse(null) : null;
        running = tag.getBoolean("Running");
        throttle = tag.contains("Throttle") ? tag.getFloat("Throttle") : 0;
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
        if (tag.contains("Fuel"))
            fuel.load(tag.getCompound("Fuel"));
    }
}
