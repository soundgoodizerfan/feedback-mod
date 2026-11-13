// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.soundgoodizerfan.feedback.machine.creative.CreativeSource;
import io.github.soundgoodizerfan.feedback.net.CreativeSourcePayload;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.client.gui.Opening;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A creative source's settings: a power switch and a text box per figure (any number; the
 * server clamps it to the figure's range). An instrument panel ({@link GuiSkin#CONSOLE}), like {@link MirrorScreen}. Every figure is
 * read from the block's {@link CreativeSource#params()}, so one screen serves heat, rotation and
 * electricity, and the next energy type's source needs no screen of its own.
 */
public class CreativeSourceScreen extends Screen {

    private static final int W = 190, ROW = 20;
    private static final GuiSkin SKIN = GuiSkin.CONSOLE;

    private final BlockPos pos;
    private final Opening opening = new Opening(SKIN);
    private final List<CreativeSource.Param> params;
    private final float[] values;
    private boolean on;

    private CreativeSourceScreen(BlockPos pos, CreativeSource source, Component title) {
        super(title);
        this.pos = pos;
        this.params = source.params();
        this.on = source.on();
        this.values = new float[params.size()];
        for (int i = 0; i < values.length; i++)
            values[i] = params.get(i).value();
    }

    public static void open(BlockPos pos) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getBlockEntity(pos) instanceof CreativeSource source)
            Minecraft.getInstance().setScreen(new CreativeSourceScreen(pos, source, level.getBlockState(pos).getBlock().getName()));
    }

    private int h() {
        return 70 + params.size() * ROW;
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - h()) / 2;
    }

    @Override
    protected void init() {
        opening.start();
        int x = left() + 12, y = top();
        addRenderableWidget(GuiStyle.toggle(SKIN, Component.translatable("screen.feedback.creative.on"),
                () -> on, b -> {
                    on = !on;
                    send();
                }, x, y + 24, W - 24));
        for (int i = 0; i < params.size(); i++) {
            int n = i, ry = y + 48 + i * ROW;
            EditBox box = new EditBox(font, x + W - 24 - 78, ry, 92, 14, Component.empty());
            box.setFilter(t -> t.matches("-?\\d*\\.?\\d*"));
            box.setValue(shown(n));
            box.setResponder(t -> set(n, t));
            addRenderableWidget(box);
        }
        addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.done"), b -> onClose(),
                left() + W / 2 - 30, y + h() - 22, 60, 14));
    }

    private String shown(int i) {
        return values[i] == Math.rint(values[i]) ? Integer.toString((int) values[i])
                : String.format(Locale.ROOT, "%.1f", values[i]);
    }

    private void set(int i, String text) {
        try {
            CreativeSource.Param p = params.get(i);
            values[i] = CreativeSource.clamp(p, Float.parseFloat(text));
            send();
        } catch (NumberFormatException ignored) {
            // half-typed ("-", "."): keep the last figure
        }
    }

    private void send() {
        List<Float> list = new ArrayList<>();
        for (float v : values)
            list.add(v);
        PacketDistributor.sendToServer(new CreativeSourcePayload(pos, on, list));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (opening.shut()) {
            super.onClose();
            return;
        }
        int x = left(), y = top();
        opening.render(g, width, height, x, y, W, h(), () -> {
            SKIN.panel(g, x, y, W, h());
            Component t = Component.literal(title.getString().toUpperCase(Locale.ROOT));
            g.drawString(font, t, x + (W - font.width(t)) / 2, y + 10, SKIN.ink(), false);
            for (int i = 0; i < params.size(); i++) {
                int ry = y + 48 + i * ROW;
                g.drawString(font, Component.translatable("screen.feedback.creative." + params.get(i).key()),
                        x + 12, ry + 3, SKIN.dim(), false);
            }
            super.render(g, mouseX, mouseY, partialTick);
        });
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void onClose() {
        opening.close();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return !opening.settled() || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        return opening.closing() || super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
