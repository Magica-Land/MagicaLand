package top.csituka.magicaland.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import top.csituka.magicaland.client.config.Config;
import top.csituka.magicaland.client.gui.ConfigScreen;
import top.csituka.magicaland.client.network.ClientNetworkHandler;
import top.csituka.magicaland.client.render.BodyTintTextures;
import top.csituka.magicaland.client.render.ManeTintTextures;
import top.csituka.magicaland.client.render.MagicGlow;
import top.csituka.magicaland.client.render.BodyFlightAura;
import top.csituka.magicaland.client.render.GlowingItem;
import top.csituka.magicaland.client.render.EyeTintTextures;
import top.csituka.magicaland.client.render.TransformationParticles;
import top.csituka.magicaland.client.animation.PonyExpressions;
import top.csituka.magicaland.client.animation.PonyFlightVisuals;
import top.csituka.magicaland.client.sound.MagicHeldItemSounds;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Client implements ClientModInitializer {
    private static KeyBinding configKeyBinding;

    @Override
    public void onInitializeClient() {
        Config.load();
        top.csituka.magicaland.client.api.AppearanceOverrideState.init();
        BodyTintTextures.init();
        ManeTintTextures.init();
        MagicGlow.init();
        BodyFlightAura.init();
        GlowingItem.initLevitation();
        EyeTintTextures.init();
        TransformationParticles.init();
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override public Identifier getFabricId() { return new Identifier("magicaland", "expressions"); }
            @Override public void reload(ResourceManager manager) {
                try (var input = manager.getResourceOrThrow(new Identifier("magicaland", "expressions.json")).getInputStream();
                     var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                    PonyExpressions.reload(reader);
                } catch (Exception e) {
                    org.slf4j.LoggerFactory.getLogger(Client.class).error("表情库加载失败，保留上一份有效配置", e);
                }
            }
        });
        top.csituka.magicaland.client.config.ModelManager.init();
        MagicHeldItemSounds.init();
        top.csituka.magicaland.client.sound.PonyHoofSounds.init();

        // 注册客户端网络处理
        ClientNetworkHandler.register();
        PonyFlightVisuals.register();
        top.csituka.magicaland.client.render.UnicornFlightRim.init();

        configKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.magicaland.config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F9,
                "category.magicaland.keys"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (configKeyBinding.wasPressed()) {
                if (!(client.currentScreen instanceof ConfigScreen)
                        && !top.csituka.magicaland.client.config.ModelManager.isEditing())
                    client.setScreen(new ConfigScreen(client.currentScreen));
            }
        });
    }
}
