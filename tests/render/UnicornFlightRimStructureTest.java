package top.csituka.magicaland.client.render;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

public final class UnicornFlightRimStructureTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        Path repo = Path.of(args[0]);
        String source = Files.readString(repo.resolve("src/client/java/top/csituka/magicaland/client/render/UnicornFlightRim.java"));
        String client = Files.readString(repo.resolve("src/client/java/top/csituka/magicaland/client/Client.java"));
        check(source.contains("PonyFlightVisuals.config(self)") && source.contains("PonyFlightVisuals.sample(self, model, partial).magic()"), "reuse applied plus anatomy config and flight fade");
        check(!source.contains("ModelManager") && !source.contains("getActiveModel") && !source.contains(".showHorn ="), "no draft or saved appearance mutation");
        check(source.contains("!model.showHorn || model.showWings") && source.contains("replacePlayerModel") && source.contains("PonyFlightVisuals.eligible(self)"), "only enabled wingless-horn body");
        check(source.contains("client.getCameraEntity() != self") && source.contains("getFocusedEntity() != self")
                && source.contains("FirstPersonItemView.forOwner(self) != null"), "nonbody camera and external passes hidden");
        int perspective = source.indexOf("!client.options.getPerspective().isFirstPerson()");
        int resetGuard = source.indexOf("{ reset(); return; }", perspective);
        check(perspective >= 0 && resetGuard > perspective && resetGuard < source.indexOf("PonyFlightVisuals.config(self)"),
                "both third-person perspectives clear rim state before rendering or flight sampling");
        check(source.contains("client.currentScreen != null") && source.contains("client.getOverlay() != null")
                && source.contains("client.options.hudHidden"), "menus loading overlay and hidden HUD suppress rim");
        check(source.contains("world != client.world || player != self") && source.contains("DISCONNECT.register"), "world and player identity clear entry");
        check(source.contains("RenderLayer.getGuiOverlay()") && source.contains("GlowingItem.getGlowColor(model)"), "existing GUI overlay and magic color");
        for (String forbidden : new String[] {"Framebuffer", "RenderSystem", "glClear", "glBind", "setPostProcessor", "depthMask", "gameplay", "sendPacket"})
            check(!source.contains(forbidden), "no new black-screen or gameplay path: " + forbidden);
        check(client.indexOf("UnicornFlightRim.init()") > client.indexOf("PonyFlightVisuals.register()")
                && client.indexOf("UnicornFlightRim.init()") == client.lastIndexOf("UnicornFlightRim.init()"), "one registration after flight state");
        try (ZipFile mc = new ZipFile(args[1])) {
            var node = new ClassNode();
            new ClassReader(mc.getInputStream(mc.getEntry("net/minecraft/client/render/RenderLayer.class"))).accept(node, 0);
            var init = node.methods.stream().filter(m -> m.name.equals("<clinit>")).findFirst().orElseThrow();
            boolean scanning = false, colorOnly = false, overlay = false, quads = false, alpha = false;
            for (var instruction : init.instructions) {
                if (instruction instanceof LdcInsnNode text && "gui_overlay".equals(text.cst)) scanning = true;
                if (!scanning || !(instruction instanceof FieldInsnNode field)) continue;
                if (field.name.equals("COLOR_MASK")) colorOnly = true;
                if (field.name.equals("ALWAYS_DEPTH_TEST")) overlay = true;
                if (field.name.equals("QUADS")) quads = true;
                if (field.name.equals("TRANSLUCENT_TRANSPARENCY")) alpha = true;
                if (field.name.equals("GUI_OVERLAY")) break;
            }
            check(colorOnly && overlay && quads && alpha, "actual MC1.20.1 GUI overlay blends alpha and never writes depth");
        }
        System.out.println("PASS unicorn flight rim actual MC layer/guards: " + checks);
    }
    private static void check(boolean okay, String name) { checks++; if (!okay) throw new AssertionError(name); }
}
