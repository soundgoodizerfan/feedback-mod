// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
    /** Ticks the fuse burns once lit; a charge's fuse part sets it. An impact fuse has no burn of its own and uses the standard one when lit by hand. */
    private int fuseLength = GunTuning.DEMO_FUSE_TICKS;
    /** What the charge item this was made from reads as (casing shape, primer, payload), or null for a charge packed by hand. */
    private Blast.Packed made;
    /** Power change from whoever lit it (Salvage: a tamped charge blasts narrower). */
    private float tamp;
    private java.util.UUID lighter;

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
            made = null;
            int room = GunTuning.DEMO_FILLER_MAX - packed();
            int taken = Math.min(room, stack.getCount());
            if (taken <= 0)
                return Component.translatable("message.feedback.demolition.full", GunTuning.DEMO_FILLER_MAX);
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
        var type = design == null ? null : io.github.soundgoodizerfan.feedback.modular.ModularTypes.known(design.type());
        if (type == null)
            return;
        filler.clear();
        casing = null;
        this.made = Blast.packed(made).orElse(null);
        fuseLength = this.made == null || this.made.impact() ? GunTuning.DEMO_FUSE_TICKS : this.made.fuseTicks();
        for (var part : design.parts()) {
            if (part.role().equals(type.wearRole()))
                casing = part.material();
            else if (Blast.isFiller(type, part.role()))
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
            net.minecraft.world.item.Item item = loose.get("dust");
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
        return Component.translatable("message.feedback.demolition.contents", packed(), GunTuning.DEMO_FILLER_MAX, case_,
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
        fuse = fuseLength;
        if (player != null) {
            tamp = io.github.soundgoodizerfan.feedback.combat.skill.Skills.modifier(player, io.github.soundgoodizerfan.feedback.combat.skill.SkillTree.Figure.CHARGE_TAMP);
            lighter = player.getUUID();
        }
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
        Optional<Cartridge.Blend> blend = Cartridge.Blend.of(materials, filler);
        Material cased = casing == null ? null : materials.get(casing);
        float power = made != null ? made.power() : blend.map(b -> Blast.power(b, cased)).orElse(0f);
        // A tamped charge blasts narrower but still goes off if it would have.
        if (power >= FTuning.DEMO_POWER_MIN && tamp != 0f)
            power = Math.max(FTuning.DEMO_POWER_MIN, power * Math.max(0f, 1f + tamp));
        // What did not burn goes up as fire: a fuel-rich charge is a firebomb, a weak one.
        boolean fire = made != null ? made.fire() : blend.map(Blast::fire).orElse(false);
        Blast.Packed shrapnel = made;
        server.removeBlockEntity(at);
        server.removeBlock(at, false);
        if (power < FTuning.DEMO_POWER_MIN) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
            server.playSound(null, at, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1f, 0.8f);
            return;
        }
        server.explode(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, power, fire, Level.ExplosionInteraction.TNT);
        if (shrapnel != null)
            Blast.scatter(server, null, lighter == null ? null : server.getServer().getPlayerList().getPlayer(lighter),
                    net.minecraft.world.phys.Vec3.atCenterOf(at), power, shrapnel);
        if (lighter != null && server.getServer().getPlayerList().getPlayer(lighter) instanceof net.minecraft.server.level.ServerPlayer p)
            io.github.soundgoodizerfan.feedback.combat.skill.SkillEarning.salvaged(p, "charge/" + at.asLong());
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
        tag.putInt("FuseLength", fuseLength);
        if (made != null) {
            tag.putFloat("MadePower", made.power());
            tag.putBoolean("MadeFire", made.fire());
            tag.putFloat("MadeFrag", made.fragDamage());
            tag.putBoolean("MadeFlash", made.flash());
        }
        tag.putFloat("Tamp", tamp);
        if (lighter != null)
            tag.putUUID("Lighter", lighter);
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
        fuseLength = tag.contains("FuseLength") ? tag.getInt("FuseLength") : GunTuning.DEMO_FUSE_TICKS;
        made = tag.contains("MadePower") ? new Blast.Packed(tag.getFloat("MadePower"), tag.getBoolean("MadeFire"), null, fuseLength, false,
                tag.getFloat("MadeFrag"), tag.getBoolean("MadeFlash")) : null;
        tamp = tag.getFloat("Tamp");
        lighter = tag.hasUUID("Lighter") ? tag.getUUID("Lighter") : null;
    }
}
