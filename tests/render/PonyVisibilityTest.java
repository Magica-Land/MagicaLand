package top.csituka.magicaland.client.render;

import java.nio.file.Files;
import java.nio.file.Path;

public final class PonyVisibilityTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        for (boolean preview : new boolean[] {false, true}) {
            for (boolean visible : new boolean[] {false, true}) {
                for (boolean visibleToViewer : new boolean[] {false, true}) {
                    for (boolean outline : new boolean[] {false, true}) {
                        PonyVisibility expected = preview || visible ? PonyVisibility.VISIBLE
                                : visibleToViewer ? PonyVisibility.TRANSLUCENT
                                : outline ? PonyVisibility.OUTLINE : PonyVisibility.HIDDEN;
                        check(PonyVisibility.select(preview, visible, visibleToViewer, outline) == expected,
                                "visibility precedence for preview, invisibility, teammates and outline");
                    }
                }
            }
        }
        check(PonyVisibility.select(false, false, true, true) == PonyVisibility.TRANSLUCENT,
                "visible invisible teammates use translucency even when glowing, as vanilla does");
        check(PonyVisibility.select(true, false, false, false) == PonyVisibility.VISIBLE,
                "inventory preview remains visible while the player is invisible");

        var repo = Path.of(args[0]);
        String mixin = Files.readString(repo.resolve("src/client/java/top/csituka/magicaland/mixin/client/PlayerEntityRendererMixin.java")).replace("\r\n", "\n");
        String renderer = Files.readString(repo.resolve("src/client/java/top/csituka/magicaland/client/render/PonyRenderer.java")).replace("\r\n", "\n");
        int hidden = mixin.indexOf("if (visibility == PonyVisibility.HIDDEN)");
        int matrix = mixin.indexOf("matrixStack.push();", hidden);
        String hiddenBranch = mixin.substring(hidden, matrix);
        check(hiddenBranch.contains("return;") && !hiddenBranch.contains("ci.cancel()"),
                "fully invisible body keeps vanilla equipment and held-item render path");
        check(mixin.contains("else if (this.hasLabel(player))"),
                "normal pony labels delegate to vanilla distance, team, camera, HUD and invisibility rules");
        check(mixin.contains("super.render(player, f, g, matrixStack, vertexConsumerProvider, i)"),
                "translucent and outlined ponies retain vanilla feature and label rendering");
        check(mixin.contains("finally {\n                magicaland$featuresOnly = previousFeaturesOnly;"),
                "feature-only render flag restored even when an equipment renderer fails");
        check(mixin.contains("finally {\n            ponyRenderer.setBodyVisibility(PonyVisibility.VISIBLE);"),
                "body visibility cannot leak into the next render or preview");
        check(mixin.contains("magicaland$featuresOnly ? null : super.getRenderLayer"),
                "vanilla body is suppressed only while its equipment renders");
        check(renderer.contains("case TRANSLUCENT -> RenderLayer.getItemEntityTranslucentCull(texture)")
                        && renderer.contains("case OUTLINE -> RenderLayer.getOutline(texture)"),
                "all per-bone textures and pupil layers use the selected visibility layer");
        check(renderer.contains("Color.ofRGBA(1f, 1f, 1f, .15f)"),
                "invisible teammate alpha matches vanilla");
        System.out.println("PASS PonyVisibilityTest: " + checks + " visibility and render-wiring checks");
    }

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
}
