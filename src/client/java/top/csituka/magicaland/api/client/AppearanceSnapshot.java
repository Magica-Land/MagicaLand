package top.csituka.magicaland.api.client;

/** Immutable applied appearance; not a source of server gameplay authority. Colors are RGB. */
public record AppearanceSnapshot(boolean modelReplacementEnabled, boolean hasHorn, boolean hasWings,
                                 int magicColor) {}
