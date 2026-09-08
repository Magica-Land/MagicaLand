package top.csituka.magicaland.client.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.Random;

public final class PonyGazeMathTest {
    private static int assertions;
    public static void main(String[] args) {
        Matrix4f identity = new Matrix4f();
        Vector3f center = new Vector3f(0, 1.6f, -0.6f);
        var left = PonyGazeMath.project(identity, new Vector3f(2, 1.6f, -3), center);
        check(left.x() < 0 && left.y() == 0, "正本地 X 对应负 position.x");
        check(PonyGazeMath.project(identity, new Vector3f(0, 3, -3), center).y() > 0, "向上看");
        check(PonyGazeMath.project(identity, new Vector3f(0, 0, -3), center).y() < 0, "向下看");
        check(PonyGazeMath.project(identity, new Vector3f(0, 2, 3), center).equals(PonyGazeMath.Offset.ZERO), "不反向追背后目标");
        check(PonyGazeMath.project(new Matrix4f().scale(0), new Vector3f(1), center).equals(PonyGazeMath.Offset.ZERO), "隐藏骨骼退化矩阵安全");
        Random random = new Random(8036);
        for (int i = 0; i < 1500; i++) {
            Vector3f target = new Vector3f(random.nextFloat() * 8 - 4, random.nextFloat() * 8 - 2, -random.nextFloat() * 6 - 1);
            var expected = PonyGazeMath.project(identity, target, center);
            Matrix4f transform = new Matrix4f().translate(random.nextFloat() * 20, random.nextFloat() * 20, random.nextFloat() * 20)
                    .rotateXYZ(random.nextFloat() * 6, random.nextFloat() * 6, random.nextFloat() * 6)
                    .scale(0.4f + random.nextFloat(), 0.4f + random.nextFloat(), 0.4f + random.nextFloat());
            var actual = PonyGazeMath.project(transform, transform.transformPosition(new Vector3f(target)), center);
            near(actual.x(), expected.x(), "任意头姿/视角 X " + i);
            near(actual.y(), expected.y(), "任意头姿/视角 Y " + i);
            check(actual.x() * actual.x() + actual.y() * actual.y() <= 1.00001f, "椭圆限位 " + i);
        }
        var slow = new PonyGazeMath.Smoother();
        var fast = new PonyGazeMath.Smoother();
        var target = new PonyGazeMath.Offset(0.6f, -0.5f);
        slow.step(target, 0);
        fast.step(target, 0);
        PonyGazeMath.Offset slowResult = null;
        PonyGazeMath.Offset fastResult = null;
        for (int tick = 1; tick <= 20; tick++) slowResult = slow.step(target, tick);
        for (int frame = 1; frame <= 200; frame++) fastResult = fast.step(target, frame / 10.0);
        near(slowResult.x(), fastResult.x(), "帧率独立 X");
        near(slowResult.y(), fastResult.y(), "帧率独立 Y");
        var repeated = fast.step(new PonyGazeMath.Offset(-1, 0), 20);
        near(repeated.x(), fastResult.x(), "同一时刻多次渲染不重复推进");
        for (int tick = 21; tick <= 60; tick++) fastResult = fast.step(PonyGazeMath.Offset.ZERO, tick);
        near(fastResult.x(), 0, "目标丢失回正 X");
        near(fastResult.y(), 0, "目标丢失回正 Y");
        check(fast.step(target, 1).equals(PonyGazeMath.Offset.ZERO), "时间回退/重生清除状态");
        fast.reset();
        check(fast.step(target, 100).equals(PonyGazeMath.Offset.ZERO), "手动关闭后无残留");
        System.out.println("PASS PonyGazeMathTest: " + assertions + " assertions");
    }
    private static void near(float a, float b, String message) { check(Math.abs(a - b) < 0.00002f, message + " " + a + "/" + b); }
    private static void check(boolean value, String message) { assertions++; if (!value) throw new AssertionError(message); }
}
