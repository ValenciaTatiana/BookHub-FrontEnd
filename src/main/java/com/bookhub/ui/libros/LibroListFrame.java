package main.java.com.bookhub.ui.libros;

import com.bookhub.dto.LibroResponse;
import com.bookhub.ui.common.ApiClient;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 * Pantalla principal para gestionar libros con CRUD completo y filtros locales.
 */
public class LibroListFrame extends JFrame {

    private final ApiClient api;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField searchField = new JTextField();
    private final TableRowSorter<TableModel> sorter;
    private final JButton btnNuevo = new JButton("Nuevo (Ctrl+N)");
    private final JButton btnEditar = new JButton("Editar (Ctrl+E)");
    private final JButton btnEliminar = new JButton("Eliminar (Del)");
    private final JButton btnRefresh = new JButton("Refrescar (F5)");

    private List<LibroResponse> libros = new ArrayList<>();

    public LibroListFrame(ApiClient api) {
        this.api = api;
        setTitle("Gestion de libros - BookHub");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(980, 560));

        tableModel = new DefaultTableModel(new Object[]{"ISBN", "Titulo", "Autor", "Categoria", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        buildLayout();
        bindActions();
        pack();
        setLocationRelativeTo(null);
        loadLibros();
    }

    private void buildLayout() {
        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(new JLabel("Buscar (titulo/autor):"), BorderLayout.WEST);
        top.add(searchField, BorderLayout.CENTER);
        top.add(btnRefresh, BorderLayout.EAST);

        JPanel bottom = new JPanel();
        bottom.add(btnNuevo);
        bottom.add(btnEditar);
        bottom.add(btnEliminar);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void bindActions() {
        btnRefresh.addActionListener(e -> loadLibros());
        btnNuevo.addActionListener(e -> openForm(null));
        btnEditar.addActionListener(e -> editSelected());
        btnEliminar.addActionListener(e -> deleteSelected());

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelected();
                }
            }
        });

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
        });

        // Atajos de teclado para accesibilidad
        JComponent root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK), "nuevo");
        root.getActionMap().put("nuevo", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { openForm(null); }
        });
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK), "editar");
        root.getActionMap().put("editar", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { editSelected(); }
        });
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "eliminar");
        root.getActionMap().put("eliminar", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { deleteSelected(); }
        });
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh");
        root.getActionMap().put("refresh", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { loadLibros(); }
        });
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "focusSearch");
        root.getActionMap().put("focusSearch", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                searchField.requestFocus();
                searchField.selectAll();
            }
        });
    }

    private void applyFilter() {
        String text = searchField.getText();
        if (text == null || text.isBlank()) {
            sorter.setRowFilter(null);
        } else {
            String regex = "(?i).*" + Pattern.quote(text.trim()) + ".*";
            sorter.setRowFilter(RowFilter.regexFilter(regex, 1, 2));
        }
    }

    private void loadLibros() {
        setControlsEnabled(false);
        new SwingWorker<List<LibroResponse>, Void>() {
            @Override
            protected List<LibroResponse> doInBackground() throws Exception {
                return api.getList("/libros", LibroResponse.class);
            }

            @Override
            protected void done() {
                setControlsEnabled(true);
                try {
                    libros = get();
                    renderTable();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(LibroListFrame.this, "Error cargando libros: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void setControlsEnabled(boolean enabled) {
        btnNuevo.setEnabled(enabled);
        btnEditar.setEnabled(enabled);
        btnEliminar.setEnabled(enabled);
        btnRefresh.setEnabled(enabled);
        table.setEnabled(enabled);
    }

    private void renderTable() {
        tableModel.setRowCount(0);
        for (LibroResponse libro : libros) {
            tableModel.addRow(new Object[]{
                libro.getIsbn(),
                libro.getTitulo(),
                libro.getAutor(),
                libro.getCategoria(),
                libro.isDisponible() ? "Disponible" : "Prestado"
            });
        }
        applyFilter();
    }

    private void openForm(LibroResponse libro) {
        LibroFormDialog dialog = new LibroFormDialog(this, api, libro, libros, this::loadLibros);
        dialog.setVisible(true);
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un libro para editar.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        String isbn = Objects.toString(tableModel.getValueAt(modelRow, 0), null);
        libros.stream()
            .filter(libro -> libro.getIsbn().equals(isbn))
            .findFirst()
            .ifPresent(this::openForm);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un libro para eliminar.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        String isbn = Objects.toString(tableModel.getValueAt(modelRow, 0), null);
        String titulo = Objects.toString(tableModel.getValueAt(modelRow, 1), "");

        int option = JOptionPane.showConfirmDialog(this,
            "Eliminar el libro \"" + titulo + "\" (" + isbn + ")?",
            "Confirmar eliminacion",
            JOptionPane.YES_NO_OPTION);
        if (option != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            ApiClient.ApiResponse resp = api.delete("/libros/" + isbn);
            if (resp.isSuccess()) {
                JOptionPane.showMessageDialog(this, "Libro eliminado.");
                loadLibros();
            } else {
                handleApiError(resp);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error de comunicacion: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleApiError(ApiClient.ApiResponse resp) {
        String msg = resp.body != null && !resp.body.isBlank() ? resp.body : "Revisa la API.";
        switch (resp.status) {
            case 400 -> JOptionPane.showMessageDialog(this, "No se pudo completar la operacion: " + msg, "Solicitud invalida", JOptionPane.WARNING_MESSAGE);
            case 404 -> JOptionPane.showMessageDialog(this, "Libro no encontrado: " + msg, "No encontrado", JOptionPane.WARNING_MESSAGE);
            case 409 -> JOptionPane.showMessageDialog(this, "Conflicto detectado: " + msg, "Conflicto", JOptionPane.WARNING_MESSAGE);
            case 500 -> JOptionPane.showMessageDialog(this, "Error interno: " + msg, "Servidor", JOptionPane.ERROR_MESSAGE);
            default -> JOptionPane.showMessageDialog(this, "Error (" + resp.status + "): " + msg, "API", JOptionPane.ERROR_MESSAGE);
        }
    }
}