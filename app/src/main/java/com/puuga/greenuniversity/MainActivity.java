package com.puuga.greenuniversity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
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
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.fabric.sdk.android.Fabric;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageRGBFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;

public class MainActivity extends AppCompatActivity implements
        GPUImageView.OnPictureSavedListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    static final int REQUEST_TAKE_PHOTO = 1;
    public static GoogleAnalytics analytics;
    public static Tracker tracker;
    GoogleApiClient mGoogleApiClient;
    AdRequest adRequest;
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

        buildGoogleApiClient();

        initGoogleAnalytics();
        initToolbar();
        initInstances();

        logDevice();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    private void logDevice() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        Log.d("app","manufacturer: "+manufacturer);
        Log.d("app","model: "+model);
    }

    private void initAdMob() {
        AdView mAdView = (AdView) findViewById(R.id.adView);

        if (adRequest != null) {
            return;
        }
        Location lastLocation = getLastKnownLocation();
        if (lastLocation == null) {
            adRequest = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .addTestDevice("3EC1EF88FD766483AA48DEDC3AAC8A18")
                    .build();
        } else {
            adRequest = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .addTestDevice("3EC1EF88FD766483AA48DEDC3AAC8A18")
                    .setLocation(lastLocation)
                    .build();
        }
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
                Intent takePictureIntent = new Intent("android.media.action.IMAGE_CAPTURE");
//                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
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

        filter = new GPUImageRGBFilter(0.2f, 0.9f, 0.2f);

//        GPUImageTwoInputFilter filter2 = new GPUImageAddBlendFilter();
//        filter2.setBitmap(BitmapFactory.decodeResource(mGPUImageView.getResources(), R.drawable.filter));
//        filter = filter2;
//        mGPUImageView.setFilter(filter);
    }

    private Location getLastKnownLocation() {
        Location mLastLocation = null;

        try {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if ( mLastLocation == null ) {
            LocationManager locationManager = (LocationManager)
                    this.getSystemService(Context.LOCATION_SERVICE);
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if ( locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ) {
                mLastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            } else if ( locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ) {
                mLastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
        }
        if (mLastLocation != null) {
            Log.d("location", mLastLocation.toString());
            Log.d("location provider", mLastLocation.getProvider());
        }

        return mLastLocation;
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

    private void galleryAddPic(Uri contentUri) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        // check photo rotation *problem from camera app
        try {
            Bitmap mBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), savedOriginalImageUri);
            if (mBitmap.getWidth() > mBitmap.getHeight()) {
                // make new photo if wrong rotation
                Log.d("app","wrong rotation");
                File rotatedPhotoFile = rotatePhoto(mBitmap);
                galleryAddPic(Uri.fromFile(rotatedPhotoFile));
                mGPUImageView.setImage(Uri.fromFile(rotatedPhotoFile));

                tracker.send(new HitBuilders.EventBuilder()
                        .setCategory("logic")
                        .setAction("wrong rotation")
                        .setLabel("MANUFACTURER:"+Build.MANUFACTURER+", MODEL:"+Build.MODEL)
                        .build());
            } else {
                mGPUImageView.setImage(savedOriginalImageUri);
            }

            mGPUImageView.setFilter(filter);

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_green.jpg";
            mGPUImageView.saveToPictures("GreenUniversity", imageFileName, this);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private File rotatePhoto(Bitmap mBitmap) throws IOException {
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap mBitmapNew = Bitmap.createBitmap(mBitmap , 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
        File file = createImageFile();
        OutputStream fOut = new FileOutputStream(file);
        mBitmapNew.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
        fOut.flush();
        fOut.close();

        return file;
    }

    private void setShareActionProvider(Uri uri) {
        mShareActionProvider.setShareIntent(
                getShareIntent("GreenUniversity", "#GreenUniversity", uri));
    }

    @Override
    public void onPictureSaved(final Uri uri) {
        Log.d("app", "Saved: " + uri.toString());
        savedGreenImageUri = uri;

        setShareActionProvider(uri);

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

            galleryAddPic(savedOriginalImageUri);
            setPic();
        }

        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() &&
                        !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            }
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

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {  // more about this later
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        initAdMob();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        initAdMob();
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (connectionResult.hasResolution()) {
            try {
                mResolvingError = true;
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
//            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
    }
}
