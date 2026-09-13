package com.caio.biblioteca.controller;

import com.caio.biblioteca.dto.request.LivroRequest;
import com.caio.biblioteca.enums.Genero;
import com.caio.biblioteca.repository.LivroRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.redis.testcontainers.RedisContainer;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LivroControllerIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer =
            new MongoDBContainer("mongo:8");

    @Container
    static RedisContainer redisContainer =
            new RedisContainer("redis:8");

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
        registry.add(
                "spring.data.mongodb.uri",
                mongoDBContainer::getReplicaSetUrl
        );

        registry.add(
                "spring.data.redis.host",
                redisContainer::getHost
        );

        registry.add(
                "spring.data.redis.port",
                () -> redisContainer.getMappedPort(6379)
        );
    }

    @BeforeEach
    void limparBanco() {
        livroRepository.deleteAll();

        Set<String> keys = redisTemplate.keys("biblioteca:livro:*");

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
    @Test
    void deveCadastrarLivro() throws Exception {

        LivroRequest request = new LivroRequest(
                "Clean Code",
                "Robert C. Martin",
                "9780132350884",
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
                .andExpect(jsonPath("$.titulo").value("Clean Code"))
                .andExpect(jsonPath("$.autor").value("Robert C. Martin"))
                .andExpect(jsonPath("$.isbn").value("9780132350884"))
                .andExpect(jsonPath("$.genero").value("TECNOLOGIA"))
                .andExpect(jsonPath("$.disponivel").value(true));
    }

    @Test
    void deveBuscarLivroPorId() throws Exception {

        LivroRequest request = new LivroRequest(
                "Clean Code",
                "Robert C. Martin",
                "9780132350884",
                2008,
                Genero.TECNOLOGIA,
                true
        );

        String response = mockMvc.perform(
                        post("/livros")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = objectMapper
                .readTree(response)
                .get("id")
                .asText();

        mockMvc.perform(get("/livros/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Clean Code"));
    }

    @Test
    void deveListarLivros() throws Exception {

        LivroRequest request = new LivroRequest(
                "Clean Code",
                "Robert C. Martin",
                "9780132350884",
                2008,
                Genero.TECNOLOGIA,
                true
        );

        mockMvc.perform(
                post("/livros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );

        mockMvc.perform(
                        get("/livros")
                                .param("pagina", "0")
                                .param("tamanho", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].titulo")
                        .value("Clean Code"))
                .andExpect(jsonPath("$.pageable.pageNumber")
                        .value(0));
    }

    @Test
    void deveAtualizarLivro() throws Exception {

        LivroRequest request = new LivroRequest(
                "Clean Code",
                "Robert C. Martin",
                "9780132350884",
                2008,
                Genero.TECNOLOGIA,
                true
        );

        String response = mockMvc.perform(
                        post("/livros")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = objectMapper
                .readTree(response)
                .get("id")
                .asText();

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
                .andExpect(jsonPath("$.titulo")
                        .value("Clean Code - Atualizado"))
                .andExpect(jsonPath("$.disponivel")
                        .value(false));
    }

    @Test
    void deveExcluirLivro() throws Exception {

        LivroRequest request = new LivroRequest(
                "Clean Code",
                "Robert C. Martin",
                "9780132350884",
                2008,
                Genero.TECNOLOGIA,
                true
        );

        String response = mockMvc.perform(
                        post("/livros")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = objectMapper
                .readTree(response)
                .get("id")
                .asText();

        mockMvc.perform(delete("/livros/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/livros/{id}", id))
                .andExpect(status().isNotFound());
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
                .andExpect(jsonPath("$.codigo")
                        .value("DADOS_INVALIDOS"));
    }

    @Test
    void deveRetornar404QuandoLivroNaoExistir() throws Exception {

        mockMvc.perform(get("/livros/{id}", "id-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo")
                        .value("LIVRO_NAO_ENCONTRADO"));
    }

    @Test
    void deveFiltrarLivrosPorGenero() throws Exception {

        LivroRequest tecnologia = new LivroRequest(
                "Clean Code",
                "Robert C. Martin",
                "9780132350884",
                2008,
                Genero.TECNOLOGIA,
                true
        );

        LivroRequest fantasia = new LivroRequest(
                "O Hobbit",
                "J. R. R. Tolkien",
                "9780261102217",
                1937,
                Genero.FANTASIA,
                true
        );

        mockMvc.perform(
                post("/livros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tecnologia))
        );

        mockMvc.perform(
                post("/livros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fantasia))
        );

        mockMvc.perform(
                        get("/livros")
                                .param("genero", "TECNOLOGIA")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].titulo")
                        .value("Clean Code"));
    }

    @Test
    void deveRetornar400QuandoIsbnJaEstiverCadastrado() throws Exception {
        LivroRequest primeiroLivro = new LivroRequest(
                "Clean Code",
                "Robert C. Martin",
                "9780132350884",
                2008,
                Genero.TECNOLOGIA,
                true
        );

        LivroRequest segundoLivro = new LivroRequest(
                "Outro Livro",
                "Outro Autor",
                "9780132350884",
                2020,
                Genero.TECNOLOGIA,
                true
        );

        mockMvc.perform(
                post("/livros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(primeiroLivro))
        ).andExpect(status().isCreated());

        mockMvc.perform(
                        post("/livros")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(segundoLivro))
                )
                .andExpect(status().isBadRequest())
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
                .andExpect(jsonPath("$.codigo").value("ANO_PUBLICACAO_INVALIDO"));
    }

    @Test
    void deveArmazenarLivroNoRedisAoBuscarPorId() throws Exception {
        LivroRequest request = new LivroRequest(
                "Clean Code",
                "Robert C. Martin",
                "9780132350884",
                2008,
                Genero.TECNOLOGIA,
                true
        );

        String response = mockMvc.perform(
                        post("/livros")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = objectMapper
                .readTree(response)
                .get("id")
                .asText();

        mockMvc.perform(get("/livros/{id}", id))
                .andExpect(status().isOk());

        String cacheKey = "biblioteca:livro:" + id;

        Long ttl = redisTemplate.getExpire(
                cacheKey,
                java.util.concurrent.TimeUnit.SECONDS
        );

        assertTrue(
                redisTemplate.hasKey(cacheKey)
        );

        assertTrue(
                ttl > 0 && ttl <= 600
        );
    }

    @Test
    void deveInvalidarCacheAoAtualizarLivro() throws Exception {
        LivroRequest request = new LivroRequest(
                "Clean Code",
                "Robert C. Martin",
                "9780132350884",
                2008,
                Genero.TECNOLOGIA,
                true
        );

        String response = mockMvc.perform(
                        post("/livros")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = objectMapper
                .readTree(response)
                .get("id")
                .asText();

        mockMvc.perform(get("/livros/{id}", id))
                .andExpect(status().isOk());

        String cacheKey = "biblioteca:livro:" + id;

        assertTrue(
                redisTemplate.hasKey(cacheKey)
        );

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

        assertFalse(
                redisTemplate.hasKey(cacheKey)
        );
    }

    @Test
    void deveInvalidarCacheAoExcluirLivro() throws Exception {
        LivroRequest request = new LivroRequest(
                "Clean Code",
                "Robert C. Martin",
                "9780132350884",
                2008,
                Genero.TECNOLOGIA,
                true
        );

        String response = mockMvc.perform(
                        post("/livros")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = objectMapper
                .readTree(response)
                .get("id")
                .asText();

        mockMvc.perform(get("/livros/{id}", id))
                .andExpect(status().isOk());

        String cacheKey = "biblioteca:livro:" + id;

        assertTrue(
                redisTemplate.hasKey(cacheKey)
        );

        mockMvc.perform(delete("/livros/{id}", id))
                .andExpect(status().isNoContent());

        assertFalse(
                redisTemplate.hasKey(cacheKey)
        );
    }
    @Test
    void deveRetornar404AoAtualizarLivroInexistente() throws Exception {
        LivroRequest request = new LivroRequest(
                "Clean Code",
                "Robert C. Martin",
                "9780132350884",
                2008,
                Genero.TECNOLOGIA,
                true
        );

        mockMvc.perform(
                        put("/livros/{id}", "id-inexistente")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo")
                        .value("LIVRO_NAO_ENCONTRADO"));
    }

    @Test
    void deveRetornar400AoAtualizarComIsbnDeOutroLivro() throws Exception {
        LivroRequest primeiroLivro = new LivroRequest(
                "Clean Code",
                "Robert C. Martin",
                "9780132350884",
                2008,
                Genero.TECNOLOGIA,
                true
        );

        LivroRequest segundoLivro = new LivroRequest(
                "O Hobbit",
                "J. R. R. Tolkien",
                "9780261102217",
                1937,
                Genero.FANTASIA,
                true
        );

        mockMvc.perform(
                post("/livros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(primeiroLivro))
        ).andExpect(status().isCreated());

        String response = mockMvc.perform(
                        post("/livros")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(segundoLivro))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String idSegundoLivro = objectMapper
                .readTree(response)
                .get("id")
                .asText();

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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo")
                        .value("ISBN_DUPLICADO"));
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
                .andExpect(jsonPath("$.codigo")
                        .value("ANO_PUBLICACAO_INVALIDO"));
    }

    @Test
    void deveAtualizarDataAtualizacaoAoAtualizarLivro() throws Exception {
        LivroRequest request = new LivroRequest(
                "Clean Code",
                "Robert C. Martin",
                "9780132350884",
                2008,
                Genero.TECNOLOGIA,
                true
        );

        String response = mockMvc.perform(
                        post("/livros")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dataInclusao").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var jsonCriacao = objectMapper.readTree(response);

        String id = jsonCriacao.get("id").asText();

        LocalDateTime dataInclusao =
                objectMapper.treeToValue(
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

        LocalDateTime dataInclusaoAposAtualizacao =
                objectMapper.treeToValue(
                        jsonCriacao.get("dataInclusao"),
                        LocalDateTime.class
                );
        LocalDateTime dataAtualizacao =
                objectMapper.treeToValue(
                        jsonCriacao.get("dataAtualizacao"),
                        LocalDateTime.class
                );
        assertEquals(
                dataInclusao,
                dataInclusaoAposAtualizacao
        );

        assertNotNull(
                dataAtualizacao
        );
    }
}