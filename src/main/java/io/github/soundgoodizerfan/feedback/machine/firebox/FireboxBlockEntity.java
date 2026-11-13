// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.firebox;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.machine.vessel.VesselTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Catchup;
import io.github.soundgoodizerfan.feedback.core.thermal.FireSpan;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.unit.Mb;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.machine.bellows.Blown;
import io.github.soundgoodizerfan.feedback.process.Fuel;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Burns fuel. Produces a flame temperature and nothing else.
 *
 * <h2>It heats nothing</h2>
 * The firebox has no idea what is above it. It publishes how hot it is burning and the vessel
 * reads that -- which is the same shape as every other pairing in the mod, and it is what lets a
 * crucible sit over a firebox, over lava, or over whatever a later slice lights, with no code
 * knowing the difference.
 *
 * <h2>Air is a rate, not a fuel</h2>
 * Blowing does not make the fire last longer or burn more coal; it makes the same fire
 * <em>hotter</em>, which is what air actually does and what the slice needs it to do. A charcoal
 * fire tops out at 1220 Tu, which is below steel's window, so no amount of patience substitutes
 * for air -- philosophy 8's hard gate again, and the reason the bellows is the beat's one verb.
 *
 * <h2>The noise floor lives here</h2>
 * Each piece of fuel is rolled once for quality when it is lit, and burns at that quality for its
 * whole burn. That makes the wandering <em>learnable in aggregate</em> rather than merely jittery
 * (§8): a batch runs hot or cool and the player can feel it and respond. Rolling per tick would
 * be the same variance with nothing to notice.
 *
 * <h2>What burns, and how hot, is data</h2>
 * See {@link Fuel}: an item burns because its material does. The firebox holds no opinion about
 * charcoal and does not consult vanilla burn times -- a vanilla burn time counts items smelted,
 * which is a fact about a furnace and carries no temperature. An item whose material has no
 * combustion figures will not light.
 */
public class FireboxBlockEntity extends BlockEntity implements HeatSource, Blown {

    private ItemStack fuel = ItemStack.EMPTY;

    private int burnTicks;
    /** The flame temperature of the piece currently burning, quality roll already applied. */
    private float burnTu = ThermalTuning.AMBIENT_TU.value();

    /** Away-time bookkeeping: see {@link Catchup}. */
    private final Catchup offline = new Catchup();
    /** What the fire did across the gap this block came back from, and when that was worked out. */
    private FireSpan span = FireSpan.NONE;
    private long spanAt = Long.MIN_VALUE / 2;

    private float air;
    /** Air actually consumed last tick, which is what sets the flame temperature. */
    private float draught;
    /** Oxygen balance of the piece burning (spec 10.1); not saved, so a reload reads it again at the next piece. */
    private float burnOxygen;
    /** One oxidiser piece (saltpetre, any item whose material has a positive oxygen balance), burned with an oxygen-poor fuel when no oxygen gas is on hand. */
    private ItemStack oxidiser = ItemStack.EMPTY;
    private final net.neoforged.neoforge.fluids.capability.templates.FluidTank oxygen =
            new net.neoforged.neoforge.fluids.capability.templates.FluidTank(VesselTuning.FIREBOX_OXYGEN_TANK_MB,
                    s -> s.getFluid() == io.github.soundgoodizerfan.feedback.registry.FFluids.OXYGEN.get()) {
                @Override
                protected void onContentsChanged() {
                    setChanged();
                }
            };

    public FireboxBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.FIREBOX.get(), pos, state);
    }

    public ItemStack getFuel() {
        return fuel;
    }

    public boolean isLit() {
        return burnTicks > 0;
    }

    /** The oxygen tank, for a pipe or bucket. */
    public net.neoforged.neoforge.fluids.capability.IFluidHandler oxygenHandler() {
        return oxygen;
    }

    public ItemStack getOxidiser() {
        return oxidiser;
    }

    private static boolean oxidising(ItemStack stack) {
        return !stack.isEmpty() && io.github.soundgoodizerfan.feedback.process.Material.find(
                io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials(), stack.getItem())
                .flatMap(l -> l.material().combustion()).map(c -> c.oxygenBalance() > 0f).orElse(false);
    }

    public boolean insertFuel(ItemStack stack) {
        if (oxidising(stack) && Fuel.find(stack).isEmpty()) {
            if (!oxidiser.isEmpty() && !ItemStack.isSameItemSameComponents(oxidiser, stack))
                return false;
            if (oxidiser.getCount() >= oxidiser.getMaxStackSize())
                return false;
            if (oxidiser.isEmpty())
                oxidiser = stack.split(1);
            else {
                oxidiser.grow(1);
                stack.shrink(1);
            }
            setChanged();
            return true;
        }
        if (stack.isEmpty() || Fuel.find(stack).isEmpty())
            return false;
        if (!fuel.isEmpty() && !ItemStack.isSameItemSameComponents(fuel, stack))
            return false;
        if (fuel.getCount() >= fuel.getMaxStackSize())
            return false;

        if (fuel.isEmpty())
            fuel = stack.split(1);
        else {
            fuel.grow(1);
            stack.shrink(1);
        }
        setChanged();
        return true;
    }

    public ItemStack removeFuel() {
        if (fuel.isEmpty() && !oxidiser.isEmpty()) {
            ItemStack taken = oxidiser;
            oxidiser = ItemStack.EMPTY;
            setChanged();
            return taken;
        }
        ItemStack taken = fuel;
        fuel = ItemStack.EMPTY;
        setChanged();
        return taken;
    }

    @Override
    public void addAir(Mb amount) {
        air = Math.min(VesselTuning.FIREBOX_MAX_AIR.value(), air + amount.value());
    }

    /** How much air is banked, for the debug helmet. Never shown to an uninstrumented player. */
    public Mb getAir() {
        return new Mb(air);
    }

    @Override
    public Tu getFireTu() {
        if (!isLit())
            return AmbientTable.at(level);
        float blown = Mth.clamp(Math.max(draught / VesselTuning.FIREBOX_AIR_PER_TICK.value(),
                burnOxygen * VesselTuning.FIREBOX_OXIDISER_DRAUGHT), 0f, 1f);
        // Air multiplies a fuel's own flame temperature rather than replacing it, so a cool fuel
        // blown hard is still a cool fire. Otherwise the bellows would silently make every fuel
        // equivalent and the fuel table would stop meaning anything.
        return new Tu(burnTu * Mth.lerp(blown, 1f, VesselTuning.FULL_AIR_TEMPERATURE_FACTOR));
    }

    @Override
    public FireSpan catchUp(long gap) {
        if (level == null)
            return FireSpan.NONE;
        refresh();
        // Answered from what the firebox itself worked out when it came back: the span its own gap
        // had. A firebox that was never away (a chunk border, the vessel's chunk was the one
        // unloaded) was ticking throughout and is lit now, which is the best statement of it.
        if (level.getGameTime() - spanAt <= 1L)
            return span.clampedTo(gap);
        return isLit() ? new FireSpan(gap, getFireTu().value()) : FireSpan.NONE;
    }

    /** Burn the fuel this block was away for, once; no-op if it was not away or already has. */
    private void refresh() {
        long away = offline.take(level);
        if (away <= 0L)
            return;
        span = burnAcross(away);
        spanAt = level.getGameTime();
        air = 0f;
        setLit(burnTicks > 0);
        setChanged();
    }

    /**
     * Burn the piece alight and then the stack, for as many of {@code gap} ticks as there was fuel.
     * Unblown (nobody was working the bellows) and at each fuel's nominal temperature: the
     * quality roll is a noise floor for a fire the player is watching, not for a gap.
     */
    private FireSpan burnAcross(long gap) {
        long left = gap;
        long lit = 0L;
        double weighted = 0.0;

        if (burnTicks > 0) {
            long t = Math.min(burnTicks, left);
            burnTicks -= (int) t;
            left -= t;
            lit += t;
            weighted += t * (double) burnTu;
        }
        while (left > 0L && !fuel.isEmpty()) {
            Optional<Fuel> maybe = Fuel.find(fuel);
            if (maybe.isEmpty()) {
                fuel = ItemStack.EMPTY;
                break;
            }
            Fuel burning = maybe.get().smouldering();
            long t = Math.min(burning.duration(), left);
            lit += t;
            left -= t;
            weighted += t * (double) burning.temperature().value();

            ItemStack remainder = fuel.getCraftingRemainingItem();
            fuel.shrink(1);
            if (!remainder.isEmpty())
                Block.popResource(level, worldPosition.above(), remainder);
            if (t < burning.duration()) {
                burnTicks = (int) (burning.duration() - t);
                burnTu = burning.temperature().value();
            }
        }
        return lit <= 0L ? FireSpan.NONE : new FireSpan(lit, (float) (weighted / lit));
    }

    public void tickServer() {
        if (level == null)
            return;
        HeatSource.radiate(this);
        refresh();

        draught = Math.min(air, VesselTuning.FIREBOX_AIR_PER_TICK.value());
        air -= draught;

        if (burnTicks > 0) {
            burnTicks--;
            setChanged();
            if (burnTicks > 0)
                return;
        }

        // Relight in the same tick the last piece runs out, so there is no one-tick gap at
        // ambient between pieces -- that gap read as a cooling tick to anything rate-limited.
        if (fuel.isEmpty()) {
            setLit(false);
            return;
        }

        Optional<Fuel> maybe = Fuel.find(fuel);
        if (maybe.isEmpty()) {
            fuel = ItemStack.EMPTY;   // somebody piped in something that will not burn
            setLit(false);
            setChanged();
            return;
        }
        Fuel burning = maybe.get();
        // An oxygen-poor fuel reaches its full flame only with an oxidiser: oxygen gas first, else a piece of saltpetre (a short, violent burn).
        if (burning.poor()) {
            if (oxygen.getFluidAmount() >= VesselTuning.FIREBOX_OXYGEN_MB_PER_PIECE) {
                oxygen.drain(VesselTuning.FIREBOX_OXYGEN_MB_PER_PIECE, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            } else if (!oxidiser.isEmpty()) {
                oxidiser.shrink(1);
                burning = new Fuel(Math.max(1, Math.round(burning.duration() * VesselTuning.NITRE_BURST_DURATION)), burning.temperature(), burning.spread(), true);
            } else
                burning = burning.smouldering();
        }

        burnTicks = burning.duration();
        // Rolled once, here, and held for the whole burn. See the class note on why per-tick
        // noise would be the same variance with nothing in it to learn.
        float roll = 1f + (level.random.nextFloat() * 2f - 1f) * burning.spread();
        burnTu = burning.temperature().value() * roll;
        burnOxygen = io.github.soundgoodizerfan.feedback.process.Material.find(
                io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials(), fuel.getItem())
                .flatMap(l -> l.material().combustion()).map(c -> c.oxygenBalance()).orElse(0f);
        // Read before shrinking: once the stack is empty its remainder is gone too.
        ItemStack remainder = fuel.getCraftingRemainingItem();
        fuel.shrink(1);
        if (!remainder.isEmpty())
            Block.popResource(level, worldPosition.above(), remainder);
        setLit(true);
        setChanged();
    }

    private void setLit(boolean lit) {
        if (level != null && getBlockState().getValue(FireboxBlock.LIT) != lit)
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(FireboxBlock.LIT, lit));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Fuel", fuel.saveOptional(registries));
        tag.put("Oxidiser", oxidiser.saveOptional(registries));
        tag.put("Oxygen", oxygen.writeToNBT(registries, new CompoundTag()));
        tag.putInt("BurnTicks", burnTicks);
        tag.putFloat("BurnTu", burnTu);
        tag.putFloat("Air", air);
        offline.save(tag, level);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuel = ItemStack.parseOptional(registries, tag.getCompound("Fuel"));
        oxidiser = ItemStack.parseOptional(registries, tag.getCompound("Oxidiser"));
        oxygen.readFromNBT(registries, tag.getCompound("Oxygen"));
        burnTicks = tag.getInt("BurnTicks");
        burnTu = tag.contains("BurnTu") ? tag.getFloat("BurnTu") : ThermalTuning.AMBIENT_TU.value();
        air = tag.getFloat("Air");
        offline.load(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
