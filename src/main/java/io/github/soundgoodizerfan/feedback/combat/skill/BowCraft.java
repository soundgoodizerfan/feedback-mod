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
package io.github.soundgoodizerfan.feedback.combat.skill;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.cast.Stamina;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.weapon.GunFu;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The bow and crossbow side of the gun skills: every gun buff has a projectile-weapon twin in the same tree, so a
 * player not tech-tree'd into guns still benefits. A projectile weapon is any item that is a
 * {@link ProjectileWeaponItem} (the arrow remembers the weapon that loosed it), never an item id. Each perk
 * reads the arrow's own speed, which is the draw: a hard-drawn bow carries more than a short one.
 * <ul>
 * <li>Observation, Far sight (twin of Dead eye): a hard-drawn arrow flies flat for its first second.</li>
 * <li>Observation, Quarry mark (twin of Called shot): a hit at range makes the target glow, longer for more energy.</li>
 * <li>Brute, Point-blank draw (twin of Muzzle stagger): a close hit staggers by the arrow's energy over the target's mass.</li>
 * <li>Chaos, Barbed shaft (twin of Marked rounds): a close hit leaves a barb; the next melee blow spends it.</li>
 * <li>Anchor, Braced draw (twin of Braced fire): loosed braced against terrain, the arrow leaves exactly down the sight line.</li>
 * <li>Veil, Snap shot (twin of Dodge charge): after a roll that swallowed a blow, the next arrow leaves at full draw.</li>
 * <li>Proficiency, Quarry breath (twin of Hot clear): a kill at range gives stamina back by the arrow's energy.</li>
 * </ul>
 * Constants are STAND-INS ([OPEN]) until played.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class BowCraft {

    /** Arrow mass, kg (stand-in), and blocks/tick to m/s. */
    private static final float ARROW_MASS = 0.05f, MPS = 20f;
    /** Full draw leaves at this many blocks per tick. */
    private static final double FULL_DRAW = 3.0;
    private static final double FLAT_SPEED = 2.5;
    private static final int FLAT_TICKS = 20, SNAP_TICKS = 60;
    private static final double CLOSE = 4.0;
    private static final int BARB_TICKS = 200, BARB_MAX = 5;
    private static final float BARB_DAMAGE = 1.5f, BREATH_PER_JOULE = 0.2f, BREATH_MAX = 30f;
    private static final int GLOW_PER_JOULE = 2, GLOW_MAX = 200;

    private record Barb(UUID shooter, int count, long until) {
    }

    private static final Map<UUID, Barb> BARBS = new HashMap<>();
    private static final Map<UUID, Long> SNAP = new HashMap<>();
    private static final Set<AbstractArrow> FLAT = Collections.newSetFromMap(new WeakHashMap<>());

    private BowCraft() {
    }

    /** Joules the arrow carries now: half m v squared. */
    private static float energy(AbstractArrow arrow) {
        float v = (float) arrow.getDeltaMovement().length() * MPS;
        return 0.5f * ARROW_MASS * v * v;
    }

    private static ServerPlayer shooter(AbstractArrow arrow) {
        return arrow.getOwner() instanceof ServerPlayer p && arrow.getWeaponItem() != null
                && arrow.getWeaponItem().getItem() instanceof ProjectileWeaponItem ? p : null;
    }

    /** A roll swallowed a blow: Snap shot arms (called from {@code GunFu.dodged}, which the roll already calls). */
    public static void dodged(LivingEntity who) {
        if (who instanceof ServerPlayer p && Skills.unlocked(p, "snap_shot"))
            SNAP.put(p.getUUID(), p.level().getGameTime() + SNAP_TICKS);
    }

    @SubscribeEvent
    public static void onLoosed(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow) || arrow.tickCount > 0)
            return;
        ServerPlayer p = shooter(arrow);
        if (p == null)
            return;
        Long snap = SNAP.remove(p.getUUID());
        Vec3 v = arrow.getDeltaMovement();
        if (snap != null && p.level().getGameTime() <= snap && v.length() < FULL_DRAW) {
            arrow.setDeltaMovement(v.normalize().scale(FULL_DRAW));
            arrow.setCritArrow(true);
        }
        if (Skills.unlocked(p, "braced_draw") && GunFu.braced(p))
            arrow.setDeltaMovement(p.getLookAngle().scale(arrow.getDeltaMovement().length()));
        if (Skills.unlocked(p, "far_sight") && arrow.getDeltaMovement().length() >= FLAT_SPEED) {
            arrow.setNoGravity(true);
            FLAT.add(arrow);
        }
    }

    /** Far sight ends: the arrow starts to drop after its first second. */
    @SubscribeEvent
    public static void onArrowTick(EntityTickEvent.Post event) {
        if (!FLAT.isEmpty() && event.getEntity() instanceof AbstractArrow arrow && arrow.tickCount >= FLAT_TICKS
                && FLAT.remove(arrow))
            arrow.setNoGravity(false);
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        if (!(source.getDirectEntity() instanceof AbstractArrow arrow) || event.getNewDamage() <= 0f)
            return;
        ServerPlayer p = shooter(arrow);
        if (p == null)
            return;
        LivingEntity target = event.getEntity();
        double dist = p.distanceTo(target);
        float joules = energy(arrow);
        // Observation pays for any projectile weapon's hit at range, the same as a gun's.
        if (dist >= SkillTuning.OBSERVE_RANGE)
            SkillEarning.award(p, SkillEarning.OBSERVATION, target.getStringUUID(), SkillTuning.GUN_HIT);
        if (dist >= SkillTuning.OBSERVE_RANGE && Skills.unlocked(p, "quarry_mark") && !target.isDeadOrDying())
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, Math.min(GLOW_MAX, Math.round(joules * GLOW_PER_JOULE))));
        if (dist <= CLOSE && !target.isDeadOrDying()) {
            if (Skills.unlocked(p, "point_blank_draw"))
                CombatTicker.stagger(target, ParryTuning.staggerTicks(joules, ParryTuning.standInMass(target)),
                        ParryTuning.STAGGER_SLOW);
            if (Skills.unlocked(p, "barbed_shaft")) {
                Barb old = BARBS.get(target.getUUID());
                long now = p.level().getGameTime();
                int n = old != null && old.shooter().equals(p.getUUID()) && now <= old.until()
                        ? Math.min(BARB_MAX, old.count() + 1) : 1;
                BARBS.put(target.getUUID(), new Barb(p.getUUID(), n, now + BARB_TICKS));
            }
        }
        if (dist >= SkillTuning.OBSERVE_RANGE && target.isDeadOrDying() && Skills.unlocked(p, "quarry_breath"))
            Stamina.gain(p, Math.min(BREATH_MAX, joules * BREATH_PER_JOULE));
    }

    /** Barbed shaft: the next melee blow on a barbed target spends the barbs as damage. */
    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (BARBS.isEmpty() || !(source.getEntity() instanceof ServerPlayer p) || source.getDirectEntity() != p
                || !source.is(DamageTypes.PLAYER_ATTACK) || GunShots.inBullet())
            return;
        Barb b = BARBS.get(event.getEntity().getUUID());
        if (b == null || !b.shooter().equals(p.getUUID()))
            return;
        BARBS.remove(event.getEntity().getUUID());
        if (p.level().getGameTime() <= b.until())
            event.setAmount(event.getAmount() + b.count() * BARB_DAMAGE);
    }
}
