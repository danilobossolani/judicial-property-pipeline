package br.com.bossolani.judicialpipeline.integration.leiloeiro.sublime;

import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteLeilaoDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SublimeLeiloesScraper {

    public Document buscarPagina(String url) throws IOException {

        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(10000)
                .get();
    }

    public String extrairNumeroProcesso(Document pagina) {

        String textoPagina = pagina.text();

        Pattern pattern = Pattern.compile(
                "\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}"
        );

        Matcher matcher =
                pattern.matcher(textoPagina);

        if (matcher.find()) {
            return matcher.group();
        }

        return null;
    }

    public String extrairValorAvaliacao(Document pagina) {

        Pattern pattern = Pattern.compile(
                "Avaliação:\\s*R\\$\\s*([\\d.,]+)"
        );

        Matcher matcher =
                pattern.matcher(pagina.text());

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    public String extrairComarca(Document pagina) {

        Pattern pattern = Pattern.compile(
                "Comarca:\\s*(.*?)\\s+Vara:"
        );

        Matcher matcher =
                pattern.matcher(pagina.text());

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    public String extrairVara(Document pagina) {

        Pattern pattern = Pattern.compile(
                "Vara:\\s*(.*?)\\s+Autor:"
        );

        Matcher matcher =
                pattern.matcher(pagina.text());

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    public String extrairEndereco(Document pagina) {

        Pattern pattern = Pattern.compile(
                "Localização\\s+(.*?)\\s+Receba as melhores ofertas",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher =
                pattern.matcher(pagina.text());

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    public String extrairNumeroEndereco(
            String endereco
    ) {

        if (endereco == null || endereco.isBlank()) {
            return null;
        }

        Pattern pattern = Pattern.compile(
                ",\\s*(\\d+[A-Za-z]?)\\s*-"
        );

        Matcher matcher =
                pattern.matcher(endereco);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    public String extrairBairro(
            String endereco,
            String cidade
    ) {

        if (endereco == null
                || endereco.isBlank()
                || cidade == null
                || cidade.isBlank()) {

            return null;
        }

        Pattern pattern = Pattern.compile(
                "-\\s*(.*?)\\s+"
                        + Pattern.quote(cidade)
                        + "\\s*-\\s*[A-Z]{2}$",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher =
                pattern.matcher(endereco);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    public String extrairTipoImovel(
            Document pagina,
            String cidade
    ) {

        if (cidade == null || cidade.isBlank()) {
            return null;
        }

        String textoPagina =
                pagina.text();

        Pattern pattern = Pattern.compile(
                "Home\\s+Residenciais\\s+(.*?)\\s+em\\s+"
                        + Pattern.quote(cidade),
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher =
                pattern.matcher(textoPagina);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    private BigDecimal converterValorMonetario(
            String valor
    ) {

        if (valor == null) {
            return null;
        }

        return new BigDecimal(
                valor
                        .replace(".", "")
                        .replace(",", ".")
        );
    }

    public LoteLeilaoDTO extrairLote(
            Document pagina,
            String url
    ) {

        String numeroProcesso =
                extrairNumeroProcesso(
                        pagina
                );

        BigDecimal valorAvaliacao =
                converterValorMonetario(
                        extrairValorAvaliacao(
                                pagina
                        )
                );

        String comarca =
                extrairComarca(
                        pagina
                );

        String vara =
                extrairVara(
                        pagina
                );

        String tipo =
                extrairTipoImovel(
                        pagina,
                        comarca
                );

        String endereco =
                extrairEndereco(
                        pagina
                );

        String numero =
                extrairNumeroEndereco(
                        endereco
                );

        String bairro =
                extrairBairro(
                        endereco,
                        comarca
                );

        return new LoteLeilaoDTO(
                numeroProcesso,
                valorAvaliacao,
                comarca,
                vara,
                tipo,
                endereco,
                numero,
                bairro,
                url
        );
    }

    public static void main(
            String[] args
    ) throws IOException {

        SublimeLeiloesScraper scraper =
                new SublimeLeiloesScraper();

        String url =
                "https://www.sublimeleiloes.com.br/lote/casa-em-sorocaba/2351/";

        Document pagina =
                scraper.buscarPagina(
                        url
                );

        LoteLeilaoDTO lote =
                scraper.extrairLote(
                        pagina,
                        url
                );

        System.out.println(
                "=== LOTE EXTRAÍDO ==="
        );

        System.out.println(
                "Tipo: " + lote.getTipo()
        );

        System.out.println(
                "Número: " + lote.getNumero()
        );

        System.out.println(
                "Bairro: " + lote.getBairro()
        );

        System.out.println(
                "Cidade: " + lote.getComarca()
        );

        System.out.println(
                "Avaliação: " + lote.getValorAvaliacao()
        );
    }
}