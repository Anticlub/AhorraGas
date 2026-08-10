package com.ahorragas.app.detail;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.ahorragas.app.BaseActivity;
import com.ahorragas.app.PriceAlertWorker;
import com.ahorragas.app.R;
import com.ahorragas.app.map.BrandLogoProvider;
import com.ahorragas.app.model.FuelType;
import com.ahorragas.app.model.Gasolinera;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import android.widget.ImageView;
import android.widget.TextView;

public class StationDetailActivity extends BaseActivity {

    public static final String EXTRA_GASOLINERA = "extra_gasolinera";
    private Gasolinera gasolinera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_detail);
        applySystemBarInsets(R.id.headerLayout, 0);

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
     *
     * @return Gasolinera reconstruida o null si faltan datos esenciales.
     */
    private Gasolinera extractGasolineraFromIntent() {
        return getIntent().getParcelableExtra(EXTRA_GASOLINERA);
    }

    /**
     * Devuelve el FuelType de la alerta que originó esta apertura, o null
     * si la pantalla se abrió de forma normal (no desde una notificación).
     *
     * @return FuelType de la alerta o null.
     */
    public FuelType getAlertFuelType() {
        String fuelName = getIntent().getStringExtra(PriceAlertWorker.EXTRA_ALERT_FUEL);
        if (fuelName == null) return null;
        return FuelType.fromString(fuelName);
    }

    private void setupHeader() {
        TextView tvBrand   = findViewById(R.id.tvDetailBrand);
        TextView tvAddress = findViewById(R.id.tvDetailAddress);

        String marca = gasolinera.getMarca();
        tvBrand.setText(marca == null || marca.trim().isEmpty()
                ? getString(R.string.sin_marca) : marca);
        tvAddress.setText(gasolinera.getDisplayAddress());
        ImageView ivLogo = findViewById(R.id.ivHeaderLogo);
        ivLogo.setImageResource(BrandLogoProvider.getLogoResId(gasolinera.getMarca()));
    }

    private void setupTabs() {
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout  = findViewById(R.id.tabLayout);

        String preciosTab = gasolinera.isElectric()
                ? getString(R.string.tab_chargers)
                : getString(R.string.tab_prices);

        String[] tabTitles = {
                getString(R.string.tab_general),
                getString(R.string.tab_location),
                preciosTab,
                getString(R.string.tab_history),
                getString(R.string.tab_promotions),
                getString(R.string.tab_reviews)
        };

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @Override
            public int getItemCount() { return tabTitles.length; }

            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0: return GeneralFragment.newInstance(gasolinera, getAlertFuelType());
                    case 1: return LocationFragment.newInstance(gasolinera);
                    case 2: return PricesFragment.newInstance(gasolinera);
                    case 4: return PromotionsFragment.newInstance(gasolinera.getMarca());
                    case 5: return ReviewsFragment.newInstance(gasolinera);
                    default: return HistoryFragment.newInstance(gasolinera.getId(), gasolinera.getMarca());
                }
            }
        });

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();
    }
}
