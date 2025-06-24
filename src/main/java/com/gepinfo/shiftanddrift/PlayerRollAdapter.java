package com.gepinfo.shiftanddrift;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

public class PlayerRollAdapter extends ArrayAdapter<PlayerClass> {
    private final Context context;
    private final List<PlayerClass> rolls;

    public PlayerRollAdapter(Context context, List<PlayerClass> rolls) {
        super(context, 0, rolls);
        this.context = context;
        this.rolls = rolls;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        PlayerClass roll = rolls.get(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
        }

        TextView text1 = convertView.findViewById(android.R.id.text1);
        TextView text2 = convertView.findViewById(android.R.id.text2);

        String label = (roll.isBot()) ? " (Bot)" : "";
        text1.setText(roll.name+label);
        if(roll.uid.equals(MyApplication.getUid())) {
            text1.setTypeface(null, Typeface.BOLD_ITALIC);
        }

        if (roll.roll < 0) {
            text2.setText(context.getString(R.string.non_ha_ancora_tirato));
        } else {
            text2.setText(context.getString(R.string.ha_tirato) + roll.roll);
        }

        return convertView;
    }
}
