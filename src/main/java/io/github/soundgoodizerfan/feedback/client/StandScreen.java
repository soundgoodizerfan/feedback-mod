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

import java.util.List;
import java.util.Map;
import java.util.Random;

import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.net.StandInputPayload;
import io.github.soundgoodizerfan.feedback.net.StandStatePayload;
import io.github.soundgoodizerfan.feedback.potion.BrewingStandBlockEntity;
import io.github.soundgoodizerfan.feedback.potion.BrewingStandMenu;
import io.github.soundgoodizerfan.feedback.potion.PotionBrew;
import io.github.soundgoodizerfan.feedback.potion.PotionTuning;
import io.github.soundgoodizerfan.feedback.process.Cooking;
import io.github.soundgoodizerfan.feedback.process.FoodCut;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The brewing stand (potions spec 3 and 11), a parchment map with the apparatus round it. The map is the subject of the screen: effect nodes
 * you have made show as their effect's icon, dark zones are patches of fallen solids or char, a vortex is a spiral that turns once the pot is
 * hot, the marker is a small flask, and the line it has walked is behind it. The faint line ahead is where the ingredients in the pot would
 * take it. Hover an ingredient (in the inventory or a slot) and a ghost line shows where adding it would take the marker, from the same
 * arithmetic the server uses ({@link PotionBrew}); the preview is cosmetic, and is the very line the ingredient would get if added now (same cut, the pot's heat now).
 * Move the mouse in circles over the map to stir. Nothing is graded here: the stand is live on the server, this screen sends a pace and a
 * choice and draws what comes back. Map colours are the map's own, not the skin's.
 */
public class StandScreen extends SkinnedScreen<BrewingStandMenu> {

    private static final int BAND_X = 26, BAND_Y = 14, MX = 94 + BAND_X, MY = 22 + BAND_Y, MS = 126, S = MS / 2, LX = 8, LW = 80, SEND_EVERY = 5, FULL_PACE_RAD_PER_TICK = 3, TRAIL = 48, CURVE_N = 40;
    private static final double MIN_ZOOM = 0.8, MAX_ZOOM = 4, ZOOM_STEP = 1.15;
    private static final int PARCHMENT = 0xFFD9C7A0, EDGE = 0x30402810, SPECK = 0x30402810, RING = 0x30503820, PATH = 0x90502810, FOG = 0xD0B09C74, TRAIL_INK = 0xC0402818,
            PRECIPITATE = 0x60503020, SOLIDS_DARK = 0xC0402818, SOLIDS_PALE = 0xC0F0E4C8, CHAR = 0xA0301808, EMBER = 0xFFFFA030, VORTEX = 0xFF3A6A98,
            NODE = 0xFF8A6A18, MARK = 0xFF70281C, GLASS = 0xFF2A1C10, LIQUID = 0xFF58B8E8, LIQUID_SPENT = 0xFFA83828;
    private static final int[] SPECKS = new int[90];

    static {
        Random random = new Random(7);
        for (int i = 0; i < SPECKS.length; i++)
            SPECKS[i] = random.nextInt(MS * MS);
    }

    /** The last state the server sent, for a screen that opens just after it arrives. */
    private static StandStatePayload last;

    private StandStatePayload s;
    private TextureAtlasSprite[] sprites = new TextureAtlasSprite[0];
    private Component[] names = new Component[0];
    private List<FormattedCharSequence> status = List.of(), hint = List.of();
    private boolean dirty = true;
    private final float[] trail = new float[TRAIL * 2];
    private int trailCount, trailHead;
    private double camX, camY, zoom = 1;
    private boolean panning;
    private float swirl, pace, lastAngle, ghostGrind;
    private PotionBrew.Segment ghostSeg;
    private float ghostX, ghostY;
    private final float[] tmp = new float[2];
    private final int[] curveX = new int[CURVE_N + 1], curveY = new int[CURVE_N + 1];
    private boolean haveAngle, ghost;
    private int ticks, sound, mouseX, mouseY;
    private Button cook;

    public StandScreen(BrewingStandMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.WOOD);
        imageWidth = BrewingStandMenu.W;
        imageHeight = BrewingStandMenu.H;
        titleLabelX = 8;
        titleLabelY = 8;
        inventoryLabelX = BrewingStandMenu.INV_X;
        inventoryLabelY = BrewingStandMenu.INV_Y - 11;
        s = last != null && last.pos().equals(menu.pos()) ? last : new StandStatePayload(menu.pos(), 0f, 0f, 1f, 20f, 0, 0, 0, 225f, 1f, "", List.of(), List.of(), new long[0]);
    }

    /** The server's word: update the stand's screen if it is open, and keep it for one about to open. */
    public static void state(StandStatePayload payload) {
        last = payload;
        if (Minecraft.getInstance().screen instanceof StandScreen screen && screen.menu.pos().equals(payload.pos())) {
            screen.s = payload;
            screen.dirty = true;
        }
    }

    @Override
    protected void init() {
        super.init();
        hint = font.split(Component.translatable("gui.feedback.stand.hint"), 170);
        cook = addRenderableWidget(GuiStyle.primary(skin, Component.translatable("gui.feedback.stand.cook"),
                b -> PacketDistributor.sendToServer(new StandInputPayload(menu.pos(), Math.round(pace * 100f), StandInputPayload.COOK)),
                leftPos + 278, topPos + 156, 60, 16, FSounds.UI_STAMP::get));
        addRenderableWidget(GuiStyle.button(skin, Component.translatable("gui.feedback.stand.tip"),
                b -> PacketDistributor.sendToServer(new StandInputPayload(menu.pos(), Math.round(pace * 100f), StandInputPayload.TIP)),
                leftPos + LX, topPos + 162, LW / 2 - 1, 14));
        addRenderableWidget(GuiStyle.button(skin, Component.translatable("gui.feedback.stand.pour"),
                b -> PacketDistributor.sendToServer(new StandInputPayload(menu.pos(), Math.round(pace * 100f), StandInputPayload.LADLE)),
                leftPos + LX + LW / 2 + 1, topPos + 162, LW / 2 - 1, 14));
    }

    /** Rebuild what depends on the server's state: the status text, and the sprites and names of the nodes made. */
    private void rebuild() {
        dirty = false;
        List<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.translatable(s.last().isEmpty() ? "gui.feedback.stand.quiet" : s.last()));
        if (s.water() <= 0)
            lines.add(Component.translatable("feedback.stand.dry"));
        else if (s.temperature() < PotionTuning.COOK_TU)
            lines.add(Component.translatable("feedback.stand.cold"));
        lines.add(Component.translatable("gui.feedback.stand.pot", s.stirred(), s.ingredients()));
        status = new java.util.ArrayList<>();
        for (Component c : lines)
            ((java.util.ArrayList<FormattedCharSequence>) status).addAll(font.split(c, LW - 8));
        if (status.size() > 9)
            status = status.subList(0, 9);
        List<StandStatePayload.Mark> marks = s.marks();
        sprites = new TextureAtlasSprite[marks.size()];
        names = new Component[marks.size()];
        for (int i = 0; i < sprites.length; i++) {
            ResourceLocation id = marks.get(i).kind() == 3 ? ResourceLocation.tryParse(marks.get(i).label()) : null;
            var holder = id == null ? java.util.Optional.<net.minecraft.core.Holder.Reference<net.minecraft.world.effect.MobEffect>>empty() : BuiltInRegistries.MOB_EFFECT.getHolder(id);
            if (holder.isPresent()) {
                sprites[i] = Minecraft.getInstance().getMobEffectTextures().get(holder.get());
                names[i] = holder.get().value().getDisplayName();
            }
        }
    }

    /** The viewport's centre on the screen. */
    private int mapX() {
        return leftPos + MX + S;
    }

    private int mapY() {
        return topPos + MY + S;
    }

    /** Pixels to a map unit now: the same transform draws every mark, and turns the cursor into map coordinates. */
    private float scale() {
        return (float) (S * zoom);
    }

    private int px(float x) {
        return mapX() + Math.round((x - (float) camX) * scale());
    }

    private int py(float y) {
        return mapY() - Math.round((y - (float) camY) * scale());
    }

    private double mapOfX(double sx) {
        return (sx - mapX()) / scale() + camX;
    }

    private double mapOfY(double sy) {
        return (mapY() - sy) / scale() + camY;
    }

    private void clampCamera() {
        camX = Mth.clamp(camX, -s.boundary(), s.boundary());
        camY = Mth.clamp(camY, -s.boundary(), s.boundary());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // Right or middle drag pans the map; the left button is left to the slots and the cursor does the stirring.
        if ((button == 1 || button == 2) && settled() && onMap(mx, my)) {
            panning = true;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (panning && (button == 1 || button == 2)) {
            panning = false;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (panning) {
            camX -= dx / scale();
            camY += dy / scale();
            clampCamera();
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    /** Zoom about the cursor, as the skill tree's web does: the map point under the cursor stays under it. */
    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (!settled() || !onMap(mx, my))
            return super.mouseScrolled(mx, my, scrollX, scrollY);
        double fx = mapOfX(mx), fy = mapOfY(my);
        zoom = Mth.clamp(zoom * Math.pow(ZOOM_STEP, scrollY), MIN_ZOOM, MAX_ZOOM);
        camX = fx - (mx - mapX()) / scale();
        camY = fy + (my - mapY()) / scale();
        clampCamera();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        // R puts the view back on the whole map.
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_R && settled() && onMap(mouseX, mouseY)) {
            camX = camY = 0;
            zoom = 1;
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    private boolean onMap(double x, double y) {
        return x >= leftPos + MX && x < leftPos + MX + MS && y >= topPos + MY && y < topPos + MY + MS;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (!settled())
            return;
        if (dirty)
            rebuild();
        cook.active = s.water() > 0 && s.temperature() >= PotionTuning.COOK_TU;
        // Pace is how far round the map's centre the mouse went this tick, inside the map, and nothing while an item is on the cursor.
        double dx = mouseX - mapX(), dy = mouseY - mapY(), d = Math.sqrt(dx * dx + dy * dy);
        float angle = (float) Math.atan2(dy, dx);
        boolean stirring = menu.getCarried().isEmpty() && d > S * 0.1 && d < S * 1.05;
        float turned = haveAngle && stirring ? Math.abs(Mth.wrapDegrees((angle - lastAngle) * Mth.RAD_TO_DEG)) * Mth.DEG_TO_RAD : 0f;
        haveAngle = stirring;
        lastAngle = angle;
        pace += (Math.min(1f, turned * 10f / FULL_PACE_RAD_PER_TICK) - pace) * 0.25f;
        swirl += 0.05f + pace * 0.2f;
        if (pace > 0.3f && --sound <= 0) {
            sound = 6;
            GuiSkin.play(SoundEvents.PLAYER_SWIM, 0.5f + 0.5f * pace);
        }
        if (++ticks % SEND_EVERY == 0)
            PacketDistributor.sendToServer(new StandInputPayload(menu.pos(), Math.round(pace * 100f), 0));
        // The trail behind the marker: a point each time it has moved on.
        int last = (trailHead + trailCount - 1) % TRAIL;
        if (trailCount == 0 || Math.hypot(s.x() - trail[last * 2], s.y() - trail[last * 2 + 1]) > 0.012) {
            int at = (trailHead + trailCount) % TRAIL;
            trail[at * 2] = s.x();
            trail[at * 2 + 1] = s.y();
            if (trailCount < TRAIL)
                trailCount++;
            else
                trailHead = (trailHead + 1) % TRAIL;
        }
        preview();
    }

    /** The ghost: the curve the hovered plant would add from the end of the path, as the server would work it out. */
    private void preview() {
        ghost = false;
        Slot slot = hoveredSlot;
        ItemStack stack = slot == null ? ItemStack.EMPTY : slot.getItem();
        // An item hovered in JEI's overlay previews like one in the inventory (JEI is optional: its class is touched only when it is loaded).
        if (stack.isEmpty() && net.neoforged.fml.ModList.get().isLoaded("jei"))
            stack = io.github.soundgoodizerfan.feedback.compat.jei.FeedbackJeiPlugin.hoveredStack();
        if (stack.isEmpty() || Minecraft.getInstance().level == null)
            return;
        var level = Minecraft.getInstance().level;
        var plant = BrewingStandBlockEntity.potionOf(level, stack).orElse(null);
        if (plant == null)
            return;
        FoodCut cut = stack.get(FDataComponents.FOOD_CUT.get());
        float rot = BrewingStandBlockEntity.rotOf(level, stack);
        ghostGrind = BrewingStandBlockEntity.surface(cut == null ? 0 : cut.level());
        ghostSeg = PotionBrew.segment(plant, rot, PotionBrew.extraction(ghostGrind, s.temperature()), s.rotDegrees());
        float sx = s.x(), sy = s.y();
        if (!s.path().isEmpty()) {
            StandStatePayload.Seg last = s.path().get(s.path().size() - 1);
            last.at(1f, tmp);
            sx = tmp[0];
            sy = tmp[1];
        }
        ghostX = sx;
        ghostY = sy;
        ghost = true;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        mouseX = mx;
        mouseY = my;
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        skin.inset(g, leftPos + LX, topPos + MY, LW, 96);
        // The compass trim is a raised band round the map, set into the frame; the viewport is sunk into it.
        skin.plaque(g, leftPos + MX - BAND_X, topPos + MY - BAND_Y, MS + 2 * BAND_X, MS + 2 * BAND_Y);
        skin.inset(g, leftPos + MX - 1, topPos + MY - 1, MS + 2, MS + 2);
        drawMap(g);
        for (Slot slot : menu.slots)
            skin.slot(g, leftPos + slot.x, topPos + slot.y);
        // The ingredient being stirred in is partly in the water: its socket is dimmed.
        for (int i = 0; i < BrewingStandBlockEntity.INGREDIENTS; i++)
            if (menu.locked(i)) {
                Slot slot = menu.slots.get(BrewingStandBlockEntity.FIRST_INGREDIENT + i);
                g.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0x80000000);
            }
        int by = topPos + 132;
        skin.bar(g, leftPos + LX + 1, by, LW - 2, 6, Mth.clamp(s.temperature() / 100f, 0f, 1f), PotionTuning.COOK_TU / 100f);
        skin.bar(g, leftPos + LX + 1, by + 20, LW - 2, 6, Mth.clamp(s.potency(), 0f, 1f), -1f);
    }

    private void drawMap(GuiGraphics g) {
        int x0 = leftPos + MX, y0 = topPos + MY;
        g.fill(x0, y0, x0 + MS, y0 + MS, PARCHMENT);
        // Darker towards the edge, like paper handled at its borders.
        for (int i = 0; i < 8; i++)
            g.renderOutline(x0 + i, y0 + i, MS - 2 * i, MS - 2 * i, EDGE);
        for (int k : SPECKS)
            g.fill(x0 + k % MS, y0 + k / MS, x0 + k % MS + 1, y0 + k / MS + 1, SPECK);
        g.enableScissor(x0, y0, x0 + MS, y0 + MS);
        int cx = mapX(), cy = mapY();
        g.fill(px(0), y0, px(0) + 1, y0 + MS, RING);
        g.fill(x0, py(0), x0 + MS, py(0) + 1, RING);
        GuiStyle.ring(g, px(0), py(0), Math.round(scale() / 2), 1, RING);
        // The wall.
        GuiStyle.ring(g, px(0), py(0), Math.round(s.boundary() * scale()), 2, 0x80503820);
        boolean hot = s.temperature() >= PotionTuning.VORTEX_TU;
        List<StandStatePayload.Mark> marks = s.marks();
        for (int i = 0; i < marks.size(); i++) {
            StandStatePayload.Mark m = marks.get(i);
            int x = px(m.x()), y = py(m.y()), r = Math.round(m.radius() * scale());
            switch (m.kind()) {
                case 0 -> patch(g, x, y, r, i, PRECIPITATE, SOLIDS_DARK, SOLIDS_PALE);
                case 1 -> {
                    // Harmless cold: flecks of char with no glow until the pot is over its temperature.
                    boolean live = s.temperature() > m.a();
                    patch(g, x, y, r, i, live ? 0x70C8641E : 0x30502010, CHAR, live ? EMBER : CHAR);
                }
                case 2 -> {
                    int colour = hot ? VORTEX : 0x603A6A98;
                    float turn = hot ? swirl : 0f;
                    int px = x, py = y;
                    for (int k = 1; k <= 16; k++) {
                        float a = turn + k * 0.55f, rad = r * k / 16f;
                        int nx = x + Math.round(Mth.cos(a) * rad), ny = y + Math.round(Mth.sin(a) * rad);
                        GuiStyle.line(g, px, py, nx, ny, colour);
                        px = nx;
                        py = ny;
                    }
                    if (!Float.isNaN(m.a()))
                        GuiStyle.ring(g, px(m.a()), py(m.b()), 3, 1, VORTEX);
                }
                default -> {
                }
            }
        }
        // Fog: what the marker has not passed near, drawn as runs along each row.
        long[] bits = s.fog();
        int n = PotionTuning.FOG_N;
        float cell = 2f * PotionTuning.FOG_EXTENT / n;
        for (int row = 0; row < n; row++) {
            int run = -1;
            for (int col = 0; col <= n; col++) {
                int c = row * n + col;
                boolean hidden = col < n && c / 64 < bits.length && (bits[c / 64] >> (c % 64) & 1L) == 0L || col < n && c / 64 >= bits.length;
                if (hidden && run < 0)
                    run = col;
                else if (!hidden && run >= 0) {
                    int fx0 = px(-PotionTuning.FOG_EXTENT + run * cell), fx1 = px(-PotionTuning.FOG_EXTENT + col * cell);
                    int fy1 = py(-PotionTuning.FOG_EXTENT + row * cell), fy0 = py(-PotionTuning.FOG_EXTENT + (row + 1) * cell);
                    g.fill(fx0, fy0, fx1, fy1, FOG);
                    run = -1;
                }
            }
        }
        for (int i = 0; i < marks.size(); i++) {
            StandStatePayload.Mark m = marks.get(i);
            int x = px(m.x()), y = py(m.y());
            switch (m.kind()) {
                case 6 -> {
                    dashed(g, px(0), py(0), x, y, 0x70503820, 3, 5);
                    if (m.a() == 0f) {
                        // An unmade node: the flask's own shape as a dark silhouette with a question mark, so the marker can be matched to it.
                        // Drawn at a fixed size on screen however far the map is zoomed.
                        g.pose().pushPose();
                        g.pose().translate(x, y + 8, 0);
                        g.pose().scale(1.6f, 1.6f, 1f);
                        flask(g, 0, 0, GLASS, 0xFF4A3A28, false);
                        g.pose().popPose();
                        g.drawCenteredString(font, "?", x, y - 3, PARCHMENT);
                    }
                }
                case 3 -> {
                    // Fixed size on screen: a node's ring and icon do not grow as the map is zoomed.
                    GuiStyle.ring(g, x, y, 10, 1, NODE);
                    if (i < sprites.length && sprites[i] != null)
                        g.blit(x - 9, y - 9, 0, 18, 18, sprites[i]);
                }
                case 7 -> {
                    // A landmark: a small mineral outcrop, harmless, to steer by.
                    GuiStyle.line(g, x - 3, y + 3, x - 1, y - 3, GLASS);
                    GuiStyle.line(g, x - 1, y - 3, x + 1, y + 3, GLASS);
                    GuiStyle.line(g, x + 1, y + 3, x + 3, y - 1, GLASS);
                    GuiStyle.line(g, x - 3, y + 3, x + 3, y + 3, GLASS);
                }
                case 4 -> {
                    // A residue: where a brew went wrong; hovering it names the class of compound the nearest unmade node leans on.
                    GuiStyle.line(g, x - 2, y - 2, x + 2, y + 2, MARK);
                    GuiStyle.line(g, x - 2, y + 2, x + 2, y - 2, MARK);
                }
                default -> {
                }
            }
        }
        for (int i = 0; i < trailCount - 1; i++) {
            int a = (trailHead + i) % TRAIL, b = (a + 1) % TRAIL;
            GuiStyle.line(g, px(trail[a * 2]), py(trail[a * 2 + 1]), px(trail[b * 2]), py(trail[b * 2 + 1]), TRAIL_INK);
        }
        for (StandStatePayload.Seg q : s.path()) {
            int cn = sample(q.segment(), q.x(), q.y(), q.from());
            for (int i = 0; i < cn; i++)
                GuiStyle.line(g, curveX[i], curveY[i], curveX[i + 1], curveY[i + 1], PATH);
            GuiStyle.disc(g, curveX[cn], curveY[cn], 1, PATH);
        }
        if (ghost) {
            int cn = sample(ghostSeg, ghostX, ghostY, 0f);
            // Grind as dashes: a whole piece is mostly gap, a minced one a solid line.
            int dash = Math.max(1, Math.round(8 * ghostGrind));
            dashedCurve(g, cn, skin.accent(), dash, 8 - dash, 0);
            dashedCurve(g, cn, skin.accent(), dash, 8 - dash, 1);
            flask(g, curveX[cn], curveY[cn], 0x80FFFFFF & GLASS | 0x60000000, 0x6058B8E8, true);
        }
        flask(g, px(s.x()), py(s.y()), GLASS, lerp(LIQUID_SPENT, LIQUID, s.potency()), true);
        g.disableScissor();
    }

    /** The segment from arc fraction {@code from} to its end, sampled into {@link #curveX} and {@link #curveY} as screen points; the last index. */
    private int sample(PotionBrew.Segment seg, float x, float y, float from) {
        for (int i = 0; i <= CURVE_N; i++) {
            seg.at(from + (1f - from) * i / CURVE_N, tmp);
            curveX[i] = px(x + tmp[0]);
            curveY[i] = py(y + tmp[1]);
        }
        return CURVE_N;
    }

    /** The sampled curve as dashes {@code dash} px long with {@code gap} between, marching along it; {@code shift} px sideways thickens it. */
    private void dashedCurve(GuiGraphics g, int n, int colour, int dash, int gap, int shift) {
        float carry = 0f;
        for (int i = 0; i < n; i++) {
            float len = (float) Math.hypot(curveX[i + 1] - curveX[i], curveY[i + 1] - curveY[i]);
            for (float t = 0f; t < len; ) {
                float phase = carry % (dash + gap), run = phase < dash ? dash - phase : dash + gap - phase, end = Math.min(len, t + run);
                if (phase < dash || gap <= 0)
                    GuiStyle.line(g, curveX[i] + shift + Math.round((curveX[i + 1] - curveX[i]) * t / len), curveY[i] + Math.round((curveY[i + 1] - curveY[i]) * t / len),
                            curveX[i] + shift + Math.round((curveX[i + 1] - curveX[i]) * end / len), curveY[i] + Math.round((curveY[i + 1] - curveY[i]) * end / len), colour);
                carry += end - t;
                t = end;
            }
        }
    }

    /** A line of dashes {@code dash} px long with {@code gap} px between; no gap is a solid line. Allocation free. */
    private static void dashed(GuiGraphics g, int x0, int y0, int x1, int y1, int colour, int dash, int gap) {
        float len = (float) Math.hypot(x1 - x0, y1 - y0);
        if (gap <= 0 || len < 1f) {
            GuiStyle.line(g, x0, y0, x1, y1, colour);
            return;
        }
        float period = dash + gap;
        for (float t = 0f; t < len; t += period) {
            float u = Math.min(len, t + dash);
            GuiStyle.line(g, x0 + Math.round((x1 - x0) * t / len), y0 + Math.round((y1 - y0) * t / len),
                    x0 + Math.round((x1 - x0) * u / len), y0 + Math.round((y1 - y0) * u / len), colour);
        }
    }

    /** A textured patch: a translucent base with flecks scattered over it, from a hash of the patch's number so they hold still. */
    private void patch(GuiGraphics g, int x, int y, int r, int seed, int base, int dark, int pale) {
        GuiStyle.disc(g, x, y, r, base);
        for (int k = 0; k < r * 3; k++) {
            int h = (k + seed * 97) * 0x9E3779B1;
            h ^= h >>> 15;
            float a = (h & 1023) / 1023f * Mth.TWO_PI, rad = r * Mth.sqrt(((h >>> 10) & 1023) / 1023f);
            int fx = x + Math.round(Mth.cos(a) * rad), fy = y + Math.round(Mth.sin(a) * rad);
            g.fill(fx, fy, fx + 1 + (h >>> 24 & 1), fy + 1, k % 3 == 0 ? pale : dark);
        }
    }

    /** A small flask standing on the point; see the map-object overload for the steered one. */
    private void flask(GuiGraphics g, int x, int y, int glass, int liquid, boolean mapObject) {
        if (mapObject) {
            // The steered flask is a map object: its body (10 px wide) is the marker's contact radius at scale 1, and it grows and shrinks with the map.
            float k = PotionTuning.MARKER_RADIUS * scale() / 5f;
            g.pose().pushPose();
            g.pose().translate(x, y + 4.5f * k, 0);
            g.pose().scale(k, k, 1f);
            flask(g, 0, 0, glass, liquid, false);
            g.pose().popPose();
            return;
        }
        g.fill(x - 1, y - 9, x + 1, y - 6, glass);
        g.fill(x - 3, y - 6, x + 3, y - 3, glass);
        g.fill(x - 5, y - 3, x + 5, y, glass);
        g.fill(x - 2, y - 5, x + 2, y - 3, liquid);
        g.fill(x - 4, y - 2, x + 4, y - 1, liquid);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        super.renderLabels(g, mx, my);
        g.drawString(font, Component.translatable("gui.feedback.stand.water"), 280, 20, skin.dim(), false);
        g.drawString(font, Component.translatable("gui.feedback.stand.ingredients"), 280, 52, skin.dim(), false);
        g.drawString(font, Component.translatable("gui.feedback.stand.bottles"), 280, 122, skin.dim(), false);
        g.drawString(font, Component.translatable("gui.feedback.stand.heat"), LX, 122, skin.dim(), false);
        g.drawString(font, Component.translatable("gui.feedback.stand.potency"), LX, 142, skin.dim(), false);
        int y = MY + 4;
        for (FormattedCharSequence line : status) {
            g.drawString(font, line, LX + 4, y, skin.ink(), false);
            y += 9;
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mx, int my) {
        super.renderTooltip(g, mx, my);
        if (hoveredSlot != null || !menu.getCarried().isEmpty())
            return;
        if (!onMap(mx, my))
            return;
        List<StandStatePayload.Mark> marks = s.marks();
        for (int i = 0; i < marks.size() && i < names.length; i++)
            if (names[i] != null && Math.hypot(mx - px(marks.get(i).x()), my - py(marks.get(i).y())) <= 10) {
                g.renderTooltip(font, names[i], mx, my);
                return;
            }
        for (StandStatePayload.Mark m : marks)
            if (m.kind() == 6 && m.a() == 0f && Math.hypot(mx - px(m.x()), my - py(m.y())) <= 9) {
                g.renderTooltip(font, Component.translatable("gui.feedback.stand.unknown"), mx, my);
                return;
            }
        for (StandStatePayload.Mark m : marks)
            if (m.kind() == 4 && !m.label().isEmpty() && Math.hypot(mx - px(m.x()), my - py(m.y())) <= 5) {
                g.renderTooltip(font, Component.translatable("gui.feedback.stand.smells", Component.translatable("compound.feedback." + m.label())), mx, my);
                return;
            }
        g.renderTooltip(font, hint, mx, my);
    }

    private static int lerp(int a, int b, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int al = (int) Mth.lerp(t, a >>> 24, b >>> 24), r = (int) Mth.lerp(t, a >> 16 & 255, b >> 16 & 255),
                gg = (int) Mth.lerp(t, a >> 8 & 255, b >> 8 & 255), bl = (int) Mth.lerp(t, a & 255, b & 255);
        return al << 24 | r << 16 | gg << 8 | bl;
    }
}
