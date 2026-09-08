package top.csituka.magicaland.gaze;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public final class GazePolicyTest {
    private static int assertions;
    private static UUID id(int n) { return new UUID(0, n); }
    private static GazePolicy.Candidate candidate(int n, int priority, double distance) {
        return new GazePolicy.Candidate(id(n), priority, distance * distance);
    }

    public static void main(String[] args) {
        check(GazePolicy.priority(true, true, true, true) == 0, "最近攻击者最高");
        check(GazePolicy.priority(false, true, false, true) == 1, "激怒中立生物归敌对");
        check(GazePolicy.priority(false, false, true, false) == 2, "其他玩家");
        check(GazePolicy.priority(false, false, false, true) == 3, "中立生物");
        check(GazePolicy.priority(false, false, false, false) == 4, "友善生物");
        check(GazePolicy.recentAttack(50, 50), "刚受伤");
        check(GazePolicy.recentAttack(149, 50), "第 99 tick 仍记忆");
        check(!GazePolicy.recentAttack(150, 50), "100 tick 到期");
        check(!GazePolicy.recentAttack(49, 50), "拒绝未来攻击时间");
        check(!GazePolicy.recentAttack(Integer.MAX_VALUE, Integer.MIN_VALUE), "无时间溢出");
        check(GazePolicy.inView(0, 0, 8, 0), "8 格边界包含");
        check(!GazePolicy.inView(0, 0, 8.001, 0), "8 格外排除");
        check(!GazePolicy.inView(0, 7, 7, 0), "球形距离不是方形搜索框");
        check(!GazePolicy.inView(0, 0, -2, 0), "正后方不注视");
        check(GazePolicy.inView(-2, 0, 0, 90), "头朝西");
        check(!GazePolicy.inView(Double.NaN, 0, 1, 0), "非有限坐标排除");
        check(GazePolicy.select(List.of(), id(1)) == null, "空集合回正");
        for (int high = 0; high < 4; high++) {
            check(id(1).equals(GazePolicy.select(List.of(candidate(1, high, 8), candidate(2, high + 1, 0.1)), id(2))),
                    "高优先级不受距离或锁定阻挡 " + high);
        }
        check(id(2).equals(GazePolicy.select(List.of(candidate(1, 2, 4), candidate(2, 2, 2)), null)), "同级最近");
        check(id(1).equals(GazePolicy.select(List.of(candidate(1, 2, 2.1), candidate(2, 2, 2)), id(1))), "小距离波动保留目标");
        check(id(2).equals(GazePolicy.select(List.of(candidate(1, 2, 2.3), candidate(2, 2, 2)), id(1))), "明显更近则切换");
        check(id(2).equals(GazePolicy.select(List.of(candidate(2, 4, 7)), id(1))), "被可见性筛除的攻击者不能继续锁定");
        check(id(1).equals(GazePolicy.select(List.of(candidate(2, 1, 2), candidate(1, 1, 2)), null)), "平距以 UUID 稳定排序");
        check(GazePolicy.select(List.of(new GazePolicy.Candidate(id(1), 0, Double.NaN), candidate(2, 0, 9)), null) == null,
                "非法候选不能胜出");
        var obscured = List.of(candidate(1, 0, 2), candidate(2, 1, 4), candidate(3, 2, 5));
        check(id(2).equals(GazePolicy.selectVisible(obscured, id(1), value -> !value.equals(id(1)))), "攻击者被挡后改看敌对");
        check(id(3).equals(GazePolicy.selectVisible(obscured, id(1), value -> value.equals(id(3)))), "高两级都被挡后看玩家");
        check(GazePolicy.selectVisible(obscured, id(1), value -> false) == null, "全部遮挡则无目标");
        check(id(2).equals(GazePolicy.selectVisible(List.of(candidate(1, 2, 2.1), candidate(2, 2, 2)), id(1),
                value -> value.equals(id(2)))), "遮挡能打断同级距离缓冲");
        int[] rays = {0};
        GazePolicy.selectVisible(obscured, null, value -> { rays[0]++; return true; });
        check(rays[0] == 1, "找到可见最高级后停止检测");
        Random random = new Random(8026);
        for (int i = 0; i < 1000; i++) {
            var choices = new ArrayList<GazePolicy.Candidate>();
            for (int j = 0; j < 20; j++) choices.add(candidate(j + 1, random.nextInt(5), random.nextDouble() * 8));
            UUID selected = GazePolicy.select(choices, null);
            Collections.shuffle(choices, random);
            check(selected.equals(GazePolicy.select(choices, null)), "查询顺序无关 " + i);
        }
        System.out.println("PASS GazePolicyTest: " + assertions + " assertions");
    }

    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
}
