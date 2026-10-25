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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.item.MassProbeItem;
import io.github.soundgoodizerfan.feedback.modular.ForgeRhythm;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Plating;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * What the instruments the player carries can read off the stack in each hand, top-left: heat,
 * work, a barrel's bore, a coat's thickness, a probe's deposit, charge, wear, a modular item's mass
 * and hardness, a circuit's response. Each line comes from the helper the
 * tooltip itself uses (so they never disagree, and both quantise through {@code Readout}) and exists
 * only if a carried instrument covers its quantity. A hand with nothing readable gets no card.
 * Toggled by a key, on by default.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class ItemInfoHud {

    public static final KeyMapping TOGGLE = new KeyMapping("key.feedback.item_info_hud", InputConstants.KEY_H, "key.categories.feedback");

    private static final GuiSkin SKIN = GuiSkin.VANILLA;
    private static final int REFRESH_TICKS = 5, MAX_LINES = 24;

    private static boolean on = true;
    /** Lines per hand (main, off), rebuilt on stack change or every {@link #REFRESH_TICKS}; the tick sets them, the frame only draws. */
    @SuppressWarnings("unchecked")
    private static final List<String>[] LINES = new List[] { List.of(), List.of() };
    private static final ItemStack[] SHOWN = { ItemStack.EMPTY, ItemStack.EMPTY };
    private static long builtAt = Long.MIN_VALUE / 2;

    private ItemInfoHud() {
    }

    static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Feedback.id("item_info"), ItemInfoHud::render);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        while (TOGGLE.consumeClick())
            if (player != null && mc.screen == null)
                on = !on;
        if (!on || player == null || mc.level == null || mc.screen != null) {
            LINES[0] = LINES[1] = List.of();
            SHOWN[0] = SHOWN[1] = ItemStack.EMPTY;
            return;
        }
        long now = mc.level.getGameTime();
        boolean stale = now - builtAt >= REFRESH_TICKS;
        for (int i = 0; i < 2; i++) {
            ItemStack held = player.getItemInHand(i == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
            if (held.isEmpty()) {
                LINES[i] = List.of();
                SHOWN[i] = ItemStack.EMPTY;
            } else if (stale || !ItemStack.isSameItemSameComponents(held, SHOWN[i])) {
                SHOWN[i] = held.copy();
                LINES[i] = lines(held, player);
            }
        }
        if (stale)
            builtAt = now;
    }

    /**
     * Only what a carried instrument reads off {@code stack}: each reading is produced by the same
     * helper the tooltip uses and is absent when no instrument covers its quantity.
     */
    private static List<String> lines(ItemStack stack, Player player) {
        List<Component> list = new ArrayList<>();
        if (stack.has(FDataComponents.TEMPERATURE.get()) && Readout.instrumented(Quantity.TEMPERATURE)) {
            Tu tu = ItemHeat.get(stack, player.level());
            if (tu.value() > FTuning.WARM_TU.value())
                list.add(Readout.temperatureReading(tu));
        }
        // Each helper below returns null when no carried instrument covers its quantity.
        list.add(WorkpieceTooltip.work(stack));
        list.add(WorkpieceTooltip.barrel(stack));
        Plating plating = stack.get(FDataComponents.PLATING.get());
        list.add(PlatingTooltip.thickness(plating));
        list.add(PlatingTooltip.deposit(stack, plating));
        Cartridge round = stack.get(FWeapons.CARTRIDGE.get());
        if (round != null && round.charged() && Readout.instrumented(Quantity.VOLTAGE))
            list.add(Readout.chargeReading(round.at(player.level().getGameTime()).volts()));
        if (stack.getItem() instanceof io.github.soundgoodizerfan.feedback.item.GeigerCounterItem && Readout.instrumented(Quantity.RADIATION))
            list.add(Readout.reading(Quantity.RADIATION, "feedback.readout.radiation",
                    Readout.figure(Quantity.RADIATION, GeigerClient.reading())));
        if (stack.getItem() instanceof io.github.soundgoodizerfan.feedback.item.DosimeterItem && Readout.instrumented(Quantity.DOSE))
            list.add(Readout.reading(Quantity.DOSE, "feedback.readout.dose", Readout.figure(Quantity.DOSE,
                    player.getData(io.github.soundgoodizerfan.feedback.registry.FCombat.RADIATION_DOSE))));
        float baton = stack.getOrDefault(FDataComponents.BATON_VOLTS.get(), 0f);
        if (baton > 0f && Readout.instrumented(Quantity.VOLTAGE))
            list.add(Readout.reading(Quantity.VOLTAGE, "feedback.readout.volts", Readout.figure(Quantity.VOLTAGE, baton)));
        // Wear on a head, bit or probe crystal: a figure only at the truth tier (calipers read machines, not stock).
        Float wear = stack.has(FDataComponents.PROBE_CONDITION.get()) ? Float.valueOf(MassProbeItem.condition(stack))
                : stack.get(FDataComponents.TOOL_CONDITION.get());
        if (wear != null && Readout.exact(Quantity.CONDITION))
            list.add(Readout.conditionReading(wear));
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        if (made != null && made.dims().containsKey(ForgeRhythm.WORK) && Readout.exact(Quantity.FIGURE))
            list.add(Readout.reading(Quantity.FIGURE, "tooltip.feedback.part.worked",
                    Readout.figure(Quantity.FIGURE, made.dims().get(ForgeRhythm.WORK) * 100f)));
        list.addAll(FoodTooltip.readings(stack, player.level()));
        ModularFigures figures = ModularStacks.figures(stack);
        if (figures != null)
            list.addAll(ModularReadings.readings(stack, figures));
        var circuit = stack.get(FDataComponents.CIRCUIT.get());
        Instrument tester = circuit == null ? null : Instruments.best(player, Quantity.RESPONSE);
        if (tester != null)
            circuit.test(tester, line -> list.add(Instruments.signed(tester, line)));
        List<String> out = new ArrayList<>();
        int max = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 3;
        for (Component c : list)
            if (c != null)
                for (var part : Minecraft.getInstance().font.split(c, max))
                    if (out.size() < MAX_LINES) {
                        var sb = new StringBuilder();
                        part.accept((i, style, cp) -> !sb.appendCodePoint(cp).isEmpty());
                        out.add(sb.toString());
                    }
        return out;
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!on || mc.player == null || mc.screen != null || mc.options.hideGui)
            return;
        Font font = mc.font;
        int y = 6, max = g.guiWidth() / 3;
        for (int i = 0; i < 2; i++) {
            List<String> lines = LINES[i];
            if (lines.isEmpty())
                continue;
            int w = 0;
            for (String s : lines)
                w = Math.max(w, Math.min(max, font.width(s)));
            GuiStyle.hudCard(g, 5, y, 5 + w + 14, y + 10 * lines.size() + 8);
            int ty = y + 4;
            for (String s : lines) {
                g.drawString(font, font.width(s) > max ? font.plainSubstrByWidth(s, max) : s, 11, ty, SKIN.ink(), false);
                ty += 10;
            }
            y += 10 * lines.size() + 12;
        }
    }
}
