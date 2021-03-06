package com.example.chloe.bceo.view;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.example.chloe.bceo.Adapter.ProductAdapter;
import com.example.chloe.bceo.DBLayout.Create;
import com.example.chloe.bceo.DBLayout.DatabaseConnector;
import com.example.chloe.bceo.DBLayout.Read;
import com.example.chloe.bceo.R;
import com.example.chloe.bceo.fragment.FragmentBottomMenu;
import com.example.chloe.bceo.model.Product;
import com.example.chloe.bceo.model.User;
import com.example.chloe.bceo.util.HTTPGet;
import com.example.chloe.bceo.util.Image64Base;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;


public class BrowseActivity extends AppCompatActivity {

    ArrayList<Product> productList;
    ArrayList<Product> gridProdList = new ArrayList<Product>();
    ProductAdapter productAdapter;
    DatabaseConnector databaseConnector;

    Spinner category;
    Button button_filter;
    private User user;
    private boolean fromGroupPage;

    public class MyAdapter extends BaseAdapter {

        private Context mContext;

        public MyAdapter(Context c) {
            // TODO Auto-generated constructor stub
            mContext = c;
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return gridProdList.size();
        }

        @Override
        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            return gridProdList.get(arg0);
        }

        @Override
        public long getItemId(int arg0) {
            // TODO Auto-generated method stub
            return arg0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub

            View grid;

            if(convertView==null){
                grid = new View(mContext);
                LayoutInflater inflater = getLayoutInflater();
                grid=inflater.inflate(R.layout.fragment_grid_item, parent, false);
            }else{
                grid = (View)convertView;
            }

            ImageView imageView = (ImageView)grid.findViewById(R.id.imagepart);
            TextView textView = (TextView)grid.findViewById(R.id.textpart);
            TextView tv_Price = (TextView)grid.findViewById(R.id.text_price);

            Product prod_tmp = gridProdList.get(position);
            int image_id = prod_tmp.getImageId();
            Read databaseReader = new Read();
            Cursor cursor = databaseReader.getOneImage(image_id, databaseConnector);
            cursor.moveToFirst();
            String response = cursor.getString(1);

            Log.d("[HTTPGet]", response);

            Bitmap bm = Image64Base.decodeBase64(response);

            //ImageView
            imageView.setImageBitmap(bm);

            //TextView
            textView.setText(prod_tmp.getpName());
            tv_Price.setText(Float.toString(prod_tmp.getpPrice()));

            return grid;
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        databaseConnector = new DatabaseConnector(this);
        user = (User) getIntent().getSerializableExtra("user");
        FragmentBottomMenu.setUser(user);
        productAdapter = new ProductAdapter();
        productList = productAdapter.getProductList();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);

        final GridView gridview = (GridView) findViewById(R.id.gridview_market);
        gridview.setAdapter(new MyAdapter(this));
        gridview.setOnItemClickListener(new ItemClickListener(BrowseActivity.this));

        //Get json
        HTTPGet httpGet = new HTTPGet();
        String urlStr = httpGet.buildURL("products?group_id=" + user.getGroupID());
        String jsonString = httpGet.getResponse(urlStr);
        Log.d("[Browse Page] -> URL: ", urlStr);
        Log.d("[Browse Page] -> Json: ", jsonString);
        //Parse json and get products
        jsonParser(jsonString);

        fromGroupPage = (boolean) getIntent().getBooleanExtra("visibility", false);
        if (fromGroupPage) {
            //Get json
            urlStr = httpGet.buildURL("all_images");
            jsonString = httpGet.getResponse(urlStr);
            Log.d("[Browse Page] -> URL: ", urlStr);
            Log.d("[Browse Page] -> Json: ", jsonString);
            jsonParser2(jsonString);
            fromGroupPage = false;
        }

        updateGridProduct("all");

        //Spinner to filter items
        category = (Spinner) findViewById(R.id.spinnerCategory);
        button_filter = (Button) findViewById(R.id.button_filter);
        button_filter.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        String text = category.getSelectedItem().toString().toLowerCase();
                        Log.d("[Spinner]", text);

                        updateGridProduct(text);
                        gridview.invalidateViews();
                    }
                });
    }

    private void updateGridProduct(String text) {
        gridProdList.clear();

        for (int i = 0; i< productList.size(); i++){
            Product p = productList.get(i);
            if (text.equals("all") || p.getCategory().equals(text))
                gridProdList.add(p);
        }
    }

    public class ItemClickListener implements AdapterView.OnItemClickListener {
        Context c;

        public ItemClickListener(Context c){
            this.c = c;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //Dialog: show which item clicked
            Toast.makeText(c, "Postion: "+ position + "\nID: " + id, Toast.LENGTH_SHORT).show();

            //Start product activity
            Intent intent = new Intent(view.getContext(), ProductActivity.class);
            intent.putExtra("user", user);
            intent.putExtra("prod", productList.get(position));
            startActivityForResult(intent, 0);
        }
    }

    public void jsonParser(String jsonStr){
        String json = "{'abridged_cast':" + jsonStr + "}";
        Log.d("[jsonParser]: ", json);

        JSONObject jsonResponse;

        try {
            ArrayList<String> temp = new ArrayList<String>();
            jsonResponse = new JSONObject(json);
            JSONArray products = jsonResponse.getJSONArray("abridged_cast");

            for(int i = 0; i < products.length(); i++){

                JSONObject p = products.getJSONObject(i);
                String category = p.getString("category");
                int id = Integer.parseInt(p.getString("id"));
                String name = p.getString("name");
                float price = Float.parseFloat(p.getString("price"));
                String description = p.getString("description");
                int waitlist = Integer.parseInt(p.getString("waitlist"));
                int image_id = Integer.parseInt(p.getString("image_id"));
                int group_id = Integer.parseInt(p.getString("group_id"));
                String status = p.getString("status");

                Product prod_tmp = new Product(id, name, price, description, waitlist, image_id, group_id, category, status);
                Log.d("[Product] ", prod_tmp.toString());
                productAdapter.addProduct(prod_tmp);
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void jsonParser2(String jsonStr){
        String json = "{'abridged_cast':" + jsonStr + "}";
        Log.d("[jsonParser]: ", json);
        JSONObject jsonResponse;
        Create databaseCreator = new Create();
        try {
            ArrayList<String> temp = new ArrayList<String>();
            jsonResponse = new JSONObject(json);
            JSONArray images = jsonResponse.getJSONArray("abridged_cast");

            for(int i = 0; i < images.length(); i++){
                JSONObject p = images.getJSONObject(i);
                int pID = p.getInt("id");
                String content = p.getString("content");
                databaseCreator.createImage(pID, content, databaseConnector);
            }
            Toast.makeText(this, "Json: "+temp, Toast.LENGTH_LONG).show();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

