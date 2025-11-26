package com.bookhub.cliente.prestamos;

import com.bookhub.dto.PrestamoResponse;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Arrays;

public class PrestamoFrame extends JFrame {

    private final PrestamoApiClient apiClient;

    private JTextField txtUsuarioId;
    private JTextField txtLibroIsbn;
    private JTextField txtFechaDevolucion;

    private JTable tablaActivos;
    private JTable tablaHistorial;

    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

    public PrestamoFrame() {
        this.apiClient = new PrestamoApiClient("http://localhost:8080");
        initComponents();
        cargarPrestamos();
    }

    private void initComponents() {
        setTitle("BookHub - Módulo Préstamos");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel panelFormulario = new JPanel(new GridLayout(2, 4, 8, 5));

        panelFormulario.add(new JLabel("Usuario ID:"));
        txtUsuarioId = new JTextField();
        panelFormulario.add(txtUsuarioId);

        panelFormulario.add(new JLabel("Libro ISBN:"));
        txtLibroIsbn = new JTextField();
        panelFormulario.add(txtLibroIsbn);

        panelFormulario.add(new JLabel("Fecha devolución (yyyy-MM-dd):"));
        txtFechaDevolucion = new JTextField();
        panelFormulario.add(txtFechaDevolucion);

        JLabel lblInfoFecha = new JLabel("Hoy ≤ devolución ≤ hoy+15 días");
        panelFormulario.add(lblInfoFecha);

        add(panelFormulario, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();

        tablaActivos = new JTable(
                new DefaultTableModel(
                        new Object[]{"ID", "Usuario", "ISBN", "F. Préstamo", "F. Devolución", "Estado"}, 0
                )
        );
        JScrollPane scrollActivos = new JScrollPane(tablaActivos);
        tabs.addTab("Préstamos Activos", scrollActivos);

        tablaHistorial = new JTable(
                new DefaultTableModel(
                        new Object[]{"ID", "Usuario", "ISBN", "F. Préstamo", "F. Devolución", "Estado"}, 0
                )
        );
        JScrollPane scrollHistorial = new JScrollPane(tablaHistorial);
        tabs.addTab("Historial", scrollHistorial);

        add(tabs, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnCrear = new JButton("Crear préstamo");
        btnCrear.addActionListener(e -> onCrearPrestamo());
        panelBotones.add(btnCrear);

        JButton btnDevolver = new JButton("Devolver préstamo");
        btnDevolver.addActionListener(e -> onDevolverPrestamo());
        panelBotones.add(btnDevolver);

        JButton btnRefrescar = new JButton("Refrescar");
        btnRefrescar.addActionListener(e -> cargarPrestamos());
        panelBotones.add(btnRefrescar);

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());
        panelBotones.add(btnCerrar);

        add(panelBotones, BorderLayout.SOUTH);
    }

    private void onCrearPrestamo() {
        String usuarioIdStr = txtUsuarioId.getText().trim();
        String isbn = txtLibroIsbn.getText().trim();
        String fechaDevStr = txtFechaDevolucion.getText().trim();

        if (usuarioIdStr.isEmpty() || isbn.isEmpty() || fechaDevStr.isEmpty()) {
            mostrarError("Todos los campos son obligatorios.");
            return;
        }

        if (!usuarioIdStr.matches("\\d+")) {
            mostrarError("El Usuario ID debe ser numérico.");
            return;
        }

        int usuarioId = Integer.parseInt(usuarioIdStr);

        LocalDate hoy = LocalDate.now();
        LocalDate fechaDev;
        try {
            fechaDev = LocalDate.parse(fechaDevStr, formatter);
        } catch (DateTimeParseException ex) {
            mostrarError("La fecha de devolución debe tener el formato yyyy-MM-dd.");
            return;
        }

        if (fechaDev.isBefore(hoy)) {
            mostrarError("La fecha de devolución no puede ser anterior a hoy.");
            return;
        }

        if (fechaDev.isAfter(hoy.plusDays(15))) {
            mostrarError("La fecha de devolución no puede superar los 15 días.");
            return;
        }

        try {
            apiClient.crearPrestamo(usuarioId, isbn, hoy, fechaDev);
            mostrarInfo("Préstamo creado correctamente.");
            cargarPrestamos();
        } catch (Exception ex) {
            mostrarError("Error creando préstamo: " + ex.getMessage());
        }
    }

    private void onDevolverPrestamo() {
        int fila = tablaActivos.getSelectedRow();
        if (fila == -1) {
            mostrarError("Seleccione un préstamo.");
            return;
        }

        int usuarioId = Integer.parseInt(tablaActivos.getValueAt(fila, 1).toString());
        String isbn = tablaActivos.getValueAt(fila, 2).toString();

        try {
            apiClient.devolverPrestamo(usuarioId, isbn);
            mostrarInfo("Préstamo devuelto correctamente.");
            cargarPrestamos();
        } catch (Exception ex) {
            mostrarError("Error devolviendo préstamo: " + ex.getMessage());
        }
    }

    private void cargarPrestamos() {
        cargarActivos();
        cargarHistorial();
    }

    private void cargarActivos() {
        try {
            PrestamoResponse[] activos = apiClient.listarActivos();
            DefaultTableModel model = (DefaultTableModel) tablaActivos.getModel();
            model.setRowCount(0);

            for (PrestamoResponse p : activos) {
                model.addRow(new Object[]{
                        p.getId(),
                        p.getUsuarioId(),
                        p.getLibroIsbn(),
                        p.getFechaPrestamo(),
                        p.getFechaDevolucion(),
                        p.getEstado()
                });
            }
        } catch (Exception ex) {
            mostrarError("No se pudieron cargar los préstamos.");
        }
    }

    private void cargarHistorial() {
        try {
            PrestamoResponse[] todos = apiClient.listarTodos();
            DefaultTableModel model = (DefaultTableModel) tablaHistorial.getModel();
            model.setRowCount(0);

            for (PrestamoResponse p : todos) {
                model.addRow(new Object[]{
                        p.getId(),
                        p.getUsuarioId(),
                        p.getLibroIsbn(),
                        p.getFechaPrestamo(),
                        p.getFechaDevolucion(),
                        p.getEstado()
                });
            }
        } catch (Exception ex) {
            mostrarError("No se pudo cargar el historial.");
        }
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(
                this,
                mensaje,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void mostrarInfo(String mensaje) {
        JOptionPane.showMessageDialog(
                this,
                mensaje,
                "Información",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}
