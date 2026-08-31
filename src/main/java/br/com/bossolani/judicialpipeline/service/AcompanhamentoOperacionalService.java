package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.model.Acompanhamento;
import br.com.bossolani.judicialpipeline.model.HistoricoAcompanhamento;
import br.com.bossolani.judicialpipeline.model.Leilao;
import br.com.bossolani.judicialpipeline.model.StatusPipeline;
import br.com.bossolani.judicialpipeline.repository.AcompanhamentoRepository;
import br.com.bossolani.judicialpipeline.repository.HistoricoAcompanhamentoRepository;
import br.com.bossolani.judicialpipeline.repository.LeilaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class AcompanhamentoOperacionalService {

    private static final int TAMANHO_MAXIMO_OBSERVACAO = 2000;

    private final AcompanhamentoRepository acompanhamentoRepository;
    private final HistoricoAcompanhamentoRepository historicoAcompanhamentoRepository;
    private final LeilaoRepository leilaoRepository;


    public AcompanhamentoOperacionalService(
            AcompanhamentoRepository acompanhamentoRepository,
            HistoricoAcompanhamentoRepository historicoAcompanhamentoRepository,
            LeilaoRepository leilaoRepository
    ) {

        this.acompanhamentoRepository =
                acompanhamentoRepository;

        this.historicoAcompanhamentoRepository =
                historicoAcompanhamentoRepository;

        this.leilaoRepository =
                leilaoRepository;
    }


    @Transactional
    public void atualizar(
            Long imovelId,
            StatusPipeline novoStatus,
            String observacao
    ) {

        if (novoStatus == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status do acompanhamento é obrigatório"
            );
        }


        validarStatusManual(
                novoStatus
        );


        String observacaoTratada =
                tratarObservacao(
                        observacao
                );


        Acompanhamento acompanhamento =
                acompanhamentoRepository
                        .findByImovelId(
                                imovelId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Acompanhamento do imóvel não encontrado"
                                )
                        );


        StatusPipeline statusAnterior =
                acompanhamento.getStatusPipeline();


        String observacaoAnterior =
                normalizarObservacao(
                        acompanhamento.getObservacao()
                );


        boolean statusMudou =
                statusAnterior != novoStatus;


        boolean observacaoMudou =
                !Objects.equals(
                        observacaoAnterior,
                        observacaoTratada
                );


        /*
         * Evita criar eventos duplicados caso o usuário
         * aperte "Salvar" sem modificar nada.
         */
        if (!statusMudou
                && !observacaoMudou) {

            return;
        }


        acompanhamento.setStatusPipeline(
                novoStatus
        );


        acompanhamento.setObservacao(
                observacaoTratada
        );


        /*
         * ENCERRADO e DESCARTADO deixam de ser considerados
         * acompanhamentos ativos.
         */
        acompanhamento.setAtivo(
                novoStatus != StatusPipeline.ENCERRADO
                        && novoStatus != StatusPipeline.DESCARTADO
        );


        acompanhamentoRepository.save(
                acompanhamento
        );


        registrarHistoricoManual(
                acompanhamento,
                imovelId,
                statusAnterior,
                novoStatus,
                observacaoTratada,
                statusMudou,
                observacaoMudou
        );
    }


    /*
     * Status que fazem sentido para operação humana.
     *
     * IDENTIFICADO fica de fora porque representa o estágio
     * inicial produzido automaticamente pelo sistema.
     */
    private void validarStatusManual(
            StatusPipeline status
    ) {

        boolean permitido =
                status == StatusPipeline.MONITORANDO_LEILAO
                        || status == StatusPipeline.AGUARDANDO_RESULTADO
                        || status == StatusPipeline.MONITORANDO_PROCESSO
                        || status == StatusPipeline.EM_ANALISE
                        || status == StatusPipeline.OPORTUNIDADE
                        || status == StatusPipeline.DESCARTADO
                        || status == StatusPipeline.ENCERRADO;


        if (!permitido) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status não permitido para alteração manual"
            );
        }
    }


    private String tratarObservacao(
            String observacao
    ) {

        String observacaoTratada =
                normalizarObservacao(
                        observacao
                );


        if (observacaoTratada != null
                && observacaoTratada.length()
                > TAMANHO_MAXIMO_OBSERVACAO) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A observação deve possuir no máximo "
                            + TAMANHO_MAXIMO_OBSERVACAO
                            + " caracteres"
            );
        }


        return observacaoTratada;
    }


    private String normalizarObservacao(
            String observacao
    ) {

        if (observacao == null) {
            return null;
        }


        String texto =
                observacao.trim();


        if (texto.isEmpty()) {
            return null;
        }


        return texto;
    }


    private void registrarHistoricoManual(
            Acompanhamento acompanhamento,
            Long imovelId,
            StatusPipeline statusAnterior,
            StatusPipeline novoStatus,
            String observacao,
            boolean statusMudou,
            boolean observacaoMudou
    ) {

        Leilao leilao =
                leilaoRepository
                        .findTopByImovelIdOrderByIdDesc(
                                imovelId
                        )
                        .orElse(null);


        HistoricoAcompanhamento historico =
                new HistoricoAcompanhamento();


        historico.setAcompanhamento(
                acompanhamento
        );


        historico.setDataEvento(
                LocalDateTime.now()
        );


        historico.setStatusPipeline(
                novoStatus
        );


        /*
         * Guardamos junto do evento manual o estado factual
         * do leilão naquele momento.
         */
        if (leilao != null) {

            historico.setStatusLeilao(
                    leilao.getStatusLeilao()
            );


            historico.setResultadoLeilao(
                    leilao.getResultadoLeilao()
            );
        }


        historico.setOrigem(
                "Atualização manual"
        );


        historico.setDescricao(
                montarDescricao(
                        statusAnterior,
                        novoStatus,
                        observacao,
                        statusMudou,
                        observacaoMudou
                )
        );


        historicoAcompanhamentoRepository.save(
                historico
        );
    }


    private String montarDescricao(
            StatusPipeline statusAnterior,
            StatusPipeline novoStatus,
            String observacao,
            boolean statusMudou,
            boolean observacaoMudou
    ) {

        StringBuilder descricao =
                new StringBuilder();


        if (statusMudou) {

            descricao
                    .append("Status alterado de ")
                    .append(
                            formatarStatus(
                                    statusAnterior
                            )
                    )
                    .append(" para ")
                    .append(
                            formatarStatus(
                                    novoStatus
                            )
                    )
                    .append(".");
        }


        if (observacaoMudou) {

            if (!descricao.isEmpty()) {
                descricao.append(" ");
            }


            if (observacao == null) {

                descricao.append(
                        "Observação removida."
                );

            } else {

                descricao
                        .append("Observação: ")
                        .append(
                                observacao
                        );
            }
        }


        return descricao.toString();
    }


    private String formatarStatus(
            StatusPipeline status
    ) {

        if (status == null) {
            return "Não informado";
        }


        return switch (status) {

            case IDENTIFICADO ->
                    "Identificado";

            case MONITORANDO_LEILAO ->
                    "Monitorando leilão";

            case AGUARDANDO_RESULTADO ->
                    "Aguardando resultado";

            case MONITORANDO_PROCESSO ->
                    "Monitorando processo";

            case EM_ANALISE ->
                    "Em análise";

            case OPORTUNIDADE ->
                    "Oportunidade";

            case DESCARTADO ->
                    "Descartado";

            case ENCERRADO ->
                    "Encerrado";
        };
    }
}