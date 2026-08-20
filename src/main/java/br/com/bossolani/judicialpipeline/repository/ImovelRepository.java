package br.com.bossolani.judicialpipeline.repository;

import br.com.bossolani.judicialpipeline.model.Imovel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImovelRepository extends JpaRepository<Imovel, Long> {
}