const path = require('node:path');
const { spawnSync } = require('node:child_process');
const config = require('../deployment.json').cloudflare;
if (config.paused) {
  console.error('Cloudflare deployment is paused in deployment.json.');
  process.exit(1);
}
const frontend = path.resolve(__dirname, '../frontend');
const npmCli = process.env.npm_execpath || path.join(path.dirname(process.execPath), 'node_modules/npm/bin/npm-cli.js');
function run(args) {
  const result = spawnSync(process.execPath, [npmCli, ...args], { cwd: frontend, stdio: 'inherit' });
  if (result.error) throw result.error;
  if (result.status !== 0) process.exit(result.status || 1);
}
run(['run', 'test:api-config']);
run(['run', 'test:access-gate']);
run(['run', 'build']);
run(['exec', '--', 'wrangler', 'pages', 'deploy', 'dist', '--project-name', config.project, '--branch', 'main', '--commit-dirty=true']);
