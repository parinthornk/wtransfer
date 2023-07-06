using System.Collections.Generic;
using System.Diagnostics;

namespace WTRANSFER
{
    internal class SiteOperations
    {
        private static string GetParam(dynamic json, string fieldName, bool required)
        {
            if (json == null)
            {
                if (required)
                {
                    throw new ArgumentNullException(fieldName);
                }
                else
                {
                    return null;
                }
            }
            string x = json[fieldName];
            if (x == null && required)
            {
                throw new ArgumentNullException(fieldName);
            }
            return x;
        }

        public static Dictionary<string, FileServer> DictServer { get; set; } = new Dictionary<string, FileServer>();

        internal static ProcessRequest Execute(string body)
        {
            // json body
            var json = Newtonsoft.Json.JsonConvert.DeserializeObject<dynamic>(body);

            // valid actions
            var actions_available = new List<string>()
            {
                "server.open",
                "server.close",
                "object.list",
                "directory.exists",
                "directory.create",
                "directory.delete",
                "file.exists",
                "file.delete",
                "file.move_internal",
                "file.move_external",
                "transfer",
                "transfer_car03",
            };

            // action -> list, delete file, delete folder, move
            string action = GetParam(json, "action", true);
            action = action.ToLower();

            // invalida ction
            if (!actions_available.Contains(action))
            {
                throw new Exception("Action \"" + action + "\" is not supported.");
            }

            // server.open
            if (action == "server.open")
            {
                var site = GetParam(json, "site", true).ToLower();
                var fileServer = FileServer.GetByName(site);
                fileServer.Open();
                string id = Guid.NewGuid().ToString();
                DictServer.Add(id, fileServer);
                return new ProcessRequest()
                {
                    StatusCode = 200,
                    Content = Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>
                    {
                        { "id", id },
                    }),
                };
            }

            // object.list
            if (action == "object.list")
            {
                string id = GetParam(json, "id", true);
                var fs = (IFileServer)(DictServer[id]);
                string directory = GetParam(json, "directory", true);
                if (directory == "/" || directory == "")
                {
                    directory = "/";
                }
                else
                {
                    if (!directory.StartsWith("/"))
                    {
                        directory = "/" + directory;
                    }
                    while (directory.EndsWith("/"))
                    {
                        directory = directory.Substring(0, directory.Length - 1);
                    }
                }

                List<FileItem> list = fs.ItemsList(directory);
                var ls = new List<object>();
                foreach (var item in list)
                {
                    ls.Add(new Dictionary<string, object>
                    {
                        { "size", item.Size },
                        { "name", item.Name },
                        { "isDirectory", item.IsDirectory },
                        { "timestamp", item.Timestamp.ToString("yyyy-MM-dd HH:mm:ss") }
                    });
                }
                
                return new ProcessRequest()
                {
                    StatusCode = 200,
                    Content = Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>
                    {
                        { "count", list.Count },
                        { "objects", ls }
                    }),
                };
            }

            // directory.exists
            if (action == "directory.exists")
            {
                string id = GetParam(json, "id", true);
                string directory = GetParam(json, "directory", true);
                var fs = (IFileServer)(DictServer[id]);
                bool exists = fs.DirectoryExists(directory);
                return new ProcessRequest()
                {
                    StatusCode = exists ? 200 : 404,
                    Content = Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>
                    {
                        { "exists", exists },
                    }),
                };
            }

            // file.exists
            if (action == "file.exists")
            {
                string id = GetParam(json, "id", true);
                string file = GetParam(json, "file", true);
                var fs = (IFileServer)(DictServer[id]);
                bool exists = fs.FileExists(file);
                return new ProcessRequest()
                {
                    StatusCode = exists ? 200 : 404,
                    Content = Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>
                    {
                        { "exists", exists },
                    }),
                };
            }

            // directory.create
            if (action == "directory.create")
            {
                string id = GetParam(json, "id", true);
                string directory = GetParam(json, "directory", true);
                var fs = (IFileServer)(DictServer[id]);
                fs.DirectoryCreate(directory);
                return new ProcessRequest()
                {
                    StatusCode = 201,
                    Content = Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>
                    {
                        { "message", "success" },
                    }),
                };
            }

            // file.delete
            if (action == "file.delete")
            {
                string id = GetParam(json, "id", true);
                string file = GetParam(json, "file", true);
                var fs = (IFileServer)(DictServer[id]);
                fs.FileDelete(file);
                return new ProcessRequest()
                {
                    StatusCode = 200,
                    Content = Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>
                    {
                        { "message", "success" },
                    }),
                };
            }

            // file.move_internal
            if (action == "file.move_internal")
            {
                string id = GetParam(json, "id", true);
                string sourceFile = GetParam(json, "sourceFile", true);
                string targetFile = GetParam(json, "targetFile", true);
                var fs = (IFileServer)(DictServer[id]);
                fs.TransferInternal(sourceFile, targetFile);
                return new ProcessRequest()
                {
                    StatusCode = 200,
                    Content = Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>
                    {
                        { "message", "success" },
                    }),
                };
            }

            // file.move_external
            if (action == "file.move_external")
            {
                string id = GetParam(json, "id", true);
                string sourceFile = GetParam(json, "sourceFile", true);
                string targetFile = GetParam(json, "targetFile", true);

                var fs = (IFileServer)(DictServer[id]);
                var targetServer = SiteInfo.GetByName(GetParam(json, "targetServer", true));

                fs.TransferExternal(targetServer, sourceFile, targetFile);
                return new ProcessRequest()
                {
                    StatusCode = 200,
                    Content = Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>
                    {
                        { "message", "success" },
                    }),
                };
            }

            // transfer
            if (action == "transfer")
            {
                string transferMode = GetParam(json, "transferMode", true);
                string sourceServer = GetParam(json, "sourceServer", true);
                string sourceFile = GetParam(json, "sourceFile", true);
                string targetServer = GetParam(json, "targetServer", true);
                string targetFile = GetParam(json, "targetFile", true);
                dynamic? pgp = null; //json["pgp"];
                if (json != null)
                {
                    pgp = json["pgp"];
                }
                string pgp_direction = GetParam(pgp, "direction", false);
                string pgp_publicKey = GetParam(pgp, "publicKey", false);
                string pgp_privateKey = GetParam(pgp, "privateKey", false);
                string pgp_password = GetParam(pgp, "password", false);

                return Transfer.Execute(transferMode, sourceServer, sourceFile, targetServer, targetFile, pgp_direction, pgp_publicKey, pgp_privateKey, pgp_password);

            }

            // transfer_car03
            if (action == "transfer_car03")
            {

                var item = json["item"];

                var transferMode = "COPY";

                var siteSourceInfo = SiteInfo.FromJsonString(json["siteSource"].ToString());

                var siteTargetInfo = SiteInfo.FromJsonString(json["siteTarget"].ToString());

                string? sourceFile = item["folderArchive"] + "/" + item["fileNameArchive"];

                string? targetFile = json["schedule"]["staticDirTarget"] + "/" + item["fileName"];

                string? pgpDirection = null;

                string? pgpPublicKeyPath = null;

                string? pgpPrivateKeyPath = null;

                string? pgpPassword = null;

                return Transfer.Execute(transferMode, siteSourceInfo, sourceFile, siteTargetInfo, targetFile, pgpDirection, pgpPublicKeyPath, pgpPrivateKeyPath, pgpPassword, item);

            }

            // server.close
            if (action == "server.close")
            {
                string id = GetParam(json, "id", true);
                var fs = (IFileServer)(DictServer[id]);
                fs.Close();
                DictServer.Remove(id);
                return new ProcessRequest()
                {
                    StatusCode = 200,
                    Content = Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>
                    {
                        { "message", "success" },
                    }),
                };
            }

            // 
            return new ProcessRequest()
            {
                StatusCode = 200,
                Content = Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>
                {
                    { "message", "hello" }, 
                    { "action", action }, 
                }),
            };
        }
    }
}