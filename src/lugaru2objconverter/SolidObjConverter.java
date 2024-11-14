package lugaru2objconverter;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SolidObjConverter extends JFrame {
    private static final long serialVersionUID = 1L;
    private JTextField solidFileField, objFileField;
    private JComboBox<String> modelTypeComboBox;
    private JLabel detectedTypeLabel, statusLabel;
    
    private enum ModelType {
        BODY, WEAPON, IMMO
    }

    public SolidObjConverter() {
        setTitle("Solid-Obj Converter");
        setSize(500, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        setResizable(false);

        // Solid to Obj Panel
        JPanel solidToObjPanel = createPanel(".solid to obj conversion");
        solidFileField = new JTextField(20);
        enableDragAndDrop(solidFileField);
        JButton browseSolidButton = new JButton("Browse");
        browseSolidButton.addActionListener(e -> selectFile(solidFileField, "solid"));
        JButton convertToObjButton = new JButton("Convert to OBJ");
        convertToObjButton.addActionListener(e -> solidToObj(solidFileField.getText()));
        detectedTypeLabel = new JLabel("Detected type: None");

        // Add components to solidToObjPanel
        solidToObjPanel.add(createFileRow(solidFileField, browseSolidButton));
        solidToObjPanel.add(createLabelRow(detectedTypeLabel, convertToObjButton));

        // Obj to Solid Panel
        JPanel objToSolidPanel = createPanel("obj to .solid conversion");
        objFileField = new JTextField(20);
        enableDragAndDrop(objFileField);
        JButton browseObjButton = new JButton("Browse");
        browseObjButton.addActionListener(e -> selectFile(objFileField, "obj"));
        JButton convertToSolidButton = new JButton("Convert to .solid");
        convertToSolidButton.addActionListener(e -> objToSolid(objFileField.getText()));

        // Model Type Selector
        modelTypeComboBox = new JComboBox<>(new String[]{"BODY", "WEAPON", "IMMO"});
        modelTypeComboBox.setSelectedIndex(2); // Default to "IMMOVABLE"

        // Add components to objToSolidPanel
        objToSolidPanel.add(createFileRow(objFileField, browseObjButton));
        objToSolidPanel.add(createLabelRow(new JLabel("Select type:"), modelTypeComboBox, convertToSolidButton));

        // Status/Error label
        statusLabel = new JLabel("---");
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add panels to the main frame
        add(solidToObjPanel);
        add(objToSolidPanel);
        add(statusLabel);
    }

    private JPanel createPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(titleLabel);
        return panel;
    }

    private JPanel createFileRow(JTextField textField, JButton browseButton) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(textField);
        panel.add(browseButton);
        return panel;
    }

    private JPanel createLabelRow(JLabel label, JComponent rightComponent) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(label);
        panel.add(Box.createHorizontalStrut(20)); // Adds spacing
        panel.add(rightComponent);
        return panel;
    }

    private JPanel createLabelRow(JLabel label, JComponent middleComponent, JComponent rightComponent) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(label);
        panel.add(middleComponent);
        panel.add(Box.createHorizontalStrut(20)); // Adds spacing
        panel.add(rightComponent);
        return panel;
    }

    private void enableDragAndDrop(JTextField textField) {
        textField.setDropTarget(new DropTarget() {
			private static final long serialVersionUID = 1L;
			public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(evt.getDropAction());
                    List<?> droppedFiles = (List<?>) evt.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    if (!droppedFiles.isEmpty()) {
                        File file = (File) droppedFiles.get(0);
                        textField.setText(file.getAbsolutePath());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void selectFile(JTextField textField, String extension) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter(extension.toUpperCase() + " Files", extension));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            textField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private ModelType detectModelType(int vertexCount) {
        if (vertexCount > 100) {
            return ModelType.BODY;
        } else if (vertexCount > 20) {
            return ModelType.WEAPON;
        } else {
            return ModelType.IMMO;
        }
    }

    private void solidToObj(String solidFilePath) {
        String objFilePath = solidFilePath.replace(".solid", ".obj");
        try (DataInputStream dis = new DataInputStream(new FileInputStream(solidFilePath));
             PrintWriter writer = new PrintWriter(new FileWriter(objFilePath))) {

            int vertexCount = dis.readUnsignedShort();
            int triangleCount = dis.readUnsignedShort();
            ModelType detectedType = detectModelType(vertexCount);
            detectedTypeLabel.setText("Detected type: " + detectedType);

            float scaleFactor = detectedType == ModelType.BODY ? 1.0f : (detectedType == ModelType.WEAPON ? 1.0f : 1.0f);//Scale used to vary, Keeping this here in case it needs to be changed again
            float invertY = detectedType == ModelType.WEAPON ? 1.0f : -1.0f;

            List<float[]> vertices = new ArrayList<>();
            for (int i = 0; i < vertexCount; i++) {
                float x = dis.readFloat() * scaleFactor;
                float y = dis.readFloat() * invertY * scaleFactor;
                float z = dis.readFloat() * scaleFactor;
                vertices.add(new float[]{x, y, z});
                writer.printf("v %f %f %f\n", x, y, z);
            }

            List<float[]> uvs = new ArrayList<>();
            for (int i = 0; i < triangleCount; i++) {
                int[] indices = new int[6];
                for (int j = 0; j < 6; j++) {
                    indices[j] = dis.readUnsignedShort();
                }
                for (int j = 0; j < 3; j++) {
                    float u = dis.readFloat();
                    float v = 1.0f - dis.readFloat();
                    uvs.add(new float[]{u, v});
                    writer.printf("vt %f %f\n", u, v);
                }
                writer.printf("f %d/%d %d/%d %d/%d\n",
                        indices[0] + 1, indices[0] + 1,
                        indices[2] + 1, indices[2] + 1,
                        indices[4] + 1, indices[4] + 1);
            }
            
            // Display success message with bold filename
            String fileName = new File(objFilePath).getName();
            statusLabel.setText("<html>Successfully converted to <b>" + fileName + "</b>!</html>");
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error converting .solid to .obj");
        }
    }

    private void objToSolid(String objFilePath) {
        String solidFilePath = objFilePath.replace(".obj", ".solid");
        ModelType selectedType = ModelType.valueOf((String) modelTypeComboBox.getSelectedItem());

        float scaleFactor = 1.0f; // Keeping consistent with solidToObj function
        float invertY = selectedType == ModelType.WEAPON ? 1.0f : -1.0f;

        try (BufferedReader reader = new BufferedReader(new FileReader(objFilePath));
             DataOutputStream dos = new DataOutputStream(new FileOutputStream(solidFilePath))) {

            List<float[]> vertices = new ArrayList<>();
            List<float[]> uvs = new ArrayList<>();
            List<int[]> triangles = new ArrayList<>();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                switch (parts[0]) {
                    case "v":
                        // Apply scale factor and Y inversion to match solidToObj conversion
                        vertices.add(new float[]{
                                Float.parseFloat(parts[1]) * scaleFactor,
                                Float.parseFloat(parts[2]) * invertY * scaleFactor,
                                Float.parseFloat(parts[3]) * scaleFactor
                        });
                        break;
                    case "vt":
                        uvs.add(new float[]{Float.parseFloat(parts[1]), 1.0f - Float.parseFloat(parts[2])});
                        break;
                    case "f":
                        if (parts.length > 4) {
                            throw new IllegalArgumentException("Only triangular faces are supported in .solid format.");
                        }
                        int[] indices = new int[3];
                        for (int i = 0; i < 3; i++) {
                            indices[i] = Integer.parseInt(parts[i + 1].split("/")[0]) - 1;
                        }
                        triangles.add(indices);
                        break;
                }
            }

            dos.writeShort(vertices.size());
            dos.writeShort(triangles.size());

            for (float[] vertex : vertices) {
                dos.writeFloat(vertex[0]);
                dos.writeFloat(vertex[1]);
                dos.writeFloat(vertex[2]);
            }

            for (int[] triangle : triangles) {
                for (int idx : triangle) {
                    dos.writeShort(idx);
                }
                for (int i = 0; i < 3; i++) {
                    float[] uv = uvs.get(triangle[i]);
                    dos.writeFloat(uv[0]);
                    dos.writeFloat(uv[1]);
                }
            }

            // Display success message with bold filename
            String fileName = new File(solidFilePath).getName();
            statusLabel.setText("<html>Successfully converted to <b>" + fileName + "</b>!</html>");
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error converting .obj to .solid");
        } catch (IllegalArgumentException e) {
            statusLabel.setText(e.getMessage());
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SolidObjConverter converter = new SolidObjConverter();
            converter.setVisible(true);
        });
    }
}

