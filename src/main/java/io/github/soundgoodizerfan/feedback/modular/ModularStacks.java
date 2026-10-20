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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Everything a modular item does to its own stack: make one from a design, rebake it when it
 * wears a step, and answer the hooks every item class forwards here. The item classes differ only
 * in which vanilla behaviour they inherit (an axe strips logs, a hoe tills); what they all share
 * lives in one place, so two classes cannot end up with two different rules for breaking.
 *
 * <h2>The bake writes vanilla's own components</h2>
 * {@code TOOL} (what it digs, at what speed, behind which hardness gates) and the attribute
 * modifiers (damage, swing speed, reach, armour) are set from the figures, so vanilla's combat and
 * mining read them with no further code -- Silent Gear's {@code GearDiggerTool}, taken: harvest by
 * block tag, here with the gate made of the edge's hardness.
 *
 * <h2>Broken is not destroyed</h2>
 * Tinkers' {@code tic_broken}, taken. An item one point from spent stops there
 * ({@link #cap}): its tool rules and its damage fall back to an empty hand until it is repaired,
 * and it is rebaked at each step of wear on the way down.
 */
public final class ModularStacks {

    private ModularStacks() {
    }

    private static final ResourceLocation DAMAGE_ID = ResourceLocation.withDefaultNamespace("base_attack_damage");
    private static final ResourceLocation SPEED_ID = ResourceLocation.withDefaultNamespace("base_attack_speed");
    private static final ResourceLocation REACH_ID = Feedback.id("modular_reach");

    public static ModularDesign design(ItemStack stack) {
        return stack.get(FModular.DESIGN.get());
    }

    public static ModularFigures figures(ItemStack stack) {
        return stack.get(FModular.FIGURES.get());
    }

    /** Whether the stack is spent: one point from its limit, where it stops. */
    public static boolean broken(ItemStack stack) {
        return stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1;
    }

    /** How worn it is, 0 new to 1 spent. */
    public static float worn(ItemStack stack) {
        return stack.isDamageableItem() ? stack.getDamageValue() / (float) Math.max(1, stack.getMaxDamage()) : 0f;
    }

    /** The wear step it should be baked at: the ladder, then one past it for broken. */
    public static int stepOf(ItemStack stack) {
        return broken(stack) ? ModularTuning.WEAR_STEPS.length + 1 : ModularBake.wearStep(worn(stack));
    }

    /** The item registered for {@code type}, as a stack with no design yet. */
    public static Optional<ItemStack> blank(ResourceLocation type) {
        return BuiltInRegistries.ITEM.getOptional(type).map(ItemStack::new);
    }

    /** The design a type's own defaults make: what an item handed out with no design becomes. */
    public static Optional<ModularDesign> starter(ResourceLocation id, ModularType type, Map<ResourceLocation, Material> materials) {
        List<Part> parts = new ArrayList<>();
        Map<String, Integer> counts = type.canonicalCounts();
        for (String role : type.order()) {
            Integer count = counts.get(role);
            ResourceLocation material = type.defaults().get(role);
            if (count == null)
                continue;
            if (material == null || !materials.containsKey(material))
                return Optional.empty();
            parts.add(new Part(role, material, type.roles().get(role).stockForms().get(0), count));
        }
        return Optional.of(new ModularDesign(id, parts, Edge.STANDARD));
    }

    /**
     * Set {@code design} on {@code stack} and bake it. Returns false, leaving the stack as it was,
     * if the table does not know a part's material or the type.
     */
    public static boolean assemble(ItemStack stack, ModularDesign design, Map<ResourceLocation, Material> materials,
                                   Map<ResourceLocation, ModularType> types) {
        ModularType type = types.get(design.type());
        if (type == null)
            return false;
        int step = stepOf(stack);
        Optional<ModularFigures> baked = ModularBake.bake(design, type, materials, step, stack.get(FDataComponents.PLATING.get()));
        if (baked.isEmpty())
            return false;
        stack.set(FModular.DESIGN.get(), design);
        stack.set(FModular.FIGURES.get(), baked.get());
        stack.set(DataComponents.MAX_DAMAGE, (int) baked.get().get(Fig.DURABILITY));
        stack.set(DataComponents.DAMAGE, Math.min(stack.getDamageValue(), stack.getMaxDamage() - 1));
        write(stack, design, type, baked.get(), broken(stack));
        return true;
    }

    /** Bake again whatever the wear step: its coat changed. */
    public static void rebake(ItemStack stack) {
        ModularDesign design = design(stack);
        if (design != null)
            assemble(stack, design, MaterialTable.get().materials(), ModularTypes.get().types());
    }

    /** Whether the stack is a modular item whose wearing part is a metal: it conducts, so it can be plated. */
    public static boolean platable(ItemStack stack) {
        ModularDesign d = design(stack);
        ModularType type = d == null ? null : ModularTypes.known().get(d.type());
        Part wear = type == null ? null : d.part(type.wearRole());
        Material m = wear == null ? null : MaterialTable.get().materials().get(wear.material());
        return m != null && m.kind().filter(k -> k == Material.Kind.METAL).isPresent();
    }

    /** Rebake at the stack's present wear if the step changed, or if it has no figures yet. */
    public static void refresh(ItemStack stack) {
        ModularDesign design = design(stack);
        ModularFigures now = figures(stack);
        if (design == null || (now != null && now.step() == stepOf(stack)))
            return;
        assemble(stack, design, MaterialTable.get().materials(), ModularTypes.get().types());
    }

    /** What a modular item does each tick in a player's pack: become something if it is nothing, and wear a step. */
    public static void tick(ItemStack stack, Level level) {
        if (level.isClientSide)
            return;
        if (design(stack) == null) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            ModularType type = ModularTypes.get().types().get(id);
            if (type != null)
                starter(id, type, MaterialTable.get().materials()).ifPresent(d ->
                        assemble(stack, d, MaterialTable.get().materials(), ModularTypes.get().types()));
            return;
        }
        refresh(stack);
    }

    /** Vanilla's components, from the figures. A broken item gets none of them: it is an empty hand. */
    private static void write(ItemStack stack, ModularDesign design, ModularType type, ModularFigures f, boolean broken) {
        ItemAttributeModifiers.Builder attrs = ItemAttributeModifiers.builder();
        EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(type.slot().equipment());
        switch (type.family()) {
            case DIG, BLADE, MACE, SPEAR -> {
                float damage = broken ? 1f : f.get(Fig.DAMAGE);
                float speed = broken ? 4f : f.get(Fig.ATTACK_SPEED);
                attrs.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(DAMAGE_ID, damage - 1, AttributeModifier.Operation.ADD_VALUE), group);
                attrs.add(Attributes.ATTACK_SPEED, new AttributeModifier(SPEED_ID, speed - 4, AttributeModifier.Operation.ADD_VALUE), group);
                if (!broken && f.get(Fig.REACH) > 0)
                    attrs.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(REACH_ID, f.get(Fig.REACH), AttributeModifier.Operation.ADD_VALUE), group);
            }
            case ARMOUR -> {
                ResourceLocation id = Feedback.id("modular_armour_" + type.slot().getSerializedName());
                if (!broken) {
                    attrs.add(Attributes.ARMOR, new AttributeModifier(id, f.get(Fig.ARMOUR), AttributeModifier.Operation.ADD_VALUE), group);
                    attrs.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id, f.get(Fig.ARMOUR_TOUGHNESS), AttributeModifier.Operation.ADD_VALUE), group);
                    attrs.add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(id, f.get(Fig.KNOCKBACK_RESISTANCE), AttributeModifier.Operation.ADD_VALUE), group);
                    attrs.add(Attributes.MOVEMENT_SPEED, new AttributeModifier(id, -f.get(Fig.SLOW), AttributeModifier.Operation.ADD_MULTIPLIED_BASE), group);
                }
                Part plate = design.part(type.wearRole());
                if (plate != null)
                    stack.set(DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(MaterialColors.of(plate.material()), false));
            }
            default -> { }
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, attrs.build());
        Tool tool = broken ? null : tool(type, f);
        if (tool == null)
            stack.remove(DataComponents.TOOL);
        else
            stack.set(DataComponents.TOOL, tool);
    }

    /** What it digs: the class gates its edge fails, then the tags it mines at its figure. */
    private static Tool tool(ModularType type, ModularFigures f) {
        if (type.family() == ModularType.Family.BLADE)
            return net.minecraft.world.item.SwordItem.createToolProperties();
        if (type.family() == ModularType.Family.SHEARS)
            return net.minecraft.world.item.ShearsItem.createToolProperties();
        if (type.mines().isEmpty())
            return null;
        List<Tool.Rule> rules = new ArrayList<>();
        float hardness = f.get(Fig.HARDNESS);
        if (hardness < ModularTuning.DIAMOND_CLASS_HARDNESS)
            rules.add(Tool.Rule.deniesDrops(blocks("minecraft:needs_diamond_tool")));
        if (hardness < ModularTuning.IRON_CLASS_HARDNESS)
            rules.add(Tool.Rule.deniesDrops(blocks("minecraft:needs_iron_tool")));
        if (hardness < ModularTuning.STONE_CLASS_HARDNESS)
            rules.add(Tool.Rule.deniesDrops(blocks("minecraft:needs_stone_tool")));
        for (ModularType.Mine m : type.mines())
            rules.add(Tool.Rule.minesAndDrops(TagKey.create(Registries.BLOCK, m.tag()), f.get(Fig.DIG_SPEED) * m.factor()));
        return new Tool(rules, 1f, 1);
    }

    private static TagKey<Block> blocks(String id) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.parse(id));
    }

    // --- hooks the item classes forward --------------------------------------------------------

    /** Never lets a hit spend the last point: broken, not destroyed. */
    public static int cap(ItemStack stack, int amount) {
        return Math.max(0, Math.min(amount, stack.getMaxDamage() - 1 - stack.getDamageValue()));
    }

    /**
     * What breaking a block costs beyond the point vanilla charges, and why: an edge on something
     * harder than it can take chips, and a head heavier than its joint carries works loose. Force
     * that cannot go into the work goes into the tool (philosophy, wear rule), and each reason is
     * its own, so a worn tool says what was done to it.
     */
    public static int misuse(ItemStack stack, Level level, BlockState state, net.minecraft.core.BlockPos pos) {
        ModularFigures f = figures(stack);
        ModularDesign d = design(stack);
        if (f == null || d == null)
            return 0;
        int extra = 0;
        float strain = state.getDestroySpeed(level, pos) * ModularTuning.CHIP_STRAIN_PER_HARDNESS * d.edge().chip();
        float toughness = Math.max(0.01f, f.get(Fig.EDGE_TOUGHNESS));
        if (strain > toughness * ModularTuning.CHIP_SLACK)
            extra += Math.min(ModularTuning.CHIP_MAX, (int) (strain / (toughness * ModularTuning.CHIP_SLACK)));
        if (f.get(Fig.JOINT) > 1 && level.random.nextFloat() < f.get(Fig.JOINT) - 1)
            extra++;
        return extra;
    }

    /** A tool used wet gives up a point to rust now and then, by how little its metal and coat resist water. */
    private static int rust(ItemStack stack, LivingEntity miner) {
        ModularFigures f = figures(stack);
        return f != null && miner.isInWaterOrRain()
                && miner.getRandom().nextFloat() < ModularTuning.CORRODE_CHANCE * (1f - f.get(Fig.CORROSION)) ? 1 : 0;
    }

    /** The extra wear to charge for a block mined. */
    public static void mined(ItemStack stack, Level level, BlockState state, net.minecraft.core.BlockPos pos, LivingEntity miner) {
        if (level.isClientSide || state.getDestroySpeed(level, pos) == 0f)
            return;
        int extra = misuse(stack, level, state, pos) + rust(stack, miner);
        if (extra > 0)
            stack.hurtAndBreak(extra, miner, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
    }

    /** Whether the stack has an edge to grind: a melee head or a blade. */
    public static boolean edged(ItemStack stack) {
        ModularDesign d = design(stack);
        ModularType type = d == null ? null : ModularTypes.known().get(d.type());
        return type != null && switch (type.family()) {
            case DIG, BLADE, MACE, SPEAR, SHEARS -> true;
            default -> false;
        };
    }

    /**
     * Grind the edge one choice on and bake again, at a grindstone only ({@link ModularEvents}): it
     * costs {@link ModularTuning#GRIND_COST} points, never the last one. Returns the new edge, or
     * null if the stack has none.
     */
    public static Edge grind(ItemStack stack, LivingEntity grinder) {
        if (!edged(stack))
            return null;
        ModularDesign d = design(stack).withEdge(design(stack).edge().step(1));
        if (!assemble(stack, d, MaterialTable.get().materials(), ModularTypes.get().types()))
            return null;
        stack.hurtAndBreak(cap(stack, ModularTuning.GRIND_COST), grinder, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        return d.edge();
    }

    /** A bow's or crossbow's launch speed, scaled by what its limbs and string make of it. */
    public static float launch(ItemStack weapon, float velocity) {
        ModularFigures f = figures(weapon);
        return f == null || f.get(Fig.VELOCITY) <= 0 || broken(weapon) ? velocity : velocity * f.get(Fig.VELOCITY);
    }

    /** Limbs stronger than the string holds strain it a little each shot: misuse, and the cost says which. */
    public static void strain(ItemStack weapon, LivingEntity shooter, net.minecraft.world.InteractionHand hand) {
        ModularFigures f = figures(weapon);
        if (f != null && f.get(Fig.JOINT) > 1 && shooter.getRandom().nextFloat() < f.get(Fig.JOINT) - 1)
            weapon.hurtAndBreak(1, shooter, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
    }

    /** Ticks to draw: the figure, or vanilla's twenty for an item with none. */
    public static float drawTicks(ItemStack weapon) {
        ModularFigures f = figures(weapon);
        return f == null || f.get(Fig.DRAW) <= 0 ? ModularTuning.DRAW_REFERENCE_TICKS : f.get(Fig.DRAW);
    }

    public static int enchantability(ItemStack stack, int fallback) {
        ModularFigures f = figures(stack);
        return f == null ? fallback : (int) f.get(Fig.ENCHANTABILITY);
    }

    /** "Iron Pickaxe": the main part's material and the type, as vanilla named its own. */
    public static Component name(ItemStack stack, Component typeName) {
        ModularDesign d = design(stack);
        if (d == null)
            return typeName;
        ModularType type = ModularTypes.known().get(d.type());
        Part main = d.part(type != null ? type.primary() : d.parts().get(0).role());
        if (main == null)
            return typeName;
        Component material = Component.translatable("material." + main.material().getNamespace() + "." + main.material().getPath());
        for (Part p : d.parts()) {
            ModularType.Role r = type == null ? null : type.roles().get(p.role());
            if (r != null && r.set())
                return Component.translatable("item.feedback.modular_name." + r.name(),
                        Component.translatable("material." + p.material().getNamespace() + "." + p.material().getPath()), material, typeName);
        }
        return Component.translatable("item.feedback.modular_name", material, typeName);
    }

    public static Item.Properties properties() {
        return new Item.Properties().durability(250);
    }
}
