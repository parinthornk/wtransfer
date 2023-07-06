using System.Net;
using System.Text;
using System.Net.Security;
using System.Security.Cryptography.X509Certificates;
using FluentFTP;

internal class ImDone001
{
    public ImDone001()
    {
        ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls;

        string ftpServer = "ftp://hq-h2o-s01.ptt.corp";
        string username = "Dispatcher";
        string password = "cD1sp@tcher";
        string remoteFileName = "/Dev/usr/sap/interface/pttor/ebpp/outbound/file_example_XLSX_2MB-0000.xlsx";

        
        //Thread.Sleep(2000);

        /*string ftpServer = "ftp://10.120.202.72:6000";
        string username = "orkalatstusr";
        string password = "R1k@3t2t!";
        string remoteFileName = "/OR-KALA_Test/file_example_XLSX_2MB-0000.xlsx";*/

        string localFilePath = "file_example_XLSX_2MB-0000.xlsx";

        Console.WriteLine("---> 1");

        // Create FTP request
        FtpWebRequest request = (FtpWebRequest)WebRequest.Create(ftpServer + remoteFileName);
        request.Method = WebRequestMethods.Ftp.UploadFile;
        request.EnableSsl = true;
        //request. = FtpEncryptionMode.Explicit; // Set explicit TLS

        Console.WriteLine("---> 2");
        //Thread.Sleep(2000);

        request.UseBinary = true; // Set binary mode
        request.Credentials = new NetworkCredential(username, password);

        Console.WriteLine("---> 3");
        //Thread.Sleep(2000);

        // Add certificate validation callback
        ServicePointManager.ServerCertificateValidationCallback += ValidateServerCertificate;

        Console.WriteLine("---> 4");
        //Thread.Sleep(2000);

        // Read the file data
        byte[] fileContents;
        using (FileStream sourceStream = File.OpenRead(localFilePath))
        {
            fileContents = new byte[sourceStream.Length];
            sourceStream.Read(fileContents, 0, fileContents.Length);
        }

        //request.Credentials.GetCredential(new Uri(ftpServer), "TLS/SSL");

        Console.WriteLine("---> 5");
        //Thread.Sleep(5000);

        // Set the request content
        request.ContentLength = fileContents.Length;
        using (Stream requestStream = request.GetRequestStream())
        {
            requestStream.Write(fileContents, 0, fileContents.Length);
        }

        Console.WriteLine("---> 6");
        //Thread.Sleep(2000);

        // Send the request and get the response
        using FtpWebResponse response = (FtpWebResponse)request.GetResponse();
        Console.WriteLine($"Upload File Complete, status {response.StatusDescription}");
    }

    private static bool ValidateServerCertificate(object sender, X509Certificate certificate, X509Chain chain, SslPolicyErrors sslPolicyErrors)
    {
        // Accept all certificates
        return true;
    }
}