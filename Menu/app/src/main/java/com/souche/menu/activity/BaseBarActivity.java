package com.souche.menu.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by wangjiguang on 15/8/27.
 */
public class BaseBarActivity extends ActionBarActivity {
    protected final String TAG_REQUEST = "MY_TAG";

    protected JsonObjectRequest jsonObjRequest;
    protected RequestQueue mVolleyQueue;


    private ProgressDialog mProgress;

    private static long lastClickTime;
    private static int lastClickViewId;
    private static final int KEY_PREVENT_TS = -20000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVolleyQueue = Volley.newRequestQueue(this);
    }


    protected void showProgress(final Context context) {
        mProgress = ProgressDialog.show(context, "", "稍等...");
    }

    protected void stopProgress() {
        if(mProgress!=null)
            mProgress.cancel();
    }


    protected void parseResponse(JSONObject response,final Context context) throws JSONException {

        showToast(context,"操作成功");
    }




    /**
     * d
     * 发送取消／恢复当前订单http请求
     *
     * @param
     */
    protected void sendPost(String url, final boolean show,final Context context) {
        if(show){
            showProgress(this);
        }
        jsonObjRequest = new JsonObjectRequest(Request.Method.POST, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response.has("status") && "0".equals(response.get("status"))) {
                        if(show){
                            stopProgress();
                        }
                        parseResponse(response, context);
                    } else if (response.has("msg")) {
                        if(show){
                            stopProgress();
                        }
                        showToast(context,response.get("msg").toString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    stopProgress();
//                    showToast(context,"JSON parse error");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.w("loan", error.getMessage());
                stopProgress();
                showToast(context,"网络错误");

            }
        });
        jsonObjRequest.setRetryPolicy(new DefaultRetryPolicy(5000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        jsonObjRequest.setTag(TAG_REQUEST);
        mVolleyQueue.add(jsonObjRequest);
    }

    /**
     * 判断是否异常的双击（300毫秒内不能点击不同控件，500毫秒内不能点击相同控件）
     *
     * @return
     */
    public static boolean isFastDoubleClick(View v) {
        long now = System.currentTimeMillis();
        //检查是否被阻止点击
        if (v.getTag(KEY_PREVENT_TS) != null && v.getTag(KEY_PREVENT_TS) instanceof Long) {
            if ((Long) v.getTag(KEY_PREVENT_TS) > now) {
                return true;
            }
        }
        long interval = now - lastClickTime;
        if (lastClickViewId == v.getId() && interval < 500) {
            return true;
        } else if (interval < 300) {
            return true;
        }
        lastClickViewId = v.getId();
        lastClickTime = now;
        return false;
    }

    protected void showToast(final Context context,final String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }
}
