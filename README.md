# Judicial Pipeline

Aplicação Spring Boot para descobrir, organizar e acompanhar imóveis de leilões judiciais em Sorocaba e Votorantim. O sistema reúne evidências; a classificação comercial continua sendo uma decisão humana.

## Regras permanentes

- somente imóveis em Sorocaba ou Votorantim;
- ações de despejo são descartadas e não aparecem como oportunidade;
- “sem lances” nunca vira oportunidade automaticamente;
- URLs e processos são deduplicados sem apagar fontes divergentes;
- decisões humanas e histórico são preservados nas atualizações automáticas.
- leilões cujo último prazo publicado já terminou saem do painel principal e
  permanecem disponíveis em **Arquivados / inativos**.

## Fontes atuais

- [Sublime Leilões](https://www.sublimeleiloes.com.br/): descoberta e coleta de lotes;
- [Mega Leilões](https://www.megaleiloes.com.br/sp/sorocaba): segunda fonte de descoberta e coleta de lotes;
- [SPY Leilões](https://spyleiloes.com.br/imoveis-leilao/sp/sorocaba/modalidade/judicial): descoberta de imóveis judiciais e terrenos;
- [Portal Zuk](https://www.portalzuk.com.br/leilao-de-imoveis/v/leilao-judicial-sao-paulo-tjsp/c/todos-imoveis/sp/interior/sorocaba): descoberta e coleta de imóveis judiciais;
- [GL Leilões](https://www.glleiloes.com.br/lotes/imovel): descoberta e coleta de lotes imobiliários judiciais;
- [PublicJud](https://www.publicjud.com.br/): descoberta em editais judiciais;
- [DJEN/CNJ](https://comunica.pje.jus.br/): consulta oficial de comunicações judiciais, com triagem estrita de editais de leilão imobiliário;
- DataJud/CNJ: complemento processual, não fonte de lotes.

Cada integração de descoberta implementa `LeiloeiroProvider`. Uma falha de fonte ou lote é registrada na Central de Auditoria e não interrompe as demais coletas.

A descoberta automática roda a cada seis horas. O ciclo de vida é verificado a
cada minuto: depois da última praça, o item deixa o painel ativo, mas seus
dados, fontes, observações e histórico continuam preservados.

## Requisitos

- Java 21;
- PostgreSQL;
- Maven 3.9+ ou Maven Wrapper;
- Chromium do Playwright para a coleta da Sublime.

Variáveis obrigatórias:

```text
DB_URL=jdbc:postgresql://localhost:5432/judicial_pipeline
DB_USER=...
DB_PASSWORD=...
DATAJUD_API_KEY=...
```

No perfil `prod`, o acesso também exige:

```text
APP_SECURITY_ENABLED=true
APP_SECURITY_USERNAME=...
APP_SECURITY_PASSWORD=...  # mínimo de 12 caracteres
APP_COOKIE_SECURE=true     # use true quando houver HTTPS
```

O perfil local mantém a autenticação desabilitada por padrão. Isso evita
atrapalhar o desenvolvimento, mas o `compose.yaml` e o perfil `prod` habilitam
a proteção e recusam inicialização sem usuário e senha válidos.

Execução local:

```powershell
./mvnw spring-boot:run
```

As migrações Flyway são aplicadas na inicialização. O Hibernate apenas valida o esquema.

## Testes e pacote

```powershell
./mvnw verify
```

O CI executa o mesmo portão de qualidade em pushes e pull requests.
O pacote executável é gerado como `target/judicial-pipeline-0.0.1-SNAPSHOT-exec.jar`.

## Docker Compose

1. Copie `.env.example` para `.env` e substitua os valores.
2. Execute `docker compose up --build -d`.
3. Consulte `http://localhost:8080/actuator/health`.

O compose é uma preparação reproduzível, não um deploy público. Servidor, domínio, HTTPS, controle de acesso e PostgreSQL de produção ainda precisam ser escolhidos e configurados.

No Windows, o pacote local também fornece `INICIAR.bat`, `PARAR.bat` e
`VER-STATUS.bat`. As instruções para entregar e executar esse pacote estão em
[ENTREGA-LOCAL.md](ENTREGA-LOCAL.md). O arquivo `.env` com credenciais nunca
deve ser incluído no ZIP ou enviado por mensagens.

Para um cliente leigo, existe também um instalador personalizado que baixa os
componentes, configura o ambiente sem login e cria o atalho do programa. O
processo de build e suas limitações de segurança estão documentados em
[installer/README.md](installer/README.md). O instalador limita a porta ao
próprio computador (`127.0.0.1`) porque o acesso local não exige senha.

Em produção, `/actuator/health` permanece público e retorna apenas `UP` ou
`DOWN`. `/actuator/prometheus` exige autenticação do operador e publica
somente métricas técnicas e contadores agregados, sem URLs, processos ou dados
de imóveis.

Consulte [OPERATIONS.md](OPERATIONS.md) para backup, restauração, agendamento, logs, atualização e rollback.
