const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const pkg = JSON.parse(fs.readFileSync(path.join(root, 'package.json'), 'utf8'));
const deploy = pkg.scripts && pkg.scripts.deploy;
const failures = [];

if (!deploy) {
  failures.push('package.json is missing scripts.deploy');
} else {
  const required = [
    'npm run test:api-config',
    'npm run test:access-gate',
    'npm run build',
    'npx wrangler pages deploy dist --project-name astrodc --branch main --commit-dirty=true',
  ];

  for (const command of required) {
    if (!deploy.includes(command)) {
      failures.push(`scripts.deploy is missing: ${command}`);
    }
  }
}

if (failures.length) {
  console.error(failures.join('\n'));
  process.exit(1);
}

console.log('Deploy script is configured.');
