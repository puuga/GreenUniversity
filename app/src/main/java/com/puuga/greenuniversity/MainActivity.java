package com.puuga.greenuniversity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.fabric.sdk.android.Fabric;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageRGBFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;

public class MainActivity extends AppCompatActivity implements GPUImageView.OnPictureSavedListener {

    public static GoogleAnalytics analytics;
    public static Tracker tracker;

    FloatingActionButton fabBtn;
    Toolbar toolbar;
    //    private ImageView imageView;
    private GPUImageView mGPUImageView;
    CoordinatorLayout rootLayout;

    String mCurrentPhotoPath;
    File photoFile;

    GPUImageFilter filter;
    Uri savedImageUri;

    static final int REQUEST_TAKE_PHOTO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        initGoogleAnalytics();
        initToolbar();
        initInstances();
    }

    private void initGoogleAnalytics() {
        analytics = GoogleAnalytics.getInstance(this);
        analytics.setLocalDispatchPeriod(1800);

        tracker = analytics.newTracker("UA-40963799-6");
        tracker.enableExceptionReporting(true);
        tracker.enableAutoActivityTracking(true);

        tracker.setScreenName("main screen");
    }

    private void initInstances() {
        rootLayout = (CoordinatorLayout) findViewById(R.id.rootLayout);

        fabBtn = (FloatingActionButton) findViewById(R.id.fabBtn);
        fabBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                        ex.printStackTrace();
                    }
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile));
                        startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                    }
                }

            }
        });

//        imageView = (ImageView)findViewById(R.id.imageView);
//        mGPUImageView.setGLSurfaceView((GLSurfaceView) findViewById(R.id.gpu_image));
        mGPUImageView = (GPUImageView) findViewById(R.id.gpu_image);
        filter = new GPUImageRGBFilter(0.3f, 1.0f, 0.3f);
    }

    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
//        File root = new File(Environment
//                .getExternalStorageDirectory()
//                + File.separator + "GreenUniversity" + File.separator);

//        boolean success = true;
//        if (!root.exists()) {
//            success = root.mkdir();
//        }
//        if (success) {
//            Log.d("app","root ok");
//        } else {
//            Log.d("app","root not ok");
//        }
//        File storageDir = Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_PICTURES);
//        File image = File.createTempFile(
//                imageFileName,  /* prefix */
//                ".jpg",         /* suffix */
//                root      /* directory */
//
//        );

        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
//        File image = new File(root, imageFileName+".jpg");

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        Log.d("app", "image uri: " + mCurrentPhotoPath);
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(photoFile);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
//        imageView.setImageURI(Uri.fromFile(photoFile));
        mGPUImageView.setImage(Uri.fromFile(photoFile));
        mGPUImageView.setFilter(filter);

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_green.jpg";
        mGPUImageView.saveToPictures("GreenUniversity", imageFileName, this);
    }

    @Override
    public void onPictureSaved(final Uri uri) {
        Log.d("app", "Saved: " + uri.toString());
        savedImageUri = uri;

        Snackbar.make(rootLayout, "Ready to SHARE!", Snackbar.LENGTH_SHORT)
                .setAction("Share", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sharePicture("GreenUniversity", "#GreenUniversity", savedImageUri);
                    }
                })
                .show();
    }

    private void sharePicture(String title, String text, Uri uri) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        shareIntent.putExtra(Intent.EXTRA_TITLE, title);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType("image/jpeg");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "send"));

        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("activity")
                .setAction("share")
                .setLabel("share")
                .build());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("app", "requestCode: " + requestCode);
        Log.d("app", "resultCode: " + resultCode);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            Log.d("app", "result: image uri: " + String.valueOf(Uri.fromFile(photoFile)));

            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory("activity")
                    .setAction("capture")
                    .setLabel("capture")
                    .build());

            galleryAddPic();
            setPic();
        }
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
            if ( savedImageUri!=null ) {
                sharePicture("GreenUniversity", "#GreenUniversity", savedImageUri);
            } else {
                Snackbar.make(rootLayout, "Must Capture a photo first!", Snackbar.LENGTH_SHORT)
                        .show();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
