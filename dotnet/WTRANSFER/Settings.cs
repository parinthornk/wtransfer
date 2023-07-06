using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace WTRANSFER
{
    internal class Settings
    {
        public static int Port { get; } = 62580;
        public static string BasePath { get; } = "/custom-file-server";
        public static string DateFormat { get; } = "yyyy-MM-dd HH:mm:ss";
    }
}
