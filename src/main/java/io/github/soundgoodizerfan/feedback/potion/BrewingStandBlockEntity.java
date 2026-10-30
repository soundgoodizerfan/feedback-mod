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
package io.github.soundgoodizerfan.feedback.potion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.net.StandStatePayload;
import io.github.soundgoodizerfan.feedback.process.Compound;
import io.github.soundgoodizerfan.feedback.process.CompoundTable;
import io.github.soundgoodizerfan.feedback.process.Cooking;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.FoodCut;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The brewing stand's pot (potions spec 3 and 11): water and up to six ingredient slots, a marker steered over the
 * water map by stirring, and bottles out. The marker model lives here and the stirring screen only draws it and sends a pace, as the stockpot does.
 *
 * <p>The marker is {@code anchor + curve(progress)}: plants are path segments (curves) in the order they went in, each reaching as far as what
 * comes out of it ({@link PotionBrew#extraction}: the cut and the pot's heat as it goes in, less what rot took; fixed from then, it never grows), and stirring advances along the
 * current one by arc length, {@code progress} being the fraction of the curve walked. Brewing stops where the marker is; segments not yet stirred are spent and add nothing. Heat is read from the fire below as the
 * stockpot does, and with water in it pins at boiling.
 *
 * <p>Per-tick work is only the heat and, while ingredients are in, the hazards at the marker.
 *
 * shortcut: nothing is caught up across an unloaded chunk (the pot just waits); the stand takes no water until its bottles are out;
 * dilution (second-slice) is not modelled.
 */
public class BrewingStandBlockEntity extends BlockEntity {

    /**
     * One plant in the water: its family, the stabiliser nudge it gives on each axis at full share, how rotten, the cut's surface, when it went in, and the
     * stack it was made from. The extraction and the segment (shape, heading, reach, rot's drift) are worked out once, as it goes in, from the cut and
     * the pot's heat then, and never change.
     */
    private record Ingredient(String family, float pullX, float pullY, float rot, float surface, long added, ItemStack key, float extraction, PotionBrew.Segment seg) {}

    /** Slot numbers in {@link #slots}: the water bucket, six ingredient slots in path order, three finished bottles. */
    public static final int WATER_SLOT = 0, FIRST_INGREDIENT = 1, INGREDIENTS = 6, FIRST_BOTTLE = 7, BOTTLES = 3, SLOTS = 10;

    private static final String BASE = "water";

    private float temperature = io.github.soundgoodizerfan.feedback.core.FTuning.AMBIENT_TU.value();
    private final SimpleContainer slots = new SimpleContainer(SLOTS);
    private final Ingredient[] meta = new Ingredient[INGREDIENTS];
    /** Ingredient slots in the order they were added: the plotted path, front first. A slot leaves it when its segment is fully stirred in (and empties) or when the item is taken back out. */
    private final List<Integer> queue = new ArrayList<>();
    /** What the stirred-in ingredients have given so far: taste sum (3), weight, and the stabilisers' pull on x and y. */
    private final float[] done = new float[6];
    private int water, consumed;
    private float progress, ax, ay, potency = 1f;
    private long stirTick, packetTick;
    private float stir;
    private UUID stirrer;
    private String last = "";

    public BrewingStandBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.BREWING_STAND.get(), pos, state);
        slots.addListener(c -> sync());
    }

    public Container container() {
        return slots;
    }

    /** What the menu's data slots show: water in mB, the slot at the front of the path (-1 if none), and whether it is part way through. */
    public int water() {
        return water;
    }

    public int front() {
        return queue.isEmpty() ? -1 : queue.get(0);
    }

    public boolean started() {
        return progress > 0f;
    }

    public String last() {
        return last;
    }

    /** The slots changed: pick up ingredients that went in or out, and fill the pot from a water bucket. */
    private void sync() {
        if (level == null || level.isClientSide)
            return;
        for (int i = 0; i < INGREDIENTS; i++) {
            ItemStack stack = slots.getItem(FIRST_INGREDIENT + i);
            if (stack.isEmpty()) {
                // Taken back out before it was stirred in: its segment goes and the later ones move up.
                meta[i] = null;
                queue.remove((Integer) i);
            } else if (meta[i] == null || !ItemStack.isSameItemSameComponents(meta[i].key(), stack)) {
                // A new ingredient: its segment goes on the end of the path.
                queue.remove((Integer) i);
                meta[i] = made(stack);
                if (meta[i] != null)
                    queue.add(i);
            }
        }
        if (water <= 0 && bottles() == 0 && slots.getItem(WATER_SLOT).getItem() instanceof BucketItem bucket && bucket.content.is(FluidTags.WATER)) {
            water = PotionTuning.BOTTLES * PotionTuning.BOTTLE_MB;
            slots.setItem(WATER_SLOT, new ItemStack(Items.BUCKET));
        }
        setChanged();
    }

    /** The ingredient a stack makes: its plant's segment, how rotten, and the surface its cut gives the water. Null if its material carries no path. */
    private Ingredient made(ItemStack stack) {
        Material.Potion plant = potionOf(level, stack).orElse(null);
        if (plant == null)
            return null;
        FoodCut cut = stack.get(FDataComponents.FOOD_CUT.get());
        float rot = rotOf(level, stack);
        float surface = surface(cut == null ? 0 : cut.level());
        float extraction = PotionBrew.extraction(surface, temperature);
        PotionBrew.Segment seg = PotionBrew.segment(plant, rot, extraction, rotDegrees(map()));
        // A stabiliser's nudge: its amount at full cut and heat, less what rot took.
        float nudge = plant.pull().map(p -> p.amount() * extraction * (1f - PotionTuning.ROT_LOSS * rot)).orElse(0f);
        boolean x = plant.pull().map(Material.Pull::axisX).orElse(false);
        return new Ingredient(plant.family(), x ? nudge : 0f, x ? 0f : nudge, rot, surface, level.getGameTime(), stack.copy(), extraction, seg);
    }

    /** What a stack does on the map: its material's {@code potion} block, found by the material system and never by item. Read client side too, for the screen's preview. */
    public static java.util.Optional<Material.Potion> potionOf(net.minecraft.world.level.Level level, ItemStack stack) {
        return Material.find(ClientMaterials.on(level), stack).flatMap(l -> l.material().potion());
    }

    /** How rotten a stack is; a thing that is not perishable food is not. */
    public static float rotOf(net.minecraft.world.level.Level level, ItemStack stack) {
        return Cooking.find(level, stack).map(e -> Cooking.current(stack, e, level, AmbientTable.at(level).value()).rot()).orElse(0f);
    }

    public static float surface(int cutLevel) {
        return PotionTuning.GRIND[Mth.clamp(cutLevel, 0, PotionTuning.GRIND.length - 1)];
    }

    public int bottles() {
        int n = 0;
        for (int i = 0; i < BOTTLES; i++)
            if (!slots.getItem(FIRST_BOTTLE + i).isEmpty())
                n++;
        return n;
    }

    /** Ingredients in the pot that have not been stirred in yet. */
    public int ingredientCount() {
        return queue.size();
    }

    public float temperature() {
        return temperature;
    }

    /** Tip the pot out: the water and everything in it is spent. */
    public void empty() {
        water = 0;
        spendIngredients();
        reset();
        setChanged();
    }

    private void spendIngredients() {
        for (int i = 0; i < INGREDIENTS; i++)
            slots.setItem(FIRST_INGREDIENT + i, ItemStack.EMPTY);
    }

    private void reset() {
        queue.clear();
        java.util.Arrays.fill(done, 0f);
        consumed = 0;
        progress = ax = ay = 0f;
        potency = 1f;
    }

    /** The player is stirring at this pace (0 to 1); it holds for half a second. */
    public void stir(ServerPlayer player, float pace) {
        long now = level.getGameTime();
        if (now - packetTick < 3L)
            return;
        packetTick = now;
        stir = Mth.clamp(pace, 0f, 1f);
        stirTick = now;
        stirrer = player.getUUID();
    }

    private List<Map.Entry<ResourceLocation, PotionFeature>> map() {
        return PotionMap.get().on(BASE);
    }

    /** Where rot pulls: the way to the nearest precipitate zone from the centre (spec 3.3), or down-left when the map has none. */
    public float rotDegrees(List<Map.Entry<ResourceLocation, PotionFeature>> map) {
        float best = Float.MAX_VALUE, angle = 225f;
        for (var e : map) {
            PotionFeature f = e.getValue();
            float d = f.distance(0f, 0f);
            if (f.kind() == PotionFeature.Kind.PRECIPITATE && d < best) {
                best = d;
                angle = (float) Math.toDegrees(Math.atan2(f.y(), f.x()));
            }
        }
        return angle;
    }

    /** The marker now. */
    public float[] marker() {
        float[] out = {ax, ay};
        if (!queue.isEmpty()) {
            float[] at = new float[2];
            meta[queue.get(0)].seg().at(progress, at);
            out[0] += at[0];
            out[1] += at[1];
        }
        return out;
    }

    public void tickServer() {
        if (level == null)
            return;
        float fire = HeatSource.below(level, worldPosition).value();
        float target = water > 0 ? Math.min(fire, io.github.soundgoodizerfan.feedback.core.FTuning.POT_BOIL_TU.value()) : fire;
        target = Math.max(target, AmbientTable.at(level).value());
        temperature += (target - temperature) * PotionTuning.RELAX_PER_TICK;
        if (water <= 0)
            return;
        var map = map();
        float stepped = 0f;
        if (level.getGameTime() - stirTick <= PotionTuning.STIR_HOLD_TICKS && !queue.isEmpty() && stir > 0f) {
            int front = queue.get(0);
            Ingredient ing = meta[front];
            // The cursor walks the curve by arc length, so a zigzag costs more stirring than a line of the same reach.
            stepped = stir * PotionTuning.STIR_DIST_PER_TICK;
            progress += stepped / Math.max(ing.seg().arcLength(), PotionTuning.MIN_SEGMENT);
            if (progress >= 1f) {
                // Fully stirred in: the item is in the water, its slot empties and is free again.
                float[] v = ing.seg().end();
                ax += v[0];
                ay += v[1];
                accumulate(done, ing, 1f);
                consumed++;
                progress = 0f;
                queue.remove(0);
                slots.setItem(FIRST_INGREDIENT + front, ItemStack.EMPTY);
            }
        }
        float[] p = marker();
        // The wall: the map's edge pushes the marker back and costs nothing, so a marker driven into it slides along it.
        float bound = PotionMap.get().boundary(BASE), radius = (float) Math.hypot(p[0], p[1]);
        if (radius > bound) {
            float k = bound / radius;
            ax += p[0] * k - p[0];
            ay += p[1] * k - p[1];
            p = new float[] {p[0] * k, p[1] * k};
        }
        if (stepped > 0f) {
            float[] at = p;
            knowledge(k -> k.reveal(at[0], at[1]));
        }
        boolean drained = false;
        PotionFeature killer = null;
        for (var e : map) {
            PotionFeature f = e.getValue();
            float d = f.distance(p[0], p[1]);
            if (d > f.radius() + PotionTuning.MARKER_RADIUS)
                continue;
            switch (f.kind()) {
                case PRECIPITATE -> {
                    drained = true;
                    killer = f;
                }
                case SCORCH -> {
                    if (temperature > f.above()) {
                        drained = true;
                        killer = f;
                    }
                }
                case VORTEX -> {
                    if (temperature < PotionTuning.VORTEX_TU)
                        break;
                    float dx = p[0] - f.x(), dy = p[1] - f.y();
                    if (d <= f.radius() * PotionTuning.VORTEX_CORE + PotionTuning.MARKER_RADIUS) {
                        // The centre of a spinning vortex: the marker jumps, and where it lands shows from then on.
                        ax += f.destination().get().get(0) - p[0];
                        ay += f.destination().get().get(1) - p[1];
                        knowledge(k -> k.usedVortex(e.getKey()));
                    } else {
                        float c = Mth.cos(PotionTuning.VORTEX_TURN_PER_TICK), s = Mth.sin(PotionTuning.VORTEX_TURN_PER_TICK);
                        ax += f.x() + dx * c - dy * s - p[0];
                        ay += f.y() + dx * s + dy * c - p[1];
                    }
                }
                default -> {
                }
            }
        }
        float lost = PotionTuning.DRAIN_STILL_PER_TICK + stepped * PotionTuning.DRAIN_PER_DISTANCE * (1f + PotionTuning.FAST_HIT * stir);
        potency = Mth.clamp(potency + (drained ? -lost : PotionTuning.RECOVER_PER_TICK), 0f, 1f);
        if (potency <= 0f && killer != null) {
            // Fully drained: the brew fails where it died (spec 3.4).
            last = "feedback.stand.drained";
            finish(null, killer.x(), killer.y(), null);
            return;
        }
        if (level instanceof ServerLevel server && level.getGameTime() % 10 == 0) {
            double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 0.9, z = worldPosition.getZ() + 0.5;
            if (drained)
                server.sendParticles(ParticleTypes.SMOKE, x, y, z, 2, 0.1, 0.05, 0.1, 0.01);
            else if (temperature >= PotionTuning.COOK_TU)
                server.sendParticles(ParticleTypes.BUBBLE_POP, x, y, z, 1, 0.1, 0.05, 0.1, 0.01);
        }
        changed();
    }

    private void knowledge(java.util.function.UnaryOperator<PotionKnowledge> change) {
        if (stirrer == null || !(level instanceof ServerLevel server) || server.getPlayerByUUID(stirrer) == null)
            return;
        var player = server.getPlayerByUUID(stirrer);
        PotionKnowledge was = player.getData(PotionAttachments.KNOWLEDGE.get()), now = change.apply(was);
        if (now != was)
            player.setData(PotionAttachments.KNOWLEDGE.get(), now);
    }

    /**
     * Pour water over the brew: the marker goes a fixed step straight toward the centre (home, if nearer), whatever the path was, and the brew
     * loses a fixed slice of potency. The only erase there is. Returns the message key, or null if it did.
     */
    public String ladle(ServerPlayer player) {
        if (water <= 0 || ingredientCount() == 0)
            return "feedback.stand.nothing";
        if (potency <= PotionTuning.LADLE_POTENCY)
            return "feedback.stand.weak";
        float[] p = marker();
        float d = (float) Math.hypot(p[0], p[1]), k = d <= PotionTuning.LADLE_STEP ? 1f : PotionTuning.LADLE_STEP / d;
        ax -= p[0] * k;
        ay -= p[1] * k;
        potency -= PotionTuning.LADLE_POTENCY;
        level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6f, 1.2f);
        setChanged();
        return null;
    }

    /**
     * Cook where the marker is: a node it sits in becomes that potion, anything else a dud draught, and either way the ingredients and the
     * water are spent. Returns the message key for the player, or null if it did not run (too cool, nothing to brew).
     */
    public String brew(ServerPlayer player) {
        if (water < PotionTuning.BOTTLE_MB || ingredientCount() == 0 && consumed == 0)
            return "feedback.stand.nothing";
        if (temperature < PotionTuning.COOK_TU)
            return "feedback.stand.cold";
        stirrer = player.getUUID();
        float[] p = marker();
        // The stabilisers in what was stirred tighten a near miss.
        float[] given = given();
        float pullX = given[4], pullY = given[5];
        float[] at = PotionBrew.pull(map(), p[0], p[1], pullX, pullY);
        return finish(player, at[0], at[1], PotionBrew.land(map(), at[0], at[1]));
    }

    /** Spend the brew. {@code landing} null is a dud; {@code player} null is a failure with no one to tell. */
    private String finish(ServerPlayer player, float x, float y, PotionBrew.Landing landing) {
        int n = Mth.clamp(water / PotionTuning.BOTTLE_MB, 1, PotionTuning.BOTTLES);
        float[] taste = taste();
        ServerPlayer who = player != null ? player : stirrer != null && level instanceof ServerLevel s ? s.getServer().getPlayerList().getPlayer(stirrer) : null;
        PotionState state;
        String message;
        if (landing != null) {
            state = new PotionState(landing.feature().effect().map(PotionFeature.Effect::effect), PotionBrew.amplifier(landing), PotionBrew.duration(landing),
                    potency, BASE, List.of(taste[0], taste[1], taste[2]), level.getGameTime(), temperature, -1);
            message = "feedback.stand.made";
            if (who != null)
                who.setData(PotionAttachments.KNOWLEDGE.get(), who.getData(PotionAttachments.KNOWLEDGE.get()).made(landing.id()));
        } else {
            // A dud's taste leans toward the compound class of the nearest node not yet made, so a tasting player reads it too (spec 3.4).
            String lean = nearestClass(who, x, y);
            var compound = CompoundTable.get().find(lean);
            float[] mix = taste.clone();
            if (compound.isPresent() && compound.get().taste().size() >= 3)
                for (int k = 0; k < 3; k++)
                    mix[k] = 0.5f * taste[k] + 0.5f * compound.get().taste().get(k);
            state = new PotionState(java.util.Optional.empty(), 0, 0, 1f, BASE, List.of(mix[0], mix[1], mix[2]), level.getGameTime(), temperature,
                    compound.flatMap(Compound::color).orElse(PotionTuning.WATER_COLOR));
            message = "feedback.stand.dud";
            if (who != null)
                who.setData(PotionAttachments.KNOWLEDGE.get(), who.getData(PotionAttachments.KNOWLEDGE.get()).failedAt(x, y, map()));
        }
        water = 0;
        spendIngredients();
        reset();
        last = message;
        for (int k = 0; k < n; k++) {
            ItemStack bottle = new ItemStack(io.github.soundgoodizerfan.feedback.registry.FItems.POTION.get());
            bottle.set(FDataComponents.POTION_STATE.get(), state);
            slots.setItem(FIRST_BOTTLE + k, bottle);
        }
        if (level instanceof ServerLevel server) {
            double cx = worldPosition.getX() + 0.5, cy = worldPosition.getY() + 0.9, cz = worldPosition.getZ() + 0.5;
            server.sendParticles(landing != null ? ParticleTypes.ENCHANT : ParticleTypes.LARGE_SMOKE, cx, cy, cz, landing != null ? 20 : 6, 0.2, 0.2, 0.2, 0.05);
            level.playSound(null, worldPosition, landing != null ? SoundEvents.BREWING_STAND_BREW : SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1f, 1f);
        }
        changed();
        return message;
    }

    /** The plant family the nearest node this player has not made leans toward (its own {@code family} field), or empty. */
    private String nearestClass(ServerPlayer who, float x, float y) {
        if (who == null)
            return "";
        PotionKnowledge known = who.getData(PotionAttachments.KNOWLEDGE.get());
        PotionFeature near = null;
        float best = Float.MAX_VALUE;
        for (var e : map()) {
            float d = e.getValue().distance(x, y);
            if (e.getValue().kind() == PotionFeature.Kind.EFFECT && !known.discovered().contains(e.getKey().toString()) && d < best) {
                best = d;
                near = e.getValue();
            }
        }
        return near == null ? "" : near.effect().get().family();
    }

    /** Add what {@code share} of an ingredient has given into {@code acc}: its family's taste weighted by the reach it has walked, and the stabilisers' pull. */
    private void accumulate(float[] acc, Ingredient ing, float share) {
        float w = Math.max(ing.seg().reach(), 0f) * share;
        var compound = CompoundTable.get().find(ing.family()).orElse(null);
        if (compound != null && compound.taste().size() >= 3) {
            for (int k = 0; k < 3; k++)
                acc[k] += w * compound.taste().get(k);
            acc[3] += w;
        }
        acc[4] += ing.pullX() * share;
        acc[5] += ing.pullY() * share;
    }

    /** What has come out so far: the stirred-in ingredients and the part of the front one. */
    private float[] given() {
        float[] acc = done.clone();
        if (!queue.isEmpty())
            accumulate(acc, meta[queue.get(0)], progress);
        return acc;
    }

    /** The strength-weighted mean taste of what has come out into the water (spec 7). */
    private float[] taste() {
        float[] acc = given();
        float[] sum = new float[3];
        if (acc[3] > 0f)
            for (int k = 0; k < 3; k++)
                sum[k] = acc[k] / acc[3];
        return sum;
    }

    /** What the stirring screen draws for this player: the marker, the path ahead, the hazards, and only the nodes and marks this player has earned. */
    public StandStatePayload state(ServerPlayer player) {
        var map = map();
        PotionKnowledge known = player.getData(PotionAttachments.KNOWLEDGE.get());
        if (!known.seen(0f, 0f)) {
            // The centre, where every brew starts, is never fogged.
            known = known.reveal(0f, 0f);
            player.setData(PotionAttachments.KNOWLEDGE.get(), known);
        }
        List<StandStatePayload.Mark> marks = new ArrayList<>();
        for (var e : map) {
            PotionFeature f = e.getValue();
            // Hazards, vortices and unmade nodes show only where the marker has been near; a made node and a landmark always do.
            boolean seen = known.seen(f.x(), f.y()) || known.seen(f.x() + f.radius(), f.y()) || known.seen(f.x() - f.radius(), f.y())
                    || known.seen(f.x(), f.y() + f.radius()) || known.seen(f.x(), f.y() - f.radius());
            switch (f.kind()) {
                case PRECIPITATE -> {
                    if (seen)
                        marks.add(new StandStatePayload.Mark(0, f.x(), f.y(), f.radius(), 0f, 0f, ""));
                }
                case SCORCH -> {
                    if (seen)
                        marks.add(new StandStatePayload.Mark(1, f.x(), f.y(), f.radius(), f.above(), 0f, ""));
                }
                case VORTEX -> {
                    boolean used = known.vortices().contains(e.getKey().toString());
                    if (seen)
                        marks.add(new StandStatePayload.Mark(2, f.x(), f.y(), f.radius(), used ? f.destination().get().get(0) : Float.NaN,
                                used ? f.destination().get().get(1) : Float.NaN, ""));
                }
                case EFFECT -> {
                    // The faint line from the origin to every node, through fog, so a fogged one can be found by following it.
                    // The line ends in an icon: a made node as its effect (always known), an unmade one as a bare silhouette. Only the position shows.
                    boolean made = known.discovered().contains(e.getKey().toString());
                    marks.add(new StandStatePayload.Mark(6, f.x(), f.y(), f.radius(), made ? 1f : 0f, 0f, ""));
                    if (made)
                        marks.add(new StandStatePayload.Mark(3, f.x(), f.y(), f.radius(), 0f, 0f, f.effect().get().effect().toString()));
                }
                case LANDMARK -> marks.add(new StandStatePayload.Mark(7, f.x(), f.y(), f.radius(), 0f, 0f, ""));
                default -> {
                }
            }
        }
        for (var r : known.residues()) {
            PotionFeature target = PotionMap.get().find(ResourceLocation.parse(r.target()));
            if (target == null)
                continue;
            // Only the class of compound the nearest node leans on; never the effect, and the dashed line already gives the place.
            marks.add(new StandStatePayload.Mark(4, r.x(), r.y(), 0f, 0f, 0f, target.effect().map(PotionFeature.Effect::family).orElse("")));
        }
        float rotDeg = rotDegrees(map);
        float[] p = marker();
        List<StandStatePayload.Seg> path = new ArrayList<>();
        float x = ax, y = ay;
        boolean first = true;
        for (int slot : queue) {
            PotionBrew.Segment seg = meta[slot].seg();
            path.add(new StandStatePayload.Seg(x, y, seg, first ? progress : 0f));
            first = false;
            float[] v = seg.end();
            x += v[0];
            y += v[1];
        }
        return new StandStatePayload(worldPosition, p[0], p[1], potency, temperature, water, consumed + queue.size(), consumed, rotDeg, PotionMap.get().boundary(BASE), last, path, marks, known.bits());
    }

    private void changed() {
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Temperature", temperature);
        tag.putInt("Water", water);
        tag.putInt("Consumed", consumed);
        tag.putIntArray("Queue", queue.stream().mapToInt(Integer::intValue).toArray());
        ListTag acc = new ListTag();
        for (float f : done)
            acc.add(net.minecraft.nbt.FloatTag.valueOf(f));
        tag.put("Done", acc);
        tag.putFloat("Progress", progress);
        tag.putFloat("Ax", ax);
        tag.putFloat("Ay", ay);
        tag.putFloat("Potency", potency);
        tag.put("Slots", slots.createTag(registries));
        ListTag list = new ListTag();
        for (int i = 0; i < INGREDIENTS; i++) {
            Ingredient ing = meta[i];
            if (ing == null)
                continue;
            CompoundTag t = new CompoundTag();
            t.putInt("Slot", i);
            t.putString("Family", ing.family());
            t.putFloat("PullX", ing.pullX());
            t.putFloat("PullY", ing.pullY());
            t.putFloat("Ext", ing.extraction());
            PotionBrew.Segment g = ing.seg();
            t.putFloat("Heading", g.heading());
            t.putFloat("Reach", g.reach());
            t.putFloat("Dx", g.dx());
            t.putFloat("Dy", g.dy());
            t.putInt("Shape", g.shape().type().ordinal());
            t.putFloat("P1", g.shape().p1());
            t.putFloat("P2", g.shape().p2());
            t.putFloat("Rot", ing.rot());
            t.putFloat("Surface", ing.surface());
            t.putLong("Added", ing.added());
            list.add(t);
        }
        tag.put("Ingredients", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("Temperature") ? tag.getFloat("Temperature") : io.github.soundgoodizerfan.feedback.core.FTuning.AMBIENT_TU.value();
        water = tag.getInt("Water");
        consumed = tag.getInt("Consumed");
        queue.clear();
        for (int q : tag.getIntArray("Queue"))
            queue.add(q);
        ListTag acc = tag.getList("Done", Tag.TAG_FLOAT);
        for (int i = 0; i < done.length; i++)
            done[i] = i < acc.size() ? acc.getFloat(i) : 0f;
        progress = tag.getFloat("Progress");
        ax = tag.getFloat("Ax");
        ay = tag.getFloat("Ay");
        potency = tag.contains("Potency") ? tag.getFloat("Potency") : 1f;
        slots.fromTag(tag.getList("Slots", Tag.TAG_COMPOUND), registries);
        java.util.Arrays.fill(meta, null);
        for (Tag raw : tag.getList("Ingredients", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) raw;
            int slot = t.getInt("Slot");
            if (slot >= 0 && slot < INGREDIENTS)
                meta[slot] = new Ingredient(t.getString("Family"), t.getFloat("PullX"), t.getFloat("PullY"), t.getFloat("Rot"), t.getFloat("Surface"), t.getLong("Added"),
                        slots.getItem(FIRST_INGREDIENT + slot).copy(), t.getFloat("Ext"),
                        new PotionBrew.Segment(t.getFloat("Heading"), t.getFloat("Reach"), new PotionShape(PotionShape.Type.values()[Math.floorMod(t.getInt("Shape"), PotionShape.Type.values().length)],
                                t.getFloat("P1"), t.getFloat("P2")), t.getFloat("Dx"), t.getFloat("Dy")));
        }
    }
}
