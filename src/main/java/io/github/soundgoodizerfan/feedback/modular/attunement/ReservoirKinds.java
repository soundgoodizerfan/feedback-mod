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
package io.github.soundgoodizerfan.feedback.modular.attunement;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.xp.XpStore;
import io.github.soundgoodizerfan.feedback.machine.creative.CreativeXpBlockEntity;
import io.github.soundgoodizerfan.feedback.modular.MaterialPerks;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The runtime kind of the Reservoir effect: {@code reserve}, a part that holds experience as an {@link XpStore},
 * summed over the parts of a tool. Its ceiling is {@link #POINTS_PER_LEVEL} points per attunement level; how many points
 * a level of charge is comes from the part material's {@code xp_store} top level, so a part of a high-level material holds more points per level
 * (progression roadmap L2). It stores and never makes. Charged by sneak-use in the air against the holder's level
 * (the lower level gives to the higher, {@link #TRANSFER} points a use) or sneak-use on a creative XP source (up to the
 * source's level); drawn on by Mend before the holder is, and filled by Tithe before orbs drop ({@link MendKinds}). Not
 * built: heat past the attuning point dumping what is stored, an exact reading of the figure (the tooltip says only how
 * full it is), and the minimum draw level (an attunement on a machine draws only while the reservoir stands at level 1
 * or more; no machine attunement draws yet).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class ReservoirKinds {

    /** Ceiling in points per attunement level (today's capacity). [OPEN] until played */
    private static final int POINTS_PER_LEVEL = 60;
    /** Top level for a part whose material has no {@code xp_store}: invented. */
    private static final float DEFAULT_TOP_LEVEL = 20f;
    /** Points one sneak-use in the air moves between the holder and the part. [OPEN] until played */
    private static final int TRANSFER = 20;

    private ReservoirKinds() {
    }

    public static void register() {
    }

    /**
     * The tool's container: ceiling from the summed attunement levels, top level the level-weighted mean of the
     * reserve parts' {@code xp_store}. Null when the tool has no Reservoir. Held points are clamped to the ceiling.
     */
    public static XpStore store(ItemStack stack) {
        int levels = 0;
        float top = 0;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(stack))
            if (a.kind().equals("reserve")) {
                levels += a.level();
                top += a.level() * a.material().xpStore().map(Material.XpCapacity::topLevel).orElse(DEFAULT_TOP_LEVEL);
            }
        if (levels == 0)
            return null;
        XpStore held = stack.get(FDataComponents.XP_STORE.get());
        return XpStore.empty(levels * POINTS_PER_LEVEL, top / levels).with(held == null ? 0 : held.points());
    }

    /** How full a Reservoir tool is, 0..1; negative when it holds none at all. For the HUD bar. */
    public static float fill(ItemStack stack) {
        XpStore s = store(stack);
        return s == null || s.maxPoints() == 0 ? -1f : s.points() / (float) s.maxPoints();
    }

    static int stored(ItemStack stack) {
        XpStore s = store(stack);
        return s == null ? 0 : s.points();
    }

    /** Puts up to {@code points} in, no higher than {@code maxLevel}; the points that went. */
    static int add(ItemStack stack, int points, float maxLevel) {
        XpStore s = store(stack);
        if (s == null)
            return 0;
        int room = Math.min(s.maxPoints(), (int) Math.ceil(maxLevel * s.perLevel())) - s.points();
        int taken = Math.max(0, Math.min(points, room));
        if (taken > 0)
            stack.set(FDataComponents.XP_STORE.get(), s.with(s.points() + taken));
        return taken;
    }

    static int add(ItemStack stack, int points) {
        return add(stack, points, Float.MAX_VALUE);
    }

    /** Takes up to {@code points} out; the points that came. */
    static int take(ItemStack stack, int points) {
        XpStore s = store(stack);
        int taken = s == null ? 0 : Math.min(points, s.points());
        if (taken > 0)
            stack.set(FDataComponents.XP_STORE.get(), s.with(s.points() - taken));
        return taken;
    }

    private static float level(Player player) {
        return player.experienceLevel + player.experienceProgress;
    }

    @SubscribeEvent
    public static void onUseOnSource(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (player.level().isClientSide || !player.isShiftKeyDown() || store(stack) == null
                || !(player.level().getBlockEntity(event.getPos()) instanceof CreativeXpBlockEntity source) || !source.on())
            return;
        int moved = add(stack, source.points(), source.level());
        player.displayClientMessage(Component.translatable(moved > 0 ? "feedback.reservoir.filled" : "feedback.reservoir.full"), true);
        event.setCanceled(true);
    }

    /** Sneak-use in the air: the lower level gives to the higher one, so the two settle toward each other. */
    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        XpStore s = store(stack);
        if (player.level().isClientSide || !player.isShiftKeyDown() || s == null)
            return;
        if (level(player) >= s.level()) {
            int moved = add(stack, Math.min(TRANSFER, player.totalExperience));
            if (moved > 0) {
                player.giveExperiencePoints(-moved);
                player.displayClientMessage(Component.translatable("feedback.reservoir.filled"), true);
            } else
                player.displayClientMessage(Component.translatable("feedback.reservoir.full"), true);
        } else {
            int moved = take(stack, TRANSFER);
            player.giveExperiencePoints(moved);
            player.displayClientMessage(Component.translatable("feedback.reservoir.drawn"), true);
        }
    }
}
