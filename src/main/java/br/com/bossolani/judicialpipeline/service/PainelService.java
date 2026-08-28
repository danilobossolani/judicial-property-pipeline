package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.dto.PipelineImovelDTO;
import br.com.bossolani.judicialpipeline.model.Acompanhamento;
import br.com.bossolani.judicialpipeline.model.Fonte;
import br.com.bossolani.judicialpipeline.model.Imovel;
import br.com.bossolani.judicialpipeline.model.Leilao;
import br.com.bossolani.judicialpipeline.repository.AcompanhamentoRepository;
import br.com.bossolani.judicialpipeline.repository.FonteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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

        List<Fonte> fontes = fonteRepository.findAll();

        List<PipelineImovelDTO> imoveisPainel =
                new ArrayList<>();

        for (Fonte fonte : fontes) {

            Leilao leilao = fonte.getLeilao();

            if (leilao == null) {
                continue;
            }

            Imovel imovel = leilao.getImovel();

            if (imovel == null) {
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
                            imovel.getValorAvaliacao(),

                            imovel.getProcesso() != null
                                    ? imovel.getProcesso().getNumeroProcesso()
                                    : null,

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

            imoveisPainel.add(dto);
        }

        return imoveisPainel;
    }
}