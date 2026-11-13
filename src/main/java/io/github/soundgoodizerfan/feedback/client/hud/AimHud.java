// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.hud;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.client.ClientSettings;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.client.render.DataLinkFrame;
import io.github.soundgoodizerfan.feedback.client.render.DataNodeRenderer;
import io.github.soundgoodizerfan.feedback.client.render.DeferredDataRenderer;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * Everything drawn around the crosshair in place of the action bar: a reading card to its right (an instrument's dial, or a
 * block's report), a needle orbiting it, a mode strip and short cues below it, and a half-made link's chip to its left.
 *
 * <h2>Taken from Create, and not</h2>
 * The reading card follows Create's Engineer's Goggles overlay ({@code GoggleOverlayRenderer}, mc1.21.1/dev, MIT; design read,
 * no code copied): a GUI layer that checks the crosshair's target each frame, a card offset from the crosshair's centre, and a
 * short slide in when it appears. Not taken: Create builds the card's lines from the client's block entity every frame, live;
 * here a reading is one deliberate click composed on the server (a sealed vessel is not watched for free, see
 * {@code ThermometerItem}), so the card holds the figure it was given while the player keeps aiming at that block or creature,
 * and fades when they look away. Also not taken: the goggles' item gate, the tooltip-box look (a dial is drawn here), and the
 * per-frame list and colour allocation -- every string is measured once when it arrives.
 *
 * <h2>Look</h2>
 * Drawn on the skin of the tool in hand, picked by item tag ({@code feedback:hud_skin/<skin>}), {@link GuiSkin#VANILLA}
 * when no tag matches; scaled by {@link ClientSettings#aimScale()}.
 */
public final class AimHud {

    private static final int FADE_MS = 300, GAP = 14, DIAL_R = 12;
    private static final TagKey<Item>[] SKIN_TAGS = skinTags();

    private AimHud() {
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Feedback.id("aim_hud"), AimHud::render);
    }

    @SuppressWarnings("unchecked")
    private static TagKey<Item>[] skinTags() {
        GuiSkin[] skins = GuiSkin.values();
        TagKey<Item>[] out = new TagKey[skins.length];
        for (int i = 0; i < skins.length; i++)
            out[i] = TagKey.create(Registries.ITEM, Feedback.id("hud_skin/" + skins[i].name().toLowerCase(java.util.Locale.ROOT)));
        return out;
    }

    /** The skin of what the player holds: main hand first, then the off hand; {@code VANILLA} when neither is tagged. */
    static GuiSkin skinInHand() {
        var player = Minecraft.getInstance().player;
        if (player == null)
            return GuiSkin.VANILLA;
        GuiSkin s = skinOf(player.getMainHandItem());
        return s != GuiSkin.VANILLA ? s : skinOf(player.getOffhandItem());
    }

    private static GuiSkin skinOf(ItemStack stack) {
        if (stack.isEmpty())
            return GuiSkin.VANILLA;
        for (int i = 0; i < SKIN_TAGS.length; i++)
            if (stack.is(SKIN_TAGS[i]))
                return GuiSkin.values()[i];
        return GuiSkin.VANILLA;
    }

    // ---- State, set from AimPayloads ----

    // Reading card
    private static byte target = -1;
    private static BlockPos pos = BlockPos.ZERO;
    private static int entity;
    private static Component title = Component.empty();
    private static List<Component> lines = List.of();
    private static int quantity = -1;
    private static float value, lo, hi;
    private static String range = "";
    private static GuiSkin readSkin = GuiSkin.VANILLA;
    private static long readBorn, readUntil, lostAt;
    private static int readW;

    public static void reading(byte kind, BlockPos at, int id, Component name, List<Component> text, int q, float v, int freeTicks) {
        Font font = Minecraft.getInstance().font;
        target = kind;
        pos = at;
        entity = id;
        title = name;
        lines = text;
        quantity = q;
        value = v;
        readSkin = skinInHand();
        long now = Util.getMillis();
        readBorn = now;
        readUntil = kind == 0 ? now + freeTicks * 50L : Long.MAX_VALUE;
        lostAt = 0;
        int w = font.width(title);
        for (Component c : lines)
            w = Math.max(w, font.width(c));
        if (q >= 0) {
            setRange(Quantity.values()[q], v);
            w = Math.max(w, font.width(range));
            w += 2 * DIAL_R + 4 + 5;
        }
        readW = w;
    }

    /** The dial's span. A fraction reads 0 to 1 and a tilt -90 to 90; anything else autoranges like a meter, to 1, 2 or 5 of a power of ten. */
    private static void setRange(Quantity q, float v) {
        switch (q) {
            case CONDITION, CHARGE, MOISTURE, FLUX, RESPONSE, CONTROL, PATHOGEN -> {
                lo = 0;
                hi = 1;
                range = "0-100%";
                return;
            }
            case ATTITUDE -> {
                lo = -90;
                hi = 90;
            }
            default -> {
                float top = nice(Math.abs(v));
                lo = v < 0 ? -top : 0;
                hi = top;
            }
        }
        range = Instruments.sig(lo, 3) + "-" + Instruments.sig(hi, 3);
    }

    private static float nice(float x) {
        if (!(x > 0))
            return 1;
        double p = Math.pow(10, Math.floor(Math.log10(x)));
        for (int m : new int[] {1, 2, 5, 10})
            if (m * p >= x)
                return (float) (m * p);
        return (float) (10 * p);
    }

    // Needle
    private static boolean needlePoint;
    private static float needleYaw, needleShown = Float.NaN;
    private static int needleStep;
    private static Component needleCaption = Component.empty();
    private static GuiSkin needleSkin = GuiSkin.VANILLA;
    private static long needleUntil, needleFrame;

    public static void bearing(boolean point, float yaw, int step, Component caption, int ticks) {
        if (!point || needleUntil < Util.getMillis())
            needleShown = Float.NaN; // a fresh needle starts where it points, not swinging in from the last one
        needlePoint = point;
        needleYaw = yaw;
        needleStep = step;
        needleCaption = caption;
        needleSkin = skinInHand();
        needleUntil = Util.getMillis() + ticks * 50L;
    }

    // Mode strip
    private static Component modeTitle = Component.empty(), modeDetail = Component.empty();
    private static List<Component> modeOptions = List.of();
    private static int modeIndex, modeFirst, modeLast, modeW;
    private static GuiSkin modeSkin = GuiSkin.VANILLA;
    private static long modeUntil;
    private static final int MODE_TICKS = 50, MODE_WINDOW = 5;

    public static void modes(Component name, List<Component> options, int index, Component detail) {
        Font font = Minecraft.getInstance().font;
        modeTitle = name;
        modeOptions = options;
        modeIndex = Mth.clamp(index, 0, Math.max(0, options.size() - 1));
        modeDetail = detail;
        modeSkin = skinInHand();
        modeUntil = Util.getMillis() + MODE_TICKS * 50L;
        // A long list shows a window round the current option.
        modeFirst = Mth.clamp(modeIndex - MODE_WINDOW / 2, 0, Math.max(0, options.size() - MODE_WINDOW));
        modeLast = Math.min(options.size(), modeFirst + MODE_WINDOW);
        int w = font.width(modeTitle) + 6;
        for (int i = modeFirst; i < modeLast; i++)
            w += font.width(options.get(i)) + 8;
        if (modeFirst > 0)
            w += font.width("..") + 4;
        if (modeLast < options.size())
            w += font.width("..") + 4;
        modeW = Math.max(w, font.width(modeDetail));
    }

    // Link chip
    @Nullable
    private static Component linkText;
    @Nullable
    private static BlockPos linkPos;
    @Nullable
    private static Item linkItem;
    private static GuiSkin linkSkin = GuiSkin.VANILLA;

    public static void link(@Nullable Component from, @Nullable BlockPos at) {
        var player = Minecraft.getInstance().player;
        linkText = from == null ? null : Component.translatable("hud.feedback.link_from", from);
        linkPos = from == null ? null : at;
        linkItem = from == null || player == null ? null : player.getMainHandItem().getItem();
        linkSkin = skinInHand();
    }

    /** The link is shown while the tool that started it is in hand (the server keeps it either way). */
    private static boolean linking(Minecraft mc) {
        return linkText != null && mc.player != null && mc.player.getMainHandItem().getItem() == linkItem;
    }

    // Crosshair cue
    private static Component cueText = Component.empty();
    private static long cueBorn, cueUntil;

    public static void cue(Component text, int ticks) {
        cueText = text;
        cueBorn = Util.getMillis();
        cueUntil = cueBorn + ticks * 50L;
    }

    // ---- Drawing ----

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui)
            return;
        long now = Util.getMillis();
        float partial = delta.getGameTimeDeltaPartialTick(true);
        float s = ClientSettings.aimScale();
        g.pose().pushPose();
        g.pose().translate(g.guiWidth() / 2f, g.guiHeight() / 2f, 0);
        g.pose().scale(s, s, 1);
        RenderSystem.enableBlend();
        int below = 20; // the parry bar sits just under the crosshair; everything else stacks below it
        drawReading(g, mc, now, partial);
        below = drawNeedle(g, mc, now, below);
        below = drawCue(g, mc, now, below);
        drawModes(g, mc, now, below);
        drawLink(g, mc);
        g.setColor(1, 1, 1, 1);
        g.pose().popPose();
    }

    private static void card(GuiGraphics g, GuiSkin skin, int x0, int y0, int x1, int y1) {
        if (skin == GuiSkin.VANILLA)
            GuiStyle.hudCard(g, x0, y0, x1, y1);
        else
            skin.plaque(g, x0, y0, x1 - x0, y1 - y0);
    }

    private static float fade(long now, long until) {
        return Mth.clamp((until - now) / (float) FADE_MS, 0, 1);
    }

    private static boolean onTarget(Minecraft mc, float partial) {
        if (target == 2) {
            Entity e = mc.crosshairPickEntity;
            return e != null && e.getId() == entity;
        }
        if (target != 1)
            return false;
        if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos))
            return true;
        // A reading taken from afar (a pyrometer) holds past the hand's reach: look along the eye that far.
        double d = mc.player.getEyePosition(partial).distanceTo(Vec3.atCenterOf(pos));
        if (d <= mc.player.blockInteractionRange() || d > 96)
            return false;
        return mc.player.pick(d + 1.5, partial, false) instanceof BlockHitResult far
                && far.getType() == HitResult.Type.BLOCK && far.getBlockPos().equals(pos);
    }

    private static void drawReading(GuiGraphics g, Minecraft mc, long now, float partial) {
        if (target < 0)
            return;
        float a;
        if (target == 0) {
            a = fade(now, readUntil);
        } else if (onTarget(mc, partial)) {
            lostAt = 0;
            a = 1;
        } else {
            if (lostAt == 0)
                lostAt = now;
            a = fade(now, lostAt + FADE_MS);
        }
        if (a <= 0) {
            target = -1;
            return;
        }
        Font font = mc.font;
        GuiSkin skin = readSkin;
        boolean dial = quantity >= 0;
        int textH = 10 * (1 + lines.size()) + (dial ? 10 : 0);
        int h = Math.max(textH, dial ? DIAL_R + 6 : 0) + 8;
        // Slides in from the right like Create's goggle card, over a fifth of a second.
        int slide = Math.round((float) Math.pow(1 - Math.min(1f, (now - readBorn) / 200f), 3) * 8);
        int x0 = GAP + slide, y0 = -h / 2, x1 = x0 + readW + 10;
        g.setColor(1, 1, 1, a);
        card(g, skin, x0, y0, x1, y0 + h);
        int tx = x0 + 5;
        if (dial) {
            drawDial(g, skin, x0 + 5, y0 + 5);
            tx += 2 * DIAL_R + 4 + 5;
        }
        int ty = y0 + 5;
        g.drawString(font, title, tx, ty, skin.dim(), false);
        for (Component c : lines)
            g.drawString(font, c, tx, ty += 10, skin.ink(), false);
        if (dial)
            g.drawString(font, range, tx, ty + 10, skin.dim(), false);
    }

    /** A half-round meter face: graduations along the arc, the needle at the figure, pegged in the warning colour past full scale. */
    private static void drawDial(GuiGraphics g, GuiSkin skin, int x, int y) {
        int w = 2 * DIAL_R + 4, h = DIAL_R + 6;
        skin.inset(g, x, y, w, h);
        int cx = x + w / 2, cy = y + h - 3;
        for (int i = 0; i <= 8; i++) {
            double t = Math.PI * (1 - i / 8.0);
            int in = i % 4 == 0 ? DIAL_R - 5 : DIAL_R - 3;
            GuiStyle.line(g, cx + (int) Math.round(Math.cos(t) * in), cy - (int) Math.round(Math.sin(t) * in),
                    cx + (int) Math.round(Math.cos(t) * (DIAL_R - 1)), cy - (int) Math.round(Math.sin(t) * (DIAL_R - 1)), skin.dim());
        }
        float f = (value - lo) / (hi - lo);
        boolean pegged = f > 1.001f || f < -0.001f;
        double t = Math.PI * (1 - Mth.clamp(f, 0, 1));
        int colour = pegged ? skin.warn() : skin.accent();
        GuiStyle.line(g, cx, cy, cx + (int) Math.round(Math.cos(t) * (DIAL_R - 2)), cy - (int) Math.round(Math.sin(t) * (DIAL_R - 2)), colour);
        g.fill(cx - 1, cy - 1, cx + 1, cy + 1, skin.ink());
    }

    private static int drawNeedle(GuiGraphics g, Minecraft mc, long now, int below) {
        float a = fade(now, needleUntil);
        if (a <= 0)
            return below;
        g.setColor(1, 1, 1, a);
        GuiSkin skin = needleSkin;
        if (needlePoint) {
            float rel = Mth.wrapDegrees(needleYaw - mc.player.getYRot());
            if (needleStep > 0) // the needle's resolution: it says which way to lean, no finer
                rel = Math.round(rel / needleStep) * needleStep;
            float dt = Math.min(0.1f, (now - needleFrame) / 1000f);
            needleShown = Float.isNaN(needleShown) ? rel : needleShown + Mth.wrapDegrees(rel - needleShown) * Math.min(1, dt * 12);
            needleFrame = now;
            double r = Math.toRadians(needleShown);
            float sx = (float) Math.sin(r), sy = (float) -Math.cos(r);
            int ix = Math.round(sx * 9), iy = Math.round(sy * 9), ox = Math.round(sx * 16), oy = Math.round(sy * 16);
            GuiStyle.line(g, ix, iy, ox, oy, skin.accent());
            GuiStyle.line(g, ix + (Math.abs(sy) > Math.abs(sx) ? 1 : 0), iy + (Math.abs(sy) > Math.abs(sx) ? 0 : 1),
                    ox + (Math.abs(sy) > Math.abs(sx) ? 1 : 0), oy + (Math.abs(sy) > Math.abs(sx) ? 0 : 1), skin.accent());
            g.fill(ox - 1, oy - 1, ox + 2, oy + 2, skin.ink());
        }
        if (!needleCaption.getString().isEmpty())
            below = caption(g, mc.font, skin, needleCaption, below);
        return below;
    }

    private static int drawCue(GuiGraphics g, Minecraft mc, long now, int below) {
        float a = fade(now, cueUntil);
        if (a <= 0)
            return below;
        g.setColor(1, 1, 1, a);
        return caption(g, mc.font, GuiSkin.VANILLA, cueText, below);
    }

    /** A one-line card centred under the crosshair at {@code y}; returns where the next one goes. */
    private static int caption(GuiGraphics g, Font font, GuiSkin skin, Component text, int y) {
        int w = font.width(text);
        card(g, skin, -w / 2 - 5, y, w / 2 + 5, y + 14);
        g.drawString(font, text, -w / 2, y + 3, skin.ink(), false);
        return y + 16;
    }

    private static void drawModes(GuiGraphics g, Minecraft mc, long now, int y) {
        float a = fade(now, modeUntil);
        if (a <= 0 || modeOptions.isEmpty())
            return;
        g.setColor(1, 1, 1, a);
        Font font = mc.font;
        GuiSkin skin = modeSkin;
        boolean detail = !modeDetail.getString().isEmpty();
        int x0 = -modeW / 2 - 5, h = detail ? 26 : 16;
        card(g, skin, x0, y, modeW / 2 + 5, y + h);
        int x = x0 + 5;
        g.drawString(font, modeTitle, x, y + 4, skin.dim(), false);
        x += font.width(modeTitle) + 6;
        if (modeFirst > 0) {
            g.drawString(font, "..", x, y + 4, skin.dim(), false);
            x += font.width("..") + 4;
        }
        for (int i = modeFirst; i < modeLast; i++) {
            Component o = modeOptions.get(i);
            int w = font.width(o);
            if (i == modeIndex)
                skin.row(g, x - 2, y + 2, w + 4, 11, true, false);
            g.drawString(font, o, x, y + 4, i == modeIndex ? skin.accent() : skin.dim(), false);
            x += w + 8;
        }
        if (modeLast < modeOptions.size())
            g.drawString(font, "..", x, y + 4, skin.dim(), false);
        if (detail)
            g.drawString(font, modeDetail, -font.width(modeDetail) / 2, y + 15, skin.ink(), false);
    }

    private static void drawLink(GuiGraphics g, Minecraft mc) {
        if (!linking(mc))
            return;
        g.setColor(1, 1, 1, 1);
        Font font = mc.font;
        int w = font.width(linkText);
        card(g, linkSkin, -GAP - w - 10, -7, -GAP, 7);
        g.drawString(font, linkText, -GAP - w - 5, -4, linkSkin.ink(), false);
    }

    /** Queues the dashed line from the link's first end to the aim, for {@link DataLinkFrame} to draw this frame. */
    public static void queueLinkLine(Minecraft mc, float partial) {
        BlockPos from = linkPos;
        if (from == null || !linking(mc))
            return;
        Vec3 a = Vec3.atCenterOf(from);
        if (a.distanceTo(mc.player.getEyePosition(partial)) > 64) // a block on a moving body sits in its plot, far off: no line
            return;
        Vec3 b =mc.hitResult != null && mc.hitResult.getType() != HitResult.Type.MISS ? mc.hitResult.getLocation()
                : mc.player.getEyePosition(partial).add(mc.player.getViewVector(partial).scale(3));
        DeferredDataRenderer renderer = DeferredDataRenderer.get();
        renderer.deferDraw((pose, consumer) -> {
            double len = a.distanceTo(b);
            int dashes = (int) Math.min(256, Math.ceil(len / 0.5));
            Vec3 step = b.subtract(a).scale(1.0 / Math.max(1, dashes));
            for (int i = 0; i < dashes; i++) {
                Vec3 p = a.add(step.scale(i));
                DataNodeRenderer.drawLine(renderer, pose, consumer, p, p.add(step.scale(0.6)), 0xFFFFD27A);
            }
        });
    }
}
