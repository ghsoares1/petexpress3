# Deploy da PetExpress

Este projeto tem duas partes:

- Frontend estatico: arquivos `.html`, `.css`, `.js` e `img/` na raiz.
- Backend Spring Boot: `petexpress-site-backend/petexpress-site-backend`.

## 1. Subir no GitHub

1. Crie um repositorio no GitHub.
2. Envie esta pasta inteira para o repositorio.
3. Confirme que `render.yaml`, `config.js` e a pasta `petexpress-site-backend/` foram enviados.

## 2. Criar o deploy no Render

1. Entre em `https://dashboard.render.com`.
2. Clique em **New** > **Blueprint**.
3. Conecte o repositorio do GitHub.
4. O Render deve encontrar o arquivo `render.yaml`.
5. Clique para criar os dois servicos:
   - `petexpress-site-backend`
   - `petexpress-site`

O backend Spring Boot sobe como Docker, porque o Render atualmente recomenda Docker para projetos Java/JVM. O Dockerfile usado fica em `petexpress-site-backend/petexpress-site-backend/dockerfile`.

O frontend publica uma pasta `dist` criada no build com apenas HTML/CSS/JS/imagens. Assim o codigo do backend nao fica exposto como arquivo estatico.

## 3. Variaveis do backend

No servico `petexpress-site-backend`, configure:

```text
MERCADOPAGO_ACCESS_TOKEN=seu_access_token_do_mercado_pago
MERCADOPAGO_PUBLIC_KEY=sua_public_key_do_mercado_pago
MERCADOPAGO_CHECKOUT_MODE=production
APP_BASE_URL=https://petexpress-site-backend.onrender.com
```

Se o Render criar uma URL diferente para o backend, atualize `APP_BASE_URL` com a URL real.

Importante: nao coloque as chaves do Mercado Pago direto no codigo. O `render.yaml` deixa essas variaveis como `sync: false`, entao o Render vai pedir os valores no painel durante a criacao do Blueprint.

## 4. Apontar o frontend para a API

Abra `config.js`.

Se o backend ficou com outra URL, troque:

```js
const productionApiUrl = 'https://petexpress-site-backend.onrender.com';
```

pela URL real do backend.

## 5. Testes depois do deploy

1. Abra a URL do backend:

```text
https://petexpress-site-backend.onrender.com/produtos
```

Se carregar uma lista ou JSON, a API subiu.

2. Abra a URL do frontend.
3. Teste:
   - cadastro
   - login
   - carrinho
   - checkout
   - redirecionamento do Mercado Pago

## Observacao sobre banco de dados

O projeto usa SQLite (`petexpress.db`). Para trabalho/teste, isso pode funcionar. Para loja real em producao, o ideal e migrar para Postgres, porque deploy gratuito pode reiniciar e perder alteracoes locais no arquivo do banco.
