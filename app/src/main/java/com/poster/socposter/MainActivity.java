package com.poster.socposter;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nileshp.multiphotopicker.photopicker.activity.PickImageActivity;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ru.ok.android.sdk.OkListener;
import ru.ok.android.sdk.Shared;

public class MainActivity extends AppCompatActivity {

    private String TAG = "SocPoster_MainActivity_";

    private final int PERMISSION_REQUEST_CODE = 123;
    private int WHAT_BUTTON_PRESSED = 0;

    private Context context;

    private CheckBox cbOK, cbInsta;

    private Button btPost;
    private Button btHashtag;

    private EditText etPostText;

    private List<String> pathList = new ArrayList<>();
    private List<ImageView> imageViewList = new ArrayList<>();
    private List<ImageView> imageViewBeforeList = new ArrayList<>();
    private List<String> pathToImagesAfterList;
    private List<String> pathToImagesBeforeList;

    private LinearLayout imageAfterLayout, imageBeforeLayout;

    private TextView tvAfter, tvBefore;

    private AlertDialog alertDialog;

    private VKInitialize vkActivity;
    private OKInitialize okInitialize;


    protected static String postText = "";

    public void requestMultiplePermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE
                },
                PERMISSION_REQUEST_CODE);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        Activity mainActivity = com.poster.socposter.MainActivity.this;

        vkActivity = new VKInitialize();
        okInitialize = new OKInitialize();

        getSupportActionBar().hide();

        requestMultiplePermissions();

        initUI();

        VKSdk.login(this, VKScope.PHOTOS, VKScope.WALL, VKScope.GROUPS, VKScope.OFFLINE);

        okInitialize.getAuth(context, mainActivity);
    }


    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {

        /*Нажата кнопка загрузки в основной альбом*/

        if (resultCode == -1 && requestCode == PickImageActivity.PICKER_REQUEST_CODE && WHAT_BUTTON_PRESSED == 1) {

            if (pathList != null)
                pathList.clear();
            tvAfter.setVisibility(View.GONE);
            imageAfterLayout.setVisibility(View.VISIBLE);

            this.pathList = data.getExtras().getStringArrayList(PickImageActivity.KEY_DATA_RESULT);
            Log.d(TAG + "pathList_size", String.valueOf(pathList.size()));
            if (this.pathList != null && !this.pathList.isEmpty()) {
                for (int i = 0; i < pathList.size(); i++) {
                    imageViewList.get(i).setImageURI(Uri.parse(pathList.get(i)));
                    pathToImagesAfterList.add(pathList.get(i));
                }
            }
        }

        /*Нажата кнопка загрузки в альбом "До"*/

        if (resultCode == -1 && requestCode == PickImageActivity.PICKER_REQUEST_CODE && WHAT_BUTTON_PRESSED == 2) {

            if (pathList != null)
                pathList.clear();
            tvBefore.setVisibility(View.GONE);
            imageBeforeLayout.setVisibility(View.VISIBLE);

            this.pathList = data.getExtras().getStringArrayList(PickImageActivity.KEY_DATA_RESULT);
            Log.d(TAG + "pathList_size", String.valueOf(pathList.size()));
            if (this.pathList != null && !this.pathList.isEmpty()) {
                for (int i = 0; i < pathList.size(); i++) {
                    imageViewBeforeList.get(i).setImageURI(Uri.parse(pathList.get(i)));
                    pathToImagesBeforeList.add(pathList.get(i));
                }
            }
        }

        /*Пришёл ответ от ВК*/

        if (!VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {

                Log.d(TAG + "VKToken", res.accessToken);
                // Пользователь успешно авторизовался
            }

            @Override
            public void onError(VKError error) {
                // Произошла ошибка авторизации (например, пользователь запретил авторизацию)
            }
        })) {
            super.onActivityResult(requestCode, resultCode, data);
        }

        /*Пришёл ответ от ОК*/


        if (requestCode == Shared.OK_AUTH_REQUEST_CODE) {

            Log.d(TAG + "OKSuccessLogin", "OK");
        }

        if (requestCode == Shared.OK_POSTING_REQUEST_CODE) {

            for (String key : data.getExtras().keySet()) {
                Log.d(TAG + "OKPostResultKey", key);
                Log.d(TAG + "OKPostResultValue", data.getExtras().get(key).toString());
            }

            if (!data.getExtras().containsKey("error")) {

                Toast.makeText(this, "Пост опубликован в ОК", Toast.LENGTH_LONG).show();
                okInitialize.getUploadUrl(pathToImagesAfterList);

                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("postText", etPostText.getText().toString());
                clipboard.setPrimaryClip(clip);

                if (!cbInsta.isChecked() & pathToImagesAfterList.size() == 1)
                    createInstagramIntent("image/*", pathToImagesAfterList.get(0));
                else
                    openInstagramIntent();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length == 2) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //showExtDirFilesCount();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    public void onBtChooseClick(View view) {

        switch (view.getId()) {
            case R.id.btChooseAfterImage:
                WHAT_BUTTON_PRESSED = 1;
                Intent AfterIntent = new Intent(getApplicationContext(), PickImageActivity.class);
                AfterIntent.putExtra(PickImageActivity.KEY_LIMIT_MAX_IMAGE, 5);
                AfterIntent.putExtra(PickImageActivity.KEY_LIMIT_MIN_IMAGE, 1);
                startActivityForResult(AfterIntent, PickImageActivity.PICKER_REQUEST_CODE);
                break;

            case R.id.btChooseBeforeImage:
                WHAT_BUTTON_PRESSED = 2;
                Intent BeforeIntent = new Intent(getApplicationContext(), PickImageActivity.class);
                BeforeIntent.putExtra(PickImageActivity.KEY_LIMIT_MAX_IMAGE, 5);
                BeforeIntent.putExtra(PickImageActivity.KEY_LIMIT_MIN_IMAGE, 1);
                startActivityForResult(BeforeIntent, PickImageActivity.PICKER_REQUEST_CODE);
                break;

            case R.id.btPost:
                btPost.setClickable(false);
                postText = etPostText.getText().toString().trim();

                if (etPostText.getText().toString().trim().equals("")) {
                    if (pathToImagesAfterList.isEmpty()) {
                        Toast.makeText(getApplicationContext(), "Публиковать нечего", Toast.LENGTH_LONG).show();
                    } else {
                        makeDialog();
                        alertDialog.show();
                    }
                } else {
                    if (pathToImagesAfterList.isEmpty()) {
                        Toast.makeText(getApplicationContext(), "Ты не выбрала фотографии", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Пробуем опубликовать", Toast.LENGTH_LONG).show();
                        vkActivity.getVKUploadServer(context, true, etPostText.getText().toString(),
                                pathToImagesAfterList, pathToImagesBeforeList);
                        if (cbOK.isChecked()) {
                            okInitialize.getUploadUrlWithPost(true, pathToImagesAfterList, pathToImagesBeforeList);

                        } else {
                            if (pathToImagesAfterList.size() == 1) {
                                okInitialize.getUploadUrl(pathToImagesAfterList);

                                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("postText", etPostText.getText().toString());
                                clipboard.setPrimaryClip(clip);

                                if (!cbInsta.isChecked())
                                    createInstagramIntent("image/*", pathToImagesAfterList.get(0));
                                else
                                    openInstagramIntent();
                            } else {
                                okInitialize.getUploadUrlWithPost(false, pathToImagesAfterList, pathToImagesBeforeList);
                            }
                        }
                    }
                }
                break;

            case R.id.btHashtag:
                btHashtag.setVisibility(View.GONE);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(etPostText.getText()).append("#маникюр #маникюрновосибирск #аппаратныйманикюр #гельлак #гельлакновосибирск #укреплениебазой #выравниваниеногтевойпластины");
                etPostText.setText(stringBuilder);
                break;
        }
    }


    private void makeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ты не ввела описание")
                .setMessage("Запостить без описания?")
                .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(getApplicationContext(), "Пробуем опубликовать", Toast.LENGTH_LONG).show();
                        vkActivity.getVKUploadServer(context, true, etPostText.getText().toString(), pathToImagesAfterList,
                                pathToImagesBeforeList);

                        if (cbOK.isChecked()) {
                            okInitialize.getUploadUrlWithPost(true, pathToImagesAfterList, pathToImagesBeforeList);

                        } else {
                            if (pathToImagesAfterList.size() == 1) {
                                okInitialize.getUploadUrl(pathToImagesAfterList);

                                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("postText", etPostText.getText().toString());
                                clipboard.setPrimaryClip(clip);

                                if (!cbInsta.isChecked())
                                    createInstagramIntent("image/*", pathToImagesAfterList.get(0));
                                else
                                    openInstagramIntent();
                            } else {
                                okInitialize.getUploadUrlWithPost(false, pathToImagesAfterList, pathToImagesBeforeList);
                            }
                        }
                    }
                })
                .setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        btPost.setClickable(true);
                        dialogInterface.cancel();
                    }
                });
        alertDialog = builder.create();
    }

    private void initUI() {
        imageAfterLayout = findViewById(R.id.imageAfterLayout);
        imageAfterLayout.setVisibility(View.GONE);

        imageBeforeLayout = findViewById(R.id.imageBeforeLayout);
        imageBeforeLayout.setVisibility(View.GONE);

        ImageView photo1 = findViewById(R.id.photo1);
        ImageView photo2 = findViewById(R.id.photo2);
        ImageView photo3 = findViewById(R.id.photo3);
        ImageView photo4 = findViewById(R.id.photo4);
        ImageView photo5 = findViewById(R.id.photo5);

        ImageView photoBefore1 = findViewById(R.id.photoBefore1);
        ImageView photoBefore2 = findViewById(R.id.photoBefore2);

        imageViewList.add(photo1);
        imageViewList.add(photo2);
        imageViewList.add(photo3);
        imageViewList.add(photo4);
        imageViewList.add(photo5);

        imageViewBeforeList.add(photoBefore1);
        imageViewBeforeList.add(photoBefore2);

        tvAfter = findViewById(R.id.tvAfter);
        tvBefore = findViewById(R.id.tvBefore);

        Button btChooseAfterImage = findViewById(R.id.btChooseAfterImage);
        Button btChooseBeforeImage = findViewById(R.id.btChooseBeforeImage);
        btPost = findViewById(R.id.btPost);

        cbOK = findViewById(R.id.cbOK);
        cbInsta = findViewById(R.id.cbInsta);
        btHashtag = findViewById(R.id.btHashtag);

        pathToImagesAfterList = new ArrayList<>();
        pathToImagesBeforeList = new ArrayList<>();

        etPostText = findViewById(R.id.etPostText);
    }

    private void createInstagramIntent(String type, String mediaPath) {

        // Create the new Intent using the 'Send' action.
        Intent share = new Intent(Intent.ACTION_SEND);

        // Set the MIME type
        share.setType(type);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        share.setPackage("com.instagram.android");

        // Create the URI from the media
        File media = new File(mediaPath);
        Uri uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".Utils.GenericFileProvider", media);

        // Add the URI to the Intent.
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.putExtra(Intent.EXTRA_TEXT, "YOUR TEXT HERE");

        // Broadcast the Intent.
        startActivity(Intent.createChooser(share, "Share to"));
    }

    private void openInstagramIntent() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.instagram.android");
        if (launchIntent != null) {
            startActivity(launchIntent);//null pointer check in case package name was not found
        }
    }
}
