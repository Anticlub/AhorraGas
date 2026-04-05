package com.example.ahorragas.detail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ahorragas.R;
import com.example.ahorragas.model.PromotionPlan;
import com.example.ahorragas.ui.PromotionsViewModel;

import java.util.List;

/**
 * Fragment que muestra la lista de planes de promoción de combustible
 * descargados del Geoportal de Gasolineras del Ministerio.
 */
public class PromotionsFragment extends Fragment {

    private static final String ARG_BRAND = "arg_brand";

    /**
     * Crea una nueva instancia del fragment filtrando por la marca de la gasolinera.
     *
     * @param brand Marca de la gasolinera (ej: "SHELL", "BP").
     * @return Nueva instancia de PromotionsFragment.
     */
    public static PromotionsFragment newInstance(String brand) {
        PromotionsFragment fragment = new PromotionsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BRAND, brand != null ? brand : "");
        fragment.setArguments(args);
        return fragment;
    }

    private PromotionsViewModel viewModel;

    private RecyclerView rvPromotions;
    private LinearLayout layoutLoading;
    private LinearLayout layoutError;
    private TextView tvErrorMessage;
    private Button btnRetry;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_promotions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupViewModel();
    }

    private String getBrand() {
        return getArguments() != null ? getArguments().getString(ARG_BRAND, "") : "";
    }

    private void initViews(View view) {
        rvPromotions  = view.findViewById(R.id.rvPromotions);
        layoutLoading = view.findViewById(R.id.layoutLoading);
        layoutError   = view.findViewById(R.id.layoutError);
        tvErrorMessage = view.findViewById(R.id.tvErrorMessage);
        btnRetry      = view.findViewById(R.id.btnRetry);

        rvPromotions.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(PromotionsViewModel.class);

        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case LOADING:
                    showLoading();
                    break;
                case SUCCESS:
                    showList();
                    break;
                case EMPTY:
                    showError(getString(R.string.promo_sin_resultados));
                    break;
                case ERROR:
                    showError(getString(R.string.promo_error_red));
                    break;
            }
        });

        viewModel.getPromotions().observe(getViewLifecycleOwner(), plans -> {
            List<PromotionPlan> filtered = viewModel.filterByBrand(plans, getBrand());
            viewModel.onPromotionsLoaded(filtered);
            if (filtered != null && !filtered.isEmpty()) {
                rvPromotions.setAdapter(new PromotionAdapter(filtered));
            }
        });

        btnRetry.setOnClickListener(v -> retryLoad());
    }

    private void retryLoad() {
        viewModel.getPromotions().removeObservers(getViewLifecycleOwner());
        setupViewModel();
    }

    private void showLoading() {
        layoutLoading.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);
        rvPromotions.setVisibility(View.GONE);
    }

    private void showList() {
        layoutLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        rvPromotions.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        layoutLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        rvPromotions.setVisibility(View.GONE);
        tvErrorMessage.setText(message);
    }
}