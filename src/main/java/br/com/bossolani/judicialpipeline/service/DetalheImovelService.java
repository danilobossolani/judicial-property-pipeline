package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.dto.DetalheImovelDTO;
import br.com.bossolani.judicialpipeline.dto.HistoricoItemDTO;
import br.com.bossolani.judicialpipeline.model.Acompanhamento;
import br.com.bossolani.judicialpipeline.model.Fonte;
import br.com.bossolani.judicialpipeline.model.HistoricoAcompanhamento;
import br.com.bossolani.judicialpipeline.model.Imovel;
import br.com.bossolani.judicialpipeline.model.Leilao;
import br.com.bossolani.judicialpipeline.model.Processo;
import br.com.bossolani.judicialpipeline.repository.AcompanhamentoRepository;
import br.com.bossolani.judicialpipeline.repository.FonteRepository;
import br.com.bossolani.judicialpipeline.repository.HistoricoAcompanhamentoRepository;
import br.com.bossolani.judicialpipeline.repository.ImovelRepository;
import br.com.bossolani.judicialpipeline.repository.LeilaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DetalheImovelService {

    private final ImovelRepository imovelRepository;

    private final LeilaoRepository leilaoRepository;

    private final AcompanhamentoRepository acompanhamentoRepository;

    private final HistoricoAcompanhamentoRepository historicoRepository;

    private final FonteRepository fonteRepository;


    public DetalheImovelService(
            ImovelRepository imovelRepository,
            LeilaoRepository leilaoRepository,
            AcompanhamentoRepository acompanhamentoRepository,
            HistoricoAcompanhamentoRepository historicoRepository,
            FonteRepository fonteRepository
    ) {

        this.imovelRepository = imovelRepository;
        this.leilaoRepository = leilaoRepository;
        this.acompanhamentoRepository = acompanhamentoRepository;
        this.historicoRepository = historicoRepository;
        this.fonteRepository = fonteRepository;
    }


    @Transactional(readOnly = true)
    public DetalheImovelDTO buscarPorId(
            Long imovelId
    ) {

        Imovel imovel =
                imovelRepository
                        .findById(imovelId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Imóvel não encontrado"
                                )
                        );


        Processo processo =
                imovel.getProcesso();


        Optional<Leilao> leilaoOptional =
                leilaoRepository
                        .findTopByImovelIdOrderByIdDesc(
                                imovelId
                        );


        Optional<Acompanhamento> acompanhamentoOptional =
                acompanhamentoRepository
                        .findByImovelId(
                                imovelId
                        );


        List<HistoricoItemDTO> historico =
                carregarHistorico(
                        acompanhamentoOptional
                );


        List<DetalheImovelDTO.FonteResumo> fontes =
                carregarFontes(
                        leilaoOptional
                );


        DetalheImovelDTO.ImovelResumo imovelResumo =
                new DetalheImovelDTO.ImovelResumo(

                        imovel.getId(),

                        imovel.getTipo(),

                        imovel.getEndereco(),

                        imovel.getNumero(),

                        imovel.getComplemento(),

                        imovel.getBairro(),

                        imovel.getCidade(),

                        imovel.getCep(),

                        imovel.getValorAvaliacao()
                );


        DetalheImovelDTO.ProcessoResumo processoResumo =
                new DetalheImovelDTO.ProcessoResumo(

                        processo != null
                                ? processo.getNumeroProcesso()
                                : null,

                        processo != null
                                ? processo.getComarca()
                                : null,

                        processo != null
                                ? processo.getVara()
                                : null,

                        processo != null
                                ? processo.getTribunal()
                                : null,

                        processo != null
                                ? processo.getGrau()
                                : null,

                        processo != null
                                ? processo.getOrgaoJulgador()
                                : null,

                        processo != null
                                ? processo.getClasse()
                                : null,

                        processo != null
                                ? processo.getSistema()
                                : null,

                        processo != null
                                ? processo.getFormato()
                                : null,

                        processo != null
                                ? processo.getDataAjuizamento()
                                : null,

                        processo != null
                                ? processo.getUltimaAtualizacao()
                                : null
                );


        DetalheImovelDTO.LeilaoResumo leilaoResumo =
                leilaoOptional
                        .map(this::mapearLeilao)
                        .orElse(null);


        DetalheImovelDTO.AcompanhamentoResumo acompanhamentoResumo =
                acompanhamentoOptional
                        .map(this::mapearAcompanhamento)
                        .orElse(null);


        return new DetalheImovelDTO(

                imovelResumo,

                processoResumo,

                leilaoResumo,

                acompanhamentoResumo,

                historico,

                fontes
        );
    }


    private DetalheImovelDTO.LeilaoResumo mapearLeilao(
            Leilao leilao
    ) {

        return new DetalheImovelDTO.LeilaoResumo(

                leilao.getId(),

                leilao.getAbertura1Praca(),

                leilao.getFechamento1Praca(),

                leilao.getLanceInicial1Praca(),

                leilao.getAbertura2Praca(),

                leilao.getFechamento2Praca(),

                leilao.getLanceInicial2Praca(),

                leilao.getPercentualDescontoFonte(),

                leilao.getLanceMinimo(),

                leilao.getIncremento(),

                leilao.getComissaoPercentual(),

                leilao.getStatusLeilao(),

                leilao.getResultadoLeilao()
        );
    }


    private DetalheImovelDTO.AcompanhamentoResumo mapearAcompanhamento(
            Acompanhamento acompanhamento
    ) {

        return new DetalheImovelDTO.AcompanhamentoResumo(

                acompanhamento.getId(),

                acompanhamento.getStatusPipeline(),

                acompanhamento.getDataIdentificacao(),

                acompanhamento.getUltimaVerificacao(),

                acompanhamento.getObservacao()
        );
    }


    private List<HistoricoItemDTO> carregarHistorico(
            Optional<Acompanhamento> acompanhamentoOptional
    ) {

        if (acompanhamentoOptional.isEmpty()) {
            return List.of();
        }


        Long acompanhamentoId =
                acompanhamentoOptional
                        .get()
                        .getId();


        List<HistoricoAcompanhamento> registros =
                historicoRepository
                        .findByAcompanhamentoIdOrderByDataEventoDesc(
                                acompanhamentoId
                        );


        return registros
                .stream()
                .map(registro ->
                        new HistoricoItemDTO(

                                registro.getDataEvento(),

                                registro.getStatusPipeline(),

                                registro.getStatusLeilao(),

                                registro.getResultadoLeilao(),

                                registro.getOrigem(),

                                registro.getDescricao()
                        )
                )
                .toList();
    }


    private List<DetalheImovelDTO.FonteResumo> carregarFontes(
            Optional<Leilao> leilaoOptional
    ) {

        if (leilaoOptional.isEmpty()) {
            return List.of();
        }


        Long leilaoId =
                leilaoOptional
                        .get()
                        .getId();


        List<Fonte> fontes =
                fonteRepository
                        .findByLeilaoIdOrderByDataCapturaDesc(
                                leilaoId
                        );


        List<DetalheImovelDTO.FonteResumo> resultado =
                new ArrayList<>();


        for (Fonte fonte : fontes) {

            resultado.add(
                    new DetalheImovelDTO.FonteResumo(

                            fonte.getId(),

                            fonte.getTipo(),

                            fonte.getOrigemNome(),

                            fonte.getUrlOrigem(),

                            fonte.getDataCaptura()
                    )
            );
        }


        return resultado;
    }
}