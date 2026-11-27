// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack;
import io.github.soundgoodizerfan.feedback.modular.trait.ModularTraits;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The battery a firearm's powered sight runs on: a battery item ({@link BatteryStack}, the same stack the pulse gun and the suit carry,
 * filled at the Charger) loaded into the gun by a right click on the gun with the battery on the cursor, kept in the stack's container
 * component. A gun whose sight is a rangefinder or a laser ({@link #RANGEFINDER}, {@link #LASER}: traits of the sight's variants, each
 * with a {@code draw_eu} per second) draws that many Eu a second from it while the gun is held; an empty cell darkens the sight.
 * The rest of a sight's behaviour (the range line, the beam) is client side and reads {@link #powered}.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class SightCell {

    public static final String RANGEFINDER = "gun_rangefinder", LASER = "gun_laser";

    private SightCell() {
    }

    /** Eu a second the gun's powered sights draw while it is held: the sum of their {@code draw_eu}; 0 for a gun with none. */
    public static float drawPerSecond(ItemStack gun) {
        float sum = 0f;
        for (var a : ModularTraits.actives(gun))
            if (a.id().equals(RANGEFINDER) || a.id().equals(LASER))
                sum += a.params().get("draw_eu", 1f);
        return sum;
    }

    /** Whether the gun carries a sight that wants a cell. */
    public static boolean fitted(ItemStack gun) {
        return gun.getItem() instanceof GunItem && drawPerSecond(gun) > 0f;
    }

    /** Whether the gun has the sight trait {@code id} and a cell with charge in it. */
    public static boolean powered(ItemStack gun, String id) {
        if (!(gun.getItem() instanceof GunItem))
            return false;
        boolean has = false;
        for (var a : ModularTraits.actives(gun))
            has |= a.id().equals(id);
        if (!has)
            return false;
        CompoundTag state = BatteryStack.state(cell(gun));
        return state != null && BatteryStack.charge(state) > 0f;
    }

    /** The battery loaded, or empty. */
    public static ItemStack cell(ItemStack gun) {
        ItemContainerContents c = gun.get(DataComponents.CONTAINER);
        if (c == null)
            return ItemStack.EMPTY;
        for (ItemStack s : c.nonEmptyItems())
            return s.copy();
        return ItemStack.EMPTY;
    }

    public static void setCell(ItemStack gun, ItemStack cell) {
        if (cell.isEmpty())
            gun.remove(DataComponents.CONTAINER);
        else
            gun.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(cell.copyWithCount(1))));
    }

    /**
     * A battery on the cursor is loaded into the gun in the slot (or swapped for the one in it); an empty cursor takes the cell out.
     * False when the gun's sights want no cell or the cursor holds something else.
     */
    public static boolean load(ItemStack gun, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player) || !fitted(gun))
            return false;
        if (other.isEmpty()) {
            ItemStack held = cell(gun);
            if (held.isEmpty())
                return false;
            setCell(gun, ItemStack.EMPTY);
            access.set(held);
            player.playSound(FSounds.GUN_EGUN_CELL_OUT.get(), 0.8f, 1f);
            return true;
        }
        if (!BatteryStack.is(other))
            return false;
        ItemStack old = cell(gun);
        setCell(gun, other);
        access.set(old);
        player.playSound(FSounds.GUN_EGUN_CELL_IN.get(), 0.8f, 1f);
        return true;
    }

    /** Once a second the held guns' sights take their draw from their cells. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0)
            return;
        draw(player.getMainHandItem());
        draw(player.getOffhandItem());
    }

    private static void draw(ItemStack gun) {
        float eu = drawPerSecond(gun);
        ItemStack cell = eu > 0f ? cell(gun) : ItemStack.EMPTY;
        CompoundTag state = cell.isEmpty() ? null : BatteryStack.state(cell);
        if (state == null || BatteryStack.charge(state) <= 0f)
            return;
        CompoundTag next = state.copy();
        BatteryStack.draw(next, eu);
        cell.set(FDataComponents.BATTERY.get(), next);
        setCell(gun, cell);
    }
}
