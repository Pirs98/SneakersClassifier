package com.example.sneakersclassifier;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import com.example.sneakersclassifier.ImageClassifier;

public class MainActivity extends AppCompatActivity {

    // requests codes to identify select, camera and permission requests
    private static final int SELECT_IMAGE_REQUEST_CODE = 10000;
    private static final int CAMERA_REQUEST_CODE = 10001;
    private static final int PERMISSION_REQUEST_CODE = 200;


    // define XML elements
    ImageView imageView;
    Button classify_button;
    Button take_a_picture;
    TextView results;
    TextView link_result;
    Uri imageuri;

    private Bitmap bitmap;
    private ImageClassifier imageClassifier;
    private boolean imageLoaded;
    private Map<String, Float> predictions;

    // link
    private String link_aj11 = "https://restocks.net/it/c/sneakers/air-jordan/Air-Jordan-11";
    private String link_blazer = "https://stockx.com/search?s=blazer%20mid";
    private String link_am1 = "https://restocks.net/it/c/sneakers/nike/air-max/air-max-1";
    private String link_yeezy_700 = "https://restocks.net/it/c/sneakers/adidas/yeezy/700/v1";
    private String link_yeezy_350 = "https://restocks.net/it/c/sneakers/adidas/yeezy/350/v2";
    private String link_aj4 = "https://restocks.net/it/c/sneakers/air-jordan/air-jordan-4";
    private String link_aj1 = "https://restocks.net/it/c/sneakers/air-jordan/air-jordan-1/high";
    private String link_yeezy_slide = "https://restocks.net/it/c/sneakers/adidas/yeezy/slide";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check if camera has permission
        if (! checkPermission()) {
            requestPermission();
        }

        imageLoaded = false;

        try {
            imageClassifier = new ImageClassifier(this);
        } catch (IOException e) {
            Log.e("Image Classifier Error", "ERROR: " + e);
        }

        initializeUIElements();
    }


    private void initializeUIElements() {

        imageView = (ImageView)findViewById(R.id.imageView);
        classify_button = (Button)findViewById(R.id.classify);
        take_a_picture = (Button)findViewById(R.id.take_a_picture);
        results = (TextView)findViewById(R.id.results);
        link_result = (TextView)findViewById(R.id.link_text);

        // select an image from gallery
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select image"),SELECT_IMAGE_REQUEST_CODE);
                imageLoaded = true;
            }
        });

        // take a picture from camera
        take_a_picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // camera permission already checked
                openCamera();
            }
        });

        classify_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(imageLoaded) {
                    showResult();
                } else {
                    Toast.makeText(getApplicationContext(), "Please select an image", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
    }

    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // permission is not granted
            return false;
        }
        return true;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            showMessageOKCancel("You need to allow access permissions",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermission();
                                            }
                                        }
                                    });
                        }
                    }
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK && data != null) {
            switch (requestCode) { // switch used for futureproof
                case SELECT_IMAGE_REQUEST_CODE:
                    imageuri = data.getData();
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageuri);
                        imageView.setImageBitmap(bitmap);
                        predictions = imageClassifier.recognizeImage(bitmap, 0);
                    } catch (IOException e) {
                        Log.e("Image Classifier", "ERROR: " + e);
                    }
                break;
                case CAMERA_REQUEST_CODE:
                    try {
                        Bitmap photo = (Bitmap) Objects.requireNonNull(Objects.requireNonNull(data).getExtras()).get("data");
                        imageView.setImageBitmap(photo);
                        predictions = imageClassifier.recognizeImage(photo, 0);
                    } catch (Exception e) {
                        Log.e("Image Classifier", "ERROR: " + e);
                    }
                break;
            }
        }
    }

    private void showResult() {
        String buy_link = "";
        float maxValueInMap = (Collections.max(predictions.values()));
        for ( String key : predictions.keySet() ) {
            if (predictions.get(key) == maxValueInMap) {
                float prob = predictions.get(key) * 100;
                String result_string = "The sneakers is a " + key + " with " + prob + "% probability";
                switch (key){
                    case "Air Jordan 11":
                        buy_link = link_aj11;
                    break;
                    case "Nike Blazer Mid":
                        buy_link = link_blazer;
                    break;
                    case "Nike Air Max 1":
                        buy_link = link_am1;
                    break;
                    case "Yeezy Boost 700":
                        buy_link = link_yeezy_700;
                    break;
                    case "Yeezy Boost 350 V2":
                        buy_link = link_yeezy_350;
                    break;
                    case "Air Jordan 4":
                        buy_link = link_aj4;
                    break;
                    case "Air Jordan 1 High":
                        buy_link = link_aj1;
                    break;
                    case "Yeezy Slide":
                        buy_link = link_yeezy_slide;
                    break;
                }
                results.setText(result_string);
                link_result.setText("Buy one at " + buy_link);
            }
        }

    }
}


