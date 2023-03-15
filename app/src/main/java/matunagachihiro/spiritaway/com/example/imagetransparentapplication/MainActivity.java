package matunagachihiro.spiritaway.com.example.imagetransparentapplication;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
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
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    ImageView imageView;
    SeekBar seekBar;
    BitmapIO bitIO = new BitmapIO();
    int imageWidth; //画像の幅
    int imageHeight; //画像の高さ
    int[] pixels;
    int tv = 80; //二値化の閾値
    InterstitialAd mInterstitialAd;
    AdRequest adRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        seekBar = findViewById(R.id.seekBar);
        imageView = findViewById(R.id.imageView2);

        bitIO.setBitmap(((BitmapDrawable)imageView.getDrawable()).getBitmap());

        // 初期値
        seekBar.setProgress(30);
        // 最大値
        seekBar.setMax(130);
        seekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    //ツマミがドラッグされると呼ばれる
                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar, int progress, boolean fromUser) {
                        tv = progress;
                    }

                    //ツマミがタッチされた時に呼ばれる
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    //ツマミがリリースされた時に呼ばれる
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                });

        //バナー広告表示
        MobileAds.initialize(this,
                initializationStatus -> {
                });

        //AdRequest
        AdView adView = findViewById(R.id.adView);
        adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    @Override
    protected void onResume() {
        super.onResume();


        InterstitialAd.load(this,
                "ca-app-pub-2742833893230662/3974111824",
                adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until an ad is loaded.
                        mInterstitialAd = interstitialAd;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        mInterstitialAd = null;
                        Toast.makeText(MainActivity.this,
                                "広告を読み込み中です。", Toast.LENGTH_LONG).show();
                    }
                });
    }

    public void saveButton(View v){
        bitIO.setType(false);
        bitIO.addAdCount();
        createFile();


        //インタースティシャル広告の表示
        if(bitIO.getAdcount()){
        showInterstitial();
        }
    }

    public void showInterstitial(){
        if (mInterstitialAd != null) {
            mInterstitialAd.show(MainActivity.this);
        } else {
            Toast.makeText(MainActivity.this,
                    "広告の読み込みに失敗しました。", Toast.LENGTH_LONG).show();
        }
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
                                    bitIO.convertJPEG(bitIO.getBitmap(), outputStream);
                                }else{
                                    bitIO.convertPNG(bitIO.getBitmap(), outputStream);
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
                            imageView.setImageBitmap(bitmap);
                            //ここにデータが保存される
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

    public void TPButton(View v){ //透過するボタン
        imageView.setImageBitmap(bitIO.getBitmap());
        imageWidth = bitIO.getBitmap().getWidth();
        imageHeight = bitIO.getBitmap().getHeight();
        pixels = new int[imageWidth * imageHeight];

        int backColor = bitIO.getBitmap().getPixel(0,0); //0,0の背景色
        int WR = (backColor >> 16) & 0xff;
        int WG = (backColor >> 8) & 0xff;
        int WB = backColor & 0xff;  //消えやすくなる色を決めるためのウェイト
        int tmp = Math.max(WB ,Math.max(WR ,WG));
        if(tmp == WR && tmp == WB && tmp == WG){
            WR = 1;
            WB = 1;
            WG = 1;
        }
        else if(tmp == WR){
            WR = 1;
            WB = 0;
            WG = 0;
        }else if(tmp == WB){
            WB = 1;
            WR = 0;
            WG = 0;
        }else{
            WG = 1;
            WR = 0;
            WB = 0;
        }

        Bitmap bitmap = Bitmap.createBitmap(imageWidth,imageHeight,Bitmap.Config.ARGB_8888 );
        bitIO.getBitmap().getPixels(pixels, 0, imageWidth, 0, 0, imageWidth, imageHeight);
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                int p = pixels[x + y * imageWidth];
                int R = (p >> 16) & 0xff;
                int G = (p >> 8) & 0xff;
                int B = p & 0xff;
                int newcolor = (R*WR+G*WG+B*WB)/3;
                if( tv < newcolor) {
                    pixels[x + y * imageWidth] = 0;
                }
            }
        }
        bitmap.eraseColor(Color.argb(0, 0, 0, 0));
        bitmap.setPixels(pixels, 0, imageWidth, 0, 0, imageWidth, imageHeight);
        bitIO.setBitmap(bitmap);
        imageView.setImageBitmap(bitmap);
    }

    public void goPrivacyActivity (View v){
        Intent intent = new Intent(this,PrivacyActivity.class);
        startActivity(intent);
    }
}