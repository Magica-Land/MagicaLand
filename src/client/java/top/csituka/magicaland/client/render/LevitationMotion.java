package top.csituka.magicaland.client.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 仅驱动画面；不修改实体位置、物品使用状态或网络数据。 */
public final class LevitationMotion {
    public record Point(double x, double y, double z) {
        public Point add(Point p) { return new Point(x + p.x, y + p.y, z + p.z); }
        public Point subtract(Point p) { return new Point(x - p.x, y - p.y, z - p.z); }
        public Point multiply(double n) { return new Point(x * n, y * n, z * n); }
        public double length() { return Math.sqrt(x * x + y * y + z * z); }
        public boolean finite() { return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z); }
    }

    public record Profile(double frequency, double horizontalLimit, double verticalLimit,
            double angleLimit, double trailLife, double trailLength, double trailWidth) {}
    public static final Profile WORLD = new Profile(19, .20, .14, 8, .18, .42, .018);
    public static final Profile FIRST_PERSON = new Profile(24, .085, .065, 3, .10, .20, .010);
    public static final Point ZERO = new Point(0, 0, 0);
    private static final double DAMPING = .94;
    public record Pose(Point offset, double yaw, boolean reset) {}
    public record TrailPoint(Point position, float alpha) {}
    public record Key(UUID player, boolean mainHand, boolean firstPerson) {}

    private final Profile profile;
    private Point goal, position, velocity = ZERO, previousPlayer;
    private double heading, angle, angularVelocity, time;
    private boolean initialized;
    private final List<Stamp> trail = new ArrayList<>();
    private double lastTrailTime = -Double.MAX_VALUE;
    private record Stamp(Point point, double time) {}

    public LevitationMotion(Profile profile) { this.profile = profile; }

    public Pose sample(Point target, Point player, double yaw, double seconds, boolean using) {
        if (!target.finite() || !player.finite() || !Double.isFinite(yaw) || !Double.isFinite(seconds)) {
            clear();
            return new Pose(ZERO, 0, true);
        }
        double dt = seconds - time;
        if (!initialized || dt < 0 || dt > .25 || target.subtract(goal).length() > 2
                || player.subtract(previousPlayer).length() > 2 || Math.abs(wrap(yaw - heading)) > 100) {
            goal = position = target;
            previousPlayer = player;
            velocity = ZERO;
            heading = angle = yaw;
            angularVelocity = 0;
            time = seconds;
            initialized = true;
            trail.clear();
            lastTrailTime = -Double.MAX_VALUE;
            return new Pose(ZERO, 0, true);
        }
        if (dt > 1e-8) {
            double targetYaw = heading + wrap(yaw - heading);
            double frequency = profile.frequency * (using ? 1.65 : 1);
            double[] sx = spring(position.x, velocity.x, goal.x, target.x, dt, frequency);
            double[] sy = spring(position.y, velocity.y, goal.y, target.y, dt, frequency);
            double[] sz = spring(position.z, velocity.z, goal.z, target.z, dt, frequency);
            double[] sa = spring(angle, angularVelocity, heading, targetYaw, dt, frequency);
            position = new Point(sx[0], sy[0], sz[0]);
            velocity = new Point(sx[1], sy[1], sz[1]);
            angle = sa[0];
            angularVelocity = sa[1];
            heading = targetYaw;
            goal = target;
            previousPlayer = player;
            time = seconds;
        }
        double strength = using ? .22 : 1;
        Point offset = bounded(position.subtract(target), profile.horizontalLimit * strength,
                profile.verticalLimit * strength);
        if (offset.subtract(position.subtract(target)).length() > 1e-9) {
            position = target.add(offset);
            velocity = ZERO;
        }
        double yawOffset = Math.max(-profile.angleLimit * strength,
                Math.min(profile.angleLimit * strength, angle - heading));
        if (Math.abs(yawOffset - (angle - heading)) > 1e-9) {
            angle = heading + yawOffset;
            angularVelocity = 0;
        }
        return new Pose(offset, yawOffset, false);
    }

    // 对线性移动的目标解析积分，不依赖每帧固定 lerp 系数。
    private static double[] spring(double x, double v, double from, double to, double dt, double omega) {
        double targetVelocity = (to - from) / dt;
        double equilibrium = -2 * DAMPING * targetVelocity / omega;
        double displacement = x - from - equilibrium;
        double relativeVelocity = v - targetVelocity;
        double decayRate = DAMPING * omega;
        double oscillation = omega * Math.sqrt(1 - DAMPING * DAMPING);
        double sine = Math.sin(oscillation * dt), cosine = Math.cos(oscillation * dt);
        double b = (relativeVelocity + decayRate * displacement) / oscillation;
        double decay = Math.exp(-decayRate * dt);
        double relative = decay * (displacement * cosine + b * sine);
        double resultVelocity = decay * (-decayRate * (displacement * cosine + b * sine)
                + oscillation * (-displacement * sine + b * cosine));
        return new double[] {to + equilibrium + relative, targetVelocity + resultVelocity};
    }

    public List<TrailPoint> trail(Point actual, double seconds, boolean sprinting, double horizontalSpeed,
            boolean using) {
        trail.removeIf(stamp -> seconds - stamp.time > profile.trailLife || seconds < stamp.time);
        boolean emit = actual.finite() && initialized && sprinting && horizontalSpeed > .6 && !using;
        if (emit && seconds - lastTrailTime >= .025) {
            if (trail.isEmpty() || actual.subtract(trail.get(trail.size() - 1).point).length() >= .018) {
                trail.add(new Stamp(actual, seconds));
                lastTrailTime = seconds;
            }
        }
        while (trail.size() > 8) trail.remove(0);
        if (trail.isEmpty()) return List.of();
        List<TrailPoint> result = new ArrayList<>();
        Stamp head = emit ? new Stamp(actual, seconds) : trail.get(trail.size() - 1);
        result.add(new TrailPoint(head.point, alpha(seconds, head.time)));
        double length = 0;
        Stamp previous = head;
        for (int i = trail.size() - 1; i >= 0; i--) {
            Stamp stamp = trail.get(i);
            double segment = stamp.point.subtract(previous.point).length();
            if (segment < .000001) continue;
            if (length + segment > profile.trailLength) {
                double remaining = profile.trailLength - length;
                if (remaining > .000001) {
                    double fraction = remaining / segment;
                    Point clipped = previous.point.add(stamp.point.subtract(previous.point).multiply(fraction));
                    double clippedTime = previous.time + (stamp.time - previous.time) * fraction;
                    result.add(0, new TrailPoint(clipped, alpha(seconds, clippedTime)));
                }
                break;
            }
            length += segment;
            result.add(0, new TrailPoint(stamp.point, alpha(seconds, stamp.time)));
            previous = stamp;
        }
        return List.copyOf(result);
    }

    private float alpha(double now, double emitted) {
        double life = Math.max(0, Math.min(1, 1 - (now - emitted) / profile.trailLife));
        return (float) (.16 * life * life);
    }

    public void clear() {
        initialized = false;
        goal = position = previousPlayer = null;
        velocity = ZERO;
        trail.clear();
        lastTrailTime = -Double.MAX_VALUE;
    }

    static Point bounded(Point p, double horizontal, double vertical) {
        double horizontalLength = Math.hypot(p.x, p.z);
        double scale = horizontalLength > horizontal ? horizontal / horizontalLength : 1;
        return new Point(p.x * scale, Math.max(-vertical, Math.min(vertical, p.y)), p.z * scale);
    }

    static double wrap(double degrees) { return degrees - Math.floor((degrees + 180) / 360) * 360; }

    public static final class Store {
        private final Map<Key, Entry> entries = new HashMap<>();
        private Object world;
        private boolean firstPerson;
        private long frame;

        public void beginFrame(Object world, boolean firstPerson) {
            if (this.world != world || this.firstPerson != firstPerson) clear();
            this.world = world;
            this.firstPerson = firstPerson;
            frame++;
            entries.values().removeIf(entry -> frame - entry.lastSeen > 120);
        }

        public Entry acquire(Key key, String item, Profile profile) {
            Entry entry = entries.get(key);
            if (entry == null || !entry.item.equals(item)) {
                if (entries.size() >= 512) entries.clear();
                entry = new Entry(new LevitationMotion(profile), item);
                entries.put(key, entry);
            } else if (entry.lastSeen < frame - 1) {
                entry.motion.clear();
                entry.previousPlayer = null;
                entry.previousTime = entry.speed = 0;
                entry.updatedFrame = Long.MIN_VALUE;
                entry.pose = null;
                entry.trailFrame = Long.MIN_VALUE;
                entry.trail = List.of();
            }
            entry.lastSeen = frame;
            return entry;
        }

        public Pose sample(Entry entry, Point target, Point player, double yaw, double seconds, boolean using) {
            if (entry.updatedFrame != frame) {
                double elapsed = seconds - entry.previousTime;
                entry.speed = entry.previousPlayer != null && elapsed > 0 && elapsed <= .25
                        ? Math.hypot(player.x - entry.previousPlayer.x, player.z - entry.previousPlayer.z) / elapsed : 0;
                entry.pose = entry.motion.sample(target, player, yaw, seconds, using);
                if (entry.pose.reset()) entry.speed = 0;
                entry.previousPlayer = player;
                entry.previousTime = seconds;
                entry.updatedFrame = frame;
            }
            return entry.pose;
        }

        public List<TrailPoint> recordTrail(Entry entry, Point actual, double seconds, boolean sprinting, boolean using) {
            if (entry.trailFrame != frame) {
                entry.trail = entry.motion.trail(actual, seconds, sprinting, entry.speed, using);
                entry.trailFrame = frame;
            }
            return entry.trail;
        }

        public void remove(Key key) { entries.remove(key); }
        public void clear() { entries.clear(); }
        public int size() { return entries.size(); }
        public long frame() { return frame; }
    }

    public static final class Entry {
        final LevitationMotion motion;
        final String item;
        long lastSeen, updatedFrame = Long.MIN_VALUE;
        Pose pose;
        Point previousPlayer;
        double previousTime, speed;
        long trailFrame = Long.MIN_VALUE;
        List<TrailPoint> trail = List.of();
        Entry(LevitationMotion motion, String item) { this.motion = motion; this.item = item; }
    }
}
