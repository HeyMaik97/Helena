package com.dev.helena.Monitoring;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.dev.helena.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class ListMonitoringActivity extends AppCompatActivity implements RcViewMonitoringAdapter.OnItemClickListener {

    DatabaseReference databaseReference;
    ProgressDialog progressDialog;
    List<Monitoring> list = new ArrayList<>();
    RecyclerView recyclerView;
    RcViewMonitoringAdapter adapter;
    FloatingActionButton addButton;
    private StorageReference mStorage;
    private FirebaseAuth fAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_monitoring);

        fAuth = FirebaseAuth.getInstance();
        mStorage = FirebaseStorage.getInstance().getReference();
        addButton = (FloatingActionButton) findViewById(R.id.add_button);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerMonitoringView);
        recyclerView.setLayoutManager(new LinearLayoutManager(ListMonitoringActivity.this));
        progressDialog = new ProgressDialog(ListMonitoringActivity.this);
        databaseReference = FirebaseDatabase.getInstance().getReference("all_monitorings").child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Monitoring monitoring = snapshot.getValue(Monitoring.class);
                    monitoring.setKey(snapshot.getKey());
                    list.add(monitoring);
                }
                adapter = new RcViewMonitoringAdapter(ListMonitoringActivity.this, list);
                adapter.setOnItemClickListener(ListMonitoringActivity.this);
                recyclerView.setAdapter(adapter);
                progressDialog.dismiss();
                // AlertDialog se non ci sono monitoraggi
                if (list.isEmpty()) {
                    final AlertDialog.Builder builderEmpty = new AlertDialog.Builder(ListMonitoringActivity.this);
                    Resources res = getResources();
                    String monitoringFound = res.getQuantityString(R.plurals.noMonitoring, 0);
                    builderEmpty.setTitle(monitoringFound);
                    builderEmpty.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    builderEmpty.show();
                    progressDialog.dismiss();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressDialog.dismiss();
            }
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent in = new Intent(ListMonitoringActivity.this, AddMonitoringActivity.class);
                startActivity(in);
            }
        });
    }

    @Override
    public void onItemClick(int position) {
        Monitoring clickedElement = list.get(position);
        ArrayList<String> pathElelemntsData = new ArrayList<>();
        String[] elementsData = {clickedElement.getNameMonitoring(), clickedElement.getDate()};
        String cpId = clickedElement.getKey();
        for (String str : clickedElement.getPath()) {
            pathElelemntsData.add(str);
        }
        openDetailActivity(elementsData, cpId, pathElelemntsData);
    }

    private void openDetailActivity(String[] data, String cpid, ArrayList<String> path) {
        Intent intent = new Intent(ListMonitoringActivity.this, DetailsMonitoringActivity.class);
        intent.putExtra("NAME_KEY", data[0]);
        intent.putExtra("DATE_KEY", data[1]);
        intent.putExtra("CPID_KEY", cpid);
        intent.putExtra("PATH_KEYS", path);
        startActivity(intent);
    }

    @Override
    public void onDeleteItemClick(int position) {
        final Monitoring selectedItem = list.get(position); //recupero la posizione del coupon nella lista
        final String selectedKey = selectedItem.getKey(); //recupero la chiave del coupon
        databaseReference.child(selectedKey).removeValue(); //eliminazione coupon dal database attraverso la chiave
        StorageReference imageRef = mStorage.child("users/" + fAuth.getCurrentUser().getUid() + "/monitoring/" + selectedKey);
        imageRef.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {
                for (StorageReference item : listResult.getItems()) {
                    item.delete();
                }

            }
        });
        finish();
        startActivity(getIntent());
    }

}