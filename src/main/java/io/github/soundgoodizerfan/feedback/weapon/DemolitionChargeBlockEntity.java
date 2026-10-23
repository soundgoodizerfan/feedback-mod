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

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What is packed into a demolition charge, and the fuse. The filler is a list of propellant
 * materials by count (the same {@link Cartridge.Charge} a case is loaded with), the casing is the
 * metal plate it is wrapped in, if any.
 *
 * <h2>What the blast is</h2>
 * Power is the blend's potential, burned fraction and all, under a square root (a charge twice
 * as big is not twice as strong), times how well the casing holds the gas in until it lets go
 * ({@code 1 + yield strength x FTuning.DEMO_CONFINE_PER_ST}). A fuel-rich blend burns short: less
 * power, and the rest of the fuel goes up as fire instead. Nothing here names gunpowder.
 *
 * <p>Prior art: Create Big Cannons scales its failure explosion by the charge loaded and gives
 * each shell a flat power; this takes the first and rejects the second (MIT code, nothing copied;
 * see THIRD-PARTY-LICENSES.md).
 */
public class DemolitionChargeBlockEntity extends BlockEntity {

    private final List<Cartridge.Charge> filler = new ArrayList<>();
    private ResourceLocation casing;
    /** Ticks of fuse left, or -1 while unlit. */
    private int fuse = -1;

    public DemolitionChargeBlockEntity(BlockPos pos, BlockState state) {
        super(FWeapons.DEMOLITION_CHARGE_ENTITY.get(), pos, state);
    }

    private int packed() {
        return filler.stream().mapToInt(Cartridge.Charge::count).sum();
    }

    /**
     * Pack what is in hand: a powder of a propellant material goes in as filler, a plate of a
     * metal becomes the casing. Whatever fits is taken, nothing else.
     *
     * @return what to tell the player, or null if the item is neither.
     */
    @org.jetbrains.annotations.Nullable
    public Component pack(ItemStack stack, Player player) {
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        var found = ReloadingPressBlockEntity.located(materials, stack);
        if (found.isEmpty())
            return null;
        Material m = found.get().material();
        if (m.propellant().isPresent() && ReloadingPressBlockEntity.fits(ReloadingPressBlockEntity.CHARGE, stack, materials)) {
            if (fuse >= 0)
                return Component.translatable("message.feedback.demolition.lit");
            int room = FTuning.DEMO_FILLER_MAX - packed();
            int taken = Math.min(room, stack.getCount());
            if (taken <= 0)
                return Component.translatable("message.feedback.demolition.full", FTuning.DEMO_FILLER_MAX);
            ResourceLocation id = found.get().id();
            int at = -1;
            for (int i = 0; i < filler.size(); i++)
                if (filler.get(i).material().equals(id))
                    at = i;
            if (at >= 0)
                filler.set(at, new Cartridge.Charge(id, filler.get(at).count() + taken));
            else
                filler.add(new Cartridge.Charge(id, taken));
            if (!player.getAbilities().instabuild)
                stack.shrink(taken);
            setChanged();
            return describe();
        }
        // A casing is a plate of a metal: it holds the gas in, by its yield strength.
        if (found.get().form().equals("plate") && m.mechanical().yieldStrength().isPresent() && GunSpec.buildable(m)) {
            if (casing != null)
                return Component.translatable("message.feedback.demolition.cased");
            casing = found.get().id();
            if (!player.getAbilities().instabuild)
                stack.shrink(1);
            setChanged();
            return describe();
        }
        return null;
    }

    /** Take the filler and casing a charge item was made with: what it was designed as is what is packed. */
    public void pack(ItemStack made) {
        var design = io.github.soundgoodizerfan.feedback.modular.ModularStacks.design(made);
        var type = design == null ? null : io.github.soundgoodizerfan.feedback.modular.ModularTypes.known().get(design.type());
        if (type == null)
            return;
        filler.clear();
        casing = null;
        for (var part : design.parts()) {
            if (part.role().equals(type.wearRole()))
                casing = part.material();
            else
                filler.add(new Cartridge.Charge(part.material(), part.count()));
        }
        setChanged();
    }

    /** What breaking it gives back: the filler as the powder it was, and the casing's plate. Nothing is destroyed but the fuse. */
    public List<ItemStack> contents() {
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        List<ItemStack> out = new ArrayList<>();
        for (Cartridge.Charge c : filler) {
            Material m = materials.get(c.material());
            if (m == null)
                continue;
            var loose = m.items(c.material());
            net.minecraft.world.item.Item item = loose.getOrDefault("powder", loose.get("dust"));
            if (item != null)
                out.add(new ItemStack(item, c.count()));
        }
        if (casing != null && materials.get(casing) != null) {
            net.minecraft.world.item.Item plate = materials.get(casing).items(casing).get("plate");
            if (plate != null)
                out.add(new ItemStack(plate));
        }
        return out;
    }

    /** "12 packed, steel casing" -- free to read, the figures of the charge are what is in it. */
    public Component describe() {
        Component case_ = casing == null ? Component.translatable("message.feedback.demolition.bare")
                : Component.translatable("message.feedback.demolition.casing", Component.translatable("material." + casing.getNamespace() + "." + casing.getPath()));
        return Component.translatable("message.feedback.demolition.contents", packed(), FTuning.DEMO_FILLER_MAX, case_,
                Component.translatable(fuse >= 0 ? "message.feedback.demolition.fuse_lit" : "message.feedback.demolition.fuse_dark"));
    }

    /**
     * Light the fuse.
     *
     * @return whether it took: an empty charge has nothing to light, a lit one is lit already.
     */
    public boolean light(@org.jetbrains.annotations.Nullable Player player) {
        if (level == null || level.isClientSide || fuse >= 0 || packed() == 0)
            return false;
        fuse = FTuning.DEMO_FUSE_TICKS;
        setChanged();
        level.playSound(null, worldPosition, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1f, 1f);
        if (player != null) {
            ItemStack held = player.getMainHandItem().is(io.github.soundgoodizerfan.feedback.registry.FTags.IGNITERS) ? player.getMainHandItem()
                    : player.getOffhandItem();
            if (held.isDamageableItem())
                held.hurtAndBreak(1, player, held == player.getMainHandItem() ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            else if (!player.getAbilities().instabuild)
                held.shrink(1);
        }
        return true;
    }

    public void tick() {
        if (fuse < 0 || !(level instanceof ServerLevel server))
            return;
        if (fuse-- == 0) {
            detonate();
            return;
        }
        server.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5, 1, 0.05, 0.05, 0.05, 0.0);
        if (fuse % 10 == 0)
            server.playSound(null, worldPosition, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 0.5f, 1.4f);
    }

    /** Set it off now. A charge that goes up leaves nothing behind; one with nothing to burn only smokes. */
    public void detonate() {
        if (!(level instanceof ServerLevel server) || level.isClientSide)
            return;
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        BlockPos at = worldPosition;
        Optional<Cartridge.Blend> blend = Cartridge.Blend.of(materials, filler).filter(b -> !b.coil());
        Material cased = casing == null ? null : materials.get(casing);
        float power = blend.map(b -> Blast.power(b, cased)).orElse(0f);
        // What did not burn goes up as fire: a fuel-rich charge is a firebomb, a weak one.
        boolean fire = blend.map(Blast::fire).orElse(false);
        server.removeBlockEntity(at);
        server.removeBlock(at, false);
        if (power < FTuning.DEMO_POWER_MIN) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
            server.playSound(null, at, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1f, 0.8f);
            return;
        }
        server.explode(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, power, fire, Level.ExplosionInteraction.BLOCK);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (Cartridge.Charge c : filler) {
            CompoundTag t = new CompoundTag();
            t.putString("id", c.material().toString());
            t.putInt("n", c.count());
            list.add(t);
        }
        tag.put("Filler", list);
        if (casing != null)
            tag.putString("Casing", casing.toString());
        tag.putInt("Fuse", fuse);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        filler.clear();
        for (Tag t : tag.getList("Filler", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            filler.add(new Cartridge.Charge(ResourceLocation.parse(c.getString("id")), c.getInt("n")));
        }
        casing = tag.contains("Casing") ? ResourceLocation.parse(tag.getString("Casing")) : null;
        fuse = tag.contains("Fuse") ? tag.getInt("Fuse") : -1;
    }
}
