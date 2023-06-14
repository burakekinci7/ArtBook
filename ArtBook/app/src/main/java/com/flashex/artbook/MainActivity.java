package com.flashex.artbook;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.flashex.artbook.databinding.ActivityMainBinding;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    //private ResultProfileBinding binding; -> documents (ResultProfileBinding==ActivityMainBinding)
    private ActivityMainBinding binding;//binding findbyId

    //appBar
    Toolbar toolbar;

    //art list
    ArrayList<Art> arrayList;

    //Apdater
    ArtAdapter artAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        //toolbar important => burasi olmassa calismaz
        toolbar = binding.toolbar;
        setSupportActionBar(toolbar);

        //init
        arrayList=new ArrayList<>();
        artAdapter=new ArtAdapter(arrayList);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(artAdapter);

        //call func
        getData();

    }

    private void getData(){
        try {
            //open database
            SQLiteDatabase sqLiteDatabase=this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);

            Cursor cursor=sqLiteDatabase.rawQuery("SELECT * FROM arts ",null);
            int nameIx=cursor.getColumnIndex("artname");
            int idIx=cursor.getColumnIndex("id");

            while (cursor.moveToNext()){
                String name=cursor.getString(nameIx);
                int id =cursor.getInt(idIx);
                Art art=new Art(name,id);
                arrayList.add(art);
            }
            artAdapter.notifyDataSetChanged();
            cursor.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //xml ile connect inflater in menu
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.art_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem tem) {
        //click the menu item
        if (tem.getItemId() == R.id.add_art) {
            Intent intent = new Intent(MainActivity.this, ArtActivity.class);
            intent.putExtra("info","new");
            startActivity(intent);
        }
        return super.onOptionsItemSelected(tem);
    }
}