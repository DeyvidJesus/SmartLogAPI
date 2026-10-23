// Configure only the existing SmartLog Free site. No key is printed or passed as a process argument.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {spawnSync} from 'node:child_process';

const root = fileURLToPath(new URL('../../', import.meta.url));
const siteId = '2e4e6ab4-bb98-4100-80db-4a074a4ecb66';
const accountId = '6ad2db14a3272f83290ba6a5';
const cli = 'netlify-cli@27.14.2';
function command(args) {
  const result = spawnSync('npx', ['--yes', cli, ...args], {cwd: root, encoding: 'utf8'});
  if (result.error) throw new Error('Não foi possível executar o Netlify CLI.');
  return result;
}
function api(operation, data = {}) {
  const result = command(['api', operation, '--data', JSON.stringify(data)]);
  if (result.status !== 0) throw new Error('Falha no preflight do Netlify: ' + operation);
  return JSON.parse(result.stdout);
}
const account = api('listAccountsForUser').find(value => value.id === accountId);
if (!account || account.type_name !== 'Free' || account.auto_topup_enabled)
  throw new Error('A publicação exige a equipe Free, sem recarga automática.');
const site = api('getSite', {site_id: siteId});
if (site.name !== 'smartlog-next-2026' || site.account_slug !== account.slug)
  throw new Error('O site não corresponde ao destino SmartLog autorizado.');
const values = {};
for (const line of fs.readFileSync(path.join(root, 'SmartLogAPI/.env'), 'utf8').split(/\r?\n/)) {
  const match = line.trim().match(/^(?:export\s+)?(GEMINI_API_KEY|GEMINI_MODEL)\s*=\s*(.*)$/);
  if (match) values[match[1]] = match[2].trim().replace(/^(['"])(.*)\1$/, '$2');
}
if (!values.GEMINI_API_KEY || /[\r\n']/.test(values.GEMINI_API_KEY))
  throw new Error('Configure uma chave Gemini válida no .env da API.');
const accountJson = JSON.parse(fs.readFileSync(path.join(root, 'firebase-key.json'), 'utf8'));
if (accountJson.type !== 'service_account' || accountJson.project_id !== 'smartlog-b9b37')
  throw new Error('A credencial Firebase não corresponde ao projeto SmartLog.');
const credential = JSON.stringify(accountJson);
if (credential.includes("'")) throw new Error('Formato da credencial Firebase não suportado pelo importador.');
const model = values.GEMINI_MODEL || 'gemini-3.1-flash-lite';
if (!/^[a-zA-Z0-9._-]+$/.test(model)) throw new Error('Nome de modelo inválido.');
const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'smartlog-netlify-env-'));
const file = path.join(directory, 'server.env');
try {
  fs.writeFileSync(file,
    "FIREBASE_PROJECT_ID=smartlog-b9b37\nFIREBASE_SERVICE_ACCOUNT_JSON='" + credential +
    "'\nGEMINI_API_KEY='" + values.GEMINI_API_KEY + "'\nGEMINI_MODEL=" + model + '\n',
    {mode: 0o600});
  const result = command(['env:import', file, '--site', siteId]);
  // CLI output may contain imported values; deliberately do not relay it.
  if (result.status !== 0) throw new Error('Falha ao importar variáveis do servidor no Netlify. Código: ' + result.status);
} finally {
  fs.unlinkSync(file);
  fs.rmdirSync(directory);
}
console.log('Servidor configurado no projeto Free: ' + site.ssl_url);
console.log('Gemini e Firebase enviados como variáveis do servidor, sem expor os valores.');
