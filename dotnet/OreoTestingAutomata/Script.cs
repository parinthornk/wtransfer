using System;
using System.Collections.Generic;
using System.Data.OleDb;
using System.Diagnostics;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.Reflection;
using System.Security.Policy;
using System.Text;
using System.Threading;
using System.Windows.Forms;

namespace OreoTestingAutomata
{
    internal class Script
    {
        public static void FileZillaStart()
        {
            FileZillaStop();
            var file_filezilla = @"C:\Program Files\FileZilla FTP Client\filezilla.exe";
            Process.Start(file_filezilla);

            // fucking popup
            Robot.Wait(3000);
            Robot.ESC();
            Robot.Wait(1000);
        }

        public static void FileZillaStop()
        {
            var chromeDriverProcesses = Process.GetProcesses();
            foreach (var chromeProcess in chromeDriverProcesses)
            {
                if (chromeProcess.ProcessName == "filezilla")
                {
                    chromeProcess.Kill();
                    break;
                }
            }
            Thread.Sleep(2000);
        }

        public static bool FileZillaIsAlive()
        {
            var chromeDriverProcesses = Process.GetProcesses();
            foreach (var chromeProcess in chromeDriverProcesses)
            {
                if (chromeProcess.ProcessName == "filezilla")
                {
                    return true;
                }
            }
            return false;
        }

        public static int[,] GetPixels(Bitmap bitmap)
        {
            var ret = new int[bitmap.Height, bitmap.Width];
            for (int i = 0; i < bitmap.Height; i++)
            {
                for (int j = 0; j < bitmap.Width; j++)
                {
                    ret[i, j] = bitmap.GetPixel(j, i).ToArgb();
                }
            }
            return ret;
        }

        public static int[, ] GetPixels(string file)
        {
            return GetPixels(new Bitmap(file));
        }

        private static int[] GetPosition(int[, ] mother, int[, ] child)
        {
            var mi = mother.GetLength(0);
            var mj = mother.GetLength(1);
            var ci = child.GetLength(0);
            var cj = child.GetLength(1);


            for (int i = 0; i < mi - ci; i++)
            {
                for (int j = 0; j < mj - cj; j++)
                {
                    bool _same = true;
                    for (int ii = 0; ii < ci; ii++)
                    {
                        if (!_same)
                        {
                            break;
                        }
                        for (int jj = 0; jj < cj; jj++)
                        {
                            if (child[ii, jj] != mother[i + ii, j + jj])
                            {
                                _same = false;
                                break;
                            }
                        }
                    }
                    if (_same)
                    {
                        return new int[] { i, j };
                    }
                }
            }

            return null;
        }


        public static void Save(string filename, int[,] b)
        {
            var ii = b.GetLength(0);
            var jj = b.GetLength(1);
            var bm = new Bitmap(jj, ii);
            for (int i = 0; i < ii; i++)
            {
                for (int j = 0; j < jj; j++)
                {
                    bm.SetPixel(j, i, Color.FromArgb(b[i, j]));
                }
            }
            bm.Save(filename, ImageFormat.Png);
        }

        public static bool See(int[,] child, int durationMs, out int[] position)
        {
            position = null;
            var interval = 300;
            var elapsed = 0;
            for (; ; )
            {
                var screen = GetScreenshot();
                var pos = GetPosition(screen, child);
                if (pos != null)
                {
                    position = new int[] { pos[0], pos[1] };
                    return true;
                }
                if (elapsed > durationMs)
                {
                    return false;
                }
                elapsed += interval;
                Thread.Sleep(interval);
            }
        }

        public static int[, ] GetScreenshot()
        {

            var bmpScreenshot = new Bitmap(Screen.PrimaryScreen.Bounds.Width, Screen.PrimaryScreen.Bounds.Height, PixelFormat.Format32bppArgb);

            var gfxScreenshot = Graphics.FromImage(bmpScreenshot);

            // Take the screenshot from the upper left corner to the right bottom corner.
            gfxScreenshot.CopyFromScreen(Screen.PrimaryScreen.Bounds.X, Screen.PrimaryScreen.Bounds.Y, 0, 0, Screen.PrimaryScreen.Bounds.Size, CopyPixelOperation.SourceCopy);

            return GetPixels(bmpScreenshot);
        }

        public static bool FileExistByRestAPI(string site, string tfolder, string tname, long tsize)
        {

            /*var site = "legacy-0f1b722f-3675-4348-959e-37c9177cf7ac";
            var tfolder = "/EVChargingStation/DEVELOPMENT/SALES_TRANSACTION/OUTPUT";
            var tname = "file_example_XLSX_2MB-0004.xlsx";
            long tsize = 2045009;*/

            var jmax = 30;
            for (int j = 0; j < jmax; j++)
            {
                Robot.Wait(4000);
                Console.WriteLine("[" + j + "/" + jmax + "]");
                try
                {
                    var urv = "http://10.224.143.44:8290/wtransfer/workspaces/default/sites/" + site + "/objects?path=" + tfolder;
                    var r = HttpResponse.GetResponse("get", urv, null, null);
                    var content = Encoding.ASCII.GetString(r.Content);

                    var json = Newtonsoft.Json.JsonConvert.DeserializeObject<dynamic>(content);
                    var objects = json["objects"];
                    int objectsCount = objects.Count;
                    for (int i = 0; i < objectsCount; i++)
                    {
                        string name = objects[i]["name"];
                        long size = objects[i]["size"];
                        bool isDirectory = objects[i]["isDirectory"];
                        if (!isDirectory && size == tsize && name == tname)
                        {
                            return true;
                        }
                    }
                    Console.WriteLine(urv + " -> " + objectsCount);
                }
                catch (Exception ex)
                {
                    Console.WriteLine("Error: " + ex.Message);
                }
            }

            return false;
        }

        public static void OpenFileZillaThenNavigateFolder(string host, string username, string password, int port, string folder)
        {
            // UIs
            var pic_filezilla_ready = GetPixels(@"C:\Users\parin\Documents\oreo-ftp\automata\imgs\01-filezilla-ready.png");
            var pic_02_unknown_host_key = GetPixels(@"C:\Users\parin\Documents\oreo-ftp\automata\imgs\02-unknown-host-key.png");
            var pic_03_root_folder_visibility = GetPixels(@"C:\Users\parin\Documents\oreo-ftp\automata\imgs\03-root-folder-visibility.png");
            var pic_02_02_unknown_cert_click_ok = GetPixels(@"C:\Users\parin\Documents\oreo-ftp\automata\imgs\02-02-unknown-cert-click-ok.png");

            // input coordinates
            var pos_host = new int[] { 89, 90 };
            var pos_username = new int[] { 89, 251 };
            var pos_password = new int[] { 89, 434 };
            var pos_port = new int[] { 89, 560 };
            var pos_connect = new int[] { 89, 649 };
            var pos_button_ok_unknown_host_key = new int[] { 202, 357 };
            var pos_text_area_root_folder = new int[] { 11, 177 };

            // start filezilla
            FileZillaStart();

            // find filezilla window
            if (!See(pic_filezilla_ready, 10000, out int[] icon_filezilla_position))
            {
                throw new Exception("Error: could not find FileZilla window.");
            }

            // focus on filezilla window
            Robot.Wait(1000);
            Robot.SetMousePosition(icon_filezilla_position[0], icon_filezilla_position[1] + 200);
            Robot.Wait(100);
            Robot.LeftClick();
            Robot.Wait(300);

            // input host
            Robot.SetMousePosition(icon_filezilla_position[0] + pos_host[0], icon_filezilla_position[1] + pos_host[1]);
            Robot.Wait(300);
            Robot.LeftClick();
            Robot.Wait(300);
            Robot.CopyStringToClipboard(host);
            Robot.CtrlV();
            Robot.Wait(300);

            // input username
            Robot.SetMousePosition(icon_filezilla_position[0] + pos_username[0], icon_filezilla_position[1] + pos_username[1]);
            Robot.Wait(300);
            Robot.LeftClick();
            Robot.Wait(300);
            Robot.LeftClick();
            Robot.Wait(300);
            Robot.CopyStringToClipboard(username);
            Robot.CtrlV();
            Robot.Wait(300);

            // input password
            Robot.SetMousePosition(icon_filezilla_position[0] + pos_password[0], icon_filezilla_position[1] + pos_password[1]);
            Robot.Wait(300);
            Robot.LeftClick();
            Robot.Wait(300);
            Robot.LeftClick();
            Robot.Wait(300);
            Robot.CopyStringToClipboard(password);
            Robot.CtrlV();
            Robot.Wait(300);

            // input port
            Robot.SetMousePosition(icon_filezilla_position[0] + pos_port[0], icon_filezilla_position[1] + pos_port[1]);
            Robot.Wait(300);
            Robot.LeftClick();
            Robot.Wait(300);
            Robot.LeftClick();
            Robot.Wait(300);
            Robot.CopyStringToClipboard(port.ToString());
            Robot.CtrlV();
            Robot.Wait(300);

            // click connect
            Robot.SetMousePosition(icon_filezilla_position[0] + pos_connect[0], icon_filezilla_position[1] + pos_connect[1]);
            Robot.Wait(300);
            Robot.LeftClick();
            Robot.Wait(300);
            Robot.LeftClick();
            Robot.Wait(1300);

            // if prompted
            if (See(pic_02_02_unknown_cert_click_ok, 1000, out int[] position_unknown_cert_click_ok))
            {
                Robot.SetMousePosition(position_unknown_cert_click_ok[0], position_unknown_cert_click_ok[1]);
                Robot.Wait(300);
                Robot.LeftClick();
                Robot.Wait(300);
            }
            else if (See(pic_02_unknown_host_key, 1000, out int[] position_icon_unknown_host_key))
            {
                Robot.SetMousePosition(position_icon_unknown_host_key[0] + pos_button_ok_unknown_host_key[0], position_icon_unknown_host_key[1] + pos_button_ok_unknown_host_key[1]);
                Robot.Wait(300);
                Robot.LeftClick();
                Robot.Wait(300);
            }

            // wait until root folder visible
            if (!See(pic_03_root_folder_visibility, 2000, out int[] position_root_folder_visibility))
            {
                throw new Exception("Error: failed to verify root folder visibility.");
            }

            // enter root folder
            if (!string.IsNullOrEmpty(folder))
            {
                Robot.SetMousePosition(position_root_folder_visibility[0] + pos_text_area_root_folder[0], position_root_folder_visibility[1] + pos_text_area_root_folder[1]);
                Robot.Wait(300);
                Robot.LeftClick();
                Robot.Wait(300);
                Robot.LeftClick();
                Robot.Wait(300);
                Robot.CtrlA();
                Robot.Wait(300);
                Robot.Delete();
                Robot.Wait(600);
                Robot.CopyStringToClipboard(folder);
                Robot.CtrlV();
                Robot.Wait(600);
                Robot.Enter();
                Robot.Wait(300);
            }
        }

        private static void RunAutomate(string scheduleName, string output_folder, string source_host, string source_username, string source_password, int source_port, string source_rootFolder, string target_site_name, string target_host, string target_username, string target_password, int target_port, string target_rootFolder)
        {
            // -------------------------------------------------------------------------------------------------------------------------------------------- //

            var example_file_name = "file_example_XLSX_2MB-0000.xlsx";
            var example_file_size = 2045009;

            // -------------------------------------------------------------------------------------------------------------------------------------------- //

            // for upload
            var pic_04_example_file = GetPixels(@"C:\Users\parin\Documents\oreo-ftp\automata\imgs\04-example-file.png");
            var pic_05_upload_button = GetPixels(@"C:\Users\parin\Documents\oreo-ftp\automata\imgs\05-example-file-upload-button.png");
            var pic_06_transfer_finished = GetPixels(@"C:\Users\parin\Documents\oreo-ftp\automata\imgs\06-transfer-finished-signal.png");

            // drop a source file
            {
                // enter source folder
                for (int i = 0; i < 5; i++)
                {
                    try
                    {
                        OpenFileZillaThenNavigateFolder(source_host, source_username, source_password, source_port, source_rootFolder);
                        break;
                    }
                    catch
                    {

                    }
                }

                // point to example file
                if (!See(pic_04_example_file, 5000, out int[] position_example_file))
                {
                    throw new Exception("Failed to upload example file. The example file is not visible.");
                }

                // 
                Robot.Wait(300);
                Robot.SetMousePosition(position_example_file[0], position_example_file[1]);
                Robot.Wait(600);
                Robot.RightClick();

                // upload button
                if (!See(pic_05_upload_button, 5000, out int[] position_upload_button))
                {
                    throw new Exception("Failed to upload example file. The upload button is nit visible.");
                }

                // 
                Robot.Wait(300);
                Robot.SetMousePosition(position_upload_button[0], position_upload_button[1]);
                Robot.Wait(600);
                Robot.LeftClick();

                // wait transfer fisnished signal
                Robot.Wait(3000);
                //if (!See(pic_06_transfer_finished, 5000, out int[] _))
                //{
                //    throw new Exception("Example file takes too long to be uploaded.");
                //}

                // capture filezilla window and save
                var img_06 = Robot.ScreenCapture.CaptureActiveWindowPixels();
                Save(output_folder + "\\" + "img_06.png", img_06);

                // wait until file is archive?
                int kd = 130;
                for (int k=0;k< kd; k++)
                {
                    Robot.Wait(1000);
                    Console.WriteLine("wait until file is archive -> [" + k + "/" + kd + "]");
                }
                // http://10.224.143.44:8290/wtransfer/workspaces/default/sites/legacy-0f1b722f-3675-4348-959e-37c9177cf7ac/objects?path=/EVChargingStation/DEVELOPMENT/SALES_TRANSACTION/OUTPUT
                if (!FileExistByRestAPI(target_site_name, target_rootFolder, example_file_name, example_file_size))
                {
                    throw new Exception("Error, the example file is not moved to target on time.");
                }

            }

            // verify target
            {
                // enter target folder
                for (int i = 0; i < 5; i++)
                {
                    try
                    {
                        OpenFileZillaThenNavigateFolder(target_host, target_username, target_password, target_port, target_rootFolder);
                        break;
                    }
                    catch
                    {

                    }
                }

                // wait
                Robot.Wait(2000);

                // capture filezilla window and save
                var img_07 = Robot.ScreenCapture.CaptureActiveWindowPixels();
                Save(output_folder + "\\" + "img_07.png", img_07);
            }

            // capture logs
            {
                Process.Start(get_latest_item_url(scheduleName));


                Robot.Wait(1000);
                Robot.LeftClick();
                Robot.Wait(1000);
                Robot.LeftClick();

                // capture browser window and save
                var img_08 = Robot.ScreenCapture.CaptureActiveWindowPixels(); //GetScreenshot();
                Save(output_folder + "\\" + "img_08.png", img_08);

                Robot.CtrlW();
            }

            // capture

            // wait 140 seconds

            // refresh

            // capture

            // navigate to archive

            // capture

            // end

        }

        private static void GenerateCaptureTestResult(string scheduleName, string ricefw, string ricefwCaseIndex, bool force = false)
        {
            // delete already moved file on target site




            // output directory
            var output_folder = ricefw + "\\" + ricefwCaseIndex;

            if (!force)
            {
                try
                {
                    if (File.Exists(output_folder + "\\img_06.png") && File.Exists(output_folder + "\\img_07.png") && File.Exists(output_folder + "\\img_08.png"))
                    {
                        return;
                    }
                }
                catch
                {

                }
            }


            if (Directory.Exists(output_folder))
            {
                Directory.Delete(output_folder, true);
            }
            Directory.CreateDirectory(output_folder);

            // see schedule info
            var jsonSchedule = Newtonsoft.Json.JsonConvert.DeserializeObject<dynamic>(Encoding.ASCII.GetString(HttpResponse.GetResponse("get", "http://10.224.143.44:8290/wtransfer/workspaces/default/schedules/" + scheduleName, null, null).Content));
            string siteSource = jsonSchedule["siteSource"];
            string siteTarget = jsonSchedule["siteTarget"];
            var jsonSiteSource = Newtonsoft.Json.JsonConvert.DeserializeObject<dynamic>(Encoding.ASCII.GetString(HttpResponse.GetResponse("get", "http://10.224.143.44:8290/wtransfer/workspaces/default/sites/" + siteSource, null, null).Content));
            var jsonSiteTarget = Newtonsoft.Json.JsonConvert.DeserializeObject<dynamic>(Encoding.ASCII.GetString(HttpResponse.GetResponse("get", "http://10.224.143.44:8290/wtransfer/workspaces/default/sites/" + siteTarget, null, null).Content));

            // source login
            string source_host = jsonSiteSource["host"];
            string source_username = jsonSiteSource["username"];
            string source_password = jsonSiteSource["password"];
            int source_port = jsonSiteSource["port"];
            string source_rootFolder = jsonSchedule["staticDirSource"];

            // target login
            string target_host = jsonSiteTarget["host"];
            string target_username = jsonSiteTarget["username"];
            string target_password = jsonSiteTarget["password"];
            int target_port = jsonSiteTarget["port"];
            string target_rootFolder = jsonSchedule["staticDirTarget"];
            
            // run automate
            RunAutomate(scheduleName, output_folder, source_host, source_username, source_password, source_port, source_rootFolder, siteTarget, target_host, target_username, target_password, target_port, target_rootFolder);
        }

        private static string get_latest_item_url(string schaduleName)
        {

            // get latest session
            var jsonSessions = Newtonsoft.Json.JsonConvert.DeserializeObject<dynamic>(Encoding.ASCII.GetString(HttpResponse.GetResponse("get", "http://10.224.143.44:8290/wtransfer/workspaces/default/schedules/" + schaduleName + "/sessions", null, null).Content));
            int jsonSessionsCount = jsonSessions["list"].Count;
            ulong id_max = 0;
            int imax = 0;
            for (int i = 0; i < jsonSessionsCount; i++)
            {
                var s = jsonSessions["list"][i];
                ulong id = s["id"];
                if (id > id_max)
                {
                    id_max = id;
                    imax = i;
                }
            }
            var session = jsonSessions["list"][imax];
            string sessionName = session["id"];

            // get latest items
            var jsonItems = Newtonsoft.Json.JsonConvert.DeserializeObject<dynamic>(Encoding.ASCII.GetString(HttpResponse.GetResponse("get", "http://10.224.143.44:8290/wtransfer/workspaces/default/sessions/" + sessionName + "/items", null, null).Content))["list"];
            int jsonItemsCount = jsonItems.Count;


            DateTime d = new DateTime(1970, 1, 1);
            imax = 0;
            for (int i = 0; i < jsonItemsCount; i++)
            {
                var it = jsonItems[i];
                DateTime created = it["created"];
                if (created > d)
                {
                    d = created;
                    imax = i;
                }
            }
            var item = jsonItems[imax];
            string itemName = item["name"];
            return "http://localhost:3000/sessions/" + sessionName + "/items/" + itemName;
        }

        public static void NavigateToSource(string schedule)
        {
            var jsonSchedule = Newtonsoft.Json.JsonConvert.DeserializeObject<dynamic>(Encoding.ASCII.GetString(HttpResponse.GetResponse("get", "http://10.224.143.44:8290/wtransfer/workspaces/default/schedules/" + schedule, null, null).Content));
            string siteTarget = jsonSchedule["siteSource"];
            string target_rootFolder = jsonSchedule["staticDirSource"];
            NavigateToTargetSite(siteTarget, target_rootFolder);
        }

        public static void NavigateToTarget(string schedule)
        {
            var jsonSchedule = Newtonsoft.Json.JsonConvert.DeserializeObject<dynamic>(Encoding.ASCII.GetString(HttpResponse.GetResponse("get", "http://10.224.143.44:8290/wtransfer/workspaces/default/schedules/" + schedule, null, null).Content));
            string siteTarget = jsonSchedule["siteTarget"];
            string target_rootFolder = jsonSchedule["staticDirTarget"];
            NavigateToTargetSite(siteTarget, target_rootFolder);
        }

        public static void NavigateToTargetSite(string site, string folder)
        {
            var jsonSite = Newtonsoft.Json.JsonConvert.DeserializeObject<dynamic>(Encoding.ASCII.GetString(HttpResponse.GetResponse("get", "http://10.224.143.44:8290/wtransfer/workspaces/default/sites/" + site, null, null).Content));

            // site login
            string host = jsonSite["host"];
            string username = jsonSite["username"];
            string password = jsonSite["password"];
            int port = jsonSite["port"];

            // open in filezilla
            OpenFileZillaThenNavigateFolder(host, username, password, port, folder);
        }

        private static Dictionary<string, dynamic> _dictSchedules = null;
        private static Dictionary<string, dynamic> _dictSites = null;

        private static void AutoInfo(string schedule_name, string ricefw, int index, bool force = false)
        {
            if (_dictSchedules == null)
            {
                _dictSchedules = new Dictionary<string, dynamic>();
                {
                    var schedules = Newtonsoft.Json.JsonConvert.DeserializeObject<dynamic>(Encoding.ASCII.GetString(HttpResponse.GetResponse("get", "http://10.224.143.44:8290/wtransfer/workspaces/default/schedules", null, null).Content))["list"];
                    foreach (var schedule in schedules)
                    {
                        string name = schedule["name"];
                        _dictSchedules.Add(name, schedule);
                    }
                }
            }

            if (_dictSites == null)
            {
                _dictSites = new Dictionary<string, dynamic>();
                {
                    var sites = Newtonsoft.Json.JsonConvert.DeserializeObject<dynamic>(Encoding.ASCII.GetString(HttpResponse.GetResponse("get", "http://10.224.143.44:8290/wtransfer/workspaces/default/sites", null, null).Content))["list"];
                    foreach (var site in sites)
                    {
                        string name = site["name"];
                        _dictSites.Add(name, site);
                    }
                }
            }

            {
                // ==================================================================================================== //
                //string schedule_name = info["schedule_name"];
                var ricefwCaseIndexStr = "" + (index + 1); while (ricefwCaseIndexStr.Length < 4) { ricefwCaseIndexStr = "0" + ricefwCaseIndexStr; }



                var schedule = _dictSchedules[schedule_name];

                string siteNameSource = schedule["siteSource"];
                string siteNameTarget = schedule["siteTarget"];

                var siteSource = _dictSites[siteNameSource];
                var siteTarget = _dictSites[siteNameTarget];

                var bcto = schedule_name.Split('-')[2].Replace("BC_", "");
                string targetIp = siteTarget["host"];

                var str_dest = bcto + " (" + targetIp + ")";

                var str_from = "dcloud-sftp (" + siteSource["host"] + ")";

                var description = "File transfer from " + str_from + " to " + str_dest;

                string sourceFolder = schedule["staticDirSource"];
                string targetFolder = schedule["staticDirTarget"];

                var t = ricefw + "\t" + ricefwCaseIndexStr + "\t" + schedule_name + "\t" + description + "\t" + sourceFolder + "\t" + targetFolder;
                //l2.Add(t);
                Console.WriteLine(t);

                // ---------------------------------------------------------------------------------------------------------------------------------------------------------------- //
                {
                    var success = false;
                    Exception e = null;
                    for (int r = 0; r < 5; r++)
                    {
                        try
                        {
                            GenerateCaptureTestResult(schedule_name, ricefw, ricefwCaseIndexStr, force);
                            success = true;
                            break;
                        }
                        catch (Exception ex)
                        {
                            e = ex;
                        }
                    }
                    if (!success)
                    {
                        File.WriteAllText(DateTime.Now.ToString("yyyy-MM-dd-HH-mm-ss.txt"), "Error: [" + ricefw + ", " + schedule_name + ", " + schedule_name + "], " + e.Message);
                    }
                }
                // ---------------------------------------------------------------------------------------------------------------------------------------------------------------- //
                // ==================================================================================================== //
            }
        }

        internal static void Start()
        {

            // ---------------------------------------------------------------- // display 2023-06-21 begin



            //AutoInfo("BW_FileTransfer_CS-BS_ERP_BWP100-BC_OR_LPGDashboard-64", "ZPIBWI006", 0, true);
            //AutoInfo("BW_FileTransfer_CS-BS_ERP_BWP100-BC_OR_SmartAnalytic-65", "ZPIBWI017", 0, true);
            //AutoInfo("PCard_BBLCycleTime_CS-BC_BBL-BS_ERP_ECP100-168", "ZPIFII029", 0, true);
            //AutoInfo("CM_FileTransfer_CS-BC_OR_SCB-File_Receiver_CMFileTransfer_FTPSP01_CC-1981", "ZPIFII060", 0, true);
            //AutoInfo("CM_OtherReturns_CS-BC_OR_SCB-File_Receiver_OR_CMOtherReturns_CC-490", "ZPIFII061", 0, true);
            //AutoInfo("CM_PaymentResult_CS-BS_ERP_ECP100-File_Receiver_OR_CMOutbPaymentResultSCB_CC-1215", "ZPIFII065", 0, true);
            //AutoInfo("CM_CrossBorderReportCB_CS-BC_OR_SCB-File_Receiver_OR_CMCrossBorderReportCB_CC-1760", "ZPIFII072", 0, true);
            //AutoInfo("MRWeb_MRInfoUpdateSend_CS-BC_OR_MRWeb-BS_ERP_ECP100-144", "ZPIFII110", 0, true);
            //AutoInfo("PTTOR_ETS_UniversalDocPost_CS-BC_OR_ETS-BS_ERP_ECP100-215", "ZPIFII182", 0, true);
            //AutoInfo("CM_EWHT_CS-BC_OR_SCB-File_Receiver_OR_CMEWHT_CC-1476", "ZPIFII183", 0, true);
            //AutoInfo("PTTOR_APIM_ORETAXReconciliationAndSubmissionSend_CS-BC_OR_ETAX-BS_ERP_ECP100-111", "ZPIFII187", 0, true);
            //AutoInfo("PTTOR_ETAXInvoiceFlagUpdateSend_CS-BC_OR_ETAX-BS_ERP_ECP100-112", "ZPIFII189", 0, true);
            //AutoInfo("ECC_FileTransfer_CS-BC_OR_EV-BS_ERP_ECP100-115", "ZPIFII191", 0, true);
            //AutoInfo("ECC_FileTransfer_CS-BS_ERP_ECP100-BC_OR_KALA-22", "ZPIFII208", 0, true);
            //AutoInfo("ROSE_TAS_OILLDDPost_CS-BC_TASH504-BS_ERP_ECP100-217", "ZPIISI112", 0, true);
            //AutoInfo("ECC_FileTransfer_CS-BS_ERP_ECP100-BC_OR_RetailWorkTracking-24", "ZPIMMI156", 0, true);
            //AutoInfo("ECC_FileTransfer_CS-BS_ERP_ECP100-BC_OR_WMSAMZDC-75", "ZPISDI147", 0, true);
            //AutoInfo("ECC_FileTransfer_CS-BS_ERP_ECP100-BC_OR_AmazonWS-20", "ZPISDI183", 0, true);
            //AutoInfo("ECC_FileTransfer_CS-BS_ERP_ECP100-BC_OR_EV-21", "ZPISDI183", 1, true);
            //AutoInfo("ECC_FileTransfer_CS-BS_ERP_ECP100-BC_OR_NewMarine-23", "ZSDSDI034", 1, true);

            //AutoInfo("BW_FileTransfer_CS-BS_ERP_BWP100-BC_PIMS-61", "ZPIPII039", 1, true);// ---> ??????????????????????????????

            //AutoInfo("H2O_BankPayment_KTB_CycleTime_CS-BS_ERP_ECP100-BC_OR_FTP-REF613", "ZPIFII008", 1, true);// -> need to retest

            //AutoInfo("EOrder_DAClearingInvoice_CS-BC_EORDER-File_Receiver_EOrder_DAClearingInvoice_CC-REF130", "ZPIFII017", 0, true);

            //AutoInfo("PTTOR_NewMarine_DealerDocAttachedfileSend_CS-BC_OR_NewMarine-File_Receiver_PTTOR_NewMarineAttachment_CC-REF261", "ZPISDI178", 0, true);

            // ---------------------------------------------------------------- 2023-07-13 begin

            //AutoInfo("H2O_AutoReconcile_KTB_CS-BC_OR_SFTP-File_Receiver_OR_AutoReconcile_KTBMonthly_CC-REF766", "ZPIFII017", 7, false);
            //AutoInfo("H2O_AutoReconcile_UOB_CS-BC_OR_UOB-SFTP_Receiver_OR_AutoReconcile_UOBMonthly_CC-REF974", "ZPIFII017", 9, false);


            AutoInfo("ECC_FileTransfer_CS-BS_ERP_ECP100-BC_OR_EV-21", "ZPISDI150", 0, true);
            AutoInfo("ECC_FileTransfer_CS-BC_OR_EV-BS_ERP_ECP100-176-REF1849", "ZPISDI150", 1, true);



            // ---------------------------------------------------------------- 2023-07-13 end





            /*// outbound
            AutoInfo("ECC_FileTransfer_CS-BS_ERP_ECP100-BC_OR_EV-21", "ZPISDI136", 0);

            // inbound
            */

            if ("".Length == 0)
            {
                return;
            }

            // ---------------------------------------------------------------- // display 2023-06-21 end












            /*var dictSites = new Dictionary<string, dynamic>();
            var dictSchedules = new Dictionary<string, dynamic>();
            {
                var sites = Newtonsoft.Json.JsonConvert.DeserializeObject<dynamic>(Encoding.ASCII.GetString(HttpResponse.GetResponse("get", "http://10.224.143.44:8290/wtransfer/workspaces/default/sites", null, null).Content))["list"];
                var schedules = Newtonsoft.Json.JsonConvert.DeserializeObject<dynamic>(Encoding.ASCII.GetString(HttpResponse.GetResponse("get", "http://10.224.143.44:8290/wtransfer/workspaces/default/schedules", null, null).Content))["list"];
                foreach (var site in sites)
                {
                    string name = site["name"];
                    dictSites.Add(name, site);
                }

                foreach (var schedule in schedules)
                {
                    string name = schedule["name"];
                    dictSchedules.Add(name, schedule);
                }
            }*/

            /*// inbound some TAS only
            {
                var read = File.ReadAllText(@"C:\Users\parin\eclipse-workspace\demo-excel-import\arra_in.json");
                var list = Newtonsoft.Json.JsonConvert.DeserializeObject<dynamic>(read);
                int count = list.Count;
                for (int i = 0; i < count; i++)
                {
                    var item = list[i];
                    string schedule_name = item["schedule_name"];
                    string ricefwCaseIndex = item["ricefwCaseIndex"];
                    string ricefw = ricefwCaseIndex.Split('-')[3];
                    int ricefwSubIndex = int.Parse(ricefwCaseIndex.Split('-')[4]) - 1;

                    AutoInfo(schedule_name, ricefw, ricefwSubIndex);
                }
            }*/




            //var ricefw = "ZPIBWI006";
            //var schaduleName = "BW_FileTransfer_CS-BS_ERP_BWP100-BC_OR_LPGDashboard-64";
            //var ricefwCaseIndex = "0001";

            //var ricefw = "ZPISDI150";
            //var schaduleName = "ECC_FileTransfer_CS-BS_ERP_ECP100-BC_OR_EV-21";
            //var ricefwCaseIndex = "0001";

            //var ricefw = "ZPIFII208";
            //var schaduleName = "ECC_FileTransfer_CS-BS_ERP_ECP100-BC_OR_KALA-22";
            //var ricefwCaseIndex = "0001";

            //GenerateCaptureTestResult(schaduleName, ricefw, ricefwCaseIndex);

            //NavigateToSource("H2O_AutoReconcile_KTB_CS-BS_ERP_ECP100-BC_OR_FTPS-5"); if ("".Length == 0) { return; }

            //GenerateCaptureTestResult("ECC_FileTransfer_CS-BS_ERP_ECP100-BC_OR_AmazonWS-20", "ZPISDI183", "0001", true); if ("".Length == 0) { return; }

            /*{
                var read = File.ReadAllText(@"C:\Users\parin\eclipse-workspace\demo-excel-import\arra.json");
                var list = Newtonsoft.Json.JsonConvert.DeserializeObject<dynamic>(read)["list"];
                int count = list.Count;
                var l2 = new List<string>();
                for (int i = 0; i < count; i++)
                {
                    var item = list[i];
                    bool allReachable = item["allReachable"];
                    int infoMissing = item["infoMissing"];
                    string ricefw = item["ricefw"];
                    if (allReachable)
                    {

                    }
                    else
                    {
                        //continue;
                    }

                    var listlist = item["list"];
                    int countcount = listlist.Count;
                    for (int index = 0; index < countcount; index++)
                    {
                        var x = listlist[index];

                        bool vpnReachBoth = x["vpnReachBoth"];
                        if (!vpnReachBoth)
                        {
                            continue;
                        }

                        var info = x["info"];
                        if (info == null)
                        {
                            continue;
                        }

                        AutoInfo(info, ricefw, index, dictSchedules, dictSites, ref l2);
                    }
                }
                File.WriteAllLines("l2.txt", l2);
            }*/


        }
    }
}