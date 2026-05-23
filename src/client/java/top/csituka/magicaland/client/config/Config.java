package top.csituka.magicaland.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(),
            "magicaland.json");

    public boolean replacePlayerModel = true;
    public boolean firstPersonMagicGlow = true;

    public String frontManeStyle = "TS";
    public String backManeStyle = "TS";
    public String eyeStyle = "TS";

    public String hornColor = "#FFFFFFFF";
    public String bodyColor = "#FFFFFFFF";
    public String neckColor = "#FFFFFFFF";
    public String headColor = "#FFFFFFFF";
    public String leftEarColor = "#FFFFFFFF";
    public String rightEarColor = "#FFFFFFFF";
    public String limbColor = "#FFFFFFFF";
    public String leftFrontLimbColor = "#FFFFFFFF";
    public String rightFrontLimbColor = "#FFFFFFFF";
    public String leftHindLimbColor = "#FFFFFFFF";
    public String rightHindLimbColor = "#FFFFFFFF";
    public String noseColor = "#FFFFFFFF";

    public boolean showHorn = true;

    public boolean bodyColorLocked = true;
    public boolean noseColorLocked = true;
    public boolean neckColorLocked = true;
    public boolean headColorLocked = true;
    public boolean leftEarColorLocked = true;
    public boolean rightEarColorLocked = true;
    public boolean leftFrontLimbColorLocked = true;
    public boolean rightFrontLimbColorLocked = true;
    public boolean leftHindLimbColorLocked = true;
    public boolean rightHindLimbColorLocked = true;

    private static Config instance;

    public static Config getInstance() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                instance = GSON.fromJson(reader, Config.class);
            } catch (IOException e) {
                e.printStackTrace();
                instance = new Config();
            }
        } else {
            instance = new Config();
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}