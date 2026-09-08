package top.csituka.magicaland.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import top.csituka.magicaland.client.animation.PonyExpressions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** 独立 main 回归测试，不启动 Minecraft，不执行 Gradle 构建。 */
public final class PonyFacePoseTest {
    private static int assertions;

    public static void main(String[] args) throws Exception {
        Map<String, GeoBone> bones = load(Path.of(args[0]));
        GeoBone root = bones.get("Emotions");
        check(root != null, "真实模型包含 Emotions");
        normal(bones);
        GeoBone left = bones.get("leye");
        GeoBone right = bones.get("reye");
        left.updateScale(0.9f, 0.9f, 0.9f);
        right.updateScale(1.07f, 1.07f, 1);
        left.updatePosition(0.1f, -0.25f, 0);
        right.updatePosition(-0.1f, 0.15f, 0);
        right.updateRotation(0, 0, 0.03f);
        bones.values().forEach(GeoBone::resetStateChanges);
        right.markRotationAsChanged();
        Map<String, float[]> before = snapshot(bones);

        try (PonyFacePose ignored = PonyFacePose.apply(root)) {
            for (String[] pair : new String[][] {{"leye", "leye2"}, {"leye", "leye3"},
                    {"reye", "reye2"}, {"reye", "reye3"}}) {
                GeoBone source = bones.get(pair[0]);
                GeoBone target = bones.get(pair[1]);
                check(Arrays.equals(transform(source), transform(target)), "眼仁完整姿态映射 " + pair[1]);
                float anchor = source.getPivotY();
                float transformed = target.getPivotY()
                        + (anchor - target.getPivotY()) * target.getScaleY();
                check(Math.abs(transformed - anchor) < 0.0001f, "缩放轴心不会被拉向世界原点 " + pair[1]);
            }
            for (String style : new String[] {"01", "02", "03"}) {
                Set<String> visible = visible(root, style);
                String face = style.equals("01") ? "CommonFace" : "Style" + style + "CommonFace";
                check(visible.contains(face), "选择普通眼型 " + style);
                check(visible.stream().filter(n -> n.endsWith("CommonFace")).count() == 1, "仅一组普通眼型 " + style);
                check(!visible.contains("Style03Smile") && !visible.contains("Smeile"), "普通状态无笑眼叠加 " + style);
            }
        }
        checkRestored(bones, before, "普通渲染恢复");

        normal(bones);
        scale(bones, "emot", 0);
        scale(bones, "shut", 1);
        before = snapshot(bones);
        try (PonyFacePose ignored = PonyFacePose.apply(root)) {
            for (String style : new String[] {"01", "02", "03"}) {
                Set<String> visible = visible(root, style);
                check(visible.contains("shut"), "普通眨眼显示闭眼 " + style);
                check(visible.stream().noneMatch(n -> n.endsWith("CommonFace")), "普通眨眼不漏出睁眼 " + style);
            }
        }
        checkRestored(bones, before, "眨眼恢复");

        for (String expression : new String[] {"Smeile", "close", "ScrunchedEyes"}) {
            normal(bones);
            scale(bones, "CommonFace", 0);
            scale(bones, expression, 1);
            scale(bones, "emot", 0);
            scale(bones, "shut", 1);
            before = snapshot(bones);
            try (PonyFacePose ignored = PonyFacePose.apply(root)) {
                for (String style : new String[] {"01", "02", "03"}) {
                    Set<String> visible = visible(root, style);
                    String expected = expression.equals("Smeile") && style.equals("03") ? "Style03Smile" : expression;
                    check(visible.contains(expected), "保留动作闭眼 " + expression + "/" + style);
                    check(!visible.contains("shut"), "动作闭眼不被眨眼替换 " + expression + "/" + style);
                    check(visible.stream().noneMatch(n -> n.endsWith("CommonFace")), "动作闭眼不叠加普通眼 " + style);
                    if (expression.equals("Smeile")) {
                        check(visible.contains("Smeile") != visible.contains("Style03Smile"), "笑眼样式互斥 " + style);
                    }
                }
            }
            checkRestored(bones, before, "动作表情恢复");
        }

        normal(bones);
        scale(bones, "CommonFace", 0);
        scale(bones, "Angry", 1);
        try (PonyFacePose ignored = PonyFacePose.apply(root)) {
            for (String style : new String[] {"01", "02", "03"}) {
                Set<String> visible = visible(root, style);
                check(visible.contains("Angry"), "保留已有生气表情 " + style);
                check(visible.stream().noneMatch(n -> n.endsWith("CommonFace")), "特殊眼型不穿过生气表情 " + style);
            }
        }

        normal(bones);
        scale(bones, "CommonFace", 0);
        scale(bones, "close", 1);
        scale(bones, "emot", 0.1f);
        scale(bones, "shut", 1);
        before = snapshot(bones);
        try (PonyFacePose ignored = PonyFacePose.apply(root)) {
            for (String style : new String[] {"01", "02", "03"}) {
                Set<String> visible = visible(root, style);
                check(visible.contains("close"), "睡眠/潜行闭眼可达 " + style);
                check(!visible.contains("shut"), "睡眠/潜行无第二组闭眼 " + style);
                check(visible.stream().noneMatch(n -> n.endsWith("CommonFace")), "睡眠/潜行无睁眼 " + style);
            }
        }
        checkRestored(bones, before, "睡眠/潜行恢复");
        for (int i = 0; i < 30; i++) {
            normal(bones);
            scale(bones, "CommonFace", i % 2 == 0 ? 0 : 1);
            scale(bones, "close", i % 2 == 0 ? 1 : 0);
            before = snapshot(bones);
            try (PonyFacePose ignored = PonyFacePose.apply(root)) {
                if (i % 3 == 0) throw new TestRenderException();
            } catch (TestRenderException expected) {
                // 模拟渲染异常，仍需恢复下一位玩家/下一帧所用的骨骼。
            }
            checkRestored(bones, before, "重复切换及异常恢复");
        }
        check(PonyFacePose.shouldRender("CommonFace", null), "空样式回退 01");
        check(!PonyFacePose.shouldRender("Style03Smile", "unknown"), "未知样式无 03 笑眼泄漏");
        try (PonyFacePose ignored = PonyFacePose.apply(new GeoBone(null, "Emotions", false, null, false, false))) {
            check(true, "缺少可选样式骨骼时安全跳过");
        }
        testCatalogAndNewStyle(bones, root, Path.of(args[1]));
        testGaze(bones, root, Path.of(args[1]));
        testGazeGrowth(bones, root);
        System.out.println("PASS " + assertions + " assertions (real mare geometry + GeckoLib 4.8.3 GeoBone)");
    }

    private static void testCatalogAndNewStyle(Map<String, GeoBone> bones, GeoBone root, Path configPath) throws Exception {
        String original = Files.readString(configPath);
        check(PonyExpressions.forAction("run") == PonyExpressions.forExpression("neutral"), "动作引用稳定表情 ID");
        check(PonyExpressions.forAction("attacked") == PonyExpressions.forExpression("scrunched"), "受伤映射保持挤眼");
        check(PonyExpressions.forAction("sleep") == PonyExpressions.forExpression("closed"), "睡眠闭眼来自表情表");
        check(PonyExpressions.forAction("sneak") == PonyExpressions.forExpression("closed"), "原潜行闭眼从渲染器迁移到表情表");
        check(PonyExpressions.forAction(null) == PonyExpressions.forExpression("neutral"), "缺少动作时普通表情回退");
        check(PonyExpressions.forExpression("missing") == PonyExpressions.forExpression("neutral"), "未知表情安全回退");
        check(PonyExpressions.forExpression(null) == PonyExpressions.forExpression("neutral"), "空表情安全回退");
        JsonObject config = JsonParser.parseString(original).getAsJsonObject();
        config.getAsJsonObject("actions").addProperty("jump1", "happy");
        JsonObject style = new JsonObject();
        for (String[] binding : new String[][] {{"normal", "Style04CommonFace"}, {"left_pupil", "Style04LeftEye"},
                {"right_pupil", "Style04RightEye"}, {"happy", "Style04Smile"}}) {
            style.addProperty(binding[0], binding[1]);
            GeoBone added = new GeoBone(root, binding[1], false, null, false, false);
            root.getChildBones().add(added);
            bones.put(binding[1], added);
        }
        config.getAsJsonObject("eye_styles").add("04", style);
        PonyExpressions.reload(new StringReader(config.toString()));
        check(PonyExpressions.forAction("jump1") == PonyExpressions.forExpression("happy"), "修改一条搭配即可换跳跃表情");
        normal(bones);
        Map<String, float[]> before = snapshot(bones);
        try (PonyFacePose ignored = PonyFacePose.apply(root)) {
            Set<String> visible = visible(root, "04");
            check(visible.contains("Style04CommonFace"), "新增眼型只补映射即可显示普通眼");
            check(!visible.contains("Style04Smile") && !visible.contains("Style03CommonFace"), "新眼型互斥适配");
            check(Arrays.equals(transform(bones.get("leye")), transform(bones.get("Style04LeftEye"))), "新眼型自动适配眨眼和 pivot");
        }
        checkRestored(bones, before, "新眼型恢复");
        var valid = PonyExpressions.forAction("jump1");
        config.getAsJsonObject("actions").addProperty("jump1", "nonexistent");
        try {
            PonyExpressions.reload(new StringReader(config.toString()));
            throw new AssertionError("无效表情引用不应通过");
        } catch (IllegalArgumentException expected) {
            check(PonyExpressions.forAction("jump1") == valid, "无效资源重载保留上一份有效配置");
        }
        config.getAsJsonObject("actions").addProperty("jump1", "happy");
        config.getAsJsonObject("eye_styles").getAsJsonObject("04").addProperty("normal", "Smeile");
        try {
            PonyExpressions.reload(new StringReader(config.toString()));
            throw new AssertionError("跨通道骨骼绑定不应通过");
        } catch (IllegalArgumentException expected) {
            check(PonyExpressions.forAction("jump1") == valid, "错误骨骼绑定不污染有效配置");
        }
        PonyExpressions.reload(new StringReader(original));
    }

    private static void testGaze(Map<String, GeoBone> bones, GeoBone root, Path configPath) throws Exception {
        for (String style : new String[] {"01", "02", "03"}) {
            for (float[] direction : new float[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {10, 10}}) {
                normal(bones);
                var before = snapshot(bones);
                try (PonyFacePose face = PonyFacePose.apply(root)) {
                    var adapted = snapshot(bones);
                    String leftName = face.pupil(style, true).getName();
                    String rightName = face.pupil(style, false).getName();
                    check(face.allowsGaze(), "普通眼允许注视 " + style);
                    face.gaze(style, direction[0], direction[1]);
                    var after = snapshot(bones);
                    for (var entry : adapted.entrySet()) {
                        float[] expected = entry.getValue().clone();
                        float[] actual = after.get(entry.getKey());
                        if (entry.getKey().equals(leftName) || entry.getKey().equals(rightName)) {
                            var limit = PonyExpressions.gazeLimits(style);
                            float dx = actual[3] - expected[3];
                            float dy = actual[4] - expected[4];
                            check(Math.abs(dx) <= limit.horizontal() + 0.00001f, "水平不越限 " + style);
                            check(dy >= -limit.down() - 0.00001f && dy <= limit.up() + 0.00001f, "垂直不越限 " + style);
                            boolean isLeft = entry.getKey().equals(leftName);
                            var eyeLimit = limit.eye(isLeft);
                            check(isLeft ? dx >= -0.00001f : dx <= 0.00001f, "禁止眼仁继续向鼻梁移动 " + style);
                            if (isLeft ? direction[0] <= 0 : direction[0] >= 0) check(Math.abs(dx) < 0.00001f, "非目标侧眼仁水平保持原位 " + style);
                            check(Math.abs(dx) <= eyeLimit.outward() + 0.00001f, "两眼分别限位 " + style);
                            if (direction[1] != 0) check(Math.signum(dy) == Math.signum(direction[1]), "两眼均可上下移动 " + style);
                            expected[3] = actual[3];
                            expected[4] = actual[4];
                            expected[13] = actual[13];
                        }
                        check(Arrays.equals(expected, actual), "仅选中眼型的眼仁 XY 可变 " + entry.getKey());
                    }
                    throw new TestRenderException();
                } catch (TestRenderException expected) {
                }
                checkRestored(bones, before, "注视异常后全骨骼恢复 " + style);
            }
        }
        for (String expression : new String[] {"Smeile", "close", "ScrunchedEyes", "Angry"}) {
            normal(bones);
            scale(bones, "CommonFace", 0);
            scale(bones, expression, 1);
            try (PonyFacePose face = PonyFacePose.apply(root)) {
                var before = snapshot(bones);
                check(!face.allowsGaze(), "特殊表情禁止自动注视 " + expression);
                face.gaze("01", 1, 1);
                checkRestored(bones, before, "不干扰特殊表情 " + expression);
            }
        }
        check(PonyExpressions.allowsAutomaticGaze("idle"), "idle 允许自动注视");
        check(PonyExpressions.allowsAutomaticGaze("run"), "run 保留普通眼并允许注视");
        for (String action : new String[] {"sleep", "sneak", "wave_hand", "Ballet", "backward_walk", "fall_transfer", "missing"}) {
            check(!PonyExpressions.allowsAutomaticGaze(action), "作者演出和未知动作不被覆盖 " + action);
        }
        check(!PonyExpressions.allowsAutomaticGaze(null), "无玩家预览不注视");
        String original = Files.readString(configPath);
        var bad = JsonParser.parseString(original).getAsJsonObject();
        bad.getAsJsonObject("expressions").getAsJsonObject("neutral").addProperty("gaze", "invalid");
        try { PonyExpressions.reload(new StringReader(bad.toString())); throw new AssertionError("非法注视模式被接受"); }
        catch (IllegalArgumentException expected) { check(PonyExpressions.allowsAutomaticGaze("idle"), "非法模式保留上一份配置"); }
        bad = JsonParser.parseString(original).getAsJsonObject();
        bad.getAsJsonObject("eye_styles").getAsJsonObject("01").getAsJsonObject("gaze_limits").addProperty("horizontal", 99);
        try { PonyExpressions.reload(new StringReader(bad.toString())); throw new AssertionError("非法移动限位被接受"); }
        catch (IllegalArgumentException expected) { check(PonyExpressions.gazeLimits("01").horizontal() < 1, "非法限位保留上一份配置"); }
        bad = JsonParser.parseString(original).getAsJsonObject();
        bad.getAsJsonObject("eye_styles").getAsJsonObject("03").getAsJsonObject("gaze_limits").getAsJsonObject("left").addProperty("outward", 0.99);
        try { PonyExpressions.reload(new StringReader(bad.toString())); throw new AssertionError("超出眼型总限位的单眼配置被接受"); }
        catch (IllegalArgumentException expected) { check(PonyExpressions.gazeLimits("03").left().outward() < 0.99, "非法单眼限位保留原配置"); }
        check(PonyExpressions.gazeLimits("01").left().equals(PonyExpressions.gazeLimits("01").right()), "省略单眼配置时继承公共限位");
        check(PonyExpressions.gazeLimits("03").left().outward() < PonyExpressions.gazeLimits("03").right().outward(), "03 两眼独立校准");
        var legacy = JsonParser.parseString(original).getAsJsonObject();
        legacy.getAsJsonObject("expressions").entrySet().forEach(entry -> entry.getValue().getAsJsonObject().remove("gaze"));
        PonyExpressions.reload(new StringReader(legacy.toString()));
        check(!PonyExpressions.allowsAutomaticGaze("idle"), "旧资源包没有授权时不覆盖眼神");
        PonyExpressions.reload(new StringReader(original));
    }

    private static Map<String, GeoBone> load(Path path) throws Exception {
        JsonArray definitions = JsonParser.parseString(Files.readString(path)).getAsJsonObject()
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
        Map<String, GeoBone> bones = new HashMap<>();
        for (JsonElement element : definitions) {
            JsonObject data = element.getAsJsonObject();
            String name = data.get("name").getAsString();
            GeoBone parent = data.has("parent") ? bones.get(data.get("parent").getAsString()) : null;
            check(!data.has("parent") || parent != null, "骨骼父节点已加载 " + name);
            GeoBone bone = new GeoBone(parent, name, false, null, false, false);
            if (name.matches("[lr]eye[23]?")) {
                for (JsonElement cubeElement : data.getAsJsonArray("cubes")) {
                    JsonObject cube = cubeElement.getAsJsonObject();
                    check(!cube.has("rotation"), "当前眼仁 cube 没有局部旋转 " + name);
                    JsonArray origin = cube.getAsJsonArray("origin"), size = cube.getAsJsonArray("size");
                    double x = origin.get(0).getAsDouble(), y = origin.get(1).getAsDouble(), z = origin.get(2).getAsDouble();
                    double sx = size.get(0).getAsDouble(), sy = size.get(1).getAsDouble(), sz = size.get(2).getAsDouble();
                    // 限位只读顶点极值，两个对角顶点已覆盖真实 cube 的包围范围。
                    GeoVertex[] vertices = {new GeoVertex(-(x + sx) / 16, y / 16, z / 16), new GeoVertex(-x / 16, (y + sy) / 16, (z + sz) / 16)};
                    bone.getCubes().add(new GeoCube(new GeoQuad[] {new GeoQuad(vertices, null, null)}, null, null, null, 0, false));
                }
            }
            JsonArray pivot = data.getAsJsonArray("pivot");
            if (pivot != null) bone.updatePivot(-pivot.get(0).getAsFloat(), pivot.get(1).getAsFloat(), pivot.get(2).getAsFloat());
            if (data.has("rotation")) {
                JsonArray r = data.getAsJsonArray("rotation");
                bone.updateRotation((float) Math.toRadians(-r.get(0).getAsFloat()),
                        (float) Math.toRadians(-r.get(1).getAsFloat()), (float) Math.toRadians(r.get(2).getAsFloat()));
            }
            if (parent != null) parent.getChildBones().add(bone);
            bones.put(name, bone);
        }
        return bones;
    }

    private static void normal(Map<String, GeoBone> bones) {
        scale(bones, "emot", 1);
        scale(bones, "CommonFace", 1);
        for (String name : new String[] {"Smeile", "close", "Angry", "ScrunchedEyes", "shut"}) scale(bones, name, 0);
    }

    private static void testGazeGrowth(Map<String, GeoBone> bones, GeoBone root) {
        normal(bones);
        for (String style : new String[] {"01", "02", "03"}) {
            for (boolean left : new boolean[] {true, false}) {
                for (float[] direction : new float[][] {{left ? 1 : -1, 0}, {0, 1}, {0, -1}}) {
                    float[] displacement = new float[2];
                    for (int i = 0; i < 2; i++) {
                        for (String name : new String[] {"leye", "reye"}) {
                            bones.get(name).updateScale(i == 0 ? 1 : 1.07f, i == 0 ? 1 : 1.07f, 1);
                            bones.get(name).updatePosition(0, 0, 0);
                        }
                        try (PonyFacePose face = PonyFacePose.apply(root)) {
                            face.gaze(style, direction[0], direction[1]);
                            var pupil = face.pupil(style, left);
                            displacement[i] = Math.abs(direction[0] == 0 ? pupil.getPosY() : pupil.getPosX());
                        }
                    }
                    check(displacement[0] > displacement[1], "眨眼回弹放大时动态收回安全余量 " + style + "/" + left);
                }
            }
        }
    }

    private static void scale(Map<String, GeoBone> bones, String name, float value) {
        bones.get(name).updateScale(value, value, value);
    }

    private static Set<String> visible(GeoBone root, String style) {
        Set<String> result = new HashSet<>();
        visit(root, style, result);
        return result;
    }

    private static void visit(GeoBone bone, String style, Set<String> result) {
        if (!PonyFacePose.shouldRender(bone.getName(), style)
                || bone.getScaleX() <= 0 || bone.getScaleY() <= 0 || bone.getScaleZ() <= 0) return;
        if (!bone.isHidden()) result.add(bone.getName());
        if (!bone.isHidingChildren()) bone.getChildBones().forEach(child -> visit(child, style, result));
    }

    private static float[] transform(GeoBone b) {
        return new float[] {b.getScaleX(), b.getScaleY(), b.getScaleZ(), b.getPosX(), b.getPosY(), b.getPosZ(),
                b.getRotX(), b.getRotY(), b.getRotZ(), b.getPivotX(), b.getPivotY(), b.getPivotZ()};
    }

    private static Map<String, float[]> snapshot(Map<String, GeoBone> bones) {
        Map<String, float[]> result = new HashMap<>();
        bones.forEach((name, b) -> {
            float[] state = Arrays.copyOf(transform(b), 17);
            state[12] = b.hasScaleChanged() ? 1 : 0;
            state[13] = b.hasPositionChanged() ? 1 : 0;
            state[14] = b.hasRotationChanged() ? 1 : 0;
            state[15] = b.isHidden() ? 1 : 0;
            state[16] = b.isHidingChildren() ? 1 : 0;
            result.put(name, state);
        });
        return result;
    }

    private static void checkRestored(Map<String, GeoBone> bones, Map<String, float[]> expected, String label) {
        Map<String, float[]> actual = snapshot(bones);
        expected.forEach((name, state) -> check(Arrays.equals(state, actual.get(name)), label + ": " + name));
    }

    private static void check(boolean condition, String label) {
        assertions++;
        if (!condition) throw new AssertionError(label);
    }

    private static final class TestRenderException extends RuntimeException {}
}
