import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import top.csituka.magicaland.client.config.ModelManager;
import top.csituka.magicaland.client.config.Config;
import top.csituka.magicaland.client.network.ClientNetworkHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class ModelManagerDraftTest {
    private static int checks;
    private static final Gson GSON = new Gson();
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory(Path.of(args[0]), "draft-test-");
        System.setProperty("magicaland.test.config", root.toString());
        ModelManager.init();
        check(ModelManager.createModel("紫悦"), "setup preset");
        var original = ModelManager.getActiveModel();
        original.irisColor = "#8833DD";
        original.frontManeDyeColors = new String[] {"#111111", "#222222", "#333333", "#444444", "#555555", "#666666"};
        ModelManager.saveActiveModel();
        Path ponies = root.resolve("magicaland/ponies");
        Path broken = ponies.resolve("损坏.json");
        Files.writeString(broken, "{ user damaged data — keep exactly");
        ModelManager.refreshModelList();
        String beforeApplied = GSON.toJson(ModelManager.getAppliedModel());
        Map<String, byte[]> disk = snapshot(root);
        int sends = ClientNetworkHandler.sends, saves = Config.saves;
        check(ModelManager.beginEditing(), "begin");
        Object firstSession = ModelManager.editingSessionIdentity();
        check(firstSession != null, "editing session identity available");
        check(ModelManager.beginEditing(), "begin idempotent");
        check(firstSession == ModelManager.editingSessionIdentity(), "idempotent opening preserves history identity");
        check(!ModelManager.isPresetDirty("紫悦") && !ModelManager.isPresetDirty(null)
                && !ModelManager.isPresetDirty("missing"), "per-preset clean and missing guards");
        check(!ModelManager.isDirty(), "clean opening");
        check(ModelManager.getAppliedModel() != ModelManager.getActiveModel(), "separate applied and draft");
        check(!ModelManager.getAvailableModels().contains("损坏"), "broken presets not editable");
        check(!ModelManager.renameActiveModel("损坏"), "broken name protected");
        ModelManager.getActiveModel().irisColor = "#11BB55";
        check(ModelManager.isDirty(), "direct field edit tracked");
        check(ModelManager.isPresetDirty("紫悦"), "dropdown marks edited preset dirty");
        ModelManager.saveActiveModel();
        ModelManager.requestSaveActiveModel();
        ClientTickEvents.END_CLIENT_TICK.fire();
        var source = ModelManager.getActiveModel();
        check(ModelManager.duplicateActiveModel(" copy"), "duplicate draft");
        String duplicateName = ModelManager.getActiveModel().name;
        check(ModelManager.isPresetDirty(duplicateName), "new duplicate marked unsaved");
        check(!duplicateName.equals(source.name), "unique copy name");
        check(ModelManager.getActiveModel().frontManeDyeColors != source.frontManeDyeColors, "deep array copy");
        ModelManager.getActiveModel().frontManeDyeColors[0] = "#ABCDEF";
        check(source.frontManeDyeColors[0].equals("#111111"), "copy edits isolated");
        check(ModelManager.renameActiveModel("草稿副本"), "draft rename");
        check(ModelManager.isPresetDirty("草稿副本") && !ModelManager.isPresetDirty(duplicateName), "rename updates dropdown keys");
        check(ModelManager.createModel("临时"), "draft create");
        check(ModelManager.loadModel("紫悦"), "draft switch");
        check(ModelManager.getActiveModel().irisColor.equals("#11BB55"), "switch retains unsaved edit");
        check(ModelManager.getActiveModel() == source && firstSession == ModelManager.editingSessionIdentity(), "switch preserves draft and session identity for history");
        check(ModelManager.deleteModel("临时"), "draft delete");
        check(!ModelManager.deleteModel("损坏"), "broken file never deleted");
        check(ModelManager.loadModel("草稿副本"), "draft selection");
        check(Config.getInstance().activeModelName.equals("紫悦"), "Config unchanged while editing");
        check(beforeApplied.equals(GSON.toJson(ModelManager.getAppliedModel())), "published snapshot unchanged");
        sameDisk(disk, snapshot(root), "no implicit preset/config writes");
        check(ClientNetworkHandler.sends == sends && Config.saves == saves, "no implicit network or Config.save");
        ModelManager.discardEditing();
        check(!ModelManager.isEditing(), "discard ends session");
        check(ModelManager.editingSessionIdentity() == null && !ModelManager.isPresetDirty("紫悦"), "discard releases session and dirty flags");
        check(beforeApplied.equals(GSON.toJson(ModelManager.getActiveModel())), "discard restores baseline");
        sameDisk(disk, snapshot(root), "discard leaves disk untouched");
        check(ClientNetworkHandler.sends == sends && Config.saves == saves, "discard no publication");

        check(ModelManager.beginEditing(), "new session");
        check(ModelManager.editingSessionIdentity() != firstSession, "new editor has fresh history scope");
        check(ModelManager.renameActiveModel("改名"), "rename draft away");
        check(ModelManager.renameActiveModel("紫悦"), "rename draft back");
        check(!ModelManager.isDirty(), "undo rename returns clean");
        check(!ModelManager.isPresetDirty("紫悦"), "rename back clears per-preset dirty marker");
        check(ModelManager.duplicateActiveModel(), "duplicate 1");
        String firstCopy = ModelManager.getActiveModel().name;
        check(ModelManager.loadModel("紫悦") && ModelManager.duplicateActiveModel(), "duplicate 2");
        check(!firstCopy.equals(ModelManager.getActiveModel().name), "second duplicate has unique name");
        check(ModelManager.renameActiveModel("正式新预设"), "final chosen name");
        ModelManager.getActiveModel().pupilColor = "#554488";
        Config.getInstance().automaticGaze = false;
        Config.save();
        Path configPath = root.resolve("magicaland/config.json");
        var externalSettings = GSON.fromJson(Files.readString(configPath), com.google.gson.JsonObject.class);
        externalSettings.addProperty("broadcastOwnModel", false);
        externalSettings.addProperty("unknownFutureSetting", "preserve this field");
        Files.writeString(configPath, GSON.toJson(externalSettings));
        saves = Config.saves;
        check(ModelManager.commitEditing(), "explicit commit");
        check(!ModelManager.isEditing() && !ModelManager.isDirty(), "successful session closed");
        check(ModelManager.editingSessionIdentity() == null && !ModelManager.isPresetDirty("正式新预设"), "commit releases history scope and dirty markers");
        check(Files.exists(ponies.resolve("正式新预设.json")), "committed new preset");
        check(ModelManager.getAppliedModel().name.equals("正式新预设"), "applied snapshot updated");
        check(Config.getInstance().activeModelName.equals("正式新预设"), "Config active updated");
        var configJson = GSON.fromJson(Files.readString(root.resolve("magicaland/config.json")), com.google.gson.JsonObject.class);
        check(!configJson.get("automaticGaze").getAsBoolean(), "latest Settings retained in transaction");
        check(!configJson.get("broadcastOwnModel").getAsBoolean(), "disk-only external setting retained");
        check(configJson.get("unknownFutureSetting").getAsString().equals("preserve this field"), "unknown Config fields retained");
        check(configJson.get("activeModelName").getAsString().equals("正式新预设"), "active name persisted");
        check(ClientNetworkHandler.sends == sends && Config.saves == saves, "commit leaves sole publication to caller");
        check(Files.readString(broken).equals("{ user damaged data — keep exactly"), "broken file raw bytes preserved");
        check(snapshot(root).keySet().stream().noneMatch(name -> name.contains(".appearance-edit-")), "successful temp cleanup");

        check(ModelManager.beginEditing(), "disk-only selection change session");
        var changedSelection = GSON.fromJson(Files.readString(configPath), com.google.gson.JsonObject.class);
        changedSelection.addProperty("activeModelName", "another externally selected preset");
        Files.writeString(configPath, GSON.toJson(changedSelection));
        check(ModelManager.commitEditing(), "explicit save reconciles disk-only selection change");
        check(GSON.fromJson(Files.readString(configPath), com.google.gson.JsonObject.class).get("activeModelName").getAsString()
                .equals(ModelManager.getAppliedModel().name), "unchanged memory selection still persisted explicitly");

        check(ModelManager.beginEditing(), "failure session");
        var failedDraft = ModelManager.getActiveModel();
        failedDraft.irisColor = "#DD9933";
        Path changed = ponies.resolve(failedDraft.name + ".json");
        byte[] old = Files.readAllBytes(changed);
        Files.writeString(changed, "{ externally changed file");
        var changedSnapshot = snapshot(root);
        String appliedBeforeFailure = GSON.toJson(ModelManager.getAppliedModel());
        check(!ModelManager.commitEditing(), "external conflict rejects commit");
        check(ModelManager.isEditing() && ModelManager.isDirty(), "failure keeps draft");
        check(failedDraft.irisColor.equals("#DD9933"), "failed draft value retained");
        check(appliedBeforeFailure.equals(GSON.toJson(ModelManager.getAppliedModel())), "failure never changes applied");
        check(ClientNetworkHandler.sends == sends && Config.saves == saves, "failure no Config/network side effects");
        sameDisk(changedSnapshot, snapshot(root), "failure preserves external changes");
        Files.write(changed, old);
        check(ModelManager.commitEditing(), "retry unchanged file succeeds");

        check(ModelManager.beginEditing(), "new-name conflict session");
        check(ModelManager.createModel("竞争"), "create only in draft");
        Path collision = ponies.resolve("竞争.json");
        Files.writeString(collision, "{ someone else's file");
        var collisionSnapshot = snapshot(root);
        check(!ModelManager.commitEditing(), "external new-name conflict rejects");
        sameDisk(collisionSnapshot, snapshot(root), "new-name conflict not overwritten");
        ModelManager.discardEditing();

        check(ModelManager.beginEditing(), "unchanged-active conflict session");
        Path activePath = ponies.resolve(ModelManager.getActiveModel().name + ".json");
        byte[] activeBytes = Files.readAllBytes(activePath);
        Files.writeString(activePath, "{ externally replaced unchanged active");
        check(!ModelManager.isDirty(), "UI unchanged before active-file conflict");
        check(!ModelManager.commitEditing(), "unchanged selected preset still checked optimistically");
        ModelManager.discardEditing();
        Files.write(activePath, activeBytes);
        check(ModelManager.beginEditing(), "unrelated-change session");
        Path unrelated = ponies.resolve("anon.json");
        Files.writeString(unrelated, "{ externally changed unrelated preset");
        check(ModelManager.commitEditing(), "unrelated untouched external change does not block apply");
        check(Files.readString(unrelated).equals("{ externally changed unrelated preset"), "unrelated external bytes preserved");
        var beforeEntry = GSON.fromJson(Files.readString(activePath), com.google.gson.JsonObject.class);
        beforeEntry.addProperty("irisColor", "#000123");
        Files.writeString(activePath, GSON.toJson(beforeEntry));
        String appliedIris = ModelManager.getAppliedModel().irisColor;
        check(ModelManager.beginEditing(), "pre-entry external change session");
        check(ModelManager.getActiveModel().irisColor.equals(appliedIris), "preview begins from applied appearance");
        check(ModelManager.commitEditing(), "explicit apply reconciles applied and stored state");
        check(GSON.fromJson(Files.readString(activePath), com.google.gson.JsonObject.class).get("irisColor").getAsString().equals(appliedIris),
                "stored preset matches published appearance even when file changed before entry");
        Files.writeString(activePath, "{ broken active blocks session");
        String appliedBeforeBegin = GSON.toJson(ModelManager.getAppliedModel());
        check(!ModelManager.beginEditing(), "failed snapshot read blocks entry");
        check(!ModelManager.isEditing(), "failed begin never enters write-through UI session");
        check(appliedBeforeBegin.equals(GSON.toJson(ModelManager.getAppliedModel())), "failed begin keeps applied");
        Files.write(activePath, activeBytes);

        testRollback(root);
        check(ClientNetworkHandler.sends == sends && Config.saves == saves, "all transaction paths leave network to caller");
        System.out.println("PASS ModelManagerDraftTest: " + checks + " draft isolation, duplicate, commit, conflict and rollback checks.");
    }

    private static void testRollback(Path root) throws Exception {
        Path target = root.resolve("magicaland/ponies/rollback-target.json");
        byte[] before = "original bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(target, before);
        var changeClass = Class.forName("top.csituka.magicaland.client.config.ModelManager$FileChange");
        var constructor = changeClass.getDeclaredConstructor(Path.class, byte[].class, byte[].class);
        constructor.setAccessible(true);
        Object first = constructor.newInstance(target, before, "replacement".getBytes());
        Object second = constructor.newInstance(root.resolve("missing-parent/unwritable.json"), null, "new".getBytes());
        var transaction = ModelManager.class.getDeclaredMethod("applyTransaction", List.class);
        transaction.setAccessible(true);
        try {
            transaction.invoke(null, List.of(first, second));
            throw new AssertionError("expected second-write failure");
        } catch (java.lang.reflect.InvocationTargetException expected) {
            check(expected.getCause() instanceof java.io.IOException, "controlled mid-transaction I/O failure");
        }
        check(Arrays.equals(before, Files.readAllBytes(target)), "first write rolled back byte-for-byte");
        check(!Files.exists(root.resolve("missing-parent/unwritable.json")), "failed target absent");
        check(snapshot(root).keySet().stream().noneMatch(name -> name.contains(".appearance-edit-")), "rollback temp cleanup");
    }

    private static Map<String, byte[]> snapshot(Path root) throws Exception {
        Map<String, byte[]> result = new LinkedHashMap<>();
        try (var paths = Files.walk(root)) {
            for (Path file : paths.filter(Files::isRegularFile).toList()) result.put(root.relativize(file).toString(), Files.readAllBytes(file));
        }
        return result;
    }
    private static void sameDisk(Map<String, byte[]> first, Map<String, byte[]> second, String message) {
        check(first.keySet().equals(second.keySet()), message + " names");
        for (String name : first.keySet()) check(Arrays.equals(first.get(name), second.get(name)), message + " " + name);
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
