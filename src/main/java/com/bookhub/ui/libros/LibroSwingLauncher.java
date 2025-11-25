package main.java.com.bookhub.ui.libros;

import com.bookhub.ui.common.ApiClient;
import javax.swing.SwingUtilities;

public class LibroSwingLauncher {

    public static void main(String[] args) {
        String baseApi = System.getProperty("bookhub.api", "http://localhost:8080/api");
        ApiClient api = new ApiClient(baseApi);
        SwingUtilities.invokeLater(() -> new LibroListFrame(api).setVisible(true));
    }
}
