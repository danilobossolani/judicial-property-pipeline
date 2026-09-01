# Manual de operação

## Parâmetros do ambiente

| Item | Padrão | Observação |
|---|---:|---|
| Porta HTTP | `8080` | No compose, altere com `APP_PORT`. |
| Fuso horário | `America/Sao_Paulo` | Definido no contêiner e no PostgreSQL. |
| Primeira descoberta | 2 minutos após iniciar | `descoberta.atraso-inicial-ms`. |
| Intervalo de descoberta | 6 horas após concluir | `descoberta.intervalo-ms`. |
| Timeout por requisição | 15 segundos | `integracao.fontes.timeout-ms`. |
| Retentativas | 3 | Espera progressiva a partir de 400 ms. |
| Health check | `/actuator/health` | Não expõe detalhes internos. |

Credenciais devem existir somente como segredos/variáveis do ambiente. Não coloque senha, chave DataJud ou URL privada no Git, imagem Docker ou logs.

## Subida e teste de fumaça

1. Garanta um backup recente do PostgreSQL.
2. Inicie a aplicação; o Flyway aplicará apenas migrações pendentes.
3. Confirme que `/actuator/health` responde `UP`.
4. Abra o painel, a Central de Auditoria e um detalhe de imóvel.
5. Execute uma descoberta manual supervisionada.
6. Confirme contadores, fonte por lote, descartes, falhas e ausência de despejos.
7. Aprove ou descarte um item de teste e confirme o evento no histórico.

## Backup e restauração

Backup lógico, executado por um operador com acesso seguro ao banco:

```text
pg_dump --format=custom --file=judicial-pipeline.dump --dbname=<URL_DO_BANCO>
```

Restaure primeiro em um banco isolado e valide contagens e telas:

```text
pg_restore --clean --if-exists --no-owner --dbname=<URL_DO_BANCO_DE_RESTAURACAO> judicial-pipeline.dump
```

Em produção, prefira PostgreSQL gerenciado com backup automático, retenção definida e teste periódico de restauração. Não considere um backup válido sem ensaio de restauração.

## Logs e falhas externas

- monitore inicialização, Flyway, falhas de fonte e encerramento de cada descoberta;
- use a Central de Auditoria para falhas por fonte/lote e decisões registradas;
- não registre corpo completo de páginas, chave DataJud ou credenciais;
- uma fonte indisponível deve resultar em `CONCLUIDA_COM_FALHAS`, mantendo as demais fontes em execução.

## Atualização e rollback

1. Só publique commit com CI verde e `mvn verify` aprovado.
2. Registre a versão/commit implantado.
3. Faça backup antes de uma migração nova.
4. Implante e realize o teste de fumaça.
5. Para rollback do código, execute a imagem anterior. Migrações de banco não devem ser desfeitas automaticamente; restaure backup apenas com avaliação e janela de manutenção.

## Checklist antes de acesso do cliente

- [ ] servidor e PostgreSQL de produção escolhidos;
- [ ] segredos configurados fora do repositório;
- [ ] backup automático e restauração testada;
- [ ] domínio e HTTPS configurados;
- [ ] acesso protegido por autenticação no proxy, VPN ou camada equivalente;
- [ ] logs, monitoramento e alerta de indisponibilidade configurados;
- [ ] CI verde no commit implantado;
- [ ] descoberta real supervisionada validada;
- [ ] responsável pela análise humana e rotina operacional definidos.

Sem esses itens, a aplicação está preparada para piloto/local, mas não deve ser declarada em produção pública.
