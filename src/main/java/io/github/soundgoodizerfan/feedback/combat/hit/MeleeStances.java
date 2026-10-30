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
package io.github.soundgoodizerfan.feedback.combat.hit;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.skill.Rearms;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillTree.Rearm;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.StrikeSource;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The state-reading melee techniques (skill spec 4.3). Each reads where or how a blow landed, never what
 * weapon it was, and each is keyed on the weapon's own baked reach and balance:
 * <ul>
 * <li><b>Far End</b> and <b>Close Work</b> (exclusive stances): a blow landing out at the tip of the
 * wielder's reach hits harder, scaled by how head-heavy the weapon is (a head far out moves fastest); a blow
 * landing inside half reach recharges the swing faster. Neither is the right one: a long spear wants the
 * first, a dagger in a doorway the second.</li>
 * <li><b>Pry Open</b>: a blow landing on a target whose weapon was just beaten (it is staggered, or its
 * guard is up) binds that hand. A raised shield is disabled instead. Softer or more worn steel binds longer.
 * Spent until the wielder completes a parry, so it is the follow-up to an opening, not a free move.</li>
 * </ul>
 * Main-hand blows only: an off-hand strike reaches by its own weapon's figures.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class MeleeStances {

    private MeleeStances() {
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onIncoming(LivingIncomingDamageEvent event) {
        var source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player) || source.getDirectEntity() != player
                || !source.is(DamageTypes.PLAYER_ATTACK) || GunShots.inBullet()
                || StrikeSource.hand(source) != InteractionHand.MAIN_HAND)
            return;
        LivingEntity target = event.getEntity();
        double reach = player.entityInteractionRange();
        double dist = Math.sqrt(target.getBoundingBox().distanceToSqr(player.getEyePosition()));
        ItemStack weapon = player.getMainHandItem();
        var figures = ModularStacks.figures(weapon);

        if (dist >= reach * MeleeTuning.FAR_FRACTION && Skills.unlocked(player, "far_end")) {
            float balance = figures == null ? 0.5f : Mth.clamp(figures.get(Fig.BALANCE), 0f, 1f);
            event.setAmount(event.getAmount() * (1 + MeleeTuning.FAR_BONUS * (0.5f + balance)));
        } else if (dist <= reach * MeleeTuning.CLOSE_FRACTION && Skills.unlocked(player, "close_work")) {
            // Vanilla resets the swing clock after the blow is dealt, so the shave goes in once that is done.
            player.getServer().execute(() -> player.attackStrengthTicker += MeleeTuning.CLOSE_SHAVE);
        }

        if (Skills.unlocked(player, "pry_open") && Rearms.ready(player, "pry_open")
                && (target.isBlocking() || CombatState.of(target).phase() == CombatPhase.STAGGER)) {
            float soft = figures(target) == null ? 1f
                    : Mth.clamp(ModularTuning.IRON_CLASS_HARDNESS / Math.max(0.1f, figures(target).get(Fig.HARDNESS)), 0.5f, 2f);
            float worn = 1 + ModularStacks.worn(target.getMainHandItem());
            int ticks = Math.round(MeleeTuning.PRY_TICKS * soft * worn);
            if (pry(target, ticks)) {
                Rearms.spend(player, "pry_open");
                io.github.soundgoodizerfan.feedback.combat.cast.Disorder.note(player, target, io.github.soundgoodizerfan.feedback.combat.cast.Disorder.Kind.STRIPPED);
            }
        }
    }

    private record Held(ItemStack stack, ModularType.Family family) {
    }

    private static final java.util.Map<java.util.UUID, Held> HELD = new java.util.HashMap<>();

    /**
     * Draw Switch: swapping to a weapon of a different baked family gives the swing back at once, where vanilla
     * would restart the recharge. Spent on the clock, so it is once in a while, not a way to double the swing rate.
     */
    @SubscribeEvent
    public static void onTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p))
            return;
        ItemStack now = p.getMainHandItem();
        Held prev = HELD.get(p.getUUID());
        if (prev != null && !ItemStack.matches(prev.stack(), now)) {
            var fam = io.github.soundgoodizerfan.feedback.combat.skill.SkillEarning.family(now);
            if (fam != null && prev.family() != null && fam != prev.family() && Skills.unlocked(p, "draw_switch")
                    && Rearms.ready(p, "draw_switch")) {
                p.attackStrengthTicker = (int) Math.ceil(p.getCurrentItemAttackStrengthDelay());
                Rearms.spend(p, "draw_switch");
            }
        }
        if (prev == null || !ItemStack.matches(prev.stack(), now))
            HELD.put(p.getUUID(), new Held(now.copy(), io.github.soundgoodizerfan.feedback.combat.skill.SkillEarning.family(now)));
    }

    private static io.github.soundgoodizerfan.feedback.modular.ModularFigures figures(LivingEntity who) {
        return ModularStacks.figures(who.getMainHandItem());
    }

    /**
     * Binds {@code target}'s weapon hand for {@code ticks}: a raised shield is disabled, a player's held
     * weapon goes on cooldown, and anything else is held in its stagger as long (a mob has no cooldown to put on).
     * @return whether anything was bound
     */
    private static boolean pry(LivingEntity target, int ticks) {
        if (target.isBlocking()) {
            if (target instanceof Player p)
                p.getCooldowns().addCooldown(p.getUseItem().getItem(), ticks);
            target.stopUsingItem();
            target.level().broadcastEntityEvent(target, (byte) 30);
            return true;
        }
        if (target instanceof Player p && !p.getMainHandItem().isEmpty())
            p.getCooldowns().addCooldown(p.getMainHandItem().getItem(), ticks);
        else
            CombatTicker.stagger(target, ticks, io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning.STAGGER_SLOW);
        return true;
    }
}
