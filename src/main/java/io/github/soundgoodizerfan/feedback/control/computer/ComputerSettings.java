// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.computer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * How one Computer's desktop looks: the phosphor, the wallpaper, the CRT effects, which widgets sit on the wallpaper and which side the
 * drive icons are on. Purely cosmetic and kept on the block, so everyone who opens that Computer sees the same desktop. Enums are stored by
 * ordinal and clamped on read, so a bad value from a client or a file can only ever pick a valid choice.
 */
public record ComputerSettings(Phosphor phosphor, Wallpaper wallpaper, boolean scanlines, boolean glow, boolean band, boolean boot,
                               boolean clock, boolean scope, boolean memory, boolean iconsRight) {

    public enum Phosphor { GREEN, AMBER, WHITE, BLUE }

    public enum Wallpaper { GRID, SCANS, CIRCUIT, HORIZON, BLANK }

    public static final ComputerSettings DEFAULT = new ComputerSettings(Phosphor.GREEN, Wallpaper.GRID, true, true, true, true, true, true, true, false);

    public static final StreamCodec<RegistryFriendlyByteBuf, ComputerSettings> STREAM_CODEC = StreamCodec.of(
            (buf, s) -> {
                buf.writeByte(s.phosphor.ordinal());
                buf.writeByte(s.wallpaper.ordinal());
                buf.writeByte(s.flags());
            },
            buf -> of(buf.readByte(), buf.readByte(), buf.readByte()));

    private int flags() {
        int f = 0;
        boolean[] bits = {scanlines, glow, band, boot, clock, scope, memory, iconsRight};
        for (int i = 0; i < bits.length; i++)
            if (bits[i])
                f |= 1 << i;
        return f;
    }

    private static ComputerSettings of(int phosphor, int wallpaper, int f) {
        Phosphor[] p = Phosphor.values();
        Wallpaper[] w = Wallpaper.values();
        return new ComputerSettings(p[Math.floorMod(phosphor, p.length)], w[Math.floorMod(wallpaper, w.length)],
                (f & 1) != 0, (f & 2) != 0, (f & 4) != 0, (f & 8) != 0, (f & 16) != 0, (f & 32) != 0, (f & 64) != 0, (f & 128) != 0);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putByte("Phosphor", (byte) phosphor.ordinal());
        tag.putByte("Wallpaper", (byte) wallpaper.ordinal());
        tag.putByte("Flags", (byte) flags());
        return tag;
    }

    public static ComputerSettings load(CompoundTag tag) {
        return tag.isEmpty() ? DEFAULT : of(tag.getByte("Phosphor"), tag.getByte("Wallpaper"), tag.getByte("Flags") & 0xFF);
    }

    public ComputerSettings with(Phosphor p) {
        return new ComputerSettings(p, wallpaper, scanlines, glow, band, boot, clock, scope, memory, iconsRight);
    }

    public ComputerSettings with(Wallpaper w) {
        return new ComputerSettings(phosphor, w, scanlines, glow, band, boot, clock, scope, memory, iconsRight);
    }

    /** One of the eight switches, by index: scanlines, glow, band, boot, clock, scope, memory, icons on the right. */
    public ComputerSettings toggled(int bit) {
        return of(phosphor.ordinal(), wallpaper.ordinal(), flags() ^ (1 << Math.floorMod(bit, 8)));
    }

    public boolean flag(int bit) {
        return (flags() & (1 << bit)) != 0;
    }
}
