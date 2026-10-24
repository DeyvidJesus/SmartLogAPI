# Comprovante, ocorrências e acompanhamento

Os novos endpoints são implementados no adaptador Node/Netlify, a API usada
em produção. O backend Spring Boot alternativo não recebeu esses endpoints.
Para desenvolvimento integrado deste recurso, use o adaptador Node com Firebase
emulado ou o servidor sintético de testes descrito abaixo.

## Contratos

Todos os endpoints abaixo usam o token Firebase do usuário, exceto o link de acompanhamento.
O servidor resolve empresa e perfil pelo cadastro, nunca por campos enviados pelo cliente.

| Endpoint | Quem usa | Comportamento |
| --- | --- | --- |
| POST /api/v1/entregas/:id/comprovante | Motorista atribuído ou gestor | Confirma a entrega atomicamente com recebedor, 1–2 fotos, assinatura PNG, coordenadas, precisão e volumes |
| GET /api/v1/entregas/:id/comprovante | Gestor, motorista atribuído ou cliente da entrega | Retorna o comprovante completo |
| POST /api/v1/entregas/:id/ocorrencias | Motorista atribuído ou gestor | Registra motivo, descrição, foto opcional e mensagem ao cliente |
| GET /api/v1/entregas/:id/ocorrencias | Usuário autorizado na entrega | Lista ocorrências; clientes recebem somente informações destinadas ao cliente |
| GET /api/v1/entregas/:id/ocorrencias/:oid | Usuário autorizado na entrega | Detalhe; foto e notas internas ficam restritas à operação |
| PATCH /api/v1/entregas/:id/ocorrencias/:oid | Gestor | Resolve a ocorrência com ação tomada e mensagem pública obrigatórias |
| GET /api/v1/ocorrencias | Gestor | Alimenta a central de ocorrências da empresa |
| POST /api/v1/entregas/:id/rastreamento | Gestor ou cliente da entrega | Emite link secreto válido por sete dias e invalida o anterior |
| DELETE /api/v1/entregas/:id/rastreamento | Gestor ou cliente da entrega | Revoga links existentes |
| GET /api/v1/rastreamento/:token | Portador do link | Status, janela cadastrada, eventos, mensagens públicas, fotos do recebimento e recebedor |

O link público não expõe endereço, posição GPS, assinatura, nomes de funcionários,
IDs internos nem notas internas de ocorrências. Não há promessa de horário de chegada.
O comprovante completo permanece disponível para usuários autenticados autorizados.

## Persistência e limites

- Comprovante gravado em `entregas.comprovante`, junto ao status e histórico, numa transação Firestore.
- Ocorrências em `ocorrencias`, sempre com `empresaId` e `entregaId`.
- Links em `_trackingLinks`, identificados pelo SHA-256 de um token aleatório de 256 bits. A entrega guarda somente o hash ativo.
- Somente o servidor acessa as novas coleções. As regras existentes já negam acesso direto a elas.
- Não há migração, recriação de contas ou alteração de dados anteriores. Comprovantes legados continuam exibidos como legados; não se inventam imagens ou assinaturas.
- Fotos JPEG/PNG limitadas a 150 KB cada; assinatura a 50 KB; corpo JSON a 600 KB. Imagens ficam no documento protegido pelo servidor, dentro do limite de 1 MiB do Firestore. A listagem de entregas só retorna um indicador de disponibilidade.
- Envio do comprovante e abertura de ocorrência têm `requestId` estável para evitar duplicação em tentativas repetidas. O horário é definido pelo servidor.
- O comprovante final não pode ser sobrescrito nem a entrega reaberta pelo endpoint de status depois dele.
- Entrega parcial registra a quantidade recebida e mantém o pedido aberto. A confirmação final exige o total cadastrado; nova tentativa deve registrar todos os volumes recebidos acumulados.
- A confirmação manual antiga por status continua disponível para compatibilidade; somente o novo endpoint produz um comprovante completo.

## Ensaio integrado

1. No app do motorista: detalhes da entrega → Registrar ocorrência. Para entrega parcial, informe os volumes abaixo do total cadastrado.
2. No Web: Ocorrências → selecione a entrega → ação tomada e mensagem ao cliente → Resolver.
3. No app do motorista: Entregue → recebedor, volumes, foto, localização e assinatura → Confirmar entrega.
4. No Web: detalhes da entrega mostram o comprovante, fotos, assinatura, GPS e horário. A consulta se atualiza a cada 15 segundos.
5. No Web ou app do cliente: gerar link. Abra-o sem login; consulte os avisos e o recebimento. O portal atualiza a cada 20 segundos.
6. Revogue o link no painel e confirme que ele deixa de revelar os dados.

## Testes locais sem produção

Em `SmartLogAPI/netlify`, execute `npm test` para os contratos e controles de acesso.
Após compilar o SmartLogWeb, execute `node test/support/experience-server.mjs` no mesmo diretório.
Esse servidor ouve somente em 127.0.0.1:4300 e usa dados sintéticos em memória; não toca no Firebase.
Em Smartlog, execute:

    flutter test test/delivery_http_flow_test.dart --dart-define=SMARTLOG_INTEGRATION=true --dart-define=API_BASE_URL=http://127.0.0.1:4300/api/v1

O teste usa o cliente Flutter real e o handler HTTP de produção para conferir o registro do motorista,
a leitura administrativa e a consulta pública. Não substitui o ensaio físico de câmera/GPS em Android
nem a verificação do Firebase e Netlify de produção após o merge.
