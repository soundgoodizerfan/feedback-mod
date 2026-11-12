// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Optional;

/**
 * The Reloading Press: four slots (case, charge, projectile, and the round that comes out) and
 * a die set, which is the bore it seats for. Pulling the lever {@link #seat} <em>seats whatever
 * is in it</em> -- the press never names a cartridge (machines define operations, not recipes),
 * and never knows when a charge is enough: it seats the count that is in the slot, up to what a
 * case physically takes, and the round's own spec sheet says whether that was wise.
 *
 * <p>What fills a slot is read off the material system: a case is the {@code case} form of a
 * metal, a projectile its {@code bullet} form, a charge the item of any material that has a
 * {@link Material.Propellant}. No item id is named here.
 */
public class ReloadingPressBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int CASE = 0, CHARGE = 1, CHARGE_B = 2, PROJECTILE = 3, CORE = 4, PAYLOAD = 5, OUTPUT = 6;
    public static final int SLOTS = 7;

    /** The perks a payload may carry to a living thing it strikes: what the press accepts besides a tracer, an incendiary or a filler. */
    private static final java.util.Set<Material.Perk> PAYLOAD_PERKS = java.util.EnumSet.of(Material.Perk.TOXIC,
            Material.Perk.CHILL, Material.Perk.CLING, Material.Perk.RADIANT, Material.Perk.BOUNCE, Material.Perk.SMITE,
            Material.Perk.IMPALING, Material.Perk.ECHO, Material.Perk.FIRE_ASPECT, Material.Perk.BLINK, Material.Perk.SIPHON);

    /** The form names a charge or a payload may be in: a dust. A whole rod is not a charge. */
    private static final java.util.Set<String> LOOSE_FORMS = java.util.Set.of("dust");

    private final NonNullList<ItemStack> slots = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private int boreMm = 9;

    public ReloadingPressBlockEntity(BlockPos pos, BlockState state) {
        super(FWeapons.PRESS_ENTITY.get(), pos, state);
    }

    public int boreMm() {
        return boreMm;
    }

    /** What an item is, as a part of a round, in {@code materials}: the material and the form it was found as. */
    public static Optional<Material.Located> located(Map<net.minecraft.resources.ResourceLocation, Material> materials, ItemStack stack) {
        return stack.isEmpty() ? Optional.empty() : Material.find(materials, stack.getItem());
    }

    /** Whether {@code stack} may go in {@code slot}. */
    public static boolean fits(int slot, ItemStack stack, Map<net.minecraft.resources.ResourceLocation, Material> materials) {
        if (slot == OUTPUT)
            return false;
        // A batch from the Mixer is a charge already blended (guns spec 13.4b).
        if ((slot == CHARGE || slot == CHARGE_B) && stack.has(FWeapons.MIXTURE.get()))
            // A fluid part cannot be seated: it is not a powder.
            return !stack.has(FWeapons.MIXTURE_FLUID.get());
        // A seed (anything that places a crop) goes in the payload slot to be planted where the round lands.
        if (slot == PAYLOAD && isSeed(stack))
            return true;
        // A brewed potion goes in the payload slot as a vial: a dud holds nothing.
        if (slot == PAYLOAD && stack.has(FDataComponents.POTION_STATE.get()))
            return !stack.get(FDataComponents.POTION_STATE.get()).dud();
        var found = located(materials, stack);
        if (found.isEmpty())
            return false;
        var material = found.get().material();
        boolean loose = LOOSE_FORMS.contains(found.get().form());
        return switch (slot) {
            case CASE -> found.get().form().equals("case") || found.get().form().equals("paper_case");
            // A bullet seats as a bullet; a nugget is formed into shot by the die (spec 12).
            case PROJECTILE -> BulletShape.ofForm(found.get().form()).isPresent();
            // A core is a small piece of a harder or denser metal set inside the bullet's jacket.
            case CORE -> found.get().form().equals("nugget") && material.density().muPerMb() > 0;
            // A charge is the powder of a propellant: a whole blaze rod is not one.
            case CHARGE, CHARGE_B -> material.propellant().isPresent() && loose;
            // A payload is read by what it is: a luminous powder makes a tracer, a flammable one
            // an incendiary. Nothing here names glowstone or blaze powder.
            // An explosive filler is a propellant that carries its own oxidizer: it goes up where the round stops.
            case PAYLOAD -> blob(material, found.get().form()) || rope(material, found.get().form()) || loose && (material.traits().contains(Material.Trait.LUMINOUS)
                    || material.traits().contains(Material.Trait.FLAMMABLE) || material.propellant().isPresent()
                    || material.perks().keySet().stream().anyMatch(PAYLOAD_PERKS::contains));
            default -> false;
        };
    }

    /** Whether an item places a crop: what a seed round plants. */
    public static boolean isSeed(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.BlockItem block
                && (block.getBlock() instanceof net.minecraft.world.level.block.CropBlock
                || block.getBlock() instanceof net.minecraft.world.level.block.StemBlock);
    }

    /** A sticky lump (slime, magma cream, a daub of mud) a round can carry: it glues what it hits. */
    private static boolean blob(Material material, String form) {
        return material.traits().contains(Material.Trait.STICKY) && (form.equals("ball") || form.equals("raw"));
    }

    /** A strand (string, a thread) a round can trail behind it: it tethers. */
    private static boolean rope(Material material, String form) {
        return material.traits().contains(Material.Trait.FIBROUS) && form.equals("string");
    }

    /**
     * The parts a mixture seats as: the mixture table's answer when its shares fall under a row that resolves
     * into a material, else the parts as mixed. The press never names what a row resolves into.
     */
    public static java.util.List<Cartridge.Charge> partsOf(ItemStack mixture,
            Map<net.minecraft.resources.ResourceLocation, Material> materials, net.minecraft.world.level.Level level) {
        var parts = mixture.get(FWeapons.MIXTURE.get());
        var row = io.github.soundgoodizerfan.feedback.process.Mixture.resolve(
                io.github.soundgoodizerfan.feedback.process.Mixture.table(level), parts,
                io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable.at(level));
        if (row.isPresent() && !row.get().result().isEmpty()) {
            var made = Material.find(materials, row.get().result());
            if (made.isPresent())
                return java.util.List.of(new Cartridge.Charge(made.get().id(), parts.stream().mapToInt(Cartridge.Charge::count).sum()));
        }
        return parts;
    }

    /** The screen's setting, and with {@code seat} the lever. */
    public void commit(int bore, boolean seat, Player player) {
        boreMm = Math.max(GunSpec.MIN_BORE_MM, Math.min(GunSpec.MAX_BORE_MM, bore));
        setChanged();
        if (!seat)
            return;
        var materials = MaterialTable.get().materials();
        if (!slots.get(OUTPUT).isEmpty()) {
            io.github.soundgoodizerfan.feedback.net.AimPayloads.screen(player, Component.translatable("message.feedback.press.output_full"));
            return;
        }
        var cas = located(materials, slots.get(CASE));
        var bullet = located(materials, slots.get(PROJECTILE));
        if (cas.isEmpty() || bullet.isEmpty() || !fits(CASE, slots.get(CASE), materials)
                || !fits(PROJECTILE, slots.get(PROJECTILE), materials)
                || (!slots.get(CHARGE).isEmpty() && !fits(CHARGE, slots.get(CHARGE), materials))
                || (!slots.get(CHARGE_B).isEmpty() && !fits(CHARGE_B, slots.get(CHARGE_B), materials))
                || (!slots.get(CORE).isEmpty() && !fits(CORE, slots.get(CORE), materials))
                || (!slots.get(PAYLOAD).isEmpty() && !fits(PAYLOAD, slots.get(PAYLOAD), materials))) {
            io.github.soundgoodizerfan.feedback.net.AimPayloads.screen(player, Component.translatable("message.feedback.press.missing"));
            return;
        }
        // Two charge slots are the blend: what is in each is seated as is, in the ratio it is in.
        // The press seats the ratio it is given, or a Mixer batch; it never mixes.
        java.util.List<Cartridge.Charge> charges = new java.util.ArrayList<>();
        for (int slot : new int[]{CHARGE, CHARGE_B}) {
            ItemStack stack = slots.get(slot);
            if (stack.isEmpty())
                continue;
            var parts = stack.has(FWeapons.MIXTURE.get()) ? partsOf(stack, materials, level)
                    : java.util.List.of(new Cartridge.Charge(located(materials, stack).get().id(), stack.getCount()));
            for (var part : parts) {
                var existing = charges.stream().filter(c -> c.material().equals(part.material())).findFirst();
                if (existing.isPresent()) {
                    charges.set(charges.indexOf(existing.get()),
                            new Cartridge.Charge(part.material(), existing.get().count() + part.count()));
                } else
                    charges.add(part);
            }
        }
        int n = charges.stream().mapToInt(Cartridge.Charge::count).sum();
        if (n > Cartridge.maxCharge(boreMm)) {
            io.github.soundgoodizerfan.feedback.net.AimPayloads.screen(player, Component.translatable("message.feedback.press.too_much", Cartridge.maxCharge(boreMm)));
            return;
        }
        var vial = java.util.Optional.ofNullable(slots.get(PAYLOAD).get(FDataComponents.POTION_STATE.get()));
        var seed = isSeed(slots.get(PAYLOAD)) ? java.util.Optional.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(slots.get(PAYLOAD).getItem()))
                : java.util.Optional.<net.minecraft.resources.ResourceLocation>empty();
        var payload = slots.get(PAYLOAD).isEmpty() || vial.isPresent() || seed.isPresent() ? java.util.Optional.<net.minecraft.resources.ResourceLocation>empty()
                : java.util.Optional.of(located(materials, slots.get(PAYLOAD)).get().id());
        BulletShape shape = BulletShape.ofForm(bullet.get().form()).orElse(BulletShape.ROUND_NOSE);
        boolean shot = shape.shot();
        // A core sits inside a bullet's jacket; shot has none.
        var core = shot || slots.get(CORE).isEmpty() ? java.util.Optional.<net.minecraft.resources.ResourceLocation>empty()
                : java.util.Optional.of(located(materials, slots.get(CORE)).get().id());
        Cartridge cartridge = new Cartridge(boreMm, cas.get().id(), charges, bullet.get().id(), payload, shot, shape, core, vial, seed);
        if (cartridge.load(materials).isEmpty()) {
            io.github.soundgoodizerfan.feedback.net.AimPayloads.screen(player, Component.translatable("message.feedback.press.no_blend"));
            return;
        }
        ItemStack round = new ItemStack(FWeapons.CARTRIDGE_ITEM.get());
        round.set(FWeapons.CARTRIDGE.get(), cartridge);
        slots.get(CASE).shrink(1);
        slots.set(CHARGE, ItemStack.EMPTY);
        slots.set(CHARGE_B, ItemStack.EMPTY);
        slots.get(PROJECTILE).shrink(1);
        slots.get(CORE).shrink(1);
        slots.get(PAYLOAD).shrink(1);
        slots.set(OUTPUT, round);
        setChanged();
        io.github.soundgoodizerfan.feedback.net.AimPayloads.screen(player, Component.translatable("message.feedback.press.seated"));
    }

    // --- Container -----------------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return slots.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return slots.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack out = ContainerHelper.removeItem(slots, slot, amount);
        setChanged();
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(slots, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        slots.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return fits(slot, stack, MaterialTable.get().materials());
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        slots.clear();
    }

    // --- MenuProvider --------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.feedback.reloading_press");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ReloadingPressMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, slots, registries);
        tag.putInt("Bore", boreMm);

    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        slots.clear();
        ContainerHelper.loadAllItems(tag, slots, registries);
        if (tag.contains("Bore"))
            boreMm = tag.getInt("Bore");
    }
}
