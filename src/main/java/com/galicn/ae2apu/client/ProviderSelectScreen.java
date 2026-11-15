package com.galicn.ae2apu.client;

import com.galicn.ae2apu.network.ProvidersListPayload;
import com.galicn.ae2apu.network.RequestProvidersPayload;
import com.galicn.ae2apu.network.UploadPatternPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ProviderSelectScreen extends Screen {
    private static final int PAGE_SIZE = 6;
    private static final int PANEL_HALF_WIDTH = 128;

    private final Screen parent;
    private final List<Group> groups = new ArrayList<>();
    private final List<Group> filtered = new ArrayList<>();
    private final Button[] rows = new Button[PAGE_SIZE];
    private final int[] rowIndexes = new int[PAGE_SIZE];

    private List<ProvidersListPayload.Entry> entries = List.of();
    private EditBox search;
    private EditBox alias;
    private Button prev;
    private Button next;
    private String query = "";
    private String preferredCategory = "";
    private int defaultIndex = -1;
    private int page;
    private boolean suggestionApplied;
    private Component statusText;
    private long statusUntil;

    private static final class Group {
        String name;
        int totalSlots;
        int count;
        int bestSlots;
        int bestIndex;
        boolean isDefault;
    }

    public ProviderSelectScreen(Screen parent, ProvidersListPayload payload) {
        super(Component.translatable("ae2apu.screen.title"));
        this.parent = parent;
        load(payload);
    }

    public void update(ProvidersListPayload payload) {
        load(payload);
        page = Math.min(page, maxPage());
        if (search != null && !search.getValue().equals(query)) {
            search.setValue(query);
        }
        rebuildWidgets();
    }

    private void load(ProvidersListPayload payload) {
        this.entries = payload.providers();
        this.defaultIndex = payload.defaultIndex();
        if (!suggestionApplied) {
            preferredCategory = RecipeCategoryResolver.resolve(payload.productId(), payload.method());
            query = preferredCategory;
            suggestionApplied = true;
        }
        buildGroups();
        applyFilter();
    }

    private void buildGroups() {
        groups.clear();
        Map<String, Group> byName = new LinkedHashMap<>();
        for (ProvidersListPayload.Entry entry : entries) {
            Group group = byName.computeIfAbsent(entry.name(), key -> {
                Group created = new Group();
                created.name = key;
                return created;
            });
            group.totalSlots += Math.max(0, entry.emptySlots());
            group.count++;
            if (entry.emptySlots() > group.bestSlots) {
                group.bestSlots = entry.emptySlots();
                group.bestIndex = entry.index();
            }
            if (entry.index() == defaultIndex) {
                group.isDefault = true;
            }
        }
        if (defaultIndex >= 0) {
            for (ProvidersListPayload.Entry entry : entries) {
                if (entry.index() != defaultIndex) {
                    continue;
                }
                Group group = byName.get(entry.name());
                if (group != null) {
                    group.bestIndex = entry.index();
                    group.bestSlots = entry.emptySlots();
                }
            }
        }
        groups.addAll(byName.values());
        groups.sort(Comparator.comparingInt((Group group) -> group.isDefault ? 0 : 1));
    }

    private void applyFilter() {
        filtered.clear();
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        for (Group group : groups) {
            if (needle.isEmpty() || group.name.toLowerCase(Locale.ROOT).contains(needle)) {
                filtered.add(group);
            }
        }
        if (!needle.isEmpty() && filtered.isEmpty()) {
            filtered.addAll(groups);
        }
    }

    private int maxPage() {
        return Math.max(0, (filtered.size() - 1) / PAGE_SIZE);
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int top = this.height / 2 - 100;
        int navY = top + 26 + PAGE_SIZE * 24 + 2;
        int aliasY = navY + 28;

        if (search == null) {
            search = new EditBox(this.font, cx - 120, top, 214, 18, Component.empty());
            search.setMaxLength(64);
            search.setHint(Component.translatable("ae2apu.hint.search"));
            search.setResponder(text -> {
                query = text;
                page = 0;
                applyFilter();
                refreshRows();
            });
            search.setValue(query);
        } else {
            search.setPosition(cx - 120, top);
        }
        addRenderableWidget(search);

        addRenderableWidget(Button.builder(Component.literal("↻"), button ->
                        PacketDistributor.sendToServer(new RequestProvidersPayload(false)))
                .bounds(cx + 98, top, 18, 18)
                .tooltip(Tooltip.create(Component.translatable("ae2apu.button.reload")))
                .build());
        addRenderableWidget(Button.builder(Component.literal("×"), button -> clearSearch())
                .bounds(cx + 118, top, 18, 18)
                .tooltip(Tooltip.create(Component.translatable("ae2apu.button.clear")))
                .build());

        for (int i = 0; i < PAGE_SIZE; i++) {
            int slot = i;
            rows[i] = addRenderableWidget(Button.builder(Component.empty(), button -> select(slot))
                    .bounds(cx - 120, top + 26 + i * 24, 240, 20)
                    .build());
        }

        prev = addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1))
                .bounds(cx - 45, navY, 20, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1))
                .bounds(cx + 25, navY, 20, 20).build());

        if (alias == null) {
            alias = new EditBox(this.font, cx - 120, aliasY, 140, 18, Component.empty());
            alias.setMaxLength(64);
            alias.setHint(Component.translatable("ae2apu.hint.alias"));
        } else {
            alias.setPosition(cx - 120, aliasY);
        }
        addRenderableWidget(alias);

        addRenderableWidget(Button.builder(Component.translatable("ae2apu.button.add"), button -> addMapping())
                .bounds(cx + 26, aliasY, 46, 18)
                .tooltip(Tooltip.create(Component.translatable("ae2apu.tip.add")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("ae2apu.button.delete"), button -> deleteMapping())
                .bounds(cx + 76, aliasY, 46, 18)
                .tooltip(Tooltip.create(Component.translatable("ae2apu.tip.delete")))
                .build());

        refreshRows();
    }

    private void refreshRows() {
        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            Button row = rows[i];
            if (row == null) {
                return;
            }
            int index = start + i;
            if (index < filtered.size()) {
                Group group = filtered.get(index);
                String label = (group.isDefault ? "★ " : "") + group.name
                        + " (" + group.totalSlots + ") x" + group.count;
                row.setMessage(Component.literal(label));
                row.visible = true;
                row.active = true;
                rowIndexes[i] = index;
            } else {
                row.visible = false;
                row.active = false;
                rowIndexes[i] = -1;
            }
        }
        if (prev != null) {
            prev.active = page > 0;
        }
        if (next != null) {
            next.active = (page + 1) * PAGE_SIZE < filtered.size();
        }
    }

    private void select(int slot) {
        int index = rowIndexes[slot];
        if (index < 0 || index >= filtered.size()) {
            return;
        }
        PacketDistributor.sendToServer(new UploadPatternPayload(filtered.get(index).bestIndex));
        restoreParent();
    }

    private void changePage(int delta) {
        int target = page + delta;
        if (target < 0 || target > maxPage()) {
            return;
        }
        page = target;
        refreshRows();
    }

    private void clearSearch() {
        search.setValue("");
        setInitialFocus(search);
    }

    private void addMapping() {
        String key = preferredCategory.isBlank()
                ? (query == null ? "" : query.trim())
                : preferredCategory;
        String value = alias.getValue().trim();
        if (key.isEmpty() || value.isEmpty()) {
            status(Component.translatable("ae2apu.msg.mapping_missing"));
            return;
        }
        RecipeNameMappings.add(key, value);
        RecipeNameMappings.reload();
        status(Component.translatable("ae2apu.msg.mapping_added", value));
    }

    private void deleteMapping() {
        String value = alias.getValue().trim();
        if (value.isEmpty()) {
            status(Component.translatable("ae2apu.msg.mapping_missing"));
            return;
        }
        int removed = RecipeNameMappings.removeByValue(value);
        RecipeNameMappings.reload();
        status(removed > 0
                ? Component.translatable("ae2apu.msg.mapping_deleted", removed)
                : Component.translatable("ae2apu.msg.mapping_not_found"));
    }

    private void status(Component text) {
        statusText = text;
        statusUntil = System.currentTimeMillis() + 2500L;
    }

    private void restoreParent() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = this.width / 2 - PANEL_HALF_WIDTH;
        int right = this.width / 2 + PANEL_HALF_WIDTH;
        int top = this.height / 2 - 124;
        int bottom = this.height / 2 + 126;
        graphics.fill(left, top, right, bottom, 0xE0121214);
        graphics.fill(left, top, right, top + 1, 0xFF5A5A66);
        graphics.fill(left, bottom - 1, right, bottom, 0xFF5A5A66);
        graphics.fill(left, top, left + 1, bottom, 0xFF5A5A66);
        graphics.fill(right - 1, top, right, bottom, 0xFF5A5A66);
        super.render(graphics, mouseX, mouseY, partialTick);
        int navY = this.height / 2 - 100 + 26 + PAGE_SIZE * 24 + 2;
        graphics.drawCenteredString(this.font, (page + 1) + " / " + (maxPage() + 1),
                this.width / 2, navY + 6, 0xDDDDDD);
        if (statusText != null && System.currentTimeMillis() < statusUntil) {
            graphics.drawCenteredString(this.font, statusText, this.width / 2, top + 7, 0x8AD98A);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            restoreParent();
            return true;
        }
        if ((keyCode == 257 || keyCode == 335) && !(getFocused() instanceof EditBox)) {
            select(0);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && search != null && search.isMouseOver(mouseX, mouseY)) {
            clearSearch();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
