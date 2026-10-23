# Publicação SmartLog no Netlify

Atualizado em 23/10/2026. Publicação concluída: Netlify público, regras Firestore publicadas, três índices READY e teste online com Firebase/Gemini aprovado.

## Endereços

- Site e painel: https://smartlog-next-2026.netlify.app
- API: https://smartlog-next-2026.netlify.app/api/v1
- APK ARM64: https://smartlog-next-2026.netlify.app/downloads/smartlog-arm64.apk
- Projeto: https://app.netlify.com/projects/smartlog-next-2026
- Site ID: 2e4e6ab4-bb98-4100-80db-4a074a4ecb66

O serviço funciona com o desktop desligado. O notebook do colega precisa apenas de navegador e internet. O tablet precisa do APK e internet.

## Arquitetura publicada

O Angular é servido pelo CDN do Netlify. As rotas /api/v1 são atendidas pela Function Node.js em netlify/functions/smartlog.mjs, com o mesmo contrato usado no Flutter e no painel. Firebase Authentication e Firestore continuam no projeto smartlog-b9b37. Gemini é chamado somente pelo servidor.

O código Spring Boot permanece no repositório como implementação alternativa. Ele não está executando no Netlify. A Function foi adaptada para a hospedagem disponível e validada com os 17 cenários de integração do Spring e um cenário adicional de edição de perfil do motorista.

As alterações do Lucas são a base do app. As coleções raiz e os vínculos existentes foram preservados. Nenhuma migração automática de dados comerciais foi executada.

## Custo e limites

A equipe foi confirmada no plano Free, sem cartão e com recarga automática desativada. O plano tem 300 créditos mensais e limite rígido: o serviço pode ser pausado ao esgotar a franquia. Deploys de produção, requisições, tráfego e execução consomem créditos. O APK tem aproximadamente 116 MB; distribua-o apenas aos aparelhos necessários à demonstração.

Consulte as regras vigentes:
https://docs.netlify.com/manage/accounts-and-billing/billing/billing-for-credit-based-plans/credit-based-pricing-plans/

O projeto Google não precisa de Cloud Run ou Secret Manager para esta implantação. Firebase e Gemini têm suas próprias cotas. A aplicação informa indisponibilidade/limite do provedor; não habilita cobrança nem usa um provedor pago automaticamente.

## Credenciais

No Netlify estão configuradas FIREBASE_PROJECT_ID, FIREBASE_SERVICE_ACCOUNT_JSON, GEMINI_API_KEY e GEMINI_MODEL. A service account é utilizada somente pela Function. A varredura do bundle web e do ZIP da Function confirmou ausência das credenciais. O Firebase API key presente no cliente é a configuração pública do Firebase Auth, não a chave secreta do Gemini.

Para reaplicar as variáveis no projeto já criado:

    node SmartLogAPI/netlify/configure-site.mjs

O script lê os arquivos locais autorizados, usa um arquivo temporário com permissão restrita e o apaga ao terminar. Não imprime os valores. O login do Netlify deve estar ativo no WSL.

## Reproduzir o deploy no WSL

O workspace contém três repositórios irmãos:

    smartlog/
      Smartlog/
      SmartLogAPI/
      SmartLogWeb/
      netlify.toml

O template de netlify.toml está versionado em netlify/netlify.workspace.toml no repositório da API. Copie-o para a raiz do workspace se necessário. A publicação atual foi feita pelo CLI, sem deploy automático ligado ao Git.

Na raiz do workspace:

    npx --yes netlify-cli@27.14.2 login
    npx --yes netlify-cli@27.14.2 link --id 2e4e6ab4-bb98-4100-80db-4a074a4ecb66
    npx --yes netlify-cli@27.14.2 build

Para incluir o APK atualizado, depois do build web:

    cd Smartlog
    flutter build apk --release --target-platform android-arm64 --dart-define=API_BASE_URL=https://smartlog-next-2026.netlify.app/api/v1
    cd ..
    mkdir -p SmartLogWeb/dist/SmartLogWeb/browser/downloads
    cp Smartlog/build/app/outputs/flutter-apk/app-release.apk SmartLogWeb/dist/SmartLogWeb/browser/downloads/smartlog-arm64.apk

Publicar:

    npx --yes netlify-cli@27.14.2 deploy --prod --no-build --site 2e4e6ab4-bb98-4100-80db-4a074a4ecb66

O build Angular limpa a pasta dist. Recoloque o APK em downloads depois de cada build se quiser manter esse link disponível.

Regras e índices são publicados separadamente por uma conta administradora Firebase:

    npx --yes firebase-tools login --no-localhost
    npx --yes firebase-tools deploy --only firestore:rules,firestore:indexes --project smartlog-b9b37

O login Netlify não autentica o Firebase. A conta de execução da API tem acesso aos dados e ao Auth, mas não permissão para publicar regras. Não amplie essa conta para contornar o login administrativo.

## Validação

- 28 testes do adaptador: npm --prefix SmartLogAPI/netlify test
- 18 cenários nos emuladores, incluindo Gemini real: iniciar os emuladores, executar node SmartLogAPI/netlify/local-server.mjs --emulator e rodar SMARTLOG_TEST_API=http://127.0.0.1:8081/api/v1 SMARTLOG_TEST_AI_LIVE=1 node scripts/integration-smoke.mjs
- Teste online isolado: node SmartLogAPI/netlify/production-smoke.mjs --live
- Acrescente --rules ao teste online para conferir as consultas do app, leitura própria, GPS, edição de perfil e bloqueios entre empresas, de elevação de papel e de reatribuição de rota pelas regras publicadas.

O teste online cria contas aleatórias e documentos fictícios em uma empresa exclusiva, usa apenas esses dados e remove todos no bloco finally. Não gera contas permanentes de apresentação, não envia e-mails e não imprime senhas, tokens ou chaves.

## Ensaio no tablet e notebook

1. Abra o site no notebook e entre com a conta administradora da empresa da demonstração.
2. Cadastre ou selecione motorista, cliente e rota. Novas contas acessam pelo fluxo de recuperação de senha do Firebase.
3. Crie uma entrega nessa rota pelo painel.
4. Instale o APK ARM64 e entre com a conta do motorista atribuído.
5. Autorize o GPS; inicie a rota e confira a posição e o horário enviados.
6. Altere o status da entrega no app e aguarde confirmação; confira a atualização no painel.
7. Pergunte ao assistente pelo código da mesma entrega e compare o status.

A validação física no tablet e a conferência visual do dashboard ainda dependem do ensaio. Os testes HTTP e de componentes não substituem essas verificações.
