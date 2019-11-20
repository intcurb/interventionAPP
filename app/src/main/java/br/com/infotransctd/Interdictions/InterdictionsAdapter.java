package br.com.infotransctd.Interdictions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import br.com.infotransctd.R;

public class InterdictionsAdapter extends BaseAdapter {

    private Context context;
    private List<Interdiction> lista;
    private TextView description;
    private TextView organization;
    private TextView street_one;
    private TextView street_two;

    public InterdictionsAdapter(){

    }

    public InterdictionsAdapter(Context context, List<Interdiction> lista){
        this.context = context;
        this.lista = lista;
    }

    @Override
    public int getCount() {
        return lista.size();
    }

    @Override
    public Object getItem(int position) {
        return lista.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Interdiction interdiction = lista.get(position);

        String[] origin = interdiction.getOrigin().getStreet().toString().split(",");
        String[] destination = interdiction.getDestination().getStreet().toString().split(",");


        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.interdictions, null);

        description = (TextView) layout.findViewById(R.id.txtDescription);
        organization = (TextView) layout.findViewById(R.id.txtOrganization);
        street_one = (TextView) layout.findViewById(R.id.txtStreetOne);
        street_two = (TextView) layout.findViewById(R.id.txtStreetTwo);

        try {

            description.setText(interdiction.getDescription());
            organization.setText(interdiction.getOrganization());
            street_one.setText(origin[0]);
            street_two.setText(destination[0]);

        }catch (Exception e){
            e.printStackTrace();
        }

        return layout;
    }
}
