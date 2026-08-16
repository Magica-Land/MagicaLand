package top.csituka.magicaland.client.config.style;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Single source of truth for every pony mane/tail/eye style: which ids exist,
 * which body part each one is selectable for, how old character-abbreviation
 * save values (e.g. "RD") translate to the new numeric ids, and the bone-name
 * prefix used to render each one.
 *
 * <p>This replaces the {@code Set<String>}/{@code String[]} style lists that
 * used to be duplicated across {@code ModelConfig}, {@code ManePage},
 * {@code FacePage} and the ad-hoc RD/AJ string-equality checks in
 * {@code PonyRenderer}. Adding a new (non-legacy) style is a single
 * {@link #register} call here.
 */
public final class PonyStyleRegistry {
    private PonyStyleRegistry() {
    }

    /** Default style id assigned to new models and used as the sanitize() fallback. */
    public static final String DEFAULT_ID = "01";

    private static final List<PonyStyleDefinition> ALL = new ArrayList<>();
    private static final Map<String, PonyStyleDefinition> BY_ID = new LinkedHashMap<>();

    private static final Set<PonyStylePart> ALL_PARTS_NO_EYE =
            EnumSet.of(PonyStylePart.FRONT_MANE, PonyStylePart.BACK_MANE, PonyStylePart.TAIL);
    private static final Set<PonyStylePart> ALL_PARTS_WITH_EYE =
            EnumSet.of(PonyStylePart.FRONT_MANE, PonyStylePart.BACK_MANE, PonyStylePart.TAIL, PonyStylePart.EYE);
    private static final Set<PonyStylePart> BACK_MANE_AND_TAIL_ONLY =
            EnumSet.of(PonyStylePart.BACK_MANE, PonyStylePart.TAIL);

    static {
        // id  legacy   available parts          legacy-id overrides per part
        register("01", "TS", ALL_PARTS_WITH_EYE, Map.of());
        register("02", "RD", ALL_PARTS_NO_EYE, Map.of());
        register("03", "RR", ALL_PARTS_WITH_EYE, Map.of());
        register("04", "PP", ALL_PARTS_NO_EYE, Map.of());
        // AJ has no front-mane asset of its own: it shares RD's (Style02FrontMane),
        // so it is not front-mane-selectable, and a legacy frontManeStyle="AJ"
        // save value must resolve to "02" instead of "05".
        register("05", "AJ", BACK_MANE_AND_TAIL_ONLY, Map.of(PonyStylePart.FRONT_MANE, "02"));
        register("06", "FS", ALL_PARTS_WITH_EYE, Map.of());
        // Future original (non-Mane-Six) styles: register("07", null, ALL_PARTS_NO_EYE, Map.of());
    }

    private static void register(String id, String legacyCode, Set<PonyStylePart> availableParts,
            Map<PonyStylePart, String> legacyIdOverrideByPart) {
        PonyStyleDefinition def = new PonyStyleDefinition(id, legacyCode, availableParts, legacyIdOverrideByPart);
        ALL.add(def);
        BY_ID.put(id, def);
    }

    /** All styles selectable for the given part, in registration (id) order. */
    public static List<PonyStyleDefinition> stylesFor(PonyStylePart part) {
        return ALL.stream().filter(def -> def.isAvailableFor(part)).collect(Collectors.toUnmodifiableList());
    }

    public static PonyStyleDefinition byId(String id) {
        return id == null ? null : BY_ID.get(id);
    }

    public static boolean isValidStyleId(PonyStylePart part, String id) {
        PonyStyleDefinition def = byId(id);
        return def != null && def.isAvailableFor(part);
    }

    /**
     * Translates an old character-abbreviation save value (e.g. "RD") to the
     * new numeric id for the given part. Returns null if {@code legacyCode}
     * isn't a known legacy code (including when it's already a new-format id).
     */
    public static String legacyCodeToId(PonyStylePart part, String legacyCode) {
        if (legacyCode == null) {
            return null;
        }
        for (PonyStyleDefinition def : ALL) {
            if (legacyCode.equals(def.legacyCode)) {
                return def.legacyIdFor(part);
            }
        }
        return null;
    }

    /**
     * This style's 1-based position within {@link #stylesFor(PonyStylePart)}
     * for the given part — the number actually shown to players, kept
     * contiguous (no gaps) even where the underlying id set has one (e.g.
     * front-mane skips "05"). Returns -1 if the id isn't selectable for that part.
     */
    public static int displayOrdinal(PonyStylePart part, String id) {
        List<PonyStyleDefinition> list = stylesFor(part);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(id)) {
                return i + 1;
            }
        }
        return -1;
    }
}
