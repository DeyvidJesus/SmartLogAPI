# SmartLog API

Fundação da API REST do SmartLog, construída com Java 21 e Spring Boot.

## Requisitos

- JDK 21+
- Maven 3.9+

## Executar localmente

```bash
mvn spring-boot:run
```

Ou gerar e executar o artefato:

```bash
mvn clean package
java -jar target/smartlog-api-0.0.1-SNAPSHOT.jar
```

Por padrão a aplicação escuta em `http://localhost:8080`. A porta pode ser alterada
com `SERVER_PORT=8081`.

## Verificar a aplicação

```bash
curl http://localhost:8080/api/v1
curl http://localhost:8080/actuator/health
```

O endpoint público de informações é versionado sob `/api/v1`. Entregas usam
`/api/v1/entregas` e exigem token Firebase.

## Organização prevista

As próximas implementações devem manter as responsabilidades separadas:

```text
br.com.smartlog
├── api              # controllers e DTOs REST
├── application      # casos de uso e serviços
├── domain           # modelos e regras de negócio
└── infrastructure   # Firebase Authentication e repositórios Firestore
```

Decisões importantes da arquitetura:

- Firestore será acessado somente pelo backend, através da infraestrutura.
- O `empresaId` será obtido do contexto autenticado, nunca confiado no payload.
- Firebase Authentication fornecerá o token Bearer; RBAC usará ADMIN, MOTORISTA e CLIENTE.
- Credenciais Firebase e chaves externas devem ser fornecidas por variáveis de ambiente,
  nunca versionadas.

## Firebase Authentication e Firestore

Por segurança, a integração fica desabilitada por padrão. Para executar com um projeto
Firebase localmente, use Application Default Credentials apontando para uma chave de
service account armazenada fora do repositório:

```bash
export GOOGLE_APPLICATION_CREDENTIALS="/caminho/seguro/service-account.json"
export FIREBASE_ENABLED=true
export FIREBASE_PROJECT_ID="seu-project-id"
mvn spring-boot:run
```

Não informe credenciais no `application.yml`, no código ou em commits. Em ambientes
Google, prefira ADC/Workload Identity em vez de arquivos de chave.

Claims obrigatórias do ID token:

```json
{"role":"ADMIN|MOTORISTA|CLIENTE","empresaId":"empresa-001"}
```

O Firestore usa o caminho `empresas/{empresaId}/entregas`, garantindo o isolamento
multi-tenant definido nos documentos A02–A05. O `empresaId` do payload não é aceito.

### Permissões de entregas

- `ADMIN`: lista, cria e atualiza entregas da própria empresa.
- `MOTORISTA`: lista o escopo operacional e atualiza status.
- `CLIENTE`: lista somente suas entregas; não cria nem atualiza status.

Falhas de token retornam `401`, acessos por perfil inválido retornam `403` e falhas
do Firestore retornam `503` com código `PERSISTENCE_FAILURE`.

## Swagger

Com a aplicação em execução, a documentação está disponível em:

- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/v3/api-docs`

## Validação

```bash
mvn test
```
