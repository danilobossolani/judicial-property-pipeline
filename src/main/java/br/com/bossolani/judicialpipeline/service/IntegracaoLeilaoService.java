package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.integration.DataJudClient;
import br.com.bossolani.judicialpipeline.integration.datajud.dto.DataJudProcessoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteEnriquecidoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteLeilaoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.sublime.SublimeLeiloesScraper;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class IntegracaoLeilaoService {

    private final SublimeLeiloesScraper sublimeScraper;
    private final DataJudClient dataJudClient;

    public IntegracaoLeilaoService(
            SublimeLeiloesScraper sublimeScraper,
            DataJudClient dataJudClient
    ) {
        this.sublimeScraper = sublimeScraper;
        this.dataJudClient = dataJudClient;
    }

    public LoteEnriquecidoDTO buscarLote(String url) throws IOException {

        Document pagina = sublimeScraper.buscarPagina(url);

        LoteLeilaoDTO lote =
                sublimeScraper.extrairLote(pagina, url);

        DataJudProcessoDTO processo =
                dataJudClient.buscarProcesso(lote.getNumeroProcesso());

        return new LoteEnriquecidoDTO(
                lote,
                processo,
                true
        );
    }
}