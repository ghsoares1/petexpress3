# Publicação do PetExpress

A publicação usa um único aplicativo Java que serve o site e a API no mesmo endereço HTTPS. O Dockerfile da raiz inclui HTML, CSS, JavaScript e imagens no pacote do backend. O navegador chama a API do próprio endereço do site.

## GitHub

Envie a raiz inteira deste projeto para o repositório escolhido, incluindo Dockerfile, .dockerignore, render.yaml, img/ e petexpress-site-backend/. As regras de Git ignoram bancos locais, arquivos de ambiente, logs, capturas de teste e saídas de compilação.

## Hospedagem no Render

O render.yaml configura um Web Service Docker chamado petexpress com disco persistente em /app/data e verificação de saúde em /health. Esse arquivo usa o plano Starter e disco de 1 GB. A criação desse serviço tem custo no Render. Confirme o plano e o preço no painel antes de concluir a criação.

1. Conecte o repositório no Render e crie um Blueprint usando o render.yaml da raiz.
2. Confira o serviço Docker, plano e disco persistente antes de publicar.
3. O JWT_SECRET é gerado pelo Render. Guarde a mesma chave para manter as sessões válidas entre publicações.
4. Configure PETEXPRESS_VENDEDOR_IDS com os IDs das contas autorizadas. Na primeira publicação, deixe o valor vazio, cadastre sua conta no site, consulte seu ID na resposta de login e então autorize esse ID no Render.
5. Configure as credenciais do Mercado Pago no painel do Render para habilitar o checkout. Catálogo, cadastro, login e painel funcionam sem essas credenciais.
6. O backend usa automaticamente RENDER_EXTERNAL_URL como URL pública. Caso use domínio próprio, configure APP_BASE_URL com esse domínio.
7. Abra a URL HTTPS indicada pelo Render. Esse é o link para compartilhar com outras pessoas.

Para um serviço Docker criado manualmente, mantenha o contexto de build na raiz do repositório e selecione ./Dockerfile. A raiz de execução é /app, e o banco fica em /app/data/petexpress.db. Use uma única instância com este banco SQLite.

## Dados existentes

A instalação nova aplica as migrações e carrega o catálogo inicial. As contas, pedidos e produtos cadastrados no computador ficam no banco local. Para levar esses dados ao servidor, transfira uma cópia consistente do banco utilizado para o disco persistente antes de iniciar o aplicativo. Essa transferência deve ser feita por um canal privado da hospedagem.

A conta equipe@example.test é usada na demonstração local. Para a publicação, cadastre e autorize sua própria conta.

## Validação da publicação

- GET /health deve responder com status ok.
- / deve abrir a home e /produtos deve listar o catálogo.
- Confira as imagens, cadastro e login em outro dispositivo.
- Acesse vendedor-resumo.html usando a conta autorizada e teste cadastro de produto, estoque e pedidos.
- Reinicie o serviço e confirme que os dados persistem no disco.

## Execução local

Use JDK 17 ou superior e execute o Maven Wrapper em petexpress-site-backend/petexpress-site-backend. O arquivo application.properties contém as opções de ambiente. O servidor local usa a porta 8082. Para servir os arquivos da raiz em desenvolvimento, inicie o backend na sua pasta interna.

## Pagamento, estoque e promoções

Configure MERCADOPAGO_ACCESS_TOKEN da conta recebedora. O retorno e as notificações de pagamento usam o endereço público do aplicativo. APP_BASE_URL e PETEXPRESS_FRONTEND_URL podem ser configurados caso os endereços sejam personalizados.

O checkout reserva embalagens por 30 minutos. O servidor verifica as reservas vencidas com uma margem adicional de 10 minutos. Pagamentos em processamento mantêm a reserva. Aprovações tardias e estornos ficam em revisão.

As ofertas podem ser programadas por produto ou tamanho. Entre ofertas simultâneas, vale o maior percentual. Um cupom adicional pode ser aplicado por pedido, respeitando validade, compra mínima e limite de uso. O pedido preserva o desconto e os preços da compra.

Os relatórios consideram pedidos aprovados. Pedidos antigos sem forma de pagamento registrada aparecem como Não informado. A entrega via Uber é uma simulação com taxa fixa de R$ 19,90.

