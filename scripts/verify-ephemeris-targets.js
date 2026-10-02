const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const compiler = require('vue-template-compiler');

const root = path.resolve(__dirname, '..');
const groups = require(path.join(root, 'src/data/ephemeris-targets.json'));
const targets = groups.flatMap(group => group.targets);
assert.deepEqual(groups.map(group => group.targets.length), [3, 10, 60, 62, 29, 16, 5]);
assert.equal(new Set(targets.map(target => target.code)).size, 185);
assert.equal(new Set(targets.map(target => target.name)).size, 185);
const exceptions = {
  J14: '5006', J15: '5007', J16: '5008', J6: '10001',
  J_BARYCENTER_INNER: '5999', J_BARYCENTER_OUTER: '10000',
  S32: '6044', S33: '6045', S34: '6046', S35: '6059', S49: '6060',
  U24: '7022', U22: '7023', U23: '7024', U26: '7025', U27: '7026', U25: '7027',
  N3: '8008', N4: '8007', N7: '8004', N8: '8003', N10: '8013', N11: '8010',
  P0: '9004', P_BARYCENTER: '9000', 'S/2003 J2': '10033', 'S/2004 S 7': '6032'
};
for (const [name, code] of Object.entries(exceptions)) {
  assert.equal(targets.find(target => target.name === name)?.code, code, name);
}

if (process.argv[2]) {
  const supplied = fs.readFileSync(process.argv[2], 'utf8').split(/\r?\n/)
    .filter(line => line.startsWith('|') && line.includes('`'))
    .map(line => {
      const [name, code, type, label] = line.split('|').map(value => value.trim()).filter(Boolean);
      return { name, code: code.replaceAll('`', ''), type, label };
    });
  assert.deepEqual(targets, supplied, 'Every target must match the supplied source, in order');
}

const component = compiler.parseComponent(fs.readFileSync(path.join(root, 'src/components/EphemerisTool.vue'), 'utf8'));
assert.deepEqual(compiler.compile(component.template.content).errors, []);
const requests = [];
const script = component.script.content
  .replace(/^import .*;\s*$/gm, '')
  .replace('export default', 'module.exports =');
const context = {
  module: { exports: {} }, SATELLITE_GROUPS: groups,
  axios: { post: async (url, payload) => { requests.push({ url, payload }); return { data: { success: true, data: [] } }; } }
};
vm.runInNewContext(script, context);
const options = context.module.exports;
const instance = { ...options.data(), $message: { warning: assert.fail, error: assert.fail } };
for (const [name, method] of Object.entries(options.methods)) instance[name] = method.bind(instance);

(async () => {
  for (const target of targets) {
    instance.selectedSat = target.code;
    instance.onSatelliteChange();
    assert.equal(instance.resolveSatelliteName(), target.name);
    await instance.submitCalculation();
    const request = requests.at(-1);
    assert.equal(request.url, '/api/ephemeris/calculate');
    assert.equal(request.payload.satellite, target.code, target.name);
    assert.equal(instance.currentSatelliteName, target.name);
  }
  instance.customSatellite = ' 10033 ';
  instance.onCustomChange(instance.customSatellite);
  assert.equal(instance.selectedSat, '');
  assert.equal(instance.resolveSatellite(), '10033');
  instance.selectedSat = '9004';
  instance.onSatelliteChange();
  assert.equal(instance.customSatellite, '');
  assert.equal(instance.resolveSatelliteName(), 'P0');
  console.log('185 mappings, template compilation, calculation payloads and custom selection passed.');
})().catch(error => { console.error(error); process.exitCode = 1; });
