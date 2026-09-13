# Biblioteca API

API REST para gerenciamento de uma biblioteca de livros, desenvolvida como parte de um desafio técnico. O projeto implementa um CRUD completo com cache Redis, persistência em MongoDB, validação de dados, documentação OpenAPI e testes de unidade e integração com Testcontainers.

[![Coverage](https://img.shields.io/badge/coverage-89%25-brightgreen)](#-testes)
[![Java](https://img.shields.io/badge/Java-21-blue)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-brightgreen)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-8-green)](https://www.mongodb.com/)
[![Redis](https://img.shields.io/badge/Redis-8-red)](https://redis.io/)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

---

## 📋 Sumário

- [Stack](#-stack)
- [Arquitetura](#-arquitetura)
- [Como Executar](#-como-executar)
- [Endpoints](#-endpoints)
- [Cache](#-cache)
- [Validações](#-validações)
- [Testes](#-testes)
- [Decisões Técnicas](#-decisões-técnicas)

---

## 🛠 Stack

| Tecnologia | Versão |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.16 |
| Spring Data MongoDB | (via Spring Boot) |
| Spring Data Redis | (via Spring Boot) |
| Maven | 3.9+ |
| Lombok | (via Spring Boot) |
| ModelMapper | 3.2.4 |
| SpringDoc OpenAPI | 2.8.13 |
| JUnit 5 | (via Spring Boot) |
| Mockito | (via Spring Boot) |
| Testcontainers | (via Spring Boot) |
| JaCoCo | 0.8.14 |

---

## 🏗 Arquitetura

O projeto segue o padrão **Clean Architecture simplificado**, com separação clara de responsabilidades:

```
Controller → Service → Repository → MongoDB
                ↕
           Redis (Cache)
```

- **Controller**: apenas orquestra requisições HTTP, sem regras de negócio.
- **Service**: contém todas as regras de negócio, validações e integração com cache.
- **Repository**: apenas acesso ao MongoDB via Spring Data.
- **DTOs**: usam `record` do Java 21 para entrada e saída de dados.
- **Entidade**: anotada com `@Document` do Spring Data MongoDB.

### Estrutura de Pacotes

```
com.caio.biblioteca
├── config/              # Configurações (Redis, Mongo, ModelMapper, OpenAPI)
├── controller/          # Endpoints REST
├── dto/
│   ├── request/         # DTOs de entrada (records)
│   │   └── deserializer/ # Desserializadores customizados (ISBN)
│   └── response/        # DTOs de saída (records)
├── entity/              # Entidades MongoDB
├── enums/               # Enums do domínio (Genero)
├── exception/           # Exceções customizadas e handler global
├── mapper/              # Mapeamento entre DTOs e entidades (ModelMapper)
├── repository/          # Repositórios Spring Data MongoDB
├── service/             # Regras de negócio
└── validation/          # Validadores customizados (AnoPublicacaoValido)
```

---

## 🚀 Como Executar

### Pré-requisitos

- Java 21
- Maven 3.9+
- Docker (para subir MongoDB e Redis)

### 1. Subir MongoDB e Redis

```bash
docker run -d --name mongo-local -p 27017:27017 mongo:8
docker run -d --name redis-local -p 6379:6379 redis:8
```

### 2. Executar a aplicação

```bash
mvn spring-boot:run
```

A API estará disponível em `http://localhost:8080`.

### 3. Acessar a documentação Swagger

Abra o navegador em: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## 📡 Endpoints

| Método | Endpoint | Descrição | Status de Sucesso |
|---|---|---|---|
| `POST` | `/livros` | Cadastrar um novo livro | `201 Created` |
| `GET` | `/livros/{id}` | Buscar livro por ID (com cache Redis) | `200 OK` |
| `GET` | `/livros` | Listar livros com paginação e filtro por gênero | `200 OK` |
| `PUT` | `/livros/{id}` | Atualizar um livro existente | `200 OK` |
| `DELETE` | `/livros/{id}` | Remover um livro | `204 No Content` |

### Exemplos de Requisição

**POST /livros**

```json
{
  "titulo": "Clean Code",
  "autor": "Robert C. Martin",
  "isbn": "978-01-32350-88-4",
  "anoPublicacao": 2008,
  "genero": "TECNOLOGIA",
  "disponivel": true
}
```

**Resposta (201 Created)**

```json
{
  "id": "65f1a2b3c4d5e6f7a8b9c0d1",
  "titulo": "Clean Code",
  "autor": "Robert C. Martin",
  "isbn": "9780132350884",
  "anoPublicacao": 2008,
  "genero": "TECNOLOGIA",
  "disponivel": true,
  "dataInclusao": "2025-04-27T14:30:00",
  "dataAtualizacao": null
}
```

**GET /livros?pagina=0&tamanho=10&genero=TECNOLOGIA**

```json
{
  "content": [
    {
      "id": "65f1a2b3c4d5e6f7a8b9c0d1",
      "titulo": "Clean Code",
      "autor": "Robert C. Martin",
      "isbn": "9780132350884",
      "anoPublicacao": 2008,
      "genero": "TECNOLOGIA",
      "disponivel": true,
      "dataInclusao": "2025-04-27T14:30:00",
      "dataAtualizacao": null
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": { "empty": true, "sorted": false, "unsorted": true },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "size": 10,
  "number": 0,
  "numberOfElements": 1,
  "first": true,
  "empty": false
}
```

### Formato de Erro Padronizado

Todos os erros retornam o seguinte formato:

```json
{
  "codigo": "LIVRO_NAO_ENCONTRADO",
  "mensagem": "Livro com id '123' não encontrado.",
  "timestamp": "2025-04-27T14:30:00"
}
```

| Código | Status HTTP | Descrição |
|---|---|---|
| `LIVRO_NAO_ENCONTRADO` | 404 | Livro não encontrado |
| `ISBN_DUPLICADO` | 409 | ISBN já cadastrado |
| `ANO_PUBLICACAO_INVALIDO` | 400 | Ano de publicação inválido |
| `DADOS_INVALIDOS` | 400 | Erro de validação nos campos |
| `ERRO_INTERNO` | 500 | Erro inesperado no servidor |

---

## 💾 Cache

O projeto utiliza **Spring Cache** com **Redis** como provider para otimizar leituras.

- **Chave de cache padrão**: `biblioteca:livro:{id}`
- **TTL**: 10 minutos
- **Invalidação**: O cache é invalidado automaticamente nas operações de atualização (`PUT`) e exclusão (`DELETE`).
- **Nunca armazenar dados críticos apenas no cache**: O MongoDB é sempre a fonte primária de dados.

### Configuração

O TTL e o prefixo são configuráveis via `application.yml`:

```yaml
biblioteca:
  cache:
    prefixo: "biblioteca:"
    ttl: 10m
```

---

## ✅ Validações

### Validações no DTO (`LivroRequest`)

| Campo | Validação |
|---|---|
| `titulo` | Obrigatório, máximo 255 caracteres |
| `autor` | Obrigatório, máximo 255 caracteres |
| `isbn` | Obrigatório, formato ISBN-10 ou ISBN-13 |
| `anoPublicacao` | Obrigatório, maior que 1000 e menor ou igual ao ano atual |
| `genero` | Obrigatório, deve ser um valor válido do enum `Genero` |
| `disponivel` | Obrigatório |

### Normalização de ISBN

O ISBN é normalizado automaticamente na desserialização (remove hífens e espaços, converte `X` para maiúsculo) através de um `@JsonDeserialize` customizado. Isso garante que `"978-85-1234-567-8"` e `"9788512345678"` sejam considerados o mesmo ISBN para fins de unicidade.

### Validação de Ano de Publicação

A validação do ano é feita via anotação customizada `@AnoPublicacaoValido`, que compara o valor com o ano atual dinamicamente (`Year.now()`).

### Exceções

- `NegocioException`: exceção customizada que carrega um código de erro e um `HttpStatus`.
- `GlobalExceptionHandler`: trata todas as exceções globalmente via `@RestControllerAdvice`, incluindo:
    - `NegocioException`
    - `MethodArgumentNotValidException` (validação de DTO)
    - `HandlerMethodValidationException` (validação de parâmetros)
    - `MethodArgumentTypeMismatchException` (enum inválido na query)
    - `HttpMessageNotReadableException` (JSON malformado)
    - `ConstraintViolationException`
    - `Exception` (fallback para 500)

---

## 🧪 Testes

O projeto possui **39 testes** (13 unitários + 26 de integração) com cobertura de **89% de linhas** e **90% de instruções**.

### Testes Unitários (`LivroServiceTest`)

- Mockam o repositório e o mapper com Mockito.
- Cobrem cenários de sucesso, livro não encontrado, ISBN duplicado, ano inválido, entre outros.

### Testes de Integração (`LivroControllerIntegrationTest`)

- Usam **Testcontainers** para subir instâncias reais de MongoDB e Redis.
- Testam todos os endpoints (`POST`, `GET`, `PUT`, `DELETE`).
- Cobrem cenários de erro (`400`, `404`, `409`).
- Testam o comportamento do cache (armazenamento, TTL e invalidação).

### Como Executar os Testes

```bash
mvn clean verify
```

> **Nota:** O comando `mvn test` apenas executa os testes. O comando `mvn verify` também executa o `jacoco:check`, que valida a cobertura mínima de 80% (linhas).

### Relatório de Cobertura

O relatório HTML do JaCoCo é gerado em:

```
target/site/jacoco/index.html
```

Abra este arquivo no navegador para visualizar os detalhes da cobertura.

---

## 🧠 Decisões Técnicas

1. **Validação no DTO e no Service**: As validações de formato são feitas no DTO (Bean Validation). As regras de negócio que dependem de estado (como unicidade de ISBN) são feitas no Service. A validação de ano é feita em ambos, como defesa em profundidade.

2. **Normalização de ISBN na Desserialização**: Optou-se por normalizar o ISBN via `@JsonDeserialize` no DTO, garantindo que o `@Pattern` valide o valor já limpo e que o Service não precise se preocupar com isso.

3. **Cache com Prefixo Configurável**: O prefixo do cache (`biblioteca:`) e o TTL (10 minutos) são externalizados no `application.yml` e injetados via `@ConfigurationProperties`, permitindo ajustes por ambiente sem recompilação.

4. **Auditoria com `@EnableMongoAuditing`**: As datas `dataInclusao` e `dataAtualizacao` são preenchidas automaticamente pelo Spring Data MongoDB, sem necessidade de código manual no Service.

5. **Índice Único no ISBN**: O campo `isbn` possui `@Indexed(unique = true)` na entidade, garantindo unicidade no nível do banco de dados, além da validação no Service.

6. **Tratamento Global de Exceções**: O `@RestControllerAdvice` centraliza o tratamento de erros, garantindo um formato de resposta padronizado e mensagens descritivas.

7. **Testes com Testcontainers**: A escolha de Testcontainers em vez de mocks para os testes de integração garante que o comportamento real do MongoDB e do Redis seja validado.

---

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.