using Org.BouncyCastle.Bcpg;
using Org.BouncyCastle.Bcpg.OpenPgp;
using Org.BouncyCastle.Crypto.IO;
using Org.BouncyCastle.Utilities.IO;
using Org.BouncyCastle.Utilities.Zlib;
using Renci.SshNet;
using Renci.SshNet.Messages;
using Renci.SshNet.Security;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;
using System.Net.Sockets;
using System.Text;
using System.Threading.Tasks;
using static System.Runtime.InteropServices.JavaScript.JSType;

namespace WTRANSFER
{
    internal class TestWithPGP
    {

        

        /*private static Stream PgpDecrypt(Stream sourceStream, string pgpPrivateKeyPath, string privateKeyPassphrase)
        {
            using var publicKeyStream = File.OpenRead(pgpPrivateKeyPath);
            var publicKeyRingBundle = new PgpPublicKeyRingBundle(PgpUtilities.GetDecoderStream(publicKeyStream));


            var publicKeyRing = publicKeyRingBundle.GetKeyRings().OfType<PgpSecretKeyRing>().FirstOrDefault();


            var decryptionKey = publicKeyRing.GetSecretKeys().OfType<PgpSecretKey>().FirstOrDefault(key => key.IsSigningKey && key.ExtractPrivateKey(privateKeyPassphrase.ToCharArray()) != null);
            var privateKey = decryptionKey.ExtractPrivateKey(privateKeyPassphrase.ToCharArray());



            var decryptor = new PgpPrivateKeyDecryptor(privateKey);
            var decryptedData = encryptedData.DecryptData(decryptor);
            var decryptedObjectFactory = new PgpObjectFactory(decryptedData);


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
        }*/


        private static PgpPrivateKey FindSecretKey(PgpSecretKeyRingBundle pgpSec, long keyId, char[] pass)
        {
            PgpSecretKey pgpSecKey = pgpSec.GetSecretKey(keyId);
            
            return pgpSecKey.ExtractPrivateKey(pass);
        }

        public static void Decrypt(Stream inputStream, Stream privateKeyStream, string passPhrase, string outputFile)
        {

            PgpObjectFactory pgpF = null;
            PgpEncryptedDataList enc = null;
            PgpObject o = null;
            PgpPrivateKey sKey = null;
            PgpPublicKeyEncryptedData pbe = null;
            PgpSecretKeyRingBundle pgpSec = null;

            pgpF = new PgpObjectFactory(PgpUtilities.GetDecoderStream(inputStream));
            // find secret key
            pgpSec = new PgpSecretKeyRingBundle(PgpUtilities.GetDecoderStream(privateKeyStream));

            if (pgpF != null)
            {
                o = pgpF.NextPgpObject();
            }


            // the first object might be a PGP marker packet.
            if (o is PgpEncryptedDataList)
            {
                enc = (PgpEncryptedDataList)o;
            }
            else
            {
                enc = (PgpEncryptedDataList)pgpF.NextPgpObject();
            }

            // decrypt
            foreach (PgpPublicKeyEncryptedData pked in enc.GetEncryptedDataObjects())
            {
                sKey = FindSecretKey(pgpSec, pked.KeyId, passPhrase.ToCharArray());

                if (sKey != null)
                {
                    pbe = pked;
                    break;
                }
            }


            PgpObjectFactory plainFact = null;

            using (Stream clear = pbe.GetDataStream(sKey))
            {
                plainFact = new PgpObjectFactory(clear);
            }

            PgpObject message = plainFact.NextPgpObject();

            if (message is PgpCompressedData)
            {
                PgpCompressedData cData = (PgpCompressedData)message;
                PgpObjectFactory of = null;

                using (Stream compDataIn = cData.GetDataStream())
                {
                    of = new PgpObjectFactory(compDataIn);
                }

                message = of.NextPgpObject();
                if (message is PgpOnePassSignatureList)
                {
                    message = of.NextPgpObject();
                    PgpLiteralData Ld = null;
                    Ld = (PgpLiteralData)message;
                    using (Stream output = File.Create(outputFile))
                    {
                        Stream unc = Ld.GetInputStream();
                        Streams.PipeAll(unc, output);
                    }
                }
                else
                {
                    PgpLiteralData Ld = null;
                    Ld = (PgpLiteralData)message;
                    using (Stream output = File.Create(outputFile))
                    {
                        Stream unc = Ld.GetInputStream();
                        Streams.PipeAll(unc, output);
                    }
                }
            }
            else if (message is PgpLiteralData)
            {
                PgpLiteralData ld = (PgpLiteralData)message;
                string outFileName = ld.FileName;

                using (Stream fOut = File.Create(outputFile))
                {
                    Stream unc = ld.GetInputStream();
                    Streams.PipeAll(unc, fOut);
                }
            }
        }

        public static PgpPrivateKey GetPrivateKeyFromFile(string privateKeyFilePath, string passphrase)
        {
            // Load the private key file
            using (Stream privateKeyStream = File.OpenRead(privateKeyFilePath))
            {
                // Create a stream for reading the private key with the provided passphrase
                PgpSecretKeyRingBundle secretKeyRingBundle = new PgpSecretKeyRingBundle(PgpUtilities.GetDecoderStream(privateKeyStream));

                // Iterate over the secret key rings and find the desired secret key
                foreach (PgpSecretKeyRing secretKeyRing in secretKeyRingBundle.GetKeyRings())
                {
                    foreach (PgpSecretKey secretKey in secretKeyRing.GetSecretKeys())
                    {
                        // Extract the private key using the provided passphrase
                        PgpPrivateKey privateKey = secretKey.ExtractPrivateKey(passphrase.ToCharArray());
                        return privateKey;
                    }
                }
            }

            return null; // Private key not found
        }


        public static Stream DecryptStream(ArmoredInputStream encryptedStream, PgpPublicKeyRing publicKeyRing, PgpPrivateKey privateKey)
        {
            //encryptedStream.Reset();

            //PgpObjectFactory pgpFactory = new(encryptedStream, new ArmoredInputStreamCallback());

            var pgpFactory = new PgpObjectFactory(PgpUtilities.GetDecoderStream(encryptedStream));

            PgpEncryptedDataList encryptedDataList = (PgpEncryptedDataList)pgpFactory.NextPgpObject();

            PgpPublicKeyEncryptedData encryptedData = encryptedDataList.GetEncryptedDataObjects().Cast<PgpPublicKeyEncryptedData>().FirstOrDefault(e => publicKeyRing.GetPublicKey(e.KeyId) != null);

            if (encryptedData == null)
            {
                throw new Exception("No matching public key found.");
            }

            

            Stream decryptedDataStream = encryptedData.GetDataStream(privateKey);

            PgpObjectFactory decryptedFactory = new PgpObjectFactory(decryptedDataStream);
            PgpCompressedData compressedData = (PgpCompressedData)decryptedFactory.NextPgpObject();
            Stream compressedDataStream = compressedData.GetDataStream();

            PgpObjectFactory compressedFactory = new PgpObjectFactory(compressedDataStream);
            PgpLiteralData literalData = (PgpLiteralData)compressedFactory.NextPgpObject();

            Stream literalDataStream = literalData.GetInputStream();
            return literalDataStream;
        }


        /*public Stream Decrypt(Stream encryptedStream)
        {
            var encryptedData = PgpUtilities.GetDecoderStream(encryptedStream);
            var encryptedMessage = new PgpObjectFactory(encryptedData).NextPgpObject() as PgpEncryptedDataList;

            var decryptor = new PgpPublicKeyEncryptedDataDecryptorFactory(publicKeyRing).CreatePrivateKeyDecryptor(privateKey);

            var decryptedDataStream = encryptedMessage.GetEncryptedDataObjects().Cast<PgpPublicKeyEncryptedData>()
                .Select(decryptor.DecryptSessionKey)
                .OfType<PgpCompressedData>()
                .Select(data => data.GetDataStream())
                .OfType<PgpLiteralData>()
                .FirstOrDefault()?.GetInputStream();

            return decryptedDataStream;
        }*/



        private static PgpPrivateKey GetPrivateKey(PgpSecretKeyRingBundle secretKeyRingBundle, long keyId, string password)
        {
            PgpSecretKeyRing secretKeyRing = secretKeyRingBundle.GetSecretKeyRing(keyId);
            if (secretKeyRing != null)
            {
                PgpSecretKey secretKey = secretKeyRing.GetSecretKey();
                PgpPrivateKey privateKey = secretKey.ExtractPrivateKey(password.ToCharArray());
                if (privateKey != null)
                {
                    return privateKey;
                }
            }
            return null;
        }

        public static void CreateDecodedStream(Stream encryptedStream, string privateKeyFilePath, string privateKeyPassword, ref MemoryStream ms)
        {
            using (Stream privateKeyStream = File.OpenRead(privateKeyFilePath))
            {
                PgpSecretKeyRingBundle secretKeyRingBundle = new PgpSecretKeyRingBundle(PgpUtilities.GetDecoderStream(privateKeyStream));

                PgpObjectFactory pgpFactory = new PgpObjectFactory(PgpUtilities.GetDecoderStream(encryptedStream));
                PgpEncryptedDataList encryptedDataList;

                // Look for the encrypted data packet
                PgpObject pgpObject = pgpFactory.NextPgpObject();
                if (pgpObject is PgpEncryptedDataList)
                {
                    encryptedDataList = (PgpEncryptedDataList)pgpObject;
                }
                else
                {
                    encryptedDataList = (PgpEncryptedDataList)pgpFactory.NextPgpObject();
                }

                // Find the corresponding secret key
                PgpPrivateKey privateKey = null;
                PgpPublicKeyEncryptedData encryptedData = null;
                foreach (PgpPublicKeyEncryptedData data in encryptedDataList.GetEncryptedDataObjects())
                {
                    privateKey = GetPrivateKey(secretKeyRingBundle, data.KeyId, privateKeyPassword);
                    if (privateKey != null)
                    {
                        encryptedData = data;
                        break;
                    }
                }

                if (privateKey == null)
                {
                    throw new PgpException("Private key for decryption not found.");
                }

                Stream decodedStream = encryptedData.GetDataStream(privateKey);

                ms = new MemoryStream();
                decodedStream.CopyTo(ms);
                ms.Position = 0;

                //return PgpUtilities.GetDecoderStream(ms);
            }
        }

        internal static void Test()
        {
            // sftp source -> dcloud
            var siteSourceName = "dcloud-sftp";
            var sourceFile = "/test-zparinthornk/20230510/archive/encrypted/encrypted-ปรินทร.xlsx.pgp";

            // sftp target -> ev
            var siteTargetName = "legacy-0f1b722f-3675-4348-959e-37c9177cf7ac";
            var targetFile = "/EVChargingStation/DEVELOPMENT/STATUS_BILLING/INPUT/WSO2_TEST/decrypted-ปรินทร.xlsx";

            // pgp
            var pgpDirection = "decrypt";
            var pgpPublicKeyPath = "pgp-keys\\0xBD06F3AF-pub.asc";
            var pgpPrivateKeyPath = "pgp-keys\\0xBD06F3AF-sec.asc";
            var pgpPassword = "P@ssw0rd";

            // load site info
            var siteSourceInfo = SiteInfo.GetByName(siteSourceName);
            var siteTargetInfo = SiteInfo.GetByName(siteTargetName);

            // Connect to the source SFTP server
            using (var sourceClient = new SftpClient(siteSourceInfo.IP, siteSourceInfo.Username, siteSourceInfo.Password))
            {
                // connect to source
                sourceClient.Connect();

                // Connect to the destination SFTP server
                using (var destClient = new SftpClient(siteTargetInfo.IP, siteTargetInfo.Username, siteTargetInfo.Password))
                {
                    // connect to target
                    destClient.Connect();

                    // -----------------------------------------------------------------------> pgp encrypt
                    if (pgpDirection.ToLower() == "encrypt")
                    {
                        // Open the source file for reading
                        using var sourceStream = sourceClient.OpenRead(sourceFile);
                        // Create the destination file on the destination SFTP server
                        using var destStream = destClient.Create(targetFile);

                        // Encrypt the file using PGP
                        using var encryptedStream = PGP.Encrypt(sourceStream, pgpPublicKeyPath);

                        // Stream the encrypted file contents from the source to the destination
                        encryptedStream.CopyTo(destStream);
                    }

                    // -----------------------------------------------------------------------> pgp decrypt
                    else if (pgpDirection.ToLower() == "decrypt")
                    {
                        // Open the source file for reading
                        using var sourceStream = sourceClient.OpenRead(sourceFile);

                        // Create the destination file on the destination SFTP server
                        using var destStream = destClient.Create(targetFile);

                        using var localStream = PGP.Decrypt(sourceStream, pgpPrivateKeyPath, pgpPassword, out string fileName);

                        try { localStream.CopyTo(destStream); } catch { }

                        try { File.Delete(fileName); } catch { }
                    }

                    // -----------------------------------------------------------------------> pgp non
                    else
                    {
                        // Open the source file for reading
                        using var sourceStream = sourceClient.OpenRead(sourceFile);

                        // Create the destination file on the destination SFTP server
                        using var destStream = destClient.Create(targetFile);

                        // Stream the file contents from the source to the destination
                        sourceStream.CopyTo(destStream);
                    }

                    // -----------------------------------------------------------------------> end move file

                    destClient.Disconnect();
                }

                sourceClient.Disconnect();
            }

            Console.WriteLine("File transfer completed.");
        }
    }
}