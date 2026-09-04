package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.dto.ResultadoTriagemLoteDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.LeiloeiroProvider;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteDescobertoDTO;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class TriagemLoteService {

    private static final Set<String> CIDADES_PERMITIDAS =
            Set.of(
                    "sorocaba",
                    "votorantim"
            );

    private static final Set<String> INDICADORES_DE_IMOVEL =
            Set.of(
                    "imovel",
                    "residencial",
                    "residenciais",
                    "comercial",
                    "comerciais",
                    "terreno",
                    "terrenos",
                    "casa",
                    "casas",
                    "apartamento",
                    "apartamentos",
                    "area",
                    "predio",
                    "galpao",
                    "sala",
                    "loja",
                    "sitio",
                    "fazenda",
                    "rural",
                    "rurais",
                    "urbano",
                    "urbanos"
            );

    private static final Set<String> INDICADORES_DE_BEM_MOVEL =
            Set.of(
                    "veiculo",
                    "veiculos",
                    "carro",
                    "carros",
                    "moto",
                    "motos",
                    "caminhao",
                    "caminhoes",
                    "onibus",
                    "aeronave",
                    "aeronaves",
                    "joia",
                    "joias",
                    "eletrodomestico",
                    "eletrodomesticos",
                    "eletronico",
                    "eletronicos",
                    "mobiliario",
                    "vestuario"
            );

    private final List<LeiloeiroProvider> providers;


    public TriagemLoteService(
            List<LeiloeiroProvider> providers
    ) {

        this.providers =
                List.copyOf(
                        providers
                );
    }

    public ResultadoTriagemLoteDTO avaliar(
            LoteDescobertoDTO lote
    ) {

        if (lote == null) {

            return new ResultadoTriagemLoteDTO(
                    false,
                    "Lote não informado pela fonte.",
                    null
            );
        }


        if (!cidadePermitida(
                lote.cidade()
        )) {

            return new ResultadoTriagemLoteDTO(
                    false,
                    "Cidade fora do escopo: somente Sorocaba e Votorantim são elegíveis.",
                    normalizarUrlComSeguranca(
                            lote.url()
                    )
            );
        }


        String urlNormalizada;


        try {

            urlNormalizada =
                    normalizarUrl(
                            lote.url()
                    );

        } catch (IllegalArgumentException exception) {

            return new ResultadoTriagemLoteDTO(
                    false,
                    "URL inválida ou fora das fontes oficiais permitidas.",
                    null
            );
        }


        String titulo =
                normalizarTexto(
                        valorOuVazio(lote.titulo())
                );

        String texto =
                textoDoLote(
                        lote
                );


        if (contemPalavra(
                texto,
                "despejo"
        )) {

            return new ResultadoTriagemLoteDTO(
                    false,
                    "Ação de despejo não representa oportunidade imobiliária.",
                    urlNormalizada
            );
        }


        boolean tituloDeImovel =
                INDICADORES_DE_IMOVEL
                        .stream()
                        .anyMatch(indicador ->
                                contemPalavra(
                                        titulo,
                                        indicador
                                )
                        );

        boolean bemMovelNoTitulo =
                INDICADORES_DE_BEM_MOVEL
                        .stream()
                        .anyMatch(indicador ->
                                contemPalavra(
                                        titulo,
                                        indicador
                                )
                        );

        boolean bemMovelNoTexto =
                INDICADORES_DE_BEM_MOVEL
                        .stream()
                        .anyMatch(indicador ->
                                contemPalavra(
                                        texto,
                                        indicador
                                )
                        );


        if (bemMovelNoTitulo
                || !tituloDeImovel && bemMovelNoTexto) {

            return new ResultadoTriagemLoteDTO(
                    false,
                    "Bem móvel identificado na descrição do lote.",
                    urlNormalizada
            );
        }


        boolean imovel =
                tituloDeImovel
                        || INDICADORES_DE_IMOVEL
                        .stream()
                        .anyMatch(indicador ->
                                contemPalavra(
                                        texto,
                                        indicador
                                )
                        );


        if (!imovel) {

            return new ResultadoTriagemLoteDTO(
                    false,
                    "O lote não contém indicador objetivo de imóvel.",
                    urlNormalizada
            );
        }


        return new ResultadoTriagemLoteDTO(
                true,
                "Imóvel localizado em Sorocaba ou Votorantim.",
                urlNormalizada
        );
    }

    public boolean elegivel(
            LoteDescobertoDTO lote
    ) {

        return avaliar(
                lote
        ).elegivel();
    }

    public String normalizarUrl(
            String url
    ) {

        if (url == null
                || url.isBlank()) {

            throw new IllegalArgumentException(
                    "URL do lote é obrigatória"
            );
        }


        try {

            URI uri =
                    new URI(
                            url.trim()
                    );


            LeiloeiroProvider provider =
                    providers.stream()
                            .filter(item ->
                                    item.suporta(
                                            uri
                                    )
                            )
                            .findFirst()
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "URL fora das fontes oficiais permitidas"
                                    )
                            );


            return provider.normalizarUrl(
                    uri
            );

        } catch (URISyntaxException exception) {

            throw new IllegalArgumentException(
                    "URL de lote inválida",
                    exception
            );
        }
    }

    private boolean cidadePermitida(
            String cidade
    ) {

        return CIDADES_PERMITIDAS.contains(
                normalizarTexto(
                        cidade
                ).trim()
        );
    }

    private String normalizarUrlComSeguranca(
            String url
    ) {

        try {

            return normalizarUrl(
                    url
            );

        } catch (IllegalArgumentException exception) {

            return null;
        }
    }

    private String textoDoLote(
            LoteDescobertoDTO lote
    ) {

        return normalizarTexto(
                String.join(
                        " ",
                        valorOuVazio(
                                lote.titulo()
                        ),
                        valorOuVazio(
                                lote.resumo()
                        )
                )
        );
    }

    private boolean contemPalavra(
            String texto,
            String palavra
    ) {

        Pattern pattern =
                Pattern.compile(
                        "(^|[^a-z0-9])"
                                + Pattern.quote(
                                palavra
                        )
                                + "([^a-z0-9]|$)"
                );


        return pattern.matcher(
                texto
        ).find();
    }

    private String normalizarTexto(
            String texto
    ) {

        if (texto == null) {
            return "";
        }


        return Normalizer.normalize(
                        texto,
                        Normalizer.Form.NFD
                )
                .replaceAll(
                        "\\p{M}",
                        ""
                )
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private String valorOuVazio(
            String valor
    ) {

        return valor == null
                ? ""
                : valor;
    }
}
