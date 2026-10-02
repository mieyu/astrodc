const { spawnSync } = require('node:child_process');
const path = require('node:path');
const frontend = path.resolve(__dirname, '../frontend');
const scripts = require('../frontend/package.json').scripts;
const npmCli = process.env.npm_execpath || path.join(path.dirname(process.execPath), 'node_modules/npm/bin/npm-cli.js');
for (const name of Object.keys(scripts).filter(name => name.startsWith('test:'))) {
  const result = spawnSync(process.execPath, [npmCli, 'run', name], { cwd: frontend, stdio: 'inherit' });
  if (result.error) throw result.error;
  if (result.status !== 0) process.exit(result.status || 1);
}
