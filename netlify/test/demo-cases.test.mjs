import test from 'node:test';
import assert from 'node:assert/strict';
import {CASES,DEMO_TENANT,buildDemoCases,planDemoReset} from '../demo-cases.mjs';
const users=Object.fromEntries(['admin','motorista1','motorista2','cliente1','cliente2','cliente3'].map(alias=>[alias,{uid:alias,email:`${alias}@example.test`,name:alias}]));
const fixture=()=>buildDemoCases({users,at:'2026-10-24T12:00:00.000Z',photo:'fixture-photo',signature:'fixture-signature'});
test('four mutable live cases and six prepared examples have one distinct delivery and route each',()=>{
  const rows=fixture(),deliveries=rows.filter(row=>row.path.startsWith('entregas/')),routes=rows.filter(row=>row.path.startsWith('rotas/'));
  assert.equal(CASES.filter(row=>row.live).length,4);assert.equal(deliveries.length,10);assert.equal(routes.length,10);
  assert.equal(deliveries.filter(row=>row.data.demonstracaoAoVivo).length,4);
  for(const row of deliveries){
    assert.match(row.data.observacoes,/CASE \d{2}/);assert.match(row.data.observacoes,/FICTÍCIOS/);
    assert.equal(routes.filter(route=>route.path===`rotas/${row.data.rotaId}`).length,1);
    if(row.data.demonstracaoAoVivo){assert.ok(!['Entregue','Cancelada'].includes(row.data.status));assert.equal(row.data.motoristaId,'motorista1');assert.equal(row.data.comprovante,undefined);}
  }
  assert.equal(routes.filter(row=>row.data.motoristaId==='motorista1'&&row.data.status==='Em Rota').length,1,'GPS start must select one active route on the presentation tablet');
  assert.ok(routes.every(row=>!row.data.localizacaoAtual),'do not invent current GPS');
});
test('prepared receipt is explicitly illustrative and open/resolved incidents are separated',()=>{
  const rows=fixture(),receipt=rows.find(row=>row.path==='entregas/next-case-05').data.comprovante;
  assert.equal(receipt.demonstracao,true);assert.match(receipt.recebedor,/FICTÍCIO/);assert.equal(receipt.latitude,null);
  const incidents=rows.filter(row=>row.path.startsWith('ocorrencias/'));
  assert.equal(incidents.length,4);assert.equal(incidents.filter(row=>row.data.status==='ABERTA').length,3);
  const partial=rows.find(row=>row.path==='entregas/next-case-09').data;
  assert.notEqual(partial.status,'Entregue');assert.equal(partial.volumes,5);
  assert.equal(rows.find(row=>row.path==='ocorrencias/next-ocorrencia-09').data.volumesRecebidos,2);
});
test('reset plan refuses accounts other companies and broad targets before any mutation',()=>{
  for(const bad of [{path:'usuarios/admin',data:{empresaId:DEMO_TENANT}},
    {path:'entregas/customer-data',data:{empresaId:'other-company'}},{path:'entregas/nested/doc',data:{empresaId:DEMO_TENANT}}])
    assert.throws(()=>planDemoReset([bad],fixture()),/fora dos dados/);
  const plan=planDemoReset([{path:'entregas/old-demo',data:{empresaId:DEMO_TENANT}}],fixture());
  assert.equal(plan.remove.length,1);assert.ok(plan.write.every(row=>!row.path.startsWith('usuarios/')));
  assert.throws(()=>planDemoReset(Array.from({length:450},(_,n)=>({path:`entregas/old-${n}`,data:{empresaId:DEMO_TENANT}})),fixture()),/Limite de segurança/);
});
