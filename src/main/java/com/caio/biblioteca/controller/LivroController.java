package com.caio.biblioteca.controller;

import com.caio.biblioteca.dto.request.LivroRequest;
import com.caio.biblioteca.dto.response.LivroResponse;
import com.caio.biblioteca.enums.Genero;
import com.caio.biblioteca.service.LivroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/livros")
@Tag(name = "Livros", description = "Operações de gerenciamento de livros")
public class LivroController {

    private final LivroService livroService;

    public LivroController(LivroService livroService) {
        this.livroService = livroService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cadastrar livro",
            description = "Cadastra um novo livro na biblioteca"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Livro cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou ISBN já cadastrado")
    })
    public LivroResponse criar(@Valid @RequestBody LivroRequest request) {
        return livroService.criar(request);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar livro por ID",
            description = "Busca um livro pelo seu identificador"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Livro encontrado"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado")
    })
    public LivroResponse buscarPorId(@PathVariable String id) {
        return livroService.buscarPorId(id);
    }

    @GetMapping
    @Operation(
            summary = "Listar livros",
            description = "Lista livros de forma paginada, podendo filtrar por gênero"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos")
    })
    public Page<LivroResponse> listar(
            @PageableDefault(size = 10, sort = "titulo") Pageable pageable,
            @RequestParam(required = false) Genero genero
    ) {
        return (genero == null)
                ? livroService.listar(pageable)
                : livroService.listarPorGenero(genero, pageable);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar livro",
            description = "Atualiza os dados de um livro existente"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Livro atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou ISBN já cadastrado"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado")
    })
    public LivroResponse atualizar(
            @PathVariable String id,
            @Valid @RequestBody LivroRequest request
    ) {
        return livroService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Excluir livro",
            description = "Remove um livro da biblioteca"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Livro excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado")
    })
    public void excluir(@PathVariable String id) {
        livroService.excluir(id);
    }
}