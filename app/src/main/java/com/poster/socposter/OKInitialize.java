package com.poster.socposter;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.poster.socposter.Utils.MultipartUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.ok.android.sdk.Odnoklassniki;
import ru.ok.android.sdk.OkListener;
import ru.ok.android.sdk.OkRequestMode;
import ru.ok.android.sdk.util.OkAuthType;
import ru.ok.android.sdk.util.OkScope;

import static com.vk.sdk.api.VKApiConst.REDIRECT_URI;

public class OKInitialize {

    private String TAG = "SocPoster_OKInitialize_";
    private String APP_ID = "127516032";
    private String APP_KEY = "CBAIGCANEBABABAB";
    private String charset = "UTF-8";


    private boolean isChecked = false;
    private static boolean inAlbum;


    private List<String> pathToImagesAfterList;
    private List<String> pathToImagesBeforeList;
    private List<String> allPath;
    private List<String> photo_ids_list = new ArrayList<>();
    private List<String> tokenList = new ArrayList<>();
    private List<String> assigned_photo_id_list = new ArrayList<>();

    private String AID_MAIN = "85098288923";
    private String AID_SECOND = "88007180110";

    private Odnoklassniki odnoklassniki;


    private Context context;
    private Activity activity;


    /*Логин Одноклассники*/
    protected void getAuth(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;

        Odnoklassniki.createInstance(context, APP_ID, APP_KEY);
        odnoklassniki = Odnoklassniki.getInstance();
        odnoklassniki.requestAuthorization(activity, REDIRECT_URI, OkAuthType.ANY, OkScope.VALUABLE_ACCESS, OkScope.LONG_ACCESS_TOKEN, OkScope.PHOTO_CONTENT, OkScope.GROUP_CONTENT);
    }

    /*Получаем альбомы пользователя*/
    protected void getOKAlbums() {
        try {
            String albums = odnoklassniki.request("photos.getAlbums", null, OkRequestMode.DEFAULT);
            Log.d(TAG + "albums", albums);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*Получаем url для загрузки фото и последующего создания поста*/
    protected void getUploadUrlWithPost(boolean isChecked, List<String> pathToImagesAfterList, final List<String> pathToImagesBeforeList) {

        inAlbum = false;
        this.isChecked = isChecked;
        this.pathToImagesAfterList = pathToImagesAfterList;
        this.pathToImagesBeforeList = pathToImagesBeforeList;
        allPath = new ArrayList<>(pathToImagesAfterList);

        if (isChecked)
            allPath.addAll(pathToImagesBeforeList);


        new Thread(new Runnable() {
            @Override
            public void run() {
                String getUploadUrl = null;
                String upload_url = null;
                Map<String, String> params = new HashMap<>();

                photo_ids_list.clear();


                params.put("aid", AID_SECOND);

                params.put("count", String.valueOf(allPath.size()));
                try {
                    getUploadUrl = odnoklassniki.request("photosV2.getUploadUrl", params, OkRequestMode.DEFAULT);
                    Log.d(TAG + "getUploadUrl_response", getUploadUrl);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG + "getUploadUrl_error", e.getMessage());
                }
                try {
                    JSONObject jsonResponse = new JSONObject(getUploadUrl);
                    JSONArray photo_ids = jsonResponse.getJSONArray("photo_ids");
                    upload_url = jsonResponse.getString("upload_url");
                    Log.d(TAG + "upload_url", upload_url);
                    for (int i = 0; i < photo_ids.length(); i++) {
                        photo_ids_list.add(String.valueOf(photo_ids.get(i)));
                        Log.d(TAG + "photo_id", photo_ids_list.get(i));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                sendMutlipart(upload_url);
            }
        }).start();


    }

    /*Получаем url для загрузки фото*/
    protected void getUploadUrl(List<String> pathToImagesAfterList) {

        inAlbum = true;
        this.pathToImagesAfterList = pathToImagesAfterList;

        allPath = new ArrayList<>(pathToImagesAfterList);
        String s = allPath.get(0);
        allPath = new ArrayList<>();
        allPath.add(s);
        for (String str : allPath)
            Log.d(TAG + "allPathToString", str);

        Log.d(TAG + "allPath_size", String.valueOf(allPath.size()));

        new Thread(new Runnable() {
            @Override
            public void run() {
                String getUploadUrl = null;
                String upload_url = null;
                Map<String, String> params = new HashMap<>();


                photo_ids_list = new ArrayList<>();

                Log.d(TAG + "photo_ids_size", String.valueOf(photo_ids_list.size()));

                for (String str : allPath)
                    Log.d(TAG + "allPathToString", str);

                params.put("aid", AID_MAIN);

                params.put("count", "1");
                try {
                    getUploadUrl = odnoklassniki.request("photosV2.getUploadUrl", params, OkRequestMode.DEFAULT);
                    Log.d(TAG + "getUploadUrl_response", getUploadUrl);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG + "getUploadUrl_error", e.getMessage());
                }
                try {
                    JSONObject jsonResponse = new JSONObject(getUploadUrl);
                    JSONArray photo_ids = jsonResponse.getJSONArray("photo_ids");
                    upload_url = jsonResponse.getString("upload_url");
                    Log.d(TAG + "upload_url", upload_url);
                    for (int i = 0; i < photo_ids.length(); i++) {
                        photo_ids_list.add(String.valueOf(photo_ids.get(i)));
                        Log.d(TAG + "photo_id", photo_ids_list.get(i));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                sendMutlipart(upload_url);
            }
        }).start();

    }

    /*Отправляем фото на сервер*/
    protected void sendMutlipart(String requestURL) {

        MultipartUtility multipart = null;
        JSONObject photos = null;

        List<String> files;

        files = allPath;

        try {
            multipart = new MultipartUtility(requestURL, charset);

        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < files.size(); i++) {
            File file = new File(files.get(i));
            try {
                assert multipart != null;
                multipart.addFilePart("file" + i, file);
                Log.d(TAG + "multi_file", String.valueOf(file));
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG + "mutli_file_error", e.getMessage());

            }
        }

        try {
            assert multipart != null;
            List<String> response = multipart.finish();
            JSONObject jsonResponse = new JSONObject(response.get(0));
            Log.d(TAG + "multi_response", jsonResponse.toString());
            photos = jsonResponse.getJSONObject("photos");

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Log.d(TAG + "mutli_error", e.getMessage());
        }

        if (photo_ids_list.size() == 1)
            inAlbum = true;

        for (int i = 0; i < photo_ids_list.size(); i++) {

            Log.d(TAG + "photo_id", photo_ids_list.get(i));


            try {
                assert photos != null;
                tokenList.add(String.valueOf(photos.getJSONObject(photo_ids_list.get(i)).get("token")));
                photoCommit(photo_ids_list.get(i), String.valueOf(photos.getJSONObject(photo_ids_list.get(i)).get("token")));


                /*Добавляем описание к фото*/

                HashMap<String, String> params = new HashMap<>();
                params.put("photo_id", assigned_photo_id_list.get(i));
                params.put("description", MainActivity.postText);
                params.put("uid", "59024169166");


                if (inAlbum) {
                    odnoklassniki.request("photos.editPhoto", params, OkRequestMode.DEFAULT, new OkListener() {
                        @Override
                        public void onSuccess(JSONObject json) {
                            Log.d(TAG + "jsonFromEditPhoto", json.toString());
                            Toast.makeText(context, "Фото опубликовано в альбом Мои работы", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onError(String error) {
                            Log.d(TAG + "jsonFromEditPhotoError", error);
                        }
                    });
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (tokenList.size() != 1)
            postOnOKWall(tokenList, assigned_photo_id_list, MainActivity.postText);
        else {
            if (!inAlbum)
                getUploadUrl(pathToImagesAfterList);
        }
    }

    /*Подтверждаем загрузку фото*/
    private void photoCommit(String photo_id, String token) {
        final Map<String, String> params = new HashMap<>();
        params.put("photo_id", photo_id);
        params.put("token", token);
        String assigned_photo_id;
        try {
            String photos_commit = odnoklassniki.request("photosV2.commit", params, OkRequestMode.DEFAULT);
            JSONObject jsonObject = new JSONObject(photos_commit);
            Log.d(TAG + "commit_response", jsonObject.toString());
            JSONArray photos = jsonObject.getJSONArray("photos");
            assigned_photo_id = String.valueOf(photos.getJSONObject(0).get("assigned_photo_id"));
            assigned_photo_id_list.add(assigned_photo_id);

            Log.d(TAG + "photos", photos.toString());
            Log.d(TAG + "assigned_photo_id", assigned_photo_id);


            Log.d("OKCommit", photos_commit);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        for (String s : tokenList)
            Log.d(TAG + "tokenList", s);
        for (String s : assigned_photo_id_list)
            Log.d(TAG + "assigned_photo_id_list", s);

    }

    /*Создаём пост на стене пользователя*/
    private void postOnOKWall(List<String> tokenList, List<String> assigned_photo_id_list, String postText) {

        StringBuilder jsonStringToPost = new StringBuilder("{ \"media\":[ ");

        if (!postText.equals("")) {
            jsonStringToPost.append("{ \"type\": \"text\", \"text\": \"" + postText + "\" }, ");
        }
        jsonStringToPost.append("{ \"type\": \"photo\", \"list\":[");

        Log.d(TAG + "equals", String.valueOf(tokenList.size() == assigned_photo_id_list.size()));

        for (int i = 0; i < tokenList.size(); i++) {
            jsonStringToPost.append(" {\"id\": \"" + tokenList.get(i) + "\" }, { \"photoId\": \"" + assigned_photo_id_list.get(i) + "\"},");
        }
        jsonStringToPost.deleteCharAt(jsonStringToPost.toString().length() - 1);
        jsonStringToPost.append("] }]}");


        Log.d(TAG + "_jsonPostString", jsonStringToPost.toString());

        JSONObject jsonPostString = null;
        try {
            jsonPostString = new JSONObject(jsonStringToPost.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        this.tokenList = new ArrayList<>();
        this.assigned_photo_id_list = new ArrayList<>();
        odnoklassniki.performPosting(activity, String.valueOf(jsonPostString), false, null);
    }
}

