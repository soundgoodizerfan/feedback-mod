// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.suit;

import java.util.List;

import io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * The four worn armour pieces read as one suit. A piece is part of the suit by its fittings (a servo, a gauge, a sensor head, a battery
 * bay: {@link Fig#SUIT_DRIVE} and the rest), never by what it is called, so any modular armour with a fitting counts. Pure reads of the
 * stacks: nothing here ticks.
 */
public final class ExoSuit {

    private ExoSuit() {
    }

    /** The worn pieces and what their fittings add up to. {@code bay} is true when the chestplate has a battery bay. */
    public record Rig(ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet,
                      float driveChest, float driveLegs, float driveFeet, float sense, float lossChest, float lossLegs, float lossFeet,
                      float gauge, boolean bay, int available, float[] mod) {

        /** A figure that changes whenever the upgrades fitted or their power change. */
        public int modSig() {
            return mod == null ? 0 : java.util.Arrays.hashCode(mod);
        }

        public boolean worn() {
            return bay;
        }

        public float slow() {
            return ExoSuit.slow(head) + ExoSuit.slow(chest) + ExoSuit.slow(legs) + ExoSuit.slow(feet);
        }

        public float mass() {
            return ExoSuit.mass(head) + ExoSuit.mass(chest) + ExoSuit.mass(legs) + ExoSuit.mass(feet);
        }

        public boolean can(Ability a) {
            return (available & a.bit()) != 0;
        }
    }

    private static ModularFigures figures(ItemStack stack) {
        return stack.isEmpty() || ModularStacks.broken(stack) ? null : ModularStacks.figures(stack);
    }

    private static float fig(ItemStack stack, Fig fig) {
        ModularFigures f = figures(stack);
        return f == null ? 0f : f.get(fig);
    }

    static float slow(ItemStack stack) {
        return fig(stack, Fig.SLOW);
    }

    static float mass(ItemStack stack) {
        return fig(stack, Fig.MASS);
    }

    private static float loss(ItemStack stack) {
        float l = fig(stack, Fig.SUIT_LOSS);
        return l > 0f ? l : io.github.soundgoodizerfan.feedback.modular.ModularTuning.SUIT_LOSS_FLOOR;
    }

    /** Whether the stack has a battery bay: the one cheap test that a chestplate is part of a suit. */
    public static boolean hasBay(ItemStack chest) {
        return fig(chest, Fig.SUIT_BAY) > 0f;
    }

    public static Rig of(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD), chest = player.getItemBySlot(EquipmentSlot.CHEST),
                legs = player.getItemBySlot(EquipmentSlot.LEGS), feet = player.getItemBySlot(EquipmentSlot.FEET);
        boolean bay = fig(chest, Fig.SUIT_BAY) > 0f;
        float dc = fig(chest, Fig.SUIT_DRIVE), dl = fig(legs, Fig.SUIT_DRIVE), df = fig(feet, Fig.SUIT_DRIVE), sense = fig(head, Fig.SUIT_SENSE);
        float gauge = 1f;
        boolean anyGauge = false;
        for (ItemStack s : List.of(head, chest, legs, feet)) {
            float g = fig(s, Fig.SUIT_GAUGE);
            if (g > 0f) {
                gauge = Math.min(gauge, g);
                anyGauge = true;
            }
        }
        int mask = 0;
        if (bay) {
            if (dc > 0f)
                mask |= Ability.STRENGTH.bit() | Ability.PLATING.bit();
            if (dl > 0f || df > 0f)
                mask |= Ability.JUMP_DASH.bit();
            if (sense > 0f)
                mask |= Ability.NIGHT.bit() | Ability.THERMAL.bit();
        }
        float[] mod = null;
        for (ItemStack s : List.of(head, chest, legs, feet)) {
            int id = Math.round(fig(s, Fig.SUIT_MODULE));
            if (id > 0 && id < io.github.soundgoodizerfan.feedback.suit.SuitModules.COUNT) {
                if (mod == null)
                    mod = new float[io.github.soundgoodizerfan.feedback.suit.SuitModules.COUNT];
                mod[id] = Math.max(mod[id], fig(s, Fig.SUIT_POWER));
            }
        }
        return new Rig(head, chest, legs, feet, dc, dl, df, sense, loss(chest), loss(legs), loss(feet), anyGauge ? gauge : 0f, bay, mask, mod);
    }

    // --- the battery in the bay ----------------------------------------------------------------------

    /** The battery stack in the chestplate's bay, or empty. The stack is the chestplate's own: copy before writing. */
    public static ItemStack battery(ItemStack chest) {
        ItemContainerContents c = chest.get(DataComponents.CONTAINER);
        if (c == null)
            return ItemStack.EMPTY;
        for (ItemStack s : c.nonEmptyItems())
            if (BatteryStack.is(s))
                return s;
        return ItemStack.EMPTY;
    }

    public static void setBattery(ItemStack chest, ItemStack battery) {
        if (battery.isEmpty())
            chest.remove(DataComponents.CONTAINER);
        else
            chest.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(battery)));
    }

    /** Charge left in the bay's battery as a share of what it can hold now, 0 when there is none or it is dry. */
    public static float chargeShare(ItemStack chest) {
        CompoundTag t = BatteryStack.state(battery(chest));
        return t == null || !BatteryStack.ready(t) ? 0f : BatteryStack.stateOfCharge(t);
    }

    public static SuitState state(ItemStack chest) {
        SuitState s = chest.get(FDataComponents.SUIT.get());
        return s == null ? SuitState.NEW : s;
    }

    /** The pack's carry, scaled while the suit's strength is working (a stronger servo carries more). */
    public static float carryFactor(Player player) {
        return SuitServer.carryFactor(player);
    }
}
