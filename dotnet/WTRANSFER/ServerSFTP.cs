using Renci.SshNet.Sftp;
using Renci.SshNet;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using static System.Net.WebRequestMethods;
using FluentFTP;
using System.Diagnostics.Metrics;

namespace WTRANSFER
{
    public class ServerSFTP : FileServer, IFileServer
    {
        public SiteInfo SiteInfo { get; set; }
        public ServerSFTP(SiteInfo siteInfo)
        {
            SiteInfo = siteInfo;
        }

        public SftpClient Client { get; set; } = null;

        private object _locker = new object();

        public void Open()
        {
            Exception? e = null;
            lock (_locker)
            {
                try
                {
                    Client = new SftpClient(SiteInfo.IP, SiteInfo.Port, SiteInfo.Username, SiteInfo.Password);
                    Client.Connect();
                }
                catch (Exception ex)
                {
                    e = ex;
                }
            }
            if (e != null)
            {
                throw e;
            }
        }

        public void Close()
        {
            lock (_locker)
            {
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
            //Client.CreateDirectory(path);

            CreateRemoteDirectory(Client, path);
        }

        private static void CreateRemoteDirectory(SftpClient sftpClient, string directoryPath)
        {
            string[] directories = directoryPath.Split(new[] { '/' }, StringSplitOptions.RemoveEmptyEntries);
            string currentPath = "/";

            foreach (string directory in directories)
            {
                currentPath += directory + "/";
                if (!sftpClient.Exists(currentPath))
                {
                    sftpClient.CreateDirectory(currentPath);
                }
            }
        }

        public bool DirectoryExists(string path)
        {
            return Client.Exists(path);
        }

        public bool FileExists(string path)
        {
            return Client.Exists(path);
        }

        public List<FileItem> FilesList(string path)
        {
            throw new NotImplementedException();
        }

        public List<FileItem> ItemsList(string path)
        {
            var list = Client.ListDirectory(path);
            var ret = new List<FileItem>();

            foreach (var v in list)
            {
                var item = new FileItem();
                item.Name = v.Name;
                item.Size = v.Length;
                item.IsDirectory = v.IsDirectory;
                item.Timestamp = v.LastWriteTime;
                ret.Add(item);
            }

            return ret;
        }

        public void TransferExternal(SiteInfo targetSiteInfo, string filePathSource, string filePathTarget)
        {
            Exception? e = null;
            SftpFileStream? sourceStream = null;
            ServerFTP? fTP = null;
            ServerFTPS? fTPS = null;
            Stream? destinationStream = null;

            lock (_locker)
            {
                try
                {
                    sourceStream = Client.OpenRead(filePathSource);
                    var target = Create(targetSiteInfo);

                    if (target is ServerFTP)
                    {
                        fTP = (ServerFTP)target;
                        fTP.Open();
                        destinationStream = fTP.Client.OpenWrite(filePathTarget);
                        byte[] buffer = new byte[8192]; // 8KB buffer size
                        int bytesRead;
                        while ((bytesRead = sourceStream.Read(buffer, 0, buffer.Length)) > 0)
                        {
                            destinationStream.Write(buffer, 0, bytesRead);
                        }
                    }
                    else if (target is ServerFTPS)
                    {
                        fTPS = (ServerFTPS)target;

                        
                        fTPS.Open();
                        
                        destinationStream = fTPS.Client.OpenWrite(filePathTarget);
                        byte[] buffer = new byte[8192]; // 8KB buffer size
                        int bytesRead;
                        while ((bytesRead = sourceStream.Read(buffer, 0, buffer.Length)) > 0)
                        {
                            destinationStream.Write(buffer, 0, bytesRead);
                        }
                    }
                    else
                    {
                        throw new Exception("Error Transfer to target, unsupport target protocol.");
                    }
                }
                catch (Exception ex)
                {
                    e = ex;
                }

                if (destinationStream != null) { try { destinationStream.Close(); } catch { } }
                if (fTP != null) { try { fTP.Close(); } catch { } }
                if (fTPS != null) { try { fTPS.Close(); } catch { } }
                if (sourceStream != null) { try { sourceStream.Close(); } catch { } }
            }

            if (e != null)
            {
                throw e;
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
                throw new Exception("File transfer aborted. Error acquiring SFTP stream sender from source file \"" + filePath + "\". " + ex.Message);
            }
        }

        public Stream GetStreamReceive(string filePath)
        {
            try
            {
                return Client.Create(filePath);
            }
            catch (Exception ex)
            {
                throw new Exception("File transfer aborted. Error acquiring SFTP stream receiver at target file \"" + filePath + "\". " + ex.Message);
            }
        }

        public void FileRename(string before, string after)
        {
            Client.RenameFile(before, after);
        }

        public void TransferInternal(string absPathOld, string absPathNew)
        {
            Client.RenameFile(absPathOld, absPathNew);
        }

        public Dictionary<string, bool> ListItemsNameRecursively(string folder)
        {
            throw new NotImplementedException();
        }

        public void FolderDelete(string folder)
        {
            DeleteDirectory(Client, folder);
        }

        public void DeleteDirectory(SftpClient sftpClient, string directoryPath)
        {
            // Check if the target directory exists
            if (!sftpClient.Exists(directoryPath))
            {
                throw new DirectoryNotFoundException($"Directory does not exist: {directoryPath}");
            }

            // Get the list of files and directories within the current directory
            var directoryContents = sftpClient.ListDirectory(directoryPath);

            // Iterate over each item in the directory
            foreach (var item in directoryContents)
            {
                // Ignore "." and ".." directories
                if (item.Name.Equals(".") || item.Name.Equals(".."))
                {
                    continue;
                }

                // Build the full path of the item
                string itemPath = $"{directoryPath}/{item.Name}";

                // Delete the item if it's a file
                if (item.IsRegularFile)
                {
                    sftpClient.DeleteFile(itemPath);
                    Console.WriteLine($"Deleted file: {itemPath}");
                }
                // Recursively delete the item if it's a directory
                else if (item.IsDirectory)
                {
                    DeleteDirectory(sftpClient, itemPath);
                }
            }

            // Delete the current directory once all its contents have been deleted
            sftpClient.DeleteDirectory(directoryPath);
            Console.WriteLine($"Deleted directory: {directoryPath}");
        }
    }
}