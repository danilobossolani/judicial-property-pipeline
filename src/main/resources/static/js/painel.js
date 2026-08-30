document.addEventListener("DOMContentLoaded", () => {

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


    function normalizarTexto(valor) {

        if (!valor) {
            return "";
        }

        return valor
            .toString()
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .toLowerCase()
            .trim();
    }


    function normalizarProcesso(valor) {

        if (!valor) {
            return "";
        }

        return valor
            .toString()
            .replace(/\D/g, "");
    }


    function adicionarOpcao(
        select,
        valor
    ) {

        if (!valor) {
            return;
        }

        const option =
            document.createElement("option");

        option.value = valor;
        option.textContent = valor;

        select.appendChild(option);
    }


    function carregarOpcoes() {

        const cidades = new Set();
        const bairros = new Set();
        const statuses = new Set();
        const resultados = new Set();

        cards.forEach(card => {

            if (card.dataset.cidade) {
                cidades.add(card.dataset.cidade);
            }

            if (card.dataset.bairro) {
                bairros.add(card.dataset.bairro);
            }

            if (card.dataset.status) {
                statuses.add(card.dataset.status);
            }

            if (card.dataset.resultado) {
                resultados.add(card.dataset.resultado);
            }

        });


        Array.from(cidades)
            .sort()
            .forEach(valor =>
                adicionarOpcao(
                    filtroCidade,
                    valor
                )
            );


        Array.from(bairros)
            .sort()
            .forEach(valor =>
                adicionarOpcao(
                    filtroBairro,
                    valor
                )
            );


        Array.from(statuses)
            .sort()
            .forEach(valor =>
                adicionarOpcao(
                    filtroStatus,
                    valor
                )
            );


        Array.from(resultados)
            .sort()
            .forEach(valor =>
                adicionarOpcao(
                    filtroResultado,
                    valor
                )
            );
    }


    function filtrarImoveis() {

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

            const processo =
                normalizarProcesso(
                    card.dataset.processo
                );

            const cidade =
                normalizarTexto(
                    card.dataset.cidade
                );

            const bairro =
                normalizarTexto(
                    card.dataset.bairro
                );

            const status =
                normalizarTexto(
                    card.dataset.status
                );

            const resultado =
                normalizarTexto(
                    card.dataset.resultado
                );


            const processoCorresponde =
                processoBuscado === ""
                || processo.includes(
                    processoBuscado
                );

            const cidadeCorresponde =
                cidadeBuscada === ""
                || cidade === cidadeBuscada;

            const bairroCorresponde =
                bairroBuscado === ""
                || bairro === bairroBuscado;

            const statusCorresponde =
                statusBuscado === ""
                || status === statusBuscado;

            const resultadoCorresponde =
                resultadoBuscado === ""
                || resultado === resultadoBuscado;


            const mostrar =
                processoCorresponde
                && cidadeCorresponde
                && bairroCorresponde
                && statusCorresponde
                && resultadoCorresponde;


            card.classList.toggle(
                "d-none",
                !mostrar
            );


            if (mostrar) {
                visiveis++;
            }

        });


        if (cards.length > 0) {

            resultadoFiltro.textContent =
                `${visiveis} de ${cards.length} imóvel(is) exibido(s)`;

        } else {

            resultadoFiltro.textContent = "";

        }


        semResultados.classList.toggle(
            "d-none",
            visiveis !== 0
            || cards.length === 0
        );
    }


    function limparFiltros() {

        filtroProcesso.value = "";
        filtroCidade.value = "";
        filtroBairro.value = "";
        filtroStatus.value = "";
        filtroResultado.value = "";

        filtrarImoveis();
    }


    filtroProcesso.addEventListener(
        "input",
        filtrarImoveis
    );

    filtroCidade.addEventListener(
        "change",
        filtrarImoveis
    );

    filtroBairro.addEventListener(
        "change",
        filtrarImoveis
    );

    filtroStatus.addEventListener(
        "change",
        filtrarImoveis
    );

    filtroResultado.addEventListener(
        "change",
        filtrarImoveis
    );

    botaoLimpar.addEventListener(
        "click",
        limparFiltros
    );


    carregarOpcoes();

    filtrarImoveis();

});