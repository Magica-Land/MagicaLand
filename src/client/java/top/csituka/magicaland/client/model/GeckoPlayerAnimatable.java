package top.csituka.magicaland.client.model;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GeckoPlayerAnimatable implements GeoAnimatable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private AbstractClientPlayerEntity player;

    private static final RawAnimation FLY_ANIM = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation ELYTRA_FLY_ANIM = RawAnimation.begin().thenLoop("elytra_fly");
    private static final RawAnimation SWIM_ANIM = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation SWIM_HOLD_ANIM = RawAnimation.begin().thenLoop("swim_hold");
    private static final RawAnimation SNEAK_ANIM = RawAnimation.begin().thenLoop("sneak");
    private static final RawAnimation SNEAKING_ANIM = RawAnimation.begin().thenLoop("sneaking");
    private static final RawAnimation RUN_ANIM = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation BACKWARD_WALK_ANIM = RawAnimation.begin().thenLoop("backward_walk");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation BLINK_ANIM = RawAnimation.begin().thenLoop("blink_parallel");
    private static final RawAnimation EAR_ANIM = RawAnimation.begin().thenLoop("ear_parallel");
    private static final RawAnimation TAIL_ANIM = RawAnimation.begin().thenLoop("tail_parallel");
    private static final RawAnimation ATTACKED_ANIM = RawAnimation.begin().thenPlay("attacked");
    private static final RawAnimation JUMP_ANIM = RawAnimation.begin().thenPlayAndHold("jump1");

    private static final RawAnimation FALL_TRANSFER_ANIM = RawAnimation.begin().thenPlay("fall_transfer")
            .thenLoop("fall");
    private static final RawAnimation LAND_ONLY_ANIM = RawAnimation.begin().thenPlay("land");
    private static final RawAnimation LARGER_LAND_ONLY_ANIM = RawAnimation.begin().thenPlay("larger_land");

    private static class PlayerFallState {
        float maxFallDistance = 0;
        int fallStartTime = -1;
        int landStartTime = -1;
        int jumpStartTime = -1;
        boolean landed = false;
        boolean isLarge = false;
        boolean wasOnGround = true;
    }

    private final Map<UUID, PlayerFallState> fallStates = new HashMap<>();

    public GeckoPlayerAnimatable() {
    }

    public void setPlayer(AbstractClientPlayerEntity player) {
        this.player = player;
    }

    public AbstractClientPlayerEntity getPlayer() {
        return player;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 3, this::predicate));
        controllers.add(new AnimationController<>(this, "blink_controller", 3, this::blinkPredicate));
        controllers.add(new AnimationController<>(this, "ear_controller", 3, this::earPredicate));
        controllers.add(new AnimationController<>(this, "tail_controller", 3, this::tailPredicate));
    }

    private boolean isIdle(AnimationState<GeckoPlayerAnimatable> state) {
        if (player == null)
            return false;

        if (player.hurtTime > 0)
            return false;

        PlayerFallState fallState = fallStates.computeIfAbsent(player.getUuid(), k -> new PlayerFallState());
        boolean isOnGround = player.isOnGround();
        boolean moving = state.isMoving() || player.forwardSpeed != 0 || player.sidewaysSpeed != 0;

        if (player.getAbilities().flying)
            return false;
        if (player.isTouchingWater() && moving)
            return false;
        if (!isOnGround && !player.isTouchingWater() && !player.getAbilities().flying && (player.getVelocity().y > 0 || player.fallDistance > 0.1f || (fallState.jumpStartTime != -1 && player.age - fallState.jumpStartTime < 10)))
            return false;
        if (fallState.landed)
            return false;
        if (player.isSneaking())
            return false;
        if (player.isSprinting())
            return false;
        if (player.forwardSpeed < 0)
            return false;
        if (moving)
            return false;

        return true;
    }

    private PlayState blinkPredicate(AnimationState<GeckoPlayerAnimatable> state) {
        if (player == null)
            return PlayState.STOP;
        state.getController().setAnimation(BLINK_ANIM);
        return PlayState.CONTINUE;
    }

    private PlayState earPredicate(AnimationState<GeckoPlayerAnimatable> state) {
        if (!isIdle(state))
            return PlayState.STOP;
        state.getController().setAnimation(EAR_ANIM);
        return PlayState.CONTINUE;
    }

    private PlayState tailPredicate(AnimationState<GeckoPlayerAnimatable> state) {
        if (!isIdle(state))
            return PlayState.STOP;
        state.getController().setAnimation(TAIL_ANIM);
        return PlayState.CONTINUE;
    }

    private PlayState predicate(AnimationState<GeckoPlayerAnimatable> state) {
        if (player == null)
            return PlayState.STOP;

        if (player.hurtTime > 0) {
            state.getController().setAnimation(ATTACKED_ANIM);
            return PlayState.CONTINUE;
        }

        PlayerFallState fallState = fallStates.computeIfAbsent(player.getUuid(), k -> new PlayerFallState());
        boolean isOnGround = player.isOnGround();
        boolean moving = state.isMoving() || player.forwardSpeed != 0 || player.sidewaysSpeed != 0;

        if (!isOnGround && !player.getAbilities().flying && !player.isTouchingWater()) {
            fallState.maxFallDistance = Math.max(fallState.maxFallDistance, player.fallDistance);
            if (player.fallDistance > 0.1f && fallState.fallStartTime == -1) {
                fallState.fallStartTime = player.age;
            }
            if (player.getVelocity().y > 0) {
                fallState.jumpStartTime = player.age;
            }
        } else {
            fallState.fallStartTime = -1;
            fallState.jumpStartTime = -1;
        }

        if (isOnGround && !fallState.wasOnGround) {
            if (fallState.maxFallDistance > 0.5f) {
                fallState.landed = true;
                fallState.isLarge = fallState.maxFallDistance >= 3.0f;
                fallState.landStartTime = player.age;
            }
            fallState.maxFallDistance = 0;
        }

        fallState.wasOnGround = isOnGround;

        if (moving || player.isSneaking() || player.getAbilities().flying || player.isTouchingWater()) {
            fallState.landed = false;
        }

        if (player.getAbilities().flying) {
            if (player.isSprinting()) {
                state.getController().setAnimation(ELYTRA_FLY_ANIM);
            } else {
                state.getController().setAnimation(FLY_ANIM);
            }
            return PlayState.CONTINUE;
        }

        if (player.isTouchingWater() && moving) {
            if (player.isSprinting()) {
                state.getController().setAnimation(SWIM_ANIM);
            } else {
                state.getController().setAnimation(SWIM_HOLD_ANIM);
            }
            return PlayState.CONTINUE;
        }

        if (!isOnGround && !player.isTouchingWater() && !player.getAbilities().flying) {
            if (player.getVelocity().y > 0) {
                state.getController().setAnimation(JUMP_ANIM);
                return PlayState.CONTINUE;
            } else if (fallState.jumpStartTime != -1) {
                state.getController().setAnimation(JUMP_ANIM);
                return PlayState.CONTINUE;
            } else if (player.fallDistance > 0.1f && fallState.fallStartTime != -1) {
                state.getController().setAnimation(FALL_TRANSFER_ANIM);
                return PlayState.CONTINUE;
            }
        }

        if (fallState.landed) {
            int landDuration = fallState.isLarge ? 20 : 10;
            if (player.age - fallState.landStartTime < landDuration) {
                if (fallState.isLarge) {
                    state.getController().setAnimation(LARGER_LAND_ONLY_ANIM);
                } else {
                    state.getController().setAnimation(LAND_ONLY_ANIM);
                }
                return PlayState.CONTINUE;
            } else {
                fallState.landed = false;
            }
        }

        if (player.isSneaking()) {
            if (moving) {
                state.getController().setAnimation(SNEAK_ANIM);
            } else {
                state.getController().setAnimation(SNEAKING_ANIM);
            }
            return PlayState.CONTINUE;
        }

        if (player.isSprinting()) {
            state.getController().setAnimation(RUN_ANIM);
            return PlayState.CONTINUE;
        }

        if (player.forwardSpeed < 0) {
            state.getController().setAnimation(BACKWARD_WALK_ANIM);
            return PlayState.CONTINUE;
        }

        if (moving) {
            state.getController().setAnimation(WALK_ANIM);
            return PlayState.CONTINUE;
        }

        state.getController().setAnimation(IDLE_ANIM);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public double getTick(Object o) {
        return player != null ? player.age : 0;
    }
}
