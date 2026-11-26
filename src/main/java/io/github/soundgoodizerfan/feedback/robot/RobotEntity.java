// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

import io.github.soundgoodizerfan.feedback.control.controller.ControllerCore;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.item.DataConnectorItem;
import io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A robot: a light entity (no Sable body, no tip-over) that runs an ordinary Controller program. Its cards name its own channels
 * ({@link RobotChannel}); the program evaluates every {@link RobotTuning#STEP_TICKS} ticks while movement and work run every tick. The
 * battery is a loose stack (its charge drawn in batches so no per-tick allocation), the program a Punch Card in the core's slot, the
 * cargo nine slots. Picked up or destroyed it comes back as its item with all of that kept. What it does to the world is
 * {@link RobotWorker}'s, shared with the arm.
 *
 * <p>Prior art: PneumaticCraft: Repressurized drones (GPL-3.0), design only: a reduced program step rate against full-rate movement,
 * the real break pipeline's cancellable event, a drop-as-item death carrying program and charge. Rejected: its fake player per drone (one
 * shared per level here), item-id filters, teleporting when stuck, owner UUIDs, pressure as the only power.
 */
public class RobotEntity extends Entity implements ControllerCore.Locals, RobotWorker.Body {

    private static final EntityDataAccessor<ItemStack> DESIGN = SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final String STATE = "Robot";
    private static final long NO_CHUNK = Long.MIN_VALUE;
    /** The actions a robot starts first to last when the program holds several on. */
    private static final RobotChannel[] ORDER = { RobotChannel.MINE, RobotChannel.PLACE, RobotChannel.PICK_UP, RobotChannel.DROP,
            RobotChannel.TURN_LEFT, RobotChannel.TURN_RIGHT, RobotChannel.GIVE, RobotChannel.TAKE, RobotChannel.USE };

    private ItemStack battery = ItemStack.EMPTY;
    private final SimpleContainer cargo = new SimpleContainer(RobotTuning.CARGO);
    private final boolean[] on = new boolean[RobotChannel.values().length];
    private RobotSpec spec = RobotSpec.NONE;
    private final ControllerCore core = new ControllerCore(this::level, this::setChangedFlag, this, this::powered, s -> s.is(FTags.PUNCH_CARDS))
            .cardCap(() -> spec.cards());

    /** Eu in the battery as of the last flush, and Eu drawn since: the battery stack is written once a step, not every tick. */
    private float chargeEu, pendingEu;
    private RobotChannel doing;
    private int doingTicks;
    private BlockPos doingPos;
    /** Where dead reckoning starts (the block it was set down on) and the error it has gathered since a beacon last corrected it. */
    private int originX, originY, originZ;
    private float errX, errZ;
    private boolean beaconNear, switched;
    /** The pair id of the endernet upgrade fitted as this robot's leash (its base is the Data Port bonded under it), or null. */
    @Nullable
    private java.util.UUID leash;
    private java.util.UUID leashJoined;
    /** The places it remembers (the lattice's memory of them), in its own dead reckoning: set, east, south; and the one chosen. */
    private final boolean[] placeSet = new boolean[io.github.soundgoodizerfan.feedback.process.ProcessTuning.CIRCUIT_MEMORY_MAX];
    private final float[] placeX = new float[placeSet.length], placeZ = new float[placeSet.length];
    private int placeAt;
    /** The job-board slot it has chosen, the one it holds a claim on (-1: none), and how many steps' worth of waiting the step budget has cost it. */
    private int slot, claimedSlot = -1, slow = 1;
    private boolean waiting;
    private final BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos();
    /** The reel this robot is tied to, or null; it may roam {@link RobotTuning#TETHER_LENGTH} blocks from it. */
    @Nullable
    private BlockPos tether;
    /** The chunk this robot holds loaded, or {@link #NO_CHUNK}. */
    private long ticketChunk = NO_CHUNK;

    public RobotEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    private void setChangedFlag() {
        // an entity saves with its chunk; nothing to mark
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DESIGN, ItemStack.EMPTY);
    }

    /** The finished robot item this robot is built to. */
    public ItemStack design() {
        return entityData.get(DESIGN);
    }

    public ControllerCore core() {
        return core;
    }

    /** The action it is carrying out, or null. */
    @Nullable
    public RobotChannel doing() {
        return doing;
    }

    @Override
    public float maxUpStep() {
        return spec.stepUp();
    }

    /** Sets the robot up from the item it was set down from: design, and any battery, program and cargo it carried. */
    public void setup(ItemStack held) {
        ItemStack design = held.copyWithCount(1);
        var custom = design.get(DataComponents.CUSTOM_DATA);
        CompoundTag state = custom == null ? null : custom.copyTag().getCompound(STATE);
        design.remove(DataComponents.CUSTOM_DATA);
        entityData.set(DESIGN, design);
        spec = RobotSpec.of(design);
        originX = blockPosition().getX();
        originY = blockPosition().getY();
        originZ = blockPosition().getZ();
        if (state != null && !state.isEmpty())
            loadState(state, registryAccess());
        refreshCharge();
    }

    // --- the body the worker acts through --------------------------------------------------------------

    @Override
    public BlockPos pos() {
        return blockPosition();
    }

    @Override
    public Direction facing() {
        return Direction.fromYRot(getYRot());
    }

    @Override
    public Vec3 centre() {
        return position();
    }

    @Override
    public RobotSpec spec() {
        return spec;
    }

    @Override
    public SimpleContainer cargo() {
        return cargo;
    }

    @Override
    public long owner() {
        return getUUID().getMostSignificantBits();
    }

    @Override
    public boolean afford(float eu) {
        return chargeEu - pendingEu >= eu;
    }

    @Override
    public void spend(float eu) {
        pendingEu += eu;
    }

    // --- power -----------------------------------------------------------------------------------------

    private boolean powered() {
        return chargeEu - pendingEu > 0f;
    }

    private void refreshCharge() {
        CompoundTag t = BatteryStack.state(battery);
        chargeEu = t == null || !BatteryStack.ready(t) ? 0f : BatteryStack.charge(t);
    }

    /** Writes the Eu drawn since the last step onto the battery stack: one tag copy a step, none between. */
    private void flush() {
        if (pendingEu <= 0f)
            return;
        CompoundTag t = BatteryStack.state(battery);
        if (t != null) {
            CompoundTag next = t.copy();
            BatteryStack.draw(next, pendingEu);
            battery.set(FDataComponents.BATTERY.get(), next);
        }
        pendingEu = 0f;
        refreshCharge();
    }

    /** A share of the battery's charge, 0 to 1; 0 without a battery. */
    public float chargeShare() {
        CompoundTag t = BatteryStack.state(battery);
        if (t == null || !BatteryStack.ready(t))
            return 0f;
        float cap = BatteryStack.effectiveCapacity(t);
        return cap <= 0f ? 0f : Math.max(0f, Math.min(1f, (chargeEu - pendingEu) / cap));
    }

    /** The battery's saved state, or null without one; the dock reads it to ask for the right current. */
    @Nullable
    public CompoundTag batteryState() {
        return BatteryStack.state(battery);
    }

    /** Puts what a dock or reel is delivered into the battery, as a charger does a battery item. */
    public void charge(float amps, VoltageBand band, float packetEu) {
        CompoundTag t = BatteryStack.state(battery);
        if (t == null)
            return;
        CompoundTag next = t.copy();
        BatteryStack.charge(next, amps, band, packetEu);
        battery.set(FDataComponents.BATTERY.get(), next);
        refreshCharge();
    }

    @Nullable
    public BlockPos tether() {
        return tether;
    }

    /** Whether the robot is within its cable's length of its reel (always true untied). */
    public boolean withinTether() {
        if (tether == null)
            return true;
        double dx = getX() - (tether.getX() + 0.5), dy = getY() - (tether.getY() + 0.5), dz = getZ() - (tether.getZ() + 0.5);
        return dx * dx + dy * dy + dz * dz <= RobotTuning.TETHER_LENGTH * RobotTuning.TETHER_LENGTH;
    }

    // --- the program's own channels --------------------------------------------------------------------

    @Override
    public float read(int channel, int argument) {
        RobotChannel c = RobotChannel.of(channel);
        if (c == null)
            return 0f;
        if (c.actuator())
            return on[channel] ? 1f : 0f;
        float shared = RobotWorker.sense(this, c, scan);
        if (!Float.isNaN(shared))
            return shared;
        return switch (c) {
            case CHARGE -> chargeShare();
            case BEACON_NEAR -> beaconNear ? 1f : 0f;
            case POS_X -> (float) (getX() - (originX + 0.5)) + errX;
            case POS_Z -> (float) (getZ() - (originZ + 0.5)) + errZ;
            case ALTITUDE -> (float) (getY() - originY);
            case HEADING -> spec.sensor().equals(RobotSpec.INERTIAL) ? facing().get2DDataValue() : 0f;
            case SLOT -> board() != null ? slot : 0f;
            case SLOT_STATE -> board() instanceof RobotBoardBlockEntity b ? b.stateFor(this, slot) : 0f;
            case SLOT_VALUE -> board() instanceof RobotBoardBlockEntity b ? b.valueOf(slot) : 0f;
            case BOARD_HEARD -> board() != null ? 1f : 0f;
            case PLACES -> placeCount();
            case PLACE_DX -> placeSet[placeAt] ? placeX[placeAt] - readX() : 0f;
            case PLACE_DZ -> placeSet[placeAt] ? placeZ[placeAt] - readZ() : 0f;
            case PLACE_DIST -> placeSet[placeAt] ? (float) Math.hypot(placeX[placeAt] - readX(), placeZ[placeAt] - readZ()) : 0f;
            case AT_PLACE -> placeSet[placeAt] && Math.hypot(placeX[placeAt] - readX(), placeZ[placeAt] - readZ()) <= RobotTuning.PLACE_NEAR ? 1f : 0f;
            default -> 0f;
        };
    }

    @Override
    public void write(int channel, boolean value) {
        RobotChannel c = RobotChannel.of(channel);
        if (c == null || !c.actuator())
            return;
        boolean was = on[channel];
        on[channel] = value;
        // the two memory switches act once, as they come on
        if (value && !was && c == RobotChannel.REMEMBER)
            remember();
        else if (value && !was && c == RobotChannel.NEXT_PLACE && spec.places() > 0)
            placeAt = (placeAt + 1) % Math.min(placeSet.length, spec.places());
        // the board switches act once as they come on, and only where a board hears this robot
        else if (value && !was && c == RobotChannel.NEXT_SLOT) {
            releaseHeld();
            slot = (slot + 1) % RobotTuning.BOARD_SLOTS;
        } else if (value && !was && c == RobotChannel.DONE) {
            if (board() instanceof RobotBoardBlockEntity b)
                b.done(this, claimedSlot >= 0 ? claimedSlot : slot);
            claimedSlot = -1;
        } else if (value && !was && c == RobotChannel.POST && board() instanceof RobotBoardBlockEntity b) {
            int stacks = 0;
            for (int i = 0; i < cargo.getContainerSize(); i++)
                if (!cargo.getItem(i).isEmpty())
                    stacks++;
            b.post(this, slot, stacks);
        }
    }

    /** The job board that hears this robot, or null; a robot with no radio part is heard by none. */
    @Nullable
    private RobotBoardBlockEntity board() {
        return spec.radio() ? RobotBoardBlockEntity.hearing(level(), this) : null;
    }

    private void releaseHeld() {
        if (claimedSlot >= 0 && board() instanceof RobotBoardBlockEntity b)
            b.release(this, claimedSlot);
        claimedSlot = -1;
    }

    /** Each step: while CLAIM is on the chosen slot is claimed or refreshed; off, the claim is let go (a done slot stays done). */
    private void boardStep() {
        if (on[RobotChannel.CLAIM.ordinal()]) {
            if (board() instanceof RobotBoardBlockEntity b && b.claim(this, slot))
                claimedSlot = slot;
        } else
            releaseHeld();
    }

    /** How many steps' worth of time its program step is stretched to by the server's step budget (1: not at all). */
    public int slowed() {
        return slow;
    }

    private float readX() {
        return (float) (getX() - (originX + 0.5)) + errX;
    }

    private float readZ() {
        return (float) (getZ() - (originZ + 0.5)) + errZ;
    }

    private int placeCount() {
        int n = 0;
        for (int i = 0; i < Math.min(placeSet.length, spec.places()); i++)
            if (placeSet[i])
                n++;
        return n;
    }

    /** Stores where it stands, as its dead reckoning has it, in the chosen place; with no lattice there is no place to keep. */
    private void remember() {
        if (spec.places() <= 0)
            return;
        placeAt = Math.min(placeAt, Math.min(placeSet.length, spec.places()) - 1);
        placeSet[placeAt] = true;
        placeX[placeAt] = readX();
        placeZ[placeAt] = readZ();
    }

    // --- the leash ---------------------------------------------------------------------------------------

    /** Whether the leash can carry now: an upgrade is fitted, the brain is live and the pack has charge. */
    public boolean leashLive() {
        return leash != null && spec.leashes() && powered();
    }

    @Nullable
    public java.util.UUID leashPair() {
        return leash;
    }

    /** Keeps the leash registry as the robot stands: joined while it can carry, out of it otherwise. */
    private void syncLeash() {
        java.util.UUID want = leashLive() ? leash : null;
        if (java.util.Objects.equals(want, leashJoined))
            return;
        if (leashJoined != null)
            RobotLeash.leave(leashJoined, this);
        leashJoined = want;
        if (want != null)
            RobotLeash.join(want, this);
    }

    /** Whether a landmark block stands within reach; a beacon wipes the position error. */
    private boolean scanBeacon() {
        int r = spec.senseReach(RobotTuning.BEACON_RANGE);
        BlockPos at = blockPosition();
        for (int dx = -r; dx <= r; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -r; dz <= r; dz++) {
                    scan.set(at.getX() + dx, at.getY() + dy, at.getZ() + dz);
                    if (level().isLoaded(scan) && level().getBlockState(scan).is(FTags.ROBOT_BEACONS))
                        return true;
                }
        return false;
    }

    /** A machine ahead that can be switched is held on or off by the SWITCH channel, once each time the program changes it. */
    private void applySwitch() {
        boolean want = on[RobotChannel.SWITCH.ordinal()];
        if (want == switched)
            return;
        switched = want;
        if (level().getBlockEntity(blockPosition().relative(facing())) instanceof io.github.soundgoodizerfan.feedback.control.Switchable sw)
            sw.setEngaged(want);
    }

    // --- chunk loading and the caps --------------------------------------------------------------------

    /** Holds the chunk it stands in loaded while a fitted loader and a running program say so, within the server's cap. */
    private void holdChunk(boolean live) {
        if (!(level() instanceof ServerLevel server))
            return;
        boolean want = spec.loader() && live && !core.getItem(0).isEmpty();
        ChunkPos here = chunkPosition();
        if (ticketChunk != NO_CHUNK && (!want || ticketChunk != here.toLong())) {
            ChunkPos old = new ChunkPos(ticketChunk);
            RobotTickets.release(server, this, old.x, old.z);
            ticketChunk = NO_CHUNK;
        }
        if (want && ticketChunk == NO_CHUNK && RobotTickets.hold(server, this, here.x, here.z))
            ticketChunk = here.toLong();
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide)
            RobotCaps.add(level());
    }

    @Override
    public void onRemovedFromLevel() {
        if (!level().isClientSide) {
            RobotCaps.remove(level());
            if (leashJoined != null) {
                RobotLeash.leave(leashJoined, this);
                leashJoined = null;
            }
            if (ticketChunk != NO_CHUNK && level() instanceof ServerLevel server) {
                ChunkPos old = new ChunkPos(ticketChunk);
                RobotTickets.release(server, this, old.x, old.z);
                ticketChunk = NO_CHUNK;
            }
        }
        super.onRemovedFromLevel();
    }

    // --- tick ------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide)
            return;
        pendingEu += RobotTuning.EU_IDLE_PER_TICK;
        if (ticketChunk != NO_CHUNK)
            pendingEu += RobotTuning.EU_LOADER_PER_TICK;
        // the brain's heater and the leash's standing power come out of the pack
        pendingEu += spec.heaterEu();
        if (leash != null && spec.leashes())
            pendingEu += RobotTuning.EU_LEASH_PER_TICK;
        boolean stepTick = tickCount % RobotTuning.STEP_TICKS == 0;
        if (stepTick)
            flush();
        // the program step asks the server's budget; refused, it keeps its state and asks again next tick (claims run on game time)
        if ((stepTick || waiting) && powered()) {
            if (RobotBudget.grant(level())) {
                core.tickServer();
                applySwitch();
                boardStep();
                slow = 1;
                waiting = false;
            } else {
                waiting = true;
                slow = Math.min(6, slow + 1);
            }
        } else if (waiting) {
            waiting = false;
            slow = 1;
        }
        if (stepTick) {
            holdChunk(powered());
            syncLeash();
            if (tickCount % (RobotTuning.STEP_TICKS * 5) == 0) {
                beaconNear = scanBeacon();
                if (beaconNear) {
                    errX = 0f;
                    errZ = 0f;
                }
            }
        }
        boolean live = powered();
        if (!live) {
            if (doing == RobotChannel.MINE)
                RobotClaims.release(level(), doingPos, owner());
            doing = null;
            for (int i = 0; i < on.length; i++)
                on[i] = false;
        }
        if (doing != null) {
            if (!on[doing.ordinal()] || !live) {
                if (doing == RobotChannel.MINE)
                    RobotClaims.release(level(), doingPos, owner());
                doing = null;
            } else if (--doingTicks <= 0) {
                finish(doing);
                doing = null;
            }
        }
        if (doing == null && live)
            begin();
        if (tether != null && tickCount % RobotTuning.STEP_TICKS == 0 && level().isLoaded(tether)
                && !level().getBlockState(tether).is(FBlocks.ROBOT_REEL.get()))
            tether = null; // the reel is gone: the cable lies slack
        boolean walking = doing == null && live && on[RobotChannel.WALK.ordinal()] && withinTether();
        boolean flying = live && spec.flies();
        Direction heading = facing();
        if (walking) {
            pendingEu += RobotTuning.EU_MOVE_PER_TICK;
            float wobble = RobotTuning.DRIFT_PER_BLOCK * spec.drift() * spec.speed();
            errX += (random.nextFloat() - 0.5f) * 2f * wobble;
            errZ += (random.nextFloat() - 0.5f) * 2f * wobble;
        }
        if (flying) {
            pendingEu += spec.hoverEu();
            float rise = on[RobotChannel.ASCEND.ordinal()] ? spec.speed() * 0.6f : on[RobotChannel.DESCEND.ordinal()] ? -spec.speed() * 0.6f : 0f;
            if (!walking && rise == 0f) {
                setDeltaMovement(0, 0, 0);
                return; // hovering: nothing to move
            }
            setDeltaMovement(walking ? heading.getStepX() * spec.speed() : 0, rise, walking ? heading.getStepZ() * spec.speed() : 0);
        } else if (spec.floats()) {
            // a gas cell holds its height with no power at all: dead, it drifts nowhere
            setDeltaMovement(0, 0, 0);
            return;
        } else if (!walking && onGround()) {
            return; // idle on the ground: nothing to move, nothing allocated
        } else {
            setDeltaMovement(walking ? heading.getStepX() * spec.speed() : 0, onGround() ? 0 : Math.max(-1.0, getDeltaMovement().y - 0.08),
                    walking ? heading.getStepZ() * spec.speed() : 0);
        }
        move(MoverType.SELF, getDeltaMovement());
    }

    /** Starts the first action the program holds on and the robot can afford. */
    private void begin() {
        BlockPos ahead = null;
        for (RobotChannel c : ORDER) {
            if (!on[c.ordinal()])
                continue;
            if (ahead == null)
                ahead = blockPosition().relative(facing()); // only when a program holds an action on
            int ticks = RobotWorker.begin(this, c, ahead);
            if (ticks < 0)
                continue;
            doing = c;
            doingTicks = ticks;
            doingPos = ahead;
            return;
        }
    }

    private void finish(RobotChannel c) {
        switch (c) {
            case TURN_LEFT -> setYRot(getYRot() - 90f);
            case TURN_RIGHT -> setYRot(getYRot() + 90f);
            default -> RobotWorker.finish(this, c, blockPosition().relative(facing()));
        }
    }

    // --- hands -----------------------------------------------------------------------------------------

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level().isClientSide)
            return InteractionResult.SUCCESS;
        if (held.getItem() instanceof DataConnectorItem) {
            // a mast selected first: pair it with this robot; otherwise step through this robot's channels
            var pending = io.github.soundgoodizerfan.feedback.control.data.DataLinkManager.get().peekPending(player);
            if (pending.isPresent()
                    && level().getBlockEntity(pending.get().pos()) instanceof RobotMastBlockEntity mast) {
                io.github.soundgoodizerfan.feedback.control.data.DataLinkManager.get().takePending(player);
                player.displayClientMessage(Component.translatable(mast.link(this) ? "robot.feedback.mast_linked" : "robot.feedback.mast_no_radio"), true);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }
        if (held.getItem() instanceof TetherCableItem) {
            BlockPos reel = TetherCableItem.reel(held);
            if (player.isShiftKeyDown() && tether != null) {
                tether = null;
                player.displayClientMessage(Component.translatable("robot.feedback.tether_cut"), true);
            } else if (reel != null) {
                tether = reel;
                player.displayClientMessage(Component.translatable("robot.feedback.tether_tied"), true);
            } else {
                player.displayClientMessage(Component.translatable("robot.feedback.tether_none"), true);
            }
            return InteractionResult.CONSUME;
        }
        if (held.getItem() instanceof io.github.soundgoodizerfan.feedback.item.EndernetUpgradeItem) {
            // an endernet upgrade fitted as the leash: its bond is this robot's, and the Data Port at the other end is its base
            java.util.UUID pair = io.github.soundgoodizerfan.feedback.item.EndernetUpgradeItem.pairOf(held);
            if (pair != null && leash == null) {
                leash = pair;
                if (!player.getAbilities().instabuild)
                    held.shrink(1);
                player.displayClientMessage(Component.translatable("robot.feedback.leash_fitted"), true);
            }
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown() && held.isEmpty()) {
            pack();
            return InteractionResult.CONSUME;
        }
        if (BatteryStack.is(held) && battery.isEmpty()) {
            battery = held.copyWithCount(1);
            refreshCharge();
            if (!player.getAbilities().instabuild)
                held.shrink(1);
            return InteractionResult.CONSUME;
        }
        if (held.is(FTags.PUNCH_CARDS) && player.isShiftKeyDown()) {
            ItemStack out = core.getItem(0);
            if (!out.isEmpty()) {
                core.setItem(0, ItemStack.EMPTY);
                player.getInventory().placeItemBackInInventory(out);
            }
            return InteractionResult.CONSUME;
        }
        if (held.is(FTags.PUNCH_CARDS)) {
            var refusal = core.refusalFor(held);
            if (refusal.isPresent()) {
                player.displayClientMessage(Component.translatable("feedback.programmer.error." + refusal.get()), true);
                return InteractionResult.CONSUME;
            }
            ItemStack old = core.getItem(0);
            core.setItem(0, held.copyWithCount(1));
            if (!player.getAbilities().instabuild)
                held.shrink(1);
            if (!old.isEmpty())
                player.getInventory().placeItemBackInInventory(old);
            return InteractionResult.CONSUME;
        }
        if (held.isEmpty() && !battery.isEmpty() && !player.isShiftKeyDown()) {
            var status = Component.translatable("robot.feedback.status", Math.round(chargeShare() * 100),
                    Component.translatable(core.getItem(0).isEmpty() ? "robot.feedback.no_program" : "robot.feedback.program"));
            player.displayClientMessage(slow > 1 ? status.append(Component.literal(", ")).append(Component.translatable("robot.feedback.slowed", slow)) : status, true);
            return InteractionResult.CONSUME;
        }
        if (held.isEmpty() && battery.isEmpty()) {
            player.displayClientMessage(Component.translatable("robot.feedback.no_battery"), true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /** The robot as its item, its battery, program and cargo inside. */
    private ItemStack asItem() {
        flush();
        ItemStack out = design().copy();
        CompoundTag tag = new CompoundTag();
        tag.put(STATE, saveState(registryAccess()));
        out.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        return out;
    }

    private void pack() {
        ItemStack out = asItem();
        spawnAtLocation(out);
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // only a blow from something (a player, a mob) knocks a robot into its item; fire, lava and the void do not
        if (level().isClientSide || isRemoved() || isInvulnerableTo(source) || source.getEntity() == null)
            return false;
        pack();
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public ItemStack getPickResult() {
        return design().copy();
    }

    // --- save ------------------------------------------------------------------------------------------

    private CompoundTag saveState(HolderLookup.Provider provider) {
        CompoundTag t = new CompoundTag();
        if (!battery.isEmpty())
            t.put("Battery", battery.save(provider));
        t.put("Cargo", cargo.createTag(provider));
        CompoundTag c = new CompoundTag();
        core.save(c, provider);
        t.put("Core", c);
        if (tether != null)
            t.putLong("Tether", tether.asLong());
        t.putInt("OriginX", originX);
        t.putInt("OriginY", originY);
        t.putInt("OriginZ", originZ);
        t.putFloat("ErrX", errX);
        t.putFloat("ErrZ", errZ);
        if (leash != null)
            t.putUUID("Leash", leash);
        t.putInt("PlaceAt", placeAt);
        t.putInt("Slot", slot);
        for (int i = 0; i < placeSet.length; i++)
            if (placeSet[i]) {
                t.putFloat("PlaceX" + i, placeX[i]);
                t.putFloat("PlaceZ" + i, placeZ[i]);
            }
        return t;
    }

    private void loadState(CompoundTag t, HolderLookup.Provider provider) {
        battery = t.contains("Battery") ? ItemStack.parseOptional(provider, t.getCompound("Battery")) : ItemStack.EMPTY;
        if (t.contains("Cargo", Tag.TAG_LIST))
            cargo.fromTag(t.getList("Cargo", Tag.TAG_COMPOUND), provider);
        core.load(t.getCompound("Core"), provider);
        tether = t.contains("Tether") ? BlockPos.of(t.getLong("Tether")) : null;
        if (t.contains("OriginX")) {
            originX = t.getInt("OriginX");
            originY = t.getInt("OriginY");
            originZ = t.getInt("OriginZ");
        }
        errX = t.getFloat("ErrX");
        errZ = t.getFloat("ErrZ");
        leash = t.hasUUID("Leash") ? t.getUUID("Leash") : null;
        placeAt = Math.max(0, Math.min(placeSet.length - 1, t.getInt("PlaceAt")));
        slot = Math.max(0, Math.min(RobotTuning.BOARD_SLOTS - 1, t.getInt("Slot")));
        for (int i = 0; i < placeSet.length; i++) {
            placeSet[i] = t.contains("PlaceX" + i);
            placeX[i] = t.getFloat("PlaceX" + i);
            placeZ[i] = t.getFloat("PlaceZ" + i);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Design", design().saveOptional(registryAccess()));
        tag.put(STATE, saveState(registryAccess()));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ItemStack design = ItemStack.parseOptional(registryAccess(), tag.getCompound("Design"));
        entityData.set(DESIGN, design);
        spec = RobotSpec.of(design);
        loadState(tag.getCompound(STATE), registryAccess());
        refreshCharge();
    }
}
