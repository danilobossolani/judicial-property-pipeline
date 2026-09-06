# Entrega local para Windows

Este pacote inicia o Judicial Pipeline no computador do operador. Os dados
ficam armazenados localmente pelo Docker e permanecem depois que o sistema é
parado ou o computador é reiniciado.

## Primeira instalação

1. Instale o [Docker Desktop para Windows](https://www.docker.com/products/docker-desktop/).
2. Abra o Docker Desktop e aguarde o motor ficar disponível.
3. Extraia o pacote completo para uma pasta comum. Não execute diretamente de dentro do ZIP.
4. Clique duas vezes em `INICIAR.bat`.
5. Na primeira execução, o arquivo `.env` será aberto. Substitua:
   - `POSTGRES_PASSWORD` por uma senha forte para o banco;
   - `DATAJUD_API_KEY` pela chave autorizada do DataJud;
   - `APP_SECURITY_USERNAME` pelo usuário do operador;
   - `APP_SECURITY_PASSWORD` por uma senha de acesso com pelo menos 12 caracteres.
6. Salve o arquivo e execute `INICIAR.bat` novamente.

A primeira inicialização pode levar alguns minutos porque o Docker baixa e
monta os componentes. Ao terminar, o navegador abrirá
`http://localhost:8080`.

## Uso normal

- iniciar e abrir o sistema: `INICIAR.bat`;
- parar sem apagar os dados e liberar os recursos do Docker/WSL: use o atalho
  `Encerrar Judicial Pipeline` ou, para suporte técnico, `PARAR.bat`;
- consultar estado e erros recentes: `VER-STATUS.bat`.

O Docker Desktop precisa estar aberto enquanto o sistema estiver sendo usado.
Quando o atalho de encerramento for acionado, as buscas automáticas ficam
pausadas até a próxima abertura, sem perda do banco ou do histórico.
Nunca envie o arquivo `.env` por e-mail ou aplicativo de mensagens, pois ele
contém credenciais.

## Limitação do pacote local

O endereço `localhost` funciona somente no computador onde o pacote está
rodando. Para acesso por vários computadores ou fora da residência/escritório,
use a implantação online com domínio, HTTPS, PostgreSQL gerenciado e backup.
