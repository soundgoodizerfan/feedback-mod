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

import net.minecraft.world.item.ItemStack;

/**
 * What a gun stack looks like, read off its design: the receiver type, the action, the barrel's
 * real length and diameter, which parts are fitted and each part's material colour. Everything the
 * renderer and the motion read; nothing here is stored.
 *
 * <p>Units are centimetres. The frame: the bore axis runs along z with the muzzle toward -z, the
 * breech face at z = 0, y up, x to the gun's right.
 *
 * @param action   the action part's variant ({@code self_loading}, {@code bolt}, {@code lever}, ...),
 *                 or {@code none}
 * @param slide    whether the action is a slide (role A) rather than a bolt group (role K)
 * @param len      barrel length
 * @param r        barrel radius (half the outer diameter)
 * @param bore     bore radius
 * @param mag      the detachable magazine in the gun, or null
 * @param rounds   rounds held, chamber included
 */
record GunView(GunLayout.Type type, String action, boolean slide, float len, float r, float bore, float len2, float r2,
               Map<String, Integer> colours, Map<String, String> variants, MagazineSize mag, int rounds) {

    private static final int BARE = 0xFFB4B4B8;

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
            return new GunView(GunLayout.Type.HANDGUN, "self_loading", true, 12f, 0.75f, 0.45f, 0, 0, colours, variants,
                    mag, rounds);
        }
        for (ModularDesign.Part p : design.parts()) {
            colours.put(p.role(), 0xFF000000 | MaterialColors.of(p.material()));
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
        return new GunView(kind, action, slide, b[0], b[1], b[2], d[0], d[1], colours, variants, mag, rounds);
    }

    /** {length, radius, bore radius} in centimetres, zeros when the role is empty. */
    private static float[] barrel(ModularType type, String letter, ModularDesign.Part part) {
        ModularType.Role role = type.roles().get(letter);
        if (part == null || role == null)
            return new float[3];
        float length = role.dim(part, BarrelGeometry.LENGTH) * 100f;
        float outer = role.dim(part, BarrelGeometry.OUTER) / 10f;
        float bore = role.dim(part, BarrelGeometry.BORE) / 10f;
        return new float[]{Math.max(length, 3f), Math.max(outer / 2f, 0.3f), Math.max(bore / 2f, 0.1f)};
    }

    boolean has(String letter) {
        return colours.containsKey(letter);
    }

    int colour(String letter) {
        return colours.getOrDefault(letter, BARE);
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
            case HANDGUN -> new float[]{0, -5.6f, 4.8f};
            case RIFLE -> new float[]{0, -3.4f, 16.5f};
            case SHOTGUN -> new float[]{0, -3.9f, 14.5f};
            case MACHINE_GUN -> new float[]{0, -5.6f, 19.2f};
        };
    }

    /** Top of the receiver or slide, where a sight sits. */
    float top() {
        return switch (type) {
            case HANDGUN -> openFrame() ? 1.35f : 1.7f;
            case RIFLE -> slide ? 2.4f : 1.6f;
            case SHOTGUN -> 1.9f;
            case MACHINE_GUN -> 3.1f;
        };
    }

    /** The slide's front, a handgun's: it covers the barrel up to here. */
    float slideFront() {
        return Math.max(7.5f, Math.min(len, 12f));
    }

    /** Where the muzzle is: the barrel's end, or a pistol slide's face when the barrel ends inside it. */
    float muzzle() {
        return type == GunLayout.Type.HANDGUN && slide ? Math.max(len, slideFront()) : len;
    }

    /** The height of the line of sight above the bore: what aiming lines up with the middle of the screen. */
    float sightLine() {
        if (has("S"))
            return variant("S").equals("scope") ? top() + 1.9f : top() + 1.45f;
        return type == GunLayout.Type.HANDGUN ? top() + 0.5f : r + 1.0f;
    }

    /** How far forward of the muzzle the furthest muzzle device reaches. */
    float muzzleReach() {
        float reach = 0;
        if (has("N"))
            reach += 4f;
        if (has("M"))
            reach += 13f;
        if (has("a"))
            reach = Math.max(reach, 16f);
        return reach;
    }

    /** Rearmost point: slide back, stock butt. */
    float rear() {
        return switch (type) {
            case HANDGUN -> has("d") ? 9f : 7.3f;
            case RIFLE -> 44.4f;
            case SHOTGUN -> 42.4f;
            case MACHINE_GUN -> 39f;
        };
    }

    /** Lowest point: grip butt or the bottom of the magazine. */
    float bottom() {
        float base = switch (type) {
            case HANDGUN -> -12f;
            case RIFLE, SHOTGUN -> -9f;
            case MACHINE_GUN -> -12.5f;
        };
        if (boxMagazine() && longGun())
            base = Math.min(base, -1.6f - magLength() - 2f);
        return base;
    }

    /** A box magazine's body length below its well. */
    float magLength() {
        if (mag == null)
            return 0;
        return switch (mag) {
            case SMALL -> type == GunLayout.Type.HANDGUN ? 0f : 4f;
            case STANDARD -> type == GunLayout.Type.HANDGUN ? 0.6f : 7f;
            case EXTENDED -> type == GunLayout.Type.HANDGUN ? 5f : 12f;
            case DRUM, BELT -> type == GunLayout.Type.HANDGUN ? 0.6f : 7f;
        };
    }
}
