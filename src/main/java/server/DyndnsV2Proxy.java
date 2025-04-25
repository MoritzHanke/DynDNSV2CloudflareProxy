package server;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class DyndnsV2Proxy extends NanoHTTPD {

    public DyndnsV2Proxy(String ip, int port) throws IOException {
        super(ip, port);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    /**
     * create HTTPS server
     * */
    public DyndnsV2Proxy(String ip, int port, String path2KeyStore, String passphrase) throws IOException {
        super(ip, port);
        makeSecure(makeSSLSocketFactory(path2KeyStore, passphrase.toCharArray()), null);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    @Override
    public Response serve(IHTTPSession session){
        return newFixedLengthResponse("test");
    }
}
