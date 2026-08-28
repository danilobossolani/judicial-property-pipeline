package br.com.bossolani.judicialpipeline.repository;

import br.com.bossolani.judicialpipeline.model.HistoricoAcompanhamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HistoricoAcompanhamentoRepository
        extends JpaRepository<HistoricoAcompanhamento, Long> {

    Optional<HistoricoAcompanhamento>
    findTopByAcompanhamentoIdOrderByDataEventoDesc(
            Long acompanhamentoId
    );
}