package com.example.childapp;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.childapp.model.AppHelper;
import com.example.childapp.model.VolleyMultipartRequest;
import com.example.childapp.model.VolleySingleton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity{
    public static final int CAM_REQUEST_CODE = 102;
    private static final int CAM_PERMISSION_CODE = 101;
    private static final int GALLERY_REQUEST_CODE = 103;
    private ImageView temp;
    private Uri image_uri;
    private final String TAG = "111111111111111";

    ImageView personImg;
    Button submit;
    ProgressDialog progressDialog;
    FloatingActionButton fab;
    BottomNavigationView bottomNavigationView;
    String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        prg = findViewById(R.id.progressbar);
        personImg = findViewById(R.id.personImg);
        submit = findViewById(R.id.submit_btn);
        submit.setOnClickListener(this::postData);

        bottomNavigationView = findViewById(R.id.bottomNavigation);
        fab = findViewById(R.id.fab_btn);

        if(savedInstanceState == null){
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, new MainFragment()).commit();
        }

        replaceFragment(new MainFragment());

        bottomNavigationView.setBackground(null);
        bottomNavigationView.setOnItemSelectedListener(item -> {

            switch (item.getItemId()) {
                case R.id.nav_main:
                    replaceFragment(new MainFragment());
                    break;
                case R.id.nav_results:

                    break;
            }

            return true;
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showBottomDialog();
            }
        });
    }

    private  void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }

    private void showBottomDialog() {

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottomsheetlayout);

        LinearLayout galleryLayout = dialog.findViewById(R.id.layoutGallery);
        LinearLayout cameraLayout = dialog.findViewById(R.id.layoutCamera);

        galleryLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Toast.makeText(MainActivity.this,"Upload a Image is clicked",Toast.LENGTH_SHORT).show();
                Intent gallery = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(gallery, GALLERY_REQUEST_CODE);

            }
        });

        cameraLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                askCameraPermission();
            }
        });

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }


    public void postData(View v){
        if (personImg.getDrawable() != null){
//            prg.setVisibility(View.VISIBLE);
            String url = "http://10.0.2.2:8000/api/submit";
            VolleyMultipartRequest multipartRequest= new VolleyMultipartRequest(Request.Method.POST, url,
                    new Response.Listener<NetworkResponse>() {
                        @Override
                        public void onResponse(NetworkResponse response) {
                            if (progressDialog != null) {
                                progressDialog.dismiss();
                            }
                            String urls = new String(response.data);
                            Log.d("@@@", urls);
                            Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                            intent.putExtra("urls", urls);
                            startActivity(intent);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d("@@@", new String(error.getMessage()));
                        }
                    }){
                @Override
                protected Map<String, String> getParams(){
                    Map<String,String> params = new HashMap<>();
                    params.put("tittle", "upload Image");
                    return params;
                }
                @Override
                protected Map<String, DataPart> getByteData() {
                    Map<String, DataPart> params = new HashMap<>();
                    params.put("image", new DataPart("personImg.jpg", AppHelper.getFileDataFromDrawable(getBaseContext(), personImg.getDrawable()), "image/jpeg"));
                    return params;
                }
            };
            multipartRequest.setRetryPolicy(new DefaultRetryPolicy(
                    15000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Processing");
            progressDialog.show();

            VolleySingleton.getInstance(getBaseContext()).addToRequestQueue(multipartRequest);
        }
    }


    public void askCameraPermission() {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.CAMERA}, CAM_PERMISSION_CODE);
        }else {
            dispatchTakePictureIntent();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAM_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAM_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                File f = new File(currentPhotoPath);
                personImg.setImageURI(Uri.fromFile(f));
                Log.d(TAG, "Absolute Url of Img is:" + Uri.fromFile(f));

                //Chụp xong tự lưu vô gallery
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(f);
                mediaScanIntent.setData(contentUri);
                this.sendBroadcast(mediaScanIntent);
            }
        }

        if(requestCode == GALLERY_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){
                Uri contentUri = data.getData();
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = "JPEG_" + timeStamp +"."+getFileExt(contentUri);
                Log.d(TAG, "onActivityResult: Gallery Image Uri:  " +  imageFileName);
                personImg.setImageURI(contentUri);

//                uploadImageToFirebase(imageFileName,contentUri);
            }
        }
    }

    //Dùng để lấy đuôi và show ra tên file
    private String getFileExt(Uri contentUri) {
        ContentResolver c = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(c.getType(contentUri));
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
//        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        //Mo camera
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Create the File where the photo should go/ Tao 1 img file va directory cho file do
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {

        }
        // Neu photoFile đã tạo thành công thì sẽ tạo photoURI
        // photoFile - là một file để chứa ảnh (1 căn nhà có tên để chứa ảnh) - và lúc này cũng chưa có địa chỉ lưu trữ
        // photoURI tức là một 'chuỗi định danh duy nhất' để xác định địa chỉ của bức ảnh - tức dòng 'Uri photoURI =...' để tạo địa chỉ lưu trữ của ảnh
        // photoURI sẽ được tạo là 1 địa chỉ URI và thông qua Authority của FileProvider để cho phép photoFile sở hữu địa chỉ URI này.
        // => Tức photoURI (địa chỉ) được cấp cho 1 căn nhà là photoFile (bức ảnh) mới được xây nên thông qa sự cho phép của FileProder (thiết bị Android)
        // Dòng takePictureIntent - là lúc này sẽ chụp 1 bức ảnh - tạo 1 đối tượng là bức ảnh (tạo ra 1 người dân).
        // Hàm MediaStore.EXTRA_OUTPUT sẽ chỉ định  vị trí lưu trữ của bức ảnh này, và photoUri chính là địa chỉ - tức là MediaStore sẽ cho bức ảnh (người dân) này vào căn nhà photoFile với địa chỉ là photoURI
        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this,
                    "com.example.android.fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI); // Uri (photoUri) lay duoc tu FileProvider sẽ được thêm vào extra, cho máy ảnh biết cái nơi ảnh chụp được lưu trữ, và lưu vào đó
            startActivityForResult(takePictureIntent, CAM_REQUEST_CODE);
        }

    }
}






//    private void openCamera() {
//        ContentValues values = new ContentValues();
//        values.put(MediaStore.Images.Media.TITLE,"new image");
//        values.put(MediaStore.Images.Media.DESCRIPTION,"From the camera");
//        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
//        Intent intent_cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        intent_cam.putExtra(MediaStore.EXTRA_OUTPUT,image_uri);
//        launchCameraAcForResult.launch(intent_cam);
//    }
//    ActivityResultLauncher<Intent> launchCameraAcForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),rs-> {
//        if (rs.getResultCode() == Activity.RESULT_OK){
//            Intent intent = rs.getData();
//            if (intent != null){
//                temp.setImageURI(image_uri);
//                temp.setClickable(false);
//            }
//        }
//    });

//    private void openCamera() {
//        ContentValues values = new ContentValues();
//        values.put(MediaStore.Images.Media.TITLE,"new image");
//        values.put(MediaStore.Images.Media.DESCRIPTION,"From the camera");
//        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
//        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        startActivityForResult(camera, CAM_REQUEST_CODE);
//    }