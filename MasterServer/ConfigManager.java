import java.io.*;
import java.util.*;

public class ConfigManager {
    private static Properties config = new Properties();

    public static void loadConfig(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            config.load(fis);
        }
    }

    public static int getMasterPort() {
        return Integer.parseInt(config.getProperty("MASTER_PORT"));
    }

    public static String[] getSlaveAddresses() {
        return config.getProperty("SLAVE_ADDRESSES").split(",");
    }

    public static int[] getSlavePorts() {
        String[] ports = config.getProperty("SLAVE_PORTS").split(",");
        int[] slavePorts = new int[ports.length];
        for (int i = 0; i < ports.length; i++) {
            slavePorts[i] = Integer.parseInt(ports[i]);
        }
        return slavePorts;
    }

    public static int getNumSlaves() {
        return getSlaveAddresses().length;
    }    

    public static void validateConfig() {
        String[] addresses = getSlaveAddresses();
        int[] ports = getSlavePorts();
    
        if (addresses.length != ports.length) {
            throw new IllegalArgumentException("Le nombre d'adresses esclaves ne correspond pas au nombre de ports esclaves.");
        }
    }
    
}
