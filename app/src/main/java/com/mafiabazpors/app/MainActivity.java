package com.mafiabazpors.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private static final int BG = Color.rgb(5, 6, 9);
    private static final int SURFACE = Color.rgb(16, 17, 22);
    private static final int SURFACE_2 = Color.rgb(22, 23, 30);
    private static final int GOLD = Color.rgb(216, 174, 62);
    private static final int GOLD_SOFT = Color.rgb(245, 214, 132);
    private static final int WHITE = Color.rgb(247, 246, 242);
    private static final int MUTED = Color.rgb(162, 164, 173);
    private static final int RED = Color.rgb(205, 70, 70);
    private static final int GREEN = Color.rgb(73, 179, 117);

    private LinearLayout content;
    private SharedPreferences prefs;

    private final ArrayList<Player> players = new ArrayList<>();
    private final HashMap<String, String> nightActions = new HashMap<>();
    private final HashMap<String, Integer> votes = new HashMap<>();
    private final HashSet<String> interrogationPair = new HashSet<>();

    private int dayNumber = 1;
    private String phase = "setup";
    private int revealIndex = 0;

    private static class Player {
        String name;
        String role;
        boolean alive = true;

        Player(String name, String role) {
            this.name = name;
            this.role = role;
        }
    }

    private static class RoleInfo {
        final String name;
        final String team;
        final String description;
        final int image;

        RoleInfo(String name, String team, String description, int image) {
            this.name = name;
            this.team = team;
            this.description = description;
            this.image = image;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("mafia_state", MODE_PRIVATE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );
        showHome();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable gradient(int start, int end, float radiusDp) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{start, end}
        );
        g.setCornerRadius(dp((int) radiusDp));
        g.setStroke(dp(1), Color.argb(35, 255, 255, 255));
        return g;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(size);
        t.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        t.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setPadding(dp(8), dp(6), dp(8), dp(6));
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        return t;
    }

    private TextView centered(String value, float size, int color, boolean bold) {
        TextView t = text(value, size, color, bold);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private View spacer(int height) {
        View s = new View(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height)));
        return s;
    }

    private Button actionButton(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(15);
        b.setTextColor(primary ? BG : GOLD_SOFT);
        b.setAllCaps(false);
        b.setTypeface(Typeface.create("sans", Typeface.BOLD));
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(8), dp(4), dp(8), dp(4));
        b.setBackground(primary
                ? gradient(GOLD_SOFT, GOLD, 18)
                : gradient(SURFACE_2, SURFACE, 18));
        b.setMinHeight(dp(52));
        b.setOnTouchListener((v, e) -> {
            if (e.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(.985f).scaleY(.985f).setDuration(90).start();
            } else if (e.getAction() == android.view.MotionEvent.ACTION_UP
                    || e.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
            }
            return false;
        });
        return b;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        c.setBackground(gradient(SURFACE_2, SURFACE, 24));
        return c;
    }

    private void add(View view) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.bottomMargin = dp(12);
        content.addView(view, lp);
        view.setAlpha(0f);
        view.setTranslationY(dp(12));
        view.animate().alpha(1f).translationY(0f)
                .setDuration(320)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void base(String title, String eyebrow) {
        FrameLayout shell = new FrameLayout(this);
        shell.setBackground(gradient(BG, Color.rgb(10, 10, 15), 0));

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(dp(16), dp(18), dp(16), dp(14));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setGravity(Gravity.RIGHT);

        labels.addView(text(eyebrow.toUpperCase(), 10, GOLD, true));
        labels.addView(text(title, 25, WHITE, true));

        TextView mark = centered("♠", 28, GOLD, true);
        mark.setBackground(gradient(Color.rgb(30, 27, 15), Color.rgb(15, 15, 18), 20));

        top.addView(labels, new LinearLayout.LayoutParams(0, dp(66), 1f));
        LinearLayout.LayoutParams markLp = new LinearLayout.LayoutParams(dp(64), dp(64));
        markLp.setMargins(dp(12), 0, 0, 0);
        top.addView(mark, markLp);
        outer.addView(top);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(10), 0, dp(24));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        outer.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ));

        shell.addView(outer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setContentView(shell);
    }

    private void showHome() {
        phase = "home";
        base("مافیا | بازپرس", "GAME MASTER CONSOLE");

        LinearLayout hero = card();
        hero.addView(centered("سناریوی بازپرس", 28, GOLD_SOFT, true));
        hero.addView(spacer(8));
        hero.addView(centered(
                "کنسول مدیریت بازی برای ۱۲ تا ۱۳ بازیکن\nطراحی شده برای اجرای آفلاین، سریع و بدون لو رفتن نقش",
                14, MUTED, false
        ));
        add(hero);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        String[][] statData = {
                {"۱۲–۱۳", "بازیکن"},
                {"۲", "فاز"},
                {"۱۰+", "نقش"}
        };
        for (String[] item : statData) {
            LinearLayout pill = card();
            pill.setGravity(Gravity.CENTER);
            pill.addView(centered(item[0], 18, GOLD_SOFT, true));
            pill.addView(centered(item[1], 11, MUTED, false));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(84), 1f);
            lp.setMargins(dp(4), 0, dp(4), 0);
            stats.addView(pill, lp);
        }
        add(stats);

        Button newGame = actionButton("شروع بازی جدید", true);
        newGame.setOnClickListener(v -> newGame());
        add(newGame);

        if (hasSavedGame()) {
            Button resume = actionButton("ادامه بازی ذخیره‌شده", false);
            resume.setOnClickListener(v -> loadGameAndOpen());
            add(resume);
        }

        Button roles = actionButton("کتابخانه نقش‌ها", false);
        roles.setOnClickListener(v -> rules());
        add(roles);

        LinearLayout note = card();
        note.addView(text("حریم بازی", 14, GOLD_SOFT, true));
        note.addView(text(
                "محافظت از اسکرین‌کپچر فعال است. نقش‌ها در مرحله افشا فقط برای همان بازیکن نمایش داده می‌شوند.",
                12, MUTED, false
        ));
        add(note);
    }

    private void newGame() {
        clearSavedGame();
        phase = "setup";
        base("بازی جدید", "NEW SESSION");

        LinearLayout chooser = card();
        chooser.addView(centered("تعداد بازیکنان", 20, WHITE, true));
        chooser.addView(spacer(8));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        Button b12 = actionButton("۱۲ نفر", true);
        Button b13 = actionButton("۱۳ نفر", false);
        b12.setOnClickListener(v -> setupPlayers(12));
        b13.setOnClickListener(v -> setupPlayers(13));

        row.addView(b12, new LinearLayout.LayoutParams(0, dp(58), 1f));
        LinearLayout.LayoutParams b13lp = new LinearLayout.LayoutParams(0, dp(58), 1f);
        b13lp.setMargins(dp(10), 0, 0, 0);
        row.addView(b13, b13lp);
        chooser.addView(row);
        add(chooser);

        LinearLayout roster = card();
        roster.addView(text("ترکیب پیشنهادی سناریو", 15, GOLD_SOFT, true));
        roster.addView(text(
                "پدرخوانده، ناتو، جاسوس، مافیای ساده، دکتر، کارآگاه، بازپرس، ضدگلوله، تک‌تیرانداز و شهروندها.",
                12, MUTED, false
        ));
        add(roster);

        Button back = actionButton("بازگشت", false);
        back.setOnClickListener(v -> showHome());
        add(back);
    }

    private void setupPlayers(int count) {
        base("ثبت بازیکنان", "PLAYER ROSTER");
        ArrayList<EditText> inputs = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            LinearLayout row = card();

            TextView index = centered(String.valueOf(i), 14, GOLD, true);
            EditText e = new EditText(this);
            e.setSingleLine(true);
            e.setHint("نام بازیکن " + i);
            e.setTextColor(WHITE);
            e.setHintTextColor(MUTED);
            e.setTextSize(15);
            e.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            e.setPadding(dp(10), 0, dp(10), 0);
            inputs.add(e);

            row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(index, new LinearLayout.LayoutParams(dp(42), dp(52)));
            LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(0, dp(52), 1f);
            ep.setMargins(dp(8), 0, 0, 0);
            row.addView(e, ep);
            add(row);
        }

        Button start = actionButton("ساخت بازی و قرعه‌کشی نقش‌ها", true);
        start.setOnClickListener(v -> {
            ArrayList<String> names = new ArrayList<>();
            HashSet<String> uniqueNames = new HashSet<>();

            for (int i = 0; i < inputs.size(); i++) {
                String name = inputs.get(i).getText().toString().trim();
                name = name.replaceAll("\\s+", " ");
                if (name.isEmpty()) name = "بازیکن " + (i + 1);

                String key = name;
                if (!uniqueNames.add(key)) {
                    Toast.makeText(
                            this,
                            "نام بازیکن «" + name + "» تکراری است.",
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }
                names.add(name);
            }

            players.clear();
            for (String name : names) {
                players.add(new Player(name, ""));
            }

            assignRoles(count);
            revealIndex = 0;
            dayNumber = 1;
            phase = "reveal";
            nightActions.clear();
            votes.clear();
            interrogationPair.clear();
            saveGame();
            revealRole(0);
        });
        add(start);
    }

    private void assignRoles(int count) {
        ArrayList<String> pool = new ArrayList<>();
        Collections.addAll(pool,
                "پدرخوانده", "ناتو", "جاسوس", "مافیای ساده",
                "دکتر", "کارآگاه", "بازپرس", "ضدگلوله", "تک‌تیرانداز"
        );
        while (pool.size() < count) pool.add("شهروند");
        Collections.shuffle(pool);

        for (int i = 0; i < players.size(); i++) {
            players.get(i).role = pool.get(i);
            players.get(i).alive = true;
        }
    }

    private void revealRole(int index) {
        if (players.isEmpty() || index < 0 || index >= players.size()) {
            showHome();
            return;
        }

        revealIndex = index;
        phase = "reveal";
        saveGame();
        Player p = players.get(index);

        base("افشای مخفی", "PRIVATE ROLE REVEAL");

        LinearLayout intro = card();
        intro.addView(centered("اکنون گوشی دست این بازیکن است:", 13, MUTED, false));
        intro.addView(centered(p.name, 24, GOLD_SOFT, true));
        intro.addView(centered("صفحه را به هیچ بازیکن دیگری نشان ندهید.", 11, RED, true));
        add(intro);

        FrameLayout roleFrame = new FrameLayout(this);
        roleFrame.setBackground(gradient(
                Color.rgb(25, 23, 16), Color.rgb(10, 11, 15), 30
        ));

        ImageView roleImage = new ImageView(this);
        roleImage.setImageResource(R.drawable.role_card_back);
        roleImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        roleFrame.addView(roleImage, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(280)
        ));
        add(roleFrame);

        TextView hint = centered("برای دیدن نقش، دکمه را بزنید", 13, MUTED, false);
        add(hint);

        Button reveal = actionButton("نمایش نقش", true);
        reveal.setOnClickListener(v -> {
            RoleInfo info = roleInfo(p.role);
            roleImage.setImageResource(info.image);
            roleImage.setContentDescription("تصویر نقش " + info.name);
            roleImage.setScaleX(0.15f);
            roleImage.setAlpha(0.1f);
            roleImage.animate()
                    .scaleX(1f).alpha(1f)
                    .setDuration(480)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();

            hint.setText(info.team + " • " + info.description);

            LinearLayout details = card();
            details.addView(centered(info.name, 30, GOLD_SOFT, true));
            details.addView(centered(
                    info.team,
                    13,
                    info.team.contains("مافیا") ? RED : GREEN,
                    true
            ));
            details.addView(text(info.description, 13, WHITE, false));

            if (isMafiaRole(p.role)) {
                details.addView(spacer(8));
                details.addView(text("هم‌تیمی‌های قابل مشاهده", 13, GOLD, true));
                details.addView(text(mafiaTeammates(p.name), 13, MUTED, false));
            }

            add(details);
            reveal.setVisibility(View.GONE);

            Button next = actionButton(
                    index + 1 < players.size()
                            ? "پنهان کن و نفر بعد"
                            : "پنهان کن و ورود به کنترل بازی",
                    false
            );
            next.setOnClickListener(x -> {
                if (index + 1 < players.size()) {
                    revealRole(index + 1);
                } else {
                    phase = "game";
                    saveGame();
                    game();
                }
            });
            add(next);
        });
        add(reveal);
    }

    private String mafiaTeammates(String currentName) {
        StringBuilder out = new StringBuilder();
        for (Player x : players) {
            if (!x.name.equals(currentName) && isMafiaRole(x.role)) {
                if (out.length() > 0) out.append("، ");
                out.append(x.name);
            }
        }
        return out.length() == 0 ? "تنها عضو تیم مافیا" : out.toString();
    }

    private boolean isMafiaRole(String role) {
        return role != null && (
                role.equals("پدرخوانده")
                        || role.equals("ناتو")
                        || role.equals("جاسوس")
                        || role.equals("مافیای ساده")
        );
    }

    private boolean hasAliveRole(String role) {
        if (role == null) return false;
        for (Player p : players) {
            if (p.alive && role.equals(p.role)) return true;
        }
        return false;
    }

    private boolean isNightActionAvailable(String action) {
        if (action.equals("قتل مافیا")) {
            return hasAliveRole("پدرخوانده")
                    || hasAliveRole("ناتو")
                    || hasAliveRole("جاسوس")
                    || hasAliveRole("مافیای ساده");
        }
        if (action.equals("نجات دکتر")) return hasAliveRole("دکتر");
        if (action.equals("استعلام")) return hasAliveRole("کارآگاه");
        if (action.equals("بازپرسی")) return hasAliveRole("بازپرس");
        if (action.equals("شلیک تک‌تیرانداز")) return hasAliveRole("تک‌تیرانداز");
        return false;
    }

    private RoleInfo roleInfo(String role) {
        if (role == null || role.trim().isEmpty()) role = "شهروند";

        if (role.equals("پدرخوانده"))
            return new RoleInfo(role, "تیم مافیا",
                    "رهبر تیم مافیا و تصمیم‌گیر اصلی در حذف شب.",
                    R.drawable.role_godfather);
        if (role.equals("ناتو"))
            return new RoleInfo(role, "تیم مافیا",
                    "عضو مافیا با توانایی ویژه سناریو که اجرای دقیق آن بر عهده گرداننده است.",
                    R.drawable.role_nato);
        if (role.equals("جاسوس"))
            return new RoleInfo(role, "تیم مافیا",
                    "عضو اطلاعاتی تیم مافیا؛ وظایف ویژه را گرداننده ثبت می‌کند.",
                    R.drawable.role_spy);
        if (role.equals("مافیای ساده"))
            return new RoleInfo(role, "تیم مافیا",
                    "عضو عادی تیم مافیا و همراه تصمیم شبانه تیم.",
                    R.drawable.role_mafia);
        if (role.equals("دکتر"))
            return new RoleInfo(role, "تیم شهروند",
                    "هر شب می‌تواند یک بازیکن را برای نجات انتخاب کند.",
                    R.drawable.role_doctor);
        if (role.equals("کارآگاه"))
            return new RoleInfo(role, "تیم شهروند",
                    "با استعلام، هویت تیمی یک بازیکن را برای گرداننده مشخص می‌کند.",
                    R.drawable.role_detective);
        if (role.equals("بازپرس"))
            return new RoleInfo(role, "تیم شهروند",
                    "مجری مکانیزم بازپرسی و رأی ویژه در سناریو.",
                    R.drawable.role_investigator);
        if (role.equals("ضدگلوله"))
            return new RoleInfo(role, "تیم شهروند",
                    "در برابر شلیک معمولی مقاومت دارد؛ نتیجه نهایی با منطق سناریو ثبت می‌شود.",
                    R.drawable.role_bulletproof);
        if (role.equals("تک‌تیرانداز"))
            return new RoleInfo(role, "تیم شهروند",
                    "شلیک ویژه دارد و هدف آن توسط گرداننده ثبت می‌شود.",
                    R.drawable.role_sniper);
        return new RoleInfo("شهروند", "تیم شهروند",
                "قدرت ویژه ندارد و با تحلیل و رأی‌گیری به کشف مافیا کمک می‌کند.",
                R.drawable.role_citizen);
    }

    private void game() {
        phase = "game";
        saveGame();
        base("کنترل بازی", "LIVE TABLE");
        add(statusCard());

        if (!players.isEmpty() && !winState().equals("بازی ادامه دارد")) {
            LinearLayout end = card();
            end.addView(centered("بازی به پایان رسیده است", 21, GOLD_SOFT, true));
            end.addView(centered(winState(), 20,
                    winState().contains("مافیا") ? RED : GREEN, true));
            end.addView(text(
                    "برای اجرای بازی جدید، وضعیت ذخیره‌شده را پاک کنید.",
                    12, MUTED, false
            ));
            add(end);

            Button resetFinished = actionButton("بازی جدید", true);
            resetFinished.setOnClickListener(v -> newGame());
            add(resetFinished);

            Button rolesFinished = actionButton("کتابخانه نقش‌ها", false);
            rolesFinished.setOnClickListener(v -> rules());
            add(rolesFinished);
            return;
        }

        Button night = actionButton("شروع فاز شب", true);
        night.setOnClickListener(v -> startNight());
        add(night);

        Button day = actionButton("شروع فاز روز", false);
        day.setOnClickListener(v -> day());
        add(day);

        Button guide = actionButton("کتابخانه نقش‌ها", false);
        guide.setOnClickListener(v -> rules());
        add(guide);

        Button reset = actionButton("حذف بازی ذخیره‌شده", false);
        reset.setOnClickListener(v -> {
            clearSavedGame();
            showHome();
        });
        add(reset);
    }

    private LinearLayout statusCard() {
        LinearLayout c = card();
        String phaseTitle =
                phase.equals("night") ? "شب" :
                phase.equals("day") ? "روز" :
                phase.equals("voting") ? "رأی‌گیری" : "آماده";
        c.addView(centered(
                "فاز فعلی: " + phaseTitle + " • روز " + dayNumber,
                20, GOLD_SOFT, true
        ));
        c.addView(centered(
                "بازیکنان زنده: " + aliveCount() + " / " + players.size(),
                13, MUTED, false
        ));
        c.addView(centered(winState(), 12, WHITE, true));
        return c;
    }

    private String winState() {
        int mafia = 0;
        int citizens = 0;
        for (Player p : players) {
            if (!p.alive) continue;
            if (isMafiaRole(p.role)) mafia++;
            else citizens++;
        }

        if (players.isEmpty()) return "بازی ادامه دارد";
        if (mafia == 0 && citizens == 0) return "پایان همزمان؛ هیچ تیمی باقی نمانده است";
        if (mafia == 0) return "پیروزی شهروندان";
        if (citizens == 0 || mafia >= citizens) return "پیروزی مافیا";
        return "بازی ادامه دارد";
    }

    private int aliveCount() {
        int count = 0;
        for (Player p : players) if (p.alive) count++;
        return count;
    }

    private void startNight() {
        if (players.isEmpty() || !winState().equals("بازی ادامه دارد")) {
            game();
            return;
        }
        phase = "night";
        nightActions.clear();
        votes.clear();
        saveGame();
        night();
    }

    private void night() {
        if (players.isEmpty() || !winState().equals("بازی ادامه دارد")) {
            game();
            return;
        }

        phase = "night";
        saveGame();

        base("فاز شب", "NIGHT ENGINE");
        add(statusCard());

        if (hasAliveRole("پدرخوانده") || hasAliveRole("ناتو")
                || hasAliveRole("جاسوس") || hasAliveRole("مافیای ساده")) {
            add(actionCard("مافیا", "انتخاب هدف حذف شب", "ثبت هدف",
                    v -> chooseNightTarget("قتل مافیا")));
        }
        if (hasAliveRole("دکتر")) {
            add(actionCard("دکتر", "انتخاب بازیکن برای نجات", "ثبت نجات",
                    v -> chooseNightTarget("نجات دکتر")));
        }
        if (hasAliveRole("کارآگاه")) {
            add(actionCard("کارآگاه", "استعلام تیمی یک بازیکن", "استعلام",
                    v -> chooseInvestigation()));
        }
        if (hasAliveRole("بازپرس")) {
            add(actionCard("بازپرس", "ثبت بازپرسی ویژه سناریو", "بازپرسی",
                    v -> interrogation(true)));
        }
        if (hasAliveRole("تک‌تیرانداز")) {
            add(actionCard("تک‌تیرانداز", "ثبت شلیک ویژه", "انتخاب هدف",
                    v -> chooseNightTarget("شلیک تک‌تیرانداز")));
        }

        LinearLayout summary = card();
        summary.addView(text("اقدامات ثبت‌شده امشب", 14, GOLD_SOFT, true));
        summary.addView(text(nightActionsSummary(), 12, MUTED, false));
        add(summary);

        Button finish = actionButton("پایان شب و ورود به روز", true);
        finish.setOnClickListener(v -> finishNight());
        add(finish);

        Button back = actionButton("بازگشت به کنترل بازی", false);
        back.setOnClickListener(v -> game());
        add(back);
    }

    private LinearLayout actionCard(
            String title,
            String desc,
            String buttonText,
            View.OnClickListener listener
    ) {
        LinearLayout c = card();
        c.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title, 16, GOLD_SOFT, true));
        labels.addView(text(desc, 11, MUTED, false));

        Button b = actionButton(buttonText, false);
        b.setOnClickListener(listener);

        c.addView(labels, new LinearLayout.LayoutParams(0, dp(70), 1f));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(112), dp(60));
        bp.setMargins(dp(8), 0, 0, 0);
        c.addView(b, bp);
        return c;
    }

    private String nightActionsSummary() {
        if (nightActions.isEmpty()) return "هنوز اقدامی ثبت نشده است.";
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> e : nightActions.entrySet()) {
            if (out.length() > 0) out.append("\n");
            out.append("• ").append(e.getKey()).append(" ← ").append(e.getValue());
        }
        return out.toString();
    }

    private void chooseNightTarget(String action) {
        if (!phase.equals("night") || !isNightActionAvailable(action)) {
            night();
            return;
        }

        base("انتخاب هدف", "NIGHT ACTION");
        add(centered(action, 20, GOLD_SOFT, true));

        boolean hasTarget = false;
        for (Player p : players) {
            if (!p.alive) continue;
            if (action.equals("قتل مافیا") && isMafiaRole(p.role)) continue;

            hasTarget = true;
            Button target = actionButton(p.name, false);
            target.setOnClickListener(v -> {
                nightActions.put(action, p.name);
                saveGame();
                Toast.makeText(this,
                        "ثبت شد: " + p.name,
                        Toast.LENGTH_SHORT
                ).show();
                night();
            });
            add(target);
        }

        if (!hasTarget) {
            LinearLayout empty = card();
            empty.addView(centered(
                    "هدف مجازی برای این اقدام وجود ندارد.",
                    16, MUTED, true
            ));
            add(empty);
        }

        Button back = actionButton("بازگشت به فاز شب", false);
        back.setOnClickListener(v -> night());
        add(back);
    }

    private void chooseInvestigation() {
        if (!phase.equals("night") || !hasAliveRole("کارآگاه")) {
            night();
            return;
        }

        base("استعلام کارآگاه", "PRIVATE INVESTIGATION");
        add(centered("بازیکن مورد نظر را انتخاب کنید", 18, GOLD_SOFT, true));

        for (Player p : players) {
            if (!p.alive) continue;
            Button target = actionButton(p.name, false);
            target.setOnClickListener(v -> {
                boolean mafia = isMafiaRole(p.role);

                LinearLayout result = card();
                result.addView(centered("نتیجه استعلام", 18, GOLD_SOFT, true));
                result.addView(centered(
                        p.name + " → " + (mafia ? "تیم مافیا" : "تیم شهروند"),
                        17, mafia ? RED : GREEN, true
                ));
                result.addView(text(
                        "این نتیجه فقط برای گرداننده بازی نمایش داده می‌شود.",
                        11, MUTED, false
                ));
                add(result);

                Button back = actionButton("بازگشت به فاز شب", false);
                back.setOnClickListener(x -> night());
                add(back);
            });
            add(target);
        }
    }

    private void finishNight() {
        if (!phase.equals("night") || players.isEmpty()) {
            night();
            return;
        }

        String mafiaTarget = nightActions.get("قتل مافیا");
        String doctorTarget = nightActions.get("نجات دکتر");
        String sniperTarget = nightActions.get("شلیک تک‌تیرانداز");

        ArrayList<String> deaths = new ArrayList<>();

        if (mafiaTarget != null && !mafiaTarget.equals(doctorTarget)) {
            Player p = playerByName(mafiaTarget);
            if (p != null && p.alive && !isMafiaRole(p.role)
                    && !p.role.equals("ضدگلوله")) {
                p.alive = false;
                deaths.add(p.name);
            }
        }

        if (sniperTarget != null) {
            Player p = playerByName(sniperTarget);
            if (p != null && p.alive) {
                p.alive = false;
                if (!deaths.contains(p.name)) deaths.add(p.name);
            }
        }

        phase = "day";
        nightActions.clear();
        saveGame();

        base("اعلام نتیجه شب", "NIGHT REPORT");
        LinearLayout report = card();
        report.addView(centered("نتیجه ثبت‌شده", 21, GOLD_SOFT, true));

        if (deaths.isEmpty()) {
            report.addView(centered(
                    "امشب مرگ ثبت نشده است.", 17, GREEN, true
            ));
        } else {
            report.addView(text("بازیکنان حذف‌شده:", 13, WHITE, true));
            for (String death : deaths) {
                report.addView(text("• " + death, 15, RED, true));
            }
        }
        add(report);

        String state = winState();
        if (!state.equals("بازی ادامه دارد")) {
            addCenteredWin(state);
            Button finish = actionButton("پایان بازی", true);
            finish.setOnClickListener(v -> game());
            add(finish);
        } else {
            Button toDay = actionButton("ورود به فاز روز", true);
            toDay.setOnClickListener(v -> day());
            add(toDay);
        }
    }

    private void day() {
        phase = "day";
        saveGame();

        if (!players.isEmpty() && !winState().equals("بازی ادامه دارد")) {
            game();
            return;
        }

        base("فاز روز", "DAY ENGINE");
        add(statusCard());

        if (hasAliveRole("بازپرس")) {
            Button interrogation = actionButton("بازپرسی دو نفره", true);
            interrogation.setOnClickListener(v -> interrogation(false));
            add(interrogation);
        }

        Button vote = actionButton("رأی‌گیری و حذف", false);
        vote.setOnClickListener(v -> voting());
        add(vote);

        Button night = actionButton("بازگشت به شب", false);
        night.setOnClickListener(v -> startNight());
        add(night);
    }

    private void interrogation(boolean nightMode) {
        if (!players.isEmpty() && !winState().equals("بازی ادامه دارد")) {
            game();
            return;
        }

        if (nightMode) {
            if (!phase.equals("night") || !hasAliveRole("بازپرس")) {
                night();
                return;
            }
        } else {
            if (!phase.equals("day") || !hasAliveRole("بازپرس")) {
                day();
                return;
            }
        }

        base("بازپرسی", nightMode
                ? "NIGHT INVESTIGATION" : "DAY INTERROGATION");
        add(centered("دو بازیکن را انتخاب کنید", 18, GOLD_SOFT, true));

        interrogationPair.clear();

        for (Player p : players) {
            if (!p.alive) continue;

            CheckBox cb = new CheckBox(this);
            cb.setText(p.name);
            cb.setTextColor(WHITE);
            cb.setTextSize(15);
            cb.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            cb.setButtonTintList(
                    android.content.res.ColorStateList.valueOf(GOLD)
            );

            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (interrogationPair.size() >= 2) {
                        buttonView.setChecked(false);
                        Toast.makeText(this,
                                "فقط دو نفر قابل انتخاب است.",
                                Toast.LENGTH_SHORT
                        ).show();
                    } else {
                        interrogationPair.add(p.name);
                    }
                } else {
                    interrogationPair.remove(p.name);
                }
            });

            LinearLayout row = card();
            row.addView(cb);
            add(row);
        }

        Button execute = actionButton("ثبت بازپرسی ویژه", true);
        execute.setOnClickListener(v -> {
            if (interrogationPair.size() != 2) {
                Toast.makeText(this,
                        "دقیقاً دو بازیکن را انتخاب کنید.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            ArrayList<String> pair = new ArrayList<>(interrogationPair);
            LinearLayout result = card();
            result.addView(centered(
                    "زوج بازپرسی", 18, GOLD_SOFT, true
            ));
            result.addView(centered(
                    pair.get(0) + "  ×  " + pair.get(1),
                    18, WHITE, true
            ));
            result.addView(text(
                    "نتیجه و رأی ویژه این سناریو توسط گرداننده اجرا و در جریان بازی ثبت می‌شود.",
                    12, MUTED, false
            ));
            add(result);
            saveGame();
        });
        add(execute);

        Button back = actionButton("بازگشت", false);
        back.setOnClickListener(v -> {
            if (nightMode) night(); else day();
        });
        add(back);
    }

    private void voting() {
        if (players.isEmpty() || !winState().equals("بازی ادامه دارد")) {
            game();
            return;
        }

        boolean restoring = phase.equals("voting") && !votes.isEmpty();
        if (!restoring) votes.clear();
        phase = "voting";
        saveGame();

        base("رأی‌گیری", "DAY VOTE");
        add(centered(
                "تعداد رأی هر بازیکن را دستی ثبت کنید",
                18, GOLD_SOFT, true
        ));

        for (Player p : players) {
            if (!p.alive) continue;

            LinearLayout row = card();
            row.setOrientation(LinearLayout.HORIZONTAL);

            int initial = votes.containsKey(p.name) ? votes.get(p.name) : 0;
            TextView count = centered(String.valueOf(initial), 20, GOLD_SOFT, true);
            Button minus = actionButton("−", false);
            Button plus = actionButton("+", false);

            votes.put(p.name, initial);

            minus.setOnClickListener(v -> {
                int value = Math.max(0, votes.get(p.name) - 1);
                votes.put(p.name, value);
                count.setText(String.valueOf(value));
                saveGame();
            });

            plus.setOnClickListener(v -> {
                int value = votes.get(p.name) + 1;
                votes.put(p.name, value);
                count.setText(String.valueOf(value));
                saveGame();
            });

            row.addView(
                    text(p.name, 15, WHITE, true),
                    new LinearLayout.LayoutParams(0, dp(56), 1f)
            );
            row.addView(
                    minus,
                    new LinearLayout.LayoutParams(dp(52), dp(56))
            );

            LinearLayout.LayoutParams countLp =
                    new LinearLayout.LayoutParams(dp(54), dp(56));
            countLp.setMargins(dp(6), 0, dp(6), 0);
            row.addView(count, countLp);

            row.addView(
                    plus,
                    new LinearLayout.LayoutParams(dp(52), dp(56))
            );

            add(row);
        }

        Button finish = actionButton("تعیین نتیجه رأی‌گیری", true);
        finish.setOnClickListener(v -> finishVote());
        add(finish);

        Button back = actionButton("بازگشت به روز", false);
        back.setOnClickListener(v -> day());
        add(back);
    }

    private void finishVote() {
        if (!phase.equals("voting") || players.isEmpty()) {
            day();
            return;
        }

        String winner = null;
        int max = 0;
        boolean tie = false;

        for (Map.Entry<String, Integer> entry : votes.entrySet()) {
            Player candidate = playerByName(entry.getKey());
            if (candidate == null || !candidate.alive) continue;

            int count = entry.getValue();

            if (count > max) {
                max = count;
                winner = entry.getKey();
                tie = false;
            } else if (count == max && count > 0) {
                tie = true;
            }
        }

        base("نتیجه رأی‌گیری", "VOTE RESULT");
        LinearLayout result = card();

        if (max == 0) {
            result.addView(centered(
                    "هیچ رأیی ثبت نشده است.", 18, MUTED, true
            ));
        } else if (tie) {
            result.addView(centered(
                    "مساوی شد؛ حذف انجام نشد.", 18, GOLD_SOFT, true
            ));
        } else {
            Player player = playerByName(winner);
            if (player != null) player.alive = false;

            result.addView(centered(
                    "بازیکن حذف‌شده", 13, MUTED, false
            ));
            result.addView(centered(
                    winner, 24, RED, true
            ));

            if (player != null) {
                result.addView(centered(
                        roleInfo(player.role).name, 15, WHITE, true
                ));
            }
        }

        phase = "day";
        votes.clear();
        saveGame();
        add(result);

        String state = winState();
        if (!state.equals("بازی ادامه دارد")) {
            addCenteredWin(state);

            Button finish = actionButton("پایان بازی", true);
            finish.setOnClickListener(v -> game());
            add(finish);
            return;
        }

        Button next = actionButton("شروع شب بعدی", true);
        next.setOnClickListener(v -> {
            dayNumber++;
            startNight();
        });
        add(next);

        Button back = actionButton("بازگشت به روز", false);
        back.setOnClickListener(v -> day());
        add(back);
    }

    private void addCenteredWin(String state) {
        LinearLayout win = card();
        win.addView(centered("پایان بازی", 13, MUTED, false));
        win.addView(centered(state, 24, GOLD_SOFT, true));
        add(win);
    }

    private void rules() {
        base("کتابخانه نقش‌ها", "ROLE LIBRARY");

        for (String role : roleNames()) {
            RoleInfo info = roleInfo(role);

            LinearLayout c = card();
            c.setOrientation(LinearLayout.HORIZONTAL);

            ImageView image = new ImageView(this);
            image.setImageResource(info.image);
            image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            image.setContentDescription("تصویر نقش " + info.name);

            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.VERTICAL);
            labels.addView(text(info.name, 17, GOLD_SOFT, true));
            labels.addView(text(
                    info.team,
                    11,
                    info.team.contains("مافیا") ? RED : GREEN,
                    true
            ));
            labels.addView(text(info.description, 11, MUTED, false));

            c.addView(
                    image,
                    new LinearLayout.LayoutParams(dp(96), dp(96))
            );

            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            );
            labelLp.setMargins(dp(12), 0, 0, 0);
            c.addView(labels, labelLp);

            add(c);
        }

        Button back = actionButton("بازگشت", false);
        back.setOnClickListener(v -> showHome());
        add(back);
    }

    private List<String> roleNames() {
        ArrayList<String> roles = new ArrayList<>();
        Collections.addAll(
                roles,
                "پدرخوانده", "ناتو", "جاسوس", "مافیای ساده",
                "دکتر", "کارآگاه", "بازپرس", "ضدگلوله",
                "تک‌تیرانداز", "شهروند"
        );
        return roles;
    }

    private Player playerByName(String name) {
        if (name == null) return null;
        for (Player p : players) {
            if (p.name.equals(name)) return p;
        }
        return null;
    }

    private boolean hasSavedGame() {
        return prefs != null && prefs.contains("state");
    }

    private void saveGame() {
        try {
            JSONArray array = new JSONArray();

            for (Player p : players) {
                JSONObject obj = new JSONObject();
                obj.put("name", p.name);
                obj.put("role", p.role);
                obj.put("alive", p.alive);
                array.put(obj);
            }

            JSONObject root = new JSONObject();
            root.put("version", 2);
            root.put("players", array);
            root.put("day", dayNumber);
            root.put("phase", phase);
            root.put("revealIndex", revealIndex);

            JSONObject actions = new JSONObject();
            for (Map.Entry<String, String> entry : nightActions.entrySet()) {
                actions.put(entry.getKey(), entry.getValue());
            }
            root.put("nightActions", actions);

            JSONObject savedVotes = new JSONObject();
            for (Map.Entry<String, Integer> entry : votes.entrySet()) {
                savedVotes.put(entry.getKey(), entry.getValue());
            }
            root.put("votes", savedVotes);

            prefs.edit().putString("state", root.toString()).apply();
        } catch (Exception e) {
            Toast.makeText(this, "ذخیره وضعیت بازی ناموفق بود.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadGameAndOpen() {
        try {
            JSONObject root = new JSONObject(
                    prefs.getString("state", "{}")
            );

            players.clear();
            JSONArray array = root.getJSONArray("players");

            if (array.length() != 12 && array.length() != 13) {
                throw new IllegalStateException("invalid player count");
            }

            HashSet<String> uniqueNames = new HashSet<>();
            List<String> knownRoles = roleNames();

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String name = obj.getString("name").trim();
                String role = obj.getString("role").trim();
                if (name.isEmpty() || role.isEmpty()
                        || !uniqueNames.add(name)
                        || !knownRoles.contains(role)) {
                    throw new IllegalStateException("invalid player data");
                }

                Player p = new Player(name, role);
                p.alive = obj.optBoolean("alive", true);
                players.add(p);
            }

            dayNumber = Math.max(1, root.optInt("day", 1));
            phase = root.optString("phase", "game");
            if (!phase.equals("reveal")
                    && !phase.equals("night")
                    && !phase.equals("day")
                    && !phase.equals("voting")
                    && !phase.equals("game")) {
                phase = "game";
            }

            revealIndex = root.optInt("revealIndex", 0);
            if (revealIndex < 0 || revealIndex >= players.size()) {
                revealIndex = 0;
            }

            nightActions.clear();
            JSONObject actions = root.optJSONObject("nightActions");
            if (actions != null) {
                java.util.Iterator<String> keys = actions.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = actions.getString(key);
                    if (value != null && playerByName(value) != null) {
                        nightActions.put(key, value);
                    }
                }
            }

            votes.clear();
            JSONObject savedVotes = root.optJSONObject("votes");
            if (savedVotes != null) {
                java.util.Iterator<String> keys = savedVotes.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Player p = playerByName(key);
                    if (p != null && p.alive) {
                        votes.put(key, Math.max(0, savedVotes.optInt(key, 0)));
                    }
                }
            }

            if (phase.equals("reveal")) {
                revealRole(revealIndex);
            } else if (phase.equals("night")) {
                night();
            } else if (phase.equals("voting")) {
                voting();
            } else if (phase.equals("day")) {
                day();
            } else {
                game();
            }
        } catch (Exception e) {
            clearSavedGame();
            showHome();
            Toast.makeText(this,
                    "بازی ذخیره‌شده معتبر نبود و پاک شد.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void clearSavedGame() {
        prefs.edit().remove("state").apply();
        players.clear();
        nightActions.clear();
        votes.clear();
        interrogationPair.clear();
    }
}
