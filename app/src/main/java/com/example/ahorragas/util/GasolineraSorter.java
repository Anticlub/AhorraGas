package com.example.ahorragas.util;

import com.example.ahorragas.model.Gasolinera;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class GasolineraSorter {

    private GasolineraSorter() {}

    /**
     * Filtra gasolineras con coords inválidas, calcula distanceMeters y ordena por cercanía.
     */
    public static List<Gasolinera> filterComputeAndSort(List<Gasolinera> gasolineras,
                                                        double userLat, double userLon) {

        List<Gasolinera> result = new ArrayList<>();
        if (gasolineras == null) return result;

        for (Gasolinera g : gasolineras) {
            if (!GeoValidation.isValidLatLon(g.getLat(), g.getLon())) continue;

            double d = GeoUtils.distanceMeters(userLat, userLon, g.getLat(), g.getLon());
            g.setDistanceMeters(d);
            result.add(g);
        }

        result.sort(Comparator.comparingDouble(g ->
                g.getDistanceMeters() == null ? Double.MAX_VALUE : g.getDistanceMeters()
        ));

        return result;
    }
}