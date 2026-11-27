// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.registry.FSounds;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;

/** Rend: a strike that grinds the worn armour of whatever it hits, hardness against hardness. */
final class Rend {

    private Rend() {
    }

    /** Rend: a short blow at what you face; each armour piece it wears loses durability by your weapon's hardness against that piece's, and your weapon is worn by theirs. Nothing worn, nothing happens. */
    static boolean act(ServerPlayer p) {
        ItemStack w = p.getMainHandItem();
        LivingEntity t = ahead(p, 3.2, 0.5);
        if (w.isEmpty() || w.getItem() instanceof GunItem || t == null)
            return false;
        float struck = Disorder.wearArmour(t, Disorder.hardness(w));
        if (struck <= 0f)
            return false;
        t.invulnerableTime = 0;
        t.hurt(p.damageSources().playerAttack(p), 2f);
        Disorder.selfWear(p, struck);
        Disorder.note(p, t, Disorder.Kind.WORN);
        // Tainted wound: the opened armour takes a dose of the pack's potion.
        if (io.github.soundgoodizerfan.feedback.combat.skill.Skills.unlocked(p, "tainted_wound"))
            Vials.dose(p, t, Vials.draw(p, io.github.soundgoodizerfan.feedback.potion.PotionTuning.SPRAY_DOSE_MB));
        p.swing(InteractionHand.MAIN_HAND, true);
        ServerLevel lv = p.serverLevel();
        Vec3 chest = t.position().add(0, t.getBbHeight() * 0.65, 0);
        CastFx.shards(lv, chest, CastFx.EMBER, 1.0f, 6);
        CastFx.streak(lv, chest.add(-0.5, 0.5, 0), chest.add(0.5, -0.5, 0), CastFx.STEEL, 0.03f, 5);
        CastFx.sound(lv, chest, FSounds.CAST_SLASH.get(), 1f, 0.8f);
        CastFx.sound(lv, chest, FSounds.CAST_SHATTER.get(), 0.6f, 1.2f);
        return true;
    }
}
