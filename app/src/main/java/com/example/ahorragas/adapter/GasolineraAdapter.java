package com.example.ahorragas.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ahorragas.R;
import com.example.ahorragas.map.BrandLogoProvider;
import com.example.ahorragas.map.MarkerBitmapFactory;
import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.Gasolinera;
import com.example.ahorragas.model.PriceLevel;
import com.example.ahorragas.model.PriceRange;
import com.example.ahorragas.util.DiscountPrefs;
import com.example.ahorragas.util.GasolineraSorter;

import java.util.ArrayList;
import java.util.List;

public class GasolineraAdapter extends RecyclerView.Adapter<GasolineraAdapter.ViewHolder> {

    public interface OnGasolineraClickListener {
        void onGasolineraClick(Gasolinera gasolinera);
    }

    private List<Gasolinera> gasolineras;
    private FuelType currentFuel;
    private PriceRange currentPriceRange;
    private final OnGasolineraClickListener clickListener;

    public GasolineraAdapter(List<Gasolinera> gasolineras,
                             FuelType fuel,
                             OnGasolineraClickListener clickListener) {
        this.gasolineras = new ArrayList<>(gasolineras);
        this.currentFuel = fuel;
        this.currentPriceRange = new PriceRange(null, null, 0);
        this.clickListener = clickListener;
    }

    /**
     * Actualiza los datos mostrados en la lista.
     *
     * @param newGasolineras Lista de gasolineras a mostrar.
     * @param fuel           Tipo de combustible seleccionado.
     * @param priceRange     Rango de precios para calcular el nivel de precio con descuento.
     */
    public void updateData(List<Gasolinera> newGasolineras, FuelType fuel, PriceRange priceRange) {
        this.gasolineras = new ArrayList<>(newGasolineras);
        this.currentFuel = fuel;
        this.currentPriceRange = priceRange != null ? priceRange : new PriceRange(null, null, 0);
        notifyDataSetChanged();
    }

    public int getPositionOf(Gasolinera target) {
        for (int i = 0; i < gasolineras.size(); i++) {
            if (gasolineras.get(i).getId() == target.getId()) return i;
        }
        return -1;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gasolinera, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(gasolineras.get(position), currentFuel, currentPriceRange, clickListener);
    }

    @Override
    public int getItemCount() {
        return gasolineras.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final View vPriceStripe;
        private final ImageView ivBrandLogo;
        private final TextView tvBrandName;
        private final TextView tvAddress;
        private final TextView tvPrice;
        private final TextView tvDistance;

        ViewHolder(View itemView) {
            super(itemView);
            vPriceStripe = itemView.findViewById(R.id.vPriceStripe);
            ivBrandLogo  = itemView.findViewById(R.id.ivBrandLogo);
            tvBrandName  = itemView.findViewById(R.id.tvBrandName);
            tvAddress    = itemView.findViewById(R.id.tvAddress);
            tvPrice      = itemView.findViewById(R.id.tvPrice);
            tvDistance   = itemView.findViewById(R.id.tvDistance);
        }

        void bind(Gasolinera gasolinera,
                  FuelType fuel,
                  PriceRange priceRange,
                  OnGasolineraClickListener clickListener) {
            int logoResId = BrandLogoProvider.getLogoResId(gasolinera.getMarca());
            ivBrandLogo.setImageResource(logoResId);

            String marca = gasolinera.getMarca();
            tvBrandName.setText(marca == null || marca.trim().isEmpty()
                    ? itemView.getContext().getString(R.string.sin_marca)
                    : marca);
            tvAddress.setText(gasolinera.getDisplayAddress());

            if (gasolinera.isElectric()) {
                // Electrolineras: mostrar resumen de conectores en lugar del precio
                String resumen = gasolinera.getResumenConectores();
                tvPrice.setText(resumen != null ? resumen : "Sin datos");
                tvPrice.setTextColor(MarkerBitmapFactory.getElectricColor());
                vPriceStripe.setBackgroundColor(MarkerBitmapFactory.getElectricColor());
            } else {
                tvPrice.setText(gasolinera.getFormattedPrice(fuel));
                int priceColor = MarkerBitmapFactory.getPriceLevelColor(gasolinera.getPriceLevel());
                tvPrice.setTextColor(priceColor);
                vPriceStripe.setBackgroundColor(priceColor);
            }

            String distance = gasolinera.getFormattedDistance();
            tvDistance.setText(distance.isEmpty() ? "" : "\uD83D\uDCCD " + distance);

            itemView.setOnClickListener(v -> clickListener.onGasolineraClick(gasolinera));
        }
    }
}