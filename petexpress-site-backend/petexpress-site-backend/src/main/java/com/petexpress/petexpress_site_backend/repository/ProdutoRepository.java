package com.petexpress.petexpress_site_backend.repository;

import com.petexpress.petexpress_site_backend.model.Produto;
import com.petexpress.petexpress_site_backend.model.Produto.Situacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    List<Produto> findBySituacao(Situacao situacao);
    List<Produto> findBySituacaoAndNomeIgnoreCaseOrderByIdAsc(Situacao situacao, String nome);
    List<Produto> findBySituacaoAndTipoAnimalIgnoreCase(Situacao situacao, String tipoAnimal);
    List<Produto> findAllByOrderByIdDesc();
}
