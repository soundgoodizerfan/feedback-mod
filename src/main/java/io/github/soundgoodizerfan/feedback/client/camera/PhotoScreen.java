// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.camera;

import com.mojang.blaze3d.platform.NativeImage;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.item.Photo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Looks at one photograph: its pixels become a dynamic texture drawn scaled up, edged in white. Esc closes it. */
public final class PhotoScreen extends Screen {

    private static final ResourceLocation TEXTURE = Feedback.id("photograph_view");
    private final DynamicTexture texture;

    private PhotoScreen(Photo photo) {
        super(Component.translatable("item.feedback.photograph"));
        NativeImage image = new NativeImage(Photo.WIDTH, Photo.HEIGHT, false);
        for (int y = 0; y < Photo.HEIGHT; y++)
            for (int x = 0; x < Photo.WIDTH; x++) {
                int i = (y * Photo.WIDTH + x) * 3;
                int r = photo.rgb()[i] & 0xFF, g = photo.rgb()[i + 1] & 0xFF, b = photo.rgb()[i + 2] & 0xFF;
                image.setPixelRGBA(x, y, 0xFF000000 | b << 16 | g << 8 | r);
            }
        texture = new DynamicTexture(image);
        Minecraft.getInstance().getTextureManager().register(TEXTURE, texture);
    }

    public static void open(Photo photo) {
        if (photo.valid())
            Minecraft.getInstance().setScreen(new PhotoScreen(photo));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        int scale = Math.max(1, Math.min((width - 40) / Photo.WIDTH, (height - 40) / Photo.HEIGHT));
        int w = Photo.WIDTH * scale, h = Photo.HEIGHT * scale, x = (width - w) / 2, y = (height - h) / 2;
        g.fill(x - 4, y - 4, x + w + 4, y + h + 4, 0xFFF4F1E8);
        g.blit(TEXTURE, x, y, 0, 0f, 0f, w, h, w, h);
    }

    @Override
    public void removed() {
        texture.close();
        Minecraft.getInstance().getTextureManager().release(TEXTURE);
    }
}
