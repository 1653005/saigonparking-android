package com.vtb.parkingmap.communication;

import android.content.Context;

import com.bht.saigonparking.api.grpc.auth.AuthServiceGrpc;
import com.bht.saigonparking.api.grpc.booking.BookingServiceGrpc;
import com.bht.saigonparking.api.grpc.contact.ContactServiceGrpc;
import com.bht.saigonparking.api.grpc.parkinglot.ParkingLotServiceGrpc;
import com.bht.saigonparking.api.grpc.user.UserServiceGrpc;

import io.grpc.ManagedChannel;
import lombok.AccessLevel;
import lombok.Getter;

/**
 *
 * All service stubs of saigonparking system
 * ManagedChannel is private used only, cannot be accessed from outside !
 * <p>
 * Please be aware of 2 types of stub:
 * + stub:         asynchronous stub
 * + blockingStub:  synchronous stub
 *
 * @author bht
 */
@Getter
public final class SaigonParkingServiceStubs {

    @Getter(AccessLevel.NONE)
    private ManagedChannel saigonParkingChannel;

    private AuthServiceGrpc.AuthServiceStub authServiceStub;
    private AuthServiceGrpc.AuthServiceBlockingStub authServiceBlockingStub;
    private UserServiceGrpc.UserServiceStub userServiceStub;
    private UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;
    private ParkingLotServiceGrpc.ParkingLotServiceStub parkingLotServiceStub;
    private ParkingLotServiceGrpc.ParkingLotServiceBlockingStub parkingLotServiceBlockingStub;
    private ContactServiceGrpc.ContactServiceStub contactServiceStub;
    private ContactServiceGrpc.ContactServiceBlockingStub contactServiceBlockingStub;
    private BookingServiceGrpc.BookingServiceStub bookingServiceStub;
    private BookingServiceGrpc.BookingServiceBlockingStub bookingServiceBlockingStub;

    public SaigonParkingServiceStubs(Context applicationContext) {
        saigonParkingChannel = new SaigonParkingChannelConfiguration(applicationContext).getManagedChannel();
        initAllSaigonParkingServiceStubs();
    }

    private void initAllSaigonParkingServiceStubs() {
        authServiceStub = AuthServiceGrpc.newStub(saigonParkingChannel);
        authServiceBlockingStub = AuthServiceGrpc.newBlockingStub(saigonParkingChannel);
        userServiceStub = UserServiceGrpc.newStub(saigonParkingChannel);
        userServiceBlockingStub = UserServiceGrpc.newBlockingStub(saigonParkingChannel);
        parkingLotServiceStub = ParkingLotServiceGrpc.newStub(saigonParkingChannel);
        parkingLotServiceBlockingStub = ParkingLotServiceGrpc.newBlockingStub(saigonParkingChannel);
        contactServiceStub = ContactServiceGrpc.newStub(saigonParkingChannel);
        contactServiceBlockingStub = ContactServiceGrpc.newBlockingStub(saigonParkingChannel);
        bookingServiceStub = BookingServiceGrpc.newStub(saigonParkingChannel);
        bookingServiceBlockingStub = BookingServiceGrpc.newBlockingStub(saigonParkingChannel);
    }
}