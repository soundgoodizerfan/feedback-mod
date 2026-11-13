// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.attunement;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.capacitor.LeydenJarBlockEntity;
import io.github.soundgoodizerfan.feedback.core.hazard.Shock;
import io.github.soundgoodizerfan.feedback.modular.bake.MaterialPerks;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
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
 * The runtime kinds of the Piezo effect. A poled part is
 * piezoelectric. {@code pulse} on a head or blade: striking a Leyden jar with it pushes a small charge in,
 * by hand and at a tiny rate (manual production stays possible), and no further than {@link #JAR_CAP_SHARE} of what the jar holds.
 * A blow on anything else makes a pulse with nowhere to go and is lost. {@code sense} (the mirror): the part
 * reports how hard a landed blow was instead of making power, in words -- a sensor for the player's own use that
 * controls nothing and needs no wiring yet.
 * Armour face (roster 6b), on a worn plate: {@code arc} -- the blow a conductive plate takes arcs onto whatever
 * struck it through {@link Shock}, its band rising with the hit (the wearer is never shocked; an insulating plate
 * arcs nothing); {@code soak} (the mirror) -- the plate takes the spike of a big hit and wears for it.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class PiezoKinds {

    /** Share of a jar's capacity one strike puts in, per level. [OPEN] until played */
    private static final float PULSE_SHARE_PER_LEVEL = 0.002f;
    /** The most a hand can fill a jar to, as a share of its capacity. [OPEN] until played */
    private static final float JAR_CAP_SHARE = 0.2f;
    /** Damage below and above which a landed blow reads as glancing or heavy. [OPEN] until played */
    private static final float GLANCING = 3f, HEAVY = 8f;

    /** Points of damage taken (times level) at which an arc steps up a band: MV, HV, UHV; under the first it is LV. One packet of Eu rides it. [OPEN] until played */
    private static final float[] ARC_BAND_STEP_DAMAGE = {5f, 20f, 80f};
    /** Damage over which a soak plate cuts the blow, the share of the excess cut per level, and its ceiling. [OPEN] until played */
    private static final float SOAK_ABOVE = 6f, SOAK_CUT_PER_LEVEL = 0.25f, SOAK_CUT_MAX = 0.75f;

    /** Muzzle energy below and above which a shot reads as glancing or heavy on the gauge. [OPEN] until played */
    private static final float SHOT_GLANCING = 200f, SHOT_HEAVY = 1500f;

    private PiezoKinds() {
    }

    /**
     * A shot of a gun with a piezo bolt or slide: {@code cycle_gauge} reports the shot's muzzle energy in words, and in
     * a figure when the shooter carries an instrument for it.
     */
    public static void fired(Player shooter, ItemStack gun, io.github.soundgoodizerfan.feedback.weapon.GunFigures f) {
        int gauge = 0;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(gun))
            if (a.kind().equals("cycle_gauge"))
                gauge += a.level();
        if (gauge > 0) {
            float e = f.muzzleEnergy();
            Component word = Component.translatable(e < SHOT_GLANCING ? "feedback.piezo.glancing"
                    : e < SHOT_HEAVY ? "feedback.piezo.solid" : "feedback.piezo.heavy");
            var instrument = io.github.soundgoodizerfan.feedback.instrument.Instruments.reading(shooter,
                    io.github.soundgoodizerfan.feedback.instrument.Quantity.FIGURE);
            shooter.displayClientMessage(instrument == null ? word : Component.empty().append(word).append(" (" + io.github.soundgoodizerfan.feedback.instrument.Instruments.format(
                    e, instrument.resolution(io.github.soundgoodizerfan.feedback.instrument.Quantity.FIGURE)) + ")"), true);
        }
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
            jar.inject(PULSE_SHARE_PER_LEVEL * level * jar.capacity(), JAR_CAP_SHARE);
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
                    {
                        float hit = event.getAmount() * a.level();
                        io.github.soundgoodizerfan.feedback.core.unit.VoltageBand arc = io.github.soundgoodizerfan.feedback.core.unit.VoltageBand.LV;
                        for (int i = 0; i < ARC_BAND_STEP_DAMAGE.length; i++)
                            if (hit >= ARC_BAND_STEP_DAMAGE[i])
                                arc = io.github.soundgoodizerfan.feedback.core.unit.VoltageBand.values()[i + 1];
                        Shock.apply(attacker, arc);
                    }
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
