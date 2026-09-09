package top.csituka.magicaland.cutiemark;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import com.google.gson.Gson;
import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.config.ModelManager;
import top.csituka.magicaland.client.network.ClientNetworkHandler;

public final class CutieMarkDraftTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory(Path.of(args[0]), "cutie-mark-draft-");
        System.setProperty("magicaland.test.config", root.toString());
        ModelManager.init();
        check(ModelManager.createModel("标志测试"), "create temporary preset");
        int[] left = new int[144]; left[1] = 0xFFEA3189;
        int[] right = new int[144]; right[9] = 0xFF1234EF;
        ModelManager.getActiveModel().setCutieMarkPixels(true, left);
        ModelManager.saveActiveModel();
        Path preset = root.resolve("magicaland/ponies/标志测试.json");
        String before = Files.readString(preset);
        int sends = ClientNetworkHandler.sends;
        check(ModelManager.beginEditing(), "open draft");
        ModelManager.getActiveModel().setCutieMarkLinked(false, true);
        ModelManager.getActiveModel().setCutieMarkPixels(false, right);
        ModelManager.saveActiveModel();
        ModelManager.requestSaveActiveModel();
        check(Files.readString(preset).equals(before), "painting never writes a preset before Save");
        check(ClientNetworkHandler.sends == sends, "painting does not transmit to other players");
        check(ModelManager.getAppliedModel().cutieMarkLinked, "world appearance stays linked before Save");
        check(Arrays.equals(ModelManager.getAppliedModel().cutieMarkPixels(false), left), "world still sees old pixels");
        ModelManager.discardEditing();
        check(ModelManager.getActiveModel().cutieMarkLinked && Arrays.equals(ModelManager.getActiveModel().cutieMarkPixels(false), left), "cancel restores both lock and pixels");
        check(Files.readString(preset).equals(before), "cancel does not alter disk");
        check(ModelManager.beginEditing(), "second draft");
        ModelManager.getActiveModel().setCutieMarkLinked(false, true);
        ModelManager.getActiveModel().setCutieMarkPixels(false, right);
        check(ModelManager.commitEditing(), "explicit Save commits pixel data");
        ModelConfig stored = new Gson().fromJson(Files.readString(preset), ModelConfig.class);
        check(!stored.cutieMarkLinked && Arrays.equals(stored.cutieMarkPixels(true), left)
                && Arrays.equals(stored.cutieMarkPixels(false), right), "both sides survive JSON persistence");
        check(ModelManager.loadModel("标志测试"), "reload saved preset");
        check(Arrays.equals(ModelManager.getAppliedModel().cutieMarkPixels(false), right), "reloaded world model contains mark");
        check(ModelManager.beginEditing(), "copy draft");
        ModelConfig original = ModelManager.getActiveModel();
        check(ModelManager.duplicateActiveModel(), "copy entire preset");
        ModelConfig copy = ModelManager.getActiveModel();
        check(copy != original && Arrays.equals(copy.cutieMarkPixels(false), right), "copied appearance includes independent designs");
        copy.setCutieMarkPixels(false, new int[144]);
        check(Arrays.equals(original.cutieMarkPixels(false), right), "erasing copy never erases original");
        ModelManager.discardEditing();
        check(stored.cutieMarkRight.equals(ModelManager.getAppliedModel().cutieMarkRight)
                && ModelManager.getAppliedModel().name.equals("标志测试"), "original remains applied after cancelled copy");
        System.out.println("PASS CutieMarkDraftTest: " + checks + " checks");
    }

    private static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
}
