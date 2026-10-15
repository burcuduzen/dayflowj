const os = require('node:os');
const { execFile } = require('node:child_process');
function summarize(processes) {
  return {
    processes: processes.map(p => ({pid:p.pid,type:p.type,cpuPercent:p.cpu?.percentCPUUsage ?? null,workingSetBytes:Number.isFinite(p.memory?.workingSetSize)?p.memory.workingSetSize*1024:null})),
    workingSetBytes: processes.every(p=>Number.isFinite(p.memory?.workingSetSize))?processes.reduce((sum,p)=>sum+p.memory.workingSetSize*1024,0):null,
  };
}
function javaWorkingSet(pid) {
  if(process.platform!=='win32'||!Number.isSafeInteger(pid)||pid<=0)return Promise.resolve(null);
  return new Promise(resolve=>execFile('powershell.exe',['-NoProfile','-NonInteractive','-Command',`(Get-Process -Id ${pid} -ErrorAction Stop).WorkingSet64`],{windowsHide:true,timeout:2500,maxBuffer:1024},(error,stdout)=>{const value=Number(stdout.trim());resolve(!error&&Number.isFinite(value)&&value>0?value:null);}));
}
async function snapshot(app,pid) {
  const electron=summarize(app.getAppMetrics());
  return {time:new Date().toISOString(),electron,javaWorkingSetBytes:await javaWorkingSet(pid),logicalCores:os.availableParallelism?.()||os.cpus().length};
}
module.exports={summarize,snapshot};
