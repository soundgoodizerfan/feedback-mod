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
package io.github.soundgoodizerfan.feedback.modular;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Units;
import io.github.soundgoodizerfan.feedback.item.HandSwing;
import io.github.soundgoodizerfan.feedback.item.HandToolItem;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.process.Operation;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A tool worked by hand, as a modular item: the hand hammer and the hand knife, and any later one (a
 * rolling pin, a whisk) that is a type file ({@code family: hand}) and one registry line. Its parts decide
 * what the old fixed items could not: the most St of an ordinary blow the head can carry is baked
 * from its mass and competence ({@code FTuning.HAND_ST_PER_MU_COMPETENCE}), and the wear bar from the head's
 * material, so a stone hammer is the first one, an iron one lasts longer and a soft or light head lands less.
 *
 * <p>A hand tool is a consumable by nature (philosophy 6's accepted exception), but a modular one is worn
 * to <em>broken</em>, as every modular item is, and not destroyed: it lands nothing and can be reworked at
 * the table. A type whose {@code strike} is 0 (the knife) bakes no blow and lands nothing in a grid.
 */
public class ModularHandTool extends HandToolItem implements Modular {

    public ModularHandTool(Item.Properties properties) {
        super(properties);
    }

    /** The St of an ordinary blow ({@link HandSwing#BLOW}) this stack lands: 0 for a tool that does not strike, an empty stack or a broken one. */
    public static float blow(ItemStack stack) {
        if (!(stack.getItem() instanceof ModularHandTool) || ModularStacks.broken(stack))
            return 0f;
        ModularFigures f = ModularStacks.figures(stack);
        return f == null ? 0f : f.get(Fig.STRIKE_ST);
    }

    /** Whether this is a hand tool whose type strikes (the hammer): by what the type is, not whether this one is broken. */
    public static boolean strikes(ItemStack stack) {
        if (!(stack.getItem() instanceof ModularHandTool))
            return false;
        ModularDesign design = ModularStacks.design(stack);
        ModularType type = ModularTypes.known().get(design != null ? design.type() : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()));
        return type != null && type.strike() > 0;
    }

    /** The swing chosen by sneak-scroll ({@link HandSwing}); an ordinary blow if none was. */
    public static HandSwing swing(ItemStack stack) {
        return stack.getOrDefault(FDataComponents.HAND_SWING, HandSwing.BLOW);
    }

    /** What a swing of this stack lands: the head's blow, scaled by how hard it is swung against the ordinary one. */
    @Override
    public St getStrength(ItemStack stack) {
        return of(stack, swing(stack));
    }

    /**
     * What {@code swing} lands with this stack's head. The arm sets the blow and the head only has to carry it, so the
     * head's figure is a share of an ordinary blow, capped at all of it: a better head never beats the swing, a
     * weaker or worn one lands less of it. (Else an iron hammer would hand-forge steel, which philosophy 7's gate forbids.)
     */
    public static St of(ItemStack stack, HandSwing swing) {
        return new St(Math.min(1f, blow(stack) / FTuning.HAND_SWING_BLOW_ST.value()) * swing.strength().value());
    }

    @Override
    public Operation getOperation() {
        return Operation.BLOW;
    }

    @Override
    public SoundEvent getWorkSound() {
        return SoundEvents.ANVIL_LAND;
    }

    /** Worn out is broken, not gone: one point worse, never past the last. */
    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack remainder = stack.copy();
        remainder.setDamageValue(remainder.getDamageValue() + Math.max(0, Math.min(1, stack.getMaxDamage() - 1 - stack.getDamageValue())));
        return remainder;
    }

    @Override
    public Component getName(ItemStack stack) {
        return ModularStacks.name(stack, super.getName(stack));
    }

    /** A mighty swing outlasts nothing: when the Strength effect goes, so does it. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        ModularStacks.tick(stack, level);
        if (!level.isClientSide && entity instanceof Player player && blow(stack) > 0 && swing(stack).within(player) != swing(stack))
            stack.set(FDataComponents.HAND_SWING, swing(stack).within(player));
    }

    /** How hard it is swung is the player's own choice, so it is printed, St and all (philosophy 8). */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ModularTooltip.add(stack, tooltip);
        if (blow(stack) > 0)
            tooltip.add(Component.translatable("tooltip.feedback.hand_swing",
                    Component.translatable("tooltip.feedback.hand_swing." + swing(stack).name().toLowerCase(Locale.ROOT)),
                    Units.figure(getStrength(stack))).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ModularStacks.enchantability(stack, 1);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return false;
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return ModularStacks.cap(stack, amount);
    }
}
