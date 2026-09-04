package br.com.bossolani.judicialpipeline.controller;

import br.com.bossolani.judicialpipeline.dto.PipelineImovelDTO;
import br.com.bossolani.judicialpipeline.model.ResultadoLeilao;
import br.com.bossolani.judicialpipeline.model.StatusPipeline;
import br.com.bossolani.judicialpipeline.service.PainelService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class PainelController {

    private final PainelService painelService;

    public PainelController(
            PainelService painelService
    ) {

        this.painelService = painelService;
    }

    @GetMapping("/")
    public String painel(
            Model model
    ) {

        return preencherPainel(
                model,
                painelService.listarImoveis(),
                false
        );
    }

    @GetMapping("/inativos")
    public String inativos(
            Model model
    ) {

        return preencherPainel(
                model,
                painelService.listarImoveisInativos(),
                true
        );
    }

    private String preencherPainel(
            Model model,
            List<PipelineImovelDTO> imoveis,
            boolean modoInativos
    ) {

        long totalImoveis = imoveis.size();

        long totalMonitorando = modoInativos
                ? 0
                : imoveis.stream()
                .filter(imovel ->
                        imovel.statusPipeline()
                                != StatusPipeline.DESCARTADO
                )
                .filter(imovel ->
                        imovel.statusPipeline()
                                != StatusPipeline.ENCERRADO
                )
                .count();

        long totalSemLances = imoveis.stream()
                .filter(imovel ->
                        imovel.resultadoLeilao()
                                == ResultadoLeilao.SEM_LANCES
                )
                .count();

        long totalOportunidades = imoveis.stream()
                .filter(imovel ->
                        imovel.statusPipeline()
                                == StatusPipeline.OPORTUNIDADE
                )
                .count();

        model.addAttribute("imoveis", imoveis);
        model.addAttribute("totalImoveis", totalImoveis);
        model.addAttribute("totalMonitorando", totalMonitorando);
        model.addAttribute("totalSemLances", totalSemLances);
        model.addAttribute("totalOportunidades", totalOportunidades);
        model.addAttribute("modoInativos", modoInativos);
        model.addAttribute(
                "rotuloTotal",
                modoInativos
                        ? "Imóveis inativos"
                        : "Imóveis ativos"
        );
        model.addAttribute(
                "tituloLista",
                modoInativos
                        ? "Imóveis inativos"
                        : "Imóveis monitorados"
        );
        model.addAttribute(
                "subtituloLista",
                modoInativos
                        ? "Leilões encerrados ou retirados do acompanhamento ativo, com histórico preservado."
                        : "Leads ativos encontrados e acompanhados pelo sistema."
        );
        model.addAttribute(
                "mensagemVazia",
                modoInativos
                        ? "Nenhum imóvel inativo."
                        : "Nenhum imóvel ativo cadastrado."
        );

        return "painel";
    }
}
