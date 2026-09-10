import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';

const repo=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const write=process.argv.includes('--write');
assert(process.argv.slice(2).every(arg=>arg==='--write'||arg==='--check'),'只支持 --check 或 --write');
const model=JSON.parse(await fs.readFile(path.join(repo,'Resources/BlockbenchProjects/Mare.bbmodel'),'utf8'));
const layout=JSON.parse(await fs.readFile(path.join(repo,'Resources/ManeDyes/flow-layout.json'),'utf8'));
assert.equal(layout.version,1);
const groups=new Map(model.groups.map(g=>[g.uuid,g.name])),owners=new Map();
function walk(nodes,parent){for(const node of nodes)if(typeof node==='string')owners.set(node,groups.get(parent));else walk(node.children||[],node.uuid);}
walk(model.outliner,null);
const fields=new Map();
for(const field of layout.fields){
 assert(!fields.has(field.uuid),'重复发块 '+field.uuid);
 assert([0,1,2].includes(field.axis)&&[field.center,field.u,field.slope].every(Number.isFinite),'非法发流坐标');
 fields.set(field.uuid,field);
}
function faces(f,t){return{
 east:[[t[0],t[1],t[2]],[t[0],t[1],f[2]],[t[0],f[1],t[2]]],
 west:[[f[0],t[1],f[2]],[f[0],t[1],t[2]],[f[0],f[1],f[2]]],
 up:[[f[0],t[1],f[2]],[t[0],t[1],f[2]],[f[0],t[1],t[2]]],
 down:[[f[0],f[1],t[2]],[t[0],f[1],t[2]],[f[0],f[1],f[2]]],
 south:[[f[0],t[1],t[2]],[t[0],t[1],t[2]],[f[0],f[1],t[2]]],
 north:[[t[0],t[1],f[2]],[f[0],t[1],f[2]],[t[0],f[1],f[2]]]
};}
const styles=['02','03','04','05','06','07','08'],grids=new Map(),visited=new Set();let faceCount=0;
for(const element of model.elements){
 if(element.export===false)continue;
 const bone=owners.get(element.uuid),match=bone?.match(/^Style(0[2-8])(FrontMane|BackMane|Tail)/);if(!match)continue;
 const style=match[1],part={FrontMane:'FRONT',BackMane:'BACK',Tail:'TAIL'}[match[2]],field=fields.get(element.uuid);
 assert(field&&field.bone===bone&&field.style===style&&field.part===part,'缺少或过期发流坐标 '+bone+' '+element.uuid);visited.add(element.uuid);
 const key=style+':'+part;if(!grids.has(key))grids.set(key,new Uint8Array(256*256));const grid=grids.get(key);
 for(const[face,vertices]of Object.entries(faces(element.from,element.to))){
  const f=element.faces[face];if(f?.texture===null)continue;assert(f?.uv&&!f.rotation,'需要无旋转逐面 UV '+bone+' '+face);
  const[x,y,x1,y1]=f.uv.map(v=>v*2),w=x1-x,h=y1-y;
  assert([x,y,x1,y1].every(Number.isInteger)&&w>0&&h>0&&x>=1&&y>=1&&x1<256&&y1<256,'UV 需在图集内留一像素边缘');
  const colors=new Uint8Array(w*h),axis=field.axis;
  for(let v=0;v<h;v++)for(let u=0;u<w;u++){
   const local=vertices[0][axis]+(vertices[1][axis]-vertices[0][axis])*(u+.5)/w+(vertices[2][axis]-vertices[0][axis])*(v+.5)/h;
   const phase=Math.max(0,Math.min(.999999,field.u+(local-field.center)*field.slope));colors[v*w+u]=1+Math.floor(phase*6+1e-10);
  }
  for(let v=-1;v<=h;v++)for(let u=-1;u<=w;u++){
   const index=(y+v)*256+x+u,color=colors[Math.max(0,Math.min(h-1,v))*w+Math.max(0,Math.min(w-1,u))];
   assert(!grid[index]||grid[index]===color,'同款同部件 UV/边缘色区冲突，请先拆开 UV：'+bone+' '+face+' @ '+(x+u)+','+(y+v));grid[index]=color;
  }faceCount++;
 }
}
assert.equal(visited.size,fields.size,'发流坐标存在已移除的发块');
const outputs=[];let regionCount=0;
for(const style of styles){const regions=[];
 for(const part of ['FRONT','BACK','TAIL']){
  const grid=grids.get(style+':'+part);if(!grid)continue;
  assert(!(style==='07'&&part==='BACK')&&!(style==='08'&&part==='TAIL'),'未完成的部件不能误加入');
  for(let channel=1;channel<=6;channel++){
   const runs=[];for(let y=0;y<256;y++)for(let x=0;x<256;x++)if(grid[y*256+x]===channel){const start=x;while(x+1<256&&grid[y*256+x+1]===channel)x++;runs.push([y,start,x+1]);}
   assert(runs.length,'空色区 '+style+' '+part+' '+channel);regions.push({part,channel,runs});regionCount++;
  }
 }
 assert(regions.flatMap(r=>r.runs).length<=8192,'遮罩超出运行时上限');
 const text=JSON.stringify({version:3,preset:'style'+style,style,texture_width:256,texture_height:256,regions})+'\n';
 for(const folder of ['Resources/ManeDyes','src/main/resources/assets/magicaland/mane_dyes'])outputs.push({file:path.join(repo,folder,'style'+style+'.json'),text});
}
// 全部资源通过检查后才允许写入；永不改动 01、模型或贴图。
for(const{file,text}of outputs)if(write)await fs.writeFile(file,text);else assert.equal((await fs.readFile(file,'utf8')).replace(/\r\n/g,'\n'),text,'遮罩与源坐标不一致 '+file);
console.log(`${write?'已生成':'检查通过'}：${visited.size} 个发块、${faceCount} 个面、${regionCount} 个色区；01 未改动。`);
