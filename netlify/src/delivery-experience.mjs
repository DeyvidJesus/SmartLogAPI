import {createHash, randomBytes} from 'node:crypto';
import {ApiError, deliveryDto, deliveryStatus, requiredText, optionalText} from './schema.mjs';

export const occurrenceTypes = ['AUSENTE', 'AVARIA', 'ENDERECO_INCORRETO', 'RECUSA', 'ENTREGA_PARCIAL', 'OUTRO'];
export const tokenHash = token => createHash('sha256').update(token).digest('hex');

function image(value, required = false, signature = false) {
  if (value == null && !required) return null;
  const match = typeof value === 'string' && value.match(/^data:image\/(jpeg|png);base64,([A-Za-z0-9+/]+={0,2})$/);
  if (!match) throw new ApiError(400, 'Envie uma imagem JPEG ou PNG válida.');
  const bytes = Buffer.from(match[2], 'base64');
  const png = bytes.subarray(0, 8).equals(Buffer.from([137,80,78,71,13,10,26,10]));
  const jpeg = bytes[0] === 255 && bytes[1] === 216 && bytes[2] === 255;
  if (bytes.length < 50 || bytes.length > (signature ? 50000 : 150000) ||
      (match[1] === 'png' ? !png : !jpeg)) throw new ApiError(400, 'Imagem inválida ou muito grande.');
  return value;
}

function integer(body, field) {
  const value = body[field];
  if (!Number.isSafeInteger(value) || value < 1 || value > 100000) throw new ApiError(400, 'Quantidade de volumes inválida.');
  return value;
}

function permission(user, data, mutate = false) {
  if (mutate && !['ADMIN', 'MOTORISTA'].includes(user.role)) throw new ApiError(403, 'Acesso não permitido.');
  const client = user.role === 'CLIENTE' && (data.clienteId === user.uid ||
    (user.profile.email && user.profile.email.toLowerCase() === String(data.emailCliente ?? '').toLowerCase()));
  if (user.role !== 'ADMIN' && !(user.role === 'MOTORISTA' && data.motoristaId === user.uid) && !client)
    throw new ApiError(403, 'Entrega fora do seu escopo.');
}

export const occurrenceSummary = row => {
  const {foto, empresaId, registradoPor, ...rest} = row;
  return {...rest, temFoto: !!foto};
};
const summary = occurrenceSummary;

export function deliveryExperience({store, now}) {
  const visibleOccurrence = (row, user) => user.role === 'CLIENTE' ? {
    docId: row.docId, entregaId: row.entregaId, codigo: row.codigo, tipo: row.tipo,
    status: row.status, criadaEm: row.criadaEm, mensagemCliente: row.mensagemCliente || 'A equipe está acompanhando esta ocorrência.',
    volumesRecebidos: row.volumesRecebidos, resolvidaEm: row.resolvidaEm ?? null, temFoto: false,
  } : summary(row);
  async function load(user, id, mutate = false) {
    const row = await store.get(user.tenant, 'entregas', id);
    if (!row) throw new ApiError(404, 'Entrega não encontrada.');
    permission(user, row, mutate);
    return row;
  }
  async function occurrences(user, id) {
    return (await store.all(user.tenant, 'ocorrencias')).filter(row => row.entregaId === id)
      .sort((a,b) => b.criadaEm.localeCompare(a.criadaEm));
  }
  async function publicTracking(token) {
    if (!/^[A-Za-z0-9_-]{43}$/.test(token)) throw new ApiError(404, 'Link inválido ou expirado.');
    const hash = tokenHash(token);
    const link = await store.resolveTracking(hash);
    if (!link || Date.parse(link.expiraEm) <= Date.parse(now())) throw new ApiError(404, 'Link inválido ou expirado.');
    const data = await store.get(link.empresaId, 'entregas', link.entregaId);
    if (!data || data.rastreamentoHash !== hash) throw new ApiError(404, 'Link inválido ou expirado.');
    const rows = await occurrences({tenant: link.empresaId}, data.docId);
    const receipt = data.comprovante;
    return {codigo: deliveryDto(data).codigo, status: deliveryStatus(data.status),
      janelaEntrega: data.janelaEntrega ?? null, atualizadaEm: deliveryDto(data).atualizadaEm,
      historico: (data.historicoStatus ?? []).map(event => ({status: event.status, em: event.em})),
      ocorrencias: rows.map(row => ({tipo: row.tipo, status: row.status, criadaEm: row.criadaEm,
        mensagem: row.mensagemCliente || 'A equipe está acompanhando esta ocorrência.',
        resolvidaEm: row.resolvidaEm ?? null})),
      comprovante: receipt ? {recebedor: receipt.recebedor, em: receipt.em,
        volumesRecebidos: receipt.volumesRecebidos, fotos: receipt.fotos} : null,
      expiraEm: link.expiraEm};
  }

  async function handle(request, user, parts, body = null) {
    const id = parts[1], feature = parts[2];
    const author = user.profile.nome || 'Usuário';
    const data = await load(user, id);
    if (feature === 'comprovante' && parts.length === 3) {
      if (request.method === 'GET') {
        if (!data.comprovante) throw new ApiError(404, 'Comprovante ainda não disponível.');
        return data.comprovante;
      }
      if (request.method === 'POST') {
        permission(user, data, true);
        const requestId = requiredText(body, 'requestId', 80);
        if (!/^[a-zA-Z0-9-]{16,80}$/.test(requestId)) throw new ApiError(400, 'Identificador de envio inválido.');
        if (data.comprovante?.requestId === requestId) return deliveryDto(data);
        const recebedor = requiredText(body, 'recebedor', 120);
        const observacao = optionalText(body, 'observacao', 500);
        if (!Array.isArray(body.fotos) || body.fotos.length < 1 || body.fotos.length > 2)
          throw new ApiError(400, 'Inclua uma ou duas fotos da entrega.');
        const fotos = body.fotos.map(value => image(value, true));
        const assinatura = image(body.assinatura, true, true);
        const {latitude, longitude, precisao} = body;
        if (![latitude, longitude, precisao].every(Number.isFinite) || Math.abs(latitude) > 90 ||
          Math.abs(longitude) > 180 || precisao < 0 || precisao > 1000) throw new ApiError(400, 'Capture uma localização válida (precisão de até 1 km).');
        const volumesRecebidos = integer(body, 'volumesRecebidos');
        const em = now();
        const updated = await store.mutate(user.tenant, 'entregas', id, previous => {
          permission(user, previous, true);
          if (previous.comprovante?.requestId === requestId) return {};
          if (previous.comprovante || ['ENTREGUE', 'CANCELADA'].includes(deliveryStatus(previous.status)))
            throw new ApiError(409, 'Entrega já encerrada. O comprovante não pode ser sobrescrito.');
          if (previous.volumes != null && volumesRecebidos !== previous.volumes)
            throw new ApiError(400, 'Quantidade diferente do pedido. Registre uma ocorrência de entrega parcial.');
          return {status: 'Entregue', recebedor, observacaoEntrega: observacao, entregueEm: em,
            confirmadoPor: user.uid, confirmadoPorNome: author, atualizadaEm: em,
            comprovante: {requestId, recebedor, observacao, em, fotos, assinatura,
              latitude, longitude, precisao, volumesRecebidos, responsavel: author},
            historicoStatus: [...(previous.historicoStatus ?? []), {status: 'ENTREGUE', em, responsavel: author}].slice(-50)};
        });
        return deliveryDto(updated);
      }
    }
    if (feature === 'ocorrencias') {
      if (request.method === 'GET' && parts.length === 3) return (await occurrences(user, id)).map(row => visibleOccurrence(row, user));
      if (request.method === 'POST' && parts.length === 3) {
        permission(user, data, true);
        const tipo = requiredText(body, 'tipo', 30);
        if (!occurrenceTypes.includes(tipo)) throw new ApiError(400, 'Tipo de ocorrência inválido.');
        const descricao = requiredText(body, 'descricao', 1500);
        const mensagemCliente = optionalText(body, 'mensagemCliente', 300);
        const requestId = requiredText(body, 'requestId', 80);
        if (!/^[a-zA-Z0-9-]{16,80}$/.test(requestId)) throw new ApiError(400, 'Identificador de envio inválido.');
        const oid = tokenHash(`${id}:${user.uid}:${requestId}`);
        const foto = image(body.foto);
        const volumesRecebidos = tipo === 'ENTREGA_PARCIAL' ? integer(body, 'volumesRecebidos') : null;
        if (tipo === 'ENTREGA_PARCIAL' && (data.volumes == null || volumesRecebidos >= data.volumes))
          throw new ApiError(400, 'Informe os volumes recebidos, abaixo do total cadastrado no pedido.');
        const saved = await store.mutate(user.tenant, 'ocorrencias', oid, previous => {
          if (previous) return {};
          return {entregaId: id, codigo: deliveryDto(data).codigo, tipo, descricao, mensagemCliente,
            foto, volumesRecebidos, status: 'ABERTA', criadaEm: now(), registradoPor: user.uid,
            responsavel: author};
        }, {create: true});
        return {status: 201, body: summary(saved)};
      }
      if (parts.length === 4) {
        const occurrence = await store.get(user.tenant, 'ocorrencias', parts[3]);
        if (!occurrence || occurrence.entregaId !== id) throw new ApiError(404, 'Ocorrência não encontrada.');
        if (request.method === 'GET') return user.role === 'CLIENTE' ? visibleOccurrence(occurrence, user) : {...summary(occurrence), foto: occurrence.foto};
        if (request.method === 'PATCH') {
          if (user.role !== 'ADMIN') throw new ApiError(403, 'Somente o gestor pode resolver ocorrências.');
          const resolucao = requiredText(body, 'resolucao', 1500);
          const mensagemCliente = requiredText(body, 'mensagemCliente', 300);
          const saved = await store.mutate(user.tenant, 'ocorrencias', parts[3], previous => {
            if (previous.status === 'RESOLVIDA') throw new ApiError(409, 'Ocorrência já resolvida.');
            return {status: 'RESOLVIDA', resolucao, mensagemCliente, resolvidaEm: now(), resolvidaPor: author};
          });
          return summary(saved);
        }
      }
    }
    if (feature === 'rastreamento' && parts.length === 3) {
      if (!['ADMIN', 'CLIENTE'].includes(user.role)) throw new ApiError(403, 'Somente o gestor ou cliente pode compartilhar o acompanhamento.');
      if (request.method === 'DELETE') {
        await store.mutate(user.tenant, 'entregas', id, previous => {permission(user, previous); return {rastreamentoHash: null};});
        return {status: 204, body: null};
      }
      if (request.method === 'POST') {
        const token = randomBytes(32).toString('base64url'), hash = tokenHash(token);
        const expiraEm = new Date(Date.parse(now()) + 7 * 86400000).toISOString();
        await store.mutate(user.tenant, '_trackingLinks', hash, () => ({entregaId: id, expiraEm}), {create: true});
        await store.mutate(user.tenant, 'entregas', id, previous => {permission(user, previous); return {rastreamentoHash: hash};});
        return {caminho: `/acompanhar/${token}`, expiraEm};
      }
    }
    throw new ApiError(405, 'Operação não disponível.');
  }
  return {handle, publicTracking};
}
