package br.com.tauasanto.kotlinvozparatexto

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {
    private var btAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var btSocket: BluetoothSocket? = null
    var activar = false
    var bluetoothIn: Handler? = null
    val handlerState = 0
    private var MyConexionBT: ConnectedThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        println("OnCreate ativado")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val conectar: Button = findViewById(R.id.conectar)
        val pairedDeveicesList = btAdapter.bondedDevices
        if (pairedDeveicesList != null) {
            for (pairedDevice in pairedDeveicesList) {
                if (pairedDevice.name == "HC-05") {
                    address = pairedDevice.address
                }
            }
        }
        conectar.setOnClickListener {
            activar = true
            onResume()
        }

        ivMic.setOnClickListener {
            // Criando intent
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }
            // Iniciando intent
            if(intent.resolveActivity(packageManager) != null) {
                startActivityForResult(intent, SPEECH_REQUEST_CODE)
            }
            else {
                Toast.makeText(this, R.string.speech_err, Toast.LENGTH_SHORT).show()
            }
        }
    }
    public override fun onResume() {
        println("onResume ativado")
        super.onResume()
        if (activar) {
            val device = btAdapter.getRemoteDevice(address)

            try {
                btSocket = createBluetoothSocket(device);
            } catch (e: IOException) {
                Toast.makeText(getBaseContext(), "A conexão com o Socket falhou", Toast.LENGTH_LONG).show();
            }
            try {
                btSocket!!.connect()
            } catch (e: IOException) {
                try {
                    btSocket!!.close()
                } catch (e2: IOException) {
                }
            }
            MyConexionBT = ConnectedThread(btSocket)
            MyConexionBT?.start()
        }
    }
    // Recebendo resultado da intent
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val spokenText: String? =
                data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).let { results ->
                    results?.get(0)
                }
            tvResult.text = spokenText
            if(spokenText == "acender"){
                MyConexionBT?.write("1")
            } else if(spokenText == "apagar"){
                MyConexionBT?.write("0")
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    // Código para receber o resultado
    companion object {
        private const val SPEECH_REQUEST_CODE = 0
        var address: String? = null
        private val BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
    @Throws(IOException::class)
    private fun createBluetoothSocket(device: BluetoothDevice): BluetoothSocket {
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID)
    }
    private fun verificarBluetooth() {
        if (!btAdapter.isEnabled) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent, 1)
        }
    }
    private inner class ConnectedThread(socket: BluetoothSocket?) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        override fun run() {
            val buffer = ByteArray(256)
            var bytes: Int

            while (true) {
                try {
                    bytes = mmInStream!!.read(buffer)
                    val readMessage = String(buffer, 0, bytes)
                    bluetoothIn!!.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget()
                } catch (e: IOException) {
                    break
                }
            }
        }

        fun write(input: String) {
            try {
                mmOutStream!!.write(input.toByteArray())
            } catch (e: IOException) {
                Toast.makeText(baseContext, "A conexão falhou", Toast.LENGTH_LONG).show()
                finish()
            }
        }

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try {
                if (socket != null) {
                    tmpIn = socket.inputStream
                    tmpOut = socket.outputStream
                }
            } catch (e: IOException) {
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
        }
    }
}