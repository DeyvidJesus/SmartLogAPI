export class ApiError extends Error {
  constructor(status, message, code = 'REQUEST_FAILED') {
    super(message);
    this.status = status;
    this.code = code;
  }
}
export const key = value => String(value ?? '').normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '').trim().toUpperCase().replaceAll(' ', '_');
export const roleOf = data => {
  const role = key(data.tipoUsuario ?? data.tipo);
  return role === 'ADMINISTRADOR' ? 'ADMIN' : role;
};
export const active = data => data.ativo !== false && key(data.status) !== 'INATIVO';
export function instant(...values) {
  for (const value of values) {
    const date = value?.toDate ? value.toDate() : value ? new Date(value) : null;
    if (date && Number.isFinite(date.getTime())) return date.toISOString();
  }
  return new Date(0).toISOString();
}
export function deliveryStatus(value) {
  const result = {
    '': 'PENDENTE', PENDENTE: 'PENDENTE', A_CAMINHO: 'EM_ROTA',
    EM_ROTA: 'EM_ROTA', EM_TRANSITO: 'EM_ROTA', ENTREGUE: 'ENTREGUE',
    CANCELADA: 'CANCELADA', CANCELADO: 'CANCELADA', ATRASADA: 'ATRASADA',
  }[key(value)];
  if (!result) throw new ApiError(503, 'Status de entrega não reconhecido no cadastro.', 'PERSISTENCE_FAILURE');
  return result;
}
const appStatuses = Object.freeze({
  PENDENTE: 'Pendente', EM_ROTA: 'Em Rota', ENTREGUE: 'Entregue',
  CANCELADA: 'Cancelada', ATRASADA: 'Atrasada',
});
export const statusToApp = value => Object.hasOwn(appStatuses, value) ? appStatuses[value] : undefined;
export function routeStatus(value) {
  return {
    '': 'PLANEJADA', PENDENTE: 'PLANEJADA', PLANEJADA: 'PLANEJADA',
    EM_ROTA: 'EM_ANDAMENTO', EM_ANDAMENTO: 'EM_ANDAMENTO', EM_TRANSITO: 'EM_ANDAMENTO',
    A_CAMINHO: 'EM_ANDAMENTO', CONCLUIDA: 'CONCLUIDA', FINALIZADA: 'CONCLUIDA',
    ENTREGUE: 'CONCLUIDA', PAUSADA: 'PAUSADA', CANCELADA: 'CANCELADA', CANCELADO: 'CANCELADA',
  }[key(value)] ?? String(value);
}
export const routeToApp = value => ({
  PLANEJADA: 'Pendente', EM_ANDAMENTO: 'Em Rota', CONCLUIDA: 'Concluida',
  PAUSADA: 'Pausada', CANCELADA: 'Cancelada',
})[routeStatus(value)] ?? String(value);
export const codeOf = document => [document.codigo, document.id].find(value =>
  typeof value === 'string' && value.startsWith('ENT-')) ??
  'ENT-' + document.docId.slice(0, 4).toUpperCase();
const optionalNumber = value => typeof value === 'number' && Number.isFinite(value) ? value : null;
export function deliveryDto(data) {
  return {
    id: data.docId, codigo: codeOf(data), empresaId: data.empresaId,
    clienteId: data.clienteId ?? '', rotaId: data.rotaId ?? '',
    motoristaId: data.motoristaId ?? '', status: deliveryStatus(data.status),
    origem: data.origem ?? '', destino: data.endereco || data.destino || '',
    criadaEm: instant(data.criadaEm, data.criadoEm, data.dataCriacao),
    atualizadaEm: instant(data.atualizadaEm, data.entregueEm, data.criadoEm, data.criadaEm),
    chaveNfe: data.chaveNfe ?? null, valorNfe: optionalNumber(data.valorNfe),
    pesoKg: optionalNumber(data.pesoKg), volumeM3: optionalNumber(data.volumeM3),
    volumes: optionalNumber(data.volumes), janelaEntrega: data.janelaEntrega ?? null,
    recebedor: data.recebedor ?? null, observacaoEntrega: data.observacaoEntrega ?? null,
    observacoes: data.observacoes ?? null,
    entregueEm: data.entregueEm ? instant(data.entregueEm) : null,
    confirmadoPorNome: data.confirmadoPorNome ?? null,
    historicoStatus: Array.isArray(data.historicoStatus) ? data.historicoStatus : [],
    comprovanteDisponivel: !!data.comprovante,
    rastreamentoAtivo: !!data.rastreamentoHash,
  };
}
export function resourceDto(resource, data) {
  const common = {
    id: data.docId, empresaId: data.empresaId,
    criadaEm: instant(data.criadaEm, data.criadoEm, data.dataCadastro),
    atualizadaEm: instant(data.atualizadaEm, data.criadoEm, data.dataCadastro),
  };
  if (resource === 'rotas') {
    const position = data.localizacaoAtual;
    const valid = position && Number.isFinite(position.latitude) && Number.isFinite(position.longitude) &&
      Math.abs(position.latitude) <= 90 && Math.abs(position.longitude) <= 180;
    return {...common, nome: data.nome ?? '', motoristaId: data.motoristaId ?? '', observacoes: data.observacoes ?? null,
      distanciaRestanteMetros: optionalNumber(data.distanciaRestanteMetros),
      distanciaCalculadaEm: data.distanciaCalculadaEm ? instant(data.distanciaCalculadaEm) : null,
      distanciaIncluiGps: data.distanciaIncluiGps === true,
      status: routeStatus(data.status), localizacaoAtual: valid ? {
        latitude: position.latitude, longitude: position.longitude,
        atualizadoEm: position.atualizadoEm ? instant(position.atualizadoEm) : null,
      } : null};
  }
  if (resource === 'veiculos') return {...common, placa: data.placa, modelo: data.modelo,
    tipo: data.tipo ?? null, capacidadeKg: optionalNumber(data.capacidadeKg),
    capacidadeM3: optionalNumber(data.capacidadeM3), ano: optionalNumber(data.ano), ativo: active(data)};
  const person = {...common, nome: data.nome, email: data.email, ativo: active(data)};
  return resource === 'motoristas' ? {...person, uid: data.uid || data.docId} :
    {...person, telefone: data.telefone ?? null};
}
export function requiredText(body, field, max = 500) {
  if (typeof body[field] !== 'string' || !body[field].trim() || body[field].length > max)
    throw new ApiError(400, 'Campo obrigatório ou inválido: ' + field, 'VALIDATION_FAILURE');
  return body[field].trim();
}
export function optionalText(body, field, max = 500) {
  if (body[field] == null) return null;
  if (typeof body[field] !== 'string' || body[field].length > max)
    throw new ApiError(400, 'Campo inválido: ' + field, 'VALIDATION_FAILURE');
  return body[field].trim();
}
export function emailOf(body) {
  const email = requiredText(body, 'email', 254).toLowerCase();
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))
    throw new ApiError(400, 'Informe um e-mail válido.', 'VALIDATION_FAILURE');
  return email;
}
export function nonnegative(body, field, integer = false) {
  if (body[field] == null) return null;
  const value = body[field];
  if (typeof value !== 'number' || !Number.isFinite(value) || value < 0 ||
      (integer && !Number.isSafeInteger(value)))
    throw new ApiError(400, 'Valor inválido: ' + field, 'VALIDATION_FAILURE');
  return value;
}
export function booleanValue(body, field, defaultValue = true) {
  if (body[field] == null) return defaultValue;
  if (typeof body[field] !== 'boolean') throw new ApiError(400, 'Campo inválido: ' + field);
  return body[field];
}
