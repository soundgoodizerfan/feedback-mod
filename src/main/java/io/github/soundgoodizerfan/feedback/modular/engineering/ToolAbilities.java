// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.engineering;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbility;

/**
 * What a hand tool can do at the Engineering Table, as NeoForge item abilities so a modded tool can join by answering
 * {@code canPerformAction}. The modular hand tools answer by what they are: the hammer {@code hammer}, the sawblade {@code saw},
 * the file {@code file}, the screwdriver {@code driver}. Hand tools may always wear (design digest), so a use costs a point.
 */
public final class ToolAbilities {

    public static final ItemAbility HAMMER = ItemAbility.get("feedback:hammer");
    public static final ItemAbility SAW = ItemAbility.get("feedback:saw");
    public static final ItemAbility FILE = ItemAbility.get("feedback:file");
    public static final ItemAbility DRIVER = ItemAbility.get("feedback:driver");

    private static final List<ItemAbility> ALL = List.of(HAMMER, SAW, FILE, DRIVER);

    private ToolAbilities() {
    }

    /** The ability a face of head kind {@code kind} gives the tool it is on, or null for a kind that is no bench tool's. */
    @Nullable
    public static ItemAbility forFace(String kind) {
        return switch (kind) {
            case "head_blank" -> HAMMER;
            case "file_head" -> FILE;
            case "driver_bit" -> DRIVER;
            default -> null;
        };
    }

    /** A live face of a hafted tool and its role: the figures its shape (variant) and axes give. */
    public record Face(io.github.soundgoodizerfan.feedback.modular.type.ModularDesign.Part part, io.github.soundgoodizerfan.feedback.modular.type.ModularType.Role role) {

        /** A figure of the face's shape by name: its axis of that name, else its variant's param, else {@code fallback}. */
        public float param(String name, float fallback) {
            return role.paramOf(part, name, fallback);
        }

        /** The axis {@code name} as a multiple of its default; 1 when the face has no such axis. */
        public float ratio(String name) {
            var axis = role.axesFor(part).get(name);
            return axis == null || axis.def() <= 0f ? 1f : role.axis(part, name) / axis.def();
        }
    }

    /** The live face of head kind {@code kind} ({@code file_head}, {@code driver_bit}) on {@code tool}, or null when it has none. */
    @Nullable
    public static Face face(ItemStack tool, String kind) {
        var design = ModularStacks.design(tool);
        var type = design == null ? null : io.github.soundgoodizerfan.feedback.modular.type.ModularTypes.known(design.type());
        if (type == null || type.family() != io.github.soundgoodizerfan.feedback.modular.type.ModularType.Family.HAFTED)
            return null;
        for (var p : io.github.soundgoodizerfan.feedback.modular.type.ModularFaces.faces(design)) {
            var role = type.roles().get(p.role());
            if (role != null && !io.github.soundgoodizerfan.feedback.modular.type.ModularFaces.spent(p)
                    && io.github.soundgoodizerfan.feedback.modular.type.ModularHeads.kindOf(p, role).equals(kind))
                return new Face(p, role);
        }
        return null;
    }

    /**
     * Fastener levels a screwdriver adds or costs a craft: the tip shape's figure for each form of fastener it drives (a cross tip centres in a
     * screw and rounds a bolt's head; a hex tip is the other way about), averaged over the fasteners {@code supply} takes, and what its shaft's
     * length adds (a long shaft reaches the seat square). 0 for a stack with no driver bit.
     */
    public static int driverLevels(ItemStack driver, List<ItemStack> fasteners, Fasteners.Supply supply) {
        Face bit = face(driver, "driver_bit");
        if (bit == null)
            return 0;
        float sum = 0f;
        int n = 0;
        for (int i = 0; i < supply.take().length && i < fasteners.size(); i++)
            if (supply.take()[i] > 0) {
                String form = io.github.soundgoodizerfan.feedback.process.MaterialIngredient.locate(fasteners.get(i).getItem()).map(l -> l.form()).orElse("");
                sum += supply.take()[i] * bit.param(form, 0f);
                n += supply.take()[i];
            }
        float shaft = (bit.ratio("shank") - 1f) * io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning.DRIVER_SHAFT_LEVELS_PER_RATIO;
        return Math.round((n == 0 ? 0f : sum / n) + shaft);
    }

    /**
     * The pawl strength (0..1) of the ratchet fitted to {@code driver}, from its material's strength; 0 when it has no driver bit or no ratchet
     * (a plain screwdriver is regripped every pull). The torque game takes it to hold torque between pulls and to slip under a hard pull.
     */
    public static float ratchet(ItemStack driver) {
        var design = ModularStacks.design(driver);
        var part = design == null || face(driver, "driver_bit") == null ? null : design.part("W");
        var material = part == null ? null : io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials().get(part.material());
        if (material == null)
            return 0f;
        return Math.max(0.01f, io.github.soundgoodizerfan.feedback.combat.gear.GearTuning.saturate(
                io.github.soundgoodizerfan.feedback.modular.bake.ModularBake.strength(material),
                io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning.RATCHET_STRENGTH_HALF));
    }

    /** The wear points a craft costs {@code driver}, from the base {@code wear}: a long shaft twists and tires. */
    public static int driverWear(ItemStack driver, int wear) {
        Face bit = face(driver, "driver_bit");
        return bit == null ? wear : Math.max(1, Math.round(wear * (float) Math.pow(bit.ratio("shank"),
                io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning.DRIVER_SHAFT_WEAR_EXPONENT)));
    }

    /** The ability a modular hand tool has by its type, or null for any other stack. */
    @Nullable
    public static ItemAbility of(ItemStack stack) {
        if (stack.is(FModular.SAWBLADE.get()))
            return SAW;
        return null;
    }

    /** An ability by its name ({@code feedback:hammer}), or null when none is known. */
    @Nullable
    public static ItemAbility byName(String name) {
        for (ItemAbility a : ALL)
            if (a.name().equals(name))
                return a;
        return ItemAbility.get(name);
    }

    /** The item tag that stands in for {@code ability} on an item that cannot answer {@code canPerformAction}: {@code feedback:hammer} is {@code #feedback:tool/hammer}. */
    public static TagKey<Item> tag(ItemAbility ability) {
        ResourceLocation id = ResourceLocation.parse(ability.name());
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "tool/" + id.getPath()));
    }

    /** Whether {@code stack} has {@code ability}, by answering it or by being in its item tag ({@link #tag}). */
    public static boolean can(ItemStack stack, ItemAbility ability) {
        return !stack.isEmpty() && (stack.canPerformAction(ability) || stack.is(tag(ability)));
    }

    /** Whether {@code stack} has any of the abilities a tool slot knows: what the table's tool slots accept. */
    public static boolean isTool(ItemStack stack) {
        for (ItemAbility a : ALL)
            if (can(stack, a))
                return true;
        return false;
    }

    /**
     * The slot of the first tool in {@code tools} with {@code ability}, preferring one that is not broken; -1 when none has it. A broken
     * one that is found alone is returned, so the caller can refuse the craft and say why.
     */
    public static int find(List<ItemStack> tools, ItemAbility ability) {
        int broken = -1;
        for (int i = 0; i < tools.size(); i++) {
            ItemStack t = tools.get(i);
            if (!can(t, ability))
                continue;
            if (!ModularStacks.broken(t))
                return i;
            if (broken < 0)
                broken = i;
        }
        return broken;
    }

    /** Wears {@code tool} by {@code points}, never past its last point (broken, not destroyed). Returns the points taken. */
    public static int wear(ItemStack tool, int points) {
        int taken = ModularStacks.cap(tool, Math.max(0, points));
        tool.setDamageValue(tool.getDamageValue() + taken);
        return taken;
    }
}
