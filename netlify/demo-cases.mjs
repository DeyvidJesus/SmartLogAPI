export const DEMO_TENANT = 'smartlog-next-demo-2026';
export const DEMO_VERSION = 'next-fair-cases-v2';
export const OPERATION_COLLECTIONS = ['entregas', 'rotas', 'veiculos', 'ocorrencias', '_trackingLinks'];

export const CASES = [
  {n:1, live:true, title:'Entrega completa com comprovante', status:'Em Rota', routeStatus:'Em Rota', client:'cliente1', volumes:2,
    instructions:'AO VIVO 01 — No tablet do motorista1, abra ENT-NEXT-01. Inicie o GPS da rota 01 (única rota em andamento). Confirme o recebimento dos 2 volumes com foto, nome, localização e assinatura. No painel, confira o comprovante e gere o link do cliente.'},
  {n:2, live:true, title:'Entrega parcial: 3 de 4 volumes', status:'Pendente', routeStatus:'Pendente', client:'cliente1', volumes:4,
    instructions:'AO VIVO 02 — Abra ENT-NEXT-02 no tablet do motorista1 e altere para Em Rota. Registre ENTREGA PARCIAL com 3 volumes recebidos. No painel, resolva com a ação de entregar o volume faltante e uma mensagem ao cliente. O pedido permanece aberto; só confirme a entrega completa quando os 4 volumes tiverem sido recebidos.'},
  {n:3, live:true, title:'Avaria identificada no recebimento', status:'Pendente', routeStatus:'Pendente', client:'cliente2', volumes:2,
    instructions:'AO VIVO 03 — Abra ENT-NEXT-03 no tablet do motorista1. Registre AVARIA com foto de demonstração e descrição do problema na embalagem. No painel, registre a ação do gestor e a mensagem pública sobre substituição/conferência. Use o link para mostrar que as notas internas não aparecem ao cliente.'},
  {n:4, live:true, title:'Cliente ausente e nova tentativa', status:'Pendente', routeStatus:'Pendente', client:'cliente3', volumes:1,
    instructions:'AO VIVO 04 — Abra ENT-NEXT-04 no tablet do motorista1. Registre DESTINATÁRIO AUSENTE. No painel, resolva informando uma nova tentativa combinada com o cliente. Mostre a atualização no acompanhamento e depois revogue o link. Este pedido está aberto e pode ser atualizado durante a apresentação.'},
  {n:5, title:'Entrega concluída com comprovante ilustrativo', status:'Entregue', routeStatus:'Concluida', client:'cliente1', volumes:3,
    instructions:'PRONTO 05 — Exemplo já concluído para explicar o comprovante. Fotos e assinatura são ILUSTRAÇÕES DE TESTE, sem recebimento real e sem validade comercial. Abra Detalhes no painel ou no app do cliente1 e mostre recebedor, volumes e horário. Não apresentar como entrega real.'},
  {n:6, title:'Pedido cancelado pelo cliente', status:'Cancelada', routeStatus:'Cancelada', client:'cliente2', volumes:2,
    instructions:'PRONTO 06 — Pedido cancelado a pedido do cliente fictício. Use o filtro CANCELADA e o histórico para mostrar que cancelamento não é contabilizado como entrega concluída. Não reabrir durante o pitch principal.'},
  {n:7, title:'Atraso com ausência em atendimento', status:'Atrasada', routeStatus:'Pausada', client:'cliente3', volumes:1,
    occurrence:'AUSENTE', instructions:'PRONTO 07 — Entrega atrasada e ocorrência de ausência ABERTA. Mostre o alerta de atraso e a Central de Ocorrências. O gestor pode combinar nova tentativa e resolver a ocorrência; o status da entrega continua exigindo ação própria.'},
  {n:8, title:'Avaria aguardando decisão da loja', status:'Em Rota', routeStatus:'Pausada', client:'cliente2', volumes:2,
    occurrence:'AVARIA', instructions:'PRONTO 08 — Ocorrência de avaria ABERTA, com foto ilustrativa. Rota pausada para decisão da loja. Compare a descrição interna com a mensagem ao cliente e explique o fluxo de resolução sem concluir indevidamente a entrega.'},
  {n:9, title:'Entrega parcial em acompanhamento', status:'Em Rota', routeStatus:'Pausada', client:'cliente1', volumes:5,
    occurrence:'ENTREGA_PARCIAL', received:2, instructions:'PRONTO 09 — Apenas 2 dos 5 volumes foram recebidos e a ocorrência permanece ABERTA. Mostre que o pedido não foi marcado como entregue e que a quantidade pendente pode ser conferida pela equipe.'},
  {n:10, title:'Ocorrência resolvida com entrega reagendada', status:'Pendente', routeStatus:'Pausada', client:'cliente3', volumes:1,
    occurrence:'AUSENTE', resolved:true, instructions:'PRONTO 10 — Ausência já RESOLVIDA pelo gestor: nova tentativa combinada. O pedido continua pendente. Mostre que resolver uma ocorrência não equivale a confirmar a entrega e compare o filtro Resolvidas com Em aberto.'},
];

export function buildDemoCases({users, at, photo, signature}) {
  const documents=[];
  const ago = hours => new Date(Date.parse(at)-hours*3600000).toISOString();
  const add=(collection,id,data)=>documents.push({path:`${collection}/${id}`,data:{...data,empresaId:DEMO_TENANT,demoSeed:DEMO_VERSION,demonstracao:true}});
  const vehicle=id=>`${DEMO_TENANT}-next-veiculo-${id}`;
  add('veiculos',vehicle(1),{placa:'DEM0A01',modelo:'Van de demonstração NEXT',tipo:'VAN',capacidadeKg:1500,capacidadeM3:10,ativo:true,ano:2024,criadaEm:at,atualizadaEm:at});
  add('veiculos',vehicle(2),{placa:'DEM0A02',modelo:'Caminhão de demonstração NEXT',tipo:'CAMINHAO',capacidadeKg:6000,capacidadeM3:28,ativo:true,ano:2023,criadaEm:at,atualizadaEm:at});
  const addresses=['Avenida Paulista, 1578, São Paulo - SP','Avenida Paulista, 900, São Paulo - SP','Rua Augusta, 1500, São Paulo - SP','Praça da República, São Paulo - SP'];
  for(const scenario of CASES) {
    const {n,title,status,volumes}=scenario;
    const number=String(n).padStart(2,'0'), id=`next-case-${number}`, routeId=`next-rota-${number}`, code=`ENT-NEXT-${number}`;
    const driver=users[scenario.live||n<=6?'motorista1':'motorista2'], client=users[scenario.client];
    const notes=`CASE ${number} | ${title}\n${scenario.instructions}\nDADOS FICTÍCIOS NEXT. Não realizar entrega, cobrança ou contato comercial.`;
    const created=ago(scenario.live?1:24);
    const history=[{status:'PENDENTE',em:created,responsavel:'Preparação da demonstração'}];
    const canonical={'Em Rota':'EM_ROTA','Entregue':'ENTREGUE','Atrasada':'ATRASADA','Cancelada':'CANCELADA'}[status];
    if(canonical)history.push({status:canonical,em:ago(scenario.live?0:2),responsavel:'Simulação NEXT — dado fictício'});
    const delivery={id:code,codigo:code,clienteId:client.uid,cliente:client.name,emailCliente:client.email,
      motoristaId:driver.uid,motorista:driver.name,rotaId:routeId,status,origem:'Centro de distribuição fictício — São Paulo',
      endereco:addresses[(n-1)%4],destino:addresses[(n-1)%4],regiao:n%2?'Paulista':'Centro',
      volumes,pesoKg:volumes*12,volumeM3:Number((volumes*.18).toFixed(2)),valorNfe:volumes*150,
      janelaEntrega:'TARDE',observacoes:notes,casoDemonstracao:number,demonstracaoAoVivo:!!scenario.live,
      criadaEm:created,criadoEm:created,atualizadaEm:at,ordemEntrega:n,historicoStatus:history};
    if(n===5)Object.assign(delivery,{recebedor:'Recebedor FICTÍCIO — NEXT',entregueEm:ago(2),confirmadoPor:driver.uid,
      confirmadoPorNome:driver.name,observacaoEntrega:'Comprovante ILUSTRATIVO de demonstração; não houve entrega real.',
      comprovante:{requestId:'next-demo-receipt-05',recebedor:'Recebedor FICTÍCIO — NEXT',em:ago(2),volumesRecebidos:volumes,
        fotos:[photo],assinatura:signature,responsavel:'Simulação NEXT — sem validade comercial',
        observacao:'Fotos e assinatura são ilustrações de teste. GPS não foi coletado de um motorista real.',
        latitude:null,longitude:null,precisao:null,demonstracao:true}});
    add('entregas',id,delivery);
    add('rotas',routeId,{nome:`${number} | ${scenario.live?'AO VIVO':'PRONTO'} | ${title}`,motoristaId:driver.uid,motoristaNome:driver.name,
      status:scenario.routeStatus,regiao:delivery.regiao,veiculoId:vehicle(n%2?1:2),observacoes:notes,
      casoDemonstracao:number,demonstracaoAoVivo:!!scenario.live,
      enderecos:[{entregaId:id,cliente:client.name,endereco:delivery.endereco}],totalEnderecos:1,criadoEm:created,criadaEm:created,atualizadaEm:at});
    if(scenario.occurrence)add('ocorrencias',`next-ocorrencia-${number}`,{entregaId:id,codigo:code,tipo:scenario.occurrence,
      descricao:`Exemplo fictício para o CASE ${number}. ${scenario.instructions}`,
      mensagemCliente:scenario.resolved?'Uma nova tentativa foi combinada. Aguarde a confirmação de entrega.':'A equipe está acompanhando esta ocorrência de demonstração.',
      foto:scenario.occurrence==='AVARIA'?photo:null,volumesRecebidos:scenario.received??null,
      status:scenario.resolved?'RESOLVIDA':'ABERTA',criadaEm:ago(3),registradoPor:driver.uid,responsavel:driver.name,
      ...(scenario.resolved?{resolucao:'Contato fictício realizado e nova tentativa combinada. A entrega ainda não foi concluída.',resolvidaEm:ago(1),resolvidaPor:users.admin.name}:{})});
  }
  return documents;
}

export function planDemoReset(existing, desired) {
  for(const document of [...existing,...desired]) {
    const [collection,id,...extra]=document.path.split('/');
    if(!OPERATION_COLLECTIONS.includes(collection)||!id||extra.length||document.data.empresaId!==DEMO_TENANT)
      throw new Error('Alvo fora dos dados operacionais da empresa DEMO.');
  }
  if(new Set(desired.map(row=>row.path)).size!==desired.length)throw new Error('IDs de cenário duplicados.');
  const desiredPaths=new Set(desired.map(row=>row.path));
  const removes=existing.filter(row=>!desiredPaths.has(row.path));
  const writes=removes.length+desired.length+1;
  if(writes>450)throw new Error('Limite de segurança excedido; exige revisão manual do plano.');
  return {remove:removes,write:desired,writes};
}
