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
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.modular.MaterialColors;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.modular.PrepWork;
import io.github.soundgoodizerfan.feedback.modular.ToolTableMenu;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import io.github.soundgoodizerfan.feedback.net.PrepareBeginPayload;
import io.github.soundgoodizerfan.feedback.net.PrepareFinishPayload;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Prep Table's finish ({@code docs/feedback_modular_dishes_spec.md} 4): the seasoning is poured onto the finished dish
 * itself, which is drawn here (its container, and its foods laid in it). Hold the button to pour, let go at the marks. The
 * food's viscosity sets the speed ({@link PrepWork#flow}, the server's own function) and the look and sound with it: thick
 * runs slow in a fat ribbon that pools, thin runs fast in a fine stream that floods out over the rim. The coat of sauce
 * grows over the dish as the pour amount does; over the marks is a price, not a refusal. One packet when the button is
 * let go (from {@link #lock}); Esc discards.
 */
public class PrepPourScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.BRICK;
    private static final int PW = 240, PH = 214, REVEAL_HOLD = 60;
    /** The dish's rim height on screen, and the glaze of the ceramic (screen-local: the dish is the subject of this screen). */
    private static final int DISH_Y = 150, CERAMIC = 0xFFD9C7A0;
    private static final int PLAY = 0, JUDGE = 1, REVEAL = 2;
    /** A sweep needs this much mouse travel (screen px) to count; each drops this many grains. */
    private static final int SWEEP_PX = 14, GRAINS_PER_STROKE = 4;

    private final Opening opening = new Opening(SKIN);
    private final Screen parent;
    private final int containerId;
    private final ItemStack food;
    /** The dish's foods, in the order laid, read from its design once at open. */
    private final ItemStack[] laid;
    /** Half the container's width at each row from the rim down; its length is the container's height. */
    private final int[] half;
    private final boolean bowl;
    private final int rgb;
    private final float flow;
    /** How the finish goes on ({@link PrepWork#mode}): poured, shaken from a shaker, or laid down piece by piece. */
    private final int mode;
    /** Shake: sweeps made, where the shaker is, the sweep under way; grains lie where they fell. Place: the pieces laid, in 0..1000 marks. */
    private int strokes, shakerX, lastX = Integer.MIN_VALUE, dir, lastStroke = -99, grains;
    private float travel;
    private final int[] grainX = new int[FTuning.SHAKE_MAX_STROKES * GRAINS_PER_STROKE], grainY = new int[FTuning.SHAKE_MAX_STROKES * GRAINS_PER_STROKE];
    private final java.util.List<Integer> pieces = new java.util.ArrayList<>();
    /** How thick it runs: 0 water to 4 honey, in decades of viscosity. */
    private final float thick;
    private int held, flourish, soundGap, phase, judge, result;
    private SimpleSoundInstance bgm;
    private boolean pouring, done, sent;
    private float slosh, partial;
    private int px, py, cx, rimY, foodY, ticks;
    private final Specks specks = new Specks(64);
    private List<net.minecraft.util.FormattedCharSequence> hint = List.of();

    private PrepPourScreen(Screen parent, int containerId, ItemStack food, Material material, int rgb, ItemStack[] laid, String container) {
        super(Component.translatable("gui.feedback.prep_pour.title"));
        this.parent = parent;
        this.containerId = containerId;
        this.food = food;
        this.rgb = rgb;
        this.laid = laid;
        this.flow = PrepWork.flow(material);
        this.mode = PrepWork.mode(material);
        this.thick = (float) Math.log10(Math.max(1f, material.molten().viscosity().orElse(1f)));
        this.bowl = !container.contains("plate") && !container.contains("skewer");
        // Bowl: deep and curved; plate: wide and flat; skewer: a stick the foods are strung on.
        this.half = new int[bowl ? 26 : container.contains("plate") ? 6 : 3];
        int top = bowl ? 58 : container.contains("plate") ? 84 : 88, bottom = bowl ? 24 : container.contains("plate") ? 66 : 88;
        for (int i = 0; i < half.length; i++) {
            float t = (float) i / half.length;
            half[i] = Math.round(top + (bottom - top) * t * t);
        }
    }

    /** Open over the Prep Table's screen and tell the server the pour has begun. */
    static void open(Screen parent, ToolTableMenu menu) {
        var level = Minecraft.getInstance().level;
        ItemStack seasoning = menu.season().copyWithCount(1);
        ModularDesign design = ModularStacks.design(menu.work());
        ModularType type = design == null ? null : ModularTypes.known().get(design.type());
        Optional<Material.Located> what = Material.find(ClientMaterials.on(level), seasoning);
        if (what.isEmpty() || type == null)
            return;
        // The foods lie in the container in layer order; their icons are the items their materials come as.
        java.util.List<ItemStack> foods = new java.util.ArrayList<>();
        for (String letter : type.placed()) {
            ModularDesign.Part p = design.part(letter);
            Material m = p == null || letter.equals(type.primary()) ? null : ClientMaterials.on(level).get(p.material());
            ResourceLocation item = m == null ? null : m.forms().get(p.form());
            if (item != null)
                BuiltInRegistries.ITEM.getOptional(item).ifPresent(i -> foods.add(new ItemStack(i)));
        }
        ModularDesign.Part container = design.part(type.primary());
        PacketDistributor.sendToServer(new PrepareBeginPayload(menu.containerId, 1));
        Minecraft.getInstance().setScreen(new PrepPourScreen(parent, menu.containerId, seasoning, what.get().material(),
                MaterialColors.of(what.get().id()), foods.toArray(new ItemStack[0]),
                container == null ? "" : container.kind().isEmpty() ? "plate" : container.kind())); // a flatbread lies flat, like a plate
    }

    @Override
    protected void init() {
        opening.start();
        px = (width - PW) / 2;
        py = Math.max(0, (height - PH) / 2);
        cx = px + PW / 2;
        rimY = py + DISH_Y;
        foodY = rimY - (bowl ? 10 : 15);
        hint = font.split(Component.translatable(mode == PrepWork.SHAKE ? "gui.feedback.prep_pour.hint_shake"
                : mode == PrepWork.PLACE ? "gui.feedback.prep_pour.hint_place" : "gui.feedback.prep_pour.hint"), PW - 24);
        shakerX = cx;
        if (mode == PrepWork.PLACE)
            addRenderableWidget(GuiStyle.primary(SKIN, Component.translatable("gui.feedback.prep_cut.done"), b -> placeDone(),
                    px + PW - 92, py + 22 + 170 - 22, 80, 16, FSounds.UI_STAMP::get));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** The strength so far, the figure the server will grade the same input to. */
    private float strengthNow() {
        return mode == PrepWork.SHAKE ? PrepWork.shaken(strokes)
                : mode == PrepWork.PLACE ? PrepWork.placed(pieces) : PrepWork.strength(PrepWork.poured(held, flow));
    }

    /** How much of the dish the sauce covers now, as drawn: 1 is the whole surface; the held ticks so far plus the part of a tick under way. */
    private float coverage() {
        return Math.min(1.15f, (PrepWork.poured(held, flow) + (pouring ? flow * partial : 0f)) / FTuning.POUR_FULL);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (phase == JUDGE) {
            reveal();
            return true;
        }
        if (!opening.settled() || sent || done || super.mouseClicked(x, y, button))
            return true;
        if (mode == PrepWork.PLACE) {
            place(x, button);
            return true;
        }
        if (button == 0) {
            pouring = true;
            if (mode == PrepWork.SHAKE)
                lastX = (int) x;
            else
                GuiSkin.play(SoundEvents.BOTTLE_EMPTY, 1.2f - thick * 0.15f);
        }
        return true;
    }

    /** Left-click lays a piece where it was aimed, right-click lifts the nearest. */
    private void place(double x, int button) {
        int reach = half[0], mark = Mth.clamp(Math.round((float) (x - (cx - reach)) / (2 * reach) * PrepWork.LINE), 0, PrepWork.LINE);
        if (button == 0 && pieces.size() < FTuning.PLACE_MAX) {
            pieces.add(mark);
            int at = pieceX(mark);
            specks.burst(at, foodY + 2, 4, 0.6f, 1.2f, rgb, 14, 2);
            GuiSkin.play(SoundEvents.GRASS_PLACE, 1.3f + pieces.size() * 0.04f);
        } else if (button == 1 && !pieces.isEmpty()) {
            int near = 0;
            for (int i = 1; i < pieces.size(); i++)
                if (Math.abs(pieces.get(i) - mark) < Math.abs(pieces.get(near) - mark))
                    near = i;
            pieces.remove(near);
            GuiSkin.play(SKIN.click(), 0.8f);
        }
    }

    private int pieceX(int mark) {
        return cx - half[0] + mark * 2 * half[0] / PrepWork.LINE;
    }

    private void placeDone() {
        if (!pieces.isEmpty() && !sent && phase == PLAY) {
            done = true;
            lock();
        }
    }

    /** Shake: the shaker follows the mouse over the dish, and a sweep back and forth drops a pinch. */
    @Override
    public void mouseMoved(double mx, double my) {
        if (mode != PrepWork.SHAKE || !pouring || sent)
            return;
        int x = (int) mx, dx = x - lastX;
        lastX = x;
        shakerX = Mth.clamp(x, cx - half[0], cx + half[0]);
        if (dx == 0)
            return;
        int sign = dx > 0 ? 1 : -1;
        if (sign == dir)
            travel += Math.abs(dx);
        else {
            if (travel >= SWEEP_PX && ticks - lastStroke >= FTuning.SHAKE_TICKS_PER_STROKE)
                stroke();
            dir = sign;
            travel = Math.abs(dx);
        }
    }

    private void stroke() {
        lastStroke = ticks;
        strokes++;
        int reach = half[0];
        java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
        for (int i = 0; i < GRAINS_PER_STROKE && grains < grainX.length; i++, grains++) {
            grainX[grains] = Mth.clamp(shakerX + r.nextInt(-12, 13), cx - reach + 3, cx + reach - 3);
            grainY[grains] = foodY + 1 + r.nextInt(7);
        }
        specks.burst(shakerX, py + 56, 3, 0.3f, 0.8f, rgb, 18, 2);
        GuiSkin.play(SoundEvents.GRAVEL_STEP, 1.8f + r.nextFloat() * 0.3f);
        if (strokes >= FTuning.SHAKE_MAX_STROKES)
            release();
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && pouring)
            release();
        return true;
    }

    private void release() {
        pouring = false;
        if (mode == PrepWork.SHAKE ? strokes > 0 : held > 0) {
            done = true;
            lock();
        }
    }

    /**
     * Let go: the one packet goes now and the result is known, but the player is not told yet. The music of
     * what was poured plays while the dish is covered; then it is shown (or skipped to with a click or Esc).
     */
    private void lock() {
        sent = true;
        PacketDistributor.sendToServer(new PrepareFinishPayload(containerId, 1,
                mode == PrepWork.PLACE ? List.copyOf(pieces) : List.of(), mode == PrepWork.SHAKE ? strokes : mode == PrepWork.PLACE ? 0 : held));
        result = PrepWork.pourClass(strengthNow());
        phase = JUDGE;
        judge = PrepWork.JUDGE_TICKS[result];
        bgm = SimpleSoundInstance.forUI(result == PrepWork.CLEAN ? FSounds.COOKING_BGM_GREAT.get()
                : result == PrepWork.SHAKY ? FSounds.COOKING_BGM_SUCCESS.get() : FSounds.COOKING_BGM_FAIL.get(), 1f);
        Minecraft.getInstance().getSoundManager().play(bgm);
    }

    /** The cover comes off: the fanfare for a good pour or a failed one, and the picture that goes with it. */
    private void reveal() {
        if (phase != JUDGE)
            return;
        phase = REVEAL;
        flourish = REVEAL_HOLD;
        stopMusic();
        boolean good = result == PrepWork.CLEAN || result == PrepWork.SHAKY;
        GuiSkin.play(good ? FSounds.COOKING_FANFARE_SUCCESS.get() : FSounds.COOKING_FANFARE_FAIL.get(), 1f);
        if (result == PrepWork.CLEAN)
            specks.burst(cx, foodY - 4, 24, 2.6f, 3.4f, 0xFFF0A0, 30, 2);
        else if (result == PrepWork.HEAVY || result == PrepWork.LIGHT)
            slosh = 4f;
    }

    private void stopMusic() {
        if (bgm != null)
            Minecraft.getInstance().getSoundManager().stop(bgm);
        bgm = null;
    }

    @Override
    public void tick() {
        ticks++;
        float lvl = PrepWork.poured(held, flow);
        if (mode == PrepWork.POUR && pouring && !sent) {
            held++;
            if (held >= FTuning.POUR_MAX_TICKS)
                release();
            if (thick < 1.5f) {
                specks.burst(cx, foodY, 2, 1.8f, 2.6f, rgb, 14, 2);
                slosh = Math.min(3f, slosh + 0.5f);
            } else {
                specks.burst(cx, foodY, 1, 0.5f, 0.9f, rgb, 22, 3);
                slosh = Math.min(2f, slosh + 0.15f);
            }
            if (lvl > FTuning.POUR_FULL) {
                // Over the rim: it runs down both sides of the container.
                specks.spawn(cx - half[0], rimY + 2, -0.8f, 0.4f, rgb, 24, 3);
                specks.spawn(cx + half[0], rimY + 2, 0.8f, 0.4f, rgb, 24, 3);
            }
            if (--soundGap <= 0) {
                soundGap = thick < 1.5f ? 3 : 6;
                float pitch = Mth.clamp(1.7f - thick * 0.25f, 0.6f, 1.7f);
                GuiSkin.play(thick < 1.5f ? SoundEvents.GENERIC_SPLASH : SoundEvents.HONEY_BLOCK_SLIDE, pitch);
            }
        }
        slosh *= 0.9f;
        specks.tick(thick < 1.5f ? 0.22f : 0.1f);
        if (phase == JUDGE && --judge <= 0)
            reveal();
        else if (phase == REVEAL && --flourish <= 0)
            opening.close();
    }

    @Override
    public void onClose() {
        // Esc while judging skips to the result; after it, or before any pour, it leaves.
        if (phase == JUDGE)
            reveal();
        else
            opening.close();
    }

    @Override
    public void removed() {
        stopMusic();
        super.removed();
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        return opening.closing() || super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
    }

    private Component word;
    private int wordClass = -1;
    private GuiGraphics frameG;
    private int frameMx, frameMy;
    private float framePt;
    private final Runnable paintFrame = () -> paint(frameG, frameMx, frameMy, framePt);

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        if (opening.shut()) {
            Minecraft.getInstance().setScreen(parent);
            return;
        }
        partial = partialTick;
        frameG = g;
        frameMx = mx;
        frameMy = my;
        framePt = partialTick;
        opening.render(g, width, height, px, py, PW, PH, 0f, paintFrame);
    }

    /** The frame body, a method so the per-frame call allocates no capturing lambda. */
    private void paint(GuiGraphics g, int mx, int my, float partialTick) {
        SKIN.panel(g, px, py, PW, PH);
        g.drawString(font, title, px + 12, py + 9, SKIN.ink(), false);
        if ((mode == PrepWork.SHAKE ? strokes > 0 : mode == PrepWork.PLACE ? !pieces.isEmpty() : held > 0) && phase != JUDGE) {
            float strength = strengthNow();
            int cls = PrepWork.pourClass(strength);
            if (cls != wordClass) {
                wordClass = cls;
                word = ToolTableMenu.pourName(strength);
            }
            g.drawString(font, word, px + PW - 12 - font.width(word), py + 9, SKIN.accent(), false);
        }
        SKIN.inset(g, px + 8, py + 22, PW - 16, 170);
        dish(g);
        cover(g);
        int hy = py + PH - 20;
        for (var l : hint) {
            g.drawString(font, l, px + 12, hy, SKIN.dim(), false);
            hy += 9;
        }
        specks.draw(g, partialTick);
    }

    /** The cloche over the dish while it is judged, wobbling as the music builds, then lifting off as the result is shown. */
    private void cover(GuiGraphics g) {
        if (phase == PLAY || (phase == REVEAL && flourish < REVEAL_HOLD - 20))
            return;
        int lift = phase == REVEAL ? (REVEAL_HOLD - flourish) * 4 : 0;
        int wob = phase == JUDGE ? Math.round(Mth.sin((ticks + partial) * 0.7f) * (1f + (1f - judge / (float) PrepWork.JUDGE_TICKS[result]) * 2f)) : 0;
        int x0 = cx - half[0] - 14 + wob, x1 = cx + half[0] + 14 + wob, y1 = rimY + half.length + 6 - lift, y0 = foodY - 30 - lift;
        g.fill(x0, y0, x1, y1, SKIN.ink());
        g.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, SKIN.dim());
        g.fill(x0 + 5, y0 + 5, x0 + 9, y1 - 6, 0x50FFFFFF);
        g.fill((x0 + x1) / 2 - 6, y0 - 6, (x0 + x1) / 2 + 6, y0, SKIN.ink());
        if (phase == JUDGE) {
            Component wait = Component.translatable("gui.feedback.prep_pour.judging");
            g.drawString(font, wait, (x0 + x1) / 2 - font.width(wait) / 2, (y0 + y1) / 2 - 4, SKIN.ink(), false);
        }
    }

    /** The dish with its foods in it, the seasoning tipped over it, the stream, and the sauce coating the dish as it is poured. */
    private void dish(GuiGraphics g) {
        int fill = 0xFF000000 | rgb;
        int reach = half[0], n = laid.length;
        // The foods, laid left to right across the container, under its rim.
        for (int i = 0; i < n; i++)
            g.renderItem(laid[i], cx - reach + (i + 1) * 2 * reach / (n + 1) - 8, foodY);
        // The container: a silhouette row by row, ink at the edges and ceramic between.
        for (int i = 0; i < half.length; i++) {
            g.fill(cx - half[i], rimY + i, cx + half[i], rimY + i + 1, SKIN.ink());
            g.fill(cx - half[i] + 2, rimY + i, cx + half[i] - 2, rimY + i + 1, CERAMIC);
        }
        // The coat: grows out from where the stream lands, a thin film for thin sauce, a fat dome for thick.
        float cover = coverage();
        int cw = Math.round(Math.min(cover, 1f) * reach), ct = Math.round(Mth.clamp(2f + thick * 1.5f, 2f, 8f));
        int sway = Math.round(Mth.sin((ticks + partial) * 0.9f) * slosh);
        if (mode == PrepWork.POUR && cw > 0) {
            g.fill(cx - cw, foodY + sway / 2, cx + cw, foodY + ct + sway / 2, fill);
            g.fill(cx - cw, foodY + sway / 2, cx + cw, foodY + 1 + sway / 2, 0xFF000000 | GuiSkin.lerp(fill, 0xFFFFFFFF, 0.35f));
            if (thick >= 1.5f)
                g.fill(cx - cw / 2, foodY + ct, cx + cw / 2, foodY + ct + 3, fill);
        }
        // The marks to stop at: the extent the coat should reach, bracketed over the dish so it reads across the stream.
        if (mode == PrepWork.POUR) {
            int mark = Math.round((float) FTuning.POUR_TARGET / FTuning.POUR_FULL * reach);
            for (int x = cx - mark; x < cx + mark; x += 4)
                g.fill(x, foodY - 8, Math.min(x + 2, cx + mark), foodY - 7, SKIN.accent());
            g.fill(cx - mark, foodY - 8, cx - mark + 1, foodY + 14, SKIN.accent());
            g.fill(cx + mark - 1, foodY - 8, cx + mark, foodY + 14, SKIN.accent());
        }
        if (mode == PrepWork.SHAKE) {
            // The shaker: its icon follows the mouse, tipped and rattling while held; grains lie where they fell.
            for (int i = 0; i < grains; i++)
                g.fill(grainX[i], grainY[i], grainX[i] + 2, grainY[i] + 2, fill);
            g.renderItem(food, shakerX - 8, py + 34 + (pouring ? Math.round(Mth.sin((ticks + partial) * 2.2f) * 2f) : 0));
        } else if (mode == PrepWork.PLACE) {
            // The pieces: round slices on the dish, with a hole; the pointer shows where the next would land.
            int light = 0xFF000000 | GuiSkin.lerp(fill, 0xFFFFFFFF, 0.4f);
            for (int m : pieces) {
                int x = pieceX(m);
                g.fill(x - 5, foodY + 1, x + 5, foodY + 8, SKIN.ink());
                g.fill(x - 4, foodY + 2, x + 4, foodY + 7, fill);
                g.fill(x - 1, foodY + 3, x + 1, foodY + 6, light);
            }
        } else
            // The pourer: the seasoning's icon, tipped over the dish.
            g.renderItem(food, cx - 8, py + 28 + (pouring ? 2 : 0));
        if (mode == PrepWork.POUR && pouring && !sent) {
            // Thin: a fine fast stream; thick: a fat slow ribbon that swings and lands in a pool.
            int w = Math.round(Mth.clamp(2f + thick * 1.5f, 2f, 8f)), y0 = py + 48;
            for (int y = y0; y < foodY; y += 3) {
                int x = cx + Math.round(Mth.sin(y * 0.18f + (ticks + partial) * (thick < 1.5f ? 1.6f : 0.4f)) * (thick < 1.5f ? 0.8f : 1.8f));
                g.fill(x - w / 2, y, x - w / 2 + w, Math.min(y + 3, foodY), fill);
                g.fill(x - w / 2, y, x - w / 2 + 1, Math.min(y + 3, foodY), 0x60FFFFFF);
            }
        }
    }
}
