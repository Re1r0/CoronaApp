package com.mirkamal.coronaapp.ui.fragments.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.mirkamal.coronaapp.R;
import com.mirkamal.coronaapp.local.Database;
import com.mirkamal.coronaapp.model.entity.Country;
import com.mirkamal.coronaapp.model.pojo.CountryPOJO;
import com.mirkamal.coronaapp.network.ApiInitHelper;
import com.mirkamal.coronaapp.repository.MainRepository;
import com.mirkamal.coronaapp.utils.callbacks.DataRequestCallback;
import com.mirkamal.coronaapp.utils.lib.Converter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HomeFragment extends Fragment {

    @BindView(R.id.swipe_to_refresh_layout)
    SwipeRefreshLayout refreshLayout;

    @BindView(R.id.recycler_view_countries)
    RecyclerView recyclerViewCountries;

    @BindView(R.id.text_view_last_updated)
    TextView textViewLastUpdated;

    private CountriesListAdapter adapter;

    private MainRepository repository;

    private Database database;

    private List<CountryPOJO> countryPOJOS = new ArrayList<>();

    private NavController navController;

    SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        database = Database.getInstance(getContext());

        navController = NavHostFragment.findNavController(this);

        sharedPreferences = requireActivity().getPreferences(Context.MODE_PRIVATE);

        ButterKnife.bind(this, view);

        repository = new MainRepository(ApiInitHelper.getInstance().getService(), getContext(), new DataRequestCallback() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccessfulRequest() {
                refreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), "Updated!", Toast.LENGTH_SHORT).show();

                String time = "Last updated: " + getCurrentTime();
                textViewLastUpdated.setText(time);

                saveTimeToSharedPreferences(time);
            }

            @Override
            public void onFailedRequest() {
                refreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), "Failed to connect!", Toast.LENGTH_SHORT).show();

                getTimeFromSharedPreferences();

                List<CountryPOJO> countryPOJOS = new ArrayList<>();
                List<Country> countries = database.getCountriesDao().getAllCountriesFromDatabase();

                for (Country country:countries) {
                    countryPOJOS.add(Converter.convertCountryToCountryPOJO(country));
                }

                if (countryPOJOS.isEmpty()) {
                    Toast.makeText(getContext(), "No local data found!", Toast.LENGTH_SHORT).show();
                }

                adapter.submitList(countryPOJOS);
            }
        });

        configureRefreshLayout();
        configureRecyclerView();
    }

    private void saveTimeToSharedPreferences(String time) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("lastUpdated", time);
        editor.apply();
    }

    @SuppressLint("SetTextI18n")
    private void getTimeFromSharedPreferences() {
        textViewLastUpdated.setText("Last updated: " + sharedPreferences.getString("lastUpdated", "Never"));
    }

    private String getCurrentTime() {

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd.MM.yyyy", Locale.getDefault());

        return sdf.format(new Date());
    }

    private void configureRefreshLayout() {
        refreshLayout.setOnRefreshListener(() -> repository.getDataFromNetwork());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setObserver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        repository.getObservable().deleteObservers();
    }

    private void setObserver() {
        repository.getObservable().addObserver((observable, o) -> {
            countryPOJOS = (List<CountryPOJO>) o;

            adapter.submitList(countryPOJOS);
        });
    }

    private void configureRecyclerView() {
        adapter = new CountriesListAdapter(new CountryItemDiffCallback(), position -> navController.navigate(HomeFragmentDirections.actionHomeFragmentToDetailsFragment(countryPOJOS.get(position).getName())));
        recyclerViewCountries.setAdapter(adapter);

        refreshLayout.setRefreshing(true);
        repository.getDataFromNetwork();
    }
}
