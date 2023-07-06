using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace WTRANSFER
{
    internal class TestMoveFile
    {
        private static Random _rnd = new Random();
        private static object _rnd_lock = new object();
        private static int RandInt(int min, int max)
        {
            int x = 0;
            lock (_rnd_lock)
            {
                x = _rnd.Next(min, max);
            }
            return x;
        }

        public static System.Security.Authentication.SslProtocols SslProtocols { get; set; }

        public static void Test()
        {

            /*// generate large file
            var length = 1000 * 1024 * 1024;
            var bytes = new byte[length];
            for (int i = 0; i < bytes.Length; i++)
            {
                int m = RandInt(65, 91);
                byte bytem = (byte)m;
                bytes[i] = bytem;
            }
            File.WriteAllBytes("bytes-" + bytes.Length + ".txt", bytes);*/

            //File.WriteAllBytes("bytes-z.txt", new byte[] { 91 });



            /*// ftps H2O
            File.WriteAllText("zparin-01.txt", "1");
            var sourceServer = FileServer.GetByName("dcloud-sftp");
            var target = SiteInfo.GetByName("legacy-53ea73b0-cbac-4381-8427-64a53e36fc0f");
            sourceServer.Open();
            sourceServer.TransferExternal(target, "/OR-KALA_Test/Archive/ไฟล์ภาษาไทย-๓.xlsx.20230607003103.arc", "/Dev/usr/sap/interface/pttor/ebpp/outbound/ไฟล์ภาษาไทย-๓.xlsx");
            sourceServer.Close();
            File.WriteAllText("zparin-02.txt", "2");*/

            /*// ftp syspicommon
            File.WriteAllText("zparin-01.txt", "1");
            var sourceServer = FileServer.GetByName("dcloud-sftp");
            var target = SiteInfo.GetByName("legacy-ec339ce1-777d-4cac-a21e-4086ee3586a1");
            sourceServer.Open();
            sourceServer.TransferExternal(target, "/OR-KALA_Test/Archive/ไฟล์ภาษาไทย-๓.xlsx.20230607003103.arc", "/PTTPIMS_dev/inbound/ไฟล์ภาษาไทย-๓.xlsx");
            sourceServer.Close();
            File.WriteAllText("zparin-02.txt", "2");*/

            /*File.WriteAllText("zparin-01.txt", "1");
            var sourceServer = FileServer.GetByName("dcloud-sftp");
            sourceServer.Open();
            sourceServer.TransferExternal(SiteInfo.GetByName("legacy-7f06fb44-5f43-4947-90a7-555f61ff359b"), "/OR-KALA_Test/Archive/ไฟล์ภาษาไทย-๓.xlsx.20230607003103.arc", "/OR-KALA_Test/ไฟล์ภาษาไทย-๓2.xlsx");
            //sourceServer.TransferExternal(SiteInfo.GetByName("legacy-f4e9cd76-e7bd-4d3a-a5a6-acf1e061ea2a"), "/OR-KALA_Test/Archive/ไฟล์ภาษาไทย-๓.xlsx.20230607003103.arc", "/ไฟล์ภาษาไทย-๓.xlsx");
            sourceServer.Close();
            File.WriteAllText("zparin-02.txt", "2");*/

            /*// this works on linux (TLS12)
            File.WriteAllText("zparin-01.txt", "1");
            var sourceServer = FileServer.GetByName("dcloud-sftp");
            var target = SiteInfo.GetByName("legacy-f4e9cd76-e7bd-4d3a-a5a6-acf1e061ea2a");
            //target.Protocol = "ftps";
            sourceServer.Open();
            sourceServer.TransferExternal(target, "/OR-KALA_Test/Archive/ไฟล์ภาษาไทย-๓.xlsx.20230607003103.arc", "/ไฟล์ภาษาไทย-๓.xlsx");
            sourceServer.Close();
            File.WriteAllText("zparin-02.txt", "2");*/



            /*// , econwebqusr
            File.WriteAllText("zparin-01.txt", "1");
            var sourceServer = FileServer.GetByName("dcloud-sftp");
            var target = SiteInfo.GetByName("legacy-8305740c-af62-45ad-a87f-f3499241b144");
            sourceServer.Open();
            sourceServer.TransferExternal(target, "/OR-KALA_Test/Archive/ไฟล์ภาษาไทย-๓.xlsx.20230607003103.arc", "/BankStatement_test/pttor/bbl/outbound/report/ไฟล์ภาษาไทย-๓.xlsx");
            sourceServer.Close();
            File.WriteAllText("zparin-02.txt", "2");*/

            var listSsls = new List<System.Security.Authentication.SslProtocols>
            {
                System.Security.Authentication.SslProtocols.Tls13,
                System.Security.Authentication.SslProtocols.Tls12,
                System.Security.Authentication.SslProtocols.Tls11,
                System.Security.Authentication.SslProtocols.Tls,
                System.Security.Authentication.SslProtocols.Ssl3,
                System.Security.Authentication.SslProtocols.Ssl2,
                System.Security.Authentication.SslProtocols.Default,
                System.Security.Authentication.SslProtocols.None
            };

            Console.WriteLine("============================================================================================================================================");
            foreach (var v in listSsls)
            {
                SslProtocols = v;
                try
                {

                    // ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ //
                    File.WriteAllText("zparin-01.txt", "1");
                    var sourceServer = FileServer.GetByName("dcloud-sftp");
                    var target = SiteInfo.GetByName("legacy-8305740c-af62-45ad-a87f-f3499241b144");
                    sourceServer.Open();
                    sourceServer.TransferExternal(target, "/OR-KALA_Test/Archive/ไฟล์ภาษาไทย-๓.xlsx.20230607003103.arc", "/BankStatement_test/pttor/bbl/outbound/report/ไฟล์ภาษาไทย-๓.xlsx");
                    sourceServer.Close();
                    File.WriteAllText("zparin-02.txt", "2");
                    // ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ //

                    Console.WriteLine("[---- Success ----], (" + v.ToString() + ")");
                }
                catch (Exception ex)
                {
                    var g = ("[---- Error ------], (" + v.ToString() + "): " + ex).Replace("\n", ", ");
                    while (g.Contains("\r"))
                    {
                        g = g.Replace("\r", "");
                    }
                    while (g.Contains("\n"))
                    {
                        g = g.Replace("\n", ", ");
                    }

                    if (g.Contains("Code: 550 Message"))
                    {
                        Console.WriteLine("[---- Success ----], (" + v.ToString() + ")");
                    }
                    else
                    {
                        Console.WriteLine(g);
                    }

                    
                }
                Console.WriteLine("============================================================================================================================================");
            }
        }
    }
}//legacy-7f06fb44-5f43-4947-90a7-555f61ff359b