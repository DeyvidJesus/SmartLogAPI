# SmartLog API

[![CI](https://github.com/DeyvidJesus/SmartLogAPI/actions/workflows/ci.yml/badge.svg)](https://github.com/DeyvidJesus/SmartLogAPI/actions/workflows/ci.yml)

API REST do SmartLog, sistema de gestão logística (entregas, rotas, clientes, motoristas e
veículos) com isolamento multi-tenant por `empresaId`. Construída com Java 21, Spring Boot
3.5, Spring Security, Firebase Admin SDK (Authentication + Cloud Firestore) e OpenAPI.

## O ecossistema SmartLog

| Repositório | Papel | Situação |
| --- | --- | --- |
| [SmartLogAPI](https://github.com/DeyvidJesus/SmartLogAPI) (este) | Backend REST em Spring Boot | Lê e grava em `empresas/{empresaId}/{entregas,rotas,clientes,motoristas,veiculos}` |
| [SmartLogWeb](https://github.com/DeyvidJesus/SmartLogWeb) | Painel web administrativo em Angular | Consome esta API via proxy (`/api/v1` → `localhost:8080`) |
| [Smartlog](https://github.com/DeyvidJesus/Smartlog) | App mobile em Flutter (projeto em equipe) | Usa o mesmo projeto Firebase, mas acessa o Firestore diretamente |

Sendo transparente sobre o estado atual: o app Flutter compartilha o projeto Firebase, porém
grava num modelo de dados diferente — coleções de nível raiz (`entregas`, `rotas`,
`clientes`, `usuarios`) em vez das subcoleções por empresa que esta API usa
(`empresas/{id}/entregas`). Por isso, hoje os dados criados pelo app **não aparecem** na API
nem no painel web, e vice-versa. Unificar o app Flutter para consumir esta API é o próximo
passo natural da arquitetura.

## Requisitos

- JDK 21+
- Não é preciso instalar Maven: o projeto inclui o Maven Wrapper (`./mvnw` / `mvnw.cmd`).

## Executar localmente (perfil `dev`)

A forma mais simples de rodar localmente é sem Firebase, com repositórios em memória
(já populados com dados de exemplo da `empresa-001`) e o atalho de autenticação de
desenvolvimento:

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
# Windows (PowerShell): $env:SPRING_PROFILES_ACTIVE="dev"; .\mvnw.cmd spring-boot:run
```

No perfil `dev`, a API aceita **somente** o token configurado em
`smartlog.security.dev-auth.token` (padrão `smartlog-dev-token`, definido apenas em
`application-dev.yml`), autenticando como `ADMIN` da `empresa-001`. Um aviso `WARN` é
registrado na inicialização sempre que esse modo está ativo.

```bash
curl http://localhost:8080/api/v1
curl -H "Authorization: Bearer smartlog-dev-token" http://localhost:8080/api/v1/entregas
```

O [SmartLogWeb](https://github.com/DeyvidJesus/SmartLogWeb) usa o mesmo valor em
`src/environments/environment.development.ts` (`devAuthToken`). Se trocar o token com
`SMARTLOG_DEV_AUTH_TOKEN`, atualize também o arquivo do front-end.

### Falha fechada

Sem o perfil `dev` (ou `SMARTLOG_DEV_AUTH_ENABLED=true`) e sem Firebase habilitado, **nenhum**
Bearer token é aceito: toda requisição a endpoint protegido recebe `401`. Habilitar o
dev-auth sem definir um token impede a aplicação de subir. Nunca ative o perfil `dev` em
produção.

## Variáveis de ambiente

| Variável | Padrão | Descrição |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | — | Use `dev` para desenvolvimento local |
| `SERVER_PORT` | `8080` | Porta HTTP |
| `FIREBASE_ENABLED` | `false` | Habilita Firebase Authentication e Firestore |
| `FIREBASE_PROJECT_ID` | — | ID do projeto Firebase |
| `GOOGLE_APPLICATION_CREDENTIALS` | — | Caminho da service account (fora do repositório) |
| `SMARTLOG_DEV_AUTH_ENABLED` | `false` (`true` no perfil `dev`) | Habilita o atalho de autenticação local |
| `SMARTLOG_DEV_AUTH_TOKEN` | vazio (`smartlog-dev-token` no perfil `dev`) | Único token aceito pelo dev-auth |
| `SMARTLOG_CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Origens permitidas no CORS, separadas por vírgula |

## Organização

```text
br.com.smartlog
├── api              # controllers REST, DTOs, OpenAPI e tratamento de erros
├── application      # serviços (casos de uso) e portas (interfaces de repositório)
├── domain           # modelos e exceções de domínio
├── infrastructure   # Firebase, repositórios Firestore e repositórios em memória
└── security         # filtro de autenticação, RBAC e CORS
```

Decisões importantes da arquitetura:

- Firestore é acessado somente pelo backend, através da infraestrutura.
- O `empresaId` é obtido do contexto autenticado, nunca confiado no payload.
- Firebase Authentication fornece o token Bearer; RBAC usa `ADMIN`, `MOTORISTA` e `CLIENTE`.
- Com `FIREBASE_ENABLED=false`, os repositórios em memória são usados; com `true`, os
  repositórios Firestore (`@ConditionalOnBean(Firestore.class)`).
- Credenciais Firebase e chaves externas são fornecidas por variáveis de ambiente, nunca
  versionadas.

## Recursos

| Recurso | Perfis |
| --- | --- |
| `GET /api/v1` | público |
| `/api/v1/entregas` | `ADMIN` (lista, cria, atualiza), `MOTORISTA` (lista e atualiza status), `CLIENTE` (lista as próprias) |
| `/api/v1/rotas` | `ADMIN` (CRUD), `MOTORISTA` (leitura) |
| `/api/v1/clientes`, `/api/v1/motoristas`, `/api/v1/veiculos` | `ADMIN` (CRUD) |

Falhas de token retornam `401`, acessos por perfil inválido retornam `403` e falhas do
Firestore retornam `503` com código `PERSISTENCE_FAILURE`.

## Firebase Authentication e Firestore

Para executar com um projeto Firebase, use Application Default Credentials apontando para
uma chave de service account armazenada fora do repositório:

```bash
export GOOGLE_APPLICATION_CREDENTIALS="/caminho/seguro/service-account.json"
export FIREBASE_ENABLED=true
export FIREBASE_PROJECT_ID="seu-project-id"
./mvnw spring-boot:run
```

Não informe credenciais no `application.yml`, no código ou em commits. Em ambientes Google,
prefira ADC/Workload Identity em vez de arquivos de chave.

Claims obrigatórias do ID token:

```json
{"role":"ADMIN|MOTORISTA|CLIENTE","empresaId":"empresa-001"}
```

O Firestore usa o caminho `empresas/{empresaId}/<coleção>`, garantindo o isolamento
multi-tenant. O `empresaId` do payload não é aceito.

## Swagger

Com a aplicação em execução:

- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/v3/api-docs`

## Testes

```bash
./mvnw test      # testes
./mvnw verify    # build completo, o mesmo executado no CI
```

## Documentação acadêmica

O SmartLog nasceu como projeto universitário. Os entregáveis de arquitetura da disciplina
(A02 — arquitetura da solução, A03 — modelo de API, A04 — estratégia de persistência e
A05 — segurança, autenticação e autorização) estão em [`docs/academico/`](docs/academico/).
