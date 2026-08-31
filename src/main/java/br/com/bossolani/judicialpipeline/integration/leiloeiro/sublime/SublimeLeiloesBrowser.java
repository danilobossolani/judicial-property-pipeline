package br.com.bossolani.judicialpipeline.integration.leiloeiro.sublime;

import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.DadosDinamicosLeilaoDTO;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SublimeLeiloesBrowser {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy - HH:mm"
            );


    public String buscarTextoRenderizado(
            String url
    ) {

        try (Playwright playwright = Playwright.create()) {

            Browser browser =
                    playwright.chromium().launch(
                            new BrowserType.LaunchOptions()
                                    .setHeadless(true)
                    );


            try {

                Page page = browser.newPage();


                page.navigate(
                        url,
                        new Page.NavigateOptions()
                                .setWaitUntil(
                                        WaitUntilState.NETWORKIDLE
                                )
                );


                return page.locator("body")
                        .innerText();

            } finally {

                browser.close();
            }
        }
    }


    public DadosDinamicosLeilaoDTO extrairDados(
            String texto
    ) {

        String bloco1Praca =
                extrairBloco(
                        texto,
                        "1º Praça",
                        "2º Praça"
                );


        String bloco2Praca =
                extrairBloco(
                        texto,
                        "2º Praça",
                        "Informações"
                );


        LocalDateTime abertura1 =
                extrairData(
                        bloco1Praca,
                        "Abertura:"
                );


        LocalDateTime fechamento1 =
                extrairData(
                        bloco1Praca,
                        "Fechamento:"
                );


        BigDecimal lanceInicial1 =
                extrairDinheiro(
                        bloco1Praca,
                        "Lance Inicial:"
                );


        LocalDateTime abertura2 =
                extrairData(
                        bloco2Praca,
                        "Abertura"
                );


        LocalDateTime fechamento2 =
                extrairData(
                        bloco2Praca,
                        "Fechamento"
                );


        BigDecimal lanceInicial2 =
                extrairDinheiro(
                        bloco2Praca,
                        "Lance Inicial:"
                );


        Integer percentualDesconto =
                extrairInteiro(
                        texto,
                        "\\((\\d+)% de desconto\\)"
                );


        String status =
                extrairStatus(
                        texto
                );


        String resultado =
                extrairResultado(
                        texto
                );


        BigDecimal lanceMinimo =
                extrairDinheiro(
                        texto,
                        "Lance Mínimo:"
                );


        BigDecimal incremento =
                extrairDinheiro(
                        texto,
                        "Incremento:"
                );


        BigDecimal comissaoPercentual =
                extrairPercentual(
                        texto,
                        "Comissão do Leiloeiro:"
                );


        return new DadosDinamicosLeilaoDTO(
                abertura1,
                fechamento1,
                lanceInicial1,
                abertura2,
                fechamento2,
                lanceInicial2,
                percentualDesconto,
                status,
                resultado,
                lanceMinimo,
                incremento,
                comissaoPercentual
        );
    }


    private String extrairStatus(
            String texto
    ) {

        String textoNormalizado =
                texto.toUpperCase();


        if (textoNormalizado.contains(
                "LEILÃO ENCERRADO"
        )) {

            return "LEILÃO ENCERRADO";
        }


        if (textoNormalizado.contains(
                "LEILÃO SUSPENSO"
        )
                || textoNormalizado.contains(
                "LOTE SUSPENSO"
        )) {

            return "LEILÃO SUSPENSO";
        }


        if (textoNormalizado.contains(
                "LEILÃO CANCELADO"
        )
                || textoNormalizado.contains(
                "LOTE CANCELADO"
        )) {

            return "LEILÃO CANCELADO";
        }


        if (textoNormalizado.contains(
                "ABERTO PARA LANCE"
        )
                || textoNormalizado.contains(
                "ABERTO PARA LANCES"
        )) {

            return "ABERTO PARA LANCES";
        }


        if (textoNormalizado.contains(
                "AGUARDANDO INÍCIO"
        )
                || textoNormalizado.contains(
                "AGUARDANDO INICIO"
        )) {

            return "AGUARDANDO INÍCIO";
        }


        return null;
    }


    private String extrairResultado(
            String texto
    ) {

        String textoNormalizado =
                texto.toUpperCase();


        if (textoNormalizado.contains(
                "SEM LANCES"
        )) {

            return "SEM LANCES";
        }


        if (textoNormalizado.contains(
                "ARREMATADO"
        )) {

            return "ARREMATADO";
        }


        if (textoNormalizado.contains(
                "DESERTO"
        )) {

            return "DESERTO";
        }


        if (textoNormalizado.contains(
                "COM LANCES"
        )) {

            return "COM LANCES";
        }


        return null;
    }


    private String extrairBloco(
            String texto,
            String inicio,
            String fim
    ) {

        int indiceInicio =
                texto.indexOf(
                        inicio
                );


        if (indiceInicio == -1) {
            return "";
        }


        int indiceFim =
                texto.indexOf(
                        fim,
                        indiceInicio + inicio.length()
                );


        if (indiceFim == -1) {
            indiceFim = texto.length();
        }


        return texto.substring(
                indiceInicio,
                indiceFim
        );
    }


    private LocalDateTime extrairData(
            String texto,
            String campo
    ) {

        Pattern pattern =
                Pattern.compile(
                        Pattern.quote(
                                campo
                        )
                                + "\\s*:?\\s*"
                                + "(\\d{2}/\\d{2}/\\d{4}\\s*-\\s*\\d{2}:\\d{2})"
                );


        Matcher matcher =
                pattern.matcher(
                        texto
                );


        if (matcher.find()) {

            return LocalDateTime.parse(
                    matcher.group(1),
                    FORMATO_DATA
            );
        }


        return null;
    }


    private BigDecimal extrairDinheiro(
            String texto,
            String campo
    ) {

        Pattern pattern =
                Pattern.compile(
                        Pattern.quote(
                                campo
                        )
                                + "\\s*R\\$\\s*([\\d.,]+)"
                );


        Matcher matcher =
                pattern.matcher(
                        texto
                );


        if (matcher.find()) {

            return converterDinheiro(
                    matcher.group(1)
            );
        }


        return null;
    }


    private BigDecimal extrairPercentual(
            String texto,
            String campo
    ) {

        Pattern pattern =
                Pattern.compile(
                        Pattern.quote(
                                campo
                        )
                                + "\\s*([\\d.,]+)%"
                );


        Matcher matcher =
                pattern.matcher(
                        texto
                );


        if (matcher.find()) {

            String valor =
                    matcher.group(1)
                            .replace(
                                    ".",
                                    ""
                            )
                            .replace(
                                    ",",
                                    "."
                            );


            return new BigDecimal(
                    valor
            );
        }


        return null;
    }


    private Integer extrairInteiro(
            String texto,
            String regex
    ) {

        Pattern pattern =
                Pattern.compile(
                        regex
                );


        Matcher matcher =
                pattern.matcher(
                        texto
                );


        if (matcher.find()) {

            return Integer.parseInt(
                    matcher.group(1)
            );
        }


        return null;
    }


    private BigDecimal converterDinheiro(
            String valor
    ) {

        String valorNormalizado =
                valor.replace(
                                ".",
                                ""
                        )
                        .replace(
                                ",",
                                "."
                        );


        return new BigDecimal(
                valorNormalizado
        );
    }


    public static void main(
            String[] args
    ) {

        SublimeLeiloesBrowser browser =
                new SublimeLeiloesBrowser();


        String url =
                "https://www.sublimeleiloes.com.br/lote/casa-em-sorocaba/2351/";


        String texto =
                browser.buscarTextoRenderizado(
                        url
                );


        DadosDinamicosLeilaoDTO dados =
                browser.extrairDados(
                        texto
                );


        System.out.println(
                "=== DADOS DINÂMICOS ==="
        );


        System.out.println(
                dados
        );
    }
}
