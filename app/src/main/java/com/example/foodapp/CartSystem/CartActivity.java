package com.example.foodapp.CartSystem;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.foodapp.DetailActivity;
import com.example.foodapp.Listeners.ICartLoadListener;
import com.example.foodapp.Listeners.MyUpdateCartEvent;
import com.example.foodapp.Model.CartModel;
import com.example.foodapp.Model.OrderModel;
import com.example.foodapp.Model.Products;
import com.example.foodapp.Model.UserModel;
import com.example.foodapp.R;
import com.example.foodapp.databinding.ActivityCartBinding;
import com.example.foodapp.databinding.DialogueDdressBinding;
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
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;


public class CartActivity extends AppCompatActivity implements ICartLoadListener {


    ActivityCartBinding binding;
    ICartLoadListener cartLoadListener;

    DatabaseReference reference;
    FirebaseUser user;

    ArrayList<CartModel> list = new ArrayList<>();
    private double sum = 0;

    String email,username,phone;

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
        loadCartItems();
    }







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.dataLyt.setVisibility(View.GONE);

        reference = FirebaseDatabase.getInstance().getReference();
        user = FirebaseAuth.getInstance().getCurrentUser();

        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());


        binding.recyclerView.setHasFixedSize(true);

        cartLoadListener = CartActivity.this;
        loadCartItems();


        binding.btnProceed.setOnClickListener(v -> {
            showOrderDialogue();
        });

        getUserData();



    }


    @Override
    public void onCartLoadListener(ArrayList<CartModel> list) {
        sum = 0;
        int count =0;

        for (CartModel cartModel : list){
            sum += cartModel.getTotalPrice();
            count +=cartModel.getQuantity();
        }
        binding.totalAmountText.setText(new StringBuilder("₹ ").append(sum));

        binding.textItemsCount.setText(new StringBuilder().append("Items (").append(count).append(")").toString());

        binding.recyclerView.setNestedScrollingEnabled(false);
        CartAdapter adapter = new CartAdapter(this,list,"cart");
        binding.recyclerView.setAdapter(adapter);



    }


    private void loadCartItems(){


        reference.child("Cart").child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()){
                            list.clear();
                            binding.progressBar.setVisibility(View.GONE);
                            binding.dataLyt.setVisibility(View.VISIBLE);


                            for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                                CartModel model = dataSnapshot.getValue(CartModel.class);
                                list.add(model);

                            }



                            checkIfNoData();
                            cartLoadListener.onCartLoadListener(list);
                        }else {
                            binding.dataLyt.setVisibility(View.GONE);
                            binding.progressBar.setVisibility(View.GONE);
                            binding.layout.root.setVisibility(View.VISIBLE);

                            binding.layout.noText.setText("No cart items!");
                            binding.layout.noImage.setImageResource(R.drawable.cart_icon);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.dataLyt.setVisibility(View.GONE);
                        Toast.makeText(CartActivity.this, "Error: "+error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkIfNoData(){
        if (list.size() > 0){
            binding.dataLyt.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.VISIBLE);
            binding.layout.root.setVisibility(View.GONE);
        }else {
            binding.dataLyt.setVisibility(View.GONE);
            binding.progressBar.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.GONE);
            binding.layout.root.setVisibility(View.VISIBLE);

            binding.layout.noText.setText("No cart items!");
            binding.layout.noImage.setImageResource(R.drawable.cart_icon);
        }
    }


    private void showOrderDialogue() {
        DialogueDdressBinding binding = DialogueDdressBinding.inflate(getLayoutInflater());
        Dialog dialog = new Dialog(CartActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(binding.getRoot());
        dialog.setCancelable(true);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));


        binding.btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String address = binding.inputAddress.getText().toString();
                if (address.isEmpty()){
                    Toast.makeText(CartActivity.this, "Enter address to continue!", Toast.LENGTH_SHORT).show();
                }else {
                    dialog.dismiss();
                    saveOrder(address);
                }
            }
        });

        dialog.show();
    }

    private void saveOrder(String address) {
        ProgressDialog progressDialog = new ProgressDialog(CartActivity.this);
        progressDialog.setMessage("Order Placing...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String id = String.valueOf(new Random().nextInt(1000));

        OrderModel model = new OrderModel();
        model.setUserId(user.getUid());
        model.setTimestamp(System.currentTimeMillis());
        model.setStatus("Placed");
        model.setTotalAmount((int) sum);
        model.setOrderId(id);
        model.setItems(list);
        model.setAddressId(address);

//        HashMap<String,Object> map = new HashMap<>();
//        map.put("username",username);
//        map.put("email",email);
//        map.put("phone",phone);
//        map.put("orderId",id);
//        map.put("items",list);
//        map.put("price",sum);
//        map.put("timestamp",System.currentTimeMillis());


        reference.child("Orders").child(user.getUid()).child(id).setValue(model)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        progressDialog.dismiss();
                        reference.child("Cart").child(user.getUid())
                                .removeValue();
                        finish();
                        Toast.makeText(CartActivity.this, "Order has been sent!", Toast.LENGTH_SHORT).show();
                    }else {
                        progressDialog.dismiss();
                        Toast.makeText(CartActivity.this, "Failed: "+
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });





    }

    private void getUserData(){
        reference.child("Users").child(user.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    UserModel model = snapshot.getValue(UserModel.class);
                    if (model !=null){
                        email = model.getEmil();
                        phone = model.getPhone();
                        username = model.getUsername();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CartActivity.this, "Error: "+
                        error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}