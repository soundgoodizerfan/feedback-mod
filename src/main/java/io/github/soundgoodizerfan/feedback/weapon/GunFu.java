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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.skill.Rearms;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.core.hazard.HotContact;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The gun-fu techniques that read a gun's own figures (skill spec 14.2, 14.3): each is the body using
 * something the gun already carries (its baked impulse, its barrel's heat, its condition, the round in its
 * chamber), never a flat bonus.
 * <ul>
 * <li><b>Muzzle Stagger</b>: a point-blank shot just after a whip lands its impulse as stagger, over the target's mass.</li>
 * <li><b>Marked Rounds</b>: a close shot leaves a powder burn; the next melee blow spends it as damage.</li>
 * <li><b>Recoil Vault</b>: a shot fired airborne or against a surface pushes the shooter by impulse over their mass.</li>
 * <li><b>Braced Fire</b>: shooting braced against terrain sheds most of the heat bloom of the spread.</li>
 * <li><b>Hot Clear</b>: sneak-reload ejects a chambered round unfired when the barrel nears cook-off.</li>
 * <li><b>Dodge Charge</b>: a roll that swallowed a blow clears the gun's cycle wait.</li>
 * <li><b>Searing Strike</b>: any held item above the burn line sears on a landed blow, and cools.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class GunFu {

    private record Mark(UUID shooter, int count, long until) {
    }

    private static final Map<UUID, Mark> MARKS = new HashMap<>();

    private GunFu() {
    }

    // --- terrain reads -------------------------------------------------------------------------

    /** Whether {@code p} is braced against terrain: crouched against a block, or in the air against a wall (a wall kick). */
    public static boolean braced(Player p) {
        if (p.horizontalCollision && (p.isCrouching() || !p.onGround()))
            return true;
        if (!p.isCrouching())
            return false;
        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        if (look.lengthSqr() < 1e-6)
            return false;
        Vec3 at = p.position().add(look.normalize().scale(0.8)).add(0, 0.5, 0);
        return !p.level().getBlockState(BlockPos.containing(at)).getCollisionShape(p.level(), BlockPos.containing(at)).isEmpty();
    }

    /** The barrel heat fraction the spread reads: a braced Braced Fire shooter keeps only a share of the bloom. */
    public static float bloomHeat(LivingEntity shooter, float heat) {
        return shooter instanceof Player p && Skills.unlocked(p, "gun_fu_braced_fire") && braced(p)
                ? heat * GunFuTuning.BRACED_HEAT_KEEP : heat;
    }

    // --- on a shot -------------------------------------------------------------------------------

    /** The baked impulse of each shooter's last shot, for casts that read the gun they just fired (Muzzle Flash). */
    private static final Map<UUID, Float> LAST_IMPULSE = new HashMap<>();

    public static float lastImpulse(LivingEntity shooter) {
        return LAST_IMPULSE.getOrDefault(shooter.getUUID(), 0f);
    }

    /** A shot has left the muzzle at {@code f}: Recoil Vault pushes the shooter, and the gun pays for it. */
    public static void shotFired(ServerLevel level, LivingEntity shooter, ItemStack gun, GunFigures f, float heat) {
        LAST_IMPULSE.put(shooter.getUUID(), f.impulse());
        if (!(shooter instanceof Player p) || !Skills.unlocked(p, "gun_fu_recoil_vault"))
            return;
        Vec3 look = p.getLookAngle();
        Vec3 eye = p.getEyePosition();
        boolean surface = level.clip(new ClipContext(eye, eye.add(look.scale(GunFuTuning.VAULT_SURFACE)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.BLOCK;
        if (p.onGround() && !surface)
            return;
        double speed = f.impulse() / (BodyMass.of(p) + f.gunMass().value()) * GunFuTuning.VAULT_GAIN;
        p.setDeltaMovement(p.getDeltaMovement().add(look.scale(-speed)));
        p.hurtMarked = true;
        p.fallDistance = 0f;
        // Hot or worn, the gun pays more for being used as a thruster.
        gun.set(FWeapons.GUN_WEAR.get(), GunCondition.wear(gun)
                + f.impulse() * GunFuTuning.VAULT_WEAR_PER_IMPULSE * (1 + Math.max(0f, heat)) / GunCondition.condition(gun));
    }

    /** A bullet of {@code impulse} (kg m/s) from {@code shooter} has hurt {@code target}. */
    public static void bulletHit(ServerLevel level, LivingEntity shooter, Entity target, float impulse) {
        if (!(shooter instanceof ServerPlayer p) || !(target instanceof LivingEntity living))
            return;
        var whip = GunBludgeon.whip(p);
        if (whip != null && whip.target().equals(living.getUUID()) && p.distanceTo(living) <= GunFuTuning.MUZZLE_RANGE
                && Skills.unlocked(p, "gun_fu_muzzle_stagger") && Rearms.ready(p, "gun_fu_muzzle_stagger")) {
            CombatTicker.stagger(living, ParryTuning.staggerTicks(impulse * GunFuTuning.MUZZLE_JOULES_PER_IMPULSE,
                    ParryTuning.standInMass(living)), ParryTuning.STAGGER_SLOW);
            Rearms.spend(p, "gun_fu_muzzle_stagger");
        }
        if (Skills.unlocked(p, "gun_fu_marked_rounds") && p.distanceTo(living) <= GunFuTuning.MARK_RANGE) {
            Mark old = MARKS.get(living.getUUID());
            int n = old != null && old.shooter().equals(p.getUUID()) && level.getGameTime() <= old.until()
                    ? Math.min(GunFuTuning.MARK_MAX, old.count() + 1) : 1;
            MARKS.put(living.getUUID(), new Mark(p.getUUID(), n, level.getGameTime() + GunFuTuning.MARK_TICKS));
            // The count is shown on the target: one puff of powder smoke a stack.
            level.sendParticles(ParticleTypes.SMOKE, living.getX(), living.getY() + living.getBbHeight() * 0.6, living.getZ(),
                    n * 3, living.getBbWidth() * 0.3, 0.2, living.getBbWidth() * 0.3, 0.01);
        }
    }

    // --- on a blow ---------------------------------------------------------------------------------

    /** The next melee blow on a marked target spends the powder burn as damage. */
    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer p) || source.getDirectEntity() != p
                || !source.is(DamageTypes.PLAYER_ATTACK) || GunShots.inBullet() || MARKS.isEmpty())
            return;
        Mark m = MARKS.get(event.getEntity().getUUID());
        if (m == null || !m.shooter().equals(p.getUUID()))
            return;
        MARKS.remove(event.getEntity().getUUID());
        if (p.level().getGameTime() > m.until())
            return;
        event.setAmount(event.getAmount() + m.count() * GunFuTuning.MARK_DAMAGE);
        CombatCues.everyone(p, FSounds.FEEL_LIGHT.get(), FeelTuning.LIGHT_VOLUME, 0.8f);
    }

    /** Searing Strike: a held, non-gun item above the burn line sears what it lands on, and cools. */
    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer p) || source.getDirectEntity() != p
                || !source.is(DamageTypes.PLAYER_ATTACK) || GunShots.inBullet() || event.getNewDamage() <= 0f
                || !Skills.unlocked(p, "searing_strike"))
            return;
        ItemStack held = p.getMainHandItem();
        if (held.isEmpty() || held.getItem() instanceof GunItem || !ItemHeat.isHot(held, p.level()))
            return;
        Tu tu = ItemHeat.get(held, p.level());
        LivingEntity target = event.getEntity();
        target.invulnerableTime = 0; // the sear is its own blow
        if (HotContact.burn(target, tu))
            ItemHeat.set(held, new Tu(Math.max(AmbientTable.at(p.level()).value(), tu.value() - GunFuTuning.SEAR_COOL_TU)), p.level());
    }

    // --- cycle -------------------------------------------------------------------------------------

    /** Hot Clear: sneak-reload with a hot barrel ejects the chambered round unfired. @return whether it did. */
    public static boolean hotClear(Player p) {
        ItemStack gun = p.getMainHandItem();
        Cartridge round = gun.get(FWeapons.CHAMBER.get());
        if (round == null || !p.isCrouching() || !Skills.unlocked(p, "gun_fu_hot_clear"))
            return false;
        var load = round.load(io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials());
        if (load.isEmpty() || GunCondition.heatFraction(gun, p.level(), load.get().ignitionTu()) < GunFuTuning.HOT_CLEAR_LINE)
            return false;
        GunAmmo.give(p, GunAmmo.loose(round));
        gun.remove(FWeapons.CHAMBER.get()); // left unfed: a round that goes in now would cook as well
        p.level().playSound(null, p.getX(), p.getEyeY(), p.getZ(), net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_OPEN,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.7f, 1.8f);
        return true;
    }

    /** Dodge Charge: a roll that swallowed {@code source} clears the cycle wait of the guns in hand. */
    public static void dodged(LivingEntity who, DamageSource source) {
        if (!(who instanceof Player p) || source.getEntity() == null || !Skills.unlocked(p, "gun_fu_dodge_charge")
                || !Rearms.ready(p, "gun_fu_dodge_charge"))
            return;
        GunShots.clearCycle(p);
        Rearms.spend(p, "gun_fu_dodge_charge");
    }
}
