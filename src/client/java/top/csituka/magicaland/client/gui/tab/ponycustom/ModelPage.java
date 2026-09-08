package top.csituka.magicaland.client.gui.tab.ponycustom;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.config.ModelManager;
import top.csituka.magicaland.client.gui.widget.CustomButton;
import top.csituka.magicaland.client.gui.widget.SectionLabel;
import top.csituka.magicaland.client.gui.widget.SettingsList;

public class ModelPage implements PonyCustomPage {
    private boolean createOpen;
    private String deleteTarget;
    private String draft = "";
    private boolean error;
    private PonyCustomPageContext context;

    @Override
    public void onEnter() {
        createOpen = false;
        deleteTarget = null;
        draft = "";
        error = false;
    }

    @Override
    public void build(PonyCustomPageContext context, SettingsList list) {
        this.context = context;
        int x = context.getX() + 10;
        int width = context.getControlWidth();
        if (createOpen || deleteTarget != null) {
            buildConfirmation(x, width);
            return;
        }
        ModelManager.refreshModelList();
        List<String> models = ModelManager.getAvailableModels();
        list.addWidget(new SectionLabel(x, 0, width, 20, tr("preset.saved")), SettingsList.Alignment.RIGHT);
        ModelConfig current = ModelManager.getActiveModel();
        for (String name : models) {
            boolean selected = current != null && name.equals(current.name);
            list.addWidget(new CustomButton(x, 0, width, 20, Text.literal((selected ? "✓ " : "") + name),
                    selected, button -> {
                        if (selected) return;
                        if (ModelManager.loadModel(name)) context.reinit();
                        else { error = true; context.reinit(); }
                    }), SettingsList.Alignment.RIGHT);
        }
        list.addWidget(new CustomButton(x, 0, width, 20, tr("preset.create"), false, button -> {
            createOpen = true;
            draft = "";
            error = false;
            context.reinit();
        }), SettingsList.Alignment.RIGHT);
        CustomButton duplicate = new CustomButton(x, 0, width, 20, tr("preset.duplicate"), false, button -> {
            if (ModelManager.duplicateActiveModel(tr("preset.copy_suffix").getString())) {
                error = false;
                context.reinit();
            } else {
                error = true;
                context.reinit();
            }
        });
        duplicate.active = current != null;
        list.addWidget(duplicate, SettingsList.Alignment.RIGHT);
        CustomButton delete = new CustomButton(x, 0, width, 20, tr("preset.delete"), false, button -> {
            deleteTarget = ModelManager.getActiveModel().name;
            error = false;
            context.reinit();
        });
        delete.active = current != null && models.size() > 1;
        list.addWidget(delete, SettingsList.Alignment.RIGHT);
    }

    private void buildConfirmation(int x, int width) {
        int y = context.getY() + 34;
        if (createOpen) {
            TextFieldWidget field = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, x, y, width,
                    20, Text.translatable("text.magicaland.config.create_model.name_placeholder"));
            field.setMaxLength(32);
            field.setText(draft);
            field.setChangedListener(value -> { draft = value; error = false; });
            context.getScreen().addConsoleWidget(field);
            context.getScreen().setFocused(field);
            field.setFocused(true);
        }
        int half = (width - 4) / 2;
        context.getScreen().addConsoleWidget(new CustomButton(x, y + 30, half, 20,
                Text.translatable("text.magicaland.config.button.confirm"), false, button -> confirm()));
        context.getScreen().addConsoleWidget(new CustomButton(x + half + 4, y + 30, half, 20,
                Text.translatable("text.magicaland.config.button.cancel"), false, button -> cancel()));
    }

    private void confirm() {
        boolean success = createOpen ? ModelManager.createModel(draft) : ModelManager.deleteModel(deleteTarget);
        if (success) { createOpen = false; deleteTarget = null; error = false; }
        else error = true;
        context.reinit();
    }

    private void cancel() {
        createOpen = false;
        deleteTarget = null;
        error = false;
        context.reinit();
    }

    @Override
    public boolean keyPressed(int key) {
        if (!createOpen && deleteTarget == null) return false;
        if (key == 256) { cancel(); return true; }
        if (key == 257 || key == 335) { confirm(); return true; }
        return false;
    }

    @Override
    public boolean isEditingPreset() { return createOpen || deleteTarget != null; }

    @Override
    public void render(PonyCustomPageContext context, DrawContext draw, int mouseX, int mouseY, float delta, float alpha) {
        int x = context.getX() + 10;
        int y = context.getY() + 10;
        if (createOpen) PonyCustomPageHelper.drawWrapped(draw, tr("preset.create"), x, y, context.getControlWidth(), 0xFFFFFFFF);
        if (deleteTarget != null) PonyCustomPageHelper.drawWrapped(draw,
                Text.translatable("text.magicaland.customize.preset.delete_confirm", deleteTarget), x, y,
                context.getControlWidth(), 0xFFFFB7B7);
        if (error) PonyCustomPageHelper.drawWrapped(draw, tr("preset.operation_error"), x, context.getY() + 102,
                context.getControlWidth(), 0xFFFF9999);
    }

    private static Text tr(String key) { return Text.translatable("text.magicaland.customize." + key); }
}
