import config.Config;
import server.DyndnsV2Proxy;

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Main {

    static String CONFIG_DEFAULT_KEY_runHTTP = "HTTP_active";
    static String CONFIG_DEFAULT_KEY_HTTPPort = "HTTP_Port";
    static String CONFIG_DEFAULT_KEY_runHTTPS = "HTTPS_active";
    static String CONFIG_DEFAULT_KEY_HTTPsPort = "HTTPS_Port";
    static String CONFIG_DEFAULT_KEY_keyStoreFilePath = "HTTPS_keyStoreFilePath";
    static String CONFIG_DEFAULT_KEY_keyStorePassPhrase = "HTTPS_keyStorePassPhrase";
    static String CONFIG_DEFAULT_KEY_IPAddress = "IPAddress";

    static String[] configDefaults = {
        CONFIG_DEFAULT_KEY_runHTTP, "false",
        CONFIG_DEFAULT_KEY_HTTPPort, "80",
        CONFIG_DEFAULT_KEY_runHTTPS, "false",
        CONFIG_DEFAULT_KEY_HTTPsPort, "443",
        CONFIG_DEFAULT_KEY_keyStoreFilePath, "",
        CONFIG_DEFAULT_KEY_keyStorePassPhrase, "",
        CONFIG_DEFAULT_KEY_IPAddress, "127.0.0.1",
    };

    static String defConfigPath = "./proxy.conf";
    static String defLogPath = "./dyndnsproxy.log";

    public static void main(String[] args) throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {

        String configPath = System.getenv("CONFIG_PATH");
        String logPath = System.getenv("LOG_PATH");
        Logger logger = Logger.getLogger("Log");

        if(configPath == null){
            configPath = defConfigPath;
        }

        if(logPath == null){
            logPath = defLogPath;
        }

        try {
            FileHandler fh;
            // This block configure the logger with handler and formatter
            fh = new FileHandler(logPath);
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
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

        if(ip == null){
            logger.log(Level.SEVERE, "missing IPAddress Field");
            System.err.println("missing IPAddress Field");
        }

        if(Boolean.parseBoolean(properties.getProperty(CONFIG_DEFAULT_KEY_runHTTP))){
            try {
                int port = Integer.parseInt(properties.getProperty(CONFIG_DEFAULT_KEY_HTTPPort));
                httpServer = new DyndnsV2Proxy(ip, port);
                System.out.printf("HTTP-Proxy started. Listening on %s:%d\n",ip, port);
            }catch (Exception e){
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        //HTTPS not supported (cant test this shit... )

        if(Boolean.parseBoolean(properties.getProperty(CONFIG_DEFAULT_KEY_runHTTPS))){
            try {
                int port = Integer.parseInt(properties.getProperty(CONFIG_DEFAULT_KEY_HTTPsPort));
                String keyStoreFilePath = properties.getProperty(CONFIG_DEFAULT_KEY_keyStoreFilePath);
                String keyStorePassPhrase = properties.getProperty(CONFIG_DEFAULT_KEY_keyStorePassPhrase);
                httpsServer = new DyndnsV2Proxy(ip, port, keyStoreFilePath, keyStorePassPhrase);
                System.out.printf("HTTPs-Proxy started. Listening on %s:%d\n",ip, port);
            }catch (Exception e){
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }


    }


}
