const fs = require('fs');
const path = require('path');

const componentPath = path.join(__dirname, '..', 'src', 'components', 'CatalogBiasCorrection.vue');
const source = fs.readFileSync(componentPath, 'utf8');
const mapDir = path.join(__dirname, '..', 'public', 'catalog-bias-maps');
const headPanelStyle = source.match(/\.cbc-head-panel\s*\{([\s\S]*?)\n\}/)?.[1] || '';
const cardStyle = source.match(/\.cbc-catalog-card\s*\{([\s\S]*?)\n\}/)?.[1] || '';
const cardHoverStyle = source.match(/\.cbc-catalog-card:hover,[\s\S]*?\.cbc-catalog-card:focus\s*\{([\s\S]*?)\n\}/)?.[1] || '';
const summaryStyle = source.match(/\.cbc-summary\s*\{([\s\S]*?)\n\}/)?.[1] || '';
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
assert(!source.includes('cbc-catalog-meta'), 'Catalog selector cards should not show redundant field-count metadata.');
assert(!source.includes('个字段'), 'Catalog selector cards should not show field-count helper text.');
assert(!cardStyle.includes('border-left'), 'Catalog selector cards should not use a left-side accent border.');
assert(!cardHoverStyle.includes('border-left'), 'Catalog selector card hover state should not use a left-side accent border.');
assert(!source.includes('cbc-catalog-arrow'), 'Catalog selector cards should not show a right-side arrow.');
assert(!source.includes('el-icon-arrow-right'), 'Catalog selector cards should not render a right-side arrow icon.');
assert(cardStyle.includes('border-radius: 8px'), 'Catalog selector cards should match the own images entry card radius.');
assert(cardStyle.includes('border: 1px solid #ebeef5'), 'Catalog selector cards should match the own images entry card border.');
assert(cardHoverStyle.includes('box-shadow: 0 4px 14px rgba(64, 158, 255, 0.15)'), 'Catalog selector hover should match the own images entry hover shadow.');
assert(cardHoverStyle.includes('transform: translateY(-2px)'), 'Catalog selector hover should match the own images entry hover lift.');
assert(source.includes('cbc-head-panel'), 'Catalog bias title and summary should live in one top panel.');
assert(source.includes('<section v-if="!activeGroup" class="cbc-head-panel">'), 'Catalog bias top panel should only appear on the catalog selector view.');
assert(source.includes('cbc-summary'), 'Catalog bias page should render a scientific dataset summary under the title.');
assert(source.includes('17 个历史星表'), 'Catalog bias summary should describe the 17 historical star catalogs.');
assert(source.includes('N_side = 64 和 256'), 'Catalog bias summary should mention the HEALPix N_side values.');
assert(
  source.indexOf('class="cbc-head-panel"') < source.indexOf('class="cbc-catalog-grid"') &&
  source.indexOf('class="cbc-title"') > source.indexOf('class="cbc-head-panel"') &&
  source.indexOf('class="cbc-summary"') > source.indexOf('class="cbc-title"') &&
  source.indexOf('class="cbc-summary"') < source.indexOf('class="cbc-catalog-grid"'),
  'Catalog bias title and summary should appear together above the data area.'
);
assert(headPanelStyle.includes('border: 1px solid var(--border)'), 'Catalog bias top panel should match the shared panel border token.');
assert(headPanelStyle.includes('border-radius: var(--radius)'), 'Catalog bias top panel should match the shared panel radius token.');
assert(headPanelStyle.includes('background: var(--bg-elevated)'), 'Catalog bias top panel should use the shared elevated surface, not a gradient.');
assert(!summaryStyle.includes('border:'), 'Catalog bias summary text should not be a separate bordered card inside the top panel.');
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
