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
package io.github.soundgoodizerfan.feedback.modular;

import java.util.Map;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * A signature behaviour of an archetype (spec 5.4): how a variant fights rather than what it
 * measures. A trait is one class with every hook defaulting to nothing; a type or variant names it
 * by id in data ({@code trait}, with its numbers in {@code params}) and {@link ModularTraits}
 * dispatches to it when the item is held. A trait owns its own state (a stack of hits, a cooldown)
 * and keeps it off the item where it can, so nothing here needs a new component.
 *
 * <p>Hooks run on the server unless a trait says otherwise; a trait that needs the client (an
 * outline, an animation) sends a payload or implements {@link #client}.
 */
public interface ModularTrait {

    /** The numbers a trait reads, by name, with a default. */
    record Params(Map<String, Float> values) {
        public float get(String name, float fallback) {
            return values.getOrDefault(name, fallback);
        }
    }

    /** A blow the wielder lands with the item held: the damage may be changed through the event. */
    default void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
    }

    /** The wielder swung at a block with the item held (server, fires as the swing starts and again while mining). */
    default void onGroundHit(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event, Player player,
                             ItemStack weapon, Params params) {
    }

    /** The wielder killed something with the item held. */
    default void onKill(LivingDeathEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
    }

    /** The wielder broke a block with the item held. */
    default void onBlockBreak(BlockEvent.BreakEvent event, ItemStack weapon, Params params) {
    }

    /** Use (right click). Pass to leave it to the item's normal behaviour. */
    default InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand, ItemStack weapon, Params params) {
        return InteractionResultHolder.pass(weapon);
    }

    /** How long use can be held, in ticks; 0 for an item that is not held out. */
    default int useDuration(ItemStack weapon, Params params) {
        return 0;
    }

    /** The pose while held out. */
    default UseAnim useAnimation(ItemStack weapon, Params params) {
        return UseAnim.NONE;
    }

    /** Every tick use is held; {@code remaining} counts down from {@link #useDuration}. */
    default void onUseTick(Level level, LivingEntity user, ItemStack weapon, int remaining, Params params) {
    }

    /** Use let go before its duration ran out. */
    default void releaseUsing(ItemStack weapon, Level level, LivingEntity user, int remaining, Params params) {
    }

    // --- worn hooks: an armour piece's trait, run for a wearer; each trait id fires once per wearer ---

    /**
     * The wearer is about to be hurt. Several worn pieces never stack one trait: {@link ModularTraits}
     * calls each trait id once per wearer, on the first piece that has it (head, chest, legs, feet).
     */
    default void onWornHurt(LivingIncomingDamageEvent event, LivingEntity wearer, ItemStack piece, Params params) {
    }

    /** The wearer lands from a fall. */
    default void onWornFall(LivingFallEvent event, LivingEntity wearer, ItemStack piece, Params params) {
    }

    /** The wearer is about to be knocked back. */
    default void onWornKnockback(LivingKnockBackEvent event, LivingEntity wearer, ItemStack piece, Params params) {
    }

    /** A mob works out how well it sees the wearer. */
    default void onWornVisibility(LivingEvent.LivingVisibilityEvent event, LivingEntity wearer, ItemStack piece, Params params) {
    }

    /** Every tick, for a player wearing the piece (server). */
    default void onWornTick(Player wearer, ItemStack piece, Params params) {
    }

    /** The first tick this trait is no longer worn by a player, to undo what {@link #onWornTick} left. */
    default void onWornRemoved(Player wearer) {
    }

    /** A projectile the weapon has just made, before it is shot (ranged weapons): its damage and piercing may be set. */
    default void onArrow(net.minecraft.world.entity.projectile.AbstractArrow arrow, ItemStack weapon, Params params) {
    }

    /** A projectile the weapon fired is about to hurt something; the damage may be changed through the event. */
    default void onArrowHit(LivingIncomingDamageEvent event, net.minecraft.world.entity.projectile.AbstractArrow arrow,
                            ItemStack weapon, Params params) {
    }

    /** A crossbow has just been loaded by a player (server). */
    default void onLoaded(net.minecraft.server.level.ServerPlayer player, ItemStack weapon, Params params) {
    }

    /** The wielder's shield is being tested against a blow (run after the shield's own capacity is applied). */
    default void onShieldBlock(net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent event, LivingEntity blocker,
                               ItemStack shield, Params params) {
    }

    /** Called once on the client at setup, for a trait with a client side (renderers, key handling). */
    default void client() {
    }
}
