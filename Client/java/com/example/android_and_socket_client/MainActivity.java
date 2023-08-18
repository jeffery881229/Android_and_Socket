package com.example.android_and_socket_client;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private EditText nameEditText;
    private EditText ipEditText;
    private EditText portEditText;
    private Button connectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nameEditText = findViewById(R.id.NameText);
        ipEditText = findViewById(R.id.IPText);
        portEditText = findViewById(R.id.PortText);
        connectButton = findViewById(R.id.ConnectButton);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToChat();
            }
        });
    }

    private void connectToChat() {
        String name = nameEditText.getText().toString();
        String ip = ipEditText.getText().toString();
        int port = Integer.parseInt(portEditText.getText().toString());

        Intent chatIntent = new Intent(MainActivity.this, Chat.class);
        chatIntent.putExtra("name", name);
        chatIntent.putExtra("ip", ip);
        chatIntent.putExtra("port", port);
        startActivity(chatIntent);
    }
}
