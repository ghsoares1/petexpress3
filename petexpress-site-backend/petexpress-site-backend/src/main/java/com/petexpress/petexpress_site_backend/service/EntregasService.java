package com.petexpress.petexpress_site_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petexpress.petexpress_site_backend.model.*;
import com.petexpress.petexpress_site_backend.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class EntregasService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final UberDirectGateway uber;
    private final PedidoRepository pedidos;
    private final String origemNome;
    private final String origemTelefone;
    private final Endereco origem;

    public EntregasService(JdbcTemplate jdbc, ObjectMapper mapper, UberDirectGateway uber, PedidoRepository pedidos,
                           @Value("${uber.direct.pickup-name:}") String nome,
                           @Value("${uber.direct.pickup-phone:}") String telefone,
                           @Value("${uber.direct.pickup-street:}") String rua,
                           @Value("${uber.direct.pickup-city:}") String cidade,
                           @Value("${uber.direct.pickup-state:}") String estado,
                           @Value("${uber.direct.pickup-cep:}") String cep) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.uber = uber;
        this.pedidos = pedidos;
        this.origemNome = nome;
        this.origemTelefone = telefone;
        this.origem = new Endereco(rua, cidade, estado, cep);
    }

    public boolean configurado() {
        return uber.configurado() && !origemNome.isBlank() && origemTelefone.matches("\\+[1-9][0-9]{9,14}")
                && origem.valido();
    }

    public Cotacao cotar(Destino destino, long usuarioId) {
        if (!configurado()) throw erro(HttpStatus.SERVICE_UNAVAILABLE, "Entrega Uber Direct indisponível. Configure as credenciais e o ponto de coleta.");
        if (destino == null || destino.nome() == null || destino.nome().isBlank() || destino.nome().length() > 100
                || destino.telefone() == null || !destino.telefone().matches("\\+[1-9][0-9]{9,14}")
                || destino.endereco() == null || !destino.endereco().valido()) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe nome, telefone com DDI, rua com número, cidade, UF e CEP completos.");
        }
        return cotar(destino, usuarioId, origem, origemNome, origemTelefone);
    }

    private Cotacao cotar(Destino destino, long usuario, Endereco coleta, String nome, String telefone) {
        UberDirectGateway.Cotacao quote;
        try {
            quote = uber.cotar(Map.of("pickup_address", enderecoJson(coleta), "dropoff_address", enderecoJson(destino.endereco())));
        } catch (RestClientException ex) {
            throw erro(HttpStatus.BAD_GATEWAY, "A Uber não conseguiu cotar este endereço. Confira a cobertura e os dados informados.");
        }
        String moeda = quote.currency_type() == null ? quote.currency() : quote.currency_type();
        if (quote.id() == null || quote.expires() == null || !quote.expires().isAfter(Instant.now())
                || !"BRL".equalsIgnoreCase(moeda) || quote.fee() < 0 || quote.fee() > Integer.MAX_VALUE) {
            throw erro(HttpStatus.BAD_GATEWAY, "A Uber retornou uma cotação inválida para esta loja.");
        }
        String id = UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO entrega_cotacao(id,usuario_id,uber_quote_id,valor_centavos,expira_em,destino_json,origem_json,origem_nome,origem_telefone)
                VALUES(?,?,?,?,?,?,?,?,?)
                """, id, usuario, quote.id(), quote.fee(), quote.expires().toString(), json(destino),
                json(coleta), nome, telefone);
        return new Cotacao(id, BigDecimal.valueOf(quote.fee(), 2), quote.expires(), quote.duration());
    }

    public Registro cotacaoParaCheckout(String id, long usuario) {
        Registro registro = buscarCotacao(id);
        if (registro.usuario() != usuario || registro.pedido() != null || !registro.ate().isAfter(Instant.now().plusSeconds(30))) {
            throw erro(HttpStatus.CONFLICT, "A cotação expirou ou já foi usada. Consulte o frete novamente.");
        }
        return registro;
    }

    public void vincular(String id, long pedido) {
        int atualizados = jdbc.update("UPDATE entrega_cotacao SET pedido_id=? WHERE id=? AND pedido_id IS NULL", pedido, id);
        if (atualizados != 1) throw erro(HttpStatus.CONFLICT, "A cotação já foi usada. Consulte o frete novamente.");
    }

    public Cotacao recotarPedido(long id) {
        Pedido pedido = pedidoPago(id);
        Registro original = buscarCotacao(pedido.getEntregaCotacaoId());
        Cotacao nova = cotar(original.destino(), original.usuario(), original.origem(), original.origemNome(), original.origemTelefone());
        vincular(nova.id(), id);
        return nova;
    }

    public synchronized Resumo solicitar(long pedidoId, Confirmacao confirmacao) {
        if (confirmacao == null || confirmacao.valorConfirmado() == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Confirme a cotação e o valor da entrega.");
        }
        Pedido pedido = pedidoPago(pedidoId);
        if (pedido.getExpedicao() != ExpedicaoStatus.EM_SEPARACAO) {
            throw erro(HttpStatus.CONFLICT, "Inicie a separação do pedido antes de solicitar a entrega.");
        }
        List<Solicitacao> existentes = solicitacoes(pedidoId);
        if (existentes.isEmpty()) {
            Registro quote = buscarCotacao(confirmacao.cotacaoId());
            if (!Objects.equals(quote.pedido(), pedidoId) || quote.valor().compareTo(confirmacao.valorConfirmado()) != 0
                    || !quote.ate().isAfter(Instant.now())) {
                throw erro(HttpStatus.CONFLICT, "A cotação ou o valor mudou. Consulte e confirme novamente.");
            }
            String chave = "petexpress-" + pedido.getReferenciaPagamento() + "-" + quote.id();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("quote_id", quote.uberId());
            payload.put("idempotency_key", chave);
            payload.put("external_id", pedido.getNumeroPedido());
            payload.put("pickup_name", quote.origemNome());
            payload.put("pickup_phone_number", quote.origemTelefone());
            payload.put("pickup_address", enderecoJson(quote.origem()));
            payload.put("dropoff_name", quote.destino().nome());
            payload.put("dropoff_phone_number", quote.destino().telefone());
            payload.put("dropoff_address", enderecoJson(quote.destino().endereco()));
            payload.put("manifest_items", pedido.getItens().stream().map(item -> Map.of(
                    "name", item.getNome(), "quantity", item.getQuantidade(), "size", "large",
                    "price", BigDecimal.valueOf(item.getPrecoUnitario()).movePointRight(2).intValueExact())).toList());
            jdbc.update("""
                    INSERT OR IGNORE INTO entrega_solicitacao(pedido_id,payload,chave,status,valor_centavos)
                    VALUES(?,?,?,'SOLICITANDO',?)
                    """, pedidoId, json(payload), chave, quote.valor().movePointRight(2).intValueExact());
        }
        Solicitacao solicitacao = solicitacoes(pedidoId).get(0);
        if (solicitacao.entregaId() != null) return consultar(pedidoId);
        try {
            UberDirectGateway.Entrega entrega = uber.solicitar(solicitacao.payload());
            atualizar(pedidoId, entrega);
            return resumo(pedidoId);
        } catch (HttpClientErrorException ex) {
            if (Set.of(400,401,403,422).contains(ex.getStatusCode().value())) {
                jdbc.update("DELETE FROM entrega_solicitacao WHERE pedido_id=? AND entrega_id IS NULL", pedidoId);
                throw erro(HttpStatus.BAD_GATEWAY, "A Uber recusou os dados ou a conta. Confira a configuração e consulte uma nova cotação.");
            }
            throw erro(HttpStatus.BAD_GATEWAY, "A Uber ainda não confirmou o resultado. Repita a mesma solicitação para reconciliar.");
        } catch (RestClientException ex) {
            jdbc.update("UPDATE entrega_solicitacao SET status='AGUARDANDO_CONFIRMACAO',erro=? WHERE pedido_id=? AND entrega_id IS NULL",
                    "Sem confirmação da Uber. Consulte ou repita a mesma solicitação para reconciliar.", pedidoId);
            throw erro(HttpStatus.BAD_GATEWAY, "A Uber ainda não confirmou a solicitação. Use a mesma solicitação para consultar o resultado.");
        }
    }

    public Resumo consultar(long pedidoId) {
        List<Solicitacao> lista = solicitacoes(pedidoId);
        if (lista.isEmpty()) return new Resumo(null, null, null, null);
        Solicitacao solicitacao = lista.get(0);
        if (solicitacao.entregaId() != null) {
            try {
                atualizar(pedidoId, uber.consultar(solicitacao.entregaId()));
            } catch (RestClientException ex) {
                throw erro(HttpStatus.BAD_GATEWAY, "Não foi possível atualizar o acompanhamento da Uber.");
            }
        }
        return resumo(pedidoId);
    }

    private void atualizar(long pedido, UberDirectGateway.Entrega entrega) {
        if (entrega.id() == null || entrega.status() == null || !Boolean.TRUE.equals(entrega.live_mode())) {
            throw erro(HttpStatus.BAD_GATEWAY, "A Uber não retornou uma entrega de produção. Confira o ambiente da conta.");
        }
        jdbc.update("UPDATE entrega_solicitacao SET entrega_id=?,status=?,tracking_url=?,erro=NULL WHERE pedido_id=?",
                entrega.id(), entrega.status(), entrega.tracking_url(), pedido);
        if (Set.of("pickup_complete", "dropoff", "delivered").contains(entrega.status())) {
            String expedicao = "delivered".equals(entrega.status()) ? "CONCLUIDO" : "ENVIADO";
            jdbc.update("UPDATE pedido SET expedicao=?,versao=versao+1 WHERE id=? AND status='PAGAMENTO_APROVADO'", expedicao, pedido);
        }
    }

    public Resumo resumo(long pedidoId) {
        return jdbc.query("""
                SELECT entrega_id,status,tracking_url,valor_centavos FROM entrega_solicitacao WHERE pedido_id=?
                """, (rs,n) -> new Resumo(rs.getString("entrega_id"), rs.getString("status"),
                rs.getString("tracking_url"), BigDecimal.valueOf(rs.getLong("valor_centavos"),2)), pedidoId)
                .stream().findFirst().orElse(new Resumo(null,null,null,null));
    }

    private Pedido pedidoPago(long id) {
        Pedido pedido = pedidos.findOneById(id).orElseThrow(() -> erro(HttpStatus.NOT_FOUND,"Pedido não encontrado."));
        if (pedido.getStatus() != PedidoStatus.PAGAMENTO_APROVADO || !"Entrega via Uber".equals(pedido.getFormaEntrega())
                || pedido.getEntregaCotacaoId() == null) {
            throw erro(HttpStatus.CONFLICT, "A solicitação exige um pedido pago com cotação Uber Direct.");
        }
        return pedido;
    }

    private List<Solicitacao> solicitacoes(long pedido) {
        return jdbc.query("SELECT payload,entrega_id FROM entrega_solicitacao WHERE pedido_id=?",
                (rs,n) -> new Solicitacao(rs.getString("payload"),rs.getString("entrega_id")), pedido);
    }

    private Registro buscarCotacao(String id) {
        if (id == null) throw erro(HttpStatus.BAD_REQUEST, "Consulte o frete antes de continuar.");
        return jdbc.query("SELECT * FROM entrega_cotacao WHERE id=?", (rs,n) -> new Registro(
                rs.getString("id"),rs.getLong("usuario_id"),rs.getString("uber_quote_id"),
                BigDecimal.valueOf(rs.getLong("valor_centavos"),2),Instant.parse(rs.getString("expira_em")),
                ler(rs.getString("destino_json"),Destino.class),ler(rs.getString("origem_json"),Endereco.class),
                rs.getString("origem_nome"),rs.getString("origem_telefone"),rs.getObject("pedido_id") == null ? null : rs.getLong("pedido_id")), id)
                .stream().findFirst().orElseThrow(() -> erro(HttpStatus.NOT_FOUND,"Cotação não encontrada."));
    }

    private String enderecoJson(Endereco endereco) {
        return json(Map.of("street_address",List.of(endereco.rua()),"city",endereco.cidade(),"state",endereco.estado(),
                "zip_code",endereco.cep(),"country","BR"));
    }

    private String json(Object dados) {
        try { return mapper.writeValueAsString(dados); }
        catch (Exception ex) { throw new IllegalStateException("Não foi possível preparar os dados da entrega.",ex); }
    }
    private <T> T ler(String dados,Class<T> tipo) {
        try { return mapper.readValue(dados,tipo); }
        catch (Exception ex) { throw new IllegalStateException("Dados de entrega inválidos.",ex); }
    }
    private ResponseStatusException erro(HttpStatus status,String mensagem) { return new ResponseStatusException(status,mensagem); }

    public record Endereco(String rua,String cidade,String estado,String cep) {
        public boolean valido() {
            return rua != null && !rua.isBlank() && rua.length() <= 255 && cidade != null && !cidade.isBlank()
                    && cidade.length() <= 100 && estado != null && estado.matches("[A-Za-z]{2}")
                    && cep != null && cep.matches("[0-9]{5}-?[0-9]{3}");
        }
        public String linha() { return rua + " · " + cidade + " / " + estado + " · CEP " + cep; }
    }
    public record Destino(String nome,String telefone,Endereco endereco) {}
    public record Cotacao(String id,BigDecimal valor,Instant expiraEm,Integer minutos) {}
    public record Registro(String id,long usuario,String uberId,BigDecimal valor,Instant ate,Destino destino,
                           Endereco origem,String origemNome,String origemTelefone,Long pedido) {}
    public record Confirmacao(String cotacaoId,BigDecimal valorConfirmado) {}
    public record Resumo(String id,String status,String trackingUrl,BigDecimal valor) {}
    private record Solicitacao(String payload,String entregaId) {}
}
