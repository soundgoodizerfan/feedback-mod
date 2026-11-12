// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.soundgoodizerfan.feedback.modular.ModularTypes;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * An order in words, built from translation keys the client resolves, so the server can word an order (for the ledger board) without a client
 * class. Bands and names only: never the figures behind them.
 */
public final class OrderWords {

    private OrderWords() {
    }

    public static Component typeName(ResourceLocation type) {
        return new ItemStack(BuiltInRegistries.ITEM.get(type)).getHoverName();
    }

    /** One term: {@code noun: word}, a must worded as at least or at most. */
    public static Component term(Order.Term t) {
        String fig = t.fig().name().toLowerCase(Locale.ROOT);
        Component word = Component.translatable("village.feedback.term." + fig + "." + t.band());
        return Component.translatable("village.feedback.line", Component.translatable("village.feedback.noun." + fig),
                t.must() ? Component.translatable("village.feedback.must." + (t.above() ? "above" : "below"), word) : word);
    }

    /** "blade: bronze" or "blade: any metal": the part by the type's own name, then the material or its class. */
    public static Component ask(ResourceLocation type, Order.Ask a) {
        var t = ModularTypes.known(type);
        var role = t == null ? null : t.roles().get(a.role());
        Component what = a.material() != null
                ? Component.translatable("material." + a.material().getNamespace() + "." + a.material().getPath())
                : Component.translatable("village.feedback.ask.class",
                        Component.translatable("feedback.jei.kind." + a.kind().name().toLowerCase(Locale.ROOT)).getString().toLowerCase(Locale.ROOT));
        return Component.translatable("village.feedback.ask", role == null ? a.role() : role.name(), what);
    }

    /** What an order asks, as lines: the parts of a tool order, or the stated terms of a dish. */
    public static List<Component> asked(Order o) {
        List<Component> out = new ArrayList<>();
        for (Order.Ask a : o.asks())
            out.add(ask(o.type(), a));
        for (Order.Term t : o.terms())
            out.add(term(t));
        return out;
    }

    /** The asks joined on one line, for a list row. */
    public static Component oneLine(Order o) {
        net.minecraft.network.chat.MutableComponent line = Component.empty();
        boolean first = true;
        for (Component c : asked(o)) {
            if (!first)
                line.append(Component.literal(", "));
            line.append(c);
            first = false;
        }
        return line;
    }
}
