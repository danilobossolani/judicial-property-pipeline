(() => {

    const CHAVE_TEMA =
        "pipelineJudicialTema";


    function temaSalvo() {

        const tema =
            localStorage.getItem(
                CHAVE_TEMA
            );

        if (tema === "dark"
            || tema === "light") {

            return tema;
        }

        return null;
    }


    function temaInicial() {

        const salvo =
            temaSalvo();

        if (salvo) {
            return salvo;
        }


        /*
         * Na primeira utilização respeitamos
         * a preferência do próprio Windows.
         */
        const sistemaEscuro =
            window.matchMedia(
                "(prefers-color-scheme: dark)"
            ).matches;


        return sistemaEscuro
            ? "dark"
            : "light";
    }


    function aplicarTema(
        tema
    ) {

        document.documentElement.setAttribute(
            "data-theme",
            tema
        );
    }


    /*
     * Executa imediatamente para reduzir o clarão
     * branco antes de a página terminar de carregar.
     */
    aplicarTema(
        temaInicial()
    );


    document.addEventListener(
        "DOMContentLoaded",
        () => {

            const botao =
                document.getElementById(
                    "botaoTema"
                );


            if (!botao) {
                return;
            }


            function atualizarBotao() {

                const temaAtual =
                    document.documentElement
                        .getAttribute(
                            "data-theme"
                        );


                if (temaAtual === "dark") {

                    botao.innerHTML =
                        "<span>☀</span><span>Modo claro</span>";

                    botao.setAttribute(
                        "aria-label",
                        "Ativar modo claro"
                    );

                } else {

                    botao.innerHTML =
                        "<span>🌙</span><span>Modo escuro</span>";

                    botao.setAttribute(
                        "aria-label",
                        "Ativar modo escuro"
                    );
                }
            }


            botao.addEventListener(
                "click",
                () => {

                    const temaAtual =
                        document.documentElement
                            .getAttribute(
                                "data-theme"
                            );


                    const novoTema =
                        temaAtual === "dark"
                            ? "light"
                            : "dark";


                    aplicarTema(
                        novoTema
                    );


                    localStorage.setItem(
                        CHAVE_TEMA,
                        novoTema
                    );


                    atualizarBotao();
                }
            );


            atualizarBotao();
        }
    );

})();