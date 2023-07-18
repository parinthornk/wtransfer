using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;

namespace WTRANSFER
{
    internal class TestSpecialCasePSom_01
    {
        private static readonly Random random = new();
        private static object _lock_rnd = new object();

        public static int GetRandomNumber(int min, int max)
        {
            lock (_lock_rnd)
            {
                return random.Next(min, max);
            }
        }

        public static ProcessRequest TestMoveEntireFolder(string transferMode, string source, string source_folder, string target, string target_folder)
        {
            /*var transferMode = "COPY";

            var source = "or-new-marine";
            var source_folder = "/PTTOR-Marine_E-Order_test/oreo/zparinthornk/complex folder";

            var target = "aws-or-sftp";
            var target_folder = "/MRWeb/MR_Web_Report/Archive/zparinthornk/result complex";*/

            // path validation
            {

            }

            var ret = new ProcessRequest
            {
                StatusCode = 200,
                Content = Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>
                {
                    { "message", "File transfer completed." },
                }),
            };

            Exception exception = null;

            IFileServer source_server = null;
            IFileServer target_server = null;

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

                // login to source server
                {
                    try
                    {
                        source_server = FileServer.GetByName(source);
                    }
                    catch (Exception ex)
                    {
                        throw new Exception("File transfer aborted. Error loading connection parameters of \"" + source + "\". " + ex.Message);
                    }
                    try
                    {
                        source_server.Open();
                    }
                    catch (Exception ex)
                    {
                        throw new Exception("File transfer aborted. Error establishing a connection with source \"" + source + "\". " + ex.Message);
                    }
                }

                // abort if no source folder found
                var source_folder_exist = source_server.DirectoryExists(source_folder);
                if (!source_folder_exist)
                {
                    throw new DirectoryNotFoundException("Source folder \"" + source_folder + "\" does not exist.");
                }

                // 2. list everything inside the source folder
                Dictionary<string, bool>? tmpDict_Path_isDir = null;
                try
                {
                    tmpDict_Path_isDir = source_server.ListItemsNameRecursively(source_folder);
                }
                catch (Exception ex)
                {
                    throw new Exception("Error retrieving items inside source folder \"" + source_folder + "\". " + ex.Message);
                }

                // login to target server
                {
                    try
                    {
                        target_server = FileServer.GetByName(target);
                    }
                    catch (Exception ex)
                    {
                        throw new Exception("File transfer aborted. Error loading connection parameters of \"" + target + "\". " + ex.Message);
                    }
                    try
                    {
                        target_server.Open();
                    }
                    catch (Exception ex)
                    {
                        throw new Exception("File transfer aborted. Error establishing a connection with target \"" + target + "\". " + ex.Message);
                    }
                }

                // delete target folder
                var target_folder_exist = false;
                try
                {
                    target_folder_exist = target_server.DirectoryExists(target_folder);
                }
                catch (Exception ex)
                {
                    throw new Exception("Error pre-checking existence of target folder \"" + target_folder + "\". " + ex.Message);
                }
                if (target_folder_exist)
                {
                    int attempts = 10;
                    for (int i = 0; i < attempts; i++)
                    {
                        try
                        {
                            target_server.FolderDelete(target_folder);
                            break;
                        }
                        catch (Exception ex)
                        {
                            if (i < attempts - 1)
                            {
                                Thread.Sleep(GetRandomNumber(250, 750));
                                continue;
                            }
                            else
                            {
                                throw new Exception("Error while deleting the already existed target folder \"" + target_folder + "\". " + ex.Message);
                            }
                        }
                    }
                }

                // create directories on target
                var target_path_for_print = string.Empty;
                try
                {
                    foreach (var path in tmpDict_Path_isDir.Keys)
                    {
                        var isDir = tmpDict_Path_isDir[path] == true;
                        if (isDir)
                        {
                            var sub_path = path.Replace(source_folder, string.Empty);
                            var target_path = target_folder + sub_path;
                            target_path_for_print = target_path;
                            var tf_exist = target_server.DirectoryExists(target_path);
                            if (!tf_exist)
                            {
                                target_server.DirectoryCreate(target_path);
                                Console.WriteLine("Created folder: " + target_path);
                            }
                        }
                    }
                }
                catch (Exception ex)
                {
                    throw new Exception("Error while preparing target sub-folders \"" + target_path_for_print + "\". " + ex.Message);
                }

                // upload all files
                var count_total = 0;

                var listSuccess = new List<string>();
                var listFailed = new List<string>();

                foreach (var source_path in tmpDict_Path_isDir.Keys)
                {
                    var isDir = tmpDict_Path_isDir[source_path] == true;
                    if (!isDir)
                    {
                        count_total++;

                        var sub_path = source_path.Replace(source_folder, string.Empty);
                        var target_path = target_folder + sub_path;

                        // try uploading for 10 times
                        Exception? transfer_error = null;
                        {
                            var attempts = 10;
                            for (int i = 0; i < attempts; i++)
                            {
                                // sender must not be a using but receiver must be a using, otherwise the file will corrupt, what the fuck?
                                Stream? sender = null;
                                try
                                {
                                    using Stream receiver = target_server.GetStreamReceive(target_path);
                                    try
                                    {
                                        sender = source_server.GetStreamSend(source_path);
                                        sender.CopyTo(receiver);
                                        transfer_error = null;
                                        break;
                                    }
                                    catch (Exception ex)
                                    {
                                        transfer_error = ex;
                                    }
                                    try { receiver.Close(); } catch { }
                                }
                                catch (Exception ex)
                                {
                                    transfer_error = ex;
                                }

                                if (sender != null) { try { sender.Close(); } catch { } }
                            }
                        }

                        if (transfer_error != null)
                        {
                            Console.WriteLine("Upload failed : " + target_path);
                            listFailed.Add(source_path);
                        }
                        else
                        {
                            Console.WriteLine("Upload success: " + target_path);
                            listSuccess.Add(source_path);
                        }
                    }
                }

                if (listSuccess.Count == count_total)
                {
                    // delete source folder if required
                    if (transferMode.ToLower() == "move")
                    {
                        try
                        {
                            // delete source
                            int attempts = 10;
                            for (int i = 0; i < attempts; i++)
                            {
                                try
                                {
                                    source_server.FolderDelete(source_folder);
                                    break;
                                }
                                catch
                                {
                                    if (i < attempts - 1)
                                    {
                                        Thread.Sleep(GetRandomNumber(250, 750));
                                        continue;
                                    }
                                    else
                                    {
                                        throw;
                                    }
                                }
                            }

                            // 100% success
                            ret.StatusCode = 200;
                            ret.Content = Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>
                            {
                                { "itemsTotal", count_total },
                                { "itemsSuccess", listSuccess.Count },
                                { "itemsFailed", listFailed.Count },
                                { "success", listSuccess },
                                { "failed", listFailed }
                            });
                        }
                        catch (Exception ex)
                        {
                            // upload success but failed to delete source
                            ret.StatusCode = 500;
                            ret.Content = Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>
                            {
                                { "itemsTotal", count_total },
                                { "itemsSuccess", listSuccess.Count },
                                { "itemsFailed", listFailed.Count },
                                { "success", listSuccess },
                                { "failed", listFailed },
                                { "message", "Target folder and sub-items was uploaded successfully but the program failed to delete the source folder. " + ex.Message }
                            });
                        }
                    }
                    else
                    {
                        // 100% success
                        ret.StatusCode = 200;
                        ret.Content = Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>
                        {
                            { "itemsTotal", count_total },
                            { "itemsSuccess", listSuccess.Count },
                            { "itemsFailed", listFailed.Count },
                            { "success", listSuccess },
                            { "failed", listFailed }
                        });
                    }
                }
                else
                {
                    if (listSuccess.Count == 0)
                    {
                        ret.StatusCode = 500;
                    }
                    else
                    {
                        ret.StatusCode = 207;
                    }
                    ret.Content = Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>
                    {
                        { "itemsTotal", count_total },
                        { "itemsSuccess", listSuccess.Count },
                        { "itemsFailed", listFailed.Count },
                        { "success", listSuccess },
                        { "failed", listFailed }
                    });
                }
            }
            catch (Exception ex)
            {
                exception = ex;
            }

            // cleanup
            if (source_server != null) { try { source_server.Close(); } catch { } }
            if (target_server != null) { try { target_server.Close(); } catch { } }

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
    }
}