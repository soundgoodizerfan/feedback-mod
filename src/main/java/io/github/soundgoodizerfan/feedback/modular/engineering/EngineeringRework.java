// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.engineering;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.combat.skill.SkillEarning;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillTree;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.modular.ModularBlueprints;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularNest;
import io.github.soundgoodizerfan.feedback.modular.ModularPartItem;
import io.github.soundgoodizerfan.feedback.modular.ModularRework;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.School;
import io.github.soundgoodizerfan.feedback.modular.ToolDesigns;
import io.github.soundgoodizerfan.feedback.modular.PartBenchMenu;
import io.github.soundgoodizerfan.feedback.modular.Treatises;
import io.github.soundgoodizerfan.feedback.net.HudReadoutPayload;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * What the Engineering Table's Fit, Repair and Treatise tabs do, on the server. Each action reads the table's container, decides, and either
 * changes it whole or leaves it alone and answers with a refusal {@code note}. The work itself is {@link ModularRework}; this adds what the
 * table owns: the hammer in the tool row (a point of wear per action, as a hand tool wears), the treatise gate for fitting a new part of a
 * gated type, and where the pieces go.
 */
public final class EngineeringRework {

    /** The notes an action leaves for the screen ({@code gui.feedback.engineering.note.*}). */
    public static final int NONE = 0, NO_TOOL = 1, BROKEN_TOOL = 2, NOT_MODULAR = 3, NOTHING = 4, REPAIRED = 5, SWAPPED = 6, TAKEN_APART = 7,
            FITTED = 8, DOES_NOT_FIT = 9, SCHOOL = 10, NO_SCHOOL = 11, NO_BLANK = 12, NO_SPECIMEN = 13, WRITTEN = 14, DISH = 15;

    /** The most stock pieces one action will try. */
    private static final int MAX_PIECES = 64;

    /** An action's note and, for {@link #SCHOOL}, the volume the item's type needs ({@code (School ordinal + 1) * 8 + volume}), else 0. */
    public record Outcome(int note, int volume) {
        static Outcome of(int note) {
            return new Outcome(note, 0);
        }
    }

    private EngineeringRework() {
    }

    // --- the hammer ---------------------------------------------------------------------------

    /** 0 when the tool row holds a hammer that works, else {@link #NO_TOOL} or {@link #BROKEN_TOOL}. */
    private static int hammer(Container table) {
        int i = ToolAbilities.find(tools(table), ToolAbilities.HAMMER);
        if (i < 0)
            return NO_TOOL;
        return ModularStacks.broken(table.getItem(EngineeringTableBlockEntity.TOOLS + i)) ? BROKEN_TOOL : 0;
    }

    private static List<ItemStack> tools(Container table) {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < 4; i++)
            out.add(table.getItem(EngineeringTableBlockEntity.TOOLS + i));
        return out;
    }

    /** A point off the hammer: the hand tool's accepted wear. */
    private static void wear(Container table) {
        int i = ToolAbilities.find(tools(table), ToolAbilities.HAMMER);
        if (i >= 0)
            ToolAbilities.wear(table.getItem(EngineeringTableBlockEntity.TOOLS + i), 1);
        table.setChanged();
    }

    private static ModularType typeOf(ItemStack item, Level level) {
        ModularDesign design = ModularStacks.design(item);
        return design == null ? null : EngineeringAssembly.types(level).get(design.type());
    }

    private static float salvage(ServerPlayer server) {
        return Skills.modifier(server, SkillTree.Figure.SALVAGE_WEAR);
    }

    // --- Repair tab ---------------------------------------------------------------------------

    /**
     * Repair ({@link ModularRework.Mode#MEND}) or part swap ({@link ModularRework.Mode#SWAP}) of the item in the Repair slot from the stock
     * grid. Repair takes as many pieces as it needs, a swap the fewest that make a part. A swap takes the old part off and gives it back
     * (the wearing part short by its wear). Neither needs a treatise: you are copying what is in front of you.
     */
    public static Outcome repair(Container table, ServerPlayer server, ModularRework.Mode mode) {
        Level level = server.level();
        ItemStack item = table.getItem(EngineeringTableBlockEntity.REPAIR_ITEM);
        ModularDesign design = ModularStacks.design(item);
        ModularType type = typeOf(item, level);
        if (type == null)
            return Outcome.of(NOT_MODULAR);
        if (PartBenchMenu.isDish(item))
            return Outcome.of(DISH);
        int tool = hammer(table);
        if (tool != 0)
            return Outcome.of(tool);
        List<ItemStack> pieces = new ArrayList<>();
        List<Integer> owner = new ArrayList<>();
        for (int i = 0; i < 9 && pieces.size() < MAX_PIECES; i++) {
            ItemStack s = table.getItem(EngineeringTableBlockEntity.REPAIR_STOCK + i);
            for (int k = 0; k < s.getCount() && pieces.size() < MAX_PIECES; k++) {
                pieces.add(s.copyWithCount(1));
                owner.add(i);
            }
        }
        Map<ResourceLocation, Material> materials = ClientMaterials.on(level);
        Optional<ItemStack> best = Optional.empty();
        int used = 0;
        for (int n = 1; n <= pieces.size(); n++) {
            Optional<ItemStack> r = ModularRework.rework(item, pieces.subList(0, n), materials, EngineeringAssembly.types(level), mode);
            if (r.isEmpty()) {
                if (best.isPresent())
                    break;
                continue;
            }
            best = r;
            used = n;
            if (mode != ModularRework.Mode.MEND || r.get().getDamageValue() == 0)
                break;
        }
        if (best.isEmpty())
            return Outcome.of(NOTHING);
        // The part a swap takes off goes back to the player.
        ModularDesign now = ModularStacks.design(best.get());
        if (mode == ModularRework.Mode.SWAP && now != null)
            for (ModularDesign.Part old : design.parts()) {
                ModularType.Role role = type.roles().get(old.role());
                if (role == null || old.equals(now.part(old.role())))
                    continue;
                int count = old.role().equals(type.wearRole()) ? (int) Math.floor(old.count() * ModularRework.kept(item, salvage(server))) : old.count();
                ToolDesigns.items(old, role, materials, count).forEach(i -> server.getInventory().placeItemBackInInventory(i));
            }
        for (int n = 0; n < used; n++) {
            int slot = EngineeringTableBlockEntity.REPAIR_STOCK + owner.get(n);
            ItemStack s = table.getItem(slot);
            s.shrink(1);
            table.setItem(slot, s.isEmpty() ? ItemStack.EMPTY : s);
        }
        table.setItem(EngineeringTableBlockEntity.REPAIR_ITEM, best.get());
        wear(table);
        return Outcome.of(mode == ModularRework.Mode.SWAP ? SWAPPED : REPAIRED);
    }

    /** Takes the Repair slot's item apart: its parts go to the player (a worn wearing part short by its wear, a held item whole). */
    public static Outcome takeApart(Container table, ServerPlayer server) {
        Level level = server.level();
        ItemStack item = table.getItem(EngineeringTableBlockEntity.REPAIR_ITEM);
        ModularType type = typeOf(item, level);
        if (type == null)
            return Outcome.of(NOT_MODULAR);
        if (PartBenchMenu.isDish(item))
            return Outcome.of(DISH);
        int tool = hammer(table);
        if (tool != 0)
            return Outcome.of(tool);
        List<ItemStack> parts = ModularRework.dismantle(item, ClientMaterials.on(level), EngineeringAssembly.types(level), salvage(server));
        if (ModularStacks.worn(item) > 0f)
            SkillEarning.salvaged(server, "apart/" + ModularStacks.design(item).type());
        table.setItem(EngineeringTableBlockEntity.REPAIR_ITEM, ItemStack.EMPTY);
        parts.forEach(p -> server.getInventory().placeItemBackInInventory(p));
        wear(table);
        return Outcome.of(TAKEN_APART);
    }

    // --- Fit tab ------------------------------------------------------------------------------

    /**
     * Fits every attachment in the grid to the Fit slot's item at once ({@link ModularRework#addAll}), all or nothing. A type with a school
     * needs its volume held ({@code mask}: the maker's inventory and shelf plus the binder) before a new part goes on.
     */
    public static Outcome fit(Container table, ServerPlayer server, int mask) {
        Level level = server.level();
        ItemStack item = table.getItem(EngineeringTableBlockEntity.FIT_ITEM);
        ModularType type = typeOf(item, level);
        if (type == null)
            return Outcome.of(NOT_MODULAR);
        if (PartBenchMenu.isDish(item))
            return Outcome.of(DISH);
        List<List<ItemStack>> groups = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            ItemStack s = table.getItem(EngineeringTableBlockEntity.ATTACHMENTS + i);
            if (s.isEmpty())
                continue;
            List<ItemStack> pieces = new ArrayList<>();
            for (int k = 0; k < s.getCount(); k++)
                pieces.add(s.copyWithCount(1));
            groups.add(pieces);
            slots.add(i);
        }
        if (groups.isEmpty())
            return Outcome.of(NOTHING);
        if (!Treatises.draws(mask, type)) {
            School s = School.of(type.school());
            return new Outcome(SCHOOL, (s.ordinal() + 1) * 8 + Math.min(School.volumeOf(type.school()), s.volumes()));
        }
        int tool = hammer(table);
        if (tool != 0)
            return Outcome.of(tool);
        ModularRework.Added added = ModularRework.addAll(item, groups, ClientMaterials.on(level), EngineeringAssembly.types(level));
        if (!added.ok())
            return Outcome.of(DOES_NOT_FIT);
        for (int g = 0; g < slots.size(); g++) {
            int slot = EngineeringTableBlockEntity.ATTACHMENTS + slots.get(g);
            ItemStack s = table.getItem(slot);
            s.shrink(added.used()[g]);
            table.setItem(slot, s.isEmpty() ? ItemStack.EMPTY : s);
        }
        table.setItem(EngineeringTableBlockEntity.FIT_ITEM, added.out().get());
        wear(table);
        return Outcome.of(FITTED);
    }

    /** Whether a stack may sit in the Fit grid or the Repair stock grid: a shaped part, a held module or stock of some material. */
    public static boolean isStock(ItemStack stack, Level level) {
        return stack.getItem() instanceof ModularPartItem || ModularNest.holdable(stack) || Material.find(ClientMaterials.on(level), stack).isPresent();
    }

    // --- Treatise tab -------------------------------------------------------------------------

    /** Whether {@code stack} is blank paper a blueprint can be written on: any {@code #feedback:blueprint_paper} item that is not already a sheet. */
    public static boolean isBlankPaper(ItemStack stack) {
        return stack.is(FTags.BLUEPRINT_PAPER) && !stack.is(FModular.BLUEPRINT.get());
    }

    /**
     * Takes the specimen apart onto the blank slot's paper. A blank treatise becomes the specimen's school volume ({@link TreatiseGame#onLearn});
     * blank blueprint paper becomes a sheet of a complex specimen's design ({@link #measure}); either way the specimen's parts go to the
     * player. A volume needs the specimen's type to have a school; a copy of a drawing needs no treatise.
     */
    public static Outcome learn(Container table, ServerPlayer server) {
        Level level = server.level();
        ItemStack blank = table.getItem(EngineeringTableBlockEntity.BLANK);
        ItemStack specimen = table.getItem(EngineeringTableBlockEntity.SPECIMEN);
        boolean paper = isBlankPaper(blank);
        if (!blank.is(FModular.BLANK_TREATISE.get()) && !paper)
            return Outcome.of(NO_BLANK);
        ModularType type = typeOf(specimen, level);
        if (type == null)
            return Outcome.of(NO_SPECIMEN);
        if (paper)
            return measure(table, server, specimen, type);
        School school = School.of(type.school());
        if (school == null)
            return Outcome.of(NO_SCHOOL);
        int tool = hammer(table);
        if (tool != 0)
            return Outcome.of(tool);
        int volume = School.volumeOf(type.school());
        List<ItemStack> parts = ModularRework.dismantle(specimen, ClientMaterials.on(level), EngineeringAssembly.types(level), salvage(server));
        table.setItem(EngineeringTableBlockEntity.BLANK, TreatiseGame.onLearn(server, school, volume, specimen));
        table.setItem(EngineeringTableBlockEntity.SPECIMEN, ItemStack.EMPTY);
        parts.forEach(p -> server.getInventory().placeItemBackInInventory(p));
        wear(table);
        HudReadoutPayload.send(server, Component.translatable("message.feedback.treatise.written", Component.translatable(school.itemKey(volume))), 60);
        return Outcome.of(WRITTEN);
    }

    /**
     * Taking a complex item apart with blank paper in the blank slot measures it: the paper becomes a blueprint sheet of its design (the parts of
     * it that are shaped; a circuit or cell it carried is not part of what a blueprint pins) and the item's parts go to the player. A simple
     * type has no drawing worth keeping, so it is refused.
     */
    private static Outcome measure(Container table, ServerPlayer server, ItemStack specimen, ModularType type) {
        if (!type.blueprint())
            return Outcome.of(NO_SCHOOL);
        int tool = hammer(table);
        if (tool != 0)
            return Outcome.of(tool);
        Level level = server.level();
        ModularDesign design = ModularStacks.design(specimen);
        ModularDesign pinned = new ModularDesign(design.type(), design.parts().stream().filter(p -> p.sub().isEmpty()).toList(), design.edge());
        var figures = ToolDesigns.predict(pinned, EngineeringAssembly.types(level), ClientMaterials.on(level));
        List<ItemStack> parts = ModularRework.dismantle(specimen, ClientMaterials.on(level), EngineeringAssembly.types(level), salvage(server));
        table.setItem(EngineeringTableBlockEntity.BLANK, ModularBlueprints.sheet(pinned, figures.orElse(null)));
        table.setItem(EngineeringTableBlockEntity.SPECIMEN, ItemStack.EMPTY);
        parts.forEach(p -> server.getInventory().placeItemBackInInventory(p));
        wear(table);
        HudReadoutPayload.send(server, Component.translatable("message.feedback.plan.measured"), 60);
        return Outcome.of(WRITTEN);
    }
}
