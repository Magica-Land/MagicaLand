import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const repo=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const read=p=>fs.readFileSync(path.join(repo,p),'utf8');
const clips=JSON.parse(read('src/main/resources/assets/magicaland/animations/mare_animation.json')).animations;
const geo=JSON.parse(read('src/main/resources/assets/magicaland/geo/mare_geo.json'))['minecraft:geometry'][0].bones;
const cadence=read('src/client/java/top/csituka/magicaland/client/sound/PonyHoofCadence.java');
let checks=0;
function yes(value,why){checks++;if(!value)throw new Error(why);}
for(const[name,length]of Object.entries({walk:.6667,run:.375,backward_walk:1,sneak:1.0417,land:.5,larger_land:1})){
 yes(clips[name].animation_length===length,`${name} authored length`);
 yes(clips[name].loop===true||(['land','larger_land'].includes(name)&&clips[name].loop==='hold_on_last_frame'),`${name} loop mode`);
 yes(new RegExp(`new Profile\\("${name}", (?:0)?${String(length).replace('.','\\.').replace(/^0/,'')},`).test(cadence),`${name} helper uses asset length`);
}
const marks={walk:{LFrontHoof:.25,RFrontHoof:.625,LHindHoof:0,RHindHoof:.2917},run:{LFrontHoof:.25,RFrontHoof:.0417,LHindHoof:.25,RHindHoof:0},backward_walk:{LFrontHoof:.9444,RFrontHoof:.4444,LHindHoof:.1944,RHindHoof:.6944},sneak:{LFrontHoof:.75,RFrontHoof:.25,LHindHoof:.4167,RHindHoof:.9167}};
for(const[action,feet]of Object.entries(marks))for(const[foot,time]of Object.entries(feet)){
 const bone=geo.find(b=>b.name===foot);
 yes(bone?.cubes?.length===1&&bone.cubes[0].origin[1]===0,`${foot} actual authored sole still present`);
 yes(bone.parent.endsWith('Calf'),`${foot} nested calf geometry retained`);
 const curves=Object.values(clips[action].bones[foot]);
 yes(curves.some(curve=>Object.keys(curve).some(key=>Math.abs(Number(key)-time)<1e-8)),`${action}/${foot} marker is an authored foot key`);
}
const sneak=read('src/client/java/top/csituka/magicaland/client/animation/PonySneakController.java');
yes(sneak.includes('NORMAL_SNEAK_LIMB_SPEED = .26, MIN_SPEED = .25'),'controller pace constants unchanged');
yes(sneak.includes('playbackTick += (tick - previousTick) * playbackSpeed'),'continuous speed integration retained');
yes(sneak.includes('Math.max(MIN_SPEED, Math.min(1, limbSpeed / NORMAL_SNEAK_LIMB_SPEED))'),'helper pace formula corresponds to controller');
console.log(`PonyHoofAssetTest: ${checks} checks PASS`);
