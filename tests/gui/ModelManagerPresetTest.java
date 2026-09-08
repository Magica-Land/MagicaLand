import java.nio.file.Files;
import java.nio.file.Path;
import top.csituka.magicaland.client.config.ModelManager;
import top.csituka.magicaland.client.config.Config;

public final class ModelManagerPresetTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory(Path.of(args[0]), "preset-test-");
        System.setProperty("magicaland.test.config", root.toString());
        ModelManager.init();
        check(ModelManager.getActiveModel() != null, "default preset");
        check(ModelManager.createModel("紫悦"), "create unicode preset");
        var active = ModelManager.getActiveModel();
        active.irisColor = "#8833DD";
        active.scleraColor = "#DDDDEE";
        active.bodyColor = "#9966BB";
        active.irisLightColorLocked = false;
        ModelManager.requestSaveActiveModel();
        check(ModelManager.loadModel("紫悦"), "reload pending preset");
        check(ModelManager.getActiveModel().irisColor.equals("#8833DD"), "pending change not stale");
        check(ModelManager.renameActiveModel("夜晚紫悦"), "rename unicode preset");
        check(ModelManager.getActiveModel().name.equals("夜晚紫悦"), "in-memory name");
        check(Config.getInstance().activeModelName.equals("夜晚紫悦"), "selected name");
        Path presets = root.resolve("magicaland/ponies");
        check(!Files.exists(presets.resolve("紫悦.json")), "old name moved");
        check(Files.exists(presets.resolve("夜晚紫悦.json")), "new name exists");
        check(ModelManager.loadModel("anon"), "switch away");
        check(ModelManager.loadModel("夜晚紫悦"), "switch back");
        check(ModelManager.getActiveModel().bodyColor.equals("#9966BB"), "body color preserved");
        check(ModelManager.getActiveModel().scleraColor.equals("#DDDDEE"), "advanced eye color preserved");
        check(!ModelManager.getActiveModel().irisLightColorLocked, "locks preserved");
        check(!ModelManager.renameActiveModel("anon"), "never overwrite existing preset");
        for (String invalid : new String[] { "../escape", "CON", "name.", "a/b", "a\\b", "", "x:y", "NUL.txt" }) {
            check(!ModelManager.renameActiveModel(invalid), "reject invalid name: " + invalid);
        }
        var snapshot = ModelManager.getAvailableModels();
        check(ModelManager.createModel("测试"), "another preset");
        check(!snapshot.contains("测试"), "stable option snapshot");
        try { snapshot.clear(); throw new AssertionError("mutable snapshot"); }
        catch (UnsupportedOperationException expected) { checks++; }
        var blocked = ModelManager.getActiveModel();
        blocked.irisColor = "#11AA55";
        ModelManager.requestSaveActiveModel();
        Path blocker = presets.resolve("测试.json.tmp");
        Files.createDirectory(blocker);
        Files.createFile(blocker.resolve("keep"));
        check(!ModelManager.loadModel("anon"), "failed save blocks switching");
        check(!ModelManager.renameActiveModel("不会创建"), "failed save blocks rename");
        check(!ModelManager.createModel("不会创建"), "failed save blocks create");
        check(!ModelManager.deleteModel("anon"), "failed save blocks delete");
        check(ModelManager.getActiveModel() == blocked, "failure preserves in-memory edits");
        check(!Files.exists(presets.resolve("不会创建.json")), "no half-created preset");
        Files.delete(blocker.resolve("keep"));
        Files.delete(blocker);
        ModelManager.saveActiveModel();
        blocked.bodyColor = "#112233";
        Files.createDirectory(blocker);
        Files.createFile(blocker.resolve("keep"));
        ModelManager.saveActiveModel();
        check(!ModelManager.loadModel("anon"), "immediate failed save also blocks switching");
        check(ModelManager.getActiveModel().bodyColor.equals("#112233"), "immediate edits retained");
        Files.delete(blocker.resolve("keep"));
        Files.delete(blocker);
        ModelManager.saveActiveModel();
        for (String name : ModelManager.getAvailableModels()) if (!name.equals("测试")) {
            Files.writeString(presets.resolve(name + ".json"), "{ corrupted");
        }
        check(!ModelManager.deleteModel("测试"), "do not delete last readable preset");
        check(Files.exists(presets.resolve("测试.json")), "last good preset remains");
        check(ModelManager.createModel("可用替代"), "valid alternate");
        check(ModelManager.loadModel("测试"), "select deletion target");
        check(ModelManager.deleteModel("测试"), "delete with readable alternate");
        check(ModelManager.getActiveModel().name.equals("可用替代"), "skip corrupted alternate files");
        ModelManager.saveActiveModel();
        check(!Files.exists(presets.resolve("测试.json")), "deleted preset cannot resurrect on exit save");
        System.out.println("PASS ModelManagerPresetTest: " + checks + " UTF-8, rename, conflict, pending-save and I/O failure checks.");
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
