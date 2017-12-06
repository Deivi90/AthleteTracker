package com.example.david.pruebabt;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class Grafico extends AppCompatActivity {

    LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>();  //Datos a graficar
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graficos);
        GraphView graph = (GraphView) findViewById(R.id.Id_grafico);
        // Recuperos los datos de la intent anterior
        double[] datos = ( double[]) getIntent().getSerializableExtra("DATA");

        series = new LineGraphSeries<DataPoint>();

        //Log.d("Grafico","llega a OnCreate");
        double x = -0.1;
        double y = 0;
        int i=0;
        while(i<10){
            y=datos[i];
            x= x + 0.1;
            series.appendData(new DataPoint(x, y), true, 100);
            i++;
        }
        graph.addSeries(series);
    }
}