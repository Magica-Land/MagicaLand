package top.csituka.magicaland.client.animation;

import java.util.Map;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.state.BoneSnapshot;

/** 仅主动作使用连续的局部时钟；Gecko 的直接倍率会重新缩放已播放时间。 */
public class PonySneakController<T extends GeoAnimatable> extends AnimationController<T> {
    private static final double NORMAL_SNEAK_LIMB_SPEED = .26, MIN_SPEED = .25;
    private double previousTick = Double.NaN, playbackTick, playbackSpeed = 1;

    public PonySneakController(T animatable, String name, int transitionTicks, AnimationStateHandler<T> handler) {
        super(animatable, name, transitionTicks, handler);
    }

    public static double speed(String action, double limbSpeed) {
        if (!"sneak".equals(action) || !Double.isFinite(limbSpeed)) return 1;
        return Math.max(MIN_SPEED, Math.min(1, limbSpeed / NORMAL_SNEAK_LIMB_SPEED));
    }

    public void setPlaybackSpeed(double speed) {
        playbackSpeed = Double.isFinite(speed) ? Math.max(MIN_SPEED, Math.min(1, speed)) : 1;
    }

    @Override
    public void process(CoreGeoModel<T> model, AnimationState<T> state, Map<String, CoreGeoBone> bones,
                        Map<String, BoneSnapshot> snapshots, double tick, boolean crashOnMissingBone) {
        if (Double.isFinite(tick) && (!Double.isFinite(previousTick) || tick > previousTick)) {
            if (!Double.isFinite(previousTick)) playbackTick = tick;
            else playbackTick += (tick - previousTick) * playbackSpeed;
            previousTick = tick;
        }
        // 所有循环、过渡与关键帧比较都使用同一时间轴，不改 Gecko 的重置语义。
        super.process(model, state, bones, snapshots, playbackTick, crashOnMissingBone);
    }
}
