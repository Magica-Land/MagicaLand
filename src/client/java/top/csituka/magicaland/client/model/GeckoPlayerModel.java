package top.csituka.magicaland.client.model;

import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class GeckoPlayerModel extends GeoModel<GeckoPlayerAnimatable> {
    private static final Identifier MODEL = new Identifier("magicaland", "geo/pony_ts.geo.json");
    private static final Identifier ANIMATION = new Identifier("magicaland", "animations/pony_ts.animation.json");

    @Override
    public Identifier getModelResource(GeckoPlayerAnimatable object) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeckoPlayerAnimatable object) {
        return new Identifier("magicaland", "textures/entity/pony_base.png");
    }

    @Override
    public Identifier getAnimationResource(GeckoPlayerAnimatable object) {
        return ANIMATION;
    }
}
