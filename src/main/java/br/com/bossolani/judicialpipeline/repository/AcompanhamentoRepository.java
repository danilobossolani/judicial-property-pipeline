package br.com.bossolani.judicialpipeline.repository;

import br.com.bossolani.judicialpipeline.model.Acompanhamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcompanhamentoRepository
        extends JpaRepository<Acompanhamento, Long> {

    Optional<Acompanhamento> findByImovelId(Long imovelId);
}