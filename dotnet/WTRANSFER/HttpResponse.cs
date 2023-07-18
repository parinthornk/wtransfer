using Renci.SshNet.Security;
using RestSharp;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace WTRANSFER
{
    internal class HttpResponse
    {
        public byte[] Content { get; private set; }
        public int StatusCode { get; private set; }

        public static HttpResponse GetResponse(string method, string url, Dictionary<string, object> headers, string text)
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
                else
                {
                    request.AddHeader("Content-Type", "application/json");
                }

                if (text != null)
                {
                    if (text.Length > 0)
                    {
                        if (m == Method.Get)
                        {
                            throw new Exception("Method \"GET\" must not have payload.");
                        }

                        request.AddStringBody(text, DataFormat.None);
                    }
                }

                var response = client.ExecuteAsync(request).Result;

                if (response.ResponseStatus != ResponseStatus.Completed)
                {
                    var err = string.Empty;
                    if (!string.IsNullOrEmpty(response.ErrorMessage))
                    {
                        err += ", [" + response.ErrorMessage + "], ";
                    }
                    if (!string.IsNullOrEmpty(response.Content))
                    {
                        err += ", [" + response.Content + "], ";
                    }

                    if (err.Length > 2)
                    {
                        err = err.Substring(0, err.Length - 2);
                    }

                    err = "Error accessing \"" + url + "\"" + err;
                    while (err.Contains("http://10.224.143.44:8290"))
                    {
                        err = err.Replace("http://10.224.143.44:8290", string.Empty);
                    }

                    throw new HttpRequestException(err);
                }

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
