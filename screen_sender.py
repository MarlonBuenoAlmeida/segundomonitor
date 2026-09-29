import io
import os
import socket
import struct
import sys
import time
import mss
from PIL import Image
import win32gui
import win32api

HOST = '127.0.0.1'
PORT = 9090

TARGET_WIDTH = 1024
TARGET_HEIGHT = 600

if getattr(sys, 'frozen', False):
    BASE_DIR = sys._MEIPASS
else:
    BASE_DIR = os.path.dirname(__file__)

CURSOR_PATH = os.path.join(BASE_DIR, "cursor.png")
cursor_img = None

if os.path.exists(CURSOR_PATH):
    try:
        cursor_img = Image.open(CURSOR_PATH).convert("RGBA")
        cursor_img = cursor_img.resize((48, 48), Image.Resampling.BILINEAR)
    except Exception as e:
        print(f"Erro ao carregar o cursor.png: {e}")

def draw_mouse_cursor(img, monitor):
    """ Desenha a imagem PNG do cursor no frame """
    try:
        cursor_x, cursor_y = win32gui.GetCursorPos()
        
        mon_left = monitor['left']
        mon_top = monitor['top']
        mon_width = monitor['width']
        mon_height = monitor['height']

        rel_x = cursor_x - mon_left
        rel_y = cursor_y - mon_top

        if 0 <= rel_x <= mon_width and 0 <= rel_y <= mon_height:
            scale_x = TARGET_WIDTH / mon_width
            scale_y = TARGET_HEIGHT / mon_height
            
            draw_x = int(rel_x * scale_x)
            draw_y = int(rel_y * scale_y)

            if cursor_img:
                img.paste(cursor_img, (draw_x, draw_y), cursor_img)
    except Exception:
        pass

def start_sender():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    
    print("=== Transmissor de Tela Estendida (Cursor PNG) ===")
    print("Conectando ao tablet...")

    while True:
        try:
            s.connect((HOST, PORT))
            print("✅ Conectado ao Tablet com sucesso!")
            break
        except Exception:
            time.sleep(2)

    with mss.MSS() as sct:
        if len(sct.monitors) < 3:
            target_monitor = sct.monitors[1]
        else:
            target_monitor = sct.monitors[2]

        try:
            while True:
                start_time = time.time()

                sct_img = sct.grab(target_monitor)
                img = Image.frombytes("RGB", sct_img.size, sct_img.bgra, "raw", "BGRX")
                img_resized = img.resize((TARGET_WIDTH, TARGET_HEIGHT), Image.Resampling.BILINEAR)
                
                draw_mouse_cursor(img_resized, target_monitor)

                buffer = io.BytesIO()
                img_resized.save(buffer, format="JPEG", quality=60)
                jpeg_bytes = buffer.getvalue()

                size_prefix = struct.pack('>I', len(jpeg_bytes))
                s.sendall(size_prefix + jpeg_bytes)

                elapsed = time.time() - start_time
                sleep_time = (1.0 / 30.0) - elapsed
                if sleep_time > 0:
                    time.sleep(sleep_time)

        except Exception as e:
            print(f"Conexão encerrada: {e}")
        finally:
            s.close()

if __name__ == '__main__':
    start_sender()