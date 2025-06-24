package com.gepinfo.shiftanddrift;

import android.content.Context;
import android.graphics.drawable.PictureDrawable;
import android.widget.ImageView;

import com.caverock.androidsvg.SVG;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class CarRenderer {

    private static String svgTemplate;

    private static String loadSvgTemplate(Context context) {
        if (svgTemplate != null) return svgTemplate;

        try (InputStream is = context.getResources().openRawResource(R.raw.race_car_androidsvg);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
            svgTemplate = sb.toString();
            return svgTemplate;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static PictureDrawable renderCar(Context context, String frontColor, String bodyColor, String rearColor) {
        try {
            String svg = loadSvgTemplate(context);

            HashMap<String, String> colors = new HashMap<>();
            colors.put("front_wing_top", frontColor);
            colors.put("front_wing_bottom", frontColor);
            colors.put("main_body", bodyColor);
            colors.put("rear_wing", rearColor);

            for (String id : colors.keySet()) {
                svg = svg.replaceAll(
                        "(<path[^>]*id=\"" + id + "\"[^>]*fill=\")#[A-Fa-f0-9]{6}(\")",
                        "$1" + colors.get(id) + "$2"
                );
            }

            SVG parsedSvg = SVG.getFromString(svg);
            return new PictureDrawable(parsedSvg.renderToPicture());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void applyToImageView(ImageView imageView, Context context, PlayerClass player) {
        PictureDrawable drawable = renderCar(
                context,
                PlayerClass.colorHexMap.get(player.carColorFront),
                PlayerClass.colorHexMap.get(player.carColorBody),
                PlayerClass.colorHexMap.get(player.carColorRear)
        );
        if (drawable != null) {
            imageView.setLayerType(ImageView.LAYER_TYPE_SOFTWARE, null);
            imageView.setImageDrawable(drawable);
        }
    }
}
