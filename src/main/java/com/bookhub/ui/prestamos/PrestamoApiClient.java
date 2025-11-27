package com.bookhub.ui.prestamos;

import com.bookhub.dto.PrestamoRequest;
import com.bookhub.dto.PrestamoResponse;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

public class PrestamoApiClient {

    private final RestClient client;

    public PrestamoApiClient(String baseUrl) {
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public PrestamoResponse[] listarActivos() {
        return client.get()
                .uri("/api/prestamos/activos")
                .retrieve()
                .body(PrestamoResponse[].class);
    }

    public PrestamoResponse[] listarTodos() {
        return client.get()
                .uri("/api/prestamos/todos")
                .retrieve()
                .body(PrestamoResponse[].class);
    }

    public PrestamoResponse crearPrestamo(String usuarioCedula, String isbn,
                                          LocalDate fechaPrestamo, LocalDate fechaDevolucion) {

        PrestamoRequest req = new PrestamoRequest(
                usuarioCedula,
                isbn,
                fechaPrestamo,
                fechaDevolucion
        );

        return client.post()
                .uri("/api/prestamos/registrar")
                .body(req)
                .retrieve()
                .body(PrestamoResponse.class);
    }

    public String devolverPrestamo(String usuarioCedula, String isbn) {

        PrestamoRequest req = new PrestamoRequest(
                usuarioCedula,
                isbn,
                null,
                null
        );

        return client.put()
                .uri("/api/prestamos/devolver")
                .body(req)
                .retrieve()
                .body(String.class);
    }
}
