CREATE TABLE IF NOT EXISTS usuario (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    senha VARCHAR(255),
    cpf VARCHAR(14) UNIQUE,
    role VARCHAR(50) DEFAULT 'USER'
);

CREATE TABLE IF NOT EXISTS pedido (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    numero_pedido VARCHAR(100) UNIQUE,
    total DOUBLE,
    status VARCHAR(50),
    preference_id VARCHAR(255),
    payment_id VARCHAR(255),
    data_criacao DATETIME,
    data_atualizacao DATETIME,
    forma_entrega VARCHAR(100),
    codigo_entrega VARCHAR(100),
    taxa_entrega DOUBLE,
    usuario_id INTEGER,
    FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS item_pedido (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    produto_id VARCHAR(255),
    nome VARCHAR(255),
    preco_unitario DOUBLE,
    quantidade INTEGER,
    imagem VARCHAR(255),
    pedido_id INTEGER,
    FOREIGN KEY (pedido_id) REFERENCES pedido(id)
);
