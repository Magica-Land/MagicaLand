package top.csituka.magicaland.client.render;

import java.util.ArrayList;
import java.util.List;

public final class UnicornFlightRimTest {
    private static int checks;
    private record V(float x, float y, float r, float g, float b, float a) {}

    public static void main(String[] args) {
        for (int[] size : new int[][] {{80, 80}, {320, 180}, {1280, 720}, {720, 1280}, {3440, 1440}})
            for (int color : new int[] {0, 0xAA00FF, 0xFFFFFF, 0x00FF00}) for (double ticks : new double[] {0, 20, 23999.5, 24000, 2_147_480_000d}) {
                var vertices = mesh(size[0], size[1], 1, color, ticks);
                check(vertices.size() == 2048, "fixed lightweight quad budget");
                float shortest = Math.min(size[0], size[1]);
                for (var v : vertices) {
                    check(Float.isFinite(v.x) && Float.isFinite(v.y) && v.x >= 0 && v.y >= 0
                            && v.x <= size[0] && v.y <= size[1], "vertices stay in screen");
                    check(v.r >= .45f && v.g >= .45f && v.b >= .45f && v.r <= 1 && v.g <= 1 && v.b <= 1, "magic color is pale, never a black mask");
                    check(v.a >= 0 && v.a <= UnicornFlightRimMath.MAX_ALPHA + .000001, "bounded shallow alpha");
                    float depth = Math.min(Math.min(v.x, size[0] - v.x), Math.min(v.y, size[1] - v.y));
                    check(depth <= shortest * .08681, "outside rim occupies at most 8.7 percent of short side");
                    if (v.a == 0) check(depth >= shortest * .0731, "transparent inner boundary stays beyond 7.3 percent");
                }
                for (int i = 0; i < vertices.size(); i += 4) {
                    var quad = vertices.subList(i, i + 4);
                    float minX = Float.POSITIVE_INFINITY, minY = minX, maxX = 0, maxY = 0;
                    double area = 0;
                    for (int j = 0; j < 4; j++) {
                        var a = quad.get(j); var b = quad.get((j + 1) % 4);
                        minX = Math.min(minX, a.x); minY = Math.min(minY, a.y);
                        maxX = Math.max(maxX, a.x); maxY = Math.max(maxY, a.y);
                        area += (double) a.x * b.y - (double) b.x * a.y;
                    }
                    check(area < 0, "GUI quad winding matches standard overlay");
                    check(maxX <= shortest * .087 || maxY <= shortest * .087
                            || minX >= size[0] - shortest * .087 || minY >= size[1] - shortest * .087,
                            "no geometry covers the large central rectangle");
                }
                for (int band = 0; band < 8; band++) {
                    int first = band * 256, last = first + 63 * 4;
                    equal(vertices.get(first), vertices.get(last + 3), "outer perimeter seam closed");
                    equal(vertices.get(first + 1), vertices.get(last + 2), "inner perimeter seam closed");
                }
            }
        check(mesh(320, 180, 0, 0, 0).isEmpty(), "no flight means no geometry");
        check(mesh(320, 180, Float.NaN, 0, 0).isEmpty(), "invalid amount disabled");
        check(mesh(320, 180, 1, 0, Double.NaN).isEmpty(), "invalid time disabled");
        check(mesh(0, 180, 1, 0, 0).isEmpty(), "zero-size window skipped");
        for (int fps : new int[] {30, 60, 144}) {
            var fade = new UnicornFlightRimMath.Entrance();
            for (int frame = 0; frame <= fps; frame++) {
                double ticks = frame * 20d / fps;
                float t = (float) Math.min(1, ticks / 4), expected = t * t * (3 - 2 * t);
                near(expected, fade.sample(ticks, 1), "time-based entry independent of frame rate");
                near(expected, fade.sample(ticks, 1), "paused/repeated render does not advance entry");
            }
            near(.4, fade.sample(20.5, .4f), "existing flight exit amount passes through");
            near(0, fade.sample(21, 0), "completed exit has no residual light");
            near(0, fade.sample(22, 1), "new flight fades from zero");
            fade.reset(); near(0, fade.sample(22.5, 1), "menu/camera reset fades on return");
            near(0, fade.sample(30, 1), "missed rendering gap restarts gently");
            near(0, fade.sample(1, 1), "world/time rollback restarts gently");
        }
        var before = mesh(1280, 720, 1, 0xAA00FF, 1000);
        var after = mesh(1280, 720, 1, 0xAA00FF, 1000 + 20d / 30);
        for (int i = 0; i < before.size(); i++) {
            check(Math.abs(before.get(i).a - after.get(i).a) < .002, "flow cannot strongly flash between frames");
            check(Math.hypot(before.get(i).x - after.get(i).x, before.get(i).y - after.get(i).y) < .3, "flow barely moves rim width");
        }
        System.out.println("PASS unicorn flight rim math/geometry: " + checks);
    }
    private static List<V> mesh(int width, int height, float amount, int color, double ticks) {
        var vertices = new ArrayList<V>();
        UnicornFlightRimMath.emit(width, height, amount, color, ticks,
                (x, y, r, g, b, a) -> vertices.add(new V(x, y, r, g, b, a)));
        return vertices;
    }
    private static void equal(V a, V b, String name) {
        near(a.x, b.x, name); near(a.y, b.y, name); near(a.a, b.a, name);
    }
    private static void near(double expected, double actual, String name) {
        check(Double.isFinite(actual) && Math.abs(expected - actual) < .000002, name);
    }
    private static void check(boolean okay, String name) { checks++; if (!okay) throw new AssertionError(name); }
}
