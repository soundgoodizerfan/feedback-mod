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

    private static final int BAND_X = 26, BAND_Y = 14, MX = 94 + BAND_X, MY = 22 + BAND_Y, MS = 126, S = MS / 2, LX = 8, LW = 80, SEND_EVERY = 5, FULL_PACE_RAD_PER_TICK = 3, TRAIL = 48;
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
    // The compass: one entry per compound class that has a direction, in order of angle; label rectangles are those placed in the last frame.
    private String[] cName = new String[0];
    private float[] cDir = new float[0];
    private int[] cColor = new int[0];
    private Component[] cLabel = new Component[0];
    /** Each class's name cut to 2, 3 and 4 letters, and how wide each cut is, for the trim's narrow bands. */
    private String[][] cShort = new String[0][];
    private int[][] cShortWidth = new int[0][];
    /** Where the trim put each class this frame: the dot's centre and radius, and the rectangle that hovers it (dot and label). */
    private final int[] dotX = new int[16], dotY = new int[16], dotR = new int[16], hx0 = new int[16], hy0 = new int[16], hx1 = new int[16], hy1 = new int[16];
    private Map<String, Float> hoverCompounds = Map.of();
    private double camX, camY, zoom = 1;
    private boolean panning;
    private float swirl, pace, lastAngle, ghostX, ghostY, ghostGrind;
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
        s = last != null && last.pos().equals(menu.pos()) ? last : new StandStatePayload(menu.pos(), 0f, 0f, 1f, 20f, 0, 0, 0, 225f, 1f, "", List.of(0f, 0f), List.of(), new long[0]);
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
        var classes = new java.util.ArrayList<Map.Entry<String, io.github.soundgoodizerfan.feedback.process.Compound>>();
        for (var e : io.github.soundgoodizerfan.feedback.process.CompoundTable.get().all().entrySet())
            if (e.getValue().direction().isPresent())
                classes.add(e);
        classes.sort(java.util.Comparator.comparing(e -> Mth.positiveModulo(e.getValue().direction().get(), 360f)));
        int n = Math.min(classes.size(), dotX.length);
        cName = new String[n];
        cDir = new float[n];
        cColor = new int[n];
        cLabel = new Component[n];
        cShort = new String[n][];
        cShortWidth = new int[n][];
        for (int i = 0; i < n; i++) {
            cName[i] = classes.get(i).getKey();
            cDir[i] = classes.get(i).getValue().direction().get();
            cColor[i] = 0xFF000000 | classes.get(i).getValue().color().orElse(0x4A3A28);
            cLabel[i] = Component.translatable("compound.feedback." + cName[i]);
            String full = cLabel[i].getString();
            cShort[i] = new String[3];
            cShortWidth[i] = new int[3];
            for (int k = 0; k < 3; k++) {
                cShort[i][k] = full.substring(0, Math.min(full.length(), k + 2));
                cShortWidth[i][k] = font.width(cShort[i][k]);
            }
        }
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

    /** The ghost: where the hovered ingredient would take the marker from the end of the path, as the server would work it out. */
    private void preview() {
        ghost = false;
        hoverCompounds = Map.of();
        Slot slot = hoveredSlot;
        ItemStack stack = slot == null ? ItemStack.EMPTY : slot.getItem();
        if (stack.isEmpty() || Minecraft.getInstance().level == null)
            return;
        var level = Minecraft.getInstance().level;
        Map<String, Float> compounds = BrewingStandBlockEntity.foodOf(level, stack).map(BrewingStandBlockEntity::steering).orElse(Map.of());
        if (compounds.isEmpty())
            return;
        hoverCompounds = compounds;
        FoodCut cut = stack.get(FDataComponents.FOOD_CUT.get());
        float rot = BrewingStandBlockEntity.rotOf(level, stack);
        float extraction = PotionBrew.extraction(BrewingStandBlockEntity.surface(cut == null ? 0 : cut.level()), s.temperature());
        float[] v = PotionBrew.vector(compounds, rot, extraction, s.rotDegrees());
        List<Float> path = s.path();
        ghostGrind = BrewingStandBlockEntity.surface(cut == null ? 0 : cut.level());
        ghostX = path.get(path.size() - 2) + v[0];
        ghostY = path.get(path.size() - 1) + v[1];
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
        drawCompass(g);
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
                    GuiStyle.ring(g, x, y, Math.max(Math.round(m.radius() * scale()), 10), 1, NODE);
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
        List<Float> path = s.path();
        for (int i = 0; i + 3 < path.size(); i += 2)
            GuiStyle.line(g, px(path.get(i)), py(path.get(i + 1)), px(path.get(i + 2)), py(path.get(i + 3)), PATH);
        for (int i = 2; i + 1 < path.size(); i += 2)
            GuiStyle.disc(g, px(path.get(i)), py(path.get(i + 1)), 1, PATH);
        if (ghost) {
            int gx0 = px(path.get(path.size() - 2)), gy0 = py(path.get(path.size() - 1)), gx1 = px(ghostX), gy1 = py(ghostY);
            // Grind as dashes: a whole piece is mostly gap, a minced one a solid line.
            int dash = Math.max(1, Math.round(8 * ghostGrind));
            dashed(g, gx0, gy0, gx1, gy1, skin.accent(), dash, 8 - dash);
            dashed(g, gx0 + 1, gy0, gx1 + 1, gy1, skin.accent(), dash, 8 - dash);
            flask(g, gx1, gy1, 0x80FFFFFF & GLASS | 0x60000000, 0x6058B8E8, true);
        }
        flask(g, px(s.x()), py(s.y()), GLASS, lerp(LIQUID_SPENT, LIQUID, s.potency()), true);
        g.disableScissor();
    }

    /**
     * The compass: a trim set into the map's frame, fixed to the viewport (it neither zooms nor pans). For each compound class, a ray from the
     * viewport's centre at the class's angle meets the viewport's edge; a dot in the class's colour sits in the band just outside that point, with a
     * short name beside it on the side away from the map. Labels are cut to what the band fits (two to four letters), slid along the border past
     * one another, and dropped to a bare dot (the tooltip has the full name) when there is no room. Nothing is drawn inside the map. Never fogged:
     * the chemistry is knowable, only the map is hidden. Hovering an ingredient enlarges the dots of the classes it carries.
     */
    private void drawCompass(GuiGraphics g) {
        float half = MS / 2f;
        int cx = leftPos + MX + MS / 2, cy = topPos + MY + MS / 2;
        for (int i = 0; i < cName.length; i++) {
            float a = cDir[i] * Mth.DEG_TO_RAD, co = Mth.cos(a), si = Mth.sin(a);
            // The ray meets the nearer of the two pairs of sides; a tie goes to the left and right ones.
            boolean side = Math.abs(co) * half >= Math.abs(si) * half * 0.9999f && Math.abs(co) >= Math.abs(si);
            float t = side ? half / Math.max(1e-4f, Math.abs(co)) : half / Math.max(1e-4f, Math.abs(si));
            float ex = cx + co * t, ey = cy - si * t;
            int dx = Math.round(ex + (side ? Math.signum(co) * 7 : 0)), dy = Math.round(ey + (side ? 0 : -Math.signum(si) * 7));
            Float strength = hoverCompounds.get(cName[i]);
            float bright = strength == null ? 0f : Mth.clamp(0.4f + strength * 1.5f, 0.4f, 1f);
            int r = 3 + Math.round(bright * 3f);
            dotX[i] = dx;
            dotY[i] = dy;
            dotR[i] = r;
            if (bright > 0f)
                GuiStyle.disc(g, dx, dy, r + 1, 0xFF1A0E04);
            GuiStyle.disc(g, dx, dy, r, cColor[i]);
            // The label: outward from the dot on a side band, along the border on the top and bottom, then slid clear of what is placed.
            hx0[i] = dx - r;
            hy0[i] = dy - r;
            hx1[i] = dx + r;
            hy1[i] = dy + r;
            for (int k = 2; k >= 0; k--) {
                // The longest cut that fits the band, tried longest first (index 2 is four letters).
                int w = cShortWidth[i][k];
                int avail = side ? BAND_X - 7 - r - 3 : MS;
                if (w > avail)
                    continue;
                boolean placedLabel = false;
                for (int slide = 0; slide <= 30 && !placedLabel; slide += 6)
                    for (int sign = slide == 0 ? 1 : -1; sign <= 1 && !placedLabel; sign += 2) {
                        int x0, y0 = side ? dy - 4 + sign * slide : dy - 4;
                        if (side)
                            x0 = co > 0 ? dx + r + 2 : dx - r - 2 - w;
                        else
                            x0 = (co >= 0 ? dx + r + 2 : dx - r - 2 - w) + sign * slide;
                        int x1 = x0 + w, y1 = y0 + 9;
                        int bandX0 = leftPos + MX - BAND_X + 2, bandX1 = leftPos + MX + MS + BAND_X - 2;
                        int bandY0 = topPos + MY - BAND_Y + 1, bandY1 = topPos + MY + MS + BAND_Y - 1;
                        // Inside the frame, and not over the map: a top or bottom label stays in its band, a side one in its.
                        boolean inside = x0 >= bandX0 && x1 <= bandX1 && y0 >= bandY0 && y1 <= bandY1
                                && (side ? x1 <= leftPos + MX || x0 >= leftPos + MX + MS : y1 <= topPos + MY || y0 >= topPos + MY + MS);
                        if (!inside)
                            continue;
                        boolean clear = true;
                        for (int j = 0; j < i && clear; j++)
                            clear = x1 + 1 < hx0[j] || x0 - 1 > hx1[j] || y1 < hy0[j] || y0 > hy1[j];
                        // Nor over a dot that comes later.
                        for (int j = i + 1; j < cName.length && clear; j++) {
                            int ox = labelDotX(j, cx, cy, half), oy = labelDotY(j, cx, cy, half);
                            clear = x1 + 1 < ox - 3 || x0 - 1 > ox + 3 || y1 < oy - 3 || y0 > oy + 3;
                        }
                        if (!clear)
                            continue;
                        g.drawString(font, cShort[i][k], x0, y0, bright > 0f ? 0xFF1A0E04 : 0xFF4A3A28, false);
                        hx0[i] = Math.min(hx0[i], x0);
                        hy0[i] = Math.min(hy0[i], y0);
                        hx1[i] = Math.max(hx1[i], x1);
                        hy1[i] = Math.max(hy1[i], y1);
                        placedLabel = true;
                    }
                if (placedLabel)
                    break;
            }
        }
    }

    /** Where class {@code i}'s dot sits, for a label to keep clear of it before its own turn. */
    private int labelDotX(int i, int cx, int cy, float half) {
        float a = cDir[i] * Mth.DEG_TO_RAD, co = Mth.cos(a), si = Mth.sin(a);
        boolean side = Math.abs(co) >= Math.abs(si);
        float t = side ? half / Math.max(1e-4f, Math.abs(co)) : half / Math.max(1e-4f, Math.abs(si));
        return Math.round(cx + co * t + (side ? Math.signum(co) * 7 : 0));
    }

    private int labelDotY(int i, int cx, int cy, float half) {
        float a = cDir[i] * Mth.DEG_TO_RAD, co = Mth.cos(a), si = Mth.sin(a);
        boolean side = Math.abs(co) >= Math.abs(si);
        float t = side ? half / Math.max(1e-4f, Math.abs(co)) : half / Math.max(1e-4f, Math.abs(si));
        return Math.round(cy - si * t + (side ? 0 : -Math.signum(si) * 7));
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
        for (int k = 0; k < cName.length; k++)
            if (mx >= hx0[k] && mx <= hx1[k] && my >= hy0[k] && my <= hy1[k]) {
                g.renderComponentTooltip(font, carrying(k), mx, my);
                return;
            }
        if (!onMap(mx, my))
            return;
        List<StandStatePayload.Mark> marks = s.marks();
        for (int i = 0; i < marks.size() && i < names.length; i++)
            if (names[i] != null && Math.hypot(mx - px(marks.get(i).x()), my - py(marks.get(i).y())) <= Math.max(10, marks.get(i).radius() * scale())) {
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

    /** The class's name, then what you hold or have in the stand that carries it: by compound, never by what the item is. */
    private List<Component> carrying(int c) {
        List<Component> lines = new java.util.ArrayList<>();
        lines.add(cLabel[c]);
        var level = Minecraft.getInstance().level;
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Slot slot : menu.slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || level == null)
                continue;
            var food = BrewingStandBlockEntity.foodOf(level, stack).orElse(null);
            if (food != null && food.compounds().containsKey(cName[c]) && seen.add(stack.getHoverName().getString()) && seen.size() <= 8)
                lines.add(stack.getHoverName().copy().withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        if (lines.size() == 1)
            lines.add(Component.translatable("gui.feedback.stand.compass.none").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        return lines;
    }

    private static int lerp(int a, int b, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int al = (int) Mth.lerp(t, a >>> 24, b >>> 24), r = (int) Mth.lerp(t, a >> 16 & 255, b >> 16 & 255),
                gg = (int) Mth.lerp(t, a >> 8 & 255, b >> 8 & 255), bl = (int) Mth.lerp(t, a & 255, b & 255);
        return al << 24 | r << 16 | gg << 8 | bl;
    }
}
