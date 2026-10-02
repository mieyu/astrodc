const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');
const failures = [];

const main = read('src/main.js');
const app = read('src/App.vue');

if (!main.includes('axios.defaults.withCredentials = true')) {
  failures.push('src/main.js must enable axios.defaults.withCredentials for access cookies');
}
if (!main.includes('axios.interceptors.response.use')) {
  failures.push('src/main.js must install a 401 response interceptor');
}
if (!main.includes('astronomy-access-required')) {
  failures.push('src/main.js must notify App.vue when access is required');
}

const requiredAppSnippets = [
  "axios.post('/api/access/unlock'",
  "axios.get('/api/access/me'",
  "axios.post('/api/access/logout'",
  'access-lock',
  'accessKey',
  'unlockAccess',
];

for (const snippet of requiredAppSnippets) {
  if (!app.includes(snippet)) {
    failures.push(`src/App.vue missing ${snippet}`);
  }
}

if (failures.length) {
  console.error(failures.join('\n'));
  process.exit(1);
}

console.log('Access gate configuration is present.');
