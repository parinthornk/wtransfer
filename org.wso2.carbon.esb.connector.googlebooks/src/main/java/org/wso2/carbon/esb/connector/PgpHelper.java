package org.wso2.carbon.esb.connector;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;
import java.util.Iterator;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;

public class PgpHelper {
   private static PgpHelper INSTANCE = null;

   public static PgpHelper getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new PgpHelper();
      }

      return INSTANCE;
   }

   private PgpHelper() {
   }

   public PGPPublicKey readPublicKey(InputStream in) throws IOException, PGPException {
      in = PGPUtil.getDecoderStream(in);
      PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(in);
      PGPPublicKey key = null;
      Iterator rIt = pgpPub.getKeyRings();

      while(key == null && rIt.hasNext()) {
         PGPPublicKeyRing kRing = (PGPPublicKeyRing)rIt.next();
         Iterator kIt = kRing.getPublicKeys();

         while(key == null && kIt.hasNext()) {
            PGPPublicKey k = (PGPPublicKey)kIt.next();
            if (k.isEncryptionKey()) {
               key = k;
            }
         }
      }

      if (key == null) {
         throw new IllegalArgumentException("Can't find encryption key in key ring.");
      } else {
         return key;
      }
   }

   public PGPPrivateKey findSecretKey(InputStream keyIn, long keyID, char[] pass) throws IOException, PGPException, NoSuchProviderException {
      PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(keyIn));
      PGPSecretKey pgpSecKey = pgpSec.getSecretKey(keyID);
      if (pgpSecKey == null) {
         return null;
      } else {
         PBESecretKeyDecryptor a = (new JcePBESecretKeyDecryptorBuilder((new JcaPGPDigestCalculatorProviderBuilder()).setProvider("BC").build())).setProvider("BC").build(pass);
         return pgpSecKey.extractPrivateKey(a);
      }
   }

   public void decryptFile(InputStream in, OutputStream out, InputStream keyIn, char[] passwd) throws Exception {
      Security.addProvider(new BouncyCastleProvider());
      in = PGPUtil.getDecoderStream(in);
      PGPObjectFactory pgpF = new PGPObjectFactory(in);
      Object o = pgpF.nextObject();
      PGPEncryptedDataList enc;
      if (o instanceof PGPEncryptedDataList) {
         enc = (PGPEncryptedDataList)o;
      } else {
         enc = (PGPEncryptedDataList)pgpF.nextObject();
      }

      Iterator<PGPPublicKeyEncryptedData> it = enc.getEncryptedDataObjects();
      PGPPrivateKey sKey = null;

      PGPPublicKeyEncryptedData pbe;
      for(pbe = null; sKey == null && it.hasNext(); sKey = this.findSecretKey(keyIn, pbe.getKeyID(), passwd)) {
         pbe = (PGPPublicKeyEncryptedData)it.next();
      }

      if (sKey == null) {
         throw new IllegalArgumentException("Secret key for message not found.");
      } else {
         PublicKeyDataDecryptorFactory b = (new JcePublicKeyDataDecryptorFactoryBuilder()).setProvider("BC").setContentProvider("BC").build(sKey);
         InputStream clear = pbe.getDataStream(b);
         PGPObjectFactory plainFact = new PGPObjectFactory(clear);
         Object message = plainFact.nextObject();
         if (message instanceof PGPCompressedData) {
            PGPCompressedData cData = (PGPCompressedData)message;
            PGPObjectFactory pgpFact = new PGPObjectFactory(cData.getDataStream());
            message = pgpFact.nextObject();
         }

         if (!(message instanceof PGPLiteralData)) {
            if (message instanceof PGPOnePassSignatureList) {
               throw new PGPException("Encrypted message contains a signed message - not literal data.");
            } else {
               throw new PGPException("Message is not a simple encrypted file - type unknown.");
            }
         } else {
            PGPLiteralData ld = (PGPLiteralData)message;
            InputStream unc = ld.getInputStream();

            int ch;
            while((ch = unc.read()) >= 0) {
               out.write(ch);
            }

            if (pbe.isIntegrityProtected() && !pbe.verify()) {
               throw new PGPException("Message failed integrity check");
            }
         }
      }
   }

   public byte[] decrypt(InputStream in, String keyInPath, char[] passwd) throws Exception {
      InputStream keyIn = new FileInputStream(keyInPath);
      byte[] bytes = null;
      Security.addProvider(new BouncyCastleProvider());
      in = PGPUtil.getDecoderStream(in);
      PGPObjectFactory pgpF = new PGPObjectFactory(in);
      Object o = pgpF.nextObject();
      PGPEncryptedDataList enc;
      if (o instanceof PGPEncryptedDataList) {
         enc = (PGPEncryptedDataList)o;
      } else {
         enc = (PGPEncryptedDataList)pgpF.nextObject();
      }

      Iterator<PGPPublicKeyEncryptedData> it = enc.getEncryptedDataObjects();
      PGPPrivateKey sKey = null;

      PGPPublicKeyEncryptedData pbe;
      for(pbe = null; sKey == null && it.hasNext(); sKey = this.findSecretKey(keyIn, pbe.getKeyID(), passwd)) {
         pbe = (PGPPublicKeyEncryptedData)it.next();
      }

      if (sKey == null) {
         throw new IllegalArgumentException("Secret key for message not found.");
      } else {
         PublicKeyDataDecryptorFactory b = (new JcePublicKeyDataDecryptorFactoryBuilder()).setProvider("BC").setContentProvider("BC").build(sKey);
         InputStream clear = pbe.getDataStream(b);
         PGPObjectFactory plainFact = new PGPObjectFactory(clear);
         Object message = plainFact.nextObject();
         if (message instanceof PGPCompressedData) {
            PGPCompressedData cData = (PGPCompressedData)message;
            PGPObjectFactory pgpFact = new PGPObjectFactory(cData.getDataStream());
            message = pgpFact.nextObject();
         }

         if (message instanceof PGPLiteralData) {
            PGPLiteralData ld = (PGPLiteralData)message;
            InputStream unc = ld.getInputStream();
            byte[] bytes2 = IOUtils.toByteArray(unc);
            if (pbe.isIntegrityProtected() && !pbe.verify()) {
               throw new PGPException("Message failed integrity check");
            } else {
               return bytes2;
            }
         } else if (message instanceof PGPOnePassSignatureList) {
            throw new PGPException("Encrypted message contains a signed message - not literal data.");
         } else {
            throw new PGPException("Message is not a simple encrypted file - type unknown.");
         }
      }
   }

   public void encryptFile(OutputStream out, String fileName, PGPPublicKey encKey, boolean armor, boolean withIntegrityCheck) throws IOException, NoSuchProviderException, PGPException {
      Security.addProvider(new BouncyCastleProvider());
      if (armor) {
         out = new ArmoredOutputStream((OutputStream)out);
      }

      ByteArrayOutputStream bOut = new ByteArrayOutputStream();
      PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(1);
      PGPUtil.writeFileToLiteralData(comData.open(bOut), 'b', new File(fileName));
      comData.close();
      JcePGPDataEncryptorBuilder c = (new JcePGPDataEncryptorBuilder(3)).setWithIntegrityPacket(withIntegrityCheck).setSecureRandom(new SecureRandom()).setProvider("BC");
      PGPEncryptedDataGenerator cPk = new PGPEncryptedDataGenerator(c);
      JcePublicKeyKeyEncryptionMethodGenerator d = (new JcePublicKeyKeyEncryptionMethodGenerator(encKey)).setProvider(new BouncyCastleProvider()).setSecureRandom(new SecureRandom());
      cPk.addMethod(d);
      byte[] bytes = bOut.toByteArray();
      OutputStream cOut = cPk.open((OutputStream)out, (long)bytes.length);
      cOut.write(bytes);
      cOut.close();
      ((OutputStream)out).close();
   }

   public void encrypt(OutputStream out, String fileName, PGPPublicKey encKey, boolean armor, boolean withIntegrityCheck) throws IOException, NoSuchProviderException, PGPException {
      Security.addProvider(new BouncyCastleProvider());
      if (armor) {
         out = new ArmoredOutputStream((OutputStream)out);
      }

      ByteArrayOutputStream bOut = new ByteArrayOutputStream();
      PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(1);
      PGPUtil.writeFileToLiteralData(comData.open(bOut), 'b', new File(fileName));
      comData.close();
      JcePGPDataEncryptorBuilder c = (new JcePGPDataEncryptorBuilder(3)).setWithIntegrityPacket(withIntegrityCheck).setSecureRandom(new SecureRandom()).setProvider("BC");
      PGPEncryptedDataGenerator cPk = new PGPEncryptedDataGenerator(c);
      JcePublicKeyKeyEncryptionMethodGenerator d = (new JcePublicKeyKeyEncryptionMethodGenerator(encKey)).setProvider(new BouncyCastleProvider()).setSecureRandom(new SecureRandom());
      cPk.addMethod(d);
      byte[] bytes = bOut.toByteArray();
      OutputStream cOut = cPk.open((OutputStream)out, (long)bytes.length);
      cOut.write(bytes);
      cOut.close();
      ((OutputStream)out).close();
   }

   public static PGPSecretKey findSecretKey(InputStream in) throws IOException, PGPException {
      in = PGPUtil.getDecoderStream(in);
      PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(in);
      PGPSecretKey key = null;
      Iterator rIt = pgpSec.getKeyRings();

      while(key == null && rIt.hasNext()) {
         PGPSecretKeyRing kRing = (PGPSecretKeyRing)rIt.next();
         Iterator kIt = kRing.getSecretKeys();

         while(key == null && kIt.hasNext()) {
            PGPSecretKey k = (PGPSecretKey)kIt.next();
            if (k.isSigningKey()) {
               key = k;
            }
         }
      }

      if (key == null) {
         throw new IllegalArgumentException("Can't find signing key in key ring.");
      } else {
         return key;
      }
   }

   public static PGPPublicKey readPublicKey(String publicKeyFilePath) throws IOException, PGPException {
      InputStream in2 = new FileInputStream(new File(publicKeyFilePath));
      InputStream in = PGPUtil.getDecoderStream(in2);
      PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(in);
      PGPPublicKey key = null;
      Iterator rIt = pgpPub.getKeyRings();

      while(key == null && rIt.hasNext()) {
         PGPPublicKeyRing kRing = (PGPPublicKeyRing)rIt.next();
         Iterator kIt = kRing.getPublicKeys();
         boolean var7 = false;

         while(key == null && kIt.hasNext()) {
            PGPPublicKey k = (PGPPublicKey)kIt.next();
            if (k.isEncryptionKey()) {
               key = k;
            }
         }
      }

      if (key == null) {
         throw new IllegalArgumentException("Can't find encryption key in key ring.");
      } else {
         return key;
      }
   }

   public byte[] encrypt(InputStream in, String encKeyPath) throws IOException, PGPException {
      PGPPublicKey encKey = readPublicKey(encKeyPath);
      byte[] clearData = IOUtils.toByteArray(in);
      Security.addProvider(new BouncyCastleProvider());
      String fileName = null;
      boolean withIntegrityCheck = true;
      boolean armor = false;
      if (fileName == null) {
         fileName = "_CONSOLE";
      }

      ByteArrayOutputStream encOut = new ByteArrayOutputStream();
      OutputStream out = encOut;
      if (armor) {
         out = new ArmoredOutputStream(encOut);
      }

      ByteArrayOutputStream bOut = new ByteArrayOutputStream();
      PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(1);
      OutputStream cos = comData.open(bOut);
      PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();
      OutputStream pOut = lData.open(cos, 'b', fileName, (long)clearData.length, new Date());
      pOut.write(clearData);
      lData.close();
      comData.close();
      PGPEncryptedDataGenerator cPk = new PGPEncryptedDataGenerator((new JcePGPDataEncryptorBuilder(3)).setWithIntegrityPacket(withIntegrityCheck).setSecureRandom(new SecureRandom()).setProvider("BC"));
      cPk.addMethod((new JcePublicKeyKeyEncryptionMethodGenerator(encKey)).setProvider("BC"));
      byte[] bytes = bOut.toByteArray();
      OutputStream cOut = cPk.open((OutputStream)out, (long)bytes.length);
      cOut.write(bytes);
      cOut.close();
      ((OutputStream)out).close();
      return encOut.toByteArray();
   }

   public byte[] inputStreamToByteArray(InputStream is) throws IOException {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      byte[] data = new byte[1024];

      int nRead;
      while((nRead = is.read(data, 0, data.length)) != -1) {
         buffer.write(data, 0, nRead);
      }

      buffer.flush();
      return buffer.toByteArray();
   }

   public void verifySignature(String fileName, byte[] b, InputStream keyIn) throws GeneralSecurityException, IOException, PGPException {
      PGPObjectFactory pgpFact = new PGPObjectFactory(b);
      PGPSignatureList p3 = null;
      Object o = pgpFact.nextObject();
      if (o instanceof PGPCompressedData) {
         PGPCompressedData c1 = (PGPCompressedData)o;
         pgpFact = new PGPObjectFactory(c1.getDataStream());
         p3 = (PGPSignatureList)pgpFact.nextObject();
      } else {
         p3 = (PGPSignatureList)o;
      }

      PGPPublicKeyRingCollection pgpPubRingCollection = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(keyIn));
      InputStream dIn = new BufferedInputStream(new FileInputStream(fileName));
      PGPSignature sig = p3.get(0);
      PGPPublicKey key = pgpPubRingCollection.getPublicKey(sig.getKeyID());
      sig.init((new JcaPGPContentVerifierBuilderProvider()).setProvider(new BouncyCastleProvider()), key);

      int ch;
      while((ch = dIn.read()) >= 0) {
         sig.update((byte)ch);
      }

      dIn.close();
      if (sig.verify()) {
         System.out.println("signature verified.");
      } else {
         System.out.println("signature verification failed.");
      }

   }

   public PGPSecretKey readSecretKey(InputStream input) throws IOException, PGPException {
      PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(input));
      Iterator keyRingIter = pgpSec.getKeyRings();

      while(keyRingIter.hasNext()) {
         PGPSecretKeyRing keyRing = (PGPSecretKeyRing)keyRingIter.next();
         Iterator keyIter = keyRing.getSecretKeys();

         while(keyIter.hasNext()) {
            PGPSecretKey key = (PGPSecretKey)keyIter.next();
            if (key.isSigningKey()) {
               return key;
            }
         }
      }

      throw new IllegalArgumentException("Can't find signing key in key ring.");
   }

   public byte[] createSignature(String fileName, InputStream keyIn, OutputStream out, char[] pass, boolean armor) throws GeneralSecurityException, IOException, PGPException {
      PGPSecretKey pgpSecKey = this.readSecretKey(keyIn);
      PGPPrivateKey pgpPrivKey = pgpSecKey.extractPrivateKey((new JcePBESecretKeyDecryptorBuilder()).setProvider(new BouncyCastleProvider()).build(pass));
      PGPSignatureGenerator sGen = new PGPSignatureGenerator((new JcaPGPContentSignerBuilder(pgpSecKey.getPublicKey().getAlgorithm(), 2)).setProvider(new BouncyCastleProvider()));
      sGen.init(0, pgpPrivKey);
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
      ArmoredOutputStream aOut = new ArmoredOutputStream(byteOut);
      BCPGOutputStream bOut = new BCPGOutputStream(byteOut);
      BufferedInputStream fIn = new BufferedInputStream(new FileInputStream(fileName));

      int ch;
      while((ch = fIn.read()) >= 0) {
         sGen.update((byte)ch);
      }

      aOut.endClearText();
      fIn.close();
      sGen.generate().encode(bOut);
      if (armor) {
         aOut.close();
      }

      return byteOut.toByteArray();
   }
}
