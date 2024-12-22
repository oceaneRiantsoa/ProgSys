import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MasterServer {
    // private static final int MASTER_PORT = 12345;
    // private static final String[] SLAVE_ADDRESSES = {"127.0.0.1", "127.0.0.1", "127.0.0.1"};
    // private static final int[] SLAVE_PORTS = {23456, 23457, 23458};
    private static int MASTER_PORT;
    private static String[] SLAVE_ADDRESSES;
    private static int[] SLAVE_PORTS;
    private static int parts;

    public static void main(String[] args) {
        try {
             // Charger et valider la configuration
            ConfigManager.loadConfig("Master_config.txt");
            ConfigManager.validateConfig();
 
             // Récupérer les configurations
            MASTER_PORT = ConfigManager.getMasterPort();
            SLAVE_ADDRESSES = ConfigManager.getSlaveAddresses();
            SLAVE_PORTS = ConfigManager.getSlavePorts();
            parts = ConfigManager.getNumSlaves(); // Dynamique

            try (ServerSocket serverSocket = new ServerSocket(MASTER_PORT)) {
                System.out.println("Serveur maître en attente de connexions...");

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connecté : " + clientSocket.getInetAddress());
                    new Thread(() -> handleClient(clientSocket)).start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

            String command = dis.readUTF(); // UPLOAD ou DOWNLOAD

            if (command.equals("LIST_FILES")) {
                File storageDir = new File("/home/hary/prog/ProgSys_S3/projetSteeve-Ocean/SlaveServer/slave1_storage");
                String[] blockFiles = storageDir.list((dir, name) -> name.startsWith("block_"));
                        
                // Utiliser un Set pour éviter les doublons
                Set<String> originalFiles = new HashSet<>();
                        
                if (blockFiles != null) {
                    for (String blockFile : blockFiles) {
                        // Extraire le nom original du fichier
                        String[] parts = blockFile.split("_temp_");
                        if (parts.length > 1) {
                            String originalFileName = parts[1]; // Nom après "block_X_temp_"
                            originalFiles.add(originalFileName);
                        }
                    }
                
                    // Envoyer la liste des fichiers originaux au client
                    dos.writeInt(originalFiles.size()); // Nombre de fichiers uniques
                    for (String originalFileName : originalFiles) {
                        dos.writeUTF(originalFileName); // Envoie chaque nom unique
                    }
                } else {
                    dos.writeInt(0); // Aucun fichier trouvé
                }

            } if (command.equalsIgnoreCase("UPLOAD")) {
                // Recevoir le fichier
                String fileName = dis.readUTF();
                long fileSize = dis.readLong();
                File tempFile = new File("temp_" + fileName);
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    long totalRead = 0;
                    while (totalRead < fileSize && (read = dis.read(buffer)) > 0) {
                        fos.write(buffer, 0, read);
                        totalRead += read;
                    }
                }

                // Diviser le fichier en blocs et envoyer aux esclaves
                List<File> blocks = splitFile(tempFile, parts);
                for (int i = 0; i < blocks.size(); i++) {
                    sendBlockToSlave(blocks.get(i), SLAVE_ADDRESSES[i], SLAVE_PORTS[i]);
                }

                // Supprimer le fichier temporaire
                tempFile.delete();
                dos.writeUTF("Fichier divisé et distribué avec succès.");
            } else if (command.equalsIgnoreCase("DOWNLOAD")) {
                String fileName = dis.readUTF();
                File assembledFile = assembleFile(fileName, parts);
                dos.writeLong(assembledFile.length());

                // Envoyer le fichier assemblé au client
                try (FileInputStream fis = new FileInputStream(assembledFile)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = fis.read(buffer)) > 0) {
                        dos.write(buffer, 0, read);
                    }
                }

                // Supprimer le fichier assemblé temporaire
                assembledFile.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<File> splitFile(File file, int parts) throws IOException {
        List<File> blocks = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file)) {
            long fileSize = file.length();       // Taille totale du fichier
            long blockSize = fileSize / parts;   // Taille standard d'un bloc => 4
            long remainingSize = fileSize;       // Taille restante à écrire
    
            for (int i = 0; i < parts; i++) {
                File block = new File("block_" + i + "_" + file.getName());
                blocks.add(block);
    
                try (FileOutputStream fos = new FileOutputStream(block)) {
                    long currentBlockSize = (i == parts - 1) ? remainingSize : blockSize;
                    byte[] buffer = new byte[(int) currentBlockSize]; // Tampon de lecture
                    long written = 0;              // Compteur d'octets écrits
                    int read;
                    // System.out.println(fis.read(buffer));
    
                    // Taille du bloc actuel (le dernier bloc peut être plus grand avec les restes)
    
                    while (written < currentBlockSize && (read = fis.read(buffer)) > 0 /*&& remainingSize > 0*/) {
                        long toWrite = Math.min(read, currentBlockSize - written);
                        // long toWrite = currentBlockSize - written;
                        fos.write(buffer, 0, (int) toWrite);
                        written += toWrite;
                        remainingSize -= toWrite;
                    }
                }
            }
        }
        return blocks;
    }
    
    private static void sendBlockToSlave(File block, String slaveAddress, int slavePort) throws IOException {
        try (Socket socket = new Socket(slaveAddress, slavePort);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             FileInputStream fis = new FileInputStream(block)) {
    
            // Indiquer l'action au serveur esclave
            dos.writeUTF("STORE");
    
            // Envoyer le nom et la taille du fichier
            dos.writeUTF(block.getName());
            dos.writeLong(block.length());
    
            // Envoyer le contenu du fichier
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, read);
            }
        }
        block.delete(); // Supprimer le bloc après l'envoi
    }
    
    private static File assembleFile(String fileName, int parts) throws IOException {
        File assembledFile = new File("assembled_" + fileName);
        try (FileOutputStream fos = new FileOutputStream(assembledFile)) {
            for (int i = 0; i < parts; i++) {
                File block = fetchBlockFromSlave(i, fileName, SLAVE_ADDRESSES[i], SLAVE_PORTS[i]);
                try (FileInputStream fis = new FileInputStream(block)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = fis.read(buffer)) > 0) {
                        fos.write(buffer, 0, read);
                    }
                }
                block.delete(); // Supprimer le bloc temporaire
            }
        }
        return assembledFile;
    }

    private static File fetchBlockFromSlave(int part, String fileName, String slaveAddress, int slavePort) throws IOException {
        File block = new File("block_" + part + "_temp_" + fileName);
        try (Socket socket = new Socket(slaveAddress, slavePort);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             FileOutputStream fos = new FileOutputStream(block)) {
            dos.writeUTF("FETCH");
            dos.writeUTF("block_" + part + "_temp_" + fileName);

            long blockSize = dis.readLong();
            byte[] buffer = new byte[4096];
            int read;
            long totalRead = 0;
            while (totalRead < blockSize && (read = dis.read(buffer)) > 0) {
                fos.write(buffer, 0, read);
                totalRead += read;
            }
        }
        return block;
    }
}
