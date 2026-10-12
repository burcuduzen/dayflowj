const { app, BrowserWindow, dialog, shell, session } = require('electron');
const { spawn } = require('node:child_process');
const path = require('node:path');
const fs = require('node:fs');

const root = app.isPackaged
  ? path.resolve(process.resourcesPath, '../../..')
  : path.resolve(__dirname, '..');
const origin = 'http://127.0.0.1:8080';
let mainWindow;
let backendProcess;
let closing = false;

app.setName('DayFlowJ');
app.setAppUserModelId('com.burcuduzen.dayflowj');
// Keep desktop preferences and stopwatch state across app updates.
app.setPath('userData', path.join(app.getPath('appData'), 'DayFlowJ'));

function localAddress(value) {
  try { return new URL(value).origin === origin; } catch { return false; }
}
function externalAddress(value) {
  try { return new URL(value).protocol === 'https:'; } catch { return false; }
}
async function backendReady() {
  try {
    const response = await fetch(`${origin}/api/settings`, { signal: AbortSignal.timeout(1200) });
    if (!response.ok) return false;
    const settings = await response.json();
    return typeof settings.dailyTaskGoal === 'number' && typeof settings.timeZone === 'string';
  } catch { return false; }
}
async function startBackend() {
  if (await backendReady()) return;
  const script = path.join(root, 'desktop', 'Start-Backend.ps1');
  if (!fs.existsSync(script)) throw new Error('Backend başlatma dosyası bulunamadı. Proje klasörünü taşımadan tekrar dene.');
  backendProcess = spawn('powershell.exe', ['-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-File', script], {
    cwd: path.join(root, 'backend'), windowsHide: true, stdio: 'ignore'
  });
  let launchError;
  backendProcess.on('error', error => { launchError = error; });
  for (let attempt = 0; attempt < 60; attempt++) {
    if (launchError) throw launchError;
    if (await backendReady()) return;
    if (backendProcess.exitCode !== null) throw new Error('Backend başlayamadı. backend/data içindeki desktop loglarını kontrol et.');
    await new Promise(resolve => setTimeout(resolve, 1000));
  }
  throw new Error('Backend 60 saniye içinde hazırlanamadı. backend/data içindeki desktop loglarını kontrol et.');
}
function stopOwnedBackend() {
  if (!backendProcess || !backendProcess.pid || backendProcess.exitCode !== null) return;
  // Kill only this app's child process tree; never an already-running server.
  spawn('taskkill.exe', ['/PID', String(backendProcess.pid), '/T', '/F'], {
    windowsHide: true, stdio: 'ignore'
  }).unref();
  backendProcess = undefined;
}
async function openWindow() {
  mainWindow = new BrowserWindow({
    title: 'DayFlowJ', width: 1320, height: 900, minWidth: 760, minHeight: 600,
    backgroundColor: '#f3e8c6', show: false, autoHideMenuBar: true,
    webPreferences: { nodeIntegration: false, contextIsolation: true, sandbox: true, webSecurity: true }
  });
  mainWindow.removeMenu();
  mainWindow.webContents.on('page-title-updated', event => { event.preventDefault(); mainWindow.setTitle('DayFlowJ'); });
  mainWindow.webContents.on('will-navigate', (event, url) => {
    if (localAddress(url)) return;
    event.preventDefault();
    if (externalAddress(url)) shell.openExternal(url);
  });
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (externalAddress(url)) shell.openExternal(url);
    return { action: 'deny' };
  });
  mainWindow.webContents.on('will-attach-webview', event => event.preventDefault());
  mainWindow.webContents.session.on('will-download', event => event.preventDefault());
  mainWindow.once('ready-to-show', () => mainWindow.show());
  mainWindow.on('closed', () => { mainWindow = undefined; });
  await mainWindow.loadURL(`${origin}/`);
  mainWindow.show();
}

if (!app.requestSingleInstanceLock()) {
  app.quit();
} else {
  app.on('second-instance', () => {
    if (mainWindow) { if (mainWindow.isMinimized()) mainWindow.restore(); mainWindow.show(); mainWindow.focus(); }
  });
  app.whenReady().then(async () => {
    session.defaultSession.setPermissionRequestHandler((contents, permission, callback) => {
      callback(permission === 'notifications' && localAddress(contents.getURL()));
    });
    session.defaultSession.setPermissionCheckHandler((contents, permission, requestingOrigin) =>
      permission === 'notifications' && localAddress(requestingOrigin));
    try { await startBackend(); await openWindow(); }
    catch (error) { dialog.showErrorBox('DayFlowJ açılamadı', error.message); app.quit(); }
  });
  app.on('window-all-closed', () => app.quit());
  app.on('before-quit', () => { if (!closing) { closing = true; stopOwnedBackend(); } });
}

