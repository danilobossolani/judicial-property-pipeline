package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.model.Imovel;
import br.com.bossolani.judicialpipeline.model.Leilao;
import br.com.bossolani.judicialpipeline.repository.ImovelRepository;
import br.com.bossolani.judicialpipeline.repository.LeilaoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeilaoService {

    private final LeilaoRepository leilaoRepository;
    private final ImovelRepository imovelRepository;

    public LeilaoService(
            LeilaoRepository leilaoRepository,
            ImovelRepository imovelRepository
    ) {
        this.leilaoRepository = leilaoRepository;
        this.imovelRepository = imovelRepository;
    }

    public Leilao salvar(Leilao leilao) {

        Long imovelId = leilao.getImovel().getId();

        Imovel imovel = imovelRepository
                .findById(imovelId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Imóvel não encontrado")
                );

        leilao.setImovel(imovel);

        return leilaoRepository.save(leilao);
    }

    public List<Leilao> listarTodos() {
        return leilaoRepository.findAll();
    }
}