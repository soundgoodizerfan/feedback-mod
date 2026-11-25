// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.hazard;

import java.util.HashMap;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FormItems;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * What a body takes from the stuff around it. The dose rate at a player is every
 * radioactive thing's figure ({@code radioactivity} in its material file, for one ingot's volume)
 * added up: what the player carries counts in full, a block or a dropped item within
 * {@link HazardTuning#RAD_REACH_BLOCKS} counts for {@code 1 / (1 + d^2)}. Armour then shields by how
 * dense it is, read off the material and never its name -- so lead does well, iron nearly as well,
 * and a uranium helmet is its own problem.
 * <p>
 * Pure reads of the world, so the server's dose and a Geiger counter's click agree: both call
 * {@link #rate}. The server keeps the running total; the counter never sees it.
 * <p>
 * A container's contents count, through its lining if it is {@link Shielded}; a wall between a
 * source and the body is worth the density of what it is made of, block by block along the line.
 * Not modelled, and left {@code [OPEN]}: what is in a chest in another player's inventory, a
 * shulker box carried (its contents are not read), and anything but a straight line.
 */
public final class Radiation {

    private Radiation() {
    }

    /**
     * What an item is worth by what material it is, found when first asked (not listed up front, so another mod's uranium ingot or lead
     * block counts as ours do): memoised per material table, which a reload replaces.
     */
    private static final class PerItem {
        private final Map<ResourceLocation, Material> from;
        private final java.util.function.Function<Material.Located, Float> figure;
        private final Map<Item, Float> memo = new java.util.concurrent.ConcurrentHashMap<>();

        PerItem(Map<ResourceLocation, Material> from, java.util.function.Function<Material.Located, Float> figure) {
            this.from = from;
            this.figure = figure;
        }

        float of(Item item) {
            return memo.computeIfAbsent(item, i -> Material.find(from, i).map(figure).orElse(0f));
        }
    }

    private static PerItem sources;
    private static PerItem densities;

    /** The activity of one item, in Ru, by material: its material's figure times its form's volume. */
    private static synchronized PerItem sources(Map<ResourceLocation, Material> materials) {
        if (sources == null || sources.from != materials)
            sources = new PerItem(materials, l -> l.material().radioactivity().map(ru -> ru.value() * FormItems.volume(l.form()) / 144f).orElse(0f));
        return sources;
    }

    /** What one stack gives at point blank, whole stack: a form's volume, a shaped part's cells, a modular item's parts. */
    public static float activity(ItemStack stack, Map<ResourceLocation, Material> materials) {
        if (stack.isEmpty())
            return 0f;
        float each = sources(materials).of(stack.getItem());
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        if (made != null) {
            Material m = materials.get(made.material());
            if (m != null)
                each += m.radioactivity().map(ru -> ru.value() * made.cells()).orElse(0f);
        }
        ModularDesign design = ModularStacks.design(stack);
        if (design != null)
            for (ModularDesign.Part part : design.parts()) {
                Material m = materials.get(part.material());
                if (m != null)
                    each += m.radioactivity().map(ru -> ru.value() * part.count() * FormItems.volume(part.form()) / 144f).orElse(0f);
            }
        if (Boolean.TRUE.equals(stack.get(FDataComponents.CONTAMINATED.get())))
            each += HazardTuning.CONTAMINATED_DROP_RU;
        return each * stack.getCount();
    }

    /** The density of what an item is made of: what a wall of it is worth. */
    private static synchronized PerItem densities(Map<ResourceLocation, Material> materials) {
        if (densities == null || densities.from != materials)
            densities = new PerItem(materials, l -> Math.max(0f, l.material().density().muPerMb()));
        return densities;
    }

    /** What a source at point blank gives, in Ru, if the block is one: its material's figure times its form's volume. */
    public static float blockActivity(net.minecraft.world.level.block.Block block, Map<ResourceLocation, Material> materials) {
        return sources(materials).of(block.asItem());
    }

    /**
     * The share of radiation from open sky that reaches the cell {@code centre}, 0 to 1: six rays along the axes, each through
     * the blocks on it as {@link #through} weighs them, averaged. A cell walled in dense plate on all sides reads near 0; one open
     * to the sky reads 1. {@code centre} is a cell of {@code level}'s own coordinates (a body's plot, for a person aboard).
     */
    public static float openShare(Level level, BlockPos centre, Map<ResourceLocation, Material> materials) {
        PerItem density = densities(materials);
        float sum = 0f;
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values())
            sum += through(level, centre, centre.relative(d, HazardTuning.AMBIENT_RAY_BLOCKS), density);
        return sum / 6f;
    }

    /** What a container lined with {@code lining} lets out, 0 to 1. */
    public static float containerShield(Material lining) {
        return 1f / (1f + HazardTuning.RAD_CONTAINER_GAIN * lining.density().muPerMb() / 10f);
    }

    /** What the blocks strictly between two cells let through, 0 to 1: each is worth its material's density, none for air or a block that is no material. */
    private static float through(Level level, BlockPos from, BlockPos to, PerItem density) {
        int n = Math.max(Math.abs(to.getX() - from.getX()), Math.max(Math.abs(to.getY() - from.getY()), Math.abs(to.getZ() - from.getZ())));
        float thickness = 0f;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 1; i < n; i++) {
            pos.set(from.getX() + Math.round((to.getX() - from.getX()) * (float) i / n),
                    from.getY() + Math.round((to.getY() - from.getY()) * (float) i / n),
                    from.getZ() + Math.round((to.getZ() - from.getZ()) * (float) i / n));
            var state = level.getBlockState(pos);
            if (!state.isAir())
                thickness += density.of(state.getBlock().asItem());
        }
        return 1f / (1f + HazardTuning.RAD_WALL_GAIN * thickness / 10f);
    }

    /**
     * The dose rate in Ru at a cell of the world, from what is around it and nothing carried: each radioactive block (its own included),
     * the contents of each container and each dropped item within reach at {@code 1 / (1 + d^2)}, through whatever stands between. What a
     * block that shows radiation reads; {@link io.github.soundgoodizerfan.feedback.machine.glow.RadiationField} caches it.
     */
    public static float rateAt(Level level, BlockPos centre, Map<ResourceLocation, Material> materials) {
        PerItem table = sources(materials);
        if (materials.values().stream().noneMatch(m -> m.radioactivity().isPresent()))
            return 0f;
        PerItem density = densities(materials);
        float total = 0f;
        int r = HazardTuning.RAD_REACH_BLOCKS;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++)
            for (int dy = -r; dy <= r; dy++)
                for (int dz = -r; dz <= r; dz++) {
                    pos.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    // A cell near the edge of what is loaded must not load its neighbours to read them.
                    if (!level.hasChunkAt(pos))
                        continue;
                    var state = level.getBlockState(pos);
                    float here = table.of(state.getBlock().asItem());
                    var be = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
                    if (be instanceof RadiationEmitter emitter)
                        here += emitter.radiationRu();
                    if (be instanceof net.minecraft.world.Container box) {
                        float held = 0f;
                        for (int slot = 0; slot < box.getContainerSize(); slot++)
                            held += activity(box.getItem(slot), materials);
                        if (box instanceof Shielded lined) {
                            Material lining = materials.get(lined.shieldMaterial());
                            if (lining != null)
                                held *= containerShield(lining);
                        }
                        here += held;
                    }
                    if (here > 0f)
                        total += here / (1f + dx * dx + dy * dy + dz * dz) * through(level, pos.immutable(), centre, density);
                }
        for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(centre).inflate(r))) {
            float act = activity(drop.getItem(), materials);
            if (act > 0f)
                total += (float) (act / (1.0 + drop.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(centre)))) * through(level, drop.blockPosition(), centre, density);
        }
        return total;
    }

    /**
     * The dose rate in Ru at {@code player}, before their armour: what they carry in full; each
     * radioactive block, the contents of each container (through its lining, if it has one) and each
     * dropped item within reach at {@code 1 / (1 + d^2)}, then through whatever stands between.
     */
    public static float rate(Player player, Map<ResourceLocation, Material> materials) {
        Level level = player.level();
        float total = 0f;
        for (ItemStack stack : player.getInventory().items)
            total += activity(stack, materials);
        for (ItemStack stack : player.getInventory().armor)
            total += activity(stack, materials);
        for (ItemStack stack : player.getInventory().offhand)
            total += activity(stack, materials);
        return total + rateAt(level, player.blockPosition(), materials);
    }

    /** What the armour lets through, 0 to 1: dense worn material in the four slots. */
    public static float shield(LivingEntity wearer, Map<ResourceLocation, Material> materials) {
        float density = 0f;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR)
                continue;
            ItemStack worn = wearer.getItemBySlot(slot);
            if (!worn.isEmpty())
                density += Material.find(materials, worn).map(l -> l.material().density().muPerMb()).orElse(0f);
        }
        return 1f / (1f + HazardTuning.RAD_SHIELD_GAIN * density / 4f / 10f);
    }
}
