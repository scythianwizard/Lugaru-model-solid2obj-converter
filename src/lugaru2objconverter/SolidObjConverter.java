package lugaru2objconverter;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SolidObjConverter extends JFrame {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextField solidFileField;
    private JTextField objFileField;
    
    public SolidObjConverter() {
        setTitle("Solid-Obj Converter");
        setSize(500, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(3, 1));

        // Solid to Obj Panel
        JPanel solidToObjPanel = new JPanel();
        solidToObjPanel.setLayout(new FlowLayout());
        solidFileField = new JTextField(20);
        JButton browseSolidButton = new JButton("Browse Solid");
        browseSolidButton.addActionListener(e -> selectFile(solidFileField, "solid"));
        JButton convertToObjButton = new JButton("Convert to Obj");
        convertToObjButton.addActionListener(e -> solidToObj(solidFileField.getText()));
        solidToObjPanel.add(new JLabel(".solid File:"));
        solidToObjPanel.add(solidFileField);
        solidToObjPanel.add(browseSolidButton);
        solidToObjPanel.add(convertToObjButton);

        // Obj to Solid Panel
        JPanel objToSolidPanel = new JPanel();
        objToSolidPanel.setLayout(new FlowLayout());
        objFileField = new JTextField(20);
        JButton browseObjButton = new JButton("Browse Obj");
        browseObjButton.addActionListener(e -> selectFile(objFileField, "obj"));
        JButton convertToSolidButton = new JButton("Convert to Solid");
        convertToSolidButton.addActionListener(e -> objToSolid(objFileField.getText()));
        objToSolidPanel.add(new JLabel(".obj File:"));
        objToSolidPanel.add(objFileField);
        objToSolidPanel.add(browseObjButton);
        objToSolidPanel.add(convertToSolidButton);

        add(solidToObjPanel);
        add(objToSolidPanel);
    }

    private void selectFile(JTextField textField, String extension) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter(extension.toUpperCase() + " Files", extension));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            textField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void solidToObj(String solidFilePath) {
        String objFilePath = solidFilePath.replace(".solid", ".obj");
        try (DataInputStream dis = new DataInputStream(new FileInputStream(solidFilePath));
             PrintWriter writer = new PrintWriter(new FileWriter(objFilePath))) {
            int vertexCount = dis.readUnsignedShort();
            int triangleCount = dis.readUnsignedShort();

         // Read vertices
            List<float[]> vertices = new ArrayList<>();
            for (int i = 0; i < vertexCount; i++) {
                float x = dis.readFloat();
                float y = -dis.readFloat(); // Invert Y coordinate to correct the model's orientation
                float z = dis.readFloat();
                vertices.add(new float[]{x, y, z});
                writer.printf("v %f %f %f\n", x, y, z);
            }

            // Read triangles and UVs
            List<float[]> uvs = new ArrayList<>();
            for (int i = 0; i < triangleCount; i++) {
                int[] indices = new int[6];
                for (int j = 0; j < 6; j++) {
                    indices[j] = dis.readUnsignedShort();
                }
                for (int j = 0; j < 3; j++) { // Only 3 pairs are valid for OBJ
                    float u = dis.readFloat();
                    float v = 1.0f - dis.readFloat();  // invert v
                    uvs.add(new float[]{u, v});
                    writer.printf("vt %f %f\n", u, v);
                }
                writer.printf("f %d/%d %d/%d %d/%d\n", 
                    indices[0] + 1, indices[0] + 1, 
                    indices[2] + 1, indices[2] + 1, 
                    indices[4] + 1, indices[4] + 1); // Mapping vertices to face
            }
            JOptionPane.showMessageDialog(this, "Converted .solid to .obj successfully!");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error converting .solid to .obj", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void objToSolid(String objFilePath) {
        String solidFilePath = objFilePath.replace(".obj", ".solid");
        try (BufferedReader reader = new BufferedReader(new FileReader(objFilePath));
             DataOutputStream dos = new DataOutputStream(new FileOutputStream(solidFilePath))) {

            List<float[]> vertices = new ArrayList<>();
            List<float[]> uvs = new ArrayList<>();
            List<int[]> triangles = new ArrayList<>();

         // Parse .obj file
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                switch (parts[0]) {
                    case "v":
                        // Invert Y coordinate to match the correction in solidToObj
                        vertices.add(new float[]{Float.parseFloat(parts[1]), -Float.parseFloat(parts[2]), Float.parseFloat(parts[3])});
                        break;
                    case "vt":
                        uvs.add(new float[]{Float.parseFloat(parts[1]), 1.0f - Float.parseFloat(parts[2])}); // invert v
                        break;
                    case "f":
                        int[] indices = new int[3];
                        for (int i = 0; i < 3; i++) {
                            indices[i] = Integer.parseInt(parts[i + 1].split("/")[0]) - 1;
                        }
                        triangles.add(indices);
                        break;
                }
            }

            // Write header
            dos.writeShort(vertices.size());
            dos.writeShort(triangles.size());

            // Write vertices
            for (float[] vertex : vertices) {
                dos.writeFloat(vertex[0]);
                dos.writeFloat(vertex[1]);
                dos.writeFloat(vertex[2]);
            }

            // Write triangles and UVs
            for (int[] triangle : triangles) {
                for (int idx : triangle) {
                    dos.writeShort(idx);
                }
                for (int i = 0; i < 3; i++) { // Only write first 3 UVs
                    float[] uv = uvs.get(triangle[i]);
                    dos.writeFloat(uv[0]);
                    dos.writeFloat(uv[1]);
                }
            }

            JOptionPane.showMessageDialog(this, "Converted .obj to .solid successfully!");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error converting .obj to .solid", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SolidObjConverter converter = new SolidObjConverter();
            converter.setVisible(true);
        });
    }
}
