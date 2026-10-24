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
import io.github.soundgoodizerfan.feedback.modular.StrikeSource;
import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The runtime kinds of the Mend effect ({@code docs/feedback_enchant_roster.md}). Registers its handlers with
 * {@link AttunementKinds} for the three shared moments, and subscribes to any other event itself, reading a part's
 * attunements through {@link MaterialPerks#attunements}.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class MendKinds {

    /** Ticks between one mend and the next while the stack is held or worn. [OPEN] until played */
    private static final int INTERVAL_TICKS = 40;
    /** Durability points restored per Mend level per mend. [OPEN] until played */
    private static final int REPAIR_PER_LEVEL = 2;
    /** Experience points a mend takes from its holder, when it has anything to repair. [OPEN] until played */
    private static final int XP_COST = 1;

    /** Experience points a level of tithe pays out per kill, and the durability it takes in exchange. [OPEN] until played */
    private static final int TITHE_XP_PER_LEVEL = 3, TITHE_WEAR_PER_LEVEL = 1;

    private MendKinds() {
    }

    /** Held or worn attuned stacks drink the holder's experience to close their wear; no experience, no repair. */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % INTERVAL_TICKS != 0)
            return;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ANIMAL_ARMOR)
                continue;
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isDamaged() || (player.totalExperience <= 0 && ReservoirKinds.stored(stack) <= 0))
                continue;
            int level = 0;
            for (MaterialPerks.Attunement a : MaterialPerks.attunements(stack))
                if (a.kind().equals("mend"))
                    level += a.level();
            if (level == 0)
                continue;
            // The tool's own reservoir pays first (roster 15); the holder only when it is dry.
            if (ReservoirKinds.take(stack, XP_COST) < XP_COST)
                player.giveExperiencePoints(-XP_COST);
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - level * REPAIR_PER_LEVEL));
        }
    }

    public static void register() {
    }

    /** Mend-: the weapon pays out experience for what it kills and is worn a little for it; never worn to nothing. */
    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player killer) || killer.level().isClientSide)
            return;
        ItemStack weapon = StrikeSource.weapon(killer, event.getSource());
        int level = 0;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(weapon))
            if (a.kind().equals("tithe"))
                level += a.level();
        if (level == 0)
            return;
        // A reservoir on the same weapon takes the points before orbs drop.
        int pay = level * TITHE_XP_PER_LEVEL;
        pay -= ReservoirKinds.add(weapon, pay);
        if (pay > 0)
            ExperienceOrb.award((ServerLevel) killer.level(), event.getEntity().position(), pay);
        if (weapon.isDamageableItem())
            weapon.setDamageValue(Math.min(weapon.getMaxDamage() - 1, weapon.getDamageValue() + level * TITHE_WEAR_PER_LEVEL));
    }
}
