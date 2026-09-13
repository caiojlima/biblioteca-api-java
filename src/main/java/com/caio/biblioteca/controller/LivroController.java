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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
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
    @Operation(
            summary = "Cadastrar livro",
            description = "Cadastra um novo livro na biblioteca"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Livro cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou ISBN já cadastrado")
    })
    public ResponseEntity<LivroResponse> criar(
            @Valid @RequestBody LivroRequest request
    ) {
        LivroResponse response = livroService.criar(request);

        return ResponseEntity
                .status(201)
                .body(response);
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
    public ResponseEntity<LivroResponse> buscarPorId(
            @PathVariable String id
    ) {
        LivroResponse response = livroService.buscarPorId(id);

        return ResponseEntity.ok(response);
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
    public ResponseEntity<Page<LivroResponse>> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho,
            @RequestParam(required = false) Genero genero
    ) {
        PageRequest pageable = PageRequest.of(pagina, tamanho);

        Page<LivroResponse> response;

        if (genero == null) {
            response = livroService.listar(pageable);
        } else {
            response = livroService.listarPorGenero(genero, pageable);
        }

        return ResponseEntity.ok(response);
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
    public ResponseEntity<LivroResponse> atualizar(
            @PathVariable String id,
            @Valid @RequestBody LivroRequest request
    ) {
        LivroResponse response = livroService.atualizar(id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Excluir livro",
            description = "Remove um livro da biblioteca"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Livro excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado")
    })
    public ResponseEntity<Void> excluir(
            @PathVariable String id
    ) {
        livroService.excluir(id);

        return ResponseEntity.noContent().build();
    }
}
