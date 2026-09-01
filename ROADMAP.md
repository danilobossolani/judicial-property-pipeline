# Judicial Pipeline — objetivos e continuidade

## Missão do produto

Descobrir, organizar e acompanhar lotes de imóveis judiciais com rastreabilidade suficiente para apoiar uma análise humana segura. O sistema não toma decisões comerciais sozinho: ele reúne fontes, registra mudanças e reduz trabalho operacional repetitivo.

## Regras de negócio permanentes

- Trabalhar somente com imóveis de Sorocaba e Votorantim no escopo atual.
- Excluir ações de despejo do funil de oportunidades imobiliárias.
- Nunca transformar automaticamente um leilão sem lances em oportunidade.
- Deduplicar por URL normalizada e número do processo, preservando fontes divergentes.
- Manter decisões humanas e histórico de alterações auditáveis.
- Uma falha em um lote não pode interromper o processamento dos demais.
- Toda informação relevante deve conservar sua origem.

## Estado atual

- Coleta da Sublime Leilões validada no site real.
- Descoberta automática e execução manual disponíveis.
- Triagem por tipo de bem e cidade.
- Deduplicação por URL e processo.
- Integração com DataJud e PostgreSQL.
- Central de Auditoria com histórico por execução e por lote.
- Interface responsiva, com temas claro e escuro.
- Testes unitários, de persistência e de inicialização da aplicação.
- Integração contínua no GitHub para testar e empacotar cada alteração.
- Endpoint de saúde sem exposição de detalhes internos.
- Perfil de produção com validação do esquema e logs SQL reduzidos.
- `open-in-view` desativado e telas validadas com PostgreSQL real.

## Portões obrigatórios de qualidade

Antes de integrar qualquer mudança ao branch `main`:

1. Executar `mvn verify` com sucesso.
2. Manter testes para cada regra de negócio corrigida ou adicionada.
3. Validar o fluxo afetado no navegador.
4. Conferir desktop e largura móvel de 390 px quando houver mudança visual.
5. Conferir temas claro e escuro quando houver mudança visual.
6. Verificar erros e avisos relevantes no console.
7. Revisar `git diff --check` e o conjunto exato de arquivos alterados.
8. Fazer commit pequeno, descritivo e reversível.
9. Confirmar a execução verde da integração contínua após o push.

## Próximos marcos

### 1. Confiabilidade operacional

- [x] Desativar `open-in-view` e validar que todas as telas carregam seus dados explicitamente.
- [x] Reduzir logs SQL no ambiente de produção.
- [x] Adicionar endpoint de saúde básico e seguro.
- [x] Adotar migrações versionadas de banco de dados antes do primeiro deploy público.
- [x] Adicionar métricas operacionais sem expor dados sensíveis.
- [x] Implementar retentativas com espera progressiva para falhas transitórias das fontes.
- [x] Definir timeout e cadência conservadora de descoberta das fontes.
- [ ] Definir retenção operacional do histórico após medir o volume do piloto.

### 2. Piloto supervisionado

- Executar descobertas reais periodicamente com revisão humana.
- Medir falsos positivos, descartes e divergências entre fontes.
- Confirmar matrícula, ocupação, débitos e situação processual antes de qualquer classificação comercial.
- Criar uma fila clara de itens que exigem decisão humana.

### 3. Deploy de produção

- Escolher o provedor de hospedagem.
- Usar PostgreSQL gerenciado com backup automático.
- Armazenar credenciais somente como segredos do ambiente.
- Configurar HTTPS, domínio, logs, monitoramento e alertas.
- Executar teste de fumaça após cada deploy.
- Documentar restauração do banco e rollback da aplicação.
- [x] Proteger interface, APIs administrativas e métricas com autenticação configurável.

### 4. Expansão controlada

- Adicionar novas cidades somente após métricas confiáveis do piloto.
- Adicionar novos leiloeiros por integrações isoladas e testadas.
- Comparar fontes sem escolher silenciosamente uma delas como verdade.
- Priorizar qualidade da triagem antes de aumentar volume.

## Definição de pronto para produção

O sistema só estará pronto para produção pública quando possuir migrações de banco, backup validado, segredos fora do repositório, integração contínua verde, endpoint de saúde, monitoramento, política de rollback e um teste completo de descoberta executado no ambiente publicado.
