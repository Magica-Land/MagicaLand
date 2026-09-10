package top.csituka.magicaland.client.config;

import java.lang.reflect.Modifier;
import java.util.Arrays;

/** Runs against the real ModelConfig, not the API fixture. */
public final class ModelDisplayCopyTest {
    private static int checks;

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        ModelConfig source = new ModelConfig();
        source.frontManeDyeColors = new String[] {"#AABBCC", ""};
        source.backManeDyeColors = new String[] {"#112233"};
        source.tailDyeColors = new String[] {"#445566"};
        source.cutieMarkLeft = "left artwork";
        source.cutieMarkRight = "right artwork";
        ModelConfig copy = source.copyForDisplay();
        check(copy != source && copy.getClass() == source.getClass(), "distinct rendering copy");
        for (var field : ModelConfig.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            Object original = field.get(source), value = field.get(copy);
            if (field.getType() == String[].class) {
                check(Arrays.equals((String[]) original, (String[]) value), field.getName() + " values retained");
                check(original != value, field.getName() + " array detached");
                ((String[]) value)[0] = "changed only in display";
                check(!Arrays.equals((String[]) original, (String[]) value), field.getName() + " no mutation leak");
            } else {
                check(field.getType().isPrimitive() || field.getType() == String.class,
                        "new mutable fields need explicit display-copy isolation: " + field.getName());
                check(java.util.Objects.equals(original, value), field.getName() + " retained");
            }
        }
        copy.showHorn = !source.showHorn;
        copy.showWings = !source.showWings;
        check(copy.showHorn != source.showHorn && copy.showWings != source.showWings, "anatomy only changes display");
        source.frontManeDyeColors = null;
        source.backManeDyeColors = null;
        source.tailDyeColors = null;
        copy = source.copyForDisplay();
        check(copy.frontManeDyeColors == null && copy.backManeDyeColors == null && copy.tailDyeColors == null,
                "absent legacy dye arrays preserved without sanitizing source");
        System.out.println("Model display copy: " + checks + " checks passed");
    }
}
