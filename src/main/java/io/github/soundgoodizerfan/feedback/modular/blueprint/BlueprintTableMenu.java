// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.blueprint;

import java.util.List;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FMenus;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.registry.FTags;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.treatise.School;
import io.github.soundgoodizerfan.feedback.modular.treatise.Treatises;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularFaces;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;

import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Blueprint Table's paper slot, one sample slot per role row and the hotbar. A sample is an
 * item that names its role's material, as at the Gun Stand; it is a pattern to read, not a cost.
 * Printing bakes the drawn design (and any pinned drafts of the same type, kept on the paper as variants) on the server's
 * own tables and turns the paper into a {@link ModularBlueprintItem}; a blueprint put in is opened
 * by the screen to draw on again.
 */
public class BlueprintTableMenu extends AbstractContainerMenu {

    public static final int PAPER_X = 294, PAPER_Y = 294, HOTBAR_X = 8, HOTBAR_Y = 294, W = 360, H = 338;
    /** The title field's place on the panel (the screen builds it). */
    public static final int TITLE_X = 8, TITLE_Y = 317, TITLE_W = 344;
    /** Sample slots: one, for the part selected on the sheet. */
    public static final int ROLE_SLOTS = 1;
    /** Most pinned drafts a printed blueprint keeps beside its own design. */
    public static final int MAX_VARIANTS = 3;
    /** Button: send the samples back to the player, as the screen's page of rows has changed. */
    public static final int BTN_CLEAR = 1;
    /** Button: copy the blueprint in the source slot onto the blank paper. */
    public static final int BTN_COPY = 2;
    public static final int SOURCE_X = 240, SOURCE_Y = 294;
    public static final int SAMPLE_X = 186, SAMPLE_Y = 294, SAMPLE_STEP = 0;

    private final Container paper = new SimpleContainer(1);
    private final Container samples = new SimpleContainer(ROLE_SLOTS);
    /** A blueprint to copy from; kept when copied. */
    private final Container source = new SimpleContainer(1);
    /** The treatise volumes the player holds here (bits of {@link School#bit}), kept by the server and synced, so the screen can leave out what cannot be drawn. */
    private final net.minecraft.world.inventory.DataSlot schools = net.minecraft.world.inventory.DataSlot.standalone();
    private final Player player;
    /** Which role rows the screen draws a material for; the other sample slots are inactive. Client side only. */
    private List<Boolean> shown = List.of();
    private final ContainerLevelAccess access;

    /** Client-side open. */
    public BlueprintTableMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public BlueprintTableMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(FMenus.BLUEPRINT_TABLE.get(), id);
        this.access = access;
        this.player = inventory.player;
        addDataSlot(schools);
        addSlot(new Slot(paper, 0, PAPER_X, PAPER_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(FTags.BLUEPRINT_PAPER);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < ROLE_SLOTS; row++) {
            int role = row; // not `index`: inside a Slot that names the slot's place in the menu
            addSlot(new Slot(samples, row, SAMPLE_X, SAMPLE_Y + row * SAMPLE_STEP) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return isSample(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean isActive() {
                    return role < shown.size() && shown.get(role);
                }
            });
        }
        addSlot(new Slot(source, 0, SOURCE_X, SOURCE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ModularBlueprintItem && ModularStacks.design(stack) != null;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, HOTBAR_X + column * 18, HOTBAR_Y));
    }

    /** The schools the player holds a treatise of, as the server last said. */
    public int schools() {
        return schools.get();
    }

    @Override
    public void broadcastChanges() {
        if (player instanceof net.minecraft.server.level.ServerPlayer)
            schools.set(Treatises.mask(player, access));
        super.broadcastChanges();
    }

    /** Whether {@code stack} is some material in some form, as this side knows the materials. */
    public boolean isSample(ItemStack stack) {
        return !stack.isEmpty() && Material.find(ClientMaterials.on(player.level()), stack).isPresent();
    }

    /** The screen's role rows, each whether it is drawn: the drawn ones have an active sample slot. */
    public void shown(List<Boolean> shown) {
        this.shown = shown;
    }

    /** The sample in role row {@code row}. */
    public ItemStack sample(int row) {
        return samples.getItem(row);
    }

    public static void send(Player player, Component text) {
        io.github.soundgoodizerfan.feedback.net.AimPayloads.screen(player, text);
    }

    /** The screen's Print: bake {@code design} here, where the tables are trusted, and write it onto the paper. */
    public void print(ModularDesign design, List<ModularDesign> variants, String title, Player player) {
        if (paper.getItem(0).isEmpty()) {
            send(player, Component.translatable("message.feedback.blueprint_table.no_paper"));
            return;
        }
        // A type of a school is drawn only by someone holding that school's treatise (in the inventory, a binder, or a shelf beside the table).
        ModularType drawn = ModularTypes.get().types().get(design.type());
        // A hafted build holding a gated face (a spear's head) is gated by that face's school.
        String gate = drawn == null ? "" : drawn.family() == ModularType.Family.HAFTED ? ModularFaces.school(design, drawn) : drawn.school();
        School school = School.of(gate);
        int volume = School.volumeOf(gate);
        if (school != null && !Treatises.has(player, access, school, volume)) {
            send(player, Component.translatable("message.feedback.blueprint_table.no_treatise", Component.translatable(school.itemKey(volume))));
            return;
        }
        // An addon whose role asks for a volume (servos on armour: robotics) is refused the same way.
        if (drawn != null)
            for (ModularDesign.Part part : design.parts()) {
                ModularType.Role role = drawn.roles().get(part.role());
                String needs = role == null ? "" : !role.school().isEmpty() ? role.school()
                        : role.variant(part).map(ModularType.Variant::school).orElse("");
                School roleSchool = School.of(needs);
                if (roleSchool != null && !Treatises.has(player, access, roleSchool, School.volumeOf(needs))) {
                    send(player, Component.translatable("message.feedback.blueprint_table.no_treatise", Component.translatable(roleSchool.itemKey(School.volumeOf(needs)))));
                    return;
                }
            }
        if (write(paper, design, variants, title, player))
            broadcastChanges();
    }

    /**
     * Bakes {@code design} on the server's own tables and turns the paper in {@code paper} slot 0 into a blueprint of it (the pinned drafts
     * of the same type ride along). Says why on the player's HUD when it cannot; true when it wrote. The Prep Table's Blueprint tab prints
     * through here too, so the two cannot disagree about what a sheet holds.
     */
    public static boolean write(Container paper, ModularDesign design, List<ModularDesign> variants, Player player) {
        return write(paper, design, variants, "", player);
    }

    /** As above, titled {@code title}; the credit's name is the printing player's, taken here and never from the client. */
    public static boolean write(Container paper, ModularDesign design, List<ModularDesign> variants, String title, Player player) {
        if (paper.getItem(0).isEmpty()) {
            send(player, Component.translatable("message.feedback.blueprint_table.no_paper"));
            return false;
        }
        var figures = ToolDesigns.predict(design, ModularTypes.get().types(), MaterialTable.get().materials());
        if (figures.isEmpty()) {
            send(player, Component.translatable("message.feedback.blueprint_table.cannot_print"));
            return false;
        }
        ItemStack blueprint = new ItemStack(FModular.BLUEPRINT.get());
        blueprint.set(FModular.DESIGN.get(), design);
        blueprint.set(FModular.FIGURES.get(), figures.get());
        blueprint.set(FModular.CREDIT.get(), new BlueprintCredit(BlueprintCredit.clean(title), player.getGameProfile().getName(), java.util.Optional.empty()));
        // Pinned drafts ride on the paper when they are of the same type and make something here too.
        List<ModularDesign> kept = variants.stream().limit(MAX_VARIANTS).filter(v -> v.type().equals(design.type())
                && ToolDesigns.predict(v, ModularTypes.get().types(), MaterialTable.get().materials()).isPresent()).toList();
        if (!kept.isEmpty())
            blueprint.set(FModular.VARIANTS.get(), kept);
        paper.setItem(0, blueprint);
        send(player, Component.translatable("message.feedback.blueprint_table.printed"));
        return true;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BTN_COPY) {
            // Copying needs no treatise: it is the same drawing, onto blank paper. The original stays.
            ItemStack from = source.getItem(0), onto = paper.getItem(0);
            if (!from.isEmpty() && !onto.isEmpty() && onto.is(net.minecraft.world.item.Items.PAPER)) {
                paper.setItem(0, from.copyWithCount(1));
                broadcastChanges();
                send(player, Component.translatable("message.feedback.blueprint_table.copied"));
            } else
                send(player, Component.translatable("message.feedback.blueprint_table.cannot_copy"));
            return true;
        }
        if (id != BTN_CLEAR)
            return false;
        access.execute((level, pos) -> clearContainer(player, samples));
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> {
            clearContainer(player, paper);
            clearContainer(player, samples);
            clearContainer(player, source);
        });
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, FModular.BLUEPRINT_TABLE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack held = slot.getItem();
        ItemStack moved = held.copy();
        int hotbar = 2 + ROLE_SLOTS;
        if (index < hotbar) {
            if (!moveItemStackTo(held, hotbar, slots.size(), true))
                return ItemStack.EMPTY;
        } else if (held.is(FTags.BLUEPRINT_PAPER)) {
            if (!moveItemStackTo(held, 0, 1, false))
                return ItemStack.EMPTY;
        } else if (isSample(held)) {
            if (!moveItemStackTo(held, 1, 1 + Math.max(shown.size(), 1), false))
                return ItemStack.EMPTY;
        } else
            return ItemStack.EMPTY;
        if (held.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        return held.getCount() == moved.getCount() ? ItemStack.EMPTY : moved;
    }
}
