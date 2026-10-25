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
package io.github.soundgoodizerfan.feedback.core.hazard;

import java.util.HashMap;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
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
 * What a body takes from the stuff around it (mechanics 9.8). The dose rate at a player is every
 * radioactive thing's figure ({@code radioactivity} in its material file, for one ingot's volume)
 * added up: what the player carries counts in full, a block or a dropped item within
 * {@link FTuning#RAD_REACH_BLOCKS} counts for {@code 1 / (1 + d^2)}. Armour then shields by how
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

    private static Map<ResourceLocation, Material> builtFrom;
    private static Map<Item, Float> sources = Map.of();

    /** The activity of one item, in Ru, by material: built once per material table, since few materials radiate. */
    private static synchronized Map<Item, Float> sources(Map<ResourceLocation, Material> materials) {
        if (builtFrom != materials) {
            Map<Item, Float> out = new HashMap<>();
            materials.forEach((id, material) -> material.radioactivity().ifPresent(ru ->
                    material.items(id).forEach((form, item) -> out.put(item, ru.value() * FormItems.volume(form) / 144f))));
            sources = out;
            builtFrom = materials;
        }
        return sources;
    }

    /** What one stack gives at point blank, whole stack: a form's volume, a shaped part's cells, a modular item's parts. */
    public static float activity(ItemStack stack, Map<ResourceLocation, Material> materials) {
        if (stack.isEmpty())
            return 0f;
        float each = sources(materials).getOrDefault(stack.getItem(), 0f);
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
        return each * stack.getCount();
    }

    private static Map<ResourceLocation, Material> densitiesFrom;
    private static Map<Item, Float> densities = Map.of();

    /** The density of every item that is some material, built once per material table: what a wall of it is worth. */
    private static synchronized Map<Item, Float> densities(Map<ResourceLocation, Material> materials) {
        if (densitiesFrom != materials) {
            Map<Item, Float> out = new HashMap<>();
            materials.forEach((id, material) -> {
                float density = material.density().muPerMb();
                if (density > 0f)
                    material.items(id).values().forEach(item -> out.put(item, density));
            });
            densities = out;
            densitiesFrom = materials;
        }
        return densities;
    }

    /** What a source at point blank gives, in Ru, if the block is one: its material's figure times its form's volume. */
    public static float blockActivity(net.minecraft.world.level.block.Block block, Map<ResourceLocation, Material> materials) {
        return sources(materials).getOrDefault(block.asItem(), 0f);
    }

    /** What a container lined with {@code lining} lets out, 0 to 1. */
    public static float containerShield(Material lining) {
        return 1f / (1f + FTuning.RAD_CONTAINER_GAIN * lining.density().muPerMb() / 10f);
    }

    /** What the blocks strictly between two cells let through, 0 to 1: each is worth its material's density, none for air or a block that is no material. */
    private static float through(Level level, BlockPos from, BlockPos to, Map<Item, Float> density) {
        int n = Math.max(Math.abs(to.getX() - from.getX()), Math.max(Math.abs(to.getY() - from.getY()), Math.abs(to.getZ() - from.getZ())));
        float thickness = 0f;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 1; i < n; i++) {
            pos.set(from.getX() + Math.round((to.getX() - from.getX()) * (float) i / n),
                    from.getY() + Math.round((to.getY() - from.getY()) * (float) i / n),
                    from.getZ() + Math.round((to.getZ() - from.getZ()) * (float) i / n));
            var state = level.getBlockState(pos);
            if (!state.isAir())
                thickness += density.getOrDefault(state.getBlock().asItem(), 0f);
        }
        return 1f / (1f + FTuning.RAD_WALL_GAIN * thickness / 10f);
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
        Map<Item, Float> table = sources(materials);
        if (table.isEmpty())
            return total;
        Map<Item, Float> density = densities(materials);
        int r = FTuning.RAD_REACH_BLOCKS;
        BlockPos centre = player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++)
            for (int dy = -r; dy <= r; dy++)
                for (int dz = -r; dz <= r; dz++) {
                    pos.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    var state = level.getBlockState(pos);
                    float here = table.getOrDefault(state.getBlock().asItem(), 0f);
                    if (state.hasBlockEntity() && level.getBlockEntity(pos) instanceof net.minecraft.world.Container box) {
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
        for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(r))) {
            float act = activity(drop.getItem(), materials);
            if (act > 0f)
                total += (float) (act / (1.0 + drop.distanceToSqr(player))) * through(level, drop.blockPosition(), centre, density);
        }
        return total;
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
        return 1f / (1f + FTuning.RAD_SHIELD_GAIN * density / 4f / 10f);
    }
}
