package matunagachihiro.spiritaway.com.example.imagetransparentapplication;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Size;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    ImageView imageView ;
    ImageView imageView1;
    BitmapIO bitIO = new BitmapIO();
    int isImported = 0; //importButtonのフラグ
    Bitmap mutableBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_synthetic);
        imageView = findViewById(R.id.imageView3);
        imageView1 = findViewById(R.id.image1);

        bitIO.setBitmap(((BitmapDrawable)imageView.getDrawable()).getBitmap());

    }

    public void BackButton(View v){
        finish();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void saveButton(View v){
        final String[] items = {"JPEG", "PNG"};
        new AlertDialog.Builder(this)
                .setTitle("保存する画像のタイプを選択してください")
                .setItems(items, (dialog, which) -> {
                    // item_which pressed
                    if(which == 0){
                        bitIO.setType(true);
                        createFile();
                    }else{
                        bitIO.setType(false);
                        createFile();
                    }
                })
                .show();
    }

    public void createFile() {
        String fileName;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

        if(bitIO.isType()) {
            fileName = "pic.jpeg";
            intent.setType("image/jpeg");
        }else{
            fileName = "pic.png";
            intent.setType("image/png");
        }

        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        activityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if ( result.getResultCode() == Activity.RESULT_OK) {
                    if (result.getData() != null) {
                        //結果を受け取った後の処理
                        Intent resultData = result.getData();
                        Uri uri = resultData.getData();
                        try(OutputStream outputStream =
                                    getContentResolver().openOutputStream(uri)) {
                            if(outputStream != null){
                                if(bitIO.isType()){
                                    bitIO.convertJPEG(mutableBitmap, outputStream);
                                }else{
                                    bitIO.convertPNG(mutableBitmap, outputStream);
                                }
                            }

                        } catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            });

    ActivityResultLauncher<Intent> _launcherSelectSingleImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent resultData = result.getData();
                    if (resultData != null) {
                        Uri uri = resultData.getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            bitIO.setBitmap(bitmap);
                            //ここにデータが保存される

                            if(isImported == 0){
                                imageView.setImageBitmap(bitIO.getBitmap());
                                isImported = 1;
                            }else if(isImported == 1){
                                imageView1.setImageBitmap(bitIO.getBitmap());
                                imageView1.setVisibility(View.VISIBLE);
                                isImported = 2;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

    public void getImage() {  //画像をアルバムから取得するメソッド
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        Intent chooserIntent = Intent.createChooser(intent, "画像を選択してください");

        _launcherSelectSingleImage.launch(chooserIntent);
    }

    public void importButton(View v){
        getImage();
    }

    public void syntheticImages(){ //画像を合成するメソッド
        if(isImported == 2) {
            mutableBitmap = bitIO.getBitmap().copy(Bitmap.Config.ARGB_8888, true);
            Canvas offScreen = new Canvas(mutableBitmap);
            Bitmap bitmap1 = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            Bitmap bitmap2 = ((BitmapDrawable) imageView1.getDrawable()).getBitmap();
            offScreen.drawBitmap(bitmap1, imageView.getImageMatrix(), null);
            offScreen.drawBitmap(bitmap2, imageView1.getImageMatrix(), null);
            bitIO.setBitmap(mutableBitmap);
        }else{
            Toast toast = Toast.makeText(this, "2枚の画像をインポートしてください", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public void SynthButton(View v){
        syntheticImages();
    }

}