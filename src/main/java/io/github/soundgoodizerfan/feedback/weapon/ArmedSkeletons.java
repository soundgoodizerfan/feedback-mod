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
package io.github.soundgoodizerfan.feedback.weapon;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.modular.ToolDesigns;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * A skeleton that sometimes spawns holding an early gun ({@code docs/spec_armed_skeletons.md}). The gun is
 * drawn from {@link GunBlueprintPool}, built by the Tool Table's own path ({@link ToolDesigns#build}), loaded
 * to its capacity and given {@link FTuning#SKELETON_GUN_SPARE} more rounds. Entity type is the only gate, as
 * vanilla gates a bow; which guns qualify is {@link GunBlueprintPool#early}. Nothing is infinite: the rounds
 * are spent for real, and what is left drops with it.
 */
public final class ArmedSkeletons {

    private ArmedSkeletons() {
    }

    /** Loose rounds carried beyond the gun's load, as the cartridge items they drop as. */
    public record Spares(List<ItemStack> stacks) {
        public static final Spares NONE = new Spares(List.of());
        public static final com.mojang.serialization.Codec<Spares> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.CODEC.listOf().fieldOf("stacks").forGetter(Spares::stacks)).apply(i, Spares::new));

        public Spares {
            stacks = List.copyOf(stacks);
        }

        public int count() {
            return stacks.stream().mapToInt(ItemStack::getCount).sum();
        }
    }

    public static int spares(AbstractSkeleton mob) {
        return mob.getData(FWeapons.SPARES).count();
    }

    /** One spare round out of the mob's carry, or null if it has none. */
    public static Cartridge takeSpare(AbstractSkeleton mob) {
        List<ItemStack> left = new ArrayList<>();
        Cartridge taken = null;
        for (ItemStack s : mob.getData(FWeapons.SPARES).stacks()) {
            ItemStack copy = s.copy();
            if (taken == null && copy.has(FWeapons.CARTRIDGE.get())) {
                taken = copy.get(FWeapons.CARTRIDGE.get());
                copy.shrink(1);
            }
            if (!copy.isEmpty())
                left.add(copy);
        }
        mob.setData(FWeapons.SPARES, new Spares(left));
        return taken;
    }

    /** A skeleton entering the world: a newly made one may be armed; one holding a gun, new or loaded back, gets the goal. */
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof AbstractSkeleton skeleton))
            return;
        if (!event.loadedFromDisk() && GunSpec.of(skeleton.getMainHandItem()).isEmpty() && skeleton.getRandom().nextFloat() < (skeleton instanceof WitherSkeleton ? FTuning.WITHER_SKELETON_GUN_CHANCE : FTuning.SKELETON_GUN_CHANCE))
            equip(skeleton, GunBlueprintPool.get().drawEarly(skeleton.getRandom()));
        if (GunSpec.of(skeleton.getMainHandItem()).isPresent())
            skeleton.goalSelector.addGoal(3, new RangedGunGoal(skeleton));
    }

    /** Arm {@code skeleton} with {@code entry} (no chance roll): the spawn event and {@code /feedback summon gunskeleton} share this. */
    public static boolean equip(AbstractSkeleton skeleton, java.util.Optional<GunBlueprintPool.Entry> picked) {
        var types = ModularTypes.get().types();
        var materials = MaterialTable.get().materials();
        return picked.map(entry -> {
            ItemStack gun = ToolDesigns.build(entry.design(), types, materials).orElse(ItemStack.EMPTY);
            Cartridge round = entry.round().orElseThrow();
            if (gun.isEmpty())
                return false;
            Magazine loaded = Magazine.EMPTY;
            for (int i = 0; i < GunAmmo.capacity(gun); i++)
                loaded = loaded.push(round);
            gun.set(FWeapons.MAGAZINE.get(), loaded);
            GunAmmo.feed(gun);
            skeleton.setItemSlot(EquipmentSlot.MAINHAND, gun);
            // More than 1 is vanilla's "always, and undamaged": the gun comes off at the condition it died in.
            skeleton.setDropChance(EquipmentSlot.MAINHAND, 2f);
            skeleton.setData(FWeapons.SPARES, new Spares(List.of(GunAmmo.loose(round).copyWithCount(FTuning.SKELETON_GUN_SPARE))));
            return true;
        }).orElse(false);
    }

    /** What it still carried falls with it. */
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof AbstractSkeleton skeleton) || skeleton.level().isClientSide || !skeleton.hasData(FWeapons.SPARES))
            return;
        for (ItemStack s : skeleton.getData(FWeapons.SPARES).stacks())
            event.getDrops().add(new ItemEntity(skeleton.level(), skeleton.getX(), skeleton.getY(), skeleton.getZ(), s.copy()));
        skeleton.setData(FWeapons.SPARES, Spares.NONE);
    }
}
