ALTER TABLE produto ADD COLUMN categoria VARCHAR(100);
ALTER TABLE produto ADD COLUMN fotos TEXT;
ALTER TABLE produto ADD COLUMN situacao VARCHAR(16) NOT NULL DEFAULT 'PUBLICADO';

UPDATE produto SET categoria = 'Ração', fotos = imagem;

CREATE TABLE produto_variacao (
    produto_id INTEGER NOT NULL REFERENCES produto(id),
    posicao INTEGER NOT NULL,
    codigo VARCHAR(36) NOT NULL,
    embalagem VARCHAR(100) NOT NULL,
    preco NUMERIC(8, 2) NOT NULL CHECK (preco > 0),
    estoque INTEGER NOT NULL CHECK (estoque >= 0),
    PRIMARY KEY (produto_id, posicao)
);

INSERT INTO produto (nome, tipo_produto, tipo_animal, preco, imagem, fotos, categoria)
SELECT 'Whiskas Adulto Carne', 'Whiskas', 'GATO', '29.9', 'img/c1.png', 'img/c1.png', 'Adultos'
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE tipo_animal = 'GATO' AND (lower(nome) = lower('Whiskas Adulto Carne') OR imagem = 'img/c1.png'));

INSERT INTO produto (nome, tipo_produto, tipo_animal, preco, imagem, fotos, categoria)
SELECT 'Golden Gatos Castrados', 'Golden', 'GATO', '64.9', 'img/c2.jpg', 'img/c2.jpg', 'Castrados'
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE tipo_animal = 'GATO' AND (lower(nome) = lower('Golden Gatos Castrados') OR imagem = 'img/c2.jpg'));

INSERT INTO produto (nome, tipo_produto, tipo_animal, preco, imagem, fotos, categoria)
SELECT 'Premier Gatos Filhotes Frango', 'Premier', 'GATO', '89.9', 'img/c3.png', 'img/c3.png', 'Filhotes'
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE tipo_animal = 'GATO' AND (lower(nome) = lower('Premier Gatos Filhotes Frango') OR imagem = 'img/c3.png'));

INSERT INTO produto (nome, tipo_produto, tipo_animal, preco, imagem, fotos, categoria)
SELECT 'GranPlus Gatos Adultos Carne e Arroz', 'GranPlus', 'GATO', '72.5', 'img/c4.jpg', 'img/c4.jpg', 'Adultos'
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE tipo_animal = 'GATO' AND (lower(nome) = lower('GranPlus Gatos Adultos Carne e Arroz') OR imagem = 'img/c4.jpg'));

INSERT INTO produto (nome, tipo_produto, tipo_animal, preco, imagem, fotos, categoria)
SELECT 'Cat Chow Naturalis Frango', 'Cat Chow', 'GATO', '55.9', 'img/c5.jpg', 'img/c5.jpg', 'Natural'
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE tipo_animal = 'GATO' AND (lower(nome) = lower('Cat Chow Naturalis Frango') OR imagem = 'img/c5.jpg'));

INSERT INTO produto (nome, tipo_produto, tipo_animal, preco, imagem, fotos, categoria)
SELECT 'Special Cat Prime Salmão', 'Special Cat', 'GATO', '59.9', 'img/c6.jpg', 'img/c6.jpg', 'Premium'
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE tipo_animal = 'GATO' AND (lower(nome) = lower('Special Cat Prime Salmão') OR imagem = 'img/c6.jpg'));

INSERT INTO produto (nome, tipo_produto, tipo_animal, preco, imagem, fotos, categoria)
SELECT 'Racao para Calopsita e Periquito PM13 Megazoo 300 g', 'Megazoo', 'PASSARO', '39.9', 'img/passaro-megazoo-pm13.jpg', 'img/passaro-megazoo-pm13.jpg', 'Racao extrusada'
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE tipo_animal = 'PASSARO' AND (lower(nome) = lower('Racao para Calopsita e Periquito PM13 Megazoo 300 g') OR imagem = 'img/passaro-megazoo-pm13.jpg'));

INSERT INTO produto (nome, tipo_produto, tipo_animal, preco, imagem, fotos, categoria)
SELECT 'Racao Nutropica Extrusado Natural para Calopsitas 300 g', 'Nutropica', 'PASSARO', '45.9', 'img/passaro-nutropica.jpg', 'img/passaro-nutropica.jpg', 'Calopsitas'
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE tipo_animal = 'PASSARO' AND (lower(nome) = lower('Racao Nutropica Extrusado Natural para Calopsitas 300 g') OR imagem = 'img/passaro-nutropica.jpg'));

INSERT INTO produto (nome, tipo_produto, tipo_animal, preco, imagem, fotos, categoria)
SELECT 'Mistura para Calopsita, Agapornis e Rose Faces Nutripassaros 500 g', 'Nutripassaros', 'PASSARO', '22.9', 'img/passaro-nutripassaros.jpg', 'img/passaro-nutripassaros.jpg', 'Mistura premium'
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE tipo_animal = 'PASSARO' AND (lower(nome) = lower('Mistura para Calopsita, Agapornis e Rose Faces Nutripassaros 500 g') OR imagem = 'img/passaro-nutripassaros.jpg'));

INSERT INTO produto (nome, tipo_produto, tipo_animal, preco, imagem, fotos, categoria)
SELECT 'Racao Extrusada para Calopsita Zootekna 350 g', 'Zootekna', 'PASSARO', '18.9', 'img/passaro-zootekna-extrusada.jpg', 'img/passaro-zootekna-extrusada.jpg', 'Calopsitas'
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE tipo_animal = 'PASSARO' AND (lower(nome) = lower('Racao Extrusada para Calopsita Zootekna 350 g') OR imagem = 'img/passaro-zootekna-extrusada.jpg'));

INSERT INTO produto (nome, tipo_produto, tipo_animal, preco, imagem, fotos, categoria)
SELECT 'Farinhada Super Premium Tropical Bird Calopsita Zootekna 400 g', 'Zootekna', 'PASSARO', '29.9', 'img/passaro-zootekna-farinhada.jpg', 'img/passaro-zootekna-farinhada.jpg', 'Farinhada'
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE tipo_animal = 'PASSARO' AND (lower(nome) = lower('Farinhada Super Premium Tropical Bird Calopsita Zootekna 400 g') OR imagem = 'img/passaro-zootekna-farinhada.jpg'));

INSERT INTO produto (nome, tipo_produto, tipo_animal, preco, imagem, fotos, categoria)
SELECT 'Racao Extrusada para Papagaio Regular Bits Megazoo 600 g', 'Megazoo', 'PASSARO', '69.9', 'img/passaro-megazoo-papagaio.jpg', 'img/passaro-megazoo-papagaio.jpg', 'Papagaios'
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE tipo_animal = 'PASSARO' AND (lower(nome) = lower('Racao Extrusada para Papagaio Regular Bits Megazoo 600 g') OR imagem = 'img/passaro-megazoo-papagaio.jpg'));

INSERT INTO produto (nome, tipo_produto, tipo_animal, preco, imagem, fotos, categoria)
SELECT 'Racao Mistura com Frutas Passaro Forte 5 kg', 'Passaro Forte', 'PASSARO', '89.9', 'img/passaro-forte-5kg.jpg', 'img/passaro-forte-5kg.jpg', 'Mistura com frutas'
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE tipo_animal = 'PASSARO' AND (lower(nome) = lower('Racao Mistura com Frutas Passaro Forte 5 kg') OR imagem = 'img/passaro-forte-5kg.jpg'));

INSERT INTO produto (nome, tipo_produto, tipo_animal, preco, imagem, fotos, categoria)
SELECT 'Racao Basica Nutrafish Nutravit 150 g', 'Nutrafish', 'PEIXE', '32.9', 'img/peixe-nutrafish.jpg', 'img/peixe-nutrafish.jpg', 'Flocos'
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE tipo_animal = 'PEIXE' AND (lower(nome) = lower('Racao Basica Nutrafish Nutravit 150 g') OR imagem = 'img/peixe-nutrafish.jpg'));

INSERT INTO produto (nome, tipo_produto, tipo_animal, preco, imagem, fotos, categoria)
SELECT 'Racao em Flocos para Peixes Basic Alcon 50 g', 'Alcon', 'PEIXE', '14.9', 'img/peixe-alcon-basic.jpg', 'img/peixe-alcon-basic.jpg', 'Flocos'
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE tipo_animal = 'PEIXE' AND (lower(nome) = lower('Racao em Flocos para Peixes Basic Alcon 50 g') OR imagem = 'img/peixe-alcon-basic.jpg'));

INSERT INTO produto (nome, tipo_produto, tipo_animal, preco, imagem, fotos, categoria)
SELECT 'Racao para Peixes Colours Alcon 10 g', 'Alcon', 'PEIXE', '8.9', 'img/peixe-alcon-colours.jpg', 'img/peixe-alcon-colours.jpg', 'Realca cores'
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE tipo_animal = 'PEIXE' AND (lower(nome) = lower('Racao para Peixes Colours Alcon 10 g') OR imagem = 'img/peixe-alcon-colours.jpg'));

INSERT INTO produto (nome, tipo_produto, tipo_animal, preco, imagem, fotos, categoria)
SELECT 'Racao Bettamix Flakes Peixe Betta Alcon 10 g', 'Alcon', 'PEIXE', '9.9', 'img/peixe-alcon-betta.jpg', 'img/peixe-alcon-betta.jpg', 'Betta'
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE tipo_animal = 'PEIXE' AND (lower(nome) = lower('Racao Bettamix Flakes Peixe Betta Alcon 10 g') OR imagem = 'img/peixe-alcon-betta.jpg'));

INSERT INTO produto (nome, tipo_produto, tipo_animal, preco, imagem, fotos, categoria)
SELECT 'Racao em flocos para Peixes Guppy Alcon 10 g', 'Alcon', 'PEIXE', '11.9', 'img/peixe-alcon-guppy.jpg', 'img/peixe-alcon-guppy.jpg', 'Guppy'
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE tipo_animal = 'PEIXE' AND (lower(nome) = lower('Racao em flocos para Peixes Guppy Alcon 10 g') OR imagem = 'img/peixe-alcon-guppy.jpg'));

INSERT INTO produto (nome, tipo_produto, tipo_animal, preco, imagem, fotos, categoria)
SELECT 'Racao Granulada Neon para Peixes Alcon 30 g', 'Alcon', 'PEIXE', '15.9', 'img/peixe-alcon-neon.jpg', 'img/peixe-alcon-neon.jpg', 'Granulada'
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE tipo_animal = 'PEIXE' AND (lower(nome) = lower('Racao Granulada Neon para Peixes Alcon 30 g') OR imagem = 'img/peixe-alcon-neon.jpg'));

INSERT INTO produto (nome, tipo_produto, tipo_animal, preco, imagem, fotos, categoria)
SELECT 'Racao Flutuante GoldFish Colour Alcon 100 g', 'Alcon', 'PEIXE', '22.9', 'img/peixe-alcon-goldfish.jpg', 'img/peixe-alcon-goldfish.jpg', 'Goldfish'
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE tipo_animal = 'PEIXE' AND (lower(nome) = lower('Racao Flutuante GoldFish Colour Alcon 100 g') OR imagem = 'img/peixe-alcon-goldfish.jpg'));

