package com.example.diseasedetection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Bundle;
import com.example.diseasedetection.ml.ModelFinal;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {

    TextView result;
    ImageView imageView;
    Button picture, readMore;
    int imageSize = 224;

    private static final int REQUEST_PERMISSION = 101;
    private static final int REQUEST_GALLERY = 2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        result = findViewById(R.id.result);
        //confidence = findViewById(R.id.confidence);
        imageView = findViewById(R.id.imageView);
        picture = findViewById(R.id.button);
        readMore = findViewById(R.id.button2);

        readMore.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(!result.getText().toString().trim().isEmpty()){
                    readMore.setEnabled(true);
                    readMore.setTextColor(Color.WHITE);
                    readMore.setBackgroundColor(Color.rgb(76, 175, 80));
                }
            }
        });

        picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Launch camera if we have permission
                if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                    Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(galleryIntent, REQUEST_GALLERY);
                } else {
                    //Request camera permission if we don't have it.
                    requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES},REQUEST_PERMISSION);
                }
            }
        });

        readMore.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent webViewIntent = new Intent(MainActivity.this, WebViewActivity.class);
                startActivity(webViewIntent);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, try opening the gallery again
                picture.performClick();
            } else {
                // Permission denied, handle accordingly.
                // You may want to show a message to the user or take other actions.
            }
        }
    }

    public void classifyImage(Bitmap image){
        try {
            ModelFinal model = ModelFinal.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int [] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
            int pixel = 0;
            for(int i=0; i < imageSize; i++ ){
                for(int j=0;j<imageSize;j++){
                    int val = intValues[pixel++];
                    byteBuffer.putFloat(((val>>16) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat(((val>>8) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
                }
            }
            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            ModelFinal.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();
            int maxPos = 0;
            float maxConfidence = 0;
            for(int i = 0; i < confidences.length; i++){
                if(confidences[i] > maxConfidence){
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }

            String[] classes = {"Apple Black Rot",
                    "Apple Healthy",
                    "Apple Rust",
                    "Apple Scab",
                    "Banana Cordana",
                    "Banana Healthy",
                    "Banana Pestalotiopsis",
                    "Banana Sigatoka",
                    "Grape Black Rot",
                    "Grape Healthy",
                    "Grape Leaf Blight",
                    "Guava Healthy",
                    "Guava Red Rust",
                    "Mango Anthracnose",
                    "Mango Bacterial Canker",
                    "Mango Cutting Weevil",
                    "Mango Die Back",
                    "Mango Gall Midge",
                    "Mango Healthy",
                    "Mango Powdery Mildew",
                    "Mango Sooty Mould"};

            result.setText(classes[maxPos]);

            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 2 && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            try {
                Bitmap image = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                int dimension = Math.min(image.getWidth(), image.getHeight());
                image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
                imageView.setImageBitmap(image);
                imageView.setBackgroundColor(Color.BLACK);

                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                classifyImage(image);
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}