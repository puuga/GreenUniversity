package com.puuga.greenuniversity;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.fabric.sdk.android.Fabric;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageRGBFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;

public class MainActivity extends AppCompatActivity implements
        GPUImageView.OnPictureSavedListener {

    static final int REQUEST_TAKE_PHOTO = 1;
    public static GoogleAnalytics analytics;
    public static Tracker tracker;
    FloatingActionButton fabBtn;
    Toolbar toolbar;
    CoordinatorLayout rootLayout;

    File photoFile;

    GPUImageFilter filter;
    Uri savedGreenImageUri;
    Uri savedOriginalImageUri;
    private GPUImageView mGPUImageView;
    private ShareActionProvider mShareActionProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        initAdMob();
        initGoogleAnalytics();
        initToolbar();
        initInstances();
    }

    private void initAdMob() {
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .setLocation(getLastKnownLocation())
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("3EC1EF88FD766483AA48DEDC3AAC8A18")
                .build();
        mAdView.loadAd(adRequest);
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
                        tracker.send(new HitBuilders.EventBuilder()
                                .setCategory("logic")
                                .setAction("create file")
                                .setLabel("can not create file")
                                .build());
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

        mGPUImageView = (GPUImageView) findViewById(R.id.gpu_image);
        Uri path = Uri.parse(
                "android.resource://com.puuga.greenuniversity/" + R.drawable.placeholder);
        mGPUImageView.setImage(path);
        mGPUImageView.setScaleType(GPUImage.ScaleType.CENTER_INSIDE);

        filter = new GPUImageRGBFilter(0.2f, 1f, 0.2f);

//        GPUImageTwoInputFilter filter2 = new GPUImageAddBlendFilter();
//        filter2.setBitmap(BitmapFactory.decodeResource(mGPUImageView.getResources(), R.drawable.filter));
//        filter = filter2;
        mGPUImageView.setFilter(filter);
    }

    private Location getLastKnownLocation() {
        LocationManager locationManager = (LocationManager)
                this.getSystemService(Context.LOCATION_SERVICE);
        String locationProvider = LocationManager.NETWORK_PROVIDER;
        Location loc = locationManager.getLastKnownLocation(locationProvider);
        Log.d("location", loc.toString());
        return loc;
    }

    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);

        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(photoFile);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        mGPUImageView.setImage(Uri.fromFile(photoFile));
        mGPUImageView.setScaleType(GPUImage.ScaleType.CENTER_INSIDE);
        mGPUImageView.setFilter(filter);

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_green.jpg";
        mGPUImageView.saveToPictures("GreenUniversity", imageFileName, this);

        setShareActionProvider();
    }

    private void setShareActionProvider() {
        mShareActionProvider.setShareIntent(
                getShareIntent("GreenUniversity", "#GreenUniversity", savedGreenImageUri));
    }

    @Override
    public void onPictureSaved(final Uri uri) {
        Log.d("app", "Saved: " + uri.toString());
        savedGreenImageUri = uri;

        Snackbar.make(rootLayout, "Ready to SHARE!", Snackbar.LENGTH_LONG)
                .setAction("Share", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        shareGreenPhoto("GreenUniversity", "#GreenUniversity", savedGreenImageUri);
                    }
                })
                .show();
    }

    private void shareGreenPhoto(String title, String text, Uri uri) {
        startActivity(Intent.createChooser(getShareIntent(title, text, uri), "send"));

        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("activity")
                .setAction("share")
                .setLabel("share green")
                .build());
    }

    private void shareOriginalPhoto(String title, String text, Uri uri) {
        startActivity(Intent.createChooser(getShareIntent(title, text, uri), "send"));

        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("activity")
                .setAction("share")
                .setLabel("share original")
                .build());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("app", "requestCode: " + requestCode);
        Log.d("app", "resultCode: " + resultCode);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            Log.d("app", "result: image uri: " + String.valueOf(Uri.fromFile(photoFile)));
            savedOriginalImageUri = Uri.fromFile(photoFile);

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

        // Set up ShareActionProvider's default share intent
        MenuItem shareItem = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);


        return super.onCreateOptionsMenu(menu);
    }

    private Intent getShareIntent(String title, String text, Uri uri) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        shareIntent.putExtra(Intent.EXTRA_TITLE, title);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType("image/*");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return shareIntent;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if ( id == R.id.action_share ) {
//            if (savedGreenImageUri != null) {
//                sharePicture("GreenUniversity", "#GreenUniversity", savedGreenImageUri);
//            } else {
//                Snackbar.make(rootLayout, "Must Capture a photo first!", Snackbar.LENGTH_LONG)
//                        .show();
//            }
//            return true;
//        }
        if (id == R.id.action_share_original) {
            if (savedOriginalImageUri != null) {
                shareOriginalPhoto("GreenUniversity", "#GreenUniversity", savedOriginalImageUri);
            } else {
                Snackbar.make(rootLayout, "Must Capture a photo first!", Snackbar.LENGTH_LONG)
                        .show();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
