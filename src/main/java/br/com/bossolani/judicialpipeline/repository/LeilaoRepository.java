package br.com.bossolani.judicialpipeline.repository;

import br.com.bossolani.judicialpipeline.model.Leilao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeilaoRepository extends JpaRepository<Leilao, Long> {
}