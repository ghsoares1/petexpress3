package com.petexpress.petexpress_site_backend.repository;

import com.petexpress.petexpress_site_backend.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByUsuarioIdOrderByDataCriacaoDesc(Long usuarioId);
}
