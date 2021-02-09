package com.jb.androidwebrtc;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class TelepresenceSelectorActivity extends AppCompatActivity {

    RadioButton radioButtonMax;
    RadioButton radioButtonElf;
    RadioButton radioButtonPad;
    RadioButton radioButtonHd;
    Button startBtn;
    TextView tvRoomName;

    boolean usePadCam = true;
    boolean isElf = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_telepresence_selector);

        radioButtonMax = findViewById(R.id.radioButtonMax);
        radioButtonElf = findViewById(R.id.radioButtonElf);
        radioButtonPad = findViewById(R.id.radioButtonPad);
        radioButtonHd = findViewById(R.id.radioButtonHd);
        startBtn = findViewById(R.id.startBtn);
        tvRoomName = findViewById(R.id.et_room_name);

        radioButtonElf.toggle();
        radioButtonPad.toggle();

        Button btn = (Button)findViewById(R.id.startBtn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent tpIntent = new Intent(TelepresenceSelectorActivity.this, TelepresenceActivity.class);
                tpIntent.putExtra("USE_PAD_CAM", usePadCam);
                tpIntent.putExtra("IS_ELF", isElf);
                String roomId = tvRoomName.getText().toString();
                Log.i("Tai", "roomId: " + roomId);
                if (!roomId.matches("")) {
                    tpIntent.putExtra("ROOM_NAME", roomId);
                    startActivity(tpIntent);
                } else {
                    Toast.makeText(getApplicationContext(), "Enter a room name first", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        String str="";
        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radioButtonElf:
                if(checked){
                    radioButtonMax.setChecked(false);
                    isElf = true;
                    str = "isElf: " + true;
                }
                break;
            case R.id.radioButtonMax:
                if(checked) {
                    radioButtonElf.setChecked(false);
                    isElf = false;
                    str = "isElf: " + false;
                }
                break;
            case R.id.radioButtonHd:
                if(checked) {
                    radioButtonPad.setChecked(false);
                    usePadCam = false;
                    str = "usePadCam: " + false;
                }
                break;
            case R.id.radioButtonPad:
                if(checked) {
                    radioButtonHd.setChecked(false);
                    usePadCam = true;
                    str = "usePadCam: " + true;
                }
                break;
        }
        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
    }
}
