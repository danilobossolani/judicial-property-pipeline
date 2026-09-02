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
[assembly: AssemblyVersion("1.1.1.0")]
[assembly: AssemblyFileVersion("1.1.1.0")]

namespace JudicialPipelineLauncher
{
    internal static class Program
    {
        [STAThread]
        private static int Main()
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            using (var form = new StartupForm())
            {
                Application.Run(form);
                return form.ExitCode;
            }
        }
    }

    internal sealed class StartupForm : Form
    {
        private readonly Label statusLabel;
        private readonly ProgressBar progressBar;

        internal int ExitCode { get; private set; }

        internal StartupForm()
        {
            Text = "Judicial Pipeline";
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
                Text = "Judicial Pipeline"
            };

            statusLabel = new Label
            {
                AutoSize = false,
                Bounds = new Rectangle(30, 68, 400, 28),
                Font = new Font("Segoe UI", 9.5F, FontStyle.Regular),
                ForeColor = Color.FromArgb(196, 219, 242),
                Text = "Preparando o sistema. Aguarde um momento..."
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
            Task<StartupResult> startupTask = Task.Factory.StartNew<StartupResult>(
                new Func<StartupResult>(StartApplication));
            startupTask.ContinueWith(
                new Action<Task<StartupResult>>(HandleResult),
                TaskScheduler.FromCurrentSynchronizationContext());
        }

        private StartupResult StartApplication()
        {
            string installationDirectory = AppDomain.CurrentDomain.BaseDirectory;
            string startScript = Path.Combine(installationDirectory, "INICIAR.bat");
            string logDirectory = Environment.GetEnvironmentVariable(
                "JUDICIAL_PIPELINE_LOG_DIR");
            if (string.IsNullOrWhiteSpace(logDirectory))
            {
                logDirectory = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "JudicialPipeline");
            }
            Directory.CreateDirectory(logDirectory);
            string launcherLog = Path.Combine(logDirectory, "inicializacao.log");

            if (!File.Exists(startScript))
            {
                return new StartupResult(2, "O arquivo de inicialização não foi encontrado.");
            }

            try
            {
                var startInfo = new ProcessStartInfo
                {
                    FileName = "cmd.exe",
                    Arguments = "/d /c \"\"" + startScript + "\" --automatico\"",
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
                        return new StartupResult(3, "O inicializador do Windows não respondeu.");
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
                    return new StartupResult(process.ExitCode, standardError);
                }
            }
            catch (Exception exception)
            {
                return new StartupResult(4, exception.Message);
            }
        }

        private void HandleResult(Task<StartupResult> task)
        {
            progressBar.Style = ProgressBarStyle.Blocks;

            if (task.IsFaulted)
            {
                ExitCode = 5;
                ShowFailure("O sistema encontrou uma falha inesperada.");
                return;
            }

            StartupResult result = task.Result;
            if (result.ExitCode == 0)
            {
                ExitCode = 0;
                Close();
                return;
            }

            ExitCode = result.ExitCode;
            ShowFailure(result.Details);
        }

        private void ShowFailure(string details)
        {
            statusLabel.Text = "Não foi possível abrir o sistema.";
            string message =
                "Não foi possível iniciar o Judicial Pipeline.\n\n" +
                "Reinicie o computador e clique novamente no atalho. " +
                "Se continuar, envie inicializacao.log ao responsável técnico.";

            if (!string.IsNullOrWhiteSpace(details))
            {
                message += "\n\nDetalhe: " + details.Trim();
            }

            MessageBox.Show(
                this,
                message,
                "Judicial Pipeline",
                MessageBoxButtons.OK,
                MessageBoxIcon.Warning);
            Close();
        }

        private sealed class StartupResult
        {
            internal StartupResult(int exitCode, string details)
            {
                ExitCode = exitCode;
                Details = details;
            }

            internal int ExitCode { get; private set; }
            internal string Details { get; private set; }
        }
    }
}
