import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const repo = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const source = fs.existsSync(path.join(repo, 'src')) ? path.join(repo, 'src') : path.join(repo, 'appearance/src');
const javaBase = 'top/csituka/magicaland';
const main = path.join(source, 'main/java', javaBase);
const client = path.join(source, 'client/java', javaBase);
const temp = fs.mkdtempSync(path.join(os.tmpdir(), 'magicaland-api-test-'));
const fixtures = {
  'net/minecraft/world/World.java': 'package net.minecraft.world; public class World {}',
  'net/minecraft/entity/Entity.java': `package net.minecraft.entity;
    public class Entity {
      public boolean removed; private final java.util.UUID id; private final net.minecraft.world.World world;
      public Entity(java.util.UUID id, net.minecraft.world.World world) { this.id=id; this.world=world; }
      public java.util.UUID getUuid() { return id; } public boolean isRemoved() { return removed; }
      public net.minecraft.world.World getWorld() { return world; }
    }`,
  'net/minecraft/entity/LivingEntity.java': `package net.minecraft.entity;
    public class LivingEntity extends Entity {
      public LivingEntity(java.util.UUID id, net.minecraft.world.World world) { super(id,world); }
    }`,
  'net/minecraft/item/ItemStack.java': 'package net.minecraft.item; public class ItemStack {}',
  'net/minecraft/client/render/item/ItemRenderer.java': 'package net.minecraft.client.render.item; public class ItemRenderer {}',
  'net/minecraft/client/render/VertexConsumerProvider.java': `package net.minecraft.client.render;
    public interface VertexConsumerProvider { class Immediate implements VertexConsumerProvider {} }`,
  'net/minecraft/client/render/model/json/ModelTransformationMode.java': `package net.minecraft.client.render.model.json;
    public enum ModelTransformationMode { GROUND }`,
  'net/minecraft/client/util/math/MatrixStack.java': 'package net.minecraft.client.util.math; public class MatrixStack {}',
  'net/minecraft/client/MinecraftClient.java': `package net.minecraft.client;
    public class MinecraftClient {
      private static final MinecraftClient INSTANCE=new MinecraftClient();
      public net.minecraft.entity.LivingEntity player; public net.minecraft.world.World world;
      public static MinecraftClient getInstance() { return INSTANCE; }
      public net.minecraft.client.render.item.ItemRenderer getItemRenderer() { return new net.minecraft.client.render.item.ItemRenderer(); }
    }`,
  'net/fabricmc/fabric/api/client/networking/v1/ClientPlayConnectionEvents.java': `package net.fabricmc.fabric.api.client.networking.v1;
    public class ClientPlayConnectionEvents {
      public static final Event DISCONNECT=new Event();
      public static class Event {
        private java.util.function.BiConsumer<Object,Object> action;
        public void register(java.util.function.BiConsumer<Object,Object> action) { this.action=action; }
        public void fire() { action.accept(null,null); }
      }
    }`,
  'org/slf4j/LoggerFactory.java': `package org.slf4j;
    public class LoggerFactory {
      public static Logger getLogger(Class<?> type) { return new Logger(); }
      public static class Logger { public void warn(String message, Throwable failure) {} }
    }`,
  [javaBase + '/client/config/ModelConfig.java']: `package top.csituka.magicaland.client.config;
    public class ModelConfig { public boolean showHorn,showWings; public int magicColor; }`,
  [javaBase + '/client/config/ModelManager.java']: `package top.csituka.magicaland.client.config;
    public class ModelManager { public static ModelConfig applied; public static ModelConfig getAppliedModel() { return applied; } }`,
  [javaBase + '/client/config/Config.java']: `package top.csituka.magicaland.client.config;
    public class Config { private static final Config INSTANCE=new Config(); public boolean replacePlayerModel=true;
      public static Config getInstance() { return INSTANCE; } }`,
  [javaBase + '/client/network/ClientNetworkHandler.java']: `package top.csituka.magicaland.client.network;
    public class ClientNetworkHandler {
      public static final java.util.Map<java.util.UUID,top.csituka.magicaland.client.config.ModelConfig> remoteModels=new java.util.HashMap<>();
    }`,
  [javaBase + '/network/NetworkHandler.java']: `package top.csituka.magicaland.network;
    public class NetworkHandler { public static boolean serverHasMod=true; }`,
  [javaBase + '/client/render/MagicOrb.java']: `package top.csituka.magicaland.client.render;
    public class MagicOrb { public static void render(net.minecraft.client.util.math.MatrixStack matrices,int color,double ticks,int seed) {} }`,
  [javaBase + '/client/render/GlowingItem.java']: `package top.csituka.magicaland.client.render;
    public class GlowingItem {
      public static final java.util.List<String> events=new java.util.ArrayList<>(); public static boolean failEnd;
      public static int getGlowColor(top.csituka.magicaland.client.config.ModelConfig config) { return config.magicColor; }
      public static void beginFirstPersonPass() { events.add("begin"); }
      public static void endFirstPersonPass(net.minecraft.client.render.VertexConsumerProvider.Immediate buffers) {
        events.add("end"); if (failEnd) throw new IllegalStateException("flush failed");
      }
      public static void renderPreviewWithGlow(net.minecraft.client.render.item.ItemRenderer renderer,
        net.minecraft.item.ItemStack stack,net.minecraft.client.render.model.json.ModelTransformationMode mode,
        net.minecraft.client.util.math.MatrixStack matrices,net.minecraft.client.render.VertexConsumerProvider buffers,
        net.minecraft.world.World world,int light,int seed,int color) {}
    }`,
};

function files(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const name = path.join(directory, entry.name);
    return entry.isDirectory() ? files(name) : [name];
  });
}
function run(command, args) {
  try { return execFileSync(command, args, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }); }
  catch (failure) { throw new Error(`${command} failed:\n${failure.stderr || failure.message}`); }
}

try {
  const fixtureSources = [];
  for (const [name, content] of Object.entries(fixtures)) {
    const file = path.join(temp, 'fixtures', name);
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, content);
    fixtureSources.push(file);
  }
  const classes = path.join(temp, 'classes');
  const publicSources = [path.join(main, 'api/ApiVersion.java'), ...files(path.join(client, 'api/client')).filter(file => file.endsWith('.java'))];
  const internalSources = ['AppearanceAccess', 'AppearanceOverrideState', 'AppearanceVisualBridge', 'OverrideRegistry', 'FirstPersonItemView']
    .map(name => path.join(client, 'client/api', name + '.java'));
  run('javac', ['--release', '17', '-d', classes, ...fixtureSources, ...publicSources, ...internalSources,
    path.join(repo, 'tests/api/AppearanceApiTest.java')]);
  process.stdout.write(run('java', ['-cp', classes, 'top.csituka.magicaland.client.api.AppearanceApiTest']));

  const apiOnly = path.join(temp, 'api-only');
  const minecraftOnly = path.join(temp, 'minecraft-only');
  fs.cpSync(path.join(classes, javaBase, 'api'), path.join(apiOnly, javaBase, 'api'), { recursive: true });
  fs.cpSync(path.join(classes, 'net/minecraft'), path.join(minecraftOnly, 'net/minecraft'), { recursive: true });
  const apiJar = path.join(temp, 'appearance-api.jar');
  const minecraftJar = path.join(temp, 'minecraft-fixture.jar');
  run('jar', ['--create', '--file', apiJar, '-C', apiOnly, '.']);
  run('jar', ['--create', '--file', minecraftJar, '-C', minecraftOnly, '.']);
  const consumer = path.join(temp, 'ApiConsumer.java');
  fs.writeFileSync(consumer, `import top.csituka.magicaland.api.ApiVersion;
    import top.csituka.magicaland.api.client.*;
    import net.minecraft.entity.*;
    import net.minecraft.item.ItemStack;
    import net.minecraft.client.render.VertexConsumerProvider;
    import net.minecraft.client.util.math.MatrixStack;
    public class ApiConsumer {
      public void use(java.util.UUID id, LivingEntity owner, Entity camera, ItemStack stack,
                      MatrixStack matrices, VertexConsumerProvider.Immediate buffers) {
        ApiVersion.requireCompatible(1,0);
        java.util.Optional<AppearanceSnapshot> snapshot=Appearances.find(id);
        try (Registration registration=AppearanceOverrides.registerGaze("addon:test",0,player -> camera)) {
          AppearanceOverrides.registerMainHandVisibility("addon:test",0,player -> AppearanceOverrides.Visibility.HIDDEN).close();
          AppearanceVisuals.renderOrb(matrices,Appearances.magicColor(id),0,0);
          AppearanceVisuals.renderGlowingItem(stack,net.minecraft.client.render.model.json.ModelTransformationMode.GROUND,
            matrices,buffers,camera.getWorld(),0,0,Appearances.magicColor(id));
          AppearanceVisuals.renderFirstPerson(owner,camera,stack,buffers,() -> {});
        }
        AppearanceOverrides.unregisterOwner("addon:test");
      }
    }`);
  run('javac', ['--release', '17', '-cp', [apiJar, minecraftJar].join(path.delimiter), '-d', path.join(temp, 'consumer'), consumer]);
  for (const file of files(apiOnly).filter(file => file.endsWith('.class'))) {
    const name = path.relative(apiOnly, file).replace(/\.class$/, '').split(path.sep).join('.');
    const signatures = run('javap', ['-public', '-classpath', apiOnly, name]);
    assert(!/top\.csituka\.magicaland\.(client|network|cutiemark)\./.test(signatures), `Internal type exposed by ${name}`);
  }
  const addon = process.env.MAGICALAND_GAMEPLAY_REPO
    ? path.resolve(process.env.MAGICALAND_GAMEPLAY_REPO, 'src') : path.resolve(repo, '../gameplay/src');
  for (const file of (fs.existsSync(addon) ? files(addon) : []).filter(file => file.endsWith('.java'))) {
    for (const match of fs.readFileSync(file, 'utf8').matchAll(/top\.csituka\.magicaland\.([\w.]+)/g)) {
      assert(match[1] === 'gameplay' || match[1].startsWith('gameplay.') || match[1].startsWith('api.'),
        `Addon accesses internal type in ${file}: ${match[0]}`);
    }
  }
  console.log('API-only consumer compilation and public signatures passed');
  console.log(fs.existsSync(addon) ? 'Addon dependency boundary passed' : 'Addon boundary skipped: set MAGICALAND_GAMEPLAY_REPO to its checkout');
} finally {
  if (path.dirname(path.resolve(temp)) !== path.resolve(os.tmpdir())
      || !path.basename(temp).startsWith('magicaland-api-test-')) throw new Error('Unexpected temporary path');
  fs.rmSync(temp, { recursive: true, force: true });
}
