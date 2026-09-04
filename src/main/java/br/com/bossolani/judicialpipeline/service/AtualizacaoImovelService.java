package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.model.Fonte;
import br.com.bossolani.judicialpipeline.model.FonteTipo;
import br.com.bossolani.judicialpipeline.model.Leilao;
import br.com.bossolani.judicialpipeline.repository.FonteRepository;
import br.com.bossolani.judicialpipeline.repository.LeilaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AtualizacaoImovelService {

    private final LeilaoRepository leilaoRepository;
    private final FonteRepository fonteRepository;
    private final PersistenciaLeilaoService persistenciaLeilaoService;
    private final TriagemLoteService triagemLoteService;

    public AtualizacaoImovelService(
            LeilaoRepository leilaoRepository,
            FonteRepository fonteRepository,
            PersistenciaLeilaoService persistenciaLeilaoService,
            TriagemLoteService triagemLoteService
    ) {
        this.leilaoRepository = leilaoRepository;
        this.fonteRepository = fonteRepository;
        this.persistenciaLeilaoService = persistenciaLeilaoService;
        this.triagemLoteService = triagemLoteService;
    }

    /*
     * Não mantemos transação aberta durante a chamada externa.
     * PersistenciaLeilaoService controla a transação da atualização.
     */
    public void atualizarDados(Long imovelId) throws Exception {
        Leilao leilao = buscarLeilao(imovelId);
        Fonte fonte = buscarFonteDeColeta(leilao.getId());
        String urlNormalizada = validarENormalizarUrl(fonte.getUrlOrigem());

        persistenciaLeilaoService.coletarESalvar(urlNormalizada);
    }

    @Transactional(readOnly = true)
    protected Leilao buscarLeilao(Long imovelId) {
        return leilaoRepository
                .findTopByImovelIdOrderByIdDesc(imovelId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Nenhum leilão encontrado para este imóvel"
                ));
    }

    @Transactional(readOnly = true)
    protected Fonte buscarFonteDeColeta(Long leilaoId) {
        List<Fonte> fontes = fonteRepository
                .findByLeilaoIdOrderByDataCapturaDesc(leilaoId);

        return fontes.stream()
                .filter(this::fonteAtualizavel)
                .filter(fonte -> fonte.getUrlOrigem() != null
                        && !fonte.getUrlOrigem().isBlank())
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Nenhuma fonte atualizável encontrada para este imóvel"
                ));
    }

    private boolean fonteAtualizavel(Fonte fonte) {
        return fonte.getTipo() == FonteTipo.LEILOEIRO_OFICIAL
                || fonte.getTipo() == FonteTipo.DJE_TJSP;
    }

    private String validarENormalizarUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A fonte não possui uma URL válida"
            );
        }

        try {
            return triagemLoteService.normalizarUrl(url);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Fonte não permitida para atualização automática",
                    exception
            );
        }
    }
}
