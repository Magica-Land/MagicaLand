import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const read = file => fs.readFileSync(path.join(root, file), 'utf8');
const base = 'src/client/java/top/csituka/magicaland/';
const sounds = read(base + 'client/sound/PonyHoofSounds.java');
const model = read(base + 'client/model/GeckoPlayerAnimatable.java');
const renderer = read(base + 'client/render/PonyRenderer.java');
const controller = read(base + 'client/animation/PonySneakController.java');
let checks = 0;
const check = (condition, label) => { checks++; assert.ok(condition, label); };
check(sounds.includes('ClientTickEvents.END_CLIENT_TICK.register(PonyHoofSounds::tick)'), 'sounds run independently of body visibility');
check(sounds.includes('ClientNetworkHandler.supportsHoofSteps()') && sounds.includes('remoteModels.containsKey'), 'remote takeover requires protocol and a pony model');
for (const guard of ['player.isSpectator()', 'player.hasVehicle()', 'player.isSleeping()', 'player.isTouchingWater()',
    'player.isInLava()', 'player.isClimbing()', 'player.isFallFlying()', 'player.getAbilities().flying',
    'PonyFlightVisuals.flying(player)', 'player.isRemoved()', 'player.isSilent()', 'client.isPaused()'])
    check(sounds.includes(guard), `preserved state: ${guard}`);
check(sounds.includes('player == client.player ? ModelManager.getAppliedModel() != null'), 'local pure-client use needs no server handshake');
check(sounds.includes('client.world != trackedWorld || client.player != trackedSelf'), 'dimension and respawn clear timeline');
check(sounds.includes('TRACKS.keySet().retainAll(present)'), 'unloaded, distant or human models release state');
check(sounds.includes('SoundCategory.PLAYERS') && !sounds.includes('SoundCategory.NEUTRAL'), 'horse samples respect player sound slider');
check(sounds.includes('SoundEvents.ENTITY_HORSE_STEP_WOOD') && sounds.includes('SoundEvents.ENTITY_HORSE_STEP;'), 'hard and soft surfaces use separate samples');
check(!sounds.includes('playSoundFromEntity(') && !sounds.includes('sendPacket('), 'playback stays client local, never rebroadcasts');
check(model.includes('if (worldSoundPass && player != null)'), 'only real world model exports phases');
check(renderer.includes('animatable.setWorldSoundPass(worldFlightRender())')
    && renderer.includes('animatable.setWorldSoundPass(previousSoundPass)'), 'world render phase scope restored');
check(renderer.includes('HornAuraPass.isWorld() && PonyGuiGaze.current() == null'), 'inventory and UI do not feed world phase');
check(controller.includes('animationState == State.RUNNING'), 'transition has no false contact phase');
check(!controller.includes('PonyHoofSounds') && !controller.includes('playSound'), 'controller is a read-only observer, not sound emitter');
check(model.includes('AnimationSelection selection = resolveMainAnimation()') && model.includes('ClientNetworkHandler.getRemoteAnimation'), 'same action selector and remote animation source');
console.log(`PASS ${checks} hoof integration structure checks (source wiring, not an in-game listening test)`);
