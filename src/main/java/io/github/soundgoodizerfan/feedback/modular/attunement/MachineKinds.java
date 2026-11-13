// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.attunement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.github.soundgoodizerfan.feedback.modular.bake.MaterialPerks;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * What an attunement does on a part of a placed machine. A machine reads the attunements through the design it was built
 * from ({@link MaterialPerks#attunements(ModularDesign)}), the same kinds a tool's parts carry; what differs is the quantity
 * the kind acts on. Three consumers:
 * <ul>
 * <li>{@link #shape}: at bake, a kind scales named figures of the machine's baked tag (heat shed, flow, drag, charge held),
 * the way a tool's bake reads its parts. Only roles a row names reach it, so a figure a machine lacks is never promised.</li>
 * <li>{@link #wearShare} and {@link #blastShare}: what a machine's misuse wear and its burst keep of what they would have been.</li>
 * <li>{@link #vessel}: the shell of a pressure holder wears above its danger line, and mend, tithe and soak act on that wear.</li>
 * </ul>
 * Wear is still only from misuse; nothing here costs a machine anything for running.
 */
public final class MachineKinds {

    /** Per level, the share of each named figure a kind adds (negative: takes away). Placeholder figures. */
    private static final Map<String, Map<String, Float>> SHAPES = new HashMap<>();

    static {
        // A cutting face that cuts deeper moves more per turn and takes more to turn; a padded one the reverse.
        shape("extra_damage", "MbPerRpm", 0.25f, "Load", 0.25f);
        shape("pad", "MbPerRpm", -0.15f, "Load", -0.2f);
        // A face coupled to fire makes more heat; coupled to cold, less (a chilled winding or rotor).
        shape("ignite_target", "Heat", 0.25f, "CoreLoss", 0.25f, "WindingDrop", 0.25f);
        shape("freeze", "Heat", -0.25f, "CoreLoss", -0.2f, "WindingDrop", -0.2f);
        // A lagging or housing that insulates sheds less of its heat; a sealed jar leaks less of its charge.
        shape("insulate", "Leak", -0.25f);
        shape("seal", "Leak", -0.2f);
        // Throwing back what it strikes is thrust and push; drawing it in is suction.
        shape("shove", "Force", 0.2f, "Power", 0.2f);
        shape("pull", "Lift", 0.2f);
        shape("momentum", "Inertia", 0.25f);
        shape("brace", "RimRpm", 0.1f);
        shape("clamp", "RimRpm", 0.1f, "FrameSu", 0.1f);
        shape("slip", "FrameSu", -0.08f);
        shape("slick", "Drag", -0.2f);
        shape("stick", "Friction", 0.2f);
        // Charge: a face that builds it holds more, a dielectric that stores it holds more, a conductor carries more.
        shape("build", "StoresEu", 0.2f, "Capacity", 0.2f, "AnodeEu", 0.2f);
        shape("capacitor", "StoresEu", 0.15f, "Capacity", 0.15f);
        shape("channel", "WindingDrop", -0.15f, "BurstAmps", 0.2f);
        shape("douse", "Heat", -0.2f);
        shape("affinity", "Load", -0.15f);
        shape("glow", "Amps", -0.15f);
        shape("dim", "Burnout", 0.15f);
        shape("draw", "Range", 0.2f);
        shape("store", "Mass", 0.25f);
        shape("release", "Leak", 0.25f);
    }

    /** Share of misuse wear one level of slip turns into slipping instead; share of a burst one level of damp soaks up, and the most that can be. */
    private static final float SLIP_WEAR_PER_LEVEL = 0.5f, DAMP_PER_LEVEL = 0.2f, DAMP_CAP = 0.7f;
    /** Condition a worn shell loses per tick at its critical line (less nearer the danger line), and the least it wears to. */
    private static final float SHELL_WEAR_PER_TICK = 0.0006f, SHELL_FLOOR = 0.4f;
    /** A worn shell gives way at this share of its lines at the floor, 1 as built. */
    private static final float LINE_AT_FLOOR = 0.6f;
    /** Ticks between mends; experience points one mend costs; condition one level of mend closes per mend. */
    private static final int MEND_INTERVAL = 40, MEND_XP = 1;
    private static final float MEND_PER_LEVEL = 0.02f;
    /** How far from a machine a mend reaches for the experience it costs, in blocks. */
    private static final double MEND_REACH = 6.0;
    /** Experience points a level of tithe pays per whole condition a shell wears away. */
    private static final float TITHE_XP_PER_CONDITION = 400f;

    private MachineKinds() {
    }

    private static void shape(String kind, Object... keyAndPer) {
        Map<String, Float> m = new HashMap<>();
        for (int i = 0; i < keyAndPer.length; i += 2)
            m.put((String) keyAndPer[i], (Float) keyAndPer[i + 1]);
        SHAPES.put(kind, m);
    }

    /** The levels of every kind on the machine's parts, summed. */
    public static Map<String, Integer> levels(ModularDesign design) {
        Map<String, Integer> out = new HashMap<>();
        if (design != null)
            for (MaterialPerks.Attunement a : MaterialPerks.attunements(design))
                out.merge(a.kind(), a.level(), Integer::sum);
        return out;
    }

    public static int level(ModularDesign design, String kind) {
        return levels(design).getOrDefault(kind, 0);
    }

    /** The components a machine item keeps its baked figures in. */
    private static final List<Supplier<DataComponentType<CompoundTag>>> FIGURES = List.of(
            FDataComponents.BLOCK_FIGURES::get, FDataComponents.DYNAMOTOR::get, FDataComponents.BATTERY::get,
            FDataComponents.PRESSURE_VESSEL::get, FDataComponents.BELT::get);

    /** Scales the figures of a freshly baked machine stack by what its attuned parts do. Run after the bake, once per bake. */
    public static void shape(ItemStack stack, ModularDesign design) {
        Map<String, Integer> levels = levels(design);
        if (levels.isEmpty())
            return;
        for (Supplier<DataComponentType<CompoundTag>> supplier : FIGURES) {
            DataComponentType<CompoundTag> type = supplier.get();
            CompoundTag tag = stack.get(type);
            if (tag == null)
                continue;
            CompoundTag shaped = tag.copy();
            levels.forEach((kind, n) -> {
                Map<String, Float> by = SHAPES.get(kind);
                if (by != null)
                    by.forEach((key, per) -> {
                        if (shaped.contains(key, Tag.TAG_FLOAT))
                            shaped.putFloat(key, shaped.getFloat(key) * Math.max(0.2f, 1f + per * n));
                    });
            });
            stack.set(type, shaped);
        }
    }

    /**
     * The share of a machine's misuse wear that gets through: soak turns some away (1 / (levels + 1)), slip turns some into slipping.
     * {@code design} is what the machine was built from (null: none).
     */
    public static float wearShare(ModularDesign design) {
        return WardKinds.wearShare(design) / (1f + SLIP_WEAR_PER_LEVEL * level(design, "slip"));
    }

    /** The share of a vessel's burst that gets through its damping shell. */
    public static float blastShare(ModularDesign design) {
        return 1f - Math.min(DAMP_CAP, level(design, "damp") * DAMP_PER_LEVEL);
    }

    /** The share of its lines a shell of this condition gives way at: 1 as built, less as it wears. */
    public static float lines(float condition) {
        return LINE_AT_FLOOR + (1f - LINE_AT_FLOOR) * Math.max(0f, Math.min(1f, (condition - SHELL_FLOOR) / (1f - SHELL_FLOOR)));
    }

    /**
     * One tick of a pressure shell's wear and its upkeep; returns the new condition (1 as built). Above its danger line the shell wears
     * (never from sitting under it), more the nearer the critical line, slowed by soak and slip; a worn shell gives way at a lower
     * pressure ({@link #lines}). Mend closes the wear at the cost of experience from whoever stands near; tithe pays experience out of the wear
     * the shell takes. {@code danger} and {@code critical} are the lines as they stand now.
     */
    public static float vessel(Level level, BlockPos pos, ModularDesign design, float condition, float pressure, float danger, float critical) {
        float before = condition;
        if (pressure > danger) {
            float span = critical - danger;
            float excess = span <= 0f ? 1f : Math.min(1f, (pressure - danger) / span);
            condition = Math.max(SHELL_FLOOR, condition - SHELL_WEAR_PER_TICK * excess * wearShare(design));
        }
        if (!(level instanceof ServerLevel server) || (before >= 1f && condition >= 1f))
            return condition;
        Map<String, Integer> levels = levels(design);
        int tithe = levels.getOrDefault("tithe", 0);
        if (tithe > 0 && before > condition) {
            float xp = (before - condition) * TITHE_XP_PER_CONDITION * tithe;
            int whole = (int) xp;
            if (server.random.nextFloat() < xp - whole)
                whole++;
            if (whole > 0)
                ExperienceOrb.award(server, Vec3.atCenterOf(pos), whole);
        }
        int mend = levels.getOrDefault("mend", 0);
        if (mend > 0 && condition < 1f && level.getGameTime() % MEND_INTERVAL == 0) {
            Player payer = server.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, MEND_REACH,
                    p -> p instanceof Player pl && pl.totalExperience > 0 && !pl.isSpectator());
            if (payer != null) {
                payer.giveExperiencePoints(-MEND_XP);
                condition = Math.min(1f, condition + mend * MEND_PER_LEVEL);
            }
        }
        return condition;
    }
}
