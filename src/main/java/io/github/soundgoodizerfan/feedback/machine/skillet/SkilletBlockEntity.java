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
package io.github.soundgoodizerfan.feedback.machine.skillet;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.process.Cooking;
import io.github.soundgoodizerfan.feedback.process.FoodState;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * A pan with up to four pieces laid in it. The pan has a temperature of its own that follows the fire below (or the
 * room, while a spatula holds it up) at a rate set by its material; each piece's heat follows the pan's, lagging by its
 * mass, and runs the same {@link Cooking} ladder a vessel does. The stack's {@code charred} is the face that is down; the
 * other face's char is kept here beside it and swapped by a flip.
 *
 * <p>An empty pan does nothing: its temperature is settled from the time since it was last awake when something is laid in.
 */
public class SkilletBlockEntity extends BlockEntity {

    public static final int SLOTS = 4;

    private final ItemStack[] pieces = new ItemStack[SLOTS];
    private final float[] foodTu = new float[SLOTS];
    private final float[] otherChar = new float[SLOTS];
    private float panTu;
    private long panStamp;
    private long liftedThrough = -1L;
    private @Nullable ResourceLocation material;
    private float relax;
    private int syncKey;

    // What the client draws: filled from the update tag, and from the real state on the server.
    private final Item[] shownItem = new Item[SLOTS];
    private final float[] shownCook = new float[SLOTS];
    private final float[] shownChar = new float[SLOTS];
    private final ItemStack[] shownStack = new ItemStack[SLOTS];
    private float shownPan;

    public SkilletBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.SKILLET.get(), pos, state);
        java.util.Arrays.fill(pieces, ItemStack.EMPTY);
        java.util.Arrays.fill(shownStack, ItemStack.EMPTY);
    }

    // --- figures ---

    /** The pan's swing per tick: the reference rate scaled up by how well its material conducts and down by how dense it is. */
    public float relax() {
        if (relax == 0f) {
            float rate = FTuning.SKILLET_PAN_RELAX;
            Material m = material == null ? null : MaterialTable.get().materials().get(material);
            if (m != null) {
                float conduct = m.thermal().conductivity().map(c -> c.workPerTickPerTu() / FTuning.SKILLET_REF_CONDUCTIVITY).orElse(1f);
                float density = m.density().muPerMb() > 0f ? m.density().muPerMb() / FTuning.SKILLET_REF_DENSITY : 1f;
                rate *= conduct / density;
            }
            relax = Mth.clamp(rate, FTuning.SKILLET_PAN_RELAX_MIN, FTuning.SKILLET_PAN_RELAX_MAX);
        }
        return relax;
    }

    public @Nullable ResourceLocation material() {
        return material;
    }

    /** The pan's heat now. An empty pan does not tick, so its heat is settled from the time since it last woke. */
    public float panTu() {
        if (level != null && !level.isClientSide && count() == 0)
            wake();
        return panTu;
    }

    public ItemStack piece(int slot) {
        return pieces[slot];
    }

    public float pieceTu(int slot) {
        return foodTu[slot];
    }

    public float otherChar(int slot) {
        return otherChar[slot];
    }

    public int count() {
        int n = 0;
        for (ItemStack s : pieces)
            if (!s.isEmpty())
                n++;
        return n;
    }

    /** The quarter of the pan a point on its top face is over: 0 to 3, left to right then back to front. */
    public static int slotAt(double localX, double localZ) {
        return (localX >= 0.5 ? 1 : 0) + (localZ >= 0.5 ? 2 : 0);
    }

    // --- acts ---

    /** Lay one piece in the slot asked for, or any free one. Returns whether it went in. */
    public boolean lay(int slot, ItemStack piece) {
        if (level == null || count() >= SLOTS)
            return false;
        int at = pieces[slot].isEmpty() ? slot : free();
        if (count() == 0)
            wake();
        pieces[at] = piece;
        foodTu[at] = AmbientTable.at(level).value();
        otherChar[at] = 0f;
        setChanged();
        sync(true);
        return true;
    }

    private int free() {
        for (int i = 0; i < SLOTS; i++)
            if (pieces[i].isEmpty())
                return i;
        return 0;
    }

    /** The piece at {@code slot} (or the first one, if that quarter is bare), cooked item and the two faces' char averaged, or empty. */
    public ItemStack take(int slot) {
        if (level == null)
            return ItemStack.EMPTY;
        int at = pieces[slot].isEmpty() ? first() : slot;
        if (at < 0)
            return ItemStack.EMPTY;
        ItemStack out = pieces[at];
        pieces[at] = ItemStack.EMPTY;
        var entry = Cooking.find(level, out);
        if (entry.isPresent()) {
            FoodState s = Cooking.state(out, entry.get());
            FoodState end = new FoodState(s.cook(), s.moisture(), s.pathogen(), s.toxin(), (s.charred() + otherChar[at]) * 0.5f,
                    s.rot(), s.stamp(), s.envTu(), s.smoke(), s.salt());
            out.set(FDataComponents.FOOD_STATE.get(), end);
            Cooking.apply(out, entry.get(), end);
        }
        otherChar[at] = 0f;
        if (count() == 0)
            panStamp = level.getGameTime();
        setChanged();
        sync(true);
        return out;
    }

    /** The temperature of the piece that {@link #take} would hand over. */
    public float takeTu(int slot) {
        int at = pieces[slot].isEmpty() ? first() : slot;
        return at < 0 ? 0f : foodTu[at];
    }

    private int first() {
        for (int i = 0; i < SLOTS; i++)
            if (!pieces[i].isEmpty())
                return i;
        return -1;
    }

    /** Turn the piece in {@code slot}: the face that was up goes down onto the pan. */
    public boolean flip(int slot) {
        if (level == null || pieces[slot].isEmpty())
            return false;
        var entry = Cooking.find(level, pieces[slot]);
        if (entry.isEmpty())
            return false;
        FoodState s = Cooking.state(pieces[slot], entry.get());
        float down = s.charred();
        pieces[slot].set(FDataComponents.FOOD_STATE.get(), new FoodState(s.cook(), s.moisture(), s.pathogen(), s.toxin(), otherChar[slot],
                s.rot(), s.stamp(), s.envTu(), s.smoke(), s.salt()));
        otherChar[slot] = down;
        level.playSound(null, worldPosition, SoundEvents.SLIME_BLOCK_STEP, SoundSource.BLOCKS, 0.6f, 1.3f);
        setChanged();
        sync(true);
        return true;
    }

    /** A spatula is holding the pan up this tick. */
    public void lift() {
        if (level != null)
            liftedThrough = level.getGameTime() + 1;
    }

    public boolean lifted() {
        return level != null && level.getGameTime() <= liftedThrough;
    }

    /** Bring the pan's temperature up to now: it has been following the fire since it was last awake. */
    private void wake() {
        long now = level.getGameTime();
        float fire = HeatSource.below(level, worldPosition).value();
        if (panStamp == 0L) {
            panTu = AmbientTable.at(level).value();
            panStamp = now;
        }
        long gap = now - panStamp;
        if (gap > 0)
            panTu = fire + (panTu - fire) * (float) Math.pow(1f - relax(), gap);
        panStamp = now;
    }

    // --- tick ---

    public void tickServer() {
        if (level == null || count() == 0)
            return;
        long now = level.getGameTime();
        float ambient = AmbientTable.at(level).value();
        float target = lifted() ? ambient : HeatSource.below(level, worldPosition).value();
        panTu += (target - panTu) * relax();
        panStamp = now;
        boolean smoke = false, steam = false;
        float hottest = 0f;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = pieces[i];
            if (stack.isEmpty())
                continue;
            var entry = Cooking.find(level, stack);
            if (entry.isEmpty())
                continue;
            foodTu[i] = SkilletBlock.warm(foodTu[i], panTu, entry.get().food().mass());
            FoodState before = Cooking.state(stack, entry.get());
            Cooking.Doneness was = Cooking.doneness(entry.get(), before);
            pieces[i] = Cooking.tick(stack, entry.get(), new Tu(foodTu[i]), level);
            FoodState after = Cooking.state(pieces[i], entry.get());
            Cooking.Doneness is = Cooking.doneness(entry.get(), after);
            if (was != is && level instanceof ServerLevel sl) {
                if (is == Cooking.Doneness.DONE)
                    sl.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.6f, 1.5f);
                else if (is == Cooking.Doneness.CHARRED)
                    sl.playSound(null, worldPosition, SoundEvents.BLAZE_BURN, SoundSource.BLOCKS, 0.4f, 0.6f);
            }
            hottest = Math.max(hottest, foodTu[i]);
            smoke |= after.charred() > 0.02f;
            steam |= foodTu[i] > FTuning.FOOD_COOK_TU.value() && after.moisture() > 0f;
        }
        if (now % 10L == 0L && level instanceof ServerLevel sl) {
            double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 0.25, z = worldPosition.getZ() + 0.5;
            if (steam)
                sl.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.2, 0.02, 0.2, 0.01);
            if (smoke)
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 2, 0.2, 0.02, 0.2, 0.01);
            if (steam)
                sl.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.1f + 0.1f * Mth.clamp(hottest / 200f, 0f, 1f), 0.9f + 0.7f * Mth.clamp(hottest / 200f, 0f, 1f));
            sync(false);
            setChanged();
        }
    }

    // --- sync: quantised, only when it changes ---

    private void refreshShown() {
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = pieces[i];
            setShown(i, s.isEmpty() ? null : s.getItem());
            var entry = s.isEmpty() || level == null ? java.util.Optional.<Cooking.Entry>empty() : Cooking.find(level, s);
            FoodState st = entry.map(e -> Cooking.state(s, e)).orElse(null);
            shownCook[i] = st == null ? 0f : st.cook();
            shownChar[i] = st == null ? 0f : st.charred();
        }
        shownPan = panTu;
    }

    private void sync(boolean force) {
        refreshShown();
        int key = Math.round(shownPan / 5f);
        for (int i = 0; i < SLOTS; i++)
            key = key * 31 + (shownItem[i] == null ? 0 : BuiltInRegistries.ITEM.getId(shownItem[i]) + 1)
                    + Math.round(shownCook[i] * 20f) * 7 + Math.round(shownChar[i] * 20f) * 13;
        if (force || key != syncKey) {
            syncKey = key;
            if (level != null)
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void setShown(int slot, @Nullable Item item) {
        if (shownItem[slot] == item)
            return;
        shownItem[slot] = item;
        shownStack[slot] = item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** The piece as the renderer draws it; built when the item changes, never per frame. */
    public ItemStack shownStack(int slot) {
        return shownStack[slot];
    }

    public @Nullable Item shownItem(int slot) {
        return shownItem[slot];
    }

    public float shownCook(int slot) {
        return shownCook[slot];
    }

    public float shownChar(int slot) {
        return shownChar[slot];
    }

    public float shownPan() {
        return shownPan;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        refreshShown();
        ListTag list = new ListTag();
        for (int i = 0; i < SLOTS; i++) {
            CompoundTag t = new CompoundTag();
            if (shownItem[i] != null) {
                t.putString("Id", BuiltInRegistries.ITEM.getKey(shownItem[i]).toString());
                t.putFloat("Cook", Math.round(shownCook[i] * 20f) / 20f);
                t.putFloat("Char", Math.round(shownChar[i] * 20f) / 20f);
            }
            list.add(t);
        }
        tag.put("Show", list);
        tag.putFloat("Pan", Math.round(shownPan / 5f) * 5f);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // --- save ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (material != null)
            tag.putString("Material", material.toString());
        tag.putFloat("PanTu", panTu);
        tag.putLong("PanStamp", panStamp);
        ListTag list = new ListTag();
        for (int i = 0; i < SLOTS; i++) {
            CompoundTag t = new CompoundTag();
            if (!pieces[i].isEmpty()) {
                t.put("Stack", pieces[i].save(registries));
                t.putFloat("Tu", foodTu[i]);
                t.putFloat("Other", otherChar[i]);
            }
            list.add(t);
        }
        tag.put("Pieces", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Show", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Show", Tag.TAG_COMPOUND);
            for (int i = 0; i < SLOTS && i < list.size(); i++) {
                CompoundTag t = list.getCompound(i);
                setShown(i, t.contains("Id") ? BuiltInRegistries.ITEM.get(ResourceLocation.parse(t.getString("Id"))) : null);
                shownCook[i] = t.getFloat("Cook");
                shownChar[i] = t.getFloat("Char");
            }
            shownPan = tag.getFloat("Pan");
            return;
        }
        material = tag.contains("Material") ? ResourceLocation.parse(tag.getString("Material")) : null;
        relax = 0f;
        panTu = tag.getFloat("PanTu");
        panStamp = tag.getLong("PanStamp");
        ListTag list = tag.getList("Pieces", Tag.TAG_COMPOUND);
        for (int i = 0; i < SLOTS; i++) {
            CompoundTag t = i < list.size() ? list.getCompound(i) : new CompoundTag();
            pieces[i] = t.contains("Stack") ? ItemStack.parse(registries, t.getCompound("Stack")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
            foodTu[i] = t.getFloat("Tu");
            otherChar[i] = t.getFloat("Other");
        }
        refreshShown();
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        MadeOf made = input.get(FDataComponents.MADE_OF.get());
        if (made != null)
            material = made.material();
        relax = 0f;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (material != null)
            components.set(FDataComponents.MADE_OF.get(), new MadeOf(material, "plate", 3));
    }

    /** Drop what is in the pan when the block goes. */
    public void dropContents() {
        if (level == null || level.isClientSide)
            return;
        for (ItemStack s : pieces)
            if (!s.isEmpty())
                net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.3, worldPosition.getZ() + 0.5, s);
    }
}
