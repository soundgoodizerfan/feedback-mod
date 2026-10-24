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
package io.github.soundgoodizerfan.feedback.modular.attunement;

import io.github.soundgoodizerfan.feedback.modular.AttunementKinds;
import io.github.soundgoodizerfan.feedback.modular.MaterialPerks;
import io.github.soundgoodizerfan.feedback.modular.StrikeSource;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.hazard.Shock;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The runtime kinds of the Charge effect ({@code docs/feedback_enchant_roster.md} 16). {@code build} on a head or
 * blade: each landed blow charges the part ({@link FDataComponents#CHARGE_STORED} on the stack). Sneak-use arms a
 * release, and the next landed blow spends the whole charge as extra damage, so a release made early is weaker.
 * The charge is held to {@link #SAFE_PER_LEVEL} a level; past it every further blow arcs through the wielder
 * ({@link Shock}) unless the handle insulates, the same rule Conduct's bolt reads. Worn {@code capacitor} (a plate or lining) adds its level, times its material's ampacity, to the bar's capacity. {@code bleed} (the mirror, on a
 * handle or grip): a ground strap, it drains the charge a little each blow and the wielder is never arced.
 * Charge comes from blows only; a lightning pickup is left {@code [OPEN]}. The pole dropping partway after a release
 * is not built (the table's attunement is not rewritten in play).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class ChargeKinds {

    /** Charge one landed blow builds, per level of build. [OPEN] until played */
    private static final float BUILD_PER_BLOW = 1f;
    /** Charge a level holds safely; the most is twice that. [OPEN] until played */
    private static final float SAFE_PER_LEVEL = 8f;
    /** Extra damage a release makes per point of charge. [OPEN] until played */
    private static final float DAMAGE_PER_CHARGE = 0.5f;
    /** Volts the arc of an overcharged blow carries (MV: it hurts). [OPEN] until played */
    private static final float ARC_VOLTS = 240f;
    /** Charge a level of bleed drains per blow. [OPEN] until played */
    private static final float BLEED_PER_LEVEL = 2f;

    /** Charge one bolt costs when the tool also draws (Conduct): a release then calls lightning, rain or not. [OPEN] until played */
    private static final float BOLT_COST = 8f;

    /** Set on a player who has armed a release; cleared by the blow that spends it. */
    private static final String ARMED = "feedback_charge_armed";

    private ChargeKinds() {
    }

    static float stored(ItemStack stack) {
        return stack.getOrDefault(FDataComponents.CHARGE_STORED.get(), 0f);
    }

    /** How full a Charge tool is, 0..1 (the safe amount is half); negative when it has no build. For the HUD bar. */
    public static float fill(ItemStack stack, LivingEntity wearer) {
        float level = capacity(stack, wearer);
        return TideKinds.level(stack, "build") == 0 ? -1f : Math.min(1f, stored(stack) / (2 * SAFE_PER_LEVEL * level));
    }

    /** Levels the bar holds: the tool's build plus the worn capacitor levels, each scaled by its material's ampacity (copper 1; no ampacity adds nothing). */
    static float capacity(ItemStack weapon, LivingEntity wearer) {
        float n = TideKinds.level(weapon, "build");
        if (n == 0)
            return 0;
        for (ItemStack worn : wearer.getArmorSlots())
            for (MaterialPerks.Attunement a : MaterialPerks.attunements(worn))
                if (a.kind().equals("capacitor"))
                    n += a.level() * a.material().ampacity().orElse(0f);
        return n;
    }

    /** Add (or, negative, take) charge on a Charge tool's meter, held to its capacity; does nothing for a tool with no build. For Grudge. */
    public static void add(ItemStack weapon, LivingEntity wearer, float amount) {
        float capacity = capacity(weapon, wearer);
        if (capacity > 0)
            weapon.set(FDataComponents.CHARGE_STORED.get(), Math.max(0f, Math.min(2 * SAFE_PER_LEVEL * capacity, stored(weapon) + amount)));
    }

    public static void register() {
        AttunementKinds.onStrike("build", (event, a) -> {
            if (!(event.getSource().getEntity() instanceof Player wielder) || wielder.level().isClientSide)
                return;
            ItemStack weapon = StrikeSource.weapon(wielder, event.getSource());
            float capacity = capacity(weapon, wielder);
            float held = stored(weapon), limit = 2 * SAFE_PER_LEVEL * capacity;
            if (held > SAFE_PER_LEVEL * capacity && TideKinds.level(weapon, "bleed") == 0
                    && !ConductKinds.insulated(weapon))
                Shock.apply(wielder, ARC_VOLTS);
            weapon.set(FDataComponents.CHARGE_STORED.get(), Math.min(limit, held + BUILD_PER_BLOW * a.level()));
        });
        AttunementKinds.onStrike("bleed", (event, a) -> {
            if (!(event.getSource().getEntity() instanceof Player wielder) || wielder.level().isClientSide)
                return;
            ItemStack weapon = StrikeSource.weapon(wielder, event.getSource());
            float held = stored(weapon);
            if (held > 0)
                weapon.set(FDataComponents.CHARGE_STORED.get(), Math.max(0f, held - BLEED_PER_LEVEL * a.level()));
        });
    }

    /** Sneak-use on a charged tool arms the release. */
    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (player.level().isClientSide || !player.isShiftKeyDown() || TideKinds.level(stack, "build") == 0 || stored(stack) <= 0)
            return;
        player.getPersistentData().putBoolean(ARMED, true);
        player.displayClientMessage(Component.translatable("feedback.charge.armed"), true);
    }

    /** An armed release spends the whole charge in the next landed blow. */
    @SubscribeEvent
    public static void onHit(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player wielder) || wielder.level().isClientSide
                || !wielder.getPersistentData().getBoolean(ARMED))
            return;
        ItemStack weapon = StrikeSource.weapon(wielder, event.getSource());
        float held = stored(weapon);
        wielder.getPersistentData().remove(ARMED);
        if (held <= 0 || TideKinds.level(weapon, "build") == 0)
            return;
        if (held >= BOLT_COST && TideKinds.level(weapon, "draw") > 0) {
            ConductKinds.strike(wielder, weapon, event.getEntity().position());
            weapon.set(FDataComponents.CHARGE_STORED.get(), held - BOLT_COST);
            return;
        }
        event.setAmount(event.getAmount() + held * DAMAGE_PER_CHARGE);
        weapon.set(FDataComponents.CHARGE_STORED.get(), 0f);
    }
}
