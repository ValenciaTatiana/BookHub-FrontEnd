package main.java.com.bookhub.ui.usuarios;

import com.bookhub.entity.Usuario;
import com.bookhub.ui.common.ApiClient;
import com.google.gson.Gson;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class UsuarioFormDialog extends JDialog {

    private final JTextField nombreField = new JTextField(30);
    private final JTextField emailField = new JTextField(30);
    private final JTextField telefonoField = new JTextField(20);

    private final ApiClient api;
    private final Usuario editing;
    private final Runnable onSaved;
    private final Gson gson = new Gson();

    public UsuarioFormDialog(Frame owner, ApiClient api, Usuario usuario, Runnable onSaved) {
        super(owner, true);
        this.api = api;
        this.editing = usuario;
        this.onSaved = onSaved;
        setTitle(usuario == null ? "Crear Usuario" : "Editar Usuario");
        init();
        if (usuario != null) populate(usuario);
        pack();
        setLocationRelativeTo(owner);
    }

    private void init() {
        setLayout(new BorderLayout(8,8));
        JPanel center = new JPanel(new GridLayout(0,2,6,6));
        center.add(new JLabel("Nombre:")); center.add(nombreField);
        center.add(new JLabel("Email:")); center.add(emailField);
        center.add(new JLabel("Teléfono:")); center.add(telefonoField);
        add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        JButton btnSave = new JButton("Guardar");
        JButton btnCancel = new JButton("Cancelar");
        bottom.add(btnSave); bottom.add(btnCancel);
        add(bottom, BorderLayout.SOUTH);

        btnSave.addActionListener(e -> onSave());
        btnCancel.addActionListener(e -> dispose());

        // focus order natural
        setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());
    }

    private void populate(Usuario u) {
        nombreField.setText(u.getNombre());
        emailField.setText(u.getEmail());
        telefonoField.setText(u.getTelefono());
    }

    private void onSave() {
        String nombre = nombreField.getText().trim();
        String email = emailField.getText().trim();
        String telefono = telefonoField.getText().trim();

        // Validaciones UI
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre es obligatorio.", "Validación", JOptionPane.WARNING_MESSAGE);
            nombreField.requestFocus();
            return;
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            JOptionPane.showMessageDialog(this, "El email no tiene formato válido.", "Validación", JOptionPane.WARNING_MESSAGE);
            emailField.requestFocus();
            return;
        }
        if (!telefono.matches("^\\d{7,15}$")) {
            JOptionPane.showMessageDialog(this, "El teléfono debe ser numérico (7-15 dígitos).", "Validación", JOptionPane.WARNING_MESSAGE);
            telefonoField.requestFocus();
            return;
        }

        Usuario u = new Usuario();
        if (editing != null) u.setId(editing.getId());
        u.setNombre(nombre);
        u.setEmail(email);
        u.setTelefono(telefono);

        try {
            ApiClient.ApiResponse resp;
            if (editing == null) {
                resp = api.post("/usuarios", u);
            } else {
                resp = api.put("/usuarios/" + u.getId(), u);
            }

            if (resp.status >= 200 && resp.status < 300) {
                JOptionPane.showMessageDialog(this, "Guardado correctamente.");
                onSaved.run();
                dispose();
            } else if (resp.status == 409) {
                // email duplicado -> conflicto
                JOptionPane.showMessageDialog(this, "Conflicto (409): " + resp.body, "Conflicto", JOptionPane.WARNING_MESSAGE);
                emailField.requestFocus();
            } else if (resp.status == 400) {
                JOptionPane.showMessageDialog(this, "Solicitud inválida (400): " + resp.body, "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Error: " + resp.status + " " + resp.body, "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error de conexión: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
