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
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.net.MortarPutPayload;
import io.github.soundgoodizerfan.feedback.net.MortarTakePayload;
import io.github.soundgoodizerfan.feedback.potion.BrewingStandBlockEntity;
import io.github.soundgoodizerfan.feedback.potion.BrewingStandMenu;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The brewing stand's grinding bowl (the Potion Craft mortar): plants float loose in a stone bowl with a little gravity and a pestle the player
 * grabs. Pounding the pestle down smashes (a hard fall onto a plant; hard stuff needs a harder blow), rubbing it side to side over a plant
 * grinds (hard stuff resists it). Each plant carries a crush fraction 0..1, shown as its icon shrinking, dust and a small arc.
 * <p>
 * All of it is client side and allocation free: parallel arrays, indices 0..N-1 are the bowl's places (they mirror the menu's mortar slots, which
 * the server owns) and index N is the pestle, a body like the rest. Physics runs in fixed 1/120 s substeps once per frame with the frame time
 * clamped. The server hears only {@link MortarPutPayload} (a plant goes in) and {@link MortarTakePayload} (a plant comes out, with its fraction,
 * which the server clamps and quantises; a cheated fraction costs nothing important and is accepted).
 * What drives the play: the plant's material hardness (log-scaled to 0..1, crystals and minerals at least 0.7) sets the blow it takes to break
 * (impact speed threshold), how much a blow does, how far rubbing alone can go and how fast, and the sound's pitch. The bowl is the subject of
 * the screen, so it draws in its own stone colours rather than the skin's.
 */
final class MortarBowl {

    private static final int N = BrewingStandMenu.MORTAR_SLOTS, PESTLE = N;
    /** Heavy things: gravity, the little bounce left, floor drag per second (plants, tall flowers), air drag, and the fastest a plant may leave the hand. Sizes are the drawn icon in px: a plant is about the pestle's size, a tall flower much bigger, and collision radii follow. */
    private static final float G = 1000f, STEP = 1f / 120f, REST = 0.12f, REST_TALL = 0f, FLOOR = 12f, FLOOR_TALL = 26f, AIR = 1.6f, THROW = 220f, ICON = 36f, ICON_TALL = 60f, RAD_K = 0.42f,
            MASS = 0.5f, MASS_TALL = 0.15f, R_HEAD = 11f, CORNER = 20f, WALL = 8f, LIP = 12, FLOOR_THICK = 14;
    private static final int STONE_DARK = 0xFF565149, STONE = 0xFF77716B, STONE_LIGHT = 0xFFA39C94, CAVITY = 0xFF2A2623, CAVITY_LOW = 0xFF3A342F, DUST = 0xFFE2D5B0, PEST = 0xFF4A4642, PEST_LIGHT = 0xFF6A645E, PEST_DARK = 0xFF2E2B28, SKIN = 0xFFE8C9A0,
            WOOD_DARK = 0xFF6B4E32, WOOD = 0xFF9A7448, HEAD = 0xFF8D867E, HEAD_LIGHT = 0xFFB9B2A8;

    /** The pestle rests on the sand; only a blow harder than this (px/s, down or sideways) displaces grains, sinking in by the excess times SINK px per px/s. */
    private static final float SOFT = 380f, SINK = 0.03f, PLOW_TIME = 0.15f;
    private float plowT;
    private static final int PESTLE_H = 34, GW = BrewingStandMenu.BOWL_W, GH = BrewingStandMenu.BOWL_H, POOL = 256, TOTAL = 2500, GRAINS = 400, GRAINS_TALL = 700;
    /** Grains are one pixel each: gravity in px/s2 for a loose one, and the fixed rate the sand grid steps at. */
    private static final float GRAIN_G = 900f, GRID_STEP = 1f / 60f;
    private static final String[] PCT = new String[101];

    static {
        for (int i = 0; i <= 100; i++)
            PCT[i] = i + "%";
    }

    private final BrewingStandMenu menu;
    private final boolean[] on = new boolean[N + 1], touch = new boolean[N];
    private final float[] x = new float[N + 1], y = new float[N + 1], vx = new float[N + 1], vy = new float[N + 1], rad = new float[N + 1], inverseMass = new float[N + 1];
    private final float[] size = new float[N], rest = new float[N + 1], floorDrag = new float[N + 1], progress = new float[N], hard = new float[N], cool = new float[N], pending = new float[N];
    /** Each plant's own colours (its sprite's); its fragments are drawn from them, relative to the plant, so the heap travels with it. */
    private final int[][] palette = new int[N][];
    /** The crushed matter: a falling-sand grid over the bowl, one cell per pixel. A cell holds a colour index (+1, 0 empty) and the slot of the plant that made it. */
    private final byte[] cell = new byte[GW * GH], owner = new byte[GW * GH], solid = new byte[GW * GH];
    private final int[] given = new int[N], grainCount = new int[N];
    private final float[] gx = new float[POOL], gy = new float[POOL], gvx = new float[POOL], gvy = new float[POOL];
    private final byte[] gcol = new byte[POOL], gown = new byte[POOL];
    private final boolean[] glive = new boolean[POOL];
    private final float[] labelX = new float[N], labelY = new float[N];
    private final int[] labelN = new int[N];
    private int total, gridSeed = 12345, gridParity;
    private float gridAcc;
    /** The pestle block in grid cells, [pl, pr) x [pt, pb): solid to grains. */
    private int pl, pr, pt, pb;
    private final RandomSource rng = RandomSource.create();
    private final Specks specks = new Specks(48);
    private int bx, by, held = -1, accent = 0x80FFFFFF;
    private boolean pestleHeld;
    private float grabX, grabY, mouseX, mouseY, dropX, dropT, shakeStart = -100f, grindSound;
    private long lastNanos;
    private boolean haveMouse;

    MortarBowl(BrewingStandMenu menu) {
        this.menu = menu;
        rad[PESTLE] = R_HEAD;
        rest[PESTLE] = 0.2f;
        floorDrag[PESTLE] = 5f;
        inverseMass[PESTLE] = 1f / 5f;
        for (int i = 0; i < N; i++) {
            rad[i] = ICON * RAD_K;
            inverseMass[i] = MASS;
            rest[i] = REST;
            floorDrag[i] = FLOOR;
            size[i] = ICON;
        }
        on[PESTLE] = true;
    }

    void layout(int left, int top) {
        int nx = left + BrewingStandMenu.BOWL_X, ny = top + BrewingStandMenu.BOWL_Y;
        // A resize moves the panel: carry the bodies with it.
        if (x[PESTLE] != 0f)
            for (int i = 0; i <= N; i++) {
                x[i] += nx - bx;
                y[i] += ny - by;
            }
        boolean moved = nx != bx || ny != by;
        bx = nx;
        by = ny;
        if (moved) {
            // The sand grid is anchored to the panel: a resize starts it over (the plants make their grains again).
            java.util.Arrays.fill(cell, (byte) 0);
            java.util.Arrays.fill(given, 0);
            java.util.Arrays.fill(glive, false);
            java.util.Arrays.fill(grainCount, 0);
            total = 0;
            for (int gy2 = 0; gy2 < GH; gy2++)
                for (int gx2 = 0; gx2 < GW; gx2++)
                    solid[gy2 * GW + gx2] = (byte) (inCavity(bx + gx2 + 0.5f, by + gy2 + 0.5f) ? 0 : 1);
        }
        if (x[PESTLE] == 0f) {
            x[PESTLE] = bx + BrewingStandMenu.BOWL_W - 22;
            y[PESTLE] = by + BrewingStandMenu.BOWL_H - FLOOR_THICK - 8;
        }
    }

    // ---- bowl shape: the cavity the bodies live in -------------------------------------------------------------------------------------------------

    private float left() {
        return bx + WALL;
    }

    private float right() {
        return bx + BrewingStandMenu.BOWL_W - WALL;
    }

    private float top() {
        return by + LIP + 5;
    }

    private float bottom() {
        return by + BrewingStandMenu.BOWL_H - FLOOR_THICK;
    }

    boolean inside(double mx, double my) {
        return mx >= bx && mx < bx + BrewingStandMenu.BOWL_W && my >= by && my < by + BrewingStandMenu.BOWL_H;
    }

    /** Keep body {@code i} inside the cavity (flat floor, rounded lower corners, straight walls), bouncing it; the speed it hit with, for sound. */
    private float contain(int i) {
        float r = rad[i], l = left() + r, rt = right() - r, floor = bottom() - r, hit = 0f;
        float ce = Math.max(CORNER, r + 1f), cl = left() + ce, cr = right() - ce, cy = bottom() - ce;
        if (y[i] > cy && (x[i] < cl || x[i] > cr)) {
            float cx = x[i] < cl ? cl : cr, dx = x[i] - cx, dy = y[i] - cy, d = Mth.sqrt(dx * dx + dy * dy), max = ce - r;
            if (d > max && d > 0.0001f) {
                float nx = dx / d, ny = dy / d, vn = vx[i] * nx + vy[i] * ny;
                x[i] = cx + nx * max;
                y[i] = cy + ny * max;
                if (vn > 0f) {
                    hit = vn;
                    vx[i] -= (1f + rest[i]) * vn * nx;
                    vy[i] -= (1f + rest[i]) * vn * ny;
                }
            }
        } else {
            if (x[i] < l) {
                x[i] = l;
                hit = Math.max(hit, -vx[i]);
                if (vx[i] < 0f)
                    vx[i] = -vx[i] * rest[i];
            } else if (x[i] > rt) {
                x[i] = rt;
                hit = Math.max(hit, vx[i]);
                if (vx[i] > 0f)
                    vx[i] = -vx[i] * rest[i];
            }
            if (y[i] > floor) {
                y[i] = floor;
                hit = Math.max(hit, vy[i]);
                if (vy[i] > 0f)
                    vy[i] = -vy[i] * rest[i];
                if (Math.abs(vy[i]) < 25f)
                    vy[i] = 0f;
                vx[i] *= 1f - Math.min(1f, floorDrag[i] * STEP);
            }
        }
        if (y[i] < top() + r) {
            y[i] = top() + r;
            if (vy[i] < 0f)
                vy[i] = -vy[i] * rest[i];
        }
        return hit;
    }

    // ---- what the plants are -------------------------------------------------------------------------------------------------------------------------

    /** Hardness 0..1 from the material: plants sit near 0.1, minerals around 4 and crystals 40 in the material tables, so it is log scaled; crystals and minerals are at least 0.7. */
    private static float hardnessOf(ItemStack stack) {
        var level = Minecraft.getInstance().level;
        if (level == null)
            return 0.1f;
        var found = Material.find(ClientMaterials.on(level), stack);
        if (found.isEmpty())
            return 0.1f;
        Material m = found.get().material();
        float h = Mth.clamp((float) Math.log10(Math.max(0.1f, m.hardness().value()) / 0.1f) / 2.2f, 0f, 1f);
        var kind = m.kind();
        if (kind.isPresent() && (kind.get() == Material.Kind.CRYSTAL || kind.get() == Material.Kind.MINERAL))
            h = Math.max(h, 0.7f);
        return h;
    }

    /** The fastest a blow must land at to break a plant of this hardness, in px/s. */
    private static float smashSpeed(float hardness) {
        return 35f + 420f * hardness;
    }

    /** How far rubbing alone can take a plant: soft ones all the way, hard ones barely at all (they need smashing). */
    private static float grindCap(float hardness) {
        return Mth.clamp(1.02f - 0.9f * hardness * hardness, 0f, 1f);
    }

    private ItemStack stackAt(int i) {
        return menu.slots.get(BrewingStandMenu.MORTAR_START + i).getItem();
    }

    ItemStack stack(int i) {
        return stackAt(i);
    }

    /** Whether place {@code i} holds a plant the screen is showing as an object. */
    boolean present(int i) {
        return on[i];
    }

    float fraction(int i) {
        return progress[i];
    }

    /** Whether a click on the bowl with this stack may put it in: a plant, and room. */
    boolean accepts(ItemStack stack) {
        var level = Minecraft.getInstance().level;
        if (stack.isEmpty() || level == null || BrewingStandBlockEntity.potionOf(level, stack).isEmpty())
            return false;
        for (int i = 0; i < N; i++)
            if (!on[i] && stackAt(i).isEmpty() && pending[i] <= 0f)
                return true;
        return false;
    }

    void put(int menuSlot) {
        PacketDistributor.sendToServer(new MortarPutPayload(menu.containerId, menuSlot));
        dropX = mouseX;
        dropT = 1.5f;
    }

    private void take(int i, boolean sound) {
        PacketDistributor.sendToServer(new MortarTakePayload(menu.containerId, i, Math.round(progress[i] * 20f)));
        on[i] = false;
        clearGrains(i);
        touch[i] = false;
        pending[i] = 1.5f;
        if (held == i)
            held = -1;
        if (sound)
            GuiSkin.play(SoundEvents.ITEM_PICKUP, 0.9f, 0.4f);
    }

    /** Closing the screen: everything still in the bowl goes back at the fraction it has. */
    void close() {
        for (int i = 0; i < N; i++)
            if (on[i])
                take(i, false);
    }

    /** The plant whose icon is under the point, or -1. */
    private int iconAt(double mx, double my) {
        for (int i = N - 1; i >= 0; i--)
            if (on[i] && progress[i] < 1f && Math.hypot(mx - x[i], my - y[i]) <= rad[i] + 3)
                return i;
        return -1;
    }

    /** The place under the mouse (the held one first), or -1. */
    int hovered(double mx, double my) {
        if (held >= 0)
            return held;
        for (int i = N - 1; i >= 0; i--)
            if (on[i] && progress[i] < 1f && Math.hypot(mx - x[i], my - y[i]) <= rad[i] + 3)
                return i;
        return grainAt(mx, my);
    }

    private boolean onPestle(double mx, double my) {
        // The whole block is the handle; its flat foot, as wide as the block, is the physics circle.
        float dy = y[PESTLE] + R_HEAD - (float) my;
        return dy >= -1 && dy <= PESTLE_H && Math.abs(mx - x[PESTLE]) <= R_HEAD + 1;
    }

    // ---- input ---------------------------------------------------------------------------------------------------------------------------------------

    /** A press on the bowl: drop the cursor's plant in, grab the pestle, or grab a plant. Returns whether the bowl used it. */
    boolean click(double mx, double my, int button) {
        if (!inside(mx, my))
            return false;
        ItemStack carried = menu.getCarried();
        if (!carried.isEmpty()) {
            if (accepts(carried))
                put(-1);
            return true;
        }
        if (button != 0)
            return true;
        if (onPestle(mx, my)) {
            pestleHeld = true;
            grabX = x[PESTLE] - (float) mx;
            grabY = y[PESTLE] - (float) my;
            vx[PESTLE] = vy[PESTLE] = 0f;
            GuiSkin.play(SoundEvents.STONE_HIT, 1.4f, 0.2f);
            return true;
        }
        int i = iconAt(mx, my);
        if (i < 0)
            i = grainAt(mx, my);
        if (i >= 0) {
            held = i;
            // Picked up, a plant's grains stay in the bowl until it is put back in the inventory (released outside the bowl).
            if (progress[i] >= 1f) {
                x[i] = (float) mx;
                y[i] = (float) my;
            }
            grabX = x[i] - (float) mx;
            grabY = y[i] - (float) my;
            vx[i] = vy[i] = 0f;
            touch[i] = false;
        }
        return true;
    }

    void release() {
        if (pestleHeld) {
            pestleHeld = false;
            // It keeps the speed of the hand, so letting go onto a plant is itself a blow.
            vx[PESTLE] = Mth.clamp(vx[PESTLE], -700f, 700f);
            vy[PESTLE] = Mth.clamp(vy[PESTLE], -700f, 900f);
        }
        if (held >= 0) {
            int i = held;
            held = -1;
            if (inside(mouseX, mouseY)) {
                vx[i] = Mth.clamp(vx[i], -THROW, THROW) * (size[i] == ICON_TALL ? 0f : 1f);
                vy[i] = Mth.clamp(vy[i], -THROW, THROW) * (size[i] == ICON_TALL ? 0f : 1f);
            } else {
                specks.burst(mouseX, mouseY, 6, 1.2f, 1.5f, DUST, 14, 1);
                take(i, true);
            }
        }
    }

    // ---- the frame -----------------------------------------------------------------------------------------------------------------------------------

    /** Advance the bowl to now: sync with the server's slots, then physics in substeps with the frame time clamped. */
    void step(float mx, float my) {
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0f : Mth.clamp((now - lastNanos) / 1e9f, 0f, 0.05f);
        lastNanos = now;
        float fromX = haveMouse ? mouseX : mx, fromY = haveMouse ? mouseY : my;
        haveMouse = true;
        mouseX = mx;
        mouseY = my;
        dropT -= dt;
        sync(dt);
        gridAcc += dt;
        for (int n = 0; n < 3 && gridAcc >= GRID_STEP; n++, gridAcc -= GRID_STEP)
            gridStep();
        gridAcc = Math.min(gridAcc, GRID_STEP);
        int steps = Math.min(8, Math.round(dt / STEP));
        for (int s = 1; s <= steps; s++) {
            float u = s / (float) steps;
            substep(fromX + (mx - fromX) * u, fromY + (my - fromY) * u);
        }
    }

    /** Places the server fills get an object dropped in from the top; places it emptied lose theirs. */
    private void sync(float dt) {
        for (int i = 0; i < N; i++) {
            ItemStack stack = stackAt(i);
            rad[i] = size[i] * RAD_K * (1f - 0.25f * progress[i]);
            pending[i] = stack.isEmpty() ? 0f : Math.max(0f, pending[i] - dt);
            if (!on[i] && !stack.isEmpty() && pending[i] <= 0f) {
                on[i] = true;
                // Tall flowers (by item tag) are bigger, heavier and dead on landing.
                boolean tall = stack.is(net.minecraft.tags.ItemTags.TALL_FLOWERS);
                size[i] = tall ? ICON_TALL : ICON;
                rad[i] = size[i] * RAD_K;
                inverseMass[i] = tall ? MASS_TALL : MASS;
                rest[i] = tall ? REST_TALL : REST;
                floorDrag[i] = tall ? FLOOR_TALL : FLOOR;
                float cx = dropT > 0f ? Mth.clamp(dropX, left() + rad[i], right() - rad[i]) : left() + rad[i] + rng.nextFloat() * (right() - left() - 2 * rad[i]);
                x[i] = cx;
                y[i] = top() + rad[i];
                vx[i] = (rng.nextFloat() - 0.5f) * 40f;
                vy[i] = 0f;
                progress[i] = stack.getOrDefault(FDataComponents.CRUSHED_FRACTION.get(), 0f);
                hard[i] = hardnessOf(stack);
                palette[i] = CrushPalette.of(stack);
                clearGrains(i);
                cool[i] = 0f;
                touch[i] = false;
            } else if (on[i] && stack.isEmpty()) {
                on[i] = false;
                clearGrains(i);
                if (held == i)
                    held = -1;
            }
        }
    }

    private void substep(float tx, float ty) {
        for (int i = 0; i < N; i++) {
            cool[i] = Math.max(0f, cool[i] - STEP);
            if (!on[i] || i == held || progress[i] >= 1f)
                continue;
            vy[i] += G * STEP;
            vx[i] *= 1f - AIR * STEP;
            vy[i] *= 1f - 0.5f * AIR * STEP;
            x[i] += vx[i] * STEP;
            y[i] += vy[i] * STEP;
        }
        if (held >= 0) {
            float nx = tx + grabX, ny = ty + grabY;
            vx[held] += ((nx - x[held]) / STEP - vx[held]) * 0.2f;
            vy[held] += ((ny - y[held]) / STEP - vy[held]) * 0.2f;
            x[held] = nx;
            y[held] = ny;
        }
        if (pestleHeld) {
            float nx = tx + grabX, ny = ty + grabY, ox = x[PESTLE], oy = y[PESTLE];
            x[PESTLE] = nx;
            y[PESTLE] = ny;
            contain(PESTLE);
            float handDown = (y[PESTLE] - oy) / STEP, handSide = (x[PESTLE] - ox) / STEP;
            sandSupport(handDown, handSide);
            vx[PESTLE] = (x[PESTLE] - ox) / STEP;
            vy[PESTLE] = (y[PESTLE] - oy) / STEP;
        } else {
            vy[PESTLE] += G * STEP;
            x[PESTLE] += vx[PESTLE] * STEP;
            y[PESTLE] += vy[PESTLE] * STEP;
        }
        for (int i = 0; i < N; i++)
            if (on[i] && i != held && progress[i] < 1f) {
                float hit = contain(i);
                if (hit > 160f)
                    GuiSkin.play(SoundEvents.STONE_HIT, 1.2f + rng.nextFloat() * 0.3f, Math.min(0.3f, hit / 1500f));
            }
        if (!pestleHeld) {
            sandSupport(vy[PESTLE], vx[PESTLE]);
            float hit = contain(PESTLE);
            if (hit > 120f) {
                GuiSkin.play(SoundEvents.STONE_HIT, 0.8f, Math.min(0.5f, hit / 1000f));
                shakeStart = GuiStyle.time();
            }
            vx[PESTLE] *= 1f - 0.8f * STEP;
        }
        for (int i = 0; i < N; i++)
            if (on[i] && i != held && progress[i] < 1f)
                pestlePair(i);
        for (int i = 0; i < N; i++)
            for (int j = i + 1; j < N; j++)
                if (on[i] && on[j] && i != held && j != held && progress[i] < 1f && progress[j] < 1f)
                    pair(i, j, 0.3f);
        for (int i = 0; i <= N; i++)
            if (on[i] && i != held && (i == N || progress[i] < 1f))
                contain(i);
    }

    /** Two plants: equal masses, pushed apart and their closing speed mostly cancelled. */
    private void pair(int a, int b, float e) {
        float dx = x[b] - x[a], dy = y[b] - y[a], min = rad[a] + rad[b], d2 = dx * dx + dy * dy;
        if (d2 >= min * min)
            return;
        float d = Mth.sqrt(d2);
        if (d < 0.0001f) {
            dx = 1f;
            dy = 0f;
            d = 1f;
        }
        float nx = dx / d, ny = dy / d, ia = inverseMass[a], ib = inverseMass[b], sum = ia + ib, over = min - d;
        x[a] -= nx * over * ia / sum;
        y[a] -= ny * over * ia / sum;
        x[b] += nx * over * ib / sum;
        y[b] += ny * over * ib / sum;
        float vn = (vx[b] - vx[a]) * nx + (vy[b] - vy[a]) * ny;
        if (vn < 0f) {
            float j = -(1f + e) * vn / sum;
            vx[a] -= j * ia * nx;
            vy[a] -= j * ia * ny;
            vx[b] += j * ib * nx;
            vy[b] += j * ib * ny;
        }
    }

    /**
     * The pestle against plant {@code o}. A new contact at a closing speed is a blow; staying pressed on a plant that is under the head and rubbing
     * across it is grinding. A plant under a held pestle is pinned between head and floor, not pushed out from under it.
     */
    private void pestlePair(int o) {
        float dx = x[o] - x[PESTLE], dy = y[o] - y[PESTLE], d = Mth.sqrt(dx * dx + dy * dy), min = R_HEAD + rad[o];
        if (d >= min) {
            touch[o] = false;
            return;
        }
        float nx = d < 0.0001f ? 0f : dx / d, ny = d < 0.0001f ? 1f : dy / d;
        // Closing speed along the line between them: how hard the head came at the plant.
        float closing = (vx[PESTLE] - vx[o]) * nx + (vy[PESTLE] - vy[o]) * ny;
        if (!touch[o]) {
            touch[o] = true;
            if (closing > 20f)
                blow(o, closing);
        }
        boolean pinned = pestleHeld && y[o] >= y[PESTLE] - 1f && Math.abs(dx) < R_HEAD + 0.3f * rad[o];
        if (pinned) {
            // Dragged along under the head, squeezed to the floor.
            x[o] += (x[PESTLE] - x[o]) * Math.min(1f, 14f * STEP);
            vx[o] = vx[PESTLE] * 0.8f;
            float speed = Math.abs(vx[PESTLE]);
            if (speed > 25f && vy[PESTLE] > -30f && progress[o] < grindCap(hard[o])) {
                progress[o] = Math.min(grindCap(hard[o]), progress[o] + speed * STEP / 1200f * (1f - 0.7f * hard[o]));
                grindSound -= STEP;
                if (grindSound <= 0f) {
                    grindSound = 0.12f;
                    GuiSkin.play(SoundEvents.GRINDSTONE_USE, 1.5f - 0.7f * hard[o], 0.12f);
                    specks.burst(x[o], y[o] + 4, 2, 0.5f, 0.6f, dustOf(o), 12, 1);
                }
            }
            return;
        }
        inverseMass[PESTLE] = pestleHeld ? 0f : 1f / 5f;
        pair(PESTLE, o, 0.2f);
        inverseMass[PESTLE] = 1f / 5f;
    }

    /** A blow of this speed on plant {@code o}: it breaks only above the plant's smash speed, more the harder it was struck. */
    private void blow(int o, float speed) {
        if (cool[o] > 0f)
            return;
        cool[o] = 0.12f;
        float need = smashSpeed(hard[o]);
        if (speed < need) {
            GuiSkin.play(SoundEvents.STONE_HIT, 1.3f, 0.15f);
            return;
        }
        float before = progress[o];
        progress[o] = Math.min(1f, progress[o] + (0.12f + 0.3f * Math.min(1f, (speed - need) / 400f)) * (1f - 0.5f * hard[o]));
        shakeStart = GuiStyle.time();
        float pitch = 1.15f - 0.45f * hard[o];
        GuiSkin.play(hard[o] < 0.3f ? SoundEvents.GRASS_BREAK : hard[o] < 0.65f ? SoundEvents.BONE_BLOCK_BREAK : SoundEvents.AMETHYST_BLOCK_BREAK, pitch, 0.5f);
        specks.burst(x[o], y[o], 4 + (int) (6 * (progress[o] - before) * 4), 1.6f, 2f, dustOf(o), 16, 1);
        vy[o] = Math.min(vy[o], -60f);
        kick(o, 6);
    }

    /** A speck colour from the plant's own palette. */
    private int dustOf(int o) {
        return palette[o] == null ? DUST : palette[o][rng.nextInt(palette[o].length)];
    }

    /** Whether a point is inside the cavity: between the walls, above the floor, and inside the rounded lower corners. */
    private boolean inCavity(float px, float py) {
        if (py < top() || py >= bottom() || px < left() || px >= right())
            return false;
        float cl = left() + CORNER, cr = right() - CORNER, cy = bottom() - CORNER;
        if (py > cy && (px < cl || px > cr)) {
            float cx = px < cl ? cl : cr;
            return (px - cx) * (px - cx) + (py - cy) * (py - cy) <= CORNER * CORNER;
        }
        return true;
    }

    private boolean free(int cx, int cy) {
        return cx >= 0 && cx < GW && cy >= 0 && cy < GH && solid[cy * GW + cx] == 0 && cell[cy * GW + cx] == 0 && !(cx >= pl && cx < pr && cy >= pt && cy < pb);
    }

    /**
     * The pestle sits on the sand: its foot cannot go below the top of the grains under it, so a soft drop or a slow push rests on the pile
     * (riding up over a mound a few pixels a step). Only a blow (or a shove) harder than {@link #SOFT} sinks in, by the excess times {@link #SINK}, and
     * only then do the grains under it give way ({@link #plow}); a hard blow also loses speed to the sand.
     */
    private void sandSupport(float down, float side) {
        int cxm = Math.round(x[PESTLE]) - bx, footRow = Math.round(y[PESTLE]) + 11 - by, top = GH;
        int boxTop = Math.max(0, footRow - PESTLE_H - 2);
        for (int c = Math.max(0, cxm - 11); c < Math.min(GW, cxm + 11) && top > boxTop; c++)
            for (int r = boxTop; r < top; r++)
                if (cell[r * GW + c] != 0) {
                    top = r;
                    break;
                }
        if (top >= GH)
            return;
        float hard = Math.max(0f, Math.max(down, Math.abs(side)) - SOFT), limit = by + top - 11 + hard * SINK;
        if (hard > 0f)
            plowT = PLOW_TIME;
        if (y[PESTLE] > limit) {
            y[PESTLE] = Math.max(limit, y[PESTLE] - 3f);
            if (down > 0f && !pestleHeld)
                vy[PESTLE] = down > SOFT ? vy[PESTLE] * 0.5f : 0f;
        }
    }

    /** The pestle moved into sand: grains inside its box are shoved out beside it (never down, never over the top), so a blow heaps sand beside it and a drag bulldozes it. Only the box's cells are visited. */
    private void plow() {
        for (int cy = Math.max(pt, 0); cy < Math.min(pb, GH); cy++)
            for (int cx = Math.max(pl, 0); cx < Math.min(pr, GW); cx++) {
                int i = cy * GW + cx;
                if (cell[i] == 0)
                    continue;
                // A grain under the block goes out sideways and up along a rising diagonal, so sand heaps beside the pestle. One with nowhere
                // to go stays where it is, hidden under the block, and falls again when the pestle moves off it.
                slide(i, cx, cy, cx - pl + 1 <= pr - cx);
            }
    }

    /**
     * Push the grain at cell {@code i} out of the pestle's box the way sand really gives way: it shoves the grains beside the box one cell
     * outward each, as a chain, until the line ends in a free cell (the nearer side first); a line wedged against the wall or already full tries
     * the rows above (the pile rises), up to eight. One that finds no room stays put.
     */
    private boolean slide(int i, int cx, int cy, boolean leftFirst) {
        for (int up = 0; up <= 8 && cy - up >= 0; up++)
            for (int s = 0; s < 2; s++) {
                boolean left = (s == 0) == leftFirst;
                int dir = left ? -1 : 1, ex = left ? pl - 1 : pr, row = (cy - up) * GW;
                for (int k = 0; k < 32; k++) {
                    int tx = ex + dir * k;
                    if (tx < 0 || tx >= GW || solid[row + tx] != 0)
                        break;
                    if (cell[row + tx] == 0) {
                        for (int j = k - 1; j >= 0; j--)
                            move(row + ex + dir * j, row + ex + dir * (j + 1));
                        move(i, row + ex);
                        return true;
                    }
                }
            }
        return false;
    }

    private int grainsFor(int i) {
        return size[i] >= ICON_TALL ? GRAINS_TALL : GRAINS;
    }

    /** Delete every grain a plant made, loose or settled (it was taken out or picked up). */
    private void clearGrains(int i) {
        for (int k = 0; k < cell.length; k++)
            if (cell[k] != 0 && owner[k] == i) {
                cell[k] = 0;
                total--;
            }
        for (int k = 0; k < POOL; k++)
            if (glive[k] && gown[k] == i) {
                glive[k] = false;
                total--;
            }
        given[i] = 0;
    }

    /** A loose grain of plant {@code i} thrown from a point: it falls on its own and joins the heap where it lands. False when the bowl is full of grains. */
    private boolean spawnGrain(int i, float px, float py, float svx, float svy) {
        if (total >= TOTAL || palette[i] == null)
            return false;
        for (int k = 0; k < POOL; k++)
            if (!glive[k]) {
                glive[k] = true;
                gx[k] = px;
                gy[k] = py;
                gvx[k] = svx;
                gvy[k] = svy;
                gcol[k] = (byte) rng.nextInt(palette[i].length);
                gown[k] = (byte) i;
                total++;
                return true;
            }
        return false;
    }

    /** Crush progress makes grains in the plant's colours around its lower half, thrown out a little sideways; they fall into the bowl. */
    private void grow(int i) {
        if (i == held)
            return;
        int target = (int) (progress[i] * grainsFor(i));
        while (given[i] < target) {
            float a = (rng.nextFloat() - 0.5f) * 2f;
            if (!spawnGrain(i, Mth.clamp(x[i] + a * rad[i] * 0.7f, left() + 1f, right() - 2f), Mth.clamp(y[i] + rad[i] * (0.2f + 0.5f * rng.nextFloat()), top() + 1f, bottom() - 2f), a * 40f, 10f * rng.nextFloat()))
                return;
            given[i]++;
        }
    }

    /** A blow kicks a few grains up. */
    private void kick(int i, int n) {
        for (int k = 0; k < n; k++)
            spawnGrain(i, x[i] + (rng.nextFloat() - 0.5f) * rad[i], y[i], (rng.nextFloat() - 0.5f) * 140f, -120f - 160f * rng.nextFloat());
    }

    private void settle(int k, int cx, int cy) {
        for (; cy >= 0; cy--)
            if (free(cx, cy)) {
                cell[cy * GW + cx] = (byte) (gcol[k] + 1);
                owner[cy * GW + cx] = gown[k];
                glive[k] = false;
                return;
            }
        glive[k] = false;
        total--;
    }

    /** One 1/60 s of the grains: loose ones fall and land (axis by axis, bouncing off walls), settled ones fall further or slide off down-left or down-right when resting on a grain or slope, scanning bottom-up, alternating direction. */
    private void gridStep() {
        int pcx = Math.round(x[PESTLE]) - bx, foot = Math.round(y[PESTLE]) + 11 - by;
        pl = pcx - 11;
        pr = pcx + 11;
        pb = foot;
        pt = foot - PESTLE_H;
        if (plowT > 0f) {
            plowT -= GRID_STEP;
            plow();
        }
        for (int k = 0; k < POOL; k++) {
            if (!glive[k])
                continue;
            gvy[k] += GRAIN_G * GRID_STEP;
            int ox = (int) Math.floor(gx[k] - bx), oy = (int) Math.floor(gy[k] - by);
            float ny = gy[k] + gvy[k] * GRID_STEP, nx = gx[k] + gvx[k] * GRID_STEP;
            int cyn = (int) Math.floor(ny - by);
            if (!free(ox, cyn)) {
                if (gvy[k] > 0f) {
                    settle(k, ox, oy);
                    continue;
                }
                gvy[k] = 0f;
            } else
                gy[k] = ny;
            int cxn = (int) Math.floor(nx - bx);
            if (!free(cxn, (int) Math.floor(gy[k] - by)))
                gvx[k] = -gvx[k] * 0.3f;
            else
                gx[k] = nx;
        }
        gridParity ^= 1;
        for (int y = GH - 2; y >= 0; y--)
            for (int k = 0; k < GW; k++) {
                int x = gridParity == 0 ? k : GW - 1 - k, i = y * GW + x;
                if (cell[i] == 0)
                    continue;
                int cy = y;
                for (int n = 0; n < 3 && free(x, cy + 1); n++) {
                    move(i, i + GW);
                    i += GW;
                    cy++;
                }
                if (cy != y || free(x, cy + 1))
                    continue;
                gridSeed = gridSeed * 1103515245 + 12345;
                boolean leftFirst = (gridSeed >>> 16 & 1) == 0;
                boolean l = free(x - 1, cy + 1), r = free(x + 1, cy + 1);
                if (l && (!r || leftFirst))
                    move(i, i + GW - 1);
                else if (r)
                    move(i, i + GW + 1);
            }
    }

    private void move(int from, int to) {
        cell[to] = cell[from];
        owner[to] = owner[from];
        cell[from] = 0;
    }

    /** The plant owning a settled grain within a few pixels of the point, or -1. */
    private int grainAt(double mx, double my) {
        int cx = (int) Math.floor(mx - bx), cy = (int) Math.floor(my - by);
        for (int d = 0; d <= 3; d++)
            for (int dy = -d; dy <= d; dy++)
                for (int dx = -d; dx <= d; dx++)
                    if ((Math.abs(dx) == d || Math.abs(dy) == d) && cx + dx >= 0 && cx + dx < GW && cy + dy >= 0 && cy + dy < GH && cell[(cy + dy) * GW + cx + dx] != 0)
                        return owner[(cy + dy) * GW + cx + dx];
        return -1;
    }

    /** Once a tick: the dust moves on. */
    void tick() {
        for (int i = 0; i < N; i++)
            if (on[i])
                grow(i);
        specks.tick(0.12f);
    }

    // ---- drawing -------------------------------------------------------------------------------------------------------------------------------------

    /** The row's half extent of a round-bottomed bowl: full width above the corners, the corner circle's chord below. */
    private static float chord(float yc, float bottom, float corner, float half) {
        float cy = bottom - corner;
        if (yc <= cy)
            return half;
        float dy = Math.min(corner, yc - cy);
        return half - corner + Mth.sqrt(Math.max(0f, corner * corner - dy * dy));
    }

    void render(GuiGraphics g, GuiSkin skin, int mx, int my) {
        accent = skin.accent();
        int w = BrewingStandMenu.BOWL_W, h = BrewingStandMenu.BOWL_H, shake = GuiStyle.shake(shakeStart, 2);
        skin.inset(g, bx, by, w, h);
        // Backdrop: the dim wall behind, a shelf board and the shapes of bottles on it, all faint.
        g.fill(bx + 1, by + 1, bx + w - 1, by + h - 1, 0x60180E06);
        g.fill(bx + 1, by + 6, bx + w - 1, by + 7, 0x90785A38);
        g.fill(bx + 1, by + 7, bx + w - 1, by + 8, 0x60301E10);
        bottle(g, bx + 6, by + 6, 7, 3, 0x503A5A70);
        bottle(g, bx + 66, by + 6, 7, 3, 0x505A7038);
        bottle(g, bx + 17, by + 6, 5, 2, 0x4070503A);
        float mid = bx + w / 2f;
        // The stone basin: a thick flared lip, sides sloping in to a base on a foot, speckled; the cavity is the physics region.
        float lipTop = by + LIP - 3, bodyBottom = by + h - 10;
        for (int row = (int) lipTop; row < by + h - 1; row++) {
            float yc = row + 0.5f, outer, inner = 0f;
            int shade;
            if (row >= by + h - 4) {
                outer = w / 2f - 1;
                shade = 0xFF6A5236;
            } else if (row >= bodyBottom) {
                outer = 30f;
                shade = STONE_DARK;
            } else {
                float t = (yc - lipTop) / (bodyBottom - lipTop);
                outer = row < lipTop + 6 ? w / 2f - 1 : chord(yc, bodyBottom, 12f, w / 2f - 4f - 3f * t);
                shade = row < lipTop + 2 ? STONE_LIGHT : row < lipTop + 6 ? STONE : t > 0.8f ? STONE_DARK : STONE;
            }
            if (row >= top() && row < bottom())
                inner = chord(yc - by, bottom() - by, CORNER, w / 2f - WALL);
            int ol = Math.round(mid - outer) + shake, or = Math.round(mid + outer) + shake;
            g.fill(ol, row, or, row + 1, shade);
            // Speckles on the stone, in the same places every frame: lighter and darker grey, only on the wall.
            int hsh = row * 0x9E3779B1;
            hsh ^= hsh >>> 15;
            int wall = Math.max(2, Math.round(outer - inner));
            g.fill(ol + Math.floorMod(hsh, wall), row, ol + Math.floorMod(hsh, wall) + 1, row + 1, (hsh >>> 5 & 1) == 0 ? STONE_LIGHT : STONE_DARK);
            g.fill(or - 1 - Math.floorMod(hsh >>> 9, wall), row, or - Math.floorMod(hsh >>> 9, wall), row + 1, (hsh >>> 14 & 1) == 0 ? STONE_DARK : STONE_LIGHT);
            if (inner > 0f)
                g.fill(Math.round(mid - inner) + shake, row, Math.round(mid + inner) + shake, row + 1, row > bottom() - 14 ? CAVITY_LOW : CAVITY);
        }
        g.enableScissor(bx, by, bx + w, by + h);
        g.pose().pushPose();
        g.pose().translate(shake, 0, 0);
        grains(g);
        for (int i = 0; i < N; i++)
            if (on[i] && i != held)
                plant(g, i, mx, my);
        pestle(g, mx, my);
        g.pose().popPose();
        g.disableScissor();
        // A plant carried by the mouse is drawn over everything, even outside the bowl.
        if (held >= 0) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            plant(g, held, mx, my);
            g.pose().popPose();
        }
        specks.draw(g, 0f);
    }

    /** A bottle's silhouette standing on the shelf: body {@code bw} wide and {@code bh} high, a neck and a stopper. */
    private static void bottle(GuiGraphics g, int x, int base, int bw, int bh, int colour) {
        g.fill(x, base - bh, x + bw, base, colour);
        g.fill(x + bw / 2 - 1, base - bh - 3, x + bw / 2 + 1, base - bh, colour);
        g.fill(x + bw / 2 - 1, base - bh - 4, x + bw / 2 + 1, base - bh - 3, 0x50705030);
    }

    private void plant(GuiGraphics g, int i, int mx, int my) {
        int cx = Math.round(x[i]), cy = Math.round(y[i]);
        if (progress[i] >= 1f) {
            // Fully crushed: no icon, only the matter. Carried, it is a small heap under the cursor; settled, it is the grains and a label over them.
            if (i == held)
                carried(g, i, cx, cy, 30);
            else if (labelN[i] > 0)
                label(g, i, Math.round(labelX[i] / labelN[i]), Math.round(labelY[i]) + 4);
            return;
        }
        if (progress[i] < 1f) {
            // The whole plant, shrinking as it is crushed; at 100% only the pile is left.
            float s = size[i] / 16f * (1f - 0.8f * progress[i]);
            g.pose().pushPose();
            g.pose().translate(cx - 8 * s, cy - 8 * s, 0);
            g.pose().scale(s, s, 1f);
            g.renderItem(stackAt(i), 0, 0);
            g.pose().popPose();
        }
        if (progress[i] > 0f && progress[i] < 1f) {
            // Progress as a short arc over the plant, clockwise from the left.
            int dots = Math.round(progress[i] * 12), r = Math.round(size[i] * 0.5f * (1f - 0.8f * progress[i])) + 3;
            for (int k = 0; k < dots; k++) {
                float a = Mth.PI + Mth.PI * k / 11f;
                int ax = cx + Math.round(Mth.cos(a) * r), ay = cy + Math.round(Mth.sin(a) * r);
                g.fill(ax, ay, ax + 1, ay + 1, 0xFFFFE08A);
            }
        }
        label(g, i, cx, cy + Math.round(rad[i] * 0.3f));
        if (held < 0 && !pestleHeld && Math.hypot(mx - x[i], my - y[i]) <= rad[i] + 3)
            GuiStyle.ring(g, cx, cy, Math.round(rad[i]) + 2, 1, accent);
    }

    /** The percent ground, always shown, centred on a point. */
    private void label(GuiGraphics g, int i, int cx, int cy) {
        String pct = PCT[Math.round(progress[i] * 100f)];
        var font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(cx - font.width(pct) * 0.3f, cy, 300);
        g.pose().scale(0.6f, 0.6f, 1f);
        g.drawString(font, pct, 0, 0, 0xFFFFFFFF, true);
        g.pose().popPose();
    }

    /** Grains on the cursor: a plant carried at 100%, a small heap of its own colours under the pointer. */
    private void carried(GuiGraphics g, int i, int cx, int cy, int grains) {
        int[] colours = palette[i];
        for (int k = 0; k < grains && colours != null; k++) {
            int hsh = k * 0x9E3779B1 + i * 0x85EBCA6B;
            hsh ^= hsh >>> 15;
            hsh *= 0x2C1B3C6D;
            hsh ^= hsh >>> 13;
            float ax = (hsh & 1023) / 511.5f - 1f;
            int px = cx + Math.round(ax * 7f), py = cy + 3 - Math.round((hsh >>> 10 & 255) / 255f * (1f - Math.abs(ax)) * 7f);
            g.fill(px, py, px + 1, py + 1, colours[(hsh >>> 20 & 255) % colours.length]);
        }
        label(g, i, cx, cy + 6);
    }

    /** Every settled grain as one pixel, in its owner's colours with a fixed per-cell shade; fully crushed plants' labels sit over the centre of their grains. */
    private void grains(GuiGraphics g) {
        java.util.Arrays.fill(labelN, 0);
        java.util.Arrays.fill(labelX, 0f);
        for (int k = 0; k < N; k++)
            labelY[k] = Float.MAX_VALUE;
        for (int cy = 0; cy < GH; cy++)
            for (int cx = 0; cx < GW; cx++) {
                int idx = cy * GW + cx, v = cell[idx];
                if (v == 0)
                    continue;
                int o = owner[idx];
                int[] colours = palette[o];
                if (colours == null)
                    continue;
                int col = colours[(v - 1) % colours.length], shade = 205 + (idx * 0x9E3779B1 >>> 28) * 3;
                g.fill(bx + cx, by + cy, bx + cx + 1, by + cy + 1, 0xFF000000 | (col >> 16 & 255) * shade / 255 << 16 | (col >> 8 & 255) * shade / 255 << 8 | (col & 255) * shade / 255);
                labelX[o] += bx + cx;
                labelN[o]++;
                labelY[o] = Math.min(labelY[o], by + cy);
            }
        // Loose grains in flight.
        for (int k = 0; k < POOL; k++)
            if (glive[k] && palette[gown[k]] != null) {
                int col = palette[gown[k]][gcol[k]];
                g.fill(Math.round(gx[k]), Math.round(gy[k]), Math.round(gx[k]) + 1, Math.round(gy[k]) + 1, col);
            }
    }

    /** The pestle: a short, stout, upright block of stone, uniform width, flat top and foot, shaded and flecked; its foot is the physics circle as wide as the block. */
    private void pestle(GuiGraphics g, int mx, int my) {
        int cx = Math.round(x[PESTLE]), foot = Math.round(y[PESTLE]) + 11, l = cx - 11, r = cx + 11, t = foot - PESTLE_H;
        g.fill(l, t, r, foot, STONE);
        g.fill(l + 1, t, r - 1, t + 1, STONE_LIGHT);
        g.fill(l, t + 1, l + 2, foot - 1, STONE_LIGHT);
        g.fill(r - 2, t + 1, r, foot, STONE_DARK);
        g.fill(l + 1, foot - 2, r, foot, STONE_DARK);
        // Grain flecks, fixed in place.
        for (int k = 0; k < 9; k++) {
            int hsh = (k + 5) * 0x9E3779B1;
            hsh ^= hsh >>> 15;
            int fx = l + 3 + Math.floorMod(hsh, 16), fy = t + 3 + Math.floorMod(hsh >>> 8, 27);
            g.fill(fx, fy, fx + 1, fy + 1, (hsh >>> 20 & 1) == 0 ? STONE_LIGHT : STONE_DARK);
        }
        if (pestleHeld) {
            // A small hand gripping the upper half of the block.
            int hx = cx, hy = t + 10;
            g.fill(hx - 3, hy - 2, hx + 4, hy + 3, SKIN);
            for (int f = 0; f < 4; f++)
                g.fill(hx - 3 + f * 2, hy - 4, hx - 2 + f * 2, hy - 2, SKIN);
            g.fill(hx - 5, hy - 1, hx - 3, hy + 1, SKIN);
            g.fill(hx - 3, hy + 3, hx + 4, hy + 4, 0xFFB08F68);
        }
    }
}
