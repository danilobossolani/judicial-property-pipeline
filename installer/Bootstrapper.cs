using System;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Reflection;
using System.Security.Cryptography;
using System.Security.Principal;
using System.Text;
using System.Windows.Forms;

[assembly: AssemblyTitle("Judicial Pipeline - Instalador")]
[assembly: AssemblyDescription("Instalador automático do Judicial Pipeline")]
[assembly: AssemblyCompany("Judicial Pipeline")]
[assembly: AssemblyProduct("Judicial Pipeline")]
[assembly: AssemblyCopyright("Copyright © 2026")]
[assembly: AssemblyVersion("1.4.1.0")]
[assembly: AssemblyFileVersion("1.4.1.0")]

namespace JudicialPipelineInstaller
{
    internal static class Bootstrapper
    {
        private const string PayloadResource = "JudicialPipeline.Payload.zip";
        private const string OfflineWslFileName = "wsl.2.7.12.0.x64.msi";
        private const long OfflineWslFileSize = 258998272L;
        private const string OfflineWslSha256 =
            "A460D4560215F2EFE003C136244B78EA3415D773824D7A688EA9DED36DBE9145";

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
                string launcherFile = Path.Combine(
                    extractedRoot,
                    "Judicial-Pipeline",
                    "Judicial Pipeline.exe");
                string windowsPreparationFile = Path.Combine(
                    extractedRoot,
                    "Judicial-Pipeline",
                    "Preparar-Windows.ps1");
                string settingsFile = Path.Combine(
                    extractedRoot,
                    "Judicial-Pipeline",
                    "installer",
                    "installer-settings.env");
                string offlineWslPackage = Path.Combine(
                    extractedRoot,
                    "Judicial-Pipeline",
                    "installer",
                    "runtime",
                    OfflineWslFileName);

                return File.Exists(installerScript)
                    && File.Exists(composeFile)
                    && File.Exists(launcherFile)
                    && File.Exists(windowsPreparationFile)
                    && File.Exists(settingsFile)
                    && IsValidOfflineWslPackage(offlineWslPackage)
                    ? 0
                    : 2;
            }
            finally
            {
                DeleteTemporaryDirectory(temporaryDirectory);
            }
        }

        private static bool IsValidOfflineWslPackage(string path)
        {
            var file = new FileInfo(path);
            if (!file.Exists || file.Length != OfflineWslFileSize)
            {
                return false;
            }

            using (SHA256 sha256 = SHA256.Create())
            using (FileStream stream = File.OpenRead(path))
            {
                string actualHash = BitConverter
                    .ToString(sha256.ComputeHash(stream))
                    .Replace("-", string.Empty);
                return string.Equals(
                    actualHash,
                    OfflineWslSha256,
                    StringComparison.OrdinalIgnoreCase);
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
