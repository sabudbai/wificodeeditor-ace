package com.sirajinhoapp.wificodeeditor;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import javax.net.ssl.SNIHostName;


public class MainActivity extends AppCompatActivity {

    private Handler handler = new Handler();
    Spinner spinner;
    SwitchCompat startSwitch, newTabSwitch;
    EditText portEdit, workspaceEdit;
    TextView address;
    ImageButton shareAddress;

    private String workspaceName = "wifiworkspace";
    SimpleWebServer simpleWebServer;

    SharedPreferences prefs;
/*
    static {
        System.loadLibrary("8cc");
    }*/


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.mipmap.ic_launcher);

        init();



        prefs = this.getSharedPreferences(
                "WifiCodeEditor", Context.MODE_PRIVATE);

        shareAddress = (ImageButton) findViewById(R.id.shareAddress);
          startSwitch = (SwitchCompat) findViewById(R.id.startSwitch);
        newTabSwitch = (SwitchCompat) findViewById(R.id.openFilesSwitch);
        portEdit = (EditText) findViewById(R.id.portEditText);
        workspaceEdit = (EditText) findViewById(R.id.workspaceEditText);
        address = (TextView) findViewById(R.id.editorAddress);

        setupViews();

        File directory = new File(Environment.getExternalStorageDirectory() + File.separator + workspaceName);
        directory.mkdirs();

        Config.init(Environment.getExternalStorageDirectory() + File.separator + workspaceName);

        Config.getCurrent().ip = getIp();
        Config.getCurrent().listenPort = prefs.getInt("port", 8080);
        Config.getCurrent().running = prefs.getBoolean("running", false);

        if(Config.getCurrent().running) {
            startSwitch.setChecked(true);
            portEdit.setText(String.valueOf(Config.getCurrent().listenPort));
            address.setText("http://"+getIp()+":"+Config.getCurrent().listenPort);
        }



        workspaceEdit.setEnabled(true);
        workspaceEdit.setText(Environment.getExternalStorageDirectory() + File.separator + workspaceName);
    }


    public String getIp() {
        WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        String ipAddress = Formatter.formatIpAddress(ip);
        Config.getCurrent().ip = ipAddress;
        return ipAddress;
    }

    public void setupViews() {
        shareAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Config.getCurrent().running) {
                    shareCodeEditor();
                }
            }
        });

        newTabSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Config.getCurrent().openInNewTab = isChecked;
                try {
                    ProcessBuilder builder = new ProcessBuilder();
                    builder.command((new String[]{"toybox_armv7l"}));
                    builder.directory(getApplicationContext().getFilesDir());
                    builder.redirectErrorStream();
                    Process process = builder.start();
                    try {
                        process.waitFor();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    String line = "";
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    Log.i("sdr", "executing recovery script");
                    while ((line = reader.readLine()) != null) {
                        Toast.makeText(getApplicationContext(),line, Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        startSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    int port = (portEdit.getText().toString().isEmpty()) ? 8080 : Integer.valueOf(portEdit.getText().toString());
                    if(port < 65535) {
                        Config.getCurrent().listenPort = port;

                        String ip = getIp();
                        address.setText("http://" + ip + ":" + port);


                        simpleWebServer = new SimpleWebServer(port, getAssets(), Environment.getExternalStorageDirectory() + File.separator + workspaceName, workspaceName);
                        simpleWebServer.context = getApplicationContext();
                        simpleWebServer.start();

                        getApplicationContext().startService(new Intent(MainActivity.this, HttpService.class));
                        Config.getCurrent().running = true;

                        Snackbar.make(portEdit.getRootView(), "Wifi Code Editor started on Port " + port, Snackbar.LENGTH_LONG).show();
                    } else {
                        Snackbar.make(portEdit.getRootView(), "Port "+ port+ " out of range.", Snackbar.LENGTH_LONG).show();
                    }
                } else {
                    if(simpleWebServer != null) {
                        simpleWebServer.stop();
                        Config.getCurrent().running = false;
                        getApplicationContext().stopService(new Intent(MainActivity.this, HttpService.class));
                        address.setText("");

                        Snackbar.make(portEdit.getRootView(), "Wifi Code Editor stopped.", Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        });

    }


    public void init() {

        copyAssets();

    }

    private void copyAssets() {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e("tag", "Failed to get asset file list.", e);
        }
        if (files != null) for (String filename : files) {
                InputStream in = null;
                FileOutputStream out = null;
                try {
                    in = assetManager.open(filename);
                    File outFile = new File(getExternalFilesDir(null), filename);
                    out = new FileOutputStream(outFile);
                    Log.i("file", "file "+ filename);
                    if(filename.equals("toybox-armv7l")) {
                        outFile = new File(getApplicationContext().getFilesDir(), "/"+filename);
                        Log.i("file", "data file "+ getApplicationContext().getFilesDir()+ "/"+filename);
                        outFile.setExecutable(true);
                        out =  getApplicationContext().openFileOutput(filename, Context.MODE_PRIVATE);

                    }
                    copyFile(in, out);
                    in.close();
                    out.close();
                } catch (IOException e) {
                    Log.e("tag", "Failed to copy asset file: " + filename, e);

                }


        }


    }


    private void copyFile(InputStream in, FileOutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        prefs.edit().putInt("port", Config.getCurrent().listenPort).apply();
        prefs.edit().putBoolean("running", Config.getCurrent().running).apply();

        //androidWebServer.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        prefs.edit().putString("ip", Config.getCurrent().ip).apply();
        prefs.edit().putInt("port", Config.getCurrent().listenPort).apply();
        prefs.edit().putBoolean("running", Config.getCurrent().running).apply();
      //  simpleWebServer.stop();
       // Config.getCurrent().running = false;
       // getApplicationContext().stopService(new Intent(MainActivity.this, HttpService.class));
      //  Toast.makeText(getApplicationContext(), "Code Editor stopped!", Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_share) {
            shareCodeEditor();
            return true;
        } else if(id == R.id.action_rate) {
            rateCodeEditor();
        }

        return super.onOptionsItemSelected(item);
    }

    public void rateCodeEditor() {
        Uri uri = Uri.parse("market://details?id=" + this.getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + this.getPackageName())));
        }
    }

    public void shareCodeEditor() {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        String shareBody = "Wifi Code Editor\nhttp://"+getIp()+":"+Config.getCurrent().listenPort;
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Wifi Code Editor");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, "Share via"));
        Snackbar.make(portEdit.getRootView(), wificompile(), Snackbar.LENGTH_LONG).show();
    }




    public native String wificompile();
}
