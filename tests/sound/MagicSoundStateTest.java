package top.csituka.magicaland.client.sound;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MagicSoundStateTest {
    private static int checks;
    private static final UUID SELF = new UUID(0, 1);
    private static MagicSoundState.Observation self(boolean holding) { return observation(1, true, holding, 0); }
    private static MagicSoundState.Observation observation(int id, boolean local, boolean holding, double distance) {
        return new MagicSoundState.Observation(new UUID(0, id), id, local, holding, distance);
    }
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static MagicSoundState.Frame advance(MagicSoundState state, MagicSoundState.Observation... observations) {
        return state.advance(List.of(observations));
    }
    private static void silent(MagicSoundState.Frame frame, String message) { check(frame.events().isEmpty(), message); }
    private static void event(MagicSoundState.Frame frame, MagicSoundState.Burst burst, String message) {
        check(frame.events().size() == 1 && frame.events().get(0).id().equals(SELF)
                && frame.events().get(0).burst() == burst, message);
    }

    public static void main(String[] args) {
        startupAndEquip();
        debounceAndCooldown();
        remoteAndSlots();
        lifecycleAndIdentity();
        distanceAndEnvelope();
        boundedLongRun();
        System.out.println("PASS MagicSoundStateTest: " + checks + " assertions");
    }

    private static void startupAndEquip() {
        MagicSoundState state = new MagicSoundState();
        MagicSoundState.Frame frame = advance(state, self(true));
        silent(frame, "joining while holding only starts loop");
        check(frame.loops().containsKey(SELF), "join restores loop");
        for (int i = 0; i < 200; i++) {
            frame = advance(state, self(true));
            silent(frame, "two hands / nonempty inventory switches are a single holding state");
            check(frame.loops().size() == 1, "one player never creates two loops");
        }
        advance(state, self(false));
        advance(state, self(false));
        event(advance(state, self(false)), MagicSoundState.Burst.END, "ordinary release ends after debounce");
        for (int i = 0; i < 8; i++) silent(advance(state, self(false)), "remaining empty is silent");
        event(advance(state, self(true)), MagicSoundState.Burst.CAST, "real pickup after baseline casts");
        check(advance(state, self(true)).loops().size() == 1, "cast does not require render loop");
    }

    private static void debounceAndCooldown() {
        for (int gap = 1; gap < MagicSoundState.EMPTY_DEBOUNCE_TICKS; gap++) {
            MagicSoundState state = new MagicSoundState();
            advance(state, self(true));
            for (int cycle = 0; cycle < 50; cycle++) {
                for (int i = 0; i < gap; i++) {
                    MagicSoundState.Frame frame = advance(state, self(false));
                    silent(frame, "brief empty gap is silent");
                    check(frame.loops().containsKey(SELF), "loop survives brief empty gap");
                }
                silent(advance(state, self(true)), "return from brief empty gap does not cast");
            }
        }
        MagicSoundState state = new MagicSoundState();
        advance(state, self(false));
        event(advance(state, self(true)), MagicSoundState.Burst.CAST, "initial actual edge casts");
        advance(state, self(false));
        advance(state, self(false));
        MagicSoundState.Frame end = advance(state, self(false));
        silent(end, "very short action obeys shared burst cooldown");
        check(end.loops().isEmpty(), "cooldown never prevents loop release");
        silent(advance(state, self(true)), "quick re-equip does not stack bursts");
        for (int i = 0; i < 8; i++) advance(state, self(true));
        advance(state, self(false));
        advance(state, self(false));
        event(advance(state, self(false)), MagicSoundState.Burst.END, "later ordinary release can play");
    }

    private static void remoteAndSlots() {
        MagicSoundState state = new MagicSoundState();
        List<MagicSoundState.Observation> players = new ArrayList<>();
        players.add(self(true));
        for (int i = 2; i <= 12; i++) players.add(observation(i, false, true, .4 * i));
        MagicSoundState.Frame frame = state.advance(players);
        silent(frame, "remote arrival never casts");
        check(frame.loops().size() == 4 && frame.loops().containsKey(SELF), "four voices includes local priority");
        check(frame.loops().containsKey(new UUID(0, 2)) && frame.loops().containsKey(new UUID(0, 4)), "nearest remotes selected");
        for (int i = 0; i < 40; i++) {
            players.set(4, observation(5, false, true, 1.59 + (i % 2) * .02));
            frame = state.advance(players);
            check(frame.loops().containsKey(new UUID(0, 4)), "small boundary motion does not churn slot");
            check(frame.loops().size() == 4, "slot hard limit");
            silent(frame, "range/slot switching never casts");
        }
        players.set(4, observation(5, false, true, .5));
        frame = state.advance(players);
        check(frame.loops().containsKey(new UUID(0, 5)), "substantially closer player takes slot");
        state.clear();
        for (int holding = 0; holding < 20; holding++) {
            frame = advance(state, observation(2, false, holding % 4 == 0, 2));
            silent(frame, "remote pickup/drop has no local burst");
        }
    }

    private static void lifecycleAndIdentity() {
        for (String reason : List.of("mute", "resource reload", "world change", "disconnect", "replace toggle")) {
            MagicSoundState state = new MagicSoundState();
            advance(state, self(false));
            event(advance(state, self(true)), MagicSoundState.Burst.CAST, "real pickup before " + reason);
            state.clear();
            check(state.trackedPlayers() == 0, reason + " clears tracking");
            MagicSoundState.Frame frame = advance(state, self(true));
            silent(frame, reason + " recovery does not replay start");
            check(frame.loops().containsKey(SELF), reason + " recovery restores held loop");
        }
        for (String reason : List.of("invisible", "death", "spectator", "sleep", "no horn", "no remote model", "outside range", "first-person effect disabled")) {
            MagicSoundState state = new MagicSoundState();
            advance(state, self(true));
            MagicSoundState.Frame absent = advance(state);
            silent(absent, reason + " disappearance is not normal put-down");
            check(absent.eligible().isEmpty() && absent.loops().isEmpty() && state.trackedPlayers() == 0, reason + " drops player");
            silent(advance(state, self(true)), reason + " reappearance is baseline");
        }
        MagicSoundState state = new MagicSoundState();
        advance(state, self(false));
        MagicSoundState.Frame frame = advance(state, new MagicSoundState.Observation(SELF, 999, true, true, 0));
        silent(frame, "entity replacement/respawn with same UUID does not cast");
        check(frame.loops().containsKey(SELF), "replacement held state restores loop");
        state.clear();
        advance(state, observation(1, false, false, 0));
        silent(advance(state, self(true)), "local/remote role replacement resets baseline");
        frame = advance(state, self(true), self(false));
        check(frame.loops().size() == 1, "duplicate observation cannot add a second voice");
    }

    private static void distanceAndEnvelope() {
        float previous = 1;
        for (int i = 0; i <= 1000; i++) {
            double distance = i * .008;
            float gain = MagicSoundState.distanceGain(distance);
            check(gain >= 0 && gain <= previous, "range curve finite bounded and monotone");
            previous = gain;
        }
        check(MagicSoundState.distanceGain(0) == 1 && MagicSoundState.distanceGain(2) == 1, "near own head stays quiet but full configured gain");
        for (double invalid : new double[]{8, 8.01, 9999, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            check(MagicSoundState.distanceGain(invalid) == 0, "invalid/outside distance is silent");
            MagicSoundState state = new MagicSoundState();
            MagicSoundState.Frame frame = advance(state, observation(1, true, true, invalid));
            check(frame.eligible().isEmpty() && frame.loops().isEmpty(), "invalid/outside never tracked");
        }
        MagicSoundState.Envelope envelope = new MagicSoundState.Envelope();
        check(envelope.level() == 0, "loop starts at zero envelope");
        previous = 0;
        for (int i = 0; i < 8; i++) {
            float value = envelope.advance(true);
            check(value > previous && value <= 1 && value - previous <= .12501, "eight-tick gentle fade-in");
            previous = value;
        }
        check(envelope.level() == 1, "fade-in reaches full configured gain");
        for (int i = 0; i < 10; i++) {
            float value = envelope.advance(false);
            check(value <= previous && value >= 0 && previous - value <= .10001, "ten-tick gentle fade-out");
            previous = value;
        }
        check(envelope.level() == 0, "fade-out reaches exact zero for disposal");
        envelope.advance(true);
        envelope.advance(true);
        envelope.advance(false);
        float before = envelope.level();
        check(envelope.advance(true) > before && envelope.level() < 1, "return while fading reverses smoothly");
    }

    private static void boundedLongRun() {
        MagicSoundState state = new MagicSoundState();
        java.util.Random random = new java.util.Random(812094);
        long lastEvent = -100;
        for (int tick = 0; tick < 5000; tick++) {
            List<MagicSoundState.Observation> observations = new ArrayList<>();
            observations.add(self(random.nextBoolean()));
            int count = random.nextInt(30);
            for (int i = 2; i < count + 2; i++) observations.add(observation(i, false, random.nextBoolean(), random.nextDouble() * 10));
            MagicSoundState.Frame frame = state.advance(observations);
            check(frame.loops().size() <= 4, "randomized crowd never exceeds loop cap");
            check(state.trackedPlayers() <= observations.size(), "disappearing players do not leak state");
            check(frame.eligible().keySet().containsAll(frame.loops().keySet()), "only eligible players have loops");
            for (MagicSoundState.Event event : frame.events()) {
                check(event.id().equals(SELF), "random crowd only local bursts");
                check(tick - lastEvent >= MagicSoundState.BURST_COOLDOWN_TICKS, "random actions obey cooldown");
                lastEvent = tick;
            }
        }
        advance(state);
        check(state.trackedPlayers() == 0, "long run clears when world players disappear");
    }
}
