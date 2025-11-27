package com.bookhub.cliente.prestamos;

import com.bookhub.dto.PrestamoResponse;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;

public class PrestamoFrame extends JFrame {

    private final PrestamoApiClient apiClient;

    private JTextField txtUsuarioCedula;
    private JTextField txtLibroIsbn;
    private JTextField txtFechaDevolucion;

    private JTable tablaActivos;
    private JTable tablaHistorial;

    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
    private List<PrestamoResponse> cacheActivos = Collections.emptyList();
    private List<PrestamoResponse> cacheHistorial = Collections.emptyList();

    public PrestamoFrame() {
        this.apiClient = new PrestamoApiClient("http://localhost:8080");
        initComponents();
        cargarPrestamos();
    }

    private void initComponents() {
        setTitle("BookHub - Modulo Prestamos");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel panelFormulario = new JPanel(new GridLayout(2, 4, 8, 5));

        panelFormulario.add(new JLabel("Usuario (cedula):"));
        txtUsuarioCedula = new JTextField();
        panelFormulario.add(txtUsuarioCedula);

        panelFormulario.add(new JLabel("Libro ISBN:"));
        txtLibroIsbn = new JTextField();
        panelFormulario.add(txtLibroIsbn);

        panelFormulario.add(new JLabel("Fecha devolucion (yyyy-MM-dd):"));
        txtFechaDevolucion = new JTextField();
        panelFormulario.add(txtFechaDevolucion);

        JLabel lblInfoFecha = new JLabel("Hoy -> devolucion -> hoy+15 dias");
        panelFormulario.add(lblInfoFecha);

        add(panelFormulario, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();

        tablaActivos = new JTable(
                new DefaultTableModel(
                        new Object[]{"ID", "Cedula", "ISBN", "F. Prestamo", "F. Devolucion", "Estado"}, 0
                )
        );
        JScrollPane scrollActivos = new JScrollPane(tablaActivos);
        tabs.addTab("Prestamos Activos", scrollActivos);

        tablaHistorial = new JTable(
                new DefaultTableModel(
                        new Object[]{"ID", "Cedula", "ISBN", "F. Prestamo", "F. Devolucion", "Estado"}, 0
                )
        );
        JScrollPane scrollHistorial = new JScrollPane(tablaHistorial);
        tabs.addTab("Historial", scrollHistorial);

        add(tabs, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnCrear = new JButton("Crear prestamo");
        btnCrear.addActionListener(e -> onCrearPrestamo());
        panelBotones.add(btnCrear);

        JButton btnDevolver = new JButton("Devolver prestamo");
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
        String usuarioCedula = txtUsuarioCedula.getText().trim();
        String isbn = txtLibroIsbn.getText().trim();
        String fechaDevStr = txtFechaDevolucion.getText().trim();

        if (usuarioCedula.isEmpty() || isbn.isEmpty() || fechaDevStr.isEmpty()) {
            mostrarError("Todos los campos son obligatorios.");
            return;
        }

        if (!usuarioCedula.matches("\\d+")) {
            mostrarError("La cedula debe ser numerica.");
            return;
        }

        LocalDate hoy = LocalDate.now();
        LocalDate fechaDev;
        try {
            fechaDev = LocalDate.parse(fechaDevStr, formatter);
        } catch (DateTimeParseException ex) {
            mostrarError("La fecha de devolucion debe tener el formato yyyy-MM-dd.");
            return;
        }

        if (fechaDev.isBefore(hoy)) {
            mostrarError("La fecha de devolucion no puede ser anterior a hoy.");
            return;
        }

        if (fechaDev.isAfter(hoy.plusDays(15))) {
            mostrarError("La fecha de devolucion no puede superar los 15 dias.");
            return;
        }

        try {
            apiClient.crearPrestamo(usuarioCedula, isbn, hoy, fechaDev);
            mostrarInfo("Prestamo creado correctamente.");
            cargarPrestamos();
        } catch (Exception ex) {
            mostrarError("Error creando prestamo: " + ex.getMessage());
        }
    }

    private void onDevolverPrestamo() {
        int fila = tablaActivos.getSelectedRow();
        if (fila == -1) {
            mostrarError("Seleccione un prestamo.");
            return;
        }

        int modelRow = tablaActivos.convertRowIndexToModel(fila);
        if (modelRow < 0 || modelRow >= cacheActivos.size()) {
            mostrarError("No se encontro el registro seleccionado.");
            return;
        }

        PrestamoResponse seleccionado = cacheActivos.get(modelRow);
        String usuarioCedula = seleccionado.getUsuarioCedula();
        String isbn = seleccionado.getLibroIsbn();

        if (usuarioCedula == null || usuarioCedula.isBlank()) {
            mostrarError("No se pudo obtener la cedula del usuario del prestamo seleccionado.");
            return;
        }

        try {
            apiClient.devolverPrestamo(usuarioCedula, isbn);
            mostrarInfo("Prestamo devuelto correctamente.");
            cargarPrestamos();
        } catch (Exception ex) {
            mostrarError("Error devolviendo prestamo: " + ex.getMessage());
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
            cacheActivos = Arrays.asList(activos);

            for (PrestamoResponse p : activos) {
                model.addRow(new Object[]{
                        p.getId(),
                        p.getUsuarioCedula(),
                        p.getLibroIsbn(),
                        p.getFechaPrestamo(),
                        p.getFechaDevolucion(),
                        p.getEstado()
                });
            }
        } catch (Exception ex) {
            mostrarError("No se pudieron cargar los prestamos.");
        }
    }

    private void cargarHistorial() {
        try {
            PrestamoResponse[] todos = apiClient.listarTodos();
            DefaultTableModel model = (DefaultTableModel) tablaHistorial.getModel();
            model.setRowCount(0);
            cacheHistorial = Arrays.asList(todos);

            for (PrestamoResponse p : todos) {
                model.addRow(new Object[]{
                        p.getId(),
                        p.getUsuarioCedula(),
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
                "Informacion",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}