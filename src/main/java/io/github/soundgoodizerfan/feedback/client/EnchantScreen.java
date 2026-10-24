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
import java.util.List;
import java.util.TreeMap;

import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.modular.Attunements;
import io.github.soundgoodizerfan.feedback.modular.EnchantGame;
import io.github.soundgoodizerfan.feedback.modular.EnchantMenu;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.net.EnchantBeginPayload;
import io.github.soundgoodizerfan.feedback.net.EnchantFinishPayload;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Enchanting Table ({@code docs/feedback_enchanting_spec.md} 7): the part on the left, and on the right the
 * gauge of the attuning game -- a track for the polarization with the target band shaded, a marker for it and a
 * second for the field held, and a bar for the part's heat with its attuning window on it. The field is the mouse:
 * hold the left button over the gauge and the field is how far the pointer sits above (positive) or below the
 * track's middle; let go and it is zero. (Keys were the other option; one is enough.)
 *
 * <p>The game ends when the part locks or on Commit; the field samples go to the server, which replays them.
 * The polarization drawn is the same model run here ({@link EnchantGame#run}), so what is shown is what is judged.
 * shortcut: the server reads the part's heat a moment after this side does (the packet's trip), so a start
 * temperature can differ by a degree or two; the replay is the server's, this is only the picture.
 */
public class EnchantScreen extends SkinnedScreen<EnchantMenu> {

    private static final int GAUGE_X = 132, TRACK_W = 14, TRACK_Y = 24, TRACK_H = 72, HEAT_X = 176, HEAT_W = 10;

    private final List<Attunements.Effect> effects = new ArrayList<>(Attunements.known().values());
    private int chosen;
    /** The searchable window that picks the effect, as the blueprint table's picks a type. */
    private final SkinPopup<Integer> effectPopup = new SkinPopup<>(skin, Component.translatable("gui.feedback.enchant.pick_effect"));
    private Button effectButton, begin, commit;

    private boolean playing;
    private float startTemperature;
    private final List<Integer> samples = new ArrayList<>();
    private boolean dragging;
    private double mouseY;
    private float polarization;
    private boolean reached;
    /** What the last tick sounded for, so each change makes one sound and not one a tick. */
    private boolean wasActing, wasInBand, wasInWindow;
    private String result = "";

    public EnchantScreen(EnchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.MYSTIC);
        imageWidth = EnchantMenu.W;
        imageHeight = EnchantMenu.H;
        titleLabelX = 12;
        titleLabelY = 10;
        inventoryLabelY = 10000; // the button column sits where the label would be
    }

    @Override
    protected void init() {
        super.init();
        effectButton = addRenderableWidget(GuiStyle.button(skin, effectLabel(), b -> {
            if (effects.isEmpty() || playing)
                return;
            List<SkinPopup.Entry<Integer>> entries = new ArrayList<>();
            for (int i = 0; i < effects.size(); i++)
                entries.add(new SkinPopup.Entry<>(i, labelOf(effects.get(i)), effectLines(effects.get(i))));
            effectPopup.open(entries, i -> {
                chosen = i;
                b.setMessage(effectLabel());
                b.setTooltip(effectTooltip());
            });
        }, leftPos + 10, topPos + 52, 104, 16));
        effectButton.setTooltip(effectTooltip());
        begin = addRenderableWidget(GuiStyle.primary(skin, Component.translatable("gui.feedback.enchant.begin"), b -> begin(),
                leftPos + 10, topPos + 72, 104, 14, FSounds.UI_STAMP::get));
        commit = addRenderableWidget(GuiStyle.button(skin, Component.translatable("gui.feedback.enchant.commit"), b -> finish(),
                leftPos + 10, topPos + 88, 104, 14));
    }

    private Attunements.Effect effect() {
        return effects.isEmpty() ? null : effects.get(chosen);
    }

    private Component effectLabel() {
        Attunements.Effect e = effect();
        return e == null ? Component.empty() : Component.translatable("attunement.feedback." + e.id().getPath());
    }

    private static Component labelOf(Attunements.Effect e) {
        return Component.translatable("attunement.feedback." + e.id().getPath());
    }

    /** The effect's name, then a line per distinct behaviour, from the row's kinds and flaws. */
    private static List<Component> effectLines(Attunements.Effect e) {
        List<Component> lines = new ArrayList<>();
        lines.add(labelOf(e).copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        roleLines(lines, e.roles());
        if (e.hasMirror()) {
            lines.add(Component.translatable("gui.feedback.enchant.effect.mirror",
                    Component.translatable("attunement.feedback." + e.id().getPath() + ".mirror")).withStyle(ChatFormatting.LIGHT_PURPLE));
            roleLines(lines, e.mirror());
        }
        return lines;
    }

    /** The chosen effect's lines as the effect button's tooltip. */
    private Tooltip effectTooltip() {
        Attunements.Effect e = effect();
        if (e == null)
            return null;
        MutableComponent t = Component.empty();
        List<Component> lines = effectLines(e);
        for (int i = 0; i < lines.size(); i++)
            t.append(i == 0 ? lines.get(i) : Component.literal("\n").append(lines.get(i)));
        return Tooltip.create(t);
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

    private String why() {
        Material.Attuning p = attuning();
        if (effect() == null)
            return "none";
        if (p == null)
            return "put";
        if (effect() != null && menu.attuned(effect().id()))
            return "attuned";
        if (!EnchantGame.hot(p, heat()))
            return "cold";
        return "ready";
    }

    private void begin() {
        Attunements.Effect e = effect();
        if (e == null || playing || !why().equals("ready"))
            return;
        startTemperature = heat();
        samples.clear();
        polarization = 0;
        reached = false;
        wasActing = wasInBand = false;
        wasInWindow = startTemperature <= attuning().temperature().value();
        result = "";
        playing = true;
        PacketDistributor.sendToServer(new EnchantBeginPayload(menu.containerId, e.id()));
    }

    private void finish() {
        Attunements.Effect e = effect();
        Material m = menu.material();
        if (!playing || e == null || m == null)
            return;
        playing = false;
        dragging = false;
        EnchantGame.Verdict v = EnchantGame.judge(e, EnchantGame.level(m), polarization, reached);
        result = v.none() ? "screen.feedback.enchant.result.none" : v.flaw() > 0 ? "screen.feedback.enchant.result.flawed"
                : "screen.feedback.enchant.result.clean";
        resultLevel = v.level();
        GuiSkin.play((v.none() ? FSounds.ENCHANT_FIZZ : v.flaw() > 0 ? FSounds.ENCHANT_FLAW : FSounds.ENCHANT_TAKE).get(), 1f);
        PacketDistributor.sendToServer(new EnchantFinishPayload(menu.containerId, e.id(), startTemperature, List.copyOf(samples)));
    }

    private int resultLevel;

    @Override
    protected void containerTick() {
        super.containerTick();
        begin.active = !playing && why().equals("ready");
        commit.visible = playing;
        effectButton.active = !playing && !effects.isEmpty();
        if (!playing) {
            if (menu.part().isEmpty())
                result = "";
            return;
        }
        Material.Attuning p = attuning();
        if (p == null || menu.part().isEmpty()) { // the part was taken out: the game is off
            playing = false;
            return;
        }
        double mid = topPos + TRACK_Y + TRACK_H / 2.0;
        int field = dragging ? (int) Math.round(Mth.clamp((mid - mouseY) / (TRACK_H / 2.0), -1, 1) * 100) : 0;
        samples.add(field);
        reached = false;
        boolean[] r = new boolean[1];
        polarization = EnchantGame.run(p, startTemperature, samples, r);
        reached = r[0];
        sounds(p, field);
        if (EnchantGame.heat(p, startTemperature, samples.size()) <= p.lock().value() || samples.size() >= io.github.soundgoodizerfan.feedback.modular.ModularTuning.ENCHANT_MAX_TICKS)
            finish();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        effectPopup.render(g, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        return effectPopup.keyPressed(key, scan, modifiers) || super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        return effectPopup.charTyped(c, modifiers) || super.charTyped(c, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        return effectPopup.mouseScrolled(scrollY) || super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    /** One sound when the part enters its window, when the field starts to act, and when P enters the band; a tone while it acts. */
    private void sounds(Material.Attuning p, int field) {
        float heat = EnchantGame.heat(p, startTemperature, samples.size()), f = Math.abs(EnchantGame.field(field));
        boolean window = heat <= p.temperature().value() && heat > p.lock().value();
        boolean acting = window && f > p.coercive();
        Attunements.Effect e = effect();
        boolean inBand = e != null && (Math.abs(polarization - e.target()) <= e.band()
                || e.hasMirror() && Math.abs(polarization + e.target()) <= e.band());
        if (window && !wasInWindow)
            GuiSkin.play(FSounds.ENCHANT_WINDOW.get(), 1f);
        if (acting && !wasActing)
            GuiSkin.play(FSounds.ENCHANT_ENGAGE.get(), 1f);
        if (acting && samples.size() % 5 == 0)
            GuiSkin.play(FSounds.ENCHANT_TONE.get(), 0.6f + 0.9f * f);
        if (inBand && !wasInBand)
            GuiSkin.play(FSounds.ENCHANT_BAND.get(), 1f);
        wasInWindow = window;
        wasActing = acting;
        wasInBand = inBand;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (effectPopup.mouseClicked(mx, my, button))
            return true;
        if (playing && settled() && button == 0 && mx >= leftPos + GAUGE_X - 14 && mx <= leftPos + HEAT_X
                && my >= topPos + TRACK_Y - 8 && my <= topPos + TRACK_Y + TRACK_H + 8) {
            dragging = true;
            mouseY = my;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            mouseY = my;
            return true;
        }
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

    private static int trackY(float value) {
        return TRACK_Y + Math.round((1f - Mth.clamp(value, -1f, 1f)) / 2f * TRACK_H);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        skin.slot(g, leftPos + EnchantMenu.PART_X, topPos + EnchantMenu.PART_Y);
        inventory(g, EnchantMenu.INV_X, EnchantMenu.INV_Y);
        Attunements.Effect e = effect();
        Material.Attuning p = attuning();
        int tx = leftPos + GAUGE_X, ty = topPos;
        // The polarization track, the target band shaded, zero marked.
        skin.inset(g, tx - 1, ty + TRACK_Y - 1, TRACK_W + 2, TRACK_H + 2);
        g.fill(tx, ty + TRACK_Y + TRACK_H / 2, tx + TRACK_W, ty + TRACK_Y + TRACK_H / 2 + 1, 0x40FFFFFF);
        if (e != null)
            g.fill(tx, ty + trackY(e.target() + e.band()), tx + TRACK_W, ty + trackY(e.target() - e.band()), 0x60F2CC5A);
        if (e != null && e.hasMirror()) // the mirror lands below zero: a band as far under as the target is over
            g.fill(tx, ty + trackY(-e.target() + e.band()), tx + TRACK_W, ty + trackY(-e.target() - e.band()), 0x609A6AE0);
        g.fill(tx - 3, ty + trackY(polarization) - 1, tx + TRACK_W + 3, ty + trackY(polarization) + 1, skin.ink());
        if (playing && dragging) {
            int fy = ty + trackY(EnchantGame.field(samples.isEmpty() ? 0 : samples.get(samples.size() - 1)));
            g.fill(tx - 9, fy - 1, tx - 4, fy + 2, skin.accent());
        }
        // The heat bar: the attuning window on it, the part's heat marked.
        int hx = leftPos + HEAT_X;
        skin.inset(g, hx - 1, ty + TRACK_Y - 1, HEAT_W + 2, TRACK_H + 2);
        if (p != null) {
            float lo = p.lock().value() - 20f, hi = Math.max(playing ? startTemperature : heat(), p.temperature().value()) + 20f;
            int wTop = ty + TRACK_Y + Math.round((1f - (p.temperature().value() - lo) / (hi - lo)) * TRACK_H);
            int wBottom = ty + TRACK_Y + Math.round((1f - (p.lock().value() - lo) / (hi - lo)) * TRACK_H);
            g.fill(hx, wTop, hx + HEAT_W, wBottom, 0x60F2CC5A);
            int hy = ty + TRACK_Y + Math.round((1f - Mth.clamp((heat() - lo) / (hi - lo), 0f, 1f)) * TRACK_H);
            g.fill(hx - 2, hy - 1, hx + HEAT_W + 2, hy + 1, skin.ink());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        g.drawString(font, Component.translatable("gui.feedback.enchant.field"), GAUGE_X - 14, TRACK_Y - 11, skin.dim(), false);
        g.drawString(font, Component.translatable("gui.feedback.enchant.heat"), HEAT_X - 4, TRACK_Y - 11, skin.dim(), false);
        // Beside the part slot: what the table needs, or what the last session came to.
        if (!result.isEmpty())
            g.drawWordWrap(font, Component.translatable(result, Math.abs(resultLevel)), 38, 26, 76, skin.accent());
        else
            g.drawWordWrap(font, Component.translatable("gui.feedback.enchant.state." + why(),
                    Readout.figure(Quantity.TEMPERATURE, heat())), 38, 26, 76, why().equals("ready") ? skin.ink() : skin.warn());
        g.drawString(font, Component.translatable("gui.feedback.enchant.cost", Component.translatable("feedback.unit.xpu",
                Readout.figure(Quantity.EXPERIENCE, io.github.soundgoodizerfan.feedback.modular.ModularTuning.ENCHANT_XP_COST.value()))),
                EnchantMenu.PART_X, 60, skin.dim(), false);
    }
}
