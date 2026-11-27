// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

import io.github.soundgoodizerfan.feedback.control.ChannelSource;
import io.github.soundgoodizerfan.feedback.control.RemoteChannels;
import io.github.soundgoodizerfan.feedback.control.Steppable;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * The base end of a radio link, an electrical sink paired to one robot that carries a radio part. A Controller's ordinary Sensor cards
 * read the robot's senses through it and its Actuator cards hold the robot's actions on or off, so a base Controller can drive a free robot.
 * The link reaches only as far as the power behind it: a blocks-per-amp range, held under a ceiling that rises with the band, so a starved
 * mast reaches less. Out of range, or in noise, every reading is 0 and every action is released; nothing is refused, it just stops hearing.
 *
 * <p>Noise is whatever throws sparks near either end: a block tagged {@code feedback:radio_noise} (the arc heads) within a few blocks of the
 * robot or of the mast. A mast boxed in by lead-lined chests ({@code feedback:radio_shields}) does not hear it at its own end; the robot's
 * end is open, so a robot working beside an arc is still deaf. The scan runs every {@link #SCAN_TICKS} ticks.
 */
public class RobotMastBlockEntity extends ElectricNode implements ChannelSource, Steppable, RemoteChannels {

    private static final int SCAN_TICKS = 20;
    /** The senses a Sensor card can pick, by channel. */
    public static final RobotChannel[] SENSES = { RobotChannel.SOLID_AHEAD, RobotChannel.BREAKABLE_AHEAD, RobotChannel.ITEM_AHEAD,
            RobotChannel.CARGO_FULL, RobotChannel.CHARGE, RobotChannel.RANGE_AHEAD, RobotChannel.HARDNESS_AHEAD, RobotChannel.HEADING,
            RobotChannel.BEACON_NEAR, RobotChannel.DOCK_AHEAD, RobotChannel.POS_X, RobotChannel.POS_Z, RobotChannel.ALTITUDE, RobotChannel.LASER_RANGE };
    /** The actions an Actuator card can hold on, by channel (channel 0 is the plain switch: the first). */
    public static final RobotChannel[] ACTIONS = { RobotChannel.WALK, RobotChannel.TURN_LEFT, RobotChannel.TURN_RIGHT, RobotChannel.MINE,
            RobotChannel.PLACE, RobotChannel.PICK_UP, RobotChannel.DROP, RobotChannel.GIVE, RobotChannel.TAKE, RobotChannel.USE,
            RobotChannel.SWITCH, RobotChannel.ASCEND, RobotChannel.DESCEND };

    @Nullable
    private UUID robotId;
    @Nullable
    private RobotEntity robot;
    private boolean jammed, heard;
    private final BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos();

    public RobotMastBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.ROBOT_MAST.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return robotId == null ? 0f : RobotTuning.MAST_AMPS;
    }

    /** Pairs the mast with a robot that carries a radio part. */
    public boolean link(RobotEntity r) {
        if (!r.spec().radio())
            return false;
        robotId = r.getUUID();
        robot = r;
        setChanged();
        return true;
    }

    /** Blocks the link reaches: a range per amp delivered, held under a ceiling that rises with the band. */
    public float range() {
        return Math.min(getAmps() * RobotTuning.RADIO_BLOCKS_PER_AMP, RobotTuning.RADIO_BAND_CEILING * (getBand().ordinal() + 1));
    }

    public void tickServer() {
        if (level == null || robotId == null)
            return;
        if (level.getGameTime() % 4 == 0)
            watchLink();
        if ((level.getGameTime() + worldPosition.hashCode()) % SCAN_TICKS != 0)
            return;
        if ((robot == null || robot.isRemoved()) && level instanceof ServerLevel server)
            robot = server.getEntity(robotId) instanceof RobotEntity r ? r : null;
        if (robot == null) {
            jammed = false;
            return;
        }
        boolean shielded = shielded(level, worldPosition);
        jammed = (!shielded && noisy(level, worldPosition, scan)) || noisy(level, robot.blockPosition(), scan);
    }

    /** Runs each tick: the moment the link is lost the robot lets go of every action, whether or not a Controller writes again. */
    private void watchLink() {
        boolean now = reach() != null;
        if (heard && !now && robot != null && !robot.isRemoved())
            for (RobotChannel c : ACTIONS)
                robot.write(c.ordinal(), false);
        heard = now;
    }

    /** Whether four or more of a base block's neighbours shield it from noise (shared with the job board). */
    static boolean shielded(net.minecraft.world.level.Level level, BlockPos pos) {
        return io.github.soundgoodizerfan.feedback.radio.RadioNoise.shielded(level, pos);
    }

    /** Whether a noise block stands within {@link RobotTuning#RADIO_NOISE_RANGE} of {@code at}; {@code scan} is the caller's scratch position. */
    static boolean noisy(net.minecraft.world.level.Level level, BlockPos at, BlockPos.MutableBlockPos scan) {
        int r = RobotTuning.RADIO_NOISE_RANGE;
        for (int dx = -r; dx <= r; dx++)
            for (int dy = -r; dy <= r; dy++)
                for (int dz = -r; dz <= r; dz++) {
                    scan.set(at.getX() + dx, at.getY() + dy, at.getZ() + dz);
                    if (level.isLoaded(scan) && level.getBlockState(scan).is(FTags.RADIO_NOISE))
                        return true;
                }
        return false;
    }

    /** The robot if the link holds right now: it exists, carries a radio, is in range and nothing drowns it. */
    @Nullable
    private RobotEntity reach() {
        if (robot == null || robot.isRemoved() || jammed || !robot.spec().radio() || robot.level() != level)
            return null;
        float range = range();
        return robot.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= range * range ? robot : null;
    }

    // --- what a Controller sees ------------------------------------------------------------------------

    @Override
    public float read(int channel) {
        RobotEntity r = reach();
        return r == null || channel < 0 || channel >= SENSES.length ? 0f : r.read(SENSES[channel].ordinal(), 0);
    }

    @Override
    public String[] senseKeys() {
        String[] keys = new String[SENSES.length];
        for (int i = 0; i < keys.length; i++)
            keys[i] = SENSES[i].langKey();
        return keys;
    }

    @Override
    public String[] channelKeys() {
        String[] keys = new String[ACTIONS.length];
        for (int i = 0; i < keys.length; i++)
            keys[i] = ACTIONS[i].langKey();
        return keys;
    }

    /** Channel 0 of an Actuator card: the first action (roll forward). Released the moment the link is lost. */
    @Override
    public void setEngaged(boolean engaged) {
        hold(0, engaged);
    }

    @Override
    public boolean isEngaged() {
        RobotEntity r = reach();
        return r != null && r.read(ACTIONS[0].ordinal(), 0) != 0f;
    }

    @Override
    public void pulse(int channel, boolean on) {
        hold(channel, on);
    }

    private void hold(int channel, boolean on) {
        if (channel < 0 || channel >= ACTIONS.length || robot == null || robot.isRemoved())
            return;
        // out of range or jammed the robot hears nothing: the action is let go
        robot.write(ACTIONS[channel].ordinal(), on && reach() != null);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (robotId != null)
            tag.putUUID("Robot", robotId);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        robotId = tag.hasUUID("Robot") ? tag.getUUID("Robot") : null;
        robot = null;
    }
}
