package com.gepinfo.shiftanddrift;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

public class PlayerListAdapter extends ArrayAdapter<PlayerClass> {
    private Context context;
    private List<PlayerClass> players;

    public PlayerListAdapter(Context context, List<PlayerClass> players) {
        super(context, 0, players);
        this.context = context;
        this.players = players;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(context).inflate(R.layout.item_player, parent, false);

        PlayerClass player = players.get(position);

        TextView textPlayerName = convertView.findViewById(R.id.textPlayerName);

        textPlayerName.setText(player.name + ((player.isBot()) ? " (Bot)" : ""));
        if(player.uid.equals(MyApplication.getUid())) {
            textPlayerName.setTypeface(null, Typeface.BOLD_ITALIC);
        }

        // Cambia immagine in base ai colori
        ImageView imageCar = convertView.findViewById(R.id.imageCar);
        CarRenderer.applyToImageView(imageCar, context, player);

        return convertView;
    }
}
