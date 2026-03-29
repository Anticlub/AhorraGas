package com.example.ahorragas.detail;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

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
        String marca = args.getString(ARG_MARCA, "Gasolinera");

        // Mini mapa
        mapView = view.findViewById(R.id.miniMapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);

        GeoPoint point = new GeoPoint(lat, lon);
        mapView.getController().setZoom(16.0);
        mapView.getController().setCenter(point);

        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(marca);
        mapView.getOverlays().add(marker);
        mapView.invalidate();

        // Botón navegar
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
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }
}