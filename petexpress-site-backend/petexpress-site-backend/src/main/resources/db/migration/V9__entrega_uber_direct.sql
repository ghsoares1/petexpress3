CREATE TABLE entrega_cotacao (
 id VARCHAR(36) PRIMARY KEY,
 usuario_id INTEGER NOT NULL,
 uber_quote_id VARCHAR(100) NOT NULL,
 valor_centavos INTEGER NOT NULL CHECK(valor_centavos >= 0),
 expira_em VARCHAR(40) NOT NULL,
 destino_json TEXT NOT NULL,
 origem_json TEXT NOT NULL,
 origem_nome VARCHAR(100) NOT NULL,
 origem_telefone VARCHAR(30) NOT NULL,
 pedido_id INTEGER
);
CREATE INDEX entrega_cotacao_usuario_idx ON entrega_cotacao(usuario_id);
CREATE TABLE entrega_solicitacao (
 pedido_id INTEGER PRIMARY KEY REFERENCES pedido(id),
 payload TEXT NOT NULL,
 chave VARCHAR(100) NOT NULL UNIQUE,
 entrega_id VARCHAR(100),
 status VARCHAR(50) NOT NULL,
 tracking_url TEXT,
 valor_centavos INTEGER NOT NULL,
 erro VARCHAR(255)
);
ALTER TABLE pedido ADD COLUMN entrega_cotacao_id VARCHAR(36);

