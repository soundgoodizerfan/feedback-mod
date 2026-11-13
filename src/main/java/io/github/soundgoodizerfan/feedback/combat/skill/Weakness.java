// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.skill;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.core.hazard.Hazards;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Observation's weakness skills (weapon-independent: they read the target, never the weapon). {@code size_up} reads the
 * creature under the crosshair for {@link #READ_TICKS}; while it is read, a blow from this player by any weapon
 * (melee, bullet, arrow) is scaled by what the target gives away:
 * <ul>
 * <li>{@code hardness_gap}: the weapon's edge hardness over the hardness of the armour it wears.</li>
 * <li>{@code opening}: facing away, unaware, burning, wet, staggered; {@code tell} adds a creature that has just swung,
 * {@code culling} one that is nearly dead.</li>
 * <li>{@code edge}: a drop, lava, fire or a cactus close behind it; the blow also shoves it that way.</li>
 * <li>{@code quench}: a hot weapon on a wet creature, which the steam cools.</li>
 * <li>{@code pressure_point}: a blow on a staggered or recovering creature staggers it by mass ratio.</li>
 * <li>{@code soft_spot} (cast) and {@code exploit} (cast): one worn piece, or the whole read, spent on the next blow.</li>
 * <li>{@code conductor}: shock hurts a read creature that is wet or in heavy armour more.</li>
 * </ul>
 * Standing knowledge: {@code case_file} (+ per kill of the type), {@code pattern} (a known type is read twice as long and
 * shows what it hits for), {@code herd_read}, {@code read_the_room} (a read player's gun state) and {@code spoor} (cast:
 * follow a read creature that left sight). STAND-IN figures, to tune in play.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Weakness {

    /** Thread the gap: ticks of stagger a fully precise weapon's soft spot blow adds ([OPEN]). */
    static final int THREAD_TICKS = 20;
    static final int READ_TICKS = 300, ARMED_TICKS = 60, RECOVER_TICKS = 12, SPOOR_TICKS = 400, KNOWN_KILLS = 10;
    static final double REACH = 24, HERD = 6, SPOOR_RANGE = 64;
    /** Shares added to a blow. */
    static final float GAP_PER_POINT = 0.03f, GAP_CAP = 0.4f, OPENING_CAP = 0.8f, EXPLOIT = 0.5f, FILE_PER_KILL = 0.01f, FILE_CAP = 0.2f,
            EDGE = 0.25f, EDGE_SHOVE = 0.5f, QUENCH = 0.4f, QUENCH_COOL_TU = 80f, QUENCH_REF_TU = 300f, SPOT_PER_POINT = 0.04f, SPOT_CAP = 0.5f,
            CONDUCT = 0.5f, HEAVY_ARMOUR = 10f, WOUNDED = 0.3f;
    /** Hardness stand-ins: an unknown weapon, a bare hand, and a vanilla piece's hardness per point of defence. */
    /** Right tool: the share a blow of a class the creature is weak to adds ([OPEN]). */
    static final float RIGHT_TOOL = 0.3f;
    static final float WEAPON_DEFAULT = 4.5f, WEAPON_BARE = 1f, PIECE_PER_DEFENCE = 0.75f;
    /** Joules a point of blow damage carries into a pressure point's stagger. */
    static final float PRESSURE_JOULES_PER_DAMAGE = 20f;

    /** The words {@link #openings} and {@link #gunWords} use, in bit order for {@code SizeUpPayload}. */
    public static final List<String> OPENING_WORDS = List.of("facing away", "unaware", "burning", "wet", "staggered", "just swung",
            "nearly dead", "gun empty", "one round left", "barrel hot", "weak to blunt", "weak to pierce", "weak to slash",
            "weak to fire", "weak to shock");

    private static final Map<UUID, Map<UUID, Long>> READ = new HashMap<>();
    private static final Map<UUID, Long> ARMED = new HashMap<>();
    private static final Map<UUID, Object[]> SPOT = new HashMap<>();
    private static final Map<UUID, Long> RECOVER = new HashMap<>();
    private static final Map<UUID, UUID> LAST = new HashMap<>();
    private static final Map<UUID, Object[]> SPOOR = new HashMap<>();

    private Weakness() {
    }

    private static boolean reading(ServerPlayer p, LivingEntity t) {
        Map<UUID, Long> mine = READ.get(p.getUUID());
        Long until = mine == null ? null : mine.get(t.getUUID());
        return until != null && until > p.level().getGameTime();
    }

    /** Whether {@code p} has {@code t} under a Size up read. */
    public static boolean isRead(ServerPlayer p, LivingEntity t) {
        return reading(p, t);
    }

    /** Whether {@code t} has not noticed anyone: asleep, or a mob with nothing targeted. */
    public static boolean unaware(LivingEntity t) {
        return t.isSleeping() || t instanceof Mob m && m.getTarget() == null;
    }

    /** Whether {@code t} faces away from {@code p}. */
    public static boolean backTurned(ServerPlayer p, LivingEntity t) {
        return t.getLookAngle().dot(p.position().subtract(t.position())) < 0;
    }

    private static LivingEntity look(ServerPlayer p) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        LivingEntity best = null;
        double bestD = REACH * REACH;
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(REACH), e -> e != p && e.isAlive())) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.lengthSqr();
            if (d < bestD && to.normalize().dot(look) > 0.97 && p.hasLineOfSight(e)) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    // --- what a target gives away ------------------------------------------------------------

    /** Armour defence and toughness one worn piece gives in its slot. */
    private static float[] pieceArmour(ItemStack piece, EquipmentSlot slot) {
        float[] out = new float[2];
        piece.forEachModifier(slot, (attr, mod) -> {
            if (attr.equals(Attributes.ARMOR))
                out[0] += (float) mod.amount();
            else if (attr.equals(Attributes.ARMOR_TOUGHNESS))
                out[1] += (float) mod.amount();
        });
        return out;
    }

    private static float pieceHardness(ItemStack piece, EquipmentSlot slot) {
        ModularFigures f = ModularStacks.figures(piece);
        if (f != null && f.get(ModularFigures.Fig.HARDNESS) > 0f)
            return f.get(ModularFigures.Fig.HARDNESS);
        return pieceArmour(piece, slot)[0] * PIECE_PER_DEFENCE;
    }

    /** The edge hardness of what {@code p} is holding, or a stand-in for a bare hand or a gun. */
    private static float weaponHardness(ServerPlayer p) {
        ItemStack held = p.getMainHandItem();
        if (held.isEmpty())
            return WEAPON_BARE;
        ModularFigures f = ModularStacks.figures(held);
        return f != null && f.get(ModularFigures.Fig.HARDNESS) > 0f ? f.get(ModularFigures.Fig.HARDNESS) : WEAPON_DEFAULT;
    }

    /** Hardness gap: the weapon's hardness over the mean hardness of the pieces worn, as a share; none if nothing is worn. */
    private static float gap(ServerPlayer p, LivingEntity t) {
        float sum = 0;
        int n = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR)
                continue;
            ItemStack piece = t.getItemBySlot(slot);
            if (!piece.isEmpty()) {
                sum += pieceHardness(piece, slot);
                n++;
            }
        }
        return n == 0 ? 0f : Math.max(0f, Math.min(GAP_CAP, (weaponHardness(p) - sum / n) * GAP_PER_POINT));
    }

    /** The weapon's hardness less the mean of the worn pieces' (negative: the wrong tool for that armour); 0 if nothing is worn. */
    private static float rawGap(ServerPlayer p, LivingEntity t) {
        float sum = 0;
        int n = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR)
                continue;
            ItemStack piece = t.getItemBySlot(slot);
            if (!piece.isEmpty()) {
                sum += pieceHardness(piece, slot);
                n++;
            }
        }
        return n == 0 ? 0f : weaponHardness(p) - sum / n;
    }

    /** The classes {@code t} is weak to, from its state and the hardness of what it wears. */
    static java.util.Set<DamageClass.Kind> weaknesses(LivingEntity t) {
        float sum = 0;
        int n = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR)
                continue;
            ItemStack piece = t.getItemBySlot(slot);
            if (!piece.isEmpty()) {
                sum += pieceHardness(piece, slot);
                n++;
            }
        }
        return DamageClass.weakTo(t, n == 0 ? 0f : sum / n, n);
    }

    private static boolean recovering(LivingEntity t) {
        Long until = RECOVER.get(t.getUUID());
        return until != null && until > t.level().getGameTime();
    }

    /** What the target gives away right now: words, and the share they add (capped) in {@code share[0]}. */
    static List<String> openings(ServerPlayer p, LivingEntity t, float[] share) {
        List<String> words = new ArrayList<>();
        float sum = 0;
        if (t.getLookAngle().dot(p.position().subtract(t.position())) < 0) {
            words.add("facing away");
            sum += 0.25f;
        }
        if (t.isSleeping() || t instanceof Mob m && m.getTarget() == null) {
            words.add("unaware");
            sum += 0.25f;
        }
        if (t.isOnFire()) {
            words.add("burning");
            sum += 0.2f;
        }
        if (t.isInWaterOrRain()) {
            words.add("wet");
            sum += 0.1f;
        }
        if (CombatState.of(t).phase() == CombatPhase.STAGGER) {
            words.add("staggered");
            sum += 0.3f;
        }
        if (Skills.unlocked(p, "tell") && recovering(t)) {
            words.add("just swung");
            sum += 0.3f;
        }
        if (Skills.unlocked(p, "culling") && t.getHealth() < t.getMaxHealth() * WOUNDED) {
            words.add("nearly dead");
            sum += 0.2f;
        }
        share[0] = Math.min(OPENING_CAP, sum);
        return words;
    }

    /** Whether something that hurts lies one or two blocks behind {@code t} from {@code p}: a drop, lava, fire, a cactus. */
    private static boolean edge(LivingEntity t, ServerPlayer p) {
        Vec3 away = t.position().subtract(p.position()).multiply(1, 0, 1);
        if (away.lengthSqr() < 1.0E-4)
            return false;
        away = away.normalize();
        Level l = t.level();
        for (int k = 1; k <= 2; k++) {
            BlockPos at = BlockPos.containing(t.getX() + away.x * k, t.getY(), t.getZ() + away.z * k);
            BlockState s = l.getBlockState(at);
            if (s.is(io.github.soundgoodizerfan.feedback.registry.FTags.HURTS_ON_CONTACT) || s.is(net.minecraft.tags.BlockTags.FIRE) || s.is(io.github.soundgoodizerfan.feedback.registry.FTags.HOT_SURFACE) || l.getFluidState(at).is(FluidTags.LAVA))
                return true;
            if (!s.getCollisionShape(l, at).isEmpty())
                continue;
            boolean ground = false;
            for (int d = 1; d <= 3 && !ground; d++)
                ground = !l.getBlockState(at.below(d)).getCollisionShape(l, at.below(d)).isEmpty();
            if (!ground)
                return true;
        }
        return false;
    }

    private static int known(ServerPlayer p, LivingEntity t) {
        return p.getPersistentData().getCompound("feedback_case_file").getInt(t.getType().builtInRegistryHolder().key().location().toString());
    }

    // --- casts -----------------------------------------------------------------------------------

    private static void read(ServerPlayer p, LivingEntity t) {
        int ticks = READ_TICKS * (Skills.unlocked(p, "pattern") && known(p, t) >= KNOWN_KILLS ? 2 : 1);
        READ.computeIfAbsent(p.getUUID(), k -> new HashMap<>()).put(t.getUUID(), p.level().getGameTime() + ticks);
        LAST.put(p.getUUID(), t.getUUID());
    }

    /** Size up: read the creature under the crosshair (twenty-four blocks, in sight) and say what it gives away. */
    public static boolean sizeUp(ServerPlayer p) {
        LivingEntity t = look(p);
        if (t == null)
            return false;
        read(p, t);
        io.github.soundgoodizerfan.feedback.core.EventTrigger.fire(p, "progress/combat/size_up");
        if (Skills.unlocked(p, "herd_read"))
            for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(HERD), e -> e != p && e != t && e.isAlive() && e.getType() == t.getType()))
                read(p, e);
        push(p);
        ServerLevel level = p.serverLevel();
        level.playSound(null, p.getX(), p.getEyeY(), p.getZ(), SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 0.8f, 1.3f);
        level.sendParticles(ParticleTypes.ENCHANT, t.getX(), t.getY() + t.getBbHeight() / 2, t.getZ(), 14, 0.3, 0.4, 0.3, 0.6);
        return true;
    }

    private static final java.util.Set<UUID> SENT = new java.util.HashSet<>();

    /** The openings of {@code t} for {@code p} as the bit set {@code SizeUpPayload} carries. */
    private static int maskOf(ServerPlayer p, LivingEntity t) {
        List<String> words = openings(p, t, new float[1]);
        if (t instanceof Player other && Skills.unlocked(p, "read_the_room"))
            words.addAll(gunWords(other));
        if (Skills.unlocked(p, "right_tool"))
            for (DamageClass.Kind k : weaknesses(t))
                words.add("weak to " + k.name().toLowerCase(java.util.Locale.ROOT));
        int mask = 0;
        for (String w : words)
            mask |= 1 << OPENING_WORDS.indexOf(w);
        return mask;
    }

    /** Tell {@code p}'s HUD every creature still read, nearest first, eight at most; an empty list once when the last is gone. */
    private static void push(ServerPlayer p) {
        Map<UUID, Long> mine = READ.get(p.getUUID());
        long now = p.level().getGameTime();
        List<LivingEntity> live = new ArrayList<>();
        if (mine != null) {
            mine.values().removeIf(u -> u <= now);
            for (UUID id : mine.keySet())
                if (p.serverLevel().getEntity(id) instanceof LivingEntity t && t.isAlive() && t.distanceTo(p) <= SPOOR_RANGE)
                    live.add(t);
        }
        if (live.isEmpty() && !SENT.remove(p.getUUID()))
            return;
        live.sort(java.util.Comparator.comparingDouble(t -> t.distanceToSqr(p)));
        Long armed = ARMED.get(p.getUUID());
        Object[] spot = SPOT.get(p.getUUID());
        List<io.github.soundgoodizerfan.feedback.net.SizeUpPayload.Entry> out = new ArrayList<>();
        for (LivingEntity t : live.subList(0, Math.min(8, live.size()))) {
            int flags = armed != null && armed > now ? 1 : 0;
            if (spot != null && spot[0].equals(t.getUUID()) && (Long) spot[2] > now)
                flags |= ((Integer) spot[3] + 1) << 1;
            boolean known = Skills.unlocked(p, "pattern") && known(p, t) >= KNOWN_KILLS;
            if (known)
                flags |= 16;
            out.add(new io.github.soundgoodizerfan.feedback.net.SizeUpPayload.Entry(t.getId(), (int) Math.max(0, mine.get(t.getUUID()) - now),
                    maskOf(p, t), flags, known ? Math.round((float) t.getAttributeValue(Attributes.ATTACK_DAMAGE)) : 0));
        }
        if (!out.isEmpty())
            SENT.add(p.getUUID());
        io.github.soundgoodizerfan.feedback.net.SizeUpPayload.send(p, out);
    }

    /** A read player's gun, in words: what a trained eye sees of a barrel and a magazine. */
    private static List<String> gunWords(Player other) {
        List<String> words = new ArrayList<>();
        ItemStack gun = other.getMainHandItem();
        if (!(gun.getItem() instanceof GunItem))
            return words;
        int rounds = GunAmmo.count(gun);
        if (rounds == 0)
            words.add("gun empty");
        else if (rounds == 1)
            words.add("one round left");
        io.github.soundgoodizerfan.feedback.weapon.GunSpec.of(gun).ifPresent(spec -> {
            if (GunCondition.temperature(gun, other.level()).value() > ThermalTuning.WARM_TU.value() * 4)
                words.add("barrel hot");
        });
        return words;
    }

    /** Exploit: spend the read on one large blow; the next hit within three seconds on that creature lands it. */
    public static boolean exploit(ServerPlayer p) {
        LivingEntity t = look(p);
        if (t == null || !reading(p, t))
            return false;
        ARMED.put(p.getUUID(), p.level().getGameTime() + ARMED_TICKS);
        p.serverLevel().playSound(null, p.getX(), p.getEyeY(), p.getZ(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.7f, 1.6f);
        push(p);
        return true;
    }

    /** Soft spot: pick the worn piece under the crosshair (head, chest, legs or feet by height); the next blow on that creature ignores it. */
    public static boolean softSpot(ServerPlayer p) {
        LivingEntity t = look(p);
        if (t == null || !reading(p, t))
            return false;
        Vec3 eye = p.getEyePosition(), dir = p.getLookAngle();
        double y = eye.y + dir.y * eye.distanceTo(t.getBoundingBox().getCenter());
        double frac = (y - t.getY()) / Math.max(0.1, t.getBbHeight());
        EquipmentSlot slot = frac > 0.8 ? EquipmentSlot.HEAD : frac > 0.5 ? EquipmentSlot.CHEST : frac > 0.25 ? EquipmentSlot.LEGS : EquipmentSlot.FEET;
        ItemStack piece = t.getItemBySlot(slot);
        float[] a = pieceArmour(piece, slot);
        if (piece.isEmpty() || a[0] + a[1] <= 0f) {
            io.github.soundgoodizerfan.feedback.net.AimPayloads.cue(p, io.github.soundgoodizerfan.feedback.net.AimPayloads.CROSSHAIR, Component.translatable("message.feedback.soft_spot.bare", Component.translatable("slot.feedback." + slot.getName())), 30);
            return false;
        }
        SPOT.put(p.getUUID(), new Object[] {t.getUUID(), Math.min(SPOT_CAP, (a[0] + a[1]) * SPOT_PER_POINT), p.level().getGameTime() + ARMED_TICKS,
                slot == EquipmentSlot.HEAD ? 0 : slot == EquipmentSlot.CHEST ? 1 : slot == EquipmentSlot.LEGS ? 2 : 3});
        io.github.soundgoodizerfan.feedback.net.AimPayloads.cue(p, io.github.soundgoodizerfan.feedback.net.AimPayloads.CROSSHAIR, Component.translatable("message.feedback.soft_spot", Component.translatable("slot.feedback." + slot.getName()), piece.getHoverName()), 30);
        p.serverLevel().playSound(null, p.getX(), p.getEyeY(), p.getZ(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.8f, 1.5f);
        push(p);
        return true;
    }

    /** Spoor: follow the last creature read, even out of sight, as a trail only you can see for twenty seconds. */
    public static boolean spoor(ServerPlayer p) {
        UUID id = LAST.get(p.getUUID());
        var found = id == null ? null : p.serverLevel().getEntity(id);
        if (!(found instanceof LivingEntity t) || !t.isAlive() || t.distanceTo(p) > SPOOR_RANGE)
            return false;
        SPOOR.put(p.getUUID(), new Object[] {id, p.level().getGameTime() + SPOOR_TICKS});
        p.serverLevel().playSound(null, p.getX(), p.getEyeY(), p.getZ(), SoundEvents.FOX_SNIFF, SoundSource.PLAYERS, 0.8f, 0.9f);
        return true;
    }

    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p))
            return;
        if (p.tickCount % 5 == 0 && (READ.containsKey(p.getUUID()) || SENT.contains(p.getUUID())))
            push(p);
        if (SPOOR.isEmpty() || p.tickCount % 4 != 0)
            return;
        Object[] s = SPOOR.get(p.getUUID());
        if (s == null)
            return;
        var found = p.serverLevel().getEntity((UUID) s[0]);
        if (!(found instanceof LivingEntity t) || !t.isAlive() || p.level().getGameTime() > (Long) s[1]) {
            SPOOR.remove(p.getUUID());
            return;
        }
        p.serverLevel().sendParticles(p, ParticleTypes.GLOW, true, t.getX(), t.getY() + 0.2, t.getZ(), 3, 0.2, 0.1, 0.2, 0.01);
        p.serverLevel().sendParticles(p, ParticleTypes.END_ROD, true, t.getX(), t.getY() + t.getBbHeight() + 0.5, t.getZ(), 1, 0, 0.3, 0, 0.02);
    }

    // --- riders ----------------------------------------------------------------------------------------

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGH)
    static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity t = event.getEntity();
        // A creature that swings is, for a moment, open.
        if (event.getSource().getEntity() instanceof LivingEntity swinger && !(swinger instanceof Player))
            RECOVER.put(swinger.getUUID(), swinger.level().getGameTime() + RECOVER_TICKS);
        // Conductor: shock finds a wet or heavily armoured read creature more easily.
        if (event.getSource().is(Hazards.SHOCK)) {
            for (var e : READ.entrySet()) {
                Long until = e.getValue().get(t.getUUID());
                var who = t.level().getServer() == null ? null : t.level().getServer().getPlayerList().getPlayer(e.getKey());
                if (until != null && until > t.level().getGameTime() && who != null && Skills.unlocked(who, "conductor")
                        && (t.isInWaterOrRain() || t.getArmorValue() >= HEAVY_ARMOUR)) {
                    event.setAmount(event.getAmount() * (1 + CONDUCT + (Skills.unlocked(who, "right_tool") && t.isInWaterOrRain() ? RIGHT_TOOL : 0f)));
                    break;
                }
            }
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer p))
            return;
        float mult = 1f;
        float raw = rawGap(p, t);
        if (raw < 0f)
            io.github.soundgoodizerfan.feedback.core.EventTrigger.fire(p, "progress/combat/wrong_tool");
        boolean read = reading(p, t);
        long now = p.level().getGameTime();
        if (read) {
            if (Skills.unlocked(p, "hardness_gap")) {
                mult += gap(p, t);
                if (gap(p, t) > 0f)
                    io.github.soundgoodizerfan.feedback.core.EventTrigger.fire(p, "progress/combat/right_tool");
            }
            float[] share = new float[1];
            List<String> words = openings(p, t, share);
            if (Skills.unlocked(p, "opening"))
                mult += share[0];
            Object[] spot = SPOT.get(p.getUUID());
            if (spot != null && spot[0].equals(t.getUUID()) && (Long) spot[2] > now && Skills.unlocked(p, "soft_spot")) {
                mult += (Float) spot[1];
                SPOT.remove(p.getUUID());
                // Thread the gap: a precise weapon's soft spot blow also staggers, longer the more precise it is.
                float precision = io.github.soundgoodizerfan.feedback.combat.hit.WeaponProfile.meleeOf(p).precision();
                if (Skills.unlocked(p, "thread_the_gap") && precision >= Tipping.MIN_PRECISION)
                    CombatTicker.stagger(t, Math.round(THREAD_TICKS * precision), ParryTuning.STAGGER_SLOW);
                io.github.soundgoodizerfan.feedback.core.EventTrigger.fire(p, "progress/combat/soft_spot");
            }
            Long armed = ARMED.get(p.getUUID());
            if (armed != null && armed > now && Skills.unlocked(p, "exploit")) {
                mult += EXPLOIT + (Skills.unlocked(p, "opening") ? share[0] : 0f);
                ARMED.remove(p.getUUID());
                io.github.soundgoodizerfan.feedback.core.EventTrigger.fire(p, "progress/combat/exploit");
                READ.getOrDefault(p.getUUID(), Map.of()).remove(t.getUUID());
                p.serverLevel().sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + t.getBbHeight() / 2, t.getZ(), 18, 0.3, 0.4, 0.3, 0.5);
            }
            if (Skills.unlocked(p, "right_tool")) {
                DamageClass.Kind kind = DamageClass.of(event.getSource(), p);
                if (kind != null && weaknesses(t).contains(kind)) {
                    mult += RIGHT_TOOL;
                    io.github.soundgoodizerfan.feedback.core.EventTrigger.fire(p, "progress/combat/right_tool");
                    p.serverLevel().sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + t.getBbHeight() / 2, t.getZ(), 6, 0.25, 0.3, 0.25, 0.3);
                }
            }
            if (Skills.unlocked(p, "edge") && edge(t, p)) {
                mult += EDGE;
                Vec3 away = t.position().subtract(p.position()).multiply(1, 0, 1);
                if (away.lengthSqr() > 1.0E-4) {
                    t.setDeltaMovement(t.getDeltaMovement().add(away.normalize().scale(EDGE_SHOVE)).add(0, 0.1, 0));
                    t.hurtMarked = true;
                }
            }
            if (Skills.unlocked(p, "quench") && t.isInWaterOrRain()) {
                ItemStack held = p.getMainHandItem();
                float temp = ItemHeat.felt(held, p.level()).value() - ThermalTuning.WARM_TU.value();
                if (temp > 0f) {
                    mult += QUENCH * Math.min(1f, temp / QUENCH_REF_TU);
                    ItemHeat.drain(held, p.level(), QUENCH_COOL_TU);
                    p.serverLevel().sendParticles(ParticleTypes.CLOUD, t.getX(), t.getY() + t.getBbHeight() / 2, t.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                    p.serverLevel().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 1.2f);
                }
            }
            if (Skills.unlocked(p, "pressure_point") && (words.contains("staggered") || words.contains("just swung"))) {
                float mass = BodyMass.of(t) + BodyMass.armour(t);
                int ticks = ParryTuning.staggerTicks(event.getAmount() * mult * PRESSURE_JOULES_PER_DAMAGE, mass);
                if (ticks > 0) {
                    CombatTicker.stagger(t, ticks, ParryTuning.STAGGER_SLOW);
                    Rearms.on(p, SkillTree.Rearm.STAGGER);
                }
            }
        }
        if (Skills.unlocked(p, "case_file"))
            mult += Math.min(FILE_CAP, known(p, t) * FILE_PER_KILL);
        if (mult > 1f) {
            event.setAmount(event.getAmount() * mult);
            if (read)
                SkillEarning.award(p, SkillEarning.OBSERVATION, t.getStringUUID(), SkillTuning.GUN_HIT);
        }
    }

    @SubscribeEvent
    static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || !Skills.unlocked(p, "case_file"))
            return;
        CompoundTag file = p.getPersistentData().getCompound("feedback_case_file");
        String type = event.getEntity().getType().builtInRegistryHolder().key().location().toString();
        file.putInt(type, file.getInt(type) + 1);
        p.getPersistentData().put("feedback_case_file", file);
    }
}
