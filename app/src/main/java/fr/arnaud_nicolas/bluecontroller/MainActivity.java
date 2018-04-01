package fr.arnaud_nicolas.bluecontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter _btAdapter;
    Button _btnPaired;
    BluetoothDevice _device;
    BluetoothSocket _socket;
    FallbackBluetoothSocket _fbsocket;
    Context _ctx;
    TextView _textView;

    ImageButton _topButton;
    ImageButton _bottomButton;
    ImageButton _leftButton;
    ImageButton _rightButton;
    FloatingActionButton _btA;
    FloatingActionButton _btB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        _btnPaired = (Button)findViewById(R.id.button);
        _ctx = getApplicationContext();
        _textView = (TextView) findViewById(R.id.tfield);
        _topButton = (ImageButton) findViewById(R.id.btTopArrow);
        _bottomButton = (ImageButton) findViewById(R.id.btBottomArrow);
        _rightButton = (ImageButton) findViewById(R.id.btRightArrow);
        _leftButton = (ImageButton) findViewById(R.id.btLeftArrow);
        _btA = (FloatingActionButton) findViewById(R.id.btA);
        _btB = (FloatingActionButton) findViewById(R.id.btB);

        _btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(_btAdapter == null)
        {
            Toast.makeText(getApplicationContext(), "Bluetooth not available", Toast.LENGTH_LONG).show();
        }
        else
        {
            if(!_btAdapter.isEnabled()){
                startActivityForResult(
                        new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                        1
                );
            }
        }

        _topButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                sendMessage("top");
            }
        });
        _bottomButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                sendMessage("bottom");
            }
        });
        _leftButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                sendMessage("left");
            }
        });
        _rightButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                sendMessage("right");
            }
        });
        _btA.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                sendMessage("A");
            }
        });
        _btB.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                sendMessage("B");
            }
        });

        _btnPaired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String addr = "B8:27:EB:79:B6:3C";

                if(_device == null){
                    Set<BluetoothDevice> pairedDevices = _btAdapter.getBondedDevices();
                    ArrayList list = new ArrayList();

                    if(pairedDevices.size() > 0)
                    {

                        for(BluetoothDevice device : pairedDevices){
                            Log.d("paired", device.getAddress());
                            if(device.getAddress().equals(addr))
                            {
                                _device = device;
                            }
                        }
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(), "no devices paired", Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                if(_device == null)
                {
                    Toast.makeText(getApplicationContext(), "can't find minion3", Toast.LENGTH_LONG).show();
                    return;
                }


                TextView tv =(TextView) findViewById(R.id.tfield);

                if(_socket == null && _fbsocket == null)
                {
                    Boolean connected = false;
                    try{
                        _socket = _device.createInsecureRfcommSocketToServiceRecord(_device.getUuids()[0].getUuid());
                        _btAdapter.cancelDiscovery();

                        try {
                            _socket.connect();
                            connected = true;
                        } catch (IOException e) {
                            //try the fallback
                            try {
                                _fbsocket = new FallbackBluetoothSocket(_socket);
                                _fbsocket.connect();
                                connected = true;
                            } catch (FallbackException e1) {
                                Log.w("BT", "Could not initialize FallbackBluetoothSocket classes.", e);
                            } catch (IOException e1) {
                                Log.w("BT", "Fallback failed. Cancelling.", e1);
                            }
                        }
                    }
                    catch (IOException e){
                        Toast.makeText(_ctx, "no connection", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (connected){
                        _callback();
                    }

                }
                else
                {
                    _callback();
                }
            }
        });
    }

    private void _callback(){
        _textView.setText("Connected");
        _btnPaired.setVisibility(View.GONE);
        sendMessage("start");
    }

    private void sendMessage(String pMessage){
        if(_socket == null && _fbsocket == null)
        {
            return;
        }

        try
        {
            _socket.getOutputStream().write(String.valueOf(pMessage).getBytes());
        }
        catch(IOException e){
            try{
                _fbsocket.getOutputStream().write(String.valueOf(pMessage).getBytes());
            }
            catch (IOException e2){
                Log.d("stream", "unlucky");
            }
        }
    }

    public static interface BluetoothSocketWrapper {

        InputStream getInputStream() throws IOException;

        OutputStream getOutputStream() throws IOException;

        String getRemoteDeviceName();

        void connect() throws IOException;

        String getRemoteDeviceAddress();

        void close() throws IOException;

        BluetoothSocket getUnderlyingSocket();

    }

    public static class NativeBluetoothSocket implements BluetoothSocketWrapper {

        private BluetoothSocket socket;

        public NativeBluetoothSocket(BluetoothSocket tmp) {
            this.socket = tmp;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return socket.getInputStream();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return socket.getOutputStream();
        }

        @Override
        public String getRemoteDeviceName() {
            return socket.getRemoteDevice().getName();
        }

        @Override
        public void connect() throws IOException {
            socket.connect();
        }

        @Override
        public String getRemoteDeviceAddress() {
            return socket.getRemoteDevice().getAddress();
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }

        @Override
        public BluetoothSocket getUnderlyingSocket() {
            return socket;
        }

    }

    public class FallbackBluetoothSocket extends NativeBluetoothSocket {

        private BluetoothSocket fallbackSocket;

        public FallbackBluetoothSocket(BluetoothSocket tmp) throws FallbackException {
            super(tmp);
            try
            {
                Class<?> clazz = tmp.getRemoteDevice().getClass();
                Class<?>[] paramTypes = new Class<?>[] {Integer.TYPE};
                Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                Object[] params = new Object[] {Integer.valueOf(1)};
                fallbackSocket = (BluetoothSocket) m.invoke(tmp.getRemoteDevice(), params);
            }
            catch (Exception e)
            {
                throw new FallbackException(e);
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return fallbackSocket.getInputStream();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return fallbackSocket.getOutputStream();
        }


        @Override
        public void connect() throws IOException {
            fallbackSocket.connect();
        }


        @Override
        public void close() throws IOException {
            fallbackSocket.close();
        }

    }

    public static class FallbackException extends Exception {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public FallbackException(Exception e) {
            super(e);
        }

    }
}
