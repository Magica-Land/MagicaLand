package top.csituka.magicaland.client.config;
public final class Config {
    private static final Config INSTANCE = new Config();
    public String activeModelName;
    public boolean broadcastOwnModel = true;
    public boolean automaticGaze = true;
    public static int saves;
    public static Config getInstance() { return INSTANCE; }
    public static void save() {
        saves++;
        try {
            var folder = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("magicaland");
            java.nio.file.Files.createDirectories(folder);
            java.nio.file.Files.writeString(folder.resolve("config.json"), new com.google.gson.Gson().toJson(INSTANCE));
        } catch (java.io.IOException e) { throw new RuntimeException(e); }
    }
}
