using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Text;
using System.Threading.Tasks;

namespace WTRANSFER
{
    internal class ProcessRequest
    {
        public string Content { get; set; } = "";
        public int StatusCode { get; set; } = 200;
        public static bool Terminate { get; private set; } = false;

        private static string GetHttpRequestBody(HttpListenerRequest request)
        {

            var body = string.Empty;
            StreamReader? sr = null;
            try
            {
                sr = new StreamReader(request.InputStream);
                body = sr.ReadToEnd();
            }
            catch (Exception ex)
            {
                Console.WriteLine("Error parsing body: " + ex.Message);
            }
            if (sr != null)
            {
                try
                {
                    sr.Close();
                }
                catch
                {

                }
            }
            return body;
        }

        public static ProcessRequest Process(HttpListenerRequest request)
        {
            // http request body
            var body = GetHttpRequestBody(request);

            // end program
            if (request.HttpMethod.ToLower() == "post" && request.RawUrl == Settings.BasePath + "/terminate")
            {
                Terminate = true;
                return new ProcessRequest() { StatusCode = 202, Content = "{\"message\": \"Terminated.\"}" };
            }

            // site operations
            if (request.HttpMethod.ToLower() == "post" && request.RawUrl == Settings.BasePath + "/site-operations")
            {
                return SiteOperations.Execute(body);
            }

            return new ProcessRequest() { StatusCode = 501, Content = "{\"message\": \"Not implemented.\"}" };
        }
    }
}
