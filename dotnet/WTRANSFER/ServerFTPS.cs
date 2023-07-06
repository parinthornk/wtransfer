using FluentFTP;
using Renci.SshNet;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Security.Authentication;
using System.Text;
using System.Threading.Tasks;

namespace WTRANSFER
{
    public class ServerFTPS : FileServer, IFileServer
    {
        public FtpClient? Client { get; set; } = null;
        public SiteInfo SiteInfo { get; set; }
        public ServerFTPS(SiteInfo siteInfo)
        {
            SiteInfo = siteInfo;
        }

        private object _locker = new object();

        public void Open()
        {
            Exception? exception = null;
            lock (_locker)
            {
                try
                {
                    Client = new FtpClient()
                    {
                        Host = SiteInfo.IP,
                        Port = SiteInfo.Port,
                        Credentials = new NetworkCredential(SiteInfo.Username, SiteInfo.Password),
                    };

                    /*Console.WriteLine("cv-1");

                    Client.Config.EncryptionMode = FtpEncryptionMode.Explicit;

                    Client.Config.SslProtocols = SslProtocols.Tls12 | SslProtocols.Ssl2 | SslProtocols.Ssl3 | SslProtocols.Tls | SslProtocols.Tls11 | SslProtocols.Tls13 | SslProtocols.Default;

                    ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls13 | SecurityProtocolType.Tls12 | SecurityProtocolType.Tls11 | SecurityProtocolType.Tls | SecurityProtocolType.Ssl3;
                    Client.ValidateCertificate += new FtpSslValidation((x, y) => { y.Accept = true; });*/

                    // this works in local
                    {
                        // https://stackoverflow.com/questions/42964418/received-an-unexpected-eof-or-0-bytes-from-the-transport-stream-when-getting-req
                        //ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls13 | SecurityProtocolType.Tls12 | SecurityProtocolType.Tls11 | SecurityProtocolType.Tls | SecurityProtocolType.Ssl3 | SecurityProtocolType.SystemDefault;
                        //ServicePointManager.SecurityProtocol = SecurityProtocolType.SystemDefault;
                        //ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls12 | SecurityProtocolType.Tls11;

                        //Console.WriteLine("cv-2");
                        // https://github.com/robinrodricks/FluentFTP/wiki/FTP-Connection#how-do-i-connect-with-ssltls--how-do-i-use-ftps
                        Client.Config.EncryptionMode = FtpEncryptionMode.Auto;
                        //Console.WriteLine("cv-3");
                        //Client.Config.SslProtocols = SslProtocols.Tls12 | SslProtocols.Ssl2 | SslProtocols.Ssl3 | SslProtocols.Tls | SslProtocols.Tls11 | SslProtocols.Tls13 | SslProtocols.Default;
                        Client.Config.SslProtocols = TestMoveFile.SslProtocols;
                        //Console.WriteLine("cv-4");
                        
                        Client.ValidateCertificate += new FtpSslValidation((x, y) => { y.Accept = true; });
                    }

                    //Console.WriteLine("c++");
                    Client.Connect();
                    //Console.WriteLine("c--");
                }
                catch (Exception ex)
                {
                    exception = ex;
                }
            }
            if (exception != null)
            {
                throw exception;
            }
        }

        public void Close()
        {
            //Console.WriteLine("1");
            lock (_locker)
            {
                //Console.WriteLine("2");
                if (Client != null)
                {
                    try { Client.Disconnect(); } catch { }
                    try { Client.Dispose(); } catch { }
                }
            }
        }

        public List<FileItem> DirectoriesList(string path)
        {
            throw new NotImplementedException();
        }

        public void DirectoryCreate(string path)
        {
            Client.CreateDirectory(path);
        }

        public bool DirectoryExists(string path)
        {
            return Client.DirectoryExists(path);
        }

        public bool FileExists(string path)
        {
            return Client.FileExists(path);
        }

        public List<FileItem> FilesList(string path)
        {
            throw new NotImplementedException();
        }

        public List<FileItem> ItemsList(string path)
        {
            var ret = new List<FileItem>();
            var directoryListing = Client.GetListing(path);
            foreach (var g in directoryListing)
            {
                FileItem i = new FileItem();
                i.Name = g.Name;
                i.Size = g.Size;
                i.Timestamp = g.RawModified;
                i.IsDirectory = g.Type == FtpObjectType.Directory;
                ret.Add(i);
            }
            return ret;
        }

        public void TransferExternal(SiteInfo targetSiteInfo, string filePathSource, string filePathTarget)
        {
            if (targetSiteInfo.Protocol.ToLower() == "sftp")
            {
                using (SftpClient targetSftpClient = new SftpClient(targetSiteInfo.IP, targetSiteInfo.Port, targetSiteInfo.Username, targetSiteInfo.Password))
                {
                    try
                    {
                        targetSftpClient.Connect();
                        using (Stream sourceFileStream = Client.OpenRead(filePathSource))
                        {
                            targetSftpClient.UploadFile(sourceFileStream, filePathTarget);
                        }
                    }
                    catch (Exception ex)
                    {
                        // Handle any exceptions or errors that occur during the FTP/SFTP operation
                        //Console.WriteLine("An error occurred while streaming the file: " + ex.Message);
                    }
                    finally
                    {
                        targetSftpClient.Disconnect();
                    }
                }
            }
            else
            {
                throw new Exception("Currently, the ServerFTPS cannot transfer file to " + targetSiteInfo.Protocol + ". Admin will implement it.");
            }
        }

        public void FileDelete(string path)
        {
            Client.DeleteFile(path);
        }

        public Stream GetStreamSend(string filePath)
        {
            try
            {
                return Client.OpenRead(filePath);
            }
            catch (Exception ex)
            {
                throw new Exception("File transfer aborted. Error acquiring FTPS stream sender from source file \"" + filePath + "\". " + ex.Message);
            }
        }

        public Stream GetStreamReceive(string filePath)
        {
            try
            {
                return Client.OpenWrite(filePath);
            }
            catch (Exception ex)
            {
                throw new Exception("File transfer aborted. Error acquiring FTPS stream receiver at target file \"" + filePath + "\". " + ex.Message);
            }
        }

        public void FileRename(string before, string after)
        {
            Client.Rename(before, after);
        }

        public void TransferInternal(string absPathOld, string absPathNew)
        {
            Client.MoveFile(absPathOld, absPathNew, FtpRemoteExists.Overwrite);
        }
    }
}