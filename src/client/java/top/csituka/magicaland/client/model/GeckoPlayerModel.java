package top.csituka.magicaland.client.model;

import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class GeckoPlayerModel extends GeoModel<GeckoPlayerAnimatable> {
    private static final Identifier MODEL = new Identifier("magicaland", "geo/mare_geo.json");
    private static final Identifier ANIMATION = new Identifier("magicaland", "animations/mare_animation.json");

    @Override
    public Identifier getModelResource(GeckoPlayerAnimatable object) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeckoPlayerAnimatable object) {
        return new Identifier("magicaland", "textures/entity/base.png");
    }

    @Override
    public Identifier getAnimationResource(GeckoPlayerAnimatable object) {
        return ANIMATION;
    }
}
