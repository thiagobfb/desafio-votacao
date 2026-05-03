# Sistema de Votação Cooperativista

> Solução para o desafio técnico de **Sistema de Votação** — backend REST em Java/Spring Boot que gerencia pautas, sessões de votação, votos de associados e apuração de resultados.
>
> O desafio original está preservado ao final deste documento.

---

## Stack

| Camada | Escolha |
|---|---|
| Linguagem | Java 21 (compilada com JDK 25 no ambiente local) |
| Framework | Spring Boot 3.3.5 |
| Build | Maven 3.9+ |
| Persistência | Spring Data JPA + Hibernate |
| Banco padrão | H2 file mode (`./data/votacao`) |
| Banco de teste | H2 in-memory |
| Banco opcional | PostgreSQL (profile `postgres`) |
| Migrations | Flyway |
| Validação | Jakarta Bean Validation |
| Documentação | springdoc-openapi 2 (Swagger UI) |
| Testes | JUnit 5, Mockito, AssertJ, Spring Boot Test |
| Boilerplate | Lombok (apenas em entidades JPA) |

---

## Pré-requisitos

- Java 21+
- Maven 3.9+

Nada mais. O banco H2 sobe junto com a aplicação no profile padrão; nenhum container ou serviço externo é necessário para executar/avaliar.

---

## Como executar

```bash
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. Os dados ficam em `./data/votacao.mv.db` (sobrevivem a restart, conforme requisito do desafio).

Para empacotar:

```bash
mvn clean package
java -jar target/votacao-0.1.0-SNAPSHOT.jar
```

### Profile alternativo com PostgreSQL

Disponível mas opcional. Suba um Postgres em `localhost:5432` (database `votacao`, user/pass `votacao`/`votacao`) e ative:

```bash
SPRING_PROFILES_ACTIVE=postgres mvn spring-boot:run
```

---

## Como testar

```bash
mvn verify
```

Roda **todos os testes** (atualmente 79): unitários de service, slice de web (`@WebMvcTest`), integração JPA (`@DataJpaTest`), e integração ponta-a-ponta (`@SpringBootTest`).

---

## Documentação da API

Com a aplicação rodando:

- **Swagger UI** — http://localhost:8080/swagger-ui.html
- **OpenAPI JSON** — http://localhost:8080/v3/api-docs
- **Console H2** (debug local) — http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/votacao`)

### Endpoints principais

| Método | Caminho | Descrição |
|---|---|---|
| `POST` | `/api/v1/pautas` | Cadastra pauta |
| `GET` | `/api/v1/pautas` | Lista pautas (paginado) |
| `GET` | `/api/v1/pautas/{id}` | Detalha pauta + estado |
| `POST` | `/api/v1/pautas/{id}/sessoes` | Abre sessão (duração em minutos, default 1) |
| `POST` | `/api/v1/pautas/{id}/votos` | Registra voto `SIM`/`NAO` |
| `GET` | `/api/v1/pautas/{id}/resultado` | Apura resultado |

### Exemplo rápido com curl

```bash
# 1. Cria pauta
curl -X POST http://localhost:8080/api/v1/pautas \
  -H 'Content-Type: application/json' \
  -d '{"titulo":"Aprovação do balanço 2026","descricao":"Balanço anual"}'

# 2. Abre sessão de 5 minutos
curl -X POST http://localhost:8080/api/v1/pautas/1/sessoes \
  -H 'Content-Type: application/json' \
  -d '{"duracaoMinutos":5}'

# 3. Registra voto (CPF é validado pelo serviço fake — Bônus 1, Spec 002)
#    O algoritmo dos dígitos verificadores valida o formato; '12345678901' é INVÁLIDO,
#    '11144477735' é VÁLIDO. Para CPF válido, ABLE_TO_VOTE / UNABLE_TO_VOTE é aleatório.
curl -X POST http://localhost:8080/api/v1/pautas/1/votos \
  -H 'Content-Type: application/json' \
  -d '{"cpf":"11144477735","voto":"SIM"}'

# 4. Apura
curl http://localhost:8080/api/v1/pautas/1/resultado
```

---

## Estrutura do projeto

Organizado **por feature** (não por camada técnica) — código que muda junto fica junto:

```
src/main/java/br/com/desafio/votacao/
├── pauta/
│   ├── api/            # PautaController + DTOs
│   ├── domain/         # Pauta entity + EstadoPauta enum
│   ├── repository/     # PautaRepository
│   └── service/        # PautaService + EstadoPautaResolver
├── sessao/             # idem
├── voto/               # idem
├── resultado/          # idem
└── shared/
    ├── config/         # ClockConfig, OpenApiConfig
    └── exception/      # NegocioException + GlobalExceptionHandler
```

Migrations em `src/main/resources/db/migration/V1__init.sql`.

---

## Spec Driven Development

O projeto foi desenhado com **SDD** (estilo [Spec Kit](https://github.com/github/spec-kit)) — cada decisão tem rastro:

```
specs/
├── constitution.md                     # 8 princípios invioláveis
├── README.md                           # como o SDD funciona aqui
└── 001-sistema-votacao/
    ├── spec.md                         # O QUÊ — RFs, RNs, fluxos, escopo
    ├── plan.md                         # COMO — stack, arquitetura, contratos REST, schema
    └── tasks.md                        # passo a passo executável
```

Antes de implementar a feature, comece por `specs/001-sistema-votacao/spec.md`.

---

## Decisões de design

- **Simplicidade acima de tudo.** Sem DDD/Hexagonal/CQRS — três camadas (Controller → Service → Repository) bastam para o domínio. Critério explícito do desafio: _"evitar over engineering"_.
- **Domínio anêmico-pragmático.** Entidades carregam estado + uma validação trivial (`Sessao.estaAbertaEm`). Regras de negócio vão para o Service.
- **FKs como `Long pautaId`** em vez de `@ManyToOne`. Sem proxies/lazy-loading; serviços compõem via repositórios.
- **Unicidade de voto e de sessão garantida no banco** (`UNIQUE` constraint), não só em código. Race conditions não dependem de ordem das chamadas.
- **`Clock` injetado** em UTC-3 (Horário de Brasília). Permite testar expiração de sessão sem `Thread.sleep` (ver `MutableClock` nos testes de integração).
- **`@RestControllerAdvice` com `ErroResponse` minimalista** (`status, message, errors[], timestamp`). Tratamento especial para enums inválidos (`"Valor inválido para 'voto'. Aceitos: [SIM, NAO]"`).
- **Versionamento de URI** (`/api/v1/...`). Estratégia completa será detalhada na Spec 003.
- **Logs estruturados** (chave-valor) em transições relevantes: criação de pauta, abertura de sessão, voto registrado/rejeitado.

> Cada decisão tem rationale completa em **[`docs/adr/`](docs/adr/README.md)**.

---

## Documentação técnica

Documentação detalhada vive em [`docs/`](docs/) e em [`specs/`](specs/):

| Tópico | Onde |
|---|---|
| **Architectural Decision Records (ADRs)** — 21 decisões de stack e arquitetura, cada uma com contexto/decisão/consequências/alternativas | [`docs/adr/README.md`](docs/adr/README.md) |
| **Arquitetura** — visão geral, camadas, schema, fluxo de requisição, observabilidade | [`docs/arquitetura.md`](docs/arquitetura.md) |
| **Estratégia de testes** — tipos presentes, mapa por arquivo, exemplos do que cada nível garante, ausências (performance) | [`docs/testes.md`](docs/testes.md) |
| **Tarefas bônus** — status real de cada uma das 3 (CPF, performance, versionamento) | [`docs/tarefas-bonus.md`](docs/tarefas-bonus.md) |
| **Setup nas IDEs** — IntelliJ, Eclipse e VS Code (importar, JDK, Lombok, rodar/debugar/testar) | [`docs/ide-setup.md`](docs/ide-setup.md) |
| **Spec 001** — requisitos, regras de negócio, plano de implementação, tasks | [`specs/001-sistema-votacao/`](specs/001-sistema-votacao/) |
| **Constituição do projeto** — 8 princípios invariantes | [`specs/constitution.md`](specs/constitution.md) |

---

## Próximas specs (placeholders, não implementadas)

Placeholders para as três tarefas bônus do enunciado. Status detalhado em [`docs/tarefas-bonus.md`](docs/tarefas-bonus.md).

- **Spec 002** — [Validação externa de CPF](specs/002-validacao-cpf/) (Tarefa Bônus 1) — ✅ **implementada** (algoritmo determinístico DV1+DV2 + habilitação aleatória + 19 testes adicionais).
- **Spec 003** — [Estratégia de versionamento de API](specs/003-versionamento-api/) (Tarefa Bônus 3) — ✅ URI prefix `/api/v1/` em vigor + política de deprecação documentada (`Deprecation`/`Sunset` IETF, 6 meses + 30 d de `410 Gone`).
- **Spec 004** — Performance e suporte a alto volume (Tarefa Bônus 2) — ⚠️ design parcial (índices, contagem agregada); sem load test medido.

---

---

# Desafio Original

## Objetivo

No cooperativismo, cada associado possui um voto e as decisões são tomadas em assembleias, por votação. Imagine que você deve criar uma solução para dispositivos móveis para gerenciar e participar dessas sessões de votação.
Essa solução deve ser executada na nuvem e promover as seguintes funcionalidades através de uma API REST:

- Cadastrar uma nova pauta
- Abrir uma sessão de votação em uma pauta (a sessão de votação deve ficar aberta por
  um tempo determinado na chamada de abertura ou 1 minuto por default)
- Receber votos dos associados em pautas (os votos são apenas 'Sim'/'Não'. Cada associado
  é identificado por um id único e pode votar apenas uma vez por pauta)
- Contabilizar os votos e dar o resultado da votação na pauta

Para fins de exercício, a segurança das interfaces pode ser abstraída e qualquer chamada para as interfaces pode ser considerada como autorizada. A solução deve ser construída em java, usando Spring-boot, mas os frameworks e bibliotecas são de livre escolha (desde que não infrinja direitos de uso).

É importante que as pautas e os votos sejam persistidos e que não sejam perdidos com o restart da aplicação.

O foco dessa avaliação é a comunicação entre o backend e o aplicativo mobile. Essa comunicação é feita através de mensagens no formato JSON, onde essas mensagens serão interpretadas pelo cliente para montar as telas onde o usuário vai interagir com o sistema. A aplicação cliente não faz parte da avaliação, apenas os componentes do servidor. O formato padrão dessas mensagens será detalhado no anexo 1.

## Como proceder

Por favor, **CLONE** o repositório e implemente sua solução, ao final, notifique a conclusão e envie o link do seu repositório clonado no GitHub, para que possamos analisar o código implementado.

Lembre de deixar todas as orientações necessárias para executar o seu código.

### Tarefas bônus

- Tarefa Bônus 1 - Integração com sistemas externos
  - Criar uma Facade/Client Fake que retorna aleátoriamente se um CPF recebido é válido ou não.
  - Caso o CPF seja inválido, a API retornará o HTTP Status 404 (Not found). Você pode usar geradores de CPF para gerar CPFs válidos
  - Caso o CPF seja válido, a API retornará se o usuário pode (ABLE_TO_VOTE) ou não pode (UNABLE_TO_VOTE) executar a operação. Essa operação retorna resultados aleatórios, portanto um mesmo CPF pode funcionar em um teste e não funcionar no outro.

```
// CPF Ok para votar
{
    "status": "ABLE_TO_VOTE
}
// CPF Nao Ok para votar - retornar 404 no client tb
{
    "status": "UNABLE_TO_VOTE
}
```

Exemplos de retorno do serviço

### Tarefa Bônus 2 - Performance

- Imagine que sua aplicação possa ser usada em cenários que existam centenas de
  milhares de votos. Ela deve se comportar de maneira performática nesses
  cenários
- Testes de performance são uma boa maneira de garantir e observar como sua
  aplicação se comporta

### Tarefa Bônus 3 - Versionamento da API

○ Como você versionaria a API da sua aplicação? Que estratégia usar?

## O que será analisado

- Simplicidade no design da solução (evitar over engineering)
- Organização do código
- Arquitetura do projeto
- Boas práticas de programação (manutenibilidade, legibilidade etc)
- Possíveis bugs
- Tratamento de erros e exceções
- Explicação breve do porquê das escolhas tomadas durante o desenvolvimento da solução
- Uso de testes automatizados e ferramentas de qualidade
- Limpeza do código
- Documentação do código e da API
- Logs da aplicação
- Mensagens e organização dos commits

## Dicas

- Teste bem sua solução, evite bugs
- Deixe o domínio das URLs de callback passiveis de alteração via configuração, para facilitar
  o teste tanto no emulador, quanto em dispositivos fisicos.
  Observações importantes
- Não inicie o teste sem sanar todas as dúvidas
- Iremos executar a aplicação para testá-la, cuide com qualquer dependência externa e
  deixe claro caso haja instruções especiais para execução do mesmo
  Classificação da informação: Uso Interno

## Anexo 1

### Introdução

A seguir serão detalhados os tipos de tela que o cliente mobile suporta, assim como os tipos de campos disponíveis para a interação do usuário.

### Tipo de tela – FORMULARIO

A tela do tipo FORMULARIO exibe uma coleção de campos (itens) e possui um ou dois botões de ação na parte inferior.

O aplicativo envia uma requisição POST para a url informada e com o body definido pelo objeto dentro de cada botão quando o mesmo é acionado. Nos casos onde temos campos de entrada
de dados na tela, os valores informados pelo usuário são adicionados ao corpo da requisição. Abaixo o exemplo da requisição que o aplicativo vai fazer quando o botão "Ação 1" for acionado:

```
POST http://seudominio.com/ACAO1
{
    "campo1": "valor1",
    "campo2": 123,
    "idCampoTexto": "Texto",
    "idCampoNumerico: 999
    "idCampoData": "01/01/2000"
}
```

Obs: o formato da url acima é meramente ilustrativo e não define qualquer padrão de formato.

### Tipo de tela – SELECAO

A tela do tipo SELECAO exibe uma lista de opções para que o usuário.

O aplicativo envia uma requisição POST para a url informada e com o body definido pelo objeto dentro de cada item da lista de seleção, quando o mesmo é acionado, semelhando ao funcionamento dos botões da tela FORMULARIO.
