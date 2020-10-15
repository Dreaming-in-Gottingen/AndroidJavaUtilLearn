package com.example.gradle_learning;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "GradleLearning";

    private AlertDialog alertDialog1; //信息框
    private AlertDialog alertDialog2; //单选框
    private AlertDialog alertDialog3; //多选框

    private Button button1;
    private Button button2;
    private Button button3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate");
    }

    public void showList(View view) {
        final String[] items = {"列表1", "列表2", "列表3", "列表4"};
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("这是列表框");
        Log.d(TAG, "showList enter!");
        alertBuilder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d(TAG, "showList : " + i);
                Toast.makeText(MainActivity.this, items[i], Toast.LENGTH_SHORT).show();
                alertDialog1.dismiss();
            }
        });
        alertDialog1 = alertBuilder.create();
        alertDialog1.show();
    }

    public void showSingleAlertDialog(View view) {
        final String[] items = {"单选1", "单选2", "单选3", "单选4"};
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("这是单选框");
        Log.d(TAG, "showSingleAlertDialog enter!");
        alertBuilder.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d(TAG, "showSingleAlertDialog : " + i);
                Toast.makeText(MainActivity.this, items[i], Toast.LENGTH_SHORT).show();
            }
        });
        alertBuilder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d(TAG, "showSingleAlertDialog confirm!");
                alertDialog2.dismiss();
            }
        });
        alertBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d(TAG, "showSingleAlertDialog cancel!");
                alertDialog2.dismiss();
            }
        });
        alertDialog2 = alertBuilder.create();
        alertDialog2.show();
    }

    public void showMultiAlertDialog(View view) {
        final String[] items = {"多选1", "多选2", "多选3", "多选4"};
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("这是多选框");
        Log.d(TAG, "showMultiAlertDialog enter!");
        alertBuilder.setMultiChoiceItems(items, null, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i, boolean isChecked) {
                Log.d(TAG, "showMultiAlertDialog [" + i + "]: " + isChecked);
                if (isChecked) {
                    Toast.makeText(MainActivity.this, "选择->" + items[i], Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "取消选择->" + items[i], Toast.LENGTH_SHORT).show();
                }
            }
        });
        alertBuilder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d(TAG, "showMultiAlertDialog confirm!");
                alertDialog3.dismiss();
            }
        });

        alertBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d(TAG, "showMultiAlertDialog cancel!");
                alertDialog3.dismiss();
            }
        });
        alertDialog3 = alertBuilder.create();
        alertDialog3.show();
    }
}
