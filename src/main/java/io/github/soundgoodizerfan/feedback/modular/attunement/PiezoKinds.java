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
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.capacitor.LeydenJarBlockEntity;
import io.github.soundgoodizerfan.feedback.core.hazard.Shock;
import io.github.soundgoodizerfan.feedback.modular.MaterialPerks;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The runtime kinds of the Piezo effect ({@code docs/feedback_enchant_roster.md} 13). A poled part is
 * piezoelectric. {@code pulse} on a head or blade: striking a Leyden jar with it pushes a small charge in,
 * by hand and at a tiny rate (manual production stays possible), and no further than {@link #JAR_CAP_VOLTS}.
 * A blow on anything else makes a pulse with nowhere to go and is lost. {@code sense} (the mirror): the part
 * reports how hard a landed blow was instead of making power, in words -- a sensor for the player's own use that
 * controls nothing and needs no wiring yet.
 * Armour face (roster 6b), on a worn plate: {@code arc} -- the blow a conductive plate takes arcs onto whatever
 * struck it through {@link Shock}, volts rising with the hit (the wearer is never shocked; an insulating plate
 * arcs nothing); {@code soak} (the mirror) -- the plate takes the spike of a big hit and wears for it.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class PiezoKinds {

    /** Charge (amp-ticks) one strike puts in, per level. A jar's volts are charge over its capacitance. [OPEN] until played */
    private static final float PULSE_PER_LEVEL = 0.5f;
    /** The most a hand can raise a jar to, volts. [OPEN] until played */
    private static final float JAR_CAP_VOLTS = 5f;
    /** Damage below and above which a landed blow reads as glancing or heavy. [OPEN] until played */
    private static final float GLANCING = 3f, HEAVY = 8f;

    /** Volts an arc carries per point of damage taken, per level; Shock turns the volts into a band. [OPEN] until played */
    private static final float ARC_VOLTS_PER_DAMAGE = 1f;
    /** Damage over which a soak plate cuts the blow, the share of the excess cut per level, and its ceiling. [OPEN] until played */
    private static final float SOAK_ABOVE = 6f, SOAK_CUT_PER_LEVEL = 0.25f, SOAK_CUT_MAX = 0.75f;

    private PiezoKinds() {
    }

    public static void register() {
        AttunementKinds.onStrike("sense", (event, a) -> {
            if (!(event.getSource().getEntity() instanceof Player wielder) || wielder.level().isClientSide)
                return;
            float damage = event.getAmount();
            wielder.displayClientMessage(Component.translatable(damage < GLANCING ? "feedback.piezo.glancing"
                    : damage < HEAVY ? "feedback.piezo.solid" : "feedback.piezo.heavy"), true);
        });
    }

    @SubscribeEvent
    public static void onClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START || event.getLevel().isClientSide())
            return;
        ItemStack stack = event.getItemStack();
        int level = TideKinds.level(stack, "pulse");
        if (level > 0 && event.getLevel().getBlockEntity(event.getPos()) instanceof LeydenJarBlockEntity jar)
            jar.inject(PULSE_PER_LEVEL * level, JAR_CAP_VOLTS);
    }

    @SubscribeEvent
    public static void onWornHit(LivingIncomingDamageEvent event) {
        LivingEntity wearer = event.getEntity();
        if (wearer.level().isClientSide || event.getAmount() <= 0f || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))
            return;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor())
                continue;
            ItemStack worn = wearer.getItemBySlot(slot);
            if (worn.isEmpty())
                continue;
            for (MaterialPerks.Attunement a : MaterialPerks.attunements(worn)) {
                if (a.kind().equals("arc")) {
                    if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker != wearer
                            && a.material().electrical().conductivity().isPresent())
                        Shock.apply(attacker, event.getAmount() * a.level() * ARC_VOLTS_PER_DAMAGE);
                } else if (a.kind().equals("soak") && event.getAmount() > SOAK_ABOVE) {
                    float cut = (event.getAmount() - SOAK_ABOVE) * Math.min(SOAK_CUT_MAX, SOAK_CUT_PER_LEVEL * a.level());
                    event.setAmount(event.getAmount() - cut);
                    // Worn only by the spike it took, a point per started 2 absorbed, never to destruction.
                    worn.hurtAndBreak(ModularStacks.cap(worn, (int) Math.ceil(cut / 2f)), wearer, slot);
                }
            }
        }
    }
}
