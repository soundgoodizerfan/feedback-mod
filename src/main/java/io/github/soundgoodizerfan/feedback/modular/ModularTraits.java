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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Finds the {@link ModularTrait} an item has and hands the game's events to it (spec 5.4). The
 * trait is the main part's variant's, else the type's own. Item classes forward their use hooks
 * here ({@code ModularItem}, {@code ModularAxe}); everything that is an event is subscribed to
 * once, here, so a trait class never subscribes to anything.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class ModularTraits {

    private ModularTraits() {
    }

    private static final Map<String, ModularTrait> REGISTRY = new HashMap<>();

    static {
        // One line per trait, the id its data names. A trait is built by replacing its stub.
        register("flurry", new io.github.soundgoodizerfan.feedback.modular.trait.Flurry());
        register("dash", new io.github.soundgoodizerfan.feedback.modular.trait.Dash());
        register("hack", new io.github.soundgoodizerfan.feedback.modular.trait.Hack());
        register("overhead", new io.github.soundgoodizerfan.feedback.modular.trait.Overhead());
        register("backstab", new io.github.soundgoodizerfan.feedback.modular.trait.Backstab());
        register("flourish", new io.github.soundgoodizerfan.feedback.modular.trait.Flourish());
        register("timber", new io.github.soundgoodizerfan.feedback.modular.trait.Timber());
        register("throw", new io.github.soundgoodizerfan.feedback.modular.trait.Throw());
        register("hook", new io.github.soundgoodizerfan.feedback.modular.trait.Hook());
        register("sounding", new io.github.soundgoodizerfan.feedback.modular.trait.Sounding());
        register("vein", new io.github.soundgoodizerfan.feedback.modular.trait.Vein());
        register("spear_charge", new io.github.soundgoodizerfan.feedback.modular.trait.SpearCharge());
        // armour (worn; one line each)
        register("open_face", new io.github.soundgoodizerfan.feedback.modular.trait.OpenFace());
        register("closed_visor", new io.github.soundgoodizerfan.feedback.modular.trait.ClosedVisor());
        register("neck_guard", new io.github.soundgoodizerfan.feedback.modular.trait.NeckGuard());
        register("studded", new io.github.soundgoodizerfan.feedback.modular.trait.Studded());
        register("rigid", new io.github.soundgoodizerfan.feedback.modular.trait.Rigid());
        register("mail", new io.github.soundgoodizerfan.feedback.modular.trait.Mail());
        register("kick", new io.github.soundgoodizerfan.feedback.modular.trait.Kick());
        register("braced", new io.github.soundgoodizerfan.feedback.modular.trait.Braced());
        register("trample", new io.github.soundgoodizerfan.feedback.modular.trait.Trample());
        register("spurs", new io.github.soundgoodizerfan.feedback.modular.trait.Spurs());
        register("scoop", new io.github.soundgoodizerfan.feedback.modular.trait.Scoop());
        register("trench", new io.github.soundgoodizerfan.feedback.modular.trait.Trench());
        register("kama", new io.github.soundgoodizerfan.feedback.modular.trait.Kama());
        register("rake", new io.github.soundgoodizerfan.feedback.modular.trait.Rake());
        register("scythe", new io.github.soundgoodizerfan.feedback.modular.trait.Scythe());
        register("billhook", new io.github.soundgoodizerfan.feedback.modular.trait.Billhook());
        register("reaping", new io.github.soundgoodizerfan.feedback.modular.trait.Reaping());
        register("slam", new io.github.soundgoodizerfan.feedback.modular.trait.Slam());
        register("pierce", new io.github.soundgoodizerfan.feedback.modular.trait.Pierce());
        register("butcher", new io.github.soundgoodizerfan.feedback.modular.trait.Butcher());
        register("line_cut", new io.github.soundgoodizerfan.feedback.modular.trait.LineCut());
        register("lunge", new io.github.soundgoodizerfan.feedback.modular.trait.Lunge());
        register("trident", new io.github.soundgoodizerfan.feedback.modular.trait.TridentThrow());
        register("smash", new io.github.soundgoodizerfan.feedback.modular.trait.SmashAttack());
        register("harpoon", new io.github.soundgoodizerfan.feedback.modular.trait.Harpoon());
        register("fork", new io.github.soundgoodizerfan.feedback.modular.trait.Fork());
        register("snap_shot", new io.github.soundgoodizerfan.feedback.modular.trait.Handling());
        register("heavy_draw", new io.github.soundgoodizerfan.feedback.modular.trait.Handling());
        register("heavy_bolt", new io.github.soundgoodizerfan.feedback.modular.trait.Handling());
        register("snap_bolt", new io.github.soundgoodizerfan.feedback.modular.trait.Handling());
        register("recurve", new io.github.soundgoodizerfan.feedback.modular.trait.Recurve());
        register("stalker", new io.github.soundgoodizerfan.feedback.modular.trait.Stalker());
        register("repeater", new io.github.soundgoodizerfan.feedback.modular.trait.Repeater());
        register("parry", new io.github.soundgoodizerfan.feedback.modular.trait.Parry());
        register("deflect", new io.github.soundgoodizerfan.feedback.modular.trait.Deflect());
        register("blocking", new io.github.soundgoodizerfan.feedback.modular.trait.Blocking());
        register("bulwark", new io.github.soundgoodizerfan.feedback.modular.trait.Bulwark());
        // a gun's mechanisms (spec 8.4): read off its parts, any number to a gun
        for (String id : new String[]{"hammer", "manual_cycle", "select_fire", "double_barrel", "closed_bolt", "off_hand"})
            register(id, new io.github.soundgoodizerfan.feedback.modular.trait.GunMechanism());
        register("thermal_fit", new io.github.soundgoodizerfan.feedback.modular.trait.GunThermalFit());
        register("counterweight", new io.github.soundgoodizerfan.feedback.modular.trait.GunCounterweight());
        register("sculk_jacket", new io.github.soundgoodizerfan.feedback.modular.trait.GunSculkJacket());
        register("resonance", new io.github.soundgoodizerfan.feedback.modular.trait.GunResonance());
        register("blowback", new io.github.soundgoodizerfan.feedback.modular.trait.GunBlowback());
        register("bolt_lockup", new io.github.soundgoodizerfan.feedback.modular.trait.GunBoltLockup());
        register("gas_operated", new io.github.soundgoodizerfan.feedback.modular.trait.GunGasOperated());
        register("gun_braced", new io.github.soundgoodizerfan.feedback.modular.trait.GunBraced());
        register("open_bolt", new io.github.soundgoodizerfan.feedback.modular.trait.GunOpenBolt());
        register("fan", new io.github.soundgoodizerfan.feedback.modular.trait.GunFan());
        register("belt_fed", new io.github.soundgoodizerfan.feedback.modular.trait.GunBeltFed());
    }

    private static void register(String id, ModularTrait trait) {
        REGISTRY.put(id, trait);
    }

    /** Every registered trait, for setup that has to reach all of them (client hooks). */
    public static Iterable<ModularTrait> all() {
        return REGISTRY.values();
    }

    /** A trait, the id its data names it by, and the numbers it reads for a stack. */
    public record Active(String id, ModularTrait trait, ModularTrait.Params params) {}

    /** The first id of a comma-separated list of trait ids that is registered, else empty. */
    private static Optional<String> firstId(String ids) {
        for (String id : ids.split(","))
            if (REGISTRY.containsKey(id.trim()))
                return Optional.of(id.trim());
        return Optional.empty();
    }

    /**
     * The variant that gives {@code design} its trait: the primary part's if it has one, else the
     * first other part's in role order (a sword's guard can be a blocking one).
     */
    public static Optional<ModularType.Variant> traitVariant(ModularDesign design, ModularType type) {
        List<String> order = new java.util.ArrayList<>(type.order());
        order.remove(type.primary());
        order.add(0, type.primary());
        for (String key : order) {
            ModularDesign.Part part = design.part(key);
            ModularType.Role role = type.roles().get(key);
            if (part == null || role == null)
                continue;
            Optional<ModularType.Variant> v = role.variant(part).filter(x -> firstId(x.trait()).isPresent());
            if (v.isPresent())
                return v;
        }
        return Optional.empty();
    }

    /** The trait {@code stack} has, if it is a modular item and its variant or type names a known one. */
    public static Optional<Active> of(ItemStack stack) {
        ModularDesign design = ModularStacks.design(stack);
        if (design == null)
            return Optional.empty();
        ModularType type = ModularTypes.known().get(design.type());
        if (type == null)
            return Optional.empty();
        Optional<ModularType.Variant> variant = traitVariant(design, type);
        if (variant.isPresent()) {
            String id = firstId(variant.get().trait()).get();
            return Optional.of(new Active(id, REGISTRY.get(id), new ModularTrait.Params(variant.get().params())));
        }
        if (!type.trait().isEmpty() && REGISTRY.containsKey(type.trait()))
            return Optional.of(new Active(type.trait(), REGISTRY.get(type.trait()), new ModularTrait.Params(type.traitParams())));
        return Optional.empty();
    }

    /**
     * Every trait {@code stack} has, each id once: a gun has several, read off its parts (spec 8.4). In role
     * order with the primary part first, a part's variant may name several ({@code trait} is a comma-separated
     * list) and a fitted option adds its own ({@link io.github.soundgoodizerfan.feedback.weapon.GunOption#trait});
     * the type's own comes last. The first part to name a trait gives the numbers it reads.
     */
    public static List<Active> allOf(ItemStack stack) {
        ModularDesign design = ModularStacks.design(stack);
        ModularType type = design == null ? null : ModularTypes.known().get(design.type());
        return type == null ? List.of() : allOf(design, type);
    }

    private static void add(Map<String, Active> out, String ids, Map<String, Float> params) {
        for (String id : ids.split(","))
            if (REGISTRY.containsKey(id.trim()))
                out.putIfAbsent(id.trim(), new Active(id.trim(), REGISTRY.get(id.trim()), new ModularTrait.Params(params)));
    }

    /** As {@link #allOf(ItemStack)} for a design and its type: what the bake of a gun applies. */
    public static List<Active> allOf(ModularDesign design, ModularType type) {
        Map<String, Active> out = new java.util.LinkedHashMap<>();
        List<String> order = new ArrayList<>(type.order());
        order.remove(type.primary());
        order.add(0, type.primary());
        for (String key : order) {
            ModularDesign.Part part = design.part(key);
            ModularType.Role role = type.roles().get(key);
            if (part == null || role == null)
                continue;
            role.variant(part).ifPresent(v -> add(out, v.trait(), v.params()));
            if (type.family() == ModularType.Family.GUN)
                io.github.soundgoodizerfan.feedback.weapon.GunOptions.ofForm(part.kind()).ifPresent(o -> add(out, o.trait(), o.params()));
        }
        add(out, type.trait(), type.traitParams());
        return List.copyOf(out.values());
    }

    // --- a gun's hooks ----------------------------------------------------------------------------

    public static io.github.soundgoodizerfan.feedback.weapon.GunFigures gunBaked(io.github.soundgoodizerfan.feedback.weapon.GunFigures f,
                                                                                  io.github.soundgoodizerfan.feedback.weapon.GunSpec spec) {
        for (Active a : spec.traits())
            f = a.trait().gunBaked(f, spec, a.params());
        return f;
    }

    public static io.github.soundgoodizerfan.feedback.weapon.GunFigures gunAimed(Player shooter, ItemStack gun,
                                                                                  io.github.soundgoodizerfan.feedback.weapon.GunFigures f) {
        for (Active a : allOf(gun))
            f = a.trait().gunAimed(shooter, gun, f, a.params());
        return f;
    }

    public static void gunFired(net.minecraft.server.level.ServerPlayer shooter, ItemStack gun,
                                io.github.soundgoodizerfan.feedback.weapon.GunFigures f) {
        for (Active a : allOf(gun))
            a.trait().gunFired(shooter, gun, f, a.params());
    }

    public static void gunReloaded(Player shooter, ItemStack gun) {
        for (Active a : allOf(gun))
            a.trait().gunReloaded(shooter, gun, a.params());
    }

    public static void gunRoundLanded(net.minecraft.server.level.ServerLevel level, Player shooter, ItemStack gun,
                                      net.minecraft.world.phys.Vec3 at, net.minecraft.world.entity.Entity hit, float energy) {
        for (Active a : allOf(gun))
            a.trait().gunRoundLanded(level, shooter, gun, at, hit, energy, a.params());
    }

    public static float gunReloadMult(ItemStack gun) {
        float m = 1f;
        for (Active a : allOf(gun))
            m *= a.trait().gunReloadMult(a.params());
        return m;
    }

    /** Whether a round left in the gun's hot chamber may cook off: a closed bolt has one there, an open bolt has none. */
    public static boolean cooksOff(ItemStack gun) {
        return has(gun, "closed_bolt") && !has(gun, "open_bolt");
    }

    /** Whether {@code stack} has the trait {@code id} (a gun's, usually). */
    public static boolean has(ItemStack stack, String id) {
        return allOf(stack).stream().anyMatch(a -> a.id().equals(id));
    }

    // --- item hooks, forwarded by the item classes ---------------------------------------------

    public static InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand, ItemStack stack) {
        return of(stack).map(a -> a.trait().use(level, player, hand, stack, a.params())).orElse(InteractionResultHolder.pass(stack));
    }

    public static int useDuration(ItemStack stack) {
        return of(stack).map(a -> a.trait().useDuration(stack, a.params())).orElse(0);
    }

    public static UseAnim useAnimation(ItemStack stack) {
        return of(stack).map(a -> a.trait().useAnimation(stack, a.params())).orElse(UseAnim.NONE);
    }

    public static void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        of(stack).ifPresent(a -> a.trait().onUseTick(level, user, stack, remaining, a.params()));
    }

    public static void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        of(stack).ifPresent(a -> a.trait().releaseUsing(stack, level, user, remaining, a.params()));
    }

    // --- worn traits ----------------------------------------------------------------------------

    /** A trait on a worn piece, with the numbers its data gives and the slot the piece sits in. */
    public record Worn(String id, ModularTrait trait, ModularTrait.Params params, ItemStack piece, EquipmentSlot slot) {}

    private static final EquipmentSlot[] ARMOUR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /**
     * The traits {@code entity}'s armour has. <b>Stacking: each trait id once</b>, on the first
     * piece (head, chest, legs, feet) that carries it, so four pieces of one trait are never four
     * bonuses; the archetypes are one per slot, so in practice every worn trait is its own.
     */
    public static List<Worn> worn(LivingEntity entity) {
        List<Worn> out = new ArrayList<>(4);
        for (EquipmentSlot slot : ARMOUR) {
            ItemStack piece = entity.getItemBySlot(slot);
            if (!(piece.getItem() instanceof ModularArmor))
                continue;
            Optional<Active> a = of(piece);
            if (a.isEmpty())
                continue;
            String id = idOf(a.get().trait());
            if (id != null && out.stream().noneMatch(w -> w.id().equals(id)))
                out.add(new Worn(id, a.get().trait(), a.get().params(), piece, slot));
        }
        return out;
    }

    private static String idOf(ModularTrait trait) {
        for (Map.Entry<String, ModularTrait> e : REGISTRY.entrySet())
            if (e.getValue() == trait)
                return e.getKey();
        return null;
    }

    /** Trait ids each player wore last tick, to notice one taken off. */
    private static final io.github.soundgoodizerfan.feedback.modular.trait.WielderState<Set<String>> WORN_LAST =
            new io.github.soundgoodizerfan.feedback.modular.trait.WielderState<>();

    // --- events ---------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && !attacker.level().isClientSide
                && event.getSource().getDirectEntity() == attacker) {
            ItemStack weapon = StrikeSource.weapon(attacker, event.getSource());
            of(weapon).ifPresent(a -> a.trait().onAttack(event, attacker, weapon, a.params()));
        }
    }

    @SubscribeEvent
    public static void onWornHurt(LivingIncomingDamageEvent event) {
        LivingEntity wearer = event.getEntity();
        if (wearer.level().isClientSide)
            return;
        for (Worn w : worn(wearer))
            w.trait().onWornHurt(event, wearer, w.piece(), w.params());
    }

    @SubscribeEvent
    public static void onWornFall(LivingFallEvent event) {
        LivingEntity wearer = event.getEntity();
        if (wearer.level().isClientSide)
            return;
        for (Worn w : worn(wearer))
            w.trait().onWornFall(event, wearer, w.piece(), w.params());
    }

    @SubscribeEvent
    public static void onWornKnockback(LivingKnockBackEvent event) {
        LivingEntity wearer = event.getEntity();
        if (wearer.level().isClientSide)
            return;
        for (Worn w : worn(wearer))
            w.trait().onWornKnockback(event, wearer, w.piece(), w.params());
    }

    @SubscribeEvent
    public static void onWornVisibility(LivingEvent.LivingVisibilityEvent event) {
        LivingEntity wearer = event.getEntity();
        if (wearer.level().isClientSide)
            return;
        for (Worn w : worn(wearer))
            w.trait().onWornVisibility(event, wearer, w.piece(), w.params());
    }

    /** Worn traits tick for players only (a mob in modular armour gets the event hooks, not a per-tick one). */
    @SubscribeEvent
    public static void onWornTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide)
            return;
        List<Worn> worn = worn(player);
        Set<String> now = new HashSet<>();
        for (Worn w : worn) {
            now.add(w.id());
            w.trait().onWornTick(player, w.piece(), w.params());
        }
        Set<String> last = WORN_LAST.get(player.getUUID());
        if (last != null)
            for (String id : last)
                if (!now.contains(id))
                    REGISTRY.get(id).onWornRemoved(player);
        if (now.isEmpty())
            WORN_LAST.remove(player.getUUID());
        else
            WORN_LAST.put(player.getUUID(), now);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || event.getAction() != net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START)
            return;
        ItemStack weapon = player.getMainHandItem();
        of(weapon).ifPresent(a -> a.trait().onGroundHit(event, player, weapon, a.params()));
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && !attacker.level().isClientSide) {
            ItemStack weapon = StrikeSource.weapon(attacker, event.getSource());
            of(weapon).ifPresent(a -> a.trait().onKill(event, attacker, weapon, a.params()));
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        ItemStack weapon = event.getPlayer().getMainHandItem();
        of(weapon).ifPresent(a -> a.trait().onBlockBreak(event, weapon, a.params()));
    }
}
