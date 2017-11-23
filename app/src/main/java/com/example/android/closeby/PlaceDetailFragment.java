package com.example.android.closeby;


import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Places;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class PlaceDetailFragment extends Fragment {

    private static final String LOG_TAG = PlaceDetailFragment.class.getSimpleName();

    // List of already sorted nearby places
    private List<PlaceContainer> mSortedCurNearbyPlaces;

    // Entry point for Geo Data API
    GeoDataClient mGeoDataClient;

    // Adapter to hook up data to our views
    PlacesAdapter mPlacesAdapter;

    // Max amount of entries on list
    private static final int MAX_NEARBY_LIST_ENTRIES = 3;

    public PlaceDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Instantiate the GeoDataClient
        mGeoDataClient = Places.getGeoDataClient(getContext(), null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save the PlaceContainer items
        outState.putParcelableArrayList(MainActivity.NEARBY_PLACE_KEY
                , (ArrayList<? extends Parcelable>) mSortedCurNearbyPlaces);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_place_detail, container, false);

        // Get the RecyclerView from our rootView
        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Grab the bundle containing our sorted nearby-places list
        Bundle bundle;
        if (savedInstanceState != null) {
            bundle = savedInstanceState;
        } else {
            bundle = getArguments();
        }

        if (bundle != null) {
            mSortedCurNearbyPlaces = bundle.getParcelableArrayList(MainActivity.NEARBY_PLACE_KEY);

            // Resize our list to be <= MAX_NEARBY_LIST_ENTRIES
            while (mSortedCurNearbyPlaces.size() > MAX_NEARBY_LIST_ENTRIES)
                mSortedCurNearbyPlaces.remove(mSortedCurNearbyPlaces.size() - 1);

            // Create the adapter
            mPlacesAdapter = new PlacesAdapter(getActivity(), mGeoDataClient, mSortedCurNearbyPlaces);
        }

        // Set the adapter to use with the RecyclerView
        recyclerView.setAdapter(mPlacesAdapter);

        // Inflate the layout for this fragment
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Hide Toolbar from user
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();

        // Hide the fab from user
        getActivity().findViewById(R.id.fab).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onStop() {
        super.onStop();

        // Show toolbar to user
        ((AppCompatActivity) getActivity()).getSupportActionBar().show();

        // Show fab to user
        getActivity().findViewById(R.id.fab).setVisibility(View.VISIBLE);
    }

    /**
     * Method to set new data if this specific fragment is inflated/exists
     * @param newPlaceData
     */
    protected void setNewData(List<PlaceContainer> newPlaceData) {

        // Clear the current data
        mSortedCurNearbyPlaces.clear();

        // Find the smallest amount of entries to add and add them to our list
        int maxSize = Math.min(newPlaceData.size(), MAX_NEARBY_LIST_ENTRIES);
        for (int index = 0; index < maxSize; index++) {
            mSortedCurNearbyPlaces.add(newPlaceData.get(index));
        }

        // Notify the adapter that we have new data
        mPlacesAdapter.notifyDataSetChanged();
    }
}
