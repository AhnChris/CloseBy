package com.example.android.closeby;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlacePhotoMetadata;
import com.google.android.gms.location.places.PlacePhotoMetadataBuffer;
import com.google.android.gms.location.places.PlacePhotoMetadataResponse;
import com.google.android.gms.location.places.PlacePhotoResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.List;

public class PlacesAdapter extends RecyclerView.Adapter<PlacesAdapter.ViewHolder> {

    private List<PlaceContainer> mSortedCurNearbyPlaces;
    private Context mContext;

    private GeoDataClient mGeoDataClient;

    /**
     * Constructor that passes in the sorted list of nearby places and context
     *
     * @param context
     * @param geoDataClient
     * @param sortedCurNearbyPlaces
     */
    public PlacesAdapter(Context context, GeoDataClient geoDataClient
            , List<PlaceContainer> sortedCurNearbyPlaces) {

        mSortedCurNearbyPlaces = sortedCurNearbyPlaces;
        mContext = context;
        mGeoDataClient = geoDataClient;
    }

    /**
     * Required method for creating the viewholder objects
     *
     * @param parent   The ViewGroup in which the new view will be added after bounded by a position
     * @param viewType View type of the new View
     * @return The newly created ViewHolder
     */
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext)
                .inflate(R.layout.list_item, parent, false));
    }

    /**
     * Required method that binds the data to the ViewHolder
     *
     * @param holder   The ViewHolder into which the data should be put
     * @param position The adapter position
     */
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        // Get the current nearby place using the adapter position
        PlaceContainer curPlace = mSortedCurNearbyPlaces.get(position);

        // Populate the necessary views with the supplied data
        holder.bindTo(curPlace);
    }

    /**
     * Required method to determine the size of the data set
     *
     * @return the size of the data set
     */
    @Override
    public int getItemCount() {
        return mSortedCurNearbyPlaces.size();
    }

    /**
     * ViewHolder class that represents each row of data in the RecyclerView
     */

    public class ViewHolder extends RecyclerView.ViewHolder {
        // Client for getting out bitmap photo
//        private GeoDataClient mGeoDataClient;

        // The views to populate
        private TextView mNameText;
        private TextView mPhoneNumberText;
        private ImageView mPlaceImage;
        private RatingBar mRatingBar;
        private TextView mDistanceText;
        private TextView mWebsiteText;

        /**
         * The constructor for ViewHolder, used in onCreateViewHolder().
         *
         * @param itemView The rootview of the list_item.xml layout file
         */
        public ViewHolder(View itemView) {
            super(itemView);

            // Initialize the views
            mPlaceImage = (ImageView) itemView.findViewById(R.id.placeImage);
            mNameText = (TextView) itemView.findViewById(R.id.placeName);
            mPhoneNumberText = (TextView) itemView.findViewById(R.id.placePhoneNumber);
            mRatingBar = (RatingBar) itemView.findViewById(R.id.placeRating);
            mDistanceText = (TextView) itemView.findViewById(R.id.placeDistance);
            mWebsiteText = (TextView) itemView.findViewById(R.id.placeWebsite);
        }

        /**
         * Method for binding the data to the views
         *
         * @param curPlace - Current nearby place
         */
        public void bindTo(PlaceContainer curPlace) {
            // Run the tasks to grab the bitmap of photo
            getPhoto(curPlace);

            // Populate the views with the curPlace data
            mNameText.setText((curPlace.getName().isEmpty() || curPlace.getName() == null)
                    ? mContext.getString(R.string.name_not_available_text) : curPlace.getName());

            mPhoneNumberText.setText((curPlace.getPhoneNumber().isEmpty()
                    || curPlace.getPhoneNumber() == null)
                    ? mContext.getString(R.string.phone_number_unavailable_text) : curPlace.getPhoneNumber());

            mRatingBar.setRating(curPlace.getRating() < 0 ? 0 : curPlace.getRating());

            String distanceString = Math.round(curPlace.getDistToCurrentLoc()) + " m";
            mDistanceText.setText(distanceString);

            mWebsiteText.setText(
                    (curPlace.getWebsiteUri().toString().isEmpty() 
                            || curPlace.getWebsiteUri() == null) 
                            ? mContext.getString(R.string.website_unavailable_text) : curPlace.getWebsiteUri().toString());
        }

        /**
         * Method for obtaining the bitmap for the photo
         *
         * @param curPlace - The current nearby place
         */
        private void getPhoto(PlaceContainer curPlace) {
            // Check if the placeID is null
            if (curPlace.getID() != null && ((MainActivity) mContext).isNetworkAvailable()) {

                // Run the task to get the PlacePhotoMetadataResponse
                final Task<PlacePhotoMetadataResponse> photoMetadataResponse
                        = mGeoDataClient.getPlacePhotos(curPlace.getID());
                // Add a listener for when the task is done
                photoMetadataResponse.addOnCompleteListener(new OnCompleteListener<PlacePhotoMetadataResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<PlacePhotoMetadataResponse> task) {
                        // Grab the list of photos
                        PlacePhotoMetadataResponse photoMetadataResponse = task.getResult();

                        // Get the PlacePhotoMetadataBuffer (metadata for all of the photos)
                        PlacePhotoMetadataBuffer photoMetadataBuffer = photoMetadataResponse.getPhotoMetadata();

                        // Get the first photo in the list
                        PlacePhotoMetadata photoMetadata = photoMetadataBuffer.get(0);

                        // Get the attribution text
                        CharSequence attribution = photoMetadata.getAttributions();

                        // Now get the full-size bitmap from the photoMetadata
                        Task<PlacePhotoResponse> photoResponse = mGeoDataClient.getPhoto(photoMetadata);
                        // Add a listener for when the task is done
                        photoResponse.addOnCompleteListener(new OnCompleteListener<PlacePhotoResponse>() {
                            @Override
                            public void onComplete(@NonNull Task<PlacePhotoResponse> task) {
                                PlacePhotoResponse photo = task.getResult();
                                Bitmap bitmap = photo.getBitmap();

                                // Set the ImageView with this bitmap if it exists
                                if (bitmap != null) {
                                    mPlaceImage.setImageBitmap(bitmap);
                                }
                            }
                        });

                    }
                });
            } else {
                // Set the image to some error image
                Glide.with(mContext).load(R.drawable.error_image).into(mPlaceImage);
            }
        }
    }
}
