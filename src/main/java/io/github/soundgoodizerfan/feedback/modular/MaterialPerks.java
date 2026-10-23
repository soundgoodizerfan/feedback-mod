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

import java.util.EnumMap;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.HotContact;
import io.github.soundgoodizerfan.feedback.core.hazard.Protection;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.Material.Perk;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * What the stuff a weapon is made of does to a blow (spec 2.4): the {@code perks} of its parts'
 * material files, the qualitative counterpart to the stats. Nothing here names a material; a part
 * gives a perk because its file says so, and the level is the highest any part gives (two silver
 * parts are not two levels).
 *
 * <p>Fire aspect is the one with a cost. A part of a material that burns is a flame in the hand,
 * so the wielder is burned as a piece at the material's flame temperature would burn them
 * ({@link HotContact}), unless gloves or tongs rate for that heat ({@link Protection}) -- the
 * hazard pipeline every other hot thing already uses, a consequence and never a lock. The other
 * reading, keeping the weapon at a heat, was not taken: a workpiece's heat is stamped once and
 * cools linearly ({@code ItemHeat}), whereas a blaze rod is a fire that does not go out, and a
 * source fixed at its flame temperature is {@code HeatSource}'s idea, not a workpiece's.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class MaterialPerks {

    private MaterialPerks() {
    }

    /** The perks {@code stack}'s parts give, each at the highest level any part gives it. */
    public static Map<Perk, Integer> of(ItemStack stack) {
        Map<Perk, Integer> out = new EnumMap<>(Perk.class);
        ModularDesign design = ModularStacks.design(stack);
        if (design != null)
            for (ModularDesign.Part part : design.parts()) {
                Material m = MaterialTable.get().materials().get(part.material());
                if (m != null)
                    m.perks().forEach((perk, level) -> out.merge(perk, level, Math::max));
            }
        return out;
    }

    /** An attuned effect resolved for the role its part sits in: what it does there, how strong, and the part's material. */
    public record Attunement(String kind, int level, Material material) {}

    /**
     * The attunements a tool's parts carry ({@code attuned.*} in a part's dims, {@link Attunements}), each resolved through the
     * effect's entry for <em>that part's role</em> (its mirror entry when the level is negative) -- never by what the
     * whole tool is called. A role the effect has no entry for does nothing. The level is the magnitude; the side is already in the kind.
     */
    public static java.util.List<Attunement> attunements(ItemStack stack) {
        java.util.List<Attunement> out = new java.util.ArrayList<>();
        ModularDesign design = ModularStacks.design(stack);
        ModularType type = design == null ? null : ModularTypes.known().get(design.type());
        if (type == null)
            return out;
        for (ModularDesign.Part part : design.parts()) {
            ModularType.Role role = type.roles().get(part.role());
            Material m = MaterialTable.get().materials().get(part.material());
            if (role == null || m == null)
                continue;
            for (Map.Entry<String, Float> e : part.dims().entrySet()) {
                Attunements.Effect effect = Attunements.byKey(e.getKey());
                Attunements.Behaviour b = effect == null ? null : effect.on(role.name(), e.getValue() < 0f);
                if (b != null && !b.kind().isEmpty())
                    out.add(new Attunement(b.kind(), Math.abs(Math.round(e.getValue())), m));
            }
        }
        return out;
    }

    /** The hottest flame among the parts of {@code stack} that give fire aspect, or null. */
    private static Tu flame(ItemStack stack) {
        ModularDesign design = ModularStacks.design(stack);
        Tu hottest = null;
        if (design != null)
            for (ModularDesign.Part part : design.parts()) {
                Material m = MaterialTable.get().materials().get(part.material());
                if (m == null || !m.perks().containsKey(Perk.FIRE_ASPECT))
                    continue;
                Tu t = m.combustion().flatMap(Material.Combustion::flameTemperature).orElse(null);
                if (t != null && (hottest == null || t.value() > hottest.value()))
                    hottest = t;
            }
        for (Attunement attunement : attunements(stack)) {
            Tu t = AttunementKinds.flame(attunement);
            if (t != null && (hottest == null || t.value() > hottest.value()))
                hottest = t;
        }
        return hottest;
    }

    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker.level().isClientSide
                || event.getSource().getDirectEntity() != attacker)
            return;
        ItemStack weapon = StrikeSource.weapon(attacker, event.getSource());
        for (Attunement attunement : attunements(weapon))
            AttunementKinds.strike(event, attunement);
        Map<Perk, Integer> perks = of(weapon);
        if (perks.isEmpty())
            return;
        LivingEntity target = event.getEntity();
        float bonus = bonus(perks, target);
        if (bonus > 0f)
            event.setAmount(event.getAmount() + bonus);
        affect(perks, target);
    }

    /** An attuned plate worn sets whoever struck the wearer alight. */
    @SubscribeEvent
    public static void onWornHit(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || event.getEntity().level().isClientSide)
            return;
        for (ItemStack worn : event.getEntity().getArmorSlots())
            for (Attunement attunement : attunements(worn))
                AttunementKinds.struck(attacker, attunement);
    }

    /** Extra damage a blow with {@code perks} deals {@code target}. */
    public static float bonus(Map<Perk, Integer> perks, LivingEntity target) {
        float bonus = 0f;
        if (perks.containsKey(Perk.SMITE) && target.getType().is(EntityTypeTags.UNDEAD))
            bonus += perks.get(Perk.SMITE) * ModularTuning.PERK_DAMAGE_PER_LEVEL;
        if (perks.containsKey(Perk.IMPALING))
            bonus += perks.get(Perk.IMPALING) * ModularTuning.PERK_DAMAGE_PER_LEVEL;
        return bonus;
    }

    /** What a blow that landed does to {@code target} besides damage; shared by the swung and the thrown. */
    public static void affect(Map<Perk, Integer> perks, LivingEntity target) {
        if (perks.containsKey(Perk.FIRE_ASPECT))
            target.igniteForSeconds(perks.get(Perk.FIRE_ASPECT) * ModularTuning.PERK_IGNITE_SECONDS_PER_LEVEL);
        if (perks.containsKey(Perk.ECHO)) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, ModularTuning.PERK_ECHO_GLOW_TICKS));
            target.gameEvent(ModularTuning.PERK_ECHO_EVENT);
        }
        if (perks.containsKey(Perk.CHILL) && target.canFreeze())
            target.setTicksFrozen(Math.max(target.getTicksFrozen(),
                    perks.get(Perk.CHILL) * ModularTuning.PERK_CHILL_TICKS_PER_LEVEL));
        if (perks.containsKey(Perk.TOXIC))
            target.addEffect(new MobEffectInstance(MobEffects.POISON,
                    perks.get(Perk.TOXIC) * ModularTuning.PERK_TOXIC_TICKS_PER_LEVEL));
        if (perks.containsKey(Perk.BOUNCE)) {
            target.push(0, perks.get(Perk.BOUNCE) * ModularTuning.PERK_BOUNCE_PER_LEVEL, 0);
            target.hurtMarked = true;
        }
        if (perks.containsKey(Perk.CLING))
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    perks.get(Perk.CLING) * ModularTuning.PERK_CLING_TICKS_PER_LEVEL));
    }

    /** The share of {@code m}'s mass a swing feels: whole unless a featherlight perk takes some off. */
    public static float heft(Material m) {
        Integer level = m.perks().get(Perk.FEATHERLIGHT);
        return level == null ? 1f : Math.max(ModularTuning.PERK_FEATHER_FLOOR,
                1f - level * ModularTuning.PERK_FEATHER_PER_LEVEL);
    }

    /** A melee kill: the siphon's extra experience, the blink's step into the victim's place. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker.level().isClientSide
                || event.getSource().getDirectEntity() != attacker)
            return;
        Map<Perk, Integer> perks = of(StrikeSource.weapon(attacker, event.getSource()));
        LivingEntity victim = event.getEntity();
        if (perks.containsKey(Perk.SIPHON) && attacker.level() instanceof ServerLevel server)
            ExperienceOrb.award(server, victim.position(), perks.get(Perk.SIPHON) * ModularTuning.PERK_SIPHON_XP_PER_LEVEL);
        if (perks.containsKey(Perk.BLINK) && attacker.distanceTo(victim) <= ModularTuning.PERK_BLINK_MAX_BLOCKS
                && attacker.randomTeleport(victim.getX(), victim.getY(), victim.getZ(), true))
            attacker.level().playSound(null, attacker.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, 0.6f, 1.2f);
    }

    /** A flame in the hand, checked as often as any other hot thing held. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % FTuning.CONTACT_INTERVAL_TICKS != 0
                || player.isCreative() || player.isSpectator())
            return;
        float safe = Protection.handlesUpToTu(player);
        for (InteractionHand hand : InteractionHand.values()) {
            Tu flame = flame(player.getItemInHand(hand));
            if (flame != null && flame.value() > safe && HotContact.burn(player, flame)
                    && player instanceof ServerPlayer sp)
                sp.displayClientMessage(Component.translatable("hazard.feedback.hot_hand"), true);
        }
    }
}
