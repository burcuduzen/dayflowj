const {
  app,
  BrowserWindow,
  dialog,
  shell,
  session,
  Menu,
  ipcMain,
} = require('electron');
const path = require('node:path');
const fs = require('node:fs/promises');
const { pipeline } = require('node:stream/promises');
const { Readable } = require('node:stream');
const { Backend } = require('./backend.cjs');

app.setName('DayFlowJ');
app.setAppUserModelId('com.burcuduzen.dayflowj');
app.setPath('userData', path.join(app.getPath('appData'), 'DayFlowJ'));
const root = app.isPackaged
  ? process.resourcesPath
  : path.resolve(__dirname, '..');
const dataDirectory =
  process.env.DAYFLOW_DESKTOP_DATA_DIR ||
  path.join(app.getPath('userData'), 'data');
const backend = new Backend({
  root,
  dataDirectory,
  legacyDirectory: app.isPackaged
    ? process.env.DAYFLOW_LEGACY_DATA_DIR ||
      path.join(
        app.getPath('home'),
        'Downloads',
        'dayflowj-main',
        'backend',
        'data',
      )
    : undefined,
});
const diagnosticsEnabled=process.env.DAYFLOW_DIAGNOSTICS_ENABLED==='true';
let diagnosticsWindow;
let mainWindow;
let quitting = false;

function localAddress(value) {
  try {
    return new URL(value).origin === backend.origin;
  } catch {
    return false;
  }
}
async function showError(error) {
  await dialog.showMessageBox(mainWindow, {
    type: 'error',
    title: 'DayFlowJ',
    message: error.message,
  });
}
async function exportBackup() {
  const result = await dialog.showSaveDialog(mainWindow, {
    title: 'Verilerini yedekle',
    defaultPath: `DayFlowJ-${new Date().toISOString().slice(0, 10)}.zip`,
    filters: [{ name: 'DayFlowJ yedeği', extensions: ['zip'] }],
  });
  if (result.canceled) return;
  try {
    const response = await fetch(`${backend.origin}/api/backups/export`, {
      method: 'POST',
      headers: backend.headers,
      signal: AbortSignal.timeout(30000),
    });
    if (!response.ok)
      throw new Error('Yedek oluşturulamadı. Verilerin değişmedi.');
    const temporary = `${result.filePath}.pending`;
    try {
      await pipeline(
        Readable.fromWeb(response.body),
        require('node:fs').createWriteStream(temporary),
      );
      await fs.rename(temporary, result.filePath);
    } finally {
      await fs.rm(temporary, { force: true });
    }
    await dialog.showMessageBox(mainWindow, {
      type: 'info',
      message: 'Yedeğin kaydedildi.',
    });
  } catch (error) {
    await showError(error);
  }
}
async function restoreBackup() {
  const selected = await dialog.showOpenDialog(mainWindow, {
    title: 'Yedeği seç',
    properties: ['openFile'],
    filters: [{ name: 'DayFlowJ yedeği', extensions: ['zip'] }],
  });
  if (selected.canceled) return;
  const choice = await dialog.showMessageBox(mainWindow, {
    type: 'question',
    message: 'Seçtiğin yedek geri yüklensin mi?',
    detail:
      'Mevcut verilerin önce otomatik yedeği alınacak. Ardından seçtiğin yedekteki kayıtlar açılacak.',
    buttons: ['Vazgeç', 'Geri yükle'],
    defaultId: 0,
    cancelId: 0,
  });
  if (choice.response !== 1) return;
  mainWindow.hide();
  try {
    await backend.restore(selected.filePaths[0]);
    await mainWindow.loadURL(backend.origin);
  } catch (error) {
    await showError(error);
    if (backend.origin) await mainWindow.loadURL(backend.origin);
  } finally {
    mainWindow.show();
  }
}
async function openDiagnostics() {
  if(!diagnosticsEnabled)return;
  if(diagnosticsWindow){diagnosticsWindow.focus();return;}
  diagnosticsWindow=new BrowserWindow({title:'DayFlowJ · Geliştirici tanılama',width:1150,height:850,backgroundColor:'#f3e8c6',webPreferences:{preload:path.join(__dirname,'diagnostics-preload.cjs'),nodeIntegration:false,contextIsolation:true,sandbox:true}});
  diagnosticsWindow.webContents.setWindowOpenHandler(()=>({action:'deny'}));
  diagnosticsWindow.webContents.on('will-navigate',(event,url)=>{if(!localAddress(url))event.preventDefault();});
  diagnosticsWindow.on('closed',()=>{diagnosticsWindow=undefined;});
  await diagnosticsWindow.loadURL(`${backend.origin}/diagnostics.html`);
}
if(diagnosticsEnabled) {
  const {snapshot}=require('./diagnostics.cjs');
  let pendingMetrics=null,lastMetrics=null;
  ipcMain.handle('dayflow:developer-metrics',async event=>{
    if(!diagnosticsWindow||event.sender!==diagnosticsWindow.webContents||!localAddress(event.senderFrame?.url))throw new Error('Tanılama erişimi yok.');
    if(lastMetrics&&Date.now()-lastMetrics.at<4500)return lastMetrics.value;
    if(!pendingMetrics)pendingMetrics=snapshot(app,backend.child?.pid).then(value=>{lastMetrics={at:Date.now(),value};return value;}).finally(()=>{pendingMetrics=null;});
    return pendingMetrics;
  });
}
function configureSession() {
  session.defaultSession.webRequest.onBeforeSendHeaders((details, callback) => {
    if (localAddress(details.url))
      details.requestHeaders['X-DayFlowJ-Desktop'] = backend.token;
    callback({ requestHeaders: details.requestHeaders });
  });
  session.defaultSession.setPermissionRequestHandler(
    (contents, permission, callback) => {
      callback(
        permission === 'notifications' && localAddress(contents.getURL()),
      );
    },
  );
  session.defaultSession.setPermissionCheckHandler(
    (contents, permission, origin) =>
      permission === 'notifications' && localAddress(origin),
  );
}
async function openWindow() {
  mainWindow = new BrowserWindow({
    title: 'DayFlowJ',
    width: 1320,
    height: 900,
    minWidth: 760,
    minHeight: 600,
    backgroundColor: '#f3e8c6',
    show: false,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      sandbox: true,
      webSecurity: true,
    },
  });
  Menu.setApplicationMenu(
    Menu.buildFromTemplate([
      ...(diagnosticsEnabled?[{label:'Geliştirici',submenu:[{label:'Performans ve işlem logları',click:openDiagnostics}]}]:[]),
      {
        label: 'Veriler',
        submenu: [
          { label: 'Yedek al…', click: exportBackup },
          { label: 'Yedekten geri yükle…', click: restoreBackup },
          {
            label: 'Veri klasörünü aç',
            click: () => shell.openPath(dataDirectory),
          },
          { type: 'separator' },
          { role: 'quit', label: 'Çıkış' },
        ],
      },
      {
        label: 'Görünüm',
        submenu: [
          { role: 'resetZoom', label: 'Normal boyut' },
          { role: 'zoomIn', label: 'Büyüt' },
          { role: 'zoomOut', label: 'Küçült' },
        ],
      },
    ]),
  );
  mainWindow.webContents.on('page-title-updated', (event) => {
    event.preventDefault();
    mainWindow.setTitle('DayFlowJ');
  });
  const openExternal = (url) => {
    try {
      if (new URL(url).protocol === 'https:') shell.openExternal(url);
    } catch {}
  };
  mainWindow.webContents.on('will-navigate', (event, url) => {
    if (!localAddress(url)) {
      event.preventDefault();
      openExternal(url);
    }
  });
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    openExternal(url);
    return { action: 'deny' };
  });
  mainWindow.webContents.on('will-attach-webview', (event) =>
    event.preventDefault(),
  );
  mainWindow.on('closed', () => {
    if(diagnosticsWindow)diagnosticsWindow.close();
    mainWindow = undefined;
  });
  await mainWindow.loadURL(backend.origin);
  mainWindow.show();
}

if (!app.requestSingleInstanceLock()) app.quit();
else {
  app.on('second-instance', () => {
    if (mainWindow) {
      if (mainWindow.isMinimized()) mainWindow.restore();
      mainWindow.show();
      mainWindow.focus();
    }
  });
  app.whenReady().then(async () => {
    try {
      await backend.start();
      configureSession();
      await openWindow();
    } catch (error) {
      dialog.showErrorBox('DayFlowJ açılamadı', error.message);
      app.quit();
    }
  });
  app.on('window-all-closed', () => app.quit());
  app.on('before-quit', (event) => {
    if (quitting) return;
    event.preventDefault();
    quitting = true;
    backend.stop().finally(() => app.quit());
  });
}
