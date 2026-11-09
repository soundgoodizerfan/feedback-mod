// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Grip;
import io.github.soundgoodizerfan.feedback.core.hazard.Insulator;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import org.jetbrains.annotations.Nullable;

import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

/**
 * A glove worn in the Curios {@code hands} slot. The body (leather, cloth) is the glove: it keeps heat and a weak
 * shock off the hand as far as its own file stands it ({@link Grip}), the way a tongs' grip does, and an optional
 * lining sits against the hand. A metacarpal plate (role {@code M}, hard stock) laid over the back of the hand
 * is what makes it a fighting glove: it adds {@link Fig#FIST} to a barehanded swing ({@code FistStances}), armour
 * while worn, and a barehanded parry ({@code CombatGear.granting}); a heavy plate costs swing speed
 * ({@link Fig#SWING_DRAG}). The plate never changes what the body keeps off the hand.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public class ModularGlove extends ModularItem implements Insulator {

    /** The body role's letter in the type file. */
    private static final String BODY = "B";
    /** The metacarpal plate role's letter in the type file. */
    public static final String PLATE = "M";

    public ModularGlove(Properties properties) {
        super(properties);
    }

    /** Damage {@code stack} adds to a barehanded swing: 0 for anything else, an unplated glove or a broken one. */
    public static float fist(ItemStack stack) {
        if (!plated(stack))
            return 0f;
        ModularFigures f = ModularStacks.figures(stack);
        return f == null ? 0f : f.get(Fig.FIST);
    }

    /** The numbers the plate's variant names in the type file (claws: the bleed a barehanded hit opens); empty for a standard plate. */
    public static java.util.Map<String, Float> plateParams(ItemStack stack) {
        ModularDesign d = ModularStacks.design(stack);
        ModularType type = d == null ? null : ModularTypes.known().get(d.type());
        ModularDesign.Part plate = d == null ? null : d.part(PLATE);
        if (type == null || plate == null)
            return java.util.Map.of();
        return type.roles().get(PLATE).variant(plate).map(ModularType.Variant::params).orElse(java.util.Map.of());
    }

    /** Whether {@code stack} is a plated glove whose plate is the claw variant: its barehanded hits open a bleed. */
    public static boolean claws(ItemStack stack) {
        return plated(stack) && plateParams(stack).containsKey("bleed_damage");
    }

    /** Whether {@code stack} is a whole glove carrying a metacarpal plate. */
    public static boolean plated(ItemStack stack) {
        if (!(stack.getItem() instanceof ModularGlove) || ModularStacks.broken(stack))
            return false;
        ModularDesign d = ModularStacks.design(stack);
        return d != null && d.part(PLATE) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltip,
            net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (plated(stack))
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.feedback.glove.parry")
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
    }

    /** The plate's armour, and the swing speed its weight takes, while the glove is worn. */
    @SubscribeEvent
    public static void onCurioAttributes(CurioAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!plated(stack))
            return;
        ModularFigures f = ModularStacks.figures(stack);
        if (f == null)
            return;
        ResourceLocation id = Feedback.id("glove_" + event.getSlotContext().identifier() + "_" + event.getSlotContext().index());
        event.addModifier(Attributes.ARMOR, new AttributeModifier(id, f.get(Fig.ARMOUR), AttributeModifier.Operation.ADD_VALUE));
        event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(id, -f.get(Fig.SWING_DRAG), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    @Nullable
    private static ResourceLocation material(ItemStack stack, String role) {
        ModularDesign d = ModularStacks.design(stack);
        ModularDesign.Part p = d == null ? null : d.part(role);
        return p == null ? null : p.material();
    }

    /**
     * Unlined, the body alone is the glove (cloth holds warm work, bare leather less). A lining that insulates
     * sits against the hand and cuts what reaches it, so the glove is the better of the body alone and the lining at the
     * smaller exposure: never worse than the same body unlined, and a leather body over wool protects too.
     * shortcut: the body is not asked to stand the piece itself (a leather skin would char before the wool behind it
     * mattered); add a ceiling when a thick-bodied material needs it.
     */
    @Override
    public float handlesUpToTu(ItemStack stack, Level level) {
        ResourceLocation body = material(stack, BODY);
        if (body == null || ModularStacks.broken(stack))
            return 0f;
        float alone = Grip.safeUpTo(level, body, FTuning.GLOVE_EXPOSURE);
        ResourceLocation lining = material(stack, "L");
        if (lining == null || Grip.material(level, lining).filter(Grip::insulates).isEmpty())
            return alone;
        return Math.max(alone, Grip.safeUpTo(level, lining, FTuning.GLOVE_EXPOSURE * FTuning.GLOVE_LINING_SHARE));
    }

    /** Either layer stops a wire: the lining between the hand and the body counts as much as a cloth body. */
    @Override
    public float shockProofVolts(ItemStack stack, Level level) {
        ResourceLocation body = material(stack, BODY);
        if (body == null || ModularStacks.broken(stack))
            return 0f;
        ResourceLocation lining = material(stack, "L");
        boolean insulates = Grip.material(level, body).filter(Grip::insulates).isPresent()
                || lining != null && Grip.material(level, lining).filter(Grip::insulates).isPresent();
        return insulates ? FTuning.GLOVE_SHOCK_PROOF_VOLTS : 0f;
    }
}
