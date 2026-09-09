package top.csituka.magicaland;

import net.fabricmc.api.ModInitializer;
import top.csituka.magicaland.easteregg.CarrotMisunderstanding;
import top.csituka.magicaland.network.NetworkHandler;

public class Magical_Land implements ModInitializer {

    @Override
    public void onInitialize() {
        NetworkHandler.registerServer();
        CarrotMisunderstanding.register();
    }
}
