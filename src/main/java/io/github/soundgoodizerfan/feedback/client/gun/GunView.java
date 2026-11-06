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
package io.github.soundgoodizerfan.feedback.client.gun;

import java.util.HashMap;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.modular.MaterialColors;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.BarrelGeometry;
import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunLayout;
import io.github.soundgoodizerfan.feedback.weapon.MagazineSize;

import io.github.soundgoodizerfan.feedback.client.GlowColours;
import io.github.soundgoodizerfan.feedback.core.thermal.Incandescence;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * What a gun stack looks like, read off its design: the receiver type, the action, the barrel's
 * real length and diameter, which parts are fitted and each part's material colour. Everything the
 * renderer and the motion read; nothing here is stored.
 *
 * <p>Units are model pixels (1/16 block) on a quarter-pixel grid. The frame: the bore axis runs along
 * z with the muzzle toward -z, the rear of the receiver near z = 0, y up, x to the gun's right.
 *
 * @param action   the action part's variant ({@code self_loading}, {@code bolt}, {@code lever}, ...),
 *                 or {@code none}
 * @param slide    whether the action is a slide (role A) rather than a bolt group (role K)
 * @param len      barrel length
 * @param r        barrel radius (half the outer diameter)
 * @param bore     bore radius
 * @param mag      the detachable magazine in the gun, or null
 * @param surfaces each part's surface kind, from its material
 * @param woods    each wooden part's own bark, a low-contrast copy of its log's side ({@link GunWood})
 * @param glow     the barrel's glow as a block light level (0 cold): {@code Incandescence} of the gun's heat
 * @param rounds   rounds held, chamber included
 */
record GunView(GunLayout.Type type, String action, boolean slide, float len, float r, float bore, float len2, float r2,
               Map<String, Integer> colours, Map<String, String> variants, Map<String, Surface> surfaces, Map<String, ResourceLocation> woods, int glow,
               MagazineSize mag, int rounds) {

    private static final int BARE = 0xFFB4B4B8;

    /** What a part's material looks like on the surface, which picks its tile: grain only on wood, brushing only on metal. */
    enum Surface {
        METAL, WOOD, POLYMER, OTHER;

        static Surface of(Material m) {
            if (m == null)
                return METAL;
            Material.Kind kind = m.kind().orElse(Material.Kind.METAL);
            if (kind == Material.Kind.METAL || kind == Material.Kind.METALLOID)
                return METAL;
            if (kind == Material.Kind.ORGANIC && m.traits().contains(Material.Trait.FIBROUS))
                return WOOD;
            return kind == Material.Kind.POLYMER ? POLYMER : OTHER;
        }
    }

    /** The barrel and what sits on it: the parts that take the bore's heat and glow with it. */
    private static final java.util.Set<String> HOT = java.util.Set.of("B", "D", "M", "N", "Q", "Z", "P", "V", "H", "J");

    static GunView of(ItemStack stack) {
        ModularDesign design = ModularStacks.design(stack);
        ModularType type = design == null ? null : ModularTypes.known().get(design.type());
        Map<String, Integer> colours = new HashMap<>();
        Map<String, String> variants = new HashMap<>();
        MagazineSize mag = stack.get(FWeapons.MAGAZINE_SIZE.get());
        int rounds = GunAmmo.count(stack);
        if (design == null || type == null) {
            // A bare gun item (creative tab): a plain self-loading pistol, unpainted.
            for (String k : new String[]{"R", "B", "A", "T", "G"})
                colours.put(k, BARE);
            return new GunView(GunLayout.Type.HANDGUN, "self_loading", true, 4f, 0.625f, 0.25f, 0, 0, colours, variants,
                    Map.of(), Map.of(), 0, mag, rounds);
        }
        Map<String, Surface> surfaces = new HashMap<>();
        Map<String, ResourceLocation> woods = new HashMap<>();
        Map<ResourceLocation, Material> materials = ClientMaterials.get();
        var level = Minecraft.getInstance().level;
        float tu = level == null ? 0f : GunCondition.temperature(stack, level).value();
        int glow = Incandescence.light(tu);
        for (ModularDesign.Part p : design.parts()) {
            Material material = materials.get(p.material());
            Surface surface = Surface.of(material);
            surfaces.put(p.role(), surface);
            if (surface == Surface.WOOD) {
                ResourceLocation bark = GunWood.texture(material, p.form());
                if (bark != null)
                    woods.put(p.role(), bark);
            }
            int rgb = MaterialColors.of(p.material());
            if (glow > 0 && HOT.contains(p.role()) && material != null && Incandescence.glows(material))
                rgb = GlowColours.heated(rgb, tu);
            colours.put(p.role(), 0xFF000000 | rgb);
            ModularType.Role role = type.roles().get(p.role());
            String variant = p.variant();
            if (variant.isEmpty() && role != null)
                variant = role.part().map(s -> s.base()).orElse("");
            variants.put(p.role(), variant);
        }
        GunLayout.Type kind = GunLayout.Type.of(variants.getOrDefault("R", ""));
        boolean slide = colours.containsKey("A");
        String action = slide ? variants.getOrDefault("A", "self_loading")
                : colours.containsKey("K") ? variants.getOrDefault("K", "bolt") : "none";
        float[] b = barrel(type, "B", design.part("B"));
        float[] d = barrel(type, "D", design.part("D"));
        return new GunView(kind, action, slide, b[0], b[1], b[2], d[0], d[1], colours, variants, surfaces, woods, glow, mag, rounds);
    }

    /**
     * {length, radius, bore radius} in model pixels (1/16 block), zeros when the role is empty. Guns
     * are drawn in Minecraft's own proportions, not to scale: a barrel's length maps to 20 px per metre
     * and its diameter to a quarter pixel per millimetre over a floor, so a pistol is about 9 px long
     * and a rifle 25 to 30 (numbers taken from Scorched Guns' models, GPL-3.0, read for proportions
     * only). Radii snap to an eighth of a pixel so every face lands on the half-pixel texel grid.
     */
    private static float[] barrel(ModularType type, String letter, ModularDesign.Part part) {
        ModularType.Role role = type.roles().get(letter);
        if (part == null || role == null)
            return new float[3];
        float metres = role.dim(part, BarrelGeometry.LENGTH);
        float outerMm = role.dim(part, BarrelGeometry.OUTER), boreMm = role.dim(part, BarrelGeometry.BORE);
        float length = snap(Math.max(1.5f, Math.min(metres * 20f, 16f)), 0.25f);
        float r = snap(Math.max(0.5f, Math.min(0.25f + outerMm * 0.025f, 1.25f)), 0.125f);
        float bore = snap(Math.max(0.125f, Math.min(boreMm * 0.02f, r - 0.125f)), 0.125f);
        return new float[]{length, r, bore};
    }

    static float snap(float v, float grid) {
        return Math.round(v / grid) * grid;
    }

    boolean has(String letter) {
        return colours.containsKey(letter);
    }

    int colour(String letter) {
        return colours.getOrDefault(letter, BARE);
    }

    Surface surface(String letter) {
        return surfaces.getOrDefault(letter, Surface.METAL);
    }

    /** The bark image a wooden part draws with, or null for the sheet's grain. */
    ResourceLocation wood(String letter) {
        return woods.get(letter);
    }

    String variant(String letter) {
        return variants.getOrDefault(letter, "");
    }

    boolean longGun() {
        return type != GunLayout.Type.HANDGUN;
    }

    /** A handgun on a hammer action with no slide: a revolver or derringer frame. */
    boolean openFrame() {
        return type == GunLayout.Type.HANDGUN && !slide;
    }

    /** Whether the action cycles itself, so it locks open on the last round. */
    boolean selfLoading() {
        return action.equals("self_loading") || action.equals("automatic");
    }

    /** The detachable box magazine, if one is in and the gun takes one. */
    boolean boxMagazine() {
        return mag != null && mag.feed() == GunLayout.Feed.BOX && !has("F") && !has("E") && !has("b");
    }

    /** Where the hand holds the gun: the grip's middle, or a long gun's wrist. */
    float[] hand() {
        return switch (type) {
            case HANDGUN -> openFrame() ? new float[]{0, -3.25f, 1.25f} : new float[]{0, -3.25f, 0f};
            case RIFLE -> new float[]{0, -1.5f, 6.75f};
            case SHOTGUN -> new float[]{0, -1.75f, 6.25f};
            case MACHINE_GUN -> new float[]{0, -2.75f, 7.75f};
        };
    }

    /** Half the receiver's width. */
    float halfWidth() {
        return switch (type) {
            case HANDGUN -> 0.875f;
            case RIFLE -> 0.75f;
            case SHOTGUN -> 0.875f;
            case MACHINE_GUN -> 1f;
        };
    }

    /** Rear of the receiver, where a stock starts. */
    float back() {
        return switch (type) {
            case HANDGUN -> 0.5f;
            case RIFLE -> 5.5f;
            case SHOTGUN -> 5f;
            case MACHINE_GUN -> 9f;
        };
    }

    /** Top of the receiver or slide, where a sight sits. */
    float top() {
        return switch (type) {
            case HANDGUN -> openFrame() ? 1f : 0.75f;
            case RIFLE -> slide ? 1.5f : 1f;
            case SHOTGUN -> 1f;
            case MACHINE_GUN -> 1.75f;
        };
    }

    /** A pistol slide's front: the slide covers the barrel up to here. */
    float slideFront() {
        return 6f;
    }

    /** Where the muzzle is: the barrel's end, or a pistol slide's face when the barrel ends inside it. */
    float muzzle() {
        if (type == GunLayout.Type.HANDGUN)
            return slide ? Math.max(len, slideFront()) : len + 3.5f;
        return len;
    }

    /** Front of an optic's mount. */
    float opticZ() {
        return switch (type) {
            case HANDGUN -> openFrame() ? -1.5f : -2f;
            case RIFLE -> 0.5f;
            case SHOTGUN -> 0.25f;
            case MACHINE_GUN -> 1.75f;
        };
    }

    float scopeLength() {
        return type == GunLayout.Type.HANDGUN ? 3f : 4.5f;
    }

    /** Front of a long gun's rear iron sight. */
    float rearSight() {
        return type == GunLayout.Type.MACHINE_GUN ? back() - 3.5f : back() - 2f;
    }

    /**
     * Where the eye goes when aimed, in model pixels: on the line of sight, a little behind the rear
     * sight or the optic's eyepiece. Aiming moves the gun so this point is the camera, the way Scorched
     * Guns places its iron-sight and scope cameras: so the eye looks down the sights and the receiver is
     * behind it, never filling the screen.
     */
    float[] eye() {
        float z;
        if (has("S"))
            z = variant("S").equals("scope") ? opticZ() - 0.5f + scopeLength() + 1f + 2.5f : opticZ() + 3.5f;
        else if (type == GunLayout.Type.HANDGUN)
            z = openFrame() ? 3.5f : 3.5f;
        else
            z = rearSight() + 3.5f;
        return new float[]{0f, sightLine(), z};
    }

    /** The height of the line of sight above the bore: what aiming lines up with the middle of the screen. */
    float sightLine() {
        if (has("S"))
            return variant("S").equals("scope") ? top() + 1f : top() + 0.75f;
        if (type == GunLayout.Type.HANDGUN)
            return openFrame() ? top() + 0.0625f : top() + 0.1875f;
        return top() + 0.25f;
    }

    /** How far forward of the muzzle the furthest muzzle device reaches. */
    float muzzleReach() {
        float reach = 0;
        if (has("N"))
            reach += 1.5f;
        if (has("M"))
            reach += 4.5f;
        if (has("a"))
            reach = Math.max(reach, 6.5f);
        return reach;
    }

    /** Rearmost point: slide back, stock butt. */
    float rear() {
        return type == GunLayout.Type.HANDGUN ? 1.75f : back() + 10f;
    }

    /** Lowest point: grip butt or the bottom of the magazine. */
    float bottom() {
        float base = type == GunLayout.Type.HANDGUN ? -5.75f : -4f;
        if (boxMagazine() && longGun())
            base = Math.min(base, -1f - magLength() - 0.5f);
        return base;
    }

    /** A box magazine's body length below its well (a handgun's hangs below the grip only when extended). */
    float magLength() {
        if (mag == null)
            return 0;
        boolean pistol = type == GunLayout.Type.HANDGUN;
        return switch (mag) {
            case SMALL -> pistol ? 0f : 1.5f;
            case STANDARD, DRUM, BELT -> pistol ? 0f : 2.5f;
            case EXTENDED -> pistol ? 2f : 4.5f;
        };
    }
}
