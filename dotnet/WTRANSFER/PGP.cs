using Org.BouncyCastle.Bcpg.OpenPgp;
using Org.BouncyCastle.Bcpg;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Org.BouncyCastle.Utilities.IO;
using Org.BouncyCastle.Utilities.Zlib;

namespace WTRANSFER
{
    internal class PGP
    {
        private static Stream PgpEncrypt(Stream sourceStream, string pgpPublicKeyPath)
        {
            using var publicKeyStream = File.OpenRead(pgpPublicKeyPath);
            var publicKeyRingBundle = new PgpPublicKeyRingBundle(PgpUtilities.GetDecoderStream(publicKeyStream));
            var publicKeyRing = publicKeyRingBundle.GetKeyRings().OfType<PgpPublicKeyRing>().FirstOrDefault();
            var publicKey = publicKeyRing.GetPublicKeys().OfType<PgpPublicKey>().FirstOrDefault(key => key.IsEncryptionKey);

            var dataGenerator = new PgpEncryptedDataGenerator(SymmetricKeyAlgorithmTag.TripleDes, true);
            dataGenerator.AddMethod(publicKey);

            var encryptedStream = new MemoryStream();
            var armorGenerator = new ArmoredOutputStream(encryptedStream);
            var outputStream = dataGenerator.Open(armorGenerator, new byte[1 << 16]);
            var compressionStream = new PgpCompressedDataGenerator(CompressionAlgorithmTag.Zip).Open(outputStream);
            var integrityStream = new PgpLiteralDataGenerator().Open(compressionStream, PgpLiteralData.Binary, "", DateTime.UtcNow, new byte[1 << 16]);

            sourceStream.CopyTo(integrityStream);

            integrityStream.Close();
            compressionStream.Close();
            outputStream.Close();
            armorGenerator.Close();

            encryptedStream.Position = 0;
            return encryptedStream;
        }

        private static PgpPrivateKey GetPrivateKey(string privateKeyPath, string password)
        {
            using (Stream keyIn = File.OpenRead(privateKeyPath))
            using (Stream inputStream = PgpUtilities.GetDecoderStream(keyIn))
            {
                PgpSecretKeyRingBundle secretKeyRingBundle = new PgpSecretKeyRingBundle(inputStream);

                foreach (PgpSecretKeyRing kRing in secretKeyRingBundle.GetKeyRings())
                {
                    foreach (PgpSecretKey secretKey in kRing.GetSecretKeys())
                    {
                        PgpPrivateKey privKey = secretKey.ExtractPrivateKey(password.ToCharArray());

                        return privKey;
                    }

                }
            }

            throw new Exception("PGP private key not found.");
        }

        private static void CreateLocalDecryptedFile(Stream inputStream, string outputFile, string privateKeyLoc, string password)
        {
            using var newStream = PgpUtilities.GetDecoderStream(inputStream);
            PgpObjectFactory pgpObjF = new(newStream);
            PgpEncryptedDataList enc;
            PgpObject obj = pgpObjF.NextPgpObject();
            if (obj is PgpEncryptedDataList)
            {
                enc = (PgpEncryptedDataList)obj;
            }
            else
            {
                enc = (PgpEncryptedDataList)pgpObjF.NextPgpObject();
            }

            PgpPrivateKey privKey = GetPrivateKey(privateKeyLoc, password);

            PgpPublicKeyEncryptedData pbe = enc.GetEncryptedDataObjects().Cast<PgpPublicKeyEncryptedData>().First();

            using Stream clear = pbe.GetDataStream(privKey);
            PgpObjectFactory plainFact = new PgpObjectFactory(clear);
            PgpObject message = plainFact.NextPgpObject();

            if (message is PgpCompressedData)
            {
                PgpCompressedData cData = (PgpCompressedData)message;
                Stream compDataIn = cData.GetDataStream();
                PgpObjectFactory o = new PgpObjectFactory(compDataIn);
                message = o.NextPgpObject();
                if (message is PgpOnePassSignatureList)
                {
                    message = o.NextPgpObject();
                }
                PgpLiteralData Ld = null;
                Ld = (PgpLiteralData)message;
                using Stream output = File.Create(outputFile);
                Stream unc = Ld.GetInputStream();
                Streams.PipeAll(unc, output);
            }
        }

        public static Stream Encrypt(Stream stream, string publicKeyFile)
        {
            if (string.IsNullOrEmpty(publicKeyFile))
            {
                throw new ArgumentException("PGP public key is required for encryption.");
            }

            var path = publicKeyFile;
            while (path.StartsWith("\\"))
            {
                path = path.Substring(1, path.Length - 1);
            }
            while (path.StartsWith("/"))
            {
                path = path.Substring(1, path.Length - 1);
            }

            // public key file
            if (!File.Exists(path))
            {
                throw new FileNotFoundException("Could not find PGP public key. The file \"" + publicKeyFile + "\" does not exist.");
            }
            return PgpEncrypt(stream, path);
        }

        public static Stream Decrypt(Stream streamSource, string pgpPrivateKeyPath, string pgpPassword, out string decryptedFileName)
        {
            if (string.IsNullOrEmpty(pgpPrivateKeyPath) || string.IsNullOrEmpty(pgpPassword))
            {
                throw new ArgumentException("PGP private key and password are required for decryption.");
            }

            var path = pgpPrivateKeyPath;
            while (path.StartsWith("\\"))
            {
                path = path.Substring(1, path.Length - 1);
            }
            while (path.StartsWith("/"))
            {
                path = path.Substring(1, path.Length - 1);
            }

            // private key file
            if (!File.Exists(path))
            {
                throw new FileNotFoundException("Could not find PGP private key. The file \"" + pgpPrivateKeyPath + "\" does not exist.");
            }

            // download the encrypted remote file to local
            var localEncryptedFile = "en-" + Guid.NewGuid().ToString() + ".deleteme";
            using (var localFileStream = File.Create(localEncryptedFile))
            {
                streamSource.CopyTo(localFileStream);
            }

            //localEncryptedFile_is_not_deleted_when_exception_occur_below();

            // create local decrypted file
            Exception ex_CreateLocalDecryptedFile = null;
            decryptedFileName = "de-" + Guid.NewGuid().ToString() + ".deleteme";
            try
            {
                using (var localEncryptedStream = File.OpenRead(localEncryptedFile))
                {
                    CreateLocalDecryptedFile(localEncryptedStream, decryptedFileName, path, pgpPassword);
                }
            }
            catch (Exception ex)
            {
                ex_CreateLocalDecryptedFile = ex;
            }

            // delete encrypted local file
            try { File.Delete(localEncryptedFile); } catch { }

            if (ex_CreateLocalDecryptedFile != null)
            {
                throw ex_CreateLocalDecryptedFile;
            }

            // use the decrypted stream
            return File.OpenRead(decryptedFileName);
        }
    }
}