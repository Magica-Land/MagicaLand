import com.google.gson.Gson;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;
import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.render.IrisPalette;
import top.csituka.magicaland.client.render.EyePalette;

public final class IrisPaletteTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        var image = ImageIO.read(new File(args[0]));
        int mainPixels = 0, lightPixels = 0, changedPixels = 0;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            int source = swap(image.getRGB(x, y));
            int region = IrisPalette.region(x, y, image.getWidth(), image.getHeight());
            if (region == 1) mainPixels++;
            if (region == 2) lightPixels++;
            check(IrisPalette.recolorAbgr(source, region, IrisPalette.DEFAULT_BASE, IrisPalette.DEFAULT_LIGHT) == source,
                    "default colors preserve source pixels exactly");
            check(EyePalette.recolorAbgr(source, x, y, image.getWidth(), image.getHeight(),
                    IrisPalette.DEFAULT_BASE, IrisPalette.DEFAULT_LIGHT, 0, 0xFFFFFF) == source, "advanced defaults preserve every pixel");
            int mapped = IrisPalette.recolorAbgr(source, region, 0xE8A349, 0x81EAC6);
            check((mapped >>> 24) == (source >>> 24), "preserve alpha");
            if (region == 0) check(mapped == source, "preserve everything outside iris mask");
            if (mapped != source) changedPixels++;
        }
        check(mainPixels == 24 && lightPixels == 1 && changedPixels == 25, "exact painted iris regions");
        check(IrisPalette.region(16, 128, 256, 256) == 0, "white catchlights excluded");
        check(IrisPalette.region(20, 14, 256, 256) == 0, "black pupil excluded");
        for (int scale : new int[] {1, 2, 3, 4}) {
            int count = 0;
            for (int y = 0; y < 256 * scale; y++) for (int x = 0; x < 256 * scale; x++)
                if (IrisPalette.region(x, y, 256 * scale, 256 * scale) > 0) count++;
            check(count == 25 * scale * scale, "normalized UV mask at scaled texture size");
        }

        Random random = new Random(20260908);
        for (int i = 0; i < 4100; i++) {
            int target = i == 0 ? 0 : i == 1 ? 0xFFFFFF : i == 2 ? 0xFF0000 : i == 3 ? 0x808080 : random.nextInt(1 << 24);
            int first = -1, last = -1;
            for (int row = 128; row < 134; row++) {
                int mapped = swap(IrisPalette.recolorAbgr(swap(image.getRGB(4, row)), 1, target, 0));
                int value = Math.max(mapped & 255, Math.max(mapped >>> 8 & 255, mapped >>> 16 & 255));
                if (first < 0) first = value;
                check(value >= last, "painted gradient retains brightness order");
                last = value;
            }
            check(last > first, "gradient does not flatten, including black and white");
        }
        check(IrisPalette.recolorAbgr(0x00556677, 1, 0, 0) == 0x00556677, "transparent pixels unchanged");
        check((IrisPalette.recolorAbgr(0x80556677, 1, 0xABCDEF, 0) >>> 24) == 128, "partial alpha preserved");
        int accent = swap(image.getRGB(3, 127));
        check(IrisPalette.recolorAbgr(accent, 2, 0xFF0000, 0x44CC99)
                == IrisPalette.recolorAbgr(accent, 2, 0x0000FF, 0x44CC99), "unlocked accent independent of main color");

        Gson gson = new Gson();
        ModelConfig old = ModelConfig.sanitize(gson.fromJson("{\"name\":\"old\"}", ModelConfig.class));
        check(IrisPalette.base(old) == IrisPalette.DEFAULT_BASE && IrisPalette.light(old) == IrisPalette.DEFAULT_LIGHT,
                "old presets keep original eyes");
        check(old.irisLightColorLocked, "new accent follows main by default");
        check(old.eyelashColor.equals("#000000") && old.pupilColor.equals("#000000") && old.scleraColor.equals("#FFFFFF"),
                "old presets retain advanced defaults");
        check(EyePalette.recolorAbgr(0xFFFFFFFF, 16, 128, 256, 256, 0, 0, 0xFF0000, 0x0000FF) == 0xFFFFFFFF,
                "white catchlights unaffected by all five controls");
        check(EyePalette.recolorAbgr(0xFF000000, 20, 14, 256, 256, 0, 0, 0x123456, 0xFF00FF) == 0xFF563412,
                "black material maps independently to selected color");
        check(EyePalette.recolorAbgr(0xFFFFFFFF, 0, 124, 256, 256, 0, 0, 0xABCDEF, 0x123456) == 0xFF563412,
                "sclera independent of black material");
        old.irisColor = "#E8A349";
        int automatic = IrisPalette.light(old);
        check(automatic != IrisPalette.DEFAULT_LIGHT, "automatic accent follows edited main");
        IrisPalette.setLightLocked(old, false);
        check(IrisPalette.light(old) == automatic, "unlock preserves displayed color");
        old.irisColor = "#8844BB";
        check(IrisPalette.light(old) == automatic, "manual accent survives main edit");
        old.irisLightColor = "#005599";
        old.eyelashColor = "#884422";
        old.scleraColor = "#AACCFF";
        old.pupilColor = "#223344";
        ModelConfig remote = ModelConfig.sanitize(gson.fromJson(gson.toJson(old), ModelConfig.class));
        check(remote.irisColor.equals(old.irisColor) && remote.irisLightColor.equals(old.irisLightColor)
                && !remote.irisLightColorLocked, "save and network JSON round trip");
        check(remote.eyelashColor.equals(old.eyelashColor) && remote.scleraColor.equals(old.scleraColor)
                && remote.pupilColor.equals(old.pupilColor), "advanced colors save and network JSON round trip");
        IrisPalette.setLightLocked(old, true);
        check(IrisPalette.light(old) == IrisPalette.automaticLight(IrisPalette.base(old)), "relock follows current main");
        IrisPalette.reset(old);
        check(IrisPalette.base(old) == IrisPalette.DEFAULT_BASE && IrisPalette.light(old) == IrisPalette.DEFAULT_LIGHT,
                "reset restores exact original palette");
        ModelConfig invalid = ModelConfig.sanitize(gson.fromJson("{\"irisColor\":null,\"irisLightColor\":\"not-a-color\"}", ModelConfig.class));
        check(invalid.irisColor.equals("#516BD1") && invalid.irisLightColor.equals("#7BA1D2"), "invalid data falls back safely");
        invalid.irisColor = "#80556677";
        check(IrisPalette.base(ModelConfig.sanitize(invalid)) == 0x556677, "old ARGB config accepted as opaque RGB");
        System.out.println("PASS IrisPaletteTest: " + checks + " checks");
    }

    private static int swap(int pixel) {
        return (pixel & 0xFF00FF00) | (pixel & 255) << 16 | (pixel >>> 16 & 255);
    }

    private static void check(boolean valid, String message) {
        checks++;
        if (!valid) throw new AssertionError(message);
    }
}
