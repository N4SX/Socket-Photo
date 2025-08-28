import socket
import cv2
import numpy as np
import datetime
import os

# --- Configurações ---
HOST = '0.0.0.0'  # Escuta em todas as interfaces de rede disponíveis
PORT = 9999
SAVE_DIR = "fotos_recebidas"
WINDOW_NAME = "Foto Recebida"

# --- Funções Auxiliares ---
def create_save_dir():
    """Cria o diretório para salvar as imagens, se não existir."""
    if not os.path.exists(SAVE_DIR):
        os.makedirs(SAVE_DIR)

def receive_all(sock, n):
    """Garante que todos os n bytes são recebidos do socket."""
    data = bytearray()
    while len(data) < n:
        packet = sock.recv(n - len(data))
        if not packet:
            return None
        data.extend(packet)
    return data

def show_initial_message():
    """Mostra uma imagem inicial com a mensagem 'Aguardando foto...'."""
    img = np.zeros((480, 640, 3), dtype=np.uint8)
    text = "Aguardando foto..."
    font = cv2.FONT_HERSHEY_SIMPLEX
    text_size = cv2.getTextSize(text, font, 1, 2)[0]
    text_x = (img.shape[1] - text_size[0]) // 2
    text_y = (img.shape[0] + text_size[1]) // 2
    cv2.putText(img, text, (text_x, text_y), font, 1, (255, 255, 255), 2)
    cv2.imshow(WINDOW_NAME, img)
    cv2.waitKey(1)

# --- Lógica Principal do Servidor ---
def main():
    create_save_dir()
    
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind((HOST, PORT))
        s.listen()
        print(f"Servidor escutando em {HOST}:{PORT}")
        
        show_initial_message()

        while True:
            conn, addr = s.accept()
            with conn:
                print(f"Conectado por {addr}")
                
                # 1. Receber o tamanho da imagem (4 bytes)
                img_size_bytes = receive_all(conn, 4)
                if not img_size_bytes:
                    print("Cliente desconectado antes de enviar o tamanho.")
                    continue
                
                img_size = int.from_bytes(img_size_bytes, 'big')
                print(f"Recebendo imagem de {img_size} bytes...")

                # 2. Receber os bytes da imagem
                img_bytes = receive_all(conn, img_size)
                if not img_bytes:
                    print("Cliente desconectado durante o envio da imagem.")
                    continue

                print("Imagem recebida com sucesso!")

                # 3. Decodificar e Salvar a Imagem
                try:
                    # Converter bytes para um array numpy
                    img_np = np.frombuffer(img_bytes, np.uint8)
                    # Decodificar o array numpy para uma imagem OpenCV
                    img_decoded = cv2.imdecode(img_np, cv2.IMREAD_COLOR)

                    # Gerar nome do arquivo com timestamp
                    timestamp = datetime.datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
                    filename = os.path.join(SAVE_DIR, f"foto_{timestamp}.jpg")
                    
                    # Salvar a imagem
                    cv2.imwrite(filename, img_decoded)
                    print(f"Foto salva em: {filename}")
                    
                    # 4. Exibir a imagem
                    cv2.imshow(WINDOW_NAME, img_decoded)
                    cv2.waitKey(1) # Essencial para a janela atualizar

                except Exception as e:
                    print(f"Erro ao processar a imagem: {e}")

if __name__ == '__main__':
    main()