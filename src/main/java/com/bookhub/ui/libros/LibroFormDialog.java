package main.java.com.bookhub.ui.libros;

import com.bookhub.dto.LibroRequest;
import com.bookhub.dto.LibroResponse;
import com.bookhub.ui.common.ApiClient;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Dialogo para crear/editar libros con validaciones locales y manejo de errores de API.
 */
public class LibroFormDialog extends JDialog {

    private static final List<String> CATEGORIAS_BASE = Arrays.asList(
        "Literatura",
        "Tecnologia",
        "Ciencia",
        "Historia",
        "Infantil",
        "Arte",
        "Otro"
    );

    private final JTextField isbnField = new JTextField(20);
    private final JTextField tituloField = new JTextField(40);
    private final JTextField autorField = new JTextField(40);
    private final JComboBox<String> categoriaCombo = new JComboBox<>();
    private final JCheckBox disponibleCheck = new JCheckBox("Disponible para prestamo", true);

    private final ApiClient api;
    private final LibroResponse editing;
    private final Runnable onSaved;
    private final Set<String> existingIsbns;

    public LibroFormDialog(Frame owner, ApiClient api, LibroResponse libro, List<LibroResponse> actuales, Runnable onSaved) {
        super(owner, true);
        this.api = api;
        this.editing = libro;
        this.onSaved = onSaved;
        this.existingIsbns = new HashSet<>();
        if (actuales != null) {
            actuales.stream()
                .map(LibroResponse::getIsbn)
                .filter(isbn -> isbn != null && !isbn.isBlank())
                .forEach(existingIsbns::add);
        }

        setTitle(libro == null ? "Registrar libro" : "Editar libro");
        initComponents();
        if (libro != null) {
            populate(libro);
        }
        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // Cargar combos
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("Seleccione una categoria");
        CATEGORIAS_BASE.forEach(model::addElement);
        categoriaCombo.setModel(model);

        JPanel center = new JPanel(new GridLayout(0, 2, 8, 8));
        center.add(new JLabel("ISBN:"));
        center.add(isbnField);
        center.add(new JLabel("Titulo:"));
        center.add(tituloField);
        center.add(new JLabel("Autor:"));
        center.add(autorField);
        center.add(new JLabel("Categoria:"));
        center.add(categoriaCombo);
        center.add(new JLabel("Estado:"));
        center.add(disponibleCheck);

        add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        JButton btnSave = new JButton("Guardar");
        JButton btnCancel = new JButton("Cancelar");
        bottom.add(btnSave);
        bottom.add(btnCancel);
        add(bottom, BorderLayout.SOUTH);

        btnSave.addActionListener(e -> onSave());
        btnCancel.addActionListener(e -> dispose());

        setFocusTraversalPolicy(new java.awt.DefaultFocusTraversalPolicy());
    }

    private void populate(LibroResponse libro) {
        isbnField.setText(libro.getIsbn());
        isbnField.setEnabled(false); // No se permite cambiar ISBN existente
        tituloField.setText(libro.getTitulo());
        autorField.setText(libro.getAutor());
        disponibleCheck.setSelected(libro.isDisponible());

        if (libro.getCategoria() != null && !libro.getCategoria().isBlank()) {
            boolean exists = CATEGORIAS_BASE.stream().anyMatch(cat -> cat.equalsIgnoreCase(libro.getCategoria()));
            if (!exists) {
                categoriaCombo.addItem(libro.getCategoria());
            }
            categoriaCombo.setSelectedItem(libro.getCategoria());
        } else {
            categoriaCombo.setSelectedIndex(0);
        }
    }

    private void onSave() {
        String isbn = isbnField.getText().trim();
        String titulo = tituloField.getText().trim();
        String autor = autorField.getText().trim();
        Object selectedCategoria = categoriaCombo.getSelectedItem();
        String categoria = selectedCategoria != null ? selectedCategoria.toString() : "";
        boolean disponible = disponibleCheck.isSelected();

        // Validaciones locales
        if (isbn.isBlank()) {
            showValidation("El ISBN es obligatorio.");
            isbnField.requestFocus();
            return;
        }
        if (editing == null && existingIsbns.contains(isbn)) {
            showValidation("Ya existe un libro con el ISBN indicado.");
            isbnField.requestFocus();
            return;
        }
        if (titulo.isBlank()) {
            showValidation("El titulo es obligatorio.");
            tituloField.requestFocus();
            return;
        }
        if (autor.isBlank()) {
            showValidation("El autor es obligatorio.");
            autorField.requestFocus();
            return;
        }
        if (categoriaCombo.getSelectedIndex() <= 0) {
            showValidation("Seleccione una categoria valida.");
            categoriaCombo.requestFocus();
            return;
        }

        LibroRequest request = new LibroRequest();
        request.setIsbn(isbn);
        request.setTitulo(titulo);
        request.setAutor(autor);
        request.setCategoria(categoria);
        request.setDisponible(disponible);

        try {
            ApiClient.ApiResponse resp;
            if (editing == null) {
                resp = api.post("/libros", request);
            } else {
                resp = api.put("/libros/" + editing.getIsbn(), request);
            }
            if (resp.isSuccess()) {
                JOptionPane.showMessageDialog(this, "Libro guardado correctamente.");
                if (onSaved != null) {
                    onSaved.run();
                }
                dispose();
            } else {
                handleError(resp);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error de comunicacion: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleError(ApiClient.ApiResponse resp) {
        String msg = resp.body != null && !resp.body.isBlank() ? resp.body : "Revise el log del servidor.";
        switch (resp.status) {
            case 400 -> JOptionPane.showMessageDialog(this, "Solicitud invalida (400): " + msg, "Validacion API", JOptionPane.WARNING_MESSAGE);
            case 404 -> JOptionPane.showMessageDialog(this, "Libro no encontrado (404): " + msg, "No encontrado", JOptionPane.WARNING_MESSAGE);
            case 409 -> JOptionPane.showMessageDialog(this, "Conflicto (409): " + msg, "Duplicado", JOptionPane.WARNING_MESSAGE);
            case 500 -> JOptionPane.showMessageDialog(this, "Error interno (500): " + msg, "Servidor", JOptionPane.ERROR_MESSAGE);
            default -> JOptionPane.showMessageDialog(this, "Error (" + resp.status + "): " + msg, "API", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showValidation(String message) {
        JOptionPane.showMessageDialog(this, message, "Validacion", JOptionPane.WARNING_MESSAGE);
    }
}
