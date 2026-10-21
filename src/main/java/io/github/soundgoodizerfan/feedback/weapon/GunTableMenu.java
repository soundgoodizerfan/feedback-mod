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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.weapon;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.ModularRecipe;
import io.github.soundgoodizerfan.feedback.modular.ToolTableMenu;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FMenus;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;

/**
 * The Gun Table's menu ({@code docs/feedback_guns_spec.md} 13.6). The output is what
 * {@link ModularRecipe} makes from the blueprint and parts -- the same code the crafting grid asked,
 * so assembly and refit have one definition -- and taking it spends what the recipe spends and
 * hands back what it hands back (the blueprint stays; a refit returns the part it displaced).
 *
 * <p>The round slot only reads: with a round in it and a gun on offer, the server bakes that gun
 * against that round ({@link GunDesign#bake(java.util.Map, Cartridge)}) and puts the figures on a
 * hidden copy of the gun, which the slot sync carries to the screen exactly. Nothing on the table
 * changes a gun's or a round's figure (spec 13.1).
 */
public class GunTableMenu extends AbstractContainerMenu {

    /** Menu slot numbers: the output, the hidden spec-sheet copy, then the table's own container in order. */
    public static final int OUT = 0, SHEET = 1, PLAN = 2, PARTS = PLAN + 1, ROUND = PARTS + GunTableBlockEntity.PART_SLOTS,
            INV = ROUND + 1;

    public static final int W = 350, H = 208, INV_X = 70, INV_Y = 124;
    public static final int PLAN_X = 12, PLAN_Y = 32, ROUND_X = 12, ROUND_Y = 74, GRID_X = 70, GRID_Y = 32,
            OUT_X = 172, OUT_Y = 58, SHEET_X = 244, SHEET_Y = 38, SHEET_W = 92;

    private final Container table;
    private final SimpleContainer output = new SimpleContainer(1);
    private final SimpleContainer sheet = new SimpleContainer(1);
    private final Player player;
    private final BlockPos pos;
    /** Bit i: role i of the gun type ({@code ModularType.order()}) is wanted by the blueprint and nothing in the grid fills it. */
    private final DataSlot missing = DataSlot.standalone();

    /** Client-side open: the position comes in the open packet and the slots start empty. */
    public GunTableMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, new SimpleContainer(GunTableBlockEntity.SIZE), buf.readBlockPos());
    }

    /** Server-side open. */
    public GunTableMenu(int id, Inventory inventory, GunTableBlockEntity table) {
        this(id, inventory, table, table.getBlockPos());
    }

    private GunTableMenu(int id, Inventory inventory, Container table, BlockPos pos) {
        super(FMenus.GUN_TABLE.get(), id);
        this.player = inventory.player;
        this.pos = pos;
        this.table = new ToolTableMenu.Watched(table, () -> {
            if (!player.level().isClientSide)
                refresh();
        });
        addSlot(new Slot(output, 0, OUT_X, OUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player p, ItemStack stack) {
                taken();
            }
        });
        addSlot(new Slot(sheet, 0, -1000, -1000) {
            @Override
            public boolean isActive() {
                return false;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player p) {
                return false;
            }
        });
        addSlot(new Slot(this.table, GunTableBlockEntity.PLAN, PLAN_X, PLAN_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(FModular.BLUEPRINT.get()) && stack.has(FWeapons.GUN_DESIGN.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int i = 0; i < GunTableBlockEntity.PART_SLOTS; i++)
            addSlot(new Slot(this.table, GunTableBlockEntity.PARTS + i, GRID_X + i % 4 * 18, GRID_Y + i / 4 * 18));
        addSlot(new Slot(this.table, GunTableBlockEntity.ROUND, ROUND_X, ROUND_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.has(FWeapons.CARTRIDGE.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column + row * 9 + 9, INV_X + column * 18, INV_Y + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, INV_X + column * 18, INV_Y + 58));
        addDataSlot(missing);
        if (!player.level().isClientSide)
            compute();
    }

    /** The blueprint and the parts as the recipe's grid: the blueprint first, then the sixteen slots. */
    private CraftingInput input() {
        List<ItemStack> items = new ArrayList<>();
        items.add(table.getItem(GunTableBlockEntity.PLAN));
        for (int i = 0; i < GunTableBlockEntity.PART_SLOTS; i++)
            items.add(table.getItem(GunTableBlockEntity.PARTS + i));
        return CraftingInput.of(items.size(), 1, items);
    }

    private static ModularRecipe recipe() {
        return new ModularRecipe(CraftingBookCategory.MISC);
    }

    /** What the recipe makes now, and the gun's figures against the round in the round slot. */
    private void compute() {
        ItemStack out = ModularRecipe.build(input(), MaterialTable.get().materials(),
                io.github.soundgoodizerfan.feedback.modular.ModularTypes.get().types());
        output.setItem(0, out);
        ItemStack figures = ItemStack.EMPTY;
        Cartridge round = table.getItem(GunTableBlockEntity.ROUND).get(FWeapons.CARTRIDGE.get());
        GunDesign design = GunAssembly.isGun(out) ? out.get(FWeapons.GUN_DESIGN.get()) : null;
        if (round != null && design != null) {
            var baked = design.bake(MaterialTable.get().materials(), round);
            if (baked.isPresent()) {
                figures = out.copyWithCount(1);
                figures.set(FWeapons.GUN_FIGURES.get(), baked.get());
            }
        }
        sheet.setItem(0, figures);
        var gunType = io.github.soundgoodizerfan.feedback.modular.ModularTypes.get().types().get(io.github.soundgoodizerfan.feedback.Feedback.id("gun"));
        GunDesign planned = table.getItem(GunTableBlockEntity.PLAN).get(FWeapons.GUN_DESIGN.get());
        missing.set(out.isEmpty() && planned != null && gunType != null
                ? GunAssembly.missing(planned, items(), MaterialTable.get().materials(), gunType) : 0);
    }

    /** What the part grid holds. */
    private List<ItemStack> items() {
        List<ItemStack> parts = new ArrayList<>();
        for (int i = 0; i < GunTableBlockEntity.PART_SLOTS; i++)
            parts.add(table.getItem(GunTableBlockEntity.PARTS + i));
        return parts;
    }

    /** Bit i set: role i of the gun type, in {@code ModularType.order()}, is still to be filled. */
    public int missing() {
        return missing.get();
    }

    /** Recompute and tell the player's client, as the Tool Table does. */
    private void refresh() {
        compute();
        if (player instanceof ServerPlayer server) {
            send(server, OUT, output.getItem(0));
            send(server, SHEET, sheet.getItem(0));
        }
    }

    private void send(ServerPlayer server, int slot, ItemStack stack) {
        setRemoteSlot(slot, stack);
        server.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), slot, stack));
    }

    /** The output was taken: spend one of each part the recipe used, and put back what it gives back. */
    private void taken() {
        if (player.level().isClientSide)
            return;
        CraftingInput input = input();
        var remaining = recipe().getRemainingItems(input);
        for (int i = 0; i < input.size(); i++) {
            int index = i == 0 ? GunTableBlockEntity.PLAN : GunTableBlockEntity.PARTS + i - 1;
            ItemStack held = table.getItem(index);
            if (held.isEmpty())
                continue;
            held.shrink(1);
            ItemStack back = remaining.get(i);
            if (held.isEmpty())
                table.setItem(index, back);
            else if (!back.isEmpty())
                player.getInventory().placeItemBackInInventory(back);
        }
        table.setChanged();
    }

    public BlockPos getPos() {
        return pos;
    }

    public ItemStack round() {
        return slots.get(ROUND).getItem();
    }

    /** The gun on offer, or empty. */
    public ItemStack gun() {
        return slots.get(OUT).getItem();
    }

    /** The gun carrying the figures it bakes against the round, or empty if there is none to show. */
    public ItemStack figures() {
        return slots.get(SHEET).getItem();
    }

    @Override
    public void slotsChanged(Container container) {
        // Slots tell the table's own wrapper; nothing else here makes the output.
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(player.level(), pos), player, FWeapons.GUN_TABLE.get());
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != output && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem() || index == SHEET)
            return ItemStack.EMPTY;
        ItemStack held = slot.getItem();
        ItemStack moved = held.copy();
        if (index == OUT) {
            if (!moveItemStackTo(held, INV, slots.size(), true))
                return ItemStack.EMPTY;
            slot.onQuickCraft(held, moved);
        } else if (index < INV) {
            if (!moveItemStackTo(held, INV, slots.size(), false))
                return ItemStack.EMPTY;
        } else if (held.has(FWeapons.CARTRIDGE.get())) {
            if (!moveItemStackTo(held, ROUND, ROUND + 1, false))
                return ItemStack.EMPTY;
        } else if (!(held.is(FModular.BLUEPRINT.get()) && moveItemStackTo(held, PLAN, PLAN + 1, false))
                && !moveItemStackTo(held, PARTS, ROUND, false))
            return ItemStack.EMPTY;
        if (held.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        if (held.getCount() == moved.getCount())
            return ItemStack.EMPTY;
        slot.onTake(player, held);
        if (index == OUT)
            player.drop(held, false);
        return moved;
    }
}
