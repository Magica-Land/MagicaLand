package top.csituka.magicaland.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ModelManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File BASE_DIR = new File(FabricLoader.getInstance().getConfigDir().toFile(), "magicaland");
    private static final File MODELS_DIR = new File(BASE_DIR, "ponies");
    
    private static ModelConfig activeModel;
    private static List<String> availableModels = new ArrayList<>();

    public static void init() {
        if (!MODELS_DIR.exists()) {
            MODELS_DIR.mkdirs();
        }
        refreshModelList();
        
        if (availableModels.isEmpty()) {
            createModel("anon");
        }
        
        String lastActive = Config.getInstance().activeModelName;
        if (lastActive != null && !lastActive.isEmpty() && availableModels.contains(lastActive)) {
            loadModel(lastActive);
        } else {
            activeModel = null;
        }
    }

    public static List<String> getAvailableModels() {
        return availableModels;
    }

    public static ModelConfig getActiveModel() {
        return activeModel;
    }

    public static void setActiveModel(ModelConfig model) {
        activeModel = model;
        if (model != null) {
            Config.getInstance().activeModelName = model.name;
            Config.save();
        }
    }

    public static void refreshModelList() {
        availableModels.clear();
        if (MODELS_DIR.exists() && MODELS_DIR.isDirectory()) {
            File[] files = MODELS_DIR.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    String name = file.getName();
                    availableModels.add(name.substring(0, name.length() - 5));
                }
            }
        }
        if (availableModels.isEmpty()) {
            createModel("anon");
        }
    }

    public static boolean createModel(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        name = name.trim();
        File file = new File(MODELS_DIR, name + ".json");
        if (file.exists()) {
            return false;
        }

        ModelConfig newModel = new ModelConfig();
        newModel.name = name;
        
        newModel.frontManeStyle = "TS";
        newModel.backManeStyle = "TS";
        newModel.eyeStyle = "TS";
        newModel.hornColor = "#FFFFFFFF";
        newModel.bodyColor = "#FFFFFFFF";
        newModel.neckColor = "#FFFFFFFF";
        newModel.headColor = "#FFFFFFFF";
        newModel.leftEarColor = "#FFFFFFFF";
        newModel.rightEarColor = "#FFFFFFFF";
        newModel.limbColor = "#FFFFFFFF";
        newModel.leftFrontLimbColor = "#FFFFFFFF";
        newModel.rightFrontLimbColor = "#FFFFFFFF";
        newModel.leftHindLimbColor = "#FFFFFFFF";
        newModel.rightHindLimbColor = "#FFFFFFFF";
        newModel.noseColor = "#FFFFFFFF";
        newModel.showHorn = true;
        newModel.hornColorLocked = true;
        newModel.bodyColorLocked = true;
        newModel.noseColorLocked = true;
        newModel.neckColorLocked = true;
        newModel.headColorLocked = true;
        newModel.leftEarColorLocked = true;
        newModel.rightEarColorLocked = true;
        newModel.leftFrontLimbColorLocked = true;
        newModel.rightFrontLimbColorLocked = true;
        newModel.leftHindLimbColorLocked = true;
        newModel.rightHindLimbColorLocked = true;

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(newModel, writer);
            refreshModelList();
            setActiveModel(newModel);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean loadModel(String name) {
        File file = new File(MODELS_DIR, name + ".json");
        if (!file.exists()) {
            return false;
        }
        try (FileReader reader = new FileReader(file)) {
            ModelConfig model = GSON.fromJson(reader, ModelConfig.class);
            if (model != null) {
                model.name = name;
                setActiveModel(model);
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void saveActiveModel() {
        if (activeModel == null) {
            return;
        }
        File file = new File(MODELS_DIR, activeModel.name + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(activeModel, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean deleteModel(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (availableModels.size() <= 1) {
            return false;
        }
        File file = new File(MODELS_DIR, name + ".json");
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                refreshModelList();
                
                if (activeModel != null && activeModel.name.equals(name)) {
                    String newActiveModel = availableModels.isEmpty() ? null : availableModels.get(0);
                    if (newActiveModel != null) {
                        loadModel(newActiveModel);
                    } else {
                        activeModel = null;
                        Config.getInstance().activeModelName = "";
                        Config.save();
                    }
                }
                return true;
            }
        }
        return false;
    }
}
