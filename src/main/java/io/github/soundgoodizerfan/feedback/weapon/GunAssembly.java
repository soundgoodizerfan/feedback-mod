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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.weapon;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import org.jetbrains.annotations.Nullable;

/**
 * What {@link io.github.soundgoodizerfan.feedback.modular.ModularRecipe} does when a gun is in the
 * grid ({@code docs/feedback_modular_items_spec.md} section 8): the gun's own half of assembly and refit.
 * The recipe is the core's; the arithmetic of which parts a design takes is the gun's.
 *
 * <h2>Assembly</h2>
 * A Gun Blueprint and its machined parts make that Gun; the Blueprint is kept (spec 12). The
 * blueprint says which parts by form ({@link GunDesign#assembly}); any part that is that form of a
 * gun metal fills the place, read off its {@code MadeOf}, never an id. The gun is what was put in:
 * each part's material comes off its stack and the barrel's real shape off its
 * {@link BarrelGeometry}, so the figures are baked from the parts, not the paper -- lazily, by
 * {@link GunItem}, on the server's own material table.
 *
 * <h2>Refit</h2>
 * A built gun and one fitting part -- a suppressor, a brake, a relief vent, a shroud, of any gun
 * metal -- make that gun with the part fitted; a gun and one or two plates make it with a reflex
 * sight or a scope. Whatever the new part displaced comes back in the grid where the new part was.
 * The gun keeps its rounds, heat and wear, and loses its figures, which {@link GunItem} bakes again.
 */
public final class GunAssembly {

    private GunAssembly() {
    }

    /** The one gun blueprint in the grid that carries a design, or null if there is none or more than one. */
    @Nullable
    public static ItemStack blueprint(CraftingInput input) {
        ItemStack found = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.is(FWeapons.GUN_BLUEPRINT.get()) && s.has(FWeapons.GUN_DESIGN.get())) {
                if (found != null)
                    return null;
                found = s;
            }
        }
        return found;
    }

    /** Whether {@code s} is a built gun. */
    public static boolean isGun(ItemStack s) {
        return s.is(FWeapons.GUN.get()) && s.has(FWeapons.GUN_DESIGN.get());
    }

    /** The gun the grid builds from {@code blueprint}, or empty if it is not that gun's parts. */
    public static ItemStack assemble(CraftingInput input, ItemStack blueprint, Map<ResourceLocation, Material> materials) {
        GunDesign built = design(input, blueprint, materials);
        if (built == null)
            return ItemStack.EMPTY;
        ItemStack gun = new ItemStack(FWeapons.GUN.get());
        gun.set(FWeapons.GUN_DESIGN.get(), built);
        // A named design names every gun built from it.
        if (blueprint.has(DataComponents.CUSTOM_NAME))
            gun.set(DataComponents.CUSTOM_NAME, blueprint.get(DataComponents.CUSTOM_NAME));
        return gun;
    }

    /** The design the grid builds, with the parts' materials and the barrel's shape, or null. */
    @Nullable
    private static GunDesign design(CraftingInput input, ItemStack blueprint, Map<ResourceLocation, Material> materials) {
        GunDesign design = blueprint.get(FWeapons.GUN_DESIGN.get());
        Map<String, Integer> wanted = design.assembly();
        // A coil's winding is consolidated redstone, wound from blocks of it, on top of the parts.
        Item winding = design.windingBlocks() > 0 ? GunDesign.windingItem(materials).orElse(null) : null;
        if (design.windingBlocks() > 0 && winding == null)
            return null;
        Map<String, Integer> found = new HashMap<>();
        Map<String, ResourceLocation> metal = new HashMap<>();
        BarrelGeometry barrel = null;
        GunDesign.SecondBarrel second = null;
        ResourceLocation wrap = null;
        int windings = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty() || s == blueprint)
                continue;
            if (winding != null && s.is(winding)) {
                windings++;
                continue;
            }
            var located = Material.find(materials, s);
            var part = located.filter(l -> wanted.containsKey(l.form())
                    && (l.form().equals("grip") ? GunDesign.gripMaterial(l.material()) : GunDesign.buildable(l.material())));
            // One more thing in the grid may be a wrap for the grip: leather, cloth, honey, by property.
            if (part.isEmpty() && wrap == null && located.filter(l -> GunDesign.wrapMaterial(l.material())).isPresent()) {
                wrap = located.get().id();
                continue;
            }
            if (part.isEmpty())
                return null;
            found.merge(part.get().form(), 1, Integer::sum);
            metal.putIfAbsent(part.get().form(), part.get().id());
            if (part.get().form().equals("gun_barrel")) {
                // A double's barrels are each their own: the first fills the design, the second its pair.
                BarrelGeometry shape = s.getOrDefault(FWeapons.BARREL_GEOMETRY.get(), BarrelGeometry.STOCK);
                if (barrel == null)
                    barrel = shape;
                else
                    second = new GunDesign.SecondBarrel(part.get().id(), shape);
            }
        }
        if (!found.equals(wanted) || windings != design.windingBlocks())
            return null;
        ResourceLocation receiver = metal.get("receiver");
        ResourceLocation action = design.actionPart().map(metal::get).orElse(receiver);
        var feed = Optional.ofNullable(metal.getOrDefault("cylinder", metal.get("magazine_tube")));
        // Each fitting weighs what its part is made of, whatever the paper said.
        for (GunDesign.Fitting f : design.fittings().keySet())
            design = design.withFitting(f, metal.get(f.form()));
        return design.withMaterial(metal.get("gun_barrel"))
                .withParts(new GunDesign.Parts(action, metal.get("trigger_group"), metal.get("grip"), receiver, feed,
                        Optional.ofNullable(second), Optional.ofNullable(wrap)))
                .withBuilt(barrel);
    }

    /** What the grid holds: the gun, and the one kind of part going on it, at {@code partSlot}. */
    public record Refit(ItemStack gun, GunDesign refitted, int partSlot, Optional<ItemStack> displaced) {
    }

    @Nullable
    public static Refit refit(CraftingInput input, Map<ResourceLocation, Material> materials) {
        boolean held = false;
        for (int i = 0; i < input.size(); i++)
            held |= isGun(input.getItem(i));
        if (!held)
            return null;
        int gunSlot = -1, partSlot = -1, plates = 0;
        GunDesign.Fitting fitting = null;
        ResourceLocation fittingMetal = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty())
                continue;
            if (isGun(s)) {
                if (gunSlot >= 0)
                    return null;
                gunSlot = i;
                continue;
            }
            var part = Material.find(materials, s).filter(l -> GunDesign.buildable(l.material()));
            if (part.isEmpty())
                return null;
            var f = GunDesign.Fitting.ofForm(part.get().form());
            if (f.isPresent() && fitting == null && plates == 0) {
                fitting = f.get();
                fittingMetal = part.get().id();
                partSlot = i;
            } else if (part.get().form().equals("plate") && fitting == null && plates < 2) {
                plates++;
                partSlot = partSlot < 0 ? i : partSlot;
            } else
                return null;
        }
        if (gunSlot < 0 || partSlot < 0)
            return null;
        ItemStack gun = input.getItem(gunSlot);
        GunDesign design = gun.get(FWeapons.GUN_DESIGN.get());
        if (fitting != null) {
            // A muzzle fitting displaces whichever muzzle fitting is on; the others only themselves.
            Optional<ItemStack> displaced = Optional.empty();
            for (var on : design.fittings().entrySet())
                if (on.getKey() == fitting || (on.getKey().muzzle() && fitting.muzzle()))
                    displaced = stack(materials, on.getValue(), on.getKey().form(), 1);
            return new Refit(gun, design.withFitting(fitting, fittingMetal), partSlot, displaced);
        }
        GunDesign.Sight sight = plates == 1 ? GunDesign.Sight.REFLEX : GunDesign.Sight.SCOPE;
        if (sight == design.sight())
            return null;
        // A sight's plates were never told apart by metal; what comes off is the receiver's, which it sits on.
        Optional<ItemStack> displaced = design.sight().plates() == 0 ? Optional.empty()
                : stack(materials, design.parts().receiver(), "plate", design.sight().plates());
        return new Refit(gun, design.withSight(sight), partSlot, displaced);
    }

    /** The refitted gun: the same stack with the new design, its figures dropped to be baked again. */
    public static ItemStack refitted(Refit r) {
        ItemStack out = r.gun().copyWithCount(1);
        out.set(FWeapons.GUN_DESIGN.get(), r.refitted());
        out.remove(FWeapons.GUN_FIGURES.get());
        return out;
    }

    private static Optional<ItemStack> stack(Map<ResourceLocation, Material> materials, ResourceLocation metal, String form, int count) {
        Material m = materials.get(metal);
        return m == null ? Optional.empty() : m.stack(metal, form).map(s -> s.copyWithCount(count));
    }
}
