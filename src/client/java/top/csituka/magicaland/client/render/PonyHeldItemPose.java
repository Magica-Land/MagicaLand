package top.csituka.magicaland.client.render;

import software.bernie.geckolib.cache.object.GeoBone;
import top.csituka.magicaland.client.animation.PonyFlightAnimations;

final class PonyHeldItemPose implements AutoCloseable {
    private final GeoBone bone;
    private final float rx, ry, rz, px, py, pz;
    private final boolean rotationChanged, positionChanged, scaleChanged;

    static PonyHeldItemPose apply(GeoBone bone, PonyHeldItems.Frame frame, Weights weights, boolean reRender) {
        if (reRender || frame.player() == null) return null;
        String name = bone.getName();
        if (name.equals("Head")) {
            float progress = frame.mouthSwing();
            if (!(progress > 0 && progress < 1)) return null;
            var saved = new PonyHeldItemPose(bone);
            float pitch = (float) Math.sin(progress * Math.PI) * (float) Math.toRadians(3);
            float yaw = (float) Math.sin(progress * Math.PI * 2) * (float) Math.toRadians(6);
            bone.updateRotation(saved.rx + pitch, saved.ry + (frame.mainLeft() ? -yaw : yaw), saved.rz);
            return saved;
        }
        if (!PonyFlightAnimations.isFrontLeg(name)) return null;
        float weight = name.startsWith("L") ? weights.left : weights.right;
        if (weight <= .001f) return null;
        var source = carryingPose(name, weights.usePitch(name.startsWith("L")));
        if (source == null) return null;
        var saved = new PonyHeldItemPose(bone);
        bone.updateRotation(lerp(saved.rx, source.rx(), weight), lerp(saved.ry, source.ry(), weight), lerp(saved.rz, source.rz(), weight));
        bone.updatePosition(lerp(saved.px, source.x(), weight), lerp(saved.py, source.y(), weight), lerp(saved.pz, source.z(), weight));
        return saved;
    }

    static PonyFlightAnimations.Limb carryingPose(String name) {
        return carryingPose(name, 0);
    }

    static PonyFlightAnimations.Limb carryingPose(String name, float usePitch) {
        var source = PonyFlightAnimations.limb(name);
        if (source == null || !PonyFlightAnimations.isFrontLeg(name)) return null;
        float factor = .95f;
        float rx = source.rx() * factor, ry = source.ry() * factor, rz = source.rz() * factor;
        if (PonyFlightAnimations.isFrontUpper(name)) {
            String side = name.substring(0, 1);
            var calf = PonyFlightAnimations.limb(side + "FrontCalf");
            var hoof = PonyFlightAnimations.limb(side + "FrontHoof");
            if (calf == null || hoof == null) return null;
            // 整条蜷腿转向前方、蹄底朝上，子关节角度及补偿保持成套。
            rx = -(float) Math.PI - (calf.rx() + hoof.rx()) * factor;
            // 只绕肩部抬动整条前腿，保留小腿与蹄子的角度、接缝补偿。
            rx -= Math.max(0, Math.min((float) Math.toRadians(32), usePitch));
            ry = name.startsWith("L") ? (float) Math.PI : -(float) Math.PI; rz = 0;
        }
        return new PonyFlightAnimations.Limb(rx, ry, rz, source.x() * factor, source.y() * factor, source.z() * factor);
    }

    private PonyHeldItemPose(GeoBone bone) {
        this.bone = bone;
        rx = bone.getRotX(); ry = bone.getRotY(); rz = bone.getRotZ();
        px = bone.getPosX(); py = bone.getPosY(); pz = bone.getPosZ();
        rotationChanged = bone.hasRotationChanged(); positionChanged = bone.hasPositionChanged(); scaleChanged = bone.hasScaleChanged();
    }
    @Override public void close() {
        bone.updateRotation(rx, ry, rz); bone.updatePosition(px, py, pz); bone.resetStateChanges();
        if (rotationChanged) bone.markRotationAsChanged();
        if (positionChanged) bone.markPositionAsChanged();
        if (scaleChanged) bone.markScaleAsChanged();
    }
    private static float lerp(float from, float to, float weight) { return from + (to - from) * weight; }

    static final class Weights {
        float left, right;
        private float leftUse, rightUse;
        private double previous = Double.NaN;
        void update(double tick, PonyHeldItems.Frame frame) {
            update(tick, frame.raises(true), frame.raises(false), frame.consumingPitch(true), frame.consumingPitch(false));
        }
        void update(double tick, boolean raiseLeft, boolean raiseRight) {
            update(tick, raiseLeft, raiseRight, 0, 0);
        }
        private void update(double tick, boolean raiseLeft, boolean raiseRight, float useLeft, float useRight) {
            if (!Double.isFinite(tick)) { reset(); return; }
            if (!Double.isFinite(previous) || tick < previous || tick - previous > 20) {
                left = raiseLeft ? 1 : 0; right = raiseRight ? 1 : 0;
                leftUse = useLeft; rightUse = useRight;
            } else {
                float step = (float) Math.min(1, Math.max(0, tick - previous) / 4);
                left += Math.max(-step, Math.min(step, (raiseLeft ? 1 : 0) - left));
                right += Math.max(-step, Math.min(step, (raiseRight ? 1 : 0) - right));
                float angleStep = (float) (Math.max(0, tick - previous) * Math.toRadians(10));
                leftUse += Math.max(-angleStep, Math.min(angleStep, useLeft - leftUse));
                rightUse += Math.max(-angleStep, Math.min(angleStep, useRight - rightUse));
            }
            previous = tick;
        }
        float usePitch(boolean leftArm) { return leftArm ? leftUse : rightUse; }
        void reset() { previous = Double.NaN; left = right = leftUse = rightUse = 0; }
    }
}
