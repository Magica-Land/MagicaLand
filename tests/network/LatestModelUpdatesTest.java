package top.csituka.magicaland.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class LatestModelUpdatesTest {
    private static final long MS = 1_000_000L;
    private static final UUID PLAYER = new UUID(1, 1);
    private static final UUID OTHER = new UUID(2, 2);
    private static int checks;

    public static void main(String[] args) {
        jitteredFinalSave();
        replaceBeforeApplying();
        noResurrection();
        independentPlayers();
        floodStillLimited();
        rollover();
        System.out.println("PASS LatestModelUpdatesTest: " + checks + " queue/timing/lifecycle checks");
    }

    private static void jitteredFinalSave() {
        var queue = queue();
        queue.offer(PLAYER, "initial", false);
        require(queue.poll(PLAYER, 0).modelData().equals("initial"), "initial immediate");
        // 客户端已隔100ms发送，服务端因网络抖动仅隔96ms收到。
        queue.offer(PLAYER, "final", true);
        require(queue.poll(PLAYER, 96 * MS) == null, "jittered save retained, not early");
        require(queue.pendingPlayers().equals(List.of(PLAYER)), "one pending per player");
        var applied = queue.poll(PLAYER, 150 * MS);
        require(applied.modelData().equals("final") && applied.transformation(), "next tick delivers final+flag");
        require(queue.poll(PLAYER, 200 * MS) == null, "no duplicate publication");
    }

    private static void replaceBeforeApplying() {
        var queue = queue();
        queue.offer(PLAYER, "initial", false);
        queue.poll(PLAYER, 0);
        queue.offer(PLAYER, "old", true);
        require(queue.poll(PLAYER, 50 * MS) == null, "old pending is throttled");
        queue.offer(PLAYER, "latest", false);
        var latest = queue.poll(PLAYER, 100 * MS);
        require(latest.modelData().equals("latest"), "new packet replaces old before drain");
        require(!latest.transformation(), "old transformation cannot attach to other model");
        queue.offer(PLAYER, "same", true);
        queue.offer(PLAYER, "same", false);
        var same = queue.poll(PLAYER, 200 * MS);
        require(same.modelData().equals("same") && same.transformation(), "same-data ordinary sync retains saved flag");
        queue.offer(PLAYER, "discarded", false);
        queue.offer(PLAYER, "saved", true);
        var saved = queue.poll(PLAYER, 300 * MS);
        require(saved.modelData().equals("saved") && saved.transformation(), "new save keeps its own flag");
        for (int i = 0; i < 1000; i++) queue.offer(PLAYER, "preview" + i, (i & 1) == 0);
        require(queue.pendingPlayers().size() == 1, "pending memory is bounded per player");
        require(queue.poll(PLAYER, 400 * MS).modelData().equals("preview999"), "only newest survives flood");
    }

    private static void noResurrection() {
        for (String event : List.of("model_remove", "disconnect")) {
            var queue = queue();
            queue.offer(PLAYER, "applied", false);
            queue.poll(PLAYER, 0);
            queue.offer(PLAYER, "pending", true);
            queue.remove(PLAYER);
            require(queue.pendingPlayers().isEmpty(), event + " clears pending");
            require(queue.poll(PLAYER, 200 * MS) == null, event + " cannot resurrect model");
            queue.offer(PLAYER, "new-session", false);
            var next = queue.poll(PLAYER, MS);
            require(next != null && !next.transformation(), event + " clears rate history/old flag");
        }
        var queue = queue();
        queue.offer(PLAYER, "old-world", true);
        queue.offer(OTHER, "old-other", true);
        queue.clear();
        require(queue.pendingPlayers().isEmpty(), "server stop clears every player");
        require(queue.poll(PLAYER, 500 * MS) == null && queue.poll(OTHER, 500 * MS) == null,
                "server restart cannot replay old queue");
    }

    private static void independentPlayers() {
        var queue = queue();
        queue.offer(PLAYER, "first", false);
        queue.poll(PLAYER, 0);
        queue.offer(PLAYER, "second", true);
        queue.offer(OTHER, "other", false);
        require(queue.poll(PLAYER, MS) == null, "first player limited");
        require(queue.poll(OTHER, MS).modelData().equals("other"), "other player not blocked");
        queue.remove(PLAYER);
        queue.offer(OTHER, "other-later", true);
        require(queue.poll(OTHER, 50 * MS) == null, "removing another player does not reset own limit");
        require(queue.poll(OTHER, 101 * MS).transformation(), "remaining player queue intact");
    }

    private static void floodStillLimited() {
        var queue = queue();
        List<Long> applyTimes = new ArrayList<>();
        String finalValue = null;
        for (long ms = 0; ms <= 999; ms++) {
            queue.offer(PLAYER, "save-" + ms, true);
            var update = queue.poll(PLAYER, ms * MS);
            if (update != null) {
                applyTimes.add(ms);
                finalValue = update.modelData();
            }
        }
        var finalUpdate = queue.poll(PLAYER, 1000 * MS);
        require(finalUpdate != null && finalUpdate.modelData().equals("save-999"), "flood final state eventually delivered");
        require(applyTimes.size() == 10 && finalValue.equals("save-900"), "100ms rate remains effective");
        for (int i = 1; i < applyTimes.size(); i++) {
            require(applyTimes.get(i) - applyTimes.get(i - 1) >= 100, "applications never violate interval");
        }
    }

    private static void rollover() {
        var queue = queue();
        queue.offer(PLAYER, "before", false);
        queue.poll(PLAYER, Long.MAX_VALUE - 49 * MS);
        queue.offer(PLAYER, "after", true);
        require(queue.poll(PLAYER, Long.MIN_VALUE + 40 * MS) == null, "nanoTime rollover preserves early limit");
        require(queue.poll(PLAYER, Long.MIN_VALUE + 60 * MS).modelData().equals("after"), "nanoTime rollover releases latest");
    }

    private static LatestModelUpdates queue() { return new LatestModelUpdates(100 * MS); }
    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
