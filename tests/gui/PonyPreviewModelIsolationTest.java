package top.csituka.magicaland.client.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Map;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import software.bernie.geckolib.util.JsonUtil;

public final class PonyPreviewModelIsolationTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path repo = Path.of(args[0]);
        Model raw = JsonUtil.GEO_GSON.fromJson(Files.readString(repo.resolve(
                "appearance/src/main/resources/assets/magicaland/geo/mare_geo.json")), Model.class);
        BakedGeoModel world = bake(raw), rest = bake(raw);
        world.getBone("Head").orElseThrow().setRotX(1.2f);
        world.getBone("Neck").orElseThrow().setRotY(-.8f);
        world.getBone("RightEar").orElseThrow().setRotZ(.7f);
        BakedGeoModel preview = bake(raw);
        var model = new GeckoPlayerModel();
        model.getAnimationProcessor().setActiveModel(preview);
        Map<GeoBone, Boolean> sharedBones = new IdentityHashMap<>();
        for (GeoBone root : world.topLevelBones()) collect(root, sharedBones);
        for (GeoBone root : rest.topLevelBones()) collect(root, sharedBones);
        String worldBefore = pose(world), restBefore = pose(rest);
        for (GeoBone root : preview.topLevelBones()) mutatePreview(root, sharedBones, model);
        check(pose(world).equals(worldBefore), "animated preview cannot overwrite paused world pose");
        check(pose(rest).equals(restBefore), "animated preview cannot overwrite static bounds/thumbnail rest tree");

        BakedGeoModel reloadedWorld = bake(raw), reloadedPreview = bake(raw);
        check(reloadedWorld != world && reloadedPreview != preview, "resource reload creates fresh source and private identities");
        String oldPreviewPose = pose(preview);
        model.getAnimationProcessor().setActiveModel(reloadedPreview);
        Map<GeoBone, Boolean> oldTrees = new IdentityHashMap<>(sharedBones);
        for (GeoBone root : preview.topLevelBones()) collect(root, oldTrees);
        for (GeoBone root : reloadedWorld.topLevelBones()) collect(root, oldTrees);
        String reloadedWorldPose = pose(reloadedWorld);
        for (GeoBone root : reloadedPreview.topLevelBones()) mutatePreview(root, oldTrees, model);
        check(pose(preview).equals(oldPreviewPose), "reload does not retain or mutate old animated bone objects");
        check(pose(reloadedWorld).equals(reloadedWorldPose), "reloaded shared world model stays untouched");
        sourceGuard(repo);
        System.out.println("PASS PonyPreviewModelIsolationTest: " + checks + " checks");
    }

    private static BakedGeoModel bake(Model raw) {
        return BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(GeometryTree.fromModel(raw));
    }

    private static void collect(GeoBone bone, Map<GeoBone, Boolean> bones) {
        bones.put(bone, true);
        for (GeoBone child : bone.getChildBones()) collect(child, bones);
    }

    private static void mutatePreview(GeoBone bone, Map<GeoBone, Boolean> shared, GeckoPlayerModel model) {
        check(!shared.containsKey(bone), "private bone is not shared: " + bone.getName());
        check(model.getAnimationProcessor().getBone(bone.getName()) == bone, "animation processor writes private bone");
        check(bone.getInitialSnapshot() != null, "private initial snapshot is registered");
        bone.setRotX(bone.getRotX() + .11f);
        bone.setRotY(bone.getRotY() - .17f);
        bone.setPosZ(bone.getPosZ() + .23f);
        bone.setScaleY(bone.getScaleY() * .9f);
        bone.setHidden(!bone.isHidden());
        for (GeoBone child : bone.getChildBones()) mutatePreview(child, shared, model);
    }

    private static String pose(BakedGeoModel model) {
        StringBuilder result = new StringBuilder();
        for (GeoBone root : model.topLevelBones()) pose(root, result);
        return result.toString();
    }

    private static void pose(GeoBone bone, StringBuilder out) {
        out.append(bone.getName()).append(':').append(bone.getRotX()).append(',').append(bone.getRotY()).append(',')
                .append(bone.getRotZ()).append(',').append(bone.getPosZ()).append(',').append(bone.getScaleY())
                .append(',').append(bone.isHidden()).append(';');
        for (GeoBone child : bone.getChildBones()) pose(child, out);
    }

    private static void sourceGuard(Path repo) throws Exception {
        String source = Files.readString(repo.resolve("appearance/src/client/java/top/csituka/magicaland/client/gui/tab/PonyCustom.java"));
        String method = source.substring(source.indexOf("public BakedGeoModel getBakedModel(Identifier location)"),
                source.indexOf("public void handleAnimations("));
        check(method.contains("BakedGeoModel source = super.getBakedModel(location);"), "shared identity observes GeckoLib resource reload");
        check(method.contains("if (source != sharedSource)"), "private model persists between render frames");
        int branch = method.indexOf('{', method.indexOf("if (source != sharedSource)"));
        int depth = 1, branchEnd = branch + 1;
        while (depth != 0 && branchEnd < method.length()) {
            char value = method.charAt(branchEnd++);
            if (value == '{') depth++;
            else if (value == '}') depth--;
        }
        check(method.contains("FileLoader.loadModelFile(location,") && method.contains("GeometryTree.fromModel(raw)"),
                "private model is freshly baked from resource, not copied from current world pose");
        int bind = method.indexOf("getAnimationProcessor().setActiveModel(previewModel);");
        int mark = method.indexOf("sharedSource = source;");
        check(bind > 0 && mark > bind, "reload identity commits only after private processor binding");
        check(bind > branch && bind < branchEnd
                        && method.indexOf("setActiveModel", method.indexOf("setActiveModel") + 14) == -1,
                "unchanged source on consecutive frames never rebinds or reinitializes animation processor");
        check(method.contains("return previewModel;") && !method.contains("PreviewGeometryBounds.model()"),
                "renderer receives private animated tree, not shared static bounds tree");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
