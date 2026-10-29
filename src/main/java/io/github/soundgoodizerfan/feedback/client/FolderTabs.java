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
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * A strip of folder tabs standing on the top edge of any panel, drawn through the skin ({@link GuiSkin#folderTab}) so it matches on every
 * skin. The selected tab is flush with the panel; the others sit back. It only draws and hit-tests: the selected index is read every frame
 * from {@code selected} (a synced menu value, {@code modular.MenuTabs}) and a click calls {@code select} and plays the skin's STAGE cue; no
 * cover moves over the screen. A screen adopts it with {@link SkinnedScreen#folderTabs}, which also makes room for {@link #HEIGHT} above the
 * panel. A strip of fewer than two tabs is not built.
 */
public final class FolderTabs extends AbstractWidget {

    /** The band the strip needs above the panel's top edge. */
    public static final int HEIGHT = 14;

    /** One tab: its label, an optional icon drawn before it, and whether it can be chosen. */
    public record Def(Component label, ItemStack icon, boolean enabled) {
        public Def(Component label) {
            this(label, ItemStack.EMPTY, true);
        }
    }

    private final GuiSkin skin;
    private final List<Def> tabs;
    private final IntSupplier selected;
    private final IntConsumer select;
    private final int[] edge;
    private final float[] glow;
    private long last;

    /** The strip's left end is at {@code x}; its top at {@code y} (the panel's top edge less {@link #HEIGHT}). */
    public FolderTabs(GuiSkin skin, int x, int y, List<Def> tabs, IntSupplier selected, IntConsumer select) {
        super(x, y, 0, HEIGHT, Component.empty());
        this.skin = skin;
        this.tabs = tabs;
        this.selected = selected;
        this.select = select;
        this.edge = new int[tabs.size() + 1];
        this.glow = new float[tabs.size()];
        var font = Minecraft.getInstance().font;
        edge[0] = 0;
        for (int i = 0; i < tabs.size(); i++)
            edge[i + 1] = edge[i] + font.width(tabs.get(i).label()) + 16 + (tabs.get(i).icon().isEmpty() ? 0 : 18) - (i == 0 ? 0 : 1);
        this.width = edge[tabs.size()];
    }

    private int at(double mouseX) {
        int rel = (int) (mouseX - getX());
        for (int i = 0; i < tabs.size(); i++)
            if (rel < edge[i + 1])
                return i;
        return tabs.size() - 1;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = net.minecraft.Util.getMillis();
        float dt = Math.min(now - last, 100L) / 1000f;
        last = now;
        int over = active && isHovered() ? at(mouseX) : -1, picked = Mth.clamp(selected.getAsInt(), 0, tabs.size() - 1);
        var font = Minecraft.getInstance().font;
        // Back tabs first, the selected one last so its face overlaps its neighbours' edges.
        for (int pass = 0; pass < 2; pass++)
            for (int i = 0; i < tabs.size(); i++) {
                if ((i == picked) != (pass == 1))
                    continue;
                glow[i] += ((i == over && i != picked && tabs.get(i).enabled() ? 1f : 0f) - glow[i]) * Math.min(1f, dt * 12f);
                int x = getX() + edge[i], w = edge[i + 1] - edge[i] + (i == 0 ? 0 : 1);
                skin.folderTab(g, x, getY(), w, HEIGHT, i == picked, glow[i]);
                int tx = x + 8, ty = getY() + (i == picked ? 3 : 5);
                if (!tabs.get(i).icon().isEmpty()) {
                    g.renderItem(tabs.get(i).icon(), tx - 2, ty - 4);
                    tx += 18;
                }
                int colour = !tabs.get(i).enabled() ? skin.dim() & 0xFFFFFF | 0x80000000 : i == picked ? skin.ink() : skin.dim();
                g.drawString(font, tabs.get(i).label(), tx, ty, colour, false);
            }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        int i = at(mouseX);
        if (i != selected.getAsInt() && tabs.get(i).enabled()) {
            skin.cue(GuiSkin.Cue.STAGE);
            select.accept(i);
        }
    }

    @Override
    public void playDownSound(SoundManager manager) {
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, tabs.get(Mth.clamp(selected.getAsInt(), 0, tabs.size() - 1)).label());
    }
}
