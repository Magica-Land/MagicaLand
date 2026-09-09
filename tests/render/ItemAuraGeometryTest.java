package top.csituka.magicaland.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class ItemAuraGeometryTest {
    private static int checks;
    private static final Identifier ATLAS = new Identifier("minecraft", "textures/atlas/blocks.png");

    public static void main(String[] args) {
        for (boolean left : new boolean[] {false, true}) for (float scale : new float[] {1, 64}) {
            MatrixStack pose = new MatrixStack();
            pose.translate(2, 3, 4);
            pose.scale(left ? -scale : scale, scale, scale);
            pose.multiply(new org.joml.Quaternionf().rotateXYZ(0.2f, 0.6f, -0.1f));
            pose.peek().getNormalMatrix().set(new org.joml.Matrix3f(pose.peek().getPositionMatrix()).invert().transpose());
            var capture = new ItemAuraGeometry.Capture(pose.peek());
            Sink original = new Sink();
            VertexConsumer recorder = capture.wrap(original, ATLAS);
            sprite(recorder, pose.peek(), 0.01f, 1);
            sprite(recorder, pose.peek(), -0.01f, -1);
            var mesh = capture.finish();
            check(original.values.size() == 8, "original mesh forwarded exactly once");
            check(mesh.batches.size() == 1 && mesh.batches.get(0).vertices().size() == 8,
                    "flat item captured left=" + left + " scale=" + scale + " batches=" + mesh.batches);
            check(near(mesh.min.x, -0.5f) && near(mesh.max.x, 0.5f), "root/mode transform stripped from bounds");
            check(near(mesh.max.z - mesh.min.z, 0.02f), "flat item keeps real thickness, no sphere");
            Sink output = new Sink();
            mesh.render(mesh.batches.get(0), output, 0x6611CC, 1200);
            check(output.values.size() == 8 * ItemAuraGeometry.OPACITY.length, "bounded soft contour layers");
            Matrix4f inverse = new Matrix4f(pose.peek().getPositionMatrix()).invert();
            for (int i = 0; i < output.values.size(); i++) {
                Sample sample = output.values.get(i);
                Vector3f local = inverse.transformPosition(new Vector3f(sample.x, sample.y, sample.z));
                check(Math.abs(local.x) < 0.63f && Math.abs(local.y) < 0.63f && Math.abs(local.z) < 0.06f,
                        "thicker sprite shell remains bounded at all preview scales/handedness");
                check(sample.effect == 2 && sample.clock == 1200, "per-vertex item flow metadata");
                check(sample.height == (i % 4 < 2 ? 0 : 32767), "flow spans complete local height");
                Sample source = original.values.get(i % 8);
                check(sample.u == source.u && sample.v == source.v, "atlas UV never scrolls or enters adjacent sprite");
                int layer = i / 8;
                check(near(sample.alpha, (int) (ItemAuraGeometry.OPACITY[layer] * 255) / 255f), "soft layers fade outward");
            }
            pose.translate(1000, 1000, 1000);
            Sink copied = new Sink();
            mesh.render(mesh.batches.get(0), copied, 0x6611CC, 1200);
            check(near(copied.values.get(0).x, output.values.get(0).x), "deferred root pose is immutable snapshot");
        }

        MatrixStack pose = new MatrixStack();
        var capture = new ItemAuraGeometry.Capture(pose.peek());
        Sink original = new Sink();
        VertexConsumer first = capture.wrap(original, ATLAS);
        sprite(first, pose.peek(), 0, 1);
        VertexConsumer second = capture.wrap(original, new Identifier("minecraft", "textures/entity/shield_base.png"));
        sprite(second, pose.peek(), 0.03f, -1);
        check(capture.finish().batches.size() == 2, "special item textures remain separate");

        for (int alpha : new int[] {0, 64, 128, 255}) {
            var faded = new ItemAuraGeometry.Capture(pose.peek());
            Sink forwarded = new Sink();
            sprite(faded.wrap(forwarded, ATLAS), pose.peek(), 0, 1, alpha);
            var mesh = faded.finish();
            Sink output = new Sink();
            mesh.render(mesh.batches.get(0), output, 0x6611CC, 1200);
            for (Sample originalSample : forwarded.values)
                check(near(originalSample.alpha, alpha / 255f), "original item alpha is not amplified");
            for (int layer = 0; layer < ItemAuraGeometry.OPACITY.length; layer++) for (int vertex = 0; vertex < 4; vertex++) {
                Sample sample = output.values.get(layer * 4 + vertex);
                float expected = (int) (ItemAuraGeometry.OPACITY[layer] * (alpha / 255f) * 255) / 255f;
                check(near(sample.alpha, expected), "thickness change preserves source alpha and layer opacity");
                check(sample.u == forwarded.values.get(vertex).u && sample.v == forwarded.values.get(vertex).v,
                        "transparent sprite UV coverage unchanged");
            }
        }

        var limited = new ItemAuraGeometry.Capture(pose.peek());
        Sink sink = new Sink();
        VertexConsumer recorder = limited.wrap(sink, ATLAS);
        for (int i = 0; i < ItemAuraGeometry.MAX_VERTICES / 4 + 4; i++) sprite(recorder, pose.peek(), 0, 1);
        check(limited.finish().batches.get(0).vertices().size() == ItemAuraGeometry.MAX_VERTICES, "geometry capture bound");
        check(sink.values.size() == ItemAuraGeometry.MAX_VERTICES + 16, "capture limit never truncates original item");
        check(ItemAuraGeometry.height(0, 0, 0) == 16384, "zero-height face safe");
        check(ItemAuraGeometry.height(10, 0, 1) == 32767, "packed height upper bound");
        check(ItemAuraGeometry.height(-10, 0, 1) == 0, "packed height lower bound");
        System.out.println("PASS item aura geometry: " + checks + " checks");
    }

    private static void sprite(VertexConsumer consumer, MatrixStack.Entry root, float z, float normalZ) {
        sprite(consumer, root, z, normalZ, 255);
    }

    private static void sprite(VertexConsumer consumer, MatrixStack.Entry root, float z, float normalZ, int alpha) {
        for (int i = 0; i < 4; i++) {
            float x = i == 0 || i == 3 ? -0.5f : 0.5f, y = i < 2 ? -0.5f : 0.5f;
            Vector3f position = root.getPositionMatrix().transformPosition(new Vector3f(x, y, z));
            Vector3f normal = root.getNormalMatrix().transform(new Vector3f(0, 0, normalZ));
            consumer.vertex(position.x, position.y, position.z).color(255, 255, 255, alpha)
                    .texture(0.3f + (x + 0.5f) * 0.05f, 0.6f + (y + 0.5f) * 0.1f)
                    .overlay(0, 0).light(240, 240).normal(normal.x, normal.y, normal.z).next();
        }
    }

    private static final class Sample {
        float x, y, z, u, v, alpha;
        int clock, effect, height;
    }

    private static final class Sink implements VertexConsumer {
        final List<Sample> values = new ArrayList<>();
        private Sample current = new Sample();
        public VertexConsumer vertex(double x, double y, double z) { current.x = (float)x; current.y = (float)y; current.z = (float)z; return this; }
        public VertexConsumer color(int r, int g, int b, int a) { current.alpha = a / 255f; return this; }
        public VertexConsumer texture(float u, float v) { current.u = u; current.v = v; return this; }
        public VertexConsumer overlay(int u, int v) { current.clock = u; current.effect = v; return this; }
        public VertexConsumer light(int u, int v) { current.height = v; return this; }
        public VertexConsumer normal(float x, float y, float z) { return this; }
        public void next() { values.add(current); current = new Sample(); }
        public void fixedColor(int r, int g, int b, int a) { current.alpha = a / 255f; }
        public void unfixColor() {}
    }

    private static boolean near(float a, float b) { return Math.abs(a - b) < 0.0001f; }
    private static void check(boolean valid, String message) { checks++; if (!valid) throw new AssertionError(message); }
}
