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
import net.minecraft.server.level.ServerPlayer;
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
        return hottest;
    }

    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker.level().isClientSide
                || event.getSource().getDirectEntity() != attacker)
            return;
        Map<Perk, Integer> perks = of(StrikeSource.weapon(attacker, event.getSource()));
        if (perks.isEmpty())
            return;
        LivingEntity target = event.getEntity();
        float bonus = 0f;
        if (perks.containsKey(Perk.SMITE) && target.getType().is(EntityTypeTags.UNDEAD))
            bonus += perks.get(Perk.SMITE) * ModularTuning.PERK_DAMAGE_PER_LEVEL;
        if (perks.containsKey(Perk.IMPALING))
            bonus += perks.get(Perk.IMPALING) * ModularTuning.PERK_DAMAGE_PER_LEVEL;
        if (bonus > 0f)
            event.setAmount(event.getAmount() + bonus);
        if (perks.containsKey(Perk.FIRE_ASPECT))
            target.igniteForSeconds(perks.get(Perk.FIRE_ASPECT) * ModularTuning.PERK_IGNITE_SECONDS_PER_LEVEL);
        if (perks.containsKey(Perk.ECHO))
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, ModularTuning.PERK_ECHO_GLOW_TICKS));
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
