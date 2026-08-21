package br.com.bossolani.judicialpipeline.service;

import br.com.bossolani.judicialpipeline.model.Processo;
import br.com.bossolani.judicialpipeline.repository.ProcessoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProcessoService {

    private final ProcessoRepository processoRepository;

    public ProcessoService(ProcessoRepository processoRepository) {
        this.processoRepository = processoRepository;
    }

    public Processo salvar(Processo processo) {
        return processoRepository.save(processo);
    }

    public List<Processo> listarTodos() {
        return processoRepository.findAll();
    }
}