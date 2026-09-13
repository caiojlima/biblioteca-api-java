package com.caio.biblioteca.service;

import com.caio.biblioteca.dto.request.LivroRequest;
import com.caio.biblioteca.dto.response.LivroResponse;
import com.caio.biblioteca.entity.Livro;
import com.caio.biblioteca.enums.Genero;
import com.caio.biblioteca.exception.NegocioException;
import com.caio.biblioteca.mapper.LivroMapper;
import com.caio.biblioteca.repository.LivroRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Year;

@Slf4j
@Service
public class LivroService {

    private final LivroRepository livroRepository;
    private final LivroMapper livroMapper;

    public LivroService(
            LivroRepository livroRepository,
            LivroMapper livroMapper
    ) {
        this.livroRepository = livroRepository;
        this.livroMapper = livroMapper;
    }

    public LivroResponse criar(LivroRequest request) {
        log.info("Criando livro com ISBN={}", request.isbn());

        validarAnoPublicacao(request.anoPublicacao());
        validarIsbnUnico(request.isbn());

        Livro livro = livroMapper.toEntity(request);
        Livro livroSalvo = livroRepository.save(livro);

        log.info("Livro criado com sucesso. id={}, ISBN={}", livroSalvo.getId(), livroSalvo.getIsbn());
        return livroMapper.toResponse(livroSalvo);
    }

    @Cacheable(cacheNames = "livro", key = "#id")
    public LivroResponse buscarPorId(String id) {
        log.debug("Buscando livro por id={} (cache miss)", id);

        Livro livro = buscarLivroOuLancar(id);

        log.debug("Livro encontrado no MongoDB. id={}", id);
        return livroMapper.toResponse(livro);
    }

    public Page<LivroResponse> listar(Genero genero, Pageable pageable) {
        log.debug("Listando livros. genero={}, pagina={}, tamanho={}",
                genero, pageable.getPageNumber(), pageable.getPageSize());

        Page<Livro> pagina = (genero == null)
                ? livroRepository.findAll(pageable)
                : livroRepository.findByGenero(genero, pageable);

        log.debug("Listagem retornou {} elementos de {} no total",
                pagina.getNumberOfElements(), pagina.getTotalElements());

        return pagina.map(livroMapper::toResponse);
    }

    @CacheEvict(cacheNames = "livro", key = "#id")
    public LivroResponse atualizar(String id, LivroRequest request) {
        log.info("Atualizando livro id={}", id);

        Livro livro = buscarLivroOuLancar(id);

        validarAnoPublicacao(request.anoPublicacao());
        validarIsbnUnicoNaAtualizacao(request.isbn(), id);

        livroMapper.updateEntity(request, livro);
        Livro livroAtualizado = livroRepository.save(livro);

        log.info("Livro atualizado com sucesso. id={}", livroAtualizado.getId());
        log.debug("Cache invalidado para id={}", id);

        return livroMapper.toResponse(livroAtualizado);
    }

    @CacheEvict(cacheNames = "livro", key = "#id")
    public void excluir(String id) {
        log.info("Excluindo livro id={}", id);

        Livro livro = buscarLivroOuLancar(id);
        livroRepository.delete(livro);

        log.info("Livro excluído com sucesso. id={}", id);
        log.debug("Cache invalidado para id={}", id);
    }

    private Livro buscarLivroOuLancar(String id) {
        return livroRepository.findById(id)
                .orElseThrow(() -> livroNaoEncontrado(id));
    }

    private void validarIsbnUnico(String isbn) {
        if (livroRepository.existsByIsbn(isbn)) {
            log.warn("Tentativa de cadastro com ISBN duplicado. ISBN={}", isbn);
            throw isbnDuplicado(isbn);
        }
    }

    private void validarIsbnUnicoNaAtualizacao(String isbn, String id) {
        if (livroRepository.existsByIsbnAndIdNot(isbn, id)) {
            log.warn("Tentativa de atualização com ISBN duplicado. id={}, ISBN={}", id, isbn);
            throw isbnDuplicado(isbn);
        }
    }

    private void validarAnoPublicacao(Integer anoPublicacao) {
        int anoAtual = Year.now().getValue();

        if (anoPublicacao == null || anoPublicacao <= 1000 || anoPublicacao > anoAtual) {
            log.warn("Ano de publicação inválido. valor={}, anoAtual={}", anoPublicacao, anoAtual);
            throw new NegocioException(
                    "ANO_PUBLICACAO_INVALIDO",
                    "O ano de publicação deve ser maior que 1000 e menor ou igual a "
                            + anoAtual + ".",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private NegocioException livroNaoEncontrado(String id) {
        log.warn("Livro não encontrado. id={}", id);
        return new NegocioException(
                "LIVRO_NAO_ENCONTRADO",
                "Livro com id '" + id + "' não encontrado.",
                HttpStatus.NOT_FOUND
        );
    }

    private NegocioException isbnDuplicado(String isbn) {
        return new NegocioException(
                "ISBN_DUPLICADO",
                "Já existe um livro cadastrado com o ISBN '" + isbn + "'.",
                HttpStatus.CONFLICT
        );
    }
}