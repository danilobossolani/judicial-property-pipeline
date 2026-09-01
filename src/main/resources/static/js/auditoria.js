document.addEventListener("DOMContentLoaded", () => {

    const filtroBusca =
        document.getElementById("filtroBuscaAuditoria");

    const filtroCategoria =
        document.getElementById("filtroCategoriaAuditoria");

    const filtroStatus =
        document.getElementById("filtroStatusAuditoria");

    const filtroPeriodo =
        document.getElementById("filtroPeriodoAuditoria");

    const botaoLimpar =
        document.getElementById("botaoLimparAuditoria");

    const resultado =
        document.getElementById("resultadoAuditoria");

    const semResultados =
        document.getElementById("semResultadosAuditoria");

    const eventos =
        Array.from(
            document.querySelectorAll(".evento-auditoria")
        );

    const filtroBuscaDescoberta =
        document.getElementById("filtroBuscaDescoberta");

    const filtroDecisaoDescoberta =
        document.getElementById("filtroDecisaoDescoberta");

    const filtroFonteDescoberta =
        document.getElementById("filtroFonteDescoberta");

    const botaoLimparDescoberta =
        document.getElementById("botaoLimparDescoberta");

    const resultadoLotesDescoberta =
        document.getElementById("resultadoLotesDescoberta");

    const semResultadosDescoberta =
        document.getElementById("semResultadosDescoberta");

    const lotesDescoberta =
        Array.from(
            document.querySelectorAll(".lote-descoberta")
        );

    const execucoesDescoberta =
        Array.from(
            document.querySelectorAll(".execucao-descoberta")
        );

    const botaoExecutarDescoberta =
        document.getElementById("botaoExecutarDescoberta");

    const spinnerDescoberta =
        document.getElementById("spinnerDescoberta");

    const textoBotaoDescoberta =
        document.getElementById("textoBotaoDescoberta");

    const feedbackDescoberta =
        document.getElementById("feedbackDescoberta");

    const secaoDescoberta =
        document.querySelector(".descoberta-automatica");

    const csrfToken =
        document.querySelector('meta[name="_csrf"]')
            ?.getAttribute("content");

    const csrfHeader =
        document.querySelector('meta[name="_csrf_header"]')
            ?.getAttribute("content");

    function normalizarTexto(valor) {

        return (valor || "")
            .toString()
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .toLowerCase()
            .trim();
    }

    function carregarStatuses() {

        if (!filtroStatus) {
            return;
        }


        const statuses =
            new Set(
                eventos
                    .map(evento => evento.dataset.status)
                    .filter(Boolean)
            );


        Array.from(statuses)
            .sort((a, b) =>
                a.localeCompare(
                    b,
                    "pt-BR"
                )
            )
            .forEach(status => {

                const opcao =
                    document.createElement("option");

                opcao.value = status;
                opcao.textContent = status;

                filtroStatus.appendChild(opcao);
            });
    }

    function dentroDoPeriodo(
        dataIso,
        periodoEmDias
    ) {

        if (!periodoEmDias) {
            return true;
        }


        const dataEvento =
            new Date(dataIso);


        if (Number.isNaN(dataEvento.getTime())) {
            return false;
        }


        const limite =
            new Date();

        limite.setHours(
            limite.getHours()
            - Number(periodoEmDias) * 24
        );


        return dataEvento >= limite;
    }

    function filtrarEventos() {

        if (!filtroBusca
            || !filtroCategoria
            || !filtroStatus
            || !filtroPeriodo) {

            return;
        }


        const busca =
            normalizarTexto(
                filtroBusca.value
            );

        const categoria =
            filtroCategoria.value;

        const status =
            normalizarTexto(
                filtroStatus.value
            );

        const periodo =
            filtroPeriodo.value;


        let visiveis = 0;


        eventos.forEach(evento => {

            const conteudo =
                normalizarTexto(
                    [
                        evento.dataset.processo,
                        evento.dataset.tipoImovel,
                        evento.dataset.localizacao,
                        evento.dataset.descricao,
                        evento.dataset.origem
                    ].join(" ")
                );


            const corresponde =
                (!busca || conteudo.includes(busca))
                && (!categoria
                    || evento.dataset.categoria === categoria)
                && (!status
                    || normalizarTexto(evento.dataset.status) === status)
                && dentroDoPeriodo(
                    evento.dataset.data,
                    periodo
                );


            evento.classList.toggle(
                "d-none",
                !corresponde
            );


            if (corresponde) {
                visiveis++;
            }
        });


        if (resultado) {

            resultado.textContent =
                eventos.length === 0
                    ? ""
                    : `${visiveis} de ${eventos.length} evento(s) exibido(s)`;
        }


        if (semResultados) {

            semResultados.classList.toggle(
                "d-none",
                visiveis !== 0
                || eventos.length === 0
            );
        }
    }

    function limparEventos() {

        if (!filtroBusca
            || !filtroCategoria
            || !filtroStatus
            || !filtroPeriodo) {

            return;
        }


        filtroBusca.value = "";
        filtroCategoria.value = "";
        filtroStatus.value = "";
        filtroPeriodo.value = "";

        filtrarEventos();
        filtroBusca.focus();
    }

    function filtrarLotes() {

        if (!filtroBuscaDescoberta
            || !filtroDecisaoDescoberta
            || !filtroFonteDescoberta) {

            return;
        }


        const busca =
            normalizarTexto(
                filtroBuscaDescoberta.value
            );

        const decisao =
            filtroDecisaoDescoberta.value;

        const fonte =
            normalizarTexto(
                filtroFonteDescoberta.value
            );

        const filtroAtivo =
            Boolean(
                busca
                || decisao
                || fonte
            );


        let visiveis = 0;


        lotesDescoberta.forEach(lote => {

            const corresponde =
                (!busca
                    || normalizarTexto(
                        lote.dataset.conteudo
                    ).includes(busca))
                && (!decisao
                    || lote.dataset.decisao === decisao)
                && (!fonte
                    || normalizarTexto(lote.dataset.fonte) === fonte);


            lote.classList.toggle(
                "d-none",
                !corresponde
            );


            if (corresponde) {
                visiveis++;
            }
        });


        execucoesDescoberta.forEach(execucao => {

            const lotesDaExecucao =
                Array.from(
                    execucao.querySelectorAll(".lote-descoberta")
                );

            const possuiLoteVisivel =
                lotesDaExecucao.some(lote =>
                    !lote.classList.contains("d-none")
                );

            const ocultar =
                filtroAtivo
                && !possuiLoteVisivel;


            execucao.classList.toggle(
                "d-none",
                ocultar
            );


            if (filtroAtivo
                && possuiLoteVisivel) {

                execucao.open = true;
            }
        });


        if (resultadoLotesDescoberta) {

            resultadoLotesDescoberta.textContent =
                `${visiveis} de ${lotesDescoberta.length} lote(s) exibido(s)`;
        }


        if (semResultadosDescoberta) {

            semResultadosDescoberta.classList.toggle(
                "d-none",
                visiveis !== 0
                || lotesDescoberta.length === 0
            );
        }
    }

    function limparLotes() {

        if (!filtroBuscaDescoberta
            || !filtroDecisaoDescoberta
            || !filtroFonteDescoberta) {

            return;
        }


        filtroBuscaDescoberta.value = "";
        filtroDecisaoDescoberta.value = "";
        filtroFonteDescoberta.value = "";

        filtrarLotes();
        filtroBuscaDescoberta.focus();
    }

    function atualizarEstadoExecucao(
        executando,
        mensagem,
        tipo
    ) {

        if (botaoExecutarDescoberta) {
            botaoExecutarDescoberta.disabled = executando;
        }


        spinnerDescoberta?.classList.toggle(
            "d-none",
            !executando
        );


        if (textoBotaoDescoberta) {

            textoBotaoDescoberta.textContent =
                executando
                    ? "Descoberta em execução"
                    : "Executar agora";
        }


        secaoDescoberta?.setAttribute(
            "aria-busy",
            executando.toString()
        );


        if (feedbackDescoberta) {

            feedbackDescoberta.textContent =
                mensagem || "";

            feedbackDescoberta.classList.toggle(
                "feedback-sucesso",
                tipo === "sucesso"
            );

            feedbackDescoberta.classList.toggle(
                "feedback-erro",
                tipo === "erro"
            );
        }
    }

    async function executarDescoberta() {

        atualizarEstadoExecucao(
            true,
            "Consultando as fontes oficiais configuradas. O histórico será atualizado ao concluir.",
            ""
        );


        try {

            const headers = {
                "Accept": "application/json"
            };

            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }

            const resposta =
                await fetch(
                    "/api/descobertas",
                    {
                        method: "POST",
                        headers
                    }
                );

            if (resposta.status === 401) {
                window.location.assign("/login");
                return;
            }


            const corpo =
                await resposta.json()
                    .catch(() => ({}));


            if (!resposta.ok) {

                const erroResposta =
                    new Error(
                    corpo.erro
                    || "Não foi possível iniciar a descoberta automática."
                );

                erroResposta.execucaoEmAndamento =
                    resposta.status === 409;

                throw erroResposta;
            }


            if (corpo.status === "FALHOU") {

                atualizarEstadoExecucao(
                    false,
                    corpo.erroResumo
                    || "A descoberta falhou. Consulte o histórico para ver o erro registrado.",
                    "erro"
                );

            } else {

                atualizarEstadoExecucao(
                    false,
                    `Execução #${corpo.execucaoId} concluída: ${corpo.importados} importado(s), ${corpo.duplicados} já cadastrado(s) e atualizado(s), ${corpo.descartados} descartado(s) e ${corpo.falhas} falha(s).`,
                    "sucesso"
                );
            }


            window.setTimeout(
                () => window.location.reload(),
                900
            );

        } catch (erro) {

            atualizarEstadoExecucao(
                Boolean(
                    erro.execucaoEmAndamento
                ),
                erro.message
                || "Falha de comunicação ao executar a descoberta.",
                "erro"
            );


            if (erro.execucaoEmAndamento) {

                window.setTimeout(
                    () => window.location.reload(),
                    5000
                );
            }
        }
    }

    filtroBusca?.addEventListener(
        "input",
        filtrarEventos
    );

    filtroCategoria?.addEventListener(
        "change",
        filtrarEventos
    );

    filtroStatus?.addEventListener(
        "change",
        filtrarEventos
    );

    filtroPeriodo?.addEventListener(
        "change",
        filtrarEventos
    );

    botaoLimpar?.addEventListener(
        "click",
        limparEventos
    );

    filtroBuscaDescoberta?.addEventListener(
        "input",
        filtrarLotes
    );

    filtroDecisaoDescoberta?.addEventListener(
        "change",
        filtrarLotes
    );

    filtroFonteDescoberta?.addEventListener(
        "change",
        filtrarLotes
    );

    botaoLimparDescoberta?.addEventListener(
        "click",
        limparLotes
    );

    botaoExecutarDescoberta?.addEventListener(
        "click",
        executarDescoberta
    );


    if (botaoExecutarDescoberta?.dataset.emExecucao === "true") {

        atualizarEstadoExecucao(
            true,
            "Uma descoberta já está em execução.",
            ""
        );

        window.setTimeout(
            () => window.location.reload(),
            5000
        );
    }


    carregarStatuses();
    filtrarEventos();
    filtrarLotes();
});
