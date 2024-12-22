    import javax.swing.*;
    import java.awt.*;
    import java.awt.event.ActionEvent;
    import java.awt.event.ActionListener;
    import java.awt.event.ComponentAdapter;
    import java.awt.event.ComponentEvent;
    import java.io.DataInputStream;
    import java.io.DataOutputStream;
    import java.io.File;
    import java.io.FileInputStream;
    import java.io.FileOutputStream;
    import java.io.IOException;
    import java.net.Socket;
    import java.util.HashSet;
    import java.util.Set;

    public class FileTransferGUI {
        // private static final String MASTER_ADDRESS = "192.168.75.105";
        // private static final int MASTER_PORT = 12345;

        private String MASTER_ADDRESS;
        private int MASTER_PORT;

        private JTextArea logArea;
        private JFrame frame;
        private JPanel mainPanel;
        private DefaultListModel<String> listModel;
        private File defaultDirectory;
        private File currentDirectory;
        private CardLayout cardLayout;

        public FileTransferGUI() {
            ConfigManager config = new ConfigManager("Client_config.txt");
            this.MASTER_ADDRESS = config.getMasterAddress();
            this.MASTER_PORT = config.getMasterPort();
            this.defaultDirectory = new File(config.getDefaultDirectory());
            this.currentDirectory = defaultDirectory;

            frame = new JFrame("File Transfer GUI");
            cardLayout = new CardLayout();
            mainPanel = new JPanel(cardLayout);
            logArea = new JTextArea();

            JPanel choicePanel = createChoicePanel();
            JPanel uploadPanel = createUploadPanel();
            JPanel downloadPanel = createDownloadPanel();

            mainPanel.add(choicePanel, "CHOICE");
            mainPanel.add(uploadPanel, "UPLOAD");
            mainPanel.add(downloadPanel, "DOWNLOAD");

            frame.add(mainPanel);
            frame.setSize(600, 400);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);

            cardLayout.show(mainPanel, "CHOICE");
        }

        private JPanel createChoicePanel() {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            JLabel label = new JLabel("Que voulez-vous choisir ?");
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            label.setFont(new Font("Arial", Font.BOLD, 18));

            JButton uploadButton = new JButton("Upload");
            uploadButton.setPreferredSize(new Dimension(100, 50));
            uploadButton.setMaximumSize(new Dimension(100, 50));
            uploadButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            uploadButton.addActionListener(e -> cardLayout.show(mainPanel, "UPLOAD"));

            JButton downloadButton = new JButton("Download");
            downloadButton.setPreferredSize(new Dimension(100, 50));
            downloadButton.setMaximumSize(new Dimension(100, 50));
            downloadButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            downloadButton.addActionListener(e -> cardLayout.show(mainPanel, "DOWNLOAD"));

            panel.add(Box.createVerticalGlue());
            panel.add(label);
            panel.add(Box.createRigidArea(new Dimension(0, 20)));
            panel.add(uploadButton);
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
            panel.add(downloadButton);
            panel.add(Box.createVerticalGlue());

            return panel;
        }

        private JPanel createUploadPanel() {
            JPanel panel = new JPanel(new BorderLayout());
        
            JLabel label = new JLabel("Fichiers disponibles pour l'upload :");
            label.setFont(new Font("Arial", Font.PLAIN, 16));
        
            listModel = new DefaultListModel<>();
            // defaultDirectory = new File("RepClient"); // Répertoire par défaut
            // currentDirectory = defaultDirectory;
            updateFileList();
        
            JList<String> fileList = new JList<>(listModel);
            JScrollPane scrollPane = new JScrollPane(fileList);
        
            JButton uploadButton = new JButton("Uploader");
            uploadButton.addActionListener(e -> {
                String selectedFile = fileList.getSelectedValue();
                if (selectedFile != null) {
                    handleUpload(new File(currentDirectory, selectedFile).getAbsolutePath());
                } else {
                    log("Aucun fichier sélectionné pour l'upload.");
                }
            });
        
            JButton switchButton = new JButton("Changer de répertoire");
            switchButton.addActionListener(e -> {
                if (currentDirectory.getName().equals("RepClient")) {
                    currentDirectory = new File("RepDownload");
                } else {
                    currentDirectory = new File("RepClient");
                }
                updateFileList();
            });
        
            JButton deleteButton = new JButton("Supprimer");
            deleteButton.addActionListener(e -> {
                String selectedFile = fileList.getSelectedValue();
                if (selectedFile != null) {
                    File fileToDelete = new File(currentDirectory, selectedFile);
                    if (fileToDelete.exists() && fileToDelete.delete()) {
                        log("Fichier supprimé : " + selectedFile);
                        updateFileList(); // Met à jour la liste des fichiers affichés
                    } else {
                        log("Échec de la suppression du fichier : " + selectedFile);
                    }
                } else {
                    log("Aucun fichier sélectionné pour suppression.");
                }
            });
        
            JButton backButton = new JButton("Retour");
            backButton.addActionListener(e -> cardLayout.show(mainPanel, "CHOICE"));
        
            JPanel buttonPanel = new JPanel();
            buttonPanel.add(uploadButton);
            buttonPanel.add(switchButton);
            buttonPanel.add(deleteButton); // Ajout du bouton de suppression
            buttonPanel.add(backButton);
        
            panel.add(label, BorderLayout.NORTH);
            panel.add(scrollPane, BorderLayout.CENTER);
            panel.add(buttonPanel, BorderLayout.SOUTH);
        
            return panel;
        }
        
        /*private JPanel createDownloadPanel() {
            JPanel panel = new JPanel(new BorderLayout());

            JLabel label = new JLabel("Fichiers disponibles pour le download :");
            label.setFont(new Font("Arial", Font.PLAIN, 16));

            File slaveServerDir = new File("../SlaveServer");
            File[] slaveDirs = {
                new File(slaveServerDir, "slave1_storage"),
                new File(slaveServerDir, "slave2_storage"),
                new File(slaveServerDir, "slave3_storage")
            };
            Set<String> reconstructedFiles = new HashSet<>();
            for (File slaveDir : slaveDirs) {
                if (slaveDir.exists() && slaveDir.isDirectory()) {
                    for (File file : slaveDir.listFiles()) {
                        String fileName = file.getName();
                        if (fileName.startsWith("block_")) {
                            String originalName = fileName.split("_temp_")[1];
                            reconstructedFiles.add(originalName);
                        }
                    }
                }
            }

            DefaultListModel<String> listModel = new DefaultListModel<>();
            for (String fileName : reconstructedFiles) {
                listModel.addElement(fileName);
            }

            JList<String> fileList = new JList<>(listModel);
            JScrollPane scrollPane = new JScrollPane(fileList);

            JButton downloadButton = new JButton("Télécharger");
            downloadButton.addActionListener(e -> {
                String selectedFile = fileList.getSelectedValue();
                if (selectedFile != null) {
                    System.out.println("Downloading: " + selectedFile);
                    // Appeler la méthode pour le download ici
                    handleDownload(selectedFile);
                } else {
                    // JOptionPane.showMessageDialog(frame, "Veuillez sélectionner un fichier à télécharger.", "Erreur", JOptionPane.ERROR_MESSAGE);
                    log("No file selected for download.");
                }
            });

            JButton backButton = new JButton("Retour");
            backButton.addActionListener(e -> cardLayout.show(mainPanel, "CHOICE"));

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(downloadButton);
            buttonPanel.add(backButton);

            panel.add(label, BorderLayout.NORTH);
            panel.add(scrollPane, BorderLayout.CENTER);
            panel.add(buttonPanel, BorderLayout.SOUTH);

            return panel;
        }*/

        /*private JPanel createDownloadPanel() {
            JPanel panel = new JPanel(new BorderLayout());
        
            JLabel label = new JLabel("Fichiers disponibles pour le download :");
            label.setFont(new Font("Arial", Font.PLAIN, 16));
        
            // Récupérer les fichiers depuis le serveur
            Set<String> serverFiles = getFilesFromServer();
            DefaultListModel<String> listModel = new DefaultListModel<>();
            for (String fileName : serverFiles) {
                listModel.addElement(fileName);
            }
        
            JList<String> fileList = new JList<>(listModel);
            JScrollPane scrollPane = new JScrollPane(fileList);
        
            JButton downloadButton = new JButton("Télécharger");
            downloadButton.addActionListener(e -> {
                String selectedFile = fileList.getSelectedValue();
                if (selectedFile != null) {
                    handleDownload(selectedFile);
                } else {
                    log("Aucun fichier sélectionné pour téléchargement.");
                }
            });
        
            JButton backButton = new JButton("Retour");
            backButton.addActionListener(e -> cardLayout.show(mainPanel, "CHOICE"));
        
            JPanel buttonPanel = new JPanel();
            buttonPanel.add(downloadButton);
            buttonPanel.add(backButton);
        
            panel.add(label, BorderLayout.NORTH);
            panel.add(scrollPane, BorderLayout.CENTER);
            panel.add(buttonPanel, BorderLayout.SOUTH);
        
            return panel;
        }*/
        
        private JPanel createDownloadPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            
            JLabel label = new JLabel("Fichiers disponibles pour le téléchargement :");
            label.setFont(new Font("Arial", Font.PLAIN, 16));
            
            // Créez un modèle de liste mais ne le remplissez pas immédiatement
            DefaultListModel<String> listModel = new DefaultListModel<>();
            JList<String> fileList = new JList<>(listModel);
            JScrollPane scrollPane = new JScrollPane(fileList);
        
            // Ajoutez un bouton pour le téléchargement
            JButton downloadButton = new JButton("Télécharger");
            downloadButton.addActionListener(e -> {
                String selectedFile = fileList.getSelectedValue();
                if (selectedFile != null) {
                    handleDownload(selectedFile);
                } else {
                    log("Aucun fichier sélectionné pour le téléchargement.");
                }
            });
        
            // Bouton de retour
            JButton backButton = new JButton("Retour");
            backButton.addActionListener(e -> cardLayout.show(mainPanel, "CHOICE"));
        
            // Panneau pour les boutons
            JPanel buttonPanel = new JPanel();
            buttonPanel.add(downloadButton);
            buttonPanel.add(backButton);
        
            // Ajoutez les composants au panneau principal
            panel.add(label, BorderLayout.NORTH);
            panel.add(scrollPane, BorderLayout.CENTER);
            panel.add(buttonPanel, BorderLayout.SOUTH);
        
            // Ajoutez un hook pour actualiser la liste des fichiers quand le panneau est affiché
            panel.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentShown(ComponentEvent e) {
                    updateFileList(listModel);
                }
            });
        
            return panel;
        }
        

        private void handleUpload(String filePath) {
            File file = new File(filePath);

            if (!file.exists()) {
                log("File not found: " + filePath);
                return;
            }

            try (Socket socket = new Socket(MASTER_ADDRESS, MASTER_PORT);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                FileInputStream fis = new FileInputStream(file)) {

                dos.writeUTF("UPLOAD");
                dos.writeUTF(file.getName());
                dos.writeLong(file.length());

                byte[] buffer = new byte[4096];
                int read;
                while ((read = fis.read(buffer)) > 0) {
                    dos.write(buffer, 0, read);
                }

                String response = dis.readUTF();
                log("Server response: " + response);

            } catch (IOException ex) {
                log("Error during upload: " + ex.getMessage());
            }
        }

        private void handleDownload(String fileName) {
            // Répertoire de téléchargement
            File downloadDir = new File("RepDownload");
        
            // Vérifie si le répertoire existe, sinon le crée
            if (!downloadDir.exists()) {
                if (!downloadDir.mkdirs()) {
                    log("Erreur : Impossible de créer le répertoire de téléchargement.");
                    return;
                }
            }
        
            // Crée le fichier dans le répertoire RepDownload
            File downloadedFile = new File(downloadDir, "downloaded_" + fileName);
            
            try (Socket socket = new Socket(MASTER_ADDRESS, MASTER_PORT);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                FileOutputStream fos = new FileOutputStream(downloadedFile)) {
        
                dos.writeUTF("DOWNLOAD");
                dos.writeUTF(fileName);
        
                long fileSize = dis.readLong();
                byte[] buffer = new byte[4096];
                int read;
                long totalRead = 0;
        
                while (totalRead < fileSize && (read = dis.read(buffer)) > 0) {
                    fos.write(buffer, 0, read);
                    totalRead += read;
                }
        
                log("Fichier téléchargé avec succès dans : " + downloadedFile.getAbsolutePath());
        
            } catch (IOException ex) {
                log("Erreur lors du téléchargement : " + ex.getMessage());
            }
        }
        
        private Set<String> getFilesFromServer() {
            Set<String> files = new HashSet<>();
            try (Socket socket = new Socket(MASTER_ADDRESS, MASTER_PORT);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream())) {
        
                // Envoie la commande "LIST_FILES" au serveur
                dos.writeUTF("LIST_FILES");
        
                // Lit le nombre de fichiers
                int fileCount = dis.readInt();
                for (int i = 0; i < fileCount; i++) {
                    files.add(dis.readUTF());
                }
        
            } catch (IOException ex) {
                log("Erreur lors de la récupération des fichiers depuis le serveur : " + ex.getMessage());
            }
            return files;
        }
        

        private void log(String message) {
            logArea.append(message + "\n");
        }

        private void updateFileList() {
            listModel.clear(); // Vider le modèle actuel
            if (currentDirectory.exists() && currentDirectory.isDirectory()) {
                for (File file : currentDirectory.listFiles()) {
                    if (file.isFile()) 
                        listModel.addElement(file.getName()); // Ajouter chaque fichier au modèle
                }
            } else 
                log("Répertoire introuvable : " + currentDirectory.getAbsolutePath());
        }

        private void updateFileList(DefaultListModel<String> listModel) {
            // Efface l'ancien contenu
            listModel.clear();
        
            // Récupère les fichiers depuis le serveur
            Set<String> serverFiles = getFilesFromServer();
        
            // Ajoute les fichiers récupérés au modèle
            for (String fileName : serverFiles) {
                listModel.addElement(fileName);
            }
        }
        
        

        public static void main(String[] args) {
            SwingUtilities.invokeLater(FileTransferGUI::new);
        }
    }
