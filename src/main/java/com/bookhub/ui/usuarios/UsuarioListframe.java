package main.java.com.bookhub.ui.usuarios;

import com.bookhub.entity.Usuario;
import com.bookhub.ui.common.ApiClient;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UsuarioListFrame extends JFrame {

    private final ApiClient api;
    private final DefaultTableModel model;
    private final JTable table;
    private List<Usuario> usuarios;
    private final TableRowSorter<TableModel> sorter;
    private final JTextField filtroField;

    public UsuarioListFrame(ApiClient api) {
        this.api = api;
        setTitle("Usuarios - BookHub");
        setSize(900, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        model = new DefaultTableModel(new Object[]{"ID", "Nombre", "Email", "Teléfono"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JScrollPane scroll = new JScrollPane(table);

        JPanel top = new JPanel(new BorderLayout(6,6));
        filtroField = new JTextField();
        top.add(new JLabel("Filtrar por nombre/email: "), BorderLayout.WEST);
        top.add(filtroField, BorderLayout.CENTER);

        JButton btnRefresh = new JButton("Refrescar");
        top.add(btnRefresh, BorderLayout.EAST);

        JPanel bottom = new JPanel();
        JButton btnNuevo = new JButton("Nuevo");
        JButton btnEditar = new JButton("Editar");
        JButton btnEliminar = new JButton("Eliminar");
        bottom.add(btnNuevo);
        bottom.add(btnEditar);
        bottom.add(btnEliminar);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        // listeners
        btnRefresh.addActionListener(e -> loadUsuarios());
        btnNuevo.addActionListener(e -> openForm(null));
        btnEditar.addActionListener(e -> editSelected());
        btnEliminar.addActionListener(e -> deleteSelected());

        // doble click para editar
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) editSelected();
            }
        });

        // filtro live
        filtroField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        // focus traversal natural (tab order)
        setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());

        // carga inicial
        loadUsuarios();
    }

    private void applyFilter() {
        String text = filtroField.getText();
        if (text == null || text.isBlank()) {
            sorter.setRowFilter(null);
        } else {
            String regex = "(?i).*" + Pattern.quote(text) + ".*";
            sorter.setRowFilter(javax.swing.RowFilter.regexFilter(regex, 1, 2));
        }
    }

    private void loadUsuarios() {
        SwingUtilities.invokeLater(() -> {
            model.setRowCount(0);
            try {
                usuarios = api.getList("/usuarios", Usuario.class);
                for (Usuario u : usuarios) {
                    model.addRow(new Object[]{u.getId(), u.getNombre(), u.getEmail(), u.getTelefono()});
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error cargando usuarios: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void openForm(Usuario usuario) {
        UsuarioFormDialog dialog = new UsuarioFormDialog(this, api, usuario, this::loadUsuarios);
        dialog.setVisible(true);
    }

    private void editSelected() {
        int sel = table.getSelectedRow();
        if (sel == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un usuario para editar.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(sel);
        int id = (int) model.getValueAt(modelRow, 0);
        Usuario u = usuarios.stream().filter(x -> x.getId() == id).findFirst().orElse(null);
        if (u != null) openForm(u);
    }

    private void deleteSelected() {
        int sel = table.getSelectedRow();
        if (sel == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un usuario para eliminar.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(sel);
        int id = (int) model.getValueAt(modelRow, 0);

        int conf = JOptionPane.showConfirmDialog(this, "¿Eliminar usuario con id=" + id + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;

        try {
            ApiClient.ApiResponse resp = api.delete("/usuarios/" + id);
            if (resp.status >= 200 && resp.status < 300) {
                JOptionPane.showMessageDialog(this, "Usuario eliminado correctamente.");
                loadUsuarios();
            } else if (resp.status == 409) {
                JOptionPane.showMessageDialog(this, "No se puede eliminar: " + resp.body, "Conflicto (409)", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Error: " + resp.status + " " + resp.body, "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error de conexión: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
