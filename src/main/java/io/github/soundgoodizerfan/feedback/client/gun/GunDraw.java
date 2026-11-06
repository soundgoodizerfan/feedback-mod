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

import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.client.gun.GunMesh.Tile;
import io.github.soundgoodizerfan.feedback.weapon.GunLayout;
import io.github.soundgoodizerfan.feedback.weapon.MagazineSize;

/**
 * The gun's shape, part by part ({@link GunView}): each fitted part is its own small shape in its
 * own material's colour, placed by the receiver type and sized by the part's real measures (the
 * barrel at its length and diameter, the magazine by its size band). Internal parts nobody can see
 * (sears, obturators, a resonance weight's threads aside from its collar) are not drawn. Moving
 * parts take their place from {@link GunMotion.Parts}. Centimetres, on the {@link GunView} frame.
 */
final class GunDraw {

    private GunDraw() {
    }

    // --- the gun, part by part ---------------------------------------------------------------------

    static void draw(GunMesh m, GunView v, GunMotion.Parts p) {
        switch (v.type()) {
            case HANDGUN -> handgun(m, v, p);
            case RIFLE, SHOTGUN -> longGun(m, v, p);
            case MACHINE_GUN -> machineGun(m, v, p);
        }
        // A break action's barrels and everything on them tip about the hinge under the breech.
        m.push();
        if (p.breakOpen() > 0)
            m.turnX(-38f * p.breakOpen(), -v.r() - (v.has("D") ? 2 * v.r2() : 0) - 0.4f, -0.6f);
        barrels(m, v);
        muzzle(m, v);
        underBarrel(m, v, p);
        m.pop();
        sight(m, v);
        sides(m, v);
    }

    private static void handgun(GunMesh m, GunView v, GunMotion.Parts p) {
        float sl = v.slideFront();
        int frame = v.colour("R");
        boolean revolver = v.openFrame();
        float frameBottom = revolver && v.has("F") ? -3.6f : -2.7f;
        m.colour(frame);
        if (!revolver) {
            m.box(-1.05f, -2.7f, -sl + 0.6f, 1.05f, -0.85f, 6.0f, Tile.PANEL);
            m.box(-0.9f, -1.7f, 6.0f, 0.9f, -0.85f, 7.3f, Tile.METAL); // beavertail
            m.box(-0.75f, -2.75f, -sl + 0.8f, 0.75f, -2.6f, -2.8f, Tile.RIBBED); // rail
            // The slide, which runs back on the shot and stays back on the last round.
            m.push();
            m.move(0, 0, p.slide() * 3.2f);
            m.colour(v.colour("A"));
            m.box(-1.2f, -0.85f, -sl, 1.2f, 1.55f, 6.6f, Tile.METAL);
            m.box(-1.23f, -0.6f, 3.7f, 1.23f, 1.35f, 6.3f, Tile.RIBBED);
            m.box(-1.23f, -0.6f, -sl + 0.3f, 1.23f, 1.35f, -sl + 1.9f, Tile.RIBBED);
            m.box(-0.6f, 1.55f, -sl + 0.4f, 0.6f, 1.68f, 6.2f, Tile.BRUSHED);
            m.box(1.2f, 0.05f, -0.8f, 1.25f, 1.3f, 2.3f, Tile.BORE); // ejection port
            if (!v.has("S")) {
                m.box(-0.22f, 1.55f, -sl + 0.5f, 0.22f, 2.2f, -sl + 1.1f, Tile.METAL);
                m.box(-0.95f, 1.55f, 5.3f, -0.3f, 2.2f, 6.1f, Tile.METAL);
                m.box(0.3f, 1.55f, 5.3f, 0.95f, 2.2f, 6.1f, Tile.METAL);
                if (v.has("e"))
                    m.colour(0xFF40FF60).glow(-0.12f, 1.85f, -sl + 0.46f, 0.12f, 2.05f, -sl + 0.5f, Tile.LAMP);
            }
            m.pop();
        } else {
            boolean cylinder = v.has("F");
            if (cylinder) {
                m.box(-1.0f, -3.6f, -1.5f, 1.0f, -3.1f, 6.0f, Tile.PANEL);
                m.box(-0.9f, 0.65f, -1.5f, 0.9f, 1.35f, 4.2f, Tile.METAL);
                m.box(-1.1f, -3.1f, 4.2f, 1.1f, 1.35f, 6.2f, Tile.PANEL);
                m.box(-1.0f, -3.1f, -1.5f, 1.0f, 0.65f, 0.1f, Tile.METAL);
                m.box(-0.18f, 1.35f, 3.6f, 0.18f, 1.42f, 4.2f, Tile.BORE); // sight groove
                cylinder(m, v, p, 0f, -1.25f, 0.2f, 4.1f, 1.85f);
                m.colour(frame).box(-0.45f, -1.7f, -Math.min(v.len(), 7f), 0.45f, -0.85f, -1.5f, Tile.METAL);
            } else {
                m.box(-1.05f, -2.7f, -1.5f, 1.05f, 1.35f, 6.2f, Tile.PANEL);
            }
            // The hammer: cocked back while the action works, down on the shot.
            m.colour(v.has("K") ? v.colour("K") : frame);
            m.push();
            m.turnX(-30f * p.hammer(), 0.6f, 5.9f);
            m.box(-0.3f, 0.6f, 5.5f, 0.3f, 2.1f, 6.2f, Tile.METAL);
            m.box(-0.42f, 1.75f, 5.9f, 0.42f, 2.15f, 6.9f, Tile.KNURL);
            m.pop();
        }

        grip(m, v, p, 0f, frameBottom, revolver ? 5.0f : 4.3f, revolver ? 7.6f : 8.6f, revolver ? -24f : -16f, !revolver,
                revolver ? 0.8f : 1f);
        trigger(m, v, frameBottom, -2.4f, 2.0f);
    }

    /**
     * A pistol grip hung below {@code (y, z)}, raked by {@code rake} degrees, {@code length} long.
     * A magazine rides up inside it when {@code well}.
     */
    private static void grip(GunMesh m, GunView v, GunMotion.Parts p, float x, float y, float z, float length, float rake,
                             boolean well) {
        grip(m, v, p, x, y, z, length, rake, well, 1f);
    }

    private static void grip(GunMesh m, GunView v, GunMotion.Parts p, float x, float y, float z, float length, float rake,
                             boolean well, float depth) {
        m.push();
        m.turnX(rake, y, z);
        m.colour(v.has("G") ? v.colour("G") : v.colour("R"));
        float f = 2.4f * depth, b = 2.2f * depth;
        m.box(x - 1.25f, y - length, z - f, x + 1.25f, y, z + b, Tile.PANEL);
        m.box(x - 1.36f, y - length + 0.8f, z - f + 0.4f, x + 1.36f, y - 0.9f, z + b - 0.5f, Tile.CHECKER);
        m.box(x - 1.1f, y - length + 0.6f, z + b, x + 1.1f, y - 1.4f, z + b + 0.3f, Tile.RIBBED); // backstrap
        if (v.has("X")) {
            m.colour(v.colour("X"));
            for (int i = 0; i < 4; i++) {
                float yy = y - 1.6f - i * (length - 2.4f) / 3.5f;
                m.box(x - 1.42f, yy - 0.5f, z - 2.46f, x + 1.42f, yy, z + 2.28f, Tile.RUBBER);
            }
        }
        if (well && (v.boxMagazine() || p.magDrop() > 0) && !p.magOut()) {
            float drop = p.magDrop() * 14f;
            float ext = v.magLength();
            m.push();
            m.move(0, -drop, 0);
            m.colour(magColour(v));
            if (v.mag() == MagazineSize.DRUM) {
                m.push();
                m.move(x, y - length - 2.6f, z);
                m.pose.mulPose(Axis.YP.rotationDegrees(90));
                m.rod(0, 0, 2.7f, -1.2f, 1.2f, Tile.METAL);
                m.pop();
            } else {
                // The body rides inside the grip, seen only as it drops out or where an extended one hangs below.
                m.box(x - 0.95f, y - length - ext, z - 1.9f, x + 0.95f, y - 0.6f, z + 1.5f, Tile.RIBBED);
                m.box(x - 1.2f, y - length - ext - 0.5f, z - 2.3f, x + 1.2f, y - length - ext, z + 1.9f, Tile.METAL);
            }
            m.pop();
        }
        m.pop();
    }

    /** The magazine's colour: the gun says nothing of its material, so it is drawn in blued steel. */
    private static int magColour(GunView v) {
        return 0xFF5A5E66;
    }

    /** The guard and blade ahead of a grip, under {@code y}, between {@code z0} and {@code z1}. */
    private static void trigger(GunMesh m, GunView v, float y, float z0, float z1) {
        m.colour(v.has("T") ? v.colour("T") : v.colour("R"));
        float drop = 2.0f;
        m.box(-0.35f, y - drop, z0, 0.35f, y, z0 + 0.5f, Tile.METAL);
        m.box(-0.35f, y - drop - 0.15f, z0, 0.35f, y - drop + 0.35f, z1, Tile.METAL);
        float tz = z0 + (z1 - z0) * 0.55f;
        m.push();
        m.turnX(-12f, y, tz);
        m.box(-0.25f, y - 1.35f, tz - 0.25f, 0.25f, y, tz + 0.2f, Tile.BRUSHED);
        m.pop();
    }

    /** A revolving cylinder on axis (x, y), chambers round it; it swings out to the left to load. */
    private static void cylinder(GunMesh m, GunView v, GunMotion.Parts p, float x, float y, float z0, float z1, float r) {
        m.push();
        m.colour(v.colour("F"));
        m.move(-2.6f * p.cylinderOut(), 0, 0);
        m.turnZ(p.cylinderTurn(), x, y);
        m.rod(x, y, r, z0, z1, Tile.BRUSHED);
        // Flutes and chamber mouths: six round the axis.
        for (int i = 0; i < 6; i++) {
            m.push();
            m.turnZ(i * 60f + 30f, x, y);
            m.box(x - 0.18f, y + r - 0.08f, z0 + 0.9f, x + 0.18f, y + r + 0.02f, z1 - 0.9f, Tile.BORE);
            m.box(x - 0.42f, y + r * 0.5f - 0.42f, z0 - 0.02f, x + 0.42f, y + r * 0.5f + 0.42f, z0 + 0.01f, Tile.BORE);
            m.pop();
        }
        m.pop();
    }

    private static void longGun(GunMesh m, GunView v, GunMotion.Parts p) {
        boolean shotgun = v.type() == GunLayout.Type.SHOTGUN;
        float hw = shotgun ? 1.5f : 1.25f, lo = shotgun ? -2.2f : -1.6f, hi = shotgun ? 1.9f : 1.6f;
        float back = shotgun ? 12f : 14f;
        int receiver = v.colour("R");
        m.colour(receiver);
        m.box(-hw, lo, -1f, hw, hi, back, Tile.PANEL);
        m.box(-hw * 0.7f, hi, 0f, hw * 0.7f, hi + 0.25f, back - 1f, Tile.BRUSHED);
        m.box(hw, -0.3f, 2f, hw + 0.04f, 1.0f, 7.5f, Tile.BORE); // ejection port

        // Furniture: the forend under the barrel is in the stock's material.
        int furniture = v.has("G") ? v.colour("G") : receiver;
        float under = barrelsBottom(v);
        float fe = Math.min(Math.max(v.len() * 0.55f + 2f, 8f), 32f);
        if (v.action().equals("pump")) {
            m.push();
            m.move(0, 0, p.pump() * 7f);
            m.colour(v.colour("K"));
            m.box(-1.7f, under - 2.6f, -fe, 1.7f, under + 0.4f, -fe * 0.4f, Tile.RIBBED);
            m.pop();
            m.colour(receiver).box(-0.3f, under - 0.6f, -fe * 0.4f, 0.3f, under, -1f, Tile.METAL); // action bar
        } else {
            m.colour(furniture).box(-1.55f, under - 2.2f, -fe, 1.55f, under + 0.6f, -1f, Tile.GRAIN);
        }

        // The stock: wrist, comb, the butt's sloped underside, a pad.
        m.colour(furniture);
        m.push();
        m.turnX(-8f, lo, back);
        m.box(-1.3f, lo - 3.2f, back, 1.3f, hi - 0.6f, back + 7f, Tile.GRAIN);
        m.pop();
        float butt = back + 30f;
        m.box(-1.5f, -1.3f, back + 6f, 1.5f, 1.2f, butt, Tile.GRAIN);
        // The butt deepens toward the shoulder: a block hung from the comb, tipped so its top stays inside it.
        m.push();
        m.turnX(6f, -1.3f, butt);
        m.box(-1.48f, -8.2f, back + 6.5f, 1.48f, -1.3f, butt, Tile.GRAIN);
        m.pop();
        m.box(-1.5f, -1.3f, butt - 0.01f, 1.5f, 1.2f, butt + 0.01f, Tile.END_GRAIN);
        m.colour(darken(furniture)).box(-1.56f, -8.3f, butt, 1.56f, 1.26f, butt + 0.8f, Tile.RUBBER);

        action(m, v, p, hw, hi, back);
        trigger(m, v, lo, back * 0.42f, back * 0.42f + 5.4f);
        feed(m, v, p, lo, 1.4f, 6.2f);
    }

    private static void machineGun(GunMesh m, GunView v, GunMotion.Parts p) {
        int receiver = v.colour("R");
        m.colour(receiver);
        m.box(-1.7f, -2.2f, -2f, 1.7f, 2.6f, 22f, Tile.PANEL);
        m.box(1.7f, -0.6f, 2f, 1.74f, 1.2f, 9f, Tile.BORE);
        // Top cover, hinged at the back; it lifts to lay a belt.
        m.push();
        m.turnX(-55f * p.cover(), 2.6f, 16f);
        m.box(-1.75f, 2.6f, 3.5f, 1.75f, 3.1f, 16f, Tile.METAL);
        m.pop();
        if (!v.has("S")) {
            m.box(-0.5f, 3.1f, 6f, 0.5f, 4.6f, 7f, Tile.METAL);
            m.box(-0.5f, 3.1f, 13f, 0.5f, 4.6f, 14f, Tile.METAL);
            m.box(-0.55f, 4.6f, 5.6f, 0.55f, 5.3f, 14.4f, Tile.KNURL);
        }
        // Skeleton buttstock in the receiver's metal, a pad at the shoulder.
        m.box(-1.2f, 0.2f, 22f, 1.2f, 1.6f, 38f, Tile.BRUSHED);
        m.push();
        m.turnX(10f, -3.5f, 38f);
        m.box(-1.2f, -4.6f, 25f, 1.2f, -3.3f, 38f, Tile.BRUSHED);
        m.pop();
        m.box(-1.2f, -3.5f, 36.5f, 1.2f, 0.2f, 38f, Tile.METAL);
        m.colour(darken(receiver)).box(-1.4f, -4.2f, 38f, 1.4f, 1.9f, 39f, Tile.RUBBER);
        // Ventilated handguard over the barrel's root.
        float under = barrelsBottom(v);
        float fe = Math.min(Math.max(v.len() * 0.5f, 8f), 26f);
        m.colour(v.has("G") ? v.colour("G") : receiver).box(-1.6f, under - 2.0f, -fe, 1.6f, 1.2f, -2f, Tile.VENT);

        action(m, v, p, 1.7f, 2.6f, 22f);
        grip(m, v, p, 0f, -2.2f, 19.5f, 7.4f, -14f, false);
        trigger(m, v, -2.2f, 13.5f, 18.4f);
        feed(m, v, p, -2.2f, 3f, 8.5f);
    }

    /** The action's visible bits on a long gun: a bolt handle, a lever loop, a charging handle, a top lever. */
    private static void action(GunMesh m, GunView v, GunMotion.Parts p, float hw, float hi, float back) {
        int c = v.has("A") ? v.colour("A") : v.colour("K");
        m.colour(c);
        switch (v.action()) {
            case "bolt" -> {
                // Lifts a quarter turn about the bore, then draws back.
                m.push();
                m.move(0, 0, p.boltBack() * 7f);
                m.turnZ(-65f * p.boltLift(), 0, 0);
                m.box(hw - 0.2f, -0.2f, back - 3.6f, hw + 2.1f, 0.35f, back - 3.0f, Tile.METAL);
                m.box(hw + 1.7f, -0.55f, back - 3.8f, hw + 2.6f, 0.45f, back - 2.8f, Tile.KNURL);
                m.box(-hw * 0.7f, hi - 0.1f, back - 6f, hw * 0.7f, hi + 0.15f, back - 0.3f, Tile.BRUSHED);
                m.pop();
            }
            case "lever" -> {
                m.push();
                m.turnX(48f * p.lever(), -1.6f, back * 0.42f - 0.5f);
                float z = back * 0.42f;
                m.box(-0.3f, -4.6f, z - 0.6f, 0.3f, -1.6f, z, Tile.METAL);
                m.box(-0.3f, -5.1f, z - 0.6f, 0.3f, -4.6f, z + 7.5f, Tile.METAL);
                m.box(-0.3f, -4.6f, z + 7f, 0.3f, -2.4f, z + 7.5f, Tile.METAL);
                m.pop();
            }
            case "break" -> {
                m.box(-0.35f, hi, back - 2.5f, 0.35f, hi + 0.35f, back + 0.5f, Tile.KNURL); // top lever
            }
            case "self_loading", "automatic" -> {
                float travel = v.type() == GunLayout.Type.MACHINE_GUN ? 6f : 5f;
                m.push();
                m.move(0, 0, p.slide() * travel);
                if (v.has("A") && v.type() != GunLayout.Type.MACHINE_GUN)
                    m.box(-1.0f, hi - 0.05f, 0f, 1.0f, hi + 0.8f, back - 2f, Tile.METAL); // carrier cover
                m.box(hw - 0.1f, 0.2f, back * 0.55f, hw + 1.1f, 0.75f, back * 0.55f + 0.9f, Tile.KNURL); // charging handle
                m.box(hw - 0.05f, -0.2f, 2.4f, hw + 0.02f, 0.9f, 7f, Tile.BRUSHED); // bolt face in the port
                m.pop();
            }
            default -> {
            }
        }
    }

    /** What feeds the gun below or beside the receiver: a box magazine, a fixed box, a belt and its can. */
    private static void feed(GunMesh m, GunView v, GunMotion.Parts p, float lo, float z0, float z1) {
        if (v.has("b")) {
            m.colour(v.colour("b")).box(-1.15f, lo - 6.5f, z0, 1.15f, lo, z1, Tile.PANEL);
            return;
        }
        if (v.has("Y") || v.mag() == MagazineSize.BELT) {
            m.colour(v.has("Y") ? v.colour("Y") : magColour(v));
            m.box(-1.25f - 5.2f, lo - 7f, z0 - 1f, -1.3f, lo + 1.5f, z1 + 2f, Tile.PANEL);
            m.box(-1.32f, lo + 1.2f, z0, -0.6f, lo + 2.6f, z1, Tile.LINKS);
            return;
        }
        if (v.has("F")) {
            cylinder(m, v, p, 0f, -1.25f, 0.2f, 4.4f, 2.1f);
            return;
        }
        if (!(v.boxMagazine() || p.magDrop() > 0) || p.magOut())
            return;
        float len = v.magLength();
        m.push();
        m.turnX(12f, lo, (z0 + z1) / 2);
        m.move(0, -p.magDrop() * 16f, 0);
        m.colour(magColour(v));
        if (v.mag() == MagazineSize.DRUM) {
            m.push();
            m.move(0, lo - 5.5f, (z0 + z1) / 2);
            m.pose.mulPose(Axis.YP.rotationDegrees(90));
            m.rod(0, 0, 4f, -2.2f, 2.2f, Tile.METAL);
            m.pop();
            m.box(-0.9f, lo - 2f, z0 + 0.4f, 0.9f, lo, z1 - 0.4f, Tile.RIBBED);
        } else {
            m.box(-0.85f, lo - len, z0 + 0.2f, 0.85f, lo, z1 - 0.2f, Tile.RIBBED);
            m.box(-1.0f, lo - len - 0.5f, z0, 1.0f, lo - len, z1, Tile.METAL);
        }
        m.pop();
    }

    // --- along the barrel ----------------------------------------------------------------------------

    /** Lowest point of the barrel or barrels. */
    private static float barrelsBottom(GunView v) {
        return v.has("D") ? -v.r() - 2 * v.r2() : -v.r();
    }

    private static void barrels(GunMesh m, GunView v) {
        float start = v.type() == GunLayout.Type.HANDGUN ? 0f : -1f;
        m.colour(v.colour("B"));
        m.rod(0, 0, v.r(), start, -v.len(), Tile.BRUSHED);
        if (v.has("D")) {
            float y = -v.r() - v.r2();
            m.colour(v.colour("D"));
            m.rod(0, y, v.r2(), start, -v.len2(), Tile.BRUSHED);
            m.box(-v.r2() * 0.4f, y - v.r2() * 0.4f, -v.len2() - 0.02f, v.r2() * 0.4f, y + v.r2() * 0.4f, -v.len2() + 0.01f, Tile.BORE);
        }
        // Iron front post at the muzzle on a long gun with no optic.
        if (v.longGun() && !v.has("S"))
            m.colour(v.colour("B")).box(-0.2f, v.r() - 0.1f, -v.len() + 0.6f, 0.2f, v.r() + 1.0f, -v.len() + 1.3f, Tile.METAL);
        if (v.longGun() && !v.has("S") && v.type() != GunLayout.Type.MACHINE_GUN) {
            m.colour(v.colour("R"));
            m.box(-0.8f, v.top(), 8.5f, -0.25f, v.top() + 0.8f, 9.3f, Tile.METAL);
            m.box(0.25f, v.top(), 8.5f, 0.8f, v.top() + 0.8f, 9.3f, Tile.METAL);
        }

        float r = v.r(), len = v.len();
        if (v.has("H"))
            m.colour(v.colour("H")).rod(0, 0, r + 0.55f, -1.2f, -Math.max(2f, len * 0.75f), Tile.VENT);
        if (v.has("Z")) {
            float end = -Math.min(len - 1f, 12f);
            m.colour(v.colour("Z"));
            m.rod(0, 0, r + 0.12f, -1.5f, end, Tile.METAL);
            m.rings(0, 0, r + 0.55f, -1.5f, end, (int) ((-end - 1.5f) / 0.9f), 0.35f, Tile.RIBBED);
        }
        if (v.has("J"))
            m.colour(v.colour("J")).rod(0, 0, r + 0.45f, -len * 0.3f, -len * 0.62f, Tile.PANEL);
        if (v.has("W")) {
            float end = -Math.min(len - 1f, 11f);
            m.colour(v.colour("W")).rings(0, 0, r + 0.6f, -1f, end, 7, 1.0f, Tile.COIL);
        }
        if (v.has("Q"))
            m.colour(v.colour("Q")).rod(0, 0, r + 0.4f, -len + 0.4f, -len + Math.min(6f, len * 0.4f), Tile.BRUSHED);
        if (v.has("P")) {
            float f = switch (v.variant("P")) {
                case "thread_breech" -> 0.18f;
                case "thread_muzzle" -> 0.88f;
                default -> 0.5f;
            };
            m.colour(v.colour("P")).rod(0, 0, r + 0.6f, -len * f - 0.9f, -len * f + 0.9f, Tile.KNURL);
        }
        if (v.has("V")) {
            m.colour(v.colour("V"));
            for (int i = 0; i < 3; i++) {
                float z = -len + 1.2f + i * 1.4f;
                if (z < -1.5f)
                    m.box(-0.38f, r - 0.1f, z, 0.38f, r + 0.35f, z + 0.8f, Tile.VENT);
            }
        }
    }

    /** Devices on the muzzle, front to front: a brake, then a can; the bore's mouth on the last. */
    private static void muzzle(GunMesh m, GunView v) {
        float z = -v.muzzle(), r = v.r();
        if (v.has("N")) {
            m.colour(v.colour("N"));
            m.rod(0, 0, r + 0.35f, z, z - 4f, Tile.METAL);
            for (int i = 0; i < 2; i++) {
                float zz = z - 1f - i * 1.4f;
                m.box(-r - 0.37f, -r * 0.55f, zz - 0.7f, r + 0.37f, r * 0.55f, zz, Tile.BORE);
            }
            z -= 4f;
        }
        if (v.has("M")) {
            float rr = Math.max(1.5f, r + 0.7f);
            m.colour(v.colour("M"));
            m.rod(0, 0, rr, z, z - 13f, Tile.BRUSHED);
            m.rod(0, 0, rr + 0.06f, z - 0.2f, z - 1f, Tile.KNURL);
            m.rod(0, 0, rr + 0.06f, z - 12.2f, z - 13f, Tile.KNURL);
            z -= 13f;
        }
        float b = v.bore() * (v.has("M") ? 1.3f : 1f);
        m.box(-b, -b, z - 0.02f, b, b, z + 0.01f, Tile.BORE);
    }

    /** Hung under the barrel: a tube magazine, a spring gun's spring, a counterweight, a bipod, a bayonet. */
    private static void underBarrel(GunMesh m, GunView v, GunMotion.Parts p) {
        float len = v.len(), under = barrelsBottom(v);
        float y = under - 0.85f;
        if (v.has("E")) {
            float end = -Math.min(Math.max(len * 0.85f, 6f), len - 1f);
            m.colour(v.colour("E"));
            m.rod(0, y, 0.82f, -1f, end, Tile.METAL);
            m.rod(0, y, 0.95f, end, end - 0.8f, Tile.KNURL);
            y -= 1.7f;
        }
        if (v.has("U")) {
            float rr = v.variant("U").equals("heavy_spring") ? 1.35f : 1.0f;
            m.colour(v.colour("U")).rod(0, y - rr + 0.8f, rr, -1f, -Math.min(len * 0.6f + 2f, 14f), Tile.COIL);
            y -= 2 * rr;
        }
        if (v.has("C"))
            m.colour(v.colour("C")).box(-0.9f, y - 1.6f, -len + 0.5f, 0.9f, y + 0.8f, -len + 5f, Tile.METAL);
        if (v.has("I")) {
            float zc = -len * 0.72f;
            m.colour(v.colour("I"));
            m.rod(0, 0, v.r() + 0.4f, zc + 0.6f, zc - 0.6f, Tile.KNURL);
            for (float side : new float[]{-1f, 1f}) {
                m.push();
                m.turnX(8f, under, zc);
                m.box(side * 0.35f - 0.2f, under - 0.9f, zc - 12f, side * 0.35f + 0.2f, under - 0.5f, zc, Tile.METAL);
                m.box(side * 0.35f - 0.35f, under - 1.1f, zc - 12.6f, side * 0.35f + 0.35f, under - 0.4f, zc - 11.8f, Tile.RUBBER);
                m.pop();
            }
        }
        if (v.has("a")) {
            m.colour(v.colour("a"));
            m.rod(0, 0, v.r() + 0.35f, -len + 0.2f, -len + 2.2f, Tile.METAL);
            m.box(-0.35f, under - 0.7f, -len + 0.5f, 0.35f, under + 0.2f, -len + 2.0f, Tile.METAL);
            m.box(-0.14f, under - 1.8f, -len - 14f, 0.14f, under - 0.6f, -len + 1f, Tile.BRUSHED);
            m.push();
            m.turnX(-24f, under - 0.6f, -len - 14f);
            m.box(-0.14f, under - 1.4f, -len - 16.2f, 0.14f, under - 0.6f, -len - 14f, Tile.BRUSHED);
            m.pop();
        }
    }

    /** An optic on the top, or a lit dot on the irons. */
    private static void sight(GunMesh m, GunView v) {
        if (!v.has("S"))
            return;
        float top = v.top();
        float z0 = switch (v.type()) {
            case HANDGUN -> v.openFrame() ? 1.0f : 2.0f;
            case RIFLE -> 2.5f;
            case SHOTGUN -> 1.5f;
            case MACHINE_GUN -> 6.5f;
        };
        int lamp = switch (v.variant("e")) {
            case "ring_reticle" -> 0xFFFFA030;
            case "cross_reticle" -> 0xFF40FF60;
            default -> 0xFFFF3030;
        };
        m.colour(v.colour("S"));
        if (v.variant("S").equals("scope")) {
            float len = v.type() == GunLayout.Type.HANDGUN ? 9f : 13f;
            float zs = v.type() == GunLayout.Type.HANDGUN ? z0 - 4f : z0 - 1f, ze = zs + len, y = top + 1.9f;
            m.box(-0.75f, top, zs + 1.5f, 0.75f, y, zs + 2.4f, Tile.METAL);
            m.box(-0.75f, top, ze - 2.4f, 0.75f, y, ze - 1.5f, Tile.METAL);
            m.rod(0, y, 0.95f, zs, ze, Tile.BRUSHED);
            m.rod(0, y, 1.4f, zs - 3f, zs, Tile.METAL);
            m.rod(0, y, 1.2f, ze, ze + 2.4f, Tile.METAL);
            m.box(-0.45f, y + 0.8f, (zs + ze) / 2 - 0.5f, 0.45f, y + 1.55f, (zs + ze) / 2 + 0.5f, Tile.KNURL);
            m.box(1.0f, y - 0.45f, (zs + ze) / 2 - 0.5f, 1.65f, y + 0.45f, (zs + ze) / 2 + 0.5f, Tile.KNURL);
            m.box(-1.15f, y - 1.15f, zs - 3.04f, 1.15f, y + 1.15f, zs - 3.0f, Tile.LENS);
            m.box(-0.95f, y - 0.95f, ze + 2.4f, 0.95f, y + 0.95f, ze + 2.44f, Tile.LENS);
            if (v.has("e"))
                m.colour(lamp).glow(-0.1f, y - 0.1f, ze + 2.44f, 0.1f, y + 0.1f, ze + 2.47f, Tile.LAMP);
        } else {
            m.box(-0.9f, top, z0, 0.9f, top + 0.5f, z0 + 3.2f, Tile.METAL);
            m.box(-1.0f, top + 0.5f, z0 + 0.2f, -0.78f, top + 2.6f, z0 + 1.1f, Tile.METAL);
            m.box(0.78f, top + 0.5f, z0 + 0.2f, 1.0f, top + 2.6f, z0 + 1.1f, Tile.METAL);
            m.box(-1.0f, top + 2.4f, z0 + 0.2f, 1.0f, top + 2.8f, z0 + 1.1f, Tile.METAL);
            m.box(-0.78f, top + 0.5f, z0 + 0.6f, 0.78f, top + 2.4f, z0 + 0.66f, Tile.GLASS);
            m.box(-0.3f, top + 0.5f, z0 + 2.2f, 0.3f, top + 1.0f, z0 + 3.0f, Tile.KNURL);
            if (v.has("e"))
                m.colour(lamp).glow(-0.11f, top + 1.34f, z0 + 0.67f, 0.11f, top + 1.56f, z0 + 0.7f, Tile.LAMP);
        }
    }

    /** On the sides: a selector, a steam chest with its valve and lagging, a hydraulic buffer. */
    private static void sides(GunMesh m, GunView v) {
        float hw = switch (v.type()) {
            case HANDGUN -> 1.1f;
            case RIFLE -> 1.25f;
            case SHOTGUN -> 1.5f;
            case MACHINE_GUN -> 1.7f;
        };
        float rear = switch (v.type()) {
            case HANDGUN -> 4.5f;
            case RIFLE -> 9.5f;
            case SHOTGUN -> 8.5f;
            case MACHINE_GUN -> 17f;
        };
        float base = v.type() == GunLayout.Type.HANDGUN ? -1.9f : -0.6f;
        if (v.has("L"))
            m.colour(v.colour("L")).box(-hw - 0.35f, base - 0.3f, rear - 0.9f, -hw, base + 0.3f, rear + 0.9f, Tile.KNURL);
        if (v.has("d"))
            m.colour(v.colour("d")).rod(-hw - 0.6f, base + 0.9f, 0.55f, rear + 1.2f, rear + 4.2f, Tile.BRUSHED);
        if (v.has("O")) {
            boolean large = v.variant("O").equals("large_chest");
            float rr = large ? 2.6f : 2.0f, len = large ? 15f : 10f, x = -hw - rr - 0.2f;
            float z0 = v.type() == GunLayout.Type.HANDGUN ? 1f : -1f;
            m.colour(v.colour("O"));
            m.rod(x, 0, rr, z0, z0 - len, Tile.METAL);
            m.rod(x, 0, rr + 0.1f, z0, z0 - 0.6f, Tile.PANEL);
            m.rod(x, 0, rr + 0.1f, z0 - len + 0.6f, z0 - len, Tile.PANEL);
            m.box(x + rr * 0.6f, -0.5f, z0 - 0.4f, -hw, 0.5f, z0 + 0.6f, Tile.METAL); // feed pipe
            if (v.has("h"))
                m.colour(v.colour("h")).rings(x, 0, rr + 0.18f, z0 - 1f, z0 - len + 1f, (int) (len / 1.3f), 0.8f, Tile.RUBBER);
            if (v.has("g")) {
                float zc = z0 - len * 0.5f;
                m.colour(v.colour("g"));
                m.box(x - 0.25f, rr, zc - 0.25f, x + 0.25f, rr + 0.9f, zc + 0.25f, Tile.METAL);
                if (v.variant("g").equals("dump_valve"))
                    m.box(x - 0.2f, rr + 0.9f, zc - 0.2f, x + 0.2f, rr + 1.2f, zc + 2.4f, Tile.KNURL);
                else
                    m.box(x - 0.9f, rr + 0.9f, zc - 0.9f, x + 0.9f, rr + 1.15f, zc + 0.9f, Tile.KNURL);
            }
        }
    }

    private static int darken(int argb) {
        int r = (argb >> 16 & 0xFF) * 6 / 10, g = (argb >> 8 & 0xFF) * 6 / 10, b = (argb & 0xFF) * 6 / 10;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
