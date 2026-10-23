// Adapter for contract tests. Published code uses functions/smartlog.mjs directly.
import fs from 'node:fs';
import http from 'node:http';
import {Readable} from 'node:stream';
import handler from './functions/smartlog.mjs';

const emulator = process.argv.includes('--emulator');
if (!emulator && !process.argv.includes('--firebase-real')) {
  throw new Error('Escolha explicitamente --emulator ou --firebase-real.');
}
if (emulator) {
  process.env.SMARTLOG_EMULATOR = 'true';
  process.env.FIREBASE_PROJECT_ID = 'demo-smartlog-integration';
  process.env.FIREBASE_AUTH_EMULATOR_HOST = '127.0.0.1:9099';
  process.env.FIRESTORE_EMULATOR_HOST = '127.0.0.1:8085';
} else {
  delete process.env.FIREBASE_AUTH_EMULATOR_HOST;
  delete process.env.FIRESTORE_EMULATOR_HOST;
  delete process.env.SMARTLOG_EMULATOR;
  process.env.FIREBASE_PROJECT_ID = 'smartlog-b9b37';
  process.env.FIREBASE_SERVICE_ACCOUNT_JSON = fs.readFileSync(new URL('../../firebase-key.json', import.meta.url), 'utf8');
}
const envFile = new URL('../.env', import.meta.url);
if (fs.existsSync(envFile)) {
  for (const line of fs.readFileSync(envFile, 'utf8').split(/\r?\n/)) {
    const match = line.trim().match(/^(?:export\s+)?(GEMINI_API_KEY|GEMINI_MODEL)\s*=\s*(.*)$/);
    if (match) process.env[match[1]] = match[2].trim().replace(/^(['"])(.*)\1$/, '$2');
  }
}
const port = Number(process.env.SMARTLOG_NETLIFY_PORT || 8081);
const server = http.createServer(async (req, res) => {
  try {
    const request = new Request('http://127.0.0.1:' + port + req.url, {
      method: req.method, headers: req.headers,
      ...(!['GET', 'HEAD'].includes(req.method) ? {body: req, duplex: 'half'} : {}),
    });
    const response = await handler(request);
    res.writeHead(response.status, Object.fromEntries(response.headers));
    if (response.body) Readable.fromWeb(response.body).pipe(res);
    else res.end();
  } catch {
    res.writeHead(503, {'Content-Type': 'application/json'});
    res.end(JSON.stringify({message: 'Central indisponível.'}));
  }
});
server.listen(port, '127.0.0.1', () => console.log('SmartLog Netlify adapter em 127.0.0.1:' + port + ' (' + (emulator ? 'emuladores' : 'Firebase real') + ')'));
for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => server.close(() => process.exit(0)));
