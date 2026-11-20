package dev.rinchan.keepbutpenalty.client;

import dev.rinchan.keepbutpenalty.config.DurabilityPenaltyMode;
import dev.rinchan.keepbutpenalty.config.KeepButPenaltyConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Native zero-dependency configuration screen using vanilla Minecraft 1.20.1 widgets. */
public final class KeepButPenaltyConfigScreen extends Screen {
    private final Screen parent;
    private boolean keepInventory;
    private boolean skipOnPvp;
    private boolean enableExperiencePenalty;
    private double experienceKeepRatio;
    private boolean enableDurabilityPenalty;
    private DurabilityPenaltyMode penaltyMode;
    private double durabilityPercent;
    private int durabilityFlat;
    private boolean allowBreak;
    private boolean preserveHungerOnDeath;
    private int minRespawnFoodLevel;
    private boolean showRespawnSummary;

    public KeepButPenaltyConfigScreen(Screen parent) {
        super(Component.translatable("screen.keep_but_penalty.title"));
        this.parent = parent;
        loadFrom(KeepButPenaltyConfig.get());
    }

    private void loadFrom(KeepButPenaltyConfig cfg) {
        this.keepInventory = cfg.keepInventory();
        this.skipOnPvp = cfg.skipOnPvp();
        this.enableExperiencePenalty = cfg.enableExperiencePenalty();
        this.experienceKeepRatio = cfg.experienceKeepRatio();
        this.enableDurabilityPenalty = cfg.enableDurabilityPenalty();
        this.penaltyMode = cfg.penaltyMode();
        this.durabilityPercent = cfg.durabilityPercent();
        this.durabilityFlat = cfg.durabilityFlat();
        this.allowBreak = cfg.allowBreak();
        this.preserveHungerOnDeath = cfg.preserveHungerOnDeath();
        this.minRespawnFoodLevel = cfg.minRespawnFoodLevel();
        this.showRespawnSummary = cfg.showRespawnSummary();
    }

    @Override
    protected void init() {
        int totalW = Math.min(340, this.width - 24);
        int gap = 8;
        int colW = (totalW - gap) / 2;
        int left = (this.width - totalW) / 2;
        int right = left + colW + gap;
        int y = 32;
        int stepY = 24;

        // Row 1: Keep Inventory | Skip on PvP
        this.addRenderableWidget(CycleButton.onOffBuilder(this.keepInventory)
                .create(left, y, colW, 20,
                        Component.translatable("screen.keep_but_penalty.keep_inventory"),
                        (btn, value) -> this.keepInventory = value));
        this.addRenderableWidget(CycleButton.onOffBuilder(this.skipOnPvp)
                .create(right, y, colW, 20,
                        Component.translatable("screen.keep_but_penalty.skip_on_pvp"),
                        (btn, value) -> this.skipOnPvp = value));
        y += stepY;

        // Row 2: XP Penalty | XP Keep Ratio
        this.addRenderableWidget(CycleButton.onOffBuilder(this.enableExperiencePenalty)
                .create(left, y, colW, 20,
                        Component.translatable("screen.keep_but_penalty.xp_penalty"),
                        (btn, value) -> this.enableExperiencePenalty = value));
        this.addRenderableWidget(new PercentSlider(
                right, y, colW, 20,
                "screen.keep_but_penalty.xp_keep_ratio",
                this.experienceKeepRatio,
                v -> this.experienceKeepRatio = v));
        y += stepY;

        // Row 3: Durability Penalty | Penalty Mode
        this.addRenderableWidget(CycleButton.onOffBuilder(this.enableDurabilityPenalty)
                .create(left, y, colW, 20,
                        Component.translatable("screen.keep_but_penalty.durability_penalty"),
                        (btn, value) -> this.enableDurabilityPenalty = value));
        this.addRenderableWidget(CycleButton.<DurabilityPenaltyMode>builder(KeepButPenaltyConfigScreen::modeLabel)
                .withValues(DurabilityPenaltyMode.PERCENT_MAX, DurabilityPenaltyMode.FLAT)
                .withInitialValue(this.penaltyMode)
                .create(right, y, colW, 20,
                        Component.translatable("screen.keep_but_penalty.penalty_mode"),
                        (btn, value) -> {
                            this.penaltyMode = value;
                            this.rebuildWidgets();
                        }));
        y += stepY;

        // Row 4: Durability Loss Value | Allow Break
        if (this.penaltyMode == DurabilityPenaltyMode.PERCENT_MAX) {
            this.addRenderableWidget(new PercentSlider(
                    left, y, colW, 20,
                    "screen.keep_but_penalty.durability_percent",
                    this.durabilityPercent,
                    v -> this.durabilityPercent = v));
        } else {
            this.addRenderableWidget(new IntSlider(
                    left, y, colW, 20,
                    "screen.keep_but_penalty.durability_flat",
                    0, 500, 5, this.durabilityFlat,
                    v -> this.durabilityFlat = v));
        }
        this.addRenderableWidget(CycleButton.onOffBuilder(this.allowBreak)
                .create(right, y, colW, 20,
                        Component.translatable("screen.keep_but_penalty.allow_break"),
                        (btn, value) -> this.allowBreak = value));
        y += stepY;

        // Row 5: Preserve Hunger | Min Respawn Food
        this.addRenderableWidget(CycleButton.onOffBuilder(this.preserveHungerOnDeath)
                .create(left, y, colW, 20,
                        Component.translatable("screen.keep_but_penalty.preserve_hunger"),
                        (btn, value) -> this.preserveHungerOnDeath = value));
        this.addRenderableWidget(new IntSlider(
                right, y, colW, 20,
                "screen.keep_but_penalty.min_respawn_food",
                0, 20, 1, this.minRespawnFoodLevel,
                v -> this.minRespawnFoodLevel = v));
        y += stepY;

        // Row 6: Show Respawn Summary
        this.addRenderableWidget(CycleButton.onOffBuilder(this.showRespawnSummary)
                .create(left, y, totalW, 20,
                        Component.translatable("screen.keep_but_penalty.show_summary"),
                        (btn, value) -> this.showRespawnSummary = value));

        // Bottom Action Buttons
        int btnY = Math.min(this.height - 28, y + 32);
        int btnW = (totalW - 12) / 3;
        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.keep_but_penalty.reset"),
                        btn -> {
                            loadFrom(KeepButPenaltyConfig.defaults());
                            this.rebuildWidgets();
                        })
                .bounds(left, btnY, btnW, 20)
                .build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.keep_but_penalty.cancel"),
                        btn -> onClose())
                .bounds(left + btnW + 6, btnY, btnW, 20)
                .build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.keep_but_penalty.save"),
                        btn -> saveAndClose())
                .bounds(left + (btnW + 6) * 2, btnY, totalW - (btnW + 6) * 2, 20)
                .build());
    }

    private void saveAndClose() {
        KeepButPenaltyConfig updated = KeepButPenaltyConfig.get().withScreenValues(
                this.keepInventory,
                this.skipOnPvp,
                this.enableExperiencePenalty,
                this.experienceKeepRatio,
                this.enableDurabilityPenalty,
                this.penaltyMode,
                this.durabilityPercent,
                this.durabilityFlat,
                this.allowBreak,
                this.preserveHungerOnDeath,
                this.minRespawnFoodLevel,
                this.showRespawnSummary
        );
        KeepButPenaltyConfig.updateAndSave(updated);
        onClose();
    }

    private static Component modeLabel(DurabilityPenaltyMode mode) {
        return Component.translatable(mode == DurabilityPenaltyMode.FLAT
                ? "screen.keep_but_penalty.mode.flat"
                : "screen.keep_but_penalty.mode.percent_max");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private static final class PercentSlider extends AbstractSliderButton {
        private final String translationKey;
        private final java.util.function.DoubleConsumer onChange;

        PercentSlider(int x, int y, int w, int h, String translationKey, double initial, java.util.function.DoubleConsumer onChange) {
            super(x, y, w, h, Component.empty(), Math.max(0.0D, Math.min(1.0D, initial)));
            this.translationKey = translationKey;
            this.onChange = onChange;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int pct = (int) Math.round(this.value * 100.0D);
            this.setMessage(Component.translatable(this.translationKey, pct));
        }

        @Override
        protected void applyValue() {
            double snapped = Math.round(this.value * 100.0D) / 100.0D;
            this.onChange.accept(snapped);
        }
    }

    private static final class IntSlider extends AbstractSliderButton {
        private final String translationKey;
        private final int min;
        private final int max;
        private final int step;
        private final java.util.function.IntConsumer onChange;

        IntSlider(int x, int y, int w, int h, String translationKey, int min, int max, int step, int initial, java.util.function.IntConsumer onChange) {
            super(x, y, w, h, Component.empty(),
                    max > min ? (double) (Math.max(min, Math.min(max, initial)) - min) / (double) (max - min) : 0.0D);
            this.translationKey = translationKey;
            this.min = min;
            this.max = max;
            this.step = Math.max(1, step);
            this.onChange = onChange;
            updateMessage();
        }

        private int currentInt() {
            int raw = this.min + (int) Math.round(this.value * (this.max - this.min));
            int snapped = ((raw + this.step / 2) / this.step) * this.step;
            return Math.max(this.min, Math.min(this.max, snapped));
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable(this.translationKey, currentInt()));
        }

        @Override
        protected void applyValue() {
            this.onChange.accept(currentInt());
        }
    }
}
