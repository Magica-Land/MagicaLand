import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import top.csituka.magicaland.client.config.Config;
import top.csituka.magicaland.client.config.ModelManager;

public final class ConfigPersistenceTest {
    private static final String NAME = "夜晚紫悦与朋友";
    private static int checks;

    public static void main(String[] args) throws Exception {
        if (args.length > 1) {
            check(Charset.defaultCharset().equals(Charset.forName(args[2])), "requested default charset is active");
            System.setProperty("magicaland.test.config", args[0]);
            if (args[1].equals("write")) write(Path.of(args[0]));
            else read(Path.of(args[0]));
            System.out.println("ConfigPersistenceTest child " + args[1] + "/" + args[2] + ": " + checks + " checks passed");
            return;
        }
        Path root = Files.createTempDirectory(Path.of(args[0]), "config-encoding-");
        for (String encoding : new String[] { "UTF-8", "windows-1252", "GBK" }) {
            Path config = Files.createDirectory(root.resolve(encoding));
            run(config, "write", "UTF-8");
            run(config, "read", encoding);
        }
        System.out.println("ConfigPersistenceTest: " + checks + " child processes passed");
    }

    private static void run(Path root, String operation, String encoding) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        int exit = new ProcessBuilder(java, "-Dfile.encoding=" + encoding, "-cp", System.getProperty("java.class.path"),
                ConfigPersistenceTest.class.getName(), root.toString(), operation, encoding).inheritIO().start().waitFor();
        check(exit == 0, operation + " succeeds under " + encoding);
    }

    private static void write(Path root) throws Exception {
        Config.load();
        Path file = root.resolve("magicaland/config.json");
        check(Files.exists(file), "missing config is created");
        Files.writeString(file, "{\"activeModelName\":\"legacy\",\"replacePlayerModel\":false}", StandardCharsets.UTF_8);
        Config.load();
        check(Config.getInstance().activeModelName.equals("legacy"), "legacy config still reads");
        check(!Config.getInstance().replacePlayerModel && Config.getInstance().broadcastOwnModel
                && Config.getInstance().automaticGaze, "old settings and defaults retained");
        ModelManager.init();
        check(ModelManager.createModel(NAME), "create second, non-default preset");
        ModelManager.getActiveModel().bodyColor = "#FF8877BB";
        ModelManager.saveActiveModel();
        Config.getInstance().magicSounds = false;
        Config.getInstance().automaticGaze = false;
        Config.save();
        check(Files.readString(file, StandardCharsets.UTF_8).contains(NAME), "saved config is UTF-8");
        check(!Files.exists(file.resolveSibling("config.json.tmp")), "successful atomic save cleans temp file");

        byte[] original = Files.readAllBytes(file);
        Path blocker = Files.createDirectory(file.resolveSibling("config.json.tmp"));
        Path keep = Files.createFile(blocker.resolve("keep"));
        Config.getInstance().activeModelName = "must not replace saved preset";
        PrintStream previousError = System.err;
        try (PrintStream expectedError = new PrintStream(new ByteArrayOutputStream())) {
            System.setErr(expectedError);
            Config.save();
        } finally {
            System.setErr(previousError);
        }
        check(Arrays.equals(original, Files.readAllBytes(file)), "failed atomic save preserves config bytes");
        check(Files.exists(keep), "failed save does not remove unrelated blocker contents");
        Files.delete(keep);
        Files.delete(blocker);
    }

    private static void read(Path root) throws Exception {
        Path file = root.resolve("magicaland/config.json");
        byte[] original = Files.readAllBytes(file);
        Config.load();
        check(Config.getInstance().activeModelName.equals(NAME), "Chinese selection is decoded exactly");
        check(Arrays.equals(original, Files.readAllBytes(file)), "valid config load does not rewrite it");
        check(!Config.getInstance().magicSounds && !Config.getInstance().automaticGaze
                && !Config.getInstance().replacePlayerModel, "other persisted settings survive restart");
        ModelManager.init();
        check(ModelManager.getAvailableModels().contains("anon"), "fallback preset exists for regression");
        check(ModelManager.getActiveModel().name.equals(NAME), "restart keeps selected Chinese preset, not fallback");
        check(ModelManager.getActiveModel().bodyColor.equals("#FF8877BB"), "correct preset content loaded");
        Config.save();
        Config.load();
        check(Config.getInstance().activeModelName.equals(NAME), "save and reload under non-UTF-8 default remain stable");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
