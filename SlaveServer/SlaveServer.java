import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class SlaveServer implements Runnable{
    private final int port;
    private final String storageDirectory;

    public SlaveServer(int port, String storageDirectory) {
        this.port = port;
        this.storageDirectory = storageDirectory;

        File directory = new File(storageDirectory);
        if (!directory.exists()) {
            directory.mkdirs(); // Crée le répertoire si nécessaire
        }
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serveur esclave démarré sur le port " + port);

            while (true) {
                Socket masterSocket = serverSocket.accept();
                new Thread(() -> handleMasterRequest(masterSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleMasterRequest(Socket masterSocket) {
        try (DataInputStream dis = new DataInputStream(masterSocket.getInputStream());
             DataOutputStream dos = new DataOutputStream(masterSocket.getOutputStream())) {

            String command = dis.readUTF(); // "STORE" ou "FETCH"

            if (command.equalsIgnoreCase("STORE")) {
                // Stocker un bloc envoyé par le serveur maître
                String blockName = dis.readUTF();
                long blockSize = dis.readLong();
                File blockFile = new File(storageDirectory + File.separator + blockName);

                try (FileOutputStream fos = new FileOutputStream(blockFile)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    long totalRead = 0;
                    while (totalRead < blockSize && (read = dis.read(buffer)) > 0) {
                        fos.write(buffer, 0, read);
                        totalRead += read;
                    }
                }

                System.out.println("Bloc reçu et stocké : " + blockFile.getName());

            } else if (command.equalsIgnoreCase("FETCH")) {
                // Renvoyer un bloc demandé par le serveur maître
                String blockName = dis.readUTF();
                File blockFile = new File(storageDirectory + File.separator + blockName);

                if (!blockFile.exists()) {
                    dos.writeLong(0);
                    System.out.println("Bloc introuvable : " + blockName);
                    return;
                }

                dos.writeLong(blockFile.length());
                try (FileInputStream fis = new FileInputStream(blockFile)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = fis.read(buffer)) > 0) {
                        dos.write(buffer, 0, read);
                    }
                }

                System.out.println("Bloc envoyé : " + blockFile.getName());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        start();
    }

    public static void main(String[] args) {
        // Paramètres pour les trois serveurs esclaves
        // int[] ports = {23456, 23457, 23458};
        // String[] storageDirs = {"slave1_storage", "slave2_storage", "slave3_storage"};
        try {
            ConfigManager.loadConfig("../MasterServer/Master_config.txt");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        int[] ports = ConfigManager.getSlavePorts();

        // Démarrer chaque serveur dans un thread
        for (int i = 0; i < ports.length; i++) {
            int port = ports[i];
            String storageDir = "slave" + (i + 1) + "_storage";
            new Thread(new SlaveServer(port, storageDir)).start();
        }
    }
}
