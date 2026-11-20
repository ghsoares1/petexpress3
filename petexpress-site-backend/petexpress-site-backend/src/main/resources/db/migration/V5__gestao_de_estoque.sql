ALTER TABLE produto ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
ALTER TABLE produto_variacao ADD COLUMN estoque_minimo INTEGER NOT NULL DEFAULT 0 CHECK (estoque_minimo >= 0);

CREATE TABLE estoque_movimentacao (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    produto_id INTEGER NOT NULL,
    variacao_codigo VARCHAR(36) NOT NULL,
    produto_nome VARCHAR(255) NOT NULL,
    embalagem VARCHAR(100) NOT NULL,
    vendedor_id INTEGER NOT NULL,
    vendedor_nome VARCHAR(511) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    motivo VARCHAR(255) NOT NULL,
    saldo_anterior INTEGER NOT NULL CHECK (saldo_anterior >= 0),
    saldo_atual INTEGER NOT NULL CHECK (saldo_atual >= 0),
    registrado_em VARCHAR(40) NOT NULL
);

CREATE INDEX estoque_movimentacao_variacao_idx ON estoque_movimentacao (variacao_codigo, id);
