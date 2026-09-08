package top.csituka.magicaland.client.gui.tab.ponycustom;

import net.minecraft.text.Text;
import net.minecraft.client.gui.tooltip.Tooltip;
import java.util.EnumSet;
import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.config.ModelManager;
import top.csituka.magicaland.client.config.style.PonyStylePart;
import top.csituka.magicaland.client.gui.widget.ColorPicker;
import top.csituka.magicaland.client.gui.widget.CustomButton;
import top.csituka.magicaland.client.gui.widget.SectionLabel;
import top.csituka.magicaland.client.gui.widget.SettingsList;
import top.csituka.magicaland.client.gui.widget.Toggle;
import top.csituka.magicaland.client.render.ManeDye;
import top.csituka.magicaland.client.render.BodyPalette;
import top.csituka.magicaland.client.render.ManePalette;
import top.csituka.magicaland.client.render.ManePalette.Part;

public class ManePage implements PonyCustomPage {
    private final EnumSet<Part> expandedParts = EnumSet.noneOf(Part.class);
    private final EnumSet<Part> expandedDyes = EnumSet.noneOf(Part.class);

    @Override
    public void onEnter() {
        expandedParts.clear();
        expandedDyes.clear();
    }

    @Override
    public void build(PonyCustomPageContext context, SettingsList list) {
        ModelConfig config = ModelManager.getActiveModel();
        if (config == null) return;
        ModelConfig.sanitize(config);

        int buttonWidth = Math.min(180, context.getWidth() / 2);
        int buttonHeight = 20;
        int buttonX = getButtonX(context, buttonWidth);

        list.addWidget(createBackButton(context, buttonX, buttonWidth, buttonHeight,
                "text.magicaland.config.mane_menu.name"), SettingsList.Alignment.RIGHT);
        list.addWidget(new SectionLabel(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.section.mane_styles.name")), SettingsList.Alignment.RIGHT);

        list.addWidget(PonyCustomPageHelper.createStyleButton(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.front_mane_style.name"),
                PonyStylePart.FRONT_MANE, config.frontManeStyle, style -> {
                    config.frontManeStyle = style;
                    ModelManager.saveActiveModel();
                    context.refreshKeepingScroll();
                }), SettingsList.Alignment.RIGHT);
        list.addWidget(PonyCustomPageHelper.createStyleButton(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.back_mane_style.name"),
                PonyStylePart.BACK_MANE, config.backManeStyle, style -> {
                    config.backManeStyle = style;
                    ModelManager.saveActiveModel();
                    context.refreshKeepingScroll();
                }), SettingsList.Alignment.RIGHT);
        list.addWidget(PonyCustomPageHelper.createStyleButton(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.tail_style.name"),
                PonyStylePart.TAIL, config.tailStyle, style -> {
                    config.tailStyle = style;
                    ModelManager.saveActiveModel();
                    context.refreshKeepingScroll();
                }), SettingsList.Alignment.RIGHT);

        list.addWidget(new SectionLabel(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.section.mane_colors.name")), SettingsList.Alignment.RIGHT);
        boolean soft = !"legacy".equals(config.maneShadingMode);
        CustomButton shading = new CustomButton(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.mane_shading.name"),
                Text.translatable("text.magicaland.config.body_shading." + (soft ? "soft" : "legacy")).getString(), false,
                button -> {
                    config.maneShadingMode = soft ? "legacy" : "soft";
                    ModelManager.saveActiveModel();
                    context.refreshKeepingScroll();
                });
        shading.setTooltip(Tooltip.of(Text.translatable("text.magicaland.config.mane_shading.tooltip")));
        list.addWidget(shading, SettingsList.Alignment.RIGHT);
        addDye(context, list, config, buttonX, buttonWidth, buttonHeight);
        addPart(context, list, config, Part.FRONT, "front_mane_color", buttonX, buttonWidth, buttonHeight, soft);
        addPart(context, list, config, Part.BACK, "back_mane_color", buttonX, buttonWidth, buttonHeight, soft);
        addPart(context, list, config, Part.TAIL, "tail_color", buttonX, buttonWidth, buttonHeight, soft);
        if (soft) {
            list.addWidget(new CustomButton(buttonX, 0, buttonWidth, buttonHeight,
                    Text.translatable("text.magicaland.config.mane_shading.reset"), false,
                    button -> {
                        ManePalette.resetAutomatic(config);
                        ModelManager.saveActiveModel();
                        context.refreshKeepingScroll();
                    }, false, true), SettingsList.Alignment.RIGHT);
        }
    }

    private void addDye(PonyCustomPageContext context, SettingsList list, ModelConfig config,
            int x, int width, int height) {
        boolean supported = ManeDye.supports(config, Part.FRONT) || ManeDye.supports(config, Part.BACK) || ManeDye.supports(config, Part.TAIL);
        Toggle toggle = new Toggle(x, 0, width, height,
                Text.translatable("text.magicaland.config.mane_dye.name"), config.maneDyeEnabled, button -> {
                    config.maneDyeEnabled = button.getState();
                    ModelManager.saveActiveModel();
                    context.refreshKeepingScroll();
                });
        toggle.active = supported;
        toggle.setTooltip(Tooltip.of(Text.translatable("text.magicaland.config.mane_dye.tooltip")));
        list.addWidget(toggle, SettingsList.Alignment.RIGHT);
        if (!supported || !config.maneDyeEnabled) return;
        list.addWidget(new SectionLabel(x + 12, 0, width - 12, height,
                Text.translatable("text.magicaland.config.mane_dye.stripe01")), SettingsList.Alignment.RIGHT);
    }

    private void addDyePart(PonyCustomPageContext context, SettingsList list, ModelConfig config,
            Part part, int x, int width, int height) {
        if (!ManeDye.enabled(config, part)) return;
        boolean expanded = expandedDyes.contains(part);
        String partKey = switch (part) { case FRONT -> "front"; case BACK -> "back"; case TAIL -> "tail"; };
        list.addWidget(new CustomButton(x, 0, width, height,
                Text.literal(expanded ? "\u25bc " : "\u25b6 ").append(Text.translatable("text.magicaland.config.mane_dye.regions." + partKey)),
                false, button -> {
                    if (expanded) expandedDyes.remove(part); else expandedDyes.add(part);
                    context.refreshKeepingScroll();
                }, false, true), SettingsList.Alignment.RIGHT);
        if (!expanded) return;
        for (int i = 0; i < ManeDye.REGION_COUNT; i++) {
            final int region = i;
            ColorPicker picker = new ColorPicker(x + 12, 0, width - 12, height,
                    Text.translatable("text.magicaland.config.mane_dye.region", region + 1),
                    BodyPalette.hex(ManeDye.colors(config, part, region).base()), color -> {
                        ManeDye.setColor(config, part, region, color);
                        ModelManager.requestSaveActiveModel();
                    });
            picker.setAutomaticColor(() -> BodyPalette.hex(ManePalette.base(config, part)));
            picker.setLocked(ManeDye.linked(config, part, region));
            picker.setOnLockChanged(locked -> {
                ManeDye.setLinked(config, part, region, locked);
                ModelManager.saveActiveModel();
                context.refreshKeepingScroll();
            });
            picker.setTooltip(Tooltip.of(Text.translatable("text.magicaland.config.mane_dye.region.tooltip")));
            list.addWidget(picker, SettingsList.Alignment.RIGHT);
        }
        list.addWidget(new CustomButton(x + 12, 0, width - 12, height,
                Text.translatable("text.magicaland.config.mane_dye.regions.reset"), false, button -> {
                    ManeDye.resetRegions(config, part);
                    ModelManager.saveActiveModel();
                    context.refreshKeepingScroll();
                }, false, true), SettingsList.Alignment.RIGHT);
    }

    private void addPart(PonyCustomPageContext context, SettingsList list, ModelConfig config,
            Part part, String label, int x, int width, int height, boolean soft) {
        ColorPicker base = new ColorPicker(x, 0, width, height,
                Text.translatable("text.magicaland.config." + label + ".name"),
                BodyPalette.hex(ManePalette.base(config, part)), color -> {
                    ManePalette.setBase(config, part, color);
                    ModelManager.requestSaveActiveModel();
                });
        if (part != Part.FRONT) {
            base.setAutomaticColor(() -> BodyPalette.hex(ManePalette.base(config, Part.FRONT)));
            base.setLocked(ManePalette.linked(config, part));
            base.setOnLockChanged(locked -> {
                ManePalette.setLinked(config, part, locked);
                ModelManager.saveActiveModel();
                context.refreshKeepingScroll();
            });
            base.setTooltip(Tooltip.of(Text.translatable("text.magicaland.config.mane_link.tooltip")));
        }
        boolean hasDetails = soft && !ManePalette.linked(config, part);
        if (hasDetails) {
            boolean expanded = expandedParts.contains(part);
            base.setDisclosure(expanded, value -> {
                if (value) expandedParts.add(part);
                else expandedParts.remove(part);
                context.refreshKeepingScroll();
            });
            base.setTooltip(Tooltip.of(Text.translatable(part == Part.FRONT
                    ? "text.magicaland.config.color_details.tooltip"
                    : "text.magicaland.config.mane_link.tooltip").copy().append("\n")
                    .append(Text.translatable("text.magicaland.config.color_details." + (expanded ? "collapse" : "expand")))));
        }
        list.addWidget(base, SettingsList.Alignment.RIGHT);
        if (hasDetails && expandedParts.contains(part)) {
            addStop(context, list, config, part, false, x + 12, width - 12, height);
            addStop(context, list, config, part, true, x + 12, width - 12, height);
        }
        addDyePart(context, list, config, part, x + 12, width - 12, height);
    }

    private void addStop(PonyCustomPageContext context, SettingsList list, ModelConfig config,
            Part part, boolean highlight, int x, int width, int height) {
        String key = highlight ? "body_highlight" : "body_shadow";
        ColorPicker picker = new ColorPicker(x, 0, width, height,
                Text.translatable("text.magicaland.config." + key + ".name"),
                BodyPalette.hex(ManePalette.stop(config, part, highlight)), color -> {
                    ManePalette.setStop(config, part, highlight, color);
                    ModelManager.requestSaveActiveModel();
                });
        picker.setAutomaticColor(() -> BodyPalette.hex(ManePalette.automatic(ManePalette.base(config, part), highlight)));
        picker.setLocked(ManePalette.stopLocked(config, part, highlight));
        picker.setOnLockChanged(locked -> {
            ManePalette.setStopLocked(config, part, highlight, locked);
            ModelManager.saveActiveModel();
            context.refreshKeepingScroll();
        });
        picker.setTooltip(Tooltip.of(Text.translatable("text.magicaland.config.mane_stop.tooltip")));
        list.addWidget(picker, SettingsList.Alignment.RIGHT);
    }

    private CustomButton createBackButton(PonyCustomPageContext context, int x, int width, int height,
            String labelKey) {
        return new CustomButton(x, 0, width, height,
                Text.literal("\u2190 " + Text.translatable(labelKey).getString()), false,
                button -> context.openPage(PonyCustomPageContext.Page.MAIN, -1), false, true);
    }

    private int getButtonX(PonyCustomPageContext context, int buttonWidth) {
        if (context.getWidth() < 250) {
            return context.getX() + (context.getWidth() - buttonWidth) / 2;
        }
        return context.getX() + context.getWidth() - buttonWidth - 20;
    }
}
