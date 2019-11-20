package br.com.infotransctd.Interdictions;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import br.com.infotransctd.R;

public class InterdictionsList extends AppCompatActivity {

    private List<Interdiction> interdictions = new ArrayList<>();
    private ListView lvInterdictions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interdictions_list);

        lvInterdictions = (ListView) findViewById(R.id.lvInterdictions);

        try{
            Intent i = getIntent();
            interdictions = (List<Interdiction>) i.getSerializableExtra("interdictions");

//            String[] interdiction = new String[interdictions.size()];
//            interdiction = interdictions.toArray(interdiction);

            ArrayAdapter<Interdiction> adapter = new ArrayAdapter<Interdiction>(this,
                    android.R.layout.simple_list_item_1, interdictions);

            lvInterdictions.setAdapter(new InterdictionsAdapter(this, interdictions));

        }catch (Exception e){
            e.printStackTrace();
        }


    }



}
