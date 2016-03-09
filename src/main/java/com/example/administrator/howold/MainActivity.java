package com.example.administrator.howold;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facepp.error.FaceppParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;


public class MainActivity extends Activity implements View.OnClickListener {

    private static final int PICK_CODE = 0x110;
    private static final int MSG_SUCCESS = 0x111;
    private static final int MSG_ERROR = 0x112;
    private static final int TAKE_PHONE_CODE = 0x113;
    private Button bt_look;
    private Button bt_select;
    private Button bt_takePhoto;
    private TextView tv_hint;
    private ImageView imageView;
    //定义图片的存储路径
    private String currentPhotoStr;
    //图片的bitmap
    private Bitmap mbitmap;
    //拍照的bitmap
    private Bitmap takeBitmap;

    private Paint mpaint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initEvent();
    }

    private void initEvent() {
        bt_look.setOnClickListener(this);
        bt_select.setOnClickListener(this);
        bt_takePhoto.setOnClickListener(this);
        mpaint = new Paint();
    }

    private void initViews() {
        bt_look = (Button) findViewById(R.id.bt_look);
        bt_select = (Button) findViewById(R.id.bt_select);
        bt_takePhoto = (Button) findViewById(R.id.bt_takePhoto);
        tv_hint = (TextView) findViewById(R.id.tv_hint);
        imageView = (ImageView) findViewById(R.id.imageView);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case PICK_CODE: {
                if (intent != null) {
                    Uri uri = intent.getData();
                    //获取游标
                    Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                    cursor.moveToFirst();
                    //获取当前图片的索引
                    int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    //获取当前图片的路径
                    currentPhotoStr = cursor.getString(idx);
                    cursor.close();

                    //压缩图片
                    resizePhoto();
                    //显示图片
                    imageView.setImageBitmap(mbitmap);
                    //设置提示文字
                    tv_hint.setText("提示：请点击查看按钮");

                }
            }
            break;
            case TAKE_PHONE_CODE: {
//                Bundle bundle = intent.getExtras();
//                //获取相机返回的数据，并转换成bitmap格式
//                takeBitmap = (Bitmap) bundle.get("data");
//                imageView.setImageBitmap(takeBitmap);
                String sdStatus = Environment.getExternalStorageState();
                // 检测sd是否可用
                if (!sdStatus.equals(Environment.MEDIA_MOUNTED)) {
                    Log.i("TestFile",
                            "SD card is not avaiable/writeable right now.");
                    return;
                }
                new DateFormat();
                String name = DateFormat.format("yyyyMMdd_hhmmss",
                        Calendar.getInstance(Locale.CHINA))
                        + ".jpg";
                Bundle bundle = intent.getExtras();
                // 获取相机返回的数据，并转换为Bitmap图片格式
                takeBitmap = (Bitmap) bundle.get("data");
                FileOutputStream b = null;
                File file = new File("/sdcard/Image/");
                // 创建文件夹
                file.mkdirs();
                String fileName = "/sdcard/Image/" + name;


                try {
                    b = new FileOutputStream(fileName);
                    // 把数据写入文件
                    takeBitmap.compress(Bitmap.CompressFormat.JPEG, 100, b);
                    imageView.setImageBitmap(takeBitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        b.flush();
                        b.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }
            }
            break;
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    private void resizePhoto() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        //设置不返回Bitmap,只返回图片的宽和高
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(currentPhotoStr, options);
        //设置压缩值
        double ratio = Math.max(options.outWidth * 1.0d / 1024f, options.outHeight * 1.0d / 1024f);

        options.inSampleSize = (int) Math.ceil(ratio);

        options.inJustDecodeBounds = false;
        mbitmap = BitmapFactory.decodeFile(currentPhotoStr, options);
    }


    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MSG_SUCCESS:
                    JSONObject jsonObject = (JSONObject) msg.obj;
                    //重新生成一个Bitmap
                    prepareRsBitmap(jsonObject);

                    //设置imageview
                    imageView.setImageBitmap(mbitmap);

                    break;
                case MSG_ERROR:
                    String errorMessage = (String) msg.obj;
                    if (TextUtils.isEmpty(errorMessage)) {
                        tv_hint.setText("ERROR");
                    } else {
                        tv_hint.setText(errorMessage);
                    }
                    break;
            }
            super.handleMessage(msg);
        }

    };


    private void prepareRsBitmap(JSONObject jsonObject) {

        Bitmap bitmap = Bitmap.createBitmap(mbitmap.getWidth(), mbitmap.getHeight(), mbitmap.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(mbitmap, 0, 0, null);
        try {
            //拿到faces数组对象
            JSONArray faces = jsonObject.getJSONArray("face");
            int faceCount = faces.length();
            tv_hint.setText("发现" + faceCount + "张脸");
            for (int i = 0; i < faceCount; i++) {
                //拿到单独的face对象
                JSONObject face = faces.getJSONObject(i);
                JSONObject position = face.getJSONObject("position");
                //获取脸的中心坐标
                float x = (float) position.getJSONObject("center").getDouble("x");
                float y = (float) position.getJSONObject("center").getDouble("y");
                //获取脸的宽度和高度
                float w = (float) position.getDouble("width");
                float h = (float) position.getDouble("height");

                //将百分比转化成实际的像素值
                x = x / 100 * bitmap.getWidth();
                y = y / 100 * bitmap.getHeight();

                w = w / 100 * bitmap.getWidth();
                h = h / 100 * bitmap.getHeight();


                //设置画脸上线的颜色
                mpaint.setColor(0xffffffff);
                //设置线的宽度
                mpaint.setStrokeWidth(3);
                //绘制脸上的四根线
                canvas.drawLine(x - w / 2, y - h / 2, x - w / 2, y + h / 2, mpaint);
                canvas.drawLine(x - w / 2, y - h / 2, x + w / 2, y - h / 2, mpaint);
                canvas.drawLine(x + w / 2, y - h / 2, x + w / 2, y + h / 2, mpaint);
                canvas.drawLine(x - w / 2, y + h / 2, x + w / 2, y + h / 2, mpaint);


                //得到年龄和性别
                int age = face.getJSONObject("attribute").getJSONObject("age").getInt("value");
                String gender = face.getJSONObject("attribute").getJSONObject("gender").getString("value");

                //构造一个年龄和性别的bitmap对象
                Bitmap agebitmap = buildAgeBitmap(age, "Male".equals(gender));

                //缩放bitmap
                int ageWidth = agebitmap.getWidth();
                int ageHeight = agebitmap.getHeight();

                if (bitmap.getWidth() < mbitmap.getWidth() && bitmap.getHeight() < mbitmap.getHeight()) {
                    float ratio = Math.max(bitmap.getWidth() * 1.0f / mbitmap.getWidth(),
                            bitmap.getHeight() * 1.0f / mbitmap.getHeight());

                    agebitmap = Bitmap.createScaledBitmap(agebitmap, (int) (ageWidth * ratio),
                            (int) (ageHeight * ratio), false);

                }
                canvas.drawBitmap(agebitmap, x - agebitmap.getWidth() / 2,
                        y - h / 2 - agebitmap.getHeight(), null);

                mbitmap = bitmap;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Bitmap buildAgeBitmap(int age, boolean isMale) {
        TextView tv_age_and_gender = (TextView) findViewById(R.id.tv_age_and_gender);
        tv_age_and_gender.setText(age + "");
        if (isMale) {
            tv_age_and_gender.setCompoundDrawablesWithIntrinsicBounds(
                    getResources().getDrawable(R.drawable.male), null, null, null);
        } else {
            tv_age_and_gender.setCompoundDrawablesWithIntrinsicBounds(
                    getResources().getDrawable(R.drawable.female), null, null, null);
        }

        tv_age_and_gender.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(tv_age_and_gender.getDrawingCache());
        tv_age_and_gender.destroyDrawingCache();

        return bitmap;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_look:

                final ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setTitle("加载中");
                progressDialog.setCancelable(false);
                progressDialog.show();

                if (currentPhotoStr != null && !currentPhotoStr.equals("")) {
                    resizePhoto();
                } else if (takeBitmap != null) {
                    mbitmap = takeBitmap;
                } else {
                    mbitmap = BitmapFactory.decodeResource(getResources(),
                            R.drawable.demo);
                }
                FaceDetect.detect(mbitmap, new FaceDetect.CallBack() {
                    @Override
                    public void success(JSONObject result) {
                        progressDialog.dismiss();
                        Message message = Message.obtain();
                        message.what = MSG_SUCCESS;
                        message.obj = result;
                        mHandler.sendMessage(message);
                    }

                    @Override
                    public void error(FaceppParseException e) {
                        progressDialog.dismiss();
                        Message message = Message.obtain();
                        message.what = MSG_ERROR;
                        message.obj = e.getErrorMessage();
                        mHandler.sendMessage(message);
                    }
                });
                break;

            case R.id.bt_takePhoto: {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, TAKE_PHONE_CODE);
                break;
            }

            case R.id.bt_select:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_CODE);

                break;

        }
    }
}
