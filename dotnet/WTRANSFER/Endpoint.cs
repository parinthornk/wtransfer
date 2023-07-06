using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Text;
using System.Threading.Tasks;

namespace WTRANSFER
{
    internal class Endpoint
    {
        public static void Start()
        {
            var listener = new HttpListener();
            listener.Prefixes.Add("http://127.0.0.1:" + Settings.Port + Settings.BasePath + "/");
            listener.Start();
            Console.WriteLine("Listening...");

            while (true)
            {
                var context = listener.GetContext();
                var request = context.Request;

                ProcessRequest? result;
                try
                {
                    result = ProcessRequest.Process(request);
                }
                catch (Exception ex)
                {
                    result = new ProcessRequest()
                    {
                        StatusCode = 500,
                        Content = Newtonsoft.Json.JsonConvert.SerializeObject(new Dictionary<string, object>
                        {
                            { "message", "" + ex }
                        }),
                    };
                }


                var response = context.Response;


                var buffer = Encoding.UTF8.GetBytes(result.Content);

                response.ContentLength64 = buffer.Length;
                response.StatusCode = result.StatusCode;
                response.Headers.Add("Content-Type", "application/json");
                response.Headers.Add("Content-Length", buffer.Length.ToString());
                var output = response.OutputStream;
                output.Write(buffer, 0, buffer.Length);

                output.Close();

                if (ProcessRequest.Terminate)
                {
                    break;
                }
            }

            listener.Close();
        }
    }
}