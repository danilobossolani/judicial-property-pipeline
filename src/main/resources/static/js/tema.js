(() => {

    const CHAVE_TEMA =
        "pipelineJudicialTema";

    const TEMA_CLARO =
        "light";

    const TEMA_ESCURO =
        "dark";


    function lerTemaSalvo() {

        try {

            const tema =
                localStorage.getItem(
                    CHAVE_TEMA
                );

            if (tema === TEMA_CLARO
                || tema === TEMA_ESCURO) {

                return tema;
            }

        } catch (erro) {

            console.warn(
                "Não foi possível ler a preferência de tema.",
                erro
            );
        }

        return null;
    }


    function salvarTema(
        tema
    ) {

        try {

            localStorage.setItem(
                CHAVE_TEMA,
                tema
            );

        } catch (erro) {

            console.warn(
                "Não foi possível salvar a preferência de tema.",
                erro
            );
        }
    }


    function detectarTemaDoSistema() {

        const prefereEscuro =
            window.matchMedia(
                "(prefers-color-scheme: dark)"
            ).matches;

        return prefereEscuro
            ? TEMA_ESCURO
            : TEMA_CLARO;
    }


    function obterTemaAtual() {

        return document.documentElement
            .getAttribute(
                "data-theme"
            ) || TEMA_CLARO;
    }


    function atualizarBotoes() {

        const temaAtual =
            obterTemaAtual();

        const botoes =
            document.querySelectorAll(
                "#botaoTema, [data-theme-toggle]"
            );


        botoes.forEach(botao => {

            if (temaAtual === TEMA_ESCURO) {

                botao.textContent =
                    "☀ Modo claro";

                botao.setAttribute(
                    "aria-label",
                    "Ativar modo claro"
                );

                botao.setAttribute(
                    "title",
                    "Ativar modo claro"
                );

            } else {

                botao.textContent =
                    "🌙 Modo escuro";

                botao.setAttribute(
                    "aria-label",
                    "Ativar modo escuro"
                );

                botao.setAttribute(
                    "title",
                    "Ativar modo escuro"
                );
            }

        });
    }


    function aplicarTema(
        tema,
        persistir
    ) {

        document.documentElement
            .setAttribute(
                "data-theme",
                tema
            );


        if (persistir) {

            salvarTema(
                tema
            );
        }


        atualizarBotoes();
    }


    function alternarTema() {

        const temaAtual =
            obterTemaAtual();


        const novoTema =
            temaAtual === TEMA_ESCURO
                ? TEMA_CLARO
                : TEMA_ESCURO;


        aplicarTema(
            novoTema,
            true
        );
    }


    /*
     * Tema inicial.
     */
    const temaInicial =
        lerTemaSalvo()
        || detectarTemaDoSistema();


    document.documentElement
        .setAttribute(
            "data-theme",
            temaInicial
        );


    /*
     * Delegação de evento.
     *
     * O clique é capturado pelo documento,
     * então funciona mesmo se o botão for
     * renderizado/recarregado depois.
     */
    document.addEventListener(
        "click",
        event => {

            const botao =
                event.target.closest(
                    "#botaoTema, [data-theme-toggle]"
                );


            if (!botao) {
                return;
            }


            event.preventDefault();

            alternarTema();
        }
    );


    /*
     * Atualiza o texto do botão quando
     * a página terminar de carregar.
     */
    if (document.readyState === "loading") {

        document.addEventListener(
            "DOMContentLoaded",
            atualizarBotoes,
            {
                once: true
            }
        );

    } else {

        atualizarBotoes();
    }

})();