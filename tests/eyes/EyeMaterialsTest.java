package top.csituka.magicaland.client.render;

import java.nio.file.Files;
import java.nio.file.Path;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import software.bernie.geckolib.util.JsonUtil;

public final class EyeMaterialsTest {
    private static int eyes, pupils, checks;

    public static void main(String[] args) throws Exception {
        Model raw = JsonUtil.GEO_GSON.fromJson(Files.readString(Path.of(args[0])), Model.class);
        var baked = BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(GeometryTree.fromModel(raw));
        for (var bone : raw.minecraftGeometry()[0].bones()) {
            if (!EyeMaterials.isEyeBone(bone.name())) continue;
            eyes++;
            GeoBone rendered = baked.getBone(bone.name()).orElseThrow();
            for (int i = 0; i < bone.cubes().length; i++) {
                var cube = bone.cubes()[i];
                boolean expectedPupil = cube.uv().isBoxUV() && cube.uv().boxUVCoords()[0] == 8 && cube.uv().boxUVCoords()[1] == 5;
                check(EyeMaterials.isPupil(rendered.getCubes().get(i)) == expectedPupil, "material role: " + bone.name() + " cube " + i);
                if (expectedPupil) pupils++;
            }
        }
        check(pupils == 8, "six normal pupils plus two inline angry pupils");
        check(eyes >= 14, "normal, all eye styles, smile, closed, scrunched and legacy blink");
        for (String name : new String[] {"Body", "Head", "Horn", "Style01FrontMane", "Mouth", "Emotions"})
            check(!EyeMaterials.isEyeBone(name), "non-eye bone excluded");
        System.out.println("PASS real GeckoLib eye geometry: " + checks + " checks, " + eyes + " eye bones, " + pupils + " pupils");
    }

    private static void check(boolean valid, String message) {
        checks++;
        if (!valid) throw new AssertionError(message);
    }
}
