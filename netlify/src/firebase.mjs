import {initializeApp, getApps, cert} from 'firebase-admin/app';
import {getAuth} from 'firebase-admin/auth';
import {getFirestore} from 'firebase-admin/firestore';
import {createHash, randomBytes} from 'node:crypto';
import {ApiError, roleOf, active} from './schema.mjs';

function clean(data) {
  return Object.fromEntries(Object.entries(data).filter(([name, value]) => name !== 'docId' && value !== undefined));
}
export class ScopedStore {
  constructor(database) { this.db = database; }
  async resolveTracking(hash) {
    const doc = await this.db.collection('_trackingLinks').doc(hash).get();
    return doc.exists ? doc.data() : null;
  }
  async all(tenant, collection) {
    const result = await this.db.collection(collection).where('empresaId', '==', tenant).get();
    return result.docs.map(doc => ({...doc.data(), docId: doc.id}));
  }
  async get(tenant, collection, id) {
    const doc = await this.db.collection(collection).doc(id).get();
    return doc.exists && doc.get('empresaId') === tenant ? {...doc.data(), docId: doc.id} : null;
  }
  async mutate(tenant, collection, id, change, {create = false} = {}) {
    const ref = this.db.collection(collection).doc(id);
    return this.db.runTransaction(async transaction => {
      const doc = await transaction.get(ref);
      if (doc.exists && doc.get('empresaId') !== tenant) throw new ApiError(404, 'Registro não encontrado.');
      if (!doc.exists && !create) throw new ApiError(404, 'Registro não encontrado.');
      const previous = doc.exists ? {...doc.data(), docId: doc.id} : null;
      const patch = clean(await change(previous));
      const merged = {...(previous ?? {}), ...patch, empresaId: tenant, docId: id};
      transaction.set(ref, {...patch, empresaId: tenant}, {merge: true});
      return merged;
    });
  }
  async remove(tenant, collection, id, validate = () => {}) {
    const ref = this.db.collection(collection).doc(id);
    await this.db.runTransaction(async transaction => {
      const doc = await transaction.get(ref);
      if (!doc.exists || doc.get('empresaId') !== tenant) throw new ApiError(404, 'Registro não encontrado.');
      validate(doc.data());
      transaction.delete(ref);
    });
  }
  async claimChatSlot(tenant, uid) {
    const id = createHash('sha256').update(tenant + ':' + uid).digest('hex');
    const ref = this.db.collection('_assistantRateLimits').doc(id);
    const now = Date.now();
    await this.db.runTransaction(async transaction => {
      const previous = await transaction.get(ref);
      if (previous.exists && now - previous.get('lastAt') < 3000)
        throw new ApiError(429, 'Aguarde alguns segundos antes da próxima pergunta.');
      transaction.set(ref, {lastAt: now});
    });
  }
}

export function firebaseDependencies(env = process.env) {
  const projectId = env.FIREBASE_PROJECT_ID || 'smartlog-b9b37';
  const emulator = env.SMARTLOG_EMULATOR === 'true';
  const hasEmulators = env.FIREBASE_AUTH_EMULATOR_HOST && env.FIRESTORE_EMULATOR_HOST;
  if (emulator && (!hasEmulators || !projectId.startsWith('demo-')))
    throw new ApiError(503, 'Os dois emuladores e um projeto demo são obrigatórios.');
  if (!emulator && (env.FIREBASE_AUTH_EMULATOR_HOST || env.FIRESTORE_EMULATOR_HOST))
    throw new ApiError(503, 'Configuração Firebase inconsistente.');
  const name = 'smartlog-netlify-' + projectId;
  let app = getApps().find(value => value.name === name);
  if (!app) {
    if (emulator) app = initializeApp({projectId}, name);
    else {
      if (!env.FIREBASE_SERVICE_ACCOUNT_JSON)
        throw new ApiError(503, 'Configure a identidade Firebase no servidor.');
      let account;
      try { account = JSON.parse(env.FIREBASE_SERVICE_ACCOUNT_JSON); }
      catch { throw new ApiError(503, 'A identidade Firebase do servidor é inválida.'); }
      if (account.type !== 'service_account' || account.project_id !== projectId)
        throw new ApiError(503, 'A identidade Firebase não corresponde ao projeto.');
      app = initializeApp({credential: cert(account), projectId}, name);
    }
  }
  const database = getFirestore(app);
  const auth = getAuth(app);
  const store = new ScopedStore(database);
  return {
    store,
    async authenticate(request) {
      const bearer = request.headers.get('authorization')?.match(/^Bearer ([^\s]+)$/i)?.[1];
      if (!bearer || bearer.length > 16000) throw new ApiError(401, 'Autenticação necessária.', 'UNAUTHENTICATED');
      let decoded;
      try { decoded = await auth.verifyIdToken(bearer); }
      catch { throw new ApiError(401, 'Sessão inválida ou expirada.', 'UNAUTHENTICATED'); }
      const doc = await database.collection('usuarios').doc(decoded.uid).get();
      const profile = doc.data();
      if (!profile || !['ADMIN', 'MOTORISTA', 'CLIENTE'].includes(roleOf(profile)) ||
          typeof profile.empresaId !== 'string' || !profile.empresaId.trim() || !active(profile))
        throw new ApiError(401, 'Cadastro sem acesso à operação.', 'UNAUTHENTICATED');
      return {uid: decoded.uid, tenant: profile.empresaId, role: roleOf(profile), profile};
    },
    async provision({tenant, uid, email, name, role}) {
      let user;
      try { user = uid ? await auth.getUser(uid) : await auth.getUserByEmail(email); }
      catch (error) {
        if (error.code !== 'auth/user-not-found') throw new ApiError(503, 'Cadastro Firebase indisponível.');
        if (uid) throw new ApiError(400, 'O UID informado não existe no Firebase.');
      }
      if (user) {
        if (user.email?.toLowerCase() !== email.toLowerCase())
          throw new ApiError(409, 'UID e e-mail não correspondem.');
        const doc = await database.collection('usuarios').doc(user.uid).get();
        if (doc.exists && (doc.get('empresaId') !== tenant || roleOf(doc.data()) !== role))
          throw new ApiError(409, 'Essa conta já possui outro vínculo. Utilize o cadastro existente.');
        return {uid: user.uid, created: false};
      }
      try {
        user = await auth.createUser({email, displayName: name, emailVerified: false,
          password: randomBytes(48).toString('base64url')});
      } catch (error) {
        if (error.code === 'auth/email-already-exists') throw new ApiError(409, 'O e-mail já está cadastrado.');
        throw new ApiError(503, 'Não foi possível criar a conta Firebase.');
      }
      return {uid: user.uid, created: true};
    },
    async rollback(account) {
      if (account?.created) {
        try { await auth.deleteUser(account.uid); }
        catch { console.error('Falha ao remover uma conta sem perfil após erro no cadastro.'); }
      }
    },
  };
}
