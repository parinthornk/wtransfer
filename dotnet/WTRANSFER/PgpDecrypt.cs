using Org.BouncyCastle.Bcpg.OpenPgp;
using Org.BouncyCastle.Utilities.IO;
using Renci.SshNet.Sftp;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace WTRANSFER
{
    internal class PgpDecrypt
    {
        

        

        /*public static void Test()
        {
            var pgpPrivateKeyPath = "C:\\Users\\parin\\Documents\\filezilla\\pgp-keys\\0xBD06F3AF-sec.asc";
            var pgpPassword = "P@ssw0rd";
            DecryptFile2(File.OpenRead("C:\\Users\\parin\\Documents\\filezilla\\pgp-received\\moved-ปรินทร.xlsx.pgp"), "out-จ๓.xlsx", pgpPrivateKeyPath, pgpPassword);
        }

        internal static Stream CreateLocalStream(SftpFileStream sourceStream, string pgpPrivateKeyPath, string pgpPassword, out string fileName)
        {
            fileName = new Guid().ToString();
            DecryptFile2(File.OpenRead("C:\\Users\\parin\\Documents\\filezilla\\pgp-received\\moved-ปรินทร.xlsx.pgp"), fileName, pgpPrivateKeyPath, pgpPassword);
            return File.OpenRead(fileName);
        }*/
    }
}
