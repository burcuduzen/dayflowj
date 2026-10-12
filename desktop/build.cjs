const fs = require('node:fs');
const path = require('node:path');
const root = __dirname;
const source = path.join(root, 'node_modules', 'electron', 'dist');
const target = path.join(root, 'runtime');
if (!fs.existsSync(path.join(source, 'electron.exe'))) throw new Error('Electron runtime missing. Install electron and run its install.js first.');
fs.mkdirSync(target, { recursive: true });
fs.cpSync(source, target, { recursive: true });
fs.copyFileSync(path.join(target, 'electron.exe'), path.join(target, 'DayFlowJ.exe'));
const application = path.join(target, 'resources', 'app');
fs.mkdirSync(application, { recursive: true });
for (const file of ['main.cjs', 'package.json']) fs.copyFileSync(path.join(root, file), path.join(application, file));
console.log(path.join(target, 'DayFlowJ.exe'));

