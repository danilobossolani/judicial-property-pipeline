package br.com.bossolani.judicialpipeline.controller;

import br.com.bossolani.judicialpipeline.model.StatusPipeline;
import br.com.bossolani.judicialpipeline.service.AcompanhamentoOperacionalService;
import br.com.bossolani.judicialpipeline.service.AtualizacaoImovelService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AcompanhamentoController {

    private final AcompanhamentoOperacionalService acompanhamentoOperacionalService;
    private final AtualizacaoImovelService atualizacaoImovelService;


    public AcompanhamentoController(
            AcompanhamentoOperacionalService acompanhamentoOperacionalService,
            AtualizacaoImovelService atualizacaoImovelService
    ) {

        this.acompanhamentoOperacionalService =
                acompanhamentoOperacionalService;

        this.atualizacaoImovelService =
                atualizacaoImovelService;
    }


    /*
     * =========================================================
     * ATUALIZAÇÃO MANUAL DO PIPELINE
     * =========================================================
     */

    @PostMapping("/imoveis/{id}/acompanhamento")
    public String atualizarAcompanhamento(
            @PathVariable Long id,

            @RequestParam
            StatusPipeline statusPipeline,

            @RequestParam(required = false)
            String observacao,

            RedirectAttributes redirectAttributes
    ) {

        acompanhamentoOperacionalService.atualizar(
                id,
                statusPipeline,
                observacao
        );


        redirectAttributes.addFlashAttribute(
                "mensagemSucesso",
                "Acompanhamento atualizado com sucesso."
        );


        return "redirect:/imoveis/" + id;
    }


    /*
     * =========================================================
     * ATUALIZAÇÃO DOS DADOS EXTERNOS
     * =========================================================
     */

    @PostMapping("/imoveis/{id}/atualizar-dados")
    public String atualizarDados(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {

        try {

            atualizacaoImovelService.atualizarDados(
                    id
            );


            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso",
                    "Dados atualizados com sucesso."
            );

        } catch (Exception e) {

            /*
             * Não mostramos stack trace, endereço interno
             * ou detalhes técnicos para o usuário.
             */
            redirectAttributes.addFlashAttribute(
                    "mensagemErro",
                    "Não foi possível atualizar os dados agora. "
                            + "A fonte externa pode estar indisponível. "
                            + "Tente novamente em alguns instantes."
            );
        }


        return "redirect:/imoveis/" + id;
    }
}