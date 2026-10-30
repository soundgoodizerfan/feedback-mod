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
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.net.HudReadoutPayload;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.machine.creative.CreativeXpBlockEntity;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FMenus;

import net.minecraft.core.Direction;
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
 * <p>shortcut: one slot and no reagent. The spec's reagent slot waits on its [OPEN] questions; the cost is
 * {@link ModularTuning#ENCHANT_XP_COST} of experience, drawn from a Creative XP source beside the table (nothing else
 * produces XPu yet) or else the player's own points, spent whatever the outcome. A part takes several effects up to
 * its material's {@link EnchantGame#capacity}; a later one is an independent session and disturbs no earlier one (ruled 2026-10-23).
 */
public class EnchantMenu extends AbstractContainerMenu {

    public static final int W = 292, H = 232, PART_X = 16, PART_Y = 27, INV_X = 65, INV_Y = 148;

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
                return takes(stack);
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

    /**
     * Whether {@code stack} can be attuned: a shaped part, or raw stock in a form some tool's role takes (a rod, a grip, a
     * bone) -- the parts that are not shaped but still sit in a tool.
     */
    private boolean takes(ItemStack stack) {
        if (stack.getItem() instanceof ModularPartItem)
            return stack.has(FDataComponents.MADE_OF.get());
        return Material.find(ClientMaterials.on(player.level()), stack)
                .filter(l -> ModularTypes.known().values().stream()
                        .anyMatch(t -> t.roles().values().stream().anyMatch(r -> r.part().isEmpty() && r.forms().contains(l.form()))))
                .isPresent();
    }

    private Material.Located located() {
        return part().isEmpty() ? null : Material.find(ClientMaterials.on(player.level()), part()).orElse(null);
    }

    /** The names of the roles the part in the slot could fill in some tool; empty when it is not known or the slot is empty. */
    public java.util.Set<String> roles() {
        ItemStack stack = part();
        Material.Located l = located();
        String form = stack.getItem() instanceof ModularPartItem
                ? net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()
                : l == null ? null : l.form();
        java.util.Set<String> out = new java.util.HashSet<>();
        if (form != null)
            for (ModularType t : ModularTypes.known().values())
                t.roles().values().forEach(r -> {
                    if (r.forms().contains(form))
                        out.add(r.name());
                });
        return out;
    }

    /** The material of the part in the slot, as this side knows it, or null. */
    public Material material() {
        Material.Located l = located();
        return l == null ? null : l.material();
    }

    /**
     * Whether the part holds all it can: its attunements count against its material's capacity
     * ({@link EnchantGame#capacity}). Re-attuning an effect it already has replaces that one.
     */
    public boolean attuned(ResourceLocation effect) {
        MadeOf made = part().get(FDataComponents.MADE_OF.get());
        Material m = material();
        if (made == null || m == null)
            return false;
        Attunements.Effect e = Attunements.get().effects().get(effect);
        if (e != null && made.dims().containsKey(e.key()))
            return false;
        return made.dims().keySet().stream().filter(k -> k.startsWith(Attunements.PREFIX)).count() >= EnchantGame.capacity(m);
    }

    /** The Creative XP source beside the table that can pay a session, or null. */
    private CreativeXpBlockEntity tap(int cost) {
        // evaluate wraps its result in Optional.of, so a null from the lambda is an NPE: return an Optional of our own.
        return access.evaluate((level, pos) -> {
            for (Direction d : Direction.values())
                if (level.getBlockEntity(pos.relative(d)) instanceof CreativeXpBlockEntity source && source.on()
                        && source.xpu().value() >= cost)
                    return Optional.of(source);
            return Optional.<CreativeXpBlockEntity>empty();
        }).flatMap(o -> o).orElse(null);
    }

    /** The slot of a potion bottle in {@code server}'s inventory matching the effect's optional potion reagent, or -1. */
    private static int reagentSlot(ServerPlayer server, Attunements.Effect e) {
        if (e.reagent().isEmpty())
            return -1;
        var inv = server.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            io.github.soundgoodizerfan.feedback.potion.PotionState state = s.getItem() instanceof io.github.soundgoodizerfan.feedback.potion.PotionItem
                    ? s.get(FDataComponents.POTION_STATE.get()) : null;
            if (state != null && state.effect().filter(e.reagent().get()::equals).isPresent() && state.amplifierNow(server.level()) >= 0)
                return i;
        }
        return -1;
    }

    /** The experience a session costs this player: the game's price, less a share of it when a potion reagent is at hand. */
    private static int price(ServerPlayer server, Attunements.Effect e, int cost) {
        int slot = reagentSlot(server, e);
        if (slot < 0)
            return cost;
        float potency = server.getInventory().getItem(slot).get(FDataComponents.POTION_STATE.get()).potencyNow(server.level());
        return Math.max(1, Math.round(cost * (1f - ModularTuning.ENCHANT_REAGENT_DISCOUNT * potency)));
    }

    private static void say(ServerPlayer player, String key) {
        HudReadoutPayload.send(player, Component.translatable("message.feedback.enchant." + key), 60);
    }

    /** The player pressed Begin: refuse an already-full or unaffordable part, else note when and how hot. */
    public void begin(ServerPlayer server, ResourceLocation effect) {
        Material m = material();
        if (m == null || !Attunements.get().effects().containsKey(effect))
            return;
        float temperature = EnchantGame.start(m.attuning(), ItemHeat.get(part(), server.level()).value());
        int owed = price(server, Attunements.get().effects().get(effect), EnchantGame.cost(m.attuning(), temperature));
        if (attuned(effect)) {
            say(server, "attuned");
        } else if (!server.isCreative() && tap(owed) == null && server.totalExperience < owed) {
            say(server, "xp");
        } else
            EnchantGame.begin(server.getUUID(), new EnchantGame.Session(effect, server.level().getGameTime(), temperature));
    }

    /** The pen the player held, tick by tick: replay it, judge it, and mark the part. Never trusts a client result. */
    public void finish(ServerPlayer server, ResourceLocation effect, float start, List<Integer> samples) {
        EnchantGame.Session session = EnchantGame.end(server.getUUID());
        Attunements.Effect e = Attunements.get().effects().get(effect);
        Material m = material();
        if (session == null || e == null || m == null || !session.effect().equals(effect) || part().isEmpty()) {
            say(server, "lost");
            return;
        }
        // A result cannot have more ticks in it than have gone by since Begin.
        if (samples.size() > server.level().getGameTime() - session.started() + ModularTuning.ENCHANT_TICK_SLACK) {
            say(server, "lost");
            return;
        }
        // The screen began from its own reading of the heat; Begin took a few ticks to arrive, so allow that much cooling.
        float temperature = Math.abs(start - session.temperature())
                <= FTuning.ITEM_COOLING_TU_PER_TICK.tuPerTick() * ModularTuning.ENCHANT_TICK_SLACK ? start : session.temperature();
        List<Integer> played = EnchantGame.bounded(samples);
        Sigil.Result result = Sigil.run(m.attuning(), temperature, Sigil.word(e), played);
        EnchantGame.Verdict verdict = EnchantGame.judge(e, EnchantGame.level(m), result.accuracy(), result.negative(), result.reached());
        int cost = price(server, e, EnchantGame.cost(m.attuning(), session.temperature()));
        // The potion reagent is burned with the session, whatever the outcome; the empty bottle comes back.
        int reagent = reagentSlot(server, e);
        if (reagent >= 0) {
            server.getInventory().setItem(reagent, new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE));
            server.containerMenu.broadcastChanges();
        }
        if (!server.isCreative() && tap(cost) == null)
            server.giveExperiencePoints(-cost);
        if (verdict.none()) {
            say(server, "nothing");
            return;
        }
        ItemStack stack = part();
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        if (made == null) // raw stock: the stamp carries the attunement, the item still says what it is
            made = new MadeOf(located().id(), located().form(), 1);
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
