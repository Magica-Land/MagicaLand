# 魔法音效无播放解码检查

`MagicSoundDecodeTest` 直接调用 Minecraft 1.20.1 的 `OggAudioStream` 和 `RepeatingAudioStream`，不启动 Minecraft、不初始化 OpenAL、不连接输出设备。

使用 Java 17 编译目标与现有游戏依赖 classpath，入口参数为：

1. 包含 `cast_1.ogg`、`cast_2.ogg`、`aura.ogg`、`end.ogg` 的目录。
2. 音频清单 JSON（数组，包含每个文件的 `output` 和 `output_sha256`）。
3. 解码指标 JSON 的输出路径。

检查范围：清单哈希、Ogg 页 CRC/序号/完整 EOS、48 kHz 单声道 signed 16-bit PCM、全部样本有效、无满幅削波、样本数与容器最终 granule 精确一致、整段与两种分块读取逐字节一致，以及 aura 三遍循环没有空缓冲或增删帧。

循环首尾差值小于内部样本差的第 99 百分位，只说明未检测到突出的数值接缝；不等同于试听确认。峰值是文件 PCM 的 dBFS，不代表扬声器实际响度，多声源叠加仍需运行时限量和音量控制。
