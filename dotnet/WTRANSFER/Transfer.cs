using Org.BouncyCastle.Ocsp;
using Org.BouncyCastle.Utilities;
using Renci.SshNet;
using System;
using System.Net;
using System.Text;
using System.Text.Json.Nodes;
using System.Xml.Linq;
using WTRANSFER;
using static System.Net.Mime.MediaTypeNames;
using static Transfer;

internal class Transfer
{
    public enum LogType
    {
        INFO,
        WARNING,
        ERROR,
    }

    public enum ItemStatus
    {
        CREATED,
        QUEUED,
        ERROR_ENQUEUE,
        DEQUEUED,
        EXECUTING,
        WAITING_FOR_RETRY,
        FAILED,
        SUCCESS,
        DISMISS,
    }

    public enum LogTitle
    {
        DEFAULT,
        /*PREPARATION,
        CONNECTION,
        TRANSFER,
        FINISHED,*/
    }

    public static void AddItemLog(dynamic item, LogType logType, LogTitle title, string body)
    {
        if (item == null)
        {
            return;
        }

        string endpoint = Config.Get("endpoint-wtransfer");
        if (string.IsNullOrEmpty(endpoint))
        {
            return;
        }

        string workspace = item["workspace"];
        if (string.IsNullOrEmpty(workspace))
        {
            return;
        }

        string name = item["name"];
        if (string.IsNullOrEmpty(name))
        {
            return;
        }

        string s_logType = logType.ToString();
        string s_title = title.ToString();

        var bytes = Encoding.UTF8.GetBytes(Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>()
        {
            { "id", 0 },
            { "item", name },
            { "logType", s_logType },
            { "title", s_title },
            { "body", body },
        }));

        int attempts = 15;
        int sleep = 200;
        for (int i = 0; i < attempts; i++)
        {
            try
            {
                var response = HttpResponse.GetResponse("post", endpoint + "/workspaces/" + item.workspace + "/items/" + item.name + "/logs", null, bytes);
                if (199 < response.StatusCode && response.StatusCode < 300)
                {
                    break;
                }
            }
            catch
            {
                if (i < attempts - 1)
                {
                    Thread.Sleep(sleep);
                    continue;
                }
                else
                {
                    throw;
                }
            }
        }
    }

    public static void SetItemStatus(dynamic item, ItemStatus status)
    {
        AddItemLog(item, LogType.INFO, LogTitle.DEFAULT, "Set item status to \"" + status.ToString() + "\".");

        if (item == null)
        {
            return;
        }

        string endpoint = Config.Get("endpoint-wtransfer");
        if (string.IsNullOrEmpty(endpoint))
        {
            return;
        }

        string workspace = item["workspace"];
        if (string.IsNullOrEmpty(workspace))
        {
            return;
        }

        string name = item["name"];
        if (string.IsNullOrEmpty(name))
        {
            return;
        }

        string session = item["session"];
        if (string.IsNullOrEmpty(session))
        {
            return;
        }

        var bytes = Encoding.UTF8.GetBytes(Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>()
        {
            { "id", 0 },
            { "status", status.ToString().ToUpper() },
        }));

        int attempts = 15;
        int sleep = 200;
        for (int i = 0; i < attempts; i++)
        {
            try
            {
                var response = HttpResponse.GetResponse("post", endpoint + "/workspaces/" + workspace + "/sessions/" + session + "/items/" + name + "/patch", null, bytes);

                if (199 < response.StatusCode && response.StatusCode < 300)
                {
                    break;
                }
            }
            catch
            {
                if (i < attempts - 1)
                {
                    Thread.Sleep(sleep);
                    continue;
                }
                else
                {
                    throw;
                }
            }
        }
    }

    internal static ProcessRequest Execute(string transferMode, string siteSourceName, string sourceFile, string siteTargetName, string targetFile, string pgpDirection, string pgpPublicKeyPath, string pgpPrivateKeyPath, string pgpPassword, dynamic item = null)
    {

        var ret = new ProcessRequest
        {
            StatusCode = 200,
            Content = Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>
            {
                { "message", "File transfer completed." },
            }),
        };

        Exception exception = null;

        try
        {
            // validate transferMode
            if (transferMode == null)
            {
                ret.StatusCode = 400;
                throw new MissingFieldException("Field \"transferMode\" is required.");
            }
            else
            {
                if (transferMode.ToLower() != "copy" && transferMode.ToLower() != "move")
                {
                    ret.StatusCode = 400;
                    throw new InvalidOperationException("The field \"transferMode\" must be assigned to \"MOVE\" or \"COPY\".");
                }
            }

            // load source site info
            SiteInfo siteSourceInfo;
            try
            {
                AddItemLog(item, LogType.INFO, LogTitle.DEFAULT, "Loading site configuration \"" + siteSourceName + "\"...");
                siteSourceInfo = SiteInfo.GetByName(siteSourceName);
            }
            catch (Exception ex)
            {
                throw new Exception("File transfer aborted. Error loading connection parameters of \"" + siteSourceName + "\". " + ex.Message);
            }

            // load target site info
            SiteInfo siteTargetInfo;
            try
            {
                AddItemLog(item, LogType.INFO, LogTitle.DEFAULT, "Loading site configuration \"" + siteTargetName + "\"...");
                siteTargetInfo = SiteInfo.GetByName(siteTargetName);
            }
            catch (Exception ex)
            {
                throw new Exception("File transfer aborted. Error loading connection parameters of \"" + siteTargetName + "\". " + ex.Message);
            }

            // execute
            ret = Execute(transferMode, siteSourceInfo, sourceFile, siteTargetInfo, targetFile, pgpDirection, pgpPublicKeyPath, pgpPrivateKeyPath, pgpPassword, item);
        }
        catch (Exception ex)
        {
            exception = ex;
        }

        // throw if there is error
        if (exception != null)
        {
            if (ret.StatusCode == 200)
            {
                ret.StatusCode = 500;
            }
            ret.Content = Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>
            {
                { "message", exception.Message },
            });
        }

        return ret;
    }

    internal static ProcessRequest Execute(string transferMode, SiteInfo siteSourceInfo, string sourceFile, SiteInfo siteTargetInfo, string targetFile, string pgpDirection, string pgpPublicKeyPath, string pgpPrivateKeyPath, string pgpPassword, dynamic item = null)
    {

        SetItemStatus(item, ItemStatus.EXECUTING);

        /*// transfer mode -> COPY, MOVE
        var transferMode = "COPY";*/

        /*// sftp source -> dcloud
        var siteSourceName = "dcloud-sftp";
        var sourceFile = "/test-zparinthornk/encrypted/encrypted-ปรินทร.xlsx.pgp";*/

        /*// sftp source -> FTPS, legacy-f4e9cd76-e7bd-4d3a-a5a6-acf1e061ea2a
        var siteSourceName = "legacy-f4e9cd76-e7bd-4d3a-a5a6-acf1e061ea2a";
        var sourceFile = "/WSO2/zparinthornk/encrypted/encrypted-ปรินทร.xlsx.pgp";*/

        /*// sftp target -> ev
        var siteTargetName = "legacy-0f1b722f-3675-4348-959e-37c9177cf7ac";
        var targetFile = "/EVChargingStation/DEVELOPMENT/STATUS_BILLING/INPUT/WSO2_TEST/decrypted/decrypted-ปรินทร.xlsx";*/

        /*// sftp target -> FTPS, legacy-8305740c-af62-45ad-a87f-f3499241b144
        var siteTargetName = "legacy-8305740c-af62-45ad-a87f-f3499241b144";
        var targetFile = "/BankStatement_test/test/WSO2_TEST/acf1e061ea2a/decrypted/decrypted-ปรินทร.xlsx";*/

        /*// pgp
        var pgpDirection = "decrypt";
        var pgpPublicKeyPath = "\\pgp-keys\\0xBD06F3AF-pub.asc";
        var pgpPrivateKeyPath = "\\pgp-keys\\0xBD06F3AF-sec.asc";
        var pgpPassword = "P@ssw0rd";*/

        var ret = new ProcessRequest
        {
            StatusCode = 200,
            Content = Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>
            {
                { "message", "File transfer completed." },
            }),
        };

        // dispose objects
        IFileServer siteSource = null;
        Stream pgpStream = null;
        IFileServer siteTarget = null;

        // exception
        Exception exception = null;

        // tmp file for pgp decryption
        string pgpDecryptTmp = string.Empty;

        // start execute
        try
        {
            // validate transferMode
            AddItemLog(item, LogType.INFO, LogTitle.DEFAULT, "Validating transferMode \"" + transferMode + "\"...");
            if (transferMode == null)
            {
                ret.StatusCode = 400;
                throw new MissingFieldException("Field \"transferMode\" is required.");
            }
            else
            {
                if (transferMode.ToLower() != "copy" && transferMode.ToLower() != "move")
                {
                    ret.StatusCode = 400;
                    throw new InvalidOperationException("The field \"transferMode\" must be assigned to \"MOVE\" or \"COPY\".");
                }
            }

            // source open
            AddItemLog(item, LogType.INFO, LogTitle.DEFAULT, "Connecting to source server \"" + siteSourceInfo.IP + ":" + siteSourceInfo.Port + "\" as \"" + siteSourceInfo.Username + "\"...");
            try
            {
                siteSource = FileServer.Create(siteSourceInfo);
                siteSource.Open();
            }
            catch (Exception ex)
            {
                throw new Exception("File transfer aborted. Error establishing a connection with source \"" + siteSourceInfo.Name + "\". " + ex.Message);
            }

            // source stream
            AddItemLog(item, LogType.INFO, LogTitle.DEFAULT, "Establishing source sender stream of \"" + sourceFile + "\"...");
            using var streamSource = siteSource.GetStreamSend(sourceFile);

            // pgp process
            if (!string.IsNullOrEmpty(pgpDirection))
            {
                if (pgpDirection.ToLower() == "encrypt")
                {
                    try
                    {
                        AddItemLog(item, LogType.INFO, LogTitle.DEFAULT, "Establishing PGP-encrypted stream using public key \"" + pgpPublicKeyPath + "\"...");
                        pgpStream = PGP.Encrypt(streamSource, pgpPublicKeyPath);
                    }
                    catch (Exception ex)
                    {
                        throw new Exception("File transfer aborted. Error encrypting source stream. " + ex.Message);
                    }
                }
                else if (pgpDirection.ToLower() == "decrypt")
                {
                    try
                    {
                        AddItemLog(item, LogType.INFO, LogTitle.DEFAULT, "Establishing PGP-decrypted stream using private key \"" + pgpPrivateKeyPath + "\"...");
                        pgpStream = PGP.Decrypt(streamSource, pgpPrivateKeyPath, pgpPassword, out pgpDecryptTmp);
                    }
                    catch (Exception ex)
                    {
                        throw new Exception("File transfer aborted. Error decrypting source stream. " + ex.Message);
                    }
                }
                else
                {
                    AddItemLog(item, LogType.INFO, LogTitle.DEFAULT, "No PGP configured.");
                    pgpStream = streamSource;
                }
            }
            else
            {
                AddItemLog(item, LogType.INFO, LogTitle.DEFAULT, "No PGP configured.");
                pgpStream = streamSource;
            }

            // target open
            AddItemLog(item, LogType.INFO, LogTitle.DEFAULT, "Connecting to target server \"" + siteTargetInfo.IP + ":" + siteTargetInfo.Port + "\" as \"" + siteTargetInfo.Username + "\"...");
            try
            {
                siteTarget = FileServer.Create(siteTargetInfo);
                siteTarget.Open();
            }
            catch (Exception ex)
            {
                throw new Exception("File transfer aborted. Error establishing a connection with the target \"" + siteTargetInfo.Name + "\". " + ex.Message);
            }

            // delete tha existing file with the same name that we want to rename into
            AddItemLog(item, LogType.INFO, LogTitle.DEFAULT, "Checking if the file \"" + targetFile + "\" already exist on target server...");
            bool targetAlreadyExists = false;
            try
            {
                targetAlreadyExists = siteTarget.FileExists(targetFile);
            }
            catch (Exception ex)
            {
                throw new Exception("File transfer aborted. Error pre-checking existence of target file \"" + targetFile + "\". " + ex.Message);
            }
            if (targetAlreadyExists)
            {
                try
                {
                    AddItemLog(item, LogType.INFO, LogTitle.DEFAULT, "Deleting the already existed file \"" + targetFile + "\"...");
                    siteTarget.FileDelete(targetFile);
                }
                catch (Exception ex)
                {
                    throw new Exception("File transfer aborted. Error deleting pre-existed target file \"" + targetFile + "\". " + ex.Message);
                }
            }

            // create target directory if not exist
            AddItemLog(item, LogType.INFO, LogTitle.DEFAULT, "Checking if target folder already exist...");
            var tmpp = targetFile.Split('/');
            var targetFolderName = "";
            try
            {
                targetFolderName = targetFile[..(targetFile.Length - tmpp[^1].Length - 1)];
            }
            catch (Exception ex)
            {
                throw new Exception("File transfer aborted. Error extracting folder name from absolute path \"" + targetFile + "\". " + ex.Message);
            }
            if (targetFolderName != "" && targetFolderName != "/")
            {
                var targetDirExists = false;
                try
                {
                    AddItemLog(item, LogType.INFO, LogTitle.DEFAULT, "Checking if target folder \"" + targetFolderName + "\" already exist...");
                    targetDirExists = siteTarget.DirectoryExists(targetFolderName);
                }
                catch (Exception ex)
                {
                    throw new Exception("File transfer aborted. Error checking existence of target folder \"" + targetFolderName + "\". " + ex.Message);
                }
                if (!targetDirExists)
                {
                    try
                    {
                        AddItemLog(item, LogType.INFO, LogTitle.DEFAULT, "Target folder does not exist, creating new folder \"" + targetFolderName + "\"...");
                        siteTarget.DirectoryCreate(targetFolderName);
                    }
                    catch (Exception ex)
                    {
                        throw new Exception("File transfer aborted. Error creating target folder \"" + targetFolderName + "\". " + ex.Message);
                    }
                }
            }

            // temporary file name
            var temporaryFileName = targetFolderName + "/" + Guid.NewGuid().ToString() + ".lock";

            // stream transfer
            AddItemLog(item, LogType.INFO, LogTitle.DEFAULT, "Establishing target receiver stream as \"" + temporaryFileName + "\"...");
            using (var streamTarget = siteTarget.GetStreamReceive(temporaryFileName))
            {
                try
                {
                    AddItemLog(item, LogType.INFO, LogTitle.DEFAULT, "Copying...");
                    
                    // internal retries
                    int attempts = 10;
                    int sleep = 1000;
                    for (int i = 0; i < attempts; i++)
                    {
                        try
                        {
                            pgpStream.CopyTo(streamTarget);
                            break;
                        }
                        catch
                        {
                            if (i < attempts - 1)
                            {
                                Thread.Sleep(sleep);
                                continue;
                            }
                            else
                            {
                                throw;
                            }
                        }
                    }

                }
                catch (Exception ex)
                {
                    throw new Exception("File transfer aborted. Error while uploading to target \"" + temporaryFileName + "\". " + ex.Message);
                }
            }

            // rename temporaryFileName to the real name
            AddItemLog(item, LogType.INFO, LogTitle.DEFAULT, "Renaming from \"" + temporaryFileName + "\" to \"" + targetFile + "\"...");
            try
            {
                siteTarget.FileRename(temporaryFileName, targetFile);
            }
            catch (Exception ex)
            {
                // what happen to FTP ??? -> just restart the connection and then we can rename
                if ((siteTarget is ServerFTPS || siteTarget is ServerFTP) && ex.Message.ToLower().Contains("Bad sequence of commands".ToLower()))
                {
                    try { siteTarget.Close(); } catch { }
                    try { siteTarget.Open(); } catch { }
                    try
                    {
                        siteTarget.FileRename(temporaryFileName, targetFile);
                    }
                    catch (Exception ex2)
                    {
                        throw new Exception("The file was successfully uploaded as \"" + temporaryFileName + "\" but it was failed to be renamed back to \"" + targetFile + "\". (2)" + ex2.Message);
                    }
                }
                else
                {
                    throw new Exception("The file was successfully uploaded as \"" + temporaryFileName + "\" but it was failed to be renamed back to \"" + targetFile + "\". (1)" + ex.Message);
                }
            }

            // delete source file if needed
            if (transferMode.ToLower() == "move")
            {
                AddItemLog(item, LogType.INFO, LogTitle.DEFAULT, "Deleting the source file \"" + sourceFile + "\"...");
                try
                {
                    siteSource.FileDelete(sourceFile);
                }
                catch (Exception ex)
                {
                    throw new Exception("File transfer is success but the program failed to delete the source file \"" + sourceFile + "\". " + ex.Message);
                }
            }

            SetItemStatus(item, ItemStatus.SUCCESS);
            AddItemLog(item, LogType.INFO, LogTitle.DEFAULT, "File transfer completed.");
        }
        catch (Exception ex)
        {
            // this will be thrown after all resources are cleared
            AddItemLog(item, LogType.ERROR, LogTitle.DEFAULT, "File transfer error: " + ex.Message);
            exception = ex;

            // prepare for next retry
            try
            {
                int retryRemaining = item["retryRemaining"] - 1;
                int retryIntervalMs = item["retryIntervalMs"];
                var t_now = DateTime.Now;
                var t_nxt = t_now.AddMilliseconds(retryIntervalMs);
                var status = retryRemaining < 1 ? ItemStatus.FAILED : ItemStatus.WAITING_FOR_RETRY;
                SetItemStatus(item, status);
                UpdateOnExecuteFailed(item, retryRemaining, t_nxt, t_now);
            }
            catch
            {

            }
        }

        // clear all resources
        if (siteSource != null) { try { siteSource.Close(); } catch { } }
        if (pgpStream != null) { try { pgpStream.Close(); } catch { } }
        if (siteTarget != null) { try { siteTarget.Close(); } catch { } }

        // delete tmp file
        if (!string.IsNullOrEmpty(pgpDecryptTmp)) { try { File.Delete(pgpDecryptTmp); } catch { } }

        // throw if there is error
        if (exception != null)
        {
            if (ret.StatusCode == 200)
            {
                ret.StatusCode = 500;
            }
            ret.Content = Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>
            {
                { "message", exception.Message },
            });
        }

        return ret;
    }

    private static void UpdateOnExecuteFailed(dynamic item, int retryRemaining, DateTime t_nxt, DateTime t_now)
    {
        if (item == null)
        {
            return;
        }

        string endpoint = Config.Get("endpoint-wtransfer");
        if (string.IsNullOrEmpty(endpoint))
        {
            return;
        }

        string workspace = item["workspace"];
        if (string.IsNullOrEmpty(workspace))
        {
            return;
        }

        string name = item["name"];
        if (string.IsNullOrEmpty(name))
        {
            return;
        }

        string session = item["session"];
        if (string.IsNullOrEmpty(session))
        {
            return;
        }

        var bytes = Encoding.UTF8.GetBytes(Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>()
        {
            { "timeLatestRetry", t_now.ToString(Settings.DateFormat) },
            { "timeNextRetry", t_nxt.ToString(Settings.DateFormat) },
            { "retryRemaining", retryRemaining },
        }));

        int attempts = 15;
        int sleep = 200;
        for (int i = 0; i < attempts; i++)
        {
            try
            {
                var response = HttpResponse.GetResponse("post", endpoint + "/workspaces/" + workspace + "/sessions/" + session + "/items/" + name + "/patch", null, bytes);

                if (199 < response.StatusCode && response.StatusCode < 300)
                {
                    break;
                }
            }
            catch
            {
                if (i < attempts - 1)
                {
                    Thread.Sleep(sleep);
                    continue;
                }
                else
                {
                    throw;
                }
            }
        }
    }

    public static void ExecuteFromCAR03(string receivedJsonString)
    {

        var receivedJsonObject = Newtonsoft.Json.JsonConvert.DeserializeObject<dynamic>(receivedJsonString);

        var item = receivedJsonObject["item"];

        var transferMode = "COPY";

        var siteSourceInfo = SiteInfo.FromJsonString(receivedJsonObject["siteSource"].ToString());

        var siteTargetInfo = SiteInfo.FromJsonString(receivedJsonObject["siteTarget"].ToString());

        string? sourceFile = item["folderArchive"] + "/" + item["fileNameArchive"];

        string? targetFile = receivedJsonObject["schedule"]["staticDirTarget"] + "/" + item["fileName"];

        string? pgpDirection = null;

        string? pgpPublicKeyPath = null;

        string? pgpPrivateKeyPath = null;

        string? pgpPassword = null;

        ProcessRequest result = Execute(transferMode, siteSourceInfo, sourceFile, siteTargetInfo, targetFile, pgpDirection, pgpPublicKeyPath, pgpPrivateKeyPath, pgpPassword, item);

        File.WriteAllText("zresult.txt", result.Content);

        Console.WriteLine(result.Content);


        /*var siteTargetInfo = SiteInfo.GetByName("legacy-fa5367ec-be69-42fa-8465-63f122bd7baf");

        IFileServer server = FileServer.Create(siteTargetInfo);

        server.Open();

        server.FileRename("/SapFax/CNDNDATA/not_exists/9970727a-229f-4fab-bbb9-57db14bd0392.lock", "/SapFax/CNDNDATA/not_exists/file_example_XLSX_2MB-0000.xlsx");

        server.Close();*/
    }
}