package com.petexpress.petexpress_site_backend.repository;

import com.petexpress.petexpress_site_backend.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByTipoAnimalIgnoreCase(String tipoAnimal);

}
