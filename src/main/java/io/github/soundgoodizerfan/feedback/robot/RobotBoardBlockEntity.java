// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

import io.github.soundgoodizerfan.feedback.control.ChannelSource;
import io.github.soundgoodizerfan.feedback.control.RemoteChannels;
import io.github.soundgoodizerfan.feedback.control.Steppable;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.process.Circuit;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.Nullable;

/**
 * A job board: sixteen slots a fleet of robots shares over radio. A slot is free, claimed by one robot or done, and carries a figure. A robot
 * with a radio part that the board hears chooses a slot, claims it with the CLAIM switch (refreshed every step; a claim not refreshed for
 * {@link RobotTuning#BOARD_CLAIM_TICKS} game ticks lapses, so a lost robot holds nothing), marks it done, or posts a job in it; a base
 * Controller reads every slot and posts or clears them through the same Sensor and Actuator cards it uses on a mast.
 *
 * <p>Radio reach is the mast's: a blocks-per-amp range under a ceiling that rises with the band, and noise drowns a robot the same way.
 * Bandwidth is what the line pays for: the board hears at most {@code floor(amps / BOARD_AMPS_PER_ROBOT x (1 + circuit speed))} robots, the
 * nearest first, recomputed every {@link #SCAN_TICKS} ticks; a robot it does not hear reads nothing from it and its switches do nothing.
 * The circuit built into the board by its recipe is the arbiter: a faster one hears more robots on the same current.
 */
public class RobotBoardBlockEntity extends ElectricNode implements ChannelSource, Steppable, RemoteChannels {

    private static final int SCAN_TICKS = 20;
    private static final byte FREE = 0, CLAIMED = 1, DONE = 2;
    /** Loaded boards by dimension, so a robot finds the one that hears it without scanning blocks. */
    private static final Map<ResourceKey<Level>, List<RobotBoardBlockEntity>> BOARDS = new HashMap<>();

    private static final int N = RobotTuning.BOARD_SLOTS;
    private static final String[] SENSE_KEYS = new String[2 * N], ACTION_KEYS = new String[N];

    static {
        for (int i = 0; i < N; i++) {
            SENSE_KEYS[i] = "robot.feedback.board.state." + i;
            SENSE_KEYS[N + i] = "robot.feedback.board.value." + i;
            ACTION_KEYS[i] = "robot.feedback.board.post." + i;
        }
    }

    private final byte[] state = new byte[N];
    private final float[] value = new float[N];
    private final UUID[] claimer = new UUID[N];
    private final long[] refresh = new long[N];
    /** The circuit its recipe built in; read as built. */
    private ItemStack circuit = ItemStack.EMPTY;
    private final List<RobotEntity> heard = new ArrayList<>();
    private int inRange;
    /** Which slots a Controller holds posted, so a post or a clear happens on the change and not every step. */
    private int posted;
    private final BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos();

    public RobotBoardBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.ROBOT_BOARD.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return RobotTuning.BOARD_AMPS;
    }

    /** Blocks the radio reaches: the mast's range per amp delivered, held under the band's ceiling. */
    public float range() {
        return Math.min(getAmps() * RobotTuning.RADIO_BLOCKS_PER_AMP, RobotTuning.RADIO_BAND_CEILING * (getBand().ordinal() + 1));
    }

    /** How many robots the board hears at once: its amps over the cost of one, stretched by the circuit's speed. */
    public int bandwidth() {
        return (int) Math.floor(getAmps() / RobotTuning.BOARD_AMPS_PER_ROBOT * (1f + Circuit.of(circuit).speed()));
    }

    // --- registry ------------------------------------------------------------------------------------

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            List<RobotBoardBlockEntity> list = BOARDS.computeIfAbsent(level.dimension(), k -> new ArrayList<>());
            if (!list.contains(this))
                list.add(this);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            List<RobotBoardBlockEntity> list = BOARDS.get(level.dimension());
            if (list != null)
                list.remove(this);
        }
        super.setRemoved();
    }

    /** The board that hears {@code r} now, or null. No allocation: a handful of boards, a handful of heard robots. */
    @Nullable
    public static RobotBoardBlockEntity hearing(Level level, RobotEntity r) {
        List<RobotBoardBlockEntity> list = BOARDS.get(level.dimension());
        if (list == null)
            return null;
        for (int i = 0; i < list.size(); i++)
            if (list.get(i).hears(r))
                return list.get(i);
        return null;
    }

    public boolean hears(RobotEntity r) {
        for (int i = 0; i < heard.size(); i++)
            if (heard.get(i) == r)
                return true;
        return false;
    }

    public int heardCount() {
        return heard.size();
    }

    public int inRangeCount() {
        return inRange;
    }

    /** What a Controller's cards and the HUD read of a slot: 0 free, 1 claimed, 2 done. */
    public int slotState(int slot) {
        return state[slot];
    }

    /** Short tag of the robot holding a slot (the head of its id), or empty. */
    public String claimerTag(int slot) {
        return claimer[slot] == null ? "" : claimer[slot].toString().substring(0, 4);
    }

    // --- the tick ------------------------------------------------------------------------------------

    public void tickServer() {
        if (level == null)
            return;
        long now = level.getGameTime();
        if ((now + worldPosition.hashCode()) % SCAN_TICKS != 0)
            return;
        hear();
        // Prior art: PneumaticCraft: Repressurized (GPL-3.0), DroneClaimManager: a per-level claim map with auto-expiry, refreshed while the
        // owner works, and a fixed step cadence. Taken as design only. Rejected: anonymous claims, per-goal claim opt-in, automatic
        // dock-seeking, teleporting when stuck, an admin kill switch as the only lag control, owner gating. See THIRD-PARTY-LICENSES.md.
        for (int i = 0; i < N; i++)
            if (state[i] == CLAIMED && now - refresh[i] > RobotTuning.BOARD_CLAIM_TICKS) {
                state[i] = FREE;
                claimer[i] = null;
                changed();
            }
    }

    /** Picks the robots the radio and the bandwidth reach: those with a radio part in range, nearest first, skipping any drowned by noise. */
    private void hear() {
        heard.clear();
        inRange = 0;
        float range = range();
        if (range <= 0f)
            return;
        boolean deaf = !RobotMastBlockEntity.shielded(level, worldPosition) && RobotMastBlockEntity.noisy(level, worldPosition, scan);
        List<RobotEntity> near = new ArrayList<>();
        double cx = worldPosition.getX() + 0.5, cy = worldPosition.getY() + 0.5, cz = worldPosition.getZ() + 0.5;
        for (RobotEntity r : level.getEntitiesOfClass(RobotEntity.class, new AABB(worldPosition).inflate(range)))
            if (r.spec().radio() && r.distanceToSqr(cx, cy, cz) <= range * range)
                near.add(r);
        inRange = near.size();
        if (deaf)
            return;
        near.sort((a, b) -> Double.compare(a.distanceToSqr(cx, cy, cz), b.distanceToSqr(cx, cy, cz)));
        int cap = bandwidth();
        for (int i = 0; i < near.size() && heard.size() < cap; i++)
            if (!RobotMastBlockEntity.noisy(level, near.get(i).blockPosition(), scan))
                heard.add(near.get(i));
    }

    private void changed() {
        setChanged();
        if (level != null)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // --- what a robot does ---------------------------------------------------------------------------

    /** A slot as a robot reads it: 0 free, 1 claimed by it, 2 claimed by another, 3 done. */
    public int stateFor(RobotEntity r, int slot) {
        return state[slot] == DONE ? 3 : state[slot] == FREE ? 0 : r.getUUID().equals(claimer[slot]) ? 1 : 2;
    }

    public float valueOf(int slot) {
        return value[slot];
    }

    /** Claims a free slot, or refreshes one the robot already holds; false for a slot that is done or held by another. */
    public boolean claim(RobotEntity r, int slot) {
        if (!hears(r) || state[slot] == DONE)
            return false;
        if (state[slot] == FREE) {
            state[slot] = CLAIMED;
            claimer[slot] = r.getUUID();
            changed();
        } else if (!r.getUUID().equals(claimer[slot]))
            return false;
        refresh[slot] = level.getGameTime();
        return true;
    }

    /** Lets go of a claim the robot holds; a done slot stays done. */
    public void release(RobotEntity r, int slot) {
        if (hears(r) && state[slot] == CLAIMED && r.getUUID().equals(claimer[slot])) {
            state[slot] = FREE;
            claimer[slot] = null;
            changed();
        }
    }

    /** Marks a slot the robot holds done, its figure kept. */
    public void done(RobotEntity r, int slot) {
        if (hears(r) && state[slot] == CLAIMED && r.getUUID().equals(claimer[slot])) {
            state[slot] = DONE;
            claimer[slot] = null;
            changed();
        }
    }

    /** Posts a job: a free slot with a figure. A slot someone holds is left alone. */
    public void post(RobotEntity r, int slot, float figure) {
        if (hears(r) && state[slot] != CLAIMED)
            set(slot, figure);
    }

    private void set(int slot, float figure) {
        if (state[slot] == FREE && value[slot] == figure)
            return;
        state[slot] = FREE;
        claimer[slot] = null;
        value[slot] = figure;
        changed();
    }

    // --- what a Controller sees ----------------------------------------------------------------------

    /** Channels 0 to 15 read a slot's state (0 free, 1 claimed, 2 done), 16 to 31 its figure. */
    @Override
    public float read(int channel) {
        if (channel < 0 || channel >= 2 * N)
            return 0f;
        return channel < N ? state[channel] : value[channel - N];
    }

    @Override
    public String[] senseKeys() {
        return SENSE_KEYS;
    }

    @Override
    public String[] channelKeys() {
        return ACTION_KEYS;
    }

    /** Channel 0 of an Actuator card is slot 0; each channel posts its slot (free, figure 1) on, and clears it (figure 0, a done slot acknowledged) off. */
    @Override
    public void setEngaged(boolean engaged) {
        pulse(0, engaged);
    }

    @Override
    public boolean isEngaged() {
        return (posted & 1) != 0;
    }

    @Override
    public void pulse(int channel, boolean on) {
        if (channel < 0 || channel >= N || ((posted >> channel & 1) != 0) == on)
            return;
        posted ^= 1 << channel;
        if (state[channel] != CLAIMED)
            set(channel, on ? 1f : 0f);
    }

    // --- circuit and save ----------------------------------------------------------------------------

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        ItemStack built = input.get(FDataComponents.CIRCUIT_STACK.get());
        circuit = built == null ? ItemStack.EMPTY : built.copy();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!circuit.isEmpty())
            components.set(FDataComponents.CIRCUIT_STACK.get(), circuit.copy());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Circuit", circuit.saveOptional(registries));
        tag.putInt("Posted", posted);
        ListTag slots = new ListTag();
        for (int i = 0; i < N; i++) {
            CompoundTag t = new CompoundTag();
            t.putByte("State", state[i]);
            t.putFloat("Value", value[i]);
            t.putLong("Refresh", refresh[i]);
            if (claimer[i] != null)
                t.putUUID("Claimer", claimer[i]);
            slots.add(t);
        }
        tag.put("Slots", slots);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        circuit = ItemStack.parseOptional(registries, tag.getCompound("Circuit"));
        posted = tag.getInt("Posted");
        ListTag slots = tag.getList("Slots", Tag.TAG_COMPOUND);
        for (int i = 0; i < N; i++) {
            CompoundTag t = i < slots.size() ? slots.getCompound(i) : new CompoundTag();
            state[i] = t.getByte("State");
            value[i] = t.getFloat("Value");
            refresh[i] = t.getLong("Refresh");
            claimer[i] = t.hasUUID("Claimer") ? t.getUUID("Claimer") : null;
        }
    }
}
