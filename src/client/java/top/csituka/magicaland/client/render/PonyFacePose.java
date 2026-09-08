package top.csituka.magicaland.client.render;

import software.bernie.geckolib.cache.object.GeoBone;
import top.csituka.magicaland.client.animation.PonyExpressions;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/** 只适配已有表情轨道；所有临时修改在本次面部渲染后恢复。 */
final class PonyFacePose implements AutoCloseable {
    private final Map<String, GeoBone> bones = new HashMap<>();
    private final Map<GeoBone, SavedPose> saved = new IdentityHashMap<>();

    private PonyFacePose(GeoBone root) {
        collect(root);
    }

    static PonyFacePose apply(GeoBone root) {
        PonyFacePose pose = new PonyFacePose(root);
        if (pose.isVisible("Smeile") || pose.isVisible("close") || pose.isVisible("ScrunchedEyes")) {
            // 动作已经指定闭眼时，不让普通眨眼替换其眼型。
            pose.scale("emot", 1);
            pose.scale("shut", 0);
        }

        for (var style : PonyExpressions.eyeStyles().values()) {
            style.bones().forEach((channel, target) -> pose.copy(PonyExpressions.CHANNELS.get(channel), target,
                    channel.endsWith("_pupil")));
        }
        return pose;
    }

    static boolean shouldRender(String boneName, String eyeStyle) {
        return PonyExpressions.shouldRender(boneName, eyeStyle);
    }

    private void collect(GeoBone bone) {
        bones.put(bone.getName(), bone);
        for (GeoBone child : bone.getChildBones()) {
            collect(child);
        }
    }

    private boolean isVisible(String name) {
        GeoBone bone = bones.get(name);
        return bone != null && !bone.isHidden()
                && bone.getScaleX() > 0.001f && bone.getScaleY() > 0.001f && bone.getScaleZ() > 0.001f;
    }

    private void scale(String name, float value) {
        GeoBone bone = bones.get(name);
        if (bone == null) return;
        save(bone);
        bone.updateScale(value, value, value);
    }

    private void copy(String sourceName, String targetName, boolean copyPivot) {
        GeoBone source = bones.get(sourceName);
        GeoBone target = bones.get(targetName);
        if (source == null || target == null || source == target) return;
        save(target);
        target.updateScale(source.getScaleX(), source.getScaleY(), source.getScaleZ());
        target.updatePosition(source.getPosX(), source.getPosY(), source.getPosZ());
        target.updateRotation(source.getRotX(), source.getRotY(), source.getRotZ());
        if (copyPivot) {
            // 备用眼仁原 pivot 在原点；缩放时借用同侧眼仁的轴心，避免漂移。
            target.updatePivot(source.getPivotX(), source.getPivotY(), source.getPivotZ());
        }
    }

    private void save(GeoBone bone) {
        saved.computeIfAbsent(bone, SavedPose::capture);
    }

    @Override
    public void close() {
        saved.forEach((bone, pose) -> pose.restore(bone));
    }

    private record SavedPose(float sx, float sy, float sz, float x, float y, float z,
                             float rx, float ry, float rz, float px, float py, float pz,
                             boolean scaleChanged, boolean positionChanged, boolean rotationChanged) {
        static SavedPose capture(GeoBone bone) {
            return new SavedPose(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ(),
                    bone.getPosX(), bone.getPosY(), bone.getPosZ(),
                    bone.getRotX(), bone.getRotY(), bone.getRotZ(),
                    bone.getPivotX(), bone.getPivotY(), bone.getPivotZ(),
                    bone.hasScaleChanged(), bone.hasPositionChanged(), bone.hasRotationChanged());
        }

        void restore(GeoBone bone) {
            bone.updateScale(sx, sy, sz);
            bone.updatePosition(x, y, z);
            bone.updateRotation(rx, ry, rz);
            bone.updatePivot(px, py, pz);
            bone.resetStateChanges();
            if (scaleChanged) bone.markScaleAsChanged();
            if (positionChanged) bone.markPositionAsChanged();
            if (rotationChanged) bone.markRotationAsChanged();
        }
    }
}
