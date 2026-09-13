package com.caio.biblioteca.repository;

import com.caio.biblioteca.entity.Livro;
import com.caio.biblioteca.enums.Genero;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LivroRepository extends MongoRepository<Livro, String> {

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, String id);

    Page<Livro> findByGenero(Genero genero, Pageable pageable);
}