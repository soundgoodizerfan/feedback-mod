// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

/**
 * A substrate with traces laid on it: the data component on a trace board. It
 * records what was done to the board and nothing derived: the trace route, the metal or dust the
 * traces are, the substrate, the solder once pasted, and how the joints came out. Ratings, limits
 * and windows are read from the materials when asked, so a retuned material retunes its boards.
 *
 * @param trace         how the traces were laid
 * @param traceMaterial the fine wire's metal, or the paste's dust
 * @param substrate     the ceramic the board is on
 * @param solder        the solder pasted on, once it is
 * @param joint         how the joints came out
 * @param parts         what is mounted on it (phase 4): soldered on with the traces, read into a
 *                      {@link Circuit} when the joints are graded
 */
public record Board(Trace trace, ResourceLocation traceMaterial, ResourceLocation substrate,
                    Optional<ResourceLocation> solder, Joint joint, List<Part> parts) {

    /** What a mounted part is to a circuit. */
    public enum Kind implements StringRepresentable {
        /** A rectifying crystal: fast, quiet in proportion to its figure. */
        SWITCH("switch"),
        /** A relay: contacts, slow and fairly clean. */
        RELAY("relay"),
        /** A coherer: slowest and noisiest, the granular part. */
        COHERER("coherer"),
        /** A ringing piezo plate: switches nothing, steadies the rest. */
        CLOCK("clock"),
        /** A sealed vacuum tube: fast and quiet once its heater has warmed it, carries its own figures ({@link Part#tube}). */
        TUBE("tube"),
        /** An amethyst resonator: switches nothing, sets a host's clock steadiness, channels and link bandwidth ({@link Part#line}). */
        RESONATOR("resonator"),
        /** A sculk lattice: switches nothing, holds extra variables and learns repeated readings ({@link Part#line}). */
        LATTICE("lattice");

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        private final String key;

        Kind(String key) {
            this.key = key;
        }

        @Override
        public String getSerializedName() {
            return key;
        }
    }

    /**
     * One mounted part: its class and the material figure that sets how good it is (a crystal's
     * {@code rectifying}, a plate's {@code piezo_coupling}; 1 for the classes that have none). A tube carries its own figures.
     */
    public record Part(Kind kind, float figure, Optional<Tube> tube, Optional<Circuit.Line> line, Optional<ItemStack> source, boolean dead) {
        public Part(Kind kind, float figure) {
            this(kind, figure, Optional.empty(), Optional.empty(), Optional.empty(), false);
        }

        public Part(Kind kind, float figure, Optional<Tube> tube) {
            this(kind, figure, tube, Optional.empty(), Optional.empty(), false);
        }

        public Part(Kind kind, float figure, Optional<Tube> tube, Optional<Circuit.Line> line) {
            this(kind, figure, tube, line, Optional.empty(), false);
        }

        /** The same part remembering the item that was mounted, so a lift hands that exact item back. */
        public Part withSource(ItemStack stack) {
            return new Part(kind, figure, tube, line, stack.isEmpty() ? Optional.empty() : Optional.of(stack.copyWithCount(1)), dead);
        }

        /** The same part cooked: still on the board, no longer an element, and nothing comes back when it is lifted. */
        public Part ruined() {
            return new Part(kind, figure, tube, line, Optional.empty(), true);
        }

        // An ItemStack compares by identity; a board must still stack with an equal one.
        @Override
        public boolean equals(Object o) {
            return o instanceof Part p && kind == p.kind && Float.compare(figure, p.figure) == 0 && dead == p.dead && tube.equals(p.tube)
                    && line.equals(p.line) && source.isPresent() == p.source.isPresent()
                    && (source.isEmpty() || ItemStack.matches(source.get(), p.source.get()));
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(kind, figure, dead, tube, line, source.map(ItemStack::getItem).orElse(null));
        }

        public static final Codec<Part> CODEC = RecordCodecBuilder.create(i -> i.group(
                Kind.CODEC.fieldOf("kind").forGetter(Part::kind),
                Codec.FLOAT.fieldOf("figure").forGetter(Part::figure),
                Tube.CODEC.optionalFieldOf("tube").forGetter(Part::tube),
                Circuit.Line.CODEC.optionalFieldOf("line").forGetter(Part::line),
                ItemStack.CODEC.optionalFieldOf("source").forGetter(Part::source),
                Codec.BOOL.optionalFieldOf("dead", false).forGetter(Part::dead)).apply(i, Part::new));
    }

    /** How the traces were laid. */
    public enum Trace implements StringRepresentable {
        /** Fine wire on the substrate: fine, and carries power. */
        ETCHED("etched"),
        /** Paste printed on the substrate: quantised, lossy, signal only. */
        PRINTED("printed");

        public static final Codec<Trace> CODEC = StringRepresentable.fromEnum(Trace::values);
        private final String key;

        Trace(String key) {
            this.key = key;
        }

        @Override
        public String getSerializedName() {
            return key;
        }
    }

    /** How the joints came out. A lifted trace is not a grade: it returns the bare substrate. */
    public enum Joint implements StringRepresentable {
        /** Nothing soldered yet. */
        NONE("none"),
        /** Solder did not flow, or burnt: an intermittent joint. Run again to rework it. */
        COLD("cold"),
        /** Flowed within the alloy's window. */
        GOOD("good");

        public static final Codec<Joint> CODEC = StringRepresentable.fromEnum(Joint::values);
        private final String key;

        Joint(String key) {
            this.key = key;
        }

        public String translationKey() {
            return "feedback.joint." + key;
        }

        @Override
        public String getSerializedName() {
            return key;
        }
    }

    public static final Codec<Board> CODEC = RecordCodecBuilder.create(i -> i.group(
            Trace.CODEC.fieldOf("trace").forGetter(Board::trace),
            ResourceLocation.CODEC.fieldOf("trace_material").forGetter(Board::traceMaterial),
            ResourceLocation.CODEC.fieldOf("substrate").forGetter(Board::substrate),
            ResourceLocation.CODEC.optionalFieldOf("solder").forGetter(Board::solder),
            Joint.CODEC.optionalFieldOf("joint", Joint.NONE).forGetter(Board::joint),
            Part.CODEC.listOf().optionalFieldOf("parts", List.of()).forGetter(Board::parts)).apply(i, Board::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Board> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public Board pasted(ResourceLocation solderMaterial) {
        return new Board(trace, traceMaterial, substrate, Optional.of(solderMaterial), Joint.NONE, parts);
    }

    public Board withJoint(Joint grade) {
        return new Board(trace, traceMaterial, substrate, solder, grade, parts);
    }

    /** This board with its solder and joint cleared (taken up by a wick) and these parts left on it. */
    public Board unsoldered(List<Part> remaining) {
        return new Board(trace, traceMaterial, substrate, Optional.empty(), Joint.NONE, List.copyOf(remaining));
    }

    /** This board with its parts replaced (cooked ones marked). */
    public Board withParts(List<Part> replaced) {
        return new Board(trace, traceMaterial, substrate, solder, joint, List.copyOf(replaced));
    }

    /** This board with one more part soldered-to-be. */
    public Board mounted(Part part) {
        List<Part> all = new ArrayList<>(parts);
        all.add(part);
        return new Board(trace, traceMaterial, substrate, solder, joint, List.copyOf(all));
    }
}
