package top.csituka.magicaland.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class TransformationMessageTest {
    private static int checks;

    public static void main(String[] args) {
        for (String value : new String[] {"{}", "{\"transform\":false}", "{\"transform\":\"true\"}",
                "{\"transform\":1}", "{\"transform\":null}", "{\"transform\":[]}", "{\"transform\":{}}"}) {
            require(!TransformationMessage.requested(object(value)), "invalid/old flag: " + value);
        }
        JsonObject transformed = object("{\"transform\":true}");
        require(TransformationMessage.requested(transformed), "boolean flag accepted");
        require(!TransformationMessage.requested(null), "null message safe");
        require(!TransformationMessage.modelChanged("{}", "[]"), "array rejected");
        require(!TransformationMessage.modelChanged("{}", "broken"), "malformed rejected");
        require(!TransformationMessage.modelChanged("{}", null), "null rejected");
        require(TransformationMessage.modelChanged(null, "{\"bodyColor\":\"white\"}"), "first accepted update");
        require(!TransformationMessage.modelChanged("{\"name\":\"a\",\"bodyColor\":1}",
                "{ \"bodyColor\" : 1, \"name\" : \"b\" }"), "rename/JSON reorder cannot trigger");
        require(!TransformationMessage.modelChanged("{\"frontManeDyeColors\":[1,2]}",
                "{ \"frontManeDyeColors\": [1,2] }"), "nested equality");
        require(TransformationMessage.modelChanged("{\"frontManeDyeColors\":[1,2]}",
                "{\"frontManeDyeColors\":[2,1]}"), "slot changes recognized");
        require(!TransformationMessage.modelChanged("{\"bodyColor\":1,\"random\":1}",
                "{\"bodyColor\":1,\"random\":2}"), "unknown fields cannot trigger");
        require(!TransformationMessage.modelChanged(null, "{}"), "empty model cannot trigger");
        require(TransformationMessage.mayBroadcast(transformed, "{}", "{\"bodyColor\":1}", null, 0), "first flag");
        require(!TransformationMessage.mayBroadcast(transformed, "{}", "{}", null, 0), "same model no flag");
        require(!TransformationMessage.mayBroadcast(object("{}"), "{}", "{\"bodyColor\":1}", null, 0), "history no flag");
        require(!TransformationMessage.mayBroadcast(transformed, "{}", "{\"bodyColor\":1}", 0L, 999_999_999L), "cooldown");
        require(TransformationMessage.mayBroadcast(transformed, "{}", "{\"bodyColor\":1}", 0L, 1_000_000_000L), "cooldown boundary");
        require(!TransformationMessage.mayBroadcast(transformed, "{}", "{\"bodyColor\":1}", 5L, 4L), "clock regression safe");
        require(TransformationMessage.mayBroadcast(transformed, "{}", "{\"bodyColor\":1}", Long.MAX_VALUE - 99,
                Long.MIN_VALUE + 1_000_000_000L), "nanoTime rollover");
        System.out.println("PASS TransformationMessageTest: " + checks + " protocol and cooldown checks");
    }

    private static JsonObject object(String text) { return JsonParser.parseString(text).getAsJsonObject(); }
    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
