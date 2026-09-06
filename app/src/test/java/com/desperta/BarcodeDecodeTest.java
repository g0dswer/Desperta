package com.desperta;

import static org.junit.Assert.*;

import com.google.zxing.*;
import com.google.zxing.common.*;
import org.junit.Test;

public class BarcodeDecodeTest {
  @Test
  public void qrCameraPixelDecoderPreservesExactPayload() throws Exception {
    roundTrip("DESPERTA-ACORDAR-2026", BarcodeFormat.QR_CODE);
  }

  @Test
  public void ean13CameraPixelDecoderPreservesProductCode() throws Exception {
    roundTrip("7891000315507", BarcodeFormat.EAN_13);
  }

  @Test
  public void code128CameraPixelDecoderPreservesLeadingZeros() throws Exception {
    roundTrip("0001234567", BarcodeFormat.CODE_128);
  }

  void roundTrip(String value, BarcodeFormat format) throws Exception {
    BitMatrix m = new MultiFormatWriter().encode(value, format, 800, 500);
    int[] pixels = new int[m.getWidth() * m.getHeight()];
    for (int y = 0; y < m.getHeight(); y++)
      for (int x = 0; x < m.getWidth(); x++)
        pixels[y * m.getWidth() + x] = m.get(x, y) ? 0xff000000 : 0xffffffff;
    Result r =
        new MultiFormatReader()
            .decode(
                new BinaryBitmap(
                    new HybridBinarizer(
                        new RGBLuminanceSource(m.getWidth(), m.getHeight(), pixels))));
    assertEquals(value, r.getText());
    assertTrue(MissionLogic.barcodeMatches(value, r.getText()));
    assertFalse(MissionLogic.barcodeMatches(value + "X", r.getText()));
  }
}
