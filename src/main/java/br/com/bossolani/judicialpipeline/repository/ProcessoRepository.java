package br.com.bossolani.judicialpipeline.repository;

import br.com.bossolani.judicialpipeline.model.Processo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessoRepository extends JpaRepository<Processo, Long> {
}