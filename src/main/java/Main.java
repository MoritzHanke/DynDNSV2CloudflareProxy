import config.Config;
import server.DyndnsV2Proxy;

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Properties;
import java.util.logging.*;

public class Main {

    static String CONFIG_DEFAULT_KEY_HTTP_active = "HTTP_active";
    static String CONFIG_DEFAULT_KEY_HTTP_port = "HTTP_port";

    static String CONFIG_DEFAULT_KEY_HTTPS_active = "HTTPS_active";
    static String CONFIG_DEFAULT_KEY_HTTPS_port = "HTTPS_port";
    static String CONFIG_DEFAULT_KEY_HTTPS_keyStoreFilePath = "HTTPS_keyStoreFilePath";
    static String CONFIG_DEFAULT_KEY_HTTPS_keyStorePassphrase = "HTTPS_keyStorePassphrase";

    static String CONFIG_DEFAULT_KEY_IPAddress = "IPAddress";

    // makes really only sense if used in local network/with https
    // though maybe you update it in a local network via http, but via port forwarding also from the outside...
    static String CONFIG_DEFAULT_KEY_BasicAuth_active = "BasicAuth_active";
    static String CONFIG_DEFAULT_KEY_BasicAuth_username = "BasicAuth_username";
    static String CONFIG_DEFAULT_KEY_BasicAuth_password = "BasicAuth_password";


    static String[] configDefaults = {
            CONFIG_DEFAULT_KEY_HTTP_active, "false",
            CONFIG_DEFAULT_KEY_HTTP_port, "80",
            CONFIG_DEFAULT_KEY_HTTPS_active, "false",
            CONFIG_DEFAULT_KEY_HTTPS_port, "443",
            CONFIG_DEFAULT_KEY_HTTPS_keyStoreFilePath, "",
            CONFIG_DEFAULT_KEY_HTTPS_keyStorePassphrase, "",
            CONFIG_DEFAULT_KEY_IPAddress, "127.0.0.1",
            CONFIG_DEFAULT_KEY_BasicAuth_active, "true",
            CONFIG_DEFAULT_KEY_BasicAuth_username, "",
            CONFIG_DEFAULT_KEY_BasicAuth_password, "",
    };

    static String defConfigPath = "./proxy.conf";
    static String defLogPath = "./dyndnsproxy.log";

    public static void main(String[] args) throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        //init logger
        Logger logger = Logger.getLogger("Log");
        initLogger( logger);

        String configPath = System.getenv("CONFIG_PATH");

        if(configPath == null){
            configPath = defConfigPath;
        }

        Properties properties;
        try {
            properties = Config.loadPropertiesFillDefaults(new File(configPath), configDefaults);
        }catch (Exception e){
            e.printStackTrace();
            logger.log(Level.SEVERE, e.getMessage(), e);
            System.err.println("error with Config. See StackTrace");
            return;
        }

        DyndnsV2Proxy httpServer = null;
        DyndnsV2Proxy httpsServer = null;

        String ip = properties.getProperty(CONFIG_DEFAULT_KEY_IPAddress);
        boolean useBasicAuth = Boolean.parseBoolean(properties.getProperty(CONFIG_DEFAULT_KEY_BasicAuth_active));
        DyndnsV2Proxy.BasicAuth basicAuth = null;

        if(useBasicAuth){
            basicAuth = new DyndnsV2Proxy.BasicAuth(
                    properties.getProperty(CONFIG_DEFAULT_KEY_BasicAuth_username),
                    properties.getProperty(CONFIG_DEFAULT_KEY_BasicAuth_password)
            );
        }

        if(ip == null){
            logger.log(Level.SEVERE, "missing IPAddress Field");
            System.err.println("missing IPAddress Field");
        }


        if(Boolean.parseBoolean(properties.getProperty(CONFIG_DEFAULT_KEY_HTTP_active))){
            try {
                int port = Integer.parseInt(properties.getProperty(CONFIG_DEFAULT_KEY_HTTP_port));
                httpServer = new DyndnsV2Proxy(ip, port, basicAuth);
                System.out.printf("HTTP-Proxy started. Listening on %s:%d\n",ip, port);
            }catch (Exception e){
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        //HTTPS not supported (cant test this shit... )

        if(Boolean.parseBoolean(properties.getProperty(CONFIG_DEFAULT_KEY_HTTPS_active))){
            try {
                int port = Integer.parseInt(properties.getProperty(CONFIG_DEFAULT_KEY_HTTPS_port));
                String keyStoreFilePath = properties.getProperty(CONFIG_DEFAULT_KEY_HTTPS_keyStoreFilePath);
                String keyStorePassPhrase = properties.getProperty(CONFIG_DEFAULT_KEY_HTTPS_keyStorePassphrase);
                httpsServer = new DyndnsV2Proxy(ip, port, keyStoreFilePath, keyStorePassPhrase, basicAuth);
                System.out.printf("HTTPs-Proxy started. Listening on %s:%d\n",ip, port);
            }catch (Exception e){
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }


    }

    public static void initLogger(Logger logger){
        String logPath = System.getenv("LOG_PATH");

        if(logPath == null){
            logPath = defLogPath;
        }

        try {
            FileHandler fh;
            // This block configure the logger with handler and formatter
            fh = new FileHandler(logPath);
            logger.addHandler(fh);
            fh.setFormatter( new SimpleFormatter());
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }

        /*
        final ConsoleHandler handler = new ConsoleHandler() {
            @Override
            protected void setOutputStream(final OutputStream out) throws SecurityException {
                super.setOutputStream(System.out);
            }
        };

        logger.addHandler(handler);
        */
    }


}
