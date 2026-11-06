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
package io.github.soundgoodizerfan.feedback.logistics.belt;

import java.util.List;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricPropagator;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.logistics.LogisticsTuning;
import io.github.soundgoodizerfan.feedback.logistics.sort.Band;
import io.github.soundgoodizerfan.feedback.machine.Wearing;
import io.github.soundgoodizerfan.feedback.machine.pipe.PipeBlockEntity;
import io.github.soundgoodizerfan.feedback.net.HudReadoutPayload;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FLogistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * A belt cell: what its band is made of ({@link BeltSpec}), how worn it is, the one module that
 * occupies its space and that module's setting. The cargo over it belongs to the run; this saves it
 * only so a reload can hand it back. It is an {@link ElectricNode} so a powered module takes {@code Eu}
 * from the wire beside it, and {@link Switchable} so a controller can engage or release a module.
 */
public class BeltBlockEntity extends ElectricNode implements Wearing, Switchable, CargoSave.Holder, ModuleHost, io.github.soundgoodizerfan.feedback.control.ChannelSource {

    private BeltSpec spec = BeltSpec.PLAIN;
    private float condition = 1f;
    private BeltModule module;
    private Direction face = Direction.NORTH;
    private Band band = Band.BRIGHT;
    private int aperture;
    private boolean engaged = true;
    private boolean flip;
    private int dirt, fines;
    private long passed, rateAt;
    private float rate;
    private List<Object[]> pending = List.of();
    private long wetUntil;

    public BeltBlockEntity(BlockPos pos, BlockState state) {
        super(FLogistics.BELT_ENTITY.get(), pos, state);
    }

    // --- state ----------------------------------------------------------------------------------

    public BeltSpec spec() {
        return spec;
    }

    @Override
    public BeltModule module() {
        return module;
    }

    @Override
    public Direction face() {
        return face;
    }

    @Override
    public Band band() {
        return band;
    }

    @Override
    public int aperture() {
        return aperture;
    }

    @Override
    public boolean engaged() {
        return engaged;
    }

    /** Alternates a splitter's side: answers which it is now, then turns. */
    @Override
    public boolean flip() {
        flip = !flip;
        return !flip;
    }

    @Override
    public void soil() {
        dirt++;
    }

    /** Whether the module is clogged or dirty and needs wiping. */
    public boolean soiled() {
        return (module == BeltModule.SCREEN && fouled()) || (module == BeltModule.OPTICAL && dirty());
    }

    @Override
    public boolean dirty() {
        return dirt >= LogisticsTuning.OPTICAL_DIRT_AT;
    }

    @Override
    public void foul() {
        fines++;
    }

    @Override
    public boolean fouled() {
        return fines >= LogisticsTuning.SCREEN_FOUL_AT;
    }

    @Override
    public void pass(long time) {
        passed++;
        float since = Math.max(1, time - rateAt);
        rate = rate * Math.max(0f, 1f - since / LogisticsTuning.COUNTER_WINDOW_TICKS) + 1f * 20f / LogisticsTuning.COUNTER_WINDOW_TICKS;
        rateAt = time;
    }

    /** Only a cell fitted with a counter is a source: key C reads the count, any other key (R by default) the rate per second. */
    @Override
    public boolean offersChannels() {
        return module == BeltModule.COUNTER;
    }

    @Override
    public float read(int channel) {
        return channel == 67 ? getCount() : getRate();
    }

    @Override
    public int defaultChannel() {
        return 82;
    }

    /** Units counted since placed (a counter's reading, for the controller). */
    public long getCount() {
        return passed;
    }

    /** Units a second, decayed over the counter's window. */
    public float getRate() {
        long since = level == null ? 0 : level.getGameTime() - rateAt;
        return rate * Math.max(0f, 1f - since / (float) LogisticsTuning.COUNTER_WINDOW_TICKS);
    }

    @Override
    public boolean live() {
        return module == null || !module.powered || ElectricRules.rate(getVolts(), getAmps(), getWantedAmps(), ratedBand()) >= 0.5f;
    }

    /** The highest gas pressure (Pu) in a pipe run touching this cell; a puff draws a mB from it. */
    @Override
    public float jetPressure() {
        if (level == null)
            return 0f;
        float best = 0f;
        for (Direction d : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(d)) instanceof PipeBlockEntity pipe && pipe.network != null) {
                float pu = pipe.network.pressure().value();
                if (pu > best) {
                    IFluidHandler h = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(d), d.getOpposite());
                    if (h != null && !h.drain(1, IFluidHandler.FluidAction.EXECUTE).isEmpty())
                        best = pu;
                }
            }
        }
        return best;
    }

    // --- wet ------------------------------------------------------------------------------------

    /** Rain or a fluid above wets the band unless a hood covers it; a wet band dries slowly. */
    boolean wetNow() {
        if (level == null)
            return false;
        long now = level.getGameTime();
        BlockPos up = worldPosition.above();
        if (module != BeltModule.HOOD && (level.isRainingAt(up) || !level.getFluidState(up).isEmpty()))
            wetUntil = now + LogisticsTuning.WET_DRY_TICKS;
        return wetUntil > now;
    }

    /** A unit carrying fluid rode the band. */
    void soak() {
        if (level != null)
            wetUntil = level.getGameTime() + LogisticsTuning.WET_DRY_TICKS;
    }

    // --- wear -----------------------------------------------------------------------------------

    @Override
    public float getCondition() {
        return condition;
    }

    @Override
    public boolean isRunning() {
        return level != null && BeltRuns.of(level).speedAt(worldPosition) != 0f;
    }

    void wear(float amount) {
        float next = Math.max(LogisticsTuning.BELT_CONDITION_FLOOR, condition - amount);
        if (next != condition) {
            condition = next;
            setChanged();
        }
    }

    // --- Switchable -----------------------------------------------------------------------------

    @Override
    public void setEngaged(boolean engaged) {
        if (this.engaged == engaged)
            return;
        this.engaged = engaged;
        setChanged();
        sync();
        if (level != null && !level.isClientSide)
            BeltRuns.of(level).wake(worldPosition);
    }

    @Override
    public boolean isEngaged() {
        return engaged;
    }

    // --- electric -------------------------------------------------------------------------------

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return module != null && module.powered ? LogisticsTuning.POWERED_AMPS : 0f;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.LV;
    }

    @Override
    public boolean conducting() {
        return module != null && module.powered;
    }

    // --- modules --------------------------------------------------------------------------------

    /** Places {@code m} in this cell, acting on {@code side} if it has one. */
    @Override
    public void install(BeltModule m, Direction side) {
        module = m;
        face = side;
        band = m == BeltModule.WEIGHER ? Band.HEAVY : Band.BRIGHT;
        aperture = 0;
        engaged = true;
        dirt = 0;
        fines = 0;
        changedModule();
    }

    private void changedModule() {
        setChanged();
        sync();
        if (level != null && !level.isClientSide) {
            BeltRuns.of(level).queue(worldPosition);
            ElectricPropagator.reconnect(level, worldPosition);
        }
    }

    public Optional<ItemStack> moduleDrop() {
        return module == null ? Optional.empty() : Optional.of(new ItemStack(FLogistics.MODULE_ITEMS.get(module).get()));
    }

    /** An empty hand on the cell: sneaking pulls the module (or turns the cell), a click on the top takes a unit off, a click on a side works the module. */
    @Override
    public boolean handUse(Player player, BlockHitResult hit) {
        if (level == null)
            return false;
        if (player.isShiftKeyDown()) {
            if (module != null) {
                moduleDrop().ifPresent(s -> {
                    if (!player.getInventory().add(s))
                        player.drop(s, false);
                });
                module = null;
                changedModule();
            } else {
                BlockState s = getBlockState();
                level.setBlockAndUpdate(worldPosition, s.setValue(BeltBlock.FACING, s.getValue(BeltBlock.FACING).getClockWise()));
                BeltRuns.of(level).queue(worldPosition);
            }
            return true;
        }
        if (hit.getDirection() == Direction.UP) {
            ItemStack taken = BeltRuns.of(level).takeNear(worldPosition, hit.getLocation());
            if (!taken.isEmpty()) {
                if (!player.getInventory().add(taken))
                    player.drop(taken, false);
                return true;
            }
        }
        if (module == null)
            return false;
        if ((module == BeltModule.SCREEN && fouled()) || (module == BeltModule.OPTICAL && dirty())) {
            fines = 0;
            dirt = 0;
            say(player, "message.feedback.belt.wiped");
        } else if (module.banded()) {
            band = band.next();
            say(player, "message.feedback.belt.band." + band.key());
        } else if (module == BeltModule.SCREEN) {
            aperture = (aperture + 1) % 3;
            say(player, "message.feedback.belt.aperture." + aperture);
        } else if (module == BeltModule.GATE || module == BeltModule.DIVERTER || module == BeltModule.ELECTROMAGNET || module == BeltModule.AIR_JET) {
            setEngaged(!engaged);
            say(player, engaged ? "message.feedback.belt.engaged" : "message.feedback.belt.released");
            return true;
        } else {
            return false;
        }
        setChanged();
        sync();
        return true;
    }

    private void say(Player player, String key) {
        if (player instanceof ServerPlayer sp)
            HudReadoutPayload.send(sp, Component.translatable(key), 40);
    }

    private void sync() {
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    // --- heat and handlers ----------------------------------------------------------------------

    /**
     * The cargo over this cell as a read-only handler. A heat source's push into "any item handler it
     * reaches" warms these stacks in place, so a plain coil block set on a belt is the coil module.
     */
    public IItemHandler cargoView() {
        List<ItemStack> over = level == null ? List.of() : BeltRuns.of(level).stacksOver(worldPosition);
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return over.size();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return slot < over.size() ? over.get(slot) : ItemStack.EMPTY;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return false;
            }
        };
    }

    @Override
    public List<Object[]> takePending() {
        List<Object[]> out = pending;
        pending = List.of();
        return out;
    }

    // --- lifecycle and persistence --------------------------------------------------------------

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide)
            BeltRuns.of(level).queue(worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide)
            BeltRuns.of(level).queue(worldPosition);
    }

    private void writeState(CompoundTag tag) {
        spec.write(tag);
        tag.putFloat("Condition", condition);
        if (module != null) {
            tag.putInt("Module", module.ordinal());
            tag.putInt("Face", face.get3DDataValue());
            tag.putInt("Band", band.ordinal());
            tag.putInt("Aperture", aperture);
            tag.putBoolean("Engaged", engaged);
            tag.putInt("Dirt", dirt);
            tag.putInt("Fines", fines);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeState(tag);
        List<Object[]> over = level == null || level.isClientSide ? pending : BeltRuns.of(level).savedOver(worldPosition);
        if (over.isEmpty())
            over = pending;
        if (!over.isEmpty())
            tag.put("Cargo", CargoSave.write(over, registries, level == null ? 0L : level.getGameTime()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        spec = BeltSpec.read(tag);
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
        module = tag.contains("Module") ? BeltModule.byOrdinal(tag.getInt("Module")) : null;
        face = Direction.from3DDataValue(tag.getInt("Face"));
        band = Band.byOrdinal(tag.getInt("Band"));
        aperture = tag.getInt("Aperture");
        engaged = !tag.contains("Engaged") || tag.getBoolean("Engaged");
        dirt = tag.getInt("Dirt");
        fines = tag.getInt("Fines");
        if (tag.contains("Cargo"))
            pending = CargoSave.read(tag.getList("Cargo", 10), registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeState(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag tag = input.get(FDataComponents.BELT.get());
        if (tag != null) {
            spec = BeltSpec.read(tag);
            condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        CompoundTag tag = new CompoundTag();
        spec.write(tag);
        tag.putFloat("Condition", condition);
        components.set(FDataComponents.BELT.get(), tag);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        for (String k : new String[]{"Material", "Friction", "MaxTu", "Cleat", "Condition"})
            tag.remove(k);
    }
}
