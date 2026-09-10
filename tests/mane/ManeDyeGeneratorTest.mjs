import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';

const repo = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const temp = fs.mkdtempSync(path.join(os.tmpdir(), 'magicaland-generator-test-'));
const styles = ['02','03','04','05','06','07','08'];
const masks = styles.flatMap(style => [
    `Resources/ManeDyes/style${style}.json`,
    `src/main/resources/assets/magicaland/mane_dyes/style${style}.json`
]);
try {
    for (const file of ['tools/generate-mane-dye-masks.mjs', 'Resources/BlockbenchProjects/Mare.bbmodel',
        'Resources/ManeDyes/flow-layout.json', ...masks]) {
        const target = path.join(temp, file);
        fs.mkdirSync(path.dirname(target), {recursive:true});
        fs.copyFileSync(path.join(repo, file), target);
    }
    const check = () => spawnSync(process.execPath, [path.join(temp, 'tools/generate-mane-dye-masks.mjs'), '--check'],
        {encoding:'utf8', maxBuffer:4 * 1024 * 1024});
    for (const newline of ['\n', '\r\n']) {
        for (const file of masks) {
            const target = path.join(temp, file);
            fs.writeFileSync(target, fs.readFileSync(target, 'utf8').replace(/\r\n/g, '\n').replace(/\n/g, newline));
        }
        const result = check();
        assert.equal(result.status, 0, `Generated data must match with ${JSON.stringify(newline)}: ${result.stderr.slice(0,500)}`);
    }
    const target = path.join(temp, masks[0]);
    const broken = JSON.parse(fs.readFileSync(target, 'utf8'));
    broken.regions[0].runs[0][0] += 1;
    fs.writeFileSync(target, JSON.stringify(broken) + '\r\n');
    const result = check();
    assert.notEqual(result.status, 0, 'Real region changes must still fail');
    assert.match(result.stderr, /遮罩与源坐标不一致/, 'Failure must be data mismatch, not unrelated setup');
    console.log('PASS ManeDyeGeneratorTest: LF, CRLF and real region mismatch (4 checks)');
} finally {
    fs.rmSync(temp, {recursive:true, force:true});
}
