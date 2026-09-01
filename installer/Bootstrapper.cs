using System;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Reflection;
using System.Security.Principal;
using System.Text;
using System.Windows.Forms;

namespace JudicialPipelineInstaller
{
    internal static class Bootstrapper
    {
        private const string PayloadResource = "JudicialPipeline.Payload.zip";

        [STAThread]
        private static int Main(string[] args)
        {
            try
            {
                if (args.Any(argument => string.Equals(argument, "--validate", StringComparison.OrdinalIgnoreCase)))
                {
                    return ValidatePayload();
                }

                if (!IsAdministrator())
                {
                    return RestartAsAdministrator(args);
                }

                return RunInstaller();
            }
            catch (Exception exception)
            {
                MessageBox.Show(
                    "Não foi possível iniciar a instalação do Judicial Pipeline.\n\n" + exception.Message,
                    "Judicial Pipeline",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Error);
                return 1;
            }
        }

        private static int ValidatePayload()
        {
            string temporaryDirectory = CreateTemporaryDirectory();
            try
            {
                string extractedRoot = ExtractPayload(temporaryDirectory);
                string installerScript = Path.Combine(
                    extractedRoot,
                    "Judicial-Pipeline",
                    "installer",
                    "Instalar-JudicialPipeline.ps1");
                string composeFile = Path.Combine(extractedRoot, "Judicial-Pipeline", "compose.yaml");
                string settingsFile = Path.Combine(
                    extractedRoot,
                    "Judicial-Pipeline",
                    "installer",
                    "installer-settings.env");

                return File.Exists(installerScript) && File.Exists(composeFile) && File.Exists(settingsFile)
                    ? 0
                    : 2;
            }
            finally
            {
                DeleteTemporaryDirectory(temporaryDirectory);
            }
        }

        private static int RunInstaller()
        {
            string temporaryDirectory = CreateTemporaryDirectory();
            try
            {
                string extractedRoot = ExtractPayload(temporaryDirectory);
                string projectRoot = Path.Combine(extractedRoot, "Judicial-Pipeline");
                string installerScript = Path.Combine(
                    projectRoot,
                    "installer",
                    "Instalar-JudicialPipeline.ps1");

                var processStartInfo = new ProcessStartInfo
                {
                    FileName = "powershell.exe",
                    Arguments = "-NoProfile -ExecutionPolicy Bypass -File "
                        + Quote(installerScript)
                        + " -Origem "
                        + Quote(projectRoot),
                    UseShellExecute = false,
                    CreateNoWindow = false,
                    WorkingDirectory = projectRoot
                };

                using (Process process = Process.Start(processStartInfo))
                {
                    if (process == null)
                    {
                        throw new InvalidOperationException("O processo de instalação não pôde ser iniciado.");
                    }

                    process.WaitForExit();
                    return process.ExitCode;
                }
            }
            finally
            {
                DeleteTemporaryDirectory(temporaryDirectory);
            }
        }

        private static string ExtractPayload(string temporaryDirectory)
        {
            string payloadPath = Path.Combine(temporaryDirectory, "payload.zip");
            Assembly assembly = Assembly.GetExecutingAssembly();

            using (Stream resource = assembly.GetManifestResourceStream(PayloadResource))
            {
                if (resource == null)
                {
                    throw new InvalidOperationException("O conteúdo interno do instalador não foi encontrado.");
                }

                using (FileStream output = File.Create(payloadPath))
                {
                    resource.CopyTo(output);
                }
            }

            string extractedRoot = Path.Combine(temporaryDirectory, "conteudo");
            Directory.CreateDirectory(extractedRoot);
            ZipFile.ExtractToDirectory(payloadPath, extractedRoot);
            return extractedRoot;
        }

        private static bool IsAdministrator()
        {
            using (WindowsIdentity identity = WindowsIdentity.GetCurrent())
            {
                var principal = new WindowsPrincipal(identity);
                return principal.IsInRole(WindowsBuiltInRole.Administrator);
            }
        }

        private static int RestartAsAdministrator(string[] args)
        {
            var processStartInfo = new ProcessStartInfo
            {
                FileName = Application.ExecutablePath,
                Arguments = string.Join(" ", args.Select(Quote)),
                Verb = "runas",
                UseShellExecute = true
            };

            try
            {
                Process.Start(processStartInfo);
                return 0;
            }
            catch (System.ComponentModel.Win32Exception)
            {
                MessageBox.Show(
                    "A instalação precisa da autorização de administrador do Windows.",
                    "Judicial Pipeline",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Warning);
                return 3;
            }
        }

        private static string CreateTemporaryDirectory()
        {
            string path = Path.Combine(
                Path.GetTempPath(),
                "JudicialPipelineInstaller-" + Guid.NewGuid().ToString("N"));
            Directory.CreateDirectory(path);
            return path;
        }

        private static void DeleteTemporaryDirectory(string path)
        {
            try
            {
                if (Directory.Exists(path))
                {
                    Directory.Delete(path, true);
                }
            }
            catch
            {
                // O Windows limpará o diretório temporário posteriormente.
            }
        }

        private static string Quote(string value)
        {
            if (string.IsNullOrEmpty(value))
            {
                return "\"\"";
            }

            var result = new StringBuilder("\"");
            int backslashes = 0;

            foreach (char character in value)
            {
                if (character == '\\')
                {
                    backslashes++;
                    continue;
                }

                if (character == '\"')
                {
                    result.Append('\\', backslashes * 2 + 1);
                    result.Append('\"');
                    backslashes = 0;
                    continue;
                }

                result.Append('\\', backslashes);
                result.Append(character);
                backslashes = 0;
            }

            result.Append('\\', backslashes * 2);
            result.Append('\"');
            return result.ToString();
        }
    }
}

