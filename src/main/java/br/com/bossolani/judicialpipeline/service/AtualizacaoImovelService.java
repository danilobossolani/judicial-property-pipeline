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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Service
public class AtualizacaoImovelService {

    private static final String DOMINIO_SUBLIME =
            "sublimeleiloes.com.br";

    private final LeilaoRepository leilaoRepository;
    private final FonteRepository fonteRepository;
    private final PersistenciaLeilaoService persistenciaLeilaoService;


    public AtualizacaoImovelService(
            LeilaoRepository leilaoRepository,
            FonteRepository fonteRepository,
            PersistenciaLeilaoService persistenciaLeilaoService
    ) {

        this.leilaoRepository =
                leilaoRepository;

        this.fonteRepository =
                fonteRepository;

        this.persistenciaLeilaoService =
                persistenciaLeilaoService;
    }


    /*
     * Não mantemos esta transação aberta durante toda
     * a chamada externa.
     *
     * O PersistenciaLeilaoService já possui a própria
     * transação para salvar os dados coletados.
     */
    public void atualizarDados(
            Long imovelId
    ) throws Exception {

        Leilao leilao =
                buscarLeilao(
                        imovelId
                );


        Fonte fonte =
                buscarFonteDeColeta(
                        leilao.getId()
                );


        validarUrlDaFonte(
                fonte.getUrlOrigem()
        );


        persistenciaLeilaoService.coletarESalvar(
                fonte.getUrlOrigem()
        );
    }


    @Transactional(readOnly = true)
    protected Leilao buscarLeilao(
            Long imovelId
    ) {

        return leilaoRepository
                .findTopByImovelIdOrderByIdDesc(
                        imovelId
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Nenhum leilão encontrado para este imóvel"
                        )
                );
    }


    @Transactional(readOnly = true)
    protected Fonte buscarFonteDeColeta(
            Long leilaoId
    ) {

        List<Fonte> fontes =
                fonteRepository
                        .findByLeilaoIdOrderByDataCapturaDesc(
                                leilaoId
                        );


        return fontes
                .stream()
                .filter(fonte ->
                        fonte.getTipo()
                                == FonteTipo.LEILOEIRO_OFICIAL
                )
                .filter(fonte ->
                        fonte.getUrlOrigem() != null
                                && !fonte.getUrlOrigem().isBlank()
                )
                .findFirst()
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Nenhuma fonte de leiloeiro encontrada para este imóvel"
                        )
                );
    }


    private void validarUrlDaFonte(
            String url
    ) {

        if (url == null
                || url.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A fonte não possui uma URL válida"
            );
        }


        try {

            URI uri =
                    new URI(
                            url
                    );


            String esquema =
                    uri.getScheme();


            String host =
                    uri.getHost();


            if (!"https".equalsIgnoreCase(
                    esquema
            )) {

                throw fonteNaoPermitida();
            }


            if (host == null) {

                throw fonteNaoPermitida();
            }


            String hostNormalizado =
                    host.toLowerCase();


            boolean dominioPermitido =
                    hostNormalizado.equals(
                            DOMINIO_SUBLIME
                    )
                            || hostNormalizado.endsWith(
                            "." + DOMINIO_SUBLIME
                    );


            if (!dominioPermitido) {

                throw fonteNaoPermitida();
            }

        } catch (URISyntaxException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A URL cadastrada para a fonte é inválida"
            );
        }
    }


    private ResponseStatusException fonteNaoPermitida() {

        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Fonte não permitida para atualização automática"
        );
    }
}