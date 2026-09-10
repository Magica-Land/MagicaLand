import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const read = file => fs.readFileSync(path.join(root, file), 'utf8');
const exists = file => fs.existsSync(path.join(root, file));
const json = file => JSON.parse(read(file));
const walk = dir => fs.readdirSync(path.join(root, dir), {withFileTypes: true})
    .flatMap(entry => entry.isDirectory() ? walk(`${dir}/${entry.name}`) : [`${dir}/${entry.name}`]);
const properties = Object.fromEntries(read('gradle.properties').split(/\r?\n/)
    .filter(line => line && !line.startsWith('#')).map(line => {
        const separator = line.indexOf('=');
        return [line.slice(0, separator), line.slice(separator + 1)];
    }));
let checks = 0;
function check(condition, message) { checks++; assert.ok(condition, message); }

check(exists('src/main/java') && exists('src/client/java'), 'root contains common and client sources');
check(!exists('appearance') && !exists('gameplay'), 'no nested legacy modules');
check(!/^\s*include(?:Build)?\b/m.test(read('settings.gradle')), 'single independent Gradle project');
const build = read('build.gradle');
check(!/evaluationDependsOn|project\s*\(/.test(build), 'build has no sibling project dependencies');
check(!/magicaland-gameplay|magicaland_gameplay/.test(build + read('gradle.properties')), 'no reverse gameplay dependency');
check(build.includes("version '1.16.3'"), 'Loom version is pinned');
check(/^\d+\.\d+\.\d+(?:[-+].+)?$/.test(properties.mod_version), 'appearance has its own semantic release version');
check(properties.archives_base_name === 'magicaland-appearance', 'appearance artifact coordinate');
check(build.includes("tasks.register('apiJar', Jar)") && build.includes("tasks.named('remapJar')"), 'API classes come from remapped mod output');
check(build.includes("include 'top/csituka/magicaland/api/**'"), 'classifier restricts implementation surface');
check(build.includes("'Fabric-Loom-Remap': 'true'"), 'API-only dependency opts into mapping conversion');
check(build.includes("archiveClassifier = 'api-sources'"), 'API sources are published');
check(build.includes("gradleProperty('appearanceMavenRepo')"), 'publication directory is configurable');

const metadata = json('src/main/resources/fabric.mod.json');
check(metadata.id === 'magicaland', 'legacy appearance mod ID remains');
check(metadata.environment === '*', 'one jar supports client and common/server synchronization');
check(metadata.entrypoints.main?.length > 0 && metadata.entrypoints.client?.length > 0, 'sync and client entrypoints remain');
check(!metadata.depends.magicaland_gameplay, 'appearance runs without gameplay');
check(metadata.depends.geckolib && metadata.depends['fabric-api'], 'technical runtime dependencies remain');
check(exists('src/main/resources/' + metadata.accessWidener), 'access widener remains');

const classes = new Set();
for (const file of walk('src').filter(file => file.endsWith('.java'))) {
    const source = read(file);
    const pkg = source.match(/^package ([\w.]+);/m)?.[1];
    check(pkg && file.endsWith(pkg.replaceAll('.', '/') + '/' + path.basename(file)), `package path: ${file}`);
    const name = pkg + '.' + path.basename(file, '.java');
    check(!classes.has(name), `no duplicate class: ${name}`);
    classes.add(name);
    check(!/top\.csituka\.magicaland\.gameplay|CarrotMisunderstanding|CarrotApproachGoal|HorseGoalSelectorAccessor|automaticAbilityThirdPerson|AppearancePreferences/.test(source), `no gameplay coupling: ${file}`);
    if (file.startsWith('src/main/')) {
        check(!/^import (?:net\.minecraft\.client\.|top\.csituka\.magicaland\.(?:client|api\.client)\.)/m.test(source), `common source avoids client API: ${file}`);
    }
}
for (const entries of Object.values(metadata.entrypoints)) {
    for (const entry of entries) check(classes.has(typeof entry === 'string' ? entry : entry.value), `entrypoint exists: ${entry}`);
}
for (const entry of metadata.mixins) {
    const name = typeof entry === 'string' ? entry : entry.config;
    const candidates = ['main', 'client'].map(side => `src/${side}/resources/${name}`).filter(exists);
    check(candidates.length === 1, `unique mixin config: ${name}`);
    const config = json(candidates[0]);
    for (const name of [...(config.mixins ?? []), ...(config.client ?? []), ...(config.server ?? [])]) {
        check(classes.has(config.package + '.' + name), `mixin class exists: ${name}`);
    }
}
for (const name of ['ApiVersion', 'client.Registration', 'client.AppearanceSnapshot',
    'client.Appearances', 'client.AppearanceOverrides', 'client.AppearanceVisuals']) {
    check(classes.has('top.csituka.magicaland.api.' + name), `public API exists: ${name}`);
}
check(!exists('src/main/resources/data/magicaland/advancements/not_what_i_meant.json'), 'encounter advancement is owned by gameplay');
for (const locale of ['zh_cn', 'en_us']) {
    check(!Object.keys(json(`src/main/resources/assets/magicaland/lang/${locale}.json`))
        .some(key => key.includes('ability_view')), 'ability perspective labels are owned by gameplay');
}
for (const file of walk('src').filter(file => file.endsWith('.json'))) { json(file); checks++; }
for (const file of ['gradlew', 'gradlew.bat', 'gradle/wrapper/gradle-wrapper.jar']) check(exists(file), `wrapper present: ${file}`);
check(read('gradle/wrapper/gradle-wrapper.properties').includes('distributionSha256Sum='), 'wrapper distribution has checksum');
console.log(`PASS Appearance RepositoryArchitectureTest: ${checks} checks (source/configuration only)`);
