package top.csituka.magicaland.client.config.style;

import java.util.Map;
import java.util.Set;

/**
 * A single pony style (formerly identified by a Mane Six character
 * abbreviation, e.g. "RD"). Immutable; the full set is built once by
 * {@link PonyStyleRegistry}.
 */
public final class PonyStyleDefinition {
    /** Stable, zero-padded two-digit internal id, e.g. "01". Never shown to players. */
    public final String id;

    /**
     * The old character-abbreviation code this style used to be saved as
     * (e.g. "TS"), kept only so {@link PonyStyleRegistry#legacyCodeToId}
     * can translate old save files. Null for styles that never had a legacy
     * code (future original styles added from "07" onward). Never shown to players.
     */
    public final String legacyCode;

    private final Set<PonyStylePart> availableParts;

    /**
     * Per-part override for what id a *legacy* save value for this style
     * should be translated to, when that differs from {@link #id}. Used for
     * the RD/AJ shared front-mane case: AJ's own id is "05", but AJ has no
     * front-mane asset of its own (it shares RD's), so a legacy
     * {@code frontManeStyle == "AJ"} save value must resolve to "02", not "05".
     */
    private final Map<PonyStylePart, String> legacyIdOverrideByPart;

    PonyStyleDefinition(String id, String legacyCode, Set<PonyStylePart> availableParts,
            Map<PonyStylePart, String> legacyIdOverrideByPart) {
        this.id = id;
        this.legacyCode = legacyCode;
        this.availableParts = availableParts;
        this.legacyIdOverrideByPart = legacyIdOverrideByPart;
    }

    /** Whether this style has its own selectable slot for the given part. */
    public boolean isAvailableFor(PonyStylePart part) {
        return availableParts.contains(part);
    }

    /** The id a legacy save value for this style resolves to for the given part. */
    String legacyIdFor(PonyStylePart part) {
        return legacyIdOverrideByPart.getOrDefault(part, id);
    }

    /** The bone-name prefix used to match every bone belonging to this style/part, e.g. "Style02FrontMane". */
    public String boneNamePrefix(PonyStylePart part) {
        return "Style" + id + part.boneSuffix;
    }
}
