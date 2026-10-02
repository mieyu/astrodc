const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const compiler = require('vue-template-compiler');

const root = path.resolve(__dirname, '..');
const component = compiler.parseComponent(fs.readFileSync(path.join(root, 'src/components/EphemerisTool.vue'), 'utf8'));
const cases = [
  ['0 -3 38.040836', '-0 3 38.040836'],
  ['0 0 -0.040836', '-0 0 0.040836'],
  ['0 -0 38.040836', '-0 0 38.040836'],
  ['-0 3 38.040836', '-0 3 38.040836'],
  ['-12 3 38.040836', '-12 3 38.040836'],
  ['0 3 38.040836', '0 3 38.040836'],
  ['+12 3 38.040836', '+12 3 38.040836'],
  ['0 0 0.000000', '0 0 0.000000'],
  ['00 -03 38.040836123456789', '-00 03 38.040836123456789'],
  ['-12 -3 -38.040836', '-12 3 38.040836']
];
const display = value => {
  const [degrees, minutes, seconds] = value.split(' ');
  return `${degrees}° ${minutes}' ${seconds}"`;
};
const rows = cases.map(([value], index) => ({
  time: '2026-09-12 00:00:00', de: display(value), de_pure: value,
  ra: '0h -3m 38.040836s', ra_pure: '0 -3 38.040836', raw_line: `original-${index}`
}));
const requests = [];
const context = {
  module: { exports: {} },
  SATELLITE_GROUPS: require(path.join(root, 'src/data/ephemeris-targets.json')),
  axios: { post: async (url, payload) => {
    requests.push({ url, payload });
    return { data: url.endsWith('/calculate') ? { success: true, data: rows } : { success: false } };
  } },
  setTimeout: () => 0,
  clearTimeout: () => {}
};
vm.runInNewContext(component.script.content.replace(/^import .*;\s*$/gm, '').replace('export default', 'module.exports ='), context);
const options = context.module.exports;
const instance = { ...options.data(), $message: { warning: assert.fail, error: assert.fail } };
for (const [name, method] of Object.entries(options.methods)) instance[name] = method.bind(instance);

(async () => {
  await instance.submitCalculation();
  assert.equal(instance.results.length, cases.length);
  for (const [index, [input, expected]] of cases.entries()) {
    const row = instance.results[index];
    assert.equal(row.de, display(expected), `Display: ${input}`);
    assert.equal(row.de_pure, expected, `Plain declination: ${input}`);
    for (const field of ['ra', 'ra_pure', 'time', 'raw_line']) assert.equal(row[field], rows[index][field], `${field} must remain unchanged`);
    assert.equal(rows[index].de_pure, input, 'Original response must not be mutated');
    let copied;
    instance.copyText = async text => { copied = text; return true; };
    await instance.copyCoords(row, index);
    assert.equal(copied, `${row.ra_pure} ${expected}`);
    await instance.handlePreviewDSS(index);
    const request = requests.at(-1);
    assert.equal(request.url, '/api/ephemeris/download_chart');
    assert.equal(request.payload.dec, expected);
    assert.equal(request.payload.ra, rows[index].ra_pure);
  }
  for (const value of [null, undefined, '', 'unavailable']) assert.equal(context.normalizeDeclination(value), value);
  for (const [, expected] of cases) assert.equal(context.normalizeDeclination(expected), expected, 'Normalization must be idempotent');
  console.log('10 declination cases passed: display, copy, DSS request, negative zero and decimal precision; right ascension unchanged.');
})().catch(error => { console.error(error); process.exitCode = 1; });
