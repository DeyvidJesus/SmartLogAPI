import {deliveryDto, routeStatus} from './schema.mjs';
export function operationalAnswer(question, rows, routes, role) {
  const q = question.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[?!.]/g, '').trim();
  const open = rows.filter(r => !['ENTREGUE', 'CANCELADA'].includes(deliveryDto(r).status));
  if (/^(qual (e )?(a )?(minha )?)?(minha )?proxima entrega$/.test(q) || q === 'minha rota') {
    if (role !== 'MOTORISTA') return 'Para consultar sua próxima parada, entre com o perfil de motorista. No painel, consulte as entregas por rota.';
    if (!open.length) return 'Você não tem entregas em aberto.';
    const routeIds = new Set(open.map(r => r.rotaId).filter(Boolean));
    const underway = routes.filter(r => routeIds.has(r.docId) && routeStatus(r.status) === 'EM_ANDAMENTO');
    const selected = underway.length === 1 ? underway[0].docId : routeIds.size === 1 ? [...routeIds][0] : null;
    if (!selected) return 'Há entregas em mais de uma rota. Inicie a rota desejada para identificar a próxima parada.';
    const stops = open.filter(r => r.rotaId === selected).sort((a,b) =>
      (Number.isFinite(a.ordemEntrega) ? a.ordemEntrega : Infinity) - (Number.isFinite(b.ordemEntrega) ? b.ordemEntrega : Infinity));
    if (stops.length > 1 && stops.some(r => !Number.isFinite(r.ordemEntrega)) ||
        stops.length > 1 && stops[0].ordemEntrega === stops[1].ordemEntrega)
      return 'A sequência das paradas ainda não está definida. Organize a ordem da rota no aplicativo.';
    const next = deliveryDto(stops[0]);
    return 'Próxima entrega na sequência cadastrada: ' + next.codigo + '. Destino: ' +
      (next.destino || 'endereço não informado') + '. Restam ' + stops.length + ' entregas nesta rota.';
  }
  if (/^(quantas (entregas )?faltam|quais entregas (ainda )?estao pendentes|entregas pendentes)$/.test(q)) {
    const pending = q.includes('pendentes') ? open.filter(r => deliveryDto(r).status === 'PENDENTE') : open;
    return pending.length + ' entrega(s) ' + (q.includes('pendentes') ? 'pendente(s)' : 'em aberto') +
      (pending.length ? ': ' + pending.slice(0, 10).map(r => {const d=deliveryDto(r);return d.codigo + ' — ' + (d.destino || 'endereço não informado');}).join('; ') +
      (pending.length > 10 ? '. Exibindo as primeiras 10.' : '.') : '.');
  }
  if (q === 'resumir operacao' || q === 'resumo da operacao') return operationSummary(rows);
  return null;
}
export function operationSummary(rows) {
  const counts = {};
  for (const row of rows) { const status=deliveryDto(row).status; counts[status]=(counts[status] || 0)+1; }
  return rows.length ? 'Registros atuais: ' + Object.entries(counts).map(([s,n])=>n+' '+s.replaceAll('_',' ').toLowerCase()).join('; ')+'.' : 'Nenhuma entrega no seu escopo.';
}
