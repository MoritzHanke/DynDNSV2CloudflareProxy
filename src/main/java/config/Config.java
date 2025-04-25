package config;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Config {

    /**
     * @param configFile File to configFile. May not exists and will be created if not.
     * @return Properties-Object on configFile
     * @throws IOException if the given file is an existing directory or if thrown by {@link File#createNewFile()}, {@link Properties#load}
     * @throws SecurityException if thrown by {@link File#mkdirs()}, {@link File#createNewFile()} or {@link FileInputStream#FileInputStream(File)}
     * @throws IllegalArgumentException if thrown by {@link Properties#load} (meaning config file is not formed correctly)
     * */
    public static Properties loadProperties(File configFile) throws IOException, SecurityException, IllegalArgumentException{


        if(!configFile.exists()){
            configFile.getParentFile().mkdirs(); //may throw security exception
            configFile.createNewFile(); //may throw IO/Security Exception
            System.out.println("Creating new config file at: " + configFile.getAbsolutePath());
        }

        if(configFile.isDirectory()){
            throw new IOException("config file is a directory");
        }

        Properties p = new Properties();

        try (FileInputStream fis = new FileInputStream(configFile)) { //may still throw security exception
            p.load(fis); //
        } catch (FileNotFoundException ex) {
            assert false; //should be caught above
        }
        return p;
    }


    /**
     * same as #loadProperties(File) and make sure the default entries exist
     * @param defaults Array consisting of key1, val1, key2, val2, ...
     * */
    public static Properties loadPropertiesFillDefaults(File configFile, String[] defaults) throws IOException, SecurityException, IllegalArgumentException{
        Properties p = loadProperties(configFile);

        boolean valuesChanged = false;

        for (int i = 0; i < defaults.length / 2; i++) {
            if(!p.containsKey(defaults[i*2])){
                valuesChanged=true;
                p.setProperty(defaults[i*2], defaults[i*2+1]);
            }
        }

        if(valuesChanged){
            try { //may still throw security exception
                //p.store(fw, null);
                storeOrdered(p, configFile, null);
            } catch (FileNotFoundException ex) {
                assert false; //should be caught above
            }
        }

        return p;
    }

    public static void storeOrdered(Properties properties, File file, String comments) throws IOException {
        Properties tmp = new Properties() {
            @Override public synchronized Set<Map.Entry<Object, Object>> entrySet() {
                return Collections.synchronizedSet(
                        super.entrySet()
                                .stream()
                                .sorted(Comparator.comparing(e -> e.getKey().toString()))
                                .collect(Collectors.toCollection(LinkedHashSet::new)));
            }
        };

        tmp.putAll(properties);
        tmp.store(new FileWriter(file), comments);
    }
}
