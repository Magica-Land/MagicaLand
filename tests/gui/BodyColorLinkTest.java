import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.config.ModelManager;
import top.csituka.magicaland.client.gui.ponycustom.PonyCustomPageHelper;

public final class BodyColorLinkTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory(Path.of(args[0]), "body-color-link-");
        System.setProperty("magicaland.test.config", root.toString());
        ModelManager.init();
        check(ModelManager.createModel("锁色天马"), "create preset");
        ModelConfig original = ModelManager.getActiveModel();
        original.showWings = true;
        original.bodyColor = original.wingColor = "#FF778899";
        ModelManager.saveActiveModel();
        Path preset = root.resolve("magicaland/ponies/锁色天马.json");
        byte[] saved = Files.readAllBytes(preset);
        Method sync = PonyCustomPageHelper.class.getDeclaredMethod("syncLockedBodyColors", String.class);
        sync.setAccessible(true);

        check(ModelManager.beginEditing(), "begin draft without creating a wing picker");
        sync.invoke(null, "#FFCC99BB");
        ModelManager.requestSaveActiveModel();
        ModelConfig draft = ModelManager.getActiveModel();
        check(draft.bodyColor.equals("#FFCC99BB") && draft.wingColor.equals(draft.bodyColor), "locked wings follow body without visiting wing page");
        check(ModelManager.getAppliedModel().wingColor.equals("#FF778899"), "draft does not change published wings");
        check(Arrays.equals(saved, Files.readAllBytes(preset)), "draft does not write preset before save");
        check(ModelManager.commitEditing(), "save locked-wing draft");
        check(ModelManager.loadModel("锁色天马"), "reload locked-wing preset");
        check(ModelManager.getActiveModel().wingColor.equals("#FFCC99BB"), "linked wings survive save and reload");

        check(ModelManager.beginEditing(), "begin independent-wing draft");
        draft = ModelManager.getActiveModel();
        draft.wingColorLocked = false;
        draft.wingColor = "#FF113355";
        sync.invoke(null, "#FFAA6622");
        check(draft.bodyColor.equals("#FFAA6622") && draft.wingColor.equals("#FF113355"), "unlocked wings keep independent color");
        check(ModelManager.commitEditing(), "save independent-wing draft");
        check(ModelManager.loadModel("锁色天马"), "reload independent-wing preset");
        check(!ModelManager.getActiveModel().wingColorLocked
                && ModelManager.getActiveModel().wingColor.equals("#FF113355"), "unlocked color and lock survive reload");

        check(ModelManager.beginEditing(), "begin relocked draft");
        draft = ModelManager.getActiveModel();
        draft.wingColorLocked = true;
        draft.leftEarColorLocked = false;
        draft.leftEarColor = "#FF223344";
        sync.invoke(null, "#FF556677");
        check(draft.wingColor.equals("#FF556677"), "relocked wings resume following");
        for (String part : new String[] { "horn", "body", "neck", "head", "nose", "rightEar", "leftFrontLimb",
                "rightFrontLimb", "leftHindLimb", "rightHindLimb" }) {
            check(ModelConfig.class.getField(part + "Color").get(draft).equals("#FF556677"), part + " remains linked");
        }
        check(draft.leftEarColor.equals("#FF223344"), "other unlocked parts remain independent");
        ModelManager.discardEditing();
        check(!ModelManager.getActiveModel().wingColorLocked
                && ModelManager.getActiveModel().wingColor.equals("#FF113355"), "discard restores saved independent wings");
        System.out.println("BodyColorLinkTest: " + checks + " checks passed");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
