package br.com.bossolani.judicialpipeline.repository;

import br.com.bossolani.judicialpipeline.model.Processo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessoRepository
        extends JpaRepository<Processo, Long> {

    Optional<Processo> findByNumeroProcesso(
            String numeroProcesso
    );
}