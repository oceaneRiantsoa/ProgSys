import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {
    private String masterAddress;
    private int masterPort;
    private String defaultDirectory;

    public ConfigManager(String configFilePath) {
        loadConfig(configFilePath);
    }

    private void loadConfig(String filePath) {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(filePath)) {
            properties.load(fis);
            masterAddress = properties.getProperty("MASTER_ADDRESS", "127.0.0.1");
            masterPort = Integer.parseInt(properties.getProperty("MASTER_PORT", "12345"));
            defaultDirectory = properties.getProperty("DEFAULT_DIRECTORY", "RepClient");
        } catch (IOException e) {
            System.err.println("Erreur de chargement de la configuration : " + e.getMessage());
        }
    }

    public String getMasterAddress() {
        return masterAddress;
    }

    public int getMasterPort() {
        return masterPort;
    }

    public String getDefaultDirectory() {
        return defaultDirectory;
    }
}
