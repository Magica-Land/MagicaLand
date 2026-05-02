package top.csituka.magicaland.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import top.csituka.magicaland.client.config.Config;
import top.csituka.magicaland.client.gui.ConfigScreen;

public class Client implements ClientModInitializer {
    private static KeyBinding configKeyBinding;

    @Override
    public void onInitializeClient() {
        Config.load();

        configKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.magicaland.config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F9,
                "category.magicaland.keys"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (configKeyBinding.wasPressed()) {
                client.setScreen(new ConfigScreen(client.currentScreen));
            }
        });
    }
}
