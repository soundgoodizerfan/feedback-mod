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
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.MicroRotation;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationPropagator;
import io.github.soundgoodizerfan.feedback.machine.bearing.Joint;
import io.github.soundgoodizerfan.feedback.machine.bearing.JointPart;
import io.github.soundgoodizerfan.feedback.machine.bearing.RotaryBearingBlock;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;
import io.github.soundgoodizerfan.feedback.core.unit.Mu;
import io.github.soundgoodizerfan.feedback.core.unit.Units;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The micro-blocks inside one {@link MicroCellBlock} ({@code docs/specs/feedback_micro_spec.md}): up to
 * 64 block states, one per quarter-block voxel, four to a side. A voxel is a bare block state, so
 * only blocks with no block entity of their own can be one.
 *
 * <p>Voxel {@code (x, y, z)} is index {@code x + 4y + 16z}, and the same bit of {@link #filled}.
 * Saved with a palette (a cell is mostly a few kinds of block) and synced, because the renderer
 * draws the voxels on the client and the outline is traced from them there.
 *
 * <p>A voxel whose block is a {@link MicroRotation} (shaft, cog, gearbox) also has a
 * {@link MicroRotor}, a rotation network member held here: no block entity per voxel. The cell
 * tells the propagator when rotors come and go, and saves and syncs their speed and angle.
 */
public class MicroCellBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {

    /** Voxels along one edge of a cell: each is a quarter block. */
    public static final int N = 4;
    public static final int VOXELS = N * N * N;
    /** A block is 1000 mB (philosophy section 17), so a voxel is a 64th of that. */
    private static final float VOXEL_MB = 1000f / VOXELS;

    private final BlockState[] voxels = new BlockState[VOXELS];
    /** Bit {@code index(x, y, z)}: that voxel holds a block. */
    private long filled;
    private final MicroRotor[] rotors = new MicroRotor[VOXELS];
    /** The block a wedge voxel is made of (its look, weight and what mining it gives back); null: the stock wedge. */
    private final BlockState[] skins = new BlockState[VOXELS];
    /** Belts from pulley voxels to pulley voxels (see {@link MicroBelts}), each with its condition. */
    private final it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<MicroBelts.End> belts = new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>();
    private final float[] beltWear = new float[VOXELS];
    /** Which voxels are pulleys: only these are looked at for belts. */
    private long pulleys;
    /** Which voxels are differentials, and how far each has shared its speed from even. */
    private long diffs;
    private final float[] shares = new float[VOXELS];
    /** Which voxels are flywheels: only these are looked at for rim strain. */
    private long flywheels;
    /** Server: rotors taken out since the last {@link #commit}, and whether any came in. */
    private final List<MicroRotor> gone = new ArrayList<>();
    private boolean rotorAdded;
    /**
     * Octants left out of this cell's physics collider (not its players' shape): a micro bearing's
     * clearance. After a split the turned body's cells keep their places, so a body cell and the world
     * cell it was split from share a position and would share half-block octants -- and the bearing
     * voxel's octant would touch the turned voxel's -- so the body would collide with its own stator
     * and stall (playtest 2026-11-02: "turns a little and stops"). Set on both sides by
     * {@link MicroBearings}, cleared on put-back.
     */
    private int clear;
    /**
     * Voxels left out of the voxel collider (MicroColliders): a micro bearing's hinge -- the bearing
     * voxel on the stator side, the voxel in front on the turned side -- whose faces touch at the
     * turning face and would rub. Set by MicroBearings, cleared on put-back.
     */
    private long hinge;
    /** The voxel collider is to be written on the next server tick (after a load). */
    private boolean colliderDue = true;
    /** A piston voxel's commanded level, -1 to 1 of its travel, by voxel index; set by a card or a test, not saved. */
    private final float[] levels = new float[VOXELS];

    public float level(int index) {
        return levels[index];
    }

    public void setLevel(int index, float level) {
        levels[index] = Math.clamp(level, -1f, 1f);
    }

    /** A driven hinge's brake level, 0 to 1, by voxel index. */
    private final float[] brakes = new float[VOXELS];

    public void setBrake(int index, float level) {
        brakes[index] = Math.clamp(level, 0f, 1f);
    }

    private final VoxelNode[] nodes = new VoxelNode[VOXELS];

    /** The voxel as a part a card can set, or null if it is not one. */
    @Nullable
    public VoxelNode nodeFor(int index) {
        if (index < 0 || index >= VOXELS || !VoxelNode.settable(voxels[index]))
            return null;
        if (nodes[index] == null)
            nodes[index] = new VoxelNode(this, index);
        return nodes[index];
    }

    /** Micro bearings among the voxels that have a body assembled, by voxel index. */
    private final Int2ObjectOpenHashMap<Joint> bearings = new Int2ObjectOpenHashMap<>();
    /** Set by chunk unload, so setRemoved does not take an unload for the cell being broken. */
    private boolean unloaded;
    @Nullable
    private VoxelShape shape;

    public MicroCellBlockEntity(BlockPos pos, BlockState state) {
        super(FVehicles.MICRO_CELL_ENTITY.get(), pos, state);
    }

    public static int index(int x, int y, int z) {
        return x + N * y + N * N * z;
    }

    /** The block in voxel {@code index}, or null if it is empty. */
    @Nullable
    public BlockState get(int index) {
        return voxels[index];
    }

    @Nullable
    public BlockState get(int x, int y, int z) {
        return voxels[index(x, y, z)];
    }

    public boolean isEmpty() {
        return filled == 0;
    }

    /** How many voxels hold a block; the air drag of a body of cells goes by it ({@code AirDrag}). */
    public int voxelCount() {
        return Long.bitCount(filled);
    }

    public long filled() {
        return filled;
    }

    /** Puts {@code state} in voxel {@code index}, or empties it for null. Call {@link #commit} once the edits are done. */
    public void set(int index, @Nullable BlockState state) {
        voxels[index] = state;
        skins[index] = null;
        filled = state == null ? filled & ~(1L << index) : filled | 1L << index;
        shape = null;
        if (rotors[index] != null)
            gone.add(rotors[index]);
        // A bearing voxel taken out lets its body go (it falls free, as a full-size bearing broken does).
        Joint was = bearings.remove(index);
        if (was != null) {
            was.letGo();
            if (level != null && !level.isClientSide)
                MicroBearings.unclear(level, was);
        }
        float wear = rotors[index] != null && state != null && rotors[index].state() == state ? rotors[index].condition() : 1f;
        rotors[index] = state != null && state.getBlock() instanceof MicroRotation ? new MicroRotor(this, index, state) : null;
        if (rotors[index] != null)
            rotors[index].setCondition(wear);
        unlinkBelt(index);
        pulleys = state != null && state.getBlock() instanceof io.github.soundgoodizerfan.feedback.machine.pulley.PulleyBlock
                ? pulleys | 1L << index : pulleys & ~(1L << index);
        diffs = state != null && state.getBlock() instanceof io.github.soundgoodizerfan.feedback.machine.differential.DifferentialBlock
                ? diffs | 1L << index : diffs & ~(1L << index);
        shares[index] = 0;
        flywheels = state != null && state.getBlock() instanceof io.github.soundgoodizerfan.feedback.machine.shaft.FlywheelBlock
                ? flywheels | 1L << index : flywheels & ~(1L << index);
        rotorAdded |= rotors[index] != null;
    }

    @Nullable
    public MicroBelts.End belt(int index) {
        return belts.get(index);
    }

    void setBelt(int index, MicroBelts.End far) {
        belts.put(index, far);
        beltWear[index] = 1;
        syncSpin();
    }

    float beltCondition(int index) {
        return beltWear[index] == 0 ? 1 : beltWear[index];
    }

    void wearBelt(int index, float condition) {
        beltWear[index] = condition;
        setChanged();
    }

    /** Drops the belt at voxel {@code index} from both ends; the far end only if it is loaded. */
    void unlinkBelt(int index) {
        MicroBelts.End far = belts.remove(index);
        beltWear[index] = 0;
        if (far == null)
            return;
        syncSpin();
        if (level != null && level.isLoaded(far.pos()) && level.getBlockEntity(far.pos()) instanceof MicroCellBlockEntity other) {
            MicroBelts.End back = other.belts.get(far.index());
            if (back != null && back.pos().equals(worldPosition) && back.index() == index) {
                other.belts.remove(far.index());
                other.syncSpin();
            }
        }
    }

    /** The block voxel {@code index}, a wedge, is made of; null for the stock wedge or a voxel that is not one. */
    @Nullable
    public BlockState skin(int index) {
        return skins[index];
    }

    /** Makes wedge voxel {@code index} of {@code skin}; call {@link #commit} after. */
    public void setSkin(int index, @Nullable BlockState skin) {
        skins[index] = voxels[index] != null && MicroWedgeBlock.is(voxels[index]) ? skin : null;
    }

    /** The rotor in voxel {@code index}, or null if that voxel is not a rotation part. */
    @Nullable
    public MicroRotor rotor(int index) {
        return rotors[index];
    }

    /** Every rotor in this cell. */
    public List<MicroRotor> rotors() {
        List<MicroRotor> out = new ArrayList<>();
        for (MicroRotor rotor : rotors)
            if (rotor != null)
                out.add(rotor);
        return out;
    }

    /** Hands every filled voxel's block to {@code each}. */
    public void forEachVoxel(Consumer<BlockState> each) {
        for (BlockState state : voxels)
            if (state != null)
                each.accept(state);
    }

    // --- physics -------------------------------------------------------------------------------

    /** Bit {@code x/2 + 2 (y/2) + 4 (z/2)}: that half-block octant holds a voxel of {@code filled}. Zero only if none is. */
    public static int octants(long filled) {
        int out = 0;
        for (int i = 0; i < VOXELS; i++) {
            int x = i & 3, y = i >> 2 & 3, z = i >> 4;
            if ((filled >>> i & 1) != 0)
                out |= 1 << (x >> 1) + 2 * (y >> 1) + 4 * (z >> 1);
        }
        return out;
    }

    /**
     * What a voxel of {@code state} weighs, in Sable's unit: its block's material density over a
     * 64th of a block. The material table is the one source; a block no material claims weighs
     * {@link FTuning#MICRO_FALLBACK_DENSITY}.
     */
    public static double voxelKpg(BlockState state) {
        var found = Material.find(MaterialTable.get().materials(), state.getBlock().asItem());
        float density = found.isPresent() && found.get().material().density().muPerMb() > 0
                ? found.get().material().density().muPerMb() : FTuning.MICRO_FALLBACK_DENSITY.muPerMb();
        // A wedge is half a cube.
        return Units.kpg(new Mu(density * VOXEL_MB * (MicroWedgeBlock.is(state) ? 0.5f : 1f)));
    }

    /** The cell's Sable mass, kpg: its voxels summed. */
    public double kpg() {
        // The material lookup scans the whole table, so weigh each kind of block once.
        Map<Block, Double> each = new HashMap<>();
        double sum = 0;
        for (BlockState state : voxels)
            if (state != null)
                sum += each.computeIfAbsent(state.getBlock(), b -> voxelKpg(state));
        // A skinned wedge weighs half its skin's block, not half the stock wedge's.
        for (int i = 0; i < VOXELS; i++)
            if (skins[i] != null && voxels[i] != null)
                sum += 0.5 * voxelKpg(skins[i]) - voxelKpg(voxels[i]);
        return sum;
    }

    /**
     * Brings the world in line with the voxels after an edit: the cell goes when its last voxel
     * does, otherwise its block state takes the new octants and mass class (Sable reads both off
     * the state, never off this entity -- {@link MicroCellBlock}), and the clients get the voxels.
     */
    public void commit() {
        shape = null;
        if (level == null)
            return;
        // Before the cell can go: a removed rotor's run is rebuilt without it, an added one queued.
        if (!level.isClientSide) {
            RotationPropagator.onRemoved(level, List.copyOf(gone));
            if (rotorAdded)
                RotationPropagator.queue(level, worldPosition);
        }
        gone.clear();
        rotorAdded = false;
        int lit = light();
        boolean relit = lit != light;
        light = lit;
        if (filled == 0) {
            level.removeBlock(worldPosition, false);
            return;
        }
        // Never zero: Sable weighs a state only if its collision shape is non-empty (VoxelNeighborhoodState.isSolid),
        // so a fully cleared mask would give the cell no mass at all. The exact voxel collider decides what collides.
        int mask = octants(filled) & ~clear;
        BlockState want = MicroCellBlock.stateFor(mask != 0 ? mask : octants(filled), kpg());
        // The same block with another state keeps this entity.
        if (level.getBlockState(worldPosition) != want)
            level.setBlock(worldPosition, want, Block.UPDATE_ALL);
        // After the state: Sable has just written the state's octant collider here; this replaces it.
        applyCollider();
        if (relit)
            level.getLightEngine().checkBlock(worldPosition);
        setChanged();
        level.sendBlockUpdated(worldPosition, want, want, Block.UPDATE_CLIENTS);
    }

    // --- rotation ------------------------------------------------------------------------------

    @Override
    public void onLoad() {
        super.onLoad();
        gone.clear();
        rotorAdded = false;
        if (level != null && !level.isClientSide && !rotors().isEmpty())
            RotationPropagator.queue(level, worldPosition);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        unloaded = true;
    }

    /** As {@link io.github.soundgoodizerfan.feedback.core.rotation.RotationNode#setRemoved}: an unload leaves the runs without a rebuild. */
    @Override
    public void setRemoved() {
        for (Joint joint : bearings.values())
            joint.letGo();
        if (level != null && !level.isClientSide) {
            if (!unloaded)
                RotationPropagator.onRemoved(level, rotors());
            else
                for (MicroRotor rotor : rotors())
                    if (rotor.getNetwork() != null)
                        rotor.getNetwork().remove(rotor);
        }
        super.setRemoved();
    }

    /** The voxels that are tire stock ({@link MicroWheels#TIRE}): a raycast does the tread's work, so they have no collider. */
    public long tireBits() {
        long bits = 0;
        for (int i = 0; i < VOXELS; i++)
            if (voxels[i] != null && MicroWheels.isTire(voxels[i]))
                bits |= 1L << i;
        return bits;
    }

    private int light = -1;

    /** The highest light level of the voxels' blocks, 0 to 15. */
    public int light() {
        int lit = 0;
        for (BlockState v : voxels)
            if (v != null)
                lit = Math.max(lit, v.getLightEmission());
        return lit;
    }

    /** The wedge voxels: their colliders are the steps of the ramp, not a cube. */
    public long wedgeBits() {
        long bits = 0;
        for (int i = 0; i < VOXELS; i++)
            if (voxels[i] != null && MicroWedgeBlock.is(voxels[i]))
                bits |= 1L << i;
        return bits;
    }

    /** The step boxes of every wedge voxel, in blocks of this cell. */
    List<double[]> wedgeBoxes() {
        List<double[]> out = new ArrayList<>();
        for (int i = 0; i < VOXELS; i++) {
            BlockState v = voxels[i];
            if (v == null || !MicroWedgeBlock.is(v))
                continue;
            double ox = (i & 3) * 0.25, oy = (i >> 2 & 3) * 0.25, oz = (i >> 4) * 0.25;
            // The steps in voxel sixteenths (0 to 4) become blocks of the cell: a sixteenth is 1/16.
            for (int[] b : MicroWedgeBlock.steps(v.getValue(MicroWedgeBlock.FACING), v.getValue(MicroWedgeBlock.HALF), MicroWedgeBlock.STEPS))
                out.add(new double[]{ox + b[0] / 16.0, oy + b[1] / 16.0, oz + b[2] / 16.0, ox + b[3] / 16.0, oy + b[4] / 16.0, oz + b[5] / 16.0});
        }
        return out;
    }

    public boolean hasTire() {
        return tireBits() != 0;
    }

    /** Writes this cell's voxel collider into Sable's scene (MicroColliders); server only. */
    public void applyCollider() {
        colliderDue = false;
        if (level instanceof ServerLevel server && filled != 0)
            MicroColliders.apply(server, worldPosition, getBlockState(), filled & ~hinge & ~tireBits() & ~wedgeBits(), wedgeBoxes());
    }

    /** Sets the voxels left out of the voxel collider (see {@link #hinge}). */
    public void setHinge(long voxels) {
        if (hinge == voxels)
            return;
        hinge = voxels;
        setChanged();
        applyCollider();
    }

    public long hinge() {
        return hinge;
    }

    public int clear() {
        return clear;
    }

    /** Sets the octants left out of the physics collider (see {@link #clear}) and updates the state. */
    public void setClear(int octants) {
        if (clear == octants)
            return;
        clear = octants;
        commit();
    }

    /** True if a micro bearing here holds a body. */
    /** A Lift carried this cell by {@code delta} blocks: every joint it holds follows. */
    public void moved(BlockPos delta) {
        for (Joint joint : bearings.values())
            joint.shift(delta);
        // The partners moved with it (a belt links two pulleys of one body): their addresses follow.
        for (var e : belts.int2ObjectEntrySet())
            e.setValue(new MicroBelts.End(e.getValue().pos().offset(delta), e.getValue().index()));
    }

    public boolean holdsBearing() {
        return !bearings.isEmpty();
    }

    /** The bearings this cell holds, by voxel index: kept across a re-made cell (MicroBearings). */
    public Int2ObjectOpenHashMap<Joint> bearings() {
        return bearings;
    }

    /** The hold of the micro bearing in voxel {@code index}, or null if it has no body. */
    @Nullable
    public Joint bearing(int index) {
        return bearings.get(index);
    }

    /** Voxel {@code index}, a micro bearing, now holds {@code joint} (null: none). */
    public void setBearing(int index, @Nullable Joint joint) {
        if (joint == null)
            bearings.remove(index);
        else
            bearings.put(index, joint);
        setChanged();
    }

    /** The centre of voxel {@code index}'s face toward {@code f}, in this cell's level: a micro bearing's hinge. */
    public Vector3d faceCentre(int index, Direction f) {
        return new Vector3d(worldPosition.getX() + ((index & 3) + 0.5 + 0.5 * f.getStepX()) / N,
                worldPosition.getY() + ((index >> 2 & 3) + 0.5 + 0.5 * f.getStepY()) / N,
                worldPosition.getZ() + ((index >> 4) + 0.5 + 0.5 * f.getStepZ()) / N);
    }

    /** Drives the micro bearings that hold a body; nothing for a cell without one. */
    public static void tickServer(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, MicroCellBlockEntity cell) {
        // Staggered, so a build's cells do not all refresh on one tick.
        if (cell.colliderDue || (level.getGameTime() + pos.hashCode()) % MicroColliders.REFRESH == 0)
            cell.applyCollider();
        if (cell.flywheels != 0 && !level.isClientSide)
            cell.strainFlywheels();
        if (cell.pulleys != 0 && !cell.belts.isEmpty() && !level.isClientSide)
            for (int i : cell.belts.keySet().toIntArray())
                MicroBelts.step(level, cell, i);
        if (cell.diffs != 0 && !level.isClientSide)
            for (long m = cell.diffs; m != 0; m &= m - 1) {
                int i = Long.numberOfTrailingZeros(m);
                if (cell.rotors[i] != null && cell.voxels[i] != null)
                    cell.shares[i] = io.github.soundgoodizerfan.feedback.machine.differential.DifferentialBlock.step(level, cell.rotors[i], cell.voxels[i], cell.shares[i]);
            }
        if (cell.bearings.isEmpty() || !(level instanceof ServerLevel server))
            return;
        boolean rebuild = false;
        for (var it = cell.bearings.int2ObjectEntrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            BlockState voxel = cell.voxels[e.getIntKey()];
            MicroRotor rotor = cell.rotors[e.getIntKey()];
            boolean hinge = e.getValue().kind() == Joint.Kind.HINGE;
            boolean driven = hinge && VoxelNode.role(voxel == null ? cell.get(e.getIntKey()) : voxel) == 0;
            if (voxel == null || (driven && rotor == null) || !(voxel.getBlock() instanceof JointPart) || e.getValue().gone(server)) {
                e.getValue().letGo();
                MicroBearings.unclear(server, e.getValue());
                it.remove();
                rebuild = true;
                continue;
            }
            Direction f = voxel.getValue(RotaryBearingBlock.FACING);
            rebuild |= e.getValue().tick(server, pos, cell.faceCentre(e.getIntKey(), f), f,
                    driven ? rotor.getRpm().value() : cell.levels[e.getIntKey()]);
            e.getValue().setBrake(cell.brakes[e.getIntKey()]);
            if (driven && rotor.getNetwork() != null) {
                float road = e.getValue().takeRoadSu(f.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1, 1);
                if (road != 0)
                    rotor.getNetwork().driveExternally(new io.github.soundgoodizerfan.feedback.core.unit.Su(road),
                            new io.github.soundgoodizerfan.feedback.core.unit.Drag(0f), new io.github.soundgoodizerfan.feedback.core.unit.Inertia(0f));
            }
        }
        if (rebuild) {
            cell.setChanged();
            RotationPropagator.rebuildFrom(server, pos);
        }
    }

    /**
     * Rim strain, as {@code ShaftBlockEntity} has it: past the rim speed a flywheel's condition falls, to the
     * floor and never to a burst. A voxel's rim is a quarter of the radius, so it holds {@link #N} times the speed.
     */
    private void strainFlywheels() {
        var iron = io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials().get(io.github.soundgoodizerfan.feedback.Feedback.id("cast_iron"));
        if (iron == null)
            return;
        float rim = io.github.soundgoodizerfan.feedback.machine.shaft.ShaftBlockEntity.rimLimitRpm(iron) * N;
        for (long m = flywheels; m != 0; m &= m - 1) {
            MicroRotor rotor = rotors[Long.numberOfTrailingZeros(m)];
            if (rotor == null)
                continue;
            float over = Math.abs(rotor.liveRpm()) / rim - 1f;
            if (over > 0f && rotor.condition() > io.github.soundgoodizerfan.feedback.core.FTuning.FLYWHEEL_CONDITION_FLOOR) {
                rotor.setCondition(Math.max(io.github.soundgoodizerfan.feedback.core.FTuning.FLYWHEEL_CONDITION_FLOOR,
                        rotor.condition() - io.github.soundgoodizerfan.feedback.core.FTuning.FLYWHEEL_WEAR_PER_OVER * over));
                setChanged();
            }
        }
    }

    /** A rotor's speed moved or its run was rebuilt: the clients hear the cell again. */
    void syncSpin() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    // --- shape ---------------------------------------------------------------------------------

    boolean hasShape() {
        return filled != 0;
    }

    /** The filled voxels as boxes, in the cell's own space: what players walk into and see outlined. */
    VoxelShape shape() {
        if (shape == null) {
            shape = merge(filled & ~wedgeBits());
            for (double[] b : wedgeBoxes())
                shape = net.minecraft.world.phys.shapes.Shapes.or(shape, net.minecraft.world.phys.shapes.Shapes.box(b[0], b[1], b[2], b[3], b[4], b[5]));
        }
        return shape;
    }

    /** Greedy-merges voxels into boxes: runs along x, widened along z, stacked along y. */
    static VoxelShape merge(long in) {
        long on = in;
        List<VoxelShape> boxes = new ArrayList<>();
        for (int y = 0; y < N; y++)
            for (int z = 0; z < N; z++)
                for (int x = 0; x < N; x++) {
                    if ((on >>> index(x, y, z) & 1) == 0)
                        continue;
                    int x1 = x;
                    while (x1 + 1 < N && (on >>> index(x1 + 1, y, z) & 1) != 0)
                        x1++;
                    int z1 = z;
                    while (z1 + 1 < N && run(on, x, x1, y, z1 + 1))
                        z1++;
                    int y1 = y;
                    while (y1 + 1 < N && slab(on, x, x1, y1 + 1, z, z1))
                        y1++;
                    for (int yy = y; yy <= y1; yy++)
                        for (int zz = z; zz <= z1; zz++)
                            for (int xx = x; xx <= x1; xx++)
                                on &= ~(1L << index(xx, yy, zz));
                    // Block.box is in sixteenths: a voxel is four of them.
                    boxes.add(Block.box(x * 4, y * 4, z * 4, (x1 + 1) * 4, (y1 + 1) * 4, (z1 + 1) * 4));
                }
        if (boxes.isEmpty())
            return Shapes.empty();
        return Shapes.or(boxes.get(0), boxes.subList(1, boxes.size()).toArray(VoxelShape[]::new));
    }

    private static boolean run(long on, int x0, int x1, int y, int z) {
        for (int x = x0; x <= x1; x++)
            if ((on >>> index(x, y, z) & 1) == 0)
                return false;
        return true;
    }

    private static boolean slab(long on, int x0, int x1, int y, int z0, int z1) {
        for (int z = z0; z <= z1; z++)
            if (!run(on, x0, x1, y, z))
                return false;
        return true;
    }

    /**
     * The filled voxel a ray hit at {@code at} (a world position) on side {@code face} of the cell's
     * shape, or -1 if the cell is empty: the point nudged a hair back into the solid, else, if a
     * rounding error left it outside, the filled voxel nearest it.
     */
    public int pick(Vec3 at, Direction face) {
        double lx = (at.x - worldPosition.getX()) * N - face.getStepX() * 1e-3;
        double ly = (at.y - worldPosition.getY()) * N - face.getStepY() * 1e-3;
        double lz = (at.z - worldPosition.getZ()) * N - face.getStepZ() * 1e-3;
        int i = index(Mth.clamp((int) Math.floor(lx), 0, N - 1), Mth.clamp((int) Math.floor(ly), 0, N - 1),
                Mth.clamp((int) Math.floor(lz), 0, N - 1));
        if ((filled >>> i & 1) != 0)
            return i;
        int best = -1;
        double bestD = Double.MAX_VALUE;
        for (int j = 0; j < VOXELS; j++) {
            if ((filled >>> j & 1) == 0)
                continue;
            double dx = Math.max(Math.max((j & 3) - lx, lx - ((j & 3) + 1)), 0);
            double dy = Math.max(Math.max((j >> 2 & 3) - ly, ly - ((j >> 2 & 3) + 1)), 0);
            double dz = Math.max(Math.max((j >> 4) - lz, lz - ((j >> 4) + 1)), 0);
            double d = dx * dx + dy * dy + dz * dz;
            if (d < bestD) {
                best = j;
                bestD = d;
            }
        }
        return best;
    }

    // --- save and sync -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        List<BlockState> kinds = new ArrayList<>();
        ListTag palette = new ListTag();
        // 0 is empty, n is palette entry n - 1; at most 64 kinds, so a byte holds it.
        byte[] ids = new byte[VOXELS];
        for (int i = 0; i < VOXELS; i++) {
            if (voxels[i] == null)
                continue;
            int kind = kinds.indexOf(voxels[i]);
            if (kind < 0) {
                kind = kinds.size();
                kinds.add(voxels[i]);
                palette.add(NbtUtils.writeBlockState(voxels[i]));
            }
            ids[i] = (byte) (kind + 1);
        }
        tag.put("Palette", palette);
        tag.putByteArray("Voxels", ids);
        ListTag spin = new ListTag();
        for (MicroRotor rotor : rotors())
            if (rotor.liveRpm() != 0 || rotor.livePhase() != 0) {
                CompoundTag one = new CompoundTag();
                one.putByte("I", (byte) rotor.index());
                one.putFloat("Rpm", rotor.liveRpm());
                one.putFloat("Phase", rotor.livePhase());
                spin.add(one);
            }
        tag.put("Spin", spin);
        ListTag worn = new ListTag();
        for (MicroRotor rotor : rotors())
            if (rotor.condition() < 1f) {
                CompoundTag one = new CompoundTag();
                one.putByte("I", (byte) rotor.index());
                one.putFloat("C", rotor.condition());
                worn.add(one);
            }
        tag.put("Worn", worn);
        ListTag strapped = new ListTag();
        for (var e : belts.int2ObjectEntrySet()) {
            CompoundTag one = new CompoundTag();
            one.putByte("I", (byte) e.getIntKey());
            one.putLong("P", e.getValue().pos().asLong());
            one.putByte("J", (byte) e.getValue().index());
            one.putFloat("C", beltCondition(e.getIntKey()));
            strapped.add(one);
        }
        tag.put("Belts", strapped);
        ListTag skinned = new ListTag();
        for (int i = 0; i < VOXELS; i++)
            if (skins[i] != null) {
                CompoundTag one = new CompoundTag();
                one.putByte("I", (byte) i);
                one.put("S", NbtUtils.writeBlockState(skins[i]));
                skinned.add(one);
            }
        tag.put("Skins", skinned);
        ListTag held = new ListTag();
        for (var e : bearings.int2ObjectEntrySet()) {
            CompoundTag one = e.getValue().save();
            one.putByte("I", (byte) e.getIntKey());
            held.add(one);
        }
        tag.put("Bearings", held);
        tag.putInt("Clear", clear);
        tag.putLong("Hinge", hinge);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ListTag palette = tag.getList("Palette", Tag.TAG_COMPOUND);
        BlockState[] kinds = new BlockState[palette.size()];
        for (int i = 0; i < kinds.length; i++)
            kinds[i] = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), palette.getCompound(i));
        byte[] ids = tag.getByteArray("Voxels");
        // A load is the cell as it stands: its belts are read back below, and the far ends are not touched.
        belts.clear();
        Arrays.fill(beltWear, 0);
        Arrays.fill(voxels, null);
        Arrays.fill(skins, null);
        flywheels = 0;
        diffs = 0;
        pulleys = 0;
        filled = 0;
        // A block that no longer exists reads back as air, and a voxel of air is no voxel.
        for (int i = 0; i < Math.min(ids.length, VOXELS); i++) {
            int kind = ids[i] - 1;
            if (kind >= 0 && kind < kinds.length && !kinds[kind].isAir())
                set(i, kinds[kind]);
        }
        shape = null;
        // A load is the cell as it stands, not an edit: nothing for the propagator to undo.
        gone.clear();
        bearings.clear();
        clear = tag.getInt("Clear");
        hinge = tag.getLong("Hinge");
        colliderDue = true;
        ListTag held = tag.getList("Bearings", Tag.TAG_COMPOUND);
        for (int k = 0; k < held.size(); k++) {
            Joint joint = Joint.load(held.getCompound(k));
            if (joint != null)
                bearings.put(held.getCompound(k).getByte("I") & (VOXELS - 1), joint);
        }
        ListTag skinned = tag.getList("Skins", Tag.TAG_COMPOUND);
        for (int k = 0; k < skinned.size(); k++) {
            BlockState skin = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), skinned.getCompound(k).getCompound("S"));
            if (!skin.isAir())
                setSkin(skinned.getCompound(k).getByte("I") & (VOXELS - 1), skin);
        }
        ListTag strapped = tag.getList("Belts", Tag.TAG_COMPOUND);
        for (int k = 0; k < strapped.size(); k++) {
            CompoundTag one = strapped.getCompound(k);
            int i = one.getByte("I") & (VOXELS - 1);
            belts.put(i, new MicroBelts.End(BlockPos.of(one.getLong("P")), one.getByte("J") & (VOXELS - 1)));
            beltWear[i] = one.getFloat("C");
        }
        ListTag worn = tag.getList("Worn", Tag.TAG_COMPOUND);
        for (int k = 0; k < worn.size(); k++) {
            MicroRotor rotor = rotors[worn.getCompound(k).getByte("I") & (VOXELS - 1)];
            if (rotor != null)
                rotor.setCondition(worn.getCompound(k).getFloat("C"));
        }
        ListTag spin = tag.getList("Spin", Tag.TAG_COMPOUND);
        for (int k = 0; k < spin.size(); k++) {
            CompoundTag one = spin.getCompound(k);
            MicroRotor rotor = rotors[one.getByte("I") & (VOXELS - 1)];
            if (rotor != null)
                rotor.load(one.getFloat("Rpm"), one.getFloat("Phase"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
