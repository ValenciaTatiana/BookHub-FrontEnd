package main.java.com.bookhub.ui.usuarios;

import com.bookhub.ui.common.ApiClient;
import javax.swing.SwingUtilities;

public class SwingLauncher {
    public static void main(String[] args) {
        String baseApi = "http://localhost:8080/api";
        ApiClient api = new ApiClient(baseApi);
        SwingUtilities.invokeLater(() -> {
            UsuarioListFrame frame = new UsuarioListFrame(api);
            frame.setVisible(true);
        });
    }
}

