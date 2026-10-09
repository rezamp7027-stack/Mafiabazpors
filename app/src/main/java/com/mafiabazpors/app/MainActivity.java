package com.mafiabazpors.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final String PREFS = "mafia_local_store_v1";
    private static final String GOLD = "#D8B15B";
    private static final String BACKGROUND = "#080808";
    private static final String PANEL = "#151515";
    private static final String PANEL_2 = "#202020";
    private static final String MUTED = "#A6A6A6";
    private static final String WHITE = "#F4F0E8";
    private static final String RED = "#D96A65";
    private static final String GREEN = "#8BC49A";
    private static final int PICK_EXPORT = 401;
    private static final int PICK_IMPORT = 402;
    private static final int PICK_ROLE_IMAGE = 403;
    private static final int DATA_SCHEMA_VERSION = 2;

    private SharedPreferences preferences;
    private ArrayList<Role> roles = new ArrayList<>();
    private ArrayList<Lineup> lineups = new ArrayList<>();
    private HashMap<String, Integer> builderCounts = new HashMap<>();
    private int playerCount = 10;
    private String currentScreen = "home";
    private String roleSearch = "";
    private String roleFactionFilter = "همه جناح‌ها";
    private String lineupBeingEditedId = null;
    private String activeLineupName = "ترکیب جدید";
    private ArrayList<String> playerNames = new ArrayList<>();
    private ArrayList<Role> dealtDeck = new ArrayList<>();
    private int currentPlayerIndex = 0;
    private boolean cardRevealed = false;
    private boolean dealingPaused = false;
    private boolean roleOnlyExport = false;
    private View dealCardView;
    private boolean animateDealCard = false;
    private ScrollView pageScrollView;
    private String renderedScreen = null;
    private String pendingImageRoleId = null;
    private final HashMap<String, Integer> screenScrollPositions = new HashMap<>();
    private final HashMap<String, Bitmap> portraitCache = new HashMap<>();
    private Bitmap portraitSheet;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.parseColor(BACKGROUND));
        getWindow().setNavigationBarColor(Color.parseColor(BACKGROUND));
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadLocalData();
        ensurePlayerNames();
        render();
    }

    private void loadLocalData() {
        int storedSchema = preferences.getInt("schemaVersion", 0);
        try {
            String rawRoles = preferences.getString("roles", null);
            if (rawRoles != null) {
                JSONArray array = new JSONArray(rawRoles);
                for (int i = 0; i < array.length(); i++) roles.add(Role.fromJson(array.getJSONObject(i)));
            }
            String rawLineups = preferences.getString("lineups", null);
            if (rawLineups != null) {
                JSONArray array = new JSONArray(rawLineups);
                for (int i = 0; i < array.length(); i++) lineups.add(Lineup.fromJson(array.getJSONObject(i)));
            }
            playerCount = Math.max(1, Math.min(GameLogic.SAFE_CARD_LIMIT,
                    preferences.getInt("playerCount", 10)));
        } catch (Exception ignored) {
            roles.clear();
            lineups.clear();
        }

        ArrayList<Role> defaults = defaultRoles();
        if (roles.isEmpty()) {
            roles.addAll(defaults);
        } else if (storedSchema < DATA_SCHEMA_VERSION) {
            // One-time migration: refresh built-in entries and add new scenario roles,
            // without touching user-created roles or their saved lineups.
            for (Role seed : defaults) {
                int found = -1;
                for (int i = 0; i < roles.size(); i++) {
                    if (roles.get(i).id.equals(seed.id)) { found = i; break; }
                }
                if (found >= 0) {
                    seed.imageUri = roles.get(found).imageUri;
                    roles.set(found, seed);
                } else {
                    roles.add(seed);
                }
            }
        }
        preferences.edit().putInt("schemaVersion", DATA_SCHEMA_VERSION).apply();
        saveAll();
    }

    private ArrayList<Role> defaultRoles() {
        ArrayList<Role> result = new ArrayList<>();

        // Role descriptions below follow one published guide to the Bazpors scenario.
        // The scenario is run in different editions; the group’s own rulebook takes precedence.
        result.add(new Role("godfather", "رئیس مافیا", "مافیایی", "رهبر", "◆",
                "رهبر تیم مافیا؛ انتخاب شلیک شب و یک فرصت سوداگری در نسخهٔ مرجع.",
                "هر شب، انتخاب شلیک تیم با رئیس مافیاست. استعلام او برای کارآگاه منفی گزارش می‌شود. در صورت زنده‌بودن رئیس، یک‌بار در بازی می‌تواند سوداگری را اجرا کند: یکی از اعضای مافیا را قربانی و برای جذب شهروند ساده یا رویین‌تن مذاکره کند. مذاکره با نقش‌های دیگر در نسخهٔ مرجع ناموفق است. اسنایپر نمی‌تواند رئیس مافیا را حذف کند.",
                "پیروزی جناح مافیا؛ در راهنمای مرجع، رسیدن تعداد مافیا به تعداد شهروندان شرط برد عنوان شده است.", 1, true, true));
        result.add(new Role("nato", "ناتو", "مافیایی", "نفوذ و حدس نقش", "◆",
                "یک‌بار در بازی می‌تواند نقش دقیق یک بازیکن را حدس بزند.",
                "یک بار در کل بازی، نقش دقیق یک نفر را حدس می‌زند. اگر حدس درست باشد، هدف در روز از بازی خارج می‌شود؛ اگر اشتباه باشد اثری ندارد. در شبی که ناتو از این توانایی استفاده می‌کند، تیم مافیا شلیک شبانه ندارد.",
                "پیروزی با جناح مافیا؛ قواعد استفاده را با نسخهٔ میز خود تطبیق دهید.", 1, true, true));
        result.add(new Role("shayad", "شیاد", "مافیایی", "اختلال استعلام", "◆",
                "نقشی برای هدف‌گرفتن کارآگاه و مخدوش‌کردن استعلام‌ها.",
                "طبق راهنمای مرجع، هدف اصلی شیاد یافتن کارآگاه است. اگر کارآگاه را پیدا کند، استعلام اعضای مافیا برای کارآگاه منفی خواهد شد. راهنما اجازهٔ انتخاب یک هدف در شب و تکرار همان هدف در دو شب پیاپی را ذکر می‌کند.",
                "پیروزی با جناح مافیا؛ جزئیات استعلام ممکن است در نسخه‌های مختلف متفاوت باشد.", 1, true, true));
        result.add(new Role("mafia-simple", "مافیای ساده", "مافیایی", "پایه", "◆",
                "عضو تیم مافیا بدون قابلیت شبانهٔ مستقل.",
                "در شب توانایی ویژهٔ جداگانه‌ای ندارد و در انتخاب شلیک به رئیس مافیا کمک می‌کند. در روز با گفت‌وگو و رأی‌گیری تلاش می‌کند هویت هم‌تیمی‌ها را پنهان نگه دارد.",
                "پیروزی با جناح مافیا؛ طبق راهنمای مرجع، رسیدن شمار مافیا به شمار شهروندان شرط برد است.", 0, true, true));
        result.add(new Role("doctor-template", "پزشک", "شهروندی", "نجات", "✚",
                "هر شب می‌تواند یک بازیکن را از شلیک مافیا نجات دهد.",
                "هر شب یک نفر را انتخاب می‌کند تا در برابر شلیک شبانه نجات یابد. راهنمای مرجع برای کل بازی دو بار اجازهٔ خودنجاتی ذکر می‌کند. پیش از بازی، تعداد و محدودیت خودنجاتی را با نسخهٔ قوانین گروه نهایی کنید.",
                "پیروزی با شهر؛ شناسایی و حذف اعضای مافیا از راه گفت‌وگو و رأی‌گیری.", 1, true, true));
        result.add(new Role("detective-template", "کارآگاه", "شهروندی", "استعلام", "⌕",
                "هر شب استعلام مافیایی‌بودن یک بازیکن را می‌گیرد.",
                "هر شب یک بازیکن را برای استعلام انتخاب می‌کند. رئیس مافیا در راهنمای مرجع همیشه استعلام منفی می‌گیرد؛ اگر شیاد کارآگاه را شناسایی کرده باشد، استعلام اعضای مافیا نیز منفی می‌شود. نتیجهٔ استعلام به‌تنهایی جایگزین بحث و رأی‌گیری نیست.",
                "پیروزی با شهر؛ کمک به شناسایی مافیا بدون آشکارکردن زودهنگام اطلاعات حیاتی.", 1, true, true));
        result.add(new Role("roein-tan", "رویین‌تن", "شهروندی", "مقاومت", "◆",
                "در نسخهٔ مرجع با شلیک شبانهٔ مافیا حذف نمی‌شود.",
                "مصونیت این نقش در راهنمای مرجع مربوط به شلیک مافیا در شب است؛ رأی‌گیری روز همچنان می‌تواند او را از بازی خارج کند. رئیس مافیا می‌تواند رویین‌تن را یکی از اهداف مجاز مذاکره برای سوداگری انتخاب کند.",
                "پیروزی با شهر؛ با مشارکت در استدلال و رأی‌گیری به شناسایی مافیا کمک کنید.", 1, true, true));
        result.add(new Role("sniper", "اسنایپر", "شهروندی", "شلیک محدود", "◆",
                "یک تیر در کل بازی؛ انتخاب اشتباه می‌تواند به حذف خودش منجر شود.",
                "یک بار در طول بازی شلیک می‌کند. طبق راهنمای مرجع، اگر شهروند را به اشتباه هدف بگیرد خودش از بازی خارج می‌شود. هدف‌گرفتن عضو مافیا (به‌جز رئیس مافیا) می‌تواند او را حذف کند، مگر اینکه همان شب پزشک نجاتش داده باشد.",
                "پیروزی با شهر؛ شلیک را با اطلاعات و شواهد کافی انجام دهید.", 1, true, false));
        result.add(new Role("mohaqeq", "محقق", "شهروندی", "پیوند", "◆",
                "شبانه به یک بازیکن پیوند می‌زند؛ حذف بعضی نقش‌های منفی می‌تواند پیامد دوم داشته باشد.",
                "به‌جز شب معارفه، هر شب یک بازیکن را انتخاب می‌کند و به او پیوند می‌زند. طبق راهنمای مرجع، اگر محقق از بازی خارج شود و پیوند آخرش روی ناتو یا شیاد باشد، آن بازیکن هم خارج می‌شود؛ پیوند به رئیس مافیا یا مافیای ساده چنین اثری ندارد. در این راهنما اثر پیوند شب آخر ملاک است.",
                "پیروزی با شهر؛ انتخاب هدف و زمان‌بندی پیوند مهم است.", 1, true, true));
        result.add(new Role("bazpors-template", "بازپرس", "ویژه سناریوی بازپرس", "بازپرسی", "⚖",
                "یک بار در بازی، دو نفر را برای دفاعیهٔ ویژهٔ روز بعد انتخاب می‌کند.",
                "طبق راهنمای مرجع، بازپرس یک‌بار دو بازیکن را برای بازپرسی انتخاب می‌کند. اگر هر دو تا صبح در بازی بمانند، هر کدام دو نوبت ۳۰ ثانیه‌ای برای دفاع دارند؛ سپس بازپرس می‌تواند روند رأی‌گیری بین آن دو را ادامه دهد یا لغو کند. با ادامه‌دادن، همه باید به یکی از آن دو رأی دهند و فرد دارای رأی بیشتر با اعلام نقش خارج می‌شود. لغو یا رأی برابر باعث ماندن هر دو می‌شود. اگر یکی از دو هدف همان شب حذف شود، توانایی در راهنمای مرجع بازمی‌گردد؛ حتی حذف‌شدن خود بازپرس نیز الزاماً این روند را لغو نمی‌کند.",
                "پیروزی با شهر؛ اجرای دقیق ترتیب دفاع و رأی‌گیری باید توسط گرداننده انجام شود.", 1, true, false));
        result.add(new Role("citizen-simple", "شهروند ساده", "شهروندی", "پایه", "●",
                "بدون عمل شبانه؛ قدرت اصلی از تحلیل، گفت‌وگو و رأی‌گیری می‌آید.",
                "در شب قابلیت مستقلی ندارد. در روز اطلاعات گفت‌وگوها را تحلیل می‌کند، تناقض‌ها را می‌سنجد، اتهام و دفاع را ارزیابی می‌کند و رأی می‌دهد.",
                "پیروزی با شهر؛ جناح مافیا را شناسایی کنید تا توان آن برای پیروزی از بین برود.", 0, true, true));

        result.add(new Role("lawyer-variant", "وکیل (نسخهٔ جایگزین)", "قابل تنظیم", "نقش نسخه‌های دیگر", "◆",
                "این نقش در برخی خلاصه‌های نسخهٔ تلویزیونی ذکر شده، اما قانونش میان منابع یکسان نیست.",
                "قانون دقیق وکیل در مرجع متنی استفاده‌شده برای این نسخه مشخص نشده است. پیش از پخش، قابلیت و محدودیت نسخهٔ میز خود را در همین صفحه ثبت کنید.",
                "طبق جناح و قانون ثبت‌شده توسط مدیر بازی.", 0, true, true));
        result.add(new Role("hunter-variant", "شکارچی (نسخهٔ جایگزین)", "قابل تنظیم", "نقش نسخه‌های دیگر", "◆",
                "در بعضی فهرست‌های آموزشی به‌عنوان نقش افزوده ذکر می‌شود؛ قانون قطعی در این نسخه پیش‌فرض نشده است.",
                "قابلیت و محدودیت شکارچی به نسخهٔ سناریو بستگی دارد. متن دقیق قوانین مورد استفادهٔ گروه را از مدیریت نقش وارد کنید.",
                "طبق جناح و قانون ثبت‌شده توسط مدیر بازی.", 0, true, true));
        result.add(new Role("independent-template", "نقش مستقل", "مستقل و خنثی", "مستقل", "◇",
                "قالبی برای نقش مستقل یا خنثی سفارشی.",
                "قابلیت، محدودیت و اهداف این نقش باید به‌صورت سفارشی توسط مدیر تعریف شود؛ قانون رسمی از پیش فرض نشده است.",
                "شرایط پیروزی مستقل را مدیر بازی مشخص کند.", 0, true, true));
        result.add(new Role("custom-template", "قالب نقش سفارشی", "قابل تنظیم", "سفارشی", "✦",
                "قالب آماده برای افزودن نقش جدید؛ پیش از استفاده متن آن را تکمیل کنید.",
                "قابلیت این نقش هنوز تعریف نشده است. از بخش مدیریت نقش‌ها، قابلیت، جناح و محدودیت‌ها را وارد کنید.",
                "شرایط پیروزی هنوز تعریف نشده است.", 0, false, true));
        return result;
    }

    private void saveAll() {
        try {
            JSONArray roleArray = new JSONArray();
            for (Role role : roles) roleArray.put(role.toJson());
            JSONArray lineupArray = new JSONArray();
            for (Lineup lineup : lineups) lineupArray.put(lineup.toJson());
            preferences.edit().putString("roles", roleArray.toString())
                    .putString("lineups", lineupArray.toString())
                    .putInt("playerCount", playerCount)
                    .putInt("schemaVersion", DATA_SCHEMA_VERSION).apply();
        } catch (JSONException e) {
            toast("ذخیره‌سازی انجام نشد: داده‌ها قابل تبدیل نیستند.");
        }
    }

    private void render() {
        if (pageScrollView != null && renderedScreen != null) {
            screenScrollPositions.put(renderedScreen, pageScrollView.getScrollY());
        }
        final int restoreScrollY = Math.max(0, screenScrollPositions.getOrDefault(currentScreen, 0));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(Color.parseColor(BACKGROUND));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(15), dp(20), dp(10));
        TextView brand = text("مافیا  /  بازپرس", 21, GOLD, true);
        header.addView(brand);
        TextView tagline = text("پخش محرمانهٔ نقش‌ها  •  آفلاین  •  فارسی", 12, MUTED, false);
        setTopMargin(tagline, 3);
        header.addView(tagline);
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        if (!"deal".equals(currentScreen) && !"moderator".equals(currentScreen)) {
            HorizontalScrollView navScroll = new HorizontalScrollView(this);
            navScroll.setHorizontalScrollBarEnabled(false);
            LinearLayout nav = new LinearLayout(this);
            nav.setPadding(dp(12), dp(4), dp(12), dp(10));
            String[][] items = {
                    {"خانه", "home"}, {"ساخت ترکیب", "builder"}, {"بانک نقش‌ها", "bank"},
                    {"مدیریت نقش", "roles"}, {"تنظیمات", "settings"}
            };
            for (String[] item : items) {
                Button b = button(item[0], () -> { currentScreen = item[1]; render(); },
                        currentScreen.equals(item[1]));
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, dp(42));
                p.setMargins(dp(4), 0, dp(4), 0);
                nav.addView(b, p);
            }
            navScroll.addView(nav);
            root.addView(navScroll, new LinearLayout.LayoutParams(-1, -2));
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        pageScrollView = scroll;
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(8), dp(18), dp(30));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        dealCardView = null;
        switch (currentScreen) {
            case "builder": showBuilder(content); break;
            case "bank": showBank(content, false); break;
            case "roles": showBank(content, true); break;
            case "review": showReview(content); break;
            case "deal": showDeal(content); break;
            case "moderator": showModerator(content); break;
            case "settings": showSettings(content); break;
            case "home":
            default: showHome(content);
        }
        setContentView(root);
        renderedScreen = currentScreen;
        scroll.post(() -> scroll.scrollTo(0, restoreScrollY));
        if (animateDealCard && "deal".equals(currentScreen) && dealCardView != null) {
            animateDealCard = false;
            dealCardView.setCameraDistance(dp(8000));
            dealCardView.setRotationY(cardRevealed ? -90f : 90f);
            dealCardView.animate().rotationY(0f).setDuration(170).start();
        } else {
            animateDealCard = false;
        }
    }

    private void showHome(LinearLayout content) {
        addEyebrow(content, "اتاق بازی شما");
        addTitle(content, "نقش‌ها را بچین.\nبازی را شروع کن.");
        addBody(content, "بدون اینترنت و بدون حساب کاربری. ترکیب دلخواهت را بساز، تعداد کارت‌ها را کنترل کن و نقش‌ها را یک‌به‌یک و محرمانه پخش کن.");

        LinearLayout hero = panel();
        addText(hero, "حالت فعلی", 12, MUTED, false);
        addText(hero, "پخش یک‌نفره با تحویل گوشی", 17, WHITE, true);
        addText(hero, "نقش بازیکن بعدی تا زمان لمس دکمهٔ مشاهده، روی صفحه نمایش داده نمی‌شود.", 13, MUTED, false);
        addGap(hero, 10);
        hero.addView(button("ساخت ترکیب و ادامه", () -> { currentScreen = "builder"; render(); }, true));
        content.addView(hero, matchWrap());

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.addView(statCard("بازیکنان", String.valueOf(playerCount)), new LinearLayout.LayoutParams(0, -2, 1));
        stats.addView(spaceWidth(8), new LinearLayout.LayoutParams(dp(8), 1));
        stats.addView(statCard("نقش‌های بانک", String.valueOf(roles.size())), new LinearLayout.LayoutParams(0, -2, 1));
        stats.addView(spaceWidth(8), new LinearLayout.LayoutParams(dp(8), 1));
        stats.addView(statCard("ترکیب‌های ذخیره", String.valueOf(lineups.size())), new LinearLayout.LayoutParams(0, -2, 1));
        setTopMargin(stats, 12);
        content.addView(stats);

        addSectionTitle(content, "دسترسی سریع");
        content.addView(button("ساخت ترکیب جدید  →", () -> { lineupBeingEditedId = null; currentScreen = "builder"; render(); }, false), matchWrap());
        content.addView(button("مدیریت بانک نقش‌ها  →", () -> { currentScreen = "roles"; render(); }, false), matchWrap());
        if (dealingPaused && !dealtDeck.isEmpty()) {
            addSectionTitle(content, "پخش نیمه‌تمام");
            content.addView(button("ادامه از بازیکن " + (currentPlayerIndex + 1), () -> {
                dealingPaused = false; cardRevealed = false; currentScreen = "deal"; render();
            }, true), matchWrap());
        }
        if (!lineups.isEmpty()) {
            addSectionTitle(content, "ترکیب‌های اخیر");
            ArrayList<Lineup> recent = new ArrayList<>(lineups);
            recent.sort((a, b) -> Long.compare(b.updatedAt, a.updatedAt));
            for (int i = 0; i < Math.min(3, recent.size()); i++) {
                Lineup lineup = recent.get(i);
                LinearLayout card = panel();
                addText(card, lineup.name, 16, WHITE, true);
                addText(card, lineup.playerCount + " بازیکن  •  " + GameLogic.countCards(lineup.counts) + " کارت", 12, MUTED, false);
                addGap(card, 8);
                card.addView(button("باز کردن ترکیب", () -> loadLineup(lineup), false));
                setTopMargin(card, 8);
                content.addView(card, matchWrap());
            }
        }
        addBody(content, "یادآوری: نقش‌های نمونهٔ بانک، از جمله بازپرس، توضیح رسمیِ تأییدشده ندارند؛ قابلیت هر نقش را مطابق نسخهٔ معتبر قوانین خودتان تعریف کنید.");
    }

    private View statCard(String label, String value) {
        LinearLayout box = panel();
        addText(box, value, 22, GOLD, true);
        addText(box, label, 11, MUTED, false);
        return box;
    }

    private void showBuilder(LinearLayout content) {
        addEyebrow(content, "ویرایش ترکیب");
        addTitle(content, "ساخت ترکیب");
        addBody(content, "تعداد نقش‌ها آزاد است. تنها شرط شروع پخش این است که تعداد کارت‌ها دقیقاً با تعداد بازیکنان برابر باشد.");

        LinearLayout playersCard = panel();
        addText(playersCard, "تعداد بازیکنان", 15, WHITE, true);
        EditText playersInput = input(String.valueOf(playerCount), "مثلاً ۱۲", InputType.TYPE_CLASS_NUMBER);
        playersCard.addView(playersInput, matchWrapTop(9));
        playersCard.addView(button("اعمال تعداد بازیکنان", () -> {
            String value = playersInput.getText().toString().trim();
            try {
                long n = Long.parseLong(value);
                if (n < 1 || n > GameLogic.SAFE_CARD_LIMIT) {
                    toast("تعداد باید بین ۱ تا " + GameLogic.SAFE_CARD_LIMIT + " باشد؛ این حد برای محافظت از حافظهٔ دستگاه است.");
                    return;
                }
                playerCount = (int) n;
                ensurePlayerNames();
                saveAll();
                render();
            } catch (Exception e) { toast("تعداد بازیکنان را به‌صورت عدد صحیح وارد کن."); }
        }, false), matchWrapTop(8));
        content.addView(playersCard, matchWrap());

        long total = GameLogic.countCards(builderCounts);
        long remaining = playerCount - total;
        LinearLayout tally = panel();
        addText(tally, "خلاصهٔ کارت‌ها", 14, WHITE, true);
        LinearLayout counters = new LinearLayout(this);
        counters.setOrientation(LinearLayout.HORIZONTAL);
        counters.addView(miniStat("کارت‌ها", String.valueOf(total), total == playerCount ? GREEN : GOLD), new LinearLayout.LayoutParams(0, -2, 1));
        counters.addView(spaceWidth(7), new LinearLayout.LayoutParams(dp(7), 1));
        counters.addView(miniStat("بازیکنان", String.valueOf(playerCount), WHITE), new LinearLayout.LayoutParams(0, -2, 1));
        counters.addView(spaceWidth(7), new LinearLayout.LayoutParams(dp(7), 1));
        counters.addView(miniStat("باقی‌مانده", (remaining > 0 ? "+" : "") + remaining, remaining == 0 ? GREEN : RED), new LinearLayout.LayoutParams(0, -2, 1));
        setTopMargin(counters, 9);
        tally.addView(counters);
        addText(tally, remaining == 0 ? "تعداد کارت‌ها و بازیکنان برابر است." :
                (remaining > 0 ? "هنوز " + remaining + " کارت کم است." : "تعداد کارت‌ها " + Math.abs(remaining) + " عدد بیشتر از بازیکنان است."),
                12, remaining == 0 ? GREEN : RED, false);
        content.addView(tally, matchWrapTop(10));

        addSectionTitle(content, "بانک نقش‌ها");
        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        EditText searchInput = input(roleSearch, "جست‌وجوی نقش", InputType.TYPE_CLASS_TEXT);
        searchRow.addView(searchInput, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button searchButton = button("جست‌وجو", () -> {
            roleSearch = searchInput.getText().toString().trim(); render();
        }, false);
        LinearLayout.LayoutParams searchButtonParams = new LinearLayout.LayoutParams(-2, dp(48));
        searchButtonParams.setMargins(dp(7), 0, 0, 0);
        searchRow.addView(searchButton, searchButtonParams);
        content.addView(searchRow, matchWrap());
        Spinner factionSpinner = spinner(factionOptions(true));
        factionSpinner.setSelection(Math.max(0, factionOptions(true).indexOf(roleFactionFilter)));
        factionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean first = true;
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String picked = String.valueOf(parent.getItemAtPosition(position));
                if (first) { first = false; return; }
                roleFactionFilter = picked;
                render();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        content.addView(factionSpinner, matchWrapTop(7));

        ArrayList<Role> filtered = getFilteredRoles(false);
        if (filtered.isEmpty()) addBody(content, "نقشی با این فیلتر پیدا نشد.");
        for (Role role : filtered) {
            LinearLayout row = panel();
            LinearLayout mainRow = new LinearLayout(this);
            mainRow.setGravity(Gravity.CENTER_VERTICAL);
            mainRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            mainRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.addView(text(role.name, 15, WHITE, true));
            info.addView(text(role.faction + "  •  " + role.shortDescription, 11, MUTED, false));
            mainRow.addView(rolePortraitView(role, 56, 66), new LinearLayout.LayoutParams(dp(56), dp(66)));
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, -2, 1);
            infoParams.setMargins(dp(8), 0, 0, 0);
            mainRow.addView(info, infoParams);
            Button minus = button("−", () -> {
                int next = Math.max(0, builderCounts.getOrDefault(role.id, 0) - 1);
                if (next == 0) builderCounts.remove(role.id); else builderCounts.put(role.id, next);
                render();
            }, false);
            Button count = button(String.valueOf(builderCounts.getOrDefault(role.id, 0)), null, false);
            count.setEnabled(false);
            Button plus = button("+", () -> {
                int next = builderCounts.getOrDefault(role.id, 0) + 1;
                if (GameLogic.countCards(builderCounts) >= GameLogic.SAFE_CARD_LIMIT) {
                    toast("حد ایمن حافظهٔ این نسخه پر شده است."); return;
                }
                builderCounts.put(role.id, next); render();
            }, true);
            LinearLayout.LayoutParams small = new LinearLayout.LayoutParams(dp(44), dp(42));
            small.setMargins(dp(3), 0, dp(3), 0);
            mainRow.addView(minus, small);
            mainRow.addView(count, small);
            mainRow.addView(plus, small);
            row.addView(mainRow);
            setTopMargin(row, 7);
            content.addView(row, matchWrap());
        }

        addSectionTitle(content, "ذخیره و بازبینی");
        content.addView(button(lineupBeingEditedId == null ? "ذخیرهٔ ترکیب با نام دلخواه" : "ذخیرهٔ تغییرات ترکیب", this::saveCurrentLineup, true), matchWrap());
        content.addView(button("بررسی نهایی و پخش کارت‌ها  →", () -> { currentScreen = "review"; render(); }, false), matchWrapTop(8));
        if (!lineups.isEmpty()) {
            content.addView(button("نمایش ترکیب‌های ذخیره‌شده", () -> showSavedLineupsDialog(), false), matchWrapTop(8));
        }
    }

    private View miniStat(String label, String value, String valueColor) {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(dp(8), dp(9), dp(8), dp(9));
        v.setBackground(round(PANEL_2, 10, "#282828", 1));
        addText(v, value, 18, valueColor, true);
        addText(v, label, 10, MUTED, false);
        return v;
    }

    private void showBank(LinearLayout content, boolean admin) {
        addEyebrow(content, admin ? "تعریف و ویرایش" : "مرجع نقش‌ها");
        addTitle(content, admin ? "مدیریت نقش‌ها" : "بانک نقش‌ها");
        addBody(content, admin
                ? "نقش‌ها را اضافه، ویرایش، فعال/غیرفعال یا حذف کن. توضیحات نقش‌های سناریوی بازپرس در این نسخه به‌صورت قالب هستند و باید با قوانین معتبر تکمیل شوند."
                : "نقش‌های فعال بانک در ساخت ترکیب قابل انتخاب‌اند. این بانک قابل توسعه است و اطلاعات رسمی تأییدنشده را به‌عنوان قانون قطعی نمایش نمی‌دهد.");
        if (admin) content.addView(button("＋ افزودن نقش سفارشی", () -> openRoleEditor(null), true), matchWrap());

        EditText search = input(roleSearch, "جست‌وجوی نام نقش", InputType.TYPE_CLASS_TEXT);
        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.addView(search, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button apply = button("اعمال", () -> { roleSearch = search.getText().toString().trim(); render(); }, false);
        LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(-2, dp(48));
        applyParams.setMargins(dp(7), 0, 0, 0);
        searchRow.addView(apply, applyParams);
        setTopMargin(searchRow, 8);
        content.addView(searchRow);

        Spinner factionSpinner = spinner(factionOptions(false));
        factionSpinner.setSelection(Math.max(0, factionOptions(false).indexOf(roleFactionFilter)));
        factionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean first = true;
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String picked = String.valueOf(parent.getItemAtPosition(position));
                if (first) { first = false; return; }
                roleFactionFilter = picked; render();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        content.addView(factionSpinner, matchWrapTop(7));

        for (Role role : getFilteredRoles(true)) {
            LinearLayout card = panel();
            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            top.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.addView(text(role.name, 16, WHITE, true));
            info.addView(text(role.faction + "  •  " + role.category + (role.enabled ? "  •  فعال" : "  •  غیرفعال"), 11, role.enabled ? GOLD : MUTED, false));
            top.addView(rolePortraitView(role, 76, 88), new LinearLayout.LayoutParams(dp(76), dp(88)));
            LinearLayout infoParams = new LinearLayout.LayoutParams(0, -2, 1);
            infoParams.setMargins(dp(8), 0, dp(8), 0);
            top.addView(info, infoParams);
            top.addView(button("جزئیات", () -> showRoleDetails(role), false));
            card.addView(top);
            addText(card, role.shortDescription, 12, MUTED, false);
            if (admin) {
                LinearLayout actions = new LinearLayout(this);
                actions.setOrientation(LinearLayout.HORIZONTAL);
                Button edit = button("ویرایش", () -> openRoleEditor(role), false);
                actions.addView(edit, new LinearLayout.LayoutParams(0, dp(42), 1));
                Button picture = button("تصویر", () -> chooseRoleImage(role), false);
                LinearLayout.LayoutParams pictureParams = new LinearLayout.LayoutParams(0, dp(42), 1);
                pictureParams.setMargins(dp(4), 0, 0, 0);
                actions.addView(picture, pictureParams);
                Button toggle = button(role.enabled ? "غیرفعال‌سازی" : "فعال‌سازی", () -> {
                    role.enabled = !role.enabled; saveAll(); render();
                }, false);
                LinearLayout.LayoutParams actionP = new LinearLayout.LayoutParams(0, dp(42), 1);
                actionP.setMargins(dp(6), 0, 0, 0);
                actions.addView(toggle, actionP);
                Button del = button("حذف", () -> confirmDeleteRole(role), false);
                LinearLayout.LayoutParams delP = new LinearLayout.LayoutParams(0, dp(42), 1);
                delP.setMargins(dp(6), 0, 0, 0);
                actions.addView(del, delP);
                setTopMargin(actions, 8);
                card.addView(actions);
            }
            setTopMargin(card, 8);
            content.addView(card, matchWrap());
        }
        if (getFilteredRoles(true).isEmpty()) addBody(content, "نتیجه‌ای با این جست‌وجو پیدا نشد.");
        if (!admin) {
            addSectionTitle(content, "اطلاعات این نسخه");
            addBody(content, "قابلیت بازپرس و دیگر نقش‌های سناریویی از منبع رسمی در این پروژه پیش‌فرض نشده است؛ مدیر می‌تواند جزئیات معتبر نقش‌ها را در بخش «مدیریت نقش» وارد کند.");
            content.addView(button("خروجی گرفتن از بانک نقش‌ها", () -> exportJson(true), false), matchWrap());
        }
    }

    private ArrayList<Role> getFilteredRoles(boolean includeDisabled) {
        ArrayList<Role> out = new ArrayList<>();
        String query = roleSearch == null ? "" : roleSearch.trim().toLowerCase(Locale.ROOT);
        for (Role role : roles) {
            if (!includeDisabled && !role.enabled) continue;
            if (!"همه جناح‌ها".equals(roleFactionFilter) && !roleFactionFilter.equals(role.faction)) continue;
            String haystack = (role.name + " " + role.faction + " " + role.category + " " + role.shortDescription).toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !haystack.contains(query)) continue;
            out.add(role);
        }
        out.sort(Comparator.comparing(r -> r.faction + r.name));
        return out;
    }

    private ArrayList<String> factionOptions(boolean includeEverything) {
        ArrayList<String> list = new ArrayList<>();
        list.add("همه جناح‌ها");
        for (Role role : roles) if (!list.contains(role.faction)) list.add(role.faction);
        return list;
    }

    private void showRoleDetails(Role role) {
        String message = "جناح: " + role.faction + "\nدسته: " + role.category + "\n\n" +
                "توضیح: " + role.shortDescription + "\n\nقابلیت: " + role.ability +
                "\n\nشرایط پیروزی: " + role.winCondition + "\n\nتعداد پیشنهادی: " + role.suggestedCount +
                "\nتکرار نقش: " + (role.repeatAllowed ? "مجاز" : "معمولاً غیرتکراری؛ در ترکیب سفارشی همچنان قابل تنظیم") +
                "\nوضعیت: " + (role.enabled ? "فعال" : "غیرفعال");
        new AlertDialog.Builder(this).setTitle(role.icon + "  " + role.name).setMessage(message)
                .setPositiveButton("بستن", null).show();
    }

    private void showReview(LinearLayout content) {
        addEyebrow(content, "پیش از شروع");
        addTitle(content, "بررسی نهایی");
        long total = GameLogic.countCards(builderCounts);
        boolean valid = GameLogic.hasSameSize(playerCount, builderCounts);
        LinearLayout summary = panel();
        addText(summary, "" + playerCount + " بازیکن  /  " + total + " کارت", 19, valid ? GREEN : RED, true);
        addText(summary, valid ? "ترکیب از نظر تعداد آمادهٔ پخش است." :
                "تعداد کارت‌ها و بازیکنان برابر نیست. تا رفع اختلاف، پخش آغاز نمی‌شود.",
                13, valid ? GREEN : RED, false);
        content.addView(summary, matchWrap());

        addSectionTitle(content, "فهرست نقش‌ها");
        ArrayList<Map.Entry<String, Integer>> entries = new ArrayList<>(builderCounts.entrySet());
        entries.sort(Comparator.comparing(e -> roleName(e.getKey())));
        for (Map.Entry<String, Integer> entry : entries) {
            Role role = roleById(entry.getKey());
            if (role == null) continue;
            addRoleLine(content, role, "× " + entry.getValue() + " نسخه");
        }
        if (entries.isEmpty()) addBody(content, "هنوز نقشی در ترکیب انتخاب نشده است.");

        addSectionTitle(content, "بازیکنان و صندلی‌ها");
        addBody(content, "نام‌ها اختیاری‌اند. در صورت خالی بودن، بازیکنان با شمارهٔ صندلی نمایش داده می‌شوند.");
        content.addView(button("تنظیم نام مستعار بازیکنان", this::openPlayerNamesDialog, false), matchWrap());
        for (int i = 0; i < Math.min(playerNames.size(), playerCount); i++) {
            addSimpleLine(content, "صندلی " + (i + 1), TextUtils.isEmpty(playerNames.get(i)) ? "بازیکن " + (i + 1) : playerNames.get(i));
        }
        addSectionTitle(content, "توزیع محرمانه");
        addBody(content, "ترتیب نقش‌ها با مولد تصادفی امن سیستم مخلوط می‌شود. هر کارت یک نسخهٔ مستقل است و پس از شروع پخش، ترکیب این دور ثابت می‌ماند.");
        content.addView(button("بازگشت به ساخت ترکیب", () -> { currentScreen = "builder"; render(); }, false), matchWrap());
        content.addView(button("شروع پخش محرمانه", this::startDeal, true), matchWrapTop(8));
    }

    private void startDeal() {
        if (!GameLogic.hasSameSize(playerCount, builderCounts)) {
            new AlertDialog.Builder(this).setTitle("ترکیب آماده نیست")
                    .setMessage("تعداد بازیکنان: " + playerCount + "\nتعداد کارت‌ها: " + GameLogic.countCards(builderCounts) +
                            "\n\nبرای جلوگیری از کارت اضافه یا بازیکن بدون نقش، ابتدا تعدادها را برابر کن.")
                    .setPositiveButton("ویرایش ترکیب", (d, w) -> { currentScreen = "builder"; render(); }).show();
            return;
        }
        try {
            ArrayList<Role> deck = new ArrayList<>(GameLogic.createDeck(roles, builderCounts));
            GameLogic.secureShuffle(deck);
            dealtDeck = deck;
            currentPlayerIndex = 0;
            cardRevealed = false;
            dealingPaused = false;
            ensurePlayerNames();
            currentScreen = "deal";
            render();
        } catch (IllegalArgumentException e) { toast(e.getMessage()); }
    }

    private void showDeal(LinearLayout content) {
        if (dealtDeck.isEmpty()) {
            addTitle(content, "پخشی فعال نیست");
            content.addView(button("بازگشت به ساخت ترکیب", () -> { currentScreen = "builder"; render(); }, true), matchWrap());
            return;
        }
        if (dealingPaused) {
            addEyebrow(content, "پخش متوقف شده");
            addTitle(content, "همین‌جا مکث کن.");
            addBody(content, "وضعیت کارت‌ها حفظ شده است. ادامه از همان بازیکن انجام می‌شود.");
            content.addView(button("ادامهٔ پخش", () -> { dealingPaused = false; cardRevealed = false; render(); }, true), matchWrap());
            content.addView(button("لغو این دور", this::confirmResetGame, false), matchWrapTop(8));
            return;
        }
        if (currentPlayerIndex >= dealtDeck.size()) {
            addEyebrow(content, "پخش کامل شد");
            addTitle(content, "همهٔ کارت‌ها تحویل داده شدند.");
            addBody(content, "اطلاعات نقش‌ها از این صفحه نمایش داده نمی‌شود. برای شروع یک توزیع تازه، وارد مدیریت بازی شو.");
            content.addView(button("بازگشت به خانه", () -> { currentScreen = "home"; dealtDeck.clear(); render(); }, true), matchWrap());
            content.addView(button("پخش مجدد همین ترکیب", this::confirmRedeal, false), matchWrapTop(8));
            return;
        }
        int shownPlayer = currentPlayerIndex + 1;
        String player = displayPlayerName(currentPlayerIndex);
        addEyebrow(content, "پخش محرمانه  /  " + shownPlayer + " از " + dealtDeck.size());
        addTitle(content, cardRevealed ? "نقش شما" : "گوشی را به بازیکن بدهید");

        if (!cardRevealed) {
            LinearLayout revealCard = panel();
            revealCard.setGravity(Gravity.CENTER_HORIZONTAL);
            revealCard.setPadding(dp(22), dp(28), dp(22), dp(28));
            TextView back = text("✦", 50, GOLD, true);
            back.setGravity(Gravity.CENTER);
            revealCard.addView(back, new LinearLayout.LayoutParams(-1, dp(95)));
            addText(revealCard, "MAFIA / کارت محرمانه", 12, GOLD, true);
            addText(revealCard, player, 15, WHITE, true);
            addText(revealCard, "اطمینان پیدا کن دیگران صفحه را نمی‌بینند؛ سپس نقش را مشاهده کن.", 12, MUTED, false);
            addGap(revealCard, 14);
            revealCard.setCameraDistance(dp(8000));
            dealCardView = revealCard;
            addText(revealCard, "مافیا  •  بازپرس", 12, GOLD, true);
            revealCard.addView(button("مشاهدهٔ نقش", () -> {
                revealCard.animate().rotationY(90f).setDuration(150).withEndAction(() -> {
                    cardRevealed = true;
                    animateDealCard = true;
                    render();
                }).start();
            }, true));
            content.addView(revealCard, matchWrap());
        } else {
            Role role = dealtDeck.get(currentPlayerIndex);
            LinearLayout revealCard = panel();
            revealCard.setGravity(Gravity.CENTER_HORIZONTAL);
            revealCard.setPadding(dp(20), dp(25), dp(20), dp(25));
            revealCard.setBackground(round("#17140E", 18, GOLD, 1));
            revealCard.setCameraDistance(dp(8000));
            dealCardView = revealCard;
            revealCard.addView(rolePortraitView(role, 184, 194), new LinearLayout.LayoutParams(dp(184), dp(194)));
            TextView roleName = text(role.name, 25, WHITE, true);
            roleName.setGravity(Gravity.CENTER);
            revealCard.addView(roleName, matchWrapTop(9));
            TextView faction = text(role.faction, 13, GOLD, true);
            faction.setGravity(Gravity.CENTER);
            revealCard.addView(faction, matchWrapTop(4));
            addGap(revealCard, 13);
            addText(revealCard, role.ability, 14, WHITE, false);
            addGap(revealCard, 10);
            addText(revealCard, "شرایط پیروزی", 12, GOLD, true);
            addText(revealCard, role.winCondition, 13, MUTED, false);
            addGap(revealCard, 13);
            revealCard.addView(button("متوجه شدم؛ پنهان کن", () -> {
                revealCard.animate().rotationY(-90f).setDuration(150).withEndAction(() -> {
                    cardRevealed = false;
                    currentPlayerIndex++;
                    animateDealCard = true;
                    render();
                }).start();
            }, true));
            content.addView(revealCard, matchWrap());
        }
        addGap(content, 10);
        content.addView(button("توقف موقت پخش", () -> {
            new AlertDialog.Builder(this).setTitle("پخش متوقف شود؟")
                    .setMessage("می‌توانی بعداً از همین بازیکن ادامه بدهی.")
                    .setNegativeButton("ادامه بده", null)
                    .setPositiveButton("توقف", (d, w) -> { dealingPaused = true; cardRevealed = false; render(); }).show();
        }, false), matchWrap());
        addBody(content, "نکته: این اپ برای جلوگیری از مشاهدهٔ تصادفی در گوشی مشترک طراحی شده است، نه برای مقابله با فردی که به خود دستگاه یا ابزارهای توسعه دسترسی دارد.");
    }

    private void showModerator(LinearLayout content) {
        addEyebrow(content, "دسترسی گرداننده");
        addTitle(content, "نمایش مدیریتی دور");
        addBody(content, "این صفحه همهٔ تخصیص‌ها را آشکار می‌کند. فقط در اختیار گرداننده باشد.");
        if (dealtDeck.isEmpty()) {
            addBody(content, "هنوز دوری پخش نشده است.");
            content.addView(button("بازگشت", () -> { currentScreen = "settings"; render(); }, true), matchWrap());
            return;
        }
        for (int i = 0; i < dealtDeck.size(); i++) {
            Role role = dealtDeck.get(i);
            LinearLayout card = panel();
            addText(card, "صندلی " + (i + 1) + "  •  " + displayPlayerName(i), 14, GOLD, true);
            LinearLayout roleRow = new LinearLayout(this);
            roleRow.setOrientation(LinearLayout.HORIZONTAL);
            roleRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            roleRow.setGravity(Gravity.CENTER_VERTICAL);
            roleRow.addView(rolePortraitView(role, 68, 78), new LinearLayout.LayoutParams(dp(68), dp(78)));
            LinearLayout roleMeta = new LinearLayout(this);
            roleMeta.setOrientation(LinearLayout.VERTICAL);
            roleMeta.setPadding(dp(9), 0, 0, 0);
            addText(roleMeta, role.name, 16, WHITE, true);
            addText(roleMeta, role.faction, 12, MUTED, false);
            roleRow.addView(roleMeta, new LinearLayout.LayoutParams(0, -2, 1));
            card.addView(roleRow);
            setTopMargin(card, 7);
            content.addView(card, matchWrap());
        }
        content.addView(button("پخش مجدد با همین ترکیب", this::confirmRedeal, true), matchWrapTop(12));
        content.addView(button("بازنشانی کامل دور", this::confirmResetGame, false), matchWrapTop(8));
        content.addView(button("بازگشت به تنظیمات", () -> { currentScreen = "settings"; render(); }, false), matchWrapTop(8));
    }

    private void showSettings(LinearLayout content) {
        addEyebrow(content, "روی همین دستگاه");
        addTitle(content, "تنظیمات و پشتیبان‌گیری");
        addBody(content, "نقش‌ها و ترکیب‌ها در حافظهٔ محلی همین برنامه ذخیره می‌شوند. برای انتقال به دستگاه دیگر از خروجی JSON استفاده کن.");
        LinearLayout privacy = panel();
        addText(privacy, "حریم خصوصی", 16, GOLD, true);
        addText(privacy, "این برنامه به اینترنت، حساب کاربری یا سرور نیاز ندارد. داده‌ها تنها در فضای محلی برنامه ذخیره می‌شوند. پشتیبان JSON می‌تواند شامل نام ترکیب‌ها و توضیحات نقش‌ها باشد؛ آن را در اختیار دیگران قرار نده.", 13, MUTED, false);
        content.addView(privacy, matchWrap());
        addSectionTitle(content, "مدیریت داده‌ها");
        content.addView(button("خروجی گرفتن از بانک نقش‌ها (JSON)", () -> exportJson(true), false), matchWrap());
        content.addView(button("پشتیبان کامل نقش‌ها و ترکیب‌ها", () -> exportJson(false), false), matchWrapTop(8));
        content.addView(button("بازیابی از فایل JSON", this::importJson, false), matchWrapTop(8));

        addSectionTitle(content, "مدیریت دور جاری");
        content.addView(button("ورود به حالت گرداننده و نمایش همهٔ کارت‌ها", () -> {
            if (dealtDeck.isEmpty()) { toast("ابتدا یک دور را پخش کن."); return; }
            new AlertDialog.Builder(this).setTitle("نمایش اطلاعات محرمانه")
                    .setMessage("در این بخش نقش تمام بازیکنان دیده می‌شود. مطمئن شو فقط گرداننده صفحه را می‌بیند.")
                    .setNegativeButton("انصراف", null)
                    .setPositiveButton("ورود", (d, w) -> { currentScreen = "moderator"; render(); }).show();
        }, true), matchWrap());
        content.addView(button("پخش مجدد دور جاری", this::confirmRedeal, false), matchWrapTop(8));
        content.addView(button("بازنشانی کامل دور جاری", this::confirmResetGame, false), matchWrapTop(8));
        addSectionTitle(content, "درباره");
        addBody(content, "مافیا | بازپرس — نسخهٔ بومی اندروید، آفلاین، بدون بک‌اند. نقش‌های سناریوی بازپرس در بانک اولیه قالب قابل‌ویرایش هستند؛ متن رسمی یا توانایی تأییدنشده به آن‌ها نسبت داده نشده است.");
    }

    private void saveCurrentLineup() {
        if (GameLogic.countCards(builderCounts) > GameLogic.SAFE_CARD_LIMIT) {
            toast("تعداد کارت‌ها بیش از حد ایمن حافظه است."); return;
        }
        EditText nameInput = input(activeLineupName, "نام ترکیب", InputType.TYPE_CLASS_TEXT);
        LinearLayout form = new LinearLayout(this);
        form.setPadding(dp(8), dp(5), dp(8), dp(5));
        form.addView(nameInput, matchWrap());
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("ذخیرهٔ ترکیب")
                .setView(form).setNegativeButton("انصراف", null)
                .setPositiveButton("ذخیره", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (name.isEmpty()) { nameInput.setError("یک نام وارد کن"); return; }
            Lineup saved = null;
            if (lineupBeingEditedId != null) {
                for (Lineup item : lineups) if (item.id.equals(lineupBeingEditedId)) { saved = item; break; }
            }
            if (saved == null) {
                saved = new Lineup(UUID.randomUUID().toString(), name, playerCount, builderCounts);
                lineups.add(saved);
                lineupBeingEditedId = saved.id;
            } else {
                saved.name = name;
                saved.playerCount = playerCount;
                saved.counts = new HashMap<>(builderCounts);
                saved.updatedAt = System.currentTimeMillis();
            }
            activeLineupName = name;
            saveAll();
            dialog.dismiss();
            toast("ترکیب ذخیره شد.");
            currentScreen = "builder";
            render();
        }));
        dialog.show();
    }

    private void loadLineup(Lineup lineup) {
        playerCount = Math.max(1, Math.min(GameLogic.SAFE_CARD_LIMIT, lineup.playerCount));
        builderCounts = new HashMap<>(lineup.counts);
        lineupBeingEditedId = lineup.id;
        activeLineupName = lineup.name;
        ensurePlayerNames();
        currentScreen = "builder";
        saveAll();
        render();
    }

    private void showSavedLineupsDialog() {
        if (lineups.isEmpty()) { toast("ترکیب ذخیره‌شده‌ای وجود ندارد."); return; }
        String[] names = new String[lineups.size()];
        for (int i = 0; i < lineups.size(); i++) names[i] = lineups.get(i).name;
        new AlertDialog.Builder(this).setTitle("ترکیب‌های ذخیره‌شده")
                .setItems(names, (dialog, which) -> showLineupActions(lineups.get(which))).show();
    }

    private void showLineupActions(Lineup lineup) {
        String[] actions = {"باز کردن و ویرایش", "ساخت کپی", "حذف ترکیب"};
        new AlertDialog.Builder(this).setTitle(lineup.name).setItems(actions, (d, index) -> {
            if (index == 0) loadLineup(lineup);
            else if (index == 1) duplicateLineup(lineup);
            else confirmDeleteLineup(lineup);
        }).show();
    }

    private void duplicateLineup(Lineup original) {
        EditText input = input(original.name + " - کپی", "نام ترکیب جدید", InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(this).setTitle("کپی ترکیب").setView(input)
                .setNegativeButton("لغو", null).setPositiveButton("ساخت کپی", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) name = original.name + " - کپی";
                    Lineup copy = new Lineup(UUID.randomUUID().toString(), name, original.playerCount, original.counts);
                    lineups.add(copy); saveAll(); toast("کپی ترکیب ساخته شد.");
                }).show();
    }

    private void confirmDeleteLineup(Lineup lineup) {
        new AlertDialog.Builder(this).setTitle("حذف ترکیب")
                .setMessage("ترکیب «" + lineup.name + "» حذف شود؟")
                .setNegativeButton("انصراف", null).setPositiveButton("حذف", (d, w) -> {
                    lineups.remove(lineup);
                    if (lineup.id.equals(lineupBeingEditedId)) lineupBeingEditedId = null;
                    saveAll(); render();
                }).show();
    }

    private void openRoleEditor(Role roleToEdit) {
        final Role target = roleToEdit == null ? null : roleToEdit;
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(6), dp(4), dp(6), dp(4));
        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(false);
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        EditText name = formField(fields, "نام نقش *", target == null ? "" : target.name, false);
        EditText icon = formField(fields, "آیکون / نماد", target == null ? "✦" : target.icon, false);
        ArrayList<String> factionChoices = new ArrayList<>();
        factionChoices.add("مافیایی");
        factionChoices.add("شهروندی");
        factionChoices.add("مستقل و خنثی");
        factionChoices.add("ویژه سناریوی بازپرس");
        factionChoices.add("قابل تنظیم");
        if (target != null && !factionChoices.contains(target.faction)) factionChoices.add(target.faction);
        Spinner faction = spinner(factionChoices);
        if (target != null) faction.setSelection(indexOfSpinner(faction, target.faction));
        addFieldLabel(fields, "جناح"); fields.addView(faction, matchWrapTop(4));
        EditText category = formField(fields, "دسته", target == null ? "سفارشی" : target.category, false);
        EditText shortDesc = formField(fields, "توضیح کوتاه", target == null ? "" : target.shortDescription, false);
        EditText ability = formField(fields, "توضیح کامل قابلیت", target == null ? "قابلیت را مدیر بازی تعریف کند." : target.ability, true);
        EditText win = formField(fields, "شرایط پیروزی", target == null ? "شرایط پیروزی را مدیر بازی تعریف کند." : target.winCondition, true);
        EditText suggested = formField(fields, "تعداد پیشنهادی (صرفاً راهنما)", target == null ? "0" : String.valueOf(target.suggestedCount), false);
        suggested.setInputType(InputType.TYPE_CLASS_NUMBER);
        CheckBox enabled = new CheckBox(this);
        enabled.setText("نقش فعال و قابل استفاده در ترکیب باشد"); enabled.setTextColor(Color.parseColor(WHITE));
        enabled.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.parseColor(GOLD)));
        enabled.setChecked(target == null || target.enabled); fields.addView(enabled);
        CheckBox repeat = new CheckBox(this);
        repeat.setText("تکرار این نقش در ترکیب معمولاً مجاز است (صرفاً راهنما)"); repeat.setTextColor(Color.parseColor(WHITE));
        repeat.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.parseColor(GOLD)));
        repeat.setChecked(target == null || target.repeatAllowed); fields.addView(repeat);
        scroller.addView(fields);
        form.addView(scroller, new LinearLayout.LayoutParams(-1, dp(420)));

        if (target != null) {
            fields.addView(button("انتخاب تصویر اختصاصی برای این نقش", () -> chooseRoleImage(target), false), matchWrapTop(5));
            addText(fields, "تصویر انتخاب‌شده پس از بازگشت از انتخاب‌گر ذخیره می‌شود.", 11, MUTED, false);
        } else {
            addBody(fields, "ابتدا نقش را ذخیره کن؛ سپس از فهرست مدیریت نقش‌ها تصویر اختصاصی آن را انتخاب کن.");
        }
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(target == null ? "نقش جدید" : "ویرایش نقش")
                .setView(form).setNegativeButton("انصراف", null).setPositiveButton("ذخیره", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String roleName = name.getText().toString().trim();
            if (roleName.isEmpty()) { name.setError("نام نقش اجباری است"); return; }
            int suggestedCount;
            try { suggestedCount = Math.max(0, Integer.parseInt(suggested.getText().toString().trim().isEmpty() ? "0" : suggested.getText().toString().trim())); }
            catch (Exception e) { suggested.setError("عدد صحیح وارد کن"); return; }
            String id = target == null ? UUID.randomUUID().toString() : target.id;
            Role updated = new Role(id, roleName, String.valueOf(faction.getSelectedItem()),
                    value(category, "سفارشی"), value(icon, "✦"), value(shortDesc, ""), value(ability, "قابلیت توسط مدیر تعیین نشده است."),
                    value(win, "شرایط پیروزی توسط مدیر تعیین نشده است."), suggestedCount,
                    enabled.isChecked(), repeat.isChecked());
            if (target == null) roles.add(updated);
            else {
                for (int i = 0; i < roles.size(); i++) if (roles.get(i).id.equals(target.id)) { roles.set(i, updated); break; }
            }
            saveAll(); dialog.dismiss(); toast("اطلاعات نقش ذخیره شد."); render();
        }));
        dialog.show();
    }

    private void confirmDeleteRole(Role role) {
        new AlertDialog.Builder(this).setTitle("حذف نقش")
                .setMessage("نقش «" + role.name + "» از بانک حذف شود؟ شمارش آن از ترکیب‌های ذخیره‌شده نیز پاک خواهد شد.")
                .setNegativeButton("انصراف", null).setPositiveButton("حذف", (d, w) -> {
                    roles.remove(role);
                    builderCounts.remove(role.id);
                    for (Lineup lineup : lineups) lineup.counts.remove(role.id);
                    saveAll(); render();
                }).show();
    }

    private void openPlayerNamesDialog() {
        ensurePlayerNames();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < playerCount; i++) {
            if (i > 0) current.append('\n');
            current.append(playerNames.get(i) == null ? "" : playerNames.get(i));
        }
        EditText names = input(current.toString(), "هر خط = یک صندلی", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        names.setMinLines(Math.min(5, Math.max(3, playerCount)));
        names.setGravity(Gravity.TOP | Gravity.RIGHT);
        names.setHint("بازیکن ۱\nبازیکن ۲\n...");
        names.setSelection(names.getText().length());
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setPadding(dp(6), dp(4), dp(6), dp(4));
        wrapper.addView(names, matchWrap());
        new AlertDialog.Builder(this).setTitle("نام بازیکنان / ترتیب صندلی")
                .setView(wrapper).setNegativeButton("انصراف", null)
                .setNeutralButton("پاک‌کردن همه", (d, w) -> {
                    playerNames.clear(); ensurePlayerNames();
                    if ("review".equals(currentScreen)) render();
                })
                .setPositiveButton("ذخیره", (d, w) -> {
                    String[] lines = names.getText().toString().split("\\r?\\n", -1);
                    playerNames.clear();
                    for (int i = 0; i < playerCount; i++) playerNames.add(i < lines.length ? lines[i].trim() : "");
                    if ("review".equals(currentScreen)) render();
                    toast("ترتیب صندلی‌ها ذخیره شد.");
                }).show();
    }

    private void ensurePlayerNames() {
        while (playerNames.size() < playerCount) playerNames.add("");
        while (playerNames.size() > playerCount) playerNames.remove(playerNames.size() - 1);
    }

    private String displayPlayerName(int index) {
        if (index >= 0 && index < playerNames.size() && !TextUtils.isEmpty(playerNames.get(index))) return playerNames.get(index);
        return "بازیکن " + (index + 1);
    }

    private void confirmRedeal() {
        if (dealtDeck.isEmpty()) { toast("دوری برای پخش مجدد وجود ندارد."); return; }
        new AlertDialog.Builder(this).setTitle("پخش مجدد؟")
                .setMessage("تأیید کن: نقش‌ها دوباره به‌صورت تصادفی بین صندلی‌ها توزیع می‌شوند.")
                .setNegativeButton("انصراف", null).setPositiveButton("پخش مجدد", (d, w) -> {
                    ArrayList<Role> deck = new ArrayList<>();
                    for (Role role : dealtDeck) deck.add(role.copy());
                    GameLogic.secureShuffle(deck);
                    dealtDeck = deck; currentPlayerIndex = 0; cardRevealed = false; dealingPaused = false;
                    currentScreen = "deal"; render();
                }).show();
    }

    private void confirmResetGame() {
        new AlertDialog.Builder(this).setTitle("بازنشانی کامل دور")
                .setMessage("وضعیت پخش و تخصیص کارت‌های دور جاری پاک شود؟ ترکیب‌های ذخیره‌شده و بانک نقش‌ها باقی می‌مانند.")
                .setNegativeButton("انصراف", null).setPositiveButton("بازنشانی", (d, w) -> {
                    dealtDeck.clear(); currentPlayerIndex = 0; cardRevealed = false; dealingPaused = false;
                    currentScreen = "home"; render();
                }).show();
    }

    private void exportJson(boolean onlyRoles) {
        roleOnlyExport = onlyRoles;
        String filename = onlyRoles ? "mafiabazpors-roles.json" : "mafiabazpors-backup.json";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, filename);
        startActivityForResult(intent, PICK_EXPORT);
    }

    private void importJson() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_IMPORT);
    }

    private void chooseRoleImage(Role role) {
        if (role == null) { toast("ابتدا نقش را ذخیره کن."); return; }
        pendingImageRoleId = role.id;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_ROLE_IMAGE);
    }

    private void saveRoleImage(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
            // Some document providers offer a transient but usable grant.
        }
        Role role = roleById(pendingImageRoleId);
        if (role == null) { toast("نقش مربوط به تصویر پیدا نشد."); return; }
        role.imageUri = uri.toString();
        portraitCache.remove(role.id);
        saveAll();
        currentScreen = "roles";
        toast("تصویر اختصاصی ذخیره شد.");
        render();
        pendingImageRoleId = null;
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == PICK_EXPORT) writeExport(uri, roleOnlyExport);
        else if (requestCode == PICK_IMPORT) readImport(uri);
        else if (requestCode == PICK_ROLE_IMAGE) saveRoleImage(uri);
    }

    private JSONObject toBackup(boolean onlyRoles) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("schemaVersion", 1);
        root.put("exportType", onlyRoles ? "roles" : "backup");
        JSONArray roleArray = new JSONArray();
        for (Role role : roles) roleArray.put(role.toJson());
        root.put("roles", roleArray);
        if (!onlyRoles) {
            JSONArray lineupArray = new JSONArray();
            for (Lineup lineup : lineups) lineupArray.put(lineup.toJson());
            root.put("lineups", lineupArray);
            root.put("playerCount", playerCount);
        }
        return root;
    }

    private void writeExport(Uri uri, boolean onlyRoles) {
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new IllegalStateException("فایل برای نوشتن باز نشد.");
            out.write(toBackup(onlyRoles).toString(2).getBytes(StandardCharsets.UTF_8));
            toast("فایل JSON ذخیره شد.");
        } catch (Exception e) { toast("خروجی گرفتن ناموفق بود: " + e.getMessage()); }
    }

    private void readImport(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder raw = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) raw.append(line);
            JSONObject root = new JSONObject(raw.toString());
            JSONArray roleArray = root.optJSONArray("roles");
            if (roleArray == null || roleArray.length() == 0) throw new JSONException("فایل بانک نقش معتبر ندارد.");
            ArrayList<Role> importedRoles = new ArrayList<>();
            for (int i = 0; i < roleArray.length(); i++) importedRoles.add(Role.fromJson(roleArray.getJSONObject(i)));
            if (importedRoles.isEmpty()) throw new JSONException("فهرست نقش‌ها خالی است.");
            ArrayList<Lineup> importedLineups = new ArrayList<>(lineups);
            if ("backup".equals(root.optString("exportType"))) {
                JSONArray lineupArray = root.optJSONArray("lineups");
                if (lineupArray != null) {
                    importedLineups.clear();
                    for (int i = 0; i < lineupArray.length(); i++) importedLineups.add(Lineup.fromJson(lineupArray.getJSONObject(i)));
                }
            }
            new AlertDialog.Builder(this).setTitle("بازیابی اطلاعات")
                    .setMessage("بانک فعلی با " + importedRoles.size() + " نقش جایگزین می‌شود." +
                            ("backup".equals(root.optString("exportType")) ? " ترکیب‌های ذخیره‌شده نیز بازیابی می‌شوند." : " ترکیب‌های فعلی حفظ می‌شوند.") +
                            "\n\nاز اطلاعات فعلی نسخهٔ پشتیبان داری؟")
                    .setNegativeButton("انصراف", null).setPositiveButton("جایگزینی", (d, w) -> {
                        roles = importedRoles;
                        if ("backup".equals(root.optString("exportType"))) {
                            lineups = importedLineups;
                            playerCount = Math.max(1, Math.min(GameLogic.SAFE_CARD_LIMIT, root.optInt("playerCount", playerCount)));
                        }
                        builderCounts.entrySet().removeIf(e -> roleById(e.getKey()) == null);
                        for (Lineup lineup : lineups) lineup.counts.entrySet().removeIf(e -> roleById(e.getKey()) == null);
                        ensurePlayerNames(); saveAll(); render(); toast("اطلاعات بازیابی شد.");
                    }).show();
        } catch (Exception e) { toast("فایل نامعتبر یا ناخواناست: " + e.getMessage()); }
    }

    private void addRoleLine(LinearLayout parent, Role role, String right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.setBackground(round(PANEL, 11, "#3A3020", 1));
        row.addView(rolePortraitView(role, 50, 56), new LinearLayout.LayoutParams(dp(50), dp(56)));
        TextView left = text(role.name, 13, WHITE, true);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, -2, 1);
        leftParams.setMargins(dp(9), 0, dp(9), 0);
        row.addView(left, leftParams);
        row.addView(text(right, 12, GOLD, false), new LinearLayout.LayoutParams(-2, -2));
        setTopMargin(row, 6);
        parent.addView(row, matchWrap());
    }

    private View rolePortraitView(Role role, int widthDp, int heightDp) {
        LinearLayout frame = new LinearLayout(this);
        frame.setOrientation(LinearLayout.VERTICAL);
        frame.setPadding(dp(2), dp(2), dp(2), dp(2));
        frame.setBackground(round("#16120C", 11, GOLD, 1));
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundColor(Color.parseColor("#171717"));
        Bitmap bitmap = loadRolePortrait(role);
        if (bitmap != null) image.setImageBitmap(bitmap);
        image.setClipToOutline(true);
        frame.addView(image, new LinearLayout.LayoutParams(dp(Math.max(12, widthDp - 4)), dp(Math.max(12, heightDp - 4))));
        return frame;
    }

    private Bitmap loadRolePortrait(Role role) {
        if (role == null) return null;
        String cacheKey = role.imageUri != null && !role.imageUri.isEmpty()
                ? "uri:" + role.imageUri : "built-in:" + role.id;
        if (portraitCache.containsKey(cacheKey)) return portraitCache.get(cacheKey);
        Bitmap bitmap = null;
        if (role.imageUri != null && !role.imageUri.trim().isEmpty()) {
            try {
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                try (InputStream in = getContentResolver().openInputStream(Uri.parse(role.imageUri))) {
                    BitmapFactory.decodeStream(in, null, bounds);
                }
                int sample = 1;
                while (bounds.outWidth / sample > 720 || bounds.outHeight / sample > 900) sample *= 2;
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = sample;
                try (InputStream in = getContentResolver().openInputStream(Uri.parse(role.imageUri))) {
                    bitmap = BitmapFactory.decodeStream(in, null, options);
                }
            } catch (Exception ignored) {
                bitmap = null;
            }
        }
        if (bitmap == null) {
            try {
                if (portraitSheet == null) portraitSheet = BitmapFactory.decodeResource(getResources(), R.drawable.role_portraits);
                if (portraitSheet != null) {
                    int col = portraitIndex(role) % 4;
                    int row = portraitIndex(role) / 4;
                    int cellW = portraitSheet.getWidth() / 4;
                    int cellH = portraitSheet.getHeight() / 3;
                    bitmap = Bitmap.createBitmap(portraitSheet, col * cellW, row * cellH, cellW, cellH);
                }
            } catch (Exception ignored) {
                bitmap = null;
            }
        }
        if (bitmap != null) portraitCache.put(cacheKey, bitmap);
        return bitmap;
    }

    private int portraitIndex(Role role) {
        String id = role.id == null ? "" : role.id.toLowerCase(Locale.ROOT);
        String name = role.name == null ? "" : role.name;
        if (id.contains("godfather") || name.contains("رئیس مافیا")) return 0;
        if (id.equals("nato") || name.equals("ناتو")) return 1;
        if (id.equals("shayad") || name.contains("شیاد")) return 2;
        if (id.equals("mafia-simple") || name.contains("مافیای ساده")) return 3;
        if (id.contains("doctor") || name.contains("پزشک")) return 4;
        if (id.contains("detective") || name.contains("کارآگاه")) return 5;
        if (id.contains("roein-tan") || name.contains("رویین‌تن") || name.contains("رویین تن")) return 6;
        if (id.contains("sniper") || name.contains("اسنایپر")) return 7;
        if (id.contains("mohaqeq") || name.contains("محقق")) return 8;
        if (id.contains("bazpors") || name.contains("بازپرس")) return 9;
        if (id.contains("citizen") || name.contains("شهروند")) return 10;
        return 11;
    }

    private Role roleById(String id) {
        for (Role role : roles) if (role.id.equals(id)) return role;
        return null;
    }

    private String roleName(String id) {
        Role role = roleById(id);
        return role == null ? id : role.name;
    }

    private EditText formField(LinearLayout form, String label, String value, boolean multiline) {
        addFieldLabel(form, label);
        EditText field = input(value, label, InputType.TYPE_CLASS_TEXT | (multiline ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0));
        if (multiline) { field.setMinLines(3); field.setGravity(Gravity.TOP | Gravity.RIGHT); }
        form.addView(field, matchWrapTop(4));
        addGap(form, 5);
        return field;
    }

    private void addFieldLabel(LinearLayout parent, String label) {
        TextView t = text(label, 12, MUTED, true);
        setTopMargin(t, 5);
        parent.addView(t);
    }

    private int indexOfSpinner(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) if (value.equals(spinner.getItemAtPosition(i))) return i;
        return spinner.getCount() - 1;
    }

    private String value(EditText editText, String fallback) {
        String v = editText.getText().toString().trim();
        return v.isEmpty() ? fallback : v;
    }

    private void addSimpleLine(LinearLayout parent, String left, String right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackground(round(PANEL, 10, "#252525", 1));
        TextView l = text(left, 13, WHITE, true);
        TextView r = text(right, 12, GOLD, false);
        r.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        row.addView(l, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(r, new LinearLayout.LayoutParams(-2, -2));
        setTopMargin(row, 6);
        parent.addView(row, matchWrap());
    }

    private void addEyebrow(LinearLayout parent, String value) {
        TextView t = text(value.toUpperCase(Locale.ROOT), 11, GOLD, true);
        t.setLetterSpacing(0.03f);
        parent.addView(t, matchWrap());
    }

    private void addTitle(LinearLayout parent, String value) {
        TextView t = text(value, 27, WHITE, true);
        t.setLineSpacing(dp(3), 1.0f);
        setTopMargin(t, 5);
        parent.addView(t, matchWrap());
        addGap(parent, 5);
    }

    private void addBody(LinearLayout parent, String value) {
        TextView t = text(value, 13, MUTED, false);
        t.setLineSpacing(dp(3), 1.05f);
        setTopMargin(t, 5);
        parent.addView(t, matchWrap());
        addGap(parent, 8);
    }

    private void addSectionTitle(LinearLayout parent, String value) {
        TextView t = text(value, 16, GOLD, true);
        setTopMargin(t, 18);
        parent.addView(t, matchWrap());
        addGap(parent, 4);
    }

    private void addText(LinearLayout parent, String value, int size, String color, boolean bold) {
        TextView t = text(value, size, color, bold);
        t.setLineSpacing(dp(2), 1.0f);
        parent.addView(t, matchWrapTop(4));
    }

    private TextView text(String value, int size, String color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.parseColor(color));
        t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        t.setIncludeFontPadding(true);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout panel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(13), dp(14), dp(13));
        panel.setBackground(round(PANEL, 14, "#29251B", 1));
        return panel;
    }

    private Button button(String label, Runnable action, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setMinHeight(dp(42));
        b.setPadding(dp(13), dp(6), dp(13), dp(6));
        b.setTextColor(Color.parseColor(primary ? "#11100D" : WHITE));
        b.setBackground(round(primary ? GOLD : PANEL_2, 10, primary ? GOLD : "#39342A", 1));
        if (action != null) b.setOnClickListener(v -> action.run());
        return b;
    }

    private EditText input(String value, String hint, int type) {
        EditText e = new EditText(this);
        e.setSingleLine((type & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
        e.setInputType(type);
        e.setText(value == null ? "" : value);
        e.setHint(hint);
        e.setHintTextColor(Color.parseColor("#777777"));
        e.setTextColor(Color.parseColor(WHITE));
        e.setTextSize(14);
        e.setPadding(dp(12), dp(8), dp(12), dp(8));
        e.setBackground(round("#1B1B1B", 9, "#343434", 1));
        e.setTextDirection(View.TEXT_DIRECTION_RTL);
        return e;
    }

    private Spinner spinner(String[] entries) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, entries);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);
        s.setBackground(round(PANEL_2, 8, "#343434", 1));
        return s;
    }

    private Spinner spinner(ArrayList<String> entries) {
        return spinner(entries.toArray(new String[0]));
    }

    private GradientDrawable round(String fill, int radius, String stroke, int strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.parseColor(fill));
        d.setCornerRadius(dp(radius));
        if (stroke != null) d.setStroke(dp(strokeDp), Color.parseColor(stroke));
        return d;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams matchWrapTop(int top) {
        LinearLayout.LayoutParams p = matchWrap();
        p.topMargin = dp(top);
        return p;
    }

    private void setTopMargin(View view, int margin) {
        ViewGroup.LayoutParams raw = view.getLayoutParams();
        if (raw instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) raw).topMargin = dp(margin);
            view.setLayoutParams(raw);
        } else {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(raw == null ? -1 : raw.width, raw == null ? -2 : raw.height);
            p.topMargin = dp(margin);
            view.setLayoutParams(p);
        }
    }

    private void addGap(LinearLayout parent, int height) {
        View gap = new View(this);
        parent.addView(gap, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private View spaceWidth(int width) {
        View gap = new View(this);
        gap.setLayoutParams(new LinearLayout.LayoutParams(dp(width), 1));
        return gap;
    }

    private int dp(float n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
