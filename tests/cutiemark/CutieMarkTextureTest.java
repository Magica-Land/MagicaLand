package top.csituka.magicaland.client.render;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import software.bernie.geckolib.util.JsonUtil;

public final class CutieMarkTextureTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path repo = Path.of(args[0]);
        var image = ImageIO.read(repo.resolve("src/main/resources/assets/magicaland/textures/entity/base.png").toFile());
        check(image.getWidth() == 256 && image.getHeight() == 256, "real production atlas");
        for (var side : CutieMarkTexture.Side.values()) {
            for (int scale : new int[] {1, 2, 4}) {
                int[] counts = new int[144];
                for (int y = 0; y < 256 * scale; y++) for (int x = 0; x < 256 * scale; x++) {
                    int index = CutieMarkTexture.pixelIndex(side, x, y, 256 * scale, 256 * scale);
                    boolean inside = x >= side.x * scale && x < (side.x + 12) * scale && y >= side.y * scale && y < (side.y + 12) * scale;
                    check((index >= 0) == inside, "no outside-face writes");
                    if (index >= 0) { check(index < 144, "bounded mark sample"); counts[index]++; }
                }
                for (int count : counts) check(count == scale * scale, "nearest-neighbour exact pixel replication");
            }
            for (int y = 0; y < 12; y++) for (int x = 0; x < 12; x++) {
                int argb = image.getRGB(side.x + x, side.y + y), source = swap(argb);
                check((argb >>> 24) == 255, "real outer face is opaque");
                int red = CutieMarkTexture.composite(source, 0xFF112233, 0xFFFF0000);
                check((red & 0x00FFFF00) == 0, "mark hue cannot inherit blue or green body tint");
                check(red == CutieMarkTexture.composite(source, 0xFFEEEEEE, 0xFFFF0000), "mark independent of body palette");
                check(CutieMarkTexture.composite(source, 0xFF112233, 0) == 0xFF112233, "eraser preserves complete underlying shading");
                check((red >>> 24) == 255, "paint never cuts holes in body");
            }
        }
        check(CutieMarkTexture.composite(0xFFFFFFFF, 0xFF123456, 0xFF123456) == 0xFF563412, "ARGB to ABGR order");
        check(CutieMarkTexture.composite(0xFF808080, 0xFF112233, 0xFFFFFFFF) == 0xFF808080, "stable grayscale lighting retained");
        check(CutieMarkTexture.composite(0x00FFFFFF, 0x00112233, 0xFFFF0000) == 0x00112233, "never create geometry in transparent atlas space");
        check(CutieMarkTexture.legacyTint(0xFF804020, 0x123456) == 0xFF2B0D02, "legacy vertex multiply baked before mark");
        for (String bone : new String[] {"Body", "LHindLeg2", "RHindLeg2", "LFrontLeg", "Tail", "Horn", "Eye01", "", null})
            check(CutieMarkTexture.sideForBone(bone) == null, "unrelated bones cannot see mark texture: " + bone);
        check(CutieMarkTexture.pixelIndex(CutieMarkTexture.Side.LEFT, 88, 54, 256, 512) == -1, "unknown resource-pack layout ignored safely");
        geometryCheck(repo);
        sourceGuard(repo);
        System.out.println("PASS CutieMarkTextureTest: " + checks + " checks");
    }

    private static void geometryCheck(Path repo) throws Exception {
        Model raw = JsonUtil.GEO_GSON.fromJson(Files.readString(repo.resolve("src/main/resources/assets/magicaland/geo/mare_geo.json")), Model.class);
        var model = BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(GeometryTree.fromModel(raw));
        for (String name : new String[] {"LHindLeg", "RHindLeg"}) {
            var side = CutieMarkTexture.sideForBone(name);
            var bone = model.getBone(name).orElseThrow();
            int matching = 0;
            for (var cube : bone.getCubes()) for (var quad : cube.quads()) if (quad != null) {
                float minU = 999, maxU = -999, minV = 999, maxV = -999;
                for (var v : quad.vertices()) {
                    minU = Math.min(minU, v.texU() * 256); maxU = Math.max(maxU, v.texU() * 256);
                    minV = Math.min(minV, v.texV() * 256); maxV = Math.max(maxV, v.texV() * 256);
                }
                boolean overlaps = minU < side.x + 12 - .01 && maxU > side.x + .01 && minV < side.y + 12 - .01 && maxV > side.y + .01;
                if (overlaps) {
                    matching++;
                    check(Math.abs(minU - side.x) < .01 && Math.abs(maxU - side.x - 12) < .01
                            && Math.abs(minV - side.y) < .01 && Math.abs(maxV - side.y - 12) < .01, "paint covers exactly one complete production quad: " + name);
                    check(Math.abs(quad.normal().x) > .99 && Math.abs(quad.normal().y) < .01 && Math.abs(quad.normal().z) < .01, "lateral face normal");
                    check(side == CutieMarkTexture.Side.LEFT ? quad.normal().x < -.99 : quad.normal().x > .99,
                            "paint faces away from body after Gecko X mirror: " + name);
                }
            }
            check(matching == 1, "UV does not overlap another face on target bone: " + name);
        }
    }

    private static void sourceGuard(Path repo) throws Exception {
        String body = Files.readString(repo.resolve("src/client/java/top/csituka/magicaland/client/render/BodyTintTextures.java"));
        String renderer = Files.readString(repo.resolve("src/client/java/top/csituka/magicaland/client/render/PonyRenderer.java"));
        check(body.contains("new Draft[2]"), "bounded draft slots independent of stroke count");
        check(body.contains("config == ModelManager.getActiveModel()") && body.contains("ModelManager.isEditing()"), "draft texture never mutates saved or remote cache");
        check(body.contains("fill(previous.texture.getImage(), key)") && body.contains("previous.texture.upload()"), "repaint reuses registered texture rather than filling cache");
        check(body.contains("if (!ModelManager.isEditing()) clearDrafts()") && body.contains("clearDrafts();"), "draft resources released after editing and resource reset");
        check(body.contains("key.legacy ? CutieMarkTexture.legacyTint") && renderer.contains("if (colorField != null && !usingPalette)"), "legacy mark is not vertex-tinted twice");
        check(renderer.contains("BodyTintTextures.colorForBone(config, bone.getName()), bone.getName())"), "only targeted bone receives composed texture");
        check(body.contains("if (bytes + cost > MAX_BYTES) return null") && body.contains("bytes -= DRAFTS[i].bytes"), "draft native/GPU memory has shared budget and release accounting");
    }

    private static int swap(int argb) { return argb & 0xFF00FF00 | (argb & 255) << 16 | argb >> 16 & 255; }
    private static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
}
