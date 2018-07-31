package com.example.david.athletetracker;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;



public class VideoList extends AppCompatActivity {


    String date;
    String time;
    String maxVel;
    String buffer;
    private  File path = new File(Environment.getExternalStorageDirectory() + "/Atlethe Tracker/");
    private  File[] files = path.listFiles();



    List<String> item = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // para quitar titulo
        setContentView(R.layout.activity_videos_list);
        item = new ArrayList<String>();

        //ver como ordenar por ultima fecha
        for(int i = 0; i < files.length; i++)
        {
            File file = files[i];
            if (file.isDirectory()) // No hace falta
                item.add(file.getName() + "/");
            else {
                buffer = file.getName();
                // Los archivos se guardan con el formato "yyyyMMddHHmmSS.avi"
                date = buffer.substring(0, 4) + "/" + buffer.substring(4, 6) + "/" + buffer.substring(6, 8);
                time = buffer.substring(8, 10) + ":" + buffer.substring(10, 12) + ":" + buffer.substring(12, 14);
                maxVel = "Max. Vel:  " + buffer.substring(14, 18);
                item.add(date + "\t" + "\t" + "\t" + "\t" + "\t" + "\t" + "\t" + "\t" + "\t" + "\t" + "\t" + "\t" + maxVel + "\n" + time);
            }
        }


        //Mostramos la ruta en el layout
        // TextView ruta = (TextView) findViewById(R.id.IdTitulo);
        // ruta.setText(Environment.getExternalStorageDirectory() + "/Atlethe Tracker/");

        //Localizamos y llenamos la lista
        ListView lstOpciones = (ListView) findViewById(R.id.IdLista);
        ArrayAdapter<String> fileList = new ArrayAdapter<String> (this, android.R.layout.simple_list_item_1, item);
        lstOpciones.setAdapter(fileList);

        // Accion para realizar al pulsar sobre la lista
        lstOpciones.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {

                // Devuelvo los datos a la activity principal
                Intent videoPlay = new Intent(VideoList.this, VideoPlay.class);
                videoPlay.putExtra("filename", files[position].getName());
                setResult(RESULT_OK, videoPlay);

                startActivity(videoPlay);
            }
        });
    }
}





