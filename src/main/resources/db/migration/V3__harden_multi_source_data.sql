ALTER TABLE processos
    ALTER COLUMN numero_processo TYPE VARCHAR(25);

UPDATE historicos_acompanhamento historico
SET origem = 'Mega Leilões'
WHERE historico.origem = 'Sublime Leilões'
  AND EXISTS (
      SELECT 1
      FROM acompanhamentos acompanhamento
      JOIN leiloes leilao
        ON leilao.imovel_id = acompanhamento.imovel_id
      JOIN fontes fonte
        ON fonte.leilao_id = leilao.id
      WHERE acompanhamento.id = historico.acompanhamento_id
        AND fonte.origem_nome = 'Mega Leilões'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM acompanhamentos acompanhamento
      JOIN leiloes leilao
        ON leilao.imovel_id = acompanhamento.imovel_id
      JOIN fontes fonte
        ON fonte.leilao_id = leilao.id
      WHERE acompanhamento.id = historico.acompanhamento_id
        AND fonte.origem_nome <> 'Mega Leilões'
  );
