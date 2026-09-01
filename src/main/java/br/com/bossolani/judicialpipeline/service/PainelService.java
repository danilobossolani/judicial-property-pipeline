package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.dto.PipelineImovelDTO;
import br.com.bossolani.judicialpipeline.model.Acompanhamento;
import br.com.bossolani.judicialpipeline.model.Fonte;
import br.com.bossolani.judicialpipeline.model.Imovel;
import br.com.bossolani.judicialpipeline.model.Leilao;
import br.com.bossolani.judicialpipeline.model.Processo;
import br.com.bossolani.judicialpipeline.repository.AcompanhamentoRepository;
import br.com.bossolani.judicialpipeline.repository.FonteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PainelService {

    private final FonteRepository fonteRepository;
    private final AcompanhamentoRepository acompanhamentoRepository;

    public PainelService(
            FonteRepository fonteRepository,
            AcompanhamentoRepository acompanhamentoRepository
    ) {
        this.fonteRepository = fonteRepository;
        this.acompanhamentoRepository = acompanhamentoRepository;
    }

    @Transactional(readOnly = true)
    public List<PipelineImovelDTO> listarImoveis() {

        List<Fonte> fontes =
                new ArrayList<>(
                        fonteRepository.findAll()
                );


        fontes.sort(
                Comparator.comparing(
                        Fonte::getDataCaptura,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                )
        );


        Map<Long, PipelineImovelDTO> imoveisPorId =
                new LinkedHashMap<>();

        for (Fonte fonte : fontes) {

            Leilao leilao = fonte.getLeilao();

            if (leilao == null) {
                continue;
            }

            Imovel imovel = leilao.getImovel();

            if (imovel == null) {
                continue;
            }


            if (processoDeDespejo(
                    imovel.getProcesso()
            )) {
                continue;
            }

            Acompanhamento acompanhamento =
                    acompanhamentoRepository
                            .findByImovelId(imovel.getId())
                            .orElse(null);

            PipelineImovelDTO dto =
                    new PipelineImovelDTO(
                            imovel.getId(),
                            imovel.getTipo(),
                            imovel.getEndereco(),
                            imovel.getNumero(),
                            imovel.getBairro(),
                            imovel.getCidade(),
                            leilao.getValorAvaliacaoFonte() != null
                                    ? leilao.getValorAvaliacaoFonte()
                                    : imovel.getValorAvaliacao(),

                            imovel.getProcesso() != null
                                    ? imovel.getProcesso().getNumeroProcesso()
                                    : null,

                            leilao.getLanceInicial1Praca(),
                            leilao.getLanceInicial2Praca(),
                            leilao.getLanceMinimo(),
                            leilao.getPercentualDescontoFonte(),
                            leilao.getStatusLeilao(),
                            leilao.getResultadoLeilao(),

                            acompanhamento != null
                                    ? acompanhamento.getStatusPipeline()
                                    : null,

                            fonte.getOrigemNome(),
                            fonte.getUrlOrigem()
                    );

            imoveisPorId.putIfAbsent(
                    imovel.getId(),
                    dto
            );
        }


        return new ArrayList<>(
                imoveisPorId.values()
        );
    }

    private boolean processoDeDespejo(
            Processo processo
    ) {

        if (processo == null
                || processo.getClasse() == null) {
            return false;
        }


        String classeNormalizada =
                Normalizer.normalize(
                                processo.getClasse(),
                                Normalizer.Form.NFD
                        )
                        .replaceAll(
                                "\\p{M}",
                                ""
                        )
                        .toLowerCase(
                                Locale.ROOT
                        );


        return classeNormalizada.contains(
                "despejo"
        );
    }
}
