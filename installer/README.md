# Instalador personalizado para Windows

O instalador é um bootstrapper pequeno: ele contém o código versionado da
aplicação, configura o ambiente local sem tela de login e baixa Docker,
PostgreSQL e imagens necessárias na primeira execução.

## Gerar

Defina `DATAJUD_API_KEY` apenas no processo local de build e execute:

```powershell
./installer/build-installer.ps1
```

O executável e o SHA-256 são gravados em `dist/` por padrão. A chave entra
somente no executável personalizado e nunca deve ser versionada. O build usa
`git archive HEAD`, portanto somente arquivos já commitados entram no pacote.

Validação estrutural sem instalar:

```powershell
./dist/Judicial-Pipeline-Instalador.exe --validate
if ($LASTEXITCODE -ne 0) { throw "Instalador inválido" }
```

## Comportamento no computador do cliente

- solicita elevação administrativa pelo UAC;
- verifica, ativa, atualiza e, se necessário, repara o WSL automaticamente;
  instalações incompletas usam o MSI oficial da Microsoft, validado por tamanho
  e SHA-256, com progresso, repetição automática, log e limite de tempo; o
  cliente nunca precisa executar `wsl --update`;
- instala Docker Desktop quando necessário;
- cria `C:\ProgramData\JudicialPipeline`;
- gera senhas internas aleatórias e desabilita o login somente no ambiente
  local, que fica limitado a `127.0.0.1`;
- cria um inicializador visual e atalhos na área de trabalho e no menu Iniciar;
- remove atalhos legados duplicados durante a atualização;
- inicia PostgreSQL e aplicação via Docker Compose;
- abre `http://localhost:8080` quando o health check estiver `UP`;
- preserva a senha interna e o volume do PostgreSQL quando o instalador é
  executado novamente para atualizar a aplicação;
- se Docker/WSL exigir reinicialização, preserva a instalação e orienta o
  usuário a reiniciar e clicar no atalho.

O diagnóstico da preparação do Windows fica em
`%LOCALAPPDATA%\JudicialPipeline\preparacao-windows.log`.
Quando a reparação direta é necessária, o log detalhado do Windows Installer
fica em `%LOCALAPPDATA%\JudicialPipeline\reparo-wsl-msi.log`.

O executável não possui assinatura Authenticode comercial. O cliente pode
receber aviso do SmartScreen até que seja adquirido e aplicado um certificado
de assinatura de código.

O ícone do instalador e dos atalhos está em
`installer/assets/judicial-pipeline-icon.ico`; a versão PNG original é mantida
ao lado para futuras derivações de tamanho.
