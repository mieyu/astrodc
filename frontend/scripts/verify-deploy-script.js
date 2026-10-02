const assert = require('node:assert/strict');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const result = spawnSync(process.execPath, [path.join(__dirname, 'deploy.cjs')], { encoding: 'utf8' });
assert.equal(result.status, 1);
assert.match(result.stderr, /Cloudflare deployment is paused/);
console.log('Cloudflare deployment is blocked while paused.');
