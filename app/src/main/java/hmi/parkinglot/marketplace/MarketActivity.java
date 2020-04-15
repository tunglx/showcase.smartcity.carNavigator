package hmi.parkinglot.marketplace;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Message;
import android.preference.PreferenceManager;
import android.transition.Scene;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import hmi.parkinglot.Application;
import hmi.parkinglot.MainActivity;
import hmi.parkinglot.R;

/**
 * Data Marketplace View
 */
public class MarketActivity {
    private static Activity activity;
    private Context context;
    private WebView webView;
    private RelativeLayout mPbar;

    public MarketActivity(Context ctx) {
        context = ctx;
        activity = Application.mainActivity;
    }

    public void start() {
        ViewGroup rootContainer = (ViewGroup) activity.findViewById(R.id.mainFrame);
        activity.getLayoutInflater().inflate(R.layout.route, rootContainer);

        ViewGroup routeContainer = (ViewGroup) activity.findViewById(R.id.frameRoute);
        Scene scene1 = Scene.getSceneForLayout(routeContainer, R.layout.activity_market, activity);

        TransitionManager.go(scene1);

        // Create a web view containing the Business API Ecosystem web page
        webView = (WebView) activity.findViewById(R.id.market);
        mPbar = (RelativeLayout) activity.findViewById(R.id.spinner);


        webView.setWebViewClient(new SpinnerClient());
        webView.setWebChromeClient(new WebChromeClient() {
            // Handle window.open requests in order to support PayPal redirection
            @Override
            public boolean onCreateWindow(
                    WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {

                mPbar.setVisibility(View.VISIBLE);

                webView.removeAllViews();
                webView.scrollTo(0, 0);

                WebView newView = new WebView(context);
                newView.setVisibility(View.GONE);
                newView.getSettings().setJavaScriptEnabled(true);

                newView.setWebViewClient(new SpinnerClient());

                newView.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onCloseWindow(WebView view) {
                        // Remove the new view when the window is closed in order to return to the marketplace
                        ((WebView) view.getParent()).removeView(view);
                    }
                });

                // Create dynamically a new view
                newView.setLayoutParams(
                        new RelativeLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));

                webView.addView(newView);

                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newView);
                resultMsg.sendToTarget();
                return true;
            }
        });

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setSupportMultipleWindows(true);
        webView.addJavascriptInterface(new WebAppTokenInterface(activity), "Android");

        webView.loadUrl(Application.MARKET_URL);

        setupHeader();
    }

    public void back() {
        ((MainActivity) activity).onMarketplaceClosed();
    }

    private void setupHeader() {
        Toolbar myToolbar = (Toolbar) activity.findViewById(R.id.my_toolbar);
        ((AppCompatActivity) activity).setSupportActionBar(myToolbar);

        ((AppCompatActivity) activity).getSupportActionBar().setTitle("Data Marketplace");
        ((AppCompatActivity) activity).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private class SpinnerClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            view.setVisibility(View.VISIBLE);
            mPbar.setVisibility(View.GONE);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            mPbar.setVisibility(View.VISIBLE);
        }
    }

    private class WebAppTokenInterface {
        Context context;

        WebAppTokenInterface(Context c) {
            context = c;
        }

        private SharedPreferences.Editor openPreferences() {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            return prefs.edit();
        }

        /**
         * Save the username and the access token of the user logged in the Marketplace
         * This method is called from the JavaScript code of the Marketplace
         */
        @JavascriptInterface
        public void saveToken(String username, String token) {
            SharedPreferences.Editor prefsEditor = openPreferences();
            prefsEditor.putString(Application.BF_USER, username);
            prefsEditor.putString(Application.BF_TOKEN, token);
            prefsEditor.commit();

        }

        /**
         * Remove the existing username and access token of the Marketplace user when
         * she logs out. This method is called from the JavaScript code of the Marketplace
         */
        @JavascriptInterface
        public void clearToken() {
            SharedPreferences.Editor prefsEditor = openPreferences();
            prefsEditor.remove(Application.BF_USER);
            prefsEditor.remove(Application.BF_TOKEN);
            prefsEditor.commit();

            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        }
    }
}
