// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.fishwheel;

import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.machine.waterwheel.WaterWheelBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FAutomation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A water wheel that also fishes. It turns on moving water as any wheel does but gives half the power, because the blades carry baskets.
 * While turning it draws a catch from the biome's fish table every {@link #WHEEL_TICKS} ticks, faster the more of its blades are in the
 * current. Never treasure. A full basket stops the catch, not the wheel.
 */
public class FishWheelBlockEntity extends WaterWheelBlockEntity {

    /** Invented. Ticks between catches with one blade in the current. */
    private static final int WHEEL_TICKS = 1200;
    /** Invented. The share of a plain wheel's capacity it gives. */
    private static final float POWER_SHARE = 0.5f;

    private final ItemStackHandler catches = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private int soaked;

    public FishWheelBlockEntity(BlockPos pos, BlockState state) {
        super(FAutomation.FISH_WHEEL_ENTITY.get(), pos, state);
    }

    public ItemStackHandler catches() {
        return catches;
    }

    @Override
    public Su getCapacitySu() {
        return new Su(super.getCapacitySu().value() * POWER_SHARE);
    }

    @Override
    public void tickServer() {
        super.tickServer();
        if (!(level instanceof ServerLevel sl) || sl.getGameTime() % 20 != 0 || flowing() == 0)
            return;
        soaked += 20 * flowing();
        if (soaked < WHEEL_TICKS)
            return;
        soaked = 0;
        LootParams params = new LootParams.Builder(sl)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
                .create(LootContextParamSets.FISHING);
        var table = sl.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING_FISH);
        for (ItemStack drop : table.getRandomItems(params))
            ItemHandlerHelper.insertItem(catches, drop, false);
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
        for (int i = 0; i < catches.getSlots(); i++)
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), catches.getStackInSlot(i));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Catches", catches.serializeNBT(registries));
        tag.putInt("Soaked", soaked);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        catches.deserializeNBT(registries, tag.getCompound("Catches"));
        soaked = tag.getInt("Soaked");
    }
}
