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
package io.github.soundgoodizerfan.feedback.process;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.weapon.BarrelGeometry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cutting that changes a part's shape rather than turning it into the next form (spec 12):
 * datapack {@code machining/}, one row per form, operation and dimension. A drill on a gun barrel
 * grows its bore by so much per Fu, a lathe thins its outside, a cutoff wheel shortens it -- and
 * none of them knows the target, so the part keeps changing for as long as the machine runs. Cut
 * until the wall or the length is gone and it is the material's swarf: the core pitch, on a part.
 *
 * <p>Fu are reckoned as a removal's are -- force over the material's hardness, a tool softer than
 * the material does nothing -- so the same drive on steel is a slower, more controllable cut than
 * on copper. {@link Removing} asks here when its own table has no answer.
 */
public class Machining extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "machining";

    /** What a row changes. */
    public enum Dimension implements StringRepresentable {
        BORE("bore"), OUTER("outer"), LENGTH("length");

        public static final Codec<Dimension> CODEC = StringRepresentable.fromEnum(Dimension::values);
        private final String id;

        Dimension(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    /**
     * One row: {@code per_fu} is mm of diameter per Fu for bore and outer (bore grows, outer
     * shrinks), metres per Fu for length (it shrinks).
     */
    public record Step(String form, Operation operation, Dimension dimension, float perFu) {
        public static final Codec<Step> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("form").forGetter(Step::form),
                Operation.CODEC.fieldOf("operation").forGetter(Step::operation),
                Dimension.CODEC.fieldOf("dimension").forGetter(Step::dimension),
                Codec.FLOAT.fieldOf("per_fu").forGetter(Step::perFu)
        ).apply(i, Step::new));
    }

    private static final Machining INSTANCE = new Machining();

    private List<Step> steps = List.of();
    private final Map<Item, Optional<Material.Located>> parts = new HashMap<>();
    private int partsGeneration = -1;

    private Machining() {
        super(new GsonBuilder().create(), DIRECTORY);
    }

    public static Machining get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        List<Step> loaded = new ArrayList<>();
        json.forEach((id, element) -> Step.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad machining step {}: {}", id, error))
                .ifPresent(loaded::add));
        steps = List.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} machining steps", steps.size());
    }

    /**
     * The material and form of {@code item} if some row machines that form, else empty. Cached
     * per item and flushed when the material table reloads: finding a material by item walks it.
     */
    public Optional<Material.Located> part(ItemStack stack) {
        if (stack.has(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get()))
            return Material.find(MaterialTable.get().materials(), stack).filter(this::machined);
        Item item = stack.getItem();
        if (partsGeneration != MaterialTable.generation()) {
            parts.clear();
            partsGeneration = MaterialTable.generation();
        }
        return parts.computeIfAbsent(item, it -> Material.find(MaterialTable.get().materials(), it).filter(this::machined));
    }

    private boolean machined(Material.Located l) {
        return steps.stream().anyMatch(s -> s.form().equals(l.form()));
    }

    /**
     * A part fresh from the form step that made it, given its stock shape so a reading has
     * something to say. Untouched if it is not a machined part.
     */
    public ItemStack stamp(ItemStack stack) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        if (made != null && part(stack).isPresent() && made.dims().isEmpty())
            stack.set(FDataComponents.MADE_OF.get(), withGeometry(made, BarrelGeometry.STOCK));
        return stack;
    }

    /** The barrel's real shape on its part: the three measures the barrel role declares as {@code dims}. */
    public static MadeOf withGeometry(MadeOf made, BarrelGeometry g) {
        return made.withDim(BarrelGeometry.BORE, g.boreMm()).withDim(BarrelGeometry.OUTER, g.outerMm())
                .withDim(BarrelGeometry.LENGTH, g.lengthM());
    }

    /** The barrel's real shape on its part: what a machine left on it, else fresh from the drill. */
    public static BarrelGeometry geometry(MadeOf made) {
        if (made == null || made.dims().isEmpty())
            return BarrelGeometry.STOCK;
        return new BarrelGeometry(made.dims().getOrDefault(BarrelGeometry.BORE, BarrelGeometry.STOCK.boreMm()),
                made.dims().getOrDefault(BarrelGeometry.OUTER, BarrelGeometry.STOCK.outerMm()),
                made.dims().getOrDefault(BarrelGeometry.LENGTH, BarrelGeometry.STOCK.lengthM()));
    }

    /**
     * Cut {@code passes} passes of {@code driveForce} into {@code workpiece}'s shape, or empty if
     * no row machines this form by this operation.
     */
    public Optional<Removing.Cut> cut(ItemStack workpiece, St driveForce, St toolHardness, Operation operation, float passes) {
        Optional<Material.Located> located = part(workpiece);
        if (located.isEmpty())
            return Optional.empty();
        Optional<Step> step = steps.stream()
                .filter(s -> s.form().equals(located.get().form()) && s.operation() == operation).findFirst();
        if (step.isEmpty())
            return Optional.empty();
        Material material = located.get().material();
        ItemStack swarf = material.item(located.get().id(), "swarf").map(ItemStack::new).orElse(ItemStack.EMPTY);
        if (toolHardness.value() <= material.hardness().value())
            return Optional.of(new Removing.Cut(workpiece, Removing.Outcome.WRONG_TOOL, 0f, 0f, swarf));
        float fu = driveForce.value() / material.hardness().value() * passes;
        MadeOf made = workpiece.get(FDataComponents.MADE_OF.get());
        BarrelGeometry g = geometry(made);
        float d = step.get().perFu() * fu;
        BarrelGeometry cut = switch (step.get().dimension()) {
            case BORE -> new BarrelGeometry(g.boreMm() + d, g.outerMm(), g.lengthM());
            case OUTER -> new BarrelGeometry(g.boreMm(), g.outerMm() - d, g.lengthM());
            case LENGTH -> new BarrelGeometry(g.boreMm(), g.outerMm(), g.lengthM() - d);
        };
        ItemStack result;
        if (cut.whole()) {
            result = workpiece.copy();
            if (made != null)
                result.set(FDataComponents.MADE_OF.get(), withGeometry(made, cut));
        } else // cut through: nothing left that is a barrel
            result = swarf.copy();
        return Optional.of(new Removing.Cut(result, Removing.Outcome.WORKED, fu, 0f, swarf));
    }
}
