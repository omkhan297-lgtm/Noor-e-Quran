package com.noorequran.app

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.*
import android.location.Location
import android.os.*
import android.view.*
import android.widget.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

private const val GREEN = 0xFF064E3B.toInt()
private const val GOLD = 0xFFD8AF55.toInt()
private const val CREAM = 0xFFF7F3EA.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()

class MainActivity : Activity(), SensorEventListener {
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var title: TextView
    private val prefs by lazy { getSharedPreferences("noor", MODE_PRIVATE) }
    private var currentTab = 0
    private var qiblaBearing = 0.0
    private var heading = 0f
    private var qiblaView: TextView? = null
    private var sensorManager: SensorManager? = null
    private var prayerTimes = linkedMapOf<String,String>()
    private var lat = 33.6844
    private var lon = 73.0479
    private val mainHandler = Handler(Looper.getMainLooper())
    private val clockTick = object : Runnable { override fun run() { if (::content.isInitialized && currentTab == 0) render(); mainHandler.postDelayed(this, 60_000) } }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        requestLocation(); requestLastLocation(); requestNotifications(); setupSensors(); createNotificationChannel(); showApp(); mainHandler.postDelayed(clockTick, 60_000)
    }

    private fun showApp(){
        val dark=prefs.getBoolean("dark",false); root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(if(dark) 0xFF101614.toInt() else CREAM)}
        val bar=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(18,16,10,12);setBackgroundColor(GREEN);gravity=Gravity.CENTER_VERTICAL}
        title=TextView(this).apply{text="Noor-e-Quran";setTextColor(WHITE);textSize=22f;setTypeface(null,1)}
        bar.addView(title,LinearLayout.LayoutParams(0,60,1f)); val bell=Button(this).apply{text="🔔";setTextColor(WHITE);setBackgroundColor(Color.TRANSPARENT);setOnClickListener{settings()}}
        bar.addView(bell,LinearLayout.LayoutParams(58,60)); root.addView(bar)
        content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(14,12,14,10)}; root.addView(content,LinearLayout.LayoutParams(-1,0,1f))
        val nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setBackgroundColor(WHITE)}
        listOf("Home","Duas","Tasbeeh","More").forEachIndexed{ i,n-> val b=Button(this).apply{text=n;setOnClickListener{currentTab=i;render()};setTextColor(if(i==0)GREEN else Color.DKGRAY);setBackgroundColor(Color.TRANSPARENT)};nav.addView(b,LinearLayout.LayoutParams(0,70,1f)) }
        root.addView(nav);setContentView(root);render()
    }

    private fun render(){ content.removeAllViews(); title.text=listOf("Noor-e-Quran","Duas","Tasbeeh","More Features")[currentTab]; when(currentTab){0->home();1->duas();2->tasbeeh();3->more()} }
    private fun tv(s:String,size:Float=16f,color:Int=Color.DKGRAY,bold:Boolean=false):TextView=TextView(this).apply{text=s;textSize=sizef(size);setTextColor(color);if(bold)setTypeface(null,1);setPadding(6,6,6,6)}
    private fun sizef(x:Float)=x
    private fun button(s:String, click:()->Unit):Button=Button(this).apply{text=s;setOnClickListener{click()};setTextColor(GREEN)}
    private fun card(v:View):LinearLayout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(14,12,14,12);setBackgroundColor(WHITE);addView(v)}
    private fun add(view:View,h:Int=-2){content.addView(view,LinearLayout.LayoutParams(-1,h).apply{setMargins(0,0,0,10)})}

    private fun home(){
        add(tv("السلام علیکم",26f,GREEN,true)); add(tv("Your complete Islamic companion • Offline-first",15f,Color.GRAY))
        val loc=tv("📍 ${String.format(Locale.US,"%.4f, %.4f",lat,lon)}",14f,GREEN); add(card(loc))
        prayerTimes=PrayerCalc.times(Date(),lat,lon,2)
        val next=PrayerCalc.next(prayerTimes); val p=tv("NEXT PRAYER\n${next.first}\n${next.second}",24f,WHITE,true); p.setBackgroundColor(GREEN); p.setPadding(20,20,20,20); add(p)
        val grid=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        listOf(listOf("🕌 Prayer Times","🧭 Qibla","🤲 Duas"),listOf("📿 Tasbeeh","🌙 Calendar","🔔 Adhkar"),listOf("🔊 Adhan","⚙️ Settings","ℹ️ About")).forEach{row->val r=LinearLayout(this);row.forEach{n->r.addView(button(n){when(n){"🕌 Prayer Times"->prayerScreen();"🧭 Qibla"->qibla();"🤲 Duas"->{currentTab=1;render()};"📿 Tasbeeh"->{currentTab=2;render()};"🌙 Calendar"->calendar();"🔔 Adhkar"->adhkar();"🔊 Adhan"->adhan();"⚙️ Settings"->settings();"ℹ️ About"->about() }},LinearLayout.LayoutParams(0,60,1f))};grid.addView(r)};add(grid)
        val ay=tv("Daily Ayah\n\nوَمَن يَتَوَكَّلْ عَلَى ٱللَّهِ فَهُوَ حَسْبُهُۥ\n\n“And whoever relies upon Allah — He is sufficient for him.”\nQur’an 65:3",18f,GREEN);add(card(ay))
        add(button("⚙ Settings",::settings))
    }

    private fun duas(){
        val search=EditText(this).apply{hint="Search dua"};add(search);val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val draw={q:String->list.removeAllViews();DUAS.filter{q.isBlank()||it.title.contains(q,true)||it.arabic.contains(q)}.forEachIndexed{i,d->list.addView(button("${i+1}. ${d.title}"){duaDetail(d)})}}
        search.addTextChangedListener(SimpleWatcher{draw(search.text.toString())});draw("");add(list)
    }
    private fun duaDetail(d:Dua){content.removeAllViews();title.text="Dua";add(button("← Back"){render()});val x=tv("${d.title}\n\n${d.arabic}\n\nMeaning:\n${d.meaning}\n\nReference: ${d.source}\n\nUse: ${d.use}",21f,Color.DKGRAY);x.gravity=Gravity.RIGHT;add(card(x))}

    private fun tasbeeh(){
        val names=listOf("SubhanAllah","Alhamdulillah","Allahu Akbar","La ilaha illallah","Astaghfirullah","Salawat","Hasbunallahu wa ni'mal wakeel","Custom Zikr")
        val selected=prefs.getString("zikr","SubhanAllah") ?: names[0]; val sp=Spinner(this); sp.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,names); sp.setSelection(names.indexOf(selected).coerceAtLeast(0)); sp.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{override fun onNothingSelected(p:AdapterView<*>?){};override fun onItemSelected(p:AdapterView<*>?,v:View?,pos:Int,id:Long){prefs.edit().putString("zikr",names[pos]).apply()}}; add(sp)
        val key="count_${selected.replace(" ","_")}"; var count=prefs.getInt(key,0); val n=tv("$count",64f,GREEN,true); n.gravity=Gravity.CENTER; add(n)
        add(button("+1  Zikr"){count++;prefs.edit().putInt(key,count).apply();n.text=count.toString()})
        add(button("Reset"){count=0;prefs.edit().putInt(key,0).apply();n.text="0"})
        add(button("Target: 33"){prefs.edit().putInt("target",33).apply();Toast.makeText(this,"Target saved: 33",Toast.LENGTH_SHORT).show()})
        add(button("Target: 100"){prefs.edit().putInt("target",100).apply();Toast.makeText(this,"Target saved: 100",Toast.LENGTH_SHORT).show()})
        add(tv("Each zikr has its own saved counter. Everything remains offline.",13f,Color.GRAY))
    }

    private fun more(){
        listOf("🕌 Prayer Times & Calculation","🧭 Qibla Compass","🔊 Adhan Library (50 voice slots)","🌙 Islamic Calendar","🔔 Adhkar & Reminders","🔖 Bookmarks","🎨 Theme","⚙ Settings","ℹ About / Sources").forEach{n->add(button(n){when{n.startsWith("🕌")->prayerScreen();n.startsWith("🧭")->qibla();n.startsWith("🔊")->adhan();n.startsWith("🌙")->calendar();n.startsWith("🔔")->adhkar();n.startsWith("🔖")->bookmarks();n.startsWith("🎨")->theme();n.startsWith("⚙")->settings();else->about()}})}
    }

    private fun prayerScreen(){content.removeAllViews();title.text="Prayer Times";add(button("← Back"){render()});add(tv("Location: ${lat.f2()}, ${lon.f2()}\nMethod: University of Islamic Sciences, Karachi • Asr: Hanafi",16f,GREEN,true));prayerTimes=PrayerCalc.times(Date(),lat,lon,2);val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};prayerTimes.forEach{(k,v)->box.addView(tv("$k   $v",21f,if(k==PrayerCalc.next(prayerTimes).first)GREEN else Color.DKGRAY,true))};add(card(box));add(button("🔔 Enable prayer notifications"){schedulePrayerNotifications();Toast.makeText(this,"Next prayer notification scheduled",Toast.LENGTH_SHORT).show()});add(tv("Prayer times are calculated on-device; no internet is required. Calculation methods can differ by a few minutes.",13f,Color.GRAY))}

    private fun qibla(){content.removeAllViews();title.text="Qibla Compass";add(button("← Back"){render()});qiblaBearing=PrayerCalc.qibla(lat,lon);qiblaView=tv("Qibla: ${qiblaBearing.roundToInt()}°\nPhone heading: ${heading.roundToInt()}°",28f,GREEN,true);qiblaView!!.gravity=Gravity.CENTER;add(card(qiblaView!!));add(tv("Face the direction shown. Move the phone in a figure‑8 to calibrate the compass if needed.",15f,Color.GRAY));add(button("📍 Refresh location"){requestLocation();Toast.makeText(this,"Location permission requested",Toast.LENGTH_SHORT).show()})}

    private fun calendar(){content.removeAllViews();title.text="Islamic Calendar";add(button("← Back"){render()});val now=Date(); val base=Hijri.fromGregorian(now); val adj=prefs.getInt("hijriAdj",0); val h=Hijri.adjust(base,adj); add(card(tv("Gregorian\n${SimpleDateFormat("EEEE, dd MMMM yyyy",Locale.getDefault()).format(now)}\n\nHijri\n${h.day} ${h.monthName} ${h.year} AH",23f,GREEN,true)));add(tv("Hijri date is calculated offline. Local moon sighting can differ by one day; a manual adjustment setting is provided below.",13f,Color.GRAY));add(button("Hijri +1 day"){prefs.edit().putInt("hijriAdj",prefs.getInt("hijriAdj",0)+1).apply();calendar()});add(button("Hijri −1 day"){prefs.edit().putInt("hijriAdj",prefs.getInt("hijriAdj",0)-1).apply();calendar()})}

    private fun adhkar(){content.removeAllViews();title.text="Daily Adhkar";add(button("← Back"){render()}); ADHKAR.forEach{d->add(card(tv("${d.first}\n\n${d.second}\n\nReference: ${d.third}",18f,GREEN,true)))} }
    private fun settings(){content.removeAllViews();title.text="Settings";add(button("← Back"){render()});add(tv("Noor-e-Quran Settings",24f,GREEN,true));add(button("Use current device location"){requestLocation()});add(button("Enable notifications"){requestNotifications()});add(button("Schedule next prayer notification"){schedulePrayerNotifications()});add(tv("Offline mode: duas, tasbeeh, calendar and prayer calculations use local data. Internet is not required for these core functions.",14f,Color.GRAY));}
    private fun about(){content.removeAllViews();title.text="About";add(button("← Back"){render()});add(card(tv("Noor-e-Quran\n\nA privacy-friendly, offline-first Islamic companion.\n\nQuran source: Tanzil Project (Uthmani) — https://tanzil.net\nPrayer calculation reference: AlAdhan / established astronomical calculation methods.\n\nThis prototype is not a religious authority; references should be checked with qualified scholars before public release.",16f,GREEN,true)))}

    private fun schedulePrayerNotifications(){
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestNotifications();return}
        prayerTimes=PrayerCalc.times(Date(),lat,lon,2); val am=getSystemService(ALARM_SERVICE) as AlarmManager; val now=Calendar.getInstance();
        prayerTimes.filterKeys{it!="Sunrise"}.forEachIndexed{idx,(name,time)-> val hm=time.substring(0,5).split(":"); val c=Calendar.getInstance().apply{set(Calendar.HOUR_OF_DAY,hm[0].toInt());set(Calendar.MINUTE,hm[1].toInt());set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0);if(timeOfDay(this)<=timeOfDay(now)) add(Calendar.DAY_OF_YEAR,1)}; val i=Intent(this,PrayerAlarmReceiver::class.java).putExtra("prayer",name).putExtra("adhan_uri",prefs.getString("adhan_${(idx%50)+1}","")); val pi=PendingIntent.getBroadcast(this,300+idx,i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE); if(Build.VERSION.SDK_INT>=31) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.timeInMillis,pi) else am.setExact(AlarmManager.RTC_WAKEUP,c.timeInMillis,pi)}
        Toast.makeText(this,"Prayer notifications scheduled",Toast.LENGTH_SHORT).show()
    }
    private fun timeOfDay(c:Calendar)=c.get(Calendar.HOUR_OF_DAY)*60+c.get(Calendar.MINUTE)


    private fun createNotificationChannel(){
        if(Build.VERSION.SDK_INT>=26){ val nm=getSystemService(NOTIFICATION_SERVICE) as NotificationManager; nm.createNotificationChannel(NotificationChannel("prayer","Prayer Times",NotificationManager.IMPORTANCE_HIGH).apply{description="Noor-e-Quran prayer notifications"}) }
    }

    private fun requestLastLocation(){ try { val lm=getSystemService(LOCATION_SERVICE) as android.location.LocationManager; val p=if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) Manifest.permission.ACCESS_FINE_LOCATION else return; val providers=lm.getProviders(true);var best:Location?=null;for(pr in providers){val l=lm.getLastKnownLocation(pr)?:continue;if(best==null||l.accuracy<best!!.accuracy)best=l};best?.let{lat=it.latitude;lon=it.longitude}}catch(_:Exception){} }
    override fun onRequestPermissionsResult(r:Int,p:Array<out String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==10&&g.isNotEmpty()&&g[0]==PackageManager.PERMISSION_GRANTED){requestLastLocation();render()}}

    private fun Double.f2()=String.format(Locale.US,"%.4f",this)
}

data class Ayah(val number:Int,val text:String)
data class Surah(val number:Int,val name:String,val arabic:String,val ayahs:List<Ayah>)
data class Dua(val title:String,val arabic:String,val meaning:String,val source:String,val use:String)

class SimpleWatcher(private val f:(String)->Unit): android.text.TextWatcher{override fun beforeTextChanged(s:CharSequence?,st:Int,c:Int,a:Int){};override fun onTextChanged(s:CharSequence?,st:Int,b:Int,c:Int){f(s?.toString() ?: "")};override fun afterTextChanged(e:android.text.Editable?){}}

object PrayerCalc {
    private const val D2R=Math.PI/180; private const val R2D=180/Math.PI
    fun times(date:Date,lat:Double,lon:Double,asrFactor:Int):LinkedHashMap<String,String>{
        val cal=Calendar.getInstance();cal.time=date;val jd=367*cal.get(Calendar.YEAR)-floor(7*(cal.get(Calendar.YEAR)+floor((cal.get(Calendar.MONTH)+10)/12))/4)+floor(275*(cal.get(Calendar.MONTH)+1)/9)+cal.get(Calendar.DAY_OF_MONTH)+1721013.5
        val tz=TimeZone.getDefault().getOffset(date.time)/3600000.0;val t=calc(jd,lat,lon,tz,asrFactor)
        return linkedMapOf("Fajr" to fmt(t[0]),"Sunrise" to fmt(t[1]),"Dhuhr" to fmt(t[2]),"Asr" to fmt(t[3]),"Maghrib" to fmt(t[4]),"Isha" to fmt(t[5]))
    }
    private fun calc(jd:Double,lat:Double,lon:Double,tz:Double,asr:Int):DoubleArray{
        val d=jd-2451545.0;val g=fix(357.529+0.98560028*d);val q=fix(280.459+0.98564736*d);val l=fix(q+1.915*sin(g*D2R)+0.020*sin(2*g*D2R));val e=23.439-0.00000036*d;val decl=asin(sin(e*D2R)*sin(l*D2R))*R2D;val ra=atan2(cos(e*D2R)*sin(l*D2R),cos(l*D2R))*R2D/15;val eq=q/15-ra;val noon=12+tz-lon/15-eq
        fun hour(angle:Double):Double{val noonRad=0.0; val x=(-sin(angle*D2R)-sin(lat*D2R)*sin(decl*D2R))/(cos(lat*D2R)*cos(decl*D2R));return acos(x.coerceIn(-1.0,1.0))*R2D/15}
        val rise=hour(0.833);val fajr=hour(18.0);val isha=hour(18.0);val asAngle=R2D*acot(asr+tan(abs(lat-decl)*D2R));
        return doubleArrayOf(noon-fajr,noon-rise,noon,noon+hour(asAngle),noon+rise,noon+isha)
    }
    private fun acot(x:Double)=atan(1/x)
    private fun fix(x:Double):Double{var y=x%360; if(y<0)y+=360;return y}
    private fun fmt(x:Double):String{var h=floor(x).toInt();var m=round((x-h)*60).toInt();if(m>=60){h++;m-=60};h=(h%24+24)%24;val ap=if(h>=12)"PM" else "AM";val hh=if(h%12==0)12 else h%12;return String.format(Locale.US,"%02d:%02d %s",hh,m,ap)}
    fun next(t:Map<String,String>):Pair<String,String>{val now=Calendar.getInstance();val min=now.get(Calendar.HOUR_OF_DAY)*60+now.get(Calendar.MINUTE);for((k,v) in t){val hm=v.substring(0,5).split(":");val x=hm[0].toInt()*60+hm[1].toInt();if(x>min)return k to v};return t.entries.first().toPair().let{it.key to it.value}}
    fun qibla(lat:Double,lon:Double):Double{val kaLat=21.4225*D2R;val kaLon=39.8262*D2R;val p=lat*D2R;val dl=kaLon-lon*D2R;var b=atan2(sin(dl),cos(p)*tan(kaLat)-sin(p)*cos(dl))*R2D;if(b<0)b+=360;return b}
}

data class HijriDate(val day:Int,val month:Int,val year:Int,val monthName:String)
object Hijri {
    private val names=listOf("Muharram","Safar","Rabi al-Awwal","Rabi al-Thani","Jumada al-Awwal","Jumada al-Thani","Rajab","Shaban","Ramadan","Shawwal","Dhul Qadah","Dhul Hijjah")
    fun adjust(h:HijriDate, delta:Int):HijriDate { var d=h.day+delta; var m=h.month; var y=h.year; while(d>30){d-=30; m++; if(m>12){m=1;y++}}; while(d<1){d+=30;m--;if(m<1){m=12;y--}}; return HijriDate(d,m,y,names[(m-1).coerceIn(0,11)]) }
    fun fromGregorian(date:Date):HijriDate{val c=Calendar.getInstance();c.time=date;var y=c.get(Calendar.YEAR);var m=c.get(Calendar.MONTH)+1;val d=c.get(Calendar.DAY_OF_MONTH);var jd=(367*y-floor(7*(y+floor((m+9)/12))/4)+floor(275*m/9)+d+1721013.5).toInt();var l=jd-1948440+10632;val n=(l-1)/10631;l=l-10631*n+354;val j=((10985-l)/5316)*((50*l)/17719)+(l/5670)*((43*l)/15238);l=l-((30-j)/15)*((17719*j)/50)-(j/16)*((15238*j)/43)+29;val mm=(24*l)/709;val dd=l-(709*mm)/24;val yy=30*n+j-30;return HijriDate(dd,mm,yy,names[(mm-1).coerceIn(0,11)])}
}

val ADHKAR = listOf(
    Triple("Morning Adhkar","Ayat al-Kursi; Surah Al-Ikhlas, Al-Falaq and An-Nas; and authentic morning remembrance should be read according to a trusted collection.","Sahih/verified collections should be consulted for exact counts and wording."),
    Triple("Evening Adhkar","Ayat al-Kursi; Surah Al-Ikhlas, Al-Falaq and An-Nas; and authentic evening remembrance.","Sahih/verified collections should be consulted for exact counts and wording."),
    Triple("After Prayer","Astaghfirullah and the established post-prayer remembrances.","Sahih Muslim and other verified hadith collections."),
    Triple("Before Sleep","Ayat al-Kursi, the last two verses of Al-Baqarah, and the three Quls.","Sahih/verified hadith collections."),
    Triple("Upon Waking","Alhamdulillah and the established waking remembrance.","Sahih al-Bukhari and Muslim."),
    Triple("Protection","Seek refuge in Allah and recite the established protective supplications.","Quran and authentic hadith collections."),
    Triple("Istighfar","Astaghfirullaha wa atubu ilayh.","Authentic hadith collections; verify exact recommended counts."),
    Triple("Salawat","Allahumma salli wa sallim ala Muhammad.","General salawat; exact forms should be taken from authentic sources.")
)

object DUAS {
    val list = listOf(
        Dua("Dua for Guidance","رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِنْ لَدُنْكَ رَحْمَةً ۚ إِنَّكَ أَنتَ الْوَهَّابُ","Our Lord, do not let our hearts deviate after You have guided us, and grant us mercy from You.","Qur'an 3:8","Guidance and steadfastness"),
        Dua("Dua for Forgiveness","رَبَّنَا ظَلَمْنَا أَنفُسَنَا وَإِن لَّمْ تَغْفِرْ لَنَا وَتَرْحَمْنَا لَنَكُونَنَّ مِنَ الْخَاسِرِينَ","Our Lord, we have wronged ourselves. If You do not forgive us and have mercy on us, we will surely be among the losers.","Qur'an 7:23","Repentance"),
        Dua("Dua for Parents","رَبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا","My Lord, have mercy upon them as they raised me when I was small.","Qur'an 17:24","Parents"),
        Dua("Dua for Knowledge","رَبِّ زِدْنِي عِلْمًا","My Lord, increase me in knowledge.","Qur'an 20:114","Study and learning"),
        Dua("Dua for Ease","رَبِّ اشْرَحْ لِي صَدْرِي ۝ وَيَسِّرْ لِي أَمْرِي","My Lord, expand my chest and make my task easy for me.","Qur'an 20:25–26","Difficulty and confidence"),
        Dua("Dua for Good in Both Worlds","رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ","Our Lord, give us good in this world and good in the Hereafter and protect us from the punishment of the Fire.","Qur'an 2:201","Comprehensive supplication"),
        Dua("Dua for Patience","رَبَّنَا أَفْرِغْ عَلَيْنَا صَبْرًا وَتَوَفَّنَا مُسْلِمِينَ","Our Lord, pour upon us patience and let us die as Muslims.","Qur'an 7:126","Patience"),
        Dua("Dua for Acceptance","رَبَّنَا تَقَبَّلْ مِنَّا ۖ إِنَّكَ أَنتَ السَّمِيعُ الْعَلِيمُ","Our Lord, accept from us; indeed You are the Hearing, the Knowing.","Qur'an 2:127","Good deeds"),
        Dua("Dua for Righteous Family","رَبَّنَا هَبْ لَنَا مِنْ أَزْوَاجِنَا وَذُرِّيَّاتِنَا قُرَّةَ أَعْيُنٍ وَاجْعَلْنَا لِلْمُتَّقِينَ إِمَامًا","Our Lord, grant us from our spouses and offspring comfort to our eyes and make us an example for the righteous.","Qur'an 25:74","Family"),
        Dua("Dua for Provision","رَبِّ إِنِّي لِمَا أَنزَلْتَ إِلَيَّ مِنْ خَيْرٍ فَقِيرٌ","My Lord, indeed I am in need of whatever good You send me.","Qur'an 28:24","Provision"),
        Dua("Dua of Prophet Yunus","لَا إِلَٰهَ إِلَّا أَنتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ","There is no deity except You; glory be to You. Indeed, I have been of the wrongdoers.","Qur'an 21:87","Distress"),
        Dua("Dua for Healing","وَإِذَا مَرِضْتُ فَهُوَ يَشْفِينِ","And when I am ill, it is He who cures me.","Qur'an 26:80","Illness"),
        Dua("Dua for Safety","رَبِّ نَجِّنِي مِنَ الْقَوْمِ الظَّالِمِينَ","My Lord, save me from the wrongdoing people.","Qur'an 28:21","Protection"),
        Dua("Dua for Gratitude","رَبِّ أَوْزِعْنِي أَنْ أَشْكُرَ نِعْمَتَكَ الَّتِي أَنْعَمْتَ عَلَيَّ","My Lord, enable me to be grateful for Your favor upon me.","Qur'an 27:19","Gratitude"),
        Dua("Dua for Righteousness","رَبِّ هَبْ لِي مِن لَّدُنكَ ذُرِّيَّةً طَيِّبَةً ۖ إِنَّكَ سَمِيعُ الدُّعَاءِ","My Lord, grant me from You a good offspring. Indeed, You are the Hearer of supplication.","Qur'an 3:38","Family"),
        Dua("Dua for Light","رَبَّنَا أَتْمِمْ لَنَا نُورَنَا وَاغْفِرْ لَنَا ۖ إِنَّكَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ","Our Lord, perfect for us our light and forgive us. Indeed, You are over all things competent.","Qur'an 66:8","Faith"),
        Dua("Dua for Mercy","رَبَّنَا آتِنَا مِن لَّدُنكَ رَحْمَةً وَهَيِّئْ لَنَا مِنْ أَمْرِنَا رَشَدًا","Our Lord, grant us mercy from Yourself and guide our affair to what is right.","Qur'an 18:10","Guidance"),
        Dua("Dua for Refuge from Evil","رَبِّ أَعُوذُ بِكَ مِنْ هَمَزَاتِ الشَّيَاطِينِ ۝ وَأَعُوذُ بِكَ رَبِّ أَن يَحْضُرُونِ","My Lord, I seek refuge in You from the incitements of the devils, and I seek refuge in You from their presence.","Qur'an 23:97–98","Protection"),
        Dua("Dua for Mercy and Forgiveness","رَبِّ اغْفِرْ وَارْحَمْ وَأَنتَ خَيْرُ الرَّاحِمِينَ","My Lord, forgive and have mercy, and You are the best of the merciful.","Qur'an 23:118","Forgiveness"),
        Dua("Dua for Straight Path","اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ","Guide us to the straight path.","Qur'an 1:6","Every prayer"),
        Dua("Dua for Sincere Faith","رَبَّنَا آمَنَّا فَاغْفِرْ لَنَا وَارْحَمْنَا وَأَنتَ خَيْرُ الرَّاحِمِينَ","Our Lord, we have believed, so forgive us and have mercy upon us.","Qur'an 23:109","Faith"),
        Dua("Dua for Protection from Hell","رَبَّنَا اصْرِفْ عَنَّا عَذَابَ جَهَنَّمَ ۖ إِنَّ عَذَابَهَا كَانَ غَرَامًا","Our Lord, avert from us the punishment of Hell; its punishment is ever adhering.","Qur'an 25:65","Hereafter"),
        Dua("Dua for Justice","رَبَّنَا أَفْرِغْ عَلَيْنَا صَبْرًا وَثَبِّتْ أَقْدَامَنَا وَانصُرْنَا عَلَى الْقَوْمِ الْكَافِرِينَ","Our Lord, pour upon us patience, make our feet firm and help us.","Qur'an 2:250","Courage"),
        Dua("Dua for Mercy after Error","رَبَّنَا لَا تُؤَاخِذْنَا إِن نَّسِينَا أَوْ أَخْطَأْنَا","Our Lord, do not impose blame upon us if we forget or make a mistake.","Qur'an 2:286","Mistakes"),
        Dua("Dua for Burden","رَبَّنَا وَلَا تَحْمِلْ عَلَيْنَا إِصْرًا كَمَا حَمَلْتَهُ عَلَى الَّذِينَ مِن قَبْلِنَا","Our Lord, do not burden us as You burdened those before us.","Qur'an 2:286","Hardship"),
        Dua("Dua for Forgiveness and Mercy","رَبَّنَا لَا تُحَمِّلْنَا مَا لَا طَاقَةَ لَنَا بِهِ وَاعْفُ عَنَّا وَاغْفِرْ لَنَا وَارْحَمْنَا","Our Lord, do not burden us beyond our capacity; pardon us, forgive us and have mercy on us.","Qur'an 2:286","Hardship"),
        Dua("Dua for Steadfastness","رَبَّنَا أَفْرِغْ عَلَيْنَا صَبْرًا وَتَوَفَّنَا مُسْلِمِينَ","Our Lord, pour upon us patience and let us die as Muslims.","Qur'an 7:126","Steadfastness"),
        Dua("Dua for a Good Ending","رَبَّنَا لَا تَجْعَلْنَا فِتْنَةً لِّلَّذِينَ كَفَرُوا وَاغْفِرْ لَنَا رَبَّنَا ۖ إِنَّكَ أَنتَ الْعَزِيزُ الْحَكِيمُ","Our Lord, do not make us a trial for those who disbelieve and forgive us.","Qur'an 60:5","Protection"),
        Dua("Dua for Truth","رَبَّنَا اكْشِفْ عَنَّا الْعَذَابَ إِنَّا مُؤْمِنُونَ","Our Lord, remove the punishment from us; indeed, we are believers.","Qur'an 44:12","Repentance"),
        Dua("Dua for a Blessed Home","رَبِّ أَدْخِلْنِي مُدْخَلَ صِدْقٍ وَأَخْرِجْنِي مُخْرَجَ صِدْقٍ وَاجْعَل لِّي مِن لَّدُنكَ سُلْطَانًا نَّصِيرًا","My Lord, cause me to enter with truth and exit with truth and grant me supporting authority from You.","Qur'an 17:80","Travel and transitions"),
        Dua("Dua for Thankfulness","رَبِّ أَوْزِعْنِي أَنْ أَشْكُرَ نِعْمَتَكَ وَأَنْ أَعْمَلَ صَالِحًا تَرْضَاهُ","My Lord, enable me to be grateful for Your favor and to do righteousness that You approve.","Qur'an 46:15","Gratitude"),
        Dua("Dua for Righteous Action","وَقُل رَّبِّ أَدْخِلْنِي مُدْخَلَ صِدْقٍ وَأَخْرِجْنِي مُخْرَجَ صِدْقٍ","My Lord, let my entrance and exit be with truth.","Qur'an 17:80","Beginnings and endings"),
        Dua("Dua for Wisdom","رَبِّ هَبْ لِي حُكْمًا وَأَلْحِقْنِي بِالصَّالِحِينَ","My Lord, grant me wisdom and join me with the righteous.","Qur'an 26:83","Wisdom"),
        Dua("Dua for a Good Reputation","وَاجْعَل لِّي لِسَانَ صِدْقٍ فِي الْآخِرِينَ","Grant me an honorable mention among later generations.","Qur'an 26:84","Good character"),
        Dua("Dua for Paradise","وَاجْعَلْنِي مِن وَرَثَةِ جَنَّةِ النَّعِيمِ","Make me among the inheritors of the Garden of Bliss.","Qur'an 26:85","Hereafter"),
        Dua("Dua for the Day of Judgment","يَوْمَ لَا يَنفَعُ مَالٌ وَلَا بَنُونَ ۝ إِلَّا مَنْ أَتَى اللَّهَ بِقَلْبٍ سَلِيمٍ","On the Day when wealth and children will not benefit, except one who comes to Allah with a sound heart.","Qur'an 26:88–89","Sound heart"),
        Dua("Dua for Family Prayer","رَبِّ اجْعَلْنِي مُقِيمَ الصَّلَاةِ وَمِن ذُرِّيَّتِي ۚ رَبَّنَا وَتَقَبَّلْ دُعَاءِ","My Lord, make me an establisher of prayer, and [many] from my descendants. Our Lord, accept my supplication.","Qur'an 14:40","Prayer"),
        Dua("Dua for a Blessed City","رَبِّ اجْعَلْ هَٰذَا بَلَدًا آمِنًا وَارْزُقْ أَهْلَهُ مِنَ الثَّمَرَاتِ","My Lord, make this a secure city and provide its people with fruits.","Qur'an 2:126","Safety and provision"),
        Dua("Dua for Thanking Allah","الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ","All praise is for Allah, Lord of the worlds.","Qur'an 1:2","Praise"),
        Dua("Dua for Trust","حَسْبِيَ اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ ۖ عَلَيْهِ تَوَكَّلْتُ","Allah is sufficient for me; there is no deity except Him. Upon Him I rely.","Qur'an 9:129","Reliance on Allah"),
    )
}
