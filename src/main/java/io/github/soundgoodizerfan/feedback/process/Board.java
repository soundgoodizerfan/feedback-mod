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

/**
 * A substrate with traces laid on it (mechanics 6.20): the data component on a trace board. It
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
        CLOCK("clock");

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
     * {@code rectifying}, a plate's {@code piezo_coupling}; 1 for the classes that have none).
     */
    public record Part(Kind kind, float figure) {
        public static final Codec<Part> CODEC = RecordCodecBuilder.create(i -> i.group(
                Kind.CODEC.fieldOf("kind").forGetter(Part::kind),
                Codec.FLOAT.fieldOf("figure").forGetter(Part::figure)).apply(i, Part::new));
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

    /** This board with one more part soldered-to-be. */
    public Board mounted(Part part) {
        List<Part> all = new ArrayList<>(parts);
        all.add(part);
        return new Board(trace, traceMaterial, substrate, solder, joint, List.copyOf(all));
    }
}
