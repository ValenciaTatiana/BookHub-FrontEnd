package com.bookhub.cliente.prestamos;

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
                .uri("/api/prestamos/activos")
                .retrieve()
                .body(PrestamoResponse[].class);
    }

    public PrestamoResponse crearPrestamo(int usuarioId, String isbn,
                                          LocalDate fechaPrestamo, LocalDate fechaDevolucion) {

        PrestamoRequest req = new PrestamoRequest(
                usuarioId,
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

    public String devolverPrestamo(int usuarioId, String isbn) {
        return client.put()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/prestamos/devolver")
                        .queryParam("usuarioId", usuarioId)
                        .queryParam("libroIsbn", isbn)
                        .build())
                .retrieve()
                .body(String.class);
    }
}
