# SmartLog integrado — configuração e validação

Atualização: 23/10/2026. A base funcional é o Flutter atualizado do Lucas. As telas, o tema e os fluxos de GPS/rotas foram preservados; o backend passou a compreender o schema desse aplicativo.

## O que foi conectado

- **Fonte compartilhada:** Firestore nas coleções raiz `usuarios`, `entregas`, `rotas` e `veiculos`, com `empresaId`. A API não copia nem move documentos das subcoleções antigas automaticamente.
- **Identidade:** app e painel usam Firebase Authentication. A API valida o ID Token e lê o papel/empresa no cadastro `usuarios/{uid}`. Não transforma token inválido em administrador nem exige custom claims adicionais do app existente.
- **Entregas:** o backend normaliza os status e timestamps do app. O ID do documento permanece o identificador técnico; o código ENT permanece o código exibido ao usuário.
- **Confirmação:** a mudança de status do motorista passa pela API. Sem confirmação do servidor, o aplicativo informa falha ou incerteza; não simula sucesso em cache.
- **Visibilidade:** o Flutter acompanha as entregas autorizadas por snapshots. O Angular consulta a operação a cada 5 segundos. O mapa mostra o último GPS enviado em `rotas.localizacaoAtual`, com o horário informado.
- **Rotas:** a API traduz os status Pendente/Em Rota do Flutter para PLANEJADA/EM_ANDAMENTO do painel, preservando os filtros e o início do GPS.
- **Atribuição e carga:** entregas criadas pelo painel herdam o motorista da rota. Peso, volumes, valor e demais campos de carga aceitos pelo formulário são persistidos e preservados nas alterações de status.
- **Cadastro:** novos motoristas e clientes criados pela API podem receber uma identidade Firebase real. O primeiro acesso usa a recuperação de senha do Firebase; nenhuma senha compartilhada é devolvida pelo backend. Cadastros comerciais legados continuam sendo reconhecidos por seu vínculo/e-mail.
- **IA:** Flutter e Angular consultam `POST /api/v1/chat`. O contexto vem da operação autorizada e o provedor é Gemini. A Function e o Worker alternativos também foram adaptados ao mesmo provedor, mantendo seus contextos e verificação de identidade.
- **Layout:** permanecem a identidade azul/laranja, os cartões, a navegação e a página inicial. Login e assistente web usam os mesmos padrões existentes.

Os KPIs artificiais de OTIF, SLA, tonelagem e ocupação foram substituídos por contadores verificáveis. Alertas visíveis são derivados de entregas sem motorista; não há previsão de tráfego embutida nessa regra.

## Gemini: conta, chave e custo

Provedor selecionado: Gemini Developer API, modelo padrão `gemini-3.1-flash-lite`, com free tier conforme [preços oficiais](https://ai.google.dev/gemini-api/docs/pricing). No Netlify, a integração usa a API nativa generateContent do Gemini; o Spring mantém o endpoint compatível com Chat Completions.

A chave já foi criada pelo usuário e salva em `SmartLogAPI/.env`. O teste direto retornou HTTP 200. O teste autenticado completo também passou: API → Gemini → resposta com o código e o status confirmado da entrega fictícia ENT-NEXT-mvkh1bw5. Ao todo, passaram 132 testes automatizados (incluindo 28 do adaptador Netlify) e 18 cenários de integração; consulte [as evidências](VALIDACAO_INTEGRADA.md). A credencial não foi incluída no aplicativo, no Angular ou nos logs.

Configuração local:

```dotenv
GEMINI_API_KEY=sua-chave
GEMINI_MODEL=gemini-3.1-flash-lite
```

Use um projeto com free tier ativo e acompanhe os limites no AI Studio. A gratuidade do provedor não significa hospedagem ilimitada gratuita: Netlify, Firebase e rede seguem os limites dos planos gratuitos escolhidos. O serviço informa 429 quando limitado e 503 quando falta configuração; não troca automaticamente para um provedor pago.

No free tier, a política do provedor pode permitir uso dos dados para melhorar seus produtos. Para o evento, use a empresa e as entregas de demonstração.

## Executar a integração isolada no WSL

Os comandos abaixo são executados em `/home/deyvid/documents/smartlog`. Nenhum dado é criado no projeto Firebase publicado.

Terminal 1:

```bash
bash scripts/run-integration-local.sh
```

Isso inicia Auth em `127.0.0.1:9099` e Firestore em `127.0.0.1:8085`, no projeto fictício `demo-smartlog-integration`.

Terminal 2, depois dos emuladores prontos:

```bash
node scripts/run-api-emulator.mjs
```

Esse comando lê somente `GEMINI_API_KEY` e `GEMINI_MODEL` do `.env`, sem imprimir os valores, e inicia a API em `8080` com o perfil `emulator`. Esse perfil exige os dois emuladores; não é configuração de produção.

Terminal 3:

```bash
cd SmartLogWeb
npm start -- --configuration emulator
```

O login web usa somente o Auth emulado nessa configuração. O build de produção continua usando o Firebase real.

Teste da integração em outro terminal:

```bash
SMARTLOG_TEST_AI_LIVE=1 node scripts/integration-smoke.mjs
```

O script cria contas/entregas fictícias, verifica status compartilhado, escopos de acesso, GPS, campos preservados e uma resposta real do Gemini. A senha fixa existente nesse script serve apenas às contas descartáveis do emulador. Tokens não são impressos.

Para testar um servidor deliberadamente sem chave de IA, execute o mesmo smoke sem `SMARTLOG_TEST_AI_LIVE`; esse modo espera 503 no chat. Não execute os scripts contra produção.

## Executar com o Firebase real

1. Configure Application Default Credentials ou `GOOGLE_APPLICATION_CREDENTIALS` apontando para uma service account fora dos repositórios.
2. Defina `FIREBASE_ENABLED=true`, `FIREBASE_PROJECT_ID=smartlog-b9b37`, `GEMINI_API_KEY` e as origens CORS necessárias.
3. Não ative `dev` ou `emulator` nesse ambiente e não use tokens de demonstração como identidade do motorista.
4. A identidade de execução precisa acessar o Firestore e gerenciar as contas Firebase para o provisionamento administrativo. Não use chave administrativa no tablet ou no navegador.
5. Inicie a API com `./mvnw spring-boot:run`; para o painel local, use `npm start` e faça login com uma conta administradora existente. Remova o token dev local quando for usar a autenticação real.

O Spring não lê `.env` automaticamente. O runner dos emuladores faz essa leitura restrita; em produção, injete as variáveis pelo ambiente/Secret Manager.

## Publicação Netlify

Site/painel: https://smartlog-next-2026.netlify.app

API: https://smartlog-next-2026.netlify.app/api/v1

Angular e a Function Node foram publicados no Netlify Free. O Spring permanece como alternativa e não é o processo publicado. Firebase Auth, Firestore e Gemini reais passaram no teste online isolado, com remoção das contas/documentos temporários. As regras foram publicadas e verificadas online. Os três índices foram confirmados em estado READY. A publicação está concluída; o ensaio físico no tablet e a revisão visual permanecem pendentes.

O serviço funciona sem o desktop ligado. A chave Gemini e a service account estão no ambiente da Function, fora dos artefatos públicos. O guia [NETLIFY.md](NETLIFY.md) contém os comandos de publicação, limites e ensaio. Os scripts antigos de Google/Cloud Run são alternativas e não fazem parte desta implantação.

## APK para o tablet

Build Android ARM64 final, com 116.3 MB, preparado em:

`Smartlog/build/app/outputs/flutter-apk/app-release.apk`

Download: https://smartlog-next-2026.netlify.app/downloads/smartlog-arm64.apk

O pacote continua `com.example.smartlife`, conforme o registro Firebase original. É um artefato de demonstração assinado com a configuração de debug já existente no projeto; não é um release para a Play Store.

- Confirme que o tablet suporta ARM64.
- Use o APK atualizado para https://smartlog-next-2026.netlify.app/api/v1.
- Instale o APK e entre com a conta do motorista atribuída à rota da demonstração.
- Se houver incompatibilidade de assinatura com a instalação anterior, a reinstalação pode exigir remover a versão antiga; preserve primeiro qualquer dado local necessário.
- Autorize o GPS e teste uma primeira posição real antes do ensaio. Não foi validado o uso em segundo plano/tela apagada.
- Para outro servidor, gere o app com `--dart-define=API_BASE_URL=https://seu-host/api/v1`. O chat segue esse endereço; `CHAT_BASE_URL` permite selecionar a Function alternativa explicitamente.

Compatibilidade ajustada para o Flutter instalado: Gradle 8.14.3 e AGP 8.11.1. A compilação usa até 3 GB de heap Gradle, dois workers e Kotlin no mesmo processo para caber melhor no WSL de 8 GB. Permanecem avisos de versões que serão descontinuadas pelo Flutter e de dependências Android.

## Critério de ensaio do evento

1. Abrir a mesma entrega no painel e no tablet, conferindo o código e a atribuição.
2. No tablet, mudar o status e aguardar confirmação.
3. No painel, observar o novo status em até um ciclo normal de consulta, sem esconder recarga manual se for usada.
4. Perguntar ao assistente sobre o código daquela entrega e comparar a resposta com o registro.
5. Testar uma rodada sem conexão: a tela não deve anunciar uma gravação confirmada.
6. Repetir com outra entrega de demonstração. Não resetar um status às escondidas durante a apresentação.

A captura de foto/assinatura POD, o tráfego externo e os serviços Oracle guardados nos stashes não foram anunciados como novas funcionalidades desta integração. O trabalho local anterior continua preservado nos stashes.

## Limites de verificação

O teste físico no tablet depende da instalação e das permissões no aparelho. A conferência visual do dashboard no navegador não foi realizada nesta etapa porque a permissão da ferramenta de navegador foi recusada. Os builds e testes de componentes são evidências complementares, não substitutos dessa conferência visual.
