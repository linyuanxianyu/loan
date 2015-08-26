package com.souche.menu;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.google.gson.Gson;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.souche.menu.activity.AnswerConditionActivity;
import com.souche.menu.activity.BaseActicity;
import com.souche.menu.activity.DetailActivity;
import com.souche.menu.adapter.OrderListAdapter;
import com.souche.menu.model.OrderModel;
import com.souche.menu.queryparam.QueryParam;
import com.souche.menu.view.XListView;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends BaseActicity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,XListView.IXListViewListener {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
        init();
    }




    private XListView mListView;
    private OrderListAdapter mAdapter;
    private Handler mHandler;
    /** Called when the activity is first created. */

    QueryParam queryParam;

    private int pageNum=1;


    private void init(){

        queryParam = new QueryParam();
        queryParam.setOrderFlow(100);


        mListView = (XListView) findViewById(R.id.xListView);
        mListView.setPullLoadEnable(true);
        mAdapter = getOrderListAdapter();
        mAdapter.mImageLoader.init(ImageLoaderConfiguration.createDefault(MainActivity.this));

        mListView.setAdapter(mAdapter);

        mListView.setXListViewListener(this);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0) {
                    OrderModel orderModel = mAdapter.getItem(position - 1);
                    Intent intent;
                    if (getString(R.string.STATUS_VALID).equals(orderModel.getStatus())&&
                            getString(R.string.UntreatedOrder).equals(orderModel.getOrderFlow())) {
                        intent = new Intent(MainActivity.this, AnswerConditionActivity.class);
                    }else {
                        intent = new Intent(MainActivity.this, DetailActivity.class);
                    }
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("orderModel", orderModel);
                    intent.putExtras(bundle);
                    startActivity(intent);

                }
            }
        });

        mHandler = new Handler();
    }


    private OrderListAdapter getOrderListAdapter(){
        return new OrderListAdapter(this);
    }



    @Override
    protected void onPause() {
        super.onPause();
        //获取焦点，否则在回到这个页面时位置会变化
        mListView.requestFocus();

    }

    @Override
    protected void onResume(){
        super.onResume();
        onRefreshShow(true);
    }



    private void onRefreshShow(final Boolean showDialog){
        pageNum=1;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mAdapter.mDataList.clear();
                sendPost(getOrderListUrl(queryParam), showDialog, MainActivity.this);
                mAdapter = getOrderListAdapter();
                mListView.setAdapter(mAdapter);
                onLoad();
            }
        }, 1000);
    }

    private void onLoad() {
        mListView.stopRefresh();
        mListView.stopLoadMore();
        mListView.setRefreshTime("刚刚");
    }

    @Override
    public void onRefresh() {
        onRefreshShow(false);
    }
    @Override
    public void onLoadMore() {
        pageNum ++;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendPost(getOrderListUrl(queryParam), false, MainActivity.this);
                mAdapter.notifyDataSetChanged();
                onLoad();
            }
        }, 1000);
    }

    private String getOrderListUrl(QueryParam queryParam){
        String url = getString(R.string.baseUrl);
        url += getString(R.string.getV3OrderList);
        Uri.Builder builder = Uri.parse(url).buildUpon();
        builder.appendQueryParameter("pageNum", String.valueOf(pageNum));
        if(queryParam.getOrderFlow()!=null){
            builder.appendQueryParameter("orderFlow", String.valueOf(queryParam.getOrderFlow()));
        }

        if(StringUtils.isNotBlank(queryParam.getCarName())){
            builder.appendQueryParameter("carName", queryParam.getCarName());
        }


        if(StringUtils.isNotBlank(queryParam.getApplyCity())){
            builder.appendQueryParameter("applyCity", queryParam.getApplyCity());
        }


        if(StringUtils.isNotBlank(queryParam.getBuyPhone())){
            builder.appendQueryParameter("buyerPhone",queryParam.getBuyPhone());
        }


        if(StringUtils.isNotBlank(queryParam.getSellPhone())){
            builder.appendQueryParameter("sellPhone",queryParam.getSellPhone());
        }


        if(StringUtils.isNotBlank(queryParam.getVinNumber())){
            builder.appendQueryParameter("vinNumber", queryParam.getVinNumber());
        }


        if(queryParam.getOrderId()!=null){
            builder.appendQueryParameter("orderId", queryParam.getOrderId().toString());
        }


        if(StringUtils.isNotBlank(queryParam.getTimeBegin())){
            builder.appendQueryParameter("timeBegin", queryParam.getTimeBegin());
        }

        if(StringUtils.isNotBlank(queryParam.getTimeEnd())){
            builder.appendQueryParameter("timeEnd", queryParam.getTimeEnd());
        }


        builder.appendQueryParameter("status", queryParam.getStatus());
        builder.appendQueryParameter("pageSize", getString(R.string.pageSize));
        return builder.toString();
    }



    @Override
    protected void parseResponse(JSONObject response,final Context context) throws JSONException {
        if(response.has("status")&&"0".equals(response.get("status"))) {
            try {
                JSONObject photos = response.getJSONObject("data");
                JSONArray jsonArray = photos.getJSONArray("items");

                for(int index = 0 ; index < jsonArray.length(); index++) {

                    JSONObject jsonObj = jsonArray.getJSONObject(index);
                    Gson gson = new Gson();
                    OrderModel orderModel = gson.fromJson(jsonObj.toString(),OrderModel.class);
                    mAdapter.mDataList.add(orderModel);
                }
                for(int i =0;i<5;i++){
                    OrderModel orderModel = new OrderModel();
                    orderModel.setCarName("多点点的茶");
                    orderModel.setOrderId(queryParam.getOrderFlow()+i);
                    orderModel.setBuyerPhone("15700129331");
                    orderModel.setSeller("15700129333");
                    mAdapter.mDataList.add(orderModel);
                }
                mAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }




















    @Override
    public void onNavigationDrawerItemSelected(int position) {

        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();

    }

    public void onSectionAttached(int number) {
        mTitle = getResources().getStringArray(R.array.orderFlow)[number - 1];
        if(number==2){
            queryParam.setOrderFlow(200);

        }else if(number==1){
            queryParam.setOrderFlow(100);
        }
        mAdapter.mDataList.clear();
        sendPost(getOrderListUrl(queryParam), true, MainActivity.this);
        mAdapter.notifyDataSetChanged();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);


    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView;
            switch (getArguments().getInt(ARG_SECTION_NUMBER)){
                case  1:
                    rootView = inflater.inflate(R.layout.fragment_main, container, false);
                    break;
                default:
                    rootView = inflater.inflate(R.layout.f_wait, container, false);
            }
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

}
