package br.com.bossolani.judicialpipeline.repository;

import br.com.bossolani.judicialpipeline.model.HistoricoAcompanhamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoAcompanhamentoRepository
        extends JpaRepository<HistoricoAcompanhamento, Long> {

    boolean existsByAcompanhamentoId(Long acompanhamentoId);
}