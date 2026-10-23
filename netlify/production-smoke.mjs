// Explicit, isolated live verification. Never reads or changes existing customer fixtures.
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {randomBytes, createHash} from 'node:crypto';
import {initializeApp, cert, deleteApp} from 'firebase-admin/app';
import {getAuth} from 'firebase-admin/auth';
import {getFirestore} from 'firebase-admin/firestore';

if (!process.argv.includes('--live')) throw new Error('Use --live for the isolated production smoke test.');
const base = 'https://smartlog-next-2026.netlify.app';
const account = JSON.parse(await readFile(new URL('../../firebase-key.json', import.meta.url)));
assert.equal(account.project_id, 'smartlog-b9b37');
const environment = await readFile(new URL('../../SmartLogWeb/src/environments/environment.ts', import.meta.url), 'utf8');
const apiKey = environment.match(/firebaseApiKey:\s*'([^']+)'/)[1];
const app = initializeApp({credential: cert(account)}, 'netlify-smoke');
const auth = getAuth(app), db = getFirestore(app);
const run = 'netlify-smoke-' + Date.now().toString(36);
const docs = new Set(), users = new Set();
const request = (path, token, method = 'GET', body) => fetch(base + '/api/v1' + path, {
  method, headers: {'Content-Type': 'application/json', Origin: base, ...(token ? {Authorization: 'Bearer ' + token} : {})},
  ...(body ? {body: JSON.stringify(body)} : {}), signal: AbortSignal.timeout(55000),
});
async function seed(path, data) {
  await db.doc(path).create(data);
  docs.add(path);
}
async function user(role, tenant) {
  const email = run + '-' + role.toLowerCase() + '@example.test';
  const password = randomBytes(32).toString('base64url');
  const record = await auth.createUser({email, password, displayName: 'Teste isolado ' + role});
  users.add(record.uid);
  await seed('usuarios/' + record.uid, {nome: 'Teste ' + role, email, empresaId: tenant, tipoUsuario: role, ativo: true});
  const response = await fetch('https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=' + apiKey, {
    method: 'POST', headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({email, password, returnSecureToken: true}), signal: AbortSignal.timeout(30000),
  });
  assert.equal(response.status, 200, 'Firebase password sign-in');
  return {...record, token: (await response.json()).idToken};
}
let cleanupFailed = false;
try {
  let response = await request('');
  assert.equal(response.status, 200, 'Public Netlify API');
  assert.equal((await response.json()).plataforma, 'netlify-functions');
  console.log('PASS public HTTPS API without Netlify login');
  const admin = await user('ADMINISTRADOR', run), driver = await user('MOTORISTA', run), client = await user('CLIENTE', run);
  const route = run + '-route', delivery = run + '-delivery', foreign = run + '-foreign';
  await seed('rotas/' + route, {nome: 'Rota fictícia NEXT', empresaId: run, motoristaId: driver.uid, status: 'Em Rota',
    localizacaoAtual: {latitude: -23.55, longitude: -46.63, atualizadoEm: new Date().toISOString()}});
  await seed('entregas/' + delivery, {id: 'ENT-' + run, empresaId: run, clienteId: 'legacy-' + run,
    emailCliente: client.email, cliente: 'Cliente fictício', motoristaId: driver.uid, rotaId: route,
    status: 'Pendente', endereco: 'Endereço fictício', recebedor: 'Preservar', latitude: -23.55});
  await seed('entregas/' + foreign, {id: 'FOREIGN-' + run, empresaId: run + '-other', status: 'Pendente'});
  response = await request('/me', admin.token);
  assert.equal(response.status, 200); assert.equal((await response.json()).role, 'ADMIN');
  console.log('PASS real Firebase login and server profile');
  response = await request('/entregas', admin.token);
  assert.equal(response.status, 200);
  let rows = (await response.json()).data;
  assert.ok(rows.some(row => row.id === delivery)); assert.ok(!rows.some(row => row.id === foreign));
  console.log('PASS real Firestore list scoped by company');
  response = await request('/entregas/' + delivery + '/status', driver.token, 'PATCH', {status: 'ENTREGUE'});
  assert.equal(response.status, 200);
  response = await request('/entregas/' + delivery, admin.token);
  assert.equal((await response.json()).status, 'ENTREGUE');
  const persisted = (await db.doc('entregas/' + delivery).get()).data();
  assert.equal(persisted.status, 'Entregue'); assert.equal(persisted.recebedor, 'Preservar'); assert.equal(persisted.latitude, -23.55);
  console.log('PASS driver status persisted and visible to administrator; legacy fields preserved');
  response = await request('/entregas', client.token);
  rows = (await response.json()).data;
  assert.ok(rows.some(row => row.id === delivery));
  assert.equal((await request('/entregas/' + delivery + '/status', client.token, 'PATCH', {status: 'PENDENTE'})).status, 403);
  assert.equal((await request('/entregas/' + foreign, driver.token)).status, 404);
  assert.equal((await request('/me', 'invalid')).status, 401);
  console.log('PASS legacy client read; client write, cross-company access and invalid token blocked');
  response = await request('/rotas', admin.token);
  rows = await response.json();
  assert.ok(rows.some(row => row.id === route && row.status === 'EM_ANDAMENTO' && row.localizacaoAtual.latitude === -23.55));
  console.log('PASS persisted GPS and translated route status');
  docs.add('_assistantRateLimits/' + createHash('sha256').update(run + ':' + driver.uid).digest('hex'));
  response = await request('/chat', driver.token, 'POST', {pergunta: 'Qual o status da ENT-' + run + '?'});
  assert.equal(response.status, 200, 'Real Gemini response');
  const answer = await response.json();
  assert.equal(answer.provedor, 'gemini'); assert.equal(answer.perfil, 'MOTORISTA');
  assert.match(answer.resposta, /entregue/i); assert.ok(answer.resposta.toUpperCase().includes(('ENT-' + run).toUpperCase()));
  console.log('PASS real Gemini uses the persisted delivery status');
  if (process.argv.includes('--rules')) {
    const endpoint = 'https://firestore.googleapis.com/v1/projects/smartlog-b9b37/databases/(default)/documents/';
    const direct = await fetch(endpoint + 'entregas/' + delivery, {headers: {Authorization: 'Bearer ' + driver.token}});
    assert.equal(direct.status, 200, 'Driver direct read');
    const denied = await fetch(endpoint + 'entregas/' + foreign, {headers: {Authorization: 'Bearer ' + driver.token}});
    assert.equal(denied.status, 403, 'Cross-company Firestore rules');
    const query = async (collection, token, field, value, expected) => {
      const result = await fetch(endpoint.replace(/documents\/$/, 'documents:runQuery'), {
        method: 'POST', headers: {Authorization: 'Bearer ' + token, 'Content-Type': 'application/json'},
        body: JSON.stringify({structuredQuery: {from: [{collectionId: collection}], where: {
          compositeFilter: {op: 'AND', filters: [['empresaId', run], [field, value]].map(([name, text]) => ({
            fieldFilter: {field: {fieldPath: name}, op: 'EQUAL', value: {stringValue: text}},
          }))},
        }}}), signal: AbortSignal.timeout(30000),
      });
      assert.equal(result.status, 200, 'App Firestore query: ' + collection + '/' + field);
      const records = await result.json();
      assert.ok(records.some(row => row.document?.name.endsWith('/' + expected)), 'App query returns its fixture');
      assert.ok(records.every(row => !row.document || row.document.fields.empresaId.stringValue === run));
    };
    await query('entregas', driver.token, 'motoristaId', driver.uid, delivery);
    await query('entregas', client.token, 'emailCliente', client.email, delivery);
    await query('rotas', driver.token, 'motoristaId', driver.uid, route);
    console.log('PASS app Firestore queries for driver deliveries, legacy client deliveries and driver routes');
    const fields = object => Object.fromEntries(Object.entries(object).map(([key, value]) => [key,
      value && typeof value === 'object' ? {mapValue: {fields: fields(value)}} :
      typeof value === 'number' ? {doubleValue: value} : {stringValue: String(value)}]));
    const patch = (path, changes) => fetch(endpoint + path + '?' +
      Object.keys(changes).map(key => 'updateMask.fieldPaths=' + encodeURIComponent(key)).join('&'), {
        method: 'PATCH', headers: {Authorization: 'Bearer ' + driver.token, 'Content-Type': 'application/json'},
        body: JSON.stringify({fields: fields(changes)}),
      });
    assert.equal((await patch('usuarios/' + driver.uid, {nome: 'Motorista atualizado', email: driver.email,
      atualizadoEm: new Date().toISOString()})).status, 200, 'Lucas profile update');
    assert.equal((await patch('usuarios/' + driver.uid, {email: client.email})).status, 403, 'Verified email protected');
    assert.equal((await patch('usuarios/' + driver.uid, {tipoUsuario: 'ADMINISTRADOR'})).status, 403, 'Role protected');
    assert.equal((await patch('rotas/' + route, {localizacaoAtual: {latitude: -23.56, longitude: -46.64,
      atualizadoEm: new Date().toISOString()}})).status, 200, 'Driver GPS write');
    assert.equal((await patch('rotas/' + route, {motoristaId: client.uid})).status, 403, 'Route assignment protected');
    response = await request('/rotas', admin.token);
    assert.ok((await response.json()).some(row => row.id === route && row.localizacaoAtual.latitude === -23.56));
    console.log('PASS published Firestore rules: own reads, GPS and profile edits; company, email, role and assignment isolation');
  }
  console.log('LIVE_INTEGRATION_OK');
} catch (error) {
  console.error('LIVE_INTEGRATION_FAILED', error.name, error.message);
  process.exitCode = 1;
} finally {
  for (const path of docs) {
    try { await db.doc(path).delete(); } catch { cleanupFailed = true; console.error('CLEANUP_DOCUMENT_FAILED', path); }
  }
  for (const uid of users) {
    try { await auth.deleteUser(uid); } catch { cleanupFailed = true; console.error('CLEANUP_ACCOUNT_FAILED', uid); }
  }
  await deleteApp(app);
  if (cleanupFailed) process.exitCode = 1;
  else console.log('PASS all temporary accounts and documents removed');
}
