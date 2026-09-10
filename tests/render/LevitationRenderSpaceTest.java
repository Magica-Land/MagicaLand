package top.csituka.magicaland.client.render;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class LevitationRenderSpaceTest {
    private static int checks;
    private static final float EPS = .00005f;

    public static void main(String[] args) {
        cameraAnglesAndEffects();
        movingCamera();
        System.out.println("PASS LevitationRenderSpaceTest: " + checks + " checks");
    }

    private static void cameraAnglesAndEffects() {
        for (int yaw = -360; yaw <= 360; yaw += 15) {
            for (int pitch = -90; pitch <= 90; pitch += 10) {
                for (int effect = 0; effect < 4; effect++) {
                    Matrix4f view = camera(yaw, pitch, effect, .37f);
                    Vector3f anchor = new Vector3f(3.2f, -1.7f, 7.25f);
                    Matrix4f item = new Matrix4f(view).translate(anchor)
                            .rotateXYZ(.28f, -.73f, .42f).scale(.85f, 1.1f, -.7f);
                    Matrix4f savedView = new Matrix4f(view), savedItem = new Matrix4f(item);
                    near(LevitationRenderSpace.anchor(view, item), anchor, "camera-independent anchor");
                    check(view.equals(savedView) && item.equals(savedItem), "anchor query preserves input matrices");

                    Vector3f offset = new Vector3f(.23f, -.41f, .19f);
                    Vector3f local = new Matrix3f(view).transform(new Vector3f(offset));
                    new Matrix3f(item).invert().transform(local);
                    Matrix4f shifted = new Matrix4f(item).translate(local);
                    near(LevitationRenderSpace.anchor(view, shifted), new Vector3f(anchor).add(offset),
                            "world motion maps through render/local bases consistently");

                    Vector3f relativeCenter = new Vector3f(.18f, .24f, -.09f);
                    Vector3f renderedCenter = item.transformPosition(relativeCenter, new Vector3f());
                    Vector3f expectedCenter = new Matrix4f().translate(anchor)
                            .rotateXYZ(.28f, -.73f, .42f).scale(.85f, 1.1f, -.7f)
                            .transformPosition(relativeCenter, new Vector3f());
                    near(new Matrix4f(view).invert().transformPosition(renderedCenter), expectedCenter,
                            "deferred geometry/trail center includes local display offset");
                }
            }
        }
    }

    private static void movingCamera() {
        double targetX = 1234567.125, targetY = 89.75, targetZ = -2345678.5;
        Vector3f stableOffset = new Vector3f(.27f, .1f, -.36f);
        Matrix4f entityPose = new Matrix4f().rotateXYZ(.2f, -.9f, .1f);
        Vector3f worldOffset = entityPose.transformDirection(stableOffset, new Vector3f());
        double previousX = 0, previousY = 0, previousZ = 0;
        for (int frame = 0; frame <= 4000; frame++) {
            double t = frame / 144.0;
            double cameraX = targetX + Math.cos(t) * 6.5;
            double cameraY = targetY + 2 + Math.sin(t * .7);
            double cameraZ = targetZ + Math.sin(t) * 6.5;
            Matrix4f view = camera((float) (t * 127), (float) (Math.sin(t) * 80), 3, (float) t);
            Matrix4f item = new Matrix4f(view)
                    .translate((float) (targetX - cameraX), (float) (targetY - cameraY), (float) (targetZ - cameraZ))
                    .mul(entityPose).translate(stableOffset).scale(.85f);
            Vector3f relative = LevitationRenderSpace.anchor(view, item);
            double actualX = cameraX + relative.x, actualY = cameraY + relative.y, actualZ = cameraZ + relative.z;
            near(actualX, targetX + worldOffset.x, "nonzero camera origin x");
            near(actualY, targetY + worldOffset.y, "nonzero camera origin y");
            near(actualZ, targetZ + worldOffset.z, "nonzero camera origin z");
            if (frame > 0) {
                near(actualX, previousX, "moving camera cannot add x jitter");
                near(actualY, previousY, "moving camera cannot add y jitter");
                near(actualZ, previousZ, "moving camera cannot add z jitter");
            }
            previousX = actualX; previousY = actualY; previousZ = actualZ;
        }
    }

    private static Matrix4f camera(float yaw, float pitch, int effect, float phase) {
        Matrix4f result = new Matrix4f();
        if (effect >= 1) result.rotateY(.12f).rotateZ((float) Math.sin(phase) * .23f).rotateY(-.12f);
        if (effect >= 2) result.translate((float) Math.sin(phase) * .08f, -.045f, .013f)
                .rotateZ((float) Math.sin(phase) * .03f).rotateX(.017f);
        if (effect >= 3) result.rotateY(.37f).scale(.93f, 1.07f, .97f).rotateY(-.37f);
        return result.rotateX((float) Math.toRadians(pitch)).rotateY((float) Math.toRadians(yaw + 180));
    }

    private static void near(Vector3f actual, Vector3f expected, String reason) {
        check(actual.isFinite() && actual.distance(expected) < EPS, reason + ": " + actual + " != " + expected);
    }

    private static void near(double actual, double expected, String reason) {
        check(Double.isFinite(actual) && Math.abs(actual - expected) < EPS, reason + ": " + actual + " != " + expected);
    }

    private static void check(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason);
    }
}
