// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.darkcell;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.machine.sculk.SculkSites;
import io.github.soundgoodizerfan.feedback.registry.FAutomation;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The dark cell (spec section 4). The room around the controller is the cell: a flood fill finds its inside, a dark inside is a live
 * cell, and the blocks enclosing it are the shield. State is data: a count of each hostile type, never an entity. {@link #advance}
 * catches the spawning up in one step, so an unloaded cell is current on its first tick back.
 *
 * <h2>Killing</h2>
 * Every cycle each kill method in the wall (a {@link KillMethodBlock} touching the inside) works on the population by what it can harm;
 * drops are rolled from the mob's loot table with no player in the damage, so only drops that need no player appear, and a Catalyst Tap in
 * reach takes the XP of the player lineage at the machine tax. Heat cooks food drops and chars what waits; radiation leaves the drops
 * contaminated.
 *
 * <h2>Shield and breach</h2>
 * Mobs the cell does not kill press on its walls once the population passes half its capacity. The shield is the walls' total hardness; it
 * falls under that pressure and is mended only by hand. At nothing left the stored mobs spawn into the room as real entities, capped.
 */
public class DarkCellBlockEntity extends BlockEntity {

    private static final int OUTPUT_SLOTS = 27;

    private final Map<ResourceLocation, Integer> population = new LinkedHashMap<>();
    private final ItemStackHandler output = new ItemStackHandler(OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private double shield = -1;
    private long lastTime = -1;
    private float spawnOwed;

    // Rebuilt by scan(), never saved.
    private boolean valid;
    private boolean dark;
    private int volume;
    private float wallHardness;
    private final List<BlockPos> interior = new ArrayList<>();
    private final List<BlockPos> methods = new ArrayList<>();
    private long scannedAt = Long.MIN_VALUE;

    public DarkCellBlockEntity(BlockPos pos, BlockState state) {
        super(FAutomation.DARK_CELL_ENTITY.get(), pos, state);
    }

    public IItemHandler outputView() {
        return new net.neoforged.neoforge.items.wrapper.RangedWrapper(output, 0, OUTPUT_SLOTS) {
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return stack;
            }
        };
    }

    // --- scanning -----------------------------------------------------------------------------

    private static boolean open(ServerLevel level, BlockPos p) {
        return level.isLoaded(p) && level.getBlockState(p).getCollisionShape(level, p).isEmpty();
    }

    private void scan(ServerLevel sl) {
        interior.clear();
        methods.clear();
        wallHardness = 0f;
        dark = true;
        Set<BlockPos> seen = new HashSet<>();
        Set<BlockPos> wall = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (Direction d : Direction.values()) {
            BlockPos p = worldPosition.relative(d);
            if (open(sl, p) && seen.add(p))
                queue.add(p);
        }
        valid = !queue.isEmpty();
        while (valid && !queue.isEmpty()) {
            BlockPos cur = queue.poll();
            interior.add(cur);
            if (interior.size() > FTuning.DARKCELL_MAX_VOLUME) {
                valid = false;
                break;
            }
            if (sl.getBrightness(LightLayer.BLOCK, cur) > 0 || sl.getBrightness(LightLayer.SKY, cur) > 0)
                dark = false;
            for (Direction d : Direction.values()) {
                BlockPos n = cur.relative(d);
                if (seen.contains(n))
                    continue;
                if (open(sl, n)) {
                    seen.add(n);
                    queue.add(n);
                } else {
                    wall.add(n);
                }
            }
        }
        wall.remove(worldPosition);
        wall.add(worldPosition);
        if (!valid || interior.size() < 8) {
            valid = false;
            volume = 0;
            return;
        }
        volume = interior.size();
        for (BlockPos w : wall) {
            if (!sl.isLoaded(w))
                continue;
            BlockState state = sl.getBlockState(w);
            float h = state.getDestroySpeed(sl, w);
            wallHardness += h < 0f ? 50f : Math.max(0.5f, h);
            if (state.getBlock() instanceof KillMethodBlock)
                methods.add(w);
        }
        double max = wallHardness * FTuning.DARKCELL_SHIELD_PER_HARDNESS;
        shield = shield < 0 ? max : Math.min(shield, max);
    }

    private double maxShield() {
        return wallHardness * FTuning.DARKCELL_SHIELD_PER_HARDNESS;
    }

    private float capacity() {
        return volume * FTuning.DARKCELL_MASS_PER_BLOCK;
    }

    // --- the clock ----------------------------------------------------------------------------

    public void tickServer() {
        if (!(level instanceof ServerLevel sl))
            return;
        long now = sl.getGameTime();
        if (now - scannedAt >= 100) {
            scan(sl);
            scannedAt = now;
        }
        if (now % 20 == 0) {
            advance(sl, now);
            if (!pushOut(sl))
                setChanged();
        }
        if (now % FTuning.DARKCELL_CYCLE_TICKS == 0 && valid && dark)
            cycle(sl);
    }

    /** Brings spawning and shield pressure up to {@code now} in one step. */
    public void advance(ServerLevel sl, long now) {
        if (lastTime < 0) {
            lastTime = now;
            return;
        }
        long dt = now - lastTime;
        lastTime = now;
        if (!valid || !dark || dt <= 0)
            return;
        spawnOwed += dt / FTuning.DARKCELL_SPAWN_TICKS_PER_100_BLOCKS * (volume / 100f);
        var candidates = spawnTypes(sl);
        float mass = popMass(sl);
        while (spawnOwed >= 1f && !candidates.isEmpty()) {
            spawnOwed -= 1f;
            ResourceLocation id = candidates.get(sl.random.nextInt(candidates.size()));
            float m = MobFacts.of(sl, id).mass();
            if (mass + m > capacity())
                break;
            mass += m;
            population.merge(id, 1, Integer::sum);
            if (sl.dimension() == net.minecraft.world.level.Level.END)
                io.github.soundgoodizerfan.feedback.core.Awards.near(sl, worldPosition, "progress/ender/void_farm");
        }
        // Pressure on the walls: only a crowded cell hurts them.
        float pressure = capacity() <= 0f ? 0f : mass / capacity();
        if (pressure > 0.5f && !population.isEmpty()) {
            float attack = 0f;
            for (var e : population.entrySet())
                attack += e.getValue() * MobFacts.of(sl, e.getKey()).attack();
            shield -= attack * FTuning.DARKCELL_ATTACK_K * (pressure - 0.5f) * 2f * (dt / 20f);
            if (shield <= 0.0) {
                shield = 0;
                breach(sl);
            }
        }
        setChanged();
    }

    private List<ResourceLocation> spawnTypes(ServerLevel sl) {
        String dim = sl.dimension().location().getPath();
        String name = dim.equals("the_nether") ? "nether" : dim.equals("the_end") ? "end" : "overworld";
        TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, Feedback.id("dark_cell/" + name));
        List<ResourceLocation> out = new ArrayList<>();
        BuiltInRegistries.ENTITY_TYPE.getTag(tag).ifPresent(set -> set.forEach(h -> h.unwrapKey().ifPresent(k -> out.add(k.location()))));
        return out;
    }

    private float popMass(ServerLevel sl) {
        float m = 0f;
        for (var e : population.entrySet())
            m += e.getValue() * MobFacts.of(sl, e.getKey()).mass();
        return m;
    }

    /** At nothing left of the shield the stored mobs spawn into the room as real entities, as many as the room has space for, and the rest are lost. */
    private void breach(ServerLevel sl) {
        int room = Math.min(FTuning.BREACH_MAX_MOBS, Math.round(volume / FTuning.BREACH_BLOCKS_PER_MOB));
        List<ResourceLocation> bag = new ArrayList<>();
        population.forEach((id, n) -> {
            for (int i = 0; i < n; i++)
                bag.add(id);
        });
        java.util.Collections.shuffle(bag, new java.util.Random(sl.random.nextLong()));
        for (int i = 0; i < Math.min(room, bag.size()); i++) {
            Entity e = BuiltInRegistries.ENTITY_TYPE.get(bag.get(i)).create(sl);
            if (e == null)
                continue;
            BlockPos at = interior.get(sl.random.nextInt(interior.size()));
            e.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, sl.random.nextFloat() * 360f, 0f);
            if (e instanceof Mob mob)
                mob.finalizeSpawn(sl, sl.getCurrentDifficultyAt(at), MobSpawnType.SPAWNER, null);
            sl.addFreshEntity(e);
        }
        population.clear();
        shield = maxShield() * 0.1;
    }

    // --- killing ------------------------------------------------------------------------------

    private void cycle(ServerLevel sl) {
        if (population.isEmpty() || methods.isEmpty() || outputFull())
            return;
        int drops = 0;
        boolean heating = false;
        for (BlockPos mp : List.copyOf(methods)) {
            BlockState state = sl.getBlockState(mp);
            if (!(state.getBlock() instanceof KillMethodBlock kmb))
                continue;
            KillMethod method = kmb.method(sl, mp, state);
            if (method == null || !method.ready())
                continue;
            if (method.kind() == KillKind.HEAT)
                heating = true;
            int killed = 0;
            float mass = 0f;
            for (ResourceLocation id : List.copyOf(population.keySet())) {
                int have = population.getOrDefault(id, 0);
                MobFacts f = MobFacts.of(sl, id);
                float dmg = method.damage(f);
                if (have <= 0 || dmg <= 0f)
                    continue;
                float share = Math.min(1f, dmg / Math.max(1f, f.health())) * FTuning.DARKCELL_THROUGHPUT;
                int k = (int) Math.floor(have * share);
                if (sl.random.nextFloat() < have * share - k)
                    k++;
                k = Math.min(k, have);
                if (k <= 0)
                    continue;
                if (have - k <= 0)
                    population.remove(id);
                else
                    population.put(id, have - k);
                killed += k;
                mass += k * f.mass();
                drops += process(sl, f, k, method.kind());
                if (drops >= FTuning.DARKCELL_MAX_DROPS_PER_CYCLE)
                    break;
            }
            if (killed > 0)
                method.used(killed, mass);
        }
        if (heating)
            overcook(sl);
        setChanged();
    }

    /** XP to a tap and loot to the output, for {@code kills} deaths by {@code kind}. Returns how many items were made. */
    private int process(ServerLevel sl, MobFacts f, int kills, KillKind kind) {
        if (f.lineage() && f.xp() > 0) {
            float share = FTuning.MACHINE_XP_SHARE * kind.xpShare();
            int points = Math.round(f.xp() * kills * share);
            SculkSites.capture(sl, worldPosition, points);
        }
        var dummy = f.type().create(sl);
        if (!(dummy instanceof LivingEntity living))
            return 0;
        BlockPos at = interior.isEmpty() ? worldPosition : interior.get(0);
        dummy.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        var table = sl.getServer().reloadableRegistries().getLootTable(f.type().getDefaultLootTable());
        LootParams params = new LootParams.Builder(sl)
                .withParameter(LootContextParams.THIS_ENTITY, living)
                .withParameter(LootContextParams.ORIGIN, living.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, sl.damageSources().generic())
                .create(LootContextParamSets.ENTITY);
        int made = 0;
        for (int i = 0; i < kills; i++)
            for (ItemStack drop : table.getRandomItems(params)) {
                ItemStack out = affect(sl, drop, kind, f);
                made += out.getCount();
                ItemStack left = ItemHandlerHelper.insertItem(output, out, false);
                if (!left.isEmpty())
                    return made;
            }
        living.discard();
        return made;
    }

    /** What the kill method does to a drop: heat cooks food, shock scorches what a big body leaves, radiation contaminates. */
    private ItemStack affect(ServerLevel sl, ItemStack drop, KillKind kind, MobFacts f) {
        switch (kind) {
            case HEAT -> {
                var cooked = sl.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(drop), sl);
                if (cooked.isPresent() && cooked.get().value().getResultItem(sl.registryAccess()).has(net.minecraft.core.component.DataComponents.FOOD))
                    return cooked.get().value().getResultItem(sl.registryAccess()).copyWithCount(drop.getCount());
            }
            case SHOCK -> {
                if (f.mass() >= 3f && sl.random.nextBoolean() && !drop.is(FItems.CHARRED_REMAINS.get()))
                    return new ItemStack(FItems.CHARRED_REMAINS.get(), drop.getCount());
            }
            case RADIATION -> drop.set(FDataComponents.CONTAMINATED.get(), true);
            default -> {
            }
        }
        return drop;
    }

    /** A hot plate left to work on what already sits in the output chars the food in it: overrun acts on the finished product. */
    private void overcook(ServerLevel sl) {
        for (int i = 0; i < output.getSlots(); i++) {
            ItemStack s = output.getStackInSlot(i);
            if (!s.isEmpty() && s.has(net.minecraft.core.component.DataComponents.FOOD) && sl.random.nextFloat() < 0.25f)
                output.setStackInSlot(i, new ItemStack(FItems.CHARRED_REMAINS.get(), s.getCount()));
        }
    }

    private boolean outputFull() {
        for (int i = 0; i < output.getSlots(); i++)
            if (output.getStackInSlot(i).isEmpty())
                return false;
        return true;
    }

    /** Hands the output to any inventory touching the controller. Returns whether anything moved. */
    private boolean pushOut(ServerLevel sl) {
        boolean moved = false;
        for (Direction d : Direction.values()) {
            IItemHandler target = sl.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(d), d.getOpposite());
            if (target == null)
                continue;
            for (int i = 0; i < output.getSlots(); i++) {
                ItemStack s = output.getStackInSlot(i);
                if (s.isEmpty())
                    continue;
                ItemStack left = ItemHandlerHelper.insertItem(target, s.copy(), false);
                if (left.getCount() != s.getCount()) {
                    output.setStackInSlot(i, left);
                    moved = true;
                }
            }
        }
        return moved;
    }

    // --- by hand ------------------------------------------------------------------------------

    /** Mends the shield with a wall block's hardness; returns whether the item was spent. */
    public boolean repair(float hardness, Player player) {
        if (hardness <= 0f)
            return false;
        shield = Math.min(maxShield(), Math.max(0, shield) + hardness * FTuning.DARKCELL_REPAIR_PER_HARDNESS);
        setChanged();
        player.displayClientMessage(report(), true);
        return true;
    }

    public Component report() {
        if (!valid)
            return Component.translatable("feedback.darkcell.open");
        if (!dark)
            return Component.translatable("feedback.darkcell.lit");
        int total = population.values().stream().mapToInt(Integer::intValue).sum();
        long pct = maxShield() <= 0 ? 0 : Math.round(100 * Math.max(0, shield) / maxShield());
        return Component.translatable("feedback.darkcell.report", volume, total, pct, methods.size());
    }

    // --- persistence --------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag pop = new ListTag();
        population.forEach((id, n) -> {
            CompoundTag e = new CompoundTag();
            e.putString("Id", id.toString());
            e.putInt("N", n);
            pop.add(e);
        });
        tag.put("Population", pop);
        tag.put("Output", output.serializeNBT(registries));
        tag.putDouble("Shield", shield);
        tag.putLong("LastTime", lastTime);
        tag.putFloat("SpawnOwed", spawnOwed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        population.clear();
        for (var t : tag.getList("Population", 10)) {
            CompoundTag e = (CompoundTag) t;
            population.put(ResourceLocation.parse(e.getString("Id")), e.getInt("N"));
        }
        output.deserializeNBT(registries, tag.getCompound("Output"));
        shield = tag.contains("Shield") ? tag.getDouble("Shield") : -1;
        lastTime = tag.contains("LastTime") ? tag.getLong("LastTime") : -1;
        spawnOwed = tag.getFloat("SpawnOwed");
    }

    public void dropOutput() {
        if (level == null)
            return;
        for (int i = 0; i < output.getSlots(); i++)
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), output.getStackInSlot(i));
    }
}
