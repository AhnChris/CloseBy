package com.example.android.closeby;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.SphericalUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class PlaceContainer implements Parcelable{

    // Custom sort to sort off of distance from current location
    public static class PlaceDistanceComparator implements Comparator<PlaceContainer> {

        @Override
        public int compare(PlaceContainer pc1, PlaceContainer pc2) {
            if (pc1.getDistToCurrentLoc() < pc2.getDistToCurrentLoc())
                return -1;
            if (pc1.getDistToCurrentLoc() > pc2.getDistToCurrentLoc())
                return 1;

            return 0;
        }
    }

    private double mDistToCurrentLoc;
    private String mAddress;
    private String mAttributions;
    private String mID;
    private LatLng mLatLng;
    private Locale mLocale;
    private String mName;
    private String mPhoneNumber;
    private List<Integer> mPlaceTypes;
    private int mPriceLevel;
    private float mRating;
    private LatLngBounds mViewPort;
    private Uri mWebsiteUri;

    public PlaceContainer(Place place, LatLng currentLoc) {
        // Set all PlaceContainer variables
        setAddress((String) place.getAddress());
        setAttributions((String) place.getAttributions());
        setID(place.getId());
        setLatLng(place.getLatLng());
        setLocale(place.getLocale());
        setName((String) place.getName());
        setPhoneNumber((String) place.getPhoneNumber());
        setPlaceTypes(place.getPlaceTypes());
        setPriceLevel(place.getPriceLevel());
        setRating(place.getRating());
        setViewPort(place.getViewport());
        setWebsiteUri(place.getWebsiteUri());

        // Calculate the distance of current location and this place
        mDistToCurrentLoc = SphericalUtil.computeDistanceBetween(currentLoc, mLatLng);
    }

    protected PlaceContainer(Parcel in) {
        mDistToCurrentLoc = in.readDouble();
        mAddress = in.readString();
        mAttributions = in.readString();
        mID = in.readString();
        mLatLng = in.readParcelable(LatLng.class.getClassLoader());
        mName = in.readString();
        mPhoneNumber = in.readString();
        mPriceLevel = in.readInt();
        mRating = in.readFloat();
        mViewPort = in.readParcelable(LatLngBounds.class.getClassLoader());
        mWebsiteUri = in.readParcelable(Uri.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(mDistToCurrentLoc);
        dest.writeString(mAddress);
        dest.writeString(mAttributions);
        dest.writeString(mID);
        dest.writeParcelable(mLatLng, flags);
        dest.writeString(mName);
        dest.writeString(mPhoneNumber);
        dest.writeInt(mPriceLevel);
        dest.writeFloat(mRating);
        dest.writeParcelable(mViewPort, flags);
        dest.writeParcelable(mWebsiteUri, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PlaceContainer> CREATOR = new Creator<PlaceContainer>() {
        @Override
        public PlaceContainer createFromParcel(Parcel in) {
            return new PlaceContainer(in);
        }

        @Override
        public PlaceContainer[] newArray(int size) {
            return new PlaceContainer[size];
        }
    };

    // Getters & Setters
    public void setAddress(String address) {
        mAddress = address;
    }

    public String getAddress() {
        return mAddress;
    }

    public void setAttributions(String attributions) {
        mAttributions = attributions;
    }

    public String getAttributions() {
        return mAttributions;
    }

    public void setID(String id) {
        mID = id;
    }

    public String getID() {
        return mID;
    }

    public void setLatLng(LatLng latLng) {
        mLatLng = latLng;
    }

    public LatLng getLatLng() {
        return mLatLng;
    }

    public void setLocale(Locale locale) {
        mLocale = locale;
    }

    public Locale getLocale() {
        return mLocale;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public void setPhoneNumber(String phoneNumber) {
        mPhoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    public void setPlaceTypes(List<Integer> placeTypes) {
        mPlaceTypes = placeTypes;
    }

    public List<Integer> getPlaceTypes() {
        return mPlaceTypes;
    }

    public void setPriceLevel(int priceLevel) {
        mPriceLevel = priceLevel;
    }

    public int getPriceLevel() {
        return mPriceLevel;
    }

    public void setRating(float rating) {
        mRating = rating;
    }

    public float getRating() {
        return mRating;
    }

    public void setViewPort(LatLngBounds viewPort) {
        mViewPort = viewPort;
    }

    public LatLngBounds getviewPort() {
        return mViewPort;
    }

    public void setWebsiteUri(Uri websiteUri) {
        mWebsiteUri = websiteUri;
    }

    public Uri getWebsiteUri() {
        return mWebsiteUri;
    }

    public double getDistToCurrentLoc() {
        return mDistToCurrentLoc;
    }
}
