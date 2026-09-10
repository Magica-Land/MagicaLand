import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import net.fabricmc.classtweaker.api.ClassTweaker;
import net.fabricmc.classtweaker.api.ClassTweakerReader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public final class PrepareTestMinecraft {
    public static void main(String[] args) throws Exception {
        ClassTweaker tweaker = ClassTweaker.newInstance();
        ClassTweakerReader.create(tweaker).read(Files.readAllBytes(Path.of(args[1])), "named");
        int changed = 0;
        try (ZipFile input = new ZipFile(args[0]);
             ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(Path.of(args[2])))) {
            var entries = input.entries();
            while (entries.hasMoreElements()) {
                ZipEntry source = entries.nextElement();
                byte[] bytes;
                try (var stream = input.getInputStream(source)) { bytes = stream.readAllBytes(); }
                if (source.getName().endsWith(".class")) {
                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, 0);
                    reader.accept(tweaker.createClassVisitor(Opcodes.ASM9, writer,
                            (name, generated) -> { throw new IllegalStateException("Unexpected generated class: " + name); }), 0);
                    byte[] widened = writer.toByteArray();
                    if (!Arrays.equals(bytes, widened)) changed++;
                    bytes = widened;
                }
                ZipEntry target = new ZipEntry(source.getName());
                target.setTime(source.getTime());
                output.putNextEntry(target);
                output.write(bytes);
                output.closeEntry();
            }
        }
        System.out.println("PASS access widener: applied current project rules to " + changed + " Minecraft classes");
    }
}
