import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const baseline = process.argv[2];
if (!baseline || !/^[a-f0-9]{7,40}$/.test(baseline)) throw Error('Provide the pre-split commit hash');
const git = args => execFileSync('git', args, {cwd: root, maxBuffer: 32 * 1024 * 1024});
const files = git(['ls-tree', '-r', '--name-only', baseline, '--', 'src']).toString().trim().split('\n');
assert.ok(files.length > 100, 'baseline must contain the original source tree');
const batch = execFileSync('git', ['cat-file', '--batch'], {
    cwd: root, input: files.map(f => `${baseline}:${f}\n`).join(''), maxBuffer: 32 * 1024 * 1024
});
let offset = 0, checks = 0;
for (const file of files) {
    const end = batch.indexOf(10, offset);
    const header = batch.subarray(offset, end).toString().split(' ');
    assert.equal(header[1], 'blob');
    const size = Number(header[2]);
    const original = batch.subarray(end + 1, end + 1 + size);
    offset = end + 1 + size + 1;
    let target = 'appearance/' + file;
    if (/^src\/main\/java\/top\/csituka\/magicaland\/(easteregg|mixin)\//.test(file)) {
        target = 'gameplay/' + file.replace('/magicaland/', '/magicaland/gameplay/');
    } else if (file.endsWith('/magicaland.mixins.json')) {
        target = 'gameplay/src/main/resources/magicaland.gameplay.mixins.json';
    } else if (file.endsWith('/advancements/not_what_i_meant.json')) {
        target = 'gameplay/' + file;
    }
    const actual = fs.readFileSync(path.join(root, target));
    if (/\.(png|ogg)$/.test(file)) {
        assert.ok(original.equals(actual), `binary unchanged: ${file}`);
    } else {
        let expected = original.toString('utf8').replaceAll('\r\n', '\n');
        const current = actual.toString('utf8').replaceAll('\r\n', '\n');
        if (target.startsWith('gameplay/') && file.endsWith('.java')) {
            expected = expected.replaceAll('top.csituka.magicaland.easteregg', 'top.csituka.magicaland.gameplay.easteregg')
                .replaceAll('top.csituka.magicaland.mixin', 'top.csituka.magicaland.gameplay.mixin');
        }
        if (file.endsWith('/Magical_Land.java')) expected = expected
            .replace('import top.csituka.magicaland.easteregg.CarrotMisunderstanding;\n', '')
            .replace('        CarrotMisunderstanding.register();\n', '');
        if (file.endsWith('/magicaland.mixins.json')) expected = expected
            .replace('top.csituka.magicaland.mixin', 'top.csituka.magicaland.gameplay.mixin');
        if (file.endsWith('/fabric.mod.json')) {
            const old = JSON.parse(expected), now = JSON.parse(current);
            old.name = 'Magical Land: Appearance';
            old.description = 'Pony appearance, customization and animation. Independent of the gameplay module.';
            old.mixins = old.mixins.filter(m => m !== 'magicaland.mixins.json');
            assert.deepEqual(now, old, 'only intended appearance metadata edits');
        } else if (/\/lang\/(en_us|zh_cn)\.json$/.test(file)) {
            const game = JSON.parse(fs.readFileSync(path.join(root, 'gameplay/' + file), 'utf8'));
            assert.deepEqual({...JSON.parse(current), ...game}, JSON.parse(expected), 'translations preserved across split');
        } else {
            assert.equal(current, expected, `only intended migration edits: ${file}`);
        }
    }
    checks++;
}
console.log(`PASS MigrationSnapshotTest: ${checks} original files preserved or explicitly migrated`);
