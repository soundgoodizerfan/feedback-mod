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
    /** Heavy things: gravity, the little bounce left, floor drag per second (plants, tall flowers), air drag, and the fastest a plant may leave the hand. Sizes are icon scale over 16 px. */
    private static final float G = 1000f, STEP = 1f / 120f, REST = 0.12f, REST_TALL = 0f, FLOOR = 12f, FLOOR_TALL = 26f, AIR = 1.6f, THROW = 220f, SIZE = 1.5f, SIZE_TALL = 1.9f,
            MASS = 0.5f, MASS_TALL = 0.15f, R_OBJ = 6f, R_HEAD = 7f, CORNER = 16f, WALL = 5f;
    private static final int STONE_DARK = 0xFF565149, STONE = 0xFF77716B, STONE_LIGHT = 0xFFA39C94, CAVITY = 0xFF2A2623, CAVITY_LOW = 0xFF3A342F, DUST = 0xFFE2D5B0,
            WOOD_DARK = 0xFF6B4E32, WOOD = 0xFF9A7448, HEAD = 0xFF8D867E, HEAD_LIGHT = 0xFFB9B2A8;

    private static final String[] PCT = new String[101];

    static {
        for (int i = 0; i <= 100; i++)
            PCT[i] = i + "%";
    }

    private final BrewingStandMenu menu;
    private final boolean[] on = new boolean[N + 1], touch = new boolean[N];
    private final float[] x = new float[N + 1], y = new float[N + 1], vx = new float[N + 1], vy = new float[N + 1], rad = new float[N + 1], inverseMass = new float[N + 1];
    private final float[] size = new float[N], rest = new float[N + 1], floorDrag = new float[N + 1], progress = new float[N], hard = new float[N], cool = new float[N], pending = new float[N];
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
            rad[i] = R_OBJ * SIZE;
            inverseMass[i] = MASS;
            rest[i] = REST;
            floorDrag[i] = FLOOR;
            size[i] = SIZE;
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
        bx = nx;
        by = ny;
        if (x[PESTLE] == 0f) {
            x[PESTLE] = bx + BrewingStandMenu.BOWL_W - 16;
            y[PESTLE] = by + BrewingStandMenu.BOWL_H - 14;
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
        return by + 6;
    }

    private float bottom() {
        return by + BrewingStandMenu.BOWL_H - WALL - 1;
    }

    boolean inside(double mx, double my) {
        return mx >= bx && mx < bx + BrewingStandMenu.BOWL_W && my >= by && my < by + BrewingStandMenu.BOWL_H;
    }

    /** Keep body {@code i} inside the cavity (flat floor, rounded lower corners, straight walls), bouncing it; the speed it hit with, for sound. */
    private float contain(int i) {
        float r = rad[i], l = left() + r, rt = right() - r, floor = bottom() - r, hit = 0f;
        float cl = left() + CORNER, cr = right() - CORNER, cy = bottom() - CORNER;
        if (y[i] > cy && (x[i] < cl || x[i] > cr)) {
            float cx = x[i] < cl ? cl : cr, dx = x[i] - cx, dy = y[i] - cy, d = Mth.sqrt(dx * dx + dy * dy), max = CORNER - r;
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

    /** The place under the mouse (the held one first), or -1. */
    int hovered(double mx, double my) {
        if (held >= 0)
            return held;
        for (int i = N - 1; i >= 0; i--)
            if (on[i] && Math.hypot(mx - x[i], my - y[i]) <= rad[i] + 3)
                return i;
        return -1;
    }

    private boolean onPestle(double mx, double my) {
        // The whole block is the handle; its striking face (the physics circle) is the bottom of it.
        return mx >= x[PESTLE] - 8 && mx <= x[PESTLE] + 8 && my >= y[PESTLE] - 21 && my <= y[PESTLE] + 9;
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
        int i = hovered(mx, my);
        if (i >= 0) {
            held = i;
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
                vx[i] = Mth.clamp(vx[i], -THROW, THROW) * (size[i] == SIZE_TALL ? 0f : 1f);
                vy[i] = Mth.clamp(vy[i], -THROW, THROW) * (size[i] == SIZE_TALL ? 0f : 1f);
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
            pending[i] = stack.isEmpty() ? 0f : Math.max(0f, pending[i] - dt);
            if (!on[i] && !stack.isEmpty() && pending[i] <= 0f) {
                on[i] = true;
                // Tall flowers (by item tag) are bigger, heavier and dead on landing.
                boolean tall = stack.is(net.minecraft.tags.ItemTags.TALL_FLOWERS);
                size[i] = tall ? SIZE_TALL : SIZE;
                rad[i] = R_OBJ * size[i];
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
                cool[i] = 0f;
                touch[i] = false;
            } else if (on[i] && stack.isEmpty()) {
                on[i] = false;
                if (held == i)
                    held = -1;
            }
        }
    }

    private void substep(float tx, float ty) {
        for (int i = 0; i < N; i++) {
            cool[i] = Math.max(0f, cool[i] - STEP);
            if (!on[i] || i == held)
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
            vx[PESTLE] = (x[PESTLE] - ox) / STEP;
            vy[PESTLE] = (y[PESTLE] - oy) / STEP;
        } else {
            vy[PESTLE] += G * STEP;
            x[PESTLE] += vx[PESTLE] * STEP;
            y[PESTLE] += vy[PESTLE] * STEP;
        }
        for (int i = 0; i < N; i++)
            if (on[i] && i != held) {
                float hit = contain(i);
                if (hit > 160f)
                    GuiSkin.play(SoundEvents.STONE_HIT, 1.2f + rng.nextFloat() * 0.3f, Math.min(0.3f, hit / 1500f));
            }
        if (!pestleHeld) {
            float hit = contain(PESTLE);
            if (hit > 120f) {
                GuiSkin.play(SoundEvents.STONE_HIT, 0.8f, Math.min(0.5f, hit / 1000f));
                shakeStart = GuiStyle.time();
            }
            vx[PESTLE] *= 1f - 0.8f * STEP;
        }
        for (int i = 0; i < N; i++)
            if (on[i] && i != held)
                pestlePair(i);
        for (int i = 0; i < N; i++)
            for (int j = i + 1; j < N; j++)
                if (on[i] && on[j] && i != held && j != held)
                    pair(i, j, 0.3f);
        for (int i = 0; i <= N; i++)
            if (on[i] && i != held)
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
                    specks.burst(x[o], y[o] + 4, 2, 0.5f, 0.6f, DUST, 12, 1);
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
        specks.burst(x[o], y[o], 4 + (int) (6 * (progress[o] - before) * 4), 1.6f, 2f, DUST, 16, 1);
        vy[o] = Math.min(vy[o], -60f);
    }

    /** Once a tick: the dust moves on. */
    void tick() {
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
        float mid = bx + w / 2f;
        // The stone and its cavity, row by row (the cavity is the physics region).
        for (int row = by + 1; row < by + h - 1; row++) {
            float outer = chord(row + 0.5f - by, h - 1, CORNER + WALL, w / 2f - 1);
            int shade = row < by + 5 ? STONE_LIGHT : row > by + h - 9 ? STONE_DARK : STONE;
            g.fill(Math.round(mid - outer) + shake, row, Math.round(mid + outer) + shake, row + 1, shade);
            // Pits in the stone, in the same places every frame.
            int hsh = row * 0x9E3779B1;
            hsh ^= hsh >>> 15;
            g.fill(Math.round(mid - outer) + 1 + Math.floorMod(hsh, 8) + shake, row, Math.round(mid - outer) + 3 + Math.floorMod(hsh, 8) + shake, row + 1, STONE_DARK);
            g.fill(Math.round(mid + outer) - 4 - Math.floorMod(hsh >>> 8, 8) + shake, row, Math.round(mid + outer) - 3 - Math.floorMod(hsh >>> 8, 8) + shake, row + 1, STONE_LIGHT);
            if (row >= top() && row < bottom()) {
                float inner = chord(row + 0.5f - by, bottom() - by, CORNER, w / 2f - WALL);
                g.fill(Math.round(mid - inner) + shake, row, Math.round(mid + inner) + shake, row + 1, row > by + h - 22 ? CAVITY_LOW : CAVITY);
            }
        }
        g.enableScissor(bx, by, bx + w, by + h);
        g.pose().pushPose();
        g.pose().translate(shake, 0, 0);
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

    private void plant(GuiGraphics g, int i, int mx, int my) {
        float s = (0.75f - 0.3f * progress[i]) * size[i];
        int cx = Math.round(x[i]), cy = Math.round(y[i]);
        g.pose().pushPose();
        g.pose().translate(cx - 8 * s, cy - 8 * s, 0);
        g.pose().scale(s, s, 1f);
        g.renderItem(stackAt(i), 0, 0);
        g.pose().popPose();
        // Powder round its foot: more of it the finer it is, in the same places each time.
        int dust = (int) (progress[i] * 9);
        for (int k = 0; k < dust; k++) {
            int hsh = (k + i * 31) * 0x9E3779B1;
            hsh ^= hsh >>> 15;
            int r = Math.round(rad[i]), px = cx - r + Math.floorMod(hsh, 2 * r + 1), py = cy + r - 2 + (hsh >>> 8 & 1) + (hsh >>> 9 & 1);
            g.fill(px, py, px + 1, py + 1, k % 2 == 0 ? DUST : 0xFFB8A880);
        }
        if (progress[i] > 0f && progress[i] < 1f) {
            // Progress as a short arc over the plant, clockwise from the left.
            int dots = Math.round(progress[i] * 12);
            for (int k = 0; k < dots; k++) {
                float a = Mth.PI + Mth.PI * k / 11f;
                g.fill(cx + Math.round(Mth.cos(a) * (rad[i] + 3)), cy + Math.round(Mth.sin(a) * (rad[i] + 3)), cx + Math.round(Mth.cos(a) * (rad[i] + 3)) + 1, cy + Math.round(Mth.sin(a) * (rad[i] + 3)) + 1, 0xFFFFE08A);
            }
        }
        // The percent ground, always shown, over the foot of the plant.
        String pct = PCT[Math.round(progress[i] * 100f)];
        var font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(cx - font.width(pct) * 0.3f, cy + rad[i] * 0.3f, 300);
        g.pose().scale(0.6f, 0.6f, 1f);
        g.drawString(font, pct, 0, 0, 0xFFFFFFFF, true);
        g.pose().popPose();
        if (held < 0 && !pestleHeld && Math.hypot(mx - x[i], my - y[i]) <= R_OBJ + 3)
            GuiStyle.ring(g, cx, cy, Math.round(rad[i]) + 2, 1, accent);
    }

    private void pestle(GuiGraphics g, int mx, int my) {
        // One chunky stone block (a tejolote): rectangle with clipped corners, lit from the top left, a few grain lines.
        int cx = Math.round(x[PESTLE]), cy = Math.round(y[PESTLE]), l = cx - 7, r = cx + 7, t = cy - 19, b = cy + 7;
        g.fill(l + 1, t, r - 1, b, HEAD);
        g.fill(l, t + 1, r, b - 1, HEAD);
        g.fill(l + 1, t, r - 1, t + 2, HEAD_LIGHT);
        g.fill(l, t + 2, l + 2, b - 1, HEAD_LIGHT);
        g.fill(r - 2, t + 2, r, b - 1, STONE_DARK);
        g.fill(l + 1, b - 2, r - 1, b, STONE_DARK);
        g.fill(l + 3, t + 7, r - 4, t + 8, STONE_DARK);
        g.fill(l + 4, t + 14, r - 3, t + 15, STONE_DARK);
        g.fill(cx - 1, t + 3, cx + 2, t + 4, HEAD_LIGHT);
        if (pestleHeld || held < 0 && onPestle(mx, my))
            g.renderOutline(l - 1, t - 1, 16, 28, 0x80FFFFFF);
    }
}
