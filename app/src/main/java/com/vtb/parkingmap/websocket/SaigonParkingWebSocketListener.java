package com.vtb.parkingmap.websocket;


import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.bht.saigonparking.api.grpc.contact.BookingAcceptanceContent;
import com.bht.saigonparking.api.grpc.contact.BookingProcessingContent;
import com.bht.saigonparking.api.grpc.contact.BookingRejectContent;
import com.bht.saigonparking.api.grpc.contact.NotificationContent;
import com.bht.saigonparking.api.grpc.contact.SaigonParkingMessage;
import com.bht.saigonparking.api.grpc.contact.TextMessageContent;
import com.vtb.parkingmap.R;
import com.vtb.parkingmap.SaigonParkingApplication;
import com.vtb.parkingmap.activity.BookingActivity;
import com.vtb.parkingmap.activity.ChatActivity;
import com.vtb.parkingmap.activity.PlaceDetailsActivity;
import com.vtb.parkingmap.base.BaseSaigonParkingActivity;
import com.vtb.parkingmap.database.SaigonParkingDatabaseEntity;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.Serializable;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

@RequiredArgsConstructor
public final class SaigonParkingWebSocketListener extends WebSocketListener {

    private static final int NORMAL_CLOSURE_STATUS = 1000;

    private final SaigonParkingApplication applicationContext;

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {

    }

    @SneakyThrows
    @Override
    public void onMessage(@NotNull WebSocket webSocket, ByteString bytes) {
        SaigonParkingMessage message = SaigonParkingMessage.parseFrom(bytes.toByteArray());
        try {
            BaseSaigonParkingActivity currentActivity = applicationContext.getCurrentActivity();

            switch (message.getType()) {
                case NOTIFICATION:
                    NotificationContent notificationContent = NotificationContent.parseFrom(message.getContent());
                    Log.d("BachMap", "Ket qua:" + notificationContent);

                    break;
                case TEXT_MESSAGE: {
                    applicationContext.getCurrentActivity().runOnUiThread(() -> {
                        try {
                            TextMessageContent textMessageContent = TextMessageContent.parseFrom(message.getContent());
                            Log.d("BachMap", "1" + textMessageContent);
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("name", textMessageContent.getSender());
                            jsonObject.put("message", textMessageContent.getMessage());
                            jsonObject.put("isSent", false);
                            applicationContext.getMessageAdapter().addItem(jsonObject);
                            addNotification(textMessageContent.getSender(), textMessageContent.getMessage());

                        } catch (Exception exception) {
                            Log.d("BachMap", exception.getMessage());
                        }
                    });
                }
                break;
                case BOOKING_ACCEPTANCE:
                    BookingAcceptanceContent bookingAcceptanceContent = BookingAcceptanceContent.parseFrom(message.getContent());

                    if (currentActivity instanceof BookingActivity) {
                        BookingActivity activity = (BookingActivity) currentActivity;
                        SaigonParkingDatabaseEntity bookingEntity = SaigonParkingDatabaseEntity.builder()
                                .id(activity.getId())
                                .latitude(activity.getLatitude())
                                .longitude(activity.getLongitude())
                                .mylat(activity.getMylat())
                                .mylong(activity.getMylong())
                                .position3lat(activity.getPosition3lat())
                                .position3long(activity.getPosition3long())
                                .tmpType(activity.getTmpType())
                                .bookingId(bookingAcceptanceContent.getBookingId())
                                .build();


                        Log.d("BachMap", bookingEntity.toString());
                        applicationContext.getSaigonParkingDatabase().insertBookingTable(bookingEntity);
                    }
                    break;

                case BOOKING_PROCESSING:
                    BookingProcessingContent bookingProcessingContent = BookingProcessingContent.parseFrom(message.getContent());
                    Log.d("BachMap", "Tai ID: " + bookingProcessingContent.getBookingId());

                    /* open Booking Activity + send bookingProcessingContent to Booking Activity */
                    Intent intent = new Intent(applicationContext.getCurrentActivity(), BookingActivity.class);

                    if (currentActivity instanceof PlaceDetailsActivity) {
                        PlaceDetailsActivity activity = (PlaceDetailsActivity) currentActivity;
                        intent.putExtra("parkingLot", activity.getParkingLot());
                        intent.putExtra("placedetaillat", (Serializable) activity.getLatitude());
                        intent.putExtra("placedetaillong", (Serializable) activity.getLongitude());
                        intent.putExtra("mylatfromplacedetail", (Serializable) activity.getMylat());
                        intent.putExtra("mylongfromplacedetail", (Serializable) activity.getMylong());
                        intent.putExtra("placedetailtype", (Serializable) activity.getTmpType());
                        intent.putExtra("idplacedetail", (Serializable) activity.getId());

                        if (activity.getPosition3lat() != 1234) {
                            intent.putExtra("position3lat", (Serializable) activity.getPosition3lat());
                            intent.putExtra("position3long", (Serializable) activity.getPosition3long());
                        }
                    }

                    intent.putExtra("bookingProcessingContent", bookingProcessingContent);
                    applicationContext.getCurrentActivity().startActivity(intent);
                    applicationContext.getCurrentActivity().overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                    break;

                case BOOKING_REJECT:
                    BookingRejectContent bookingRejectContent = BookingRejectContent.parseFrom(message.getContent());
                    Log.d("BachMap", "1 : BOOKING REJ" + bookingRejectContent);
                    String rejectNotification = "Parking full slot! Please choose other parking lots!";
                    Toast.makeText(applicationContext.getCurrentActivity(), rejectNotification, Toast.LENGTH_SHORT).show();
                    applicationContext.setIsBooked(false);
                    break;

                case IMAGE:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onClosing(WebSocket webSocket, int code, @NotNull String reason) {
        webSocket.close(NORMAL_CLOSURE_STATUS, null);
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, Throwable t, Response response) {
        Log.d("BachMap", t.getMessage());
    }

    private void addNotification(String name, String message) {

        // Builds your notification
        ComponentName componentName;
        ActivityManager activityManager = (ActivityManager) applicationContext.getSystemService(Context.ACTIVITY_SERVICE);

        //noinspection deprecation
        componentName = activityManager.getRunningTasks(1).get(0).topActivity;
        String tmp = "com.vtb.parkingmap.activity.ChatActivity";

        if (!tmp.equals(componentName.getShortClassName())) {

            Intent notificationIntent = new Intent(applicationContext.getCurrentActivity(), ChatActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(applicationContext.getCurrentActivity(),
                    0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(applicationContext.getCurrentActivity(),
                    "ID_Notification")
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(name)
                    .setContentText(message)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setVibrate(new long[5])
                    .setAutoCancel(true)
                    .setContentIntent(contentIntent);

            // Add as notification
            NotificationManager manager = (NotificationManager) applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(0, builder.build());
        }
    }
}