using System;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

[assembly: AssemblyTitle("Judicial Pipeline")]
[assembly: AssemblyDescription("Inicializador do Judicial Pipeline")]
[assembly: AssemblyCompany("Judicial Pipeline")]
[assembly: AssemblyProduct("Judicial Pipeline")]
[assembly: AssemblyCopyright("Copyright © 2026")]
[assembly: AssemblyVersion("1.4.1.0")]
[assembly: AssemblyFileVersion("1.4.1.0")]

namespace JudicialPipelineLauncher
{
    internal static class Program
    {
        [STAThread]
        private static int Main(string[] arguments)
        {
            bool shutdownRequested = false;
            foreach (string argument in arguments)
            {
                if (string.Equals(argument, "--stop", StringComparison.OrdinalIgnoreCase))
                {
                    shutdownRequested = true;
                    break;
                }
            }

            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            using (var form = new OperationForm(shutdownRequested))
            {
                Application.Run(form);
                return form.ExitCode;
            }
        }
    }

    internal sealed class OperationForm : Form
    {
        private readonly bool shutdownRequested;
        private readonly Label statusLabel;
        private readonly ProgressBar progressBar;

        internal int ExitCode { get; private set; }

        internal OperationForm(bool shutdownRequested)
        {
            this.shutdownRequested = shutdownRequested;
            Text = shutdownRequested ? "Encerrar Judicial Pipeline" : "Judicial Pipeline";
            ClientSize = new Size(460, 165);
            StartPosition = FormStartPosition.CenterScreen;
            FormBorderStyle = FormBorderStyle.FixedDialog;
            MaximizeBox = false;
            MinimizeBox = true;
            BackColor = Color.FromArgb(9, 20, 40);
            ForeColor = Color.White;
            ExitCode = 1;

            try
            {
                Icon = Icon.ExtractAssociatedIcon(Application.ExecutablePath);
            }
            catch
            {
                // O ícone padrão do Windows será usado se a leitura falhar.
            }

            Label titleLabel = new Label
            {
                AutoSize = false,
                Bounds = new Rectangle(28, 24, 404, 32),
                Font = new Font("Segoe UI", 15F, FontStyle.Bold),
                ForeColor = Color.White,
                Text = shutdownRequested ? "Encerrar Judicial Pipeline" : "Judicial Pipeline"
            };

            statusLabel = new Label
            {
                AutoSize = false,
                Bounds = new Rectangle(30, 68, 400, 28),
                Font = new Font("Segoe UI", 9.5F, FontStyle.Regular),
                ForeColor = Color.FromArgb(196, 219, 242),
                Text = shutdownRequested
                    ? "Encerrando o sistema e liberando a memória..."
                    : "Preparando o sistema. Aguarde um momento..."
            };

            progressBar = new ProgressBar
            {
                Bounds = new Rectangle(30, 108, 400, 18),
                Style = ProgressBarStyle.Marquee,
                MarqueeAnimationSpeed = 28
            };

            Controls.Add(titleLabel);
            Controls.Add(statusLabel);
            Controls.Add(progressBar);
            Shown += OnShown;
        }

        private void OnShown(object sender, EventArgs eventArgs)
        {
            Task<OperationResult> operationTask = Task.Factory.StartNew<OperationResult>(
                new Func<OperationResult>(RunOperation));
            operationTask.ContinueWith(
                new Action<Task<OperationResult>>(HandleResult),
                TaskScheduler.FromCurrentSynchronizationContext());
        }

        private OperationResult RunOperation()
        {
            string installationDirectory = AppDomain.CurrentDomain.BaseDirectory;
            string scriptName = shutdownRequested ? "PARAR.bat" : "INICIAR.bat";
            string scriptPath = Path.Combine(installationDirectory, scriptName);
            string logDirectory = Environment.GetEnvironmentVariable(
                "JUDICIAL_PIPELINE_LOG_DIR");
            if (string.IsNullOrWhiteSpace(logDirectory))
            {
                logDirectory = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "JudicialPipeline");
            }
            Directory.CreateDirectory(logDirectory);
            string launcherLog = Path.Combine(
                logDirectory,
                shutdownRequested ? "encerramento-launcher.log" : "inicializacao.log");

            if (!File.Exists(scriptPath))
            {
                return new OperationResult(2, "O arquivo necessário não foi encontrado.");
            }

            try
            {
                var startInfo = new ProcessStartInfo
                {
                    FileName = "cmd.exe",
                    Arguments = "/d /c \"\"" + scriptPath + "\" --automatico\"",
                    WorkingDirectory = installationDirectory,
                    UseShellExecute = false,
                    CreateNoWindow = true,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true
                };

                using (Process process = Process.Start(startInfo))
                {
                    if (process == null)
                    {
                        return new OperationResult(3, "O Windows não respondeu.");
                    }

                    string standardOutput = process.StandardOutput.ReadToEnd();
                    string standardError = process.StandardError.ReadToEnd();
                    process.WaitForExit();

                    var log = new StringBuilder();
                    log.AppendLine("[" + DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss") + "]");
                    log.AppendLine(standardOutput);
                    if (!string.IsNullOrWhiteSpace(standardError))
                    {
                        log.AppendLine(standardError);
                    }

                    File.AppendAllText(launcherLog, log.ToString(), Encoding.UTF8);
                    string details = string.IsNullOrWhiteSpace(standardError)
                        ? standardOutput
                        : standardError;
                    return new OperationResult(process.ExitCode, details);
                }
            }
            catch (Exception exception)
            {
                return new OperationResult(4, exception.Message);
            }
        }

        private void HandleResult(Task<OperationResult> task)
        {
            progressBar.Style = ProgressBarStyle.Blocks;

            if (task.IsFaulted)
            {
                ExitCode = 5;
                ShowFailure("O sistema encontrou uma falha inesperada.");
                return;
            }

            OperationResult result = task.Result;
            if (result.ExitCode == 0)
            {
                ExitCode = 0;
                if (shutdownRequested &&
                    !string.Equals(
                        Environment.GetEnvironmentVariable("JUDICIAL_PIPELINE_NO_DIALOG"),
                        "1",
                        StringComparison.Ordinal))
                {
                    statusLabel.Text = "Sistema encerrado. A memória foi liberada.";
                    MessageBox.Show(
                        this,
                        "O Judicial Pipeline foi encerrado por completo.\n\n" +
                        "Os imóveis, as anotações e o histórico foram preservados. " +
                        "Para usar novamente, clique no atalho Judicial Pipeline.",
                        "Judicial Pipeline",
                        MessageBoxButtons.OK,
                        MessageBoxIcon.Information);
                }
                Close();
                return;
            }

            ExitCode = result.ExitCode;
            ShowFailure(result.Details);
        }

        private void ShowFailure(string details)
        {
            statusLabel.Text = shutdownRequested
                ? "Não foi possível encerrar o sistema."
                : "Não foi possível abrir o sistema.";

            string message;
            if (shutdownRequested)
            {
                message =
                    "Não foi possível encerrar completamente o Judicial Pipeline.\n\n" +
                    "Reinicie o computador. Se continuar, envie encerramento.log " +
                    "ao responsável técnico.";
            }
            else
            {
                message =
                    "Não foi possível iniciar o Judicial Pipeline.\n\n" +
                    "Reinicie o computador e clique novamente no atalho. " +
                    "Se continuar, envie inicializacao.log ao responsável técnico.";
            }

            if (!string.IsNullOrWhiteSpace(details))
            {
                string compactDetails = details.Trim();
                if (compactDetails.Length > 600)
                {
                    compactDetails = compactDetails.Substring(compactDetails.Length - 600);
                }
                message += "\n\nDetalhe: " + compactDetails;
            }

            MessageBox.Show(
                this,
                message,
                "Judicial Pipeline",
                MessageBoxButtons.OK,
                MessageBoxIcon.Warning);
            Close();
        }

        private sealed class OperationResult
        {
            internal OperationResult(int exitCode, string details)
            {
                ExitCode = exitCode;
                Details = details;
            }

            internal int ExitCode { get; private set; }
            internal string Details { get; private set; }
        }
    }
}
