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
import java.util.Locale;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.modular.ForgeRhythm;
import io.github.soundgoodizerfan.feedback.modular.ToolTableMenu;
import io.github.soundgoodizerfan.feedback.modular.ToolTableShaping;
import io.github.soundgoodizerfan.feedback.net.ForgeBeginPayload;
import io.github.soundgoodizerfan.feedback.net.ForgeFinishPayload;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The forging rhythm at the Tool Table and the smithing table, ported from the hand hammer's own screen: <b>tap</b> on the
 * left lane (left click or Z) and <b>heavy</b> on the right (right click or X) as notes fall to a hit line, the anvil ringing
 * each blow, a metronome clicking every step after a four-click count-in. A note that falls while the stock is out of its
 * window is dim and dead, and the stock's own heat is shown as a glow behind the part. The panel only plays: it sends the
 * blows and the server judges them; the result shown is the client's own judging of the same hits, seeded by the hits
 * themselves, so the two agree. Owns the sparks, the lanes' grade flashes and the streak.
 */
final class ForgePanel implements WorkPanel {

    /** Ticks of notes shown above the hit line, and the pixels each tick of them takes. */
    private static final int LOOKAHEAD = 40, PX_PER_TICK = 2;
    private static final int LANE_W = 46, TRACK_H = LOOKAHEAD * PX_PER_TICK;

    private enum Phase { NONE, PLAY, DONE }

    private final ToolTableScreen host;
    private Button forge;

    ForgePanel(ToolTableScreen host) {
        this.host = host;
    }

    @Override
    public void init() {
        forge = host.add(GuiStyle.primary(host.skin, Component.translatable("gui.feedback.tool_table.forge"),
                b -> start(), host.left() + ToolTableMenu.TOOL_X, host.top() + ToolTableMenu.TOOL_Y + 10, 70, 14,
                FSounds.UI_STAMP::get));
    }

    @Override
    public boolean busy() {
        return phase != Phase.NONE;
    }

    @Override
    public void tick() {
        forge.visible = host.menu().mode() == ToolTableMenu.MODE_PART && !busy() && host.current().filter(ToolTableShaping.Option::forged).isPresent();
        forge.active = host.canForge();
        tickSparks();
        if (phase != Phase.PLAY)
            return;
        if (flash > 0)
            flash--;
        // The metronome: a click every step from the start, higher on the first of each four, so the
        // count-in is four clicks and the tempo is in the player's ear before a note is due.
        if (ticks % step == 0 && ticks <= ForgeRhythm.end(chosen.variant().rhythm(), step)) {
            int beat = ticks / step;
            GuiSkin.play(SoundEvents.NOTE_BLOCK_HAT.value(), beat % ForgeRhythm.COUNT_IN == 0 ? 1.6f : 1.1f);
        }
        ticks++;
        if (ticks > ForgeRhythm.end(chosen.variant().rhythm(), step) + 2 * step)
            finish();
    }

    @Override
    public boolean click(double mouseX, double mouseY, int button) {
        if (phase == Phase.PLAY) {
            if (button == 0 || button == 1)
                strike(button == 1);
            return true;
        }
        if (phase == Phase.DONE) {
            phase = Phase.NONE;
            return true;
        }
        return false;
    }

    @Override
    public boolean key(int key, int scan, int modifiers) {
        if (phase == Phase.NONE || key == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE)
            return false;
        if (phase == Phase.PLAY && (key == com.mojang.blaze3d.platform.InputConstants.KEY_Z || key == com.mojang.blaze3d.platform.InputConstants.KEY_X))
            strike(key == com.mojang.blaze3d.platform.InputConstants.KEY_X);
        else if (phase == Phase.DONE)
            phase = Phase.NONE;
        return true;
    }

    // The forging screen.
    private Phase phase = Phase.NONE;
    private ToolTableShaping.Option chosen;
    private int chosenIndex;
    private Material material;
    private int step;
    private int[] live;
    private int ticks;
    private final List<ForgeRhythm.Hit> hits = new ArrayList<>();
    private ForgeRhythm.Result result;
    private int flash;
    private boolean flashHeavy;
    private String flashText = "";
    /** Sparks thrown off each blow and off a perfect part: screen-space, short-lived, drawn over the forge. */
    private record Spark(float x, float y, float vx, float vy, int life, int color) { }
    private final List<Spark> sparks = new ArrayList<>();
    private final List<Integer> sparkAge = new ArrayList<>();
    private final net.minecraft.util.RandomSource rng = net.minecraft.util.RandomSource.create();
    /** Ticks each lane stays lit after a blow, and the colour of the grade that lit it. */
    private final int[] laneGlow = new int[2], laneColor = new int[2];
    /** Clean blows in a row; any other blow breaks it. */
    private int streak;


    private void start() {
        var level = host.level();
        Optional<ToolTableShaping.Option> option = host.current();
        if (!host.canForge() || option.isEmpty())
            return;
        var stock = ToolTableShaping.check(host.station(), option.get(), host.menu().stock(), ClientMaterials.on(level), level);
        if (stock.isEmpty() || !ForgeRhythm.workable(stock.get().what().material(), stock.get().coldest()))
            return;
        if (host.menu().machine()) {
            host.press(ToolTableMenu.BTN_MACHINE);
            return;
        }
        chosen = option.get();
        chosenIndex = host.menu().picked();
        material = stock.get().what().material();
        step = ForgeRhythm.step(material);
        live = ForgeRhythm.live(material, stock.get().coldest());
        ticks = 0;
        hits.clear();
        phase = Phase.PLAY;
        PacketDistributor.sendToServer(new ForgeBeginPayload(host.menu().containerId, chosenIndex));
    }

    private void finish() {
        String rhythm = chosen.variant().rhythm();
        result = ForgeRhythm.judge(rhythm, step, hits, live, ForgeRhythm.noiseFor(hits));
        if (result.perfect()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(FSounds.FORGE_FANFARE.get(), 1f));
            float px = host.left() + 8 + (host.width() - 16) / 2f, py = host.top() + 16 + 25;
            for (int wave = 0; wave < 4; wave++)
                burst(px, py, 40, 2f + wave * 1.3f, 40, 0xFFD84A, 0xFFFFFF, 0xFFA030, 0xFFF0A0);
        }
        else if (result.flaw() >= io.github.soundgoodizerfan.feedback.modular.ForgeRhythm.MAX_FLAW)
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(FSounds.FORGE_FAILURE.get(), 1f));
        PacketDistributor.sendToServer(new ForgeFinishPayload(host.menu().containerId, chosenIndex,
                hits.stream().map(ForgeRhythm.Hit::tick).toList(), hits.stream().map(ForgeRhythm.Hit::heavy).toList()));
        phase = Phase.DONE;
    }

    private void strike(boolean heavy) {
        if (phase != Phase.PLAY || hits.size() >= 256)
            return;
        int tick = Math.round(ticks + host.partial());
        hits.add(new ForgeRhythm.Hit(tick, heavy));
        grade(tick, heavy);
        boolean dead = tick < live[0] || tick > live[1];
        GuiSkin.play(dead ? SoundEvents.ANVIL_PLACE : SoundEvents.ANVIL_LAND, heavy ? 0.7f : 1.15f);
    }

    private void burst(float x, float y, int n, float speed, int life, int... colors) {
        for (int i = 0; i < n && sparks.size() < 400; i++) {
            float a = rng.nextFloat() * Mth.TWO_PI, v = speed * (0.3f + rng.nextFloat());
            sparks.add(new Spark(x, y, Mth.cos(a) * v, Mth.sin(a) * v - speed * 0.5f, life / 2 + rng.nextInt(life / 2 + 1),
                    colors[rng.nextInt(colors.length)]));
            sparkAge.add(0);
        }
    }

    private void tickSparks() {
        for (int i = sparks.size() - 1; i >= 0; i--) {
            Spark s = sparks.get(i);
            int age = sparkAge.get(i) + 1;
            if (age >= s.life()) {
                sparks.remove(i);
                sparkAge.remove(i);
                continue;
            }
            sparks.set(i, new Spark(s.x() + s.vx(), s.y() + s.vy(), s.vx() * 0.94f, s.vy() * 0.94f + 0.25f, s.life(), s.color()));
            sparkAge.set(i, age);
        }
        laneGlow[0] = Math.max(0, laneGlow[0] - 1);
        laneGlow[1] = Math.max(0, laneGlow[1] - 1);
    }

    private void drawSparks(GuiGraphics g) {
        for (int i = 0; i < sparks.size(); i++) {
            Spark s = sparks.get(i);
            float life = 1f - (sparkAge.get(i) + host.partial()) / s.life();
            int a = Mth.clamp((int) (life * 255), 0, 255);
            int px = Math.round(s.x() + s.vx() * host.partial()), py = Math.round(s.y() + s.vy() * host.partial());
            g.fill(px, py, px + 2, py + 2, a << 24 | s.color() & 0xFFFFFF);
        }
    }

    /** What the blow just struck would score, shown on its lane for a moment: the client's guess, the server decides. */
    private void grade(int tick, boolean heavy) {
        String rhythm = chosen.variant().rhythm();
        int best = Integer.MAX_VALUE;
        for (int i = 0; i < rhythm.length(); i++) {
            char c = rhythm.charAt(i);
            if (ForgeRhythm.note(c) && (c == 'h') == heavy)
                best = Math.min(best, Math.abs(tick - ForgeRhythm.noteTick(i, step)));
        }
        flashHeavy = heavy;
        flash = 12;
        flashText = tick < live[0] || tick > live[1] ? "forge.feedback.grade.cold"
                : best <= ForgeRhythm.CLEAN_TICKS ? "forge.feedback.grade.clean"
                : best <= ForgeRhythm.LOOSE_TICKS ? "forge.feedback.grade.loose" : "forge.feedback.grade.miss";
        boolean clean = flashText.endsWith("clean"), loose = flashText.endsWith("loose");
        streak = clean ? streak + 1 : 0;
        int lane = heavy ? 1 : 0;
        int w = host.width() - 16;
        float cx = host.left() + 8 + w / 2f + (heavy ? 4 + LANE_W / 2f : -LANE_W - 4 + LANE_W / 2f), cy = host.top() + 16 + 44 + TRACK_H;
        laneGlow[lane] = 8;
        laneColor[lane] = clean ? 0x9BE08A : loose ? 0xF0D060 : 0xE06060;
        int n = (heavy ? 22 : 12) * (clean ? 2 : loose ? 1 : 0) + (clean || loose ? 0 : 5);
        if (clean)
            burst(cx, cy, n, heavy ? 3.4f : 2.6f, 22, 0xFFFFFF, 0xFFE070, 0xFFA030);
        else if (loose)
            burst(cx, cy, n, 2.2f, 16, 0xF0A040, 0xC06020);
        else
            burst(cx, cy, n, 1.2f, 12, 0x707070, 0x504040);
    }


    /** A glow from dull red through orange to yellow-white, by where the stock is against its window. */
    private int glow(float temperature) {
        float from = Math.max(300f, material.minTemperature().value() - 150f), to = Math.min(1500f, material.minTemperature().value() + 500f);
        float t = Math.max(0f, Math.min(1f, (temperature - from) / Math.max(1f, to - from)));
        int r = 0x40 + (int) (0xBF * t), gr = (int) (0x90 * t * t), b = (int) (0x50 * t * t * t);
        return 0xFF000000 | r << 16 | gr << 8 | b;
    }

    @Override
    public void overlay(GuiGraphics g) {
        if (phase == Phase.NONE)
            return;
        int x = host.left() + 8, y = host.top() + 16, w = host.width() - 16, h = 160;
        g.fill(x, y, x + w, y + h, 0xF01A1715);
        host.skin.inset(g, x, y, w, h);
        Component t = Component.literal(chosen.label().getString().toUpperCase(Locale.ROOT));
        g.drawString(host.font(), t, x + (w - host.font().width(t)) / 2, y + 4, host.skin.ink(), false);
        int cx = x + w / 2;
        g.fill(cx - 12, y + 14, cx + 12, y + 36, glow(host.menu().heat(host.level())));
        g.renderItem(partStack(), cx - 8, y + 17);
        if (phase == Phase.PLAY)
            track(g, x, y, w);
        else
            summary(g, x, y, w);
        drawSparks(g);
    }

    /** The part this shape makes, for the picture and for the message: one of its own stock, as a stand-in. */
    private ItemStack partStack() {
        return BuiltInRegistries.ITEM.getOptional(chosen.shaping().item()).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    /** A filled disc, drawn a scanline at a time. */
    private static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.round(Math.sqrt(r * r - dy * dy + 0.25));
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    /** A ring: a disc with the middle taken out. */
    private static void ring(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int outer = (int) Math.round(Math.sqrt(r * r - dy * dy + 0.25));
            int inner = Math.abs(dy) >= r - 1 ? -1 : (int) Math.round(Math.sqrt((r - 2) * (r - 2) - dy * dy + 0.25));
            if (inner < 0) {
                g.fill(cx - outer, cy + dy, cx + outer + 1, cy + dy + 1, color);
            } else {
                g.fill(cx - outer, cy + dy, cx - inner, cy + dy + 1, color);
                g.fill(cx + inner + 1, cy + dy, cx + outer + 1, cy + dy + 1, color);
            }
        }
    }

    /** A note: a rimmed disc with a highlight, a small pale one for a tap and a big warm one for a heavy blow. */
    private static void note(GuiGraphics g, int cx, int cy, boolean heavy, boolean dead) {
        int r = heavy ? 10 : 7;
        int rim = dead ? 0xFF4A4A4A : heavy ? 0xFF8A4A10 : 0xFF6C7480;
        int face = dead ? 0xFF6E6E6E : heavy ? 0xFFF0A848 : 0xFFE6ECF2;
        int shine = dead ? 0xFF8A8A8A : heavy ? 0xFFFFD890 : 0xFFFFFFFF;
        disc(g, cx, cy, r, rim);
        disc(g, cx, cy, r - 2, face);
        disc(g, cx - r / 3, cy - r / 3, Math.max(1, r / 4), shine);
    }

    private void track(GuiGraphics g, int x, int y, int w) {
        String rhythm = chosen.variant().rhythm();
        int tapX = x + w / 2 - LANE_W - 4, heavyX = x + w / 2 + 4;
        int top = y + 44, line = top + TRACK_H;
        int tapC = tapX + LANE_W / 2, heavyC = heavyX + LANE_W / 2;
        g.fill(tapX, top, tapX + LANE_W, line + 12, 0x50000000);
        g.fill(heavyX, top, heavyX + LANE_W, line + 12, 0x50000000);
        float now = ticks + host.partial();
        // The beat, sliding down with the notes: a faint line each step, bright on the first of four.
        for (int beat = 0; beat * step <= ForgeRhythm.end(rhythm, step); beat++) {
            float ahead = beat * step - now;
            if (ahead > LOOKAHEAD || ahead < -2)
                continue;
            int by = line - Math.round(ahead * PX_PER_TICK);
            int c = beat % ForgeRhythm.COUNT_IN == 0 ? 0x50FFFFFF : 0x20FFFFFF;
            g.fill(tapX, by, heavyX + LANE_W, by + 1, c);
        }
        for (int lane = 0; lane < 2; lane++)
            if (laneGlow[lane] > 0) {
                int lx = lane == 0 ? tapX : heavyX;
                g.fillGradient(lx, top, lx + LANE_W, line + 12, 0x00000000, laneGlow[lane] * 12 << 24 | laneColor[lane]);
            }
        ring(g, tapC, line, 9, host.skin.accent());
        ring(g, heavyC, line, 12, host.skin.accent());
        g.drawString(host.font(), Component.translatable("screen.feedback.forge.tap"), tapX + 4, line + 16, host.skin.dim(), false);
        g.drawString(host.font(), Component.translatable("screen.feedback.forge.heavy"), heavyX + 4, line + 16, host.skin.dim(), false);
        for (int i = 0; i < rhythm.length(); i++) {
            char c = rhythm.charAt(i);
            if (!ForgeRhythm.note(c))
                continue;
            int due = ForgeRhythm.noteTick(i, step);
            float ahead = due - now;
            if (ahead > LOOKAHEAD || ahead < -8)
                continue;
            int ny = line - Math.round(ahead * PX_PER_TICK);
            note(g, c == 'h' ? heavyC : tapC, ny, c == 'h', due < live[0] || due > live[1]);
        }
        if (now < ForgeRhythm.COUNT_IN * step) {
            Component count = Component.literal(String.valueOf(ForgeRhythm.COUNT_IN - (int) (now / step)));
            g.drawString(host.font(), count, x + (w - host.font().width(count)) / 2, top - 8, host.skin.accent(), false);
        }
        if (streak >= 3) {
            Component combo = Component.literal(streak + "x");
            g.pose().pushPose();
            g.pose().translate(x + w / 2f, top + 6, 0);
            float pop = 1.4f + Math.min(streak, 20) * 0.03f;
            g.pose().scale(pop, pop, 1f);
            g.drawString(host.font(), combo, -host.font().width(combo) / 2, 0, streak >= 10 ? 0xFFFFD84A : 0xFF9BE08A, true);
            g.pose().popPose();
        }
        if (flash > 0) {
            Component text = Component.translatable(flashText);
            int fc = flashText.endsWith("clean") ? 0xFF9BE08A : flashText.endsWith("loose") ? 0xFFF0D060 : 0xFFE06060;
            g.drawString(host.font(), text, (flashHeavy ? heavyC : tapC) - host.font().width(text) / 2, line - 24, fc, false);
        }
    }

    private void summary(GuiGraphics g, int x, int y, int w) {
        int ly = y + 46;
        Component head = result.perfect() ? Component.translatable("forge.feedback.perfect")
                : Component.translatable("forge.feedback.result." + result.flaw(), chosen.label());
        for (var l : host.font().split(head, w - 24)) {
            g.drawString(host.font(), l, x + (w - host.font().width(l)) / 2, ly, result.perfect() ? 0xFFFFD84A : result.flaw() == 0 ? host.skin.accent() : host.skin.warn(), false);
            ly += 10;
        }
        ly += 6;
        String[] rows = {"clean", "loose", "missed", "dead", "stray"};
        int[] counts = {result.clean(), result.loose(), result.missed(), result.dead(), result.stray()};
        for (int i = 0; i < rows.length; i++) {
            g.drawString(host.font(), Component.translatable("screen.feedback.forge." + rows[i], counts[i]), x + 40, ly, host.skin.ink(), false);
            ly += 11;
        }
        Component more = Component.translatable("gui.feedback.tool_table.continue");
        g.drawString(host.font(), more, x + (w - host.font().width(more)) / 2, y + 140, host.skin.dim(), false);
    }
}
