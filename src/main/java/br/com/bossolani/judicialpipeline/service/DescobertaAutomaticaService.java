package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.dto.ResultadoDescobertaDTO;
import br.com.bossolani.judicialpipeline.dto.ResultadoTriagemLoteDTO;
import br.com.bossolani.judicialpipeline.exception.DescobertaEmAndamentoException;
import br.com.bossolani.judicialpipeline.exception.LoteDescartadoException;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteDescobertoDTO;
import br.com.bossolani.judicialpipeline.integration.leiloeiro.sublime.SublimeLeiloesDiscoveryBrowser;
import br.com.bossolani.judicialpipeline.model.DecisaoLoteDescoberta;
import br.com.bossolani.judicialpipeline.model.ExecucaoDescoberta;
import br.com.bossolani.judicialpipeline.model.Fonte;
import br.com.bossolani.judicialpipeline.model.Imovel;
import br.com.bossolani.judicialpipeline.model.Leilao;
import br.com.bossolani.judicialpipeline.model.OrigemExecucaoDescoberta;
import br.com.bossolani.judicialpipeline.model.Processo;
import br.com.bossolani.judicialpipeline.model.ResultadoLoteDescoberta;
import br.com.bossolani.judicialpipeline.model.StatusExecucaoDescoberta;
import br.com.bossolani.judicialpipeline.repository.ExecucaoDescobertaRepository;
import br.com.bossolani.judicialpipeline.repository.FonteRepository;
import br.com.bossolani.judicialpipeline.repository.ResultadoLoteDescobertaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class DescobertaAutomaticaService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    DescobertaAutomaticaService.class
            );

    private static final String FONTE =
            "Sublime Leilões";

    private final SublimeLeiloesDiscoveryBrowser discoveryBrowser;
    private final TriagemLoteService triagemLoteService;
    private final FonteRepository fonteRepository;
    private final PersistenciaLeilaoService persistenciaLeilaoService;
    private final ExecucaoDescobertaRepository execucaoRepository;
    private final ResultadoLoteDescobertaRepository resultadoLoteRepository;
    private final AtomicBoolean emExecucao =
            new AtomicBoolean(false);

    public DescobertaAutomaticaService(
            SublimeLeiloesDiscoveryBrowser discoveryBrowser,
            TriagemLoteService triagemLoteService,
            FonteRepository fonteRepository,
            PersistenciaLeilaoService persistenciaLeilaoService,
            ExecucaoDescobertaRepository execucaoRepository,
            ResultadoLoteDescobertaRepository resultadoLoteRepository
    ) {

        this.discoveryBrowser =
                discoveryBrowser;

        this.triagemLoteService =
                triagemLoteService;

        this.fonteRepository =
                fonteRepository;

        this.persistenciaLeilaoService =
                persistenciaLeilaoService;

        this.execucaoRepository =
                execucaoRepository;

        this.resultadoLoteRepository =
                resultadoLoteRepository;
    }

    @Scheduled(
            initialDelayString = "${descoberta.sublime.atraso-inicial-ms:120000}",
            fixedDelayString = "${descoberta.sublime.intervalo-ms:21600000}"
    )
    public void executarAgendamento() {

        try {

            ResultadoDescobertaDTO resultado =
                    executarDescoberta(
                            OrigemExecucaoDescoberta.AGENDADA
                    );


            log.info(
                    "Descoberta automática concluída. Execução={}, status={}, encontrados={}, elegíveis={}, importados={}, duplicados={}, descartados={}, falhas={}.",
                    resultado.execucaoId(),
                    resultado.status(),
                    resultado.encontrados(),
                    resultado.elegiveis(),
                    resultado.importados(),
                    resultado.duplicados(),
                    resultado.descartados(),
                    resultado.falhas()
            );

        } catch (DescobertaEmAndamentoException exception) {

            log.info(
                    "Agendamento ignorado porque já existe uma descoberta em execução."
            );
        }
    }

    public ResultadoDescobertaDTO executarDescoberta() {

        return executarDescoberta(
                OrigemExecucaoDescoberta.MANUAL
        );
    }

    public ResultadoDescobertaDTO executarDescoberta(
            OrigemExecucaoDescoberta origem
    ) {

        if (!emExecucao.compareAndSet(
                false,
                true
        )) {

            throw new DescobertaEmAndamentoException();
        }


        ExecucaoDescoberta execucao =
                iniciarExecucao(
                        origem
                );


        try {

            execucaoRepository.save(
                    execucao
            );


            log.info(
                    "Iniciando execução {} da descoberta de lotes imobiliários da Sublime em Sorocaba e Votorantim.",
                    execucao.getId()
            );


            List<LoteDescobertoDTO> lotes =
                    Optional.ofNullable(
                            discoveryBrowser.descobrirLotes()
                    ).orElseGet(
                            List::of
                    );


            execucao.setTotalEncontrado(
                    lotes.size()
            );


            execucaoRepository.save(
                    execucao
            );


            for (LoteDescobertoDTO lote : lotes) {

                processarLote(
                        execucao,
                        lote
                );
            }


            execucao.setStatus(
                    execucao.getTotalFalha() > 0
                            ? StatusExecucaoDescoberta.CONCLUIDA_COM_FALHAS
                            : StatusExecucaoDescoberta.CONCLUIDA
            );


            finalizarExecucao(
                    execucao
            );


            execucaoRepository.save(
                    execucao
            );


            return ResultadoDescobertaDTO.de(
                    execucao
            );

        } catch (Exception exception) {

            execucao.setStatus(
                    StatusExecucaoDescoberta.FALHOU
            );

            execucao.setErroResumo(
                    resumirErro(
                            exception
                    )
            );


            finalizarExecucao(
                    execucao
            );


            salvarFalhaFatal(
                    execucao,
                    exception
            );


            return ResultadoDescobertaDTO.de(
                    execucao
            );

        } finally {

            emExecucao.set(
                    false
            );
        }
    }

    public boolean estaEmExecucao() {

        return emExecucao.get();
    }

    private ExecucaoDescoberta iniciarExecucao(
            OrigemExecucaoDescoberta origem
    ) {

        ExecucaoDescoberta execucao =
                new ExecucaoDescoberta();


        execucao.setFonte(
                FONTE
        );

        execucao.setInicio(
                LocalDateTime.now()
        );

        execucao.setOrigem(
                origem == null
                        ? OrigemExecucaoDescoberta.MANUAL
                        : origem
        );

        execucao.setStatus(
                StatusExecucaoDescoberta.EM_EXECUCAO
        );


        return execucao;
    }

    private void processarLote(
            ExecucaoDescoberta execucao,
            LoteDescobertoDTO lote
    ) {

        ResultadoLoteDescoberta resultado =
                novoResultado(
                        execucao,
                        lote
                );


        ResultadoTriagemLoteDTO triagem =
                triagemLoteService.avaliar(
                        lote
                );


        resultado.setUrlNormalizada(
                triagem.urlNormalizada()
        );


        if (!triagem.elegivel()) {

            resultado.setDecisao(
                    DecisaoLoteDescoberta.DESCARTADO
            );

            resultado.setMotivo(
                    triagem.motivo()
            );

            execucao.setTotalDescartado(
                    execucao.getTotalDescartado() + 1
            );


            persistirProgresso(
                    execucao,
                    resultado
            );


            return;
        }


        execucao.setTotalElegivel(
                execucao.getTotalElegivel() + 1
        );


        String urlNormalizada =
                triagem.urlNormalizada();


        Optional<Fonte> fonteExistente =
                fonteRepository.findByUrlOrigem(
                        urlNormalizada
                );


        if (fonteExistente.isPresent()) {

            Fonte fonte =
                    fonteExistente.get();


            relacionarResultado(
                    resultado,
                    fonte
            );

            resultado.setDecisao(
                    DecisaoLoteDescoberta.DUPLICADO
            );

            resultado.setMotivo(
                    "URL normalizada já cadastrada como fonte do pipeline."
            );

            execucao.setTotalDuplicado(
                    execucao.getTotalDuplicado() + 1
            );


            persistirProgresso(
                    execucao,
                    resultado
            );


            return;
        }


        try {

            Fonte fonte =
                    persistenciaLeilaoService.coletarESalvar(
                            urlNormalizada
                    );


            Imovel imovel =
                    relacionarResultado(
                            resultado,
                            fonte
                    );


            boolean processoDuplicado =
                    imovel != null
                            && imovel.getId() != null
                            && fonteRepository.countByLeilaoImovelId(
                            imovel.getId()
                    ) > 1;


            if (processoDuplicado) {

                resultado.setDecisao(
                        DecisaoLoteDescoberta.DUPLICADO
                );

                resultado.setMotivo(
                        "Número do processo já cadastrado; a nova fonte foi preservada e vinculada ao imóvel existente."
                );

                execucao.setTotalDuplicado(
                        execucao.getTotalDuplicado() + 1
                );

            } else {

                resultado.setDecisao(
                        DecisaoLoteDescoberta.IMPORTADO
                );

                resultado.setMotivo(
                        "Lote elegível importado e relacionado ao imóvel."
                );

                execucao.setTotalImportado(
                        execucao.getTotalImportado() + 1
                );
            }

        } catch (LoteDescartadoException exception) {

            resultado.setDecisao(
                    DecisaoLoteDescoberta.DESCARTADO
            );

            resultado.setMotivo(
                    exception.getMessage()
            );

            execucao.setTotalElegivel(
                    Math.max(
                            0,
                            execucao.getTotalElegivel() - 1
                    )
            );

            execucao.setTotalDescartado(
                    execucao.getTotalDescartado() + 1
            );

        } catch (Exception exception) {

            String erro =
                    resumirErro(
                            exception
                    );


            resultado.setDecisao(
                    DecisaoLoteDescoberta.FALHA
            );

            resultado.setMotivo(
                    "Falha ao coletar ou persistir o lote: " + erro
            );

            execucao.setTotalFalha(
                    execucao.getTotalFalha() + 1
            );


            if (execucao.getErroResumo() == null) {

                execucao.setErroResumo(
                        limitarTexto(
                                "Falha em " + urlNormalizada + ": " + erro,
                                1000
                        )
                );
            }


            log.error(
                    "Falha ao importar lote descoberto '{}': {}",
                    urlNormalizada,
                    exception.getMessage(),
                    exception
            );
        }


        persistirProgresso(
                execucao,
                resultado
        );
    }

    private ResultadoLoteDescoberta novoResultado(
            ExecucaoDescoberta execucao,
            LoteDescobertoDTO lote
    ) {

        ResultadoLoteDescoberta resultado =
                new ResultadoLoteDescoberta();


        resultado.setExecucao(
                execucao
        );


        if (lote != null) {

            resultado.setTitulo(
                    lote.titulo()
            );

            resultado.setCidade(
                    lote.cidade()
            );

            resultado.setUrlOriginal(
                    lote.url()
            );
        }


        return resultado;
    }

    private Imovel relacionarResultado(
            ResultadoLoteDescoberta resultado,
            Fonte fonte
    ) {

        Leilao leilao =
                fonte != null
                        ? fonte.getLeilao()
                        : null;

        Imovel imovel =
                leilao != null
                        ? leilao.getImovel()
                        : null;

        Processo processo =
                imovel != null
                        ? imovel.getProcesso()
                        : fonte != null
                        ? fonte.getProcesso()
                        : null;


        resultado.setImovel(
                imovel
        );

        resultado.setNumeroProcesso(
                processo != null
                        ? processo.getNumeroProcesso()
                        : null
        );


        return imovel;
    }

    private void persistirProgresso(
            ExecucaoDescoberta execucao,
            ResultadoLoteDescoberta resultado
    ) {

        resultadoLoteRepository.save(
                resultado
        );

        execucaoRepository.save(
                execucao
        );
    }

    private void finalizarExecucao(
            ExecucaoDescoberta execucao
    ) {

        LocalDateTime termino =
                LocalDateTime.now();


        execucao.setTermino(
                termino
        );

        execucao.setDuracaoMs(
                Math.max(
                        0,
                        Duration.between(
                                execucao.getInicio(),
                                termino
                        ).toMillis()
                )
        );
    }

    private void salvarFalhaFatal(
            ExecucaoDescoberta execucao,
            Exception causa
    ) {

        try {

            execucaoRepository.save(
                    execucao
            );

        } catch (Exception exception) {

            log.error(
                    "Não foi possível persistir o encerramento da execução de descoberta após a falha fatal.",
                    exception
            );
        }


        log.error(
                "A execução {} da descoberta falhou antes de concluir os lotes: {}",
                execucao.getId(),
                causa.getMessage(),
                causa
        );
    }

    private String resumirErro(
            Exception exception
    ) {

        String mensagem =
                exception.getMessage();


        if (mensagem == null
                || mensagem.isBlank()) {

            mensagem =
                    exception.getClass()
                            .getSimpleName();
        }


        String resumo =
                mensagem.replaceAll(
                        "\\s+",
                        " "
                ).trim();


        return limitarTexto(
                resumo,
                900
        );
    }

    private String limitarTexto(
            String valor,
            int limite
    ) {

        return valor.length() > limite
                ? valor.substring(
                0,
                limite
        )
                : valor;
    }
}
