// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.bake.SpringPart;
import io.github.soundgoodizerfan.feedback.modular.bake.SpringSet;

import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The spring addon ({@link SpringPart}): a spring fitted to a maul, a sword or a spear. Use (right click) cocks it; the next blow lets
 * it go and adds the damage the spring stores (its volume times its stiffness), then it has to be reset for {@code reset} ticks before
 * it can be cocked again. A sword's release also lunges the wielder forward. The spring takes the reaction of what it hits, more against
 * armour, and the mount rates it by the weapon's toughness: past that rating it takes a set and stores less each time, far past it it
 * snaps ({@link SpringSet}). On a thrown weapon the spring adds to the throw instead ({@link io.github.soundgoodizerfan.feedback.modular.combat.Throwing}).
 */
public final class Spring implements ModularTrait {

    private static final String COCKED = "SpringCocked";

    private static boolean cocked(ItemStack weapon) {
        CustomData data = weapon.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBoolean(COCKED);
    }

    private static void cock(ItemStack weapon, boolean on) {
        CustomData.update(DataComponents.CUSTOM_DATA, weapon, tag -> tag.putBoolean(COCKED, on));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand, ItemStack weapon, Params params) {
        if (cocked(weapon) || ModularStacks.broken(weapon))
            return InteractionResultHolder.pass(weapon);
        if (!level.isClientSide) {
            cock(weapon, true);
            level.playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 0.8f, 0.8f);
        }
        return InteractionResultHolder.success(weapon);
    }

    @Override
    public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
        if (!cocked(weapon))
            return;
        SpringPart spring = SpringPart.of(weapon).orElse(null);
        cock(weapon, false);
        if (spring == null)
            return;
        ModularFigures f = ModularStacks.figures(weapon);
        float toughness = f == null ? 20f : f.get(ModularFigures.Fig.EDGE_TOUGHNESS);
        float rating = params.get("rating", 12f) * Mth.clamp(toughness / 20f, 0.25f, 2f);
        float bonus = spring.stored() * (1f - SpringSet.set(weapon));
        float kick = bonus * (1f + event.getEntity().getArmorValue() / 10f);
        SpringSet.Outcome outcome = SpringSet.overload(weapon, kick / rating, attacker);
        if (outcome != SpringSet.Outcome.SNAP) {
            event.setAmount(event.getAmount() + bonus);
            if (params.get("lunge", 0f) > 0f) {
                // A blade's release carries the wielder on after the blow: forward along the look, level.
                net.minecraft.world.phys.Vec3 look = attacker.getLookAngle();
                net.minecraft.world.phys.Vec3 dir = new net.minecraft.world.phys.Vec3(look.x, 0, look.z).normalize();
                attacker.push(dir.x * ModularTuning.SPRING_LUNGE * (1f - SpringSet.set(weapon)), 0, dir.z * ModularTuning.SPRING_LUNGE * (1f - SpringSet.set(weapon)));
                attacker.hurtMarked = true;
            }
        }
        attacker.level().playSound(null, attacker.blockPosition(),
                outcome == SpringSet.Outcome.SNAP ? SoundEvents.ITEM_BREAK : SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.9f, 1.2f);
        if (attacker instanceof Player player)
            player.getCooldowns().addCooldown(weapon.getItem(), Math.round(params.get("reset", 30f)));
    }
}
