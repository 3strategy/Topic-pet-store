package com.example.topic;

import android.os.Bundle;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.topic.databinding.ActivityMainBinding;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String BASE_URL = "https://petstore3.swagger.io/api/v3";
    private ActivityMainBinding binding;
    private RequestQueue queue;
    private int httpStatus;
    private final Gson gson = new Gson();
    private Pet[] pets = new Pet[0];

    /** Creates the screen and connects its network action. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        queue = Volley.newRequestQueue(getApplicationContext());
        binding.getPets.setOnClickListener(v -> getPets());
        binding.loadDemo.setOnClickListener(v -> loadDemo());
        binding.parsePets.setOnClickListener(v -> parsePets());
        binding.showAvailable.setOnClickListener(v -> showAvailable());
        binding.showAll.setOnClickListener(v -> renderPets(pets));
        binding.renameFirst.setOnClickListener(v -> renameFirst());
        binding.postPet.setOnClickListener(v -> postPet());
    }

    /** Fetches JSON as text; no Java Pet objects are created here. */
    private void getPets() {
        queue.cancelAll(this);
        binding.status.setText("Sending GET...");
        StringRequest request = new StringRequest(Request.Method.GET,
                BASE_URL + "/pet/findByStatus?status=available", text -> {
            binding.responseJson.setText(text);
            binding.jsonInput.setText(text);
            binding.status.setText("GET HTTP " + httpStatus);
        }, this::showError) {
            @Override
            public Map<String, String> getHeaders() {
                return Collections.singletonMap("Accept", "application/json");
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                httpStatus = response.statusCode;
                return Response.success(
                        new String(response.data, java.nio.charset.StandardCharsets.UTF_8), null);
            }
        };
        request.setShouldCache(false);
        request.setTag(this);
        queue.add(request);
    }

    /** Loads editable, predictable JSON without changing the last HTTP response. */
    private void loadDemo() {
        try (Scanner scanner = new Scanner(getResources().openRawResource(R.raw.pets), "UTF-8")) {
            binding.jsonInput.setText(scanner.useDelimiter("\\A").next());
        }
    }

    /** Reconstructs objects from text, then builds a table from their fields. */
    private void parsePets() {
        try {
            String jsonText = binding.jsonInput.getText().toString();
            Pet[] parsed = gson.fromJson(jsonText, Pet[].class);
            if (parsed == null) throw new JsonParseException("Expected an array, not null");
            for (Pet pet : parsed) {
                if (pet == null) throw new JsonParseException("A pet cannot be null");
            }
            pets = parsed;
            renderPets(pets);
        } catch (JsonParseException error) {
            pets = new Pet[0];
            renderPets(pets);
            binding.objectStatus.setText("Cannot deserialize: " + error.getMessage());
        }
    }

    /** Selects existing objects by their status; no JSON processing is needed. */
    private void showAvailable() {
        ArrayList<Pet> available = new ArrayList<>();
        for (Pet pet : pets) {
            if ("available".equals(pet.status)) available.add(pet);
        }
        renderPets(available.toArray(new Pet[0]));
    }

    /** Mutates an object in memory while leaving both JSON text areas untouched. */
    private void renameFirst() {
        if (pets.length == 0) {
            binding.objectStatus.setText("Deserialize some animals first");
            return;
        }
        pets[0].name = "Pixel";
        renderPets(pets);
    }

    /** Displays object fields; this method never reads or parses JSON. */
    private void renderPets(Pet[] animals) {
        binding.petTable.removeAllViews();
        addRow("ID", "Name", "Status");
        for (Pet pet : animals) {
            addRow(String.valueOf(pet.id), pet.name, pet.status);
        }
        binding.objectStatus.setText(animals.length + " animal objects shown");
    }

    /** Adds one table row with three ordinary text cells. */
    private void addRow(String id, String name, String status) {
        TableRow row = new TableRow(this);
        for (String value : new String[]{id, name, status}) {
            TextView cell = new TextView(this);
            cell.setText(String.valueOf(value));
            cell.setPadding(12, 8, 12, 8);
            row.addView(cell);
        }
        binding.petTable.addView(row);
    }

    /** Serializes a new animal and sends exactly the text displayed above the response. */
    private void postPet() {
        Pet animal = new Pet();
        animal.id = System.currentTimeMillis();
        animal.name = "Classroom cat 🐈";
        animal.status = "available";
        animal.photoUrls = new String[0];
        String outgoingJson = gson.toJson(animal);
        binding.outgoingJson.setText(outgoingJson);
        queue.cancelAll(this);
        binding.status.setText("Sending POST...");
        StringRequest request = new StringRequest(Request.Method.POST,
                BASE_URL + "/pet", text -> {
            binding.responseJson.setText(text);
            binding.status.setText("POST HTTP " + httpStatus + " — returned JSON shown below");
        }, this::showError) {
            @Override
            public Map<String, String> getHeaders() {
                return Collections.singletonMap("Accept", "application/json");
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=UTF-8";
            }

            @Override
            public byte[] getBody() {
                return outgoingJson.getBytes(StandardCharsets.UTF_8);
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                httpStatus = response.statusCode;
                return Response.success(
                        new String(response.data, java.nio.charset.StandardCharsets.UTF_8), null);
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(10000, 0, 1f));
        request.setShouldCache(false);
        request.setTag(this);
        queue.add(request);
    }

    /** Keeps HTTP error bodies visible and distinguishes them from connection failures. */
    private void showError(VolleyError error) {
        NetworkResponse response = error.networkResponse;
        binding.status.setText(response == null
                ? "Network failure: " + error.getClass().getSimpleName()
                : "HTTP " + response.statusCode);
        binding.responseJson.setText(response == null ? "No HTTP response body received."
                : new String(response.data, java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Cancels delivery to this screen when it is no longer visible. */
    @Override
    protected void onStop() {
        queue.cancelAll(this);
        binding.status.setText("Ready — requests cancelled when leaving the screen");
        super.onStop();
    }
}
