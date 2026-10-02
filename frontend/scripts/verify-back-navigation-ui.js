const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..');
const stylePath = path.join(root, 'src', 'assets', 'back-navigation.css');
const mainPath = path.join(root, 'src', 'main.js');
const componentNames = [
  'CatalogBiasCorrection.vue',
  'Search.vue',
  'DataVisualization.vue',
  'EphemerisTool.vue',
  'FileBrowser.vue',
  'TableList.vue'
];

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

assert(fs.existsSync(stylePath), 'Shared back navigation stylesheet should exist.');

const style = fs.readFileSync(stylePath, 'utf8');
const main = fs.readFileSync(mainPath, 'utf8');

assert(main.includes("import './assets/back-navigation.css'"), 'main.js should import shared back navigation styles.');
assert(style.includes('.app-back-bar'), 'Shared style should define the back bar.');
assert(style.includes('.app-back-button'), 'Shared style should define the back button.');
assert(style.includes('.app-back-context'), 'Shared style should define optional right-side context.');
assert(style.includes('border-bottom: 1px solid #eef0f4'), 'Back bar should use a restrained hairline divider instead of a boxed card.');
assert(!style.includes('box-shadow'), 'Back bar should stay flat and minimal without a card shadow.');
assert(style.includes('height: 30px'), 'Back button should have a stable compact height.');
assert(style.includes('background: transparent'), 'Back button should be a quiet ghost button by default.');

componentNames.forEach(name => {
  const source = read(path.join('src', 'components', name));
  assert(source.includes('app-back-button'), `${name} should use the shared back button.`);
  assert(!source.includes('back-link-container'), `${name} should not keep the old back-link-container wrapper.`);
  assert(!source.includes('class="el-icon-back"'), `${name} should not render a bare Element back icon.`);
  assert(!source.includes('icon="el-icon-back"'), `${name} should not use the old Element back icon button.`);
});

const catalogBias = read(path.join('src', 'components', 'CatalogBiasCorrection.vue'));
const ephemeris = read(path.join('src', 'components', 'EphemerisTool.vue'));
const tableList = read(path.join('src', 'components', 'TableList.vue'));
const fileBrowser = read(path.join('src', 'components', 'FileBrowser.vue'));

assert(catalogBias.includes('app-back-bar cbc-table-head'), 'Catalog bias detail view should use the shared back bar.');
assert(catalogBias.includes('app-back-context'), 'Catalog bias detail view should show current catalog as right-side context.');
assert(ephemeris.includes('app-back-context header-clock'), 'Ephemeris UTC clock should live in the shared back bar context.');
assert(tableList.includes('app-back-actions'), 'Table list should keep download as a secondary action in the shared toolbar.');
assert(fileBrowser.includes('app-back-context breadcrumb-path'), 'File browser breadcrumb should become right-side context.');

console.log('Back navigation UI verification passed.');
