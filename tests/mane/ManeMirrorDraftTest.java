package top.csituka.magicaland.client.render;

import java.nio.file.Files;
import java.nio.file.Path;
import com.google.gson.Gson;
import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.config.ModelManager;
import top.csituka.magicaland.client.network.ClientNetworkHandler;

public final class ManeMirrorDraftTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory(Path.of(args[0]), "mirror-draft-");
        System.setProperty("magicaland.test.config", root.toString());
        ModelManager.init(); check(ModelManager.createModel("镜像测试"), "temporary preset");
        Path preset = root.resolve("magicaland/ponies/镜像测试.json");
        String before = Files.readString(preset);
        int sends = ClientNetworkHandler.sends;
        check(ModelManager.beginEditing(), "begin editing");
        ModelManager.getActiveModel().frontManeMirrored = true;
        ModelManager.getActiveModel().backManeMirrored = true;
        ModelManager.getActiveModel().tailMirrored = true;
        ModelManager.saveActiveModel(); ModelManager.requestSaveActiveModel();
        check(Files.readString(preset).equals(before), "toggling draft never writes disk");
        check(ClientNetworkHandler.sends == sends, "toggling draft never broadcasts to other players");
        check(!ModelManager.getAppliedModel().frontManeMirrored && !ModelManager.getAppliedModel().backManeMirrored
                && !ModelManager.getAppliedModel().tailMirrored, "world stays unmirrored before Save");
        check(ModelManager.duplicateActiveModel(), "copy mirrored preset draft");
        ModelManager.getActiveModel().backManeMirrored = false;
        check(ModelManager.loadModel("镜像测试"), "return to original draft");
        check(ModelManager.getActiveModel().backManeMirrored, "copy flag change does not alter source");
        ModelManager.discardEditing();
        check(!ModelManager.getActiveModel().tailMirrored, "cancel restores original appearance");
        check(ModelManager.beginEditing(), "second edit");
        ModelManager.getActiveModel().frontManeMirrored = true;
        ModelManager.getActiveModel().tailMirrored = true;
        check(ModelManager.commitEditing(), "save");
        ModelConfig stored = new Gson().fromJson(Files.readString(preset), ModelConfig.class);
        check(stored.frontManeMirrored && !stored.backManeMirrored && stored.tailMirrored, "independent flags persisted");
        check(ModelManager.loadModel("镜像测试"), "reload");
        check(ModelManager.getAppliedModel().frontManeMirrored && ModelManager.getAppliedModel().tailMirrored, "applied/serialized model has mirror flags for sync");
        System.out.println("PASS ManeMirrorDraftTest: " + checks + " checks");
    }
    private static void check(boolean valid, String message) { checks++; if (!valid) throw new AssertionError(message); }
}
