const { spawn } = require('node:child_process');
const { randomBytes } = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');

function waitForExit(child, timeout = 15000) {
  if (!child || child.exitCode !== null) return Promise.resolve(true);
  return new Promise((resolve) => {
    const timer = setTimeout(() => {
      child.off('exit', done);
      resolve(false);
    }, timeout);
    function done() {
      clearTimeout(timer);
      resolve(true);
    }
    child.once('exit', done);
  });
}
class Backend {
  constructor({
    root,
    dataDirectory,
    port = Number(process.env.DAYFLOW_DESKTOP_PORT || 8080),
    legacyDirectory,
  }) {
    this.root = root;
    this.dataDirectory = dataDirectory;
    this.legacyDirectory =
      legacyDirectory ?? path.join(root, 'backend', 'data');
    this.port = port;
    this.token = randomBytes(32).toString('hex');
    this.origin = null;
    this.child = null;
  }
  get headers() {
    return { 'X-DayFlowJ-Request': '1', 'X-DayFlowJ-Desktop': this.token };
  }
  get java() {
    const small = path.join(
      this.root,
      'desktop',
      'jre-small',
      'bin',
      'java.exe',
    );
    if (fs.existsSync(small)) return small;
    const bundled = path.join(this.root, 'desktop', 'jre', 'bin', 'java.exe');
    if (fs.existsSync(bundled)) return bundled;
    if (process.env.JAVA_HOME)
      return path.join(process.env.JAVA_HOME, 'bin', 'java.exe');
    throw new Error(
      'Java çalışma ortamı eksik. DayFlowJ paketini yeniden hazırlamalısın.',
    );
  }
  get jar() {
    return path.join(
      this.root,
      'backend',
      'target',
      'dayflowj-0.0.1-SNAPSHOT.jar',
    );
  }
  async settings() {
    return new Promise((resolve, reject) => {
      const child = spawn(
        'powershell.exe',
        [
          '-NoProfile',
          '-NonInteractive',
          '-ExecutionPolicy',
          'Bypass',
          '-File',
          path.join(this.root, 'desktop', 'Read-Settings.ps1'),
          '-DataDirectory',
          this.dataDirectory,
          '-LegacyDirectory',
          this.legacyDirectory,
        ],
        { windowsHide: true, stdio: ['ignore', 'pipe', 'ignore'] },
      );
      let output = '';
      child.stdout.setEncoding('utf8');
      child.stdout.on('data', (value) => {
        output += value;
        if (output.length > 65536) child.kill();
      });
      child.once('error', reject);
      child.once('exit', (code) => {
        if (code !== 0)
          return reject(new Error('Kayıtlı bağlantı ayarları açılamadı.'));
        try {
          resolve(JSON.parse(output.replace(/^\uFEFF/, '').trim() || '{}'));
        } catch {
          reject(new Error('Kayıtlı bağlantı ayarları geçersiz.'));
        }
      });
    });
  }
  async start() {
    if (this.child && this.child.exitCode === null) return;
    fs.mkdirSync(this.dataDirectory, { recursive: true });
    const secrets = await this.settings();
    const environment = {
      ...process.env,
      ...secrets,
      DAYFLOW_DATA_DIR: this.dataDirectory,
      DAYFLOW_LEGACY_DATA_DIR: this.legacyDirectory,
      DAYFLOW_DESKTOP_CONTROL_TOKEN: this.token,
    };
    const log = fs.createWriteStream(
      path.join(this.dataDirectory, 'desktop.log'),
      { flags: 'a' },
    );
    const child = spawn(
      this.java,
      [
        '-jar',
        this.jar,
        '--dayflow.desktop.managed=true',
        `--server.port=${this.port}`,
      ],
      {
        cwd: this.dataDirectory,
        env: environment,
        windowsHide: true,
        stdio: ['pipe', 'pipe', 'pipe'],
      },
    );
    this.child = child;
    child.stderr.pipe(log, { end: false });
    child.once('close', () => log.end());
    await new Promise((resolve, reject) => {
      let line = '';
      const timer = setTimeout(
        () =>
          reject(
            new Error(
              'DayFlowJ 60 saniye içinde açılamadı. Veri klasöründeki desktop.log dosyasını kontrol et.',
            ),
          ),
        60000,
      );
      const fail = (message) => {
        clearTimeout(timer);
        reject(new Error(message));
      };
      child.once('error', () => fail('Java backend başlatılamadı.'));
      child.once('exit', (code) =>
        fail(
          `Backend açılamadı (${code}). Eski DayFlowJ/backend açıksa kapatıp tekrar dene. Ayrıntılar: ${path.join(this.dataDirectory, 'desktop.log')}`,
        ),
      );
      child.stdout.setEncoding('utf8');
      child.stdout.on('data', (chunk) => {
        log.write(chunk);
        line += chunk;
        let end;
        while ((end = line.indexOf('\n')) >= 0) {
          const value = line.slice(0, end).trim();
          line = line.slice(end + 1);
          if (/^DAYFLOW_READY:\d+$/.test(value)) {
            this.origin = `http://127.0.0.1:${Number(value.split(':')[1])}`;
            clearTimeout(timer);
            resolve();
          }
        }
      });
    });
  }
  async stop() {
    const child = this.child;
    if (!child || child.exitCode !== null) return;
    child.stdin.on('error', () => {});
    child.stdin.end(`STOP ${this.token}\n`);
    if (!(await waitForExit(child))) {
      await new Promise((resolve) => {
        const killer = spawn(
          'taskkill.exe',
          ['/PID', String(child.pid), '/T', '/F'],
          { windowsHide: true, stdio: 'ignore' },
        );
        killer.once('error', resolve);
        killer.once('exit', resolve);
      });
      await waitForExit(child, 3000);
    }
    this.child = null;
    this.origin = null;
  }
  async restore(archive) {
    await this.stop();
    try {
      await new Promise((resolve, reject) => {
        const child = spawn(
          this.java,
          [
            '-jar',
            this.jar,
            `--restore-backup=${archive}`,
            `--restore-target=${path.join(this.dataDirectory, 'dayflowj.mv.db')}`,
          ],
          { windowsHide: true, stdio: 'ignore' },
        );
        child.once('error', reject);
        child.once('exit', (code) =>
          code === 0
            ? resolve()
            : reject(
                new Error(
                  'Yedek geçersiz veya okunamadı. Mevcut verilerin korunuyor.',
                ),
              ),
        );
      });
    } finally {
      await this.start();
    }
  }
}
module.exports = { Backend, waitForExit };
