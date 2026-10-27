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
package io.github.soundgoodizerfan.feedback.machine.basin;

import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.item.MoldItem;
import io.github.soundgoodizerfan.feedback.process.Casting;
import io.github.soundgoodizerfan.feedback.process.CastingTable;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.process.MoltenVessel;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * A casting basin: a crucible tapped over it runs into it, and the melt sets where it lands.
 *
 * <h2>Two ways to hold a pour</h2>
 * With molds set, the melt fills them top to bottom -- the first to its capacity, then the next --
 * and each is an ordinary filled {@link MoldItem}, cooling by its own {@link ItemHeat}. With none,
 * the basin is an open pool that takes one block's worth ({@link FTuning#BASIN_POOL_MB}) and cools
 * as a single heap.
 *
 * <h2>Nothing is delivered</h2>
 * The basin never hands anything on. A set pour waits in the basin until somebody knocks it out
 * (sneak, empty-handed), which takes only what has gone solid; a mold can equally be lifted out
 * and used by hand. Leaving a pour in is not neutral: the basin keeps pouring on top of whatever
 * has room, and does not know whether the last one is done.
 *
 * <p>shortcut: a mold takes the temperature of the pour as a mix by volume, with no latent heat of
 * its own, and the pool cools by a fixed share of the gap per tick; give the pool a
 * {@code ThermalBody} mass when a heap's size should matter.
 */
public class CastingBasinBlockEntity extends BaseContainerBlockEntity {

    public static final int SLOTS = 4;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final FluidTank pool = new FluidTank(FTuning.BASIN_POOL_MB);
    private float poolTu = FTuning.AMBIENT_TU.value();

    /** Pool amount and temperature, synced to an open menu. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? pool.getFluidAmount() : Math.round(poolTu);
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public CastingBasinBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CASTING_BASIN.get(), pos, state);
    }

    // --- the tick ---------------------------------------------------------------------------

    public void tickServer() {
        if (level == null)
            return;
        if (!pool.isEmpty()) {
            poolTu += (AmbientTable.at(level).value() - poolTu) * FTuning.BASIN_COOL_PER_TICK;
            setChanged();
        }
        if (level.getBlockEntity(worldPosition.above()) instanceof MoltenVessel source)
            pourFrom(source);
    }

    private void pourFrom(MoltenVessel source) {
        FluidTank tank = source.getTank();
        FluidStack offered = tank.drain(FTuning.BASIN_POUR_MB_PER_TICK, IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty())
            return;
        int took = hasMolds() ? fillMolds(offered, source.getTemperature()) : fillPool(offered, source.getTemperature());
        if (took <= 0)
            return;
        FluidStack drawn = tank.drain(took, IFluidHandler.FluidAction.EXECUTE);
        source.onDrained(drawn);
        setChanged();
    }

    private boolean hasMolds() {
        for (ItemStack stack : items)
            if (stack.getItem() instanceof MoldItem)
                return true;
        return false;
    }

    /** Fills the first mold, top to bottom, that is empty or part-full of the same melt. */
    private int fillMolds(FluidStack offered, Tu pourTu) {
        for (ItemStack mold : items) {
            if (!(mold.getItem() instanceof MoldItem))
                continue;
            FluidStack held = MoldItem.contentOf(mold).copy();
            if (!held.isEmpty() && !FluidStack.isSameFluidSameComponents(held, offered))
                continue;
            int room = FTuning.MOLD_CAPACITY_MB - held.getAmount();
            int added = Math.min(room, offered.getAmount());
            if (added <= 0)
                continue;
            float before = held.isEmpty() ? pourTu.value() : ItemHeat.get(mold, level).value();
            int total = held.getAmount() + added;
            mold.set(FDataComponents.MOLTEN_CONTENT.get(), SimpleFluidContent.copyOf(offered.copyWithAmount(total)));
            ItemHeat.set(mold, new Tu((before * held.getAmount() + pourTu.value() * added) / total), level);
            return added;
        }
        return 0;
    }

    private int fillPool(FluidStack offered, Tu pourTu) {
        int before = pool.getFluidAmount();
        int added = pool.fill(offered, IFluidHandler.FluidAction.EXECUTE);
        if (added > 0)
            poolTu = (poolTu * before + pourTu.value() * added) / (before + added);
        return added;
    }

    // --- knocking out and quenching ---------------------------------------------------------

    /** Hands the player whatever has gone solid. Returns how many things came out. */
    public int knockOut(Player player) {
        int out = 0;
        for (ItemStack mold : items) {
            if (!(mold.getItem() instanceof MoldItem moldItem))
                continue;
            FluidStack held = MoldItem.contentOf(mold).copy();
            if (held.getAmount() < FTuning.MOLD_CAPACITY_MB)
                continue;
            Optional<Casting> casting = CastingTable.get().find(held, moldItem.shape());
            if (casting.isEmpty() || !casting.get().isSolidAt(ItemHeat.get(mold, level)))
                continue;
            mold.remove(FDataComponents.MOLTEN_CONTENT.get());
            ItemHeat.clear(mold);
            give(player, casting.get().result().copy());
            out++;
        }
        if (!hasMolds() && pool.getFluidAmount() >= FTuning.BASIN_POOL_MB) {
            Optional<Casting> casting = CastingTable.get().find(pool.getFluid(), Casting.INGOT_MOLD);
            if (casting.isPresent() && casting.get().isSolidAt(new Tu(poolTu))) {
                give(player, blockOf(casting.get().result()));
                pool.setFluid(FluidStack.EMPTY);
                out++;
            }
        }
        if (out > 0)
            setChanged();
        return out;
    }

    /** The block form of the metal an ingot casting makes; nine of the ingot where it has none. */
    private static ItemStack blockOf(ItemStack ingot) {
        var materials = MaterialTable.get().materials();
        Optional<Material.Located> located = Material.find(materials, ingot.getItem());
        if (located.isPresent()) {
            Item block = located.get().material().items(located.get().id()).get("block");
            if (block != null)
                return new ItemStack(block);
        }
        return ingot.copyWithCount(ingot.getCount() * 9);
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack))
            player.drop(stack, false);
    }

    /** Water on a pour: everything in the basin is at the room's temperature at once. */
    public boolean quench() {
        if (level == null)
            return false;
        boolean warm = false;
        float room = AmbientTable.at(level).value();
        if (!pool.isEmpty() && poolTu > room) {
            poolTu = room;
            warm = true;
        }
        for (ItemStack mold : items)
            if (mold.getItem() instanceof MoldItem && !MoldItem.contentOf(mold).isEmpty()
                    && ItemHeat.get(mold, level).value() > room) {
                ItemHeat.set(mold, new Tu(room), level);
                warm = true;
            }
        if (warm) {
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 2.0f);
            if (level instanceof ServerLevel server)
                server.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5,
                        worldPosition.getY() + 0.6, worldPosition.getZ() + 0.5, 12, 0.25, 0.1, 0.25, 0.01);
            setChanged();
        }
        return warm;
    }

    // --- container --------------------------------------------------------------------------

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.feedback.casting_basin");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new CastingBasinMenu(id, inventory, this, data);
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof MoldItem;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.put("Pool", pool.writeToNBT(registries, new CompoundTag()));
        tag.putFloat("PoolTu", poolTu);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        pool.readFromNBT(registries, tag.getCompound("Pool"));
        poolTu = tag.contains("PoolTu") ? tag.getFloat("PoolTu") : FTuning.AMBIENT_TU.value();
    }
}
