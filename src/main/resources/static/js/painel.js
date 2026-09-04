document.addEventListener("DOMContentLoaded", () => {

    const CHAVE_APRESENTACAO =
        "judicialPipelineApresentacaoV1";

    const apresentacao =
        document.getElementById("apresentacaoInicial");

    const botaoEntrarPainel =
        document.getElementById("botaoEntrarPainel");

    const botaoComoFunciona =
        document.getElementById("botaoComoFunciona");

    const filtroProcesso =
        document.getElementById("filtroProcesso");

    const filtroCidade =
        document.getElementById("filtroCidade");

    const filtroBairro =
        document.getElementById("filtroBairro");

    const filtroStatus =
        document.getElementById("filtroStatus");

    const filtroResultado =
        document.getElementById("filtroResultado");

    const botaoLimpar =
        document.getElementById("botaoLimpar");

    const semResultados =
        document.getElementById("semResultados");

    const resultadoFiltro =
        document.getElementById("resultadoFiltro");

    const cards =
        Array.from(
            document.querySelectorAll(".imovel-item")
        );


    function apresentacaoJaVista() {

        try {
            return localStorage.getItem(CHAVE_APRESENTACAO)
                === "true";

        } catch (erro) {
            console.warn(
                "Não foi possível ler a apresentação inicial.",
                erro
            );
            return false;
        }
    }


    function registrarApresentacaoVista() {

        try {
            localStorage.setItem(
                CHAVE_APRESENTACAO,
                "true"
            );

        } catch (erro) {
            console.warn(
                "Não foi possível salvar a apresentação inicial.",
                erro
            );
        }
    }


    function abrirApresentacao() {

        if (!apresentacao) {
            return;
        }

        apresentacao.hidden = false;
        document.body.classList.add("apresentacao-aberta");

        window.requestAnimationFrame(() => {
            botaoEntrarPainel?.focus();
        });
    }


    function fecharApresentacao() {

        if (!apresentacao) {
            return;
        }

        registrarApresentacaoVista();
        apresentacao.hidden = true;
        document.body.classList.remove("apresentacao-aberta");
        botaoComoFunciona?.focus();
    }


    function normalizarTexto(valor) {

        return (valor || "")
            .toString()
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .toLowerCase()
            .trim();
    }


    function normalizarProcesso(valor) {

        return (valor || "")
            .toString()
            .replace(/\D/g, "");
    }


    function adicionarOpcao(
        select,
        valor
    ) {

        if (!select
            || !valor) {

            return;
        }


        const opcao =
            document.createElement("option");

        opcao.value = valor;
        opcao.textContent = valor;

        select.appendChild(opcao);
    }


    function carregarOpcoes(
        select,
        campo
    ) {

        const valores =
            new Set(
                cards
                    .map(card => card.dataset[campo])
                    .filter(Boolean)
            );


        Array.from(valores)
            .sort((a, b) =>
                a.localeCompare(
                    b,
                    "pt-BR"
                )
            )
            .forEach(valor =>
                adicionarOpcao(
                    select,
                    valor
                )
            );
    }


    function filtrarImoveis() {

        if (!filtroProcesso
            || !filtroCidade
            || !filtroBairro
            || !filtroStatus
            || !filtroResultado) {

            return;
        }


        const processoBuscado =
            normalizarProcesso(
                filtroProcesso.value
            );

        const cidadeBuscada =
            normalizarTexto(
                filtroCidade.value
            );

        const bairroBuscado =
            normalizarTexto(
                filtroBairro.value
            );

        const statusBuscado =
            normalizarTexto(
                filtroStatus.value
            );

        const resultadoBuscado =
            normalizarTexto(
                filtroResultado.value
            );


        let visiveis = 0;


        cards.forEach(card => {

            const mostrar =
                (!processoBuscado
                    || normalizarProcesso(card.dataset.processo)
                        .includes(processoBuscado))
                && (!cidadeBuscada
                    || normalizarTexto(card.dataset.cidade)
                    === cidadeBuscada)
                && (!bairroBuscado
                    || normalizarTexto(card.dataset.bairro)
                    === bairroBuscado)
                && (!statusBuscado
                    || normalizarTexto(card.dataset.status)
                    === statusBuscado)
                && (!resultadoBuscado
                    || normalizarTexto(card.dataset.resultado)
                    === resultadoBuscado);


            card.classList.toggle(
                "d-none",
                !mostrar
            );


            if (mostrar) {
                visiveis++;
            }
        });


        if (resultadoFiltro) {

            resultadoFiltro.textContent =
                cards.length === 0
                    ? ""
                    : `${visiveis} de ${cards.length} imóvel(is) exibido(s)`;
        }


        if (semResultados) {

            semResultados.classList.toggle(
                "d-none",
                visiveis !== 0
                || cards.length === 0
            );
        }
    }


    function limparFiltros() {

        if (!filtroProcesso
            || !filtroCidade
            || !filtroBairro
            || !filtroStatus
            || !filtroResultado) {

            return;
        }


        filtroProcesso.value = "";
        filtroCidade.value = "";
        filtroBairro.value = "";
        filtroStatus.value = "";
        filtroResultado.value = "";

        filtrarImoveis();
        filtroProcesso.focus();
    }


    filtroProcesso?.addEventListener(
        "input",
        filtrarImoveis
    );

    filtroCidade?.addEventListener(
        "change",
        filtrarImoveis
    );

    filtroBairro?.addEventListener(
        "change",
        filtrarImoveis
    );

    filtroStatus?.addEventListener(
        "change",
        filtrarImoveis
    );

    filtroResultado?.addEventListener(
        "change",
        filtrarImoveis
    );

    botaoLimpar?.addEventListener(
        "click",
        limparFiltros
    );

    botaoEntrarPainel?.addEventListener(
        "click",
        fecharApresentacao
    );

    botaoComoFunciona?.addEventListener(
        "click",
        abrirApresentacao
    );

    document.addEventListener(
        "keydown",
        event => {
            if (event.key === "Escape"
                && apresentacao
                && !apresentacao.hidden) {
                fecharApresentacao();
            }
        }
    );


    carregarOpcoes(
        filtroCidade,
        "cidade"
    );

    carregarOpcoes(
        filtroBairro,
        "bairro"
    );

    carregarOpcoes(
        filtroStatus,
        "status"
    );

    carregarOpcoes(
        filtroResultado,
        "resultado"
    );


    filtrarImoveis();

    if (!apresentacaoJaVista()) {
        abrirApresentacao();
    }
});
