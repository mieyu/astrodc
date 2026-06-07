const fs = require('fs');
const path = require('path');

const componentPath = path.join(__dirname, '..', 'src', 'components', 'CatalogBiasCorrection.vue');
const source = fs.readFileSync(componentPath, 'utf8');
const mapDir = path.join(__dirname, '..', 'public', 'catalog-bias-maps');
const catalogKeys = [
  'ac',
  'acrs',
  'act',
  'agk1',
  'agk3',
  'fk4catalogue',
  'gaiadr1',
  'gaiadr2',
  'gsc1_2',
  'hipparcos',
  'ppm',
  'sao',
  'tycho2',
  'ucac2',
  'ucac4',
  'usno_a2',
  'yale'
];

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

assert(!source.includes('概览视图'), 'Catalog bias page should not expose the overview view.');
assert(!source.includes('CORE_COLUMN_PROPS'), 'Catalog bias page should not keep the old core column set.');
assert(!source.includes("viewMode: 'core'"), 'Catalog bias page should not default to the removed core view.');
assert(source.includes('cbc-catalog-grid'), 'Catalog bias page should render a catalog selector grid.');
assert(source.includes('selectGroup(group)'), 'Catalog bias page should switch to a catalog-specific table.');
assert(source.includes('返回星表选择'), 'Catalog bias table should provide a way back to the catalog selector.');
assert(source.includes('catalogMapSrc'), 'Catalog bias page should compute a map image URL for the active catalog.');
assert(source.includes('cbc-map-figure'), 'Catalog bias page should render the active catalog map figure.');
assert(source.includes('查看原图'), 'Catalog bias page should provide a direct original-image link.');

assert(fs.existsSync(mapDir), 'Catalog bias map assets should live in public/catalog-bias-maps.');
const expectedMapFiles = catalogKeys.map(key => `${key}_systematics_map.png`);
const actualMapFiles = fs.readdirSync(mapDir).filter(file => file.endsWith('.png')).sort();
assert(
  JSON.stringify(actualMapFiles) === JSON.stringify(expectedMapFiles),
  `Catalog bias maps should include exactly ${expectedMapFiles.length} canonical map images.`
);
expectedMapFiles.forEach(file => {
  const imagePath = path.join(mapDir, file);
  const size = fs.statSync(imagePath).size;
  assert(size > 1000000, `${file} should be a real map image, not an empty placeholder.`);
});

console.log('Catalog bias UI verification passed.');
