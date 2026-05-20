CREATE TABLE IF NOT EXISTS produto (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(255),
    tipo_produto VARCHAR(255),
    tipo_animal VARCHAR(255),
    preco DOUBLE,
    codigo_barras VARCHAR(255),
    descricao VARCHAR(255),
    imagem VARCHAR(255)
);

INSERT INTO produto (nome, tipo_produto, tipo_animal, preco, imagem) VALUES
('Premier Pet Formula Adultos Porte Grande', 'Premier', 'CACHORRO', 179.90, 'img/1.jpg'),
('Golden Mega Adultos Raças Grandes', 'Golden', 'CACHORRO', 161.99, 'img/2.jpg'),
('Golden Mega Filhotes Porte Grande', 'Golden', 'CACHORRO', 161.91, 'img/3.jpg'),
('Premier Pet Formula Adultos Porte Pequeno', 'Premier', 'CACHORRO', 67.90, 'img/4.png'),
('Golden Mini Bits Adultos Pequeno Porte', 'Golden', 'CACHORRO', 65.99, 'img/5.jpg'),
('Golden Filhotes Pequeno Porte', 'Golden', 'CACHORRO', 72.90, 'img/6.jpg'),
('Whiskas Adulto Carne', 'Whiskas', 'GATO', 29.90, 'img/c1.png'),
('Golden Gatos Castrados', 'Royal Canin', 'GATO', 64.90, 'img/c2.jpg'),
('Premier Gatos Filhotes Frango', 'Whiskas', 'GATO', 89.90, 'img/c3.png'),
('GranPlus Gatos Adultos Carne & Arroz', 'Royal Canin', 'GATO', 72.50, 'img/c4.jpg'),
('Cat Chow Naturalis Frango', 'Whiskas', 'GATO', 55.90, 'img/c5.jpg'),
('Special Cat Prime Salmão', 'Baw Waw', 'GATO', 59.90, 'img/c6.jpg');
