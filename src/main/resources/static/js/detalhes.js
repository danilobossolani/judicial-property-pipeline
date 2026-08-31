document.addEventListener("DOMContentLoaded", () => {

    const formAtualizarDados =
        document.getElementById("formAtualizarDados");

    if (!formAtualizarDados) {
        return;
    }


    const botaoAtualizarDados =
        document.getElementById("botaoAtualizarDados");

    const textoAtualizacao =
        document.getElementById("textoAtualizacao");

    const spinnerAtualizacao =
        document.getElementById("spinnerAtualizacao");

    const iconeAtualizacao =
        document.getElementById("iconeAtualizacao");


    formAtualizarDados.addEventListener(
        "submit",
        () => {

            /*
             * Evita que o usuário clique várias vezes
             * enquanto Playwright/DataJud estão trabalhando.
             */
            botaoAtualizarDados.disabled = true;


            textoAtualizacao.textContent =
                "Atualizando dados...";


            spinnerAtualizacao.classList.remove(
                "d-none"
            );


            iconeAtualizacao.classList.add(
                "d-none"
            );

        }
    );

});