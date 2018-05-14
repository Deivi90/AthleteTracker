package com.example.david.athletetracker;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import org.opencv.videoio.VideoWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.widget.Toast;




public class CameraRecorder extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {


    //Inicializaciones Bluetooth
    TextView IdBufferIn;
    Handler bluetoothIn;
    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;      //Bluetooth local
    private BluetoothSocket btSocket = null;        // Socket para la comm
    private StringBuilder DataStringIn = new StringBuilder();
    CameraRecorder.ConnectedThread myConexionBt;
    boolean connectionEstablished= false;

    private double velData = 0; // se guardan los ultimos 10 datos recibidos
    ArrayList<Double> velDataList = new ArrayList<Double>();
    ArrayList<Integer> velDataIndex = new ArrayList<>();
    String dataInPrint = new String();
    private enum bluetoothMsg{
        Finish, Record, CalStart, CalEnd;
    }
    boolean recording = false;
    boolean calibration = false;

    
    // Identificador unico de servicio
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    String address = null;

    //Inicializaciones Camara
    private static String TAG = "Camera";
    CustomCameraView javaCameraView;  // Instancia a la camara view
    Mat mTemp;
    VideoWriter cameraVideo;
    String filePath;

    BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS: {
                    javaCameraView.enableView();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        this.supportRequestWindowFeature(Window.FEATURE_NO_TITLE); // Para que no aparezca la barra de titulo
        setContentView(R.layout.activity_camera_recorder);

        javaCameraView = (CustomCameraView) findViewById(R.id.java_camera_view);
        //javaCameraView.setCameraIndex(0);     // 0 for rear 1 for front
        javaCameraView.setVisibility(SurfaceView.VISIBLE);  // Set the visibility state of this view
        javaCameraView.setCvCameraViewListener(this);  //**//

        IdBufferIn = (TextView) findViewById(R.id.IdBufferIn);

        //https://stackoverflow.com/questions/11407943/this-handler-class-should-be-static-or-leaks-might-occur-incominghandler
        bluetoothIn = new Handler(new Handler.Callback() {
            public boolean handleMessage(Message msg) {

                bluetoothMsg incommingMsg;
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    DataStringIn.append(readMessage);
                    if(DataStringIn.charAt(DataStringIn.length() - 1) == '#'){ //Uso el caracter # para separar los datos
                        dataInPrint = DataStringIn.substring(0, DataStringIn.length() - 1);
                        IdBufferIn.setText(dataInPrint);  // Lo que llega por bluetooth lo mando al Idbufferin
                        if(Character.isDigit(dataInPrint.charAt(0))) {
                            velData = (Double.parseDouble(dataInPrint));
                            velDataList.add((Double.parseDouble(dataInPrint)));
                        }
                        else{
                            incommingMsg = bluetoothMsg.valueOf(dataInPrint);
                            switch (incommingMsg){
                                case Finish:
                                    finish();
                                    break;
                                case Record:
                                    recording = true;
                                    break;
                                case CalStart:
                                    calibration = true;
                                    break;
                                case CalEnd:
                                    calibration = false;
                                    break;
                            }
                        }
                        DataStringIn.delete(0, DataStringIn.length());
                    }
                }
                return false;
            }
        });
        btAdapter = BluetoothAdapter.getDefaultAdapter();
    }


    @Override
    protected void onPause(){
        super.onPause();
        // Si esta la camara se deshabilita
        if (javaCameraView != null)
            javaCameraView.disableView();
        if(btSocket != null) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                finish();
            }
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (javaCameraView != null)
            javaCameraView.disableView();
    }

    @Override
    public void finish() {
        super.finish();
        if(connectionEstablished || address == null) {
            Intent processIntent = new Intent(CameraRecorder.this, VideoProcessing.class);  // intent a la proxima activity
            processIntent.putExtra("path", filePath);
            processIntent.putExtra("velData", velDataList);
            processIntent.putExtra("Index", velDataIndex);
            startActivity(processIntent);
        }
    }


    @Override
    protected void onResume(){
        super.onResume();

        if(OpenCVLoader.initDebug()){
            Log.i(TAG,"OpenCv Loaded successfully");
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);    //LLama al BaseLoaderCallback que se creó antes
        }
        else{
            Log.i(TAG,"OpenCV Failed to load");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, this, mLoaderCallBack);
        }

        // https://developer.android.com/guide/topics/connectivity/bluetooth.html
        Intent intent = getIntent(); // Creo un intent para recuperar la data de la activity anterior ///Deberia ir en onCreate//
        address = intent.getStringExtra(DispositivosBt.EXTRA_DEVICE_ADDRESS); // Recupero la data de la anterior activity
        if(address != null){        // Si se accede sin activar el Bt el address es null
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
                    Intent DispositivosBtIntent = new Intent(CameraRecorder.this, DispositivosBt.class);
                    startActivity(DispositivosBtIntent);
                    finish();
                }catch(IOException e2){
                    Toast.makeText(getBaseContext(),"No se cerro el Socket", Toast.LENGTH_LONG).show();
                }
            }
            myConexionBt = new ConnectedThread(btSocket);
            myConexionBt.start(); // Agregar algo para cuando la conexion falla.
            connectionEstablished = true;

        }

        else {
            Toast.makeText(getBaseContext(), "Se accedio sin Bluetooth", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        //inicializamos la matriz mBgra
        height = 480;
        width = 640;
        mTemp = new Mat(height, width, CvType.CV_8UC4);
        // https://stackoverflow.com/questions/44393509/android-opencv-record-video
        // https://stackoverflow.com/questions/41632203/opencv3-2-videowriter-in-android
        // Se crea el directorio y nombre del video
        File sddir = Environment.getExternalStorageDirectory();
        File vrdir = new File(sddir, "Atlethe Tracker");    // Direccion de la carpeta
        // Si la carpeta no existe se crea
        // El nombre del archivo tendra fecha y hora
        SimpleDateFormat videoTime = new SimpleDateFormat("yyyyMMddHHmmss");
        filePath = vrdir.getAbsolutePath() + "/" + videoTime.format(new Date());
        Log.i("Camera", "Direccion:  " + filePath);

        // Configuracion Camara
        javaCameraView.setRecordingHint();
        //  List<Camera.Size> res = javaCameraView.getResolutionList();
        // javaCameraView.setResolution(res.get(7));
        //  List<int[]> tam = javaCameraView.getSupportedRangeFPS();
        try {
            javaCameraView.setResolution(height,width);
        }catch (Exception e){
            Log.i("Camera", "No acepta 640x480 se usa el default");
            // Toast
        }
        try{
            javaCameraView.setPreviewFPS();
        }catch (Exception e){
            Log.i("Camera", "No acepta 60fps");
            List<int[]> listFPS = javaCameraView.getSupportedRangeFPS();
            javaCameraView.setPreviewFPS(listFPS.get(listFPS.size()-1)[0]/1000, listFPS.get(listFPS.size()-1)[1]/1000 );
        }

    }


    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
      //  Log.i(TAG, "onCameraFrame: Grabando");

        mTemp = inputFrame.rgba();
        if (cameraVideo == null) {
            cameraVideo = new VideoWriter(filePath + "TEMP.avi", VideoWriter.fourcc('M', 'J', 'P', 'G'), 15, mTemp.size());
            Log.d(TAG,"mVideoWriter good : " + cameraVideo);
            cameraVideo.open(filePath + "TEMP.avi", VideoWriter.fourcc('M', 'J', 'P', 'G'), 15, mTemp.size());
            Log.i(TAG, "onCameraFrame: recordFilePath" + filePath);
        }
        if (recording) {
            cameraVideo.write(mTemp);
            if (address != null)
                velDataIndex.add(velDataList.size() - 1);
        }
        else if(calibration){
            Imgproc.putText(mTemp,"Calibrando....",new Point(200,300),Core.FONT_HERSHEY_SIMPLEX ,
                    1,new Scalar(255, 255, 255),4 );
        }
        else{
            Imgproc.putText(mTemp,"PressTart",new Point(320,240),Core.FONT_HERSHEY_SIMPLEX ,
                    1,new Scalar(255, 255, 255),4 );
        }
        return mTemp;
    }

    @Override
    public void onCameraViewStopped() {
        javaCameraView.setVisibility(SurfaceView.INVISIBLE);  // Set the visibility state of this view
        cameraVideo.release();
        mTemp.release();
    }


    private class ConnectedThread extends Thread
    {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private ConnectedThread(BluetoothSocket socket){
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
            byte[] buffer = new byte[1];
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