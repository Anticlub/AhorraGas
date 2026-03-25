package com.example.ahorragas.adapter;

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

import java.util.ArrayList;
import java.util.List;

public class GasolineraAdapter extends RecyclerView.Adapter<GasolineraAdapter.ViewHolder> {

    public interface OnGasolineraClickListener {
        void onGasolineraClick(Gasolinera gasolinera);
    }

    private List<Gasolinera> gasolineras;
    private FuelType currentFuel;
    private final OnGasolineraClickListener clickListener;

    public GasolineraAdapter(List<Gasolinera> gasolineras,
                             FuelType fuel,
                             OnGasolineraClickListener clickListener) {
        this.gasolineras = new ArrayList<>(gasolineras);
        this.currentFuel = fuel;
        this.clickListener = clickListener;
    }

    public void updateData(List<Gasolinera> newGasolineras, FuelType fuel) {
        this.gasolineras = new ArrayList<>(newGasolineras);
        this.currentFuel = fuel;
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
        holder.bind(gasolineras.get(position), currentFuel, clickListener);
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
            ivBrandLogo = itemView.findViewById(R.id.ivBrandLogo);
            tvBrandName = itemView.findViewById(R.id.tvBrandName);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDistance = itemView.findViewById(R.id.tvDistance);
        }

        void bind(Gasolinera gasolinera,
                  FuelType fuel,
                  OnGasolineraClickListener clickListener) {
            int logoResId = BrandLogoProvider.getLogoResId(gasolinera.getMarca());
            ivBrandLogo.setImageResource(logoResId);

            tvBrandName.setText(gasolinera.getMarca() == null || gasolinera.getMarca().trim().isEmpty()
                    ? "Sin marca"
                    : gasolinera.getMarca());
            tvAddress.setText(gasolinera.getDisplayAddress());

            tvPrice.setText(gasolinera.getFormattedPrice(fuel));
            int priceColor = MarkerBitmapFactory.getPriceLevelColor(gasolinera.getPriceLevel());
            tvPrice.setTextColor(priceColor);
            vPriceStripe.setBackgroundColor(priceColor);

            String distance = gasolinera.getFormattedDistance();
            tvDistance.setText(distance.isEmpty() ? "" : "\uD83D\uDCCD " + distance);

            itemView.setOnClickListener(v -> clickListener.onGasolineraClick(gasolinera));
        }
    }
}
