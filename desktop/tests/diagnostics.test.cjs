const {test}=require('node:test');
const assert=require('node:assert/strict');
const {summarize,snapshot}=require('../diagnostics.cjs');
test('Electron memory is converted from KB to bytes and missing data stays unavailable',()=>{
  const result=summarize([{pid:1,type:'Browser',cpu:{percentCPUUsage:2},memory:{workingSetSize:1024}},{pid:2,type:'Tab',cpu:{percentCPUUsage:3},memory:{workingSetSize:512}}]);
  assert.equal(result.workingSetBytes,1572864);assert.equal(result.processes[0].workingSetBytes,1048576);
  assert.equal(result.processes[1].cpuPercent,3);
  assert.equal(summarize([{pid:1,type:'Browser',cpu:{}}]).workingSetBytes,null);
});
test('unsupported Java working set remains unavailable rather than fake zero',async()=>{
  const value=await snapshot({getAppMetrics:()=>[]},undefined);
  assert.equal(value.javaWorkingSetBytes,null);assert.equal(value.electron.processes.length,0);
});
