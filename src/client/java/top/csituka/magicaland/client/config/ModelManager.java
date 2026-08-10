package top.csituka.magicaland.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class ModelManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File BASE_DIR = new File(FabricLoader.getInstance().getConfigDir().toFile(), "magicaland");
    private static final File MODELS_DIR = new File(BASE_DIR, "ponies");
    private static final long SAVE_DEBOUNCE_NANOS = 200_000_000L;

    private static ModelConfig activeModel;
    private static List<String> availableModels = new ArrayList<>();
    private static boolean savePending;
    private static long saveRequestedAt;
    private static boolean saveTickRegistered;

    public static void init() {
        registerSaveTick();
        if (!MODELS_DIR.exists()) {
            MODELS_DIR.mkdirs();
        }
        refreshModelList();
        
        if (availableModels.isEmpty()) {
            createModel("anon");
        }
        
        String lastActive = Config.getInstance().activeModelName;
        if (lastActive != null && !lastActive.isEmpty() && availableModels.contains(lastActive)
                && loadModel(lastActive)) {
            return;
        }

        for (String modelName : new ArrayList<>(availableModels)) {
            if (loadModel(modelName)) {
                return;
            }
        }

        activeModel = null;
        Config.getInstance().activeModelName = "";
        Config.save();
    }

    public static List<String> getAvailableModels() {
        return availableModels;
    }

    public static ModelConfig getActiveModel() {
        return activeModel;
    }

    public static void setActiveModel(ModelConfig model) {
        if (activeModel != model) {
            flushPendingSaveImmediately();
        }
        savePending = false;
        activeModel = model;
        if (model != null) {
            Config.getInstance().activeModelName = model.name;
            Config.save();
        }
        syncToServer();
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

        try {
            writeAtomically(file, writer -> GSON.toJson(newModel, writer));
            refreshModelList();
            setActiveModel(newModel);
            return true;
        } catch (IOException | RuntimeException e) {
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
                ModelConfig.sanitize(model);
                model.name = name;
                setActiveModel(model);
                return true;
            }
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void saveActiveModel() {
        savePending = false;
        if (activeModel == null) {
            return;
        }
        ModelConfig.sanitize(activeModel);
        File file = new File(MODELS_DIR, activeModel.name + ".json");
        try {
            writeAtomically(file, writer -> GSON.toJson(activeModel, writer));
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        }
        syncToServer();
    }

    public static void requestSaveActiveModel() {
        if (activeModel == null) {
            return;
        }
        savePending = true;
        saveRequestedAt = System.nanoTime();
    }

    private static void syncToServer() {
        try {
            top.csituka.magicaland.client.network.ClientNetworkHandler.sendModelToServer();
        } catch (Exception ignored) {
        }
    }

    public static boolean deleteModel(String name) {
        flushPendingSaveImmediately();
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

    private static void registerSaveTick() {
        if (saveTickRegistered) {
            return;
        }
        saveTickRegistered = true;
        ClientTickEvents.END_CLIENT_TICK.register(client -> flushPendingSave());
    }

    private static void flushPendingSave() {
        if (savePending && activeModel != null
                && System.nanoTime() - saveRequestedAt >= SAVE_DEBOUNCE_NANOS) {
            saveActiveModel();
        }
    }

    private static void flushPendingSaveImmediately() {
        if (savePending) {
            saveActiveModel();
        }
    }

    private static void writeAtomically(File file, WriterConsumer writerConsumer) throws IOException {
        Path target = file.toPath();
        Path temporary = target.resolveSibling(file.getName() + ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                writerConsumer.write(writer);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @FunctionalInterface
    private interface WriterConsumer {
        void write(Writer writer);
    }
}
