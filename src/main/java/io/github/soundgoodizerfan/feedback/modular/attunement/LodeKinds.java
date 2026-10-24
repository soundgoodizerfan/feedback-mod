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
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.modular.MaterialPerks;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The runtime kinds of the Lode effect ({@code docs/feedback_enchant_roster.md} 26, magnetism). Weapon head/tip:
 * {@code draw} sends the drops of what the blow kills flying to the wielder, {@code fling} (the mirror) throws them
 * away. Armour: {@code worn_draw} is an item magnet, {@code worn_fling} turns metal-bearing arrows and tridents aside.
 * Every kind goes dead while its part is hotter than its material's Curie point. Wears nothing.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class LodeKinds {

    /** Blocks the magnet reaches per level, and the pull per tick of it. [OPEN] until played */
    private static final double MAGNET_RANGE_PER_LEVEL = 2.5, MAGNET_PULL = 0.15;
    /** Ticks between magnet pulls. [OPEN] until played */
    private static final int MAGNET_PERIOD = 4;
    /** Drops: speed a draw/fling gives, per block of distance, and the cap. [OPEN] until played */
    private static final double DROP_SPEED_PER_BLOCK = 0.15, DROP_SPEED_CAP = 1.2;
    /** Share of an arrow's velocity left after the repeller turns it, reversed. [OPEN] until played */
    private static final double DEFLECT = 0.4;

    private LodeKinds() {
    }

    public static void register() {
    }

    /** Summed level of {@code kind} across {@code stack}'s parts that are still below their material's Curie point. */
    private static int live(ItemStack stack, Level level, String kind) {
        if (stack.isEmpty())
            return 0;
        int n = 0;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(stack))
            if (a.kind().equals(kind) && !a.material().separation().curieTemperature()
                    .map(c -> ItemHeat.get(stack, level).value() > c.value()).orElse(false))
                n += a.level();
        return n;
    }

    private static int worn(LivingEntity wearer, String kind) {
        int n = 0;
        for (ItemStack piece : wearer.getArmorSlots())
            n += live(piece, wearer.level(), kind);
        return n;
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player p))
            return;
        ItemStack held = p.getMainHandItem();
        int draw = live(held, p.level(), "draw"), fling = live(held, p.level(), "fling");
        if (draw + fling == 0)
            return;
        for (ItemEntity drop : event.getDrops()) {
            Vec3 to = p.position().add(0, 0.5, 0).subtract(drop.position());
            double speed = Math.min(DROP_SPEED_CAP, Math.max(0.3, to.length() * DROP_SPEED_PER_BLOCK));
            Vec3 dir = to.lengthSqr() < 1e-4 ? new Vec3(0, 1, 0) : to.normalize();
            drop.setDeltaMovement(draw > 0 ? dir.scale(speed) : dir.scale(-speed).add(0, 0.2, 0));
        }
    }

    /** Worn magnet: loose items near the wearer drift toward them. */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || p.tickCount % MAGNET_PERIOD != 0)
            return;
        int n = worn(p, "worn_draw");
        if (n == 0)
            return;
        for (ItemEntity item : p.level().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(n * MAGNET_RANGE_PER_LEVEL))) {
            Vec3 to = p.position().add(0, 0.5, 0).subtract(item.position());
            if (to.lengthSqr() > 0.25) {
                item.setDeltaMovement(item.getDeltaMovement().add(to.normalize().scale(MAGNET_PULL)));
                item.hurtMarked = true;
            }
        }
    }

    /** Worn repeller: a metal-bearing arrow or trident about to hit the wearer is turned aside. */
    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow) || !(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity target) || worn(target, "worn_fling") == 0 || !metal(arrow))
            return;
        event.setCanceled(true);
        arrow.setDeltaMovement(arrow.getDeltaMovement().scale(-DEFLECT));
        arrow.hurtMarked = true;
    }

    /** Metal-bearing: a trident, or an arrow whose item's material is magnetic. */
    private static boolean metal(AbstractArrow arrow) {
        if (arrow instanceof ThrownTrident)
            return true;
        return Material.find(MaterialTable.get().materials(), arrow.getPickupItemStackOrigin().getItem())
                .map(m -> m.material().separation().magnetic()).orElse(false);
    }
}
