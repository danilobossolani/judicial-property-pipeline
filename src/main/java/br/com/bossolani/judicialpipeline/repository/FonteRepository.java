package br.com.bossolani.judicialpipeline.repository;

import br.com.bossolani.judicialpipeline.model.Fonte;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FonteRepository extends JpaRepository<Fonte, Long> {
}