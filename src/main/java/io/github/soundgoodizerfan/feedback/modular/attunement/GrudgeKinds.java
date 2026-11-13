// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.attunement;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.bake.MaterialPerks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * The runtime kinds of the Grudge effect. {@code grudge} (plate, lining,
 * head, blade): damage the wearer takes is added to the Charge meter of the tool in the main hand
 * ({@link ChargeKinds#add}: the same {@code CHARGE_STORED}, the same capacity), so the next armed Charge release spends
 * it. A Charge tool's own grudge part fills its own bar; with no Charge build on the held tool it does nothing.
 * {@code calm} (the mirror, same roles): damage taken bleeds the held bar instead.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class GrudgeKinds {

    /** Charge added per point of damage taken, per level of grudge. [OPEN] until played */
    private static final float CHARGE_PER_DAMAGE = 1f;
    /** Charge a level of calm bleeds per hit taken. [OPEN] until played */
    private static final float CALM_PER_LEVEL = 2f;

    private static final net.neoforged.neoforge.registries.DeferredRegister<net.neoforged.neoforge.attachment.AttachmentType<?>> ATTACHMENTS =
            net.neoforged.neoforge.registries.DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.ATTACHMENT_TYPES, Feedback.MOD_ID);
    /** The blast a grudging housing has taken and kept, as damage it will give back. */
    private static final java.util.function.Supplier<net.neoforged.neoforge.attachment.AttachmentType<Float>> KEPT = ATTACHMENTS.register("machine_grudge",
            () -> net.neoforged.neoforge.attachment.AttachmentType.builder(() -> 0f).serialize(com.mojang.serialization.Codec.FLOAT).build());

    public static void registerAttachments(net.neoforged.bus.api.IEventBus bus) {
        ATTACHMENTS.register(bus);
    }

    /** Chance per level a housing takes a blast that would break it; the most; damage it keeps per blast and the most it holds, per level. [OPEN] until played */
    private static final float TAKE_PER_LEVEL = 0.3f, TAKE_MAX = 0.9f, KEPT_PER_BLAST = 6f, KEPT_MAX_PER_LEVEL = 20f;
    /** Blocks around a grudging machine whose hostile creatures it gives its kept damage back to. */
    private static final double GRUDGE_REACH = 6.0;

    /**
     * A blast reaches machines: a housing with {@code grudge} or {@code calm} takes it (the block is not broken) with a chance per level.
     * Grudge keeps the damage and gives it back to the first hostile creature near ({@link #aura}); calm sheds it.
     */
    @SubscribeEvent
    public static void onBlast(net.neoforged.neoforge.event.level.ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level))
            return;
        var blocks = event.getAffectedBlocks();
        blocks.removeIf(pos -> {
            var be = level.getBlockEntity(pos);
            if (be == null)
                return false;
            var levels = MachineAura.levelsOf(be);
            int grudge = levels.getOrDefault("grudge", 0), calm = levels.getOrDefault("calm", 0);
            if (grudge + calm == 0 || level.random.nextFloat() >= Math.min(TAKE_MAX, TAKE_PER_LEVEL * (grudge + calm)))
                return false;
            if (grudge > 0) {
                be.setData(KEPT.get(), Math.min(KEPT_MAX_PER_LEVEL * grudge, be.getData(KEPT.get()) + KEPT_PER_BLAST));
                be.setChanged();
            }
            return true;
        });
    }

    /** A grudging machine with damage kept gives it all back to the nearest hostile creature within reach. */
    static void aura(net.minecraft.server.level.ServerLevel level, net.minecraft.world.level.block.entity.BlockEntity be, int grudge) {
        float kept = grudge > 0 ? be.getData(KEPT.get()) : 0f;
        if (kept <= 0f)
            return;
        var pos = be.getBlockPos();
        var foe = level.getNearestEntity(level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, new net.minecraft.world.phys.AABB(pos).inflate(GRUDGE_REACH)),
                net.minecraft.world.entity.ai.targeting.TargetingConditions.forNonCombat().ignoreLineOfSight(), null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (foe != null && foe.hurt(level.damageSources().generic(), kept)) {
            be.setData(KEPT.get(), 0f);
            be.setChanged();
        }
    }

    private GrudgeKinds() {
    }

    public static void register() {
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player wearer) || wearer.level().isClientSide
                || event.getNewDamage() <= 0)
            return;
        ItemStack held = wearer.getMainHandItem();
        if (TideKinds.level(held, "build") == 0)
            return;
        int grudge = TideKinds.level(held, "grudge"), calm = TideKinds.level(held, "calm");
        for (ItemStack worn : GearKinds.worn(wearer))
            for (MaterialPerks.Attunement a : MaterialPerks.attunements(worn)) {
                if (a.kind().equals("grudge"))
                    grudge += a.level();
                else if (a.kind().equals("calm"))
                    calm += a.level();
            }
        float delta = CHARGE_PER_DAMAGE * event.getNewDamage() * grudge - CALM_PER_LEVEL * calm;
        if (delta != 0)
            ChargeKinds.add(held, wearer, delta);
    }
}
