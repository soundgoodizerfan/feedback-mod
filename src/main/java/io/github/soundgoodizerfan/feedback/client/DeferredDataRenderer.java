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
package io.github.soundgoodizerfan.feedback.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/**
 * Draws data-link nodes and their connections through walls -- invisible except with the
 * connector in hand, exactly what philosophy's own §"how data gets to the controller" describes.
 *
 * <h2>Read from MrCrayfish's Furniture Mod: Refurbished (MIT)</h2>
 * `DeferredElectricRenderer` on that mod's {@code 1.21.1} branch (not its default {@code 26.1.2}
 * branch, whose renderer targets a newer Minecraft's frame-graph API this toolchain doesn't
 * have). This version is plain and portable: queue colour-quad draws through the tick, then flush
 * them once into {@code mc.levelRenderer.entityTarget()} -- the same render target vanilla uses
 * for a glowing entity's outline, which is already composited over the finished world with depth
 * information intact. Drawing there is what makes a node visible through a wall without a custom
 * texture target or shader of our own.
 */
public final class DeferredDataRenderer {

    private static final DeferredDataRenderer INSTANCE = new DeferredDataRenderer();

    public static DeferredDataRenderer get() {
        return INSTANCE;
    }

    private final List<BiConsumer<PoseStack, VertexConsumer>> queued = new ArrayList<>();

    private DeferredDataRenderer() {
    }

    public void deferDraw(BiConsumer<PoseStack, VertexConsumer> consumer) {
        queued.add(consumer);
    }

    /** Called once per frame, after block entities render. See {@code FClientSetup}. */
    public void draw(PoseStack pose) {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget target = mc.levelRenderer.entityTarget();
        if (target == null || queued.isEmpty()) {
            queued.clear();   // or a frame with no target would replay its draws into the next
            return;
        }

        target.bindWrite(false);

        Tesselator tesselator = Tesselator.getInstance();
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(true);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        pose.pushPose();
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (BiConsumer<PoseStack, VertexConsumer> consumer : queued)
            consumer.accept(pose, builder);
        MeshData data = builder.build();
        if (data != null)
            BufferUploader.drawWithShader(data);
        pose.popPose();

        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);

        mc.getMainRenderTarget().bindWrite(false);
        queued.clear();
    }

    /** A solid-colour box, every face the same colour and alpha. */
    public void drawColouredBox(Matrix4f matrix, VertexConsumer consumer, AABB box, int colour, float alpha) {
        float red = FastColor.ARGB32.red(colour) / 255f;
        float green = FastColor.ARGB32.green(colour) / 255f;
        float blue = FastColor.ARGB32.blue(colour) / 255f;
        // North
        vertex(matrix, consumer, box.minX, box.maxY, box.minZ, red, green, blue, alpha);
        vertex(matrix, consumer, box.maxX, box.maxY, box.minZ, red, green, blue, alpha);
        vertex(matrix, consumer, box.maxX, box.minY, box.minZ, red, green, blue, alpha);
        vertex(matrix, consumer, box.minX, box.minY, box.minZ, red, green, blue, alpha);
        // South
        vertex(matrix, consumer, box.maxX, box.maxY, box.maxZ, red, green, blue, alpha);
        vertex(matrix, consumer, box.minX, box.maxY, box.maxZ, red, green, blue, alpha);
        vertex(matrix, consumer, box.minX, box.minY, box.maxZ, red, green, blue, alpha);
        vertex(matrix, consumer, box.maxX, box.minY, box.maxZ, red, green, blue, alpha);
        // West
        vertex(matrix, consumer, box.minX, box.maxY, box.maxZ, red, green, blue, alpha);
        vertex(matrix, consumer, box.minX, box.maxY, box.minZ, red, green, blue, alpha);
        vertex(matrix, consumer, box.minX, box.minY, box.minZ, red, green, blue, alpha);
        vertex(matrix, consumer, box.minX, box.minY, box.maxZ, red, green, blue, alpha);
        // East
        vertex(matrix, consumer, box.maxX, box.maxY, box.minZ, red, green, blue, alpha);
        vertex(matrix, consumer, box.maxX, box.maxY, box.maxZ, red, green, blue, alpha);
        vertex(matrix, consumer, box.maxX, box.minY, box.maxZ, red, green, blue, alpha);
        vertex(matrix, consumer, box.maxX, box.minY, box.minZ, red, green, blue, alpha);
        // Up
        vertex(matrix, consumer, box.minX, box.maxY, box.minZ, red, green, blue, alpha);
        vertex(matrix, consumer, box.minX, box.maxY, box.maxZ, red, green, blue, alpha);
        vertex(matrix, consumer, box.maxX, box.maxY, box.maxZ, red, green, blue, alpha);
        vertex(matrix, consumer, box.maxX, box.maxY, box.minZ, red, green, blue, alpha);
        // Down
        vertex(matrix, consumer, box.minX, box.minY, box.minZ, red, green, blue, alpha);
        vertex(matrix, consumer, box.maxX, box.minY, box.minZ, red, green, blue, alpha);
        vertex(matrix, consumer, box.maxX, box.minY, box.maxZ, red, green, blue, alpha);
        vertex(matrix, consumer, box.minX, box.minY, box.maxZ, red, green, blue, alpha);
    }

    private static void vertex(Matrix4f matrix, VertexConsumer consumer, double x, double y, double z,
                               float r, float g, float b, float a) {
        consumer.addVertex(matrix, (float) x, (float) y, (float) z).setColor(r, g, b, a);
    }
}
