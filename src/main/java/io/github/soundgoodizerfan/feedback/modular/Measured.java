// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;

import io.github.soundgoodizerfan.feedback.modular.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * What an instrument read off a made item and put on it: the exact figure, as it was then (spec 5). The figures themselves are baked and
 * re-derived on wear, so a stamp is a <em>past reading</em> and is kept as it was; the tooltip prints a stamped figure exact and every
 * other figure as a range ({@link ModularFit#spread}). An instrument stamps the item in the other hand: the balance scale its mass and
 * balance, the calipers its axes and fit, the draw gauge its draw and launch figures.
 */
public final class Measured {

    private Measured() {
    }

    public static final Codec<Map<String, Float>> CODEC = Codec.unboundedMap(Codec.STRING, Codec.FLOAT);
    public static final StreamCodec<RegistryFriendlyByteBuf, Map<String, Float>> STREAM_CODEC =
            ByteBufCodecs.<io.netty.buffer.ByteBuf, String, Float, Map<String, Float>>map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.FLOAT)
                    .mapStream(buf -> buf);

    /** Which instrument reads. */
    public enum Kind { BALANCE, CALIPERS, DRAW }

    public static Map<String, Float> of(ItemStack stack) {
        return stack.getOrDefault(FModular.MEASURED.get(), Map.of());
    }

    public static boolean has(ItemStack stack, String key) {
        return of(stack).containsKey(key);
    }

    /** The figures {@code kind} reads off {@code item}, by key; empty when it has none of them. */
    public static Map<String, Float> read(Kind kind, ItemStack item) {
        Map<String, Float> out = new HashMap<>();
        ModularFigures f = ModularStacks.figures(item);
        ModularDesign design = ModularStacks.design(item);
        if (f == null || design == null)
            return out;
        switch (kind) {
            case BALANCE -> {
                out.put("mass", f.mass().value());
                if (f.get(Fig.BALANCE) > 0f)
                    out.put("balance", f.get(Fig.BALANCE));
            }
            case DRAW -> {
                for (Fig fig : new Fig[]{Fig.DRAW, Fig.VELOCITY, Fig.SPRING_ENERGY})
                    if (f.get(fig) > 0f)
                        out.put(fig.name().toLowerCase(java.util.Locale.ROOT), f.get(fig));
            }
            case CALIPERS -> {
                ModularType type = ModularTypes.known().get(design.type());
                for (Part p : design.parts()) {
                    ModularType.Role role = type == null ? null : type.roles().get(p.role());
                    if (role == null)
                        continue;
                    p.measures().axes().forEach((name, v) -> out.put("axis." + role.name() + "." + name, v));
                }
                ModularFit.Fit fit = ModularFit.of(item);
                if (fit != null)
                    out.put("fit", fit.fit());
            }
        }
        return out;
    }

    /** The instrument in {@code hand} reads the item in the other hand and stamps it; the reading goes to the player's HUD. */
    public static InteractionResultHolder<ItemStack> apply(Kind kind, Level level, Player player, InteractionHand hand) {
        ItemStack tool = player.getItemInHand(hand);
        ItemStack item = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (level.isClientSide)
            return InteractionResultHolder.pass(tool);
        Map<String, Float> read = read(kind, item);
        if (read.isEmpty()) {
            if (player instanceof net.minecraft.server.level.ServerPlayer sp)
                io.github.soundgoodizerfan.feedback.net.HudReadoutPayload.send(sp, Component.translatable("message.feedback.measure.nothing"), 50);
            return InteractionResultHolder.pass(tool);
        }
        Map<String, Float> stamped = new HashMap<>(of(item));
        stamped.putAll(read);
        item.set(FModular.MEASURED.get(), stamped);
        level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.6f);
        if (player instanceof net.minecraft.server.level.ServerPlayer sp)
            io.github.soundgoodizerfan.feedback.net.HudReadoutPayload.send(sp, Component.translatable("message.feedback.measure.stamped", read.size()), 50);
        return InteractionResultHolder.success(tool);
    }
}
