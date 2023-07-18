using FluentFTP;
using Renci.SshNet;
using Renci.SshNet.Sftp;
using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace WTRANSFER
{
    public class FileItem
    {
        public string Name { get; set; }
        public long Size { get; set; }
        public bool IsDirectory { get; set; }
        public DateTime Timestamp { get; set; }
    }

    public class SiteInfo
    {
        public string Name { get; set; }
        public string IP { get; set; }
        public int Port { get; set; }
        public string Protocol { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public string KeyPath { get; set; }

        internal static SiteInfo FromJsonString(string context)
        {
            var json = Newtonsoft.Json.JsonConvert.DeserializeObject<dynamic>(context);
            string name = json["name"];
            string ip = json["host"];
            int port = json["port"];
            string protocol = json["protocol"];
            string username = json["username"];
            string password = json["password"];
            string keyPath = json["keyPath"];
            return new SiteInfo()
            {
                Name = name,
                IP = ip,
                Port = port,
                Protocol = protocol,
                Username = username,
                Password = password,
                KeyPath = keyPath
            };
        }

        public static SiteInfo GetByName(string siteName)
        {
            string endpoint = Config.Get("endpoint-wtransfer");

            var response = HttpResponse.GetResponse("get", endpoint + "/workspaces/default/sites/" + siteName, null, null);

            var context = Encoding.UTF8.GetString(response.Content);

            return FromJsonString(context);
        }
    }

    public class FileServer
    {
        public static IFileServer GetByName(string siteName)
        {
            SiteInfo info = SiteInfo.GetByName(siteName);

            return Create(info);
        }

        public static IFileServer Create(SiteInfo siteInfo)
        {
            if (siteInfo.Protocol.ToLower() == "sftp")
            {
                return new ServerSFTP(siteInfo);
            }
            if (siteInfo.Protocol.ToLower() == "ftp")
            {
                return new ServerFTP(siteInfo);
            }
            if (siteInfo.Protocol.ToLower() == "ftps")
            {
                return new ServerFTPS(siteInfo);
            }
            throw new NotImplementedException();
        }
    }

    public interface IFileServer
    {
        List<FileItem> ItemsList(string path);
        List<FileItem> FilesList(string path);
        List<FileItem> DirectoriesList(string path);
        bool FileExists(string path);
        void FileDelete(string path);
        bool DirectoryExists(string path);
        void DirectoryCreate(string path);
        void TransferExternal(SiteInfo targetSiteInfo, string filePathSource, string filePathTarget);
        void TransferInternal(string absPathOld, string absPathNew);
        void Open();
        void Close();
        Stream GetStreamSend(string path);
        Stream GetStreamReceive(string path);
        void FileRename(string before, string after);
        void FolderDelete(string folder);

        Dictionary<string, bool> ListItemsNameRecursively(string folder);
    }

    public class SessionManager
    {
        private static Dictionary<string, FileServer> DictServer { get; set; } = new Dictionary<string, FileServer>();
        private static object _locker_dict_server = new();

        public static IFileServer IFileServerGet(string id)
        {
            lock (_locker_dict_server)
            {
                if (DictServer.ContainsKey(id))
                {
                    return (IFileServer)DictServer[id];
                }
                else
                {
                    throw new KeyNotFoundException("The key \"" + id + "\" could not be found.");
                }
            }
        }

        public static void IFileServerAdd(string id, dynamic fileServer)
        {
            lock (_locker_dict_server)
            {
                DictServer.Add(id, fileServer);
            }
        }

        public static void IFileServerDelete(string id)
        {
            lock (_locker_dict_server)
            {
                DictServer.Remove(id);
            }
        }
    }
}