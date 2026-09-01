package br.com.bossolani.judicialpipeline.integration.leiloeiro;

import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteDescobertoDTO;

import java.net.URI;
import java.util.List;

public interface LeiloeiroProvider {

    String nome();

    boolean suporta(
            URI uri
    );

    String normalizarUrl(
            URI uri
    );

    List<LoteDescobertoDTO> descobrirLotes()
            throws Exception;

    ColetaLeiloeiroDTO coletar(
            String url
    ) throws Exception;
}
