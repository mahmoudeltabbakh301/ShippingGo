import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.net.URL;

public class DownloadFont {
    public static void main(String[] args) throws Exception {
        String url = "https://fonts.gstatic.com/s/amiri/v26/J7aRnpd8CGxCGp6ElQ.ttf";
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, Paths.get("d:/myproj/shippinggo/src/main/resources/fonts/Amiri-Regular.ttf"), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Amiri downloaded successfully!");
        }
    }
}
