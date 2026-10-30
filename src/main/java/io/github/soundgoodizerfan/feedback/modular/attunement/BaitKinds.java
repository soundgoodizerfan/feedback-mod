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

import io.github.soundgoodizerfan.feedback.Feedback;
import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The runtime kinds of the Bait effect, on a handle or grip held in the
 * main hand, or (kinds {@code worn_lure} / {@code worn_scare}, armour plate/lining/studs) worn on any piece. {@code lure}: passive animals within a short radius walk toward the holder. {@code scare} (the mirror):
 * animals and weak hostiles flee. Checked once a second; tamed pets are left alone. Nothing wears.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class BaitKinds {

    /** Radius in blocks, per level, capped at {@link #MAX_RADIUS}. [OPEN] until played */
    private static final double RADIUS_PER_LEVEL = 4;
    private static final double MAX_RADIUS = 12;
    /** Ticks between checks. [OPEN] until played */
    private static final int PERIOD = 20;
    /** Most max health a hostile can have and still be scared off. [OPEN] until played */
    private static final float WEAK_HEALTH = 20f;

    private BaitKinds() {
    }

    public static void register() {
    }

    /** The summed level of {@code kind} across the player's worn armour parts. Allocates; call gated. */
    public static int worn(LivingEntity wearer, String kind) {
        int n = 0;
        for (ItemStack piece : wearer.getArmorSlots())
            if (!piece.isEmpty())
                n += TideKinds.level(piece, kind);
        return n;
    }

    /** As {@link #worn} but only the piece in {@code slot}. */
    public static int wornAt(LivingEntity wearer, EquipmentSlot slot, String kind) {
        ItemStack piece = wearer.getItemBySlot(slot);
        return piece.isEmpty() ? 0 : TideKinds.level(piece, kind);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % PERIOD != 0)
            return;
        ItemStack held = player.getMainHandItem();
        int lure = held.isEmpty() ? 0 : TideKinds.level(held, "lure"), scare = held.isEmpty() ? 0 : TideKinds.level(held, "scare");
        // Worn parts work like the held one, no item needed (roster 6b).
        lure += worn(player, "worn_lure");
        scare += worn(player, "worn_scare");
        if (lure + scare == 0)
            return;
        boolean scares = scare > 0;
        double r = Math.min(MAX_RADIUS, RADIUS_PER_LEVEL * (lure + scare));
        List<Mob> near = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(r),
                m -> m instanceof Animal || (scares && m instanceof Enemy && m.getMaxHealth() <= WEAK_HEALTH));
        for (Mob mob : near) {
            if (mob instanceof TamableAnimal pet && pet.isTame())
                continue;
            if (lure > 0) {
                if (mob.distanceToSqr(player) > 4)
                    mob.getNavigation().moveTo(player, 1.0);
            } else {
                Vec3 away = !(mob instanceof PathfinderMob pm) ? null : DefaultRandomPos.getPosAway(pm, 8, 4, player.position());
                if (away != null)
                    mob.getNavigation().moveTo(away.x, away.y, away.z, 1.3);
            }
        }
    }
}
