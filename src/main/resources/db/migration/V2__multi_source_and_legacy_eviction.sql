ALTER TABLE leiloes
    ADD COLUMN IF NOT EXISTS valor_avaliacao_fonte NUMERIC(15, 2);

ALTER TABLE resultados_lote_descoberta
    ADD COLUMN IF NOT EXISTS fonte VARCHAR(120);

UPDATE leiloes leilao
SET valor_avaliacao_fonte = (
    SELECT imovel.valor_avaliacao
    FROM imoveis imovel
    WHERE imovel.id = leilao.imovel_id
)
WHERE leilao.valor_avaliacao_fonte IS NULL;

UPDATE resultados_lote_descoberta resultado
SET fonte = (
    SELECT execucao.fonte
    FROM execucoes_descoberta execucao
    WHERE execucao.id = resultado.execucao_id
)
WHERE resultado.fonte IS NULL;

UPDATE acompanhamentos acompanhamento
SET ativo = FALSE,
    status_pipeline = 'DESCARTADO'
WHERE (
      acompanhamento.ativo = TRUE
      OR acompanhamento.status_pipeline <> 'DESCARTADO'
  )
  AND EXISTS (
      SELECT 1
      FROM imoveis imovel
      JOIN processos processo
        ON processo.id = imovel.processo_id
      WHERE imovel.id = acompanhamento.imovel_id
        AND LOWER(processo.classe) LIKE '%despejo%'
  );

INSERT INTO historicos_acompanhamento (
    acompanhamento_id,
    data_evento,
    status_pipeline,
    status_leilao,
    resultado_leilao,
    origem,
    descricao
)
SELECT acompanhamento.id,
       CURRENT_TIMESTAMP,
       'DESCARTADO',
       NULL,
       NULL,
       'Migração V2',
       'Registro legado de ação de despejo arquivado; histórico anterior preservado.'
FROM acompanhamentos acompanhamento
JOIN imoveis imovel
  ON imovel.id = acompanhamento.imovel_id
JOIN processos processo
  ON processo.id = imovel.processo_id
WHERE LOWER(processo.classe) LIKE '%despejo%'
  AND NOT EXISTS (
      SELECT 1
      FROM historicos_acompanhamento historico
      WHERE historico.acompanhamento_id = acompanhamento.id
        AND historico.origem = 'Migração V2'
  );

CREATE INDEX IF NOT EXISTS idx_fonte_imovel_captura
    ON fontes (leilao_id, data_captura DESC);

CREATE INDEX IF NOT EXISTS idx_resultado_fonte
    ON resultados_lote_descoberta (fonte);
