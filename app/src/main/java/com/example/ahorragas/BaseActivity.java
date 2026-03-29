package com.example.ahorragas;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ahorragas.model.Gasolinera;

public abstract class BaseActivity extends AppCompatActivity {

    protected void navigateToMap() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    protected void navigateToPreferences() {
        startActivity(new Intent(this, PreferencesActivity.class));
    }

    protected void navigateToDistanceList() {
        Intent intent = new Intent(this, DistanceListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }
    protected void navigateToPrice() {
        Intent intent = new Intent(this, PriceListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    protected void navigateToDetail(Gasolinera gasolinera) {
        Intent intent = new Intent(this, com.example.ahorragas.detail.StationDetailActivity.class);
        intent.putExtra(com.example.ahorragas.detail.StationDetailActivity.EXTRA_GASOLINERA_ID, gasolinera.getId());
        intent.putExtra(com.example.ahorragas.detail.StationDetailActivity.EXTRA_GASOLINERA_MARCA, gasolinera.getMarca());
        intent.putExtra(com.example.ahorragas.detail.StationDetailActivity.EXTRA_GASOLINERA_DIRECCION, gasolinera.getDireccion());
        intent.putExtra(com.example.ahorragas.detail.StationDetailActivity.EXTRA_GASOLINERA_MUNICIPIO, gasolinera.getMunicipio());
        intent.putExtra(com.example.ahorragas.detail.StationDetailActivity.EXTRA_GASOLINERA_LAT, gasolinera.getLat() != null ? gasolinera.getLat() : 0.0);
        intent.putExtra(com.example.ahorragas.detail.StationDetailActivity.EXTRA_GASOLINERA_LON, gasolinera.getLon() != null ? gasolinera.getLon() : 0.0);
        intent.putExtra(com.example.ahorragas.detail.StationDetailActivity.EXTRA_GASOLINERA_HORARIO, gasolinera.getHorario());
        for (com.example.ahorragas.model.FuelType fuel : com.example.ahorragas.model.FuelType.values()) {
            Double price = gasolinera.getPrecio(fuel);
            if (price != null) {
                intent.putExtra(
                        com.example.ahorragas.detail.StationDetailActivity.EXTRA_PRICES_PREFIX + fuel.name(),
                        price
                );
            }
        }
        if (gasolinera.getDistanceMeters() != null) {
            intent.putExtra(
                    com.example.ahorragas.detail.StationDetailActivity.EXTRA_DISTANCE,
                    gasolinera.getDistanceMeters()
            );
        }
        startActivity(intent);
    }
}