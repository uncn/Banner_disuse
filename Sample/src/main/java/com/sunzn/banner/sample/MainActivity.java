package com.sunzn.banner.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.sunzn.banner.library.Banner;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Banner<Pack> banner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        banner = findViewById(R.id.banner);

        final List<Pack> packs = new ArrayList<>();
        packs.add(new Pack("http://desk.fd.zol-img.com.cn/t_s1024x768c5/g5/M00/0D/01/ChMkJ1gq00WIXw_GAA47r_8gjqgAAXxJAH8qOMADjvH566.jpg"));
        packs.add(new Pack("http://desk.fd.zol-img.com.cn/t_s1024x768c5/g5/M00/0B/0D/ChMkJ1e9jHqIWT4CAA2dKPU9Js8AAUsZgMf8mkADZ1A116.jpg"));
        packs.add(new Pack("http://desk.fd.zol-img.com.cn/t_s1024x768c5/g5/M00/0B/0D/ChMkJle9jIGIMgtdAAYnBOEz3LAAAUsZwPgFgYABicc437.jpg"));
        packs.add(new Pack("http://desk.fd.zol-img.com.cn/t_s1024x768c5/g5/M00/0F/0A/ChMkJleZ8-iIBbFBAAVrdxItOlQAAT76QAFx7oABWuP846.jpg"));
        packs.add(new Pack("http://desk.fd.zol-img.com.cn/t_s1024x768c5/g5/M00/0B/04/ChMkJ1bG5kyIcwkXAAsM0s9DJzoAAKsAwJB9ncACwzq207.jpg"));

        banner.setBannerData(packs);
        banner.setOnItemClickListener(new Banner.OnItemClickListener<Pack>() {
            @Override
            public void onItemClick(int position, Pack item) {
                Toast.makeText(MainActivity.this, "position = " + position, Toast.LENGTH_SHORT).show();
            }
        });
        banner.setOnItemBindListener(new Banner.OnItemBindListener<Pack>() {
            @Override
            public void onItemBind(int position, Pack item, AppCompatImageView view) {
                Glide.with(getApplicationContext()).load(item.getUrl()).into(view);
            }
        });
    }

    private class Pack {

        String url;

        Pack(String url) {
            this.url = url;
        }

        String getUrl() {
            return url;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        banner.setPlaying(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        banner.setPlaying(false);
    }
}
