"""QR frame for API35 emulator imagefile camera (pip install 'qrcode[pil]')."""
import sys
import qrcode
from PIL import Image
image = Image.new('RGB', (1280, 960), 'white')
code = qrcode.make('DESPERTA-ACORDAR-2026').convert('RGB').resize((200, 200))
# Camera sensor rotation/crop on Pixel7 API35 maps this region into the scan box.
image.paste(code, (260, 240))
image.save(sys.argv[1] if len(sys.argv) > 1 else '/tmp/desperta-correct.png')
