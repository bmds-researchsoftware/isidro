/**
 *
 */
package edu.dartmouth.geisel.isidro.signature;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Enumeration;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignatureException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.poifs.crypt.dsig.SignatureConfig;
import org.apache.poi.poifs.crypt.dsig.SignatureInfo;

/**
 * @author Patrick Eads
 */
public final class ExcelSignature
{
    /**
     * Private constructor of utilities class.
     */
    private ExcelSignature()
    {
    }

    /**
     * Mark shreadsheet as final with given signature file.
     *
     * @param keypath
     *            String path to signature file.
     * @param passwd
     *            String password for signature file.
     * @param xlsxFilePath
     *            String path to spread to sign
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     * @throws InvalidFormatException
     * @throws XMLSignatureException
     * @throws MarshalException
     */
    public static void sign(final String keypath, final String passwd,
            final String xlsxFilePath)
            throws KeyStoreException, IOException, CertificateException,
            NoSuchAlgorithmException, UnrecoverableKeyException,
            InvalidFormatException, XMLSignatureException, MarshalException
    {
        if (passwd != null && keypath != null)
        {
            final char password[] = passwd.toCharArray();
            final File file = new File(keypath);
            final KeyStore keystore = KeyStore.getInstance("PKCS12");

            final FileInputStream fis = new FileInputStream(file);
            keystore.load(fis, password);

            final Enumeration<String> aliases = keystore.aliases();
            final String alias = aliases.nextElement();
            final Key key = keystore.getKey(alias, password);
            final X509Certificate x509 = (X509Certificate) keystore
                    .getCertificate(alias);
            final KeyPair keyPair = new KeyPair(x509.getPublicKey(),
                    (PrivateKey) key);

            final SignatureConfig signatureConfig = new SignatureConfig();
            signatureConfig.setKey(keyPair.getPrivate());
            signatureConfig.setSigningCertificateChain(
                    Collections.singletonList(x509));
            signatureConfig
                    .setSignatureDescription("isidroSignatureDescription");

            final OPCPackage pkg = OPCPackage.open(xlsxFilePath,
                    PackageAccess.READ_WRITE);
            signatureConfig.setOpcPackage(pkg);

            final SignatureInfo si = new SignatureInfo();
            si.setSignatureConfig(signatureConfig);
            si.confirmSignature();
            pkg.close();
            fis.close();
        }
    }
}
