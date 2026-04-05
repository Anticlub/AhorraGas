package com.example.ahorragas.detail;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.ahorragas.R;
import com.example.ahorragas.map.BrandLogoProvider;
import com.example.ahorragas.model.Gasolinera;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import android.widget.ImageView;
import android.widget.TextView;

public class StationDetailActivity extends AppCompatActivity {

    private static final String[] TAB_TITLES = {"General", "Ubicación", "Precios", "Histórico", "Promociones"};    public static final String EXTRA_GASOLINERA = "extra_gasolinera";
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
        return getIntent().getParcelableExtra(EXTRA_GASOLINERA);
    }

    private void setupHeader() {
        TextView tvBrand   = findViewById(R.id.tvDetailBrand);
        TextView tvAddress = findViewById(R.id.tvDetailAddress);

        String marca = gasolinera.getMarca();
        tvBrand.setText(marca == null || marca.trim().isEmpty() ? getString(R.string.sin_marca) : marca);        tvAddress.setText(gasolinera.getDisplayAddress());
        ImageView ivLogo = findViewById(R.id.ivHeaderLogo);
        ivLogo.setImageResource(BrandLogoProvider.getLogoResId(gasolinera.getMarca()));
    }

    private void setupTabs() {
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout  = findViewById(R.id.tabLayout);

        String preciosTab = gasolinera.isElectric() ? "Cargadores" : "Precios";
        String[] tabTitles = {"General", "Ubicación", preciosTab, "Histórico", "Promociones"};

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @Override
            public int getItemCount() { return tabTitles.length; }

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
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();
    }
}