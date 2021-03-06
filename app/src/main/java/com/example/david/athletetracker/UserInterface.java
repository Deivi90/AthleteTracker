package com.example.david.athletetracker;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


//https://developer.android.com/guide/topics/connectivity/bluetooth.html?hl=es-419

public class UserInterface extends AppCompatActivity {
/*
    TextView IdBufferIn;
    Handler bluetoothIn;
    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;      //Bluetooth local
    private BluetoothSocket btSocket = null;        // Socket para la comm
    private StringBuilder DataStringIn = new StringBuilder();
    private ConnectedThread myConexionBt;
    private double[] Data = new double[10]; // se guardan los ultimos 10 datos recibidos
    int DataIndex=0;  // indice de los datos recibidos
    // Identificador unico de servicio
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private String address = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_interface); //Elijo el layout de la interfaz
        // Defino cada uno de los elementos del layout
        IdBufferIn = (TextView) findViewById(R.id.IdBufferIn);
        //Manejo los datos que le llegan al bluetooth

        bluetoothIn = new Handler(){
            public void handleMessage(Message msg) {
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    DataStringIn.append(readMessage);
                    int endOfLineIndex = DataStringIn.indexOf("#"); //Uso el caracter # para separar los datos
                    if (endOfLineIndex > 0) {
                        String dataInPrint = DataStringIn.substring(0, endOfLineIndex);
                        IdBufferIn.setText(dataInPrint); // Lo que llega por bluetooth lo mando al Idbufferin
                        DataStringIn.delete(0, DataStringIn.length());
                        //Se guardan los 10 ultimos datos recibidos para realizar el grafico
                        if(DataIndex < 10 )
                        {
                            Data[DataIndex] = (Double.parseDouble(dataInPrint));
                            DataIndex++;
                        }
                        else {
                            DataIndex = 0;
                            Data[DataIndex] = (Double.parseDouble(dataInPrint));
                        }
                    }
                }
            }

        };
        btAdapter = BluetoothAdapter.getDefaultAdapter();
    }


    public void onResume()
    {
        // https://developer.android.com/guide/topics/connectivity/bluetooth.html
        super.onResume();

        Intent intent = getIntent(); // Creo un intent para recuperar la data de la activity anterior ///Deberia ir en onCreate//
        address = intent.getStringExtra(DispositivosBt.EXTRA_DEVICE_ADDRESS); // Recupero la data de la anterior activity
        BluetoothDevice device = btAdapter.getRemoteDevice(address);    // dispositivo remoto a conectarse

        try
        {
            btSocket = device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        } catch(IOException e){
            Toast.makeText(getBaseContext(),"La creacion del Socket fallo", Toast.LENGTH_LONG).show();
        }
        try {
            btSocket.connect();
        } catch(IOException e){
            try{
                Toast.makeText(getBaseContext(),"No se pudo establecer la conexión", Toast.LENGTH_LONG).show();
                btSocket.close();
                // Vuelvo a la activity que elige el Bluetooth
                Intent DispositivosBtIntent = new Intent(UserInterface.this, DispositivosBt.class);
                startActivity(DispositivosBtIntent);
                finish();
            }catch(IOException e2){
               Toast.makeText(getBaseContext(),"No se cerro el Socket", Toast.LENGTH_LONG).show();
            }
        }
        myConexionBt = new ConnectedThread(btSocket);
        myConexionBt.start(); // Agregar algo para cuando la conexion falla.
    }

    public void onPause()
    {
        super.onPause();
        try
        {
            btSocket.close();
        } catch (IOException e2){}
    }

//https://developer.android.com/guide/topics/connectivity/bluetooth.html?hl=es-419

/*
    private class ConnectedThread extends Thread
    {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        public ConnectedThread(BluetoothSocket socket){
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch(IOException e){}
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run()
        {
            byte[] buffer = new byte[256];
            int bytes;
            while(true)
            {
                try
                {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String (buffer, 0 , bytes);
                    bluetoothIn.obtainMessage(handlerState, bytes, -1,readMessage).sendToTarget();
                } catch (IOException e){
                    break;
                }
            }
        }
        public void write(String input)
        {
            try {
                mmOutStream.write(input.getBytes());
            } catch ( IOException e){
                Toast.makeText(getBaseContext(),"La Conexion fallo", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }*/
}
