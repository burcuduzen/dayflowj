const $=id=>document.getElementById(id);
let paused=false,busy=false,logEntries=[];
const mb=value=>Number.isFinite(value)?`${(value/1048576).toFixed(1)} MB`:'—';
const pct=value=>Number.isFinite(value)?`${value.toFixed(1)} %`:'—';
async function get(path){const r=await fetch(path,{headers:{'X-DayFlowJ-Request':'1'},cache:'no-store'});if(!r.ok)throw new Error(`Tanılama verisi alınamadı (${r.status}). Geliştirici modu ve backend bağlantısını kontrol et.`);return r.json();}
function cell(row,value,className){const td=document.createElement('td');td.textContent=String(value);if(className)td.className=className;row.append(td);}
function row(values){const r=document.createElement('tr');for(const v of values)cell(r,v);return r;}
function draw(history){const values=history.map((s,i)=>({i,value:s.cpuPercent})).filter(s=>Number.isFinite(s.value));const last=Math.max(1,history.length-1);$('cpu-line').setAttribute('points',values.map(s=>`${s.i/last*600},${120-Math.min(100,Math.max(0,s.value))*1.1}`).join(' '));}
async function refresh(){
  if(paused||busy||document.hidden)return;busy=true;
  try{
    const [metrics,logs]=await Promise.all([get('/api/diagnostics/metrics'),get(`/api/diagnostics/logs?level=${encodeURIComponent($('level').value)}&search=${encodeURIComponent($('search').value)}&limit=300`)]);
    const s=metrics.latest,r=metrics.requests;
    $('cpu').textContent=pct(s.cpuPercent);$('heap').textContent=mb(s.heapUsedBytes);$('heap-max').textContent=`Heap üst sınırı: ${mb(s.heapMaxBytes)} · Toplam RAM değildir.`;
    $('latency').textContent=r.sampleCount?`${r.averageMs.toFixed(1)} ms`:'—';$('p95').textContent=r.sampleCount?`P95: ${r.p95Ms.toFixed(1)} ms · ${r.sampleCount} istek örneği`:'Henüz uygulama isteği yok.';
    $('uptime').textContent=`${Math.floor(s.uptimeMs/3600000)} sa ${Math.floor(s.uptimeMs/60000)%60} dk`;
    $('requests').textContent=`${r.count} istek · ${r.errors} hata · ${s.threads} Java thread`;
    $('sample-time').textContent=`Son ölçüm: ${new Date(s.time).toLocaleTimeString('tr-TR')}`;draw(metrics.history);
    $('storage').textContent=r.storageAvailable?'Loglar yerel tanılama dosyasına kaydediliyor.':'Dosyaya yazılamıyor; loglar yalnızca bu oturumun belleğinde tutuluyor.';
    logEntries=logs;$('logs').replaceChildren();for(const e of logs){const tr=document.createElement('tr');cell(tr,new Date(e.time).toLocaleString('tr-TR'));cell(tr,e.level,`level-${e.level}`);cell(tr,e.action);cell(tr,e.status>=400?`Hata · HTTP ${e.status}`:e.status?`Başarılı · HTTP ${e.status}`:'Bilgi');cell(tr,e.durationMs?`${e.durationMs.toFixed(1)} ms`:'—');$('logs').append(tr);}
    if(!logs.length)$('logs').append(row(['—','—','Bu filtrede kayıt yok.','—','—']));
    if(window.dayflowDiagnostics){try{const desktop=await window.dayflowDiagnostics.metrics();$('processes').replaceChildren();for(const p of desktop.electron.processes)$('processes').append(row([p.type,p.pid,pct(p.cpuPercent),mb(p.workingSetBytes)]));$('desktop-status').textContent=`Electron toplam working set: ${mb(desktop.electron.workingSetBytes)} · ${desktop.electron.processes.length} işlem`;$('java-ram').textContent=`Java işleminin working set RAM’i: ${mb(desktop.javaWorkingSetBytes)} · PID ${s.pid}`;}catch{$('desktop-status').textContent='Electron ölçümü alınamadı; Java ölçümleri kullanılabilir.';}}
    else $('desktop-status').textContent='Tarayıcı modu: Electron ölçümleri yalnızca geliştirici masaüstü penceresinde kullanılabilir.';
    $('error').hidden=true;
  }catch(error){$('error').textContent=error.message;$('error').hidden=false;}finally{busy=false;}
}
$('pause').onclick=()=>{paused=!paused;$('pause').textContent=paused?'İzlemeye devam et':'İzlemeyi duraklat';if(!paused)refresh();};
$('level').onchange=refresh;let searchTimer;$('search').oninput=()=>{clearTimeout(searchTimer);searchTimer=setTimeout(refresh,250);};
$('export').onclick=()=>{const text='Zaman\tSeviye\tİşlem\tHTTP\tSüre (ms)\n'+logEntries.map(e=>[e.time,e.level,e.action,e.status,e.durationMs].join('\t')).join('\n');const url=URL.createObjectURL(new Blob([text],{type:'text/tab-separated-values;charset=utf-8'}));const a=document.createElement('a');a.href=url;a.download='dayflowj-diagnostics.tsv';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);};
setInterval(refresh,5000);document.addEventListener('visibilitychange',refresh);refresh();
