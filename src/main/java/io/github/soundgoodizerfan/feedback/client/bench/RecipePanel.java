// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.bench;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.modular.BlueprintCredit;
import io.github.soundgoodizerfan.feedback.modular.MaterialColors;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.PartBenchMenu;
import io.github.soundgoodizerfan.feedback.net.ModularBlueprintPayload;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Prep Table's Recipe tab: a vertical, ordered list of steps. The first step is the container; each step after it is an ingredient (base,
 * main, side) or a finish, drawn from a material that fits and prepared by discrete named options (how it is cut, worked and cooked), never
 * numbers. The recipe is named by the designation engine, and the player may type a name over it. It writes the same blueprint the Blueprint
 * Table does (the {@link BlueprintBoard} is the model, kept behind this panel and never drawn); a blueprint holding food reads "recipe" everywhere
 * the player sees it. The chosen options ride on the printed design as targets ({@code recipe_cut}, {@code recipe_work}, {@code recipe_cook} in
 * each part's state) for a recipe card to show. Spec: {@code docs/specs/feedback_recipe_screen_spec.md}.
 */
final class RecipePanel implements BlueprintBoard.Host {

    /** The state keys the options are written under, and how many named options each has. */
    static final String CUT = "recipe_cut", WORK = "recipe_work", COOK = "recipe_cook";
    private static final int[] OPTIONS = {3, 3, 5};
    private static final String[] KEYS = {CUT, WORK, COOK};
    private static final String[] NAMES = {"cut", "work", "cook"};

    private static final int ROW = 20, MARGIN = 10, LIST_TOP = 44, CHIP_W = 42, MAX_NAME = BlueprintCredit.MAX_TITLE;

    private final PartBenchMenu menu;
    private final GuiSkin skin;
    private final BlueprintBoard board;
    private final Map<String, int[]> chosen = new HashMap<>();
    private final StringBuilder name = new StringBuilder();
    private boolean nameFocus;
    private int scroll, left, top, hoverRow = -1;
    private long stampAt;
    private ItemStack lastPaper = ItemStack.EMPTY;

    RecipePanel(PartBenchMenu menu, GuiSkin skin) {
        this.menu = menu;
        this.skin = skin;
        board = new BlueprintBoard(this, skin);
        board.resize(PartBenchMenu.W, PartBenchMenu.BOARD_H);
    }

    boolean active() {
        return menu.blueprintTab();
    }

    private Font font() {
        return Minecraft.getInstance().font;
    }

    /** The Write button, placed beside the paper slot; the screen adds it. */
    Button printButton(int left, int top) {
        this.left = left;
        this.top = top;
        board.origin(left, top);
        board.build();
        return GuiStyle.button(skin, Component.translatable("gui.feedback.recipe.write"), b -> {
            var drawing = drawing();
            if (drawing.isPresent() && !menu.paper().isEmpty()) {
                skin.cue(GuiSkin.Cue.FINISH);
                stampAt = System.currentTimeMillis();
                PacketDistributor.sendToServer(new ModularBlueprintPayload(drawing.get(), List.of(), BlueprintCredit.clean(name.toString())));
            } else
                skin.cue(GuiSkin.Cue.REJECT);
        }, left + PartBenchMenu.PAPER_X + 20, top + PartBenchMenu.PAPER_Y, 38, 16);
    }

    /** The drawing with the chosen options written onto its parts as targets. */
    private Optional<ModularDesign> drawing() {
        Optional<ModularDesign> base = board.drawing();
        if (base.isEmpty())
            return base;
        ModularDesign design = base.get();
        for (ModularDesign.Part part : design.parts()) {
            int[] c = chosen.get(part.role());
            if (c == null)
                continue;
            var m = part.measures();
            for (int i = 0; i < KEYS.length; i++)
                m = m.withState(KEYS[i], c[i]);
            design = design.withPart(part.withMeasures(m));
        }
        return Optional.of(design);
    }

    void tick() {
        board.openPaper(menu.paper());
        ItemStack paper = menu.paper();
        if (!ItemStack.isSameItemSameComponents(paper, lastPaper)) {
            lastPaper = paper.copy();
            ModularDesign design = ModularStacks.design(paper);
            if (design != null) {
                chosen.clear();
                for (ModularDesign.Part part : design.parts()) {
                    int[] c = new int[KEYS.length];
                    boolean any = false;
                    for (int i = 0; i < KEYS.length; i++) {
                        c[i] = Math.round(part.measures().state(KEYS[i], 0f));
                        any |= c[i] != 0;
                    }
                    if (any)
                        chosen.put(part.role(), c);
                }
                BlueprintCredit credit = BlueprintCredit.of(paper);
                name.setLength(0);
                if (credit != null && credit.titleLine() != null)
                    name.append(credit.titleLine().getString());
            }
        }
    }

    private int[] opts(String role) {
        return chosen.computeIfAbsent(role, r -> new int[KEYS.length]);
    }

    // --- layout --------------------------------------------------------------------------------

    private int listH() {
        return PartBenchMenu.BOARD_H - LIST_TOP - 36;
    }

    private int visibleRows() {
        return Math.max(1, listH() / ROW);
    }

    private int rowY(int visibleIndex) {
        return top + LIST_TOP + visibleIndex * ROW;
    }

    private int listX() {
        return left + MARGIN;
    }

    private int listW() {
        return PartBenchMenu.W - 2 * MARGIN - 6;
    }

    // --- drawing -------------------------------------------------------------------------------

    void draw(GuiGraphics g, int left, int top, int mx, int my, float partial) {
        this.left = left;
        this.top = top;
        board.origin(left, top);
        Font font = font();
        g.drawString(font, Component.translatable("gui.feedback.recipe.title"), left + MARGIN, top + 9, skin.ink(), false);
        // the name: the designation engine's name until the player types one over it
        int nx = left + 82, nw = PartBenchMenu.W - 82 - MARGIN;
        skin.inset(g, nx, top + 5, nw, 14);
        String shown = name.toString();
        if (shown.isEmpty() && !nameFocus)
            g.drawString(font, Component.translatable("gui.feedback.recipe.name_hint"), nx + 4, top + 9, skin.dim(), false);
        else
            g.drawString(font, shown + (nameFocus && System.currentTimeMillis() / 400 % 2 == 0 ? "_" : ""), nx + 4, top + 9, skin.ink(), false);
        // the container: which kind of dish
        List<ResourceLocation> types = board.stepTypes();
        int cx = left + MARGIN, cy = top + 24;
        for (int i = 0; i < types.size(); i++) {
            Component label = Component.translatable("item." + types.get(i).getNamespace() + "." + types.get(i).getPath());
            int w = font.width(label) + 12;
            boolean on = i == board.stepType(), over = in(mx, my, cx, cy, w, 14);
            skin.row(g, cx, cy, w, 14, on, over);
            g.drawString(font, label, cx + 6, cy + 3, on ? skin.accent() : skin.ink(), false);
            cx += w + 4;
        }
        // the steps
        List<String> roles = board.stepRoles();
        int rows = visibleRows();
        scroll = Mth.clamp(scroll, 0, Math.max(0, roles.size() - rows));
        skin.inset(g, listX() - 3, top + LIST_TOP - 3, listW() + 6, rows * ROW + 6);
        hoverRow = -1;
        for (int v = 0; v < rows && scroll + v < roles.size(); v++) {
            String role = roles.get(scroll + v);
            int y = rowY(v), x = listX();
            boolean over = in(mx, my, x, y, listW(), ROW - 2);
            if (over)
                hoverRow = scroll + v;
            skin.row(g, x, y, listW(), ROW - 2, false, over);
            g.drawString(font, (scroll + v + 1) + ".", x + 3, y + 5, skin.dim(), false);
            g.drawString(font, font.plainSubstrByWidth(board.stepRoleName(role).getString(), 50), x + 16, y + 5, skin.dim(), false);
            Optional<Material.Located> now = board.stepChoice(role);
            int mx0 = x + 70;
            g.drawString(font, "<", mx0, y + 5, skin.ink(), false);
            if (now.isPresent()) {
                g.fill(mx0 + 10, y + 3, mx0 + 18, y + 11, 0xFF000000 | MaterialColors.of(now.get().id()));
                g.drawString(font, font.plainSubstrByWidth(BlueprintBoard.stepMaterialName(now.get()).getString(), 80), mx0 + 22, y + 5, skin.ink(), false);
            }
            g.drawString(font, ">", mx0 + 108, y + 5, skin.ink(), false);
            if (!isContainer(role)) {
                int[] c = chosen.getOrDefault(role, new int[KEYS.length]);
                for (int k = 0; k < KEYS.length; k++) {
                    int chipX = chipX(k);
                    boolean chipOver = in(mx, my, chipX, y, CHIP_W, ROW - 2);
                    g.fill(chipX, y + 1, chipX + CHIP_W, y + ROW - 3, chipOver ? 0x40FFFFFF : 0x28000000);
                    g.renderOutline(chipX, y + 1, CHIP_W, ROW - 4, 0x60000000 | skin.ink() & 0xFFFFFF);
                    Component text = Component.translatable("gui.feedback.recipe." + NAMES[k] + "." + c[k]);
                    String fitted = font.plainSubstrByWidth(text.getString(), CHIP_W - 4);
                    g.drawString(font, fitted, chipX + (CHIP_W - font.width(fitted)) / 2, y + 5, c[k] == 0 ? skin.dim() : skin.accent(), false);
                }
            }
            if (board.stepRemovable(role))
                g.drawString(font, "x", x + listW() - 9, y + 5, skin.warn(), false);
        }
        if (roles.size() > rows)
            skin.scrollbar(g, left + PartBenchMenu.W - MARGIN - 4, top + LIST_TOP, rows * ROW, roles.size(), rows, scroll);
        // add a step
        int ax = left + MARGIN, ay = top + PartBenchMenu.BOARD_H - 28;
        for (String key : board.stepAddable()) {
            Component label = Component.literal("+ ").append(board.stepKeyName(key));
            int w = font.width(label) + 10;
            skin.row(g, ax, ay, w, 14, false, in(mx, my, ax, ay, w, 14));
            g.drawString(font, label, ax + 5, ay + 3, skin.ink(), false);
            ax += w + 4;
        }
        // why it cannot be written yet
        if (drawing().isEmpty())
            g.drawString(font, Component.translatable("gui.feedback.recipe.incomplete"), left + MARGIN, top + PartBenchMenu.BOARD_H - 10, skin.warn(), false);
    }

    private int chipX(int k) {
        return listX() + 70 + 130 + k * (CHIP_W + 2) - 14;
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private boolean isContainer(String role) {
        return role.equals("C");
    }

    void overlay(GuiGraphics g, int mx, int my) {
        if (stampAt > 0)
            GuiStyle.stamp(g, Component.translatable("gui.feedback.recipe.written"), left + PartBenchMenu.W / 2f, top + 120f,
                    System.currentTimeMillis() - stampAt, skin.accent() & 0xFFFFFF);
    }

    boolean popupActive() {
        return false;
    }

    void tooltip(GuiGraphics g, int mx, int my) {
    }

    // --- input ---------------------------------------------------------------------------------

    boolean mouseClicked(double mx, double my, int button) {
        Font font = font();
        nameFocus = in(mx, my, left + 82, top + 5, PartBenchMenu.W - 82 - MARGIN, 14);
        if (nameFocus)
            return true;
        List<ResourceLocation> types = board.stepTypes();
        int cx = left + MARGIN, cy = top + 24;
        for (int i = 0; i < types.size(); i++) {
            Component label = Component.translatable("item." + types.get(i).getNamespace() + "." + types.get(i).getPath());
            int w = font.width(label) + 12;
            if (in(mx, my, cx, cy, w, 14)) {
                skin.cue(GuiSkin.Cue.SEAT, null, 1f);
                board.stepSetType(i);
                return true;
            }
            cx += w + 4;
        }
        List<String> roles = board.stepRoles();
        for (int v = 0; v < visibleRows() && scroll + v < roles.size(); v++) {
            String role = roles.get(scroll + v);
            int y = rowY(v), x = listX();
            if (!in(mx, my, x, y, listW(), ROW - 2))
                continue;
            if (board.stepRemovable(role) && mx >= x + listW() - 12) {
                skin.cue(GuiSkin.Cue.REJECT);
                board.stepRemove(role);
                return true;
            }
            if (in(mx, my, x + 66, y, 12, ROW - 2) || in(mx, my, x + 70 + 104, y, 16, ROW - 2)) {
                int dir = mx < x + 100 ? -1 : 1;
                List<Material.Located> fits = board.stepMaterials(role);
                Optional<Material.Located> now = board.stepChoice(role);
                if (!fits.isEmpty()) {
                    int at = now.map(n -> indexOf(fits, n)).orElse(0);
                    board.stepPaint(role, fits.get(Math.floorMod(at + dir, fits.size())));
                }
                return true;
            }
            if (!isContainer(role))
                for (int k = 0; k < KEYS.length; k++)
                    if (in(mx, my, chipX(k), y, CHIP_W, ROW - 2)) {
                        int[] c = opts(role);
                        c[k] = Math.floorMod(c[k] + (button == 1 ? -1 : 1), OPTIONS[k] + (k == 2 ? 0 : 0));
                        GuiSkin.play(skin.click(), 1f);
                        return true;
                    }
            return true;
        }
        int ax = left + MARGIN, ay = top + PartBenchMenu.BOARD_H - 28;
        for (String key : board.stepAddable()) {
            int w = font.width(Component.literal("+ ").append(board.stepKeyName(key))) + 10;
            if (in(mx, my, ax, ay, w, 14)) {
                skin.cue(GuiSkin.Cue.SEAT, null, 1f);
                board.stepAdd(key);
                return true;
            }
            ax += w + 4;
        }
        return false;
    }

    private static int indexOf(List<Material.Located> list, Material.Located what) {
        for (int i = 0; i < list.size(); i++)
            if (list.get(i).id().equals(what.id()))
                return i;
        return 0;
    }

    boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return false;
    }

    boolean mouseReleased(double mx, double my, int button) {
        return false;
    }

    boolean mouseScrolled(double mx, double my, double scrollY) {
        if (!in(mx, my, listX(), top + LIST_TOP, listW(), listH()))
            return false;
        scroll = Math.max(0, scroll - (int) Math.signum(scrollY));
        return true;
    }

    boolean keyPressed(int key, int scan, int modifiers) {
        if (!nameFocus)
            return false;
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && name.length() > 0)
            name.setLength(name.length() - 1);
        else if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE)
            nameFocus = false;
        return true;
    }

    boolean charTyped(char c, int modifiers) {
        if (!nameFocus || c < 32 || name.length() >= MAX_NAME)
            return false;
        name.append(c);
        return true;
    }

    // --- BlueprintBoard.Host ---------------------------------------------------------------------

    @Override
    public ItemStack carried() {
        return menu.getCarried();
    }

    @Override
    public void selectionChanged() {
    }

    @Override
    public void sampleWanted(boolean wanted) {
    }

    @Override
    public boolean foodOnly() {
        return true;
    }
}
