// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.thermal;

import net.minecraft.tags.FluidTags;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import io.github.soundgoodizerfan.feedback.core.hazard.HotContact;
import io.github.soundgoodizerfan.feedback.registry.FTags;

/**
 * A fire. Something that holds a flame temperature, which is the ceiling on anything it heats.
 *
 * <h2>The source decides where its heat goes</h2>
 * A source has a {@link #heatFacing() facing}. A heat-accepting block touching that face gets the
 * source's whole temperature; one touching a side next to it gets {@link #sideShare()} of the way
 * there from ambient (heat lost on the way round), and one behind it gets {@link #backShare()}
 * (none). Fire sources always face up and keep the old fire-under-a-vessel layout; a coil, an arc
 * head or a creative source can be rotated. A source whose heat is very concentrated overrides
 * the shares. The receiver only sums what the sources around it give; it holds no opinion on
 * geometry, which is why {@link #at} is the one reader.
 *
 * <h2>Why a source is not a {@link ThermalBody}</h2>
 * A body has a mass and a temperature that other things change. A fire has a temperature and no
 * mass worth modelling -- nothing the player does cools a fire, they either feed it or they do
 * not. Keeping them apart is what stops the crucible needing to know whether it is over charcoal,
 * over lava, or over something a later slice invents: it asks the block below how hot it burns and
 * has no further opinion.
 */
public interface HeatSource {

    /** How hot this burns right now. Ambient when it is not lit. */
    Tu getFireTu();

    /**
     * What this did across {@code gap} ticks that nobody was there to tick, and have it spend the
     * fuel it burned. Called by whatever sits on top of it as it comes back from an unloaded
     * chunk -- see {@link Catchup}. Nothing by default: a source with no fuel (an element, a
     * flame that is only ever there) has nothing to have run out of.
     */
    default FireSpan catchUp(long gap) {
        return FireSpan.NONE;
    }

    /** The face this source radiates from. Fire sources are always up. */
    default Direction heatFacing() {
        return Direction.UP;
    }

    /** Reach on the four sides next to the facing. A concentrated source (arc, plasma) returns 1. */
    default float sideShare() {
        return FTuning.HEAT_SIDE_SHARE;
    }

    /** Reach on the side behind the facing. Nothing by default. */
    default float backShare() {
        return 0f;
    }

    /**
     * Whether this source heats what stands at {@code target}. True by default; an induction source says no unless the receiver is
     * the kind of thing a field can heat (a metal charge, magnetic cookware). {@link #at} and {@link #spanAt} skip a source that says no.
     */
    default boolean heats(Level level, BlockPos target) {
        return true;
    }

    /** The facing a rotatable source block holds, up when its block has none. */
    static Direction facingOf(BlockState state) {
        return state.hasProperty(BlockStateProperties.FACING) ? state.getValue(BlockStateProperties.FACING) : Direction.UP;
    }

    /** How much of this source's heat reaches the block {@code toward} it (the direction from the source to that block). */
    default float shareToward(Direction toward) {
        Direction face = heatFacing();
        if (toward == face)
            return 1f;
        if (toward == face.getOpposite())
            return backShare();
        return sideShare();
    }


    // --- who reaches a block --------------------------------------------------------------------

    /** The six face neighbours, then the twelve edge neighbours (two steps along two axes): where a source that reaches a block can stand. */
    int REACH = 18;

    Direction[] DIRS = Direction.values();

    /**
     * The source that could reach {@code pos} from candidate {@code i} (0-5: the block on that face; 6-17: a block touching an edge),
     * or null. A face neighbour always reaches; an edge neighbour reaches only when it points at a block next to {@code pos}, which is
     * to say it faces one of the two faces it touches {@code pos} across (the block where it points, and the four round that, are lit).
     */
    private static HeatSource reaching(Level level, BlockPos pos, int i) {
        if (i < 6)
            return level.getBlockEntity(pos.relative(DIRS[i])) instanceof HeatSource source ? source : null;
        int n = i - 6, pair = n / 4, signs = n % 4;
        // pair 0: x,y   pair 1: x,z   pair 2: y,z; signs pick the direction on each axis
        Direction.Axis first = pair < 2 ? Direction.Axis.X : Direction.Axis.Y, second = pair == 0 ? Direction.Axis.Y : Direction.Axis.Z;
        Direction da = Direction.fromAxisAndDirection(first, (signs & 1) == 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
        Direction db = Direction.fromAxisAndDirection(second, (signs & 2) == 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
        if (!(level.getBlockEntity(pos.relative(da).relative(db)) instanceof HeatSource source))
            return null;
        Direction face = source.heatFacing();
        return face == da.getOpposite() || face == db.getOpposite() ? source : null;
    }

    /** The share {@code source}, found as candidate {@code i} of {@link #reaching}, gives: the face neighbours by facing, an edge neighbour its side share. */
    private static float shareAt(HeatSource source, int i) {
        return i < 6 ? source.shareToward(DIRS[i].getOpposite()) : source.sideShare();
    }


    // --- what a source pushes -------------------------------------------------------------------

    /**
     * Called from a source's own server tick: pushes its heat into the space it reaches, the same blocks {@link #at} reads from the
     * other end (the block it points at, the four round that, the four beside the source, and the back if it has a share). Every
     * {@link FTuning#HEAT_PUSH_INTERVAL} ticks, and only when hot:
     * <ul>
     *   <li>a block with an item handler warms the stacks inside toward that spot's heat ({@link FTuning#HEAT_PUSH_ITEM_GRIP} of the
     *       gap a tick), unless it is a {@link ThermalBody} (a vessel heats its own contents through {@link #at}) or in the
     *       {@link FTags#NO_HEAT_PUSH} block tag, which is how a block opts out (anything else that reads {@link #at} itself);</li>
     *   <li>a dropped item there warms the same way;</li>
     *   <li>once a second, a living thing standing there is burned as a piece at that heat would burn it ({@link HotContact#burn}),
     *       so the band, and with it the damage, follows how hot the spot is.</li>
     * </ul>
     */
    static <T extends BlockEntity & HeatSource> void radiate(T self) {
        Level level = self.getLevel();
        if (level == null || level.isClientSide)
            return;
        long beat = Math.floorMod(level.getGameTime() + self.getBlockPos().asLong(), (long) FTuning.HEAT_PUSH_INTERVAL);
        if (beat != 0L)
            return;
        float fire = self.getFireTu().value();
        if (fire <= FTuning.WARM_TU.value())
            return;
        boolean scorch = (level.getGameTime() / FTuning.HEAT_PUSH_INTERVAL) % FTuning.HEAT_SCORCH_EVERY == 0;
        float ambient = AmbientTable.at(level).value();
        Direction face = self.heatFacing();
        BlockPos at = self.getBlockPos(), front = at.relative(face);
        reach(level, self, front, 1f, ambient, fire, scorch);
        float side = self.sideShare();
        if (side > 0f)
            for (Direction d : DIRS)
                if (d.getAxis() != face.getAxis()) {
                    reach(level, self, front.relative(d), side, ambient, fire, scorch);
                    reach(level, self, at.relative(d), side, ambient, fire, scorch);
                }
        if (self.backShare() > 0f)
            reach(level, self, at.relative(face.getOpposite()), self.backShare(), ambient, fire, scorch);
    }

    private static void reach(Level level, HeatSource source, BlockPos target, float share, float ambient, float fire, boolean scorch) {
        float tu = ambient + share * (fire - ambient);
        if (tu <= FTuning.WARM_TU.value() || !source.heats(level, target))
            return;
        BlockEntity be = level.getBlockEntity(target);
        if (be != null && !(be instanceof ThermalBody) && !be.getBlockState().is(FTags.NO_HEAT_PUSH)) {
            IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, target, null);
            if (items != null) {
                boolean changed = false;
                for (int slot = 0; slot < items.getSlots(); slot++)
                    changed |= warm(items.getStackInSlot(slot), tu, level);
                if (changed)
                    be.setChanged();
            }
        }
        AABB space = new AABB(target);
        for (ItemEntity dropped : level.getEntitiesOfClass(ItemEntity.class, space))
            warm(dropped.getItem(), tu, level);
        if (scorch && tu >= FTuning.CONTACT_PAIN_TU.value())
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, space))
                HotContact.burn(victim, new Tu(tu));
    }

    private static boolean warm(ItemStack stack, float tu, Level level) {
        if (stack.isEmpty())
            return false;
        float now = ItemHeat.get(stack, level).value();
        if (tu <= now)
            return false;
        ItemHeat.set(stack, new Tu(now + FTuning.HEAT_PUSH_ITEM_GAIN * (tu - now)), level);
        return true;
    }

    /** {@link #at} across a gap: the fire the sources round {@code pos} gave over {@code gap} ticks. */
    static FireSpan spanAt(Level level, BlockPos pos, long gap) {
        FireSpan best = FireSpan.NONE;
        Tu ambient = null;
        for (int i = 0; i < REACH; i++) {
            HeatSource source = reaching(level, pos, i);
            if (source == null)
                continue;
            float share = shareAt(source, i);
            if (share <= 0f || !source.heats(level, pos))
                continue;
            FireSpan span = source.catchUp(gap).clampedTo(gap);
            if (span.litTicks() <= 0L)
                continue;
            if (ambient == null)
                ambient = AmbientTable.at(level);
            float tu = ambient.value() + share * (span.tu() - ambient.value());
            if (tu > best.tu())
                best = new FireSpan(span.litTicks(), tu);
        }
        if (level.getFluidState(pos.below()).is(FluidTags.LAVA) && FTuning.FIRE_TU_LAVA.value() > best.tu())
            return new FireSpan(gap, FTuning.FIRE_TU_LAVA.value());
        return best;
    }

    /**
     * How cold the cold reaching {@code pos} is: the coldest of the sources round it that hold a temperature below ambient, each
     * at the share its own facing gives this side. Ambient when none does. {@link #at} ignores those sources; a vessel that can be
     * chilled (the reactor) reads this when no fire reaches it.
     */
    static Tu coldAt(Level level, BlockPos pos) {
        Tu ambient = AmbientTable.at(level);
        float best = ambient.value();
        for (int i = 0; i < REACH; i++) {
            HeatSource source = reaching(level, pos, i);
            if (source == null)
                continue;
            float share = shareAt(source, i);
            float fire = source.getFireTu().value();
            if (share <= 0f || fire >= ambient.value() || !source.heats(level, pos))
                continue;
            best = Math.min(best, ambient.value() + share * (fire - ambient.value()));
        }
        return new Tu(best);
    }

    /**
     * How hot the heat reaching {@code pos} is: the hottest of the sources round it, each at the
     * share its own facing gives this side.
     *
     * <h3>Lava is handled here and nowhere else</h3>
     * It is a plain vanilla block with no block entity, so it cannot push. The block under a
     * position being lava is the one special case, as it always was, and by tag so any mod's lava
     * counts.
     */
    static Tu at(Level level, BlockPos pos) {
        Tu ambient = AmbientTable.at(level);
        float best = ambient.value();
        for (int i = 0; i < REACH; i++) {
            HeatSource source = reaching(level, pos, i);
            if (source == null)
                continue;
            float share = shareAt(source, i);
            if (share <= 0f || !source.heats(level, pos))
                continue;
            float fire = source.getFireTu().value();
            if (fire <= ambient.value())
                continue;
            best = Math.max(best, ambient.value() + share * (fire - ambient.value()));
        }
        if (level.getFluidState(pos.below()).is(FluidTags.LAVA))
            best = Math.max(best, FTuning.FIRE_TU_LAVA.value());
        return new Tu(best);
    }
}
