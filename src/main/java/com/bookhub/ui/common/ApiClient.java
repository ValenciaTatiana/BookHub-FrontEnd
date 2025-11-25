package main.java.com.bookhub.ui.common;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Cliente HTTP sencillo para consumir la API REST desde Swing.
 */
public class ApiClient {
    private final String baseUrl;
    private final Gson gson = new Gson();

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private HttpURLConnection createConnection(String endpoint, String method) throws IOException {
        URL url = new URL(baseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoInput(true);
        if (method.equals("POST") || method.equals("PUT") || method.equals("PATCH")) {
            conn.setDoOutput(true);
        }
        return conn;
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        InputStream is = conn.getResponseCode() < 400 ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) {
            return "";
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    public <T> List<T> getList(String endpoint, Class<T> clazz) throws IOException {
        HttpURLConnection conn = createConnection(endpoint, "GET");
        conn.connect();
        String json = readResponse(conn);
        Type listType = TypeToken.getParameterized(List.class, clazz).getType();
        return gson.fromJson(json, listType);
    }

    public ApiResponse post(String endpoint, Object body) throws IOException {
        return sendWithBody("POST", endpoint, body);
    }

    public ApiResponse put(String endpoint, Object body) throws IOException {
        return sendWithBody("PUT", endpoint, body);
    }

    public ApiResponse patch(String endpoint, Object body) throws IOException {
        return sendWithBody("PATCH", endpoint, body);
    }

    public ApiResponse delete(String endpoint) throws IOException {
        HttpURLConnection conn = createConnection(endpoint, "DELETE");
        conn.connect();
        int code = conn.getResponseCode();
        String body = readResponse(conn);
        return new ApiResponse(code, body);
    }

    private ApiResponse sendWithBody(String method, String endpoint, Object bodyObj) throws IOException {
        HttpURLConnection conn = createConnection(endpoint, method);
        String jsonBody = gson.toJson(bodyObj);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        String resp = readResponse(conn);
        return new ApiResponse(code, resp);
    }

    public static class ApiResponse {
        public final int status;
        public final String body;

        public ApiResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }

        public boolean isSuccess() {
            return status >= 200 && status < 300;
        }
    }
}
