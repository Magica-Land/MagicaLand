package top.csituka.magicaland.client.gui.tab.ponycustom;

import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.config.ModelManager;
import top.csituka.magicaland.client.gui.widget.CustomButton;
import top.csituka.magicaland.client.gui.widget.DropdownBox;
import top.csituka.magicaland.client.gui.widget.SettingsList;

public class ModelPage implements PonyCustomPage {
    private boolean createNewOpen;
    private boolean deleteConfirmOpen;
    private String modelToDelete;

    private DropdownBox modelDropdown;
    private CustomButton editButton;
    private CustomButton deleteButton;
    private TextFieldWidget newModelNameField;

    @Override
    public void onEnter() {
        createNewOpen = false;
        deleteConfirmOpen = false;
        modelToDelete = null;
    }

    @Override
    public void build(PonyCustomPageContext context, SettingsList list) {
        ModelManager.refreshModelList();
        List<String> models = ModelManager.getAvailableModels();

        int x = context.getX();
        int y = context.getY();
        int width = context.getWidth();
        int height = context.getHeight();
        int dropdownWidth = Math.min(120, width / 2 - 30);
        int dropdownHeight = 20;
        int iconButtonWidth = 20;

        int totalWidth = dropdownWidth + iconButtonWidth * 3 + 15;
        int startX = x + width - totalWidth - 10;
        if (width < 250) {
            startX = x + (width - totalWidth) / 2;
        }
        int centerY = y + height / 2;

        if (createNewOpen) {
            buildCreateModel(context, x, width, centerY);
        } else if (deleteConfirmOpen) {
            buildDeleteModel(context, x, width, centerY);
        } else {
            int initialSelect = -1;
            ModelConfig activeModel = ModelManager.getActiveModel();
            if (activeModel != null) {
                initialSelect = models.indexOf(activeModel.name);
            }

            modelDropdown = new DropdownBox(startX, centerY, dropdownWidth, dropdownHeight, models, initialSelect, index -> {
                editButton.active = true;
                deleteButton.active = models.size() > 1;
                ModelManager.loadModel(models.get(index));
            });
            context.getScreen().addConsoleWidget(modelDropdown);

            editButton = new CustomButton(startX + dropdownWidth + 5, centerY, iconButtonWidth, dropdownHeight,
                    Text.literal("\u270E"), Text.translatable("text.magicaland.config.tooltip.edit"), false,
                    button -> {
                        String selected = modelDropdown.getSelectedOption();
                        if (selected != null && ModelManager.loadModel(selected)) {
                            context.openPage(PonyCustomPageContext.Page.MAIN, 0);
                        }
                    });
            editButton.active = activeModel != null;
            context.getScreen().addConsoleWidget(editButton);

            deleteButton = new CustomButton(startX + dropdownWidth + iconButtonWidth + 10, centerY,
                    iconButtonWidth, dropdownHeight, Text.literal("\u2716"),
                    Text.translatable("text.magicaland.config.tooltip.delete"), false,
                    button -> {
                        String selected = modelDropdown.getSelectedOption();
                        if (selected != null && ModelManager.getAvailableModels().size() > 1) {
                            deleteConfirmOpen = true;
                            modelToDelete = selected;
                            context.reinit();
                        }
                    });
            deleteButton.active = models.size() > 1;
            context.getScreen().addConsoleWidget(deleteButton);

            CustomButton newButton = new CustomButton(startX + dropdownWidth + iconButtonWidth * 2 + 15,
                    centerY, iconButtonWidth, dropdownHeight, Text.literal("+"),
                    Text.translatable("text.magicaland.config.tooltip.new"), false,
                    button -> {
                        createNewOpen = true;
                        context.reinit();
                    });
            context.getScreen().addConsoleWidget(newButton);
        }
    }

    private void buildCreateModel(PonyCustomPageContext context, int x, int width, int centerY) {
        int fieldWidth = 150;
        int createX = x + width - fieldWidth - 20;
        if (width < 250) {
            createX = x + (width - fieldWidth) / 2;
        }

        newModelNameField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, createX, centerY - 20,
                fieldWidth, 20, Text.translatable("text.magicaland.config.create_model.name_placeholder"));
        newModelNameField.setMaxLength(32);
        newModelNameField.setFocused(true);
        context.getScreen().addConsoleWidget(newModelNameField);

        CustomButton confirmButton = new CustomButton(createX, centerY + 10, 70, 20,
                Text.translatable("text.magicaland.config.button.confirm"), false,
                button -> {
                    if (ModelManager.createModel(newModelNameField.getText())) {
                        createNewOpen = false;
                        context.reinit();
                    }
                });
        context.getScreen().addConsoleWidget(confirmButton);

        CustomButton cancelButton = new CustomButton(createX + 80, centerY + 10, 70, 20,
                Text.translatable("text.magicaland.config.button.cancel"), false,
                button -> {
                    createNewOpen = false;
                    context.reinit();
                });
        context.getScreen().addConsoleWidget(cancelButton);
    }

    private void buildDeleteModel(PonyCustomPageContext context, int x, int width, int centerY) {
        int boxWidth = 150;
        int deleteX = x + width - boxWidth - 20;
        if (width < 250) {
            deleteX = x + (width - boxWidth) / 2;
        }

        CustomButton confirmButton = new CustomButton(deleteX, centerY + 10, 70, 20,
                Text.translatable("text.magicaland.config.button.confirm"), false,
                button -> {
                    if (modelToDelete != null && ModelManager.deleteModel(modelToDelete)) {
                        deleteConfirmOpen = false;
                        modelToDelete = null;
                        context.reinit();
                    }
                });
        context.getScreen().addConsoleWidget(confirmButton);

        CustomButton cancelButton = new CustomButton(deleteX + 80, centerY + 10, 70, 20,
                Text.translatable("text.magicaland.config.button.cancel"), false,
                button -> {
                    deleteConfirmOpen = false;
                    modelToDelete = null;
                    context.reinit();
                });
        context.getScreen().addConsoleWidget(cancelButton);
    }

    @Override
    public void render(PonyCustomPageContext context, DrawContext drawContext, int mouseX, int mouseY,
            float delta, float alpha) {
        if (!createNewOpen && !deleteConfirmOpen) return;

        var textRenderer = MinecraftClient.getInstance().textRenderer;
        int textAlpha = (int) (alpha * 255);
        int x = context.getX();
        int y = context.getY();
        int width = context.getWidth();
        int height = context.getHeight();
        int boxX = x + width - 150 - 20;
        int labelY = y + height / 2 - 40;

        if (width < 250) {
            drawContext.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable(createNewOpen
                            ? "text.magicaland.config.create_model.title"
                            : "text.magicaland.config.delete_confirm.title"),
                    x + width / 2, labelY, (textAlpha << 24) | 0xFFFFFF);
            if (deleteConfirmOpen) {
                drawContext.drawCenteredTextWithShadow(textRenderer,
                        Text.translatable("text.magicaland.config.delete_confirm.message"),
                        x + width / 2, labelY + 15, (textAlpha << 24) | 0xE06060);
            }
        } else if (createNewOpen) {
            drawContext.drawTextWithShadow(textRenderer,
                    Text.translatable("text.magicaland.config.create_model.title"),
                    boxX, labelY, (textAlpha << 24) | 0xFFFFFF);
        } else {
            drawContext.drawTextWithShadow(textRenderer,
                    Text.translatable("text.magicaland.config.delete_confirm.title"),
                    boxX, labelY, (textAlpha << 24) | 0xFFFFFF);
            drawContext.drawTextWithShadow(textRenderer,
                    Text.translatable("text.magicaland.config.delete_confirm.message"),
                    boxX, labelY + 15, (textAlpha << 24) | 0xE06060);
        }
    }

    @Override
    public boolean isPreviewActive() {
        return ModelManager.getActiveModel() != null && !createNewOpen && !deleteConfirmOpen;
    }
}
