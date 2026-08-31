package br.com.bossolani.judicialpipeline.repository;

import br.com.bossolani.judicialpipeline.model.Imovel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImovelRepository extends JpaRepository<Imovel, Long> {

    Optional<Imovel> findFirstByProcessoIdOrderByIdAsc(
            Long processoId
    );
}
