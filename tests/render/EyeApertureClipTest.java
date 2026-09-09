package top.csituka.magicaland.client.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import static top.csituka.magicaland.client.render.EyeApertureClip.*;

public final class EyeApertureClipTest {
    private static int checks;

    public static void main(String[] args) {
        rectangle();
        diagonalAndUv();
        overlappingUnion();
        sideOwnership();
        actualEye03();
        motionAndReverse();
        randomUnionAndSides();
        finiteAndDuplicateGuards();
        System.out.println("PASS EyeApertureClipTest: " + checks + " clipping, union, side ownership, winding and UV checks");
    }

    private static void rectangle() {
        var aperture = prepare(List.of(rect(-1, -1, 1, 1)));
        List<Vertex> original = plane(-.5, -.5, .5, .5, .2, 0, 0);
        var inside = clip(aperture, original);
        check(inside.size() == 1 && inside.get(0).equals(original), "fully inside face uses unchanged vertices");
        for (int i = 0; i < 4; i++) check(inside.get(0).get(i) == original.get(i), "inside fast path keeps original vertex records");
        check(clip(aperture, plane(2, 0, 3, 1, 0, 0, 0)).isEmpty(), "fully outside face rejected");
        var whole = clip(aperture, plane(-2, -2, 2, 2, -.2, 0, 0));
        near(areaXY(whole), 4, 1e-9, "surrounding quad clipped to exact aperture");
        near(area3(whole), 4, 1e-9, "back plane preserves area");
        check(whole.size() == 1 && whole.get(0).size() == 4, "rectangle stays a quad");
        verify(whole, 1, -.2, 0, 0);
        List<Vertex> reversed = reverse(plane(-2, -2, 2, 2, .3, 0, 0));
        var back = clip(aperture, reversed);
        near(areaXY(back), -4, 1e-9, "back face winding remains reversed");
        verify(back, -1, .3, 0, 0);
        near(areaXY(clip(aperture, plane(.25, -2, 2, 2, 0, 0, 0))), 1.5, 1e-9, "one side clipped exactly");
        check(clip(aperture, plane(1, -2, 2, 2, 0, 0, 0)).isEmpty(), "touching with zero-area edge creates no polygon");
    }

    private static void diagonalAndUv() {
        var diamond = List.of(new Point(0, -1), new Point(1, 0), new Point(0, 1), new Point(-1, 0));
        var aperture = prepare(List.of(diamond));
        var output = clip(aperture, plane(-2, -2, 2, 2, .3, .7, -.2));
        near(areaXY(output), 2, 1e-9, "diamond clipping retains true shape rather than bounds");
        near(area3(output), 2 * Math.sqrt(1 + .7 * .7 + .2 * .2), 1e-9, "inclined plane has exact 3D area");
        verify(output, 1, .3, .7, -.2);
        var many = clip(aperture, plane(-.8, -.8, .8, .8, -.4, .4, .6));
        check(many.size() == 1 && many.get(0).size() == 8, "diagonal cuts can produce an eight-vertex convex polygon");
        near(areaXY(many), 1.84, 1e-9, "all four corners are clipped");
        verify(many, 1, -.4, .4, .6);
        var clockwise = prepare(List.of(reversePoints(diamond)));
        near(areaXY(clip(clockwise, plane(-2, -2, 2, 2, .3, .7, -.2))), 2, 1e-9,
                "input aperture winding is normalized independently of face winding");
    }

    private static void overlappingUnion() {
        List<List<Point>> overlap = List.of(rect(0, 0, 2, 2), rect(1, 1, 3, 3));
        var aperture = prepare(overlap);
        var output = clip(aperture, plane(-1, -1, 4, 4, 0, 0, 0));
        near(areaXY(output), 7, 1e-9, "overlapping rectangles union area is not double counted");
        check(aperture.regionCount() > 1, "nonconvex union is cached as multiple convex cells");
        coverage(output, overlap, -1, -1, 4, 4, 80, 1001);
        var same = prepare(List.of(rect(0, 0, 2, 2), rect(0, 0, 2, 2), rect(.2, .2, 1.8, 1.8)));
        check(same.regionCount() == 1, "duplicate and contained windows add no new regions");
        near(areaXY(clip(same, plane(-1, -1, 4, 4, 0, 0, 0))), 4, 1e-9, "duplicate windows never double draw");
        List<List<Point>> frame = List.of(rect(-2, -2, 2, -1), rect(-2, 1, 2, 2), rect(-2, -1, -1, 1), rect(1, -1, 2, 1));
        var framed = clip(prepare(frame), plane(-3, -3, 3, 3, 0, 0, 0));
        near(areaXY(framed), 12, 1e-9, "union retains central hole");
        check(count(framed, 0, 0, 1e-10) == 0, "union never replaces disconnected boundary with convex hull");
        coverage(framed, frame, -3, -3, 3, 3, 90, 8121);
        List<List<Point>> disjoint = List.of(rect(-2, -1, -.5, 1), rect(.5, -1, 2, 1));
        var separated = clip(prepare(disjoint), plane(-3, -2, 3, 2, 0, 0, 0));
        near(areaXY(separated), 6, 1e-9, "disconnected windows remain separate");
        check(count(separated, 0, 0, 1e-10) == 0, "gap between regions remains open");
    }

    private static void sideOwnership() {
        var touching = prepare(List.of(rect(-1, -1, 0, 1), rect(0, -1, 1, 1)));
        var side = side(0, -2, 0, 2, -.25, .25);
        var output = clip(touching, side);
        check(output.size() == 1, "side exactly on shared edge is emitted only once");
        near(area3(output), 1, 1e-9, "shared side area is not doubled");
        verifySide(output, side);
        var reverse = clip(touching, reverse(side));
        near(area3(reverse), 1, 1e-9, "opposite side winding remains valid");
        verifySide(reverse, reverse(side));

        var partial = prepare(List.of(rect(-1, -1, 0, .25), rect(0, -.25, 1, 1)));
        var partialOutput = clip(partial, side);
        check(partialOutput.size() == 1, "partly shared boundary intervals merge before emitting side");
        near(area3(partialOutput), 1, 1e-9, "partial shared boundary has exact union length");
        var gap = prepare(List.of(rect(-1, -1, 0, -.25), rect(0, .25, 1, 1)));
        var gapOutput = clip(gap, side);
        check(gapOutput.size() == 2, "separate side windows retain their gap");
        near(area3(gapOutput), .75, 1e-9, "side gap has exact clipped area");
        var inner = prepare(List.of(rect(-1, -1, 1, 1), rect(-.5, -.5, .5, .5)));
        near(area3(clip(inner, side)), 1, 1e-9, "overlapping aperture cannot duplicate an interior side");

        var diagonal = prepare(List.of(List.of(new Point(-1, -1), new Point(1, 1), new Point(-1, 1)),
                List.of(new Point(-1, -1), new Point(1, -1), new Point(1, 1))));
        var diagonalFace = side(-2, -2, 2, 2, -.25, .25);
        var diagonalOutput = clip(diagonal, diagonalFace);
        check(diagonalOutput.size() == 1, "slanted shared boundary has one owner");
        near(area3(diagonalOutput), Math.sqrt(2), 1e-9, "slanted side union preserves physical area");
        verifySide(diagonalOutput, diagonalFace);
        check(clip(touching, side(2, -2, 2, 2, -.25, .25)).isEmpty(), "parallel side outside extrusion is removed");
    }

    private static void actualEye03() {
        List<List<Point>> left = List.of(rect(-2.8 / 16, 25.6 / 16, -.6 / 16, 27.2 / 16),
                rect(-3.6 / 16, 23.5 / 16, -.6 / 16, 25.6 / 16),
                List.of(p(-1.895818, 24.601887), p(-3.743577, 25.367254), p(-3.016478, 27.122625), p(-1.168719, 26.357258)));
        var aperture = prepare(left);
        var output = clip(aperture, plane(-.3, 1.4, 0, 1.8, -.06, .02, -.03));
        coverage(output, left, -.3, 1.4, 0, 1.8, 170, 303);
        verify(output, 1, -.06, .02, -.03);
        double individualArea = 0;
        for (List<Point> region : left) individualArea += Math.abs(area(region));
        check(areaXY(output) < individualArea - .001, "actual 03 overlap is removed rather than alpha overdrawn");
        check(aperture.regionCount() < 30, "three eye03 white pieces remain a small cached union");
        List<List<Point>> opposite = new ArrayList<>();
        for (List<Point> region : left) opposite.add(region.stream().map(point -> new Point(-point.x(), point.y())).toList());
        var mirrored = clip(prepare(opposite), plane(0, 1.4, .3, 1.8, -.06, .02, -.03));
        near(areaXY(mirrored), areaXY(output), 1e-10, "mirrored eye retains exact union area");
    }

    private static void motionAndReverse() {
        var aperture = prepare(List.of(rect(-.2, -.2, .2, .2),
                List.of(new Point(-.25, 0), new Point(0, -.25), new Point(.25, 0), new Point(0, .25))));
        Random random = new Random(493701);
        for (int trial = 0; trial < 500; trial++) {
            double x = random.nextDouble() * .8 - .4, y = random.nextDouble() * .8 - .4;
            double z = random.nextDouble() * .2 - .1, sx = random.nextDouble() - .5, sy = random.nextDouble() - .5;
            List<Vertex> quad = plane(x - .12, y - .15, x + .12, y + .15, z, sx, sy);
            var forward = clip(aperture, quad);
            var backward = clip(aperture, reverse(quad));
            near(areaXY(forward), -areaXY(backward), 1e-9, "arbitrary translated face and reverse clip identically");
            verify(forward, 1, z, sx, sy); verify(backward, -1, z, sx, sy);
            near(area3(forward), area3(backward), 1e-9, "3D area independent of vertex order");
            check(quad.equals(plane(x - .12, y - .15, x + .12, y + .15, z, sx, sy)), "input face is never mutated");
        }
    }

    private static void finiteAndDuplicateGuards() {
        var repeated = prepare(List.of(List.of(new Point(-1, -1), new Point(0, -1), new Point(1, -1),
                new Point(1, -1), new Point(1, 1), new Point(-1, 1), new Point(-1, -1))));
        near(areaXY(clip(repeated, plane(-2, -2, 2, 2, 0, 0, 0))), 4, 1e-9, "duplicate and redundant edges normalize safely");
        var empty = prepare(List.of());
        check(empty.isEmpty() && empty.regionCount() == 0, "empty aperture has explicit empty state");
        check(clip(empty, plane(-1, -1, 1, 1, 0, 0, 0)).isEmpty(), "empty aperture draws nothing");
        List<Vertex> face = plane(-.5, -.5, .5, .5, 0, 0, 0);
        Vertex a = face.get(0), b = face.get(1), c = face.get(2), d = face.get(3);
        check(repeated.clipQuad(a, b, c, c).size() == 1, "duplicate final face vertex remains a valid triangle");
        check(repeated.clipQuad(a, a, a, a).isEmpty(), "zero-area source is rejected");
        check(repeated.clipQuad(a, b, a, b).isEmpty(), "repeated source edge creates no surface");
        check(repeated.clipQuad(null, b, c, d).isEmpty(), "null vertex rejected");
        for (double invalid : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            for (Vertex bad : List.of(new Vertex(invalid, 0, 0, 0, 0), new Vertex(0, invalid, 0, 0, 0),
                    new Vertex(0, 0, invalid, 0, 0), new Vertex(0, 0, 0, invalid, 0), new Vertex(0, 0, 0, 0, invalid)))
                check(repeated.clipQuad(bad, b, c, d).isEmpty(), "nonfinite geometry or UV is never emitted");
            rejected(() -> prepare(List.of(List.of(new Point(invalid, 0), new Point(1, 0), new Point(0, 1)))),
                    "nonfinite aperture rejected at preparation");
        }
        rejected(() -> prepare(null), "null regions rejected");
        rejected(() -> prepare(List.of(List.of(new Point(0, 0), new Point(1, 0), new Point(2, 0)))), "collinear window rejected");
        rejected(() -> prepare(List.of(List.of(new Point(0, 0), new Point(2, 0), new Point(.5, .5), new Point(0, 2)))),
                "concave region must be supplied as convex pieces");
        rejected(() -> prepare(List.of(rect(-Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE))),
                "finite coordinates overflowing geometry arithmetic rejected");
        var output = repeated.clipQuad(a, b, c, d);
        boolean immutable = false;
        try { output.get(0).clear(); } catch (UnsupportedOperationException expected) { immutable = true; }
        check(immutable, "results cannot mutate cached aperture or caller geometry");
    }

    private static void randomUnionAndSides() {
        Random random = new Random(630927);
        for (int trial = 0; trial < 120; trial++) {
            List<List<Point>> regions = new ArrayList<>();
            for (int index = 0, total = 2 + random.nextInt(5); index < total; index++) {
                double x = random.nextDouble() * 1.4 - .7, y = random.nextDouble() * 1.4 - .7;
                double width = .12 + random.nextDouble() * .65, height = .12 + random.nextDouble() * .65;
                double angle = random.nextDouble() * Math.PI, cosine = Math.cos(angle), sine = Math.sin(angle);
                List<Point> polygon = new ArrayList<>();
                for (Point corner : rect(-width / 2, -height / 2, width / 2, height / 2))
                    polygon.add(new Point(x + corner.x() * cosine - corner.y() * sine,
                            y + corner.x() * sine + corner.y() * cosine));
                regions.add(index % 2 == 0 ? polygon : reversePoints(polygon));
            }
            var aperture = prepare(regions);
            var forward = clip(aperture, plane(-1.5, -1.5, 1.5, 1.5, .07, -.2, .4));
            verify(forward, 1, .07, -.2, .4);
            coverage(forward, regions, -1.5, -1.5, 1.5, 1.5, 35, trial * 79L + 5);
            List<List<Point>> reversedInput = new ArrayList<>(regions);
            Collections.reverse(reversedInput);
            near(areaXY(clip(prepare(reversedInput), plane(-1.5, -1.5, 1.5, 1.5, 0, 0, 0))), areaXY(forward), 1e-8,
                    "rotated union area does not depend on insertion order");
            for (int sideIndex = 0; sideIndex < 6; sideIndex++) {
                double angle = random.nextDouble() * Math.PI;
                double offset = random.nextDouble() * .8 - .4;
                double dx = Math.cos(angle), dy = Math.sin(angle);
                Point start = new Point(-dx * 2 - dy * offset, -dy * 2 + dx * offset);
                Point end = new Point(dx * 2 - dy * offset, dy * 2 + dx * offset);
                List<Vertex> face = side(start.x(), start.y(), end.x(), end.y(), -.17, .23);
                var clippedSide = clip(aperture, face);
                double expectedLength = coveredLineLength(regions, start, end);
                near(area3(clippedSide), expectedLength * .4, 1e-8,
                        "arbitrary side has exact union area from independent line-boundary intersections");
                verifySide(clippedSide, face);
                near(area3(clip(aperture, reverse(face))), area3(clippedSide), 1e-8,
                        "side union remains exact with reversed winding");
            }
        }
    }

    private static double coveredLineLength(List<List<Point>> regions, Point start, Point end) {
        double dx = end.x() - start.x(), dy = end.y() - start.y();
        List<Double> cuts = new ArrayList<>(List.of(0., 1.));
        for (List<Point> polygon : regions) for (int i = 0; i < polygon.size(); i++) {
            Point a = polygon.get(i), b = polygon.get((i + 1) % polygon.size());
            double ex = b.x() - a.x(), ey = b.y() - a.y(), denominator = dx * ey - dy * ex;
            if (Math.abs(denominator) < 1e-12) continue;
            double ax = a.x() - start.x(), ay = a.y() - start.y();
            double along = (ax * ey - ay * ex) / denominator;
            double edge = (ax * dy - ay * dx) / denominator;
            if (along >= 0 && along <= 1 && edge >= 0 && edge <= 1) cuts.add(along);
        }
        cuts.sort(Double::compare);
        double length = 0;
        for (int i = 1; i < cuts.size(); i++) {
            double middle = (cuts.get(i - 1) + cuts.get(i)) / 2;
            double x = start.x() + middle * dx, y = start.y() + middle * dy;
            for (List<Point> polygon : regions) if (contains(polygon, x, y)) {
                length += cuts.get(i) - cuts.get(i - 1); break;
            }
        }
        return length * Math.hypot(dx, dy);
    }

    private static List<List<Vertex>> clip(Aperture aperture, List<Vertex> face) {
        return aperture.clipQuad(face.get(0), face.get(1), face.get(2), face.get(3));
    }
    private static List<Point> rect(double x0, double y0, double x1, double y1) {
        return List.of(new Point(x0, y0), new Point(x1, y0), new Point(x1, y1), new Point(x0, y1));
    }
    private static Point p(double x, double y) { return new Point(x / 16, y / 16); }
    private static Vertex vertex(double x, double y, double z) { return new Vertex(x, y, z, .2 + .7 * x - .3 * y + .1 * z, .8 - .2 * x + .5 * y - .4 * z); }
    private static List<Vertex> plane(double x0, double y0, double x1, double y1, double z, double sx, double sy) {
        return List.of(vertex(x0, y0, z + sx * x0 + sy * y0), vertex(x1, y0, z + sx * x1 + sy * y0),
                vertex(x1, y1, z + sx * x1 + sy * y1), vertex(x0, y1, z + sx * x0 + sy * y1));
    }
    private static List<Vertex> side(double x0, double y0, double x1, double y1, double z0, double z1) {
        return List.of(vertex(x0, y0, z0), vertex(x1, y1, z0), vertex(x1, y1, z1), vertex(x0, y0, z1));
    }
    private static List<Vertex> reverse(List<Vertex> values) { var copy = new ArrayList<>(values); Collections.reverse(copy); return copy; }
    private static List<Point> reversePoints(List<Point> values) { var copy = new ArrayList<>(values); Collections.reverse(copy); return copy; }

    private static void verify(List<List<Vertex>> polygons, int sign, double z, double sx, double sy) {
        for (var polygon : polygons) {
            check(polygon.size() >= 3, "output has a drawable polygon");
            check(areaXY(List.of(polygon)) * sign > 0, "output keeps input winding");
            for (int i = 0; i < polygon.size(); i++) {
                Vertex vertex = polygon.get(i), next = polygon.get((i + 1) % polygon.size());
                check(distance(vertex, next) > 1e-10, "no adjacent duplicate boundary vertices");
                near(vertex.z(), z + sx * vertex.x() + sy * vertex.y(), 1e-8, "clipped vertex remains on original 3D plane");
                uv(vertex);
            }
        }
    }
    private static void verifySide(List<List<Vertex>> polygons, List<Vertex> original) {
        double[] normal = normal(original);
        for (var polygon : polygons) {
            double[] clippedNormal = normal(polygon);
            check(normal[0] * clippedNormal[0] + normal[1] * clippedNormal[1] + normal[2] * clippedNormal[2] > 0,
                    "side clipping preserves 3D winding even with zero XY area");
            for (Vertex vertex : polygon) uv(vertex);
        }
    }
    private static void uv(Vertex vertex) {
        near(vertex.u(), .2 + .7 * vertex.x() - .3 * vertex.y() + .1 * vertex.z(), 1e-8, "U interpolates with original geometry");
        near(vertex.v(), .8 - .2 * vertex.x() + .5 * vertex.y() - .4 * vertex.z(), 1e-8, "V interpolates with original geometry");
    }
    private static double distance(Vertex a, Vertex b) { return Math.hypot(Math.hypot(a.x() - b.x(), a.y() - b.y()), a.z() - b.z()); }
    private static double[] normal(List<Vertex> polygon) {
        double x = 0, y = 0, z = 0;
        Vertex origin = polygon.get(0);
        for (int i = 1; i + 1 < polygon.size(); i++) {
            Vertex a = polygon.get(i), b = polygon.get(i + 1);
            double ax = a.x() - origin.x(), ay = a.y() - origin.y(), az = a.z() - origin.z();
            double bx = b.x() - origin.x(), by = b.y() - origin.y(), bz = b.z() - origin.z();
            x += ay * bz - az * by; y += az * bx - ax * bz; z += ax * by - ay * bx;
        }
        return new double[] {x, y, z};
    }
    private static double areaXY(List<List<Vertex>> polygons) {
        double area = 0;
        for (var polygon : polygons) area += normal(polygon)[2] / 2;
        return area;
    }
    private static double area3(List<List<Vertex>> polygons) {
        double area = 0;
        for (var polygon : polygons) { var normal = normal(polygon); area += Math.hypot(Math.hypot(normal[0], normal[1]), normal[2]) / 2; }
        return area;
    }
    private static double area(List<Point> polygon) {
        double result = 0;
        for (int i = 0; i < polygon.size(); i++) {
            Point a = polygon.get(i), b = polygon.get((i + 1) % polygon.size());
            result += a.x() * b.y() - a.y() * b.x();
        }
        return result / 2;
    }
    private static int count(List<List<Vertex>> polygons, double x, double y, double epsilon) {
        int count = 0;
        for (var polygon : polygons) {
            boolean contains = true;
            double sign = Math.signum(normal(polygon)[2]);
            for (int i = 0; i < polygon.size(); i++) {
                Vertex a = polygon.get(i), b = polygon.get((i + 1) % polygon.size());
                if (((b.x() - a.x()) * (y - a.y()) - (b.y() - a.y()) * (x - a.x())) * sign < -epsilon) contains = false;
            }
            if (contains) count++;
        }
        return count;
    }
    private static boolean contains(List<Point> polygon, double x, double y) {
        double sign = Math.signum(area(polygon));
        for (int i = 0; i < polygon.size(); i++) {
            Point a = polygon.get(i), b = polygon.get((i + 1) % polygon.size());
            if (((b.x() - a.x()) * (y - a.y()) - (b.y() - a.y()) * (x - a.x())) * sign < 0) return false;
        }
        return true;
    }
    private static void coverage(List<List<Vertex>> polygons, List<List<Point>> union, double x0, double y0, double x1, double y1, int resolution, long seed) {
        var random = new Random(seed);
        for (int y = 0; y < resolution; y++) for (int x = 0; x < resolution; x++) {
            double px = x0 + (x + .2 + random.nextDouble() * .6) / resolution * (x1 - x0);
            double py = y0 + (y + .2 + random.nextDouble() * .6) / resolution * (y1 - y0);
            boolean wanted = false;
            for (var region : union) wanted |= contains(region, px, py);
            int copies = count(polygons, px, py, 1e-12);
            check(copies == (wanted ? 1 : 0), "union exactly covers intended shape once, without filling gaps");
        }
    }
    private static void rejected(Runnable operation, String message) {
        boolean rejected = false;
        try { operation.run(); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, message);
    }
    private static void near(double actual, double expected, double epsilon, String message) {
        check(Double.isFinite(actual) && Math.abs(actual - expected) <= epsilon, message + ": " + actual + " != " + expected);
    }
    private static void check(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }
}
