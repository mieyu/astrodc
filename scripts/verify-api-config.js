const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const files = [
  'src/main.js',
  'src/components/AiAgent.vue',
];

const contents = files.map((file) => {
  const absolute = path.join(root, file);
  return {
    file,
    text: fs.readFileSync(absolute, 'utf8'),
  };
});

const forbidden = [
  '10.126.126.2:8088',
  'http://10.126.126.2:8088',
];

const failures = [];

for (const { file, text } of contents) {
  for (const value of forbidden) {
    if (text.includes(value)) {
      failures.push(`${file} still contains ${value}`);
    }
  }
}

const main = contents.find(({ file }) => file === 'src/main.js').text;
if (!main.includes('process.env.VUE_APP_API_BASE_URL')) {
  failures.push('src/main.js does not read VUE_APP_API_BASE_URL');
}


const productionEnvPath = path.join(root, '.env.production');
if (!fs.existsSync(productionEnvPath)) {
  failures.push('.env.production is missing');
} else {
  const productionEnv = fs.readFileSync(productionEnvPath, 'utf8');
  if (!productionEnv.includes('VUE_APP_API_BASE_URL=https://api.astrodc.top')) {
    failures.push('.env.production does not point VUE_APP_API_BASE_URL at https://api.astrodc.top');
  }
}
const aiAgent = contents.find(({ file }) => file === 'src/components/AiAgent.vue').text;
if (!aiAgent.includes("axios.post('/analyze'")) {
  failures.push('src/components/AiAgent.vue does not use the shared axios base URL for /analyze');
}

if (failures.length) {
  console.error(failures.join('\n'));
  process.exit(1);
}

console.log('API configuration is environment-based.');
