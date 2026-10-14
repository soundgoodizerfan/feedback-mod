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
package io.github.soundgoodizerfan.feedback.registry;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.weapon.GunDesign;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.FireMode;
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;
import io.github.soundgoodizerfan.feedback.weapon.CartridgeItem;
import io.github.soundgoodizerfan.feedback.weapon.Magazine;
import io.github.soundgoodizerfan.feedback.weapon.MagazineItem;
import io.github.soundgoodizerfan.feedback.weapon.MagazineSize;
import io.github.soundgoodizerfan.feedback.weapon.BarrelGeometry;
import io.github.soundgoodizerfan.feedback.weapon.ReloadingPressBlock;
import io.github.soundgoodizerfan.feedback.weapon.ReloadingPressBlockEntity;
import io.github.soundgoodizerfan.feedback.weapon.GunBlueprintItem;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunStandBlock;
import io.github.soundgoodizerfan.feedback.weapon.GunStandBlockEntity;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Weapons: every gun registration in one place ({@code feedback_guns_spec.md}). */
public class FWeapons {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Feedback.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Feedback.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Feedback.MOD_ID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Feedback.MOD_ID);

    /** What a gun was designed as -- the settings, and nothing derived. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GunDesign>> GUN_DESIGN =
            COMPONENTS.register("gun_design", () -> DataComponentType.<GunDesign>builder()
                    .persistent(GunDesign.CODEC)
                    .networkSynchronized(GunDesign.STREAM_CODEC)
                    .build());

    /** What that design makes, baked once (spec section 2). The client predicts shots from this. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GunFigures>> GUN_FIGURES =
            COMPONENTS.register("gun_figures", () -> DataComponentType.<GunFigures>builder()
                    .persistent(GunFigures.CODEC)
                    .networkSynchronized(GunFigures.STREAM_CODEC)
                    .build());

    /**
     * Spec step 1's one hand-authored gun: a 9 mm, medium barrel, standard wall, in iron.
     * Creative-tab only; there is no way to make one yet and no ammunition to spend (step 3).
     */
    public static final DeferredItem<Item> TEST_PISTOL = ITEMS.register("test_pistol",
            () -> new GunItem(new Item.Properties()
                    .component(GUN_DESIGN.get(), GunDesign.starter(Feedback.id("iron")))));

    /** The rounds a magazine or a gun's well holds (spec 5). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Magazine>> MAGAZINE =
            COMPONENTS.register("magazine", () -> DataComponentType.<Magazine>builder()
                    .persistent(Magazine.CODEC)
                    .networkSynchronized(Magazine.STREAM_CODEC)
                    .build());

    /** The round in the chamber, if any: what the next trigger pull fires. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Cartridge>> CHAMBER =
            COMPONENTS.register("chamber", () -> DataComponentType.<Cartridge>builder()
                    .persistent(Cartridge.CODEC)
                    .networkSynchronized(Cartridge.STREAM_CODEC)
                    .build());

    /**
     * Damage this gun has taken from overpressure, 0 up; at 1 it is spent. Kept apart from wear
     * (spec 6: worn against damaged by overpressure are two lines), and it is misuse's record.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> GUN_DAMAGE =
            COMPONENTS.register("gun_damage", () -> DataComponentType.<Float>builder()
                    .persistent(com.mojang.serialization.Codec.FLOAT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.FLOAT)
                    .build());

    /** Bore erosion: 0 as built, rising with use and with heat. Condition is 1 - wear, floored. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> GUN_WEAR =
            COMPONENTS.register("gun_wear", () -> DataComponentType.<Float>builder()
                    .persistent(com.mojang.serialization.Codec.FLOAT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.FLOAT)
                    .build());

    /** What one trigger pull does. Absent means automatic, which is what held fire always was. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FireMode>> FIRE_MODE =
            COMPONENTS.register("fire_mode", () -> DataComponentType.<FireMode>builder()
                    .persistent(FireMode.CODEC)
                    .networkSynchronized(FireMode.STREAM_CODEC)
                    .build());

    /** A magazine item's size band, and on a gun the size of the magazine in it (spec 12). Absent reads standard. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MagazineSize>> MAGAZINE_SIZE =
            COMPONENTS.register("magazine_size", () -> DataComponentType.<MagazineSize>builder()
                    .persistent(MagazineSize.CODEC)
                    .networkSynchronized(MagazineSize.STREAM_CODEC)
                    .build());

    /** A machined barrel's real bore, outside and length (spec 12). Absent reads as fresh from the drill. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BarrelGeometry>> BARREL_GEOMETRY =
            COMPONENTS.register("barrel_geometry", () -> DataComponentType.<BarrelGeometry>builder()
                    .persistent(BarrelGeometry.CODEC)
                    .networkSynchronized(BarrelGeometry.STREAM_CODEC)
                    .build());

    /** What a loaded round is. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Cartridge>> CARTRIDGE =
            COMPONENTS.register("cartridge", () -> DataComponentType.<Cartridge>builder()
                    .persistent(Cartridge.CODEC)
                    .networkSynchronized(Cartridge.STREAM_CODEC)
                    .build());

    public static final DeferredItem<CartridgeItem> CARTRIDGE_ITEM = ITEMS.register("cartridge",
            () -> new CartridgeItem(new Item.Properties()));

    public static final DeferredItem<MagazineItem> MAGAZINE_ITEM = ITEMS.register("magazine",
            () -> new MagazineItem(new Item.Properties()));

    /** Seats a case, a charge and a projectile (spec 5). */
    public static final DeferredBlock<ReloadingPressBlock> RELOADING_PRESS = BLOCKS.register("reloading_press",
            () -> new ReloadingPressBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL)));

    public static final DeferredItem<BlockItem> RELOADING_PRESS_ITEM = ITEMS.registerSimpleBlockItem(RELOADING_PRESS);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReloadingPressBlockEntity>> PRESS_ENTITY =
            BLOCK_ENTITIES.register("reloading_press",
                    () -> BlockEntityType.Builder.of(ReloadingPressBlockEntity::new, RELOADING_PRESS.get()).build(null));

    /** A gun made from a blueprint: no default design, it carries the blueprint's (spec section 4). */
    public static final DeferredItem<GunItem> GUN = ITEMS.register("gun", () -> new GunItem(new Item.Properties()));

    public static final DeferredItem<GunBlueprintItem> GUN_BLUEPRINT = ITEMS.register("gun_blueprint",
            () -> new GunBlueprintItem(new Item.Properties().stacksTo(1)));

    /** Where guns are designed and printed (spec section 4). */
    public static final DeferredBlock<GunStandBlock> GUN_STAND = BLOCKS.register("gun_stand",
            () -> new GunStandBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL)));

    public static final DeferredItem<BlockItem> GUN_STAND_ITEM = ITEMS.registerSimpleBlockItem(GUN_STAND);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GunStandBlockEntity>> GUN_STAND_ENTITY =
            BLOCK_ENTITIES.register("gun_stand",
                    () -> BlockEntityType.Builder.of(GunStandBlockEntity::new, GUN_STAND.get()).build(null));

    private FWeapons() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
