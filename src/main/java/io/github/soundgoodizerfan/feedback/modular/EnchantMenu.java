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
package io.github.soundgoodizerfan.feedback.modular;

import java.util.List;

import io.github.soundgoodizerfan.feedback.client.HudReadouts;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FMenus;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Enchanting Table's menu ({@code docs/feedback_enchanting_spec.md} 4): one slot for a hot shaped part, the
 * player's inventory, and the attuning game ({@link EnchantGame}) the screen plays against it. Like the vanilla table
 * it keeps nothing: the part goes back to the player when the screen closes.
 *
 * <p>shortcut: one slot and no reagent. The spec's reagent and fuel slots wait on its [OPEN] questions; the
 * cost is experience levels ({@link ModularTuning#ENCHANT_XP_LEVELS}) taken from the player, spent whatever the
 * outcome. And one effect per part (a second would disturb the first, spec 6, [OPEN]).
 */
public class EnchantMenu extends AbstractContainerMenu {

    public static final int W = 240, H = 190, PART_X = 16, PART_Y = 30, INV_X = 39, INV_Y = 108;

    private final Container part = new SimpleContainer(1);
    private final ContainerLevelAccess access;
    private final Player player;

    /** Client-side open. */
    public EnchantMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public EnchantMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(FMenus.ENCHANT.get(), id);
        this.access = access;
        this.player = inventory.player;
        addSlot(new Slot(part, 0, PART_X, PART_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ModularPartItem && stack.has(FDataComponents.MADE_OF.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column + row * 9 + 9, INV_X + column * 18, INV_Y + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, INV_X + column * 18, INV_Y + 58));
    }

    public ItemStack part() {
        return part.getItem(0);
    }

    /** The material of the part in the slot, as this side knows it, or null. */
    public Material material() {
        MadeOf made = part().get(FDataComponents.MADE_OF.get());
        return made == null ? null : ClientMaterials.on(player.level()).get(made.material());
    }

    /** Whether the part already carries an attunement: one effect per part for now. */
    public boolean attuned() {
        MadeOf made = part().get(FDataComponents.MADE_OF.get());
        return made != null && made.dims().keySet().stream().anyMatch(k -> k.startsWith(Attunements.PREFIX));
    }

    private static void say(ServerPlayer player, String key) {
        HudReadouts.send(player, Component.translatable("message.feedback.enchant." + key), 60);
    }

    /** The player pressed Begin: refuse a cold, already-attuned or unaffordable part, else note when and how hot. */
    public void begin(ServerPlayer server, ResourceLocation effect) {
        Material m = material();
        if (m == null || !Attunements.get().effects().containsKey(effect))
            return;
        float temperature = ItemHeat.get(part(), server.level()).value();
        if (attuned()) {
            say(server, "attuned");
        } else if (!EnchantGame.hot(m.attuning(), temperature)) {
            say(server, "cold");
        } else if (!server.isCreative() && server.experienceLevel < ModularTuning.ENCHANT_XP_LEVELS) {
            say(server, "xp");
        } else
            EnchantGame.begin(server.getUUID(), new EnchantGame.Session(effect, server.level().getGameTime(), temperature));
    }

    /** The field the player held, tick by tick: replay it, judge it, and mark the part. Never trusts the client's P. */
    public void finish(ServerPlayer server, ResourceLocation effect, List<Integer> samples) {
        EnchantGame.Session session = EnchantGame.end(server.getUUID());
        Attunements.Effect e = Attunements.get().effects().get(effect);
        Material m = material();
        if (session == null || e == null || m == null || !session.effect().equals(effect) || part().isEmpty())
            return;
        // A result cannot have more ticks in it than have gone by since Begin.
        if (samples.size() > server.level().getGameTime() - session.started() + 10)
            return;
        List<Integer> played = EnchantGame.bounded(samples);
        boolean[] reached = new boolean[1];
        float polarization = EnchantGame.run(m.attuning(), session.temperature(), played, reached);
        EnchantGame.Verdict verdict = EnchantGame.judge(e, EnchantGame.level(m), polarization, reached[0]);
        if (!server.isCreative())
            server.giveExperienceLevels(-ModularTuning.ENCHANT_XP_LEVELS);
        if (verdict.none()) {
            say(server, "nothing");
            return;
        }
        ItemStack stack = part();
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        stack.set(FDataComponents.MADE_OF.get(), new MadeOf(made.material(), made.stock(), made.cells(), made.variant(),
                Math.min(ForgeRhythm.MAX_FLAW, made.flaw() + verdict.flaw()), made.withDim(e.key(), verdict.level()).dims()));
        broadcastChanges();
        say(server, verdict.flaw() > 0 ? "flawed" : "clean");
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        EnchantGame.end(player.getUUID());
        access.execute((level, pos) -> clearContainer(player, part));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, FModular.ENCHANTING_TABLE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack held = slot.getItem();
        ItemStack moved = held.copy();
        if (index == 0) {
            if (!moveItemStackTo(held, 1, slots.size(), true))
                return ItemStack.EMPTY;
        } else if (slots.get(0).mayPlace(held)) {
            if (!moveItemStackTo(held, 0, 1, false))
                return ItemStack.EMPTY;
        } else
            return ItemStack.EMPTY;
        if (held.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        return moved;
    }
}
