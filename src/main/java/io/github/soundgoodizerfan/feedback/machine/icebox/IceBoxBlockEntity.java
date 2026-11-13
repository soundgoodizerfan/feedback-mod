// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.icebox;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.machine.vessel.VesselTuning;
import java.util.HashSet;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Refrigerable;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.process.Cooking;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A chest that is a small thermal volume (food spec 8). Ice put in it is spent as it melts, one
 * piece keeping the box at {@link MachineTuning#ICEBOX_COLD_TU} for {@link VesselTuning#ICEBOX_ICE_TICKS}; when
 * none is left the air drifts back to the room's and what is inside spoils at the room's rate. Food
 * in it spoils at the box's temperature, not the room's: a drifting mass, where a binary "cold
 * shelf" would be a switch.
 *
 * <p>What counts as ice is a property: a solid material that melts at or under 0 Tu. Nothing is
 * ticked per item; the box settles its own contents once a second.
 *
 * <p>shortcut: every piece of ice lasts the same; weigh it by the material's density or latent
 * heat when blue ice should outlast snow.
 */
public class IceBoxBlockEntity extends BaseContainerBlockEntity implements Refrigerable {

    private static final int SIZE = 27;
    private static Set<Item> coolants = Set.of();
    private static int coolantGeneration = -1;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private float cold, boxTu = ThermalTuning.AMBIENT_TU.value();
    private long heldUntil;
    private float heldTu = MachineTuning.ICEBOX_COLD_TU.value();

    public IceBoxBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.ICE_BOX.get(), pos, state);
    }

    private static boolean isCoolant(ItemStack stack) {
        if (coolantGeneration != MaterialTable.generation()) {
            Set<Item> found = new HashSet<>();
            MaterialTable.get().materials().forEach((id, m) -> {
                if (m.thermal().meltingPoint().map(tu -> tu.value() <= 0f).orElse(false))
                    found.addAll(m.items(id).values());
            });
            coolants = found;
            coolantGeneration = MaterialTable.generation();
        }
        return coolants.contains(stack.getItem());
    }

    @Override
    public void holdCold(Tu setpoint) {
        if (level != null) {
            heldUntil = level.getGameTime() + VesselTuning.REFRIGERATION_LAPSE_TICKS;
            heldTu = setpoint.value();
        }
    }

    public float boxTemperature() {
        return boxTu;
    }

    /** Whether a running pump is holding the box cold right now. */
    public boolean pumped() {
        return level != null && level.getGameTime() < heldUntil;
    }

    /** Whether melting ice is still holding it cold. */
    public boolean hasCold() {
        return cold > 0f;
    }

    public void tickServer() {
        if (level == null || level.getGameTime() % 20 != 0)
            return;
        // A running pump stands in for the ice: none is spent and none of the reserve is burned.
        boolean pumped = level.getGameTime() < heldUntil;
        if (cold <= 0f && !pumped)
            for (ItemStack stack : items)
                if (!stack.isEmpty() && isCoolant(stack)) {
                    stack.shrink(1);
                    cold += VesselTuning.ICEBOX_ICE_TICKS;
                    break;
                }
        float room = AmbientTable.at(level).value();
        if (cold > 0f || pumped) {
            if (!pumped)
                cold -= 20f;
            // Ice holds the chilled figure; a pump holds its own set point (frozen when it has a compressor stage), the colder of the two if both.
            float target = pumped ? (cold > 0f ? Math.min(heldTu, MachineTuning.ICEBOX_COLD_TU.value()) : heldTu) : MachineTuning.ICEBOX_COLD_TU.value();
            boxTu = Math.min(boxTu, room) + (target - Math.min(boxTu, room)) * 0.2f;
        } else
            boxTu += (room - boxTu) * VesselTuning.ICEBOX_DRIFT_PER_SECOND;
        for (ItemStack stack : items)
            if (!stack.isEmpty())
                Cooking.settle(stack, level, boxTu);
        setChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.feedback.ice_box");
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
        return ChestMenu.threeRows(id, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putFloat("Cold", cold);
        tag.putFloat("BoxTu", boxTu);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        cold = tag.getFloat("Cold");
        boxTu = tag.contains("BoxTu") ? tag.getFloat("BoxTu") : ThermalTuning.AMBIENT_TU.value();
    }
}
