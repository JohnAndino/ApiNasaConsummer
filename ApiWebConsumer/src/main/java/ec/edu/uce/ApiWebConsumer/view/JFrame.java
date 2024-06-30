package ec.edu.uce.ApiWebConsumer.view;

import ec.edu.uce.ApiWebConsumer.entities.Camera;
import ec.edu.uce.ApiWebConsumer.entities.Photo;
import ec.edu.uce.ApiWebConsumer.entities.Rover;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class JFrame {

    private static List<Photo> allPhotos = new ArrayList<>();
    private static JTable jTable;
    private static JComboBox<String> cameraComboBox;
    private static JComboBox<String> roverComboBox;
    private static JLabel messageLabel;
    private static javax.swing.JFrame frame;
    private static JLabel imageLabel;

    public static void createAndShowGUI() {
        frame = new javax.swing.JFrame("Mars Rover Photos");
        frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel filterPanel = new JPanel(new BorderLayout());
        tabbedPane.addTab("Filtros", filterPanel);

        JPanel topPanel = new JPanel();
        filterPanel.add(topPanel, BorderLayout.NORTH);

        JLabel cameraLabel = new JLabel("Cámara:");
        topPanel.add(cameraLabel);

        cameraComboBox = new JComboBox<>();
        topPanel.add(cameraComboBox);

        JLabel roverLabel = new JLabel("Rover:");
        topPanel.add(roverLabel);

        roverComboBox = new JComboBox<>();
        topPanel.add(roverComboBox);

        JLabel filterTypeLabel = new JLabel("Tipo de Filtro:");
        topPanel.add(filterTypeLabel);

        JComboBox<String> filterTypeComboBox = new JComboBox<>(new String[]{"Secuencial", "Paralelo"});
        topPanel.add(filterTypeComboBox);

        JButton filterButton = new JButton("Aplicar filtros");
        topPanel.add(filterButton);

        jTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(jTable);
        filterPanel.add(scrollPane, BorderLayout.CENTER);

        messageLabel = new JLabel("Para visualizar las imagenes primero filtrelas haciendo click en filters", SwingConstants.CENTER);
        filterPanel.add(messageLabel, BorderLayout.CENTER);

        // Crea la tabla "Visualizar Foto"
        JPanel visualizePanel = new JPanel(new BorderLayout());
        imageLabel = new JLabel("", SwingConstants.CENTER);
        visualizePanel.add(imageLabel, BorderLayout.CENTER);
        tabbedPane.addTab("Visualizar Foto", visualizePanel);

        frame.getContentPane().add(tabbedPane);

        fetchPhotosFromApi();

        filterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedCamera = (String) cameraComboBox.getSelectedItem();
                String selectedRover = (String) roverComboBox.getSelectedItem();
                String selectedFilterType = (String) filterTypeComboBox.getSelectedItem();
                applyFilters(selectedCamera, selectedRover, selectedFilterType);
            }
        });

        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 1) { // "Visualizar Foto" tab
                int selectedRow = jTable.getSelectedRow();
                if (selectedRow != -1) {
                    String imageUrl = (String) jTable.getValueAt(selectedRow, 3);
                    loadImageAsync(imageUrl, imageLabel);
                } else {
                    imageLabel.setText("Seleccione una imagen de la tabla para visualizarla.");
                }
            }
        });

        jTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && jTable.getSelectedRow() != -1) {
                    String imageUrl = (String) jTable.getValueAt(jTable.getSelectedRow(), 3);
                    loadImageAsync(imageUrl, imageLabel);
                }
            }
        });

        frame.setVisible(true);
    }

    private static void fetchPhotosFromApi() {
        try {
            URL url = new URL("https://api.nasa.gov/mars-photos/api/v1/rovers/curiosity/photos?sol=1000&api_key=DEMO_KEY");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Deserializa el JSON manualmente
                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray photosArray = jsonResponse.getJSONArray("photos");

                for (int i = 0; i < photosArray.length(); i++) {
                    JSONObject photoJson = photosArray.getJSONObject(i);
                    Photo photo = new Photo();
                    photo.setId(photoJson.getInt("id"));
                    photo.setImg_src(photoJson.getString("img_src"));

                    JSONObject cameraJson = photoJson.getJSONObject("camera");
                    Camera camera = new Camera();
                    camera.setName(cameraJson.getString("name"));
                    photo.setCamera(camera);

                    JSONObject roverJson = photoJson.getJSONObject("rover");
                    Rover rover = new Rover();
                    rover.setName(roverJson.getString("name"));
                    photo.setRover(rover);

                    allPhotos.add(photo);
                }

                populateFilterOptions();

            } else {
                throw new RuntimeException("Error al conectarnos a la API Code :" + responseCode);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void populateFilterOptions() {
        SwingUtilities.invokeLater(() -> {
            cameraComboBox.addItem("");
            roverComboBox.addItem("");

            for (Photo photo : allPhotos) {
                if (photo.getCamera() != null && photo.getCamera().getName() != null) {
                    if (cameraComboBox.getItemCount() == 0 || !itemExistsInComboBox(cameraComboBox, photo.getCamera().getName())) {
                        cameraComboBox.addItem(photo.getCamera().getName());
                    }
                }
                if (photo.getRover() != null && photo.getRover().getName() != null) {
                    if (roverComboBox.getItemCount() == 0 || !itemExistsInComboBox(roverComboBox, photo.getRover().getName())) {
                        roverComboBox.addItem(photo.getRover().getName());
                    }
                }
            }
        });
    }

    private static boolean itemExistsInComboBox(JComboBox<String> comboBox, String item) {
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            if (comboBox.getItemAt(i).equals(item)) {
                return true;
            }
        }
        return false;
    }

    private static void applyFilters(String cameraName, String roverName, String filterType) {
        SwingWorker<List<Photo>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Photo> doInBackground() {
                if ("Paralelo".equals(filterType)) {
                    return allPhotos.parallelStream()
                            .filter(photo -> cameraName.equals(photo.getCamera().getName()) || cameraName.isEmpty())
                            .filter(photo -> roverName.equals(photo.getRover().getName()) || roverName.isEmpty())
                            .collect(Collectors.toList());
                } else {
                    return allPhotos.stream()
                            .filter(photo -> cameraName.equals(photo.getCamera().getName()) || cameraName.isEmpty())
                            .filter(photo -> roverName.equals(photo.getRover().getName()) || roverName.isEmpty())
                            .collect(Collectors.toList());
                }
            }

            @Override
            protected void done() {
                try {
                    List<Photo> filteredPhotos = get();
                    updateTable(filteredPhotos);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private static void updateTable(List<Photo> photos) {
        SwingUtilities.invokeLater(() -> {
            String[] columnNames = {"ID", "Camera", "Rover", "Image URL"};
            String[][] data = new String[photos.size()][4];

            for (int i = 0; i < photos.size(); i++) {
                Photo photo = photos.get(i);
                data[i][0] = String.valueOf(photo.getId());
                data[i][1] = photo.getCamera().getName();
                data[i][2] = photo.getRover() != null ? photo.getRover().getName() : "N/A";
                data[i][3] = photo.getImg_src();
            }

            jTable.setModel(new DefaultTableModel(data, columnNames));
            messageLabel.setVisible(false);
        });
    }

    private static void loadImageAsync(String imageUrl, JLabel imageLabel) {
        SwingWorker<ImageIcon, Void> worker = new SwingWorker<>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                return new ImageIcon(new URL(imageUrl));
            }

            @Override
            protected void done() {
                try {
                    ImageIcon imageIcon = get();
                    imageLabel.setIcon(imageIcon);
                    imageLabel.setText("");
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
}
