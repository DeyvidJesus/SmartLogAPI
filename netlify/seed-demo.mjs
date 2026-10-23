// Persistent demo fixtures, explicitly requested for the NEXT rehearsal.
// Re-running adds missing fixtures only; never resets edits or existing customer data.
import {readFile, writeFile, mkdir, chmod} from 'node:fs/promises';
import {randomBytes} from 'node:crypto';
import assert from 'node:assert/strict';
import {initializeApp, cert, deleteApp} from 'firebase-admin/app';
import {getAuth} from 'firebase-admin/auth';
import {getFirestore, Timestamp} from 'firebase-admin/firestore';

if (!process.argv.includes('--apply')) throw new Error('Use --apply to populate the isolated demonstration company.');
const root = new URL('../../', import.meta.url);
const credential = JSON.parse(await readFile(new URL('firebase-key.json', root)));
assert.equal(credential.project_id, 'smartlog-b9b37');
const app = initializeApp({credential: cert(credential)}, 'smartlog-demo-seed');
const auth = getAuth(app), db = getFirestore(app);
const tenant = 'smartlog-next-demo-2026', seed = 'next-demo-v1';
const local = new URL('.local/', root), accessFile = new URL('smartlog-demo-access.json', local);
await mkdir(local, {recursive: true, mode: 0o700});
let access;
try { access = JSON.parse(await readFile(accessFile, 'utf8')); }
catch (error) {
  if (error.code !== 'ENOENT') throw error;
  access = {tenant, password: 'Next-' + randomBytes(12).toString('base64url'), users: []};
  await writeFile(accessFile, JSON.stringify(access, null, 2), {mode: 0o600});
}
assert.equal(access.tenant, tenant);
const timestamp = Timestamp.now();
let added = 0, preserved = 0;
async function put(collection, id, values) {
  const ref = db.collection(collection).doc(id), doc = await ref.get();
  if (doc.exists) {
    assert.equal(doc.get('demoSeed'), seed, 'Fixture ID already belongs to other data: ' + id);
    assert.equal(doc.get('empresaId'), tenant);
    preserved++; return;
  }
  await ref.create({...values, empresaId: tenant, demoSeed: seed, demonstracao: true});
  added++;
}
const people = [
  ['admin','ADMINISTRADOR','Gestor Demo NEXT',true],
  ['motorista1','MOTORISTA','Ana Demo — Paulista',true],
  ['motorista2','MOTORISTA','Bruno Demo — Centro',true],
  ['inativo','MOTORISTA','Motorista Demo Inativo',false],
  ['cliente1','CLIENTE','Loja Alfa Demo',true],
  ['cliente2','CLIENTE','Loja Beta Demo',true],
  ['cliente3','CLIENTE','Loja Gama Demo',true],
].map(([alias,role,name,active]) => ({alias,role,name,active,uid:tenant+'-'+alias,email:alias+'.next2026@example.test'}));
const person = alias => people.find(p => p.alias === alias);
const routeId = n => tenant+'-rota-'+n;
const deliveryId = n => 'N'+String(n).padStart(3,'0')+'-next-demo-2026';
try {
  await put('empresas',tenant,{id:tenant,nomeFantasia:'SmartLog NEXT — DEMONSTRAÇÃO',razaoSocial:'Empresa fictícia para ensaio',
    dataCriacao:timestamp,email:person('admin').email,telefone:'(11) 0000-0000',endereco:'São Paulo — operação fictícia'});
  for (const p of people) {
    try {
      const existing = await auth.getUser(p.uid);
      assert.equal(existing.email,p.email,'Demo account email changed; no automatic overwrite');
      const profile = await db.collection('usuarios').doc(p.uid).get();
      if (profile.exists) assert.equal(profile.get('demoSeed'),seed);
    } catch(error) {
      if (error.code !== 'auth/user-not-found') throw error;
      await auth.createUser({uid:p.uid,email:p.email,password:access.password,displayName:p.name,emailVerified:false});
    }
    await put('usuarios',p.uid,{uid:p.uid,id:p.uid,nome:p.name,email:p.email,tipoUsuario:p.role,tipo:p.role,
      ativo:p.active,telefone:'(11) 0000-0000',dataCadastro:timestamp,criadoEm:timestamp,atualizadoEm:timestamp});
  }
  access.users = people.map(({alias,email,role,active}) => ({alias,email,role,active}));
  await writeFile(accessFile,JSON.stringify(access,null,2),{mode:0o600});
  await chmod(accessFile,0o600);
  const vehicles = [
    {placa:'DEM0A01',modelo:'Van Demo',tipo:'VAN',capacidadeKg:1500,capacidadeM3:10,ano:2024,ativo:true},
    {placa:'DEM0A02',modelo:'Caminhão Demo',tipo:'CAMINHAO',capacidadeKg:6000,capacidadeM3:28,ano:2023,ativo:true},
    {placa:'DEM0A03',modelo:'Reserva Demo',tipo:'VAN',capacidadeKg:1000,capacidadeM3:8,ano:2021,ativo:false},
  ];
  for (const [i,v] of vehicles.entries()) await put('veiculos',tenant+'-veiculo-'+(i+1),{...v,criadaEm:timestamp,atualizadaEm:timestamp});
  const configurations = [
    [1,'Paulista — iniciar no tablet','motorista1','Pendente','Paulista'],
    [2,'Centro — GPS fictício inicial','motorista2','Em Rota','Centro'],
    [3,'Histórico — concluída','motorista2','Concluida','Centro'],
    [4,'Reserva — sem motorista',null,'Pendente','Zona Sul'],
  ];
  const statuses = ['Pendente','Pendente','Pendente','Em Rota','Em Rota','Em Rota','Entregue','Entregue','Atrasada','Cancelada','Pendente','Pendente'];
  const routes = [1,1,1,2,2,2,3,3,2,3,4,4];
  const clientAliases = ['cliente1','cliente1','cliente2','cliente2','cliente3','cliente3','cliente1','cliente2','cliente3','cliente2','cliente1','cliente3'];
  const addresses = ['Avenida Paulista, 1578, São Paulo','Avenida Paulista, 900, São Paulo','Rua Augusta, 1500, São Paulo',
    'Praça da Sé, São Paulo','Rua Boa Vista, 200, São Paulo','Praça da República, São Paulo'];
  const deliveries = [];
  for(let n=1;n<=12;n++) {
    const c = person(clientAliases[n-1]), route = configurations.find(r=>r[0]===routes[n-1]);
    const driver = route[2] ? person(route[2]) : null;
    const code = 'ENT-N'+String(n).padStart(3,'0');
    const created = Timestamp.fromMillis(timestamp.toMillis() - (13-n)*3600000);
    const data = {id:code,codigo:code,clienteId:c.uid,cliente:c.name,emailCliente:c.email,telefoneCliente:'(11) 0000-0000',
      motoristaId:driver?.uid??'',motorista:driver?.name??'',rotaId:routeId(route[0]),regiao:route[4],
      status:statuses[n-1],origem:'Centro de distribuição DEMO — São Paulo',
      endereco:addresses[(n-1)%addresses.length],destino:addresses[(n-1)%addresses.length],
      latitude:-23.5505-(n%3)*0.003,longitude:-46.6333-(n%3)*0.002,
      criadoEm:created,criadaEm:created,atualizadaEm:timestamp,ordemEntrega:n,
      pesoKg:20+n*5,volumeM3:Number((0.2+n*0.1).toFixed(1)),volumes:n%4+1,valorNfe:100+n*75,
      chaveNfe:String(n).padStart(44,'0'),janelaEntrega:n%2?'MANHA':'TARDE',
      observacoes:'DADOS FICTÍCIOS DE DEMONSTRAÇÃO. Não realizar entrega ou cobrança.',
      ...(statuses[n-1]==='Entregue'?{entregueEm:timestamp,recebedor:'Recebedor fictício'}:{})};
    await put('entregas',deliveryId(n),data); deliveries.push({n,...data});
  }
  for(const [n,name,alias,status,region] of configurations) {
    const stops = deliveries.filter(d=>d.rotaId===routeId(n)).map(d=>({entregaId:deliveryId(d.n),cliente:d.cliente,endereco:d.endereco}));
    await put('rotas',routeId(n),{nome:name,motoristaId:alias?person(alias).uid:'',motoristaNome:alias?person(alias).name:'',
      status,regiao:region,veiculoId:tenant+'-veiculo-'+(n===1?1:2),enderecos:stops,totalEnderecos:stops.length,
      criadoEm:timestamp,atualizadaEm:timestamp,...(n===2?{localizacaoAtual:{latitude:-23.5505,longitude:-46.6333,
        atualizadoEm:Timestamp.fromMillis(timestamp.toMillis()-3600000)},localizacaoDemonstrativa:true}:{})});
  }
  const lines = ['# Acessos privados — SmartLog DEMO','',
    'Site: https://smartlog-next-2026.netlify.app/login','',
    'Senha inicial das contas abaixo: '+access.password,'',
    ...people.map(p=>p.alias+': '+p.email+' — '+p.role+(p.active?'':' (acesso bloqueado por cadastro inativo)')),
    '', 'Use admin no painel e motorista1 no tablet. Cliente1 testa a visão de cliente no aplicativo.',
    'E-mails fictícios: não recebem recuperação de senha. Não publicar este arquivo.'];
  await writeFile(new URL('ACESSOS_SMARTLOG_DEMO.md',local),lines.join('\n')+'\n',{mode:0o600});
  console.log('DEMO_READY',JSON.stringify({empresa:tenant,usuarios:7,veiculos:3,rotas:4,entregas:12,adicionados:added,preservados:preserved}));

  // One compact read-only confirmation, without changing the rehearsal statuses.
  const env = await readFile(new URL('SmartLogWeb/src/environments/environment.ts',root),'utf8');
  const apiKey = env.match(/firebaseApiKey:\s*'([^']+)'/)[1];
  const signIn = await fetch('https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key='+apiKey,{
    method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email:person('admin').email,password:access.password,returnSecureToken:true})});
  assert.equal(signIn.status,200,'Demo admin login');
  const token = (await signIn.json()).idToken;
  const counts = await Promise.all(['entregas','rotas','motoristas','clientes','veiculos'].map(async resource=>{
    const r=await fetch('https://smartlog-next-2026.netlify.app/api/v1/'+resource,{headers:{Authorization:'Bearer '+token},signal:AbortSignal.timeout(30000)});
    assert.equal(r.status,200,resource); const payload=await r.json();
    const count=(resource==='entregas'?payload.data:payload).length;
    assert.ok(count>=({entregas:12,rotas:4,motoristas:3,clientes:3,veiculos:3}[resource]),resource);
    return resource+': '+count;
  }));
  console.log('PASS login and published API data:',counts.join(', '));
} finally { await deleteApp(app); }
