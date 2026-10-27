const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const compiler = require('vue-template-compiler');
const file = path.join(__dirname, '../src/components/ImageDownloadDialog.vue');
const component = compiler.parseComponent(fs.readFileSync(file, 'utf8'));
assert.deepEqual(compiler.compile(component.template.content).errors, []);
const source = component.script.content.replace(/^import .*;\r?\n/gm, '').replace('export default', 'module.exports =');
const requests = [];
const clicked = [];
let handler;
const axios = {
  get: async url => { assert.ok(url.endsWith('/check')); return {data:{code:1,data:true}}; },
  defaults: { baseURL: 'https://api.example.test/' },
  isCancel: error => error.code === 'ERR_CANCELED',
  post: async (url, body, options) => { requests.push({ url, body, options }); return handler(url, body, options); }
};
const context = { module: { exports: {} }, axios, mobileViewport: {}, AbortController, console,
  document: { body: { appendChild() {} }, createElement: () => ({ click() { clicked.push({href: this.href, target: this.target}); }, remove() {} }) }
};
vm.runInNewContext(source, context, { filename: file });
const definition = context.module.exports;
function create() {
  const instance = { ...definition.data(), visible: true, query: {object:'U0',exptime:60}, focus: {},
    $nextTick: () => Promise.resolve() };
  for (const [name, method] of Object.entries(definition.methods)) instance[name] = method.bind(instance);
  for (const [name, getter] of Object.entries(definition.computed)) Object.defineProperty(instance, name, {get: getter.bind(instance)});
  instance.$refs = { groups: {
    clearSelection: () => instance.selectionChanged([]),
    toggleRowSelection: row => instance.selectionChanged([...instance.selectedGroups.filter(item => item.id !== row.id), row])
  }};
  return instance;
}
const groups = [
  {id:'u1',date:'2024-11-03',object:'U0',tele:'T1',fileCount:12,matchedCount:1,calibrationCount:4},
  {id:'u2',date:'2024-11-04',object:'U0',tele:'T1',fileCount:8,matchedCount:2,calibrationCount:3},
  {id:'s1',date:'2024-11-03',object:'S9',tele:'T2',fileCount:6,matchedCount:6,calibrationCount:2}
];
const prepared = {token:'download-token',filename:'astrodc_U0.zip',fileCount:12,totalBytes:1048576,missingCount:0,
  missingExamples:[],expiresAt:new Date(Date.now()+600000).toISOString()};
(async () => {
  handler = async () => ({data:{code:1,data:{groups,ungroupedCount:1}}});
  const instance = create();
  instance.focus = {object:'U0',dateObs:'2024-11-03 08:00:00',tele:'T1'};
  await instance.loadGroups();
  assert.equal(requests[0].url, '/api/image/own/downloads/groups');
  assert.equal(instance.filteredGroups.length,1);
  assert.equal(instance.selectedGroups[0].id,'u1');
  assert.equal(instance.matchedOnly,false);
  assert.equal(instance.includeCalibration,false);

  instance.includeCalibration=true;
  handler = async () => ({data:{code:1,data:prepared}});
  await instance.prepare();
  const request = requests.at(-1);
  assert.equal(request.url,'/api/image/own/downloads/prepare');
  assert.equal(request.body.query,instance.query);
  assert.equal(request.body.groupIds.join(','),'u1');
  assert.equal(request.body.matchedOnly,false);
  assert.equal(request.body.includeCalibration,true);
  assert.equal(instance.formatBytes(1048576),'1.00 MiB');
  const beforeDownloadRequests=requests.length;
  await instance.startDownload();
  assert.equal(requests.length,beforeDownloadRequests,'Large FITS archive must use native browser download, never an axios Blob');
  assert.equal(clicked.at(-1).href,'https://api.example.test/api/image/own/downloads/download-token');
  assert.equal(clicked.at(-1).target,instance.frameName);

  instance.matchedOnly=true;
  instance.invalidatePlan();
  await instance.prepare();
  assert.equal(requests.at(-1).body.previousToken,'download-token');
  assert.equal(requests.at(-1).body.matchedOnly,true);

  instance.prepared={...prepared,missingCount:2};
  const beforeMissing=clicked.length;
  await instance.startDownload();
  assert.equal(clicked.length,beforeMissing,'Partial archives require explicit acknowledgment');
  instance.acceptMissing=true;
  await instance.startDownload();
  assert.equal(clicked.length,beforeMissing+1);

  instance.prepared={...prepared,expiresAt:'2000-01-01T00:00:00Z'};
  await instance.startDownload();
  assert.equal(instance.prepared,null);
  assert.match(instance.error,/过期/);

  const stale=create();
  let complete;
  handler=()=>new Promise(resolve=>{complete=resolve});
  const loading=stale.loadGroups();
  stale.reset();
  complete({data:{code:1,data:{groups,ungroupedCount:0}}});
  await loading;
  assert.equal(stale.groups.length,0,'Closed dialog must ignore late responses');

  const failure=create();
  failure.selectedGroups=[groups[0]];
  handler=async()=>{throw {response:{data:{message:'所选文件缺失'}}}};
  await failure.prepare();
  assert.equal(failure.error,'所选文件缺失');
  const table=fs.readFileSync(path.join(__dirname,'../src/components/TableList.vue'),'utf8');
  assert.match(table,/批量下载原始 FITS/);
  assert.match(table,/导出信息 CSV/);
  assert.match(table,/\/metadata\/export/);
  assert.match(table,/image-download-dialog/);
  console.log('Image download behavior: day selection, calibration, exact-query mode, missing files, native download and cancellation passed.');
})().catch(error=>{console.error(error);process.exitCode=1});
