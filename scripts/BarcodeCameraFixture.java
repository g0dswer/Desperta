import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Java 17 source launcher; pass ZXing core on --class-path, then an output PNG path. */
class BarcodeCameraFixture {
  public static void main(String[] args) throws Exception {
    BitMatrix barcode = new MultiFormatWriter().encode("7891035002427", BarcodeFormat.EAN_13, 280, 140);
    BufferedImage frame = new BufferedImage(1280, 960, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = frame.createGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, frame.getWidth(), frame.getHeight());
    graphics.dispose();
    for (int y = 0; y < barcode.getHeight(); y++)
      for (int x = 0; x < barcode.getWidth(); x++)
        frame.setRGB(130 + x, 400 + y, barcode.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
    ImageIO.write(frame, "png", new File(args[0]));
  }
}
