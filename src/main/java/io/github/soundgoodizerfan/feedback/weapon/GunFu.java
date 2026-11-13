// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
 * <li><b>Powder burn</b>: a close shot sets the target alight.</li>
 * <li><b>Roll with the kick</b>: a trained shooter takes less kick and less spread from every shot.</li>
 * <li><b>Braced Fire</b>: shooting braced against terrain sheds most of the heat bloom of the spread.</li>
 * <li><b>Hot Clear</b>: sneak-reload ejects a chambered round unfired when the barrel nears cook-off.</li>
 * <li><b>Dodge Charge</b>: a roll that swallowed a blow clears the gun's cycle wait.</li>
 * <li><b>Searing Strike</b>: any held item above the burn line sears on a landed blow, and cools.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class GunFu {

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

    /** A shot has left the muzzle at {@code f}: its impulse is remembered for casts that read the gun they just fired. */
    public static void shotFired(ServerLevel level, LivingEntity shooter, ItemStack gun, GunFigures f, float heat) {
        LAST_IMPULSE.put(shooter.getUUID(), f.impulse());
    }

    /** Roll with the kick: a trained shooter gives with the recoil, so every shot kicks the view and strains the hand less and spreads less. */
    public static GunFigures rolled(LivingEntity shooter, GunFigures f) {
        return shooter instanceof Player p && Skills.unlocked(p, "gun_fu_roll_with_kick")
                ? f.withKick(GunFuTuning.ROLL_KICK_KEEP).withSpread(GunFuTuning.ROLL_SPREAD_KEEP) : f;
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
        if (Skills.unlocked(p, "gun_fu_powder_burn") && p.distanceTo(living) <= GunFuTuning.MARK_RANGE) {
            // Real burn: the muzzle flash sets the target alight (fire ticks, not a stored count), so water or fire resistance answers it.
            if (living.getRemainingFireTicks() < GunFuTuning.POWDER_BURN_TICKS)
                living.setRemainingFireTicks(GunFuTuning.POWDER_BURN_TICKS);
            io.github.soundgoodizerfan.feedback.combat.cast.Disorder.note(p, living, io.github.soundgoodizerfan.feedback.combat.cast.Disorder.Kind.POWDERED);
            level.sendParticles(ParticleTypes.SMOKE, living.getX(), living.getY() + living.getBbHeight() * 0.6, living.getZ(),
                    6, living.getBbWidth() * 0.3, 0.2, living.getBbWidth() * 0.3, 0.01);
        }
    }

    /** Searing Strike: a held item above the burn line (a gun only through its Heat+ edge) sears what it lands on, and cools. */
    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer p) || source.getDirectEntity() != p
                || !source.is(DamageTypes.PLAYER_ATTACK) || GunShots.inBullet() || event.getNewDamage() <= 0f
                || !Skills.unlocked(p, "searing_strike"))
            return;
        ItemStack held = p.getMainHandItem();
        if (held.isEmpty())
            return;
        Tu tu = ItemHeat.feltNoBarrel(held, p.level());
        if (tu == null)
            return;
        LivingEntity target = event.getEntity();
        target.invulnerableTime = 0; // the sear is its own blow
        if (HotContact.burn(target, tu)) {
            if (Skills.unlocked(p, "scorch"))
                target.setRemainingFireTicks(target.getRemainingFireTicks() * 2);
            io.github.soundgoodizerfan.feedback.combat.cast.Disorder.kindled(p, target);
            if (!(held.getItem() instanceof GunItem))
                ItemHeat.drain(held, p.level(), GunFuTuning.SEAR_COOL_TU);
        }
    }

    // --- cycle -------------------------------------------------------------------------------------

    /** Hot Clear: sneak-reload with a hot barrel ejects the chambered round unfired. @return whether it did. */
    public static boolean hotClear(Player p) {
        ItemStack gun = p.getMainHandItem();
        Cartridge round = gun.get(FWeapons.CHAMBER.get());
        if (round == null || !p.isCrouching() || !Skills.unlocked(p, "gun_fu_hot_clear"))
            return false;
        var materials = io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials();
        var load = round.load(materials);
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
        if (source.getEntity() != null)
            io.github.soundgoodizerfan.feedback.combat.skill.BowCraft.dodged(who);
        if (!(who instanceof Player p) || source.getEntity() == null || !Skills.unlocked(p, "gun_fu_dodge_charge")
                || !Rearms.ready(p, "gun_fu_dodge_charge"))
            return;
        GunShots.clearCycle(p);
        Rearms.spend(p, "gun_fu_dodge_charge");
    }
}
