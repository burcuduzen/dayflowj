const { build } = require('electron-builder');
const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
async function packageApplication() {
  if (
    !fs.existsSync(
      path.join(__dirname, '../backend/target/dayflowj-0.0.1-SNAPSHOT.jar'),
    )
  ) {
    throw new Error('Önce backend klasöründe mvnw.cmd clean package çalıştır.');
  }
  const runtime = path.join(__dirname, 'jre-small');
  if (!fs.existsSync(path.join(runtime, 'bin/java.exe'))) {
    if (!process.env.JAVA_HOME)
      throw new Error('Java 17 JDK için JAVA_HOME ayarla.');
    const result = spawnSync(
      path.join(process.env.JAVA_HOME, 'bin/jlink.exe'),
      [
        '--add-modules',
        'java.se,jdk.crypto.ec,jdk.unsupported,jdk.charsets,jdk.zipfs,jdk.management,jdk.localedata',
        '--include-locales',
        'en,tr',
        '--strip-debug',
        '--no-header-files',
        '--no-man-pages',
        '--compress=2',
        '--output',
        runtime,
      ],
      { stdio: 'inherit', windowsHide: true },
    );
    if (result.error || result.status !== 0)
      throw new Error('Java çalışma ortamı hazırlanamadı.');
  }
  await build({ config: path.join(__dirname, 'electron-builder.yml') });
}
packageApplication().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
