ALTER TABLE item_pedido ADD COLUMN categoria VARCHAR(100);
ALTER TABLE pedido ADD COLUMN forma_pagamento VARCHAR(60);
CREATE TABLE loja_configuracao (
 id INTEGER PRIMARY KEY CHECK(id=1),
 nome VARCHAR(100) NOT NULL,
 telefone VARCHAR(30) NOT NULL,
 endereco VARCHAR(255) NOT NULL
);
INSERT INTO loja_configuracao(id,nome,telefone,endereco) VALUES(1,'Pet Shop do Bairro','','');
