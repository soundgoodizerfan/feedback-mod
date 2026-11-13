// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
        m.layered = true; // parts nest and share planes: each role draws a hair larger than the one inside it
        switch (v.type()) {
            case HANDGUN -> {
                if (v.electric())
                    electricHandgun(m, v, p);
                else
                    handgun(m, v, p);
            }
            case LONG_GUN -> longGun(m, v, p);
            case MACHINE_GUN -> machineGun(m, v, p);
        }
        if (v.electric()) {
            cellBay(m, v);
            seat(m, v);
            instruments(m, v);
        }
        // A break action's barrels and everything on them tip about the hinge under the breech.
        m.push();
        breakTurn(m, v, p);
        m.glowAt(v.glow()); // a hot barrel lights itself; its colour is already drawn toward the glow (GunView)
        barrels(m, v);
        if (v.electric()) {
            electricBarrel(m, v);
            barrelFittings(m, v);
        }
        muzzle(m, v);
        m.glowAt(0);
        underBarrel(m, v);
        m.pop();
        // The rear notch sits on the receiver, so it stays put while the barrels tip.
        if (v.longGun() && !v.has("S")) {
            m.part(v, "R");
            float z = v.rearSight();
            m.box(-0.5f, v.top(), z, -0.125f, v.top() + 0.375f, z + 0.5f, Tile.METAL);
            m.box(0.125f, v.top(), z, 0.5f, v.top() + 0.375f, z + 0.5f, Tile.METAL);
        }
        sides(m, v);
        sight(m, v); // last: its see-through panes and lens go through other buffers
    }

    /** Where a break action hinges: under the barrels at the front of the frame (a revolver's front strap, a shotgun's breech face). */
    private static float[] breakHinge(GunView v) {
        return v.openFrame() ? new float[]{-1.5f, -3.25f} : new float[]{barrelsBottom(v), -0.5f};
    }

    /** Tips everything drawn after it about the break hinge: muzzle down, the rear of the barrels up and clear of the breech. */
    private static void breakTurn(GunMesh m, GunView v, GunMotion.Parts p) {
        if (p.breakOpen() > 0) {
            float[] h = breakHinge(v);
            m.turnX(-(v.openFrame() ? 30f : 38f) * p.breakOpen(), h[0], h[1]);
        }
    }

    // --- receivers ---------------------------------------------------------------------------------

    /** The cylinder on a long or machine gun: axis height, z span and radius, shared with the receiver's window. */
    private static final float CYL_Y = -0.25f, CYL_Z0 = -0.25f, CYL_Z1 = 1.75f, CYL_R = 0.875f;

    /** An electric pistol: a plain frame the barrel screws into, a grip, a guard and a trigger; no slide, no hammer, no magazine. */
    private static void electricHandgun(GunMesh m, GunView v, GunMotion.Parts p) {
        m.part(v, "R");
        body(m, v, 0.75f, -1.75f, 1.0f, -3.5f, 1.25f);
        if (!(v.has("s") && v.variant("s").equals("flywheel")))
            m.box(-0.5f, 1.0f, -3.0f, 0.5f, 1.25f, 0.75f, Tile.BRUSHED); // top rail
        grip(m, v, p, -1.75f, 1.25f, 3.25f, -28f, 1.75f, false);
        trigger(m, v, -1.75f, -2.75f, -0.25f);
        stock(m, v, 1.5f, -1f, 1f);
    }

    /** The receiver's bottom and top (y) on an electric gun: the extent its seat is built into. */
    private static float[] recvY(GunView v) {
        return switch (v.type()) {
            case HANDGUN -> new float[]{-1.75f, 1.0f};
            case LONG_GUN -> new float[]{-1f, 1f};
            case MACHINE_GUN -> new float[]{-1.25f, 1.5f};
        };
    }

    /** Where the seat sits along the receiver (z, front and back); the receiver is built round it. */
    private static float[] seatZ(GunView v) {
        return switch (v.type()) {
            case HANDGUN -> new float[]{-3f, -0.5f};
            case LONG_GUN -> new float[]{0.25f, 3.25f};
            case MACHINE_GUN -> new float[]{1f, 5.5f};
        };
    }

    /** Where the dials and fittings sit on the receiver's flank (z), behind the seat. */
    private static float[] gaugeZ(GunView v) {
        return switch (v.type()) {
            case HANDGUN -> new float[]{-0.25f, 1.25f};
            case LONG_GUN -> new float[]{3.5f, 5.5f};
            case MACHINE_GUN -> new float[]{6f, 9f};
        };
    }

    /** The rear face of an electric receiver (z). */
    private static float recvBack(GunView v) {
        return v.type() == GunLayout.Type.HANDGUN ? 1.25f : v.back();
    }

    /** Whether the seat is cut through the receiver as a window the part sits in (rather than built on or into it another way). */
    private static boolean windowed(GunView v) {
        if (!v.electric() || !v.has("s"))
            return false;
        return switch (v.variant("s")) {
            case "flux_cycler", "burst_bank", "backpack_tether", "flywheel" -> false;
            default -> true;
        };
    }

    /** The window a seat cuts through the receiver: z front and back, y bottom and top. */
    private static float[] seatWindow(GunView v) {
        float[] z = seatZ(v), y = recvY(v);
        return new float[]{z[0], z[1], y[0] + 0.25f, y[1] - 0.25f};
    }

    /** The battery's well: z front and back, and its depth below the receiver. A magazine well is where a magazine would go. */
    private static float[] wellBox(GunView v) {
        return switch (v.type()) {
            case HANDGUN -> new float[]{-3.5f, -2.75f, 1.5f};
            case LONG_GUN -> new float[]{0.25f, 2f, 1.5f};
            case MACHINE_GUN -> new float[]{1f, 3f, 2f};
        };
    }

    /**
     * The battery's well, a magazine well under the receiver: a housing in the receiver's material, and a slot in each side where the
     * pack shows behind glass with a column of lamps that fill with its charge (an empty well is a dark slot).
     */
    private static void cellBay(GunMesh m, GunView v) {
        float[] w = wellBox(v);
        float wx = v.halfWidth() - 0.125f, y1 = recvY(v)[0], y0 = y1 - w[2], z0 = w[0], z1 = w[1];
        m.part(v, "R");
        m.box(-wx, y0, z0, wx, y1, z1, Tile.PANEL);
        float margin = z1 - z0 > 1.2f ? 0.25f : 0.125f, sy0 = y0 + 0.25f, sy1 = y1 - 0.25f;
        for (int side = -1; side <= 1; side += 2) {
            float x = side * wx, o = side * 0.0625f;
            float xa = Math.min(x, x + o), xb = Math.max(x, x + o);
            if (v.cell() < 0f) {
                m.box(xa, sy0, z0 + margin, xb, sy1, z1 - margin, Tile.BORE);
                continue;
            }
            m.colour(0xFF3A4048).box(xa, sy0, z0 + margin, xb, sy1, z1 - margin, Tile.GLASS);
            // A lamp for each quarter of the charge, from the bottom: green while it is well filled, amber then red as it runs down.
            int lit = Math.round(v.cell() * 4f);
            int colour = v.cell() > 0.5f ? 0xFF40FF60 : v.cell() > 0.2f ? 0xFFFFB030 : 0xFFFF4030;
            float step = (sy1 - sy0) / 4f, zm = (z0 + z1) / 2f, half = (z1 - z0) / 2f - margin - 0.0625f;
            float la = Math.min(x, x + 2 * o), lb = Math.max(x, x + 2 * o);
            for (int i = 0; i < 4; i++) {
                float ya = sy0 + i * step + 0.0625f, yb = sy0 + (i + 1) * step - 0.0625f;
                if (i < lit)
                    m.colour(colour).glow(la, ya, zm - half, lb, yb, zm + half, Tile.LAMP);
                else
                    m.colour(0xFF202428).box(la, ya, zm - half, lb, yb, zm + half, Tile.LAMP);
            }
        }
    }

    /** A colour from the beam's toward a hot orange as the barrel heats. */
    private static int heated(int beam, float heat) {
        int r = (beam >> 16) & 255, g = (beam >> 8) & 255, b = beam & 255;
        float t = Math.min(1f, heat);
        return 0xFF000000 | Math.round(r + (255 - r) * t) << 16 | Math.round(g + (110 - g) * t) << 8 | Math.round(b + (30 - b) * t);
    }

    /** The pulse barrel: the bore's tube and, at the muzzle, an emitter lamp in the beam's colour that goes toward orange as the barrel heats. */
    private static void electricBarrel(GunMesh m, GunView v) {
        float end = front(v), r = v.r();
        var pulse = v.pulse() == null ? GunView.Pulse.REST : v.pulse();
        m.part(v, "B");
        m.rod(0, 0, r + 0.125f, breech(v) - 0.5f, end + 0.75f, Tile.BRUSHED);
        m.colour(heated(0xFF000000 | pulse.beam(), pulse.heat())).glow(-r - 0.25f, -r - 0.25f, end, r + 0.25f, r + 0.25f, end + 0.5f, Tile.LAMP);
    }

    /**
     * The seat, built into the receiver (see {@link #body}), each variant with its own look: a cylinder's drum of six chambers (lit where
     * charged, turning one place a shot) in a window; a flux cycler's two tubes hung down the side into the barrel; a capacitor bank's rod
     * in a long window with a strip of charge lamps; a burst bank's three tubes through the receiver, its charge along the top; a flywheel's
     * disc turning in a slot through the top; a dynamo's coil in a window; a tether's socket in the rear face and a cable out the back.
     */
    private static void seat(GunMesh m, GunView v) {
        if (!v.has("s"))
            return;
        var pulse = v.pulse() == null ? GunView.Pulse.REST : v.pulse();
        float hw = v.halfWidth();
        float[] sz = seatZ(v), ry = recvY(v);
        float z0 = sz[0], z1 = sz[1], zc = (z0 + z1) / 2f, span = z1 - z0;
        float wy0 = ry[0] + 0.25f, wy1 = ry[1] - 0.25f, yc = (wy0 + wy1) / 2f, hh = (wy1 - wy0) / 2f;
        switch (v.variant("s")) {
            case "flywheel" -> {
                // A disc standing in a slot cut through the receiver's top, turning along the bore, its upper arc clear of the frame.
                m.part(v, "s");
                float r = Math.min(1.75f, span / 2f - 0.125f), cy = ry[1] + r * 0.4f;
                m.push();
                m.turnY(90f, 0, zc);
                m.rod(0, cy, r, zc - 0.25f, zc + 0.25f, Tile.BRUSHED);
                m.rod(0, cy, 0.375f, zc - 0.375f, zc + 0.375f, Tile.METAL);
                m.push();
                m.turnZ(pulse.angle(), 0, cy);
                for (int i = 0; i < 4; i++) {
                    float a = (float) Math.toRadians(90 * i);
                    float lx = (r - 0.5f) * (float) Math.cos(a), ly = cy + (r - 0.5f) * (float) Math.sin(a);
                    m.colour(0xFFE0E0E8);
                    m.glow(lx - 0.25f, ly - 0.25f, zc + 0.25f, lx + 0.25f, ly + 0.25f, zc + 0.3125f, Tile.LAMP);
                    m.glow(lx - 0.25f, ly - 0.25f, zc - 0.3125f, lx + 0.25f, ly + 0.25f, zc - 0.25f, Tile.LAMP);
                }
                m.pop();
                m.pop();
            }
            case "flux_cycler" -> {
                // Two tubes leave the receiver's right side, hang down like wires, run forward and plug into the bare barrel.
                m.part(v, "s");
                float r = v.r(), out = Math.max(hw, r) + 0.5f, ymid = (ry[0] + ry[1]) / 2f;
                float bare = v.type() == GunLayout.Type.HANDGUN ? -3.5f : -exposed(v);
                for (int i = 0; i < 2; i++) {
                    float zA = z0 + 0.5f + i, zB = Math.max(bare - 0.5f - 0.75f * i, front(v) + 0.5f), yL = -r - 0.75f - 0.5f * i;
                    float t = 0.125f;
                    m.box(hw - 0.0625f, ymid - t, zA - t, out + t, ymid + t, zA + t, Tile.KNURL);
                    m.box(out - t, yL - t, zA - t, out + t, ymid + t, zA + t, Tile.METAL);
                    m.box(out - t, yL - t, zB - t, out + t, yL + t, zA + t, Tile.METAL);
                    m.box(out - t, yL - t, zB - t, out + t, t, zB + t, Tile.METAL);
                    m.box(r - 0.0625f, -t, zB - t, out + t, t, zB + t, Tile.KNURL);
                }
            }
            case "core_breech" -> {
                // Two heat cores stacked in the receiver's window, ringed; cool they carry a lamp strip, hot they glow red end to end.
                m.part(v, "s");
                float r = Math.min(0.34f, hh / 2f - 0.04f);
                for (int i = 0; i < 2; i++) {
                    float y = yc + (i == 0 ? 1 : -1) * (r + 0.04f);
                    if (pulse.hot())
                        m.colour(0xFFFF4A20).glow(-r, y - r, z0, r, y + r, z1, Tile.LAMP);
                    else {
                        m.rod(0, y, r, z0, z1, Tile.METAL);
                        m.colour(pulse.lit() >= 1f ? chargeColour(pulse, 0xFFFFA040) : 0xFF202428).glow(-0.125f, y + r - 0.0625f, z0 + 0.25f, 0.125f, y + r + 0.0625f, z1 - 0.25f, Tile.LAMP);
                    }
                    m.rings(0, y, r + 0.0625f, z0 + 0.5f, z1 - 0.5f, 2, 0.25f, Tile.BRUSHED);
                }
            }
            case "capacitor_bank" -> {
                // A rod in the receiver's long window, ringed, with a strip of six charge lamps below it.
                m.part(v, "s");
                float rr = Math.min(hh - 0.375f, 0.625f), yr = wy1 - rr - 0.0625f;
                m.rod(0, yr, rr, z0, z1, Tile.METAL);
                m.rings(0, yr, rr + 0.0625f, z0 + 0.5f, z1 - 0.5f, 3, 0.25f, Tile.BRUSHED);
                float step = (span - 1f) / 6f;
                for (int i = 0; i < 6; i++) {
                    float z = z0 + 0.5f + i * step;
                    if (i < Math.round(pulse.lit()))
                        m.colour(chargeColour(pulse, 0xFF40C8FF)).glow(-0.125f, wy0 + 0.0625f, z, 0.125f, wy0 + 0.3125f, z + step - 0.125f, Tile.LAMP);
                    else
                        m.colour(0xFF202428).box(-0.125f, wy0 + 0.0625f, z, 0.125f, wy0 + 0.3125f, z + step - 0.125f, Tile.LAMP);
                }
            }
            case "burst_bank" -> {
                // Three tubes through the receiver, their ends showing flush on both sides; the charge in six lamps along the top.
                float yt = ry[1] - 0.625f, rt = 0.3125f, pitch = span / 3f;
                for (int i = 0; i < 3; i++) {
                    float zt = z0 + pitch * (i + 0.5f);
                    m.part(v, "s");
                    m.push();
                    m.turnY(90f, 0, zt);
                    m.rod(0, yt, rt, zt - hw - 0.0625f, zt + hw + 0.0625f, Tile.METAL);
                    m.pop();
                    m.colour(0xFF202428);
                    m.box(hw + 0.0625f, yt - 0.1875f, zt - 0.1875f, hw + 0.09375f, yt + 0.1875f, zt + 0.1875f, Tile.BORE);
                    m.box(-hw - 0.09375f, yt - 0.1875f, zt - 0.1875f, -hw - 0.0625f, yt + 0.1875f, zt + 0.1875f, Tile.BORE);
                }
                float top = v.type() == GunLayout.Type.HANDGUN ? 1.25f : v.top(), step = (span - 1f) / 6f;
                for (int i = 0; i < 6; i++) {
                    float z = z0 + 0.5f + i * step;
                    if (i < Math.round(pulse.lit()))
                        m.colour(chargeColour(pulse, 0xFF40C8FF)).glow(-0.125f, top, z, 0.125f, top + 0.0625f, z + step - 0.125f, Tile.LAMP);
                    else
                        m.colour(0xFF202428).box(-0.125f, top, z, 0.125f, top + 0.0625f, z + step - 0.125f, Tile.LAMP);
                }
            }
            case "recoil_dynamo" -> {
                // A coil of rings on its rod, in the receiver's long window.
                m.part(v, "s");
                float rr = Math.min(hh * 0.4f, 0.5f);
                m.rod(0, yc, rr, z0, z1, Tile.METAL);
                m.rings(0, yc, rr + 0.125f, z0 + 0.25f, z1 - 0.25f, Math.max(4, (int) (span / 0.75f)), 0.25f, Tile.COIL);
            }
            case "backpack_tether" -> {
                // A socket let into the rear face and a cable that leaves backward and hangs.
                m.part(v, "s");
                float rb = recvBack(v), y = (ry[0] + ry[1]) / 2f;
                m.box(-0.375f, y - 0.375f, rb, 0.375f, y + 0.375f, rb + 0.375f, Tile.PANEL);
                m.rod(0, y, 0.25f, rb + 0.375f, rb + 3f, Tile.KNURL);
                m.box(-0.25f, y - 2f, rb + 2.5f, 0.25f, y + 0.25f, rb + 3f, Tile.KNURL);
            }
            default -> {
                // Cylinder: a drum of six chambers in the receiver's window, turned about a sideways axis, the charged ones lit on both faces.
                m.part(v, "s");
                float r = Math.min(hh - 0.125f, span / 2f - 0.125f), half = hw - 0.125f, ring = r * 0.6f, l = 0.1875f;
                m.push();
                m.turnY(90f, 0, zc);
                m.rod(0, yc, r, zc - half, zc + half, Tile.BRUSHED);
                m.push();
                m.turnZ(pulse.angle(), 0, yc);
                for (int i = 0; i < 6; i++) {
                    float a = (float) Math.toRadians(60 * i);
                    float lx = ring * (float) Math.cos(a), ly = yc + ring * (float) Math.sin(a);
                    boolean on = i < Math.round(pulse.lit());
                    for (int side = -1; side <= 1; side += 2) {
                        float za = side > 0 ? zc + half : zc - half - 0.0625f;
                        if (on)
                            m.colour(chargeColour(pulse, 0xFF40FF60)).glow(lx - l, ly - l, za, lx + l, ly + l, za + 0.0625f, Tile.LAMP);
                        else
                            m.colour(0xFF202428).box(lx - l, ly - l, za, lx + l, ly + l, za + 0.0625f, Tile.LAMP);
                    }
                }
                m.pop();
                m.pop();
            }
        }
    }

    /** The colour of a seat's lit lamps: its own, or the hold or pump charge's state when one is fitted ({@link ChargeColour}). */
    private static int chargeColour(GunView.Pulse pulse, int plain) {
        return ChargeColour.of(pulse.charge(), plain);
    }

    /** How far in front of z = 0 the forend or handguard reaches (a long or machine gun): the barrel is bare beyond it. */
    private static float exposed(GunView v) {
        float len = v.len();
        return v.type() == GunLayout.Type.MACHINE_GUN ? GunView.snap(Math.min(Math.max(len * 0.5f, 3f), 9f), 0.25f)
                : GunView.snap(Math.min(Math.max(len * 0.55f + 1f, 3f), 10f), 0.25f);
    }

    /**
     * The dials and fittings a pulse gun carries: the heat governor's knob, vent or lamp, the heat gauge's dial with a needle that swings
     * with the barrel's heat, the hold-charge lever, the pump's slider, the beam lens (prongs for a splitter, a pair of tines for a fork)
     * and the zoom charge's coil.
     */
    private static void instruments(GunMesh m, GunView v) {
        var pulse = v.pulse() == null ? GunView.Pulse.REST : v.pulse();
        float hw = v.halfWidth(), ga = gaugeZ(v)[0], gb = gaugeZ(v)[1], ymid = (recvY(v)[0] + recvY(v)[1]) / 2f, yg = ymid - 0.5f;
        // The governor and the hold lever share the right flank (the gauge's dial has the left): the lever forward, the governor behind it.
        float gs = ga + 0.75f;
        if (v.has("g")) {
            m.part(v, "g");
            switch (v.variant("g")) {
                case "forced_vent" -> {
                    for (int i = 0; i < 3; i++)
                        m.box(hw + 0.0625f, yg + 0.125f + 0.25f * i, gs, hw + 0.75f, yg + 0.25f + 0.25f * i, Math.min(gs + 0.75f, gb - 0.125f), Tile.BORE);
                }
                case "dead_man_cutoff" -> m.colour(0xFFFF4030).glow(hw + 0.0625f, yg + 0.125f, gs, hw + 0.75f, yg + 0.625f, gs + 0.5f, Tile.LAMP);
                default -> m.rod(hw + 0.375f, yg + 0.5f, 0.375f, gs, Math.min(gs + 0.5f, gb), Tile.KNURL);
            }
        }
        if (v.has("u")) {
            // The heat gauge: a dial on the left flank, its needle swinging with the barrel's heat.
            m.part(v, "u");
            float zc = (ga + gb) / 2f, dr = Math.min(0.875f, (gb - ga) / 2f - 0.0625f), px = -hw - 0.125f;
            m.push();
            m.turnY(-90f, px, zc);
            m.rod(px, ymid, dr, zc - 0.25f, zc + 0.125f, Tile.PANEL);
            m.push();
            m.turnZ(-120f + 240f * Math.min(1f, pulse.heat()), px, ymid);
            m.colour(0xFFFF4030).glow(px - 0.0625f, ymid, zc + 0.125f, px + 0.0625f, ymid + dr - 0.125f, zc + 0.25f, Tile.LAMP);
            m.pop();
            m.pop();
        }
        if (v.has("h")) {
            m.part(v, "h");
            m.box(hw, ymid - 0.5f, ga, hw + 0.5f, ymid + 0.5f, ga + 0.5f, Tile.KNURL);
        }
        if (v.has("z")) {
            m.part(v, "z");
            m.rod(0, v.top() + 0.375f, 0.375f, ga, Math.min(ga + 1.5f, gb), Tile.COIL);
        }
    }

    /** The fittings on the barrel itself (the pump's slider, the beam lens): drawn inside the group a break action tips. */
    private static void barrelFittings(GunMesh m, GunView v) {
        float end = front(v);
        if (v.has("p")) {
            m.part(v, "p");
            m.rod(0, -v.r() - 0.625f, 0.375f, end + 1f, end + 4f, Tile.KNURL);
        }
        if (v.has("l")) {
            m.part(v, "l");
            float r = v.r();
            if (v.variant("l").equals("arc_fork")) {
                m.box(-r - 0.75f, -0.25f, end - 1f, -r - 0.25f, 0.25f, end + 0.5f, Tile.METAL);
                m.box(r + 0.25f, -0.25f, end - 1f, r + 0.75f, 0.25f, end + 0.5f, Tile.METAL);
                m.colour(0xFF9FD8FF).glow(-r - 0.75f, -0.25f, end - 1.125f, -r - 0.25f, 0.25f, end - 1f, Tile.LAMP);
                m.colour(0xFF9FD8FF).glow(r + 0.25f, -0.25f, end - 1.125f, r + 0.75f, 0.25f, end - 1f, Tile.LAMP);
            } else {
                // A steel frame round the lens (nothing behind it, so a clear lens shows the barrel and the facets), the lens in it.
                float rr = r + 0.5f, t = 0.25f;
                m.part(v, "lh");
                m.box(-rr, rr - t, end - 0.4f, rr, rr, end, Tile.METAL);
                m.box(-rr, -rr, end - 0.4f, rr, -rr + t, end, Tile.METAL);
                m.box(-rr, -rr + t, end - 0.4f, -rr + t, rr - t, end, Tile.METAL);
                m.box(rr - t, -rr + t, end - 0.4f, rr, rr - t, end, Tile.METAL);
                m.part(v, "l");
                m.box(-rr + t, -rr + t, end - 0.35f, rr - t, rr - t, end - 0.05f, Tile.METAL);
                for (int i = 0; i < 3; i++) {
                    float a = (float) Math.toRadians(120 * i + 90);
                    m.colour(0xFF9FD8FF).glow((float) Math.cos(a) * (r + 0.25f) - 0.125f, (float) Math.sin(a) * (r + 0.25f) - 0.125f, end - 0.875f,
                            (float) Math.cos(a) * (r + 0.25f) + 0.125f, (float) Math.sin(a) * (r + 0.25f) + 0.125f, end - 0.75f, Tile.LAMP);
                }
            }
        }
    }

    private static void handgun(GunMesh m, GunView v, GunMotion.Parts p) {

        m.part(v, "R");
        if (!v.openFrame()) {
            float sf = v.slideFront();
            m.box(-0.75f, -1.75f, -sf + 0.75f, 0.75f, -0.75f, 0.5f, Tile.PANEL);
            m.box(-0.5f, -1.25f, 0.5f, 0.5f, -0.75f, 1.25f, Tile.METAL); // beavertail
            // The slide runs back on the shot and stays back on the last round.
            m.push();
            m.move(0, 0, p.slide() * 1.5f);
            m.part(v, "A");
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
            stock(m, v, 1.25f, -1f, 0.75f);
            return;
        }
        // Revolver or single-shot frame: the barrel screws into its front at z = -3.5.
        boolean cylinder = v.has("F");
        if (cylinder) {
            m.box(-0.75f, -1.75f, -3.5f, 0.75f, -1.5f, 1.25f, Tile.PANEL); // bottom strap
            m.box(-0.875f, -1.5f, -0.25f, 0.875f, 1.0f, 1.25f, Tile.PANEL); // standing breech
            // The top strap, the front of the window and the ejector rod are hinged with the barrel on a top-break.
            m.push();
            breakTurn(m, v, p);
            m.box(-0.625f, 0.5f, -3.5f, 0.625f, 1.0f, -0.25f, Tile.METAL); // top strap
            m.box(-0.75f, -1.5f, -3.5f, 0.75f, 0.5f, -3.0f, Tile.METAL); // front of the window
            m.box(-0.125f, 1.0f, -1.25f, 0.125f, 1.0625f, -0.25f, Tile.BORE); // sight groove
            m.pop();
            cylinder(m, v, p, -0.5f, -3.0f, -0.25f, 1.0f, 0.6f);
            float[] carry = handgunSupport(v, p);
            // The rounds still in the hand: the handful taken from the pouch, one fewer once each has gone in.
            int inHand = p.grab() >= 0.5f && p.grab() < 1.5f || p.feed() >= 0f ? Math.round(p.feedOf() - p.fed() - (p.feed() > 0.5f ? 1 : 0)) : 0;
            for (int i = 0; carry != null && i < Math.min(inHand, 4); i++)
                m.colour(0xFFD9A441).box(carry[1] - 0.2f + 0.45f * i, carry[2] - 0.2f + (i == 0 ? 0f : 0.25f), carry[3] - 0.375f,
                        carry[1] + 0.2f + 0.45f * i, carry[2] + 0.2f + (i == 0 ? 0f : 0.25f), carry[3] + 0.375f, Tile.BRUSHED);
            m.part(v, "R").box(-0.25f, -0.875f, -3.5f - Math.min(v.len(), 3f), 0.25f, -0.375f, -3.5f, Tile.METAL); // ejector rod
        } else {
            m.box(-0.75f, -1.75f, -3.5f, 0.75f, 1.0f, 1.25f, Tile.PANEL);
        }
        // The hammer: cocked back while the action works, down on the shot.
        // A self-loading or automatic action has no hammer trait, so none is drawn.
        if (!v.selfLoading()) {
            m.part(v, v.has("K") ? "K" : "R");
            m.push();
            m.turnX(-30f * p.hammer(), 0.5f, 1.25f);
            m.box(-0.25f, 0.5f, 1.0f, 0.25f, 1.5f, 1.5f, Tile.METAL);
            m.box(-0.25f, 1.25f, 1.25f, 0.25f, 1.5f, 2.0f, Tile.KNURL);
            m.pop();
        }
        grip(m, v, p, -1.75f, 1.25f, 3.25f, -28f, 1.75f, false);
        trigger(m, v, -1.75f, -2.75f, -0.25f);
        stock(m, v, 1.5f, -1f, 1f);
    }

    /**
     * A pistol grip hung below {@code y} with its back at {@code z}, raked by {@code rake} degrees,
     * {@code length} long and {@code depth} front to back. A magazine rides inside it when {@code well}.
     */
    private static void grip(GunMesh m, GunView v, GunMotion.Parts p, float y, float z, float length, float rake,
                             float depth, boolean well) {
        String shape = v.has("G") ? v.variant("G") : "pistol_grip";
        boolean straight = shape.equals("straight");
        m.push();
        m.turnX(straight ? 0f : rake, y, z); // a straight grip hangs plumb
        m.part(v, v.has("G") ? "G" : "R");
        float f = z - depth;
        // Runs up into the frame: the rake swings the grip's front edge down, which would leave a wedge of daylight.
        m.box(-0.625f, y - length, f, 0.625f, y + 0.75f, z, Tile.PANEL);
        if (v.has("Gw")) {
            // The wrap: a sleeve over the hand's part of the grip, in its own material.
            m.part(v, "Gw");
            m.box(-0.6875f, y - length + 0.5f, f + 0.25f, 0.6875f, y - 0.5f, z - 0.25f, Tile.CHECKER);
            m.part(v, "G");
        } else if (!straight)
            m.box(-0.6875f, y - length + 0.5f, f + 0.25f, 0.6875f, y - 0.5f, z - 0.25f, Tile.CHECKER);
        switch (shape) {
            case "finger_groove" -> {
                // Three ridges down the front, a groove between each for the fingers.
                for (int i = 0; i < 3; i++) {
                    float yy = y - 0.75f - i * (length - 1.5f) / 3f;
                    m.box(-0.6875f, yy - 0.5f, f - 0.25f, 0.6875f, yy, f, Tile.RIBBED);
                }
            }
            case "palm_swell" -> // A bulge behind, filling the palm.
                m.box(-0.6875f, y - length * 0.8f, z, 0.6875f, y - length * 0.2f, z + 0.5f, Tile.GRAIN);
            default -> { }
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
        m.part(v, v.has("T") ? "T" : "R");
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
        m.part(v, "F");
        m.move(-1.25f * p.cylinderOut(), 0, 0);
        m.turnZ(p.cylinderTurn(), 0, y);
        m.rod(0, y, r, z0, z1, Tile.BRUSHED);
        for (int i = 0; i < 6; i++) {
            m.push();
            m.turnZ(i * 60f, 0, y);
            m.box(-0.125f, y + r * mouth - 0.125f, z0 - 0.0625f, 0.125f, y + r * mouth + 0.125f, z0, Tile.BORE);
            // A round standing in the chamber, seen from the open side: the ones already in, and the one just pushed home.
            if ((p.cylinderOut() > 0 || p.breakOpen() > 0) && p.feedOf() > 0 && i < v.rounds() + p.fed() + (p.feed() > 0.5f ? 1 : 0))
                m.colour(0xFFD9A441).box(-0.2f, y + r * mouth - 0.2f, z1, 0.2f, y + r * mouth + 0.2f, z1 + 0.375f, Tile.BRUSHED);
            m.pop();
        }
        m.pop();
    }

    /** The receiver block; with a cylinder it is a frame round the cylinder's open window, so the cylinder sits in a cut-out. */
    private static void body(GunMesh m, GunView v, float hw, float lo, float hi, float z0, float back) {
        if (windowed(v)) {
            float[] w = seatWindow(v);
            framed(m, hw, lo, hi, z0, back, w[0], w[1], w[2], w[3]);
            return;
        }
        if (v.electric() && v.has("s") && v.variant("s").equals("flywheel")) {
            // A slot through the top for the disc: the receiver in two halves along the seat, the disc turning between them.
            float[] z = seatZ(v);
            m.box(-hw, lo, z0, hw, hi, z[0], Tile.PANEL);
            m.box(-hw, lo, z[1], hw, hi, back, Tile.PANEL);
            m.box(-hw, lo, z[0], -0.4375f, hi, z[1], Tile.PANEL);
            m.box(0.4375f, lo, z[0], hw, hi, z[1], Tile.PANEL);
            return;
        }
        if (!v.has("F")) {
            m.box(-hw, lo, z0, hw, hi, back, Tile.PANEL);
            return;
        }
        // A margin keeps the frame's faces off the cylinder's, which would z-fight.
        float g = 0.125f;
        framed(m, hw, lo, hi, z0, back, CYL_Z0 - g, CYL_Z1 + g, CYL_Y - CYL_R - g, CYL_Y + CYL_R + g);
    }

    /** A receiver block with a window cut through its width (z wz0 to wz1, y wy0 to wy1): a frame of four boxes. */
    private static void framed(GunMesh m, float hw, float lo, float hi, float z0, float back, float wz0, float wz1, float wy0, float wy1) {
        if (wz0 > z0)
            m.box(-hw, lo, z0, hw, hi, wz0, Tile.PANEL);
        if (back > wz1)
            m.box(-hw, lo, wz1, hw, hi, back, Tile.PANEL);
        if (hi > wy1)
            m.box(-hw, wy1, wz0, hw, hi, wz1, Tile.PANEL);
        if (lo < wy0)
            m.box(-hw, lo, wz0, hw, wy0, wz1, Tile.PANEL);
    }

    private static void longGun(GunMesh m, GunView v, GunMotion.Parts p) {
        float hw = v.halfWidth(), lo = -1f, hi = v.top(), back = v.back();
        if (v.slide())
            hi = 1f; // the carrier cover rides above
        int receiver = v.colour("R");
        m.part(v, "R");
        body(m, v, hw, lo, hi, -0.5f, back);
        if (!v.has("F") && !v.electric())
            m.box(hw, -0.25f, 0.75f, hw + 0.0625f, 0.5f, 2.75f, Tile.BORE); // ejection port

        // Furniture: the forend under the barrel is in the stock's material.
        String furniture = v.has("i") ? "i" : v.has("G") ? "G" : "R";
        float under = barrelsBottom(v);
        float fe = GunView.snap(Math.min(Math.max(v.len() * 0.55f + 1f, 3f), 10f), 0.25f);
        if (v.action().equals("pump")) {
            m.push();
            m.move(0, 0, p.pump() * FOREND_TRAVEL);
            m.part(v, "K");
            m.box(-0.875f, under - 1.25f, -fe, 0.875f, under + 0.25f, -fe * 0.4f, Tile.RIBBED);
            m.pop();
            m.part(v, "R").box(-0.125f, under - 0.25f, -fe * 0.4f, 0.125f, under, -0.5f, Tile.METAL); // action bar
        } else {
            // The forend hangs on the barrels, so it tips with them on a break action.
            m.push();
            breakTurn(m, v, p);
            // A pulse gun's pump charge works the forend back and forth.
            m.move(0, 0, v.electric() ? p.pump() * FOREND_TRAVEL : 0f);
            m.part(v, furniture).box(-0.875f, under - 1f, -fe, 0.875f, under + 0.25f, -0.5f, Tile.GRAIN);
            m.pop();
        }

        stock(m, v, back, lo, hi);
        foregrip(m, v, under - 0.75f, -fe * 0.75f);

        action(m, v, p, hw, hi, back);
        grip(m, v, p, lo, back - 0.75f, 3.25f, -15f, 1.75f, false);
        trigger(m, v, lo, back * 0.45f, back * 0.45f + 2.25f);
        feed(m, v, p, lo, 0.25f, 2f);
    }

    /** The foregrip, if the gun has one: hung from the forend's underside at {@code y}, at {@code z}, by its variant. */
    private static void foregrip(GunMesh m, GunView v, float y, float z) {
        if (!v.has("W") || !v.longGun())
            return;
        m.part(v, "W");
        switch (v.variant("W")) {
            case "angled_grip" -> {
                m.push();
                m.turnX(28f, y, z); // the post leans forward
                m.box(-0.5f, y - 3.25f, z - 0.5f, 0.5f, y + 0.25f, z + 0.5f, Tile.PANEL);
                m.pop();
            }
            case "hand_stop" -> {
                m.box(-0.5f, y - 0.25f, z - 1f, 0.5f, y + 0.25f, z + 1f, Tile.PANEL); // the plate on the forend
                m.box(-0.625f, y - 1.5f, z - 1.25f, 0.625f, y, z - 0.5f, Tile.PANEL); // the ledge the hand stops against
            }
            case "folding_grip" -> {
                m.box(-0.5f, y - 0.5f, z - 0.75f, 0.5f, y + 0.25f, z + 0.75f, Tile.METAL); // the hinge block
                m.push();
                m.turnX(70f, y - 0.5f, z); // swung forward, nearly flat
                m.box(-0.375f, y - 3.25f, z - 0.375f, 0.375f, y - 0.5f, z + 0.375f, Tile.PANEL);
                m.pop();
            }
            default -> {
                m.box(-0.5f, y - 3f, z - 0.5f, 0.5f, y + 0.25f, z + 0.5f, Tile.PANEL); // vertical post
                m.box(-0.5625f, y - 3f, z - 0.5625f, 0.5625f, y - 2.5f, z + 0.5625f, Tile.RUBBER);
            }
        }
    }

    /** The stock part, if the gun has one: fixed (furniture), skeleton (bare rods) or folding (a hinged tube), from {@code back} behind the receiver. */
    private static void stock(GunMesh m, GunView v, float back, float lo, float hi) {
        if (!v.has("i"))
            return;
        int colour = v.colour("i");
        m.part(v, "i");
        switch (v.variant("i")) {
            case "skeleton" -> {
                float butt = back + 6.5f;
                m.box(-0.5f, 0.25f, back, 0.5f, 1f, butt, Tile.BRUSHED);
                m.push();
                m.turnX(10f, -2f, butt);
                m.box(-0.5f, -2.5f, back + 1.5f, 0.5f, -1.875f, butt, Tile.BRUSHED);
                m.pop();
                m.box(-0.5f, -2f, butt - 0.75f, 0.5f, 0.25f, butt, Tile.METAL);
                m.colour(darken(colour)).box(-0.625f, -2.5f, butt, 0.625f, 1.25f, butt + 0.5f, Tile.RUBBER);
            }
            case "folding" -> {
                float butt = back + 7.5f;
                m.box(-0.5f, -0.25f, back, 0.5f, 0.75f, back + 0.75f, Tile.METAL); // the hinge block
                m.box(-0.25f, 0.25f, back + 0.75f, 0.25f, 0.75f, butt, Tile.BRUSHED);
                m.box(-0.25f, -1.5f, back + 1.5f, 0.25f, -1f, butt, Tile.BRUSHED);
                m.colour(darken(colour)).box(-0.5f, -1.75f, butt, 0.5f, 0.75f, butt + 0.5f, Tile.RUBBER);
            }
            default -> {
                // Wrist down and back, comb, a butt deepening toward the shoulder, a pad.
                // A raked grip is the wrist (the stock starts right behind it); a straight grip leaves the stock its own.
                boolean gripIsWrist = v.longGun() && v.has("G") && !v.variant("G").equals("straight");
                if (!gripIsWrist) {
                    m.push();
                    m.turnX(-12f, lo, back);
                    m.box(-0.625f, lo - 1.25f, back, 0.625f, hi - 0.5f, back + 2.75f, Tile.GRAIN);
                    m.pop();
                }
                float butt = back + 9.5f;
                m.box(-0.75f, -0.75f, gripIsWrist ? back - 0.75f : back + 2.25f, 0.75f, 0.5f, butt, Tile.GRAIN);
                m.push();
                m.turnX(6f, -0.75f, butt);
                m.box(-0.6875f, -3.25f, gripIsWrist ? back : back + 3f, 0.6875f, -0.75f, butt, Tile.GRAIN);
                m.pop();
                m.colour(darken(colour)).box(-0.8125f, -3.25f, butt, 0.8125f, 0.5f, butt + 0.5f, Tile.RUBBER);
            }
        }
    }

    private static void machineGun(GunMesh m, GunView v, GunMotion.Parts p) {
        int receiver = v.colour("R");
        float hw = v.halfWidth(), back = v.back();
        m.part(v, "R");
        body(m, v, hw, -1.25f, 1.5f, -1f, back);
        if (!v.has("F") && !v.electric())
            m.box(hw, -0.5f, 0.5f, hw + 0.0625f, 0.5f, 3.5f, Tile.BORE);
        // Top cover, hinged at the back; it lifts to lay a belt.
        m.push();
        m.turnX(-55f * p.cover(), 1.5f, 6f);
        m.box(-hw, 1.5f, 0.5f, hw, 1.75f, 6f, Tile.METAL);
        m.pop();
        stock(m, v, back, -1.25f, 1.5f);
        // Ventilated handguard round the barrel's root.
        float under = barrelsBottom(v);
        float fe = GunView.snap(Math.min(Math.max(v.len() * 0.5f, 3f), 9f), 0.25f);
        m.part(v, v.has("G") ? "G" : "R").box(-0.875f, under - 0.75f, -fe, 0.875f, 0.875f, -1f, Tile.VENT);
        foregrip(m, v, under - 0.5f, -fe * 0.75f);

        action(m, v, p, hw, 1.5f, back);
        grip(m, v, p, -1.25f, back - 0.75f, 3.25f, -15f, 1.75f, false);
        trigger(m, v, -1.25f, back - 4.5f, back - 2.5f);
        feed(m, v, p, -1.25f, 1f, 3f);
    }

    /** The action's visible bits on a long gun: a bolt handle, a lever loop, a charging handle, a top lever. */
    private static void action(GunMesh m, GunView v, GunMotion.Parts p, float hw, float hi, float back) {
        m.part(v, v.has("A") ? "A" : "K");
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
            m.part(v, "b").box(-0.625f, lo - 2.5f, z0, 0.625f, lo, z1 + 0.5f, Tile.PANEL);
            return;
        }
        if (v.has("Y") || v.mag() == MagazineSize.BELT) {
            float hw = v.halfWidth();
            if (v.has("Y"))
                m.part(v, "Y");
            else
                m.colour(magColour());
            m.box(-hw - 2f, lo - 2.5f, z0 - 0.5f, -hw, lo + 0.5f, z1 + 1f, Tile.PANEL);
            m.box(-hw - 0.0625f, lo + 0.5f, z0, -hw + 0.25f, lo + 1.25f, z1, Tile.LINKS);
            return;
        }
        if (v.has("F")) {
            cylinder(m, v, p, CYL_Y, CYL_Z0, CYL_Z1, CYL_R, 0.55f);
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
    /** How far a pumped forend slides back, pixels. */
    static final float FOREND_TRAVEL = 3f;

    // --- where the hands go ----------------------------------------------------------------------------

    /** 0 to 1, smoothed: how far a hand has reached for a part that has moved by {@code x} of its travel. */
    private static float reach(float x) {
        float t = Math.max(0f, Math.min(1f, x * 3f));
        return t * t * (3 - 2 * t);
    }

    /** A point (y, z) turned {@code deg} about X around (py, pz), as {@link GunMesh#turnX} turns the parts. */
    private static float[] turned(float y, float z, float py, float pz, float deg) {
        double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a);
        float dy = y - py, dz = z - pz;
        return new float[]{(float) (py + dy * c - dz * s), (float) (pz + dy * s + dz * c)};
    }

    private static float smooth(float x) {
        float t = Math.max(0f, Math.min(1f, x));
        return t * t * (3 - 2 * t);
    }

    private static float[] lerp3(float[] a, float[] b, float w) {
        return new float[]{a[0] + (b[0] - a[0]) * w, a[1] + (b[1] - a[1]) * w, a[2] + (b[2] - a[2]) * w};
    }

    private static float[] between(float[] from, float w, float x, float y, float z) {
        return new float[]{from[0] + (x - from[0]) * w, from[1] + (y - from[1]) * w, from[2] + (z - from[2]) * w};
    }

    /**
     * Where the shooting hand leaves the grip to work the action, in gun space: weight 0 to 1, then x, y, z; null when it stays on the grip.
     * A bolt rifle's hand follows the bolt knob up and back, a lever gun's the lever loop.
     */
    static float[] shootTarget(GunView v, GunMotion.Parts p) {
        if (!v.longGun())
            return null;
        switch (v.action()) {
            case "bolt" -> {
                float w = reach(Math.max(p.boltLift(), p.boltBack()));
                if (w <= 0f)
                    return null;
                double a = Math.toRadians(-65f * p.boltLift());
                float r = v.halfWidth() + 1f;
                return new float[]{w, (float) (r * Math.cos(a)), (float) (r * Math.sin(a)), v.back() - 1.4f + p.boltBack() * 2.5f};
            }
            case "lever" -> {
                float w = reach(p.lever());
                if (w <= 0f)
                    return null;
                float z0 = v.back() * 0.45f;
                float[] at = turned(-2.4f, z0 + 1.5f, -1f, z0, 45f * p.lever());
                return new float[]{w, 0f, at[0], at[1]};
            }
            default -> {
                return null;
            }
        }
    }

    /**
     * How the support arm is turned from its usual pose, degrees (yaw, pitch, roll; pitch positive brings the forearm up from below,
     * negative down from above): a core breech vent swings it up over the barrels, down onto them, away, round to underneath, and up with the forend.
     */
    static float[] supportTurn(GunView v, GunMotion.Parts p) {
        float t = p.vent();
        if (t < 0f || v.type() != GunLayout.Type.LONG_GUN)
            return new float[3];
        float pitch, roll = 0f;
        if (t < 0.14f)
            pitch = -75f * smooth(t / 0.14f);
        else if (t < 0.19f)
            pitch = -75f + 55f * ((t - 0.14f) / 0.05f) * ((t - 0.14f) / 0.05f);
        else if (t < 0.26f)
            pitch = -20f;
        else if (t < 0.4f)
            pitch = -20f - 55f * smooth((t - 0.26f) / 0.14f);
        else if (t < 0.55f) {
            float k = smooth((t - 0.4f) / 0.15f);
            pitch = -75f + 100f * k;
            roll = 35f * k;
        } else if (t < 0.72f) {
            float k = (t - 0.55f) / 0.17f;
            pitch = 25f + 20f * k;
            roll = 35f;
        } else {
            float k = smooth((t - 0.72f) / 0.23f);
            pitch = 45f * (1 - k);
            roll = 35f * (1 - k);
        }
        return new float[]{0f, pitch, roll};
    }

    /** Where the free hand waits, off the gun, before it comes in to work a handgun's reload (gun space). */
    static final float[] HAND_WAITS_AT = {-40f, -16f, 0f};

    /**
     * The free hand on a one-handed gun, in gun space: weight 0 to 1 for how far it has come in from {@link #HAND_WAITS_AT}, then x, y, z.
     * Null when nothing is being worked. A revolver's hand takes the cylinder out to the left or pushes a top-break's barrel down; a pistol's
     * takes the magazine from the grip's foot and seats the fresh one.
     */
    static float[] handgunSupport(GunView v, GunMotion.Parts p) {
        if (v.type() != GunLayout.Type.HANDGUN)
            return null;
        float w = 0f;
        float[] at = null;
        if (p.fan() >= 0f) {
            // Fanning: the hand comes down on the hammer and lifts off again every cycle, and leaves when the shooting stops.
            float f = p.fan(), c = f % 1f;
            float[] hit = {0f, 1.8f, 0.2f}, up = {-5f, 5.5f, -0.6f};
            // Held up, wound up a touch, then struck in a frame or two (the in-betweens skipped), held on the hammer a beat, and lifted off.
            float[] wind = {up[0] - 1f, up[1] + 1.5f, up[2] + 0.5f};
            float[] now = c < 0.25f ? lerp3(up, wind, smooth(c / 0.25f)) : c < 0.3f ? lerp3(wind, hit, (c - 0.25f) / 0.05f)
                    : c < 0.5f ? new float[]{hit[0], hit[1] - 0.3f * smooth((c - 0.3f) / 0.05f), hit[2] - 0.3f * smooth((c - 0.3f) / 0.05f)}
                    : lerp3(hit, up, smooth((c - 0.5f) / 0.5f));
            return new float[]{f < 1f ? 1f : 1f - smooth((f - 1f) / 2f), now[0], now[1], now[2]};
        }
        // A top-break's barrel is pushed down by this hand (at the start, and again to shut it).
        float[] tip = null;
        if (v.openFrame() && p.breakOpen() > 0) {
            float[] h = breakHinge(v);
            float[] t = turned(0.8f, -3.5f - Math.min(v.len(), 3f), h[0], h[1], -30f * p.breakOpen());
            tip = new float[]{0f, t[0], t[1]};
        }
        if (v.openFrame() && v.has("F") && (p.cylinderOut() > 0 || tip != null)) {
            // The cylinder is out: the hand waits (on the barrel if it is a top-break, else at its pouch), then a round at a time takes one
            // from the pouch to the open chamber, pushes it in from behind, and goes back; the first round starts and the last ends on the barrel.
            float[] pouch = {-12f, -6f, 1.5f};
            w = reach(Math.max(p.cylinderOut(), p.breakOpen()));
            at = tip != null ? tip : pouch;
            if (p.grab() >= 0f || p.feed() >= 0f) {
                float cx = -1.25f * p.cylinderOut() - 0.3f;
                // The handful is held just clear of the cylinder (above a top-break's open frame, behind a swing-out's rear face), and each round is
                // pushed in from there.
                float[] hold = tip != null ? new float[]{cx, 5f, -1.6f} : new float[]{cx, 1.6f, 3.2f};
                float[] seat = tip != null ? new float[]{cx, 0.9f, -1.6f} : new float[]{cx, 0.1f, -0.2f};
                if (p.grab() >= 0f && p.grab() < 1f) {
                    // Over to the pouch (from the barrel it has just pushed open), a beat to take up every round, back to the cylinder.
                    float g = p.grab();
                    at = g < 0.4f ? lerp3(at, pouch, smooth(g / 0.4f)) : g < 0.6f ? pouch : lerp3(pouch, hold, smooth((g - 0.6f) / 0.4f));
                } else if (p.feed() >= 0f) {
                    float f = p.feed();
                    // Pushed in hard, held a beat, drawn back; the next round is already in the hand.
                    at = f < 0.45f ? lerp3(hold, seat, (f / 0.45f) * (f / 0.45f)) : f < 0.6f ? seat : lerp3(seat, hold, smooth((f - 0.6f) / 0.4f));
                } else if (p.grab() >= 1f) {
                    at = lerp3(hold, tip != null ? tip : pouch, smooth(p.grab() - 1f));
                }
                w = 1f;
            }
        } else if (tip != null) {
            at = tip;
            w = reach(p.breakOpen());
        }
        if (!v.openFrame() && p.magDrop() > 0) {
            w = reach(p.magDrop());
            at = new float[]{0f, -5.5f - p.magDrop() * 4.5f, 0.2f};
        }
        return at == null || w <= 0.01f ? null : new float[]{w, at[0], at[1], at[2]};
    }

    /**
     * Where the support hand is on a long gun, in gun space (x, y, z): on the forend, riding it back on a pump and down on a break action,
     * or off at the cylinder, the belt cover or the magazine while those work.
     */
    static float[] supportTarget(GunView v, GunMotion.Parts p) {
        float[] f = forend(v);
        float y = f[0], z = f[1];
        if (v.type() == GunLayout.Type.LONG_GUN)
            z += p.pump() * FOREND_TRAVEL;
        if (p.breakOpen() > 0 && v.type() == GunLayout.Type.LONG_GUN) { // a break action, or a core breech venting
            float[] h = breakHinge(v);
            float[] at = turned(y, z, h[0], h[1], -(v.openFrame() ? 30f : 38f) * p.breakOpen());
            y = at[0];
            z = at[1];
        }
        float[] out = {0f, y, z};
        if (p.vent() >= 0f && v.type() == GunLayout.Type.LONG_GUN) {
            // A core breech vent: up over the barrels, down on them, off again, under to the forend, and up with it.
            float[] h = breakHinge(v);
            float zm = -v.len() * 0.5f, top = v.r() + 0.25f, ang = -38f * p.breakOpen();
            float[] on = turned(top, zm, h[0], h[1], ang);
            float[] closedFore = forend(v);
            float t = p.vent();
            float[] above = {0f, top + 3f, zm};
            if (t < 0.14f)
                out = lerp3(new float[]{0f, closedFore[0], closedFore[1]}, above, smooth(t / 0.14f));
            else if (t < 0.19f) {
                float w = (t - 0.14f) / 0.05f;
                out = lerp3(above, new float[]{0f, top, zm}, w * w);
            } else if (t < 0.26f)
                out = new float[]{0f, on[0], on[1]};
            else if (t < 0.4f)
                out = lerp3(new float[]{0f, on[0], on[1]}, above, smooth((t - 0.26f) / 0.14f));
            else if (t < 0.55f)
                out = lerp3(above, new float[]{0f, y, z}, smooth((t - 0.4f) / 0.15f));
        } else if (v.has("F") && p.cylinderOut() > 0)
            out = between(out, reach(p.cylinderOut()), -1.25f * p.cylinderOut() - 0.5f, CYL_Y, (CYL_Z0 + CYL_Z1) / 2f);
        if (v.type() == GunLayout.Type.MACHINE_GUN && p.cover() > 0) {
            float[] at = turned(1.75f, 0.5f, 1.5f, 6f, -55f * p.cover());
            out = between(out, reach(p.cover()), 0f, at[0] + 1f, at[1]);
        }
        if (p.magDrop() > 0 && !v.has("F") && !v.has("Y") && !v.has("b") && v.mag() != MagazineSize.BELT)
            out = between(out, reach(p.magDrop()), 0f, recvY(v)[0] - v.magLength() * 0.5f - p.magDrop() * 5f,
                    v.type() == GunLayout.Type.MACHINE_GUN ? 2f : 1.125f);
        return out;
    }

    /** Where the support hand takes a long gun: under the barrels, midway along the forend (y, z). */
    static float[] forend(GunView v) {
        float fe = GunView.snap(Math.min(Math.max(v.len() * 0.55f + 1f, 3f), 10f), 0.25f);
        return new float[]{barrelsBottom(v) - 0.5f, -fe * 0.6f};
    }

    static float barrelsBottom(GunView v) {
        return v.has("D") ? -v.r() - 2 * v.r2() : -v.r();
    }

    private static void barrels(GunMesh m, GunView v) {
        float start = breech(v), end = front(v), r = v.r(), len = start - end;
        m.part(v, "B");
        m.rod(0, 0, r, start, end, Tile.BRUSHED);
        if (v.has("D")) {
            float y = -r - v.r2(), end2 = start - Math.max(v.len2(), 1f);
            m.part(v, "D");
            m.rod(0, y, v.r2(), start, end2, Tile.BRUSHED);
            float b = Math.max(0.125f, v.r2() - 0.25f);
            m.box(-b, y - b, end2 - 0.0625f, b, y + b, end2, Tile.BORE);
        }
        // Iron sights: a front post as tall as the rear notch's line, so the two line up when aimed.
        if (v.longGun() && !v.has("S")) {
            m.part(v, "B").box(-0.125f, r, end + 0.25f, 0.125f, Math.max(r + 0.5f, v.top() + 0.25f), end + 0.75f, Tile.METAL);
        }
        if (v.openFrame() && !v.has("S"))
            m.part(v, "B").box(-0.0625f, r, end + 0.25f, 0.0625f, Math.max(r + 0.25f, v.top() + 0.0625f), end + 0.75f, Tile.METAL);
        if (v.has("H"))
            m.part(v, "H").rod(0, 0, r + 0.25f, start, start - Math.max(1f, len * 0.7f), Tile.VENT);
        if (v.has("Z")) {
            float stop = start - Math.min(len - 0.5f, 6f);
            m.part(v, "Z");
            m.rings(0, 0, r + 0.25f, start - 0.25f, stop, Math.max(1, (int) ((start - stop) / 0.75f)), 0.25f, Tile.RIBBED);
        }
        if (v.has("J"))
            m.part(v, "J").rod(0, 0, r + 0.25f, start - len * 0.3f, start - len * 0.65f, Tile.PANEL);
        if (v.has("Q"))
            m.part(v, "Q").rod(0, 0, r + 0.25f, end, end + Math.min(2.5f, len * 0.4f), Tile.BRUSHED);
        if (v.has("P")) {
            float f = switch (v.variant("P")) {
                case "thread_breech" -> 0.18f;
                case "thread_muzzle" -> 0.88f;
                default -> 0.5f;
            };
            float z = GunView.snap(start - len * f, 0.25f);
            m.part(v, "P").rod(0, 0, r + 0.25f, z - 0.375f, z + 0.375f, Tile.KNURL);
        }
        if (v.has("V")) {
            m.part(v, "V");
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
            m.part(v, "N");
            m.rod(0, 0, r + 0.125f, z, z - 1.5f, Tile.METAL);
            m.box(-r - 0.1875f, -r * 0.5f, z - 1.125f, r + 0.1875f, r * 0.5f, z - 0.625f, Tile.BORE);
            z -= 1.5f;
        }
        if (v.has("M")) {
            float rr = Math.max(0.875f, r + 0.375f);
            m.part(v, "M");
            m.rod(0, 0, rr, z, z - 4.5f, Tile.BRUSHED);
            m.rod(0, 0, rr + 0.0625f, z, z - 0.5f, Tile.KNURL);
            m.rod(0, 0, rr + 0.0625f, z - 4f, z - 4.5f, Tile.KNURL);
            z -= 4.5f;
        }
        float b = v.bore();
        m.box(-b, -b, z - 0.0625f, b, b, z, Tile.BORE);
    }

    /** Hung under the barrel: a tube magazine, a counterweight, a bipod, a bayonet. */
    private static void underBarrel(GunMesh m, GunView v) {
        float start = breech(v), end = front(v), len = start - end, under = barrelsBottom(v);
        float y = under - 0.375f;
        if (v.has("E")) {
            float stop = start - Math.max(Math.min(len * 0.85f, len - 0.5f), 2f);
            m.part(v, "E");
            m.rod(0, y, 0.375f, start, stop, Tile.METAL);
            m.rod(0, y, 0.4375f, stop, stop - 0.25f, Tile.KNURL);
            y -= 0.75f;
        }
        if (v.has("C"))
            m.part(v, "C").box(-0.5f, y - 0.75f, end + 0.25f, 0.5f, y + 0.375f, end + 2f, Tile.METAL);
        if (v.has("I")) {
            float zc = GunView.snap(end + len * 0.25f, 0.25f);
            m.part(v, "I");
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
            m.part(v, "a");
            m.rod(0, 0, v.r() + 0.125f, end, end + 0.75f, Tile.METAL);
            m.box(-0.0625f, under - 0.75f, end - 5.5f, 0.0625f, under - 0.25f, end + 0.5f, Tile.BRUSHED);
            m.push();
            m.turnX(-25f, under - 0.25f, end - 5.5f);
            m.box(-0.0625f, under - 0.5f, end - 6.5f, 0.0625f, under - 0.25f, end - 5.5f, Tile.BRUSHED);
            m.pop();
        }
    }

    /** A hollow square tube of half-width {@code r} about the sight line at height {@code y}: four walls, so the view down it is clear. */
    private static void tube(GunMesh m, float y, float r, float z0, float z1, Tile tile) {
        float wall = 0.125f;
        m.box(-r, y + r - wall, z0, r, y + r, z1, tile);
        m.box(-r, y - r, z0, r, y - r + wall, z1, tile);
        m.box(-r, y - r + wall, z0, -r + wall, y + r - wall, z1, tile);
        m.box(r - wall, y - r + wall, z0, r, y + r - wall, z1, tile);
    }

    /** The colour an illuminated reticle shines. */
    static int lamp(GunView v) {
        return switch (v.variant("e")) {
            case "ring_reticle" -> 0xFFFFA030;
            case "cross_reticle" -> 0xFF40FF60;
            default -> 0xFFFF3030;
        };
    }

    /** An optic on the top, or a lit dot on it. */
    private static void sight(GunMesh m, GunView v) {
        if (!v.has("S"))
            return;
        float top = v.top();
        float z0 = v.opticZ();
        int lamp = lamp(v);
        m.part(v, "S");
        if (v.variant("S").equals("scope")) {
            float zs = z0 - 0.5f, ze = zs + v.scopeLength(), y = top + 1f;
            m.box(-0.25f, top, zs + 0.5f, 0.25f, y, zs + 1f, Tile.METAL);
            m.box(-0.25f, top, ze - 1f, 0.25f, y, ze - 0.5f, Tile.METAL);
            tube(m, y, 0.75f, zs, ze, Tile.BRUSHED);
            tube(m, y, 0.875f, zs - 1.25f, zs, Tile.METAL);
            tube(m, y, 0.875f, ze, ze + 1f, Tile.METAL);
            m.box(-0.25f, y + 0.375f, (zs + ze) / 2 - 0.25f, 0.25f, y + 0.75f, (zs + ze) / 2 + 0.25f, Tile.KNURL);
            if (m.sighted)
                scopeView(m, v, y, ze, lamp);
            else // the objective's glass, seen from outside
                m.pane(-0.75f, y - 0.75f, zs - 1f, 0.75f, y + 0.75f, zs - 0.9375f, 0x4096C8FF, false);
        } else {
            m.box(-0.5f, top, z0, 0.5f, top + 0.25f, z0 + 1.5f, Tile.METAL);
            m.box(-0.5f, top + 0.25f, z0, -0.375f, top + 1.25f, z0 + 0.5f, Tile.METAL);
            m.box(0.375f, top + 0.25f, z0, 0.5f, top + 1.25f, z0 + 0.5f, Tile.METAL);
            m.box(-0.5f, top + 1.25f, z0, 0.5f, top + 1.375f, z0 + 0.5f, Tile.METAL);
            // See-through: a pale tint on the window, and a lit dot you can look through.
            m.pane(-0.375f, top + 0.25f, z0 + 0.1875f, 0.375f, top + 1.25f, z0 + 0.25f, 0x2896C8FF, false);
            if (v.has("e"))
                m.pane(-0.0625f, top + 0.6875f, z0 + 0.15625f, 0.0625f, top + 0.8125f, z0 + 0.1875f, lamp & 0xFFFFFF | 0x99000000, true);
        }
    }

    /**
     * Looking through a scope: a dark plate behind the eyepiece frames the lens window (the lens image,
     * {@link GunLens}), with the reticle across it. Until the first lens frame is ready the window is dark.
     */
    private static void scopeView(GunMesh m, GunView v, float y, float ze, int lamp) {
        float h = v.lensHalf(), zl = v.lensZ(), pz = ze + 1f, big = 4f;
        m.colour(0xFF121212);
        m.box(-big, y + h, pz, big, y + big, pz + 0.25f, Tile.METAL);
        m.box(-big, y - big, pz, big, y - h, pz + 0.25f, Tile.METAL);
        m.box(-big, y - h, pz, -h, y + h, pz + 0.25f, Tile.METAL);
        m.box(h, y - h, pz, big, y + h, pz + 0.25f, Tile.METAL);
        if (GunLens.ready())
            m.lens(-h, y - h, h, y + h, zl);
        else
            m.box(-h, y - h, zl, h, y + h, zl + 0.0625f, Tile.METAL);
        // Fine hairlines on the lens, lit when the scope has an illuminated reticle.
        float zr = zl + 0.09375f, t = 0.015625f;
        if (v.has("e")) {
            m.colour(lamp);
            m.glow(-h, y - t, zr, h, y + t, zr + 0.03125f, Tile.LAMP);
            m.glow(-t, y - h, zr, t, y + h, zr + 0.03125f, Tile.LAMP);
        } else {
            m.colour(0xFF101010);
            m.box(-h, y - t, zr, h, y + t, zr + 0.03125f, Tile.LAMP);
            m.box(-t, y - h, zr, t, y + h, zr + 0.03125f, Tile.LAMP);
        }
    }

    /** On the sides: a selector, a hydraulic buffer. */
    private static void sides(GunMesh m, GunView v) {
        float hw = v.halfWidth();
        float at = switch (v.type()) {
            case HANDGUN -> -0.5f;
            case LONG_GUN -> 3.5f;
            case MACHINE_GUN -> 6.5f;
        };
        float base = v.type() == GunLayout.Type.HANDGUN ? -1.25f : -0.5f;
        if (v.has("L"))
            m.part(v, "L").box(-hw - 0.125f, base - 0.125f, at - 0.375f, -hw, base + 0.125f, at + 0.375f, Tile.KNURL);
        if (v.has("d"))
            m.part(v, "d").rod(-hw - 0.25f, base + 0.5f, 0.25f, at + 0.5f, at + 2f, Tile.BRUSHED);
    }

    private static int darken(int argb) {
        int r = (argb >> 16 & 0xFF) * 6 / 10, g = (argb >> 8 & 0xFF) * 6 / 10, b = (argb & 0xFF) * 6 / 10;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
