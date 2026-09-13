# Biblioteca API

API REST para gerenciamento de livros de uma biblioteca, desenvolvida com **Java 21**, **Spring Boot**, **MongoDB** e **Redis**.

O projeto implementa um CRUD completo de livros, com paginação, filtro por gênero, validações de regras de negócio, cache com Redis, tratamento global de exceções, documentação da API com OpenAPI/Swagger e testes automatizados utilizando JUnit 5, Mockito e Testcontainers.

A aplicação foi desenvolvida seguindo uma abordagem de **Clean Architecture simplificada**, mantendo uma separação clara de responsabilidades entre as camadas da aplicação.

---

## Índice

* [Tecnologias](#tecnologias)
* [Arquitetura](#arquitetura)
* [Funcionalidades](#funcionalidades)
* [Modelo de dados](#modelo-de-dados)
* [Regras de negócio](#regras-de-negócio)
* [Cache com Redis](#cache-com-redis)
* [Endpoints](#endpoints)
* [Tratamento de erros](#tratamento-de-erros)
* [Pré-requisitos](#pré-requisitos)
* [Como executar](#como-executar)
* [Documentação da API](#documentação-da-api)
* [Testes](#testes)
* [Testes de integração](#testes-de-integração)
* [Cobertura de código](#cobertura-de-código)
* [Estrutura do projeto](#estrutura-do-projeto)
* [Execução completa](#execução-completa)
* [Considerações finais](#considerações-finais)

---

## Tecnologias

* **Java 21**
* **Spring Boot 3**
* **Spring Data MongoDB**
* **Spring Data Redis**
* **MongoDB**
* **Redis**
* **Maven**
* **Lombok**
* **ModelMapper**
* **SpringDoc OpenAPI**
* **JUnit 5**
* **Mockito**
* **Testcontainers**
* **JaCoCo**
* **Docker Compose**

---

## Arquitetura

O projeto utiliza uma abordagem simplificada de **Clean Architecture**, separando as responsabilidades da aplicação em camadas.

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
MongoDB
```

O Redis é utilizado como camada de cache junto ao Service:

```text
              ┌─────────────┐
              │  Controller │
              └──────┬──────┘
                     ↓
              ┌─────────────┐
              │   Service   │
              └──────┬──────┘
                     │
            ┌────────┴────────┐
            ↓                 ↓
       ┌─────────┐       ┌────────────┐
       │  Redis  │       │ Repository │
       │  Cache  │       └──────┬─────┘
       └─────────┘              ↓
                           ┌──────────┐
                           │ MongoDB  │
                           └──────────┘
```

### Responsabilidades das camadas

**Controller**

Responsável pela exposição dos endpoints REST, recebimento das requisições e retorno das respostas HTTP.

**Service**

Concentra as regras de negócio, validações, operações de atualização e integração com o mecanismo de cache.

**Repository**

Responsável exclusivamente pela persistência e consulta dos dados utilizando Spring Data MongoDB.

**Entity**

Representa o documento persistido no MongoDB.

**DTOs**

Definem os contratos de entrada e saída da API, evitando a exposição direta da entidade de persistência.

**Mapper**

Responsável pela conversão entre DTOs e entidades utilizando ModelMapper.

**Exception**

Centraliza as exceções de negócio e o tratamento global das respostas de erro.

**Config**

Contém as configurações relacionadas à infraestrutura e aos frameworks utilizados pela aplicação.

---

## Funcionalidades

* Cadastro de livros
* Consulta de livro por ID
* Listagem paginada
* Filtro de livros por gênero
* Atualização de livros
* Exclusão de livros
* Cache de consultas com Redis
* Invalidação de cache em atualizações e exclusões
* Validação de ISBN único
* Validação do ano de publicação
* Tratamento global de exceções
* Documentação da API com OpenAPI/Swagger
* Testes unitários
* Testes de integração
* Testcontainers para MongoDB e Redis
* Análise de cobertura com JaCoCo

---

## Modelo de dados

A entidade `Livro` possui os seguintes campos:

| Campo             | Tipo          | Descrição                                         |
| ----------------- | ------------- | ------------------------------------------------- |
| `id`              | String        | Identificador gerado automaticamente pelo MongoDB |
| `titulo`          | String        | Título do livro                                   |
| `autor`           | String        | Autor do livro                                    |
| `isbn`            | String        | ISBN único do livro                               |
| `anoPublicacao`   | Integer       | Ano de publicação                                 |
| `genero`          | Genero        | Gênero literário                                  |
| `disponivel`      | Boolean       | Indica se o livro está disponível                 |
| `dataInclusao`    | LocalDateTime | Data e hora de inclusão                           |
| `dataAtualizacao` | LocalDateTime | Data e hora da última atualização                 |

### Gêneros disponíveis

```text
FICCAO_CIENTIFICA
FANTASIA
ROMANCE
TERROR
BIOGRAFIA
HISTORIA
TECNOLOGIA
INFANTIL
```

---

## Regras de negócio

A API possui as seguintes regras:

* `titulo` é obrigatório.
* `autor` é obrigatório.
* `isbn` é obrigatório.
* `genero` é obrigatório.
* `isbn` deve ser único.
* `anoPublicacao` deve ser maior que `1000`.
* `anoPublicacao` não pode ser maior que o ano atual.
* `genero` deve corresponder a um dos valores definidos no enum `Genero`.
* `dataInclusao` é preenchida automaticamente na criação.
* `dataAtualizacao` é atualizada automaticamente durante alterações.

As validações de entrada são realizadas através das validações do DTO e as regras de negócio são tratadas na camada de Service.

---

## Cache com Redis

O Redis é utilizado para otimizar as consultas de livros por ID.

A estratégia de cache utiliza:

* **Cache:** `livro`
* **Prefixo da chave:** `biblioteca:livro:`
* **TTL:** 10 minutos

Exemplo de chave:

```text
biblioteca:livro:{id}
```

### Funcionamento

Ao consultar um livro através de:

```text
GET /livros/{id}
```

a aplicação utiliza o Redis para evitar consultas repetidas ao MongoDB.

Quando um livro é atualizado ou excluído, sua entrada correspondente no cache é invalidada.

O MongoDB permanece como fonte persistente dos dados. O Redis é utilizado apenas como mecanismo de otimização, não sendo a única fonte de armazenamento das informações.

---

## Endpoints

| Método   | Endpoint       | Descrição                      |
| -------- | -------------- | ------------------------------ |
| `POST`   | `/livros`      | Cadastra um novo livro         |
| `GET`    | `/livros/{id}` | Busca um livro por ID          |
| `GET`    | `/livros`      | Lista livros de forma paginada |
| `PUT`    | `/livros/{id}` | Atualiza um livro              |
| `DELETE` | `/livros/{id}` | Exclui um livro                |

### Cadastro

```http
POST /livros
```

Exemplo de requisição:

```json
{
  "titulo": "Clean Code",
  "autor": "Robert C. Martin",
  "isbn": "9780132350884",
  "anoPublicacao": 2008,
  "genero": "TECNOLOGIA",
  "disponivel": true
}
```

### Consulta por ID

```http
GET /livros/{id}
```

### Listagem paginada

```http
GET /livros?pagina=0&tamanho=10
```

Valores padrão:

* `pagina`: `0`
* `tamanho`: `10`

### Filtro por gênero

```http
GET /livros?genero=TECNOLOGIA
```

Também é possível combinar paginação e filtro:

```http
GET /livros?pagina=0&tamanho=10&genero=TECNOLOGIA
```

### Atualização

```http
PUT /livros/{id}
```

### Exclusão

```http
DELETE /livros/{id}
```

Em caso de sucesso, a exclusão retorna:

```text
204 No Content
```

---

## Tratamento de erros

A aplicação utiliza um tratamento global de exceções através de `@RestControllerAdvice`.

As respostas de erro seguem um formato padronizado:

```json
{
  "codigo": "LIVRO_NAO_ENCONTRADO",
  "mensagem": "Livro com id '123' não encontrado.",
  "timestamp": "2026-09-13T12:30:00"
}
```

Principais códigos utilizados:

```text
LIVRO_NAO_ENCONTRADO
ISBN_DUPLICADO
ANO_PUBLICACAO_INVALIDO
DADOS_INVALIDOS
```

Erros relacionados às regras de negócio são representados pela exceção customizada `NegocioException`.

---

## Pré-requisitos

Para executar o projeto localmente, é necessário ter instalado:

* Java 21
* Maven 3.9+
* Docker
* Docker Compose

---

## Como executar

### 1. Clonar o repositório

```bash
git clone <URL_DO_REPOSITORIO>
```

Entrar no diretório:

```bash
cd biblioteca-api
```

### 2. Subir MongoDB e Redis

Na raiz do projeto:

```bash
docker compose up -d
```

Verificar os containers:

```bash
docker ps
```

Os serviços estarão disponíveis em:

```text
MongoDB → localhost:27017
Redis   → localhost:6379
```

### 3. Executar a aplicação

```bash
mvn spring-boot:run
```

A API estará disponível em:

```text
http://localhost:8080
```

---

## Documentação da API

A documentação interativa está disponível através do Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

A documentação utiliza **OpenAPI** através do SpringDoc.

Os endpoints possuem documentação de operações e respostas utilizando:

* `@Operation`
* `@ApiResponse`
* `@Tag`

---

## Testes

O projeto possui uma estratégia de testes dividida entre **testes unitários** e **testes de integração**, buscando validar tanto as regras de negócio isoladamente quanto o funcionamento da aplicação integrada às suas principais dependências.

### Testes unitários

Os testes unitários da camada de Service utilizam **JUnit 5** e **Mockito**.

Nessa abordagem, o `LivroRepository` é mockado, permitindo testar as regras de negócio de forma isolada, sem dependência de MongoDB ou Redis.

São cobertos cenários como:

* Criação de livros
* Consulta por ID
* Livro não encontrado
* ISBN duplicado
* Ano de publicação inválido
* Atualização de livros
* Exclusão de livros
* Validações de regras de negócio

---

## Testes de integração

Os testes de integração validam o comportamento da API através de requisições HTTP utilizando **MockMvc**, executando a aplicação integrada com instâncias reais de **MongoDB** e **Redis**.

Para isso, o projeto utiliza **Testcontainers**, permitindo que os testes sejam executados em ambientes Docker isolados e reproduzíveis, sem depender de instalações locais de MongoDB ou Redis.

Os containers utilizados nos testes são:

* **MongoDB** — persistência dos livros
* **Redis** — armazenamento e validação do cache

Antes de cada teste, os dados persistidos no MongoDB e as chaves do Redis são limpos para garantir isolamento entre os cenários.

### Fluxo validado

Os testes de integração validam o fluxo completo da aplicação:

```text
HTTP Request
     ↓
Controller
     ↓
Service
     ↓
Repository
     ↓
MongoDB

          ↘
           Redis
```

Dessa forma, os testes não validam apenas métodos isolados, mas também a integração entre as principais camadas da aplicação e suas dependências externas.

### Cenários de integração

Entre os principais cenários testados estão:

* `POST /livros` — criação de livro
* `GET /livros/{id}` — consulta por ID
* `GET /livros` — listagem paginada
* `GET /livros?genero=...` — filtro por gênero
* `PUT /livros/{id}` — atualização
* `DELETE /livros/{id}` — exclusão
* Dados inválidos retornando `400 Bad Request`
* Livro inexistente retornando `404 Not Found`
* ISBN duplicado
* Ano de publicação inválido
* Funcionamento do cache com Redis
* Reutilização de dados armazenados em cache
* Invalidação do cache após atualização
* Invalidação do cache após exclusão
* Atualização das datas de auditoria

### Execução dos testes

Para executar todos os testes:

```bash
mvn clean test
```

Os testes de integração iniciam automaticamente os containers necessários através do Testcontainers.

Não é necessário iniciar manualmente MongoDB ou Redis para executar a suíte de testes.

---

## Cobertura de código

O projeto utiliza **JaCoCo** para análise de cobertura dos testes.

Para executar a verificação completa:

```bash
mvn clean verify
```

O projeto possui uma configuração de cobertura mínima de **80% das linhas de código**, conforme solicitado na prova técnica.

A cobertura atual está acima do requisito mínimo.

O relatório detalhado pode ser encontrado em:

```text
target/site/jacoco/index.html
```

---

## Estrutura do projeto

```text
src/
├── main/
│   └── java/
│       └── com/caio/biblioteca/
│           ├── config/
│           ├── controller/
│           ├── dto/
│           │   ├── request/
│           │   └── response/
│           ├── entity/
│           ├── enums/
│           ├── exception/
│           ├── mapper/
│           ├── repository/
│           └── service/
│
└── test/
    └── java/
        └── com/caio/biblioteca/
            ├── controller/
            └── service/
```

### Principais arquivos da raiz

```text
pom.xml
docker-compose.yml
README.md
```

---

## Execução completa

Para validar o projeto desde a compilação até os testes e análise de cobertura:

```bash
mvn clean verify
```

Se a execução terminar com:

```text
BUILD SUCCESS
```

o projeto foi compilado, os testes foram executados com sucesso e o requisito mínimo de cobertura foi atendido.

---

## Considerações finais

O projeto foi desenvolvido buscando atender aos requisitos da prova técnica e, ao mesmo tempo, manter uma estrutura simples, organizada e de fácil manutenção.

As principais decisões foram:

* Separação de responsabilidades através de uma arquitetura em camadas.
* Uso do MongoDB como persistência principal.
* Uso do Redis para otimização das consultas por ID.
* Invalidação do cache após operações de atualização e exclusão.
* Utilização de DTOs para definir o contrato da API.
* Utilização do ModelMapper para conversão entre DTOs e entidades.
* Auditoria automática das datas de inclusão e atualização.
* Centralização do tratamento de exceções.
* Testes unitários com Mockito.
* Testes de integração com MongoDB e Redis através do Testcontainers.
* Automação da verificação de cobertura através do JaCoCo.

O objetivo foi manter a solução proporcional ao escopo proposto, evitando complexidade desnecessária e priorizando **legibilidade, testabilidade, separação de responsabilidades e facilidade de manutenção**.
