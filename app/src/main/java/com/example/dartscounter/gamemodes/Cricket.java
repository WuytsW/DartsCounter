package com.example.dartscounter.gamemodes;

import android.content.Intent;
import android.os.Bundle;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.dartscounter.BaseActivity;
import com.example.dartscounter.R;
import com.example.dartscounter.data.IntentKeys;

public class Cricket extends BaseActivity {

    GridLayout scoreContainer;
    TextView[][] fieldScoresTxt;
    TextView[][] allTxt;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cricket);
        enableImmersive();

        scoreContainer = findViewById(R.id.scoreContainer);

        // Read extras
        Intent intent = getIntent();
        int playerCount = intent.getIntExtra(IntentKeys.EXTRA_PLAYER_COUNT, 2);
        int minCricket = intent.getIntExtra(IntentKeys.EXTRA_MIN_CRICKET, 15);
        String[] names = intent.getStringArrayExtra(IntentKeys.EXTRA_PLAYER_NAMES);


        scoreContainer.setColumnCount(playerCount + 1);
        scoreContainer.setRowCount(23 - minCricket);

        fieldScoresTxt = new TextView[22 - minCricket][playerCount];
        allTxt = new TextView[23-minCricket][playerCount + 1];

        for (int i = 0; i < 22 - minCricket; i++) {
            for (int j = 0; j < playerCount; j++) {
                fieldScoresTxt[i][j] = new TextView(this);
            }
        }



        TextView emptyTxt = new TextView(this);
        scoreContainer.addView(emptyTxt);
        allTxt[0][0] = emptyTxt;

        for (int column = 0; column < playerCount; column++) {
            String name = names[column];
            TextView nameTxt= new TextView(this);
            nameTxt.setText(name + " ");
            scoreContainer.addView(nameTxt);

            allTxt[0][column + 1] = nameTxt;
        }

        for (int row = 1; row < 23 - minCricket; row++) {
            int fieldNumber = row - 1 + minCricket;
            if(fieldNumber == 21) fieldNumber = 25;
            TextView fieldNumberTxt = new TextView(this);
            fieldNumberTxt.setText("Field " + fieldNumber + ": ");
            scoreContainer.addView(fieldNumberTxt);

            allTxt[row][0] = fieldNumberTxt;

            for (int column = 1; column < playerCount + 1; column++) {
                TextView fieldScoreTxt = fieldScoresTxt[row-1][column-1];
                fieldScoreTxt.setText("0");
                scoreContainer.addView(fieldScoreTxt);

                allTxt[row][column] = fieldScoreTxt;
            }
        }




        for (int i = 0; i < 23 - minCricket; i++) {
            for (int j = 0; j < playerCount + 1; j++) {
                allTxt[i][j].setTextSize(50);
            }
        }

    }
}