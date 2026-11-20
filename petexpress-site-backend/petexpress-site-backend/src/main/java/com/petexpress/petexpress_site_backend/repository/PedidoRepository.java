package com.petexpress.petexpress_site_backend.repository;

import com.petexpress.petexpress_site_backend.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.petexpress.petexpress_site_backend.model.PedidoStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    @EntityGraph(attributePaths = {"itens"})
    Optional<Pedido> findOneById(Long id);

    List<Pedido> findByUsuarioIdOrderByDataCriacaoDesc(Long usuarioId);
    List<Pedido> findAllByOrderByDataCriacaoDesc();
    Optional<Pedido> findByUsuarioIdAndChaveCheckout(Long usuarioId, String chaveCheckout);
    Optional<Pedido> findByReferenciaPagamento(String referenciaPagamento);
    List<Pedido> findByStatusAndReservaAteBeforeOrderByReservaAteAsc(PedidoStatus status, Instant limite);
}
