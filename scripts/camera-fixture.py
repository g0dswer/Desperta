"""QR frame for API35 emulator imagefile camera (pip install 'qrcode[pil]')."""
import sys
import qrcode
from PIL import Image
image = Image.new('RGB', (1280, 960), 'white')
code = qrcode.make('DESPERTA-ACORDAR-2026').convert('RGB').resize((200, 200))
# Center the code in the portrait scanner crop on Pixel7 API35.
image.paste(code, (170, 380))
image.save(sys.argv[1] if len(sys.argv) > 1 else '/tmp/desperta-correct.png')
