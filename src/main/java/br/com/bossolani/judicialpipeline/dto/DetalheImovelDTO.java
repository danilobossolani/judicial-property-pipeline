package br.com.bossolani.judicialpipeline.dto;

import br.com.bossolani.judicialpipeline.model.FonteTipo;
import br.com.bossolani.judicialpipeline.model.ResultadoLeilao;
import br.com.bossolani.judicialpipeline.model.StatusLeilao;
import br.com.bossolani.judicialpipeline.model.StatusPipeline;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public record DetalheImovelDTO(

        ImovelResumo imovel,

        ProcessoResumo processo,

        LeilaoResumo leilao,

        AcompanhamentoResumo acompanhamento,

        List<HistoricoItemDTO> historico,

        List<FonteResumo> fontes

) {

    private static final DateTimeFormatter FORMATADOR_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final DateTimeFormatter FORMATO_DATAJUD_COMPLETO =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final DateTimeFormatter FORMATO_DATAJUD_DATA =
            DateTimeFormatter.ofPattern("yyyyMMdd");


    public record ImovelResumo(

            Long id,

            String tipo,

            String endereco,

            String numero,

            String complemento,

            String bairro,

            String cidade,

            String cep,

            BigDecimal valorAvaliacao

    ) {
    }


    public record ProcessoResumo(

            String numeroProcesso,

            String comarca,

            String vara,

            String tribunal,

            String grau,

            String orgaoJulgador,

            String classe,

            String sistema,

            String formato,

            String dataAjuizamento,

            String ultimaAtualizacao

    ) {

        public String numeroProcessoFormatado() {

            if (numeroProcesso == null) {
                return "Não informado";
            }

            String numeroNormalizado =
                    numeroProcesso.replaceAll("\\D", "");

            if (numeroNormalizado.length() != 20) {
                return numeroProcesso;
            }

            return numeroNormalizado.substring(0, 7)
                    + "-"
                    + numeroNormalizado.substring(7, 9)
                    + "."
                    + numeroNormalizado.substring(9, 13)
                    + "."
                    + numeroNormalizado.substring(13, 14)
                    + "."
                    + numeroNormalizado.substring(14, 16)
                    + "."
                    + numeroNormalizado.substring(16, 20);
        }


        public String dataAjuizamentoFormatada() {

            if (dataAjuizamento == null
                    || dataAjuizamento.isBlank()) {

                return "Não informado";
            }

            try {

                if (dataAjuizamento.matches("\\d{14}")) {

                    LocalDateTime data =
                            LocalDateTime.parse(
                                    dataAjuizamento,
                                    FORMATO_DATAJUD_COMPLETO
                            );

                    return data.format(
                            FORMATADOR_DATA
                    );
                }

                if (dataAjuizamento.matches("\\d{8}")) {

                    return java.time.LocalDate
                            .parse(
                                    dataAjuizamento,
                                    FORMATO_DATAJUD_DATA
                            )
                            .format(
                                    DateTimeFormatter.ofPattern(
                                            "dd/MM/yyyy"
                                    )
                            );
                }

            } catch (DateTimeParseException ignored) {
            }

            return dataAjuizamento;
        }


        public String ultimaAtualizacaoFormatada() {

            if (ultimaAtualizacao == null
                    || ultimaAtualizacao.isBlank()) {

                return "Não informado";
            }

            try {

                OffsetDateTime data =
                        OffsetDateTime.parse(
                                ultimaAtualizacao
                        );

                return data.format(
                        FORMATADOR_DATA
                );

            } catch (DateTimeParseException ignored) {
            }

            return ultimaAtualizacao;
        }
    }


    public record LeilaoResumo(

            Long id,

            LocalDateTime abertura1Praca,

            LocalDateTime fechamento1Praca,

            BigDecimal lanceInicial1Praca,

            LocalDateTime abertura2Praca,

            LocalDateTime fechamento2Praca,

            BigDecimal lanceInicial2Praca,

            Integer percentualDescontoFonte,

            BigDecimal lanceMinimo,

            BigDecimal incremento,

            BigDecimal comissaoPercentual,

            StatusLeilao statusLeilao,

            ResultadoLeilao resultadoLeilao

    ) {

        public String abertura1PracaFormatada() {
            return formatarData(abertura1Praca);
        }

        public String fechamento1PracaFormatada() {
            return formatarData(fechamento1Praca);
        }

        public String abertura2PracaFormatada() {
            return formatarData(abertura2Praca);
        }

        public String fechamento2PracaFormatada() {
            return formatarData(fechamento2Praca);
        }

        public String statusLeilaoFormatado() {

            if (statusLeilao == null) {
                return "Não informado";
            }

            return switch (statusLeilao) {
                case AGENDADO -> "Agendado";
                case EM_ANDAMENTO -> "Em andamento";
                case ENCERRADO -> "Encerrado";
                case SUSPENSO -> "Suspenso";
                case CANCELADO -> "Cancelado";
                case DESCONHECIDO -> "Desconhecido";
            };
        }

        public String resultadoLeilaoFormatado() {

            if (resultadoLeilao == null) {
                return "Não informado";
            }

            return switch (resultadoLeilao) {
                case COM_LANCES -> "Com lances";
                case SEM_LANCES -> "Sem lances";
                case ARREMATADO -> "Arrematado";
                case DESERTO -> "Deserto";
                case DESCONHECIDO -> "Desconhecido";
            };
        }
    }


    public record AcompanhamentoResumo(

            Long id,

            StatusPipeline statusPipeline,

            LocalDateTime dataIdentificacao,

            LocalDateTime ultimaVerificacao,

            String observacao

    ) {

        public String statusPipelineFormatado() {

            if (statusPipeline == null) {
                return "Não informado";
            }

            return switch (statusPipeline) {
                case IDENTIFICADO -> "Identificado";
                case MONITORANDO_LEILAO -> "Monitorando leilão";
                case AGUARDANDO_RESULTADO -> "Aguardando resultado";
                case MONITORANDO_PROCESSO -> "Monitorando processo";
                case EM_ANALISE -> "Em análise";
                case OPORTUNIDADE -> "Oportunidade";
                case DESCARTADO -> "Descartado";
                case ENCERRADO -> "Encerrado";
            };
        }

        public String dataIdentificacaoFormatada() {
            return formatarData(dataIdentificacao);
        }

        public String ultimaVerificacaoFormatada() {
            return formatarData(ultimaVerificacao);
        }
    }


    public record FonteResumo(

            Long id,

            FonteTipo tipo,

            String origemNome,

            String urlOrigem,

            LocalDateTime dataCaptura

    ) {

        public String tipoFormatado() {

            if (tipo == null) {
                return "Fonte";
            }

            return switch (tipo) {
                case DATAJUD_CNJ -> "DataJud / CNJ";
                case DJE_TJSP -> "DJE / TJSP";
                case LEILOEIRO_OFICIAL -> "Leiloeiro oficial";
                case CORRETOR_JUDICIAL -> "Corretor judicial";
            };
        }

        public String dataCapturaFormatada() {
            return formatarData(dataCaptura);
        }
    }


    private static String formatarData(
            LocalDateTime data
    ) {

        if (data == null) {
            return "Não informado";
        }

        return data.format(
                FORMATADOR_DATA
        );
    }
}