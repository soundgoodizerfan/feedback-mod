// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.LinkedHashMap;
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
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

/**
 * A pack worn in the Curios {@code back} slot. The shell's shape fixes the slots, the strap's strength fixes the carry rating, and the
 * contents sit in the vanilla container component on the stack. Fittings added at the Tool Table take mounting points (the shell's, plus
 * the frame's): a frame, a padded panel and a hip belt raise the carry rating; a holster, bottle loops, a bedroll lashing and a bandolier add
 * a row of typed slots under the fixed ones (see {@link PackMenu}); a crafting pad opens the 3x3 grid on firm ground. Mass is the pack plus
 * what it holds (nothing else the wearer carries); over the rating the wearer is slowed and cannot sprint, the strap wears while it stays
 * so, and a strap at its last point lets go: the pack drops off with its contents.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public class ModularBackpack extends ModularItem {

    /** The shell role's letter in the type file. */
    public static final String SHELL = "S";
    /** The strap role's letter in the type file. */
    public static final String STRAP = "T";
    /** Fitting roles (raw stock added by rework): frame, side holster, bottle loops, bedroll lashing, bandolier, padded panel, hip belt, crafting pad. */
    public static final String FRAME = "F", HOLSTER = "H", LOOPS = "B", LASHING = "R", BANDOLIER = "A", PANEL = "P", BELT = "W", PAD = "C";
    /** More fittings: hard liner, fire patch, oiled coat, warm liner, quick-release buckle, lamp bracket. */
    public static final String LINER = "L", PATCH = "X", COAT = "O", WARM = "I", BUCKLE = "Q", LAMP = "K";
    /** Roles that carry load, and so are down to the carry rating. */
    public static final List<String> CARRY_ROLES = List.of(STRAP, FRAME, PANEL, BELT);
    /** The store roles in the order their slots sit in the extra row, each with how many slots it adds. */
    public static final Map<String, Integer> STORES = new LinkedHashMap<>();
    public static final List<String> STORE_ROLES;
    static {
        STORES.put(HOLSTER, 1);
        STORES.put(LOOPS, 3);
        STORES.put(LASHING, 1);
        STORES.put(BANDOLIER, 3);
        STORES.put(LAMP, 1);
        STORE_ROLES = List.copyOf(STORES.keySet());
    }

    /** Item tags a datapack fills to say what a stack item weighs; checked before the material is. */
    private static final TagKey<Item> LIGHT = TagKey.create(Registries.ITEM, Feedback.id("pack_mass/light"));
    private static final TagKey<Item> HEAVY = TagKey.create(Registries.ITEM, Feedback.id("pack_mass/heavy"));
    private static final ResourceLocation LOAD_ID = Feedback.id("pack_load");
    private static final String OVER = "feedback_pack_over";

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

    /** Mounting points a fitting takes: the frame none (it gives some), the crafting pad most. */
    public static int cost(String role) {
        return role.equals(SHELL) || role.equals(STRAP) || role.equals(FRAME) ? 0
                : role.equals(PAD) ? ModularTuning.PACK_PAD_POINTS : ModularTuning.PACK_FITTING_POINTS;
    }

    /** Store kind of each slot of the extra row of {@code bag}, in a row of nine: 0 is never used there, a positive kind is a store, -1 a slot no fitting gives. Empty when there is no store. */
    public static int[] extraRow(ItemStack bag) {
        ModularDesign d = ModularStacks.design(bag);
        int[] row = new int[9];
        java.util.Arrays.fill(row, -1);
        if (d == null)
            return new int[0];
        int at = 0;
        for (int k = 0; k < STORE_ROLES.size(); k++)
            if (d.part(STORE_ROLES.get(k)) != null)
                for (int i = 0; i < STORES.get(STORE_ROLES.get(k)); i++)
                    row[at++] = k + 1;
        return at == 0 ? new int[0] : row;
    }

    /** Whether a slot of {@code kind} (0 the fixed slots, else the store's index plus one) takes {@code stack}. Used on both sides so the client's prediction agrees. */
    public static boolean accepts(int kind, ItemStack stack) {
        if (kind < 0 || stack.getItem() instanceof ModularBackpack)
            return false;
        if (kind == 0)
            return true;
        return switch (STORE_ROLES.get(kind - 1)) {
            case HOLSTER -> stack.getMaxStackSize() == 1 && stack.isDamageableItem() && !(stack.getItem() instanceof ModularBedroll);
            case LOOPS -> stack.has(DataComponents.POTION_CONTENTS) || stack.getCapability(Capabilities.FluidHandler.ITEM) != null;
            case LASHING -> stack.getItem() instanceof ModularBedroll;
            case LAMP -> stack.getItem() instanceof net.minecraft.world.item.BlockItem b && b.getBlock() instanceof net.minecraft.world.level.block.LanternBlock;
            default -> stack.getItem() instanceof io.github.soundgoodizerfan.feedback.weapon.CartridgeItem
                    || stack.is(net.minecraft.tags.ItemTags.ARROWS);
        };
    }

    /** Opens the worn pack's contents for {@code player}; the key's payload lands here. */
    public static void open(ServerPlayer player) {
        SlotResult r = worn(player);
        if (r == null || ModularStacks.broken(r.stack()) || slots(r.stack()) < 9)
            return;
        ItemStack bag = r.stack();
        int[] extra = extraRow(bag);
        int fixed = Math.min(3, slots(bag) / 9);
        int rows = fixed + (extra.length > 0 ? 1 : 0);
        int[] kinds = new int[rows * 9];
        for (int i = 0; i < extra.length; i++)
            kinds[fixed * 9 + i] = extra[i];
        Container held = new Contents(bag, kinds, player);
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new PackMenu(id, inv, held, rows, kinds), bag.getHoverName()),
                buf -> PackMenu.write(buf, rows, kinds));
    }

    /** Opens the 3x3 crafting grid from a fold-out pad, on firm ground. */
    public static void openPad(ServerPlayer player) {
        SlotResult r = worn(player);
        if (r == null || ModularStacks.broken(r.stack()) || ModularStacks.design(r.stack()) == null
                || ModularStacks.design(r.stack()).part(PAD) == null)
            return;
        if (!player.onGround() || player.isInWater()) {
            player.displayClientMessage(Component.translatable("message.feedback.pack_pad_ground"), true);
            return;
        }
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new CraftingMenu(id, inv, ContainerLevelAccess.create(player.level(), player.blockPosition())) {
            @Override
            public boolean stillValid(Player who) {
                SlotResult w = worn(who);
                return w != null && who.onGround() && ModularStacks.design(w.stack()) != null && ModularStacks.design(w.stack()).part(PAD) != null;
            }
        }, Component.translatable("container.crafting")));
    }

    /** The pack's slots as a container: read off the stack on opening, written back on every change. Each slot has a kind ({@link #accepts}). */
    private static final class Contents extends SimpleContainer {
        private final int[] kinds;
        private final ServerPlayer owner;
        private final ItemStack bag;

        Contents(ItemStack bag, int[] kinds, ServerPlayer owner) {
            super(kinds.length);
            this.bag = bag;
            this.kinds = kinds;
            this.owner = owner;
            List<ItemStack> held = bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).stream().toList();
            for (int i = 0; i < Math.min(kinds.length, held.size()); i++)
                setItem(i, held.get(i).copy());
            addListener(c -> {
                bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(
                        java.util.stream.IntStream.range(0, getContainerSize()).mapToObj(this::getItem).toList()));
                weigh(owner);
            });
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return accepts(kinds[slot], stack);
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

    /** {@code Mu} of the pack and everything in it, the stores' contents too. */
    public static float load(ItemStack bag) {
        ModularFigures f = ModularStacks.figures(bag);
        float total = f == null ? 0f : f.get(Fig.MASS);
        for (ItemStack s : bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItems())
            total += massOf(s.getItem()) * s.getCount();
        return total;
    }

    // --- the wearer ------------------------------------------------------------------------

    private static final String WET = "feedback_pack_wet", LIT = "feedback_pack_lit", LIGHT_AT = "feedback_pack_light";

    /** Whether {@code bag} carries the fitting {@code role}. */
    public static boolean fitted(ItemStack bag, String role) {
        ModularDesign d = ModularStacks.design(bag);
        return d != null && d.part(role) != null;
    }

    /** Index in the container list of the first slot of store {@code role}, or -1: fixed rows first, then the store's place in the extra row. */
    private static int storeIndex(ItemStack bag, String role) {
        int[] row = extraRow(bag);
        int kind = STORE_ROLES.indexOf(role) + 1;
        for (int i = 0; i < row.length; i++)
            if (row[i] == kind)
                return Math.min(3, slots(bag) / 9) * 9 + i;
        return -1;
    }

    /** The stack in store {@code role}'s first slot (empty if none). */
    public static ItemStack stored(ItemStack bag, String role) {
        int at = storeIndex(bag, role);
        List<ItemStack> held = bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).stream().toList();
        return at < 0 || at >= held.size() ? ItemStack.EMPTY : held.get(at);
    }

    /** Whether the shell is made of a material with {@code trait}. */
    private static boolean shellIs(ItemStack bag, Material.Trait trait) {
        ModularDesign d = ModularStacks.design(bag);
        ModularDesign.Part shell = d == null ? null : d.part(SHELL);
        Material m = shell == null ? null : MaterialTable.get().materials().get(shell.material());
        return m != null && m.traits().contains(trait);
    }

    /**
     * Weighs the worn pack now: over its rating the wearer is slowed and flagged so they cannot sprint, and a pack whose strap is gone
     * drops off. A cloth shell soaks in water or rain unless it has an oiled coat. Called when a pack goes on or off, when its contents
     * change, at login, and every {@link ModularTuning#PACK_FALLBACK_TICKS} as a net.
     */
    public static void weigh(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        speed.removeModifier(LOAD_ID);
        boolean over = false;
        boolean lit = false;
        SlotResult r = worn(player);
        if (r != null && ModularStacks.broken(r.stack())) {
            shed(player, r);
        } else if (r != null) {
            ItemStack bag = r.stack();
            ModularFigures f = ModularStacks.figures(bag);
            float load = load(bag);
            boolean wet = player.isInWaterOrRain() && !fitted(bag, COAT) && shellIs(bag, Material.Trait.FIBROUS);
            player.getPersistentData().putBoolean(WET, wet);
            if (wet)
                load *= ModularTuning.PACK_WET_FACTOR;
            over = f != null && load > f.get(Fig.CARRY);
            if (over)
                speed.addTransientModifier(new AttributeModifier(LOAD_ID, -ModularTuning.PACK_OVERLOAD_SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            lit = fitted(bag, LAMP) && !stored(bag, LAMP).isEmpty();
        }
        player.getPersistentData().putBoolean(OVER, over);
        player.getPersistentData().putBoolean(LIT, lit);
    }

    /** A pack going on or off the back weighs at once, after the slot has settled. */
    @SubscribeEvent
    public static void onCurioChange(CurioChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && (event.getFrom().getItem() instanceof ModularBackpack || event.getTo().getItem() instanceof ModularBackpack))
            player.server.execute(() -> weigh(player));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            weigh(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getPersistentData().putBoolean(LIT, false);
            light(player);
        }
    }

    /** Whether a lit, online wearer still follows the light at {@code pos}. */
    public static boolean holds(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        return level.players().stream().anyMatch(p -> p.getPersistentData().getBoolean(LIT)
                && p.getPersistentData().contains(LIGHT_AT) && p.getPersistentData().getLong(LIGHT_AT) == pos.asLong());
    }

    /** A lantern in the bracket lights the wearer: one light block at head height that follows, removed when it is not wanted. */
    private static void light(ServerPlayer player) {
        var data = player.getPersistentData();
        net.minecraft.core.BlockPos old = data.contains(LIGHT_AT) ? net.minecraft.core.BlockPos.of(data.getLong(LIGHT_AT)) : null;
        net.minecraft.core.BlockPos at = data.getBoolean(LIT) ? player.blockPosition().above() : null;
        var level = player.level();
        if (old != null && !old.equals(at)) {
            if (level.getBlockState(old).is(io.github.soundgoodizerfan.feedback.registry.FBlocks.PACK_LIGHT.get()))
                level.removeBlock(old, false);
            data.remove(LIGHT_AT);
        }
        if (at != null && !at.equals(old) && level.getBlockState(at).isAir()) {
            level.setBlock(at, io.github.soundgoodizerfan.feedback.registry.FBlocks.PACK_LIGHT.get().defaultBlockState(), 3);
            data.putLong(LIGHT_AT, at.asLong());
        }
    }

    /** Over the rating the pack cannot be sprinted in and the strap wears each second (a hard liner halves it); a naked cloth shell burns, a warm liner thaws; now and then the pack is weighed anyway. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        var data = player.getPersistentData();
        boolean over = data.getBoolean(OVER);
        if (over)
            player.setSprinting(false);
        if (data.getBoolean(LIT) || data.contains(LIGHT_AT)) {
            if (player.tickCount % 10 == 0)
                light(player);
        }
        if (player.tickCount % 20 != 0) {
            if (player.tickCount % ModularTuning.PACK_FALLBACK_TICKS == 10)
                weigh(player);
            return;
        }
        SlotResult r = worn(player);
        if (r == null) {
            if (over)
                weigh(player);
            return;
        }
        ItemStack bag = r.stack();
        boolean wear = over && (!fitted(bag, LINER) || player.tickCount % 40 == 0);
        if (player.isOnFire() && !fitted(bag, PATCH) && shellIs(bag, Material.Trait.FLAMMABLE)) {
            bag.setDamageValue(Math.min(bag.getMaxDamage() - 1, bag.getDamageValue() + ModularTuning.PACK_SCORCH_WEAR));
            ModularStacks.refresh(bag);
            wear = true;
        }
        if (wear) {
            if (over)
                bag.setDamageValue(Math.min(bag.getMaxDamage() - 1, bag.getDamageValue() + ModularTuning.PACK_OVERLOAD_WEAR));
            ModularStacks.refresh(bag);
        }
        if (player.getTicksFrozen() > 0 && fitted(bag, WARM))
            player.setTicksFrozen(Math.max(0, player.getTicksFrozen() - ModularTuning.PACK_THAW_TICKS));
        if (wear || over || data.getBoolean(WET) != (player.isInWaterOrRain() && !fitted(bag, COAT) && shellIs(bag, Material.Trait.FIBROUS)))
            weigh(player);
        else if (player.tickCount % ModularTuning.PACK_FALLBACK_TICKS == 0)
            weigh(player);
    }

    /** The quick-release buckle: the pack comes off at once and lands at the wearer's feet, contents and all. */
    public static void release(ServerPlayer player) {
        SlotResult r = worn(player);
        if (r == null || !fitted(r.stack(), BUCKLE))
            return;
        ItemStack bag = r.stack().copy();
        CuriosApi.getCuriosInventory(player).ifPresent(inv ->
                inv.setEquippedCurio(r.slotContext().identifier(), r.slotContext().index(), ItemStack.EMPTY));
        player.drop(bag, false);
    }

    /** Beds down in the bedroll on the lashing, without taking it out. */
    public static void sleep(ServerPlayer player) {
        SlotResult r = worn(player);
        if (r == null || !fitted(r.stack(), LASHING))
            return;
        ItemStack bag = r.stack();
        int at = storeIndex(bag, LASHING);
        List<ItemStack> held = new java.util.ArrayList<>(bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).stream().toList());
        if (at < 0 || at >= held.size() || !(held.get(at).getItem() instanceof ModularBedroll))
            return;
        ItemStack roll = held.get(at).copy();
        if (ModularBedroll.rest(player, roll)) {
            held.set(at, roll);
            bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(held));
        }
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
