package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.model.Imovel;
import br.com.bossolani.judicialpipeline.model.Processo;
import br.com.bossolani.judicialpipeline.repository.ImovelRepository;
import br.com.bossolani.judicialpipeline.repository.ProcessoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImovelService {

    private final ImovelRepository imovelRepository;
    private final ProcessoRepository processoRepository;

    public ImovelService(
            ImovelRepository imovelRepository,
            ProcessoRepository processoRepository
    ) {
        this.imovelRepository = imovelRepository;
        this.processoRepository = processoRepository;
    }

    public Imovel salvar(Imovel imovel) {

        Long processoId = imovel.getProcesso().getId();

        Processo processo = processoRepository
                .findById(processoId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Processo não encontrado")
                );

        imovel.setProcesso(processo);

        return imovelRepository.save(imovel);
    }

    public List<Imovel> listarTodos() {
        return imovelRepository.findAll();
    }
}