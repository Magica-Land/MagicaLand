package top.csituka.magicaland.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class LatestAnimationUpdatesTest {
    private static final long MS = 1_000_000L;
    private static final UUID PLAYER = new UUID(1, 1), OTHER = new UUID(2, 2);
    private static final String BODY = "controller", BLINK = "blink_controller";
    private static final String EAR = "ear_controller", TAIL = "tail_controller";
    private static int checks;

    public static void main(String[] args) {
        delayedFinalState();
        controllersRemainIndependent();
        replacementsRemainFair();
        boundedFlood();
        lifecycle();
        independentPlayersAndRollover();
        System.out.println("PASS LatestAnimationUpdatesTest: " + checks + " timing/lifecycle checks");
    }

    private static void delayedFinalState() {
        var queue = queue();
        queue.offer(PLAYER, BODY, "run");
        expect(queue.poll(PLAYER, 0), BODY, "run");
        queue.offer(PLAYER, BODY, "walk");
        require(queue.poll(PLAYER, 46 * MS) == null, "network jitter retains state until budget available");
        queue.offer(PLAYER, BODY, "idle");
        expect(queue.poll(PLAYER, 50 * MS), BODY, "idle");
        require(queue.poll(PLAYER, 100 * MS) == null, "no replay of superseded walk");
        queue.offer(PLAYER, BLINK, "blink_parallel");
        expect(queue.poll(PLAYER, 100 * MS), BLINK, "blink_parallel");
        queue.offer(PLAYER, BLINK, "");
        require(queue.poll(PLAYER, 110 * MS) == null, "stop state may wait");
        expect(queue.poll(PLAYER, 150 * MS), BLINK, "");
    }

    private static void controllersRemainIndependent() {
        var queue = queue();
        queue.offer(PLAYER, BODY, "run");
        queue.poll(PLAYER, 0);
        queue.offer(PLAYER, BLINK, "blink_parallel");
        queue.offer(PLAYER, EAR, "ear_parallel");
        queue.offer(PLAYER, TAIL, "tail_parallel");
        queue.offer(PLAYER, BODY, "idle");
        for (String controller : List.of(BLINK, EAR, TAIL, BODY)) {
            long time = (List.of(BLINK, EAR, TAIL, BODY).indexOf(controller) + 1) * 50 * MS;
            var update = queue.poll(PLAYER, time);
            require(update != null && update.controller().equals(controller), "each controller retains own update");
            require(queue.poll(PLAYER, time) == null, "controllers share one player send budget");
        }
        require(queue.pendingPlayers().isEmpty(), "all four controllers drained");
    }

    private static void replacementsRemainFair() {
        var queue = queue();
        queue.offer(PLAYER, BODY, "idle");
        queue.poll(PLAYER, 0);
        queue.offer(PLAYER, BODY, "walk");
        queue.offer(PLAYER, BLINK, "blink_parallel");
        for (int i = 1; i <= 100; i++) {
            queue.offer(PLAYER, BODY, i == 100 ? "idle" : "run");
            if (i == 50) expect(queue.poll(PLAYER, i * MS), BODY, "run");
            if (i == 100) expect(queue.poll(PLAYER, i * MS), BLINK, "blink_parallel");
        }
        expect(queue.poll(PLAYER, 150 * MS), BODY, "idle");
    }

    private static void boundedFlood() {
        var queue = queue();
        List<Long> deliveries = new ArrayList<>();
        for (long ms = 0; ms < 1000; ms++) {
            for (String controller : List.of(BODY, BLINK, EAR, TAIL)) {
                queue.offer(PLAYER, controller, "state-" + ms);
            }
            var update = queue.poll(PLAYER, ms * MS);
            if (update != null) deliveries.add(ms);
        }
        require(deliveries.size() == 20, "flood limited to twenty updates per second per player");
        for (int i = 1; i < deliveries.size(); i++) {
            require(deliveries.get(i) - deliveries.get(i - 1) >= 50, "no interval breach");
        }
        Set<String> remaining = new java.util.HashSet<>();
        for (long ms = 1000; ms <= 1150; ms += 50) {
            var update = queue.poll(PLAYER, ms * MS);
            require(update != null && update.animation().equals("state-999"), "only latest per controller survives flood");
            remaining.add(update.controller());
        }
        require(remaining.size() == 4 && queue.pendingPlayers().isEmpty(), "queue bounded to four independent slots");
        for (int i = 0; i < 1000; i++) {
            require(!queue.offer(PLAYER, "unknown-" + i, "bad"), "unknown controllers cannot grow queue");
        }
        require(queue.pendingPlayers().isEmpty(), "rejected controller creates no player state");
    }

    private static void lifecycle() {
        for (String event : List.of("disconnect", "model_remove")) {
            var queue = queue();
            queue.offer(PLAYER, BODY, "run");
            queue.poll(PLAYER, 0);
            queue.offer(PLAYER, BODY, "idle");
            queue.remove(PLAYER);
            require(queue.poll(PLAYER, 50 * MS) == null, event + " cannot resurrect animation");
            queue.offer(PLAYER, BODY, "walk");
            expect(queue.poll(PLAYER, MS), BODY, "walk");
        }
        var queue = queue();
        queue.offer(PLAYER, BODY, "run");
        queue.poll(PLAYER, 0);
        queue.offer(PLAYER, BODY, "idle");
        queue.offer(OTHER, EAR, "ear_parallel");
        queue.clear();
        require(queue.pendingPlayers().isEmpty(), "server stop clears all pending players");
        queue.offer(PLAYER, BODY, "walk");
        expect(queue.poll(PLAYER, MS), BODY, "walk");
    }

    private static void independentPlayersAndRollover() {
        var queue = queue();
        queue.offer(PLAYER, BODY, "run");
        queue.poll(PLAYER, 0);
        queue.offer(PLAYER, BODY, "idle");
        queue.offer(OTHER, BODY, "walk");
        require(queue.poll(PLAYER, MS) == null, "first player waits");
        expect(queue.poll(OTHER, MS), BODY, "walk");
        queue.remove(PLAYER);
        queue.offer(OTHER, BODY, "idle");
        require(queue.poll(OTHER, 2 * MS) == null, "other lifecycle cannot reset own limit");
        queue.clear();
        queue.offer(PLAYER, BODY, "run");
        queue.poll(PLAYER, Long.MAX_VALUE - 25 * MS);
        queue.offer(PLAYER, BODY, "idle");
        require(queue.poll(PLAYER, Long.MIN_VALUE + 10 * MS) == null, "clock rollover retains interval");
        expect(queue.poll(PLAYER, Long.MIN_VALUE + 30 * MS), BODY, "idle");
    }

    private static LatestAnimationUpdates queue() {
        return new LatestAnimationUpdates(50 * MS, Set.of(BODY, BLINK, EAR, TAIL));
    }

    private static void expect(LatestAnimationUpdates.Update update, String controller, String animation) {
        require(update != null && update.controller().equals(controller) && update.animation().equals(animation),
                "expected " + controller + "=" + animation);
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
