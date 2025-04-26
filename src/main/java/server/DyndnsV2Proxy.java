package server;

import fi.iki.elonen.NanoHTTPD;

import javax.net.ssl.KeyManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Logger;

public class DyndnsV2Proxy extends NanoHTTPD {

    public static class BasicAuth{
        String username;
        String password;

        public BasicAuth(String username, String password) {
            this.username = username;
            this.password = password;
        }

        //prob not safe for side-channel-attacks, but idc
        public boolean validate(byte[] base64Auth){
            try {
                byte[] s = Base64.getDecoder().decode(base64Auth);
                String[] credentials = new String(s).split(":");

                if(credentials.length != 2){
                    return false;
                }

                return credentials[0].equals(username) && credentials[1].equals(password);
            }catch (IllegalArgumentException e){
                return false;
            }
        }
    }

    static Response authError = newFixedLengthResponse(Response.Status.OK, "text/plain", "badauth");
    static Response noHosts = newFixedLengthResponse(Response.Status.OK, "text/plain", "notfqdn");

    Logger logger = Logger.getLogger("Log");
    BasicAuth basicAuth;

    public DyndnsV2Proxy(String ip, int port, BasicAuth basicAuth) throws IOException {
        super(ip, port);
        this.basicAuth = basicAuth;
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    /**
     * create HTTPS server
     * */
    public DyndnsV2Proxy(String ip, int port, String path2KeyStore, String passphrase, BasicAuth basicAuth) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        super(ip, port);
        this.basicAuth = basicAuth;

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

        //ignore uri

        //authentication
        if(basicAuth == null){
            if(session.getHeaders().get("authorization") != null){
                logger.warning("Request with authorization, even though it is disabled");
            }
        }else{
            String auth = session.getHeaders().get("authorization");


            if (auth == null){
                    logger.warning("Request without authorization, even though enabled");
                    return authError;
            }

            String[] authSplit = auth.split(" ");

            if(authSplit.length > 0 && !authSplit[0].equals("Basic")){
                logger.warning("Request without non-Basic authorization format");
                return authError;
            }

            if(authSplit.length != 2){
                logger.warning("Request without bad authorization format");
                return authError;
            }

            if(!basicAuth.validate(authSplit[1].getBytes(StandardCharsets.UTF_8))){
                logger.warning("authorization failed. Wrong username or password");
                return authError;
            }
        }

        //------------------------------
        //------extract parameters------
        //------------------------------

        //ip:
        String ipString = session.getParms().get("myip");

        InetAddress ip = extractInetAddress(ipString);

        if(ip == null){
            String xForwardedFor = session.getHeaders().get("X-Forwarded-For");

            if(xForwardedFor != null){
                ip = extractInetAddress(xForwardedFor.split(",")[0]);
            }else{
                ip = extractInetAddress(session.getHeaders().get("http-client-ip"));
            }
        }

        if(ip == null){
            throw new RuntimeException("cant access header back-up-header http-client-ip or it isnt correctly formatted");
        }


        //hostnames:
        String hostnameString = session.getParms().get("hostname");

        if(hostnameString == null){
            return noHosts;
        }
        String[] hostnames = Arrays.stream(hostnameString.split(",")).map(String::trim).toArray(String[]::new);

        // others parameters will be ignored (wildcard, mx, backmx, system, url and offline (offline is the only one with use)

        //--------------------
        //------evaluate------
        //--------------------
        int numHostnames = 0;

        for (String hostname : hostnames){
            if(hostname.equals("")){
                continue;
            }
            numHostnames++;

            //TODO evaluate records
        }


        if(numHostnames == 0){
            return noHosts;
        }



        return newFixedLengthResponse("test");
    }

    public static InetAddress extractInetAddress(String ip){
        if(ip == null) return null;
        try {
            final InetAddress inet = InetAddress.getByName(ip);
            if(inet.getHostAddress().equalsIgnoreCase(ip)){
                return inet;
            }else{
                return null;
            }
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
