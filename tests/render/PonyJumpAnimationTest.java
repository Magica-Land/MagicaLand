import java.nio.file.Files;
import java.nio.file.Path;
import top.csituka.magicaland.client.animation.PonyJumpAnimation;

public final class PonyJumpAnimationTest {
    public static void main(String[] args) throws Exception {
        PonyJumpAnimation jump = new PonyJumpAnimation();
        jump.update(0, 0, false);
        check(!jump.isJumping(0, 0), "ground is not a jump");
        for (int tick = 1; tick <= 5; tick++) {
            jump.update(tick, 0.42 - tick * 0.07, true);
            check(jump.isJumping(tick, 0.42 - tick * 0.07), "rising pose");
        }
        for (int tick = 6; tick < 15; tick++) {
            for (int frame = 0; frame < 4; frame++) {
                jump.update(tick, -0.1, true);
                check(jump.isJumping(tick, -0.1), "apex hold is tick-based, not frame-based");
            }
        }
        for (int tick = 15; tick < 100; tick++) {
            jump.update(tick, -1, true);
            check(!jump.isJumping(tick, -1), "long descent must release jump pose");
        }
        jump.update(100, 0.4, true);
        check(jump.isJumping(100, 0.4), "bounce can start another jump");
        jump.update(101, 0, false);
        jump.update(102, -0.1, true);
        check(!jump.isJumping(102, -0.1), "landing, water or flight clears jump memory");
        jump.update(110, 0.4, true);
        jump.update(1, -0.1, true);
        check(!jump.isJumping(1, -0.1), "entity age reset clears jump memory");
        PonyJumpAnimation walkingOffEdge = new PonyJumpAnimation();
        walkingOffEdge.update(200, -0.2, true);
        check(!walkingOffEdge.isJumping(200, -0.2), "walking off an edge does not invent a jump");
        check(!jump.isJumping(201, -0.2), "players keep independent jump state");

        String source = Files.readString(Path.of(args[0],
                "src/client/java/top/csituka/magicaland/client/model/GeckoPlayerAnimatable.java"));
        check(source.contains("fallState.jump.update(player.age, player.getVelocity().y,")
                && source.contains("!isOnGround && !flying && !player.isTouchingWater()"),
                "live animation updates the jump state only for normal airborne movement");
        check(source.split("fallState.jump.isJumping", -1).length == 3,
                "main animation and idle detection share the same bounded jump state");
        check(source.contains("new AnimationSelection(\"fall_transfer\", FALL_TRANSFER_ANIM)"),
                "fall transition and existing network animation name are preserved");
        System.out.println("PonyJumpAnimationTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
