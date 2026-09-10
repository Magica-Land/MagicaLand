package top.csituka.magicaland.client.render;

import java.util.UUID;
import static top.csituka.magicaland.client.render.LevitationMotion.*;

public final class LevitationVisualIsolationTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        var body = store("STATES");
        var visual = store("VISUAL_STATES");
        check(body != visual, "temporary visual state never aliases normal body state");
        Object world = new Object();
        body.beginFrame(world, true);
        visual.beginFrame(world, true);
        Key ownerKey = new Key(new UUID(1, 1), true, true);
        Key sourceKey = new Key(new UUID(2, 2), true, true);
        Entry owner = body.acquire(ownerKey, "item", FIRST_PERSON);
        Pose before = body.sample(owner, ZERO, ZERO, 0, 10, false);
        Entry projection = visual.acquire(sourceKey, "item", FIRST_PERSON);
        visual.sample(projection, new Point(20, 5, 20), new Point(20, 5, 20), 45, 1, true);
        check(owner != projection && before == owner.pose, "remote pose, age and action cannot overwrite body sample");
        check(visual.acquire(ownerKey, "item", FIRST_PERSON) != owner,
                "explicit visual with owner's UUID still cannot borrow body animation");
        check(visual.acquire(new Key(sourceKey.player(), true, false), "item", WORLD) != projection,
                "projection first and third person remain separate");
        visual.beginFrame(world, true);
        visual.sample(visual.acquire(sourceKey, "item", FIRST_PERSON), new Point(20.2, 5, 20),
                new Point(20.2, 5, 20), 50, 1.05, false);
        check(owner.pose == before && owner.previousTime == 10, "remote updates never advance body clock");
        visual.remove(sourceKey);
        check(body.acquire(ownerKey, "item", FIRST_PERSON) == owner, "ending projection preserves body cache");
        ItemLevitation.clear();
        check(body.size() == 0 && visual.size() == 0, "world/disconnect cleanup clears both stores");
        System.out.println("PASS visual levitation isolation: " + checks + " checks");
    }

    private static Store store(String name) throws Exception {
        var field = ItemLevitation.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Store) field.get(null);
    }

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
}
