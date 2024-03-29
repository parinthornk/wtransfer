﻿using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using static System.Collections.Specialized.BitVector32;

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

        internal static ProcessRequest Execute(string body)
        {
            int statusCode = 200;
            try
            {
                // json body
                dynamic? json = null;
                try
                {
                    if (string.IsNullOrEmpty(body))
                    {
                        statusCode = 400;
                        throw new Exception("Missing JSON body.");
                    }
                    json = Newtonsoft.Json.JsonConvert.DeserializeObject<dynamic>(body);
                }
                catch (Exception ex)
                {
                    statusCode = 400;
                    throw new InvalidDataException("Error parsing JSON data. " + ex.Message);
                }

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
                    "directory.transfer",
                };

                // action -> list, delete file, delete folder, move
                string action = GetParam(json, "action", true);
                action = action.ToLower();

                // invalida ction
                if (!actions_available.Contains(action))
                {
                    statusCode = 400;
                    throw new InvalidOperationException("Action \"" + action + "\" is not supported.");
                }

                // server.open
                if (action == "server.open")
                {
                    string site = GetParam(json, "site", true);
                    var fileServer = FileServer.GetByName(site);
                    fileServer.Open();
                    string id = Guid.NewGuid().ToString();
                    SessionManager.IFileServerAdd(id, fileServer);
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
                    var fs = SessionManager.IFileServerGet(id);
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
                    var fs = SessionManager.IFileServerGet(id);
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
                    var fs = SessionManager.IFileServerGet(id);
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
                    var fs = SessionManager.IFileServerGet(id);
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
                    var fs = SessionManager.IFileServerGet(id);
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
                    var fs = SessionManager.IFileServerGet(id);
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

                    var fs = SessionManager.IFileServerGet(id);
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

                    var schedule = json["schedule"];

                    var transferMode = "COPY";

                    var siteSourceInfo = SiteInfo.FromJsonString(json["siteSource"].ToString());

                    var siteTargetInfo = SiteInfo.FromJsonString(json["siteTarget"].ToString());

                    string? sourceFile = item["folderArchive"] + "/" + item["fileNameArchive"];

                    string? targetFile = schedule["staticDirTarget"] + "/" + item["fileName"];

                    string? pgpDirection = schedule["pgpDirection"];

                    string? pgpPublicKeyPath = schedule["pgpKeyPath"];

                    string? pgpPrivateKeyPath = schedule["pgpKeyPath"];

                    string? pgpPassword = schedule["pgpKeyPassword"];

                    return Transfer.Execute(transferMode, siteSourceInfo, sourceFile, siteTargetInfo, targetFile, pgpDirection, pgpPublicKeyPath, pgpPrivateKeyPath, pgpPassword, item);

                }

                // directory.transfer
                if (action == "directory.transfer")
                {
                    string transferMode = GetParam(json, "transferMode", true);
                    string sourceServer = GetParam(json, "sourceServer", true);
                    string sourceFile = GetParam(json, "sourceFolder", true);
                    string targetServer = GetParam(json, "targetServer", true);
                    string targetFile = GetParam(json, "targetFolder", true);
                    return TestSpecialCasePSom_01.TestMoveEntireFolder(transferMode, sourceServer, sourceFile, targetServer, targetFile);
                }

                // server.close
                if (action == "server.close")
                {
                    string id = GetParam(json, "id", true);
                    var fs = SessionManager.IFileServerGet(id);
                    fs.Close();
                    SessionManager.IFileServerDelete(id);
                    return new ProcessRequest()
                    {
                        StatusCode = 200,
                        Content = Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>
                        {
                            { "message", "success" },
                        }),
                    };
                }
            }
            catch (Exception ex)
            {
                return new ProcessRequest()
                {
                    StatusCode = 500,
                    Content = Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>
                    {
                        { "result", "failed" },
                        { "error", new Dictionary<string, object> {
                            { "message", ex.Message },
                        } },
                    }),
                };
            }

            return new ProcessRequest()
            {
                StatusCode = 200,
                Content = Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>
                {
                    { "message", "success" },
                }),
            };
        }
    }
}