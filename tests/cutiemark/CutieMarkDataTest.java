package top.csituka.magicaland.cutiemark;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import top.csituka.magicaland.client.config.ModelConfig;

public final class CutieMarkDataTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        check(CutieMarkData.SIZE == 12 && CutieMarkData.PIXELS == 144, "fixed coarse pixel canvas");
        check(CutieMarkData.BYTE_LENGTH == 576 && CutieMarkData.ENCODED_LENGTH == 768, "bounded wire size");
        check(CutieMarkData.encode(CutieMarkData.decode(null)).isEmpty(), "old configuration is blank");
        Random random = new Random(47019);
        for (int trial = 0; trial < 1200; trial++) {
            int[] pixels = new int[144];
            for (int i = 0; i < pixels.length; i++) pixels[i] = random.nextBoolean() ? random.nextInt() | 0xFF000000 : 0;
            String data = CutieMarkData.encode(pixels);
            check(data.length() == 768, "fixed size regardless of image contents");
            check(Arrays.equals(pixels, CutieMarkData.decode(data)), "RGBA transport roundtrip");
            check(Arrays.equals(pixels, CutieMarkData.mirror(CutieMarkData.mirror(pixels))), "mirror involution");
            check(CutieMarkData.decode(data) != CutieMarkData.decode(data), "no mutable alias between presets or sides");
            for (int y = 0; y < 12; y++) for (int x = 0; x < 12; x++)
                check(CutieMarkData.mirror(pixels)[y * 12 + x] == pixels[y * 12 + 11 - x], "horizontal row order");
        }
        int[] pixels = new int[144];
        pixels[0] = 0x00123456;
        check(CutieMarkData.encode(pixels).isEmpty(), "invisible RGB is discarded");
        for (int alpha = 1; alpha < 255; alpha++) {
            pixels[0] = alpha << 24 | 0x123456;
            rejects(() -> CutieMarkData.encode(pixels));
            byte[] bytes = new byte[576]; bytes[3] = (byte) alpha;
            rejects(() -> CutieMarkData.decode(Base64.getEncoder().encodeToString(bytes)));
        }
        for (int length : new int[] {0, 1, 143, 145, 10000}) rejects(() -> CutieMarkData.encode(new int[length]));
        for (String data : new String[] {"x", "x".repeat(767), "x".repeat(769), "x".repeat(100000), "!".repeat(768)}) {
            rejects(() -> CutieMarkData.decode(data));
            check(CutieMarkData.sanitize(data).isEmpty(), "broken local preset safely becomes blank");
        }
        modelChecks();
        networkChecks(Path.of(args[0]));
        System.out.println("PASS CutieMarkDataTest: " + checks + " checks");
    }

    private static void modelChecks() {
        Gson gson = new Gson();
        ModelConfig model = ModelConfig.sanitize(gson.fromJson("{\"name\":\"old\"}", ModelConfig.class));
        check(model.cutieMarkLinked && model.cutieMarkData(true).isEmpty() && model.cutieMarkData(false).isEmpty(), "legacy preset defaults");
        int[] left = new int[144]; left[0] = 0xFFFF0011; left[13] = 0xFF123456;
        model.setCutieMarkPixels(false, left);
        check(Arrays.equals(model.cutieMarkPixels(true), left) && model.cutieMarkRight.isEmpty(), "right editor changes shared mark without duplicate storage");
        model.setCutieMarkLinked(false, false);
        check(Arrays.equals(model.cutieMarkPixels(false), left), "unlock copies shared design");
        int[] right = model.cutieMarkPixels(false); right[4] = 0xFF0022FF;
        model.setCutieMarkPixels(false, right);
        check(Arrays.equals(model.cutieMarkPixels(true), left), "editing unlocked right cannot alter left");
        ModelConfig copy = gson.fromJson(gson.toJson(model), ModelConfig.class);
        model.setCutieMarkLinked(true, false);
        check(Arrays.equals(model.cutieMarkPixels(true), right) && model.cutieMarkRight.isEmpty(), "relink keeps current right design");
        check(!copy.cutieMarkLinked && Arrays.equals(copy.cutieMarkPixels(true), left), "copied preset remains independent");
        model.setCutieMarkLinked(false, true);
        model.setCutieMarkPixels(true, left);
        model.setCutieMarkLinked(true, true);
        check(Arrays.equals(model.cutieMarkPixels(false), left), "relink from left");
        copy.cutieMarkLeft = "bad";
        ModelConfig.sanitize(copy);
        check(copy.cutieMarkLeft.isEmpty() && Arrays.equals(copy.cutieMarkPixels(false), right), "sanitize only damaged side");
        model.setCutieMarkLinked(false, true);
        model.setCutieMarkPixels(false, right);
        String json = gson.toJson(ModelConfig.sanitize(model));
        check(json.length() < 16384, "both fully populated marks fit existing complete model budget: " + json.length());
        check(CutieMarkData.isValidModel(JsonParser.parseString(json).getAsJsonObject()), "normal client model passes server schema");
    }

    private static void networkChecks(Path repo) throws Exception {
        check(CutieMarkData.isValidModel(new JsonObject()), "old clients supported");
        for (String field : new String[] {"cutieMarkLeft", "cutieMarkRight"}) for (String value : new String[] {"null", "{}", "[]", "3", "true", "\"bad\""})
            check(!CutieMarkData.isValidModel(JsonParser.parseString("{\"" + field + "\":" + value + "}").getAsJsonObject()), "reject wrong image type/size");
        for (String value : new String[] {"null", "{}", "[]", "1", "\"false\""})
            check(!CutieMarkData.isValidModel(JsonParser.parseString("{\"cutieMarkLinked\":" + value + "}").getAsJsonObject()), "strict boolean flag");
        byte[] bytes = new byte[576]; bytes[0] = 19;
        JsonObject hidden = new JsonObject();
        hidden.addProperty("cutieMarkLeft", Base64.getEncoder().encodeToString(bytes));
        check(!CutieMarkData.isValidModel(hidden), "reject hidden RGB cache variants");
        int[] pixels = new int[144]; pixels[5] = 0xFFFFFFFF;
        hidden.addProperty("cutieMarkLeft", "");
        hidden.addProperty("cutieMarkRight", CutieMarkData.encode(pixels));
        check(!CutieMarkData.isValidModel(hidden), "linked models cannot carry an unused second image");
        hidden.addProperty("cutieMarkLinked", false);
        check(CutieMarkData.isValidModel(hidden), "independent right-only mark allowed");
        String server = Files.readString(repo.resolve("src/main/java/top/csituka/magicaland/network/NetworkHandler.java"));
        String client = Files.readString(repo.resolve("src/client/java/top/csituka/magicaland/client/network/ClientNetworkHandler.java"));
        check(server.contains("CutieMarkData.isValidModel(parsed.getAsJsonObject())"), "server validates before relaying");
        check(client.contains("CutieMarkData.isValidModel(parsed.getAsJsonObject())"), "receiver does not trust server payloads");
        check(client.contains("ModelManager.getAppliedModel()"), "draft paint strokes do not replace applied network model");
        check(server.contains("MAX_MODEL_DATA_LENGTH = 16384") && server.contains("MODEL_UPDATE_INTERVAL_NANOS = 100_000_000L"), "existing packet and coalescing limits retained");
    }

    private static void rejects(Runnable action) {
        boolean threw = false;
        try { action.run(); } catch (IllegalArgumentException expected) { threw = true; }
        check(threw, "malformed image rejected before use");
    }
    private static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
}
