package net.minecraft.client.texture;
import net.minecraft.util.Identifier;
import java.util.HashMap;
import java.util.Map;
public final class TextureManager {
    public final Map<Identifier, AbstractTexture> textures = new HashMap<>();
    public AbstractTexture getTexture(Identifier id) { return textures.get(id); }
}
