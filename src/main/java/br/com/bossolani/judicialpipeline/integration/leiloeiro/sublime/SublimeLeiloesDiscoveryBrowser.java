package br.com.bossolani.judicialpipeline.integration.leiloeiro.sublime;

import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteDescobertoDTO;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SublimeLeiloesDiscoveryBrowser {

    private static final URI URL_BASE =
            URI.create(
                    "https://www.sublimeleiloes.com.br/"
            );

    private static final String SELETOR_CARTAO =
            "main .dg-leiloes-item";

    private static final String SELETOR_TITULO =
            "h3.dg-leiloes-nome";

    private static final String SELETOR_LINK_LOTE =
            "a[href*='/lote/']";

    private static final int RESULTADOS_POR_PAGINA = 48;

    private static final Pattern PADRAO_TOTAL =
            Pattern.compile(
                    "Encontrados\\s*\\((\\d+)\\)\\s*resultados",
                    Pattern.CASE_INSENSITIVE
            );

    private static final List<CidadeSublime> CIDADES_ALVO =
            List.of(
                    new CidadeSublime(
                            "Sorocaba",
                            "3552205"
                    ),
                    new CidadeSublime(
                            "Votorantim",
                            "3557006"
                    )
            );


    public List<LoteDescobertoDTO> descobrirLotes() {

        Map<String, LoteDescobertoDTO> lotesPorUrl =
                new LinkedHashMap<>();


        try (Playwright playwright = Playwright.create()) {

            Browser browser =
                    playwright.chromium().launch(
                            new BrowserType.LaunchOptions()
                                    .setHeadless(true)
                    );


            try {

                for (CidadeSublime cidade : CIDADES_ALVO) {

                    descobrirNaCidade(
                            browser,
                            cidade,
                            lotesPorUrl
                    );
                }

            } finally {

                browser.close();
            }
        }


        return new ArrayList<>(
                lotesPorUrl.values()
        );
    }


    private void descobrirNaCidade(
            Browser browser,
            CidadeSublime cidade,
            Map<String, LoteDescobertoDTO> lotesPorUrl
    ) {

        int paginaAtual = 1;
        int totalPaginas = 1;


        while (paginaAtual <= totalPaginas) {

            Page page = browser.newPage();


            try {

                page.navigate(
                        montarUrlBusca(
                                cidade.codigoIbge(),
                                paginaAtual
                        ),
                        new Page.NavigateOptions()
                                .setWaitUntil(
                                        WaitUntilState.NETWORKIDLE
                                )
                );


                page.waitForFunction(
                        "() => document.querySelectorAll('main .dg-leiloes-item').length > 0"
                                + " || (document.querySelector('main')?.innerText || '')"
                                + ".includes('Não há resultados')"
                );


                Locator cartoes =
                        page.locator(
                                SELETOR_CARTAO
                        );


                if (paginaAtual == 1) {

                    totalPaginas =
                            calcularTotalPaginas(
                                    page.locator("main")
                                            .innerText()
                            );
                }


                for (int indice = 0;
                     indice < cartoes.count();
                     indice++) {

                    Locator cartao =
                            cartoes.nth(
                                    indice
                            );


                    Locator links =
                            cartao.locator(
                                    SELETOR_LINK_LOTE
                            );


                    if (links.count() == 0) {
                        continue;
                    }


                    String href =
                            links.first()
                                    .getAttribute("href");


                    if (href == null
                            || href.isBlank()) {

                        continue;
                    }


                    String url =
                            URL_BASE.resolve(
                                    href
                            ).toString();


                    String titulo =
                            extrairTitulo(
                                    cartao
                            );


                    LoteDescobertoDTO lote =
                            new LoteDescobertoDTO(
                                    url,
                                    titulo,
                                    cidade.nome(),
                                    cartao.innerText()
                            );


                    lotesPorUrl.putIfAbsent(
                            url,
                            lote
                    );
                }


                paginaAtual++;

            } finally {

                page.close();
            }
        }
    }


    private String extrairTitulo(
            Locator cartao
    ) {

        Locator titulo =
                cartao.locator(
                        SELETOR_TITULO
                );


        if (titulo.count() == 0) {
            return "";
        }


        return titulo.first()
                .innerText()
                .trim();
    }


    private int calcularTotalPaginas(
            String textoPrincipal
    ) {

        Matcher matcher =
                PADRAO_TOTAL.matcher(
                        textoPrincipal
                );


        if (!matcher.find()) {
            return 1;
        }


        int totalResultados =
                Integer.parseInt(
                        matcher.group(1)
                );


        return Math.max(
                1,
                (int) Math.ceil(
                        (double) totalResultados
                                / RESULTADOS_POR_PAGINA
                )
        );
    }


    private String montarUrlBusca(
            String codigoIbge,
            int pagina
    ) {

        return "https://www.sublimeleiloes.com.br/busca/"
                + "#Engine=Start"
                + "&Pagina=" + pagina
                + "&OrientacaoBusca=0"
                + "&Busca="
                + "&Mapa="
                + "&ID_Categoria=-1"
                + "&ID_Estado=35"
                + "&Bairro=-1"
                + "&ID_Cidade=" + codigoIbge
                + "&ID_Regiao=0"
                + "&ValorMinSelecionado=0"
                + "&ValorMaxSelecionado=0"
                + "&Ordem=0"
                + "&QtdPorPagina=" + RESULTADOS_POR_PAGINA
                + "&SubStatus="
                + "&PaginaIndex=" + pagina
                + "&BuscaProcesso="
                + "&NomesPartes="
                + "&CodLeilao="
                + "&TiposLeiloes=[]"
                + "&CFGs=[]";
    }


    private record CidadeSublime(
            String nome,
            String codigoIbge
    ) {
    }
}
