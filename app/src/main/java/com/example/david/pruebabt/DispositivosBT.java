package com.example.david.pruebabt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

//**// Significa que se debe cambiar algo para que funcione en otra app

public class DispositivosBT extends AppCompatActivity {

    //TAG se usa para depuracion de LOGCAT, es el identificador del Log para debbugear
    private static final String TAG = "DispositivoBT";
    // RequestCode para el metodo OnActivityResult
    static final int BLUETOOTH_CONNECTION_REQUEST = 1; // Le pongo estatico pq es igual para todas las instancias
                                                       // final pq es constante

    ListView IdLista;
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    private BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();  //El adaptador Bt del celular
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;        //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispositivos_bt);  //Elijo la Acctivity(Layout) que se va a presentar
    }


    public void onResume(){

        super.onResume();
        VerificarEstadoBT();

        // Este array contiene la lista de dispositivos Bluetooth vinculados
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.nonmbre_dispositivos);  //*// Cambiar  nombre de layout

        //ListView muesttra los dispositivos vinculados
        IdLista = (ListView) findViewById(R.id.IdLista);
        IdLista.setAdapter(mPairedDevicesArrayAdapter);
        IdLista.setOnItemClickListener(mDeviceClickListener);

        //mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        // Creo un set con los bluetooth detectados/emparejados.
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        if (pairedDevices.size() > 0){
            for (BluetoothDevice device : pairedDevices){
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());  //Tomo el nombre y direccion de todos los Bt detectados
            }
        }
    }

    //Extrae la MAC
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener()
    {
        public void onItemClick (AdapterView<?> av, View v, int arg2, long arg3)   //Ver
        {
            // Get the device MAC address, the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Intent para ir al proxima activity
            Intent UserInterfaceIntent = new Intent(DispositivosBT.this, UserInterface.class); //**// nombre de la clase en la que estoy y a la que voy

            UserInterfaceIntent.putExtra(EXTRA_DEVICE_ADDRESS, address);  // Envio a la proxima activity  la direccion del BT
            startActivity(UserInterfaceIntent);     //Voy a la proxima activity
            finish();
        }
    };


    private void VerificarEstadoBT(){

        //mBtAdapter =  BluetoothAdapter.getDefaultAdapter();
        if(mBtAdapter==null) {
            Toast.makeText(getBaseContext(), " El dispositivo no soporta Bluetooth", Toast.LENGTH_LONG).show();
            //Finalizamos la aplicacion
            finish();
        }
        else {
            if (mBtAdapter.isEnabled()) {
                // La depuracion avisa que ..
                Log.d(TAG, "...Bluetooth Activado...");
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); //Intent para habilitar el bt
                startActivityForResult(enableBtIntent,BLUETOOTH_CONNECTION_REQUEST); // Se inicia la actividad, se espera un resultado. 1 es el requestcode, capaz conviene ponerle un nombre
            }
        }
    }


    // onActivityResult se ejecuta luego de ejecutar startActivityForResult
    //Si no se acepto la conexion Bluetooth se cierra todo
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==BLUETOOTH_CONNECTION_REQUEST && resultCode == RESULT_CANCELED)
        {
            finish();   // Si se rechaza la conexion Bluetooth se finaliza la aplicacion
        }
    }





}
