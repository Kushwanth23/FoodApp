//Kushwanth23
package com.example.foodapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.foodapp.CartSystem.CartActivity;
import com.example.foodapp.Listeners.ICartLoadListener;
import com.example.foodapp.Listeners.MyUpdateCartEvent;
import com.example.foodapp.Model.CartModel;
import com.example.foodapp.Model.Products;
import com.example.foodapp.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements ICartLoadListener {

    ActivityMainBinding binding;

    ArrayList<Products> list = new ArrayList<>();
    ItemAdapter adapter;

    DatabaseReference reference;
    ICartLoadListener cartLoadListener;

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (EventBus.getDefault().hasSubscriberForEvent(MyUpdateCartEvent.class))
            EventBus.getDefault().removeStickyEvent(MyUpdateCartEvent.class);
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onUpdateCart(MyUpdateCartEvent event){
        checkCartStatus();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        reference = FirebaseDatabase.getInstance().getReference();

        cartLoadListener = this;

//        binding.vegetablesLyt.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this,VegetablesListActivity.class);
//                intent.putExtra("cat","Vegetables");
//                startActivity(intent);
//            }
//        });
//
//
//        binding.fruitsLyt.setOnClickListener(v -> {
//            Intent intent = new Intent(MainActivity.this,VegetablesListActivity.class);
//            intent.putExtra("cat","Fruits");
//            startActivity(intent);
//        });
//
//
//        binding.grainsLyt.setOnClickListener(v -> {
//            Intent intent = new Intent(MainActivity.this,VegetablesListActivity.class);
//            intent.putExtra("cat","Grains");
//            startActivity(intent);
//        });

        binding.cartLyt.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, CartActivity.class)));

        binding.imgProfile.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, OptionsActivity.class)));

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.recyclerView.setHasFixedSize(true);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                getProducts();
            }
        },300);

        checkCartStatus();

    }
    private void getProducts(){
        reference.child("Products").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    list.clear();
                    binding.progressBar.setVisibility(View.GONE);
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                        Products products = dataSnapshot.getValue(Products.class);
                        list.add(products);
                    }

                    GridLayoutManager gridLayoutManager = new GridLayoutManager(MainActivity.this,3);
                    binding.recyclerView.setLayoutManager(gridLayoutManager);

                    adapter = new ItemAdapter(MainActivity.this,list);
                    binding.recyclerView.setAdapter(adapter);
                }else {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "No products now!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Error: "+
                        error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkCartStatus(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        ArrayList<CartModel> cartList = new ArrayList<>();
        assert user != null;
        reference.child("Cart").child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()){
                            binding.cartItemCount.setVisibility(View.VISIBLE);
                            for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                                CartModel model = dataSnapshot.getValue(CartModel.class);
                                cartList.add(model);
                            }

                            cartLoadListener.onCartLoadListener(cartList);
                            if (cartList.size() > 0){
                                binding.cartItemCount.setVisibility(View.VISIBLE);

                            }else {
                                binding.cartItemCount.setVisibility(View.GONE);

                            }
                        }else {
                            binding.cartItemCount.setVisibility(View.GONE);

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MainActivity.this, "Error: "+error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onCartLoadListener(ArrayList<CartModel> list) {
        int cartSum = 0;
        for (CartModel cartModel : list){
            cartSum +=cartModel.getQuantity();

        }
        binding.cartItemCount.setText(String.valueOf(cartSum));
    }
}