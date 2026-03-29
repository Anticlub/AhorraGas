package com.example.ahorragas.detail;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.ahorragas.R;
import com.example.ahorragas.model.Gasolinera;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import android.widget.TextView;

public class StationDetailActivity extends AppCompatActivity {

    public static final String EXTRA_GASOLINERA_ID       = "extra_gasolinera_id";
    public static final String EXTRA_GASOLINERA_MARCA    = "extra_gasolinera_marca";
    public static final String EXTRA_GASOLINERA_DIRECCION = "extra_gasolinera_direccion";
    public static final String EXTRA_GASOLINERA_MUNICIPIO = "extra_gasolinera_municipio";
    public static final String EXTRA_GASOLINERA_LAT      = "extra_gasolinera_lat";
    public static final String EXTRA_GASOLINERA_LON      = "extra_gasolinera_lon";
    public static final String EXTRA_GASOLINERA_HORARIO  = "extra_gasolinera_horario";

    private static final String[] TAB_TITLES = {"General", "Ubicación", "Precios", "Histórico", "Promociones"};

    private Gasolinera gasolinera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_detail);

        gasolinera = extractGasolineraFromIntent();
        if (gasolinera == null) {
            finish();
            return;
        }

        setupHeader();
        setupTabs();
    }

    /**
     * Reconstruye el objeto Gasolinera a partir de los extras del Intent.
     * Se usan extras primitivos en lugar de Parcelable para evitar dependencias.
     *
     * @return Gasolinera reconstruida o null si faltan datos esenciales.
     */
    private Gasolinera extractGasolineraFromIntent() {
        Bundle extras = getIntent().getExtras();
        if (extras == null) return null;

        int id = extras.getInt(EXTRA_GASOLINERA_ID, -1);
        if (id == -1) return null;

        String marca     = extras.getString(EXTRA_GASOLINERA_MARCA, "");
        String direccion = extras.getString(EXTRA_GASOLINERA_DIRECCION, "");
        String municipio = extras.getString(EXTRA_GASOLINERA_MUNICIPIO, "");
        double lat       = extras.getDouble(EXTRA_GASOLINERA_LAT, 0.0);
        double lon       = extras.getDouble(EXTRA_GASOLINERA_LON, 0.0);
        String horario   = extras.getString(EXTRA_GASOLINERA_HORARIO, "");

        Gasolinera g = new Gasolinera(id, marca, municipio, direccion, lat, lon, null);
        g.setHorario(horario);
        return g;
    }

    private void setupHeader() {
        TextView tvBrand   = findViewById(R.id.tvDetailBrand);
        TextView tvAddress = findViewById(R.id.tvDetailAddress);

        String marca = gasolinera.getMarca();
        tvBrand.setText(marca == null || marca.trim().isEmpty() ? "Sin marca" : marca);
        tvAddress.setText(gasolinera.getDisplayAddress());
    }

    private void setupTabs() {
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout  = findViewById(R.id.tabLayout);

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @Override
            public int getItemCount() {
                return TAB_TITLES.length;
            }

            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0: return GeneralFragment.newInstance(gasolinera);
                    case 1: return LocationFragment.newInstance(gasolinera);
                    case 2: return PricesFragment.newInstance(gasolinera);
                    case 3: return ComingSoonFragment.newInstance("Histórico");
                    case 4: return ComingSoonFragment.newInstance("Promociones");
                    default: return ComingSoonFragment.newInstance("");
                }
            }
        });

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(TAB_TITLES[position])
        ).attach();
    }
}