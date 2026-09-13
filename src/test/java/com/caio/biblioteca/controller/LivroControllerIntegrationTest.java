package com.caio.biblioteca.controller;

import com.caio.biblioteca.AbstractIntegrationTest;
import com.caio.biblioteca.dto.request.LivroRequest;
import com.caio.biblioteca.enums.Genero;
import com.caio.biblioteca.repository.LivroRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class LivroControllerIntegrationTest extends AbstractIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8");

    @Container
    static RedisContainer redisContainer = new RedisContainer("redis:8");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LivroRepository livroRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @DynamicPropertySource
    static void configurarContainers(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @BeforeEach
    void limparBanco() {
        livroRepository.deleteAll();

        Set<String> keys = redisTemplate.keys("biblioteca:livro:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private LivroRequest requestPadrao() {
        return new LivroRequest(
                "Clean Code",
                "Robert C. Martin",
                "9780132350884",
                2008,
                Genero.TECNOLOGIA,
                true
        );
    }

    private String criarLivroERetornarId(LivroRequest request) throws Exception {
        String response = mockMvc.perform(
                        post("/livros")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }

    // =========================================================
    // POST /livros
    // =========================================================

    @Test
    void deveCadastrarLivro() throws Exception {
        mockMvc.perform(
                        post("/livros")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestPadrao()))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.titulo").value("Clean Code"))
                .andExpect(jsonPath("$.autor").value("Robert C. Martin"))
                .andExpect(jsonPath("$.isbn").value("9780132350884"))
                .andExpect(jsonPath("$.genero").value("TECNOLOGIA"))
                .andExpect(jsonPath("$.disponivel").value(true))
                .andExpect(jsonPath("$.dataInclusao").isNotEmpty());
    }

    @Test
    void deveNormalizarIsbnComHifensEArmazenarSemSeparadores() throws Exception {
        LivroRequest request = new LivroRequest(
                "Clean Code",
                "Robert C. Martin",
                "978-01-32350-88-4",
                2008,
                Genero.TECNOLOGIA,
                true
        );

        mockMvc.perform(
                        post("/livros")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isbn").value("9780132350884"));
    }

    @Test
    void deveRetornar400QuandoDadosForemInvalidos() throws Exception {
        LivroRequest request = new LivroRequest(
                "",
                "",
                "",
                2008,
                Genero.TECNOLOGIA,
                true
        );

        mockMvc.perform(
                        post("/livros")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));
    }

    @Test
    void deveRetornar400QuandoIsbnForInvalido() throws Exception {
        LivroRequest request = new LivroRequest(
                "Clean Code",
                "Robert C. Martin",
                "isbn-invalido",
                2008,
                Genero.TECNOLOGIA,
                true
        );

        mockMvc.perform(
                        post("/livros")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"))
                .andExpect(jsonPath("$.mensagem")
                        .value(org.hamcrest.Matchers.containsString("ISBN")));
    }

    @Test
    void deveRetornar409QuandoIsbnJaEstiverCadastrado() throws Exception {
        LivroRequest primeiroLivro = requestPadrao();

        LivroRequest segundoLivro = new LivroRequest(
                "Outro Livro",
                "Outro Autor",
                "9780132350884",
                2020,
                Genero.TECNOLOGIA,
                true
        );

        criarLivroERetornarId(primeiroLivro);

        mockMvc.perform(
                        post("/livros")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(segundoLivro))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ISBN_DUPLICADO"));
    }

    @Test
    void deveRetornar400QuandoAnoPublicacaoForInvalido() throws Exception {
        LivroRequest request = new LivroRequest(
                "Livro Inválido",
                "Autor",
                "9780132350884",
                1000,
                Genero.TECNOLOGIA,
                true
        );

        mockMvc.perform(
                        post("/livros")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"))
                .andExpect(jsonPath("$.mensagem")
                        .value(org.hamcrest.Matchers.containsString("anoPublicacao")));
    }

    @Test
    void deveRetornar400QuandoAnoPublicacaoForFuturo() throws Exception {
        int anoFuturo = Year.now().getValue() + 1;

        LivroRequest request = new LivroRequest(
                "Livro Futuro",
                "Autor",
                "9780132350884",
                anoFuturo,
                Genero.TECNOLOGIA,
                true
        );

        mockMvc.perform(
                        post("/livros")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"))
                .andExpect(jsonPath("$.mensagem")
                        .value(org.hamcrest.Matchers.containsString("anoPublicacao")));
    }

    @Test
    void deveRetornar400QuandoGeneroForInvalido() throws Exception {
        String payload = """
                {
                  "titulo": "Clean Code",
                  "autor": "Robert C. Martin",
                  "isbn": "9780132350884",
                  "anoPublicacao": 2008,
                  "genero": "POESIA",
                  "disponivel": true
                }
                """;

        mockMvc.perform(
                        post("/livros")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"))
                .andExpect(jsonPath("$.mensagem")
                        .value(org.hamcrest.Matchers.containsString("genero")));
    }

    // =========================================================
    // GET /livros/{id}
    // =========================================================

    @Test
    void deveBuscarLivroPorId() throws Exception {
        String id = criarLivroERetornarId(requestPadrao());

        mockMvc.perform(get("/livros/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Clean Code"));
    }

    @Test
    void deveRetornar404QuandoLivroNaoExistir() throws Exception {
        mockMvc.perform(get("/livros/{id}", "id-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("LIVRO_NAO_ENCONTRADO"));
    }

    // =========================================================
    // GET /livros
    // =========================================================

    @Test
    void deveListarLivros() throws Exception {
        criarLivroERetornarId(requestPadrao());

        mockMvc.perform(
                        get("/livros")
                                .param("pagina", "0")
                                .param("tamanho", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].titulo").value("Clean Code"))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0));
    }

    @Test
    void deveFiltrarLivrosPorGenero() throws Exception {
        LivroRequest tecnologia = requestPadrao();

        LivroRequest fantasia = new LivroRequest(
                "O Hobbit",
                "J. R. R. Tolkien",
                "9780261102217",
                1937,
                Genero.FANTASIA,
                true
        );

        criarLivroERetornarId(tecnologia);
        criarLivroERetornarId(fantasia);

        mockMvc.perform(
                        get("/livros")
                                .param("genero", "TECNOLOGIA")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].titulo").value("Clean Code"));
    }

    @Test
    void deveRetornar400QuandoPaginaForNegativa() throws Exception {
        mockMvc.perform(
                        get("/livros")
                                .param("pagina", "-1")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"))
                .andExpect(jsonPath("$.mensagem")
                        .value(org.hamcrest.Matchers.containsString("página não pode ser negativa")));
    }

    @Test
    void deveRetornar400QuandoTamanhoForZero() throws Exception {
        mockMvc.perform(
                        get("/livros")
                                .param("tamanho", "0")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));
    }

    @Test
    void deveRetornar400QuandoTamanhoForMaiorQue100() throws Exception {
        mockMvc.perform(
                        get("/livros")
                                .param("tamanho", "101")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));
    }

    @Test
    void deveRetornar400QuandoGeneroForInvalidoNaListagem() throws Exception {
        mockMvc.perform(
                        get("/livros")
                                .param("genero", "POESIA")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));
    }

    // =========================================================
    // PUT /livros/{id}
    // =========================================================

    @Test
    void deveAtualizarLivro() throws Exception {
        String id = criarLivroERetornarId(requestPadrao());

        LivroRequest updateRequest = new LivroRequest(
                "Clean Code - Atualizado",
                "Robert C. Martin",
                "9780132350884",
                2008,
                Genero.TECNOLOGIA,
                false
        );

        mockMvc.perform(
                        put("/livros/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Clean Code - Atualizado"))
                .andExpect(jsonPath("$.disponivel").value(false));
    }

    @Test
    void deveRetornar404AoAtualizarLivroInexistente() throws Exception {
        mockMvc.perform(
                        put("/livros/{id}", "id-inexistente")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestPadrao()))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("LIVRO_NAO_ENCONTRADO"));
    }

    @Test
    void deveRetornar409AoAtualizarComIsbnDeOutroLivro() throws Exception {
        LivroRequest primeiroLivro = requestPadrao();

        LivroRequest segundoLivro = new LivroRequest(
                "O Hobbit",
                "J. R. R. Tolkien",
                "9780261102217",
                1937,
                Genero.FANTASIA,
                true
        );

        criarLivroERetornarId(primeiroLivro);
        String idSegundoLivro = criarLivroERetornarId(segundoLivro);

        LivroRequest updateRequest = new LivroRequest(
                "O Hobbit Atualizado",
                "J. R. R. Tolkien",
                "9780132350884",
                1937,
                Genero.FANTASIA,
                false
        );

        mockMvc.perform(
                        put("/livros/{id}", idSegundoLivro)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ISBN_DUPLICADO"));
    }

    // =========================================================
    // DELETE /livros/{id}
    // =========================================================

    @Test
    void deveExcluirLivro() throws Exception {
        String id = criarLivroERetornarId(requestPadrao());

        mockMvc.perform(delete("/livros/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/livros/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar404AoExcluirLivroInexistente() throws Exception {
        mockMvc.perform(delete("/livros/{id}", "id-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("LIVRO_NAO_ENCONTRADO"));
    }

    // =========================================================
    // CACHE
    // =========================================================

    @Test
    void deveArmazenarLivroNoRedisAoBuscarPorId() throws Exception {
        String id = criarLivroERetornarId(requestPadrao());

        mockMvc.perform(get("/livros/{id}", id)).andExpect(status().isOk());
        mockMvc.perform(get("/livros/{id}", id)).andExpect(status().isOk());

        String cacheKey = "biblioteca:livro:" + id;

        assertTrue(redisTemplate.hasKey(cacheKey));

        Long ttl = redisTemplate.getExpire(cacheKey, java.util.concurrent.TimeUnit.SECONDS);

        assertNotNull(ttl);
        assertTrue(ttl > 0 && ttl <= 600, "TTL deve estar entre 0 e 600 segundos");
    }

    @Test
    void deveInvalidarCacheAoAtualizarLivro() throws Exception {
        String id = criarLivroERetornarId(requestPadrao());

        mockMvc.perform(get("/livros/{id}", id)).andExpect(status().isOk());

        String cacheKey = "biblioteca:livro:" + id;
        assertTrue(redisTemplate.hasKey(cacheKey));

        LivroRequest updateRequest = new LivroRequest(
                "Clean Code Atualizado",
                "Robert C. Martin",
                "9780132350884",
                2008,
                Genero.TECNOLOGIA,
                false
        );

        mockMvc.perform(
                        put("/livros/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest))
                )
                .andExpect(status().isOk());

        assertFalse(redisTemplate.hasKey(cacheKey));
    }

    @Test
    void deveInvalidarCacheAoExcluirLivro() throws Exception {
        String id = criarLivroERetornarId(requestPadrao());

        mockMvc.perform(get("/livros/{id}", id)).andExpect(status().isOk());

        String cacheKey = "biblioteca:livro:" + id;
        assertTrue(redisTemplate.hasKey(cacheKey));

        mockMvc.perform(delete("/livros/{id}", id))
                .andExpect(status().isNoContent());

        assertFalse(redisTemplate.hasKey(cacheKey));
    }

    // =========================================================
    // AUDITORIA
    // =========================================================

    @Test
    void deveAtualizarDataAtualizacaoAoAtualizarLivro() throws Exception {
        String response = mockMvc.perform(
                        post("/livros")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestPadrao()))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dataInclusao").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var jsonCriacao = objectMapper.readTree(response);
        String id = jsonCriacao.get("id").asText();

        LocalDateTime dataInclusao = objectMapper.treeToValue(
                jsonCriacao.get("dataInclusao"),
                LocalDateTime.class
        );

        LivroRequest updateRequest = new LivroRequest(
                "Clean Code Atualizado",
                "Robert C. Martin",
                "9780132350884",
                2008,
                Genero.TECNOLOGIA,
                false
        );

        String updateResponse = mockMvc.perform(
                        put("/livros/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataInclusao").isNotEmpty())
                .andExpect(jsonPath("$.dataAtualizacao").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var jsonAtualizacao = objectMapper.readTree(updateResponse);

        LocalDateTime dataInclusaoAposAtualizacao = objectMapper.treeToValue(
                jsonAtualizacao.get("dataInclusao"),
                LocalDateTime.class
        );

        LocalDateTime dataAtualizacao = objectMapper.treeToValue(
                jsonAtualizacao.get("dataAtualizacao"),
                LocalDateTime.class
        );

        assertEquals(dataInclusao, dataInclusaoAposAtualizacao);
        assertNotNull(dataAtualizacao);
    }

    @Test
    void deveRetornar400QuandoJsonForMalformado() throws Exception {
        String jsonInvalido = "{ \"titulo\": \"Clean Code\", \"autor\": }";

        mockMvc.perform(
                        post("/livros")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonInvalido)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));
    }

    @Test
    void deveRetornarTodosOsErrosDeValidacaoQuandoMultiplosCamposForemInvalidos() throws Exception {
        LivroRequest request = new LivroRequest(
                "",
                "",
                "",
                1000,
                Genero.TECNOLOGIA,
                true
        );

        mockMvc.perform(
                        post("/livros")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"))
                .andExpect(jsonPath("$.mensagem")
                        .value(org.hamcrest.Matchers.containsString("titulo")))
                .andExpect(jsonPath("$.mensagem")
                        .value(org.hamcrest.Matchers.containsString("autor")))
                .andExpect(jsonPath("$.mensagem")
                        .value(org.hamcrest.Matchers.containsString("isbn")));
    }
}