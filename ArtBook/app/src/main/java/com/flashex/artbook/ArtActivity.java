package com.flashex.artbook;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.flashex.artbook.databinding.ActivityArtBinding;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;

public class ArtActivity extends AppCompatActivity {
    private ActivityArtBinding binding;
    ActivityResultLauncher<Intent> activityResultLauncher;
    ActivityResultLauncher<String> permissionsLauncher;
    Bitmap selectedImage;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //binding is jatPack. findBy id
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        //image save
        registerLauncher();
        database=this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);

        Intent intent=getIntent();
        String info=intent.getStringExtra("info");
        if (info.equals("new")){
            //new art
            binding.editTextName.setText("");
            binding.editTextArtist.setText("");
            binding.editTextYear.setText("");
            binding.button.setVisibility(View.VISIBLE);
            binding.imageView.setImageResource(R.drawable.select_image);
        }else{
            //id
            int artId=intent.getIntExtra("artId",1);
            binding.button.setVisibility(View.INVISIBLE);

            try {
                Cursor cursor=database.rawQuery("SELECT * FROM arts WHERE id = ?",new String[]{String.valueOf(artId)});
                int artNameIndex=cursor.getColumnIndex("artname");
                int painterNameIndex=cursor.getColumnIndex("paintername");
                int yearIndex=cursor.getColumnIndex("year");
                int imageIndex=cursor.getColumnIndex("image");

                while (cursor.moveToNext()){
                    binding.editTextName.setText(cursor.getString(artNameIndex));
                    binding.editTextArtist.setText(cursor.getString(painterNameIndex));
                    binding.editTextYear.setText(cursor.getString(yearIndex));

                    byte[] bytes=cursor.getBlob(imageIndex);
                    Bitmap bitmap= BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    binding.imageView.setImageBitmap(bitmap);
                }

                cursor.close();

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void save(View view) {
        //xml editText get value
        String name = binding.editTextName.getText().toString();
        String artistName = String.valueOf(binding.editTextArtist.getText());
        String year = binding.editTextYear.getText().toString();

        //image smaller. the code is save in SQlLite image
        Bitmap smallImage=makeSmallerImage(selectedImage,300);
        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray= outputStream.toByteArray();

        try {

            database.execSQL("CREATE TABLE IF NOT EXISTS arts(id INTEGER PRIMARY KEY,artname VARCHAR,paintername VARCHAR,year VARCHAR,image BLOB)");

            String sqlInsert="INSERT INTO arts(artname,paintername,year,image) VALUES(?,?,?,?)";
            SQLiteStatement sqLiteStatement=database.compileStatement(sqlInsert);
            sqLiteStatement.bindString(1,name);
            sqLiteStatement.bindString(2,artistName);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4,byteArray);
            sqLiteStatement.execute();

        }catch (Exception e){
            e.printStackTrace();
        }
        Intent intent=new Intent(ArtActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public Bitmap makeSmallerImage(@NonNull Bitmap bitmap, int maxSize) {
        //image size
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();

        //iamge aspect ratio
        float bitmapRatio = (float) width / (float) height;

        //image smaller process
        if (bitmapRatio>1){
            //landscape image (horizontal)
            width=maxSize;
            height=(int)(width/bitmapRatio);
        }else{
            //portrait image (vertical)
            height=maxSize;
            width=(int) (height*bitmapRatio);
        }

        return bitmap.createScaledBitmap(bitmap,width, height, true);
    }

    public void selectImage(View view) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            //Android 33++ -> READ_MEDIA_IMAGES. Manifest.permission.READ_MEDIA_IMAGES

            //checked self permissions
            permissionFunc(Manifest.permission.READ_MEDIA_IMAGES,view);
        } else {
            //Android 32-- -> READ_EXTERNAL_STORAGE. Manifest.permission.READ_EXTERNAL_STORAGE

            //checked self permissions
            permissionFunc(Manifest.permission.READ_EXTERNAL_STORAGE,view);
        }


    }

    private void registerLauncher() {

        //user go to gallery
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == RESULT_OK) {
                    //resim secimi yaptı mı user pick the images
                    Intent intentsFromResult = result.getData();
                    if (intentsFromResult != null) {
                        //secim yaptıysa resmin urisi nedir nerede kayıtlı. where is it save
                        Uri uri = intentsFromResult.getData();
                        //binding.imageView.setImageURI(uri);

                        try {
                            if (Build.VERSION.SDK_INT >= 28) {
                                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                                selectedImage = ImageDecoder.decodeBitmap(source);
                                binding.imageView.setImageBitmap(selectedImage);
                            } else {
                                selectedImage = MediaStore.Images.Media.getBitmap(ArtActivity.this.getContentResolver(), uri);
                                binding.imageView.setImageBitmap(selectedImage);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        });

        permissionsLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if (result) {
                    //permission granted
                    Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intentToGallery);

                } else {
                    //permission denied izin yok
                    Toast.makeText(ArtActivity.this, "Permission needed!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void permissionFunc(String permission,View view){
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                Snackbar.make(view, "Permissions needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permissions", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        permissionsLauncher.launch(permission);
                    }
                }).show();
            } else {
                //request permissions
                permissionsLauncher.launch(permission);
            }
        } else {
            //gallery
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intentToGallery);
        }

    }
}

