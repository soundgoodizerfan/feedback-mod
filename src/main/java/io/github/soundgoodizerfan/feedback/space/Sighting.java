// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import io.github.soundgoodizerfan.feedback.core.Awards;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.item.ModularTelescope;
import io.github.soundgoodizerfan.feedback.modular.item.TelescopeFocus;
import io.github.soundgoodizerfan.feedback.modular.treatise.School;
import io.github.soundgoodizerfan.feedback.modular.treatise.Treatises;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Seeing a planet's disc: the first study of space science. A person looking through a telescope sharp and strong enough, at night in a
 * clear sky they can see, with a planet in the middle of the view, has resolved it. The client tests the same rule to draw the disc and
 * to say so; the server tests it again before it rewards anything.
 */
public final class Sighting {

    /** The event of the advancement a first sighting earns ({@code progress/space/first_sighting}). */
    public static final String EVENT = "progress/space/first_sighting";

    private Sighting() {
    }

    /** The telescope {@code player} is looking through now, or empty. */
    public static ItemStack scope(Player player) {
        if (!player.isUsingItem())
            return ItemStack.EMPTY;
        ItemStack stack = player.getUseItem();
        return stack.getItem() instanceof ModularTelescope && !ModularStacks.broken(stack) ? stack : ItemStack.EMPTY;
    }

    /** Whether the sky over {@code player} lets planets be seen: the Overworld, night, no rain or thunder, open sky above the eyes. */
    public static boolean skyAllows(Player player) {
        Level level = player.level();
        return level.dimension().equals(Level.OVERWORLD) && !level.isDay() && !level.isRaining() && !level.isThundering()
                && level.canSeeSky(player.blockPosition().above());
    }

    /** Whether {@code scope} resolves {@code body}: enough magnification, {@code blur} (from {@link TelescopeFocus#blur}) under {@link SkyBodies#MAX_BLUR}. */
    public static boolean sharp(ItemStack scope, SkyBodies.Body body, float blur) {
        ModularFigures f = scope.isEmpty() ? null : ModularStacks.figures(scope);
        return f != null && f.get(Fig.MAGNIFICATION) >= body.minMagnification() && blur <= SkyBodies.MAX_BLUR;
    }

    /** The body {@code player} resolves now, or null: {@code slack} widens the aim (1 on the client, {@link SkyBodies#SERVER_SLACK} on the server). */
    public static SkyBodies.Body resolved(Player player, double slack, float partial) {
        ItemStack scope = scope(player);
        if (scope.isEmpty() || !skyAllows(player))
            return null;
        ModularFigures f = ModularStacks.figures(scope);
        float magnification = f == null ? 1f : f.get(Fig.MAGNIFICATION);
        float blur = TelescopeFocus.blur(scope, player.level());
        Vec3 look = player.getViewVector(partial);
        for (SkyBodies.Body body : SkyBodies.ALL) {
            Vec3 at = SkyBodies.direction(player.level(), body, partial);
            if (SkyBodies.up(at) && sharp(scope, body, blur) && SkyBodies.offBy(look, at) <= SkyBodies.aim(magnification) * slack)
                return body;
        }
        return null;
    }

    /**
     * Server: the client says it sighted {@code id}; check it. The first sighting of anything gives the study (the advancement, Aerospace
     * Treatise IV and a sky notebook); every planet not yet resolved is written into the person's notes (what the notebook lists).
     */
    public static void claim(ServerPlayer player, String id) {
        SkyBodies.Body body = SkyBodies.byId(id);
        if (body == null || resolved(player, SkyBodies.SERVER_SLACK, 1f) != body)
            return;
        AdvancementHolder holder = player.server.getAdvancements().get(io.github.soundgoodizerfan.feedback.Feedback.id(EVENT));
        if (holder == null)
            return;
        java.util.List<String> seen = player.getData(io.github.soundgoodizerfan.feedback.registry.FCombat.SIGHTED);
        boolean fresh = !seen.contains(id);
        boolean first = !player.getAdvancements().getOrStartProgress(holder).isDone();
        if (!fresh && !first)
            return;
        if (fresh) {
            java.util.List<String> next = new java.util.ArrayList<>(seen);
            next.add(id);
            player.setData(io.github.soundgoodizerfan.feedback.registry.FCombat.SIGHTED, java.util.List.copyOf(next));
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1f);
        if (first) {
            player.getInventory().placeItemBackInInventory(Treatises.of(School.AEROSPACE, 4));
            player.getInventory().placeItemBackInInventory(new ItemStack(io.github.soundgoodizerfan.feedback.registry.FItems.SKY_NOTEBOOK.get()));
            Awards.give(player, EVENT);
        } else {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.feedback.sighting.noted",
                    net.minecraft.network.chat.Component.translatable("planet.feedback." + id)), true);
        }
    }
}
