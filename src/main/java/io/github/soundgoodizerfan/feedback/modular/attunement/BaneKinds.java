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
package io.github.soundgoodizerfan.feedback.modular.attunement;

import io.github.soundgoodizerfan.feedback.modular.AttunementKinds;
import io.github.soundgoodizerfan.feedback.modular.MaterialPerks;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;
import static io.github.soundgoodizerfan.feedback.modular.AttunementKinds.onStrike;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The runtime kinds of the Bane effect ({@code docs/feedback_enchant_roster.md}). Registers its handlers with
 * {@link AttunementKinds} for the three shared moments, and subscribes to any other event itself, reading a part's
 * attunements through {@link MaterialPerks#attunements}.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class BaneKinds {

    /** Share of a quarry's blow one level of ward blunts. [OPEN] until played. */
    private static final float WARD_PER_LEVEL = 0.1f;
    /** Most the whole worn set blunts. [OPEN] until played. */
    private static final float WARD_CAP = 0.6f;

    /** Share by which a kin-attuned wearer's undead enemies, and a calmed undead, hit harder, per level. [OPEN] until played. */
    private static final float KIN_DAMAGE_PER_LEVEL = 0.25f;
    /** Detection (visibility) a kin-attuned wearer keeps against the quarry, lost per level, never below the floor. [OPEN] until played. */
    private static final float KIN_VISIBILITY_LOSS_PER_LEVEL = 0.25f;
    private static final float KIN_VISIBILITY_FLOOR = 0.1f;
    /** Ticks a struck quarry stays calm toward its striker, per level of kin_calm. [OPEN] until played. */
    private static final int CALM_TICKS_PER_LEVEL = 100;
    private static final String CALM_UNTIL = "feedback_kin_until", CALM_WHO = "feedback_kin_who", CALM_LEVEL = "feedback_kin_level";

    private BaneKinds() {
    }

    /** [OPEN] the quarry is fixed to undead and arthropods; the table's tag picker is a later design question. */
    private static boolean quarry(Entity e) {
        return e != null && (e.getType().is(EntityTypeTags.UNDEAD) || e.getType().is(EntityTypeTags.ARTHROPOD));
    }

    public static void register() {
        onStrike("bane", (event, a) -> {
            if (quarry(event.getEntity()))
                event.setAmount(event.getAmount() + a.level() * ModularTuning.PERK_DAMAGE_PER_LEVEL);
        });
        // Kin on an edge: the struck quarry loses interest in its striker for a while, and hits harder while it lasts.
        onStrike("kin_calm", (event, a) -> {
            LivingEntity target = event.getEntity();
            if (!quarry(target) || !(event.getSource().getEntity() instanceof LivingEntity striker))
                return;
            var data = target.getPersistentData();
            data.putLong(CALM_UNTIL, target.level().getGameTime() + (long) a.level() * CALM_TICKS_PER_LEVEL);
            data.putUUID(CALM_WHO, striker.getUUID());
            data.putInt(CALM_LEVEL, a.level());
            if (target instanceof Mob mob)
                mob.setTarget(null);
        });
    }

    private static boolean calm(Entity e) {
        return e != null && e.getPersistentData().getLong(CALM_UNTIL) > e.level().getGameTime();
    }

    /** A calmed quarry does not turn back on whoever calmed it until the calm runs out. */
    @SubscribeEvent
    public static void onRetarget(LivingChangeTargetEvent event) {
        LivingEntity mob = event.getEntity();
        LivingEntity next = event.getNewAboutToBeSetTarget();
        if (next != null && calm(mob) && next.getUUID().equals(mob.getPersistentData().getUUID(CALM_WHO)))
            event.setNewAboutToBeSetTarget(null);
    }

    /** Kin on armour: the quarry notices the wearer from less far away. */
    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (!quarry(event.getLookingEntity()))
            return;
        int level = kin(event.getEntity());
        if (level > 0)
            event.modifyVisibility(Math.max(KIN_VISIBILITY_FLOOR, 1f - level * KIN_VISIBILITY_LOSS_PER_LEVEL));
    }

    private static int kin(LivingEntity wearer) {
        int level = 0;
        for (ItemStack worn : wearer.getArmorSlots())
            for (MaterialPerks.Attunement a : MaterialPerks.attunements(worn))
                if (a.kind().equals("kin_hide"))
                    level += a.level();
        return level;
    }

    /** The kin trade: a quarry that does hit a kin-attuned wearer, or one that was calmed, hits harder. */
    @SubscribeEvent
    public static void onKinHit(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (!quarry(attacker))
            return;
        int level = calm(attacker) ? attacker.getPersistentData().getInt(CALM_LEVEL) : 0;
        level += kin(event.getEntity());
        if (level > 0)
            event.setAmount(event.getAmount() * (1f + level * KIN_DAMAGE_PER_LEVEL));
    }

    @SubscribeEvent
    public static void onWard(LivingIncomingDamageEvent event) {
        if (!quarry(event.getSource().getEntity()))
            return;
        float share = 0f;
        for (ItemStack worn : event.getEntity().getArmorSlots())
            for (MaterialPerks.Attunement a : MaterialPerks.attunements(worn))
                if (a.kind().equals("bane_ward"))
                    share += a.level() * WARD_PER_LEVEL;
        if (share > 0f)
            event.setAmount(event.getAmount() * (1f - Math.min(WARD_CAP, share)));
    }
}
