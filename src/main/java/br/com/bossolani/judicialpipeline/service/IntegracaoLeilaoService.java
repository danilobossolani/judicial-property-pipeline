package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.integration.DataJudClient;
import br.com.bossolani.judicialpipeline.integration.datajud.dto.DataJudProcessoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.DadosDinamicosLeilaoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteEnriquecidoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteLeilaoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.sublime.SublimeLeiloesBrowser;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.sublime.SublimeLeiloesScraper;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class IntegracaoLeilaoService {

    private static final int TAMANHO_NUMERO_PROCESSO_CNJ = 20;

    private final SublimeLeiloesScraper sublimeScraper;
    private final SublimeLeiloesBrowser sublimeBrowser;
    private final DataJudClient dataJudClient;


    public IntegracaoLeilaoService(
            SublimeLeiloesScraper sublimeScraper,
            SublimeLeiloesBrowser sublimeBrowser,
            DataJudClient dataJudClient
    ) {

        this.sublimeScraper = sublimeScraper;
        this.sublimeBrowser = sublimeBrowser;
        this.dataJudClient = dataJudClient;
    }


    public LoteEnriquecidoDTO buscarLote(
            String url
    ) throws IOException {

        Document pagina =
                sublimeScraper.buscarPagina(
                        url
                );


        LoteLeilaoDTO lote =
                sublimeScraper.extrairLote(
                        pagina,
                        url
                );


        String textoRenderizado =
                sublimeBrowser.buscarTextoRenderizado(
                        url
                );


        DadosDinamicosLeilaoDTO leilao =
                sublimeBrowser.extrairDados(
                        textoRenderizado
                );


        DataJudProcessoDTO processo = null;
        boolean processoConfirmado = false;


        String numeroLeiloeiro =
                normalizarNumeroProcesso(
                        lote.getNumeroProcesso()
                );


        if (numeroLeiloeiro.length()
                == TAMANHO_NUMERO_PROCESSO_CNJ) {

            try {

                processo =
                        dataJudClient.buscarProcesso(
                                numeroLeiloeiro
                        );


                String numeroDataJud =
                        normalizarNumeroProcesso(
                                processo.numeroProcesso()
                        );


                processoConfirmado =
                        numeroLeiloeiro.equals(
                                numeroDataJud
                        );

            } catch (IllegalArgumentException exception) {

                processo = null;
                processoConfirmado = false;
            }
        }


        return new LoteEnriquecidoDTO(
                lote,
                leilao,
                processo,
                processoConfirmado
        );
    }


    private String normalizarNumeroProcesso(
            String numeroProcesso
    ) {

        if (numeroProcesso == null) {
            return "";
        }


        return numeroProcesso.replaceAll(
                "\\D",
                ""
        );
    }
}
