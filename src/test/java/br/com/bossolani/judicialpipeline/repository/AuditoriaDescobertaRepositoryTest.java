package br.com.bossolani.judicialpipeline.repository;

import br.com.bossolani.judicialpipeline.model.DecisaoLoteDescoberta;
import br.com.bossolani.judicialpipeline.model.ExecucaoDescoberta;
import br.com.bossolani.judicialpipeline.model.OrigemExecucaoDescoberta;
import br.com.bossolani.judicialpipeline.model.ResultadoLoteDescoberta;
import br.com.bossolani.judicialpipeline.model.StatusExecucaoDescoberta;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
class AuditoriaDescobertaRepositoryTest {

    @Autowired
    private ExecucaoDescobertaRepository execucaoRepository;

    @Autowired
    private ResultadoLoteDescobertaRepository resultadoRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void devePersistirExecucaoESeuResultadoDeLote() {

        ExecucaoDescoberta execucao =
                new ExecucaoDescoberta();

        execucao.setFonte(
                "Sublime Leilões"
        );

        execucao.setInicio(
                LocalDateTime.of(
                        2026,
                        8,
                        31,
                        9,
                        0
                )
        );

        execucao.setTermino(
                LocalDateTime.of(
                        2026,
                        8,
                        31,
                        9,
                        0,
                        3
                )
        );

        execucao.setDuracaoMs(3000L);
        execucao.setOrigem(OrigemExecucaoDescoberta.MANUAL);
        execucao.setStatus(StatusExecucaoDescoberta.CONCLUIDA_COM_FALHAS);
        execucao.setTotalEncontrado(2);
        execucao.setTotalElegivel(2);
        execucao.setTotalImportado(1);
        execucao.setTotalFalha(1);
        execucao.setErroResumo("Um lote falhou ao carregar.");


        execucao =
                execucaoRepository.saveAndFlush(
                        execucao
                );


        ResultadoLoteDescoberta resultado =
                new ResultadoLoteDescoberta();

        resultado.setExecucao(execucao);
        resultado.setTitulo("Casa em Sorocaba");
        resultado.setCidade("Sorocaba");
        resultado.setUrlOriginal("https://www.sublimeleiloes.com.br/lote/casa-em-sorocaba/2405/?origem=busca");
        resultado.setUrlNormalizada("https://www.sublimeleiloes.com.br/lote/casa-em-sorocaba/2405/");
        resultado.setNumeroProcesso("00467199720118260602");
        resultado.setDecisao(DecisaoLoteDescoberta.IMPORTADO);
        resultado.setMotivo("Lote elegível importado e relacionado ao imóvel.");


        resultadoRepository.saveAndFlush(
                resultado
        );

        entityManager.clear();


        ExecucaoDescoberta persistida =
                execucaoRepository.findById(
                        execucao.getId()
                ).orElseThrow();

        List<ResultadoLoteDescoberta> lotes =
                resultadoRepository.findByExecucaoIdOrderByIdAsc(
                        execucao.getId()
                );


        assertNotNull(
                persistida.getId()
        );

        assertEquals(
                StatusExecucaoDescoberta.CONCLUIDA_COM_FALHAS,
                persistida.getStatus()
        );

        assertEquals(
                1,
                persistida.getTotalImportado()
        );

        assertEquals(
                1,
                persistida.getTotalFalha()
        );

        assertEquals(
                1,
                lotes.size()
        );

        assertEquals(
                DecisaoLoteDescoberta.IMPORTADO,
                lotes.getFirst()
                        .getDecisao()
        );

        assertEquals(
                "https://www.sublimeleiloes.com.br/lote/casa-em-sorocaba/2405/",
                lotes.getFirst()
                        .getUrlNormalizada()
        );
    }
}
