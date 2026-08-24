package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.model.Fonte;
import br.com.bossolani.judicialpipeline.model.Leilao;
import br.com.bossolani.judicialpipeline.repository.FonteRepository;
import br.com.bossolani.judicialpipeline.repository.LeilaoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FonteService {

    private final FonteRepository fonteRepository;
    private final LeilaoRepository leilaoRepository;

    public FonteService(
            FonteRepository fonteRepository,
            LeilaoRepository leilaoRepository
    ) {
        this.fonteRepository = fonteRepository;
        this.leilaoRepository = leilaoRepository;
    }

    public Fonte salvar(Fonte fonte) {

        Long leilaoId = fonte.getLeilao().getId();

        Leilao leilao = leilaoRepository
                .findById(leilaoId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Leilão não encontrado")
                );

        fonte.setLeilao(leilao);

        if (fonte.getDataCaptura() == null) {
            fonte.setDataCaptura(LocalDateTime.now());
        }

        return fonteRepository.save(fonte);
    }

    public List<Fonte> listarTodos() {
        return fonteRepository.findAll();
    }
}