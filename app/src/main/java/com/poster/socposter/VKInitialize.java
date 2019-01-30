package com.poster.socposter;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.poster.socposter.Utils.MultipartUtility;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiPhoto;
import com.vk.sdk.util.VKJsonHelper;
import com.vk.sdk.util.VKUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VKInitialize extends Application {


    private int albumID = 25881809;
    private int isLoad = 0;

    private String charset = "UTF-8";
    private String postText = "";
    private String TAG = "SocPoster_VKInitialize_";

    private List<String> photosIdList = new ArrayList<>();
    private List<String> pathToImagesAfterList;
    private List<String> pathToImagesBeforeList;

    private Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        VKSdk.initialize(this);

        pathToImagesAfterList = new ArrayList<>();
        pathToImagesBeforeList = new ArrayList<>();
    }

    /*Получаем url для загрузки фото*/
    protected void getVKUploadServer(final Context context, final boolean isAfter, String postText, List<String> pathToImagesAfterList,
                                     List<String> pathToImagesBeforeList) {

        this.context = context;
        this.postText = postText;
        this.pathToImagesAfterList = pathToImagesAfterList;
        this.pathToImagesBeforeList = pathToImagesBeforeList;

        if (isAfter)
            albumID = 24012889;

        else
            albumID = 25881809;
        VKRequest albums = new VKRequest("photos.getUploadServer", VKParameters.from(VKApiConst.ALBUM_ID, albumID, VKApiConst.GROUP_ID, 13769055));

        albums.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                Toast.makeText(context, "getServer Good", Toast.LENGTH_LONG).show();
                JSONObject jsonResponse = null;

                String upload_url = null;
                try {
                    jsonResponse = response.json.getJSONObject("response");
                    upload_url = jsonResponse.getString("upload_url");

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Log.d(TAG + "uploadUrl", upload_url);

                /*Отправляем фото на сервер*/
                sendMultipart(upload_url, isAfter, albumID);
            }

            @Override
            public void onError(VKError error) {
                Toast.makeText(context, "getServer Error", Toast.LENGTH_LONG).show();
                Log.d(TAG + "getServer_error", error.toString());
            }
        });
    }

    /*Отправляем фото на сервер*/
    private void sendMultipart(final String requestURL, final boolean isAfter, final int albumID) {


        new Thread(new Runnable() {
            @Override
            public void run() {

                List<String> files = new ArrayList<>();

                MultipartUtility multipart = null;
                try {
                    multipart = new MultipartUtility(requestURL, charset);
                    if (isAfter)
                        files = pathToImagesAfterList;
                    else
                        files = pathToImagesBeforeList;
                    for (int i = 0; i < files.size(); i++) {
                        File file = new File(files.get(i));
                        multipart.addFilePart("file" + i, file);
                        Log.d(TAG + "mutli_file", String.valueOf(file));
                    }

                    List<String> response = multipart.finish();

                    JSONObject jsonResponse = new JSONObject(response.get(0));


                    /*Сохраняем фото в альбом*/

                    savePhotosInVKAlbum(jsonResponse, albumID, isAfter);
                    Log.d(TAG + "multi_response", response.toString());

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG + "mutli_error", e.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    /*Сохраняем фото в альбом*/
    protected void savePhotosInVKAlbum(JSONObject jsonResponse, int albumID, boolean isAfter) throws JSONException {

        if (!isAfter) {
            isLoad++;
        }

        VKRequest saveRequest = VKApi.photos().save(new VKParameters(VKJsonHelper.toMap(jsonResponse)));
        saveRequest.addExtraParameters(VKUtil.paramsFrom(VKApiConst.ALBUM_ID, albumID));
        saveRequest.addExtraParameters(VKUtil.paramsFrom(VKApiConst.GROUP_ID, 13769055));
        saveRequest.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                Log.d(TAG + "savePhotos_response", response.responseString);

                JSONObject jsonObject = response.json;
                JSONArray resp = null;
                JSONObject newResp = null;
                String id = null;

                try {
                    resp = jsonObject.getJSONArray("response");
                    for (int i = 0; i < resp.length(); i++) {
                        newResp = resp.getJSONObject(i);
                        photosIdList.add(newResp.getString("id"));
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Log.d(TAG + "photo_id", resp.toString());
                if (isLoad == 0) {
                    isLoad++;
                    if (!pathToImagesBeforeList.isEmpty())
                        getVKUploadServer(context,false, postText, pathToImagesAfterList, pathToImagesBeforeList);
                    else
                        isLoad++;
                }

                if (isLoad == 2) {
                    postOnVKWall(id, photosIdList, postText);
                    isLoad++;
                    Log.d(TAG + "photoList_size", String.valueOf(photosIdList.size()));
                }
            }

            @Override
            public void onError(VKError error) {
                Log.d(TAG + "savePhotos_error", error.toString());
            }
        });
    }

    /*Создаём пост на стене*/
    protected void postOnVKWall(String OwnerID, List<String> list, String postText) {

        StringBuilder mediaString = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            mediaString.append("photo-137690557_" + list.get(i) + ",");
        }


        VKRequest postWall = VKApi.wall().post(VKParameters.from(VKApiConst.OWNER_ID, -13769055, VKApiConst.MESSAGE, postText,
                VKApiConst.FRIENDS_ONLY, 0, VKApiConst.FROM_GROUP, 1, VKApiConst.ATTACHMENTS, mediaString));
        postWall.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                Log.d(TAG + "postOnWall_response", response.responseString);
                JSONObject resp = response.json;
                String postID = null;
                try {

                    postID = resp.getJSONObject("response").getString("post_id");
                    Log.d(TAG + "postOnWall_postID", postID);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Toast.makeText(context, "Post Good", Toast.LENGTH_LONG).show();
                repostFromVKWall(postID);
            }

            @Override
            public void onError(VKError error) {
                Toast.makeText(context, "Post Error", Toast.LENGTH_LONG).show();
                Log.d(TAG + "postOnWall_error", error.toString());
            }
        });
    }

    /*Делаем репост*/
    protected void repostFromVKWall(String postID) {

        VKRequest repostFromWall = VKApi.wall().repost(VKParameters.from());
        repostFromWall.addExtraParameter("object", "wall-13769055_" + postID);
        repostFromWall.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                Log.d(TAG + "repost_response", response.responseString);
                Toast.makeText(context, "Пост опубликован ВКонтакте", Toast.LENGTH_LONG).show();
                pathToImagesAfterList = new ArrayList<>();
                pathToImagesBeforeList = new ArrayList<>();
            }

            @Override
            public void onError(VKError error) {
                Log.d(TAG + "repost_error", error.toString());

            }
        });
    }

    /*Получаем список групп*/
    private void getGroup() {
        final VKRequest getGroups = new VKRequest("groups.get", VKParameters.from(VKApiConst.FIELDS, " place"));


        getGroups.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                Toast.makeText(context, "Group Good", Toast.LENGTH_LONG).show();
                Log.d(TAG + "getGroup_response", response.json.toString());

                JSONObject jsonResponse = null;
                JSONObject item;
                JSONArray items = null;
                int GROUP_ID = 0;
                try {
                    jsonResponse = response.json.getJSONObject("response");
                    items = jsonResponse.getJSONArray("items");
                    /*GROUP_ID = items.toString();*/
                    GROUP_ID = Integer.valueOf("-13769055");
                    /*for (int i = 0; i < items.length(); i++) {

                        item = new JSONObject(items.getString(i));
                        GROUP_ID = item.getString("");
                        Log.d("Json", GROUP_ID);
                    }*/
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Log.d("Json", jsonResponse.toString());
                Log.d("Json", items.toString());
                Log.d("JsonGROUP", String.valueOf(GROUP_ID));

                getVKAlbums();


            }
        });
    }

    /*Получаем альбомы пользователя*/
    private void getVKAlbums() {
        VKRequest albums = new VKRequest("photos.getAlbums", VKParameters.from(VKApiConst.OWNER_ID, -17668516));
        albums.setModelClass(VKApiPhoto.class);
        albums.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                Toast.makeText(context, "Photo Good", Toast.LENGTH_LONG).show();
                JSONObject jsonResponse = null;
                JSONObject item;
                JSONArray items = null;
                try {
                    jsonResponse = response.json.getJSONObject("response");
                    items = jsonResponse.getJSONArray("items");

                    for (int i = 0; i < items.length(); i++) {

                        item = new JSONObject(items.getString(i));
                        Log.d("Json", item.getString("title"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Log.d("Json", jsonResponse.toString());
                Log.d("Json", items.toString());

            }

            @Override
            public void onError(VKError error) {
                Toast.makeText(context, "Photo Error", Toast.LENGTH_LONG).show();

            }
        });
    }
}
