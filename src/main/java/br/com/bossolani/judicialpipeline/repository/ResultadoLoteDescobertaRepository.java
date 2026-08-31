package br.com.bossolani.judicialpipeline.repository;

import br.com.bossolani.judicialpipeline.model.ResultadoLoteDescoberta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResultadoLoteDescobertaRepository
        extends JpaRepository<ResultadoLoteDescoberta, Long> {

    @EntityGraph(attributePaths = {
            "imovel",
            "imovel.processo"
    })
    List<ResultadoLoteDescoberta> findByExecucaoIdOrderByIdAsc(
            Long execucaoId
    );
}
