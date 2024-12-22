import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientTerminal {

    private static File defaultDirectory;
    private static File currentDirectory;

    private static String MASTER_ADDRESS;
    private static int MASTER_PORT;

    public static void main(String[] args) {

        ConfigManager config = new ConfigManager("Client_config.txt");
        MASTER_ADDRESS = config.getMasterAddress();
        MASTER_PORT = config.getMasterPort();
        defaultDirectory = new File(config.getDefaultDirectory());
        currentDirectory = defaultDirectory;

        Scanner scanner = new Scanner(System.in);

        System.out.println("Bienvenue dans le client terminal. Tapez 'help' pour voir les commandes disponibles.");

        while (true) {
            System.out.print(currentDirectory.getAbsolutePath() + "> ");
            String input = scanner.nextLine().trim();
            String[] parts = input.split(" ", 2);
            String command = parts[0].toLowerCase();
            String argument = parts.length > 1 ? parts[1] : null;

            switch (command) {
                case "ls":
                    listLocalFiles();
                    break;
                case "rm":
                    if (argument != null) {
                        removeLocalFile(argument);
                    } else {
                        System.out.println("Usage: rm <nom_fichier>");
                    }
                    break;
                case "add":
                    if (argument != null) {
                        uploadFile(argument);
                    } else {
                        System.out.println("Usage: add <nom_fichier>");
                    }
                    break;
                case "get":
                    if (argument != null) {
                        downloadFile(argument);
                    } else {
                        System.out.println("Usage: get <nom_fichier>");
                    }
                    break;
                case "cd":
                    if (argument != null) {
                        changeDirectory(argument);
                    } else {
                        System.out.println("Usage: cd <RepClient|RepDownload>");
                    }
                    break;
                case "list_files":
                    listServerFiles();
                    break;
                case "help":
                    printHelp();
                    break;
                case "exit":
                    System.out.println("Au revoir!");
                    scanner.close();
                    return;
                default:
                    System.out.println("Commande inconnue. Tapez 'help' pour voir les commandes disponibles.");
            }
        }
    }

    private static void listLocalFiles() {
        File[] files = currentDirectory.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isFile()) {
                    System.out.println(file.getName());
                }
            }
        } else {
            System.out.println("Aucun fichier trouvé dans le répertoire actuel.");
        }
    }

    private static void removeLocalFile(String fileName) {
        File file = new File(currentDirectory, fileName);
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                System.out.println("Fichier supprimé : " + fileName);
            } else {
                System.out.println("Impossible de supprimer le fichier : " + fileName);
            }
        } else {
            System.out.println("Fichier introuvable : " + fileName);
        }
    }

    private static void uploadFile(String fileName) {
        File file = new File(currentDirectory, fileName);
        if (!file.exists() || !file.isFile()) {
            System.out.println("Fichier introuvable : " + fileName);
            return;
        }

        try (Socket socket = new Socket(MASTER_ADDRESS, MASTER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             FileInputStream fis = new FileInputStream(file)) {

            dos.writeUTF("UPLOAD");
            dos.writeUTF(file.getName());
            dos.writeLong(file.length());

            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, read);
            }

            System.out.println("Fichier envoyé : " + fileName);
        } catch (IOException e) {
            System.out.println("Erreur lors de l'envoi du fichier : " + e.getMessage());
        }
    }

    private static void downloadFile(String fileName) {
        File downloadDirectory = new File("RepDownload");
        File downloadedFile = new File(downloadDirectory, "downloaded_" + fileName);

        try (Socket socket = new Socket(MASTER_ADDRESS, MASTER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             FileOutputStream fos = new FileOutputStream(downloadedFile)) {

            dos.writeUTF("DOWNLOAD");
            dos.writeUTF(fileName);

            long fileSize = dis.readLong();
            if (fileSize <= 0) {
                System.out.println("Fichier introuvable sur le serveur.");
                return;
            }

            byte[] buffer = new byte[4096];
            int read;
            long totalRead = 0;
            while (totalRead < fileSize && (read = dis.read(buffer)) > 0) {
                fos.write(buffer, 0, read);
                totalRead += read;
            }

            System.out.println("Fichier téléchargé : " + downloadedFile.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("Erreur lors du téléchargement : " + e.getMessage());
        }
    }

    private static void changeDirectory(String directoryName) {
        File newDirectory = new File(directoryName);
        if (newDirectory.exists() && newDirectory.isDirectory()) {
            currentDirectory = newDirectory;
            System.out.println("Répertoire actuel : " + currentDirectory.getAbsolutePath());
        } else {
            System.out.println("Répertoire introuvable : " + directoryName);
        }
    }

    private static void listServerFiles() {
        try (Socket socket = new Socket(MASTER_ADDRESS, MASTER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF("LIST_FILES");

            int fileCount = dis.readInt();
            if (fileCount > 0) {
                System.out.println("Fichiers sur le serveur :");
                for (int i = 0; i < fileCount; i++) {
                    System.out.println(dis.readUTF());
                }
            } else {
                System.out.println("Aucun fichier trouvé sur le serveur.");
            }
        } catch (IOException e) {
            System.out.println("Erreur lors de la récupération des fichiers : " + e.getMessage());
        }
    }

    private static void printHelp() {
        System.out.println("Commandes disponibles :");
        System.out.println("  ls            : Liste les fichiers dans le répertoire actuel");
        System.out.println("  rm <fichier>  : Supprime un fichier dans le répertoire actuel");
        System.out.println("  add <fichier> : Ajoute un fichier au serveur depuis le répertoire actuel");
        System.out.println("  get <fichier> : Télécharge un fichier depuis le serveur");
        System.out.println("  cd <rep>      : Change le répertoire (RepClient ou RepDownload)");
        System.out.println("  list_files    : Liste les fichiers disponibles sur le serveur");
        System.out.println("  help          : Affiche ce message d'aide");
        System.out.println("  exit          : Quitte l'application");
    }
}
