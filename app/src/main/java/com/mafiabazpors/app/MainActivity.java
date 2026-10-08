package com.mafiabazpors.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout root, content;
    final int BG=Color.rgb(9,9,9), CARD=Color.rgb(22,22,22), GOLD=Color.rgb(212,175,55), WHITE=Color.WHITE, MUTED=Color.rgb(175,175,175);
    ArrayList<String> players=new ArrayList<>(), roles=new ArrayList<>();
    int current=0;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);
        showHome();
    }

    TextView tv(String s,int size,boolean bold){
        TextView t=new TextView(this); t.setText(s); t.setTextColor(WHITE); t.setTextSize(size);
        t.setGravity(Gravity.CENTER); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        t.setPadding(24,18,24,18); return t;
    }
    Button btn(String s){
        Button b=new Button(this); b.setText(s); b.setTextColor(GOLD); b.setTextSize(16); b.setAllCaps(false);
        b.setBackgroundColor(CARD); b.setPadding(12,18,12,18); return b;
    }
    void base(String title){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG); root.setPadding(18,28,18,18);
        TextView h=tv(title,25,true); h.setTextColor(GOLD); root.addView(h,new LinearLayout.LayoutParams(-1,70));
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setGravity(Gravity.CENTER_HORIZONTAL);
        ScrollView sc=new ScrollView(this); sc.addView(content); root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }
    void add(View v){ content.addView(v,new LinearLayout.LayoutParams(-1,LinearLayout.LayoutParams.WRAP_CONTENT)); }
    void showHome(){
        base("مافیا | سناریوی بازپرس");
        add(tv("مدیریت حرفه‌ای بازی مافیا",21,true));
        add(tv("۱۲ یا ۱۳ بازیکن • افشای مخفی نقش • شب و روز • رأی‌گیری",15,false));
        Button n=btn("بازی جدید"); n.setOnClickListener(v->newGame()); add(n);
        Button r=btn("قوانین و نقش‌ها"); r.setOnClickListener(v->rules()); add(r);
    }
    void newGame(){
        base("بازی جدید");
        add(tv("تعداد بازیکنان",19,true));
        Button b12=btn("۱۲ نفر"); b12.setOnClickListener(v->setup(12)); add(b12);
        Button b13=btn("۱۳ نفر"); b13.setOnClickListener(v->setup(13)); add(b13);
        Button back=btn("بازگشت"); back.setOnClickListener(v->showHome()); add(back);
    }
    void setup(int n){
        base("ثبت بازیکنان");
        players.clear(); roles.clear();
        ArrayList<EditText> inputs=new ArrayList<>();
        for(int i=1;i<=n;i++){
            EditText e=new EditText(this); e.setHint("بازیکن "+i); e.setTextColor(WHITE); e.setHintTextColor(MUTED); e.setTextSize(16);
            e.setGravity(Gravity.RIGHT); e.setPadding(20,14,20,14); inputs.add(e); add(e);
        }
        Button start=btn("قرعه‌کشی نقش‌ها و شروع"); start.setOnClickListener(v->{
            players.clear(); for(int i=0;i<inputs.size();i++){String x=inputs.get(i).getText().toString().trim(); players.add(x.isEmpty()?"بازیکن "+(i+1):x);}
            assign(n); reveal(0);
        }); add(start);
    }
    void assign(int n){
        roles.clear();
        String[] base={"پدرخوانده","ناتو","جاسوس","مافیای ساده","دکتر","کارآگاه","بازپرس","ضدگلوله","تک‌تیرانداز","شهروند","شهروند","شهروند","شهروند"};
        ArrayList<String> a=new ArrayList<>(Arrays.asList(base).subList(0,n));
        Collections.shuffle(a); roles.addAll(a);
    }
    void reveal(int i){
        current=i; base("افشای مخفی نقش");
        add(tv("گوشی را به «"+players.get(i)+"» بدهید",21,true));
        add(tv("هیچ بازیکن دیگری صفحه را نبیند.",16,false));
        Button show=btn("نمایش نقش"); show.setOnClickListener(v->{
            base("نقش مخفی");
            add(tv(roles.get(current),30,true));
            if(roles.get(current).contains("مافیا")||roles.get(current).equals("ناتو")||roles.get(current).equals("جاسوس")){
                StringBuilder s=new StringBuilder("هم‌تیمی‌های مافیا:\n");
                for(int k=0;k<roles.size();k++) if(k!=current && (roles.get(k).contains("مافیا")||roles.get(k).equals("ناتو")||roles.get(k).equals("جاسوس"))) s.append(players.get(k)).append("\n");
                add(tv(s.toString(),17,false));
            }
            Button hide=btn("پنهان کن و نفر بعد"); hide.setOnClickListener(x->{ if(current+1<players.size()) reveal(current+1); else game(); }); add(hide);
        }); add(show);
    }
    void game(){
        base("کنترل بازی");
        add(tv("بازی آماده است",24,true));
        add(tv("شب: اجرای توانایی‌های نقش‌ها\nروز: اعلام نتایج، مذاکره، بازپرسی و رأی‌گیری",17,false));
        Button night=btn("شروع شب"); night.setOnClickListener(v->night()); add(night);
        Button day=btn("شروع روز"); day.setOnClickListener(v->day()); add(day);
        Button reset=btn("شروع مجدد"); reset.setOnClickListener(v->showHome()); add(reset);
    }
    void night(){
        base("شب");
        add(tv("ترتیب پیشنهادی شب",21,true));
        String[] stages={"پدرخوانده / مافیا — انتخاب هدف","دکتر — انتخاب نجات","کارآگاه — استعلام","بازپرس — اجرای بازپرسی","تک‌تیرانداز — شلیک","جاسوس / ناتو — توانایی ویژه"};
        for(String s:stages)add(tv("• "+s,17,false));
        Button done=btn("پایان شب و ورود به روز"); done.setOnClickListener(v->day()); add(done);
    }
    void day(){
        base("روز");
        add(tv("فاز روز",24,true));
        add(tv("بحث، مذاکره، بازپرسی دو نفره و رأی‌گیری",17,false));
        Button q=btn("بازپرسی دو نفره"); q.setOnClickListener(v->interrogation()); add(q);
        Button vote=btn("رأی‌گیری"); vote.setOnClickListener(v->vote()); add(vote);
        Button back=btn("بازگشت به کنترل بازی"); back.setOnClickListener(v->game()); add(back);
    }
    void interrogation(){
        base("بازپرسی دو نفره");
        add(tv("دو بازیکن را برای بازپرسی انتخاب کنید",19,true));
        for(String p:players){Button b=btn(p); b.setOnClickListener(v->Toast.makeText(this,"انتخاب شد: "+p,Toast.LENGTH_SHORT).show()); add(b);}
        Button back=btn("پایان بازپرسی"); back.setOnClickListener(v->day()); add(back);
    }
    void vote(){
        base("رأی‌گیری");
        add(tv("بازیکنی که بیشترین رأی را دارد مشخص کنید.",19,true));
        for(String p:players){Button b=btn(p); b.setOnClickListener(v->Toast.makeText(this,"رأی ثبت شد برای "+p,Toast.LENGTH_SHORT).show()); add(b);}
        Button back=btn("پایان رأی‌گیری"); back.setOnClickListener(v->day()); add(back);
    }
    void rules(){
        base("قوانین و نقش‌ها");
        String s="پدرخوانده: رهبر تیم مافیا.\n\nناتو: نقش مافیایی با توانایی ویژه سناریو.\n\nجاسوس: عضو تیم مافیا با توانایی اطلاعاتی.\n\nمافیای ساده: عضو عادی تیم مافیا.\n\nدکتر: هر شب یک بازیکن را نجات می‌دهد.\n\nکارآگاه: درباره یک بازیکن استعلام می‌گیرد.\n\nبازپرس: مکانیزم بازپرسی و رأی ویژه را مدیریت می‌کند.\n\nضدگلوله: در برابر شلیک عادی مقاومت دارد.\n\nتک‌تیرانداز: توانایی شلیک ویژه دارد.\n\nشهروند: هدف اصلی کشف و حذف تیم مافیاست.";
        add(tv(s,16,false));
        Button b=btn("بازگشت"); b.setOnClickListener(v->showHome()); add(b);
    }
}
