// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.mixer;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.registry.FMenus;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Mixer's input slots, the drawn item, a draw button, and the readings of the drum. The drum's parts are read off the
 * block entity (the drum is open; it syncs when its parts change), uniformity, heat and the tank ride as data slots.
 */
public class MixerMenu extends AbstractContainerMenu {

    public static final int DRAW = 0;
    public static final int SLOTS_X = 10, SLOTS_Y = 20, SLOT_PX = 18, OUT_X = 82, TANK_X = 150, TANK_Y = 18, TANK_W = 18, TANK_H = 44, HEAT_X = 126, HEAT_W = 12;
    public static final int ROWS_Y = 66, ROW_PX = 11, INV_Y = 140;

    private final BlockPos pos;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final Player player;

    public MixerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainer(FTuning.MIXER_INPUT_SLOTS + 1), new SimpleContainerData(7));
    }

    public MixerMenu(int id, Inventory inventory, MixerBlockEntity mixer) {
        this(id, inventory, mixer.getBlockPos(), mixer.slots(), new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> Math.round(mixer.uniformity() * 1000f);
                    case 1 -> Math.round(mixer.trueTemperature().value());
                    case 2 -> mixer.fluidMb();
                    case 3 -> BuiltInRegistries.FLUID.getId(mixer.product().getFluid());
                    case 4 -> mixer.product().getAmount();
                    case 5 -> mixer.overmixed() ? 1 : 0;
                    case 6 -> mixer.frozen() ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 7;
            }
        });
    }

    private MixerMenu(int id, Inventory inventory, BlockPos pos, Container container, ContainerData data) {
        super(FMenus.MIXER.get(), id);
        this.pos = pos;
        this.player = inventory.player;
        this.access = ContainerLevelAccess.create(player.level(), pos);
        this.data = data;
        for (int slot = 0; slot < FTuning.MIXER_INPUT_SLOTS; slot++)
            addSlot(new Slot(container, slot, SLOTS_X + slot * SLOT_PX, SLOTS_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return MixerBlockEntity.pourable(stack, ClientMaterials.on(player.level()));
                }
            });
        addSlot(new Slot(container, MixerBlockEntity.OUTPUT, OUT_X, SLOTS_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, INV_Y + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, 8 + column * 18, INV_Y + 58));
        addDataSlots(data);
    }

    /** The block entity as this side sees it (the client's copy carries the drum's parts). */
    public MixerBlockEntity mixer() {
        return player.level().getBlockEntity(pos) instanceof MixerBlockEntity m ? m : null;
    }

    public int uniformityPermille() {
        return data.get(0);
    }

    public int temperatureTu() {
        return data.get(1);
    }

    public int fluidMb() {
        return data.get(2);
    }

    public net.minecraft.world.level.material.Fluid productFluid() {
        return BuiltInRegistries.FLUID.byId(data.get(3));
    }

    public int productMb() {
        return data.get(4);
    }

    public boolean overmixed() {
        return data.get(5) != 0;
    }

    public boolean frozen() {
        return data.get(6) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != DRAW || !(player.level().getBlockEntity(pos) instanceof MixerBlockEntity mixer))
            return false;
        if (!mixer.drawToOutput())
            io.github.soundgoodizerfan.feedback.net.AimPayloads.screen(player, net.minecraft.network.chat.Component.translatable(mixer.frozen() ? "message.feedback.mixer.frozen" : "message.feedback.mixer.draw_blocked"));
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, FWeapons.MIXER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack held = slot.getItem();
        ItemStack moved = held.copy();
        int inputs = FTuning.MIXER_INPUT_SLOTS, own = inputs + 1;
        if (index < own ? !moveItemStackTo(held, own, slots.size(), true) : !moveItemStackTo(held, 0, inputs, false))
            return ItemStack.EMPTY;
        if (held.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        return held.getCount() == moved.getCount() ? ItemStack.EMPTY : moved;
    }
}
