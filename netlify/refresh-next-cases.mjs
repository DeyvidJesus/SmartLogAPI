// Explicit maintenance of the isolated demo company. Never creates/deletes/updates Auth users.
import {readFile,writeFile,mkdir,appendFile} from 'node:fs/promises';
import {resolve} from 'node:path';
import {createHash} from 'node:crypto';
import {initializeApp,cert,deleteApp} from 'firebase-admin/app';
import {getAuth} from 'firebase-admin/auth';
import {getFirestore,FieldValue} from 'firebase-admin/firestore';
import {DEMO_TENANT,DEMO_VERSION,OPERATION_COLLECTIONS,CASES,buildDemoCases,planDemoReset} from './demo-cases.mjs';

const apply=process.argv.includes('--apply');
const backupDir=resolve(process.env.SMARTLOG_DEMO_BACKUP_DIR || '.local/next-demo-backup');
const operation=process.env.SMARTLOG_DEMO_OPERATION || DEMO_VERSION;
if(!/^[a-zA-Z0-9_-]{1,80}$/.test(operation))throw new Error('Identificador de operação inválido.');
if(!process.env.FIREBASE_SERVICE_ACCOUNT_JSON)throw new Error('Credencial administrativa Firebase ausente. Nenhum dado foi alterado.');
let account;
try{account=JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);}catch{throw new Error('Credencial Firebase inválida.');}
if(account.type!=='service_account'||account.project_id!=='smartlog-b9b37')throw new Error('Credencial não corresponde ao projeto SmartLog.');
const app=initializeApp({credential:cert(account)},'next-demo-maintenance');
const auth=getAuth(app),db=getFirestore(app);
const encoded=value=>{
  if(value==null||typeof value!=='object')return value;
  if(typeof value.toDate==='function')return {$type:'timestamp',iso:value.toDate().toISOString()};
  if(Buffer.isBuffer(value))return {$type:'bytes',base64:value.toString('base64')};
  if(value.constructor?.name==='GeoPoint')return {$type:'geopoint',latitude:value.latitude,longitude:value.longitude};
  if(value.constructor?.name==='DocumentReference')return {$type:'reference',path:value.path};
  return Array.isArray(value)?value.map(encoded):Object.fromEntries(Object.entries(value).map(([key,item])=>[key,encoded(item)]));
};
const digest=value=>createHash('sha256').update(JSON.stringify(value)).digest('hex');
const roleOf=data=>({ADMINISTRADOR:'ADMIN'}[data.tipoUsuario]||data.tipoUsuario);
async function summary(message){
  if(process.env.GITHUB_STEP_SUMMARY)await appendFile(process.env.GITHUB_STEP_SUMMARY,message+'\n');
}
let stage='validar a empresa de demonstração';
async function snapshotOperations(){
  const results=await Promise.all(OPERATION_COLLECTIONS.map(collection=>db.collection(collection).where('empresaId','==',DEMO_TENANT).get()));
  return results.flatMap(result=>result.docs).map(doc=>({path:doc.ref.path,data:doc.data(),snapshot:doc}));
}
try{
  const company=await db.collection('empresas').doc(DEMO_TENANT).get();
  if(!company.exists||company.get('demonstracao')!==true||!String(company.get('nomeFantasia')).includes('DEMONSTRA'))
    throw new Error('Empresa isolada de demonstração não confirmada. Nenhum dado foi alterado.');
  const marker=db.collection('_demoMaintenance').doc(operation);
  const previous=await marker.get();
  if(previous.exists){
    if(previous.get('empresaId')!==DEMO_TENANT||previous.get('version')!==DEMO_VERSION)throw new Error('Marcador de manutenção fora do escopo.');
    console.log('NEXT_DEMO_ALREADY_APPLIED — operação já concluída; os avanços feitos nos casos foram preservados.');
    await summary('A operação NEXT já foi aplicada. Os avanços feitos nos casos foram preservados.');
  }else{
    stage='validar os acessos existentes';
    const profiles=await db.collection('usuarios').where('empresaId','==',DEMO_TENANT).get();
    const profileFingerprint=digest(profiles.docs.map(doc=>({id:doc.id,data:encoded(doc.data())})).sort((a,b)=>a.id.localeCompare(b.id)));
    const users={};
    for(const [alias,expected] of [['admin','ADMIN'],['motorista1','MOTORISTA'],['motorista2','MOTORISTA'],['cliente1','CLIENTE'],['cliente2','CLIENTE'],['cliente3','CLIENTE']]){
      const email=`${alias}.next2026@example.test`;
      const record=await auth.getUserByEmail(email);
      const profile=profiles.docs.find(doc=>doc.id===record.uid);
      if(!profile||profile.get('empresaId')!==DEMO_TENANT||roleOf(profile.data())!==expected||profile.get('ativo')===false||record.disabled)
        throw new Error(`Acesso de demonstração ${alias} não corresponde ao perfil esperado. Nenhuma conta foi alterada.`);
      users[alias]={uid:record.uid,email:record.email,name:profile.get('nome')||record.displayName||alias};
    }
    stage='preparar e salvar o backup dos dados operacionais';
    const photo='data:image/png;base64,'+(await readFile(new URL('demo-assets/foto-demo.png',import.meta.url))).toString('base64');
    const signature='data:image/png;base64,'+(await readFile(new URL('demo-assets/assinatura-demo.png',import.meta.url))).toString('base64');
    const at=new Date().toISOString();
    const desired=buildDemoCases({users,at,photo,signature});
    const existing=await snapshotOperations();
    const plan=planDemoReset(existing,desired);
    const existingMap=new Map(existing.map(row=>[row.path,row]));
    for(const row of desired){
      if(!existingMap.has(row.path)){
        const found=await db.doc(row.path).get();
        if(found.exists)throw new Error('Um ID planejado já pertence a outro registro. Nenhum dado foi alterado.');
      }
    }
    await mkdir(backupDir,{recursive:true,mode:0o700});
    const report={empresa:DEMO_TENANT,version:DEMO_VERSION,operation,apply,at,
      deletePaths:plan.remove.map(row=>row.path),writePaths:plan.write.map(row=>row.path),
      accountsPreserved:profiles.size,liveCases:CASES.filter(row=>row.live).map(row=>({codigo:`ENT-NEXT-${String(row.n).padStart(2,'0')}`,case:row.title})),
      preparedCases:CASES.filter(row=>!row.live).map(row=>({codigo:`ENT-NEXT-${String(row.n).padStart(2,'0')}`,case:row.title}))};
    await writeFile(resolve(backupDir,'plan.json'),JSON.stringify(report,null,2),{mode:0o600});
    await writeFile(resolve(backupDir,'before.json'),JSON.stringify({empresa:DEMO_TENANT,at,
      documents:existing.map(row=>({path:row.path,data:encoded(row.data)})),
      users:profiles.docs.map(doc=>({path:doc.ref.path,data:encoded(doc.data())})),
      company:{path:company.ref.path,data:encoded(company.data())}},null,2),{mode:0o600});
    console.log('NEXT_DEMO_PLAN',JSON.stringify({empresa:DEMO_TENANT,remover:plan.remove.length,gravar:plan.write.length,perfisPreservados:profiles.size,aoVivo:4,prontos:6,backup:'before.json'}));
    if(apply){
      stage='aplicar a substituição atômica dos dados de demonstração';
      const batch=db.batch();
      for(const row of plan.remove)batch.delete(db.doc(row.path),{lastUpdateTime:row.snapshot.updateTime});
      for(const row of plan.write){
        const old=existingMap.get(row.path);
        if(old){
          const patch={...row.data};
          for(const key of Object.keys(old.data))if(!Object.hasOwn(row.data,key))patch[key]=FieldValue.delete();
          batch.update(db.doc(row.path),patch,{lastUpdateTime:old.snapshot.updateTime});
        }else batch.create(db.doc(row.path),row.data);
      }
      batch.create(marker,{empresaId:DEMO_TENANT,version:DEMO_VERSION,operation,appliedAt:at,counts:{entregas:10,rotas:10,veiculos:2,ocorrencias:4}});
      await batch.commit();
      stage='conferir o resultado e os perfis preservados';
      const afterProfiles=await db.collection('usuarios').where('empresaId','==',DEMO_TENANT).get();
      if(digest(afterProfiles.docs.map(doc=>({id:doc.id,data:encoded(doc.data())})).sort((a,b)=>a.id.localeCompare(b.id)))!==profileFingerprint)
        throw new Error('Dados operacionais preparados, mas um perfil foi alterado simultaneamente. Confira before.json; o script não grava perfis.');
      const after=await snapshotOperations();
      const counts=Object.fromEntries(OPERATION_COLLECTIONS.map(collection=>[collection,after.filter(row=>row.path.startsWith(collection+'/')).length]));
      if(counts.entregas!==10||counts.rotas!==10||counts.veiculos!==2||counts.ocorrencias!==4||counts._trackingLinks!==0)
        throw new Error('A operação foi aplicada, mas a conferência encontrou alterações simultâneas. Consulte o relatório.');
      const result={...report,success:true,counts,accountsUnchanged:true};
      await writeFile(resolve(backupDir,'result.json'),JSON.stringify(result,null,2),{mode:0o600});
      console.log('NEXT_DEMO_READY',JSON.stringify({empresa:DEMO_TENANT,counts,aoVivo:4,prontos:6,accountsUnchanged:true}));
      await summary('## Demonstração NEXT atualizada\n\nConferência concluída: **10 pedidos, 10 rotas, 2 veículos, 4 ocorrências** e nenhum link antigo.\n\n**4 casos ao vivo + 6 exemplos prontos. Todos os perfis foram preservados.** Nenhuma conta ou senha foi alterada.\n\nBackup e relatório disponíveis no artefato desta execução.');
    }else{
      console.log('NEXT_DEMO_PREVIEW_ONLY — apenas leitura e backup; nenhum dado foi alterado.');
      await summary('Prévia concluída com backup: 4 casos ao vivo + 6 exemplos prontos. Nenhum dado foi alterado.');
    }
  }
}catch(error){
  console.error('NEXT_DEMO_FAILED',error.code||error.name,error.message);
  await summary(`A preparação parou ao ${stage}. Verifique os logs e o backup desta execução antes de repetir.`);
  process.exitCode=1;
}finally{await deleteApp(app);}
