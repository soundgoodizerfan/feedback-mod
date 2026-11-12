// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
 * A skeleton that sometimes spawns holding an early gun. The gun is
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
        if (!event.loadedFromDisk() && !(skeleton.getMainHandItem().getItem() instanceof GunItem) && GunSpec.of(skeleton.getMainHandItem()).isEmpty()
                && skeleton.getRandom().nextFloat() < (skeleton instanceof WitherSkeleton ? FTuning.WITHER_SKELETON_GUN_CHANCE : FTuning.SKELETON_GUN_CHANCE)) {
            // A wither skeleton carries a weak pulse pistol; a skeleton keeps its powder gun.
            if (skeleton instanceof WitherSkeleton)
                equipPulse(skeleton);
            else
                equip(skeleton, GunBlueprintPool.get().drawEarly(skeleton.getRandom()));
        }
        if (GunSpec.of(skeleton.getMainHandItem()).isPresent())
            skeleton.goalSelector.addGoal(3, new RangedGunGoal(skeleton));
        else if (skeleton.getMainHandItem().getItem() instanceof io.github.soundgoodizerfan.feedback.modular.ModularElectricGun)
            skeleton.goalSelector.addGoal(3, new PulseGunGoal(skeleton));
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

    /**
     * Arm {@code skeleton} with a weak pulse pistol: the electric gun's own starter design (a handgun with a cylinder seat) at the Light
     * tier. Its cell counts as always charged ({@code ElectricMechanism#triggerMob}), so it carries nothing to spend.
     */
    public static boolean equipPulse(AbstractSkeleton skeleton) {
        var types = ModularTypes.get().types();
        var materials = MaterialTable.get().materials();
        var id = io.github.soundgoodizerfan.feedback.Feedback.id("electric_gun");
        return ToolDesigns.starter(id, types, materials).map(design -> {
            var seat = design.part("s");
            var light = seat == null ? design : design.withPart(seat.withMeasures(seat.measures().withAxis("tier", 1f)));
            ItemStack gun = ToolDesigns.build(light, types, materials).orElse(ItemStack.EMPTY);
            if (gun.isEmpty())
                return false;
            skeleton.setItemSlot(EquipmentSlot.MAINHAND, gun);
            skeleton.setDropChance(EquipmentSlot.MAINHAND, 2f);
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
