package top.csituka.magicaland.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class LevitationTrail {
    public static final LevitationTrail EMPTY = new LevitationTrail(List.of(), 0);
    private final List<Node> nodes;
    private final float width;
    private final Function<Vector3f, LevitationTrail> capture;
    private record Node(Vector3f point, float alpha) {}

    private LevitationTrail(List<Node> nodes, float width) { this.nodes = nodes; this.width = width; this.capture = null; }

    private LevitationTrail(Function<Vector3f, LevitationTrail> capture) {
        this.nodes = List.of(); this.width = 0; this.capture = capture;
    }

    static LevitationTrail deferred(Function<Vector3f, LevitationTrail> capture) { return new LevitationTrail(capture); }
    LevitationTrail atCenter(Vector3f center) { return capture == null ? this : capture.apply(center); }

    static LevitationTrail create(List<LevitationMotion.TrailPoint> points, Matrix4f frame,
            LevitationMotion.Point origin, float width) {
        if (points.size() < 2) return EMPTY;
        List<Node> nodes = new ArrayList<>();
        for (var point : points) {
            var relative = point.position().subtract(origin);
            Vector3f transformed = frame.transformPosition(new Vector3f((float) relative.x(), (float) relative.y(), (float) relative.z()));
            if (!transformed.isFinite()) return EMPTY;
            nodes.add(new Node(transformed, point.alpha()));
        }
        return new LevitationTrail(List.copyOf(nodes), width);
    }

    public boolean isEmpty() { return nodes.size() < 2; }

    void render(VertexConsumer buffer, int color, int clock) {
        Matrix4f view = new Matrix4f(RenderSystem.getModelViewMatrix());
        Matrix4f inverse = new Matrix4f(view).invert();
        Vector3f normal = inverse.transformDirection(new Vector3f(0, 0, 1)).normalize();
        for (int i = 1; i < nodes.size(); i++) {
            Node a = nodes.get(i - 1), b = nodes.get(i);
            Vector3f direction = view.transformDirection(new Vector3f(b.point).sub(a.point));
            Vector3f across = new Vector3f(-direction.y, direction.x, 0);
            if (across.lengthSquared() < 1e-10f) continue;
            inverse.transformDirection(across.normalize().mul(width));
            float alphaA = i == 1 ? 0 : a.alpha;
            float widthA = i == 1 ? .18f : Math.max(.18f, a.alpha / .16f);
            float widthB = Math.max(.18f, b.alpha / .16f);
            vertex(buffer, a.point, across, -widthA, 0, 0, alphaA, color, clock, normal);
            vertex(buffer, a.point, across, widthA, 0, 1, alphaA, color, clock, normal);
            vertex(buffer, b.point, across, widthB, 1, 1, b.alpha, color, clock, normal);
            vertex(buffer, b.point, across, -widthB, 1, 0, b.alpha, color, clock, normal);
        }
    }

    private static void vertex(VertexConsumer buffer, Vector3f point, Vector3f across, float side,
            float u, float v, float alpha, int color, int clock, Vector3f normal) {
        buffer.vertex(point.x + across.x * side, point.y + across.y * side, point.z + across.z * side)
                .color((color >>> 16 & 255) / 255f, (color >>> 8 & 255) / 255f, (color & 255) / 255f, alpha)
                .texture(u, v).overlay(clock, 0).light(0xF000F0)
                .normal(normal.x, normal.y, normal.z).next();
    }
}
