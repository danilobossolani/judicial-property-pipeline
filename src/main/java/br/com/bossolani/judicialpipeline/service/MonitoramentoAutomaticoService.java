package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.model.Acompanhamento;
import br.com.bossolani.judicialpipeline.repository.AcompanhamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MonitoramentoAutomaticoService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    MonitoramentoAutomaticoService.class
            );

    private final AcompanhamentoRepository acompanhamentoRepository;
    private final AtualizacaoImovelService atualizacaoImovelService;


    public MonitoramentoAutomaticoService(
            AcompanhamentoRepository acompanhamentoRepository,
            AtualizacaoImovelService atualizacaoImovelService
    ) {

        this.acompanhamentoRepository =
                acompanhamentoRepository;

        this.atualizacaoImovelService =
                atualizacaoImovelService;
    }


    @Scheduled(
            initialDelayString = "${monitoramento.atraso-inicial-ms:60000}",
            fixedDelayString = "${monitoramento.intervalo-ms:21600000}"
    )
    public void executarMonitoramento() {

        log.info(
                "Iniciando monitoramento automático de imóveis ativos."
        );


        List<Long> imoveisAtivos =
                buscarIdsDosImoveisAtivos();


        if (imoveisAtivos.isEmpty()) {

            log.info(
                    "Nenhum imóvel ativo para monitoramento."
            );

            return;
        }


        int sucessos = 0;
        int falhas = 0;


        for (Long imovelId : imoveisAtivos) {

            try {

                log.info(
                        "Atualizando automaticamente imóvel id={}",
                        imovelId
                );


                atualizacaoImovelService.atualizarDados(
                        imovelId
                );


                sucessos++;

            } catch (Exception e) {

                falhas++;


                /*
                 * Uma fonte quebrada NÃO pode interromper
                 * a atualização dos outros imóveis.
                 */
                log.error(
                        "Falha ao atualizar automaticamente imóvel id={}: {}",
                        imovelId,
                        e.getMessage()
                );
            }
        }


        log.info(
                "Monitoramento automático concluído. Sucessos={}, falhas={}.",
                sucessos,
                falhas
        );
    }


    /*
     * Abrimos uma transação curta apenas para ler
     * quais imóveis precisam ser atualizados.
     *
     * As chamadas externas acontecem depois,
     * fora desta transação.
     */
    @Transactional(readOnly = true)
    protected List<Long> buscarIdsDosImoveisAtivos() {

        List<Acompanhamento> acompanhamentos =
                acompanhamentoRepository
                        .findByAtivoTrue();


        return acompanhamentos
                .stream()
                .filter(acompanhamento ->
                        acompanhamento.getImovel() != null
                )
                .map(acompanhamento ->
                        acompanhamento
                                .getImovel()
                                .getId()
                )
                .distinct()
                .toList();
    }
}