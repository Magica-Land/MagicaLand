package top.csituka.magicaland.gaze;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import java.util.UUID;

public final class GazeMessageTest {
    private static int assertions;

    public static void main(String[] args) {
        UUID owner = new UUID(1, 2);
        UUID target = new UUID(3, 4);
        Identifier world = new Identifier("minecraft", "overworld");
        for (GazeMessage expected : new GazeMessage[] {
                new GazeMessage(owner, world, 8, target), new GazeMessage(owner, world, -1, null),
                new GazeMessage(owner, new Identifier("minecraft", "the_nether"), Integer.MAX_VALUE, target)}) {
            PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
            try {
                expected.write(buffer);
                check(expected.equals(GazeMessage.read(buffer)), "消息往返 " + expected.entityId());
                check(buffer.readableBytes() == 0, "完整消费消息");
            } finally { buffer.release(); }
        }
        reject(() -> new GazeMessage(owner, world, -3, target), "负实体 ID");
        reject(() -> new GazeMessage(owner, world, 3, null), "空目标必须为 -1");
        reject(() -> new GazeMessage(null, world, -1, null), "空源玩家");
        reject(() -> new GazeMessage(owner, new Identifier("test", "x".repeat(129)), -1, null), "超长维度 ID");
        PacketByteBuf full = new PacketByteBuf(Unpooled.buffer());
        try {
            new GazeMessage(owner, world, 8, target).write(full);
            for (int length = 0; length < full.writerIndex(); length++) {
                PacketByteBuf truncated = new PacketByteBuf(full.copy(0, length));
                try { reject(() -> GazeMessage.read(truncated), "截断消息 " + length); }
                finally { truncated.release(); }
            }
            full.writeByte(1);
            reject(() -> GazeMessage.read(full), "尾部夹带数据");
        } finally { full.release(); }
        System.out.println("PASS GazeMessageTest: " + assertions + " assertions");
    }

    private static void reject(Runnable action, String message) {
        try { action.run(); } catch (RuntimeException expected) { assertions++; return; }
        throw new AssertionError(message);
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
