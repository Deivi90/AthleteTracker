package com.example.david.athletetracker;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    Button btnVideoList;
    Dialog myDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); //Elijo el layout de la interfaz

        // Defino cada uno de los elementos del layout
        btnVideoList = (Button) findViewById(R.id.btnVideoList);
        myDialog = new Dialog(this);

        //Funcion de los botones
        btnVideoList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent videoList = new Intent(MainActivity.this, VideoList.class);  // intent a la proxima activity
                startActivity(videoList);
            }
        });
    }

    // Defino un Popup que aparece cuando se apreta el boton Grabar
    public void ShowPopup(View v){

        TextView txtclose;
        Button btnBtActivated, btnBtDisable;
        myDialog.setContentView(R.layout.popup_layout);
        txtclose = (TextView) myDialog.findViewById(R.id.txtclose);
        btnBtActivated = (Button) myDialog.findViewById(R.id.btnBtActivated);
        btnBtDisable = (Button) myDialog.findViewById(R.id.btnBtDisable);

        txtclose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myDialog.dismiss();
            }
        });

        btnBtDisable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myDialog.dismiss();
                Intent cameraIntent = new Intent(MainActivity.this, CameraRecorder.class); // Intent a la siguiente activity
                startActivity(cameraIntent);     //Voy a la proxima activity
            }
        });

        btnBtActivated.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myDialog.dismiss();
                Intent UserInterfaceIntent = new Intent(MainActivity.this, DispositivosBt.class); // Intent a la siguiente activity
                startActivity(UserInterfaceIntent);     //Voy a la proxima activity
            }
        });

        myDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        myDialog.show();
    }
}
