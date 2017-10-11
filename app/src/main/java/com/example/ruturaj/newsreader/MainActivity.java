package com.example.ruturaj.newsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> newsTitles = new ArrayList<>();
    ArrayList<String> newsURL = new ArrayList<>();
    ArrayAdapter arrayAdapter;
    SQLiteDatabase articlesDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView) findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, newsTitles);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), ContentActivity.class);
                intent.putExtra("url", newsURL.get(i));

                startActivity(intent);
            }
        });

        articlesDB = this.openOrCreateDatabase("Article", MODE_PRIVATE, null);
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS article (id INTEGER PRIMARY KEY, articleTitle VARCHAR, articleURL VARCHAR)");

        updateListView();

        DownloadTask task = new DownloadTask();

        try{
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void updateListView(){
        Cursor cursor = articlesDB.rawQuery("SELECT * FROM article", null);

        int titleIndex = cursor.getColumnIndex("articleTitle");
        int urlIndex = cursor.getColumnIndex("articleURL");

        if(cursor.moveToFirst()){
            newsTitles.clear();
            newsURL.clear();

            do{
                newsTitles.add(cursor.getString(titleIndex));
                newsURL.add(cursor.getString(urlIndex));
            }
            while (cursor.moveToNext());
        }
        arrayAdapter.notifyDataSetChanged();
    }

    public class DownloadTask extends AsyncTask<String, Void, String>{
        @Override
        protected String doInBackground(String... strings) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(strings[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                int data = inputStreamReader.read();

                while (data != -1){
                    char current = (char) data;
                    result += current;
                    data = inputStreamReader.read();
                }

                Log.i("Articles: ", result);

                JSONArray jsonArray = new JSONArray(result);
                int numberOfItems = 30;

                if(jsonArray.length() < numberOfItems) {
                    numberOfItems = jsonArray.length();
                }

                articlesDB.execSQL("DELETE FROM article");

                for(int i = 0; i < numberOfItems; i++) {
                    String articleID = jsonArray.getString(i);
                    Log.i("ArticleID: ", articleID);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleID +".json?print=pretty");
                    urlConnection =(HttpURLConnection) url.openConnection();
                    inputStream = urlConnection.getInputStream();
                    inputStreamReader = new InputStreamReader(inputStream);
                    data = inputStreamReader.read();
                    String article = "";

                    while (data != -1){
                        char current = (char) data;
                        article += current;
                        data = inputStreamReader.read();
                    }

                    Log.i("ArticleInfo: ", article);

                    JSONObject jsonObject = new JSONObject(article);

                    if(!jsonObject.isNull("title") && !jsonObject.isNull("url")){
                        String articleTitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");

                        Log.i("ArticlesTitle: ", articleTitle);
                        Log.i("ArticlesURL: ", articleUrl);

                        String sqlData = "INSERT INTO article (articleTitle, articleURL) VALUES (?, ?)";
                        SQLiteStatement statement = articlesDB.compileStatement(sqlData);
                        statement.bindString(1, articleTitle);
                        statement.bindString(2, articleUrl);

                        statement.execute();
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }
}
