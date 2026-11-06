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
 * own material's colour, placed by the receiver type and sized by the part (barrel length and
 * diameter, magazine band). Internal parts nobody can see (sear, obturator) draw nothing. Moving
 * parts take their place from {@link GunMotion.Parts}.
 *
 * <p>Model pixels on a quarter-pixel grid, in Minecraft's proportions rather than to scale: long and
 * low, a pistol about 9 px, a rifle 25 to 30, receivers 1.5 to 2 px wide. Proportions read off
 * Scorched Guns' models (GPL-3.0, its {@code master} branch; numbers only, no model or texture used).
 */
final class GunDraw {

    private GunDraw() {
    }

    static void draw(GunMesh m, GunView v, GunMotion.Parts p) {
        switch (v.type()) {
            case HANDGUN -> handgun(m, v, p);
            case RIFLE, SHOTGUN -> longGun(m, v, p);
            case MACHINE_GUN -> machineGun(m, v, p);
        }
        // A break action's barrels and everything on them tip about the hinge under the breech.
        m.push();
        if (p.breakOpen() > 0)
            m.turnX(-38f * p.breakOpen(), barrelsBottom(v), -0.5f);
        barrels(m, v);
        muzzle(m, v);
        underBarrel(m, v);
        m.pop();
        sight(m, v);
        sides(m, v);
    }

    // --- receivers ---------------------------------------------------------------------------------

    private static void handgun(GunMesh m, GunView v, GunMotion.Parts p) {
        int frame = v.colour("R");
        m.colour(frame);
        if (!v.openFrame()) {
            float sf = v.slideFront();
            m.box(-0.75f, -1.75f, -sf + 0.75f, 0.75f, -0.75f, 0.5f, Tile.PANEL);
            m.box(-0.5f, -1.25f, 0.5f, 0.5f, -0.75f, 1.25f, Tile.METAL); // beavertail
            // The slide runs back on the shot and stays back on the last round.
            m.push();
            m.move(0, 0, p.slide() * 1.5f);
            m.colour(v.colour("A"));
            m.box(-0.875f, -0.75f, -sf, 0.875f, 0.75f, 0.75f, Tile.METAL);
            m.box(-0.9375f, -0.5f, -0.75f, 0.9375f, 0.5f, 0.5f, Tile.RIBBED); // rear serrations
            m.box(-0.5f, 0.75f, -sf + 0.25f, 0.5f, 0.8125f, 0.5f, Tile.BRUSHED);
            m.box(0.875f, -0.25f, -3.25f, 0.9375f, 0.5f, -1.5f, Tile.BORE); // ejection port
            if (!v.has("S")) {
                m.box(-0.125f, 0.75f, -sf + 0.25f, 0.125f, 1.0f, -sf + 0.75f, Tile.METAL);
                m.box(-0.5f, 0.75f, 0f, -0.125f, 1.0f, 0.5f, Tile.METAL);
                m.box(0.125f, 0.75f, 0f, 0.5f, 1.0f, 0.5f, Tile.METAL);
                if (v.has("e"))
                    m.colour(0xFF40FF60).glow(-0.0625f, 0.8125f, -sf + 0.1875f, 0.0625f, 0.9375f, -sf + 0.25f, Tile.LAMP);
            }
            m.pop();
            grip(m, v, p, -1.75f, 0.5f, 3.5f, -18f, 2f, true);
            trigger(m, v, -1.75f, -3.75f, -1.25f);
            return;
        }
        // Revolver or single-shot frame: the barrel screws into its front at z = -3.5.
        boolean cylinder = v.has("F");
        if (cylinder) {
            m.box(-0.75f, -1.75f, -3.5f, 0.75f, -1.5f, 1.25f, Tile.PANEL); // bottom strap
            m.box(-0.625f, 0.5f, -3.5f, 0.625f, 1.0f, -0.25f, Tile.METAL); // top strap
            m.box(-0.875f, -1.5f, -0.25f, 0.875f, 1.0f, 1.25f, Tile.PANEL); // standing breech
            m.box(-0.75f, -1.5f, -3.5f, 0.75f, 0.5f, -3.0f, Tile.METAL); // front of the window
            m.box(-0.125f, 1.0f, -1.25f, 0.125f, 1.0625f, -0.25f, Tile.BORE); // sight groove
            cylinder(m, v, p, -0.5f, -3.0f, -0.25f, 1.0f, 0.6f);
            m.colour(frame).box(-0.25f, -0.875f, -3.5f - Math.min(v.len(), 3f), 0.25f, -0.375f, -3.5f, Tile.METAL); // ejector rod
        } else {
            m.box(-0.75f, -1.75f, -3.5f, 0.75f, 1.0f, 1.25f, Tile.PANEL);
        }
        // The hammer: cocked back while the action works, down on the shot.
        m.colour(v.has("K") ? v.colour("K") : frame);
        m.push();
        m.turnX(-30f * p.hammer(), 0.5f, 1.25f);
        m.box(-0.25f, 0.5f, 1.0f, 0.25f, 1.5f, 1.5f, Tile.METAL);
        m.box(-0.25f, 1.25f, 1.25f, 0.25f, 1.5f, 2.0f, Tile.KNURL);
        m.pop();
        grip(m, v, p, -1.75f, 1.25f, 3.25f, -28f, 1.75f, false);
        trigger(m, v, -1.75f, -2.75f, -0.25f);
    }

    /**
     * A pistol grip hung below {@code y} with its back at {@code z}, raked by {@code rake} degrees,
     * {@code length} long and {@code depth} front to back. A magazine rides inside it when {@code well}.
     */
    private static void grip(GunMesh m, GunView v, GunMotion.Parts p, float y, float z, float length, float rake,
                             float depth, boolean well) {
        m.push();
        m.turnX(rake, y, z);
        m.colour(v.has("G") ? v.colour("G") : v.colour("R"));
        float f = z - depth;
        m.box(-0.625f, y - length, f, 0.625f, y, z, Tile.PANEL);
        m.box(-0.6875f, y - length + 0.5f, f + 0.25f, 0.6875f, y - 0.5f, z - 0.25f, Tile.CHECKER);
        if (v.has("X")) {
            m.colour(v.colour("X"));
            for (int i = 0; i < 3; i++) {
                float yy = y - 0.75f - i * (length - 1.25f) / 2f;
                m.box(-0.75f, yy - 0.25f, f - 0.0625f, 0.75f, yy, z + 0.0625f, Tile.RUBBER);
            }
        }
        if (well && (v.boxMagazine() || p.magDrop() > 0) && !p.magOut()) {
            float ext = v.magLength();
            m.push();
            m.move(0, -p.magDrop() * 4.5f, 0);
            m.colour(magColour());
            if (v.mag() == MagazineSize.DRUM) {
                m.push();
                m.move(0, y - length - 1.25f, f + depth / 2);
                m.pose.mulPose(Axis.YP.rotationDegrees(90));
                m.rod(0, 0, 1.25f, -0.5f, 0.5f, Tile.METAL);
                m.pop();
            }
            // The body rides inside the grip, seen as it drops out or where an extended one hangs below.
            m.box(-0.5f, y - length - ext, f + 0.25f, 0.5f, y - 0.25f, z - 0.375f, Tile.RIBBED);
            m.box(-0.625f, y - length - ext - 0.25f, f + 0.125f, 0.625f, y - length - ext, z - 0.25f, Tile.METAL);
            m.pop();
        }
        m.pop();
    }

    /** Magazines are drawn in blued steel: the gun knows the magazine's size, not its material. */
    private static int magColour() {
        return 0xFF5A5E66;
    }

    /** The guard and blade under {@code y}, from {@code z0} (front) to {@code z1}. */
    private static void trigger(GunMesh m, GunView v, float y, float z0, float z1) {
        m.colour(v.has("T") ? v.colour("T") : v.colour("R"));
        m.box(-0.25f, y - 1f, z0, 0.25f, y, z0 + 0.25f, Tile.METAL);
        m.box(-0.25f, y - 1f, z0, 0.25f, y - 0.75f, z1, Tile.METAL);
        float tz = z0 + (z1 - z0) * 0.5f;
        m.push();
        m.turnX(-12f, y, tz);
        m.box(-0.125f, y - 0.75f, tz - 0.25f, 0.125f, y, tz, Tile.BRUSHED);
        m.pop();
    }

    /** A revolving cylinder on axis (0, y) from z0 to z1; it swings out to the left to load. */
    private static void cylinder(GunMesh m, GunView v, GunMotion.Parts p, float y, float z0, float z1, float r,
                                 float mouth) {
        m.push();
        m.colour(v.colour("F"));
        m.move(-1.25f * p.cylinderOut(), 0, 0);
        m.turnZ(p.cylinderTurn(), 0, y);
        m.rod(0, y, r, z0, z1, Tile.BRUSHED);
        for (int i = 0; i < 6; i++) {
            m.push();
            m.turnZ(i * 60f, 0, y);
            m.box(-0.125f, y + r * mouth - 0.125f, z0 - 0.0625f, 0.125f, y + r * mouth + 0.125f, z0, Tile.BORE);
            m.pop();
        }
        m.pop();
    }

    private static void longGun(GunMesh m, GunView v, GunMotion.Parts p) {
        boolean shotgun = v.type() == GunLayout.Type.SHOTGUN;
        float hw = v.halfWidth(), lo = shotgun ? -1.25f : -1f, hi = v.top(), back = v.back();
        if (v.slide())
            hi = 1f; // the carrier cover rides above
        int receiver = v.colour("R");
        m.colour(receiver);
        m.box(-hw, lo, -0.5f, hw, hi, back, Tile.PANEL);
        m.box(hw, -0.25f, 0.75f, hw + 0.0625f, 0.5f, 2.75f, Tile.BORE); // ejection port

        // Furniture: the forend under the barrel is in the stock's material.
        int furniture = v.has("G") ? v.colour("G") : receiver;
        float under = barrelsBottom(v);
        float fe = GunView.snap(Math.min(Math.max(v.len() * 0.55f + 1f, 3f), 10f), 0.25f);
        if (v.action().equals("pump")) {
            m.push();
            m.move(0, 0, p.pump() * 2.5f);
            m.colour(v.colour("K"));
            m.box(-0.875f, under - 1.25f, -fe, 0.875f, under + 0.25f, -fe * 0.4f, Tile.RIBBED);
            m.pop();
            m.colour(receiver).box(-0.125f, under - 0.25f, -fe * 0.4f, 0.125f, under, -0.5f, Tile.METAL); // action bar
        } else {
            m.colour(furniture).box(-0.875f, under - 1f, -fe, 0.875f, under + 0.25f, -0.5f, Tile.GRAIN);
        }

        // The stock: wrist down and back, comb, a butt deepening toward the shoulder, a pad.
        m.colour(furniture);
        m.push();
        m.turnX(-12f, lo, back);
        m.box(-0.625f, lo - 1.25f, back, 0.625f, hi - 0.5f, back + 2.75f, Tile.GRAIN);
        m.pop();
        float butt = back + 9.5f;
        m.box(-0.75f, -0.75f, back + 2.25f, 0.75f, 0.5f, butt, Tile.GRAIN);
        m.push();
        m.turnX(6f, -0.75f, butt);
        m.box(-0.6875f, -3.25f, back + 3f, 0.6875f, -0.75f, butt, Tile.GRAIN);
        m.pop();
        m.colour(darken(furniture)).box(-0.8125f, -3.25f, butt, 0.8125f, 0.5f, butt + 0.5f, Tile.RUBBER);

        action(m, v, p, hw, hi, back);
        trigger(m, v, lo, back * 0.45f, back * 0.45f + 2.25f);
        feed(m, v, p, lo, 0.25f, 2f);
    }

    private static void machineGun(GunMesh m, GunView v, GunMotion.Parts p) {
        int receiver = v.colour("R");
        float hw = v.halfWidth(), back = v.back();
        m.colour(receiver);
        m.box(-hw, -1.25f, -1f, hw, 1.5f, back, Tile.PANEL);
        m.box(hw, -0.5f, 0.5f, hw + 0.0625f, 0.5f, 3.5f, Tile.BORE);
        // Top cover, hinged at the back; it lifts to lay a belt.
        m.push();
        m.turnX(-55f * p.cover(), 1.5f, 6f);
        m.box(-hw, 1.5f, 0.5f, hw, 1.75f, 6f, Tile.METAL);
        m.pop();
        // Skeleton buttstock in the receiver's metal, a pad at the shoulder.
        float butt = back + 6.5f;
        m.box(-0.5f, 0.25f, back, 0.5f, 1f, butt, Tile.BRUSHED);
        m.push();
        m.turnX(10f, -2f, butt);
        m.box(-0.5f, -2.5f, back + 1.5f, 0.5f, -1.875f, butt, Tile.BRUSHED);
        m.pop();
        m.box(-0.5f, -2f, butt - 0.75f, 0.5f, 0.25f, butt, Tile.METAL);
        m.colour(darken(receiver)).box(-0.625f, -2.5f, butt, 0.625f, 1.25f, butt + 0.5f, Tile.RUBBER);
        // Ventilated handguard round the barrel's root.
        float under = barrelsBottom(v);
        float fe = GunView.snap(Math.min(Math.max(v.len() * 0.5f, 3f), 9f), 0.25f);
        m.colour(v.has("G") ? v.colour("G") : receiver).box(-0.875f, under - 0.75f, -fe, 0.875f, 0.875f, -1f, Tile.VENT);

        action(m, v, p, hw, 1.5f, back);
        grip(m, v, p, -1.25f, back - 0.75f, 3.25f, -15f, 1.75f, false);
        trigger(m, v, -1.25f, back - 4.5f, back - 2.5f);
        feed(m, v, p, -1.25f, 1f, 3f);
    }

    /** The action's visible bits on a long gun: a bolt handle, a lever loop, a charging handle, a top lever. */
    private static void action(GunMesh m, GunView v, GunMotion.Parts p, float hw, float hi, float back) {
        m.colour(v.has("A") ? v.colour("A") : v.colour("K"));
        switch (v.action()) {
            case "bolt" -> {
                // Lifts a quarter turn about the bore, then draws back.
                m.push();
                m.move(0, 0, p.boltBack() * 2.5f);
                m.turnZ(-65f * p.boltLift(), 0, 0);
                m.box(hw - 0.125f, -0.125f, back - 1.5f, hw + 1f, 0.125f, back - 1.25f, Tile.METAL);
                m.box(hw + 0.75f, -0.25f, back - 1.625f, hw + 1.25f, 0.25f, back - 1.125f, Tile.KNURL);
                m.pop();
            }
            case "lever" -> {
                float z = back * 0.45f;
                m.push();
                m.turnX(45f * p.lever(), -1f, z);
                m.box(-0.125f, -2.25f, z - 0.25f, 0.125f, -1f, z, Tile.METAL);
                m.box(-0.125f, -2.5f, z - 0.25f, 0.125f, -2.25f, z + 3f, Tile.METAL);
                m.box(-0.125f, -2.25f, z + 2.75f, 0.125f, -1.25f, z + 3f, Tile.METAL);
                m.pop();
            }
            case "break" -> m.box(-0.125f, hi, back - 1f, 0.125f, hi + 0.125f, back + 0.25f, Tile.KNURL); // top lever
            case "self_loading", "automatic" -> {
                float travel = v.type() == GunLayout.Type.MACHINE_GUN ? 2.5f : 2f;
                m.push();
                m.move(0, 0, p.slide() * travel);
                if (v.has("A") && v.type() != GunLayout.Type.MACHINE_GUN)
                    m.box(-0.5f, hi, 0f, 0.5f, hi + 0.5f, back - 0.5f, Tile.METAL); // carrier cover
                m.box(hw, 0f, back * 0.55f, hw + 0.5f, 0.25f, back * 0.55f + 0.5f, Tile.KNURL); // charging handle
                m.box(hw, -0.25f, 0.75f, hw + 0.03125f, 0.5f, 2.75f, Tile.BRUSHED); // bolt face in the port
                m.pop();
            }
            default -> {
            }
        }
    }

    /** What feeds the gun below or beside the receiver: a box magazine, a fixed box, a belt and its can. */
    private static void feed(GunMesh m, GunView v, GunMotion.Parts p, float lo, float z0, float z1) {
        if (v.has("b")) {
            m.colour(v.colour("b")).box(-0.625f, lo - 2.5f, z0, 0.625f, lo, z1 + 0.5f, Tile.PANEL);
            return;
        }
        if (v.has("Y") || v.mag() == MagazineSize.BELT) {
            float hw = v.halfWidth();
            m.colour(v.has("Y") ? v.colour("Y") : magColour());
            m.box(-hw - 2f, lo - 2.5f, z0 - 0.5f, -hw, lo + 0.5f, z1 + 1f, Tile.PANEL);
            m.box(-hw - 0.0625f, lo + 0.5f, z0, -hw + 0.25f, lo + 1.25f, z1, Tile.LINKS);
            return;
        }
        if (v.has("F")) {
            cylinder(m, v, p, -0.25f, -0.25f, 1.75f, 0.875f, 0.55f);
            return;
        }
        if (!(v.boxMagazine() || p.magDrop() > 0) || p.magOut())
            return;
        float len = v.magLength();
        m.push();
        m.turnX(10f, lo, (z0 + z1) / 2);
        m.move(0, -p.magDrop() * 5f, 0);
        m.colour(magColour());
        if (v.mag() == MagazineSize.DRUM) {
            m.push();
            m.move(0, lo - 2f, (z0 + z1) / 2);
            m.pose.mulPose(Axis.YP.rotationDegrees(90));
            m.rod(0, 0, 1.5f, -0.75f, 0.75f, Tile.METAL);
            m.pop();
            m.box(-0.375f, lo - 0.75f, z0 + 0.25f, 0.375f, lo, z1 - 0.25f, Tile.RIBBED);
        } else {
            m.box(-0.4375f, lo - len, z0 + 0.125f, 0.4375f, lo, z1 - 0.125f, Tile.RIBBED);
            m.box(-0.5f, lo - len - 0.25f, z0, 0.5f, lo - len, z1, Tile.METAL);
        }
        m.pop();
    }

    // --- along the barrel ----------------------------------------------------------------------------

    /** Where the barrel starts: inside a pistol slide, at a revolver frame's front, inside a long gun's receiver. */
    private static float breech(GunView v) {
        if (v.type() == GunLayout.Type.HANDGUN)
            return v.openFrame() ? -3.5f : 0f;
        return -0.5f;
    }

    /** The barrel's front face. */
    private static float front(GunView v) {
        return breech(v) - (v.type() == GunLayout.Type.HANDGUN && v.openFrame() ? v.len() : v.type() == GunLayout.Type.HANDGUN
                ? Math.max(v.len(), v.slideFront()) : v.len());
    }

    /** Lowest point of the barrel or barrels. */
    private static float barrelsBottom(GunView v) {
        return v.has("D") ? -v.r() - 2 * v.r2() : -v.r();
    }

    private static void barrels(GunMesh m, GunView v) {
        float start = breech(v), end = front(v), r = v.r(), len = start - end;
        m.colour(v.colour("B"));
        m.rod(0, 0, r, start, end, Tile.BRUSHED);
        if (v.has("D")) {
            float y = -r - v.r2(), end2 = start - Math.max(v.len2(), 1f);
            m.colour(v.colour("D"));
            m.rod(0, y, v.r2(), start, end2, Tile.BRUSHED);
            float b = Math.max(0.125f, v.r2() - 0.25f);
            m.box(-b, y - b, end2 - 0.0625f, b, y + b, end2, Tile.BORE);
        }
        // Iron sights: a front post as tall as the rear notch's line, so the two line up when aimed.
        if (v.longGun() && !v.has("S")) {
            m.colour(v.colour("B")).box(-0.125f, r, end + 0.25f, 0.125f, Math.max(r + 0.5f, v.top() + 0.25f), end + 0.75f, Tile.METAL);
            m.colour(v.colour("R"));
            float z = v.rearSight();
            m.box(-0.5f, v.top(), z, -0.125f, v.top() + 0.375f, z + 0.5f, Tile.METAL);
            m.box(0.125f, v.top(), z, 0.5f, v.top() + 0.375f, z + 0.5f, Tile.METAL);
        }
        if (v.openFrame() && !v.has("S"))
            m.colour(v.colour("B")).box(-0.0625f, r, end + 0.25f, 0.0625f, Math.max(r + 0.25f, v.top() + 0.0625f), end + 0.75f, Tile.METAL);
        if (v.has("H"))
            m.colour(v.colour("H")).rod(0, 0, r + 0.25f, start, start - Math.max(1f, len * 0.7f), Tile.VENT);
        if (v.has("Z")) {
            float stop = start - Math.min(len - 0.5f, 6f);
            m.colour(v.colour("Z"));
            m.rings(0, 0, r + 0.25f, start - 0.25f, stop, Math.max(1, (int) ((start - stop) / 0.75f)), 0.25f, Tile.RIBBED);
        }
        if (v.has("J"))
            m.colour(v.colour("J")).rod(0, 0, r + 0.25f, start - len * 0.3f, start - len * 0.65f, Tile.PANEL);
        if (v.has("W")) {
            float stop = start - Math.min(len - 0.5f, 5f);
            m.colour(v.colour("W")).rings(0, 0, r + 0.375f, start - 0.25f, stop, 5, 0.5f, Tile.COIL);
        }
        if (v.has("Q"))
            m.colour(v.colour("Q")).rod(0, 0, r + 0.25f, end, end + Math.min(2.5f, len * 0.4f), Tile.BRUSHED);
        if (v.has("P")) {
            float f = switch (v.variant("P")) {
                case "thread_breech" -> 0.18f;
                case "thread_muzzle" -> 0.88f;
                default -> 0.5f;
            };
            float z = GunView.snap(start - len * f, 0.25f);
            m.colour(v.colour("P")).rod(0, 0, r + 0.25f, z - 0.375f, z + 0.375f, Tile.KNURL);
        }
        if (v.has("V")) {
            m.colour(v.colour("V"));
            for (int i = 0; i < 3; i++) {
                float z = end + 0.5f + i * 0.75f;
                if (z < start - 0.5f)
                    m.box(-0.25f, r, z, 0.25f, r + 0.25f, z + 0.5f, Tile.VENT);
            }
        }
    }

    /** Devices on the muzzle, front to front: a brake, then a can; the bore's mouth on the last. */
    private static void muzzle(GunMesh m, GunView v) {
        float z = front(v), r = v.r();
        if (v.has("N")) {
            m.colour(v.colour("N"));
            m.rod(0, 0, r + 0.125f, z, z - 1.5f, Tile.METAL);
            m.box(-r - 0.1875f, -r * 0.5f, z - 1.125f, r + 0.1875f, r * 0.5f, z - 0.625f, Tile.BORE);
            z -= 1.5f;
        }
        if (v.has("M")) {
            float rr = Math.max(0.875f, r + 0.375f);
            m.colour(v.colour("M"));
            m.rod(0, 0, rr, z, z - 4.5f, Tile.BRUSHED);
            m.rod(0, 0, rr + 0.0625f, z, z - 0.5f, Tile.KNURL);
            m.rod(0, 0, rr + 0.0625f, z - 4f, z - 4.5f, Tile.KNURL);
            z -= 4.5f;
        }
        float b = v.bore();
        m.box(-b, -b, z - 0.0625f, b, b, z, Tile.BORE);
    }

    /** Hung under the barrel: a tube magazine, a spring gun's spring, a counterweight, a bipod, a bayonet. */
    private static void underBarrel(GunMesh m, GunView v) {
        float start = breech(v), end = front(v), len = start - end, under = barrelsBottom(v);
        float y = under - 0.375f;
        if (v.has("E")) {
            float stop = start - Math.max(Math.min(len * 0.85f, len - 0.5f), 2f);
            m.colour(v.colour("E"));
            m.rod(0, y, 0.375f, start, stop, Tile.METAL);
            m.rod(0, y, 0.4375f, stop, stop - 0.25f, Tile.KNURL);
            y -= 0.75f;
        }
        if (v.has("U")) {
            float rr = v.variant("U").equals("heavy_spring") ? 0.625f : 0.5f;
            m.colour(v.colour("U")).rod(0, y - rr + 0.375f, rr, start, start - Math.min(len * 0.6f + 1f, 6f), Tile.COIL);
            y -= 2 * rr;
        }
        if (v.has("C"))
            m.colour(v.colour("C")).box(-0.5f, y - 0.75f, end + 0.25f, 0.5f, y + 0.375f, end + 2f, Tile.METAL);
        if (v.has("I")) {
            float zc = GunView.snap(end + len * 0.25f, 0.25f);
            m.colour(v.colour("I"));
            m.rod(0, 0, v.r() + 0.125f, zc - 0.25f, zc + 0.25f, Tile.KNURL);
            for (float side : new float[]{-1f, 1f}) {
                m.push();
                m.turnX(8f, under, zc);
                m.box(side * 0.25f - 0.125f, under - 0.375f, zc - 5f, side * 0.25f + 0.125f, under - 0.125f, zc, Tile.METAL);
                m.box(side * 0.25f - 0.1875f, under - 0.5f, zc - 5.25f, side * 0.25f + 0.1875f, under - 0.0625f, zc - 4.75f, Tile.RUBBER);
                m.pop();
            }
        }
        if (v.has("a")) {
            m.colour(v.colour("a"));
            m.rod(0, 0, v.r() + 0.125f, end, end + 0.75f, Tile.METAL);
            m.box(-0.0625f, under - 0.75f, end - 5.5f, 0.0625f, under - 0.25f, end + 0.5f, Tile.BRUSHED);
            m.push();
            m.turnX(-25f, under - 0.25f, end - 5.5f);
            m.box(-0.0625f, under - 0.5f, end - 6.5f, 0.0625f, under - 0.25f, end - 5.5f, Tile.BRUSHED);
            m.pop();
        }
    }

    /** An optic on the top, or a lit dot on it. */
    private static void sight(GunMesh m, GunView v) {
        if (!v.has("S"))
            return;
        float top = v.top();
        float z0 = v.opticZ();
        int lamp = switch (v.variant("e")) {
            case "ring_reticle" -> 0xFFFFA030;
            case "cross_reticle" -> 0xFF40FF60;
            default -> 0xFFFF3030;
        };
        m.colour(v.colour("S"));
        if (v.variant("S").equals("scope")) {
            float zs = z0 - 0.5f, ze = zs + v.scopeLength(), y = top + 1f;
            m.box(-0.25f, top, zs + 0.5f, 0.25f, y, zs + 1f, Tile.METAL);
            m.box(-0.25f, top, ze - 1f, 0.25f, y, ze - 0.5f, Tile.METAL);
            m.rod(0, y, 0.375f, zs, ze, Tile.BRUSHED);
            m.rod(0, y, 0.625f, zs - 1.25f, zs, Tile.METAL);
            m.rod(0, y, 0.5f, ze, ze + 1f, Tile.METAL);
            m.box(-0.25f, y + 0.375f, (zs + ze) / 2 - 0.25f, 0.25f, y + 0.75f, (zs + ze) / 2 + 0.25f, Tile.KNURL);
            m.box(-0.625f, y - 0.5f, zs - 1.3125f, 0.625f, y + 0.5f, zs - 1.25f, Tile.LENS);
            m.box(-0.375f, y - 0.375f, ze + 1f, 0.375f, y + 0.375f, ze + 1.0625f, Tile.LENS);
            if (v.has("e"))
                m.colour(lamp).glow(-0.0625f, y - 0.0625f, ze + 1.0625f, 0.0625f, y + 0.0625f, ze + 1.09375f, Tile.LAMP);
        } else {
            m.box(-0.5f, top, z0, 0.5f, top + 0.25f, z0 + 1.5f, Tile.METAL);
            m.box(-0.5f, top + 0.25f, z0, -0.375f, top + 1.25f, z0 + 0.5f, Tile.METAL);
            m.box(0.375f, top + 0.25f, z0, 0.5f, top + 1.25f, z0 + 0.5f, Tile.METAL);
            m.box(-0.5f, top + 1.25f, z0, 0.5f, top + 1.375f, z0 + 0.5f, Tile.METAL);
            m.box(-0.375f, top + 0.25f, z0 + 0.1875f, 0.375f, top + 1.25f, z0 + 0.25f, Tile.GLASS);
            if (v.has("e"))
                m.colour(lamp).glow(-0.0625f, top + 0.6875f, z0 + 0.25f, 0.0625f, top + 0.8125f, z0 + 0.28125f, Tile.LAMP);
        }
    }

    /** On the sides: a selector, a steam chest with its valve and lagging, a hydraulic buffer. */
    private static void sides(GunMesh m, GunView v) {
        float hw = v.halfWidth();
        float at = switch (v.type()) {
            case HANDGUN -> -0.5f;
            case RIFLE -> 3.5f;
            case SHOTGUN -> 3f;
            case MACHINE_GUN -> 6.5f;
        };
        float base = v.type() == GunLayout.Type.HANDGUN ? -1.25f : -0.5f;
        if (v.has("L"))
            m.colour(v.colour("L")).box(-hw - 0.125f, base - 0.125f, at - 0.375f, -hw, base + 0.125f, at + 0.375f, Tile.KNURL);
        if (v.has("d"))
            m.colour(v.colour("d")).rod(-hw - 0.25f, base + 0.5f, 0.25f, at + 0.5f, at + 2f, Tile.BRUSHED);
        if (v.has("O")) {
            boolean large = v.variant("O").equals("large_chest");
            float rr = large ? 1f : 0.75f, len = large ? 6f : 4f, x = -hw - rr;
            float z0 = v.type() == GunLayout.Type.HANDGUN ? -1f : -0.5f;
            m.colour(v.colour("O"));
            m.rod(x, 0, rr, z0, z0 - len, Tile.METAL);
            m.rod(x, 0, rr + 0.0625f, z0, z0 - 0.25f, Tile.PANEL);
            m.rod(x, 0, rr + 0.0625f, z0 - len + 0.25f, z0 - len, Tile.PANEL);
            m.box(x + rr * 0.5f, -0.25f, z0 - 0.25f, -hw, 0.25f, z0 + 0.25f, Tile.METAL); // feed pipe
            if (v.has("h"))
                m.colour(v.colour("h")).rings(x, 0, rr + 0.0625f, z0 - 0.5f, z0 - len + 0.5f, (int) (len / 0.75f), 0.375f, Tile.RUBBER);
            if (v.has("g")) {
                float zc = GunView.snap(z0 - len * 0.5f, 0.25f);
                m.colour(v.colour("g"));
                m.box(x - 0.125f, rr, zc - 0.125f, x + 0.125f, rr + 0.5f, zc + 0.125f, Tile.METAL);
                if (v.variant("g").equals("dump_valve"))
                    m.box(x - 0.125f, rr + 0.5f, zc - 0.125f, x + 0.125f, rr + 0.625f, zc + 1f, Tile.KNURL);
                else
                    m.box(x - 0.375f, rr + 0.5f, zc - 0.375f, x + 0.375f, rr + 0.625f, zc + 0.375f, Tile.KNURL);
            }
        }
    }

    private static int darken(int argb) {
        int r = (argb >> 16 & 0xFF) * 6 / 10, g = (argb >> 8 & 0xFF) * 6 / 10, b = (argb & 0xFF) * 6 / 10;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
