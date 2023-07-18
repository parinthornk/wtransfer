using FluentFTP;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Authentication;
using System.Text;
using System.Threading.Tasks;

namespace WTRANSFER
{
    public class ServerFTP : FileServer, IFileServer
    {
        public FtpClient? Client { get; set; } = null;
        public SiteInfo SiteInfo { get; set; }
        public ServerFTP(SiteInfo siteInfo)
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
                        Credentials = new System.Net.NetworkCredential(SiteInfo.Username, SiteInfo.Password),
                    };

                    Client.Connect();
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
            throw new NotImplementedException();
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
                throw new Exception("File transfer aborted. Error acquiring FTP stream sender from source file \"" + filePath + "\". " + ex.Message);
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
                throw new Exception("File transfer aborted. Error acquiring FTP stream receiver at target file \"" + filePath + "\". " + ex.Message);
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

        public Dictionary<string, bool> ListItemsNameRecursively(string folder)
        {
            var ret = new Dictionary<string, bool>();
            listFilesAndFolders(Client, folder, ret);
            return ret;
        }

        private static void listFilesAndFolders(FtpClient client, string folderPath, Dictionary<string, bool> ret)
        {
            var x = client.GetListing(folderPath);
            foreach (var item in x)
            {

                if (item.Type == FtpObjectType.Directory)
                {
                    ret.Add(item.FullName, true);
                    listFilesAndFolders(client, item.FullName, ret);
                }
                else
                {
                    ret.Add(item.FullName, false);
                }
            }
        }

        public void FolderDelete(string folder)
        {
            DeleteFolder(Client, folder);
        }

        private static void DeleteFolder(FtpClient client, string folderPath)
        {
            // List the files and folders in the current directory
            foreach (var item in client.GetListing(folderPath))
            {
                // If the item is a file, delete it
                if (item.Type == FtpObjectType.File)
                {
                    client.DeleteFile(item.FullName);
                }
                else if (item.Type == FtpObjectType.Directory)
                {
                    DeleteFolder(client, item.FullName);
                }
            }

            // Delete the current directory after deleting its contents
            client.DeleteDirectory(folderPath);
        }
    }
}