using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace WTRANSFER
{
    internal class Config
    {
        private static dynamic? _jsonConfig = null;
        public static dynamic Get(string key)
        {
            if (_jsonConfig == null)
            {
                _jsonConfig = Newtonsoft.Json.JsonConvert.DeserializeObject<dynamic>(File.ReadAllText("config.json"));
            }
            return _jsonConfig[key];
        }
    }
}