package com.example.david.pruebabt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.UUID;
import java.util.Vector;

public class UserInterface extends AppCompatActivity {

    Button IdEncender, IdApagar, IdDesconectar, IdGraficar;
    TextView IdBufferIn;
    Handler bluetoothIn;
    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder DataStringIn = new StringBuilder();
    private ConnectedThread MyConexionBT;


    private double[] Data = new double[10];
    //private List<Double> Data = new ArrayList<Double>();
    // private ListIterator<Double> itData = Data.listIterator();
    int DataIndex=0;
    // Identificador unico de servicio
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private String address = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_interface);

        IdEncender = (Button) findViewById(R.id.IdEncender);
        IdApagar = (Button) findViewById(R.id.IdApagar);
        IdDesconectar = (Button) findViewById(R.id.IdDesconectar);
        IdGraficar = (Button) findViewById(R.id.IdGraficar);
        IdBufferIn = (TextView) findViewById(R.id.IdBufferIn);

        //Manejo los datos que le llegan al bluetooth
        bluetoothIn = new Handler(){
            public void handleMessage(Message msg) {
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    DataStringIn.append(readMessage);

                    int endOfLineIndex = DataStringIn.indexOf("#"); //Uso el caracter # para separa los datos

                    if (endOfLineIndex > 0) {
                        String dataInPrint = DataStringIn.substring(0, endOfLineIndex);
                        IdBufferIn.setText(dataInPrint); //**// Lo que llega por bluetooth lo mando al Idbufferin
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
        VerificarEstadoBT();

        //Funcion de los botones
        IdEncender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyConexionBT.write("1");
            }
        });

        IdApagar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyConexionBT.write("0");
            }
        });


        IdDesconectar.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                if(btSocket != null)
                {
                    try{btSocket.close();}
                    catch(IOException e)
                    {
                        Toast.makeText(getBaseContext(),"Error",Toast.LENGTH_LONG).show();
                    }

                }
                finish();
            }
        });


        IdGraficar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent graficoPantalla = new Intent(UserInterface.this, Grafico.class);
                graficoPantalla.putExtra("DATA",Data);
                startActivity(graficoPantalla);
            }
        });
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException
    {
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    public void onResume()
    {
        super.onResume();
        Intent intent = getIntent();
        address = intent.getStringExtra(DispositivosBT.EXTRA_DEVICE_ADDRESS); //**//
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        try
        {
            btSocket = createBluetoothSocket(device);
        } catch(IOException e){
            Toast.makeText(getBaseContext(),"La creacion del Socket fallo", Toast.LENGTH_LONG).show();
        }

        try {
            btSocket.connect();

        } catch(IOException e){
            try{
                btSocket.close();
            }catch(IOException e2){}
        }
        MyConexionBT = new ConnectedThread(btSocket);
        MyConexionBT.start();

    }


    public void onPause()
    {
        super.onPause();
        try
        {
            btSocket.close();
        } catch (IOException e2){}


    }


    private void VerificarEstadoBT(){
        if(btAdapter == null) {
            Toast.makeText(getBaseContext(), " El dispositivo no soporta Bluetooth", Toast.LENGTH_LONG).show();
        }
        else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent,1);
            }
        }
    }


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
    }
}
