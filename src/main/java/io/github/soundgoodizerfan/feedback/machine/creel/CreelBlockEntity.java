// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.creel;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FAutomation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A basket trap. Fully submerged, it draws the biome's own fishing fish and junk tables (a catch draws nothing from the treasure table: a
 * net cannot hook a chest) once per {@code SOAK_TICKS}, quicker with bait, which it spends. Water that is not a body (a puddle) catches
 * nothing. A catch left in the basket keeps in the water, so an unemptied trap just fills and stops.
 */
public class CreelBlockEntity extends BlockEntity {

    /** Items that lure fish: offal, chum, anything of animal remains. A datapack can add to it. */
    public static final TagKey<Item> BAIT = TagKey.create(Registries.ITEM, Feedback.id("bait"));
    /** Invented. Ticks between catches, unbaited. */
    private static final int SOAK_TICKS = 2400;
    /** Invented. Bait shortens the soak to this share. */
    private static final float BAITED_SHARE = 0.4f;
    /** Invented. Water blocks needed in the surrounding 5x3x5 to count as a body of water. */
    private static final int OPEN_WATER = 20;

    private final ItemStackHandler bait = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(BAIT);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ItemStackHandler catches = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private int soaked;

    public CreelBlockEntity(BlockPos pos, BlockState state) {
        super(FAutomation.CREEL_ENTITY.get(), pos, state);
    }

    public ItemStackHandler bait() {
        return bait;
    }

    public ItemStackHandler catches() {
        return catches;
    }

    private boolean inOpenWater(ServerLevel sl) {
        if (!sl.getFluidState(worldPosition).is(FluidTags.WATER) && !sl.getFluidState(worldPosition.above()).is(FluidTags.WATER))
            return false;
        int water = 0;
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-2, -1, -2), worldPosition.offset(2, 1, 2)))
            if (sl.getFluidState(p).is(FluidTags.WATER))
                water++;
        return water >= OPEN_WATER;
    }

    public void tickServer() {
        if (!(level instanceof ServerLevel sl) || sl.getGameTime() % 20 != 0)
            return;
        if (!inOpenWater(sl) || catches.getStackInSlot(8).getCount() > 0 && fullOfCatch())
            return;
        soaked += 20;
        boolean baited = !bait.getStackInSlot(0).isEmpty();
        if (soaked < (baited ? SOAK_TICKS * BAITED_SHARE : SOAK_TICKS))
            return;
        soaked = 0;
        if (baited)
            bait.extractItem(0, 1, false);
        LootParams params = new LootParams.Builder(sl)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
                .create(LootContextParamSets.FISHING);
        // Mostly fish; now and then something the water carried.
        var table = sl.getServer().reloadableRegistries().getLootTable(
                sl.random.nextInt(10) == 0 ? BuiltInLootTables.FISHING_JUNK : BuiltInLootTables.FISHING_FISH);
        for (ItemStack drop : table.getRandomItems(params))
            ItemHandlerHelper.insertItem(catches, drop, false);
    }

    private boolean fullOfCatch() {
        for (int i = 0; i < catches.getSlots(); i++)
            if (catches.getStackInSlot(i).isEmpty())
                return false;
        return true;
    }

    public void empty(Player player) {
        for (int i = 0; i < catches.getSlots(); i++) {
            ItemStack s = catches.extractItem(i, 64, false);
            if (!s.isEmpty() && !player.getInventory().add(s))
                player.drop(s, false);
        }
    }

    public void dropAll() {
        if (level == null)
            return;
        for (var h : new ItemStackHandler[]{bait, catches})
            for (int i = 0; i < h.getSlots(); i++)
                net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), h.getStackInSlot(i));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Bait", bait.serializeNBT(registries));
        tag.put("Catches", catches.serializeNBT(registries));
        tag.putInt("Soaked", soaked);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        bait.deserializeNBT(registries, tag.getCompound("Bait"));
        catches.deserializeNBT(registries, tag.getCompound("Catches"));
        soaked = tag.getInt("Soaked");
    }
}
