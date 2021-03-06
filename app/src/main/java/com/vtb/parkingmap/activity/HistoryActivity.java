package com.vtb.parkingmap.activity;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.bht.saigonparking.api.grpc.booking.Booking;
import com.bht.saigonparking.api.grpc.booking.BookingServiceGrpc;
import com.bht.saigonparking.api.grpc.booking.GetAllBookingOfCustomerRequest;
import com.google.protobuf.Empty;
import com.vtb.parkingmap.R;
import com.vtb.parkingmap.adapter.BookingHistoryListAdapter;
import com.vtb.parkingmap.base.BaseSaigonParkingActivity;
import com.vtb.parkingmap.models.History;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class HistoryActivity extends BaseSaigonParkingActivity {
    private ListView lvProduct;
    private BookingHistoryListAdapter adapter;
    private List<History> mProductList;
    private List<Booking> getallhistorymore;
    public Handler mHandler;
    public View ftView;
    public boolean isLoading = false;
    private BookingServiceGrpc.BookingServiceBlockingStub bookingServiceBlockingStub;
    private TextView tv_name;
    private TextView tv_description;
    private LinearLayout lnHistoryItem;

    // page
    //ID
    int pagenumber = 1;
    long countallhistory;
    //GetAllBookingHistory
    List<Booking> getallhistory;
    TextView txtcount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bookingServiceBlockingStub = serviceStubs.getBookingServiceBlockingStub();
        Intent intent = getIntent();
        setContentView(R.layout.activity_history);

        lvProduct = findViewById(R.id.listview_history);
        txtcount = findViewById(R.id.countallcomment);

        LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ftView = li.inflate(R.layout.footer_view, null);
        mHandler = new MyHandler();
        mProductList = new ArrayList<>();
        //Add sample data for list
        //We can get data from DB, webservice here
        //call api
        //COUNT ALL

        callApiWithExceptionHandling(() -> {
            countallhistory = bookingServiceBlockingStub
                    .countAllBookingOfCustomerByAuthorizationHeader(Empty.getDefaultInstance())
                    .getValue();
        });

        callApiWithExceptionHandling(() -> {
            getallhistory = bookingServiceBlockingStub
                    .getAllBookingOfCustomer(GetAllBookingOfCustomerRequest.newBuilder()
                            .setNRow(10)
                            .setPageNumber(pagenumber)
                            .build())
                    .getBookingList();
        });


        //


        //
        for (Booking booking : getallhistory) {
            mProductList.add(new History(booking.getParkingLotName(), booking.getLicensePlate(), booking.getCreatedAt(), booking.getId(), booking.getLatestStatus()));

        }
        //

        //Init adapter
        adapter = new BookingHistoryListAdapter(getApplicationContext(), mProductList);
        lvProduct.setAdapter(adapter);
        if (countallhistory != 0) {
            if (countallhistory < 10) {
                txtcount.setText(((countallhistory) + "/" + (countallhistory)));
            } else {
                txtcount.setText(((pagenumber * 10) + "/" + (countallhistory)));
            }
        } else {
            txtcount.setText(((0) + "/" + (countallhistory)));
        }

        lvProduct.setOnItemClickListener((parent, view, position, id) -> {
            lnHistoryItem = view.findViewById(R.id.lnHistoryItem);
            tv_name = view.findViewById(R.id.tv_name);
            tv_description = view.findViewById(R.id.tv_description);
            /* send booking detail to BookingHistoryDetailsActivity */
            String originBookingId = view.getTag().toString();
            Intent historydetail = new Intent(HistoryActivity.this, BookingHistoryDetailsActivity.class);
            historydetail.putExtra("originBookingId", (Serializable) originBookingId);

            Pair<View, String> p1 = Pair.create(tv_name, "ParkingLotName");
            Pair<View, String> p2 = Pair.create(tv_description, "LicensePlate");
            Pair<View, String> p3 = Pair.create(lnHistoryItem, "linearLayout");

            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this, p3, p2, p1);
            startActivityWithLoadingAndOption(historydetail, options);
        });

        lvProduct.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                //Check when scroll to last item in listview, in this tut, init data in listview = 10 item
                Log.d("BachMap", pagenumber + "");
                if (view.getLastVisiblePosition() == totalItemCount - 1 && lvProduct.getCount() >= 10 && isLoading == false && ((pagenumber * 10) < countallhistory)) {
                    isLoading = true;
                    Thread thread = new ThreadGetMoreData();
                    //Start thread
                    thread.start();
                }

            }
        });
    }

    public class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    //Add loading view during search processing
                    lvProduct.addFooterView(ftView);
                    break;
                case 1:
                    //Update data adapter and UI
                    adapter.addListItemToAdapter((ArrayList<History>) msg.obj);
                    //Remove loading view after update listview
                    lvProduct.removeFooterView(ftView);
                    isLoading = false;
                    break;
                default:
                    break;
            }
        }
    }

    private ArrayList<History> getMoreData() {

        ArrayList<History> lst = new ArrayList<>();
        pagenumber = pagenumber + 1;
        //get booking history + add to list

        callApiWithExceptionHandling(() -> {
            getallhistorymore = bookingServiceBlockingStub
                    .getAllBookingOfCustomer(GetAllBookingOfCustomerRequest.newBuilder()
                            .setNRow(5)
                            .setPageNumber(pagenumber)
                            .build())
                    .getBookingList();

            for (Booking booking : Objects.requireNonNull(getallhistorymore)) {
                lst.add(new History(booking.getParkingLotName(), booking.getLicensePlate(), booking.getCreatedAt(), booking.getId(), booking.getLatestStatus()));
            }
        });


        //

        //Sample code get new data :P

        return lst;
    }

    public class ThreadGetMoreData extends Thread {
        @Override
        public void run() {
            //Add footer view after get data
            mHandler.sendEmptyMessage(0);
            //Search more data
            ArrayList<History> lstResult = getMoreData();
            //Delay time to show loading footer when debug, remove it when release
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //Send the result to Handle
            Message msg = mHandler.obtainMessage(1, lstResult);
            mHandler.sendMessage(msg);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if ((pagenumber * 10) > countallhistory) {
                        txtcount.setText(String.valueOf((countallhistory) + "/" + String.valueOf(countallhistory)));
                    } else {
                        txtcount.setText(String.valueOf((pagenumber * 10) + "/" + String.valueOf(countallhistory)));
                    }

                }
            });
        }
    }
}