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
package io.github.soundgoodizerfan.feedback.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Perfect instrumentation, which is the one thing philosophy 8 says a player may never buy.
 *
 * <h2>What it is for</h2>
 * Every readout in the mod is an adjective until an instrument says otherwise. That is the design,
 * and it makes the mod almost impossible to <em>develop</em>: there is no honest way to ask a shaft
 * what it is really doing. Sneak-clicking each block printed the answer to chat, which worked and
 * was six copies of the same code scattered across five blocks.
 * <p>
 * This replaces all of it. Wear it and every qualitative readout -- Jade lines, the workpiece
 * tooltip -- turns into the figure the simulation actually holds. It is a cheat, deliberately, and
 * it is shaped like one: creative-only, out of the mod's own tab, living in vanilla's Operator
 * Utilities tab instead -- see {@code FCreativeTabs}.
 *
 * <h2>Why it is not a thermometer</h2>
 * Instruments are a real progression axis (§8: range, resolution, accuracy, response). This is not
 * the top of that ladder and must never be allowed to read as it -- an instrument with infinite
 * accuracy and no cost is not a better instrument, it is the absence of the mechanic. Keeping it
 * out of the creative tab is what stops it being mistaken for tier five.
 *
 * <h2>Implementing Equipable rather than extending ArmorItem</h2>
 * {@link Equipable} is what actually decides where a stack goes and gives the right-click swap for
 * free. ArmorItem would additionally demand a registered armour material and an armour-layer
 * texture, and would then render a missing-texture helmet on the player -- all to describe an item
 * that provides no protection and should be invisible anyway.
 */
public class DebugHelmetItem extends Item implements Equipable, Instrument {

    public DebugHelmetItem(Properties properties) {
        super(properties);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return swapWithEquipmentSlot(this, level, player, hand);
    }

    /** Whether this player is currently reading exact figures instead of adjectives. */
    public static boolean wornBy(Player player) {
        return player != null && player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof DebugHelmetItem;
    }

    /**
     * Reads everything, exactly. That is what makes it a cheat rather than a tier: §8 says no
     * purchasable apparatus reaches zero variance, so perfect instrumentation is the one thing a
     * player may never actually buy.
     * <p>
     * Answering for every quantity by construction is also the point of the interface — a
     * quantity added tomorrow is covered today, with no edit here.
     */
    @Override
    public boolean canRead(Quantity quantity) {
        return true;
    }

    /**
     * The truth tier, for every quantity. That is only worth anything because every display can
     * print the truth tier ({@link Instrument#TRUTH}); before that rule a display written for the
     * one instrument that read its quantity -- scratch files' ladder, a balance's tenth of a Mu --
     * showed the helmet the same coarse figure.
     */
    @Override
    public float resolution(Quantity quantity) {
        return TRUTH;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.debug");
    }
}
