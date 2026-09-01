package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.dto.CentralAuditoriaDTO;
import br.com.bossolani.judicialpipeline.dto.DescobertaAuditoriaDTO;
import br.com.bossolani.judicialpipeline.dto.EventoAuditoriaDTO;
import br.com.bossolani.judicialpipeline.dto.ExecucaoDescobertaAuditoriaDTO;
import br.com.bossolani.judicialpipeline.dto.LoteDescobertaAuditoriaDTO;
import br.com.bossolani.judicialpipeline.model.Acompanhamento;
import br.com.bossolani.judicialpipeline.model.ExecucaoDescoberta;
import br.com.bossolani.judicialpipeline.model.HistoricoAcompanhamento;
import br.com.bossolani.judicialpipeline.model.Imovel;
import br.com.bossolani.judicialpipeline.model.Processo;
import br.com.bossolani.judicialpipeline.model.ResultadoLoteDescoberta;
import br.com.bossolani.judicialpipeline.repository.ExecucaoDescobertaRepository;
import br.com.bossolani.judicialpipeline.repository.HistoricoAcompanhamentoRepository;
import br.com.bossolani.judicialpipeline.repository.ResultadoLoteDescobertaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class AuditoriaService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    AuditoriaService.class
            );

    private final HistoricoAcompanhamentoRepository historicoRepository;
    private final ExecucaoDescobertaRepository execucaoRepository;
    private final ResultadoLoteDescobertaRepository resultadoLoteRepository;

    public AuditoriaService(
            HistoricoAcompanhamentoRepository historicoRepository,
            ExecucaoDescobertaRepository execucaoRepository,
            ResultadoLoteDescobertaRepository resultadoLoteRepository
    ) {

        this.historicoRepository =
                historicoRepository;

        this.execucaoRepository =
                execucaoRepository;

        this.resultadoLoteRepository =
                resultadoLoteRepository;
    }

    @Transactional(readOnly = true)
    public CentralAuditoriaDTO carregarCentral() {

        List<EventoAuditoriaDTO> eventos =
                historicoRepository
                        .findTop200ByOrderByDataEventoDesc()
                        .stream()
                        .map(this::mapearEvento)
                        .toList();


        int totalManuais =
                (int) eventos
                        .stream()
                        .filter(evento ->
                                evento.categoria()
                                        .equals("MANUAL")
                        )
                        .count();


        long totalImoveisImpactados =
                eventos
                        .stream()
                        .map(EventoAuditoriaDTO::imovelId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .count();


        DescobertaAuditoriaDTO descoberta =
                carregarDescobertaComSeguranca();


        LocalDateTime ultimaAtividadeImoveis =
                eventos.isEmpty()
                        ? null
                        : eventos.getFirst().dataEvento();

        LocalDateTime ultimaAtividadeDescoberta =
                descoberta.ultimaExecucao() != null
                        ? descoberta.ultimaExecucao().inicio()
                        : null;


        return new CentralAuditoriaDTO(
                eventos,
                eventos.size(),
                totalManuais,
                eventos.size() - totalManuais,
                totalImoveisImpactados,
                dataMaisRecente(
                        ultimaAtividadeImoveis,
                        ultimaAtividadeDescoberta
                ),
                descoberta
        );
    }

    private DescobertaAuditoriaDTO carregarDescobertaComSeguranca() {

        try {

            List<ExecucaoDescobertaAuditoriaDTO> execucoes =
                    execucaoRepository
                            .findTop20ByOrderByInicioDesc()
                            .stream()
                            .map(this::mapearExecucao)
                            .toList();


            return new DescobertaAuditoriaDTO(
                    execucoes,
                    execucoes.isEmpty()
                            ? null
                            : execucoes.getFirst(),
                    execucoes.size(),
                    execucoes.stream()
                            .mapToInt(
                                    ExecucaoDescobertaAuditoriaDTO::totalEncontrado
                            )
                            .sum(),
                    execucoes.stream()
                            .mapToInt(
                                    ExecucaoDescobertaAuditoriaDTO::totalImportado
                            )
                            .sum(),
                    execucoes.stream()
                            .mapToInt(
                                    ExecucaoDescobertaAuditoriaDTO::totalDuplicado
                            )
                            .sum(),
                    execucoes.stream()
                            .mapToInt(
                                    ExecucaoDescobertaAuditoriaDTO::totalDescartado
                            )
                            .sum(),
                    execucoes.stream()
                            .mapToInt(
                                    ExecucaoDescobertaAuditoriaDTO::totalFalha
                            )
                            .sum(),
                    null
            );

        } catch (Exception exception) {

            log.error(
                    "Não foi possível carregar a auditoria da descoberta automática.",
                    exception
            );


            return new DescobertaAuditoriaDTO(
                    List.of(),
                    null,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    "Não foi possível carregar o histórico da descoberta automática. Tente novamente em instantes."
            );
        }
    }

    private ExecucaoDescobertaAuditoriaDTO mapearExecucao(
            ExecucaoDescoberta execucao
    ) {

        List<LoteDescobertaAuditoriaDTO> lotes =
                resultadoLoteRepository
                        .findByExecucaoIdOrderByIdAsc(
                                execucao.getId()
                        )
                        .stream()
                        .map(this::mapearLote)
                        .toList();


        return new ExecucaoDescobertaAuditoriaDTO(
                execucao.getId(),
                execucao.getFonte(),
                execucao.getInicio(),
                execucao.getTermino(),
                execucao.getDuracaoMs(),
                execucao.getOrigem(),
                execucao.getStatus(),
                execucao.getTotalEncontrado(),
                execucao.getTotalElegivel(),
                execucao.getTotalImportado(),
                execucao.getTotalDuplicado(),
                execucao.getTotalDescartado(),
                execucao.getTotalFalha(),
                execucao.getErroResumo(),
                lotes
        );
    }

    private LoteDescobertaAuditoriaDTO mapearLote(
            ResultadoLoteDescoberta resultado
    ) {

        Imovel imovel =
                resultado.getImovel();

        Processo processoRelacionado =
                imovel != null
                        ? imovel.getProcesso()
                        : null;


        return new LoteDescobertaAuditoriaDTO(
                resultado.getId(),
                resultado.getFonte(),
                resultado.getTitulo(),
                resultado.getCidade(),
                resultado.getUrlOriginal(),
                resultado.getUrlNormalizada(),
                resultado.getNumeroProcesso() != null
                        ? resultado.getNumeroProcesso()
                        : processoRelacionado != null
                        ? processoRelacionado.getNumeroProcesso()
                        : null,
                resultado.getDecisao(),
                resultado.getMotivo(),
                imovel != null
                        ? imovel.getId()
                        : null
        );
    }

    private EventoAuditoriaDTO mapearEvento(
            HistoricoAcompanhamento historico
    ) {

        Acompanhamento acompanhamento =
                historico.getAcompanhamento();

        Imovel imovel =
                acompanhamento != null
                        ? acompanhamento.getImovel()
                        : null;

        Processo processo =
                imovel != null
                        ? imovel.getProcesso()
                        : null;


        return new EventoAuditoriaDTO(
                historico.getDataEvento(),
                imovel != null
                        ? imovel.getId()
                        : null,
                processo != null
                        ? processo.getNumeroProcesso()
                        : null,
                imovel != null
                        ? imovel.getTipo()
                        : null,
                imovel != null
                        ? imovel.getBairro()
                        : null,
                imovel != null
                        ? imovel.getCidade()
                        : null,
                historico.getStatusPipeline(),
                historico.getStatusLeilao(),
                historico.getResultadoLeilao(),
                historico.getOrigem(),
                historico.getDescricao()
        );
    }

    private LocalDateTime dataMaisRecente(
            LocalDateTime primeira,
            LocalDateTime segunda
    ) {

        if (primeira == null) {
            return segunda;
        }


        if (segunda == null) {
            return primeira;
        }


        return primeira.isAfter(
                segunda
        )
                ? primeira
                : segunda;
    }
}
