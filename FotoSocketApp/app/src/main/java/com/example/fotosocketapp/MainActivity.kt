package com.example.fotosocketapp // Verifique se este é o seu pacote

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.Socket
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {

    private lateinit var etIPAddress: EditText
    private lateinit var etPort: EditText
    private lateinit var btnTakePhoto: Button

    // Lógica para abrir a câmera e pegar o resultado
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val ip = etIPAddress.text.toString()
            val port = etPort.text.toString().toIntOrNull()

            if (ip.isNotBlank() && port != null) {
                // Envia a imagem em segundo plano
                lifecycleScope.launch(Dispatchers.IO) {
                    sendImageToServer(bitmap, ip, port)
                }
            } else {
                // --- CORREÇÃO APLICADA AQUI ---
                lifecycleScope.launch {
                    showToast("IP ou Porta inválidos!")
                }
            }
        } else {
            // --- CORREÇÃO APLICADA AQUI ---
            lifecycleScope.launch {
                showToast("Nenhuma foto tirada.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Conectando o código com os botões e campos da tela
        etIPAddress = findViewById(R.id.etIPAddress)
        etPort = findViewById(R.id.etPort)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)

        // Dizendo o que o botão faz quando é clicado
        btnTakePhoto.setOnClickListener {
            takePictureLauncher.launch(null) // Abre a câmera
        }
    }

    private suspend fun sendImageToServer(bitmap: Bitmap, ip: String, port: Int) {
        try {
            val imageBytes = processAndCompressImage(bitmap)
            val imageSize = imageBytes.size

            val socket = Socket(ip, port)
            val outputStream = socket.getOutputStream()

            // 1. Envia o tamanho da imagem (4 bytes)
            val sizeBuffer = ByteBuffer.allocate(4).putInt(imageSize).array()
            outputStream.write(sizeBuffer)

            // 2. Envia os bytes da imagem
            outputStream.write(imageBytes)
            outputStream.flush()
            socket.close()

            showToast("Foto enviada com sucesso!")

        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Erro ao enviar foto: ${e.message}")
        }
    }

    private fun processAndCompressImage(bitmap: Bitmap): ByteArray {
        val maxWidth = 1280
        var processedBitmap = bitmap
        if (bitmap.width > maxWidth) {
            val scale = maxWidth.toFloat() / bitmap.width
            val newHeight = (bitmap.height * scale).toInt()
            processedBitmap = Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
        }

        val stream = ByteArrayOutputStream()
        processedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return stream.toByteArray()
    }

    private suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
        }
    }
}