const fs = require('node:fs');
const path = require('node:path');
const esbuild = require('esbuild');
const root = __dirname;
fs.mkdirSync(path.join(root, 'dist'), { recursive: true });
fs.cpSync(path.join(root, 'public'), path.join(root, 'dist'), { recursive: true });
esbuild.buildSync({ entryPoints: [path.join(root, 'src/account-page.js')], outfile: path.join(root, 'dist/account-page.js'), bundle: true, minify: true, format: 'esm', target: ['es2022'], legalComments: 'eof' });
console.log('Built public support site and account-deletion page.');
