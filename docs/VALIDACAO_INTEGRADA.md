# SmartLog — evidências de integração

Data: 23/10/2026. Ambiente: Ubuntu no WSL. Nenhuma credencial está neste relatório.

## Base utilizada

| Repositório | Commit de base |
| --- | --- |
| App Flutter | 1daff83 — merge da main original do Lucas |
| API | 7baa137 |
| Dashboard | ddacfe7 |

O trabalho anterior está preservado no stash pre-sync-20261022 em cada repositório. As alterações desta integração permanecem locais para revisão.

## Resultado dos testes

| Camada | Resultado |
| --- | --- |
| Spring Boot | 34 testes, sem falhas ou erros |
| Angular | 16 testes e build de produção aprovados |
| Flutter | 8 testes aprovados |
| Adaptador Gemini da Function | 6 testes aprovados |
| Contexto e relevância da IA | 36 testes aprovados |
| Endpoints Worker | 4 testes aprovados |
| Adaptador Netlify | 28 testes aprovados |
| Total automatizado | 132 testes aprovados |
| Integração Auth/Firestore/API/Gemini | 18 cenários aprovados |

O teste integrado usa Auth e Firestore emulados no projeto demo-smartlog-integration; a chamada ao Gemini é real e usa somente dados fictícios. Não representa um teste físico no tablet nem uma validação de dados de produção.

Os cenários cobrem autenticação sem custom claims, schema do Flutter, confirmação persistida de status, preservação de GPS/recebedor, bloqueio de outro motorista, empresa e cliente, token inválido, regras diretas do Firestore, GPS compartilhado, consulta Gemini real, cliente legado por e-mail, criação de entrega com motorista/carga, bloqueio de invasão de empresa, reatribuição de rota proibida ao motorista criação real de conta no Auth emulado e compatibilidade dos status de rota entre painel e motorista.

A resposta real do Gemini citou o código ENT-NEXT-mvkh1bw5 e o status ENTREGUE, confirmado previamente no Firestore pela API.

## Builds e avisos

- APK ARM64: Smartlog/build/app/outputs/flutter-apk/app-release.apk.
- Tamanho: 116275251 bytes.
- SHA-256: 8b5438069a12a76e1c39340c9e176bd3ffa4d90605d43093b8fa7db199b879c1.
- O APK preserva o applicationId com.example.smartlife e a assinatura de demonstração existente.
- Flutter analyze: nenhum erro; 36 ocorrências, sendo um warning de código morto e 35 informações sobre APIs obsoletas ou recomendações.
- Android: build concluído, com avisos de suporte futuro de Gradle/AGP/Kotlin.
- Angular: aviso CommonJS do Leaflet.
- Worker: o runtime de teste utiliza a data de compatibilidade 2026-03-10, anterior à solicitada 2026-08-08.
- git diff --check passou nos três repositórios. A sintaxe de deploy.sh foi validada.

Os processos de build foram encerrados após gerar o APK para liberar memória no WSL. Os testes não exigiram reiniciar o WSL ou interromper serviços do usuário.

## Publicação Netlify — 23/10/2026

- Site: https://smartlog-next-2026.netlify.app
- API Node em Netlify Functions, com Angular no CDN e Firebase/Gemini reais.
- Plano Free, recarga automática desativada, sem cartão. Não depende do desktop.
- 28 testes do adaptador e 18 cenários em emuladores aprovados; 8 testes Flutter reexecutados após alterar a URL.
- Build de produção Angular e bundle da Function aprovados. APK ARM64 recompilado para a URL Netlify.
- Varredura do Angular, ZIP da Function e APK não encontrou a chave Gemini nem o material privado da service account.
- APK publicado e baixado pela URL pública; SHA-256 idêntico ao artefato local. Deploy atual: 6adbae8e10392dbed31ebb7b.
- Teste ONLINE aprovado: API pública HTTPS, login real, perfil do servidor, isolamento entre empresas, status persistido e visível ao administrador, campos legados preservados, cliente legado, bloqueio de escrita por cliente, token inválido, GPS armazenado e resposta Gemini sobre a entrega gravada.
- Consultas diretas do app no Firestore verificadas online: entregas por empresa/motorista, entregas por empresa/e-mail do cliente e rotas por empresa/motorista.
- Todas as contas e documentos fictícios desse teste online foram removidos.
- Regras Firestore publicadas em 23/10/2026. Teste online confirmou leitura própria, escrita de GPS e edição de perfil; acesso entre empresas, troca de e-mail, elevação de papel e reatribuição de rota foram bloqueados. Os três índices foram confirmados em estado READY: entregas/empresaId+motoristaId, entregas/empresaId+emailCliente e rotas/empresaId+motoristaId.

O Spring não está hospedado no Netlify; o adaptador Node preserva seu contrato. Os testes online comprovam API → Firebase → Gemini. O GPS usado no smoke é fictício; a captura de posição real no tablet continua pendente.

Detalhes e reprodução: [NETLIFY.md](NETLIFY.md).

## Verificações presenciais pendentes

- Instalação no tablet, suporte ARM64, permissões e primeira posição GPS real.
- Validação visual do dashboard no navegador. A permissão da ferramenta de navegador foi recusada nesta etapa; não foi feita conferência visual automatizada.
- Ensaio com a mesma entrega no tablet, no painel e no assistente após a publicação.

O guia de configuração e de ensaio está em SmartLogAPI/docs/INTEGRACAO.md.

## Atualização de mapas e ícones

APK build 2: trajeto viário OSRM no motorista e alternativa sem token Mapbox na tela administrativa. SVGs locais no painel e no marcador Leaflet. Três testes específicos aprovados, consulta real retornou 71 pontos de geometria; builds Angular e Android aprovados. Consulte CORRECAO_MAPAS_ICONES.md. Ensaio visual e físico ainda pendente.
