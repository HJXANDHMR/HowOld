package com.example.administrator.howold;


import android.graphics.Bitmap;
import android.util.Log;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * Created by Administrator on 2016/3/8.
 */
public class FaceDetect {

    public interface CallBack {
        void success(JSONObject result);

        void error(FaceppParseException e);
    }


    public static void detect(final Bitmap bitmap, final CallBack callBack) {

        new Thread(new Runnable() {
            public static final String TAG = "FaceDetect";

            @Override
            public void run() {

                try {

                    HttpRequests httpRequests = new HttpRequests(Constant.KEY, Constant.SECRET, true, true);

                    Bitmap bmsmall = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight());
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bmsmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    //将输出流转化成二进制数组
                    byte[] arrays = stream.toByteArray();
                    PostParameters postParameters = new PostParameters();
                    postParameters.setImg(arrays);
                    JSONObject jsonObject = httpRequests.detectionDetect(postParameters);

                    Log.e(TAG, jsonObject.toString());

                    if (callBack != null) {
                        callBack.success(jsonObject);
                    }

                } catch (FaceppParseException e) {
                    e.printStackTrace();
                    if (callBack != null) {
                        callBack.error(e);
                    }
                }
            }
        }).start();
    }

}

