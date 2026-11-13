// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * What the server tells the crosshair HUD ({@code client.AimHud}) in place of the action bar: a reading held on the thing it was
 * taken from, a needle, a mode strip, a half-made link, a short cue, and a status line for an open screen. The {@code send}
 * statics are the server side and live here, outside {@code client/}, so a dedicated server never loads a client class
 * (design digest). Every figure arrives already composed and quantised by the server: the client only draws.
 */
public final class AimPayloads {

    private AimPayloads() {
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, List<Component>> LINES =
            ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list(16));

    // ---- Reading: a card beside the crosshair, held while the player still aims at what was read ----

    /** No target: shown for {@link #FREE_TICKS} wherever the player looks. */
    public static final byte NONE = 0, BLOCK = 1, ENTITY = 2;
    private static final int FREE_TICKS = 60;

    /**
     * {@code quantity} is -1 for a text-only card; otherwise the dial reads {@code value}, the figure as the server already
     * quantised it to the instrument's step.
     */
    public record Reading(byte target, BlockPos pos, int entity, Component title, List<Component> lines, int quantity, float value)
            implements CustomPacketPayload {

        public static final Type<Reading> TYPE = new Type<>(Feedback.id("aim_reading"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Reading> STREAM_CODEC = StreamCodec.of((buf, r) -> {
            buf.writeByte(r.target);
            buf.writeBlockPos(r.pos);
            buf.writeVarInt(r.entity);
            ComponentSerialization.STREAM_CODEC.encode(buf, r.title);
            LINES.encode(buf, r.lines);
            buf.writeVarInt(r.quantity + 1);
            buf.writeFloat(r.value);
        }, buf -> new Reading(buf.readByte(), buf.readBlockPos(), buf.readVarInt(), ComponentSerialization.STREAM_CODEC.decode(buf),
                LINES.decode(buf), buf.readVarInt() - 1, buf.readFloat()));

        @Override
        public Type<Reading> type() {
            return TYPE;
        }

        static void handle(Reading r, IPayloadContext context) {
            context.enqueueWork(() -> io.github.soundgoodizerfan.feedback.client.hud.AimHud.reading(r.target, r.pos, r.entity, r.title,
                    r.lines, r.quantity, r.value, FREE_TICKS));
        }
    }

    /** An instrument's figure on a dial, held on the block it was taken from. {@code shown} is already quantised. */
    public static void dial(Player player, BlockPos pos, Instrument self, Component figure, Quantity quantity, float shown) {
        if (player instanceof ServerPlayer sp)
            PacketDistributor.sendToPlayer(sp, new Reading(BLOCK, pos, -1, self.label(), List.of(figure), quantity.ordinal(), shown));
    }

    /** An instrument's figure on a dial, held on the creature it was taken from. */
    public static void dial(Player player, Entity target, Instrument self, Component figure, Quantity quantity, float shown) {
        if (player instanceof ServerPlayer sp)
            PacketDistributor.sendToPlayer(sp, new Reading(ENTITY, BlockPos.ZERO, target.getId(), self.label(), List.of(figure),
                    quantity.ordinal(), shown));
    }

    /** An instrument's figure on a dial with nothing in the world to hold it on (a reading of the player's own kit). */
    public static void dial(Player player, Instrument self, Component figure, Quantity quantity, float shown) {
        if (player instanceof ServerPlayer sp)
            PacketDistributor.sendToPlayer(sp, new Reading(NONE, BlockPos.ZERO, -1, self.label(), List.of(figure), quantity.ordinal(), shown));
    }

    /** An instrument's words with no single figure to dial, held on the creature. */
    public static void read(Player player, Entity target, Instrument self, Component text) {
        if (player instanceof ServerPlayer sp)
            PacketDistributor.sendToPlayer(sp, new Reading(ENTITY, BlockPos.ZERO, target.getId(), self.label(), List.of(text), -1, 0));
    }

    /** An instrument's words with no single figure to dial, held on the block. */
    public static void read(Player player, BlockPos pos, Instrument self, Component text) {
        if (player instanceof ServerPlayer sp)
            PacketDistributor.sendToPlayer(sp, new Reading(BLOCK, pos, -1, self.label(), List.of(text), -1, 0));
    }

    /** A block's own report (a right-click on it), titled with the block's name and held while the player looks at it. */
    public static void report(Player player, BlockPos pos, Component text) {
        if (player instanceof ServerPlayer sp)
            PacketDistributor.sendToPlayer(sp, new Reading(BLOCK, pos, -1, sp.level().getBlockState(pos).getBlock().getName(),
                    List.of(text), -1, 0));
    }

    // ---- Bearing: a needle orbiting the crosshair ----

    /** {@code yaw} is the world bearing in Minecraft's yaw (0 south, 90 west); {@code step} snaps it relative to the player's facing (0 for none). */
    public record Bearing(boolean point, float yaw, int step, Component caption, int ticks) implements CustomPacketPayload {

        public static final Type<Bearing> TYPE = new Type<>(Feedback.id("aim_bearing"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Bearing> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Bearing::point,
                ByteBufCodecs.FLOAT, Bearing::yaw,
                ByteBufCodecs.VAR_INT, Bearing::step,
                ComponentSerialization.STREAM_CODEC, Bearing::caption,
                ByteBufCodecs.VAR_INT, Bearing::ticks,
                Bearing::new);

        @Override
        public Type<Bearing> type() {
            return TYPE;
        }

        static void handle(Bearing b, IPayloadContext context) {
            context.enqueueWork(() -> io.github.soundgoodizerfan.feedback.client.hud.AimHud.bearing(b.point, b.yaw, b.step, b.caption, b.ticks));
        }
    }

    public static void bearing(Player player, boolean point, float yaw, int step, Component caption, int ticks) {
        if (player instanceof ServerPlayer sp)
            PacketDistributor.sendToPlayer(sp, new Bearing(point, yaw, step, caption, ticks));
    }

    // ---- Modes: every option of a setting, the current one lit ----

    public record Modes(Component title, List<Component> options, int index, Component detail) implements CustomPacketPayload {

        public static final Type<Modes> TYPE = new Type<>(Feedback.id("aim_modes"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Modes> STREAM_CODEC = StreamCodec.composite(
                ComponentSerialization.STREAM_CODEC, Modes::title,
                LINES, Modes::options,
                ByteBufCodecs.VAR_INT, Modes::index,
                ComponentSerialization.STREAM_CODEC, Modes::detail,
                Modes::new);

        @Override
        public Type<Modes> type() {
            return TYPE;
        }

        static void handle(Modes m, IPayloadContext context) {
            context.enqueueWork(() -> io.github.soundgoodizerfan.feedback.client.hud.AimHud.modes(m.title, m.options, m.index, m.detail));
        }
    }

    /** The setting now stands at {@code index} of {@code options}; {@code detail} (may be empty) says what that one does. */
    public static void modes(Player player, Component title, List<Component> options, int index, Component detail) {
        if (player instanceof ServerPlayer sp)
            PacketDistributor.sendToPlayer(sp, new Modes(title, options, index, detail));
    }

    /** Options from translation keys {@code prefix + name}. */
    public static List<Component> keys(String prefix, String... names) {
        List<Component> out = new ArrayList<>(names.length);
        for (String n : names)
            out.add(Component.translatable(prefix + n));
        return out;
    }

    // ---- Link: a half-made link, from its first end ----

    /** {@code none} clears it. {@code line} draws a line from {@code pos} to the aim (false where something else already draws one). */
    public record Link(boolean none, boolean line, BlockPos pos, Component from) implements CustomPacketPayload {

        public static final Type<Link> TYPE = new Type<>(Feedback.id("aim_link"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Link> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Link::none,
                ByteBufCodecs.BOOL, Link::line,
                BlockPos.STREAM_CODEC, Link::pos,
                ComponentSerialization.STREAM_CODEC, Link::from,
                Link::new);

        @Override
        public Type<Link> type() {
            return TYPE;
        }

        static void handle(Link l, IPayloadContext context) {
            context.enqueueWork(() -> io.github.soundgoodizerfan.feedback.client.hud.AimHud.link(l.none ? null : l.from, l.line ? l.pos : null));
        }
    }

    /** A link is waiting for its second end at {@code pos} (null: no line, only the chip). */
    public static void linkFrom(Player player, @Nullable BlockPos pos, Component from) {
        if (player instanceof ServerPlayer sp)
            PacketDistributor.sendToPlayer(sp, new Link(false, pos != null, pos == null ? BlockPos.ZERO : pos, from));
    }

    public static void linkDone(Player player) {
        if (player instanceof ServerPlayer sp)
            PacketDistributor.sendToPlayer(sp, new Link(true, false, BlockPos.ZERO, Component.empty()));
    }

    // ---- Cue: a short word on the gun card or at the crosshair ----

    public static final byte GUN = 0, CROSSHAIR = 1;

    public record Cue(byte where, Component text, int ticks) implements CustomPacketPayload {

        public static final Type<Cue> TYPE = new Type<>(Feedback.id("aim_cue"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Cue> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BYTE, Cue::where,
                ComponentSerialization.STREAM_CODEC, Cue::text,
                ByteBufCodecs.VAR_INT, Cue::ticks,
                Cue::new);

        @Override
        public Type<Cue> type() {
            return TYPE;
        }

        static void handle(Cue c, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (c.where == GUN)
                    io.github.soundgoodizerfan.feedback.client.gun.GunHud.cue(c.text, c.ticks);
                else
                    io.github.soundgoodizerfan.feedback.client.hud.AimHud.cue(c.text, c.ticks);
            });
        }
    }

    public static void cue(Player player, byte where, Component text, int ticks) {
        if (player instanceof ServerPlayer sp)
            PacketDistributor.sendToPlayer(sp, new Cue(where, text, ticks));
    }

    // ---- Screen status: a refusal said inside the open screen, not under it ----

    public record ScreenStatus(Component text) implements CustomPacketPayload {

        public static final Type<ScreenStatus> TYPE = new Type<>(Feedback.id("screen_status"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ScreenStatus> STREAM_CODEC =
                ComponentSerialization.STREAM_CODEC.map(ScreenStatus::new, ScreenStatus::text);

        @Override
        public Type<ScreenStatus> type() {
            return TYPE;
        }

        static void handle(ScreenStatus s, IPayloadContext context) {
            context.enqueueWork(() -> io.github.soundgoodizerfan.feedback.client.gui.SkinnedScreen.status(s.text));
        }
    }

    /** Says {@code text} on the status strip of the screen the player has open (the HUD readout stack if none takes it). */
    public static void screen(Player player, Component text) {
        if (player instanceof ServerPlayer sp)
            PacketDistributor.sendToPlayer(sp, new ScreenStatus(text));
    }

    /** Registered from {@code Feedback}'s payload event. */
    public static void register(PayloadRegistrar r) {
        r.playToClient(Reading.TYPE, Reading.STREAM_CODEC, Reading::handle)
                .playToClient(Bearing.TYPE, Bearing.STREAM_CODEC, Bearing::handle)
                .playToClient(Modes.TYPE, Modes.STREAM_CODEC, Modes::handle)
                .playToClient(Link.TYPE, Link.STREAM_CODEC, Link::handle)
                .playToClient(Cue.TYPE, Cue.STREAM_CODEC, Cue::handle)
                .playToClient(ScreenStatus.TYPE, ScreenStatus.STREAM_CODEC, ScreenStatus::handle);
    }
}
