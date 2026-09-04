package br.com.bossolani.judicialpipeline.integration.leiloeiro;

import br.com.bossolani.judicialpipeline.integration.leiloeiro.dto.LoteDescobertoDTO;
import br.com.bossolani.judicialpipeline.model.FonteTipo;

import java.net.URI;
import java.util.List;

public interface LeiloeiroProvider {

    String nome();

    default FonteTipo tipoFonte() {
        return FonteTipo.LEILOEIRO_OFICIAL;
    }

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
