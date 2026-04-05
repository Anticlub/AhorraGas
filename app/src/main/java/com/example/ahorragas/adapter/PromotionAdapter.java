package com.example.ahorragas.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ahorragas.R;
import com.example.ahorragas.model.PromotionPlan;

import java.util.List;
import java.util.Locale;

/**
 * Adapter para la lista de planes de promoción en el Fragment de promociones.
 * Cada item muestra el nombre del plan, operador, descripción colapsable,
 * destinatario y cifra de descuento.
 */
public class PromotionAdapter extends RecyclerView.Adapter<PromotionAdapter.ViewHolder> {

    private final List<PromotionPlan> plans;

    /**
     * @param plans Lista de planes de promoción a mostrar.
     */
    public PromotionAdapter(List<PromotionPlan> plans) {
        this.plans = plans;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_promotion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(plans.get(position));
    }

    @Override
    public int getItemCount() {
        return plans.size();
    }

    // ─── ViewHolder ──────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvPlanName;
        private final TextView tvOperator;
        private final TextView tvDescription;
        private final TextView tvToggleDescription;
        private final TextView tvRecipient;
        private final TextView tvDiscountValue;
        private final TextView tvDiscountType;

        private boolean isExpanded = false;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlanName          = itemView.findViewById(R.id.tvPlanName);
            tvOperator          = itemView.findViewById(R.id.tvOperator);
            tvDescription       = itemView.findViewById(R.id.tvDescription);
            tvToggleDescription = itemView.findViewById(R.id.tvToggleDescription);
            tvRecipient         = itemView.findViewById(R.id.tvRecipient);
            tvDiscountValue     = itemView.findViewById(R.id.tvDiscountValue);
            tvDiscountType      = itemView.findViewById(R.id.tvDiscountType);
        }

        /**
         * Rellena las vistas con los datos del plan de promoción.
         *
         * @param plan Plan de promoción a mostrar.
         */
        void bind(PromotionPlan plan) {
            isExpanded = false;

            tvPlanName.setText(plan.getPlanName());
            tvOperator.setText(plan.getOperator());
            tvRecipient.setText(plan.getRecipient());

            bindDescription(plan.getDescription());
            bindDiscount(plan);
        }

        private void bindDescription(String description) {
            tvDescription.setText(description);
            tvDescription.setMaxLines(3);

            boolean isLong = description.length() > 120;
            tvToggleDescription.setVisibility(isLong ? View.VISIBLE : View.GONE);

            tvToggleDescription.setText(R.string.promo_ver_mas);
            tvToggleDescription.setOnClickListener(v -> {
                isExpanded = !isExpanded;
                tvDescription.setMaxLines(isExpanded ? Integer.MAX_VALUE : 3);
                tvToggleDescription.setText(
                        isExpanded ? R.string.promo_ver_menos : R.string.promo_ver_mas
                );
            });
        }

        private void bindDiscount(PromotionPlan plan) {
            double value = plan.getDiscountValue();
            // Sin decimales si el valor es entero, con un decimal si no
            if (value == Math.floor(value)) {
                tvDiscountValue.setText(String.format(Locale.getDefault(), "%.0f", value));
            } else {
                tvDiscountValue.setText(String.format(Locale.getDefault(), "%.1f", value));
            }

            switch (plan.getDiscountType()) {
                case CENTS_PER_LITER:
                    tvDiscountType.setText("Cts/litro");
                    break;
                case PERCENTAGE:
                    tvDiscountType.setText("% precio");
                    break;
                default:
                    tvDiscountType.setText("");
                    break;
            }
        }
    }
}