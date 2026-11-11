// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import io.github.soundgoodizerfan.feedback.modular.attunement.WardKinds;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.item.Meal;
import io.github.soundgoodizerfan.feedback.process.Cooking;
import io.github.soundgoodizerfan.feedback.process.Dip;
import io.github.soundgoodizerfan.feedback.process.Plating;
import io.github.soundgoodizerfan.feedback.process.DishFigures;
import io.github.soundgoodizerfan.feedback.process.FoodState;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.Measures;
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
        ToolTableAssembly.keepCredit(stack, design);
        // A gun's wear is its bore's (GunCondition), not a bar of uses; a head's is TOOL_CONDITION, worn by the machine.
        if (type.family() != ModularType.Family.GUN && type.family() != ModularType.Family.CHARGE
                && type.family() != ModularType.Family.HEAD && type.family() != ModularType.Family.DISH
                && type.family() != ModularType.Family.VESSEL && type.family() != ModularType.Family.CELL
                && type.family() != ModularType.Family.MACHINE && type.family() != ModularType.Family.BELT
                && type.family() != ModularType.Family.ENGINE && type.family() != ModularType.Family.GEARBOX
                && type.family() != ModularType.Family.PROPELLER && type.family() != ModularType.Family.SUIT_MODULE
                && type.family() != ModularType.Family.PRESSURE && type.family() != ModularType.Family.BLOCK) {
            stack.set(DataComponents.MAX_DAMAGE, (int) baked.get().get(Fig.DURABILITY));
            stack.set(DataComponents.DAMAGE, Math.min(stack.getDamageValue(), stack.getMaxDamage() - 1));
        }
        write(stack, design, type, baked.get(), broken(stack));
        if (type.family() == ModularType.Family.VESSEL)
            ModularBake.xpStore(design, type, materials, stack.get(FDataComponents.XP_STORE.get()))
                    .ifPresent(s -> stack.set(FDataComponents.XP_STORE.get(), s));
        if (type.family() == ModularType.Family.CELL)
            ModularBake.cell(design, type, materials).ifPresent(c -> batteryState(stack, c));
        if (type.family() == ModularType.Family.MACHINE)
            ModularBake.machine(design, type, materials).ifPresent(m -> machineState(stack, m));
        if (type.family() == ModularType.Family.BLOCK)
            BlockFigures.of(design, type, materials).ifPresent(t -> stack.set(FDataComponents.BLOCK_FIGURES.get(), t));
        if (type.family() == ModularType.Family.PRESSURE)
            ModularBake.tank(design, type, materials).ifPresent(v -> {
                net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(FDataComponents.PRESSURE_VESSEL.get(), new net.minecraft.nbt.CompoundTag()).copy();
                tag.putFloat("Volume", v.volumeMb());
                tag.putFloat("Danger", v.dangerPu());
                tag.putFloat("Critical", v.criticalPu());
                tag.putFloat("Mass", v.mass());
                tag.putFloat("Relief", v.reliefMb());
                stack.set(FDataComponents.PRESSURE_VESSEL.get(), tag);
            });
        if (type.family() == ModularType.Family.ENGINE)
            ModularBake.engine(design, type, materials).ifPresent(e -> stack.set(io.github.soundgoodizerfan.feedback.registry.FVehicles.ENGINE_DESIGN.get(), e));
        if (type.family() == ModularType.Family.PROPELLER)
            ModularBake.propeller(design, type, materials).ifPresent(p -> stack.set(io.github.soundgoodizerfan.feedback.registry.FVehicles.PROP_DESIGN.get(), p));
        if (type.family() == ModularType.Family.GEARBOX)
            ModularBake.gearbox(design, type, materials).ifPresent(t -> stack.set(io.github.soundgoodizerfan.feedback.registry.FVehicles.TRANSMISSION_DESIGN.get(), t));
        if (type.family() == ModularType.Family.BELT)
            ModularBake.belt(design, type, materials).ifPresent(b -> {
                net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(FDataComponents.BELT.get(), new net.minecraft.nbt.CompoundTag()).copy();
                b.write(tag);
                if (!tag.contains("Condition"))
                    tag.putFloat("Condition", 1f);
                stack.set(FDataComponents.BELT.get(), tag);
            });
        if (type.family() == ModularType.Family.DISH)
            DishBake.of(design, type, materials).ifPresent(d -> eatable(stack, d));
        return true;
    }

    /** Writes a dynamotor's figures onto its stack, keeping the condition and temperature a rebake finds already there. */
    private static void machineState(ItemStack stack, ModularBake.MachineSpec m) {
        net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(FDataComponents.DYNAMOTOR.get(), new net.minecraft.nbt.CompoundTag()).copy();
        tag.putFloat("FrameSu", m.frameSu());
        tag.putFloat("VoltsPerRpm", m.voltsPerRpm());
        tag.putFloat("RatedAmps", m.ratedAmps());
        tag.putString("Band", m.band().getSerializedName());
        tag.putFloat("Mass", m.mass());
        tag.putFloat("Leak", m.leak());
        tag.putFloat("ServiceTu", m.serviceTu());
        tag.putFloat("WindingDrop", m.windingDrop());
        tag.putFloat("CoreLoss", m.coreLoss());
        tag.putBoolean("Jacketed", m.jacketed());
        if (!tag.contains("Condition"))
            tag.putFloat("Condition", 1f);
        stack.set(FDataComponents.DYNAMOTOR.get(), tag);
    }

    /** Writes a battery's cell figures onto its stack, keeping the charge, water, acid and condition a rebake finds already there. */
    private static void batteryState(ItemStack stack, ModularBake.CellSpec c) {
        net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(FDataComponents.BATTERY.get(), new net.minecraft.nbt.CompoundTag()).copy();
        tag.putInt("Cells", c.cells());
        tag.putFloat("Capacity", c.capacity());
        tag.putFloat("VoltsFull", c.plates().voltsFull());
        tag.putFloat("VoltsLow", c.plates().voltsLow());
        tag.putFloat("VoltsEmpty", c.plates().voltsEmpty());
        tag.putFloat("ColdFactor", c.plates().coldFactor());
        tag.putFloat("ChargeEfficiency", c.plates().chargeEfficiency());
        tag.putBoolean("Gassing", c.plates().gassing());
        tag.putString("Electrolyte", c.plates().electrolyte().toString());
        tag.putString("CaseBand", c.caseBand().getSerializedName());
        if (!tag.contains("Condition"))
            tag.putFloat("Condition", 1f);
        stack.set(FDataComponents.BATTERY.get(), tag);
    }

    /**
     * A pie came through the heat ({@link Cooking#tick}): every food part is cooked, with the pathogen and water the heat
     * left (the share {@code after} kept of the worst part's load, the share of the baked water), and the dish is baked
     * again from them. The old state goes first, or its raw pathogen would be kept as the worse.
     */
    public static void bakeThrough(ItemStack stack, FoodState after) {
        ModularDesign design = design(stack);
        DishFigures figures = stack.get(FDataComponents.DISH_FIGURES.get());
        if (design == null || figures == null)
            return;
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        float load = 0f;
        for (Part p : design.parts()) {
            Material m = materials.get(p.material());
            if (m != null && m.food().isPresent())
                load = Math.max(load, DishBake.stateOf(p, m.food().get()).pathogen());
        }
        float kept = load > 0f ? Math.min(1f, after.pathogen() / load) : 1f;
        float dry = Math.min(1f, after.moisture() / Math.max(0.01f, figures.moisture()));
        ModularDesign cooked = design;
        for (Part p : design.parts()) {
            Material m = materials.get(p.material());
            if (m == null || m.food().isEmpty())
                continue;
            FoodState s = DishBake.stateOf(p, m.food().get());
            Map<String, Float> state = new java.util.HashMap<>(p.measures().state());
            state.putAll(DishBake.stateOf(new FoodState(1f, s.moisture() * dry, s.pathogen() * kept, s.toxin(), Math.max(s.charred(), after.charred()),
                    s.rot(), 0L, s.envTu(), s.smoke(), s.salt())));
            cooked = cooked.withPart(new Part(p.role(), p.material(), p.form(), p.count(), p.kind(), p.variant(), p.flaw(), p.measures().withState(state)));
        }
        stack.remove(FDataComponents.FOOD_STATE.get());
        assemble(stack, cooked, materials, ModularTypes.get().types());
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
                ToolDesigns.starter(id, ModularTypes.get().types(), MaterialTable.get().materials()).ifPresent(d ->
                        assemble(stack, d, MaterialTable.get().materials(), ModularTypes.get().types()));
            return;
        }
        refresh(stack);
        // A dish nobody has read yet starts its clock the first time a pack holds it.
        DishFigures dish = stack.get(FDataComponents.DISH_FIGURES.get());
        FoodState state = stack.get(FDataComponents.FOOD_STATE.get());
        if (dish != null && state != null && state.stamp() <= 0L)
            Cooking.settle(stack, level, io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable.at(level).value());
    }

    /**
     * A dish is eaten as vanilla food (the {@code food} component) with the pot's {@code meal} so {@code BuffEvents}
     * doses its compounds, and it spoils: the baked figures and a {@code FoodState} sit on the stack and
     * {@link Cooking#apply} derives what is eaten from them. A rebake (the finish) keeps the rot, load and clock it
     * had already earned.
     */
    private static void eatable(ItemStack stack, DishBake.Dish d) {
        float seconds = (FTuning.DISH_EAT_BASE_SECONDS + FTuning.DISH_EAT_SECONDS_PER_LITRE * d.litres()) * (1f - FTuning.DISH_EAT_CUT_SCALE * d.cut())
                * (MaterialPerks.of(stack).containsKey(Material.Perk.FEATHERLIGHT) ? FTuning.DISH_FEATHER_EAT_SCALE : 1f);
        // Spilling is remembered across a rebake: what was lost from the wettest part stays lost, up to what the overflow allows.
        DishFigures old = stack.get(FDataComponents.DISH_FIGURES.get());
        float room = Math.min(1f, d.overflow() / FTuning.DISH_PART_MB);
        float spilled = old == null ? 0f : Math.min(old.spilled(), room);
        DishFigures figures = new DishFigures(Math.max(0f, d.hunger() - d.wetHunger() * spilled), Math.max(0f, d.saturation() - d.wetSaturation() * spilled),
                seconds, d.state().moisture(), spilled, d.overflow(), d.seal());
        FoodState s = d.state(), was = stack.get(FDataComponents.FOOD_STATE.get());
        if (was != null)
            s = new FoodState(Math.max(s.cook(), was.cook()), s.moisture(), Math.max(s.pathogen(), was.pathogen()), Math.max(s.toxin(), was.toxin()), s.charred(),
                    Math.max(s.rot(), was.rot()), was.stamp(), was.envTu(), s.smoke(), s.salt());
        stack.set(FDataComponents.DISH_FIGURES.get(), figures);
        stack.set(FDataComponents.FOOD_STATE.get(), s);
        stack.set(FDataComponents.MEAL.get(), new Meal("dish", d.pathogen(), d.compounds(), d.fat(), d.fibre(), Map.of()));
        Cooking.apply(stack, Cooking.dishEntry(figures), s);
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
        Tool tool = broken ? null : tool(design, type, f);
        if (tool == null)
            stack.remove(DataComponents.TOOL);
        else
            stack.set(DataComponents.TOOL, tool);
    }

    /** What it digs: the class gates its edge fails, then the tags it mines at its figure. */
    private static Tool tool(ModularDesign design, ModularType type, ModularFigures f) {
        if (type.family() == ModularType.Family.BLADE)
            return net.minecraft.world.item.SwordItem.createToolProperties();
        if (type.family() == ModularType.Family.SHEARS)
            return net.minecraft.world.item.ShearsItem.createToolProperties();
        List<ModularHeads> heads = heads(design, type);
        if (heads.isEmpty())
            return null;
        // A head shaped to dig nothing (a war pick) is a weapon and no tool: it takes the hand's rules.
        Part wearing = design.part(type.wearRole());
        if (heads.size() == 1 && wearing != null
                && type.roles().get(type.wearRole()).variant(wearing).map(v -> v.params().getOrDefault("digs", 1f) <= 0f).orElse(false))
            return null;
        // Each head digs its own tags at its own speed (the better one where both dig a tag). What
        // drops is gated by the head that digs stone, the only blocks the classes name.
        Map<ResourceLocation, Float> speed = new java.util.LinkedHashMap<>();
        float hardness = f.get(Fig.HARDNESS);
        float toughness = f.get(Fig.EDGE_TOUGHNESS);
        for (int i = 0; i < heads.size(); i++)
            for (ModularType.Mine m : heads.get(i).mines())
                speed.merge(m.tag(), headFigure(f, i, Fig.DIG_SPEED) * m.factor(), Math::max);
        for (int i = heads.size() - 1; i >= 0; i--)
            if (heads.get(i).mines().stream().anyMatch(m -> m.tag().getPath().equals("mineable/pickaxe"))) {
                hardness = headFigure(f, i, Fig.HARDNESS);
                toughness = headFigure(f, i, Fig.EDGE_TOUGHNESS);
            }
        List<Tool.Rule> rules = new ArrayList<>();
        if (hardness < ModularTuning.DIAMOND_CLASS_HARDNESS)
            rules.add(Tool.Rule.deniesDrops(blocks("minecraft:needs_diamond_tool")));
        // A brittle edge (stone) is hard enough for the iron class and shatters on it; only a
        // diamond-hard one gets by without the toughness.
        if (hardness < ModularTuning.IRON_CLASS_HARDNESS
                || (toughness < ModularTuning.IRON_CLASS_TOUGHNESS && hardness < ModularTuning.DIAMOND_CLASS_HARDNESS))
            rules.add(Tool.Rule.deniesDrops(blocks("minecraft:needs_iron_tool")));
        if (hardness < ModularTuning.STONE_CLASS_HARDNESS)
            rules.add(Tool.Rule.deniesDrops(blocks("minecraft:needs_stone_tool")));
        speed.forEach((tag, v) -> rules.add(Tool.Rule.minesAndDrops(TagKey.create(Registries.BLOCK, tag), v)));
        return new Tool(rules, 1f, 1);
    }

    /** The heads of a design, first head first: a single-head type's own, or what each striking part is. */
    private static List<ModularHeads> heads(ModularDesign design, ModularType type) {
        List<String> strikers = type.strikers();
        if (strikers.isEmpty())
            return type.mines().isEmpty() ? List.of() : List.of(new ModularHeads(type.mines(), type.strike(), 0, null));
        List<ModularHeads> out = new ArrayList<>();
        for (String role : strikers) {
            Part p = design.part(role);
            if (p != null)
                ModularHeads.of(ModularHeads.kindOf(p, type.roles().get(role))).ifPresent(out::add);
        }
        return out;
    }

    /** A figure of head {@code index}: the second head's has a figure of its own. */
    private static float headFigure(ModularFigures f, int index, Fig fig) {
        if (index == 0)
            return f.get(fig);
        return f.get(switch (fig) {
            case DIG_SPEED -> Fig.DIG_SPEED_OFF;
            case HARDNESS -> Fig.HARDNESS_OFF;
            default -> Fig.EDGE_TOUGHNESS_OFF;
        });
    }

    private static TagKey<Block> blocks(String id) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.parse(id));
    }

    // --- hooks the item classes forward --------------------------------------------------------

    /**
     * The volume the piece is, in mB: its baked mass over its wearing part's density (the handle or lining
     * is counted as that metal too -- close enough to size a cathode by, not a figure to show).
     */
    public static float volumeMb(ItemStack stack) {
        ModularFigures f = figures(stack);
        ModularDesign d = design(stack);
        ModularType type = d == null ? null : ModularTypes.known().get(d.type());
        Part wear = type == null ? null : d.part(type.wearRole());
        Material m = wear == null ? null : MaterialTable.get().materials().get(wear.material());
        return f == null || m == null || m.density().muPerMb() <= 0f ? 0f : f.get(Fig.MASS) / m.density().muPerMb();
    }

    /**
     * {@code part} with {@code coat} fired onto it (enamel in a kiln): a whole coat of the row's metal, replacing any
     * coat of another metal and restoring a chipped one of the same. A stack that cannot be plated comes back as it
     * went in -- the frit is spent either way.
     */
    public static ItemStack fired(ItemStack part, Dip.Coat coat) {
        if (!platable(part))
            return part;
        ItemStack out = part.copy();
        out.set(FDataComponents.PLATING.get(), new Plating(coat.metal(), coat.thickness(), Plating.Quality.SOUND));
        rebake(out);
        return out;
    }

    /**
     * A blow of {@code strain} on the piece: a coat of a brittle material (its toughness under {@link ModularBake#fracture})
     * past the strain it takes loses a share of what is left by how far past. Impact only, never time.
     */
    public static void struck(ItemStack stack, float strain) {
        Plating coat = stack.get(FDataComponents.PLATING.get());
        Material m = coat == null || coat.intact() <= 0f ? null : MaterialTable.get().materials().get(coat.metal());
        if (m == null)
            return;
        float limit = ModularBake.coatChipStrain(m);
        if (strain <= limit)
            return;
        stack.set(FDataComponents.PLATING.get(), coat.withIntact(coat.intact() - ModularTuning.COAT_CHIP_SHARE * strain / limit));
        rebake(stack);
    }

    /** The share of the heat from a source at {@code sourceTu} the piece's coat keeps out: none if the coat would melt in it. */
    public static float coatShield(ItemStack stack, float sourceTu) {
        Plating coat = stack.get(FDataComponents.PLATING.get());
        Material m = coat == null ? null : MaterialTable.get().materials().get(coat.metal());
        if (m == null || m.thermal().meltingPoint().map(t -> t.value() <= sourceTu).orElse(true))
            return 0f;
        return ModularTuning.COAT_HEAT_SHIELD * ModularBake.coatHold(coat);
    }

    /**
     * A hot dip that overran: {@code over} Qu of coat past what holds. Each {@link FTuning#HOT_DIP_FLAW_PER_QU} of it is a
     * flaw on the wearing part (the intermetallic layer grows brittle: a share of toughness and, by it, durability, as
     * forging flaws cost); past {@link FTuning#HOT_DIP_DISSOLVE_ABOVE_QU} the bath also eats the part, thinning it by
     * {@link FTuning#HOT_DIP_DISSOLVE_WORK} a dip until it is scrap. Returns what is left to hand back, rebaked.
     */
    public static ItemStack overrun(ItemStack piece, float over) {
        ModularDesign d = design(piece);
        ModularType type = d == null ? null : ModularTypes.known().get(d.type());
        Part wear = type == null ? null : d.part(type.wearRole());
        if (wear == null || over <= 0f)
            return piece;
        int flaw = Math.max(wear.flaw(), Math.min(ForgeRhythm.MAX_FLAW, (int) Math.ceil(over / FTuning.HOT_DIP_FLAW_PER_QU)));
        float work = wear.measures().work();
        if (over > FTuning.HOT_DIP_DISSOLVE_ABOVE_QU)
            work += FTuning.HOT_DIP_DISSOLVE_WORK;
        if (work >= FTuning.HOT_DIP_SCRAP_WORK) {
            Material m = MaterialTable.get().materials().get(wear.material());
            return new ItemStack(io.github.soundgoodizerfan.feedback.process.Failure.item(
                    m == null ? Material.Kind.METAL : m.kind().orElse(Material.Kind.METAL)));
        }
        Measures measured = wear.measures().withWork(work);
        piece.set(FModular.DESIGN.get(), d.withPart(new Part(wear.role(), wear.material(), wear.form(), wear.count(),
                wear.kind(), wear.variant(), flaw, measured)));
        rebake(piece);
        return piece;
    }

    /** Never lets a hit spend the last point: broken, not destroyed. */
    public static int cap(ItemStack stack, int amount) {
        return Math.max(0, Math.min(WardKinds.soak(stack, amount), stack.getMaxDamage() - 1 - stack.getDamageValue()));
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
        ModularType type = ModularTypes.known().get(d.type());
        int extra = 0;
        float strain = state.getDestroySpeed(level, pos) * ModularTuning.CHIP_STRAIN_PER_HARDNESS * d.edge().chip();
        List<ModularHeads> heads = type == null ? List.of() : heads(d, type);
        int index = 0;
        for (int i = 0; i < heads.size(); i++)
            if (heads.get(i).digs(state)) {
                index = i;
                break;
            }
        float toughness = Math.max(0.01f, headFigure(f, index, Fig.EDGE_TOUGHNESS));
        if (strain > toughness * ModularTuning.CHIP_SLACK)
            extra += Math.min(ModularTuning.CHIP_MAX, (int) (strain / (toughness * ModularTuning.CHIP_SLACK)));
        if (f.get(Fig.JOINT) > 1 && level.random.nextFloat() < f.get(Fig.JOINT) - 1)
            extra++;
        return extra;
    }

    /**
     * A tool used wet, or a plate struck while its wearer is wet ({@link ModularArmor#damageItem}), gives up a
     * point to rust now and then, by how little its metal and coat resist water. Water and misuse, never time.
     */
    public static int rust(ItemStack stack, LivingEntity miner) {
        ModularFigures f = figures(stack);
        return f != null && miner.isInWaterOrRain()
                && miner.getRandom().nextFloat() < ModularTuning.CORRODE_CHANCE * (1f - f.get(Fig.CORROSION)) ? 1 : 0;
    }

    /** The extra wear to charge for a block mined. */
    public static void mined(ItemStack stack, Level level, BlockState state, net.minecraft.core.BlockPos pos, LivingEntity miner) {
        if (level.isClientSide || state.getDestroySpeed(level, pos) == 0f)
            return;
        struck(stack, state.getDestroySpeed(level, pos) * ModularTuning.CHIP_STRAIN_PER_HARDNESS);
        int extra = misuse(stack, level, state, pos) + rust(stack, miner);
        if (extra > 0)
            stack.hurtAndBreak(extra, miner, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
    }

    /** Whether the stack has an edge to grind: a melee head or a blade. */
    public static boolean edged(ItemStack stack) {
        ModularDesign d = design(stack);
        ModularType type = d == null ? null : ModularTypes.known().get(d.type());
        return type != null && type.edged();
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
        if (type != null && BuildNames.names(type)) {
            Optional<Component> built = BuildNames.of(d, type, typeName);
            if (built.isPresent())
                return built.get();
        }
        if (!main.variant().isEmpty() && type != null && type.strikers().isEmpty())
            typeName = Component.translatable("modular.feedback.archetype." + main.variant());
        Component material = Component.translatable("material." + main.material().getNamespace() + "." + main.material().getPath());
        if (type != null && !type.strikers().isEmpty())
            typeName = twinName(d, type, typeName);
        for (Part p : d.parts()) {
            ModularType.Role r = type == null ? null : type.roles().get(p.role());
            if (r != null && r.set())
                return Component.translatable("item.feedback.modular_name." + r.name(),
                        Component.translatable("material." + p.material().getNamespace() + "." + p.material().getPath()), material, typeName);
        }
        return Component.translatable("item.feedback.modular_name", material, typeName);
    }

    /** A twin-headed tool by its pair of heads: the pair's own name if the type gives one (a mattock), else "Pick-Axe". */
    private static Component twinName(ModularDesign d, ModularType type, Component fallback) {
        List<String> kinds = new ArrayList<>();
        for (String role : type.strikers()) {
            Part p = d.part(role);
            if (p != null)
                kinds.add(ModularHeads.kindOf(p, type.roles().get(role)));
        }
        if (kinds.size() < 2)
            return fallback;
        List<String> sorted = kinds.stream().sorted().toList();
        String named = type.combos().get(sorted.get(0) + "+" + sorted.get(1));
        if (named != null)
            return Component.translatable("modular.feedback.combo." + named);
        return Component.translatable("modular.feedback.combo.pair",
                Component.translatable("modular.feedback.head." + kinds.get(0)),
                Component.translatable("modular.feedback.head." + kinds.get(1)));
    }

    public static Item.Properties properties() {
        return new Item.Properties().durability(250);
    }
}
