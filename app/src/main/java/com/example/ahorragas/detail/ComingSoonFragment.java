package com.example.ahorragas.detail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ahorragas.R;

public class ComingSoonFragment extends Fragment {

    private static final String ARG_TITLE = "arg_title";

    /**
     * Crea una nueva instancia del fragment con el título de la sección.
     *
     * @param title Nombre de la sección que estará disponible próximamente.
     * @return Nueva instancia de ComingSoonFragment.
     */
    public static ComingSoonFragment newInstance(String title) {
        ComingSoonFragment fragment = new ComingSoonFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_coming_soon, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String title = getArguments() != null ? getArguments().getString(ARG_TITLE, "") : "";

        TextView tvMessage = view.findViewById(R.id.tvComingSoon);
        tvMessage.setText(title + "\nPróximamente disponible 🚧");
    }
}