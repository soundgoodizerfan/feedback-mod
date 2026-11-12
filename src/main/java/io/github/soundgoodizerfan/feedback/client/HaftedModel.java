// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

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

    private static final class Resolver extends ItemOverrides {
        private final BakedModel base;
        private final Map<String, TextureAtlasSprite> sprites = new HashMap<>();
        private final Map<String, int[]> bounds = new HashMap<>();
        private final Map<List<HaftedLayers.Placement>, BakedModel> built = new HashMap<>();

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
            ModularType type = design == null ? null : ModularTypes.known().get(design.type());
            if (type != null) {
                List<HaftedLayers.Placement> layers = HaftedLayers.of(design, type, known);
                if (!layers.isEmpty())
                    return built.computeIfAbsent(layers, l -> bake(l, type));
            }
            return base.getOverrides().resolve(base, stack, level, entity, seed);
        }

        private BakedModel bake(List<HaftedLayers.Placement> layers, ModularType type) {
            SimpleBakedModel.Builder out = new SimpleBakedModel.Builder(false, false, false, base.getTransforms(), ItemOverrides.EMPTY);
            List<String> order = type.order();
            for (HaftedLayers.Placement p : layers) {
                TextureAtlasSprite sprite = sprite(p.file());
                List<BlockElement> elements = new ArrayList<>(UnbakedGeometryHelper.createUnbakedItemElements(Math.max(0, order.indexOf(p.role())), sprite));
                for (BlockElement e : elements) {
                    e.from.add(p.dx(), -p.dy(), 0);
                    e.to.add(p.dx(), -p.dy(), 0);
                }
                for (BakedQuad q : UnbakedGeometryHelper.bakeElements(elements, m -> sprite, BlockModelRotation.X0_Y0))
                    out.addUnculledFace(q);
            }
            return out.particle(sprite(layers.get(0).file())).build();
        }
    }
}
