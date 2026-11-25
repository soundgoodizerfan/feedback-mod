// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.camera;

import com.mojang.blaze3d.platform.NativeImage;

import io.github.soundgoodizerfan.feedback.item.CameraItem;
import io.github.soundgoodizerfan.feedback.item.Photo;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.item.TelescopeFocus;
import io.github.soundgoodizerfan.feedback.net.PhotoPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Takes a photograph on the client: grabs the frame as the game last drew it, crops it to the middle {@code 1 / magnification} of the picture (the
 * lens's zoom), shrinks it to {@link Photo#WIDTH} by {@link Photo#HEIGHT}, blurs it by the lens's blur, and sends the pixels to the server, which hands
 * over the photograph. With no telescope in the other hand the camera is a pinhole: no zoom and {@link CameraItem#PINHOLE_BLUR}.
 */
public final class CameraShot {

    private CameraShot() {
    }

    public static void take(Player player) {
        Minecraft mc = Minecraft.getInstance();
        ItemStack lens = CameraItem.lens(player, player.getUseItem());
        float zoom = 1f, blur = CameraItem.PINHOLE_BLUR;
        if (!lens.isEmpty()) {
            ModularFigures f = ModularStacks.figures(lens);
            zoom = f == null ? 1f : Math.max(1f, f.get(Fig.MAGNIFICATION));
            blur = TelescopeFocus.blur(lens, player.level());
        }
        try (NativeImage frame = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            byte[] rgb = shrink(frame, zoom);
            soften(rgb, Math.round(blur * 5f));
            PacketDistributor.sendToServer(new PhotoPayload(new Photo(rgb)));
        }
    }

    /** The middle {@code 1 / zoom} of the frame as {@link Photo#WIDTH} by {@link Photo#HEIGHT} pixels, each the average of the block of frame pixels it covers. */
    private static byte[] shrink(NativeImage frame, float zoom) {
        int cw = Math.max(Photo.WIDTH, Math.round(frame.getWidth() / zoom)), ch = Math.max(Photo.HEIGHT, Math.round(frame.getHeight() / zoom));
        int x0 = (frame.getWidth() - cw) / 2, y0 = (frame.getHeight() - ch) / 2;
        byte[] out = new byte[Photo.WIDTH * Photo.HEIGHT * 3];
        for (int y = 0; y < Photo.HEIGHT; y++)
            for (int x = 0; x < Photo.WIDTH; x++) {
                int bx0 = x0 + x * cw / Photo.WIDTH, bx1 = Math.max(bx0 + 1, x0 + (x + 1) * cw / Photo.WIDTH);
                int by0 = y0 + y * ch / Photo.HEIGHT, by1 = Math.max(by0 + 1, y0 + (y + 1) * ch / Photo.HEIGHT);
                long r = 0, g = 0, b = 0;
                int n = 0;
                for (int py = by0; py < by1 && py < frame.getHeight(); py++)
                    for (int px = bx0; px < bx1 && px < frame.getWidth(); px++) {
                        int abgr = frame.getPixelRGBA(px, py);
                        r += abgr & 0xFF;
                        g += (abgr >> 8) & 0xFF;
                        b += (abgr >> 16) & 0xFF;
                        n++;
                    }
                int i = (y * Photo.WIDTH + x) * 3;
                n = Math.max(1, n);
                out[i] = (byte) (r / n);
                out[i + 1] = (byte) (g / n);
                out[i + 2] = (byte) (b / n);
            }
        return out;
    }

    /** A box blur of {@code radius} pixels, one pass each way. */
    private static void soften(byte[] rgb, int radius) {
        if (radius <= 0)
            return;
        byte[] tmp = new byte[rgb.length];
        pass(rgb, tmp, 1, 0, radius);
        pass(tmp, rgb, 0, 1, radius);
    }

    private static void pass(byte[] from, byte[] to, int dx, int dy, int radius) {
        for (int y = 0; y < Photo.HEIGHT; y++)
            for (int x = 0; x < Photo.WIDTH; x++)
                for (int c = 0; c < 3; c++) {
                    int sum = 0, n = 0;
                    for (int k = -radius; k <= radius; k++) {
                        int sx = x + k * dx, sy = y + k * dy;
                        if (sx < 0 || sy < 0 || sx >= Photo.WIDTH || sy >= Photo.HEIGHT)
                            continue;
                        sum += from[(sy * Photo.WIDTH + sx) * 3 + c] & 0xFF;
                        n++;
                    }
                    to[(y * Photo.WIDTH + x) * 3 + c] = (byte) (sum / Math.max(1, n));
                }
    }
}
