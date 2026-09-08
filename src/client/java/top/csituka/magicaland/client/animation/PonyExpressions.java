package top.csituka.magicaland.client.animation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** 稳定表情 ID、作者指定的动作搭配，以及不同眼型的骨骼适配。 */
public final class PonyExpressions {
    public static final Map<String, String> CHANNELS = Map.of(
            "normal", "CommonFace", "happy", "Smeile", "angry", "Angry",
            "closed", "close", "scrunched", "ScrunchedEyes", "left_pupil", "leye", "right_pupil", "reye");
    private static volatile Catalog catalog = loadBundled();

    public record Expression(String id, String name, String animation) {}
    public record EyeStyle(String id, Map<String, String> bones) {}
    private record Catalog(Map<String, Expression> expressions, Map<String, RawAnimation> animations,
                           Map<String, RawAnimation> actions, Map<String, EyeStyle> styles, Set<String> styleBones) {}

    private PonyExpressions() {}

    public static RawAnimation forExpression(String id) {
        Catalog current = catalog;
        return id == null ? current.animations.get("neutral")
                : current.animations.getOrDefault(id, current.animations.get("neutral"));
    }

    public static RawAnimation forAction(String action) {
        Catalog current = catalog;
        return action == null ? current.animations.get("neutral")
                : current.actions.getOrDefault(action, current.animations.get("neutral"));
    }

    public static Map<String, Expression> expressions() { return catalog.expressions; }
    public static Map<String, EyeStyle> eyeStyles() { return catalog.styles; }

    public static boolean shouldRender(String boneName, String styleId) {
        Catalog current = catalog;
        if (!current.styleBones.contains(boneName)) return true;
        EyeStyle style = styleId == null ? current.styles.get("01")
                : current.styles.getOrDefault(styleId, current.styles.get("01"));
        return style.bones.containsValue(boneName);
    }

    public static void reload(Reader reader) {
        catalog = parse(reader);
    }

    private static Catalog loadBundled() {
        try (var input = PonyExpressions.class.getResourceAsStream("/assets/magicaland/expressions.json")) {
            if (input == null) throw new IllegalStateException("缺少内置 expressions.json");
            return parse(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("无法读取内置表情库", e);
        }
    }

    private static Catalog parse(Reader reader) {
        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
        if (json.get("version").getAsInt() != 1) throw new IllegalArgumentException("不支持的表情库版本");
        Map<String, Expression> definitions = new HashMap<>();
        Map<String, RawAnimation> animations = new HashMap<>();
        for (var entry : json.getAsJsonObject("expressions").entrySet()) {
            String id = entry.getKey();
            if (!id.matches("[a-z][a-z0-9_]{0,47}")) throw new IllegalArgumentException("无效的表情 ID: " + id);
            JsonObject value = entry.getValue().getAsJsonObject();
            String animation = value.get("animation").getAsString();
            if (!animation.matches("face\\.[a-z][a-z0-9_]{0,47}")) throw new IllegalArgumentException("无效的表情动画: " + animation);
            definitions.put(id, new Expression(id, value.get("name").getAsString(), animation));
            animations.put(id, RawAnimation.begin().thenLoop(animation));
        }
        if (!definitions.containsKey("neutral")) throw new IllegalArgumentException("表情库必须包含 neutral");
        Map<String, RawAnimation> actions = new HashMap<>();
        for (var entry : json.getAsJsonObject("actions").entrySet()) {
            JsonElement value = entry.getValue();
            if (value.isJsonPrimitive()) {
                actions.put(entry.getKey(), require(animations, value.getAsString()));
            } else {
                JsonObject sequence = value.getAsJsonObject();
                Expression first = require(definitions, sequence.get("expression").getAsString());
                Expression next = require(definitions, sequence.get("then_loop").getAsString());
                actions.put(entry.getKey(), RawAnimation.begin().thenPlay(first.animation).thenLoop(next.animation));
            }
        }
        Map<String, EyeStyle> styles = new HashMap<>();
        Set<String> styleBones = new HashSet<>(CHANNELS.values());
        Map<String, String> boneRoles = new HashMap<>();
        CHANNELS.forEach((role, bone) -> boneRoles.put(bone, role));
        for (var entry : json.getAsJsonObject("eye_styles").entrySet()) {
            Map<String, String> bindings = new HashMap<>(CHANNELS);
            for (var binding : entry.getValue().getAsJsonObject().entrySet()) {
                if (!CHANNELS.containsKey(binding.getKey())) throw new IllegalArgumentException("未知眼型通道: " + binding.getKey());
                String bone = binding.getValue().getAsString();
                if (bone.isBlank() || bone.length() > 80) throw new IllegalArgumentException("无效眼型骨骼名");
                bindings.put(binding.getKey(), bone);
            }
            if (new HashSet<>(bindings.values()).size() != CHANNELS.size()) {
                throw new IllegalArgumentException("同一眼型的通道不能共用同一骨骼: " + entry.getKey());
            }
            bindings.forEach((role, bone) -> {
                String previousRole = boneRoles.putIfAbsent(bone, role);
                if (previousRole != null && !previousRole.equals(role)) {
                    throw new IllegalArgumentException("骨骼不能绑定到不同的表情通道: " + bone);
                }
            });
            styleBones.addAll(bindings.values());
            styles.put(entry.getKey(), new EyeStyle(entry.getKey(), Map.copyOf(bindings)));
        }
        if (!styles.containsKey("01") || !styles.get("01").bones.equals(CHANNELS)) {
            throw new IllegalArgumentException("01 眼型必须保留标准表情通道");
        }
        return new Catalog(Map.copyOf(definitions), Map.copyOf(animations), Map.copyOf(actions),
                Map.copyOf(styles), Set.copyOf(styleBones));
    }

    private static <T> T require(Map<String, T> values, String id) {
        T value = values.get(id);
        if (value == null) throw new IllegalArgumentException("未知表情 ID: " + id);
        return value;
    }
}
