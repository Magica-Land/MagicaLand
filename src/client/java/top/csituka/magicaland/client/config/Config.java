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
    private static final File BASE_DIR = new File(FabricLoader.getInstance().getConfigDir().toFile(), "magicaland");
    private static final File CONFIG_FILE = new File(BASE_DIR, "config.json");

    public boolean replacePlayerModel = true;
    public boolean firstPersonMagicGlow = true;
    
    public String activeModelName = "";

    private static Config instance;

    public static Config getInstance() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        if (!BASE_DIR.exists()) {
            BASE_DIR.mkdirs();
        }
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
        if (!BASE_DIR.exists()) {
            BASE_DIR.mkdirs();
        }
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}