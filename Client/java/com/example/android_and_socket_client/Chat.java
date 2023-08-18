package com.example.android_and_socket_client;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class Chat extends AppCompatActivity {

    private TextView hiText;
    private EditText typingText;
    private Button sendButton;
    private Button leaveButton;
    private TextView chatTextView;
    private Socket socket;
    private PrintWriter output;
    private BufferedReader input;
    private Thread thread;
    private String ip;
    private int port;
    private Socket clientSocket;//客戶端的socket
    private BufferedWriter bw;  //取得網路輸出串流
    private BufferedReader br;  //取得網路輸入串流
    private String tmp;         //做為接收時的緩存
    private JSONObject jsonWrite, jsonRead; //從java伺服器傳遞與接收資料的json
    private String name;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        hiText = findViewById(R.id.HiText);
        typingText = findViewById(R.id.TypingText);
        sendButton = findViewById(R.id.SendButton);
        chatTextView = findViewById(R.id.chatTextView);
        leaveButton = findViewById(R.id.LeaveButton);

        name = getIntent().getStringExtra("name");
        hiText.setText("Hi, " + name + "!");

        ip = getIntent().getStringExtra("ip");
        port = getIntent().getIntExtra("port", 0);

        thread=new Thread(Connection);
        thread.start();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        leaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call a method to handle disconnection and return to the previous activity
                disconnectAndReturnToPreviousActivity();
            }
        });
    }

    private Runnable Connection=new Runnable(){
        @Override
        public void run() {
            // TODO Auto-generated method stub
            try{
                //建立連線
                clientSocket = new Socket(ip, port);
                //取得網路輸出串流
                bw = new BufferedWriter( new OutputStreamWriter(clientSocket.getOutputStream()));
                //取得網路輸入串流
                br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                // 发送自己的名字给服务器
                bw.write(name + "\n");
                bw.flush(); // 立即发送

                //檢查是否已連線
                while (clientSocket.isConnected()) {
                    //宣告一個緩衝,從br串流讀取 Server 端傳來的訊息
                    tmp = br.readLine();

                    if (tmp != null) {
                        try {
                            // Parse the JSON data
                            JSONObject jsonRead = new JSONObject(tmp);

                            // Extract "name" and "message" fields from the JSON
                            String name = jsonRead.getString("name");
                            String message = jsonRead.getString("message");

                            // Display the formatted message in chatTextView
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    chatTextView.append(name + ": " + message + "\n");
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                            // Handle JSON parsing error if needed
                        }
                    }
                }
            }catch(Exception e){
                //當斷線時會跳到 catch,可以在這裡處理斷開連線後的邏輯
                e.printStackTrace();
                Log.e("text","Socket連線="+e.toString());
                finish();    //當斷線時自動關閉 Socket
            }
        }
    };
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            //傳送離線 Action 給 Server 端
            jsonWrite = new JSONObject();
            jsonWrite.put("action","離線");

            //寫入
            bw.write(jsonWrite + "\n");
            //立即發送
            bw.flush();

            //關閉輸出入串流後,關閉Socket
            bw.close();
            br.close();
            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @SuppressLint("StaticFieldLeak")
    private void sendMessage() {
        final String message = typingText.getText().toString();
        if (!message.isEmpty()) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    try {
                        // Create a JSON object to package the message
                        JSONObject jsonWrite = new JSONObject();
                        jsonWrite.put("action", "connect");
                        jsonWrite.put("name", name);
                        jsonWrite.put("message", message);

                        // Convert JSON object to string and write to the network output stream
                        bw.write(jsonWrite.toString() + "\n");
                        bw.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    // Update UI components here (e.g., chatTextView.append)
                    typingText.setText("");
                }
            }.execute();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void disconnectAndReturnToPreviousActivity() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    // Send a disconnect action to the server
                    JSONObject jsonWrite = new JSONObject();
                    jsonWrite.put("name", name);
                    jsonWrite.put("action", "disconnect");
                    jsonWrite.put("message", name + " has left.");
                    bw.write(jsonWrite.toString() + "\n");
                    bw.flush();

                    // Close the output and input streams, and the clientSocket
                    bw.close();
                    br.close();
                    clientSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                // Finish the current activity and return to the previous one
                finish();
            }
        }.execute();
    }


}
