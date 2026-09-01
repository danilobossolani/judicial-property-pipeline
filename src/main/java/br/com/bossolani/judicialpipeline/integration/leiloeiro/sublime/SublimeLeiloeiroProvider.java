package br.com.bossolani.judicialpipeline.integration.leiloeiro.sublime;

import br.com.bossolani.judicialpipeline.integration.leiloeiro.ColetaLeiloeiroDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.LeiloeiroProvider;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.ResilienciaFonteService;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.DadosDinamicosLeilaoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteDescobertoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteLeilaoDTO;
import org.jsoup.nodes.Document;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
@Order(10)
public class SublimeLeiloeiroProvider
        implements LeiloeiroProvider {

    private static final String NOME =
            "Sublime Leilões";

    private static final String DOMINIO =
            "sublimeleiloes.com.br";

    private static final Pattern CAMINHO_LOTE =
            Pattern.compile(
                    "^/lote/[^/]+/\\d+/?$",
                    Pattern.CASE_INSENSITIVE
            );

    private final SublimeLeiloesDiscoveryBrowser discoveryBrowser;

    private final SublimeLeiloesScraper scraper;

    private final SublimeLeiloesBrowser browser;

    private final ResilienciaFonteService resiliencia;


    public SublimeLeiloeiroProvider(
            SublimeLeiloesDiscoveryBrowser discoveryBrowser,
            SublimeLeiloesScraper scraper,
            SublimeLeiloesBrowser browser,
            ResilienciaFonteService resiliencia
    ) {

        this.discoveryBrowser = discoveryBrowser;
        this.scraper = scraper;
        this.browser = browser;
        this.resiliencia = resiliencia;
    }


    @Override
    public String nome() {

        return NOME;
    }


    @Override
    public boolean suporta(
            URI uri
    ) {

        if (uri == null
                || uri.getHost() == null
                || !"https".equalsIgnoreCase(
                uri.getScheme()
        )) {
            return false;
        }


        String host =
                uri.getHost()
                        .toLowerCase(
                                Locale.ROOT
                        );


        return (host.equals(
                DOMINIO
        )
                || host.endsWith(
                "." + DOMINIO
        ))
                && CAMINHO_LOTE.matcher(
                uri.getPath()
        ).matches();
    }


    @Override
    public String normalizarUrl(
            URI uri
    ) {

        if (!suporta(
                uri
        )) {

            throw new IllegalArgumentException(
                    "URL de lote da Sublime inválida"
            );
        }


        String caminho =
                uri.getPath()
                        .endsWith("/")
                        ? uri.getPath()
                        : uri.getPath() + "/";


        try {

            return new URI(
                    "https",
                    null,
                    uri.getHost()
                            .toLowerCase(
                                    Locale.ROOT
                            ),
                    -1,
                    caminho,
                    null,
                    null
            ).toASCIIString();

        } catch (URISyntaxException exception) {

            throw new IllegalArgumentException(
                    "URL de lote da Sublime inválida",
                    exception
            );
        }
    }


    @Override
    public List<LoteDescobertoDTO> descobrirLotes()
            throws Exception {

        List<LoteDescobertoDTO> lotes =
                resiliencia.executar(
                        NOME,
                        discoveryBrowser::descobrirLotes
                );


        if (lotes == null) {
            return List.of();
        }


        return lotes.stream()
                .map(lote ->
                        new LoteDescobertoDTO(
                                lote.url(),
                                lote.titulo(),
                                lote.cidade(),
                                lote.resumo(),
                                NOME
                        )
                )
                .toList();
    }


    @Override
    public ColetaLeiloeiroDTO coletar(
            String url
    ) throws Exception {

        return resiliencia.executar(
                NOME,
                () -> coletarSemRetentativa(
                        url
                )
        );
    }


    private ColetaLeiloeiroDTO coletarSemRetentativa(
            String url
    ) throws Exception {

        Document pagina =
                scraper.buscarPagina(
                        url
                );


        LoteLeilaoDTO lote =
                scraper.extrairLote(
                        pagina,
                        url
                );


        String textoRenderizado =
                browser.buscarTextoRenderizado(
                        url
                );


        DadosDinamicosLeilaoDTO leilao =
                browser.extrairDados(
                        textoRenderizado
                );


        return new ColetaLeiloeiroDTO(
                lote,
                leilao
        );
    }
}
