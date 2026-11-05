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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.process.FormType;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FormItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

/**
 * A pack worn in the Curios {@code back} slot. The shell's shape fixes the slots, the strap's strength fixes the carry rating, and the
 * contents sit in the vanilla container component on the stack. Mass is the pack plus what it holds (nothing else the wearer carries);
 * over the strap's rating the wearer is slowed and cannot sprint, the strap wears while it stays so, and a strap at its last point
 * lets go: the pack drops off with its contents.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public class ModularBackpack extends ModularItem {

    /** The shell role's letter in the type file. */
    public static final String SHELL = "S";
    /** The strap role's letter in the type file. */
    public static final String STRAP = "T";

    /** Item tags a datapack fills to say what a stack item weighs; checked before the material is. */
    private static final TagKey<Item> LIGHT = TagKey.create(Registries.ITEM, Feedback.id("pack_mass/light"));
    private static final TagKey<Item> HEAVY = TagKey.create(Registries.ITEM, Feedback.id("pack_mass/heavy"));
    private static final ResourceLocation LOAD_ID = Feedback.id("pack_load");
    private static final String OVER = "feedback_pack_over";
    private static final MenuType<?>[] ROWS = {MenuType.GENERIC_9x1, MenuType.GENERIC_9x2, MenuType.GENERIC_9x3};

    public ModularBackpack(Properties properties) {
        super(properties);
    }

    /** The worn pack, or null. */
    private static SlotResult worn(Player player) {
        return CuriosApi.getCuriosInventory(player).flatMap(inv -> inv.findFirstCurio(s -> s.getItem() instanceof ModularBackpack)).orElse(null);
    }

    private static int slots(ItemStack bag) {
        ModularFigures f = ModularStacks.figures(bag);
        return f == null ? 0 : Math.round(f.get(Fig.SLOTS));
    }

    /** Opens the worn pack's contents for {@code player}; the key's payload lands here. */
    public static void open(ServerPlayer player) {
        SlotResult r = worn(player);
        if (r == null || ModularStacks.broken(r.stack()) || slots(r.stack()) < 9)
            return;
        ItemStack bag = r.stack();
        int rows = Math.min(ROWS.length, slots(bag) / 9);
        Container held = new Contents(bag, rows * 9, player);
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new ChestMenu(ROWS[rows - 1], id, inv, held, rows), bag.getHoverName()));
    }

    /** The pack's slots as a container: read off the stack on opening, written back on every change. A pack cannot hold a pack. */
    private static final class Contents extends SimpleContainer {
        private final ItemStack bag;
        private final Player owner;

        Contents(ItemStack bag, int size, Player owner) {
            super(size);
            this.bag = bag;
            this.owner = owner;
            List<ItemStack> held = bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).stream().toList();
            for (int i = 0; i < Math.min(size, held.size()); i++)
                setItem(i, held.get(i).copy());
            addListener(c -> bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(
                    java.util.stream.IntStream.range(0, getContainerSize()).mapToObj(this::getItem).toList())));
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return !(stack.getItem() instanceof ModularBackpack);
        }

        @Override
        public boolean stillValid(Player player) {
            SlotResult r = worn(owner);
            return player == owner && r != null && r.stack() == bag;
        }
    }

    // --- mass ------------------------------------------------------------------------------

    private static final Map<Item, Float> MASSES = new ConcurrentHashMap<>();
    private static int massGeneration = -1;

    /**
     * STAND-IN: {@code Mu} of one {@code item}: a mass-class tag first, else its material's density times its form's volume
     * (an iron ingot is a bit over a Mu), else a flat default. Replace when items carry a mass.
     */
    public static float massOf(Item item) {
        if (massGeneration != MaterialTable.generation()) {
            MASSES.clear();
            massGeneration = MaterialTable.generation();
        }
        return MASSES.computeIfAbsent(item, i -> {
            ItemStack one = new ItemStack(i);
            if (one.is(HEAVY))
                return ModularTuning.PACK_MASS_HEAVY;
            if (one.is(LIGHT))
                return ModularTuning.PACK_MASS_LIGHT;
            return Material.find(MaterialTable.get().materials(), i).map(l -> {
                FormType t = FormItems.types().get(l.form());
                return l.material().density().muPerMb() * (t == null ? FormType.DEFAULT_MB : t.mb()) / 1000f;
            }).filter(m -> m > 0f).orElse(ModularTuning.PACK_MASS_DEFAULT);
        });
    }

    /** {@code Mu} of the pack and everything in it. */
    public static float load(ItemStack bag) {
        ModularFigures f = ModularStacks.figures(bag);
        float total = f == null ? 0f : f.get(Fig.MASS);
        for (ItemStack s : bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItems())
            total += massOf(s.getItem()) * s.getCount();
        return total;
    }

    // --- the wearer ------------------------------------------------------------------------

    /** Once a second the pack is weighed; between weighings an overloaded wearer cannot sprint. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (player.tickCount % 20 != 0) {
            if (player.getPersistentData().getBoolean(OVER))
                player.setSprinting(false);
            return;
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        speed.removeModifier(LOAD_ID);
        boolean over = false;
        SlotResult r = worn(player);
        if (r != null && ModularStacks.broken(r.stack())) {
            shed(player, r);
        } else if (r != null) {
            ItemStack bag = r.stack();
            ModularFigures f = ModularStacks.figures(bag);
            over = f != null && load(bag) > f.get(Fig.CARRY);
            if (over) {
                speed.addTransientModifier(new AttributeModifier(LOAD_ID, -ModularTuning.PACK_OVERLOAD_SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                bag.setDamageValue(Math.min(bag.getMaxDamage() - 1, bag.getDamageValue() + ModularTuning.PACK_OVERLOAD_WEAR));
                ModularStacks.refresh(bag);
                if (ModularStacks.broken(bag))
                    shed(player, r);
            }
        }
        player.getPersistentData().putBoolean(OVER, over);
    }

    /** The strap has let go: the pack, contents and all, drops off the wearer. */
    private static void shed(ServerPlayer player, SlotResult r) {
        ItemStack bag = r.stack().copy();
        CuriosApi.getCuriosInventory(player).ifPresent(inv ->
                inv.setEquippedCurio(r.slotContext().identifier(), r.slotContext().index(), ItemStack.EMPTY));
        player.drop(bag, true);
        player.displayClientMessage(Component.translatable("message.feedback.pack_strap_broke"), true);
    }
}
