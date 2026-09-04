package br.com.bossolani.judicialpipeline.repository;

import br.com.bossolani.judicialpipeline.model.Leilao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeilaoRepository
        extends JpaRepository<Leilao, Long> {

    Optional<Leilao> findTopByImovelIdOrderByIdDesc(
            Long imovelId
    );

    List<Leilao> findByImovelIdOrderByIdDesc(
            Long imovelId
    );
}
