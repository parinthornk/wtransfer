using RestSharp;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace OreoTestingAutomata
{
    internal class HttpResponse
    {
        public byte[] Content { get; private set; }
        public int StatusCode { get; private set; }

        public static HttpResponse GetResponse(string method, string url, Dictionary<string, object> headers, byte[] bodyRaw)
        {
            HttpResponse httpResponse = null;
            RestClient client = null;
            Exception error = null;

            try
            {
                client = new RestClient(url);
                Method m;
                if (method.ToLower() == "get")
                {
                    m = Method.Get;
                }
                else if (method.ToLower() == "post")
                {
                    m = Method.Post;
                }
                else if (method.ToLower() == "put")
                {
                    m = Method.Put;
                }
                else if (method.ToLower() == "patch")
                {
                    m = Method.Patch;
                }
                else if (method.ToLower() == "delete")
                {
                    m = Method.Delete;
                }
                else
                {
                    throw new Exception("Method \"" + method + "\" is not supported.");
                }
                var request = new RestRequest() { Method = m };
                if (headers != null)
                {
                    foreach (var key in headers.Keys)
                    {
                        request.AddHeader(key, headers[key].ToString());
                    }
                }

                if (bodyRaw != null)
                {
                    if (bodyRaw.Length > 0)
                    {
                        if (m == Method.Get)
                        {
                            throw new Exception("Method \"GET\" must not have payload.");
                        }

                        //var chars = new char[bodyRaw.Length / sizeof(char)];
                        var chars = new char[bodyRaw.Length];
                        for (int i = 0; i < chars.Length; i++)
                        {
                            chars[i] = (char)bodyRaw[i];
                        }
                        //Buffer.BlockCopy(bodyRaw, 0, chars, 0, bodyRaw.Length);
                        request.AddStringBody(new string(chars), DataFormat.None);
                    }
                }

                var response = client.ExecuteAsync(request).Result;

                httpResponse = new HttpResponse()
                {
                    Content = response.RawBytes,
                    StatusCode = (int)response.StatusCode,
                };
            }
            catch (Exception ex)
            {
                error = ex;
            }

            if (client != null)
            {
                try
                {
                    client.Dispose();
                }
                catch
                {

                }
            }

            if (error != null)
            {
                throw error;
            }

            return httpResponse;
        }
    }
}
