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
import io.github.soundgoodizerfan.feedback.weapon.FireMode;
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;
import io.github.soundgoodizerfan.feedback.weapon.CartridgeItem;
import io.github.soundgoodizerfan.feedback.weapon.Magazine;
import io.github.soundgoodizerfan.feedback.weapon.MagazineItem;
import io.github.soundgoodizerfan.feedback.weapon.MagazineSize;
import io.github.soundgoodizerfan.feedback.weapon.ReloadingPressBlock;
import io.github.soundgoodizerfan.feedback.weapon.ReloadingPressBlockEntity;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
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

/** Weapons: every gun registration in one place ({@code docs/feedback_guns_spec.md}). */
public class FWeapons {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Feedback.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Feedback.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Feedback.MOD_ID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Feedback.MOD_ID);

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

    /** mB of steam in a steam gun's chest ({@link io.github.soundgoodizerfan.feedback.weapon.GunSteam}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> GUN_STEAM =
            COMPONENTS.register("gun_steam", () -> DataComponentType.<Float>builder()
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

    /** Where parts become a gun, read against a round (guns spec 13.6). */
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.weapon.GunTableBlock> GUN_TABLE =
            BLOCKS.register("gun_table", () -> new io.github.soundgoodizerfan.feedback.weapon.GunTableBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD)));

    /** A placed explosive, packed by hand and lit by anything hot (guns spec 13.4a). */
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.weapon.DemolitionChargeBlock> DEMOLITION_CHARGE =
            BLOCKS.register("demolition_charge", () -> new io.github.soundgoodizerfan.feedback.weapon.DemolitionChargeBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.5f).sound(SoundType.GRASS)));

    public static final DeferredItem<BlockItem> DEMOLITION_CHARGE_ITEM = ITEMS.registerSimpleBlockItem(DEMOLITION_CHARGE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<io.github.soundgoodizerfan.feedback.weapon.DemolitionChargeBlockEntity>> DEMOLITION_CHARGE_ENTITY =
            BLOCK_ENTITIES.register("demolition_charge", () -> BlockEntityType.Builder
                    .of(io.github.soundgoodizerfan.feedback.weapon.DemolitionChargeBlockEntity::new, DEMOLITION_CHARGE.get()).build(null));

    public static final DeferredItem<BlockItem> GUN_TABLE_ITEM = ITEMS.registerSimpleBlockItem(GUN_TABLE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<io.github.soundgoodizerfan.feedback.weapon.GunTableBlockEntity>> GUN_TABLE_ENTITY =
            BLOCK_ENTITIES.register("gun_table", () -> BlockEntityType.Builder
                    .of(io.github.soundgoodizerfan.feedback.weapon.GunTableBlockEntity::new, GUN_TABLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReloadingPressBlockEntity>> PRESS_ENTITY =
            BLOCK_ENTITIES.register("reloading_press",
                    () -> BlockEntityType.Builder.of(ReloadingPressBlockEntity::new, RELOADING_PRESS.get()).build(null));

    /** The mix in one batch of blended powder: what the press seats as the charge (guns spec 13.4b). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.List<Cartridge.Charge>>> BLEND =
            COMPONENTS.register("blend", () -> DataComponentType.<java.util.List<Cartridge.Charge>>builder()
                    .persistent(Cartridge.Charge.CODEC.listOf())
                    .networkSynchronized(Cartridge.Charge.STREAM_CODEC.apply(net.minecraft.network.codec.ByteBufCodecs.list()))
                    .build());

    public static final DeferredItem<io.github.soundgoodizerfan.feedback.weapon.PowderBlendItem> BLEND_ITEM =
            ITEMS.register("powder_blend", () -> new io.github.soundgoodizerfan.feedback.weapon.PowderBlendItem(
                    new Item.Properties().stacksTo(1)));

    /** Blends loose powders by turning (guns spec 13.4b). */
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.mixer.MixerBlock> MIXER =
            BLOCKS.register("mixer", () -> new io.github.soundgoodizerfan.feedback.machine.mixer.MixerBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL).noOcclusion()));

    public static final DeferredItem<BlockItem> MIXER_ITEM = ITEMS.registerSimpleBlockItem(MIXER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<io.github.soundgoodizerfan.feedback.machine.mixer.MixerBlockEntity>> MIXER_ENTITY =
            BLOCK_ENTITIES.register("mixer", () -> BlockEntityType.Builder
                    .of(io.github.soundgoodizerfan.feedback.machine.mixer.MixerBlockEntity::new, MIXER.get()).build(null));

    /** A gun: a modular item of the gun type, its parts and figures the core's (modular spec 8.3). */
    public static final DeferredItem<GunItem> GUN = ITEMS.register("gun", () -> new GunItem(new Item.Properties()));

    private FWeapons() {
    }

    /**
     * A shot is a vibration (spec 11 step 4's owed "gunfire as a vibration"): sculk hears it, and a
     * warden. Three radii, because a game event's reach is fixed per type: an open or braked muzzle
     * carries far, a suppressed one does not ({@code FTuning.GUN_QUIET_REPORT}).
     */
    public static final DeferredRegister<net.minecraft.world.level.gameevent.GameEvent> GAME_EVENTS =
            DeferredRegister.create(Registries.GAME_EVENT, Feedback.MOD_ID);
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.level.gameevent.GameEvent,
            net.minecraft.world.level.gameevent.GameEvent> GUNSHOT =
            GAME_EVENTS.register("gunshot", () -> new net.minecraft.world.level.gameevent.GameEvent(32));
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.level.gameevent.GameEvent,
            net.minecraft.world.level.gameevent.GameEvent> GUNSHOT_QUIET =
            GAME_EVENTS.register("gunshot_quiet", () -> new net.minecraft.world.level.gameevent.GameEvent(8));
    /** An electrical arc ({@code core/electric/Sparks}); heard by the coherer and the cat's whisker. Radius is {@code FTuning.SPARK_RANGE_BLOCKS}. */
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.level.gameevent.GameEvent,
            net.minecraft.world.level.gameevent.GameEvent> SPARK =
            GAME_EVENTS.register("spark", () -> new net.minecraft.world.level.gameevent.GameEvent(io.github.soundgoodizerfan.feedback.core.FTuning.SPARK_RANGE_BLOCKS));
    /** A bare supersonic shot: blast and crack both full. */
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.level.gameevent.GameEvent,
            net.minecraft.world.level.gameevent.GameEvent> GUNSHOT_THUNDEROUS =
            GAME_EVENTS.register("gunshot_thunderous", () -> new net.minecraft.world.level.gameevent.GameEvent(64));

    public static final DeferredRegister<net.neoforged.neoforge.attachment.AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.ATTACHMENT_TYPES, Feedback.MOD_ID);

    /** The loose rounds a skeleton carries beyond its gun's load ({@code docs/spec_armed_skeletons.md}); saved with it. */
    public static final java.util.function.Supplier<net.neoforged.neoforge.attachment.AttachmentType<io.github.soundgoodizerfan.feedback.weapon.ArmedSkeletons.Spares>> SPARES =
            ATTACHMENTS.register("skeleton_spares", () -> net.neoforged.neoforge.attachment.AttachmentType
                    .builder(() -> io.github.soundgoodizerfan.feedback.weapon.ArmedSkeletons.Spares.NONE)
                    .serialize(io.github.soundgoodizerfan.feedback.weapon.ArmedSkeletons.Spares.CODEC).build());

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        GAME_EVENTS.register(modBus);
        COMPONENTS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
