package top.csituka.magicaland.client.render;

public final class MagicSparkles {
    private MagicSparkles() {}
    public record Spark(float x, float y, float z, float radius, float alpha) {}

    public static Spark sample(double ticks, int seed, int slot) {
        if (!Double.isFinite(ticks) || slot < 0 || slot >= 4) return null;
        int period = 60 + Math.floorMod(seed, 21);
        double clock = ticks + Math.floorMod(seed, period);
        long cycle = (long) Math.floor(clock / period);
        int count = 2 + (int) Math.floorMod(cycle + seed, 3);
        if (slot >= count) return null;
        double progress = (clock - cycle * period - 14 - slot * 2) / 18;
        if (progress <= 0 || progress >= 1) return null;
        double angle = slot * 2.39996 + Math.floorMod(seed, 100) * 0.06283 + Math.floorMod(cycle, 16) * 0.65;
        float spread = (float) (0.075 + 0.055 * progress);
        float alpha = (float) Math.sin(progress * Math.PI) * 0.65f;
        float radius = (0.014f + slot * 0.002f) * (float) (0.75 + progress * 0.7);
        return new Spark((float) Math.cos(angle) * spread, (float) progress * 0.14f,
                (float) Math.sin(angle) * spread, radius, alpha);
    }
}
