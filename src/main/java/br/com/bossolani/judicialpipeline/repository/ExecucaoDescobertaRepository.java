package br.com.bossolani.judicialpipeline.repository;

import br.com.bossolani.judicialpipeline.model.ExecucaoDescoberta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExecucaoDescobertaRepository
        extends JpaRepository<ExecucaoDescoberta, Long> {

    List<ExecucaoDescoberta> findTop20ByOrderByInicioDesc();
}
