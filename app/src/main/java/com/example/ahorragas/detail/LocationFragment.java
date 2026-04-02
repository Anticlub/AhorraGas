package com.example.ahorragas.detail;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ahorragas.R;
import com.example.ahorragas.model.Gasolinera;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import androidx.preference.PreferenceManager;

public class LocationFragment extends Fragment {

    private static final String ARG_ID        = "arg_id";
    private static final String ARG_MARCA     = "arg_marca";
    private static final String ARG_DIRECCION = "arg_direccion";
    private static final String ARG_MUNICIPIO = "arg_municipio";
    private static final String ARG_LAT       = "arg_lat";
    private static final String ARG_LON       = "arg_lon";
    private static final String ARG_HORARIO   = "arg_horario";

    private MapView mapView;
    private FrameLayout mapContainer;
    private GeoPoint stationPoint;
    private String stationMarca;

    /**
     * Crea una nueva instancia del fragment con los datos de la gasolinera.
     *
     * @param gasolinera Gasolinera cuya ubicación se mostrará en el mapa.
     * @return Nueva instancia de LocationFragment.
     */
    public static LocationFragment newInstance(Gasolinera gasolinera) {
        LocationFragment fragment = new LocationFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ID, gasolinera.getId());
        args.putString(ARG_MARCA, gasolinera.getMarca());
        args.putString(ARG_DIRECCION, gasolinera.getDireccion());
        args.putString(ARG_MUNICIPIO, gasolinera.getMunicipio());
        args.putDouble(ARG_LAT, gasolinera.getLat() != null ? gasolinera.getLat() : 0.0);
        args.putDouble(ARG_LON, gasolinera.getLon() != null ? gasolinera.getLon() : 0.0);
        args.putString(ARG_HORARIO, gasolinera.getHorario());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Configuration.getInstance().load(
                requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext())
        );
        return inflater.inflate(R.layout.fragment_location, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) return;

        double lat = args.getDouble(ARG_LAT);
        double lon = args.getDouble(ARG_LON);
        stationMarca = args.getString(ARG_MARCA, "Gasolinera");
        stationPoint = new GeoPoint(lat, lon);

        mapContainer = view.findViewById(R.id.mapContainer);

        Button btnNavigate = view.findViewById(R.id.btnNavigate);
        btnNavigate.setOnClickListener(v -> {
            Uri geoUri = Uri.parse("geo:" + lat + "," + lon + "?q=" + lat + "," + lon);
            Intent intent = new Intent(Intent.ACTION_VIEW, geoUri);
            startActivity(Intent.createChooser(intent, "Navegar con..."));
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        buildAndAttachMap();
    }

    @Override
    public void onPause() {
        super.onPause();
        destroyMap();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        destroyMap();
    }

    /** Crea un MapView nuevo, lo configura y lo añade al contenedor. */
    private void buildAndAttachMap() {
        if (mapContainer == null || stationPoint == null) return;

        destroyMap();

        mapView = new MapView(requireContext());
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);
        mapView.getController().setZoom(16.0);
        mapView.getController().setCenter(stationPoint);

        Marker marker = new Marker(mapView);
        marker.setPosition(stationPoint);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(stationMarca);
        mapView.getOverlays().add(marker);

        mapContainer.removeAllViews();
        mapContainer.addView(mapView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        mapView.invalidate();
    }

    /** Destruye el MapView actual y lo elimina del contenedor. */
    private void destroyMap() {
        if (mapView != null) {
            mapView.onDetach();
            if (mapContainer != null) mapContainer.removeAllViews();
            mapView = null;
        }
    }
}