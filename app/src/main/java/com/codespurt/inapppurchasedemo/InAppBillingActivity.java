package com.codespurt.inapppurchasedemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.codespurt.inapppurchasedemo.util.Inventory;
import com.codespurt.inapppurchasedemo.util.IabHelper;
import com.codespurt.inapppurchasedemo.util.IabResult;
import com.codespurt.inapppurchasedemo.util.Purchase;

public class InAppBillingActivity extends AppCompatActivity implements View.OnClickListener {

    private Button clickAfterPurchase, buyClick;
    private static final String TAG = "com.inappbilling";
    private IabHelper mHelper = null;
    static final String ITEM_SKU = "android.test.purchased";
    private String base64EncodedPublicKey = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_app_billing);

        clickAfterPurchase = (Button) findViewById(R.id.btn_click_after_purchase);
        buyClick = (Button) findViewById(R.id.btn_buy_click);

        clickAfterPurchase.setEnabled(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mHelper = new IabHelper(this, base64EncodedPublicKey);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {

            @Override
            public void onIabSetupFinished(IabResult result) {
                if (result.isSuccess()) {
                    Log.d(TAG, "In-app Billing is set up OK");
                } else {
                    Log.d(TAG, "In-app Billing setup failed: " + result);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        clickAfterPurchase.setOnClickListener(this);
        buyClick.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_click_after_purchase:
                clickAfterPurchase.setEnabled(false);
                buyClick.setEnabled(true);
                Toast.makeText(this, R.string.item_consumed, Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_buy_click:
                int resultCode = 10001;
                String purchaseToken = "";
                mHelper.launchPurchaseFlow(this, ITEM_SKU, resultCode, mPurchaseFinishedListener, purchaseToken);
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        clickAfterPurchase.setOnClickListener(null);
        buyClick.setOnClickListener(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {

        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase info) {
            if (result.isFailure()) {
                Toast.makeText(InAppBillingActivity.this, result.getMessage(), Toast.LENGTH_SHORT).show();
            } else if (info.getSku().equals(ITEM_SKU)) {
                consumeItem();
                buyClick.setEnabled(false);
            }
        }
    };

    private void consumeItem() {
        mHelper.queryInventoryAsync(mReceivedInventoryListener);
    }

    IabHelper.QueryInventoryFinishedListener mReceivedInventoryListener = new IabHelper.QueryInventoryFinishedListener() {

        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
            if (result.isFailure()) {
                Toast.makeText(InAppBillingActivity.this, result.getMessage(), Toast.LENGTH_SHORT).show();
                // consume item even if error occurs, for next purchase
                if (inv.hasPurchase(ITEM_SKU)) {
                    mHelper.consumeAsync(inv.getPurchase(ITEM_SKU), mConsumeFinishedListener);
                }
            } else {
                mHelper.consumeAsync(inv.getPurchase(ITEM_SKU), mConsumeFinishedListener);
            }
        }
    };

    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            if (result.isFailure()) {
                Toast.makeText(InAppBillingActivity.this, result.getMessage(), Toast.LENGTH_SHORT).show();
            } else {
                clickAfterPurchase.setEnabled(true);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHelper != null) {
            mHelper.dispose();
        }
        mHelper = null;
    }
}