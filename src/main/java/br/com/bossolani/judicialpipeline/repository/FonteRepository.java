package br.com.bossolani.judicialpipeline.repository;

import br.com.bossolani.judicialpipeline.model.Fonte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FonteRepository
        extends JpaRepository<Fonte, Long> {

    Optional<Fonte> findByUrlOrigem(
            String urlOrigem
    );

    List<Fonte> findByLeilaoIdOrderByDataCapturaDesc(
            Long leilaoId
    );
}