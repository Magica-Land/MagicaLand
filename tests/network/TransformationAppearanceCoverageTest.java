package top.csituka.magicaland.network;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public final class TransformationAppearanceCoverageTest {
    public static void main(String[] args) throws Exception {
        Class<?> modelType = Class.forName("top.csituka.magicaland.client.config.ModelConfig", false,
                TransformationAppearanceCoverageTest.class.getClassLoader());
        Set<String> modelFields = new HashSet<>();
        for (var field : modelType.getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())
                    && !field.getName().equals("name")) modelFields.add(field.getName());
        }
        var field = TransformationMessage.class.getDeclaredField("APPEARANCE_FIELDS");
        field.setAccessible(true);
        if (!modelFields.equals(field.get(null))) {
            throw new AssertionError("ModelConfig changed: update TransformationMessage appearance field allowlist");
        }
        System.out.println("PASS TransformationAppearanceCoverageTest: all " + modelFields.size() + " current appearance fields covered");
    }
}
