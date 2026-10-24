// Local, synthetic integration fixture. Never imported by the deployed Function.
import http from 'node:http';
import {Readable} from 'node:stream';
import {readFile} from 'node:fs/promises';
import {extname, resolve} from 'node:path';
import {createHandler} from '../../src/api.mjs';
import {ApiError} from '../../src/schema.mjs';

const rows = new Map();
const store = {
  async all(tenant, collection) {return [...rows.entries()].filter(([key,row]) => key.startsWith(collection+'/') && row.empresaId===tenant).map(([,row])=>structuredClone(row));},
  async get(tenant, collection, id) {const row=rows.get(collection+'/'+id);return row?.empresaId===tenant ? structuredClone(row) : null;},
  async mutate(tenant,collection,id,change,{create=false}={}) {
    const old=rows.get(collection+'/'+id);
    if(old && old.empresaId!==tenant || !old && !create) throw new ApiError(404,'Registro não encontrado.');
    const row={...old,...await change(old?structuredClone(old):null),empresaId:tenant,docId:id};
    rows.set(collection+'/'+id,row);return structuredClone(row);
  },
  async resolveTracking(hash) {return rows.get('_trackingLinks/'+hash) ?? null;},
  async claimChatSlot() {},
};
await store.mutate('integration','entregas','delivery-e2e',()=>({id:'ENT-E2E',codigo:'ENT-E2E',motoristaId:'driver',clienteId:'client',emailCliente:'client@example.test',status:'Em Rota',volumes:2,janelaEntrega:'TARDE',origem:'Centro de distribuição teste',destino:'Endereço de teste',criadaEm:new Date().toISOString()}),{create:true});
const handler=createHandler({store,
  // Deterministic provider only in this localhost fixture; live Gemini is checked separately.
  model:async ({context})=>context.entregas.map(row=>`${row.codigo}: ${row.status}`).join('; '),
  authenticate:async request=>{
  const role={'Bearer integration-admin':'ADMIN','Bearer integration-driver':'MOTORISTA','Bearer integration-client':'CLIENTE'}[request.headers.get('authorization')];
  if(!role) throw new ApiError(401,'Autenticação necessária.');
  return {role,tenant:'integration',uid:role==='ADMIN'?'admin':role==='MOTORISTA'?'driver':'client',profile:{nome:'Usuário de teste',email:role==='CLIENTE'?'client@example.test':'driver@example.test'}};
}});
const root=resolve(new URL('../../../../SmartLogWeb/dist/SmartLogWeb/browser',import.meta.url).pathname);
const mime={'.js':'text/javascript','.css':'text/css','.html':'text/html','.woff2':'font/woff2','.webp':'image/webp','.png':'image/png'};
const port=Number(process.env.SMARTLOG_INTEGRATION_PORT || 4300);
const server=http.createServer(async(req,res)=>{
  try {
    if(req.url.startsWith('/api/')) {
      const request=new Request(`http://127.0.0.1:${port}${req.url}`,{method:req.method,headers:req.headers,...(!['GET','HEAD'].includes(req.method)?{body:req,duplex:'half'}:{})});
      const response=await handler(request);res.writeHead(response.status,Object.fromEntries(response.headers));
      if(response.body) Readable.fromWeb(response.body).pipe(res);else res.end();return;
    }
    const path=decodeURIComponent(new URL(req.url,`http://127.0.0.1:${port}`).pathname);
    let target=resolve(root,'.'+path);
    if(!target.startsWith(root+'/')) target=resolve(root,'index.html');
    let body;
    try {body=await readFile(target);}catch {target=resolve(root,'index.html');body=await readFile(target);}
    res.writeHead(200,{'Content-Type':mime[extname(target)]||'application/octet-stream','Cache-Control':'no-store'});res.end(body);
  } catch(error) {console.error(error);res.writeHead(500);res.end('Integration fixture error');}
});
server.listen(port,'127.0.0.1',()=>console.log(`Synthetic integration server listening on ${port}`));
for(const signal of ['SIGINT','SIGTERM'])process.on(signal,()=>server.close(()=>process.exit(0)));
