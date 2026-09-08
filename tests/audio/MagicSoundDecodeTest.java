import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sound.OggAudioStream;
import net.minecraft.client.sound.RepeatingAudioStream;

/** 只调用 Minecraft 解码器，不创建设备、不连接 OpenAL、不播放声音。 */
public final class MagicSoundDecodeTest {
    private static int checks;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static void main(String[] args) throws Exception {
        Path assets = Path.of(args[0]), output = Path.of(args[2]);
        JsonArray manifest = GSON.fromJson(Files.readString(Path.of(args[1])), JsonArray.class);
        List<Map<String, Object>> report = new ArrayList<>();
        for (var item : manifest) {
            var entry = item.getAsJsonObject();
            Path path = assets.resolve(entry.get("output").getAsString());
            byte[] encoded = Files.readAllBytes(path);
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(encoded));
            check(hash.equals(entry.get("output_sha256").getAsString()), "manifest checksum " + path.getFileName());
            long granule = validatePages(encoded);
            byte[] pcm;
            AudioFormat format;
            try (OggAudioStream stream = new OggAudioStream(Files.newInputStream(path))) {
                format = stream.getFormat();
                check(format.getChannels() == 1, "mono format");
                check(format.getSampleRate() == 48000, "48 kHz sample rate");
                check(format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) && format.getSampleSizeInBits() == 16, "signed 16-bit decoded PCM");
                pcm = bytes(stream.getBuffer());
                check(!stream.getBuffer(4096).hasRemaining(), "clean EOF after complete decode");
            }
            check(pcm.length > 0 && pcm.length % format.getFrameSize() == 0, "whole nonempty PCM frames");
            ByteOrder order = format.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
            ByteBuffer buffer = ByteBuffer.wrap(pcm).order(order);
            double[] samples = new double[pcm.length / 2];
            double peak = 0, sum = 0;
            for (int i = 0; i < samples.length; i++) {
                samples[i] = buffer.getShort() / 32768.0;
                check(Double.isFinite(samples[i]), "finite signed PCM sample");
                peak = Math.max(peak, Math.abs(samples[i]));
                sum += samples[i] * samples[i];
            }
            double rms = Math.sqrt(sum / samples.length), peakDb = db(peak), rmsDb = db(rms);
            check(peak > .001 && rms > .00001, "audible signal exists, not failed silent decode");
            check(peakDb <= -5.5, "decoded peak retains quiet headroom");
            check(samples.length == granule, "decoded samples match final granule exactly, without padding or truncation");
            for (int block : new int[] {4096, 16384}) {
                ByteArrayOutputStream chunked = new ByteArrayOutputStream();
                try (OggAudioStream stream = new OggAudioStream(Files.newInputStream(path))) {
                    for (int i = 0; i < 1000; i++) {
                        byte[] part = bytes(stream.getBuffer(block));
                        if (part.length == 0) break;
                        chunked.write(part);
                        check(i < 999, "stream reaches EOF");
                    }
                }
                check(Arrays.equals(pcm, chunked.toByteArray()), "chunked and full decoding match exactly");
            }
            double[] deltas = new double[samples.length - 1];
            for (int i = 1; i < samples.length; i++) deltas[i - 1] = Math.abs(samples[i] - samples[i - 1]);
            Arrays.sort(deltas);
            double p99 = deltas[(int) Math.floor((deltas.length - 1) * .99)];
            double seam = Math.abs(samples[0] - samples[samples.length - 1]);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("file", path.getFileName().toString());
            result.put("sha256", hash);
            result.put("format", format.toString());
            result.put("ogg_final_granule", granule);
            result.put("mc_pcm_samples", samples.length);
            result.put("mc_duration_seconds", samples.length / format.getSampleRate());
            result.put("mc_minus_granule_samples", samples.length - granule);
            result.put("mc_peak_dbfs", peakDb);
            result.put("mc_rms_dbfs", rmsDb);
            result.put("first_sample", samples[0]);
            result.put("last_sample", samples[samples.length - 1]);
            result.put("join_delta", seam);
            result.put("internal_delta_p99", p99);
            result.put("first_10ms_rms_dbfs", windowRms(samples, 0, 480));
            result.put("last_10ms_rms_dbfs", windowRms(samples, Math.max(0, samples.length - 480), 480));
            if (path.getFileName().toString().equals("aura.ogg")) {
                check(seam < p99, "loop seam smaller than internal 99th-percentile sample change");
                ByteArrayOutputStream repeated = new ByteArrayOutputStream();
                try (RepeatingAudioStream stream = new RepeatingAudioStream(OggAudioStream::new, Files.newInputStream(path))) {
                    for (int i = 0; repeated.size() < pcm.length * 3 && i < 1000; i++) {
                        byte[] part = bytes(stream.getBuffer(4096));
                        check(part.length > 0, "Minecraft looping stream has no empty boundary buffer");
                        repeated.write(part);
                    }
                }
                byte[] repeatedPcm = repeated.toByteArray();
                check(repeatedPcm.length >= pcm.length * 3, "three loops decoded");
                for (int loop = 0; loop < 3; loop++) check(Arrays.equals(pcm,
                        Arrays.copyOfRange(repeatedPcm, loop * pcm.length, (loop + 1) * pcm.length)), "loop " + loop + " has no added/dropped PCM frames");
                result.put("loop_repetitions_exact", 3);
                result.put("loop_seam_to_p99_ratio", seam / p99);
            }
            report.add(result);
            System.out.printf(java.util.Locale.ROOT, "%s: %d samples, %.6f s, peak %.2f dBFS, RMS %.2f dBFS, granule delta %+d%n",
                    path.getFileName(), samples.length, samples.length / format.getSampleRate(), peakDb, rmsDb, samples.length - granule);
        }
        Files.writeString(output, GSON.toJson(report) + "\n");
        System.out.println("PASS MagicSoundDecodeTest: " + checks + " real Minecraft decoder, PCM, container, chunked EOF and loop checks; no audio playback.");
    }

    private static long validatePages(byte[] data) {
        int offset = 0, serial = 0, sequence = 0, pages = 0;
        long granule = -1;
        boolean ended = false;
        while (offset < data.length) {
            check(data.length - offset >= 27, "complete Ogg page header");
            check(data[offset] == 'O' && data[offset + 1] == 'g' && data[offset + 2] == 'g' && data[offset + 3] == 'S', "Ogg signature");
            check(data[offset + 4] == 0, "supported Ogg version");
            check(!ended, "no bytes after EOS");
            ByteBuffer page = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            int pageSerial = page.getInt(offset + 14), pageSequence = page.getInt(offset + 18);
            if (pages == 0) { serial = pageSerial; check((data[offset + 5] & 2) != 0, "BOS flag"); }
            check(pageSerial == serial && pageSequence == sequence++, "continuous single Vorbis logical stream");
            int segments = data[offset + 26] & 255;
            check(offset + 27 + segments <= data.length, "complete segment table");
            int length = 27 + segments;
            for (int i = 0; i < segments; i++) length += data[offset + 27 + i] & 255;
            check(offset + length <= data.length, "complete page payload, no truncation");
            int crc = 0;
            for (int i = 0; i < length; i++) {
                int value = i >= 22 && i < 26 ? 0 : data[offset + i] & 255;
                crc ^= value << 24;
                for (int bit = 0; bit < 8; bit++) crc = (crc << 1) ^ (crc < 0 ? 0x04C11DB7 : 0);
            }
            check(crc == page.getInt(offset + 22), "Ogg page CRC");
            long current = page.getLong(offset + 6);
            if (current >= 0) { check(current >= granule, "nondecreasing sample granule"); granule = current; }
            ended = (data[offset + 5] & 4) != 0;
            pages++;
            offset += length;
        }
        check(ended && granule > 0, "complete EOS and positive duration");
        return granule;
    }
    private static byte[] bytes(ByteBuffer source) { byte[] result = new byte[source.remaining()]; source.get(result); return result; }
    private static double db(double value) { return 20 * Math.log10(Math.max(value, 1e-12)); }
    private static double windowRms(double[] samples, int start, int length) {
        int end = Math.min(samples.length, start + length); double sum = 0;
        for (int i = start; i < end; i++) sum += samples[i] * samples[i];
        return db(Math.sqrt(sum / (end - start)));
    }
    private static void check(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }
}
