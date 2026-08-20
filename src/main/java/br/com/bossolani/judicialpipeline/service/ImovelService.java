package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.model.Imovel;
import br.com.bossolani.judicialpipeline.repository.ImovelRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImovelService {

    private final ImovelRepository imovelRepository;

    public ImovelService(ImovelRepository imovelRepository) {
        this.imovelRepository = imovelRepository;
    }

    public Imovel salvar(Imovel imovel) {
        return imovelRepository.save(imovel);
    }

    public List<Imovel> listarTodos() {
        return imovelRepository.findAll();
    }
}