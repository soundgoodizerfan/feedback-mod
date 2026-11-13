// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;

import java.util.ArrayList;
import java.util.List;


import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * The Converter app: the mod's own units against each other. Metal is counted in Qu (one Qu is a nugget, nine an ingot, eighty-one a
 * block; a Qu of molten metal is 16 mB), volume in mB (1 mB is a litre, a bucket 1000), time in ticks (20 to the second). Type an amount,
 * pick what it is in, read it in the rest.
 */
final class ConverterWindow extends DesktopWindow {

    private record Unit(String key, double base) {
    }

    private record Group(String key, List<Unit> units) {
    }

    private static final List<Group> GROUPS = List.of(
            new Group("metal", List.of(new Unit("block", 81), new Unit("ingot", 9), new Unit("nugget", 1), new Unit("molten", 1 / 16.0))),
            new Group("volume", List.of(new Unit("bucket", 1000), new Unit("ingot_melt", 144), new Unit("mb", 1))),
            new Group("time", List.of(new Unit("day", 24000), new Unit("minute", 1200), new Unit("second", 20), new Unit("tick", 1))));

    private final ComputerContext ctx;
    private EditBox amount;
    private final List<AbstractWidget> unitTabs = new ArrayList<>();
    private int group, unit;

    ConverterWindow(ComputerContext ctx) {
        super(50, 16, 230, 140, 200, 120, false);
        this.ctx = ctx;
    }

    @Override
    public Component title() {
        return Component.translatable("gui.feedback.computer.converter");
    }

    @Override
    public Glyph glyph() {
        return Glyph.CONVERT;
    }

    private static Component name(Group g, Unit u) {
        return Component.translatable("gui.feedback.computer.unit." + g.key() + "." + u.key());
    }

    @Override
    protected void build(GuiSkin skin) {
        int w = contentW();
        add(GuiStyle.tabs(skin, 0, 0, w, 14, GROUPS.stream().map(g -> (Component) Component.translatable("gui.feedback.computer.group." + g.key())).toList(),
                () -> group, i -> {
                    group = i;
                    unit = 0;
                }), 0, 0);
        amount = add(new EditBox(Minecraft.getInstance().font, 0, 0, 80, 14, Component.empty()), 0, 22);
        amount.setValue("1");
        amount.setMaxLength(14);
        unitTabs.clear();
        for (int g = 0; g < GROUPS.size(); g++) {
            final Group grp = GROUPS.get(g);
            final int index = g;
            AbstractWidget tabs = add(GuiStyle.tabs(skin, 0, 0, w - 86, 14, grp.units().stream().map(u -> name(grp, u)).toList(),
                    () -> unit, i -> unit = i), 86, 22);
            unitTabs.add(tabs);
            tabs.visible = index == 0;
        }
    }

    @Override
    protected void render(Ui ui, int mx, int my) {
        GuiSkin skin = ui.skin();
        for (int g = 0; g < unitTabs.size(); g++)
            unitTabs.get(g).visible = g == group;
        Group grp = GROUPS.get(group);
        double value;
        try {
            value = Double.parseDouble(amount.getValue().trim());
        } catch (NumberFormatException e) {
            value = Double.NaN;
        }
        double base = value * grp.units().get(Math.min(unit, grp.units().size() - 1)).base();
        int y = 44;
        for (Unit u : grp.units()) {
            ui.row(2, y, ui.w() - 4, name(grp, u).getString(), Double.isNaN(base) ? "-" : CalcEval.show(base / u.base()), skin.accent());
            y += Ui.LINE + 2;
        }
        ui.wrapped(0, y + 6, ui.w(), Component.translatable("gui.feedback.computer.group." + grp.key() + ".note"), skin.dim());
    }
}
