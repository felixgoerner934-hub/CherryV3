package com.spawnerbeacon.gui;

import com.spawnerbeacon.BeaconConfig;
import com.spawnerbeacon.SpawnerBeaconClient;
import com.spawnerbeacon.spawner.SpawnerInfo;
import com.spawnerbeacon.spawner.SpawnerTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

/** Clean, responsive Cherry Blossom configuration screen. */
public final class ConfigScreen extends Screen {
    private static final int CHERRY = 0xFFE56F98;
    private static final int CHERRY_DARK = 0xFF8D3E5A;
    private static final int INK = 0xFF33262C;
    private static final int MUTED = 0xFF806A73;
    private static final int PANEL = 0xFFFDF8FA;
    private static final int CARD = 0xFFFFFBFC;
    private static final int SOFT = 0xFFF8EDF1;
    private static final int LINE = 0xFFE2C9D2;
    private static final int GREEN = 0xFF4F936C;
    private static final int RED = 0xFFB34B67;
    private static final int[] PALETTE = {
            0xE56F98, 0xF5A3B7, 0xB93B60, 0xFF7D9D, 0xFF9F68, 0xFF9D32,
            0xF3C969, 0x91E65A, 0x48D9D0, 0x5AA9E6, 0x8D6BFF, 0xB06BE8,
            0xF4F1E8, 0xA6A6A6
    };
    private static final List<String> TABS = List.of("general", "colors", "spawners", "list");

    private static final Identifier BRANCH_LEFT = Identifier.fromNamespaceAndPath(SpawnerBeaconClient.MOD_ID, "textures/gui/branch_left.png");
    private static final Identifier BRANCH_RIGHT = Identifier.fromNamespaceAndPath(SpawnerBeaconClient.MOD_ID, "textures/gui/branch_right.png");
    private static final Identifier CAR = Identifier.fromNamespaceAndPath(SpawnerBeaconClient.MOD_ID, "textures/gui/cherry_car2.png");

    private final BeaconConfig cfg = BeaconConfig.get();
    private String tab = "general";
    private String selectedType = "zombie";
    private int listScroll;
    private boolean dirty;
    private long lastChange;
    private final long openedAt = System.nanoTime();

    public ConfigScreen() {
        super(Component.translatable("screen.spawnerbeacon.title"));
    }

    private int panelW() { return Math.min(980, Math.max(620, width - 36)); }
    private int panelH() { return Math.min(560, Math.max(390, height - 28)); }
    private int left() { return (width - panelW()) / 2; }
    private int right() { return left() + panelW(); }
    private int top() { return (height - panelH()) / 2; }
    private int bottom() { return top() + panelH(); }
    private int contentLeft() { return left() + 24; }
    private int contentRight() { return right() - 24; }
    private int contentTop() { return top() + 126; }
    private int contentBottom() { return bottom() - 58; }

    @Override
    protected void init() {
        clearWidgets();
        int tabY = top() + 86;
        int gap = 8;
        int tabW = (panelW() - 48 - gap * 3) / 4;
        for (int i = 0; i < TABS.size(); i++) {
            String id = TABS.get(i);
            addRenderableWidget(new CherryButton(contentLeft() + i * (tabW + gap), tabY, tabW, 32,
                    Component.translatable("screen.spawnerbeacon.tab." + id), () -> {
                        tab = id;
                        listScroll = 0;
                        init();
                    }, () -> tab.equals(id)));
        }

        switch (tab) {
            case "general" -> initGeneral();
            case "colors" -> initColors();
            case "spawners" -> initSpawners();
            case "list" -> initList();
        }

        int footerY = bottom() - 42;
        addRenderableWidget(new CherryButton(contentLeft(), footerY, 190, 30,
                Component.translatable("screen.spawnerbeacon.reset"), () -> {
                    cfg.resetToDefaults();
                    dirty = false;
                    init();
                }));
        addRenderableWidget(new CherryButton(contentRight() - 150, footerY, 150, 30,
                Component.translatable("gui.done"), this::onClose));
    }

    private void initGeneral() {
        int x = contentLeft();
        int w = contentRight() - x;
        int gap = 12;
        int colW = (w - gap) / 2;
        int y = contentTop();

        addRenderableWidget(new CherryButton(x, y, colW, 38,
                Component.translatable(cfg.enabled ? "screen.spawnerbeacon.enabled_on" : "screen.spawnerbeacon.enabled_off"), () -> {
                    cfg.enabled = !cfg.enabled; changed(); init();
                }, () -> cfg.enabled));
        addRenderableWidget(new CherryButton(x + colW + gap, y, colW, 38,
                Component.translatable("screen.spawnerbeacon.animation", cfg.animate ? "AN" : "AUS"), () -> {
                    cfg.animate = !cfg.animate; changed(); init();
                }, () -> cfg.animate));
        y += 52;

        addRenderableWidget(new CherrySlider(x, y, colW, 34, tr("screen.spawnerbeacon.thickness"), 0.1, 5.0,
                cfg.thickness, "%.2f", v -> { cfg.thickness = v; changed(); }));
        addRenderableWidget(new CherrySlider(x + colW + gap, y, colW, 34, tr("screen.spawnerbeacon.opacity"), 0.05, 1.0,
                cfg.opacity, "%.2f", v -> { cfg.opacity = v; changed(); }));
        y += 50;

        addRenderableWidget(new CherrySlider(x, y, colW, 34, tr("screen.spawnerbeacon.height"), 64, 512,
                cfg.maxY, "%.0f", v -> { cfg.maxY = (int) Math.round(v); changed(); }));
        addRenderableWidget(new CherrySlider(x + colW + gap, y, colW, 34, tr("screen.spawnerbeacon.distance"), 2, 32,
                cfg.renderDistanceChunks, "%.0f chunks", v -> { cfg.renderDistanceChunks = (int) Math.round(v); changed(); }));
        y += 50;

        addRenderableWidget(new CherryButton(x, y, colW, 34,
                Component.translatable("screen.spawnerbeacon.fade", cfg.distanceFade ? "AN" : "AUS"), () -> {
                    cfg.distanceFade = !cfg.distanceFade; changed(); init();
                }, () -> cfg.distanceFade));
        addRenderableWidget(new CherryButton(x + colW + gap, y, colW, 34,
                Component.translatable("screen.spawnerbeacon.rainbow", cfg.rainbow ? "AN" : "AUS"), () -> {
                    cfg.rainbow = !cfg.rainbow; changed(); init();
                }, () -> cfg.rainbow));
        y += 46;

        gDimension(x, y, colW, Component.translatable("screen.spawnerbeacon.dimension_overworld", cfg.overworld ? "AN" : "AUS"), () -> {
            cfg.overworld = !cfg.overworld; changed(); init();
        }, () -> cfg.overworld);
        gDimension(x + colW + gap, y, colW, Component.translatable("screen.spawnerbeacon.dimension_nether", cfg.nether ? "AN" : "AUS"), () -> {
            cfg.nether = !cfg.nether; changed(); init();
        }, () -> cfg.nether);
        y += 42;
        gDimension(x, y, colW, Component.translatable("screen.spawnerbeacon.dimension_end", cfg.end ? "AN" : "AUS"), () -> {
            cfg.end = !cfg.end; changed(); init();
        }, () -> cfg.end);

        // Small live status card; deliberately inside the free lower-right area.
        int cardX = x + colW + gap;
        int cardY = y - 2;
        addRenderableWidget(new CherryButton(cardX, cardY, colW, 34,
                Component.translatable("screen.spawnerbeacon.status_spawners", SpawnerTracker.snapshot().size()), () -> {}, () -> false));
    }

    private void gDimension(int x, int y, int w, Component text, Runnable action, java.util.function.BooleanSupplier active) {
        addRenderableWidget(new CherryButton(x, y, w, 34, text, () -> { action.run(); }, active));
    }

    private void initColors() {
        int x = contentLeft();
        int w = contentRight() - x;
        int gap = 12;
        int sw = 28;
        int y = contentTop();

        addRenderableWidget(new CherryButton(x, y, 190, 32, selectedColorName(), () -> {
            selectedType = "other";
            init();
        }, () -> selectedType.equals("other")));
        y += 44;

        int paletteW = Math.min(w, 7 * sw + 6 * 8);
        for (int i = 0; i < PALETTE.length; i++) {
            int row = i / 7;
            int col = i % 7;
            addRenderableWidget(new ColorSwatch(x + col * (sw + 8), y + row * (sw + 8), sw, PALETTE[i], this::setSelectedColor));
        }
        y += 3 * (sw + 8) + 16;

        int previewX = x + Math.min(paletteW + 28, w - 150);
        int previewY = contentTop();
        // A larger color preview is drawn in the background; sliders remain unobstructed.
        addRenderableWidget(new CherrySlider(x, y, w, 34, "R", 0, 255, red(selectedColor()), "%.0f", v -> setChannel(0, (int) v)));
        y += 48;
        addRenderableWidget(new CherrySlider(x, y, w, 34, "G", 0, 255, green(selectedColor()), "%.0f", v -> setChannel(1, (int) v)));
        y += 48;
        addRenderableWidget(new CherrySlider(x, y, w, 34, "B", 0, 255, blue(selectedColor()), "%.0f", v -> setChannel(2, (int) v)));
    }

    private void initSpawners() {
        int x = contentLeft();
        int w = contentRight() - x;
        int gap = 10;
        int cardW = (w - gap) / 2;
        int rowH = 54;
        int y = contentTop();
        for (int i = 0; i < BeaconConfig.TYPES.size(); i++) {
            String type = BeaconConfig.TYPES.get(i);
            int col = i % 2;
            int row = i / 2;
            int xx = x + col * (cardW + gap);
            int yy = y + row * rowH;
            addRenderableWidget(new ColorSwatch(xx + 10, yy + 11, 28, cfg.colorFor(type), c -> {
                cfg.typeColors.put(type, c & 0xFFFFFF); changed();
            }));
            addRenderableWidget(new CherryButton(xx + 48, yy + 8, cardW - 58, 28, typeName(type), () -> {
                selectedType = type;
                tab = "colors";
                init();
            }, () -> selectedType.equals(type)));
            addRenderableWidget(new CherryButton(xx + 48, yy + 37, 92, 20,
                    Component.translatable(cfg.typeEnabled(type) ? "screen.spawnerbeacon.visible_short" : "screen.spawnerbeacon.hidden_short"), () -> {
                        cfg.typeEnabled.put(type, !cfg.typeEnabled(type)); changed(); init();
                    }, () -> cfg.typeEnabled(type)));
            addRenderableWidget(new CherrySlider(xx + 148, yy + 34, cardW - 158, 24, "", 0.1, 5.0,
                    cfg.thicknessFor(type), "%.1f", v -> { cfg.typeThickness.put(type, v); changed(); }));
        }
    }

    private void initList() {
        addRenderableWidget(new CherryButton(contentRight() - 220, contentTop() - 38, 220, 28,
                Component.translatable("screen.spawnerbeacon.copy_closest"), () -> {
                    List<SpawnerInfo> list = SpawnerTracker.sortedByDistance();
                    if (!list.isEmpty()) {
                        var p = list.get(0).pos();
                        GLFW.glfwSetClipboardString(Minecraft.getInstance().getWindow().handle(), formatPos(p.getX(), p.getY(), p.getZ()));
                    }
                }));
    }

    private int selectedColor() { return selectedType.equals("other") ? cfg.defaultColor : cfg.colorFor(selectedType); }
    private Component selectedColorName() { return selectedType.equals("other") ? Component.translatable("screen.spawnerbeacon.default_color") : typeName(selectedType); }

    private void setSelectedColor(int color) {
        if (selectedType.equals("other")) cfg.defaultColor = color & 0xFFFFFF;
        else cfg.typeColors.put(selectedType, color & 0xFFFFFF);
        changed();
        init();
    }

    private void setChannel(int channel, int value) {
        int c = selectedColor();
        int r = red(c), g = green(c), b = blue(c);
        if (channel == 0) r = value; else if (channel == 1) g = value; else b = value;
        int next = (r << 16) | (g << 8) | b;
        if (selectedType.equals("other")) cfg.defaultColor = next; else cfg.typeColors.put(selectedType, next);
        changed();
    }

    private int red(int c) { return (c >> 16) & 255; }
    private int green(int c) { return (c >> 8) & 255; }
    private int blue(int c) { return c & 255; }
    private Component typeName(String type) { return type.equals("other") ? Component.translatable("screen.spawnerbeacon.type.other") : Component.translatable("entity.minecraft." + type); }
    private String tr(String key) { return Component.translatable(key).getString(); }
    private String formatPos(int x, int y, int z) { return x + " " + y + " " + z; }
    private void changed() { dirty = true; lastChange = System.currentTimeMillis(); }

    @Override
    public void tick() {
        super.tick();
        if (dirty && System.currentTimeMillis() - lastChange > 250) {
            cfg.save();
            dirty = false;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        drawBackground(g);
        super.extractRenderState(g, mouseX, mouseY, delta);
        drawForeground(g, mouseX, mouseY);
    }

    private void drawBackground(GuiGraphicsExtractor g) {
        float open = Math.min(1f, (System.nanoTime() - openedAt) / 220_000_000f);
        int alpha = (int) (205 * open);
        g.fill(0, 0, width, height, (alpha << 24) | 0x160F13);

        int l = left(), t = top(), r = right(), b = bottom();
        // Outer card + inner content surface. No decorative art crosses the interactive area.
        g.fill(l, t, r, b, PANEL);
        g.outline(l, t, panelW(), panelH(), LINE);
        g.fill(l + 1, t + 1, r - 1, t + 72, 0xFFFFF7FA);
        g.horizontalLine(l + 20, r - 20, t + 72, LINE);

        // Corner branches are intentionally small and live in decorative zones.
        int branchW = Math.min(270, panelW() / 3);
        int branchH = branchW * 300 / 520;
        g.blit(RenderPipelines.GUI_TEXTURED, BRANCH_LEFT, l + 4, b - branchH - 2, 0, 0, branchW, branchH, 520, 300);
        g.blit(RenderPipelines.GUI_TEXTURED, BRANCH_RIGHT, r - branchW - 4, b - branchH - 2, 0, 0, branchW, branchH, 520, 300);

        // Header art is small enough to never compete with the title.
        g.blit(RenderPipelines.GUI_TEXTURED, CAR, r - 164, t + 10, 0, 0, 140, 62, 520, 230);
        g.text(font, Component.literal("SPAWNERBEACON"), l + 24, t + 19, INK, false);
        g.text(font, Component.translatable("screen.spawnerbeacon.subtitle"), l + 24, t + 39, MUTED, false);

        // Active status chip.
        int chipX = r - 184;
        int chipY = t + 78;
        g.fill(chipX, chipY, r - 24, chipY + 20, cfg.enabled ? 0xFFE9F4EC : 0xFFF8E5EA);
        g.outline(chipX, chipY, 160, 20, cfg.enabled ? 0xFFB8D9C3 : 0xFFE1B6C0);
        g.fill(chipX + 9, chipY + 7, chipX + 15, chipY + 13, cfg.enabled ? GREEN : RED);
        g.text(font, Component.translatable(cfg.enabled ? "screen.spawnerbeacon.status_active" : "screen.spawnerbeacon.status_disabled"), chipX + 22, chipY + 6, INK, false);

        // Content heading.
        g.text(font, Component.translatable("screen.spawnerbeacon.tab." + tab), contentLeft(), top() + 122, CHERRY_DARK, false);
        g.horizontalLine(contentLeft(), contentRight(), top() + 138, LINE);
    }

    private void drawForeground(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (tab.equals("colors")) {
            int c = selectedColor();
            int x = contentRight() - 68;
            int y = top() + 118;
            g.fill(x, y, x + 44, y + 22, 0xFF000000 | c);
            g.outline(x, y, 44, 22, LINE);
        }
        if (tab.equals("list")) drawSpawnerList(g, mouseX, mouseY);
    }

    private void drawSpawnerList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int l = contentLeft();
        int r = contentRight();
        int t = contentTop() - 6;
        int b = contentBottom();
        List<SpawnerInfo> list = SpawnerTracker.sortedByDistance();
        int rowH = 42;
        int max = Math.max(0, list.size() * rowH - (b - t));
        listScroll = Math.min(listScroll, max);
        g.enableScissor(l, t, r, b);
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        int y = t - listScroll;
        for (SpawnerInfo info : list) {
            if (y + rowH >= t && y <= b) {
                int c = cfg.colorFor(info.type());
                g.fill(l, y, r, y + rowH - 4, 0xFFFFFBFC);
                g.fill(l, y, l + 5, y + rowH - 4, 0xFF000000 | c);
                g.text(font, typeName(info.type()), l + 16, y + 6, INK, false);
                String pos = info.pos().getX() + "  " + info.pos().getY() + "  " + info.pos().getZ();
                g.text(font, pos, l + 16, y + 23, MUTED, false);
                double dist = Math.sqrt(info.pos().distToCenterSqr(camera.x, camera.y, camera.z));
                g.text(font, String.format(Locale.ROOT, "%.0fm", dist), r - 48, y + 14, CHERRY_DARK, false);
                if (mouseX >= l && mouseX < r && mouseY >= y && mouseY < y + rowH - 4) {
                    g.outline(l, y, r - l, rowH - 4, CHERRY);
                }
            }
            y += rowH;
        }
        g.disableScissor();
        if (list.isEmpty()) g.text(font, Component.translatable("screen.spawnerbeacon.list_empty"), l + 4, t + 40, MUTED, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tab.equals("list")) {
            int rowH = 42;
            int max = Math.max(0, SpawnerTracker.sortedByDistance().size() * rowH - (contentBottom() - (contentTop() - 6)));
            listScroll = Math.max(0, Math.min(max, listScroll - (int) Math.round(scrollY * 30)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        cfg.save();
        super.onClose();
    }
}
