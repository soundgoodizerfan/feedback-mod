// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.HaftedLayers;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;

import org.joml.Vector3f;

/**
 * The hafted tool drawn as a stack of layers (haft, parts, heads) the {@link HaftedLayers} layout places, each a 16x16 grey sprite from
 * {@code textures/item/hafted/} moved by whole texels and tinted by its part's material (the tint index is the role's place in the type's
 * order, as {@link ModularClient} tints). A build whose haft or head has no layer files falls through to the whole-tool models of the item
 * model's overrides, so the layers can land a batch at a time. The geometry is built once per distinct layout and kept.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HaftedModel {

    private HaftedModel() {
    }

    private static final ResourceLocation ITEM = Feedback.id("hafted_tool");

    @SubscribeEvent
    public static void wrap(ModelEvent.ModifyBakingResult event) {
        ModelResourceLocation key = ModelResourceLocation.inventory(ITEM);
        BakedModel base = event.getModels().get(key);
        if (base != null)
            event.getModels().put(key, new Layered(base));
    }

    private static final class Layered extends BakedModelWrapper<BakedModel> {
        private final ItemOverrides overrides;

        Layered(BakedModel base) {
            super(base);
            this.overrides = new Resolver(base);
        }

        @Override
        public ItemOverrides getOverrides() {
            return overrides;
        }
    }

    /**
     * The tool at two sizes: the icon that fits its slot in a screen, and the full-length tool everywhere in the world (in hand, on the
     * ground, in a frame), where a longer haft or blade takes up more space at the same texel size.
     */
    private static final class BySize extends BakedModelWrapper<BakedModel> {
        private final BakedModel held;

        BySize(BakedModel slot, BakedModel held) {
            super(slot);
            this.held = held;
        }

        @Override
        public BakedModel applyTransform(net.minecraft.world.item.ItemDisplayContext context, com.mojang.blaze3d.vertex.PoseStack pose, boolean leftHand) {
            return (context == net.minecraft.world.item.ItemDisplayContext.GUI ? originalModel : held).applyTransform(context, pose, leftHand);
        }
    }

    private static final class Resolver extends ItemOverrides {
        private final BakedModel base;
        private final Map<String, TextureAtlasSprite> sprites = new HashMap<>();
        private final Map<String, int[]> bounds = new HashMap<>();
        private final Map<List<HaftedLayers.Placement>, BakedModel> built = new HashMap<>();
        private final Map<List<BakedModel>, BakedModel> bySize = new HashMap<>();
        // The finished model per design, so a hotbar of tools costs one hash lookup each per frame, not a re-plan and re-flatten. Empty = no layer files, use the whole-tool fallback.
        private final Map<ModularDesign, java.util.Optional<BakedModel>> byDesign = new HashMap<>();

        Resolver(BakedModel base) {
            this.base = base;
        }

        private TextureAtlasSprite sprite(String file) {
            return sprites.computeIfAbsent(file, f -> Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .getSprite(Feedback.id("item/hafted/" + f)));
        }

        private final HaftedLayers.Sprites known = new HaftedLayers.Sprites() {
            @Override
            public boolean has(String file) {
                return !sprite(file).contents().name().equals(MissingTextureAtlasSprite.getLocation());
            }

            @Override
            public int lum(String file, int x, int y) {
                var c = sprite(file).contents();
                if (c.isTransparent(0, x, y))
                    return -1;
                int abgr = c.getOriginalImage().getPixelRGBA(x, y);
                return (abgr & 0xFF) + (abgr >> 8 & 0xFF) + (abgr >> 16 & 0xFF);
            }

            @Override
            public int[] bounds(String file) {
                return bounds.computeIfAbsent(file, f -> {
                    var c = sprite(f).contents();
                    int x0 = c.width(), y0 = c.height(), x1 = -1, y1 = -1;
                    for (int y = 0; y < c.height(); y++)
                        for (int x = 0; x < c.width(); x++)
                            if (!c.isTransparent(0, x, y)) {
                                x0 = Math.min(x0, x);
                                y0 = Math.min(y0, y);
                                x1 = Math.max(x1, x);
                                y1 = Math.max(y1, y);
                            }
                    return x1 < 0 ? null : new int[] {x0, y0, x1, y1};
                });
            }
        };

        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
            ModularDesign design = ModularStacks.design(stack);
            ModularType type = design == null ? null : ModularTypes.known(design.type());
            if (type != null) {
                var cached = byDesign.computeIfAbsent(design, d -> {
                    List<HaftedLayers.Placement> icon = HaftedLayers.of(d, type, known);
                    if (icon.isEmpty())
                        return java.util.Optional.empty();
                    BakedModel slot = built.computeIfAbsent(icon, l -> bake(l, type));
                    BakedModel held = built.computeIfAbsent(HaftedLayers.full(d, type, known), l -> bake(l, type));
                    return java.util.Optional.of(slot == held ? slot : bySize.computeIfAbsent(List.of(slot, held), k -> new BySize(slot, held)));
                });
                if (cached.isPresent())
                    return cached.get();
            }
            return base.getOverrides().resolve(base, stack, level, entity, seed);
        }

        private static final int PAD = HaftedLayers.PAD, GRID = HaftedLayers.GRID;

        private BakedModel bake(List<HaftedLayers.Placement> layers, ModularType type) {
            SimpleBakedModel.Builder out = new SimpleBakedModel.Builder(false, false, false, base.getTransforms(), ItemOverrides.EMPTY);
            List<String> order = type.order();
            // The flattened tool is HaftedLayers' (the offline sheet draws the same list): one texel per canvas position.
            List<HaftedLayers.Texel> texels = HaftedLayers.flatten(layers, known);
            int n = GRID;
            boolean[] filled = new boolean[n * n];
            for (HaftedLayers.Texel t : texels)
                filled[t.x() + PAD + (t.y() + PAD) * n] = true;
            List<BlockElement> elements = new ArrayList<>();
            for (HaftedLayers.Texel t : texels) {
                int x = t.x() + PAD, y = t.y() + PAD, cx = t.x(), cy = t.y();
                int tint = Math.max(0, order.indexOf(t.role()));
                String tex = Feedback.id("item/hafted/" + t.file()).toString();
                // UV of this texel in its own sprite, pulled in a hair so nearest sampling never picks up the neighbour.
                float u0 = t.ux() + 0.01f, v0 = t.uy() + 0.01f, u1 = u0 + 0.98f, v1 = v0 + 0.98f;
                Map<net.minecraft.core.Direction, BlockElementFace> faces = new java.util.EnumMap<>(net.minecraft.core.Direction.class);
                faces.put(net.minecraft.core.Direction.SOUTH, new BlockElementFace(null, tint, tex, new BlockFaceUV(new float[] {u0, v0, u1, v1}, 0)));
                faces.put(net.minecraft.core.Direction.NORTH, new BlockElementFace(null, tint, tex, new BlockFaceUV(new float[] {u1, v0, u0, v1}, 0)));
                BlockFaceUV edge = new BlockFaceUV(new float[] {u0, v0, u1, v1}, 0);
                if (x == 0 || !filled[x - 1 + y * n])
                    faces.put(net.minecraft.core.Direction.WEST, new BlockElementFace(null, tint, tex, edge));
                if (x == n - 1 || !filled[x + 1 + y * n])
                    faces.put(net.minecraft.core.Direction.EAST, new BlockElementFace(null, tint, tex, edge));
                if (y == 0 || !filled[x + (y - 1) * n])
                    faces.put(net.minecraft.core.Direction.UP, new BlockElementFace(null, tint, tex, edge));
                if (y == n - 1 || !filled[x + (y + 1) * n])
                    faces.put(net.minecraft.core.Direction.DOWN, new BlockElementFace(null, tint, tex, edge));
                elements.add(new BlockElement(new Vector3f(cx, 15 - cy, 7.5f), new Vector3f(cx + 1, 16 - cy, 8.5f), faces, null, true));
            }
            for (BakedQuad q : UnbakedGeometryHelper.bakeElements(elements, m -> sprite(m.texture().getPath().substring("item/hafted/".length())),
                    BlockModelRotation.X0_Y0))
                out.addUnculledFace(q);
            return out.particle(sprite(layers.get(0).file())).build();
        }
    }
}
