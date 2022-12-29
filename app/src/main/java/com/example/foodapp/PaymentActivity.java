//Kushwanth23
package com.example.foodapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import com.example.foodapp.Listeners.ICartLoadListener;
import com.example.foodapp.Model.CartModel;
import com.example.foodapp.Model.SellerModel;
import com.example.foodapp.Model.UserModel;
import com.example.foodapp.databinding.ActivityPaymentBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class PaymentActivity extends AppCompatActivity {

    ActivityPaymentBinding binding;
    DatabaseReference reference;
    FirebaseUser user;
    DataSnapshot snapshot;

    ICartLoadListener cartLoadListener;
    ArrayList<CartModel> list = new ArrayList<>();
    public Double sum;


    String phone,username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(R.layout.activity_payment);

        reference = FirebaseDatabase.getInstance().getReference().child("Seller");
        user = FirebaseAuth.getInstance().getCurrentUser();

        SellerModel sellerModel = snapshot.getValue(SellerModel.class);

        String number = sellerModel.getPhone();

        //binding.onlinePayment.setOnClickListener(v -> sendMessage(number));

//        binding.oflinePayment.setOnClickListener(v -> {
//            AlertDialog.Builder builder = new AlertDialog.Builder(PaymentActivity.this);
//            builder.setTitle("Are You Sure?");
//            builder.setCancelable(false);
//            builder.setPositiveButton("Yes", (dialog, which) -> {
//                dialog.dismiss();
//                Intent intent = new Intent(PaymentActivity.this, MainActivity.class);
//                startActivity(intent);
//                finish();
//            });
//            builder.setNegativeButton("No", null);
//            builder.create().show();
//        });


    }

    private void sendMessage(String phoneNumber){
        PackageManager pm=getPackageManager();

        String number;

        if (phoneNumber.startsWith("+91")){
            number = phoneNumber;
        }else {
            number = "+91"+phoneNumber;
        }

        String message = "GRIHASTA HOME FOODS\n" +
                "To continue your order, please pay the amount of "+sum;

        startActivity(
                new Intent(Intent.ACTION_VIEW,
                        Uri.parse(
                                String.format("https://api.whatsapp.com/send?phone=%s&text=%s",
                                        number, message)
                        )
                )
        );
    }

    public void onCartLoadListener(ArrayList<CartModel> list) {

        for (CartModel cartModel : list){
            sum += cartModel.getTotalPrice();
        }

    }

}

