package top.csituka.magicaland.client.render;

import java.nio.file.Files;
import java.nio.file.Path;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import software.bernie.geckolib.util.JsonUtil;

public final class HornGlowGeometryTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Model raw = JsonUtil.GEO_GSON.fromJson(Files.readString(Path.of(args[0])), Model.class);
        var baked = BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(GeometryTree.fromModel(raw));
        var horn = baked.getBone("Horn").orElseThrow().getCubes().get(0);
        var bounds = HornGlowGeometry.bounds(horn);
        var geometry = new HornGlowGeometry();
        var shells = geometry.shells(horn);
        check(shells.length == 3 && geometry.shells(horn) == shells, "three cached soft envelopes");
        float previous = 0;
        for (var shell : shells) {
            check(shell.pivot().equals(horn.pivot()) && shell.rotation().equals(horn.rotation()), "preserve horn pose");
            var expanded = HornGlowGeometry.bounds(shell);
            check(expanded[0].x < bounds[0].x && expanded[1].x > bounds[1].x
                    && expanded[0].y < bounds[0].y && expanded[1].y > bounds[1].y, "envelope extends outside horn");
            float width = expanded[1].x - expanded[0].x;
            check(width > previous, "inner-to-outer layers"); previous = width;
            check(shell.quads().length == 96, "bounded geometry complexity");
            for (var quad : shell.quads()) {
                check(quad.normal().isFinite() && Math.abs(quad.normal().length() - 1) < 1e-4, "finite unit normal");
                for (var vertex : quad.vertices()) check(vertex.position().isFinite() && vertex.texU() >= 0 && vertex.texU() <= 1
                        && vertex.texV() >= 0 && vertex.texV() <= 1, "local flow UV independent of texture atlas");
            }
        }
        check(HornGlowGeometry.bounds(horn)[0].equals(bounds[0]), "original geometry unchanged");
        float oldOuterWidth = (bounds[1].x - bounds[0].x) * 1.415f + 2 * 0.52f / 16;
        float newOuterWidth = HornGlowGeometry.bounds(shells[2])[1].x - HornGlowGeometry.bounds(shells[2])[0].x;
        check(newOuterWidth / oldOuterWidth > 1.4f && newOuterWidth / oldOuterWidth < 1.5f,
                "outer aura is about 40 percent wider, without changing horn");
        int[] bursts = new int[5];
        for (int seed : new int[] {0, 1, -97, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            int activeFrames = 0;
            for (int frame = 0; frame < 6000; frame++) {
                int active = 0;
                for (int slot = 0; slot < 4; slot++) {
                    var spark = MagicSparkles.sample(frame * 0.25, seed, slot);
                    if (spark == null) continue;
                    active++;
                    check(spark.radius() > 0 && spark.radius() < 0.03 && spark.alpha() > 0 && spark.alpha() <= 0.65,
                            "tiny short-lived spark");
                    check(spark.equals(MagicSparkles.sample(frame * 0.25, seed, slot)), "deterministic per-player, per-time");
                }
                check(active <= 4, "never more than four stars");
                bursts[active]++;
                if (active > 0) activeFrames++;
            }
            check(activeFrames < 3000 && activeFrames > 0, "3–4 second bursts still leave most frames without stars");
        }
        check(bursts[2] > 0 && bursts[3] > 0 && bursts[4] > 0, "2 to 4 stars occur");
        check(MagicSparkles.sample(Double.NaN, 0, 0) == null && MagicSparkles.sample(40, 0, 4) == null, "invalid inputs safe");
        System.out.println("PASS horn aura geometry and sparse stars: " + checks + " checks");
    }

    private static void check(boolean valid, String message) {
        checks++;
        if (!valid) throw new AssertionError(message);
    }
}
