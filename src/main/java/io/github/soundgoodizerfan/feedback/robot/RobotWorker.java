// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

import com.mojang.authlib.GameProfile;

import io.github.soundgoodizerfan.feedback.control.data.DataLinkManager;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * What a robot does to the world, shared by the {@link RobotEntity} that rolls about and the arm on its mount ({@code RobotArmBlockEntity}):
 * each works from a {@link Body} that says where it stands, which way it faces, what it carries and what it can pay. Breaking goes through
 * the real break event on one shared fake player per level; the drops are the block's own loot, whatever the tool.
 */
public final class RobotWorker {

    private RobotWorker() {
    }

    /** The thing doing the work: a robot or an arm. */
    public interface Body {
        Level level();

        BlockPos pos();

        Direction facing();

        Vec3 centre();

        RobotSpec spec();

        SimpleContainer cargo();

        /** An id that tells this body's block claims from another's. */
        long owner();

        boolean afford(float eu);

        void spend(float eu);
    }

    private static final GameProfile PROFILE = new GameProfile(UUID.nameUUIDFromBytes("feedback:robot".getBytes()), "[Robot]");

    // --- what it senses ahead --------------------------------------------------------------------------

    /** A sense never loads a chunk: ground that is not loaded reads as solid and unbreakable. */
    public static boolean solid(Body b, BlockPos pos) {
        return !b.level().isLoaded(pos) || !b.level().getBlockState(pos).getCollisionShape(b.level(), pos).isEmpty();
    }

    public static boolean breakable(Body b, BlockPos pos) {
        if (!b.level().isLoaded(pos))
            return false;
        BlockState state = b.level().getBlockState(pos);
        return !state.isAir() && state.getFluidState().isEmpty() && b.spec().canBreak(state.getDestroySpeed(b.level(), pos));
    }

    public static List<ItemEntity> items(Level level, BlockPos pos) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(0.25));
    }

    public static boolean cargoFull(SimpleContainer cargo) {
        for (int i = 0; i < cargo.getContainerSize(); i++)
            if (cargo.getItem(i).isEmpty())
                return false;
        return true;
    }

    /** Blocks to the first solid thing ahead, as far as a rangefinder reaches. */
    public static float range(Body b, BlockPos.MutableBlockPos scan) {
        // the brain's gain lengthens the reach
        int reach = b.spec().senseReach(RobotTuning.RANGE_MAX);
        for (int i = 1; i <= reach; i++) {
            scan.set(b.pos()).move(b.facing(), i);
            if (solid(b, scan))
                return i;
        }
        return reach;
    }

    public static float hardness(Body b, BlockPos pos) {
        if (!b.level().isLoaded(pos))
            return 100f;
        BlockState state = b.level().getBlockState(pos);
        if (state.isAir())
            return 0f;
        float h = state.getDestroySpeed(b.level(), pos);
        return h < 0f ? 100f : h;
    }

    /**
     * The senses a robot and an arm share, or NaN for a channel that is not one of them. A sense that needs a fitted sensor reads 0 without it.
     */
    public static float sense(Body b, RobotChannel c, BlockPos.MutableBlockPos scan) {
        BlockPos ahead = b.pos().relative(b.facing());
        RobotSpec spec = b.spec();
        return switch (c) {
            case SOLID_AHEAD -> solid(b, ahead) ? 1f : 0f;
            case BREAKABLE_AHEAD -> breakable(b, ahead) ? 1f : 0f;
            case ITEM_AHEAD -> items(b.level(), ahead).isEmpty() ? 0f : 1f;
            case CARGO_FULL -> cargoFull(b.cargo()) ? 1f : 0f;
            case DOCK_AHEAD -> b.level().isLoaded(ahead) && b.level().getBlockState(ahead).is(FTags.ROBOT_DOCKS) ? 1f : 0f;
            case DOCK_FREE -> b.level().isLoaded(ahead) && b.level().getBlockEntity(ahead) instanceof RobotDockBlockEntity d && d.isFree() ? 1f : 0f;
            case LASER_RANGE -> b.level().isLoaded(ahead) && b.level().getBlockEntity(ahead) instanceof io.github.soundgoodizerfan.feedback.machine.laser.LaserBlockEntity laser ? laser.read(0) : 0f;
            // a number as the brain gives it: wobbled by its noise, a robot with no brain most
            case RANGE_AHEAD -> spec.sensor().equals(RobotSpec.RANGEFINDER) ? spec.senseJitter(range(b, scan), b.level().random) : 0f;
            case HARDNESS_AHEAD -> spec.sensor().equals(RobotSpec.PROBE) ? spec.senseJitter(hardness(b, ahead), b.level().random) : 0f;
            default -> Float.NaN;
        };
    }

    // --- what it does ----------------------------------------------------------------------------------

    /**
     * Ticks the action takes, or -1 if it cannot start now (nothing to break, no money, a block another robot has claimed). A mine claims
     * the block for those ticks.
     */
    public static int begin(Body b, RobotChannel c, BlockPos ahead) {
        return switch (c) {
            case MINE -> {
                if (!breakable(b, ahead))
                    yield -1;
                float h = b.level().getBlockState(ahead).getDestroySpeed(b.level(), ahead);
                if (!b.afford(h * RobotTuning.EU_PER_HARDNESS))
                    yield -1;
                int ticks = b.spec().digTicks(h);
                if (!RobotClaims.claim(b.level(), ahead, b.owner(), b.level().getGameTime() + ticks + RobotTuning.STEP_TICKS))
                    yield -1;
                yield ticks;
            }
            case TURN_LEFT, TURN_RIGHT -> RobotTuning.TURN_TICKS;
            case PLACE, PICK_UP, DROP, GIVE, TAKE, USE -> RobotTuning.PLACE_TICKS;
            default -> -1;
        };
    }

    /** Carries out a finished action on the space {@code ahead}. Turning is the body's own. */
    public static void finish(Body b, RobotChannel c, BlockPos ahead) {
        switch (c) {
            case MINE -> mine(b, ahead);
            case PLACE -> place(b, ahead);
            case PICK_UP -> pickUp(b, ahead);
            case DROP -> drop(b, ahead);
            case GIVE -> give(b, ahead);
            case TAKE -> take(b, ahead);
            case USE -> use(b, ahead);
            default -> { }
        }
    }

    /**
     * Uses the first stack it carries on the block ahead, as a hand does through the block's own use: lay food in a pan, flip a piece with a
     * spatula, pour from a bottle. The hand lands where it aims only as well as its dexterity: a clumsy one lands off the centre of the face,
     * so it may lay in the wrong place. Never refused, only worse.
     */
    private static void use(Body b, BlockPos pos) {
        if (!(b.level() instanceof ServerLevel server) || !server.isLoaded(pos))
            return;
        var fake = actor(b, server);
        ItemStack held = ItemStack.EMPTY;
        int slot = -1;
        SimpleContainer cargo = b.cargo();
        for (int i = 0; i < cargo.getContainerSize(); i++)
            if (!cargo.getItem(i).isEmpty()) {
                held = cargo.getItem(i);
                slot = i;
                break;
            }
        Direction face = b.facing().getOpposite();
        float slack = (1f - b.spec().dexterity()) * 0.45f;
        double jitter = (server.random.nextDouble() - 0.5) * 2 * slack, jitter2 = (server.random.nextDouble() - 0.5) * 2 * slack;
        Vec3 at = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5 + (face.getAxis() == Direction.Axis.X ? 0 : jitter),
                face.getStepY() * 0.5 + (face.getAxis() == Direction.Axis.Y ? 0 : jitter2),
                face.getStepZ() * 0.5 + (face.getAxis() == Direction.Axis.Z ? 0 : jitter));
        fake.setItemInHand(InteractionHand.MAIN_HAND, held);
        var hit = new BlockHitResult(at, face, pos, false);
        var result = server.getBlockState(pos).useItemOn(held, server, fake, InteractionHand.MAIN_HAND, hit);
        if (!result.consumesAction())
            server.getBlockState(pos).useWithoutItem(server, fake, hit);
        ItemStack after = fake.getItemInHand(InteractionHand.MAIN_HAND);
        fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        if (slot >= 0)
            cargo.setItem(slot, after);
        else if (!after.isEmpty())
            drain(b, after);
        // whatever a use put in the fake's pockets (a bottle that came back filled) is the robot's, never the next robot's
        var pockets = fake.getInventory();
        for (int i = 0; i < pockets.getContainerSize(); i++) {
            ItemStack got = pockets.getItem(i);
            if (got.isEmpty())
                continue;
            pockets.setItem(i, ItemStack.EMPTY);
            drain(b, got);
        }
    }

    /** Puts a stack into the cargo; what does not fit lies on the ground ahead of the body. */
    private static void drain(Body b, ItemStack stack) {
        ItemStack left = b.cargo().addItem(stack);
        if (!left.isEmpty())
            b.level().addFreshEntity(new ItemEntity(b.level(), b.centre().x, b.centre().y + 0.3, b.centre().z, left));
    }

    private static net.neoforged.neoforge.common.util.FakePlayer actor(Body b, ServerLevel server) {
        var fake = FakePlayerFactory.get(server, PROFILE);
        fake.setPos(b.centre().x, b.centre().y, b.centre().z); // a claim or protection check sees where the robot stands
        return fake;
    }

    /** Breaks the block through the real break event (a protection mod may veto it), paying Work from the battery; drops land in the world. */
    private static void mine(Body b, BlockPos pos) {
        RobotClaims.release(b.level(), pos, b.owner());
        if (!(b.level() instanceof ServerLevel server) || !breakable(b, pos))
            return;
        BlockState state = server.getBlockState(pos);
        float eu = state.getDestroySpeed(server, pos) * RobotTuning.EU_PER_HARDNESS;
        if (!b.afford(eu))
            return;
        var fake = actor(b, server);
        if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(server, pos, state, fake)).isCanceled())
            return;
        b.spend(eu);
        server.destroyBlock(pos, true, fake);
    }

    private static void place(Body b, BlockPos pos) {
        if (!(b.level() instanceof ServerLevel server))
            return;
        SimpleContainer cargo = b.cargo();
        for (int i = 0; i < cargo.getContainerSize(); i++) {
            ItemStack s = cargo.getItem(i);
            if (!(s.getItem() instanceof BlockItem block))
                continue;
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            var context = new BlockPlaceContext(server, actor(b, server), InteractionHand.MAIN_HAND, s, hit);
            if (context.canPlace() && block.place(context).consumesAction())
                return;
        }
    }

    private static IItemHandler port(Body b, BlockPos pos) {
        return b.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, b.facing().getOpposite());
    }

    /** Offers the first stack it carries to the item port ahead; what the port will not take stays in the cargo. */
    private static void give(Body b, BlockPos pos) {
        IItemHandler handler = port(b, pos);
        if (handler == null)
            return;
        SimpleContainer cargo = b.cargo();
        for (int i = 0; i < cargo.getContainerSize(); i++) {
            ItemStack s = cargo.getItem(i);
            if (s.isEmpty())
                continue;
            cargo.setItem(i, ItemHandlerHelper.insertItem(handler, s, false));
            return;
        }
    }

    /** Takes the first stack the item port ahead lets out, as much as the cargo has room for. */
    private static void take(Body b, BlockPos pos) {
        IItemHandler handler = port(b, pos);
        if (handler == null)
            return;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack peek = handler.extractItem(i, 64, true);
            // only what the whole cargo can take is extracted: a port that refuses a stack back (an output slot) must not lose a remainder
            if (peek.isEmpty() || !b.cargo().canAddItem(peek))
                continue;
            b.cargo().addItem(handler.extractItem(i, peek.getCount(), false));
            return;
        }
    }

    private static void pickUp(Body b, BlockPos pos) {
        for (ItemEntity item : items(b.level(), pos)) {
            ItemStack left = b.cargo().addItem(item.getItem().copy());
            if (left.isEmpty())
                item.discard();
            else
                item.setItem(left);
        }
    }

    private static void drop(Body b, BlockPos pos) {
        SimpleContainer cargo = b.cargo();
        for (int i = 0; i < cargo.getContainerSize(); i++) {
            ItemStack s = cargo.getItem(i);
            if (s.isEmpty())
                continue;
            ItemEntity out = new ItemEntity(b.level(), pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, s.copy());
            out.setDeltaMovement(Vec3.ZERO);
            b.level().addFreshEntity(out);
            cargo.setItem(i, ItemStack.EMPTY);
            return;
        }
    }

}
