import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const read = file => fs.readFileSync(path.join(root, file), 'utf8');
const json = file => JSON.parse(read(file));
const exists = file => fs.existsSync(path.join(root, file));
const walk = dir => fs.readdirSync(path.join(root, dir), {withFileTypes: true})
    .flatMap(e => e.isDirectory() ? walk(`${dir}/${e.name}`) : [`${dir}/${e.name}`]);
let checks = 0;
function check(condition, message) { checks++; assert.ok(condition, message); }

check(!exists('src'), 'no stale root source tree');
check(read('settings.gradle').includes("include 'appearance', 'gameplay'"), 'both modules included');
const appearance = json('appearance/src/main/resources/fabric.mod.json');
const gameplay = json('gameplay/src/main/resources/fabric.mod.json');
check(appearance.id === 'magicaland', 'appearance keeps legacy mod ID');
check(gameplay.id === 'magicaland_gameplay', 'gameplay has a distinct mod ID');
check(!appearance.depends.magicaland_gameplay, 'appearance does not require gameplay');
check(gameplay.depends.magicaland === '${version}', 'gameplay requires matching appearance version');
check(appearance.environment === '*' && gameplay.environment === '*', 'both modules support server loading');
check(appearance.depends.geckolib && appearance.depends['fabric-api'], 'appearance technical dependencies retained');

const allClasses = new Set();
for (const module of ['appearance', 'gameplay']) {
    const metadata = module === 'appearance' ? appearance : gameplay;
    const sourceFiles = walk(`${module}/src`).filter(f => f.endsWith('.java'));
    const classes = new Set();
    for (const file of sourceFiles) {
        const source = read(file);
        const pkg = source.match(/^package ([\w.]+);/m)?.[1];
        check(pkg && file.endsWith(pkg.replaceAll('.', '/') + '/' + path.basename(file)), `package matches path: ${file}`);
        const name = pkg + '.' + path.basename(file, '.java');
        check(!allClasses.has(name), `no duplicated class: ${name}`);
        classes.add(name); allClasses.add(name);
        if (module === 'appearance') {
            check(!/magicaland\.gameplay|CarrotMisunderstanding|CarrotApproachGoal|HorseGoalSelectorAccessor/.test(source), `appearance independent: ${file}`);
        }
        if (file.includes('/src/main/java/')) {
            check(!/^import (?:net\.minecraft\.client\.|top\.csituka\.magicaland\.client\.)/m.test(source), `common code avoids client imports: ${file}`);
        }
    }
    for (const entries of Object.values(metadata.entrypoints)) {
        for (const entry of entries) check(classes.has(typeof entry === 'string' ? entry : entry.value), `entrypoint belongs to ${module}`);
    }
    for (const entry of metadata.mixins) {
        const name = typeof entry === 'string' ? entry : entry.config;
        const candidates = ['main', 'client'].map(side => `${module}/src/${side}/resources/${name}`).filter(exists);
        check(candidates.length === 1, `unique mixin config: ${name}`);
        const config = json(candidates[0]);
        for (const short of [...(config.mixins ?? []), ...(config.client ?? []), ...(config.server ?? [])]) {
            check(classes.has(config.package + '.' + short), `mixin exists: ${short}`);
        }
    }
    for (const file of walk(`${module}/src`).filter(f => f.endsWith('.json'))) {
        json(file); checks++;
    }
}

const appBuild = read('appearance/build.gradle');
const gameBuild = read('gameplay/build.gradle');
check(!appBuild.includes(":gameplay"), 'no reverse build dependency');
check(gameBuild.includes("configuration: 'namedElements'"), 'gameplay compiles against named appearance output');
check(gameBuild.includes("clientImplementation project(':appearance').sourceSets.client.output"), 'client output present in combined dev run');
check(gameBuild.includes("sourceSet project(':appearance').sourceSets.main")
    && gameBuild.includes("sourceSet project(':appearance').sourceSets.client"), 'appearance dev classpath group complete');
check(!/^\s*include\b/m.test(gameBuild), 'appearance not embedded in gameplay');
check(appBuild.includes('-appearance') && gameBuild.includes('-gameplay'), 'distinct artifact names');
check(!exists('appearance/src/main/resources/magicaland.mixins.json'), 'old gameplay mixin registration removed');
check(exists('appearance/src/main/resources/' + appearance.accessWidener), 'appearance access widener retained');

const advancementPath = 'src/main/resources/data/magicaland/advancements/not_what_i_meant.json';
check(!exists('appearance/' + advancementPath), 'appearance cannot grant the encounter advancement');
const advancement = json('gameplay/' + advancementPath);
check(advancement.criteria.misunderstanding.trigger === 'minecraft:impossible', 'legacy advancement criterion preserved');
check(read('gameplay/src/main/java/top/csituka/magicaland/gameplay/MagicalLandGameplay.java').includes('CarrotMisunderstanding.register();'), 'gameplay registers encounter');
check(read('gameplay/src/main/java/top/csituka/magicaland/gameplay/easteregg/CarrotMisunderstanding.java')
    .includes('new Identifier("magicaland", "not_what_i_meant")'), 'legacy advancement ID retained');
for (const lang of ['zh_cn', 'en_us']) {
    const file = `src/main/resources/assets/magicaland/lang/${lang}.json`;
    const app = json('appearance/' + file), game = json('gameplay/' + file);
    for (const key of Object.keys(game)) check(!(key in app), `no language-key collision: ${lang}/${key}`);
    for (const element of ['title', 'description']) {
        check(game[advancement.display[element].translate], `gameplay owns advancement ${element}: ${lang}`);
    }
}
const appResources = new Set(walk('appearance/src').filter(f => f.includes('/resources/'))
    .map(f => f.split('/resources/')[1]));
for (const file of walk('gameplay/src').filter(f => f.includes('/resources/'))) {
    const relative = file.split('/resources/')[1];
    check(!appResources.has(relative) || relative === 'fabric.mod.json' || /^assets\/magicaland\/lang\//.test(relative), `no duplicated payload: ${relative}`);
}
for (const folder of ['tests', 'tools']) {
    for (const file of walk(folder).filter(f => /\.(java|mjs|ps1)$/.test(f) && !f.endsWith('ModuleSplitTest.mjs'))) {
        check(!/["'`]src\/(?:main|client)\//.test(read(file)), `test/tool uses migrated paths: ${file}`);
    }
}
console.log(`PASS ModuleSplitTest: ${checks} checks`);
