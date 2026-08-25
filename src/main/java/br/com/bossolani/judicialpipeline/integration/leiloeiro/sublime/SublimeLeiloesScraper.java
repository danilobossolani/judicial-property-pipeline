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

        Pattern padraoProcesso = Pattern.compile(
                "\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}"
        );

        Matcher matcher = padraoProcesso.matcher(textoPagina);

        if (matcher.find()) {
            return matcher.group();
        }

        return null;
    }

    public String extrairValorAvaliacao(Document pagina) {

        String textoPagina = pagina.text();

        Pattern padraoAvaliacao = Pattern.compile(
                "Avaliação:\\s*R\\$\\s*([\\d.,]+)"
        );

        Matcher matcher = padraoAvaliacao.matcher(textoPagina);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    public String extrairComarca(Document pagina) {

        String textoPagina = pagina.text();

        Pattern padraoComarca = Pattern.compile(
                "Comarca:\\s*(.*?)\\s+Vara:"
        );

        Matcher matcher = padraoComarca.matcher(textoPagina);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    public String extrairVara(Document pagina) {

        String textoPagina = pagina.text();

        Pattern padraoVara = Pattern.compile(
                "Vara:\\s*(.*?)\\s+Autor:"
        );

        Matcher matcher = padraoVara.matcher(textoPagina);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    public String extrairEndereco(Document pagina) {

        String textoPagina = pagina.text();

        Pattern padraoEndereco = Pattern.compile(
                "Localização\\s+(.*?)\\s+Receba as melhores ofertas",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = padraoEndereco.matcher(textoPagina);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    private BigDecimal converterValorMonetario(String valor) {

        if (valor == null) {
            return null;
        }

        String valorNormalizado = valor
                .replace(".", "")
                .replace(",", ".");

        return new BigDecimal(valorNormalizado);
    }

    public LoteLeilaoDTO extrairLote(Document pagina, String url) {

        String numeroProcesso = extrairNumeroProcesso(pagina);
        String valorTexto = extrairValorAvaliacao(pagina);

        BigDecimal valorAvaliacao = converterValorMonetario(valorTexto);

        String comarca = extrairComarca(pagina);
        String vara = extrairVara(pagina);
        String endereco = extrairEndereco(pagina);

        return new LoteLeilaoDTO(
                numeroProcesso,
                valorAvaliacao,
                comarca,
                vara,
                endereco,
                url
        );
    }

    public static void main(String[] args) throws IOException {

        SublimeLeiloesScraper scraper = new SublimeLeiloesScraper();

        String url =
                "https://www.sublimeleiloes.com.br/lote/casa-em-sorocaba/2351/";

        Document pagina = scraper.buscarPagina(url);

        System.out.println("Título:");
        System.out.println(pagina.title());

        System.out.println();

        System.out.println("Processo encontrado:");
        System.out.println(scraper.extrairNumeroProcesso(pagina));

        System.out.println();

        System.out.println("Avaliação encontrada:");
        System.out.println(scraper.extrairValorAvaliacao(pagina));

        System.out.println();

        System.out.println("Comarca encontrada:");
        System.out.println(scraper.extrairComarca(pagina));

        System.out.println();

        System.out.println("Vara encontrada:");
        System.out.println(scraper.extrairVara(pagina));

        System.out.println();

        System.out.println("Endereço encontrado:");
        System.out.println(scraper.extrairEndereco(pagina));

        System.out.println();
        System.out.println("=== LOTE COMPLETO ===");

        LoteLeilaoDTO lote = scraper.extrairLote(pagina, url);

        System.out.println(lote);
    }
}