package com.example.FileIODemo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import android.view.View;
import android.widget.Button;

import android.os.EnvironmentEx;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class MainActivity extends Activity {
    private static final String TAG = "FileIODemo";
    private Button button0;
    private Button button1;

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(saveInstanceState);
        setContentView(R.layout.main_activity);

        // init button
        button0 = (Button) findViewById(R.id.button0);
        button0.setText("read/write test");
        button0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String state = EnvironmentEx.getExternalStoragePathState();
                boolean ext_mounted = Environment.MEDIA_MOUNTED.equals(state);
                Log.d(TAG, "state:" + state + ", ext_mounted:" + ext_mounted);
                String dir_path;
                if (ext_mounted == true) {
                    dir_path = EnvironmentEx.getExternalStoragePath().toString() + "/zjz";
                } else {
                    dir_path = EnvironmentEx.getInternalStoragePath().toString() + "/zjz";
                }
                Log.d(TAG, "dir_path:" + dir_path);

                File yuv_dir = new File(dir_path);
                if (yuv_dir.isDirectory()) {
                    File lists[] = yuv_dir.listFiles();
                    Log.d(TAG, "total file_cnt=" + lists.length + " in " + yuv_dir);
                    for (int i=0; i<lists.length; i++)
                        Log.d(TAG, "    [" + i + "]: " + lists[i].getName());

                    try {
                        String dst_file = dir_path + "/dst.yuv";
                        Log.d(TAG, "copy lists[0] to " + dst_file);
                        File f_out = new File(dst_file);
                        OutputStream out = new FileOutputStream(f_out);

                        String src_file = lists[0].getPath();
                        Log.d(TAG, "read from file=" + src_file);
                        File f_in = new File(src_file);
                        InputStream in = new FileInputStream(f_in);

                        byte[] ba = new byte[1024];
                        int len;
                        while ((len = in.read(ba))>0) {
                            Log.d(TAG, "read len=" + len);
                            out.write(ba, 0, len);
                        }

                        in.close();
                        out.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.d(TAG, "we create yuv_dir!");
                    yuv_dir.mkdirs();
                    //try {
                    //    yuv_dir.createNewFile();
                    //} catch (IOException e) {
                    //    e.printStackTrace();
                    //}
                }
            }
        });

        // quit button
        button1 = (Button) findViewById(R.id.button1);
        button1.setText("quit Activity");
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG, "quit Activity!");
                finish();
            }
        });
    }

    protected void onDestroy() {
        Log.w(TAG, "onDestroy!");
        super.onDestroy();
    }
}
