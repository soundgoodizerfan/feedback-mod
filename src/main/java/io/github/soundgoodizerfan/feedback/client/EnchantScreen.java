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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.modular.Attunements;
import io.github.soundgoodizerfan.feedback.modular.EnchantGame;
import io.github.soundgoodizerfan.feedback.modular.EnchantMenu;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.modular.SgaGlyphs;
import io.github.soundgoodizerfan.feedback.modular.Sigil;
import io.github.soundgoodizerfan.feedback.net.EnchantBeginPayload;
import io.github.soundgoodizerfan.feedback.net.EnchantFinishPayload;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Enchanting Table ({@code docs/feedback_enchanting_spec.md} 7). Left: the part's socket (a marching gold ring when
 * a part sits in it), its name, one pip per attunement it can hold, and the list of effects -- every row dimmed when it
 * does nothing on this part's role, marked when the part has it. Right: the <em>pad</em>, where the effect's name is
 * written in the Standard Galactic Alphabet one letter at a time: the letter's stars glow, and holding the left button
 * and dragging the pen through them lights them; lifting the pen finishes the letter. The pen follows the mouse with the
 * part's material as its inertia, ink runs down in a gauge at the pad's edge, and a Flip box (for effects with a mirror)
 * draws the guide reversed. The part's heat sits beside the pad; under it four lamps for what stands between you and
 * Begin; then the button. A result lands as a stamp with a burst of sparks, and the word stays written until the next Begin.
 *
 * <p>The game ends when the part locks, the word is written, or on Commit; the pen samples go to the server, which
 * replays them. The pen drawn is the same {@link Sigil.Pen} the server runs, so what is shown is what is judged.
 * shortcut: the server reads the part's heat a moment after this side does (the packet's trip), so a start
 * temperature can differ by a degree or two; the replay is the server's, this is only the picture.
 */
public class EnchantScreen extends SkinnedScreen<EnchantMenu> {

    private static final int LIST_X = 10, LIST_Y = 62, LIST_W = 104, LIST_H = 80, ROW = 11, ROWS = 7;
    private static final int SCOPE_X = 122, SCOPE_Y = 24, SCOPE_W = 138, SCOPE_H = 84;
    private static final int HEAT_X = 266, HEAT_W = 10;
    private static final int LAMP_Y = 113;
    private static final int BTN_X = 122, BTN_Y = 128, BTN_W = 154;
    private static final int GOLD = 0xF2CC5A, GOLD_HI = 0xF6DD8A, VIOLET = 0x9A6AE0, ROSE = 0xFF6A8A, AMBER = 0xFFB060;
    private static final String[] LAMPS = {"part", "heat", "effect", "xp"};

    private final List<Attunements.Effect> effects = new ArrayList<>(Attunements.known().values());
    private int chosen = -1, scroll;
    private Button begin, commit;

    private boolean playing;
    private float startTemperature;
    /** One packed pen sample per tick ({@link Sigil#pack}). */
    private final List<Integer> samples = new ArrayList<>();
    private boolean dragging, flip;
    private double mouseX, mouseY;
    private Sigil.Pen pen;
    private Button flipBox;
    /** The effect's word and its glyph cells, cached so drawing allocates nothing. */
    private String word = "";
    private int[][][] glyphs = new int[0][][];
    /** Lit stars of the current letter (display column + 5 * row), refreshed each tick; accuracy of each finished letter. */
    private final boolean[] lit = new boolean[SgaGlyphs.COLUMNS * SgaGlyphs.ROWS];
    private final float[] accuracy = new float[ModularTuning.ENCHANT_WORD_MAX];
    private int markedTicks;
    /** What the last tick sounded for, so each change makes one sound and not one a tick. */
    private boolean wasMarking, wasInWindow, hadPart;
    private String result = "";
    private int resultLevel;
    /** When the result landed (ms, for the stamp) and in motion-clock ticks (for the shake). */
    private long resultAt = -10_000;
    private float shakeAt = Float.NEGATIVE_INFINITY;
    private final Specks specks = new Specks(96);
    private final Gauge ink = Gauge.of(() -> pen == null ? 1f : pen.inkFraction()).vertical().lead(() -> skin.accent());

    public EnchantScreen(EnchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.MYSTIC);
        imageWidth = EnchantMenu.W;
        imageHeight = EnchantMenu.H;
        titleLabelX = 12;
        titleLabelY = 10;
        inventoryLabelY = 10000; // the list column sits where the label would be
        effects.sort(Comparator.comparing(e -> labelOf(e).getString()));
    }

    @Override
    protected void init() {
        super.init();
        begin = addRenderableWidget(GuiStyle.primary(skin, Component.translatable("gui.feedback.enchant.begin"), b -> begin(),
                leftPos + BTN_X, topPos + BTN_Y, BTN_W, 14, FSounds.UI_STAMP::get));
        commit = addRenderableWidget(GuiStyle.button(skin, Component.translatable("gui.feedback.enchant.commit"), b -> finish(),
                leftPos + BTN_X, topPos + BTN_Y, BTN_W, 14));
        commit.visible = false;
        flipBox = addRenderableWidget(GuiStyle.checkbox(skin, Component.translatable("gui.feedback.enchant.flip"), () -> flip,
                b -> flip = !flip, leftPos + SCOPE_X + 3, topPos + SCOPE_Y + SCOPE_H - 14, 40));
        flipBox.visible = false;
    }

    private Attunements.Effect effect() {
        return chosen < 0 || chosen >= effects.size() ? null : effects.get(chosen);
    }

    private static Component labelOf(Attunements.Effect e) {
        return Component.translatable("attunement.feedback." + e.id().getPath());
    }

    /** The effect's name, then a line per distinct behaviour, from the row's kinds and flaws. */
    private static List<Component> effectLines(Attunements.Effect e) {
        List<Component> lines = new ArrayList<>();
        lines.add(labelOf(e).copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        e.reagent().ifPresent(r -> lines.add(Component.translatable("gui.feedback.enchant.effect.reagent",
                io.github.soundgoodizerfan.feedback.potion.PotionEffects.name(r)).withStyle(ChatFormatting.GRAY)));
        roleLines(lines, e.roles());
        if (e.hasMirror()) {
            lines.add(Component.translatable("gui.feedback.enchant.effect.mirror",
                    Component.translatable("attunement.feedback." + e.id().getPath() + ".mirror")).withStyle(ChatFormatting.LIGHT_PURPLE));
            roleLines(lines, e.mirror());
        }
        return lines;
    }

    /** One line per distinct behaviour, naming every role that has it; "anything" when it covers every non-gun role. */
    private static void roleLines(List<Component> out, java.util.Map<String, Attunements.Behaviour> roles) {
        java.util.Map<Attunements.Behaviour, java.util.List<String>> byBehaviour = new java.util.LinkedHashMap<>();
        new TreeMap<>(roles).forEach((role, b) -> byBehaviour.computeIfAbsent(b, k -> new ArrayList<>()).add(role));
        byBehaviour.forEach((b, names) -> {
            MutableComponent what = Component.translatable("tooltip.feedback.attuned.desc." + b.kind());
            if (b.flaw() != 0)
                what.append(Component.translatable(b.flaw() > 0 ? "gui.feedback.enchant.effect.brittle" : "gui.feedback.enchant.effect.tough"));
            // The "On a ...:" lead in the skin's gold, the description in white, so the two read apart.
            // "Anything": the roles cover every role of every non-gun type they appear in (a charge's casing is no tool part).
            // A type's roles are keyed by pattern letter; the row names them by role name.
            java.util.Set<String> all = new java.util.HashSet<>();
            ModularTypes.known().values().stream().filter(type -> type.family() != ModularType.Family.GUN
                    && type.roles().values().stream().anyMatch(r -> names.contains(r.name())))
                    .forEach(type -> type.roles().values().forEach(r -> all.add(r.name())));
            java.util.List<String> missing = all.stream().filter(n -> !names.contains(n)).sorted().toList();
            java.util.function.Function<java.util.Collection<String>, String> joined = c -> c.stream()
                    .map(n -> Component.translatable("modular.feedback.role." + n).getString())
                    .collect(java.util.stream.Collectors.joining("/"));
            MutableComponent lead = missing.isEmpty() ? Component.translatable("gui.feedback.enchant.effect.any")
                    : missing.size() * 3 <= names.size() ? Component.translatable("gui.feedback.enchant.effect.but", joined.apply(missing))
                    : Component.translatable("gui.feedback.enchant.effect.role", joined.apply(names));
            out.add(lead.withStyle(ChatFormatting.GOLD).append(" ").append(what.withStyle(ChatFormatting.WHITE)));
        });
    }

    /** Whether the part in the slot has a role the effect does nothing on; false when the part's roles are not known. */
    private boolean inert(Attunements.Effect e) {
        java.util.Set<String> roles = menu.roles();
        return !roles.isEmpty() && roles.stream().noneMatch(r -> e.on(r, false) != null || e.on(r, true) != null);
    }

    /** What the effect does on this part, from the first role the part could fill that it acts on; null when nothing. */
    private Component behaviour(Attunements.Effect e) {
        for (String role : menu.roles()) {
            Attunements.Behaviour b = e.on(role, false);
            if (b != null)
                return Component.translatable("tooltip.feedback.attuned.desc." + b.kind());
        }
        return null;
    }

    private MadeOf made() {
        return menu.part().get(FDataComponents.MADE_OF.get());
    }

    private boolean owned(Attunements.Effect e) {
        MadeOf m = made();
        return m != null && m.dims().containsKey(e.key());
    }

    private int held() {
        MadeOf m = made();
        return m == null ? 0 : (int) m.dims().keySet().stream().filter(k -> k.startsWith(Attunements.PREFIX)).count();
    }

    private Material.Attuning attuning() {
        Material m = menu.material();
        return m == null ? null : m.attuning();
    }

    /** The part's heat now, or at this tick of the game being played. */
    private float heat() {
        Material.Attuning p = attuning();
        if (playing)
            return EnchantGame.heat(p, startTemperature, samples.size());
        float held = ItemHeat.get(menu.part(), minecraft.level).value();
        return p == null ? held : EnchantGame.start(p, held);
    }

    /** What a session would cost now: from the part's heat when it is seated, else the full price. */
    private int cost() {
        Material.Attuning p = attuning();
        return p == null ? ModularTuning.ENCHANT_XP_COST.value() : EnchantGame.cost(p, playing ? startTemperature : heat());
    }

    private String why() {
        Material.Attuning p = attuning();
        if (effects.isEmpty())
            return "none";
        if (p == null)
            return "put";
        Attunements.Effect e = effect();
        if (e == null)
            return "pick";
        if (menu.attuned(e.id()))
            return "attuned";
        return "ready";
    }

    /** Panel-relative x of lamp {@code i}: lamps and their labels run left to right with a gap, so none overlaps. */
    private int lampX(int i) {
        int x = SCOPE_X + 2;
        for (int k = 0; k < i; k++)
            x += 9 + font.width(Component.translatable("gui.feedback.enchant.lamp." + LAMPS[k])) + 9;
        return x;
    }

    /** Each lamp: 0 dark, 1 lit, 2 refusing. In the order of {@link #LAMPS}. */
    private int[] gates() {
        Material.Attuning p = attuning();
        Attunements.Effect e = effect();
        boolean paid = minecraft.player != null && (minecraft.player.isCreative()
                || minecraft.player.totalExperience >= cost());
        return new int[] {
                p != null ? 1 : 0,
                p == null ? 0 : EnchantGame.held(p, playing ? startTemperature : heat()) ? 0 : 1,
                e == null ? 0 : menu.attuned(e.id()) ? 2 : 1,
                paid ? 1 : 0};
    }

    private void pick(int i) {
        if (i == chosen)
            return;
        chosen = i;
        Attunements.Effect e = effect();
        word = e == null ? "" : Sigil.word(e);
        glyphs = new int[word.length()][][];
        for (int k = 0; k < word.length(); k++)
            glyphs[k] = SgaGlyphs.cells(word.charAt(k));
        pen = null;
        result = "";
        flip = false;
        Material m = menu.material();
        skin.cue(GuiSkin.Cue.SEAT, m == null ? null : m.kind().orElse(null), 1f);
    }

    private void begin() {
        Attunements.Effect e = effect();
        if (e == null || playing || !why().equals("ready"))
            return;
        startTemperature = heat();
        samples.clear();
        pen = new Sigil.Pen(attuning(), startTemperature, word);
        java.util.Arrays.fill(lit, false);
        java.util.Arrays.fill(accuracy, 0f);
        markedTicks = 0;
        wasMarking = false;
        wasInWindow = EnchantGame.held(attuning(), startTemperature) || startTemperature <= attuning().temperature().value();
        result = "";
        resultAt = -10_000;
        playing = true;
        skin.cue(GuiSkin.Cue.START);
        PacketDistributor.sendToServer(new EnchantBeginPayload(menu.containerId, e.id()));
    }

    private void finish() {
        Attunements.Effect e = effect();
        Material m = menu.material();
        if (!playing || e == null || m == null)
            return;
        playing = false;
        dragging = false;
        Sigil.Result r = pen.result();
        for (int i = 0; i < word.length(); i++)
            accuracy[i] = Math.max(pen.letterAccuracy(i, false), pen.letterAccuracy(i, true));
        EnchantGame.Verdict v = EnchantGame.judge(e, EnchantGame.level(m), r.accuracy(), r.negative(), r.reached());
        result = v.none() ? "screen.feedback.enchant.result.none" : v.flaw() > 0 ? "screen.feedback.enchant.result.flawed"
                : "screen.feedback.enchant.result.clean";
        resultLevel = v.level();
        resultAt = net.minecraft.Util.getMillis();
        GuiSkin.play((v.none() ? FSounds.ENCHANT_FIZZ : v.flaw() > 0 ? FSounds.ENCHANT_FLAW : FSounds.ENCHANT_TAKE).get(), 1f);
        float cx = leftPos + SCOPE_X + SCOPE_W / 2f, cy = topPos + SCOPE_Y + SCOPE_H / 2f;
        if (v.none()) {
            shakeAt = GuiStyle.time();
            specks.burst(cx, cy, 12, 1.4f, 0.6f, 0x6A5A8C, 30, 2);
        } else {
            int rgb = v.flaw() > 0 ? AMBER : GOLD_HI;
            specks.burst(cx, cy, 20 + 10 * Math.abs(v.level()), 2.4f, 3.2f, rgb, 34, 2);
            specks.burst(cx, cy, 10, 1.2f, 1.8f, 0xFFFFFF, 40, 1);
        }
        PacketDistributor.sendToServer(new EnchantFinishPayload(menu.containerId, e.id(), startTemperature, List.copyOf(samples)));
    }

    /** Panel-absolute px of a pad position in cell units. */
    private float padX(float cx) {
        return leftPos + SCOPE_X + 1 + Sigil.ORIGIN_X + cx * Sigil.CELL;
    }

    private float padY(float cy) {
        return topPos + SCOPE_Y + 1 + Sigil.ORIGIN_Y + cy * Sigil.CELL;
    }

    private int slotX(int i) {
        return leftPos + SCOPE_X + 1 + 3 + i / 4 * 14;
    }

    private int slotY(int i) {
        return topPos + SCOPE_Y + 1 + 3 + i % 4 * 18;
    }

    /** The guide cell's display column: mirrored when Flip is on. */
    private int col(int[] cell) {
        return flip ? SgaGlyphs.COLUMNS - 1 - cell[0] : cell[0];
    }

    private void refreshLit() {
        java.util.Arrays.fill(lit, false);
        if (pen == null || pen.done())
            return;
        float r2 = pen.reach() * pen.reach();
        for (int[] c : glyphs[pen.letter()])
            for (int k = 0; k < pen.points(); k++) {
                if (pen.pointLetter(k) != pen.letter())
                    continue;
                float dx = pen.pointX(k) - (col(c) + 0.5f), dy = pen.pointY(k) - (c[1] + 0.5f);
                if (dx * dx + dy * dy <= r2) {
                    lit[col(c) + SgaGlyphs.COLUMNS * c[1]] = true;
                    break;
                }
            }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        specks.tick(0.03f);
        boolean has = !menu.part().isEmpty();
        if (has && !hadPart) {
            Material m = menu.material();
            skin.cue(GuiSkin.Cue.SEAT, m == null ? null : m.kind().orElse(null), 1f);
        }
        hadPart = has;
        begin.visible = !playing;
        begin.active = !playing && why().equals("ready");
        commit.visible = playing;
        flipBox.visible = effect() != null && effect().hasMirror();
        if (!playing) {
            if (!has) {
                result = "";
                pen = null;
            }
            return;
        }
        Material.Attuning p = attuning();
        if (p == null || !has || pen == null) { // the part was taken out: the game is off
            playing = false;
            dragging = false;
            pen = null;
            return;
        }
        float prevX = pen.penX(), prevY = pen.penY();
        int x = Mth.clamp((int) Math.round((mouseX - leftPos - SCOPE_X - 1) / Sigil.PAD_W * 255), 0, 255);
        int y = Mth.clamp((int) Math.round((mouseY - topPos - SCOPE_Y - 1) / Sigil.PAD_H * 255), 0, 255);
        int sample = Sigil.pack(x, y, dragging);
        samples.add(sample);
        pen.tick(sample);
        boolean quiet = ClientSettings.reduceMotion();
        float hx = padX(pen.penX()), hy = padY(pen.penY());
        float speed = (float) Math.hypot(pen.penX() - prevX, pen.penY() - prevY);
        boolean window = !EnchantGame.held(p, startTemperature) && EnchantGame.live(p, startTemperature, samples.size() - 1);
        if (window && !wasInWindow)
            GuiSkin.play(FSounds.ENCHANT_WINDOW.get(), 1f);
        wasInWindow = window;
        if (pen.marking()) {
            if (!wasMarking)
                GuiSkin.play(FSounds.ENCHANT_ENGAGE.get(), 1f);
            if (++markedTicks % 5 == 0)
                GuiSkin.play(FSounds.ENCHANT_TONE.get(), 0.7f + Math.min(1f, speed));
            if (!quiet)
                specks.burst(hx, hy, 1, 0.5f, 0.5f, GOLD, 14, 1);
        }
        wasMarking = pen.marking();
        int done = pen.finishedLetter();
        if (done >= 0) {
            accuracy[done] = Math.max(pen.letterAccuracy(done, false), pen.letterAccuracy(done, true));
            if (!quiet)
                for (int[] c : glyphs[done]) {
                    float cx = padX(col(c) + 0.5f), cy = padY(c[1] + 0.5f);
                    specks.spawn(cx, cy, (slotX(done) + c[0] * 2 - cx) / 18f, (slotY(done) + c[1] * 2 - cy) / 18f, GOLD_HI, 18, 2);
                }
            GuiSkin.play(FSounds.ENCHANT_TONE.get(), 0.8f + 0.12f * done);
            GuiSkin.play((accuracy[done] >= 0.75f ? FSounds.ENCHANT_BAND : FSounds.ENCHANT_FLAW).get(), 1f);
        }
        refreshLit();
        if (EnchantGame.locked(p, startTemperature, samples.size()) || samples.size() >= ModularTuning.ENCHANT_MAX_TICKS || pen.done())
            finish();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (!ClientSettings.reduceMotion())
            specks.draw(g, partialTick);
        long age = net.minecraft.Util.getMillis() - resultAt;
        if (!result.isEmpty() && age < 1600) {
            String key = resultLevel == 0 ? "none" : result.endsWith("flawed") ? "flawed" : "clean";
            GuiStyle.stamp(g, Component.translatable("screen.feedback.enchant.stamp." + key, Math.abs(resultLevel)),
                    leftPos + SCOPE_X + SCOPE_W / 2f, topPos + SCOPE_Y + SCOPE_H / 2f, age,
                    resultLevel == 0 ? ROSE : key.equals("flawed") ? AMBER : GOLD);
        }
    }

    private boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= leftPos + x && mx < leftPos + x + w && my >= topPos + y && my < topPos + y + h;
    }

    /** The effect row under the pointer, or -1. */
    private int rowAt(double mx, double my) {
        if (!over(mx, my, LIST_X + 1, LIST_Y + 2, LIST_W - 6, ROW * ROWS))
            return -1;
        int i = scroll + (int) (my - topPos - LIST_Y - 2) / ROW;
        return i < effects.size() ? i : -1;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (over(mx, my, LIST_X, LIST_Y, LIST_W, LIST_H) && effects.size() > ROWS) {
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, effects.size() - ROWS);
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (settled() && button == 0) {
            int row = rowAt(mx, my);
            if (row >= 0) {
                if (playing)
                    skin.cue(GuiSkin.Cue.REJECT);
                else
                    pick(row);
                return true;
            }
            if (playing && over(mx, my, SCOPE_X, SCOPE_Y, SCOPE_W, SCOPE_H) && !(flipBox.visible && flipBox.isMouseOver(mx, my))) {
                dragging = true;
                mouseX = mx;
                mouseY = my;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        mouseX = mx;
        mouseY = my;
        super.mouseMoved(mx, my);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        mouseX = mx;
        mouseY = my;
        if (dragging)
            return true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    private static void pip(GuiGraphics g, int cx, int cy, int colour) {
        g.fill(cx, cy - 2, cx + 1, cy + 3, colour);
        g.fill(cx - 2, cy, cx + 3, cy + 1, colour);
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, colour);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        skin.slot(g, leftPos + EnchantMenu.PART_X, topPos + EnchantMenu.PART_Y);
        inventory(g, EnchantMenu.INV_X, EnchantMenu.INV_Y);
        float t = GuiStyle.time();
        Attunements.Effect e = effect();
        Material.Attuning p = attuning();

        // The socket: a gold ring marches round a part that is seated, faster while it is being worked.
        if (!menu.part().isEmpty()) {
            int x0 = leftPos + EnchantMenu.PART_X - 3, y0 = topPos + EnchantMenu.PART_Y - 3;
            if (ClientSettings.reduceMotion())
                GuiStyle.perimeter(g, x0, y0, x0 + 22, y0 + 22, 0, 84, skin.accent());
            else
                for (int k = 0; k < 84; k += 12)
                    GuiStyle.perimeter(g, x0, y0, x0 + 22, y0 + 22, (int) (t * (playing ? 1.6f : 0.5f)) + k, 6, skin.accent());
        }
        // One pip per attunement the part can hold; filled for what it has.
        Material m = menu.material();
        if (m != null) {
            int cap = EnchantGame.capacity(m), have = held();
            for (int i = 0; i < cap; i++)
                pip(g, leftPos + 43 + i * 8, topPos + 42, i < have ? skin.accent() : 0x50FFFFFF & (skin.dim() | 0xFF000000));
        }

        // The effect list.
        int lx = leftPos + LIST_X, ly = topPos + LIST_Y;
        skin.inset(g, lx, ly, LIST_W, LIST_H);
        for (int r = 0; r < ROWS && scroll + r < effects.size(); r++) {
            Attunements.Effect row = effects.get(scroll + r);
            int ry = ly + 2 + r * ROW, rx = lx + 1, rw = LIST_W - 6;
            boolean picked = scroll + r == chosen;
            skin.row(g, rx, ry, rw, ROW, picked, !playing && over(mx, my, LIST_X + 1, LIST_Y + 2 + r * ROW, rw, ROW));
            int colour = picked ? skin.accent() : inert(row) ? skin.dim() : skin.ink();
            g.drawString(font, font.plainSubstrByWidth(labelOf(row).getString(), rw - 14), rx + 4, ry + 2, colour, false);
            if (owned(row))
                pip(g, rx + rw - 6, ry + ROW / 2, skin.accent());
        }
        skin.scrollbar(g, lx + LIST_W - 4, ly + 2, LIST_H - 4, effects.size(), ROWS, scroll);

        pad(g, t, e, p, mx, my);

        // Heat bar: the attuning window on it, the part's heat marked.
        int hx = leftPos + HEAT_X, hy = topPos + SCOPE_Y;
        skin.inset(g, hx - 1, hy, HEAT_W + 2, SCOPE_H);
        if (p != null) {
            int top = hy + 1, h = SCOPE_H - 2;
            float lo = p.lock().value() - 20f, hi = Math.max(playing ? startTemperature : heat(), p.temperature().value()) + 20f;
            int wTop = top + Math.round((1f - (p.temperature().value() - lo) / (hi - lo)) * h);
            int wBottom = top + Math.round((1f - (p.lock().value() - lo) / (hi - lo)) * h);
            g.fill(hx, top, hx + HEAT_W, wTop, 0x30FF6A4A);
            g.fillGradient(hx, wTop, hx + HEAT_W, wBottom, 0x70F2CC5A, 0x30F2CC5A);
            g.fill(hx, wBottom, hx + HEAT_W, top + h, 0x302A4AA0);
            int y = top + Math.round((1f - Mth.clamp((heat() - lo) / (hi - lo), 0f, 1f)) * h);
            g.fill(hx - 2, y - 1, hx + HEAT_W + 2, y + 1, skin.ink());
        }

        // Lamps: what stands between you and Begin.
        int[] gates = gates();
        for (int i = 0; i < LAMPS.length; i++) {
            int x = leftPos + lampX(i), y = topPos + LAMP_Y;
            int colour = gates[i] == 1 ? skin.accent() : gates[i] == 2 ? skin.warn() : 0x50FFFFFF & (skin.dim() | 0xFF000000);
            g.fill(x + 1, y, x + 5, y + 6, colour);
            g.fill(x, y + 1, x + 6, y + 5, colour);
            if (gates[i] == 1)
                g.fill(x - 2, y - 2, x + 8, y + 8, 0x18000000 | GOLD);
            Component label = Component.translatable("gui.feedback.enchant.lamp." + LAMPS[i]);
            g.drawString(font, label, x + 9, y - 1, gates[i] == 0 ? skin.dim() : skin.ink(), false);
        }
    }

    /** A glyph's lit cells as {@code cell}-px squares from (x, y). */
    private void glyph(GuiGraphics g, int[][] cells, int x, int y, int cell, int colour) {
        for (int[] c : cells)
            g.fill(x + c[0] * cell, y + c[1] * cell, x + c[0] * cell + cell, y + c[1] * cell + cell, colour);
    }

    /** The pad: the letter's stars, the pen's trail and head, written letters, ink, and the word or hint when idle. */
    private void pad(GuiGraphics g, float t, Attunements.Effect e, Material.Attuning p, int mx, int my) {
        int sx = leftPos + SCOPE_X + GuiStyle.shake(shakeAt, 2), sy = topPos + SCOPE_Y;
        int ix = sx + 1, iw = SCOPE_W - 2, iy = sy + 1, ih = SCOPE_H - 2;
        skin.inset(g, sx, sy, SCOPE_W, SCOPE_H);
        boolean session = pen != null && (playing || !result.isEmpty());
        if (playing && pen != null) {
            if (!pen.done()) {
                int pulse = ClientSettings.reduceMotion() ? 0 : (int) (0x10 * Mth.sin(t * 0.15f));
                for (int[] c : glyphs[pen.letter()]) {
                    int cx = ix + Math.round(Sigil.ORIGIN_X + (col(c) + 0.5f) * Sigil.CELL), cy = iy + Math.round(Sigil.ORIGIN_Y + (c[1] + 0.5f) * Sigil.CELL);
                    boolean on = lit[col(c) + SgaGlyphs.COLUMNS * c[1]];
                    g.fill(cx - 5, cy - 5, cx + 6, cy + 6, (on ? 0x30 : 0x18) << 24 | (on ? GOLD : VIOLET));
                    g.fill(cx - 3, cy - 3, cx + 4, cy + 4, on ? 0xE0000000 | GOLD_HI : (0x40 + pulse) << 24 | VIOLET);
                }
            }
            int px = Integer.MIN_VALUE, py = 0;
            for (int k = 0; k < pen.points(); k++) {
                if (pen.pointLetter(k) != pen.letter()) {
                    px = Integer.MIN_VALUE;
                    continue;
                }
                int x = Math.round(padX(pen.pointX(k))), y = Math.round(padY(pen.pointY(k)));
                g.fill(x, y - 1, x + 1, y + 2, 0x20F6DD8A);
                if (px != Integer.MIN_VALUE)
                    GuiStyle.line(g, px, py, x, y, skin.accent());
                px = x;
                py = y;
            }
            int hx = Math.round(padX(pen.penX())), hy = Math.round(padY(pen.penY()));
            g.fill(hx - 3, hy - 3, hx + 4, hy + 4, (pen.marking() ? 0x50 : 0x28) << 24 | (pen.marking() ? GOLD_HI : VIOLET));
            g.fill(hx - 1, hy - 1, hx + 2, hy + 2, pen.marking() ? 0xFFFFFFFF : skin.ink());
            // Written letters, small in the left margin.
            for (int i = 0; i < pen.letter() && i < glyphs.length; i++)
                glyph(g, glyphs[i], slotX(i) + (sx - leftPos - SCOPE_X), slotY(i), 2, 0xFF000000 | (accuracy[i] >= 0.75f ? GOLD : skin.dim()));
            ink.render(g, skin, ix + iw - 9, iy + 3, 5, ih - 6);
            if (!pen.reached() && !dragging)
                paragraph(g, Component.translatable("gui.feedback.enchant.stars_hint"), ix + 4, iy + ih - 11 - 10, iw - 20, lerpHint(t));
            return;
        }
        // Idle or finished: the whole word, readable; coloured by accuracy once written.
        if (!word.isEmpty()) {
            int wx = ix + (iw - (word.length() * 19 - 4)) / 2, wy = iy + 8;
            for (int i = 0; i < glyphs.length; i++)
                glyph(g, glyphs[i], wx + i * 19, wy, 3, 0xFF000000 | (!session ? skin.ink() : accuracy[i] >= 0.75f ? GOLD : skin.dim()));
        }
        if (session && !result.isEmpty()) {
            g.drawString(font, Component.translatable(result, Math.abs(resultLevel)), ix + 4, iy + ih - 11, skin.accent(), false);
        } else {
            String why = why();
            Component line = Component.translatable(why.equals("ready") ? "gui.feedback.enchant.begin_hint" : "gui.feedback.enchant.state." + why);
            int y = word.isEmpty() ? iy + (ih - 10 * font.split(line, iw - 16).size()) / 2 : iy + 40;
            paragraph(g, line, ix + 8, y, iw - 16, why.equals("ready") ? lerpHint(t) : skin.warn());
        }
    }

    /** The hint breathes between dim and accent, so the eye finds it. */
    private int lerpHint(float t) {
        return ClientSettings.reduceMotion() ? skin.accent() : GuiSkin.lerp(skin.dim(), skin.accent(), 0.5f + 0.5f * Mth.sin(t * 0.12f));
    }

    private int paragraph(GuiGraphics g, Component c, int x, int y, int w, int colour) {
        for (FormattedCharSequence line : font.split(c, w)) {
            g.drawString(font, line, x, y, colour, false);
            y += 10;
        }
        return y;
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        super.renderTooltip(g, mouseX, mouseY);
        if (hoveredSlot != null)
            return;
        int row = rowAt(mouseX, mouseY);
        if (row >= 0) {
            Attunements.Effect e = effects.get(row);
            List<Component> lines = new ArrayList<>(effectLines(e));
            if (owned(e))
                lines.add(Component.translatable("gui.feedback.enchant.owned").withStyle(ChatFormatting.YELLOW));
            if (inert(e))
                lines.add(Component.translatable("gui.feedback.enchant.inert").withStyle(ChatFormatting.GRAY));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        for (int i = 0; i < LAMPS.length; i++)
            if (over(mouseX, mouseY, lampX(i) - 2, LAMP_Y - 3, 9 + font.width(Component.translatable("gui.feedback.enchant.lamp." + LAMPS[i])) + 4, 12)) {
                int state = gates()[i];
                String key = "gui.feedback.enchant.gate." + LAMPS[i] + (state == 1 ? ".ok" : state == 2 ? ".no" : ".off");
                MutableComponent tip = Component.translatable(key);
                if (i == 3)
                    tip = Component.translatable(key, Component.translatable("feedback.unit.xpu",
                            Readout.figure(Quantity.EXPERIENCE, cost())));
                g.renderTooltip(font, tip, mouseX, mouseY);
                return;
            }
        if (over(mouseX, mouseY, EnchantMenu.PART_X + 24, 40, 60, 6) && attuning() != null)
            g.renderTooltip(font, Component.translatable("gui.feedback.enchant.pips", held(), EnchantGame.capacity(menu.material())), mouseX, mouseY);
        else if (over(mouseX, mouseY, SCOPE_X, SCOPE_Y, SCOPE_W, SCOPE_H))
            g.renderTooltip(font, Component.translatable("gui.feedback.enchant.tip.gauge"), mouseX, mouseY);
        else if (over(mouseX, mouseY, HEAT_X - 1, SCOPE_Y, HEAT_W + 2, SCOPE_H))
            g.renderTooltip(font, Component.translatable("gui.feedback.enchant.tip.heat"), mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        // Beside the socket: the part, or the invitation.
        if (menu.part().isEmpty())
            g.drawString(font, Component.translatable("gui.feedback.enchant.seat"), 40, 31, skin.dim(), false);
        else
            g.drawString(font, font.plainSubstrByWidth(menu.part().getHoverName().getString(), 72), 40, 27, skin.ink(), false);
        g.drawString(font, Component.translatable("gui.feedback.enchant.list"), LIST_X + 2, LIST_Y - 10, skin.dim(), false);
        Attunements.Effect e = effect();
        g.drawString(font, e == null ? Component.translatable("gui.feedback.enchant.no_effect").getString() : font.plainSubstrByWidth(labelOf(e).getString(), SCOPE_W - 4),
                SCOPE_X + 2, SCOPE_Y - 11, e == null ? skin.dim() : skin.accent(), false);
        g.drawString(font, Component.translatable("gui.feedback.enchant.heat"), HEAT_X + HEAT_W / 2 - font.width(Component.translatable("gui.feedback.enchant.heat")) / 2,
                SCOPE_Y - 11, skin.dim(), false);
    }
}
