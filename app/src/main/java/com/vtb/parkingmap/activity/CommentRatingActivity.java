package com.vtb.parkingmap.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bht.saigonparking.api.grpc.booking.BookingRating;
import com.bht.saigonparking.api.grpc.booking.BookingServiceGrpc;
import com.bht.saigonparking.api.grpc.booking.CountAllRatingsOfParkingLotRequest;
import com.bht.saigonparking.api.grpc.booking.GetAllRatingsOfParkingLotRequest;
import com.bht.saigonparking.api.grpc.parkinglot.ParkingLot;
import com.bht.saigonparking.api.grpc.parkinglot.ParkingLotInformation;
import com.vtb.parkingmap.R;
import com.vtb.parkingmap.adapter.ProductListAdapter;
import com.vtb.parkingmap.base.BaseSaigonParkingActivity;
import com.vtb.parkingmap.models.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CommentRatingActivity extends BaseSaigonParkingActivity {
    private ParkingLot parkingLot;
    private ImageView imageView;
    private byte[] imageData;
    private ListView lvProduct;
    private ProductListAdapter adapter;
    private List<Product> mProductList;
    private List<BookingRating> getallratingmore;
    public Handler mHandler;
    public View ftView;
    public boolean isLoading = false;


    private BookingServiceGrpc.BookingServiceBlockingStub bookingServiceBlockingStub;

    // page
    //ID
    long idplacedetail;
    int pagenumber = 1;
    long countallrating;
    //GetAllRating
    List<BookingRating> getallrating;
    TextView txtcount;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        idplacedetail = (long) intent.getSerializableExtra("idplacedetail");
        parkingLot = (ParkingLot) intent.getSerializableExtra("parkingLot");
        bookingServiceBlockingStub = serviceStubs.getBookingServiceBlockingStub();
        setContentView(R.layout.activity_comment_rating);
        imageView = findViewById(R.id.imageView2);
        lvProduct = (ListView) findViewById(R.id.listview_product);
        txtcount = (TextView) findViewById(R.id.countallcomment);


        LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ftView = li.inflate(R.layout.footer_view, null);
        mHandler = new MyHandler();
        mProductList = new ArrayList<>();
        //Add sample data for list
        //We can get data from DB, webservice here
        //call api
        //COUNT ALL

        CountAllRatingsOfParkingLotRequest countAllRatingsOfParkingLotRequest = CountAllRatingsOfParkingLotRequest
                .newBuilder()
                .setParkingLotId(idplacedetail)
                .build();

        callApiWithExceptionHandling(() -> {
            countallrating = bookingServiceBlockingStub.countAllRatingsOfParkingLot(countAllRatingsOfParkingLotRequest).getValue();
        });


        GetAllRatingsOfParkingLotRequest getAllRatingsOfParkingLotRequest = GetAllRatingsOfParkingLotRequest
                .newBuilder()
                .setParkingLotId(idplacedetail)
                .setNRow(10)
                .setPageNumber(pagenumber)
                .build();

        callApiWithExceptionHandling(() -> {
            getallrating = bookingServiceBlockingStub.getAllRatingsOfParkingLot(getAllRatingsOfParkingLotRequest).getRatingList();
        });

        //
        for (BookingRating parkinglotrating : getallrating) {
            mProductList.add(new Product(parkinglotrating.getBookingId(), parkinglotrating.getUsername(), parkinglotrating.getRating(), parkinglotrating.getComment(), parkinglotrating.getLastUpdated()));
        }
        //

        //Init adapter
        adapter = new ProductListAdapter(getApplicationContext(), mProductList);
        lvProduct.setAdapter(adapter);
        if (countallrating != 0) {
            if (countallrating < 10) {
                txtcount.setText(String.valueOf((countallrating) + "/" + String.valueOf(countallrating)));
            }
            txtcount.setText(String.valueOf((pagenumber * 10) + "/" + String.valueOf(countallrating)));
        } else {
            txtcount.setText(String.valueOf((0) + "/" + String.valueOf(countallrating)));
        }

        //Load Image Parkinglot
        processParkingLot();

        lvProduct.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Do something
                //Ex: display msg with product id get from view.getTag
//                Toast.makeText(getApplicationContext(), "Clicked product id =" + view.getTag(), Toast.LENGTH_SHORT).show();
            }
        });
        lvProduct.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                //Check when scroll to last item in listview, in this tut, init data in listview = 10 item
                Log.d("BachMap", pagenumber + "");
                if (view.getLastVisiblePosition() == totalItemCount - 1 && lvProduct.getCount() >= 10 && isLoading == false && ((pagenumber * 10) < countallrating)) {
                    isLoading = true;
                    Thread thread = new ThreadGetMoreData();
                    //Start thread
                    thread.start();
                }

            }
        });
    }

    private void processParkingLot() {
        if (parkingLot != null) {
            assignParkingLotFields();
            loadFormData();
        }
    }


    private void assignParkingLotFields() {

        ParkingLotInformation information = parkingLot.getInformation();
        imageData = information.getImageData().toByteArray();
    }

    @SuppressLint("SetTextI18n")
    private void loadFormData() {

        if (imageData.length != 0) { // co hinh trong db --> load hinh moi
            Bitmap imageParkingLot = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
            imageView.setImageBitmap(imageParkingLot);
        }

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
                    adapter.addListItemToAdapter((ArrayList<Product>) msg.obj);
                    //Remove loading view after update listview
                    lvProduct.removeFooterView(ftView);
                    isLoading = false;
                    break;
                default:
                    break;
            }
        }
    }

    private ArrayList<Product> getMoreData() {

        ArrayList<Product> lst = new ArrayList<>();
        pagenumber = pagenumber + 1;
        GetAllRatingsOfParkingLotRequest getAllRatingsOfParkingLotRequest = GetAllRatingsOfParkingLotRequest
                .newBuilder()
                .setParkingLotId(idplacedetail)
                .setNRow(10)
                .setPageNumber(pagenumber)
                .build();

        callApiWithExceptionHandling(() -> {
            getallratingmore = bookingServiceBlockingStub
                    .getAllRatingsOfParkingLot(getAllRatingsOfParkingLotRequest)
                    .getRatingList();
        });

        for (BookingRating parkinglotrating : Objects.requireNonNull(getallratingmore)) {
            lst.add(new Product(parkinglotrating.getBookingId(), parkinglotrating.getUsername(), parkinglotrating.getRating(), parkinglotrating.getComment(), parkinglotrating.getLastUpdated()));
        }

        //Sample code get new data :P
        return lst;
    }

    public class ThreadGetMoreData extends Thread {
        @Override
        public void run() {
            //Add footer view after get data
            mHandler.sendEmptyMessage(0);
            //Search more data
            ArrayList<Product> lstResult = getMoreData();
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
                    if ((pagenumber * 10) > countallrating) {
                        txtcount.setText(String.valueOf((countallrating) + "/" + String.valueOf(countallrating)));
                    } else {
                        txtcount.setText(String.valueOf((pagenumber * 10) + "/" + String.valueOf(countallrating)));
                    }

                }
            });
        }
    }
}