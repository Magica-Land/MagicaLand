package top.csituka.magicaland.client.render;

public final class TransformationBurstMathTest {
    private static int checks;

    public static void main(String[] args) {
        for (int seed = 0; seed < 100; seed++) {
            var specks = TransformationBurstMath.sample(seed, "#FFFFDDCC", "#AA00FF");
            require(specks.equals(TransformationBurstMath.sample(seed, "#FFFFDDCC", "#AA00FF")), "deterministic");
            require(specks.size() == 32, "bounded count");
            require(specks.stream().filter(s -> s.star()).count() == 4, "four small stars");
            for (var s : specks) {
                require(Math.abs(s.x()) <= .48 && Math.abs(s.z()) <= .65 && s.y() >= .12 && s.y() <= 1.42, "body silhouette");
                double radiusSquared = s.x() * s.x() / (.48 * .48) + s.z() * s.z() / (.65 * .65);
                require(radiusSquared >= .58 - 1e-9 && radiusSquared <= 1 + 1e-9, "avoid hidden body center");
                require(s.x() * s.vx() + s.z() * s.vz() > 0, "outward velocity");
                require(s.size() >= .019f && s.size() <= .049f, "readable small size");
                require(Math.abs(s.vx()) < .043 && Math.abs(s.vz()) < .043 && Math.abs(s.vy()) < .023, "no explosive speed");
                for (int shift : new int[] {0, 8, 16}) {
                    int channel = (s.rgb() >> shift) & 255;
                    require(channel >= 64 && channel <= 250, "luminous without clipped white");
                }
            }
        }
        require(TransformationBurstMath.sample(1, null, "bad").equals(
                TransformationBurstMath.sample(1, "#FFFFFF", "#AA00FF")), "bad colors fall back");
        require(TransformationBurstMath.sample(1, "#99FFAABB", "#AA00FF").equals(
                TransformationBurstMath.sample(1, "#FFAABB", "#AA00FF")), "alpha ignored consistently");
        require(TransformationBurstMath.opacity(0) == 0 && TransformationBurstMath.opacity(16) == 0
                && TransformationBurstMath.opacity(100) == 0, "soft start and 0.8-second end");
        float maximum = 0;
        double previousTravel = -1;
        for (int sample = 0; sample <= 160; sample++) {
            float age = sample / 10f;
            float alpha = TransformationBurstMath.opacity(age);
            require(Float.isFinite(alpha) && alpha >= 0 && alpha <= .88f, "safe opacity");
            maximum = Math.max(maximum, alpha);
            double travel = TransformationBurstMath.displacement(age);
            require(travel >= previousTravel && travel <= 10, "bounded continuous travel");
            previousTravel = travel;
        }
        require(maximum > .85f, "readable peak without opaque flash");
        require(TransformationBurstMath.opacity(8) > .5f, "visible at half lifetime");
        var base = TransformationBurstMath.sample(1, "#442266", "#AA00FF");
        for (int yaw = -720; yaw <= 720; yaw += 15) {
            var rotated = TransformationBurstMath.sample(1, "#442266", "#AA00FF", yaw);
            double angle = Math.toRadians(yaw), cos = Math.cos(angle), sin = Math.sin(angle);
            for (int i = 0; i < base.size(); i++) {
                var a = base.get(i); var b = rotated.get(i);
                require(Math.abs(b.x() - (a.x() * cos - a.z() * sin)) < 1e-9, "facing x");
                require(Math.abs(b.z() - (a.x() * sin + a.z() * cos)) < 1e-9, "facing z");
                require(Math.abs(b.x()*b.x() + b.z()*b.z() - a.x()*a.x() - a.z()*a.z()) < 1e-9, "rotation preserves extent");
                require(b.x() * b.vx() + b.z() * b.vz() > 0, "rotated outward velocity");
                require(a.size() == b.size() && a.rgb() == b.rgb() && a.y() == b.y(), "facing does not change style");
            }
        }
        require(base.equals(TransformationBurstMath.sample(1, "#442266", "#AA00FF", Float.NaN)), "invalid facing fallback");
        System.out.println("PASS TransformationBurstMathTest: " + checks + " sample/color/lifetime checks");
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
