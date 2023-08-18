package com.example.android_and_socket;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicReference;
import java.io.BufferedWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Chat extends AppCompatActivity {

    private TextView hiText;
    private static TextView chatTextView;
    private EditText typingText;
    private Button sendButton;
    private Button leaveButton;
    private static int serverport = 7100;
    private static ServerSocket serverSocket;
    private static int count = 0;
    private static ArrayList clients = new ArrayList();
    private ExecutorService executor = Executors.newFixedThreadPool(10);
    private String name;
    private int leave = 0;
    private static boolean isServerClosed = false;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        hiText = findViewById(R.id.HiText);
        chatTextView = findViewById(R.id.chatTextView);
        typingText = findViewById(R.id.TypingText);
        sendButton = findViewById(R.id.SendButton);
        leaveButton = findViewById(R.id.LeaveButton);

        name = getIntent().getStringExtra("name");
        hiText.setText("Hi, " + name + "!");

        // Start accepting clients in a separate thread
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(serverport);
                    Log.d("Chat", "Server is start.");
                    chatTextView.append("Server is start. (" + getLocalIpAddress() + ")\n");

                    while (!serverSocket.isClosed()) {
                        waitNewClient();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = typingText.getText().toString();
                if (!message.isEmpty()) {
                    // Send the message to all connected clients
                    sendMessageToClients(message);

                    // Update UI components
                    chatTextView.append(name + ": " + message + "\n");
                    typingText.setText("");
                }
            }
        });

        leaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectAndReturnToPreviousActivity();
            }
        });
    }

    public void waitNewClient() {
        try {
            Socket socket = serverSocket.accept();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ++count;
                    Log.d("Chat", "現在使用者個數：" + count);

                    // 呼叫加入新的 Client 端
                    try {
                        addNewClient(socket);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 加入新的 Client 端
    public void addNewClient(final Socket socket) throws IOException {
        // 以新的執行緒來執行
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 增加新的 Client 端
                    clients.add(socket);
                    // 取得網路串流
                    BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    // 接收客户端发送的名字
                    String clientName = br.readLine();
                    String clientIp = socket.getRemoteSocketAddress().toString();
                    String welcome = "Welcome " + clientName + " join us.";
                    // 在 chatTextView 中显示连接消息
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            chatTextView.append(clientName + " (" + clientIp + ") Connected.\n");

                            chatTextView.append(name + ": "+ welcome + "\n");
                        }
                    });
                    sendMessageToClients(welcome);

                    // 當Socket已連接時連續執行
                    while (socket.isConnected()) {
                        // 取得網路串流的訊息
                        String msg= br.readLine();

                        if(msg==null){
                            System.out.println("Client Disconnected!");
                            break;
                        }
                        //輸出訊息
                        try {
                            // Parse the JSON data
                            JSONObject jsonRead = new JSONObject(msg);

                            // Extract "name" and "message" fields from the JSON
                            String action = jsonRead.getString("action");
                            Log.d("Chat", "action: " + action);
                            String clientname = jsonRead.getString("name");
                            String message = jsonRead.getString("message");

                            // Display the formatted message in chatTextView
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (action.equals("disconnect")){
                                        leave = 1;
                                        chatTextView.append(name + ": " + message + "\n");
                                    } else {
                                        chatTextView.append(clientname + ": " + message + "\n");
                                    }
                                }
                            });
                            if (leave == 1) {
                                sendMessageToClients(message);
                                leave = 0;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            // Handle JSON parsing error if needed
                        }
                        // 廣播訊息給其它的客戶端
                        castMsg(msg);
                    }
                } catch (IOException e) {
                    e.getStackTrace();
                }
                finally{
                    // 移除客戶端
                    clients.remove(socket);
                    --count;
                    System.out.println("現在使用者個數："+count);
                }
            }
        });

        // 啟動執行緒
        t.start();
    }

    // 廣播訊息給其它的客戶端
    public static void castMsg(String Msg){
        // 創造socket陣列
        Socket[] clientArrays =new Socket[clients.size()];
        // 將 clients 轉換成陣列存入 clientArrays
        clients.toArray(clientArrays);
        // 走訪 clientArrays 中的每一個元素
        for (Socket socket : clientArrays) {
            try {
                // 創造網路輸出串流
                BufferedWriter bw;
                bw = new BufferedWriter( new OutputStreamWriter(socket.getOutputStream()));
                // 寫入訊息到串流
                bw.write(Msg+"\n");
                // 立即發送
                bw.flush();
            } catch (IOException e) {}
        }
    }

    private void sendMessageToClients(final String message) {
        if (isServerClosed) {
            return;
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Create a JSON object for the message
                JSONObject jsonMessage = new JSONObject();
                try {
                    jsonMessage.put("name", name);
                    jsonMessage.put("message", message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // Convert the ArrayList to an array of sockets
                Socket[] clientArrays = (Socket[]) clients.toArray(new Socket[0]);

                // Iterate through all connected client sockets and send the message
                for (Socket socket : clientArrays) {
                    try {
                        // Check if the socket is not null
                        if (socket != null) {
                            // Create a network output stream
                            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                            // Write the JSON message to the stream
                            bw.write(jsonMessage.toString() + "\n");
                            // Immediately send
                            bw.flush();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();
    }

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()
                            && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("WifiPreference IpAddress", ex.toString());
        }
        return null;
    }

    @SuppressLint("StaticFieldLeak")
    private void disconnectAndReturnToPreviousActivity() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    // Broadcast the message to all connected clients
                    JSONObject jsonWrite = new JSONObject();
                    jsonWrite.put("name", name);
                    jsonWrite.put("action", "broadcast");
                    jsonWrite.put("message", "Server closed. Please press 'Leave' button");
                    castMsg(jsonWrite.toString());

                    // Set the server closed flag
                    isServerClosed = true;

                    // Close the output and input streams, and the serverSocket
                    serverSocket.close();
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
