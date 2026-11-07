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
package io.github.soundgoodizerfan.feedback.machine.pivot;

import java.util.Collections;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.control.ChannelSource;
import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.machine.HeadSlot;
import io.github.soundgoodizerfan.feedback.machine.Headed;
import io.github.soundgoodizerfan.feedback.machine.sensor.SensorBlock;
import io.github.soundgoodizerfan.feedback.machine.sensor.SensorKind;
import io.github.soundgoodizerfan.feedback.registry.FAutomation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The pivot mast: a vertical shaft whose arm sweeps a circle. The arm is a seated item (its length is the radius) drawn by the renderer; it
 * is not made of blocks. The tip applies its operation ({@link PivotTip}) to every cell the arm crosses, at the mast's own height, along
 * the whole arm: it knows nothing about the job, so a cutting tip cuts green crop and a breaking tip keeps hitting what it has loosened.
 *
 * <h2>Load</h2>
 * A lever: {@link FTuning#PIVOT_BASE_SU} plus, per block of arm, {@link FTuning#PIVOT_SU_PER_BLOCK} and, with a head seated,
 * {@link FTuning#PIVOT_HEAD_SU_PER_BLOCK}. A long arm with a heavy head stalls a weak drive.
 *
 * <h2>Wear</h2>
 * Only the seated head wears, and only from misuse: a breaking tip on a block it cannot break absorbs the difference, a cutting tip on
 * anything harder than a plant, a striking tip on whatever it hits. No head seated wears nothing and cuts, breaks and strikes nothing.
 *
 * <h2>Inventory</h2>
 * Slot 0 takes the item a planting or feeding tip uses; slots 1 to {@link #OUT} hold what a collecting tip sweeps up. A hopper or belt
 * can fill the first and empty the rest.
 */
public class PivotMastBlockEntity extends RotationNode implements Switchable, DataNode, ChannelSource, Headed {

    /** Output slots after the input slot. */
    public static final int OUT = 9;

    private ItemStack arm = ItemStack.EMPTY;
    private ItemStack tip = ItemStack.EMPTY;
    private ItemStack sensor = ItemStack.EMPTY;
    private final HeadSlot head = new HeadSlot();
    private boolean engaged = true;
    private float lastAngle = Float.NaN;
    /** The arm's own angle, 0-360. The shaft phase wraps every turn, so the gearing must be accumulated, not multiplied. */
    private float armDeg;
    private float lastShaft = Float.NaN;

    private final ItemStackHandler items = new ItemStackHandler(1 + OUT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public PivotMastBlockEntity(BlockPos pos, BlockState state) {
        super(FAutomation.PIVOT_ENTITY.get(), pos, state);
    }

    // --- state --------------------------------------------------------------------------------

    public int armLength() {
        return arm.getItem() instanceof PivotArmItem a ? a.length() : 0;
    }

    public PivotTip tipKind() {
        return tip.getItem() instanceof PivotTipItem t ? t.tip() : null;
    }

    public boolean hasSensor() {
        return !sensor.isEmpty();
    }

    @Override
    public HeadSlot getHead() {
        return head;
    }

    /** The mast's item handler: slot 0 in, the rest out. Hoppers and belts see this. */
    public IItemHandler handler(@org.jetbrains.annotations.Nullable Direction side) {
        return new net.neoforged.neoforge.items.wrapper.CombinedInvWrapper(items) {
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return slot == 0 ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return slot == 0 ? super.insertItem(slot, stack, simulate) : stack;
            }
        };
    }

    @Override
    public Su getLoadSu() {
        int len = armLength();
        if (len == 0)
            return NO_SU;
        float perBlock = FTuning.PIVOT_SU_PER_BLOCK + (head.isEmpty() ? 0f : FTuning.PIVOT_HEAD_SU_PER_BLOCK);
        return new Su(FTuning.PIVOT_BASE_SU + len * perBlock);
    }

    // --- interaction --------------------------------------------------------------------------

    /** Seats an arm, a tip, a head or a sensor block item, or puts anything else in the input slot. Returns whether it took something. */
    public boolean accept(ItemStack stack, Player player) {
        ItemStack one = stack.copyWithCount(1);
        ItemStack back = ItemStack.EMPTY;
        if (stack.getItem() instanceof PivotArmItem) {
            back = arm;
            arm = one;
            lastAngle = Float.NaN;
        } else if (stack.getItem() instanceof PivotTipItem) {
            back = tip;
            tip = one;
        } else if (stack.is(io.github.soundgoodizerfan.feedback.registry.FModular.HAND_HAMMER.get())) {
            back = head.swap(stack.copy());
            if (!player.getAbilities().instabuild)
                stack.shrink(1);
            give(player, back);
            return true;
        } else if (stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof SensorBlock sb && sb.kind() != SensorKind.TELLTALE) {
            back = sensor;
            sensor = one;
        } else {
            ItemStack left = items.insertItem(0, stack.copy(), false);
            boolean took = left.getCount() != stack.getCount();
            if (took && !player.getAbilities().instabuild)
                stack.setCount(left.getCount());
            return took;
        }
        if (!player.getAbilities().instabuild)
            stack.shrink(1);
        give(player, back);
        return true;
    }

    private static void give(Player player, ItemStack back) {
        if (!back.isEmpty() && !player.getInventory().add(back))
            player.drop(back, false);
    }

    /** One part back out, in the order tip, head, sensor, arm, then the input item. */
    public ItemStack takeOne() {
        ItemStack out;
        if (!tip.isEmpty()) {
            out = tip;
            tip = ItemStack.EMPTY;
        } else if (!head.isEmpty()) {
            out = head.take();
        } else if (!sensor.isEmpty()) {
            out = sensor;
            sensor = ItemStack.EMPTY;
        } else if (!arm.isEmpty()) {
            out = arm;
            arm = ItemStack.EMPTY;
        } else {
            out = items.extractItem(0, 64, false);
            if (out.isEmpty())
                for (int i = 1; i <= OUT && out.isEmpty(); i++)
                    out = items.extractItem(i, 64, false);
        }
        return out;
    }

    public void dropAll() {
        if (level == null)
            return;
        for (ItemStack s : new ItemStack[]{arm, tip, sensor, head.take()})
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), s);
        for (int i = 0; i < items.getSlots(); i++)
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), items.getStackInSlot(i));
    }

    public Component report() {
        if (arm.isEmpty())
            return Component.translatable("feedback.pivot.no_arm");
        PivotTip kind = tipKind();
        return Component.translatable("feedback.pivot.report", armLength(),
                kind == null ? Component.translatable("feedback.pivot.no_tip") : Component.translatable("feedback.pivot.tip." + kind.id()),
                Math.round(head.condition() * 100));
    }

    public void refresh() {
        setChanged();
        if (level != null)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // --- the sweep ----------------------------------------------------------------------------

    /** The arm's angle in degrees: the shaft's turning through the arm's reduction gearing, accumulated. */
    private float armAngle() {
        return armDeg;
    }

    private void trackArm() {
        float shaft = network == null ? visualAngle : network.getPhase() * ratio;
        if (!Float.isNaN(lastShaft))
            armDeg = ((armDeg + (((shaft - lastShaft) % 360f + 540f) % 360f - 180f) * FTuning.PIVOT_GEAR) % 360f + 360f) % 360f;
        lastShaft = shaft;
    }

    @Override
    public void tickClient() {
        super.tickClient();
        trackArm();
    }

    /** The arm's angle at a point partway through the tick, for drawing. */
    public float armAngle(float partialTick) {
        return armDeg + (getVisualAngle(partialTick) - visualAngle) * FTuning.PIVOT_GEAR;
    }

    /** The cell the arm tip stands over, `radius` blocks out at `degrees`. */
    private BlockPos cell(double degrees, int radius) {
        double rad = Math.toRadians(degrees);
        return worldPosition.offset((int) Math.round(Math.cos(rad) * radius), 0, (int) Math.round(Math.sin(rad) * radius));
    }

    public void tickServer() {
        if (!(level instanceof ServerLevel sl))
            return;
        trackArm();
        float now = armAngle();
        if (Float.isNaN(lastAngle)) {
            lastAngle = now;
            return;
        }
        float delta = ((now - lastAngle) % 360f + 540f) % 360f - 180f;
        lastAngle = now;
        PivotTip kind = tipKind();
        int len = armLength();
        if (!engaged || kind == null || len == 0 || delta == 0f || (kind.needsHead() && head.isEmpty()))
            return;
        int steps = Math.max(1, Math.min(4, (int) Math.ceil(Math.abs(Math.toRadians(delta)) * len)));
        long[] done = new long[FTuning.PIVOT_CELLS_PER_TICK];
        int count = 0;
        for (int s = 1; s <= steps && count < done.length; s++) {
            double angle = now - delta + delta * s / steps;
            for (int r = 1; r <= len && count < done.length; r++) {
                BlockPos c = cell(angle, r);
                long key = c.asLong();
                boolean seen = false;
                for (int i = 0; i < count; i++)
                    if (done[i] == key) {
                        seen = true;
                        break;
                    }
                if (seen)
                    continue;
                done[count++] = key;
                work(sl, kind, c);
            }
        }
    }

    private void work(ServerLevel sl, PivotTip kind, BlockPos c) {
        switch (kind) {
            case CUT -> cut(sl, c);
            case BREAK -> breakBlock(sl, c);
            case PLANT -> plant(sl, c);
            case FEED -> feed(sl, c);
            case STRIKE -> strike(sl, c);
            case COLLECT -> collect(sl, c);
        }
    }

    private void wear(float stAbsorbed) {
        if (head.wear(stAbsorbed, FTuning.PIVOT_HEAD_WEAR_PER_ST, FTuning.PIVOT_HEAD_FLOOR))
            refresh();
    }

    private void cut(ServerLevel sl, BlockPos c) {
        float ceiling = head.ceiling().value();
        BlockState state = sl.getBlockState(c);
        if (!state.isAir() && state.is(BlockTags.SWORD_EFFICIENT) && sl.getBlockEntity(c) == null) {
            float need = Math.max(0f, state.getDestroySpeed(sl, c)) * FTuning.PIVOT_BREAK_ST_PER_HARDNESS;
            if (ceiling >= need) {
                sl.destroyBlock(c, true);
                wear(need * 0.1f + 0.1f);
            }
        }
        FakePlayer fake = FakePlayerFactory.getMinecraft(sl);
        ItemStack shears = new ItemStack(Items.SHEARS);
        for (Entity e : sl.getEntities((Entity) null, new AABB(c), x -> x instanceof IShearable)) {
            IShearable sh = (IShearable) e;
            if (ceiling > 0f && sh.isShearable(fake, shears, sl, e.blockPosition())) {
                for (ItemStack drop : sh.onSheared(fake, shears, sl, e.blockPosition()))
                    e.spawnAtLocation(drop);
                wear(0.5f);
            }
        }
    }

    private void breakBlock(ServerLevel sl, BlockPos c) {
        BlockState state = sl.getBlockState(c);
        if (state.isAir() || c.equals(worldPosition) || sl.getBlockEntity(c) != null || !state.getFluidState().isEmpty())
            return;
        float hardness = state.getDestroySpeed(sl, c);
        if (hardness < 0f)
            return;
        float need = hardness * FTuning.PIVOT_BREAK_ST_PER_HARDNESS;
        float ceiling = head.ceiling().value();
        if (ceiling >= need) {
            sl.destroyBlock(c, true);
            wear(need * 0.1f + 0.1f);
        } else {
            // Force that cannot go into the work goes into the machine.
            wear(need - ceiling);
        }
    }

    private void plant(ServerLevel sl, BlockPos c) {
        ItemStack stack = items.getStackInSlot(0);
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem bi) || !sl.isEmptyBlock(c))
            return;
        BlockState state = bi.getBlock().defaultBlockState();
        if (!state.canSurvive(sl, c))
            return;
        sl.setBlock(c, state, 3);
        stack.shrink(1);
        setChanged();
    }

    private void feed(ServerLevel sl, BlockPos c) {
        ItemStack stack = items.getStackInSlot(0);
        if (stack.isEmpty())
            return;
        FakePlayer fake = FakePlayerFactory.getMinecraft(sl);
        fake.setPos(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
        fake.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
        boolean acted = false;
        for (Entity e : sl.getEntities((Entity) null, new AABB(c), x -> x instanceof LivingEntity && x != fake)) {
            if (e.interact(fake, InteractionHand.MAIN_HAND).consumesAction()) {
                acted = true;
                break;
            }
        }
        if (!acted && !(stack.getItem() instanceof BlockItem)) {
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(c), Direction.UP, c, false);
            acted = stack.copy().useOn(new UseOnContext(sl, fake, InteractionHand.MAIN_HAND, fake.getMainHandItem(), hit)).consumesAction();
        }
        if (acted) {
            ItemStack after = fake.getMainHandItem();
            items.setStackInSlot(0, after.isEmpty() ? ItemStack.EMPTY : after);
            fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        } else {
            fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    private void strike(ServerLevel sl, BlockPos c) {
        float ceiling = head.ceiling().value();
        for (Entity e : sl.getEntities((Entity) null, new AABB(c), x -> x instanceof LivingEntity)) {
            if (e instanceof FakePlayer)
                continue;
            float damage = Math.min(FTuning.PIVOT_STRIKE_MAX_DAMAGE, ceiling * FTuning.PIVOT_STRIKE_DAMAGE_PER_ST);
            if (damage > 0f && e.hurt(sl.damageSources().generic(), damage))
                wear(damage / FTuning.PIVOT_STRIKE_DAMAGE_PER_ST * 0.1f);
        }
    }

    private void collect(ServerLevel sl, BlockPos c) {
        for (Entity e : sl.getEntities((Entity) null, new AABB(c).inflate(0.75), x -> x instanceof ItemEntity)) {
            ItemEntity item = (ItemEntity) e;
            ItemStack stack = item.getItem().copy();
            for (int i = 1; i <= OUT && !stack.isEmpty(); i++)
                stack = items.insertItem(i, stack, false);
            if (stack.isEmpty())
                item.discard();
            else
                item.setItem(stack);
        }
    }

    // --- Switchable ---------------------------------------------------------------------------

    @Override
    public void setEngaged(boolean engaged) {
        this.engaged = engaged;
        setChanged();
    }

    @Override
    public boolean isEngaged() {
        return engaged;
    }

    // --- the sensing head ---------------------------------------------------------------------

    /** Channel 0: the seated sensor's reading at the cell under the arm tip. Zero with no sensor or no arm. */
    @Override
    public float read(int channel) {
        if (level == null || sensor.isEmpty() || armLength() == 0 || !(((BlockItem) sensor.getItem()).getBlock() instanceof SensorBlock sb))
            return 0f;
        BlockPos at = cell(armAngle(), armLength());
        return sb.kind().read(level, at.above(), Direction.DOWN, null);
    }

    // --- DataNode -----------------------------------------------------------------------------

    @Override
    public BlockPos getNodePos() {
        return worldPosition;
    }

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
    public boolean acceptsLinks() {
        return false;
    }

    @Override
    public Set<DataNodeRef> getNodeLinks() {
        return Collections.emptySet();
    }

    // --- persistence --------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Arm", arm.saveOptional(registries));
        tag.put("Tip", tip.saveOptional(registries));
        tag.put("Sensor", sensor.saveOptional(registries));
        head.save(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        tag.putBoolean("Engaged", engaged);
        tag.putFloat("ArmDeg", armDeg);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        arm = ItemStack.parseOptional(registries, tag.getCompound("Arm"));
        tip = ItemStack.parseOptional(registries, tag.getCompound("Tip"));
        sensor = ItemStack.parseOptional(registries, tag.getCompound("Sensor"));
        head.load(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        engaged = !tag.contains("Engaged") || tag.getBoolean("Engaged");
        armDeg = tag.getFloat("ArmDeg");
        lastShaft = Float.NaN;
    }

    /** What the renderer needs; the tip's id picks the drawn shape. */
    public ItemStack armStack() {
        return arm;
    }

    public ItemStack tipStack() {
        return tip;
    }
}
