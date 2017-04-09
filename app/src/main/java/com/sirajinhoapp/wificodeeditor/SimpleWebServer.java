/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sirajinhoapp.wificodeeditor;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.TextUtils;
import android.util.Log;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Implementation of a very basic HTTP server. The contents are loaded from the assets folder. This
 * server handles one request at a time. It only supports GET method.
 */
public class SimpleWebServer implements Runnable {

    private static final String TAG = "SimpleWebServer";

    /**
     * The port number we listen to
     */
    private final int mPort;

    /**
     * {@link android.content.res.AssetManager} for loading files to serve.
     */
    private final AssetManager mAssets;

    /**
     * True if the server is running.
     */
    private boolean mIsRunning;

    /**
     * The {@link java.net.ServerSocket} that we listen to.
     */
    private ServerSocket mServerSocket;
    public Context context;
    private String Filename;
    private String rootDir;
    private String currentDir;
    private String projectFolder;


    private String ip = "192.168.178.30";
    private String port = "8080";

    /**
     * WebServer constructor.
     */
    public SimpleWebServer(int port, AssetManager assets, String projectDir, String projectFolderName) {
        mPort = port;
        mAssets = assets;
        rootDir = projectDir;
        currentDir = rootDir;
        projectFolder = projectFolderName;
        ip = Config.getCurrent().ip;
    }

    /**
     * This method starts the web server listening to the specified port.
     */
    public void start() {
        mIsRunning = true;
        new Thread(this).start();
    }

    /**
     * This method stops the web server
     */
    public void stop() {
        try {
            mIsRunning = false;
            if (null != mServerSocket) {
                mServerSocket.close();
                mServerSocket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing the server socket.", e);
        }
    }

    public int getPort() {
        return mPort;
    }

    @Override
    public void run() {
        try {
            mServerSocket = new ServerSocket(mPort);
            while (mIsRunning) {
                Socket socket = mServerSocket.accept();
                handle(socket);
                socket.close();
            }
        } catch (SocketException e) {
            // The server was stopped; ignore.
        } catch (IOException e) {
            Log.e(TAG, "Web server error.", e);
        }
    }

    /**
     * Respond to a request from a client.
     *
     * @param socket The client socket.
     * @throws IOException
     */


    private void handle(Socket socket) throws IOException {
        BufferedReader reader = null;
        PrintStream output = null;
        InputStream inputStream = null;
        try {
            String route = null;
            String params = null;
            String boundary = "";
            char[] buf = new char[1024*10];

            // Read HTTP headers and parse out the route.
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            inputStream = socket.getInputStream();
            String line;
            String fileContent = "";
            int contentLenght = 0;


            while ((line = reader.readLine()) != null) {
                if (line.startsWith("GET /")) {
                    int start = line.indexOf('/') + 1;
                    int end = line.indexOf(' ', start);
                    route = line.substring(start, end);
                    Log.i("route", "route " + route);
                    if(route.contains("?")) {
                        if(route.split("\\?").length > 1)
                            params = route.split("\\?")[1];
                    }
                    Log.i("route", "params " + params);
                    break;
                }
                if(line.startsWith("POST /")) {
                    int start = line.indexOf('/') + 1;
                    int end = line.indexOf(' ', start);
                    route = line.substring(start, end);
                    if(route.contains("?")) {
                        if(route.split("\\?").length > 1)
                            params = route.split("\\?")[1];
                    }
                    Log.i("route", "post route " + route);
                }
                if(line.contains("Content-Type:")) {
                    boundary = line.split("=")[1];
                }


                Log.i("boundary", "bound " + boundary);
                if(line.equals("Content-Disposition: form-data; name=\"content\"")) {
                    Log.i("start", "start reading file");
                    boundary = "--" + boundary + "--";
                    while(!fileContent.contains(boundary)) {
                        int n = reader.read(buf);
                        fileContent += new String(buf).trim();
                        Log.i("filecontent", fileContent + " bufffer");
                    }
                }
                Log.i("request", "request "+ line);
                Log.i("filecontent", fileContent);
                if(!fileContent.isEmpty()) {
                    if(fileContent.contains(boundary)) {
                        fileContent = fileContent.replace(boundary, "");
                        break;
                    }

                }

            }

            if(params != null) {
                if(params.split("=")[0].equals("filename")) {
                    if(params.split("=").length >1 ) {
                        Filename = params.split("=")[1];
                    }
                } else if(params.split("=")[0].equals("folder")){
                    if(params.split("=").length >1 ) {
                       currentDir = params.split("=")[1];
                    }
                } else if(params.split("=")[0].equals("save")) {
                    Log.i("save", params.split("=")[1]);
                        saveCurrentFile(fileContent.trim());
                } else if(params.split("=")[0].equals("newfolder")) {
                    if(!params.split("=")[1].equals("null") && params.split("=").length >1 ) {
                        File directory = new File(currentDir + File.separator + params.split("=")[1]);
                        directory.mkdirs();
                    }
                }else if(params.split("=")[0].equals("newfile") && params.split("=").length >1) {
                    if(!params.split("=")[1].equals("null")) {
                        File file = new File(currentDir + File.separator + params.split("=")[1]);
                        file.createNewFile();
                    }
                }
            }
            Log.i("route", "params " + Filename);


            // Output stream that we send the response to
            output = new PrintStream(socket.getOutputStream());

            // Prepare the content to send.
            if (null == route) {
                writeServerError(output);
                return;
            }
            if (route.contains("?")) {
                route = route.split("\\?")[0];
            }
            byte[] bytes = loadContent(route);
            if (null == bytes) {
                writeServerError(output);
                return;
            }

            // Send out the content.
            output.println("HTTP/1.0 200 OK");
            output.println("Content-Type: " + detectMimeType(route));
            output.println("Content-Length: " + bytes.length);
            output.println();
            output.write(bytes);
            output.flush();
        } finally {
            if (null != output) {
                output.close();
            }
            if (null != reader) {
                reader.close();
            }
        }
    }

    /**
     * Writes a server error response (HTTP/1.0 500) to the given output stream.
     *
     * @param output The output stream.
     */
    private void writeServerError(PrintStream output) {
        output.println("HTTP/1.0 500 Internal Server Error");
        output.flush();
    }

    /**
     * Loads all the content of {@code fileName}.
     *
     * @param fileName The name of the file.
     * @return The content of the file.
     * @throws IOException
     */

    public void saveCurrentFile(String body) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(Filename));
            fos.write(body.getBytes());
            fos.flush();
            fos.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    String endOfAceEditor =
            "           \n</div><textarea id=\"editorContent\" style=\"display:none;\" name=\"content\"></textarea></form></div>"+
            "<script src=\"ace.js\" type=\"text/javascript\" charset=\"utf-8\"></script>\n" +
            "<script>\n" +
            "    var editor = ace.edit(\"editor\");\n" +

            "    editor.setOption(\"showInvisibles\", true);"+
            "    editor.setTheme(\"ace/theme/merbivore\");\n" +
            "    editor.getSession().setMode(\"ace/mode/java\");\n" +
            "    editor.setOptions({\n" +
            "    autoScrollEditorIntoView: true,\n" +
            "    showPrintMargin: false,\n" +
            "    enableBasicAutocompletion: true"+
            "    });"+
            "editor.getSession().on(\"change\", function () {\n" +
                    "    document.getElementById(\"editorContent\").value = editor.getValue();\n" +
                    "});"+
            "      function onClickSave() {"+
            "           document.getElementById(\"editorContent\").value = editor.getValue();" +
                    "}"+
             "      function undoFile() {" +
                    "   editor.getSession().getUndoManager().undo(false);" +
                    "};"+
                    "function redoFile() {" +
                    "   editor.getSession().getUndoManager().redo(false);" +
                    "};"+
            "    \n" +
            "</script>\n";





    private byte[] loadContent(String fileName) throws IOException {
        InputStream input = null;
        String content = "";
        byte buf[] = new byte[2048*10];

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if(fileName.equals("")) {
                input = new FileInputStream(new File(context.getExternalFilesDir(null)+ "/index.html"));
            } else {
                input = new FileInputStream(new File(context.getExternalFilesDir(null) + "/" + fileName));
            }



            byte[] buffer = new byte[1024];
            int size;
            while (-1 != (size = input.read(buffer))) {
                output.write(buffer, 0, size);
            }
            if(fileName.equals("ace.html")) {
                InputStream editFile = null;
                if(Filename != null) {
                    if (currentDir != "") {
                        editFile = new FileInputStream(new File(Filename));
                    } else {
                        editFile = new FileInputStream(new File(Filename));
                    }
                }
                else {
                    editFile = new FileInputStream(new File(context.getExternalFilesDir(null) + "/welcome.txt"));
                    Filename = context.getExternalFilesDir(null) + "/welcome.txt";
                }
                while(editFile.read(buf) != -1) {
                   output.write(buf);
                   output.flush();
                }

                output.write(endOfAceEditor.getBytes());
            }
            if(fileName.equals("filelist.html")) {
                output.write(loadSideBar(currentDir));
            }
            output.flush();
            return output.toByteArray();
        } catch (FileNotFoundException e) {
            return null;
        } finally {
            if (null != input) {
                input.close();
            }
        }
    }


    public byte[] loadSideBar(String dir) {
        File[] fileList = null;
        String content = "";

        fileList = new File(dir).listFiles();


        content = "<html>";
        content += "<head>\n" +
                "<link rel=\"stylesheet\" type=\"text/css\" href=\"bootstrap.min.css\">"+
                "</head>\n";
        content += "<table>\n" +
                "<tbody>\n" +
                "<tr>\n" +
                "<td><img src=\"icon.png\" width=\"128\" height=\"128\"></td>\n" +
                "<td><b class=\"mycss\">Wifi Code Editor</td>\n" +
                "</tr>\n" +
                "</tbody>\n" +
                "</table>";
        content += "<br><br>";
        content += "<b>&nbsp;&nbsp;<a class=\"folder\" href=#>New Folder</a><br>&nbsp;&nbsp;<a class=\"file\" href=#>New File</a></b><br><br>";
        content += "<body bgcolor=\"#2F4F4F\">";
        content += "<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js\"></script>\n" +
                "    <script src=\"bootstrap.min.js\"></script>";
        content += "   <!-- bootbox code -->\n" +
                "    <script src=\"bootbox.min.js\"></script>\n" +
                "    <script>\n" +
                "  $(document).on(\"click\", \".folder\", function(e) {\n" +
                "       bootbox.prompt(\"New Folder Name\", function(result){  window.top.location.href='http://"+ip+":"+port+"/index.html?newfolder='+result;});\n" +
                "        });"+
                "  $(document).on(\"click\", \".file\", function(e) {\n" +
                "       bootbox.prompt(\"New File Name\", function(result){  window.top.location.href='http://"+ip+":"+port+"/index.html?newfile='+result;});\n" +
                "        });"+
                "   function clicked() {\n" +
                "            window.frames[\"ace\"].document.forms[\"submit\"].submit();\n" +
                "    }"+
                "    </script>";
        content += "<table>\n" +
                "\t<tbody>\n";
       // content += "<b class=\"mycss\">";

        String[] dirs = dir.split(Config.getCurrent().workspacename);
        String preBuild  = currentDir.split(Config.getCurrent().workspacename)[0];
        String urlShown = "";
        String urlPath = "";



        if(dirs.length > 1) {

            dirs = dirs[1].split("/");
            String tmpDir = currentDir + "/";

            for (int i = 0; i < dirs.length; i++) {
                if(i == 1) {
                    urlShown = "";
                }
                urlShown = urlShown + dirs[i] + "/";
                urlPath = preBuild + Config.getCurrent().workspacename + "/"+ urlShown;

                content += "&nbsp;&nbsp;<a class=\"mycss\" href=\"#\" onClick=\"javascript:window.parent.location.href='http://" + ip + ":" + port + "/index.html?folder=" + urlPath + "'\";>" + urlShown + "</a><br>";
            }
        }

        content +=" </b><br><br>";

        if(fileList != null) {
            for (File file : fileList) {
                content += "<tr><td>";

                //    if (currentDir == "") {
                if (file.isDirectory()) {
                   content += "&nbsp;&nbsp;<img src=\"folder.png\"><a class=\"mycss\" href=\"#\" onClick=\"javascript:window.parent.location='http://" + ip + ":" + port + "/index.html?folder=" + file.getAbsolutePath() + "';\">" + file.getName() + "</a>";
                } else {
                    if(Config.getCurrent().openInNewTab) {
                        content += "&nbsp;&nbsp;<img src=\"file.png\"><a class=\"mycss\" href=\"#\" onClick=\"javascript:window.open('http://" + ip + ":" + port + "/index.html?filename=" + file.getAbsolutePath() + "');\">" + file.getName() + "</a>";
                    }else {
                        content += "&nbsp;&nbsp;<img src=\"file.png\"><a class=\"mycss\" href=\"#\" onClick=\"javascript:window.parent.location='http://" + ip + ":" + port + "/index.html?filename=" + file.getAbsolutePath() + "';\">" + file.getName() + "</a>";
                    }
                }
        /*    } else {
                if(new File(context.getExternalFilesDir(null)+"/"+rootDir+"/"+currentDir+"/"+file).isDirectory()) {
                    content += "<img src=\"folder.png\"><a class=\"mycss\" href=\"#\" onClick=\"window.open('http://"+ip+":"+port+"/index.html?folder=" + rootDir + "/" + currentDir+"/"+file+"');\">"+file+"</a>";
                } else {
                    content += "<img src=\"file.png\"><a class=\"mycss\" href=\"#\" onClick=\"window.open('http://"+ip+":"+port+"index.html?filename=" + rootDir + "/" + currentDir+"/"+file+"');\">" + file + "</a>";
                }
            }*/
            }
        }
        content += "</tbody>\n" +
                "</table></body></html>";
        return content.getBytes();
    }


    /**
     * Detects the MIME type from the {@code fileName}.
     *
     * @param fileName The name of the file.
     * @return A MIME type.
     */
    private String detectMimeType(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return null;
        } else if (fileName.endsWith(".html")) {
            return "text/html";
        } else if (fileName.endsWith(".js")) {
            return "application/javascript";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        } else {
            return "application/octet-stream";
        }
    }
}