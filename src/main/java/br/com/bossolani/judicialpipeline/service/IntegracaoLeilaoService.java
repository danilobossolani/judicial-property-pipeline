package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.exception.LoteDescartadoException;
import br.com.bossolani.judicialpipeline.integration.DataJudClient;
import br.com.bossolani.judicialpipeline.integration.datajud.dto.DataJudProcessoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.ColetaLeiloeiroDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.LeiloeiroProvider;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteEnriquecidoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@Service
public class IntegracaoLeilaoService {

    private static final Logger log =
            LoggerFactory.getLogger(IntegracaoLeilaoService.class);

    private static final int TAMANHO_NUMERO_PROCESSO_CNJ = 20;

    private final List<LeiloeiroProvider> providers;
    private final DataJudClient dataJudClient;

    public IntegracaoLeilaoService(
            List<LeiloeiroProvider> providers,
            DataJudClient dataJudClient
    ) {
        this.providers = List.copyOf(providers);
        this.dataJudClient = dataJudClient;
    }

    public LoteEnriquecidoDTO buscarLote(String url) throws IOException {
        LeiloeiroProvider provider = resolverProvider(url);
        ColetaLeiloeiroDTO coleta;

        try {
            coleta = provider.coletar(url);
        } catch (LoteDescartadoException exception) {
            throw exception;
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException(
                    "Falha ao coletar dados da fonte " + provider.nome(),
                    exception
            );
        }

        DataJudProcessoDTO processo = null;
        boolean processoConfirmado = false;
        String numeroFonte = normalizarNumeroProcesso(
                coleta.lote().getNumeroProcesso()
        );

        if (numeroFonte.length() == TAMANHO_NUMERO_PROCESSO_CNJ) {
            try {
                processo = dataJudClient.buscarProcesso(numeroFonte);
                String numeroDataJud = normalizarNumeroProcesso(
                        processo.numeroProcesso()
                );
                processoConfirmado = numeroFonte.equals(numeroDataJud);
            } catch (RuntimeException exception) {
                processo = null;
                processoConfirmado = false;
                log.warn(
                        "Não foi possível confirmar o processo {} no DataJud durante a coleta da fonte '{}': {}",
                        numeroFonte,
                        provider.nome(),
                        exception.getMessage()
                );
            }
        }

        return new LoteEnriquecidoDTO(
                coleta.lote(),
                coleta.leilao(),
                processo,
                processoConfirmado,
                provider.nome(),
                provider.tipoFonte()
        );
    }

    private LeiloeiroProvider resolverProvider(String url) {
        URI uri;

        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "URL de fonte inválida",
                    exception
            );
        }

        return providers.stream()
                .filter(provider -> provider.suporta(uri))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nenhum provedor de fonte suporta a URL informada"
                ));
    }

    private String normalizarNumeroProcesso(String numeroProcesso) {
        return numeroProcesso == null
                ? ""
                : numeroProcesso.replaceAll("\\D", "");
    }
}
