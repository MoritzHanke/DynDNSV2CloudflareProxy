package server;

import fi.iki.elonen.NanoHTTPD;

import javax.net.ssl.KeyManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class DyndnsV2Proxy extends NanoHTTPD {

    public DyndnsV2Proxy(String ip, int port) throws IOException {
        super(ip, port);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    /**
     * create HTTPS server
     * */
    public DyndnsV2Proxy(String ip, int port, String path2KeyStore, String passphrase) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        super(ip, port);

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream in = Files.newInputStream(Paths.get(path2KeyStore));
        keyStore.load(in, passphrase.toCharArray());

        if(keyStore.size() == 0){
            throw new RuntimeException("could not load keyStore");
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, passphrase.toCharArray());
        makeSecure(makeSSLSocketFactory(keyStore, keyManagerFactory), null);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);

    }

    @Override
    public Response serve(IHTTPSession session){
        return newFixedLengthResponse("test");
    }
}
