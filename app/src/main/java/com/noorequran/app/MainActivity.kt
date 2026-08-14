package com.noorequran.app

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.os.*
import android.view.Gravity
import android.view.View
import android.widget.*
import android.text.Editable
import android.text.TextWatcher
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

    private val prefs by lazy {
        getSharedPreferences("noor", MODE_PRIVATE)
    }

    private var currentTab = 0
    private var qiblaBearing = 0.0
    private var heading = 0f

    private var qiblaView: TextView? = null
    private var sensorManager: SensorManager? = null

    private var prayerTimes =
        linkedMapOf<String, String>()

    // Default location: Islamabad
    private var lat = 33.6844
    private var lon = 73.0479

    private val handler =
        Handler(Looper.getMainLooper())

    private val clockTick = object : Runnable {
        override fun run() {

            if (::content.isInitialized && currentTab == 0) {
                render()
            }

            handler.postDelayed(this, 60_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestLocation()
        requestLastLocation()
        requestNotifications()

        setupSensors()
        createNotificationChannel()

        showApp()

        handler.postDelayed(clockTick, 60_000L)
    }

    override fun onDestroy() {
        super.onDestroy()

        sensorManager?.unregisterListener(this)
        handler.removeCallbacks(clockTick)
    }

    // ---------------------------------------------------------
    // APP UI
    // ---------------------------------------------------------

    private fun showApp() {

        val dark =
            prefs.getBoolean("dark", false)

        root = LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setBackgroundColor(
                if (dark)
                    0xFF101614.toInt()
                else
                    CREAM
            )
        }

        val bar =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                setPadding(
                    18,
                    16,
                    10,
                    12
                )

                setBackgroundColor(GREEN)

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        title =
            TextView(this).apply {

                text = "Noor-e-Quran"

                setTextColor(WHITE)

                textSize = 22f

                setTypeface(null, 1)
            }

        bar.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                60,
                1f
            )
        )

        val bell =
            Button(this).apply {

                text = "🔔"

                setTextColor(WHITE)

                setBackgroundColor(
                    Color.TRANSPARENT
                )

                setOnClickListener {
                    settings()
                }
            }

        bar.addView(
            bell,
            LinearLayout.LayoutParams(
                58,
                60
            )
        )

        root.addView(bar)

        content =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    14,
                    12,
                    14,
                    10
                )
            }

        root.addView(
            content,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        val nav =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                setBackgroundColor(WHITE)
            }

        listOf(
            "Home",
            "Duas",
            "Tasbeeh",
            "More"
        ).forEachIndexed { index, name ->

            val button =
                Button(this).apply {

                    text = name

                    setOnClickListener {

                        currentTab = index

                        render()
                    }

                    setTextColor(
                        if (index == 0)
                            GREEN
                        else
                            Color.DKGRAY
                    )

                    setBackgroundColor(
                        Color.TRANSPARENT
                    )
                }

            nav.addView(
                button,
                LinearLayout.LayoutParams(
                    0,
                    70,
                    1f
                )
            )
        }

        root.addView(nav)

        setContentView(root)

        render()
    }

    private fun render() {

        content.removeAllViews()

        title.text =
            listOf(
                "Noor-e-Quran",
                "Duas",
                "Tasbeeh",
                "More Features"
            )[currentTab]

        when (currentTab) {

            0 -> home()

            1 -> duas()

            2 -> tasbeeh()

            3 -> more()
        }
    }

    private fun tv(
        textValue: String,
        size: Float = 16f,
        color: Int = Color.DKGRAY,
        bold: Boolean = false
    ): TextView {

        return TextView(this).apply {

            text = textValue

            textSize = size

            setTextColor(color)

            if (bold) {
                setTypeface(null, 1)
            }

            setPadding(
                6,
                6,
                6,
                6
            )
        }
    }

    private fun button(
        textValue: String,
        click: () -> Unit
    ): Button {

        return Button(this).apply {

            text = textValue

            setTextColor(GREEN)

            setOnClickListener {
                click()
            }
        }
    }

    private fun card(view: View):
            LinearLayout {

        return LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setPadding(
                14,
                12,
                14,
                12
            )

            setBackgroundColor(WHITE)

            addView(view)
        }
    }

    private fun add(
        view: View,
        height: Int = -2
    ) {

        content.addView(
            view,
            LinearLayout.LayoutParams(
                -1,
                height
            ).apply {
                setMargins(
                    0,
                    0,
                    0,
                    10
                )
            }
        )
    }

    // ---------------------------------------------------------
    // HOME
    // ---------------------------------------------------------

    private fun home() {

        add(
            tv(
                "السلام علیکم",
                26f,
                GREEN,
                true
            )
        )

        add(
            tv(
                "Your complete Islamic companion • Offline-first",
                15f,
                Color.GRAY
            )
        )

        add(
            card(
                tv(
                    "📍 ${
                        String.format(
                            Locale.US,
                            "%.4f, %.4f",
                            lat,
                            lon
                        )
                    }",
                    14f,
                    GREEN
                )
            )
        )

        prayerTimes =
            PrayerCalc.times(
                Date(),
                lat,
                lon,
                2
            )

        val next =
            PrayerCalc.next(prayerTimes)

        val nextView =
            tv(
                "NEXT PRAYER\n" +
                        "${next.first}\n" +
                        next.second,
                24f,
                WHITE,
                true
            )

        nextView.setBackgroundColor(GREEN)

        nextView.setPadding(
            20,
            20,
            20,
            20
        )

        add(nextView)

        val grid =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        val rows =
            listOf(

                listOf(
                    "🕌 Prayer Times",
                    "🧭 Qibla",
                    "🤲 Duas"
                ),

                listOf(
                    "📿 Tasbeeh",
                    "🌙 Calendar",
                    "🔔 Adhkar"
                ),

                listOf(
                    "🔊 Adhan",
                    "⚙️ Settings",
                    "ℹ️ About"
                )
            )

        rows.forEach { row ->

            val layout =
                LinearLayout(this)

            row.forEach { name ->

                layout.addView(
                    button(name) {

                        when (name) {

                            "🕌 Prayer Times" ->
                                prayerScreen()

                            "🧭 Qibla" ->
                                qibla()

                            "🤲 Duas" -> {
                                currentTab = 1
                                render()
                            }

                            "📿 Tasbeeh" -> {
                                currentTab = 2
                                render()
                            }

                            "🌙 Calendar" ->
                                calendar()

                            "🔔 Adhkar" ->
                                adhkar()

                            "🔊 Adhan" ->
                                adhan()

                            "⚙️ Settings" ->
                                settings()

                            "ℹ️ About" ->
                                about()
                        }
                    },
                    LinearLayout.LayoutParams(
                        0,
                        60,
                        1f
                    )
                )
            }

            grid.addView(layout)
        }

        add(grid)

        add(
            card(
                tv(
                    "Daily Ayah\n\n" +
                            "وَمَن يَتَوَكَّلْ عَلَى ٱللَّهِ فَهُوَ حَسْبُهُۥ\n\n" +
                            "“And whoever relies upon Allah — He is sufficient for him.”\n\n" +
                            "Qur’an 65:3",
                    18f,
                    GREEN
                )
            )
        )

        add(
            button(
                "⚙ Settings",
                ::settings
            )
        )
    }

    // ---------------------------------------------------------
    // DUAS
    // ---------------------------------------------------------

    private fun duas() {

        val search =
            EditText(this).apply {
                hint = "Search dua"
            }

        add(search)

        val list =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        val draw:
                (String) -> Unit = { query ->

            list.removeAllViews()

            DUAS.list
                .filter {

                    query.isBlank() ||
                            it.title.contains(
                                query,
                                true
                            ) ||
                            it.arabic.contains(query)
                }
                .forEachIndexed { index, dua ->

                    list.addView(
                        button(
                            "${index + 1}. ${dua.title}"
                        ) {
                            duaDetail(dua)
                        }
                    )
                }
        }

        search.addTextChangedListener(
            SimpleWatcher(draw)
        )

        draw("")

        add(list)
    }

    private fun duaDetail(
        dua: Dua
    ) {

        content.removeAllViews()

        title.text = "Dua"

        add(
            button("← Back") {
                render()
            }
        )

        val text =
            tv(
                "${dua.title}\n\n" +
                        "${dua.arabic}\n\n" +
                        "Meaning:\n" +
                        "${dua.meaning}\n\n" +
                        "Reference: ${dua.source}\n\n" +
                        "Use: ${dua.use}",
                21f,
                Color.DKGRAY
            )

        text.gravity =
            Gravity.RIGHT

        add(card(text))
    }

    // ---------------------------------------------------------
    // TASBEEH
    // ---------------------------------------------------------

    private fun tasbeeh() {

        val names =
            listOf(
                "SubhanAllah",
                "Alhamdulillah",
                "Allahu Akbar",
                "La ilaha illallah",
                "Astaghfirullah",
                "Salawat",
                "Hasbunallahu wa ni'mal wakeel",
                "Custom Zikr"
            )

        val selected =
            prefs.getString(
                "zikr",
                names[0]
            ) ?: names[0]

        val spinner =
            Spinner(this)

        spinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                names
            )

        spinner.setSelection(
            names.indexOf(selected)
                .coerceAtLeast(0)
        )

        spinner.onItemSelectedListener =
            object :
                AdapterView.OnItemSelectedListener {

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    prefs.edit()
                        .putString(
                            "zikr",
                            names[position]
                        )
                        .apply()
                }
            }

        add(spinner)

        val key =
            "count_${selected.replace(" ", "_")}"

        var count =
            prefs.getInt(key, 0)

        val number =
            tv(
                count.toString(),
                64f,
                GREEN,
                true
            )

        number.gravity =
            Gravity.CENTER

        add(number)

        add(
            button("+1  Zikr") {

                count++

                prefs.edit()
                    .putInt(key, count)
                    .apply()

                number.text =
                    count.toString()
            }
        )

        add(
            button("Reset") {

                count = 0

                prefs.edit()
                    .putInt(key, 0)
                    .apply()

                number.text = "0"
            }
        )

        add(
            button("Target: 33") {

                prefs.edit()
                    .putInt("target", 33)
                    .apply()

                Toast.makeText(
                    this,
                    "Target saved: 33",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        add(
            button("Target: 100") {

                prefs.edit()
                    .putInt("target", 100)
                    .apply()

                Toast.makeText(
                    this,
                    "Target saved: 100",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        add(
            tv(
                "Each zikr has its own saved counter. Everything remains offline.",
                13f,
                Color.GRAY
            )
        )
    }

    // ---------------------------------------------------------
    // MORE
    // ---------------------------------------------------------

    private fun more() {

        listOf(

            "🕌 Prayer Times & Calculation",
            "🧭 Qibla Compass",
            "🔊 Adhan Library (15+ legal audio slots)",
            "🌙 Islamic Calendar",
            "🔔 Adhkar & Reminders",
            "🔖 Bookmarks",
            "🎨 Theme",
            "⚙ Settings",
            "ℹ About / Sources"

        ).forEach { name ->

            add(
                button(name) {

                    when {

                        name.startsWith("🕌") ->
                            prayerScreen()

                        name.startsWith("🧭") ->
                            qibla()

                        name.startsWith("🔊") ->
                            adhan()

                        name.startsWith("🌙") ->
                            calendar()

                        name.startsWith("🔔") ->
                            adhkar()

                        name.startsWith("🔖") ->
                            bookmarks()

                        name.startsWith("🎨") ->
                            theme()

                        name.startsWith("⚙") ->
                            settings()

                        else ->
                            about()
                    }
                }
            )
        }
    }

    // ---------------------------------------------------------
    // PRAYER TIMES
    // ---------------------------------------------------------

    private fun prayerScreen() {

        content.removeAllViews()

        title.text =
            "Prayer Times"

        add(
            button("← Back") {
                render()
            }
        )

        add(
            tv(
                "Location: ${lat.f2()}, ${lon.f2()}\n" +
                        "Method: University of Islamic Sciences, Karachi • Asr: Hanafi",
                16f,
                GREEN,
                true
            )
        )

        prayerTimes =
            PrayerCalc.times(
                Date(),
                lat,
                lon,
                2
            )

        val box =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        val next =
            PrayerCalc.next(
                prayerTimes
            ).first

        prayerTimes.forEach { (name, time) ->

            box.addView(
                tv(
                    "$name   $time",
                    21f,
                    if (name == next)
                        GREEN
                    else
                        Color.DKGRAY,
                    true
                )
            )
        }

        add(card(box))

        add(
            button(
                "🔔 Enable prayer notifications"
            ) {
                schedulePrayerNotifications()
            }
        )

        add(
            tv(
                "Prayer times are calculated on-device. " +
                        "Internet is not required.",
                13f,
                Color.GRAY
            )
        )
    }

    // ---------------------------------------------------------
    // QIBLA
    // ---------------------------------------------------------

    private fun qibla() {

        content.removeAllViews()

        title.text =
            "Qibla Compass"

        add(
            button("← Back") {
                render()
            }
        )

        qiblaBearing =
            PrayerCalc.qibla(
                lat,
                lon
            )

        qiblaView =
            tv(
                "Qibla: ${qiblaBearing.roundToInt()}°\n" +
                        "Phone heading: ${heading.roundToInt()}°",
                28f,
                GREEN,
                true
            )

        qiblaView!!.gravity =
            Gravity.CENTER

        add(card(qiblaView!!))

        add(
            tv(
                "Move the phone in a figure-8 to calibrate the compass if needed.",
                15f,
                Color.GRAY
            )
        )

        add(
            button("📍 Refresh location") {

                requestLocation()
                requestLastLocation()

                Toast.makeText(
                    this,
                    "Location updated",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    // ---------------------------------------------------------
    // CALENDAR
    // ---------------------------------------------------------

    private fun calendar() {

        content.removeAllViews()

        title.text =
            "Islamic Calendar"

        add(
            button("← Back") {
                render()
            }
        )

        val now = Date()

        val base =
            Hijri.fromGregorian(now)

        val adjustment =
            prefs.getInt(
                "hijriAdj",
                0
            )

        val hijri =
            Hijri.adjust(
                base,
                adjustment
            )

        val gregorian =
            SimpleDateFormat(
                "EEEE, dd MMMM yyyy",
                Locale.getDefault()
            ).format(now)

        add(
            card(
                tv(
                    "Gregorian\n$gregorian\n\n" +
                            "Hijri\n" +
                            "${hijri.day} " +
                            "${hijri.monthName} " +
                            "${hijri.year} AH",
                    23f,
                    GREEN,
                    true
                )
            )
        )

        add(
            tv(
                "Hijri date is calculated offline. " +
                        "Local moon sighting can differ by one day.",
                13f,
                Color.GRAY
            )
        )

        add(
            button("Hijri +1 day") {

                prefs.edit()
                    .putInt(
                        "hijriAdj",
                        prefs.getInt(
                            "hijriAdj",
                            0
                        ) + 1
                    )
                    .apply()

                calendar()
            }
        )

        add(
            button("Hijri −1 day") {

                prefs.edit()
                    .putInt(
                        "hijriAdj",
                        prefs.getInt(
                            "hijriAdj",
                            0
                        ) - 1
                    )
                    .apply()

                calendar()
            }
        )
    }

    // ---------------------------------------------------------
    // ADHKAR
    // ---------------------------------------------------------

    private fun adhkar() {

        content.removeAllViews()

        title.text =
            "Daily Adhkar"

        add(
            button("← Back") {
                render()
            }
        )

        ADHKAR.forEach { item ->

            add(
                card(
                    tv(
                        "${item.first}\n\n" +
                                "${item.second}\n\n" +
                                "Reference: ${item.third}",
                        18f,
                        GREEN,
                        true
                    )
                )
            )
        }
    }

    // ---------------------------------------------------------
    // SETTINGS
    // ---------------------------------------------------------

    private fun settings() {

        content.removeAllViews()

        title.text =
            "Settings"

        add(
            button("← Back") {
                render()
            }
        )

        add(
            tv(
                "Noor-e-Quran Settings",
                24f,
                GREEN,
                true
            )
        )

        add(
            button("Use current device location") {

                requestLocation()
                requestLastLocation()
            }
        )

        add(
            button("Enable notifications") {
                requestNotifications()
            }
        )

        add(
            button(
                "Schedule next prayer notification"
            ) {
                schedulePrayerNotifications()
            }
        )

        add(
            button("Toggle Dark Theme") {

                val dark =
                    prefs.getBoolean(
                        "dark",
                        false
                    )

                prefs.edit()
                    .putBoolean(
                        "dark",
                        !dark
                    )
                    .apply()

                recreate()
            }
        )

        add(
            tv(
                "Duas, Tasbeeh, calendar and prayer calculations work offline.",
                14f,
                Color.GRAY
            )
        )
    }

    // ---------------------------------------------------------
    // ADHAN LIBRARY
    // ---------------------------------------------------------

    private fun adhan() {

        content.removeAllViews()

        title.text =
            "Adhan Library"

        add(
            button("← Back") {
                render()
            }
        )

        add(
            tv(
                "15+ Adhan Audio Library",
                24f,
                GREEN,
                true
            )
        )

        add(
            tv(
                "Audio recordings will only be added from " +
                        "legally licensed or public-domain sources.",
                14f,
                Color.GRAY
            )
        )

        for (i in 1..15) {

            val saved =
                prefs.getString(
                    "adhan_$i",
                    ""
                ) ?: ""

            add(
                card(
                    tv(
                        "Adhan Slot $i\n" +
                                if (saved.isBlank())
                                    "No audio configured"
                                else
                                    "Audio configured",
                        17f,
                        GREEN,
                        true
                    )
                )
            )
        }
    }

    // ---------------------------------------------------------
    // BOOKMARKS
    // ---------------------------------------------------------

    private fun bookmarks() {

        content.removeAllViews()

        title.text =
            "Bookmarks"

        add(
            button("← Back") {
                render()
            }
        )

        add(
            tv(
                "Bookmarks feature is ready for expansion.",
                17f,
                Color.DKGRAY
            )
        )
    }

    // ---------------------------------------------------------
    // THEME
    // ---------------------------------------------------------

    private fun theme() {

        content.removeAllViews()

        title.text =
            "Theme"

        add(
            button("← Back") {
                render()
            }
        )

        val dark =
            prefs.getBoolean(
                "dark",
                false
            )

        add(
            button(
                if (dark)
                    "Switch to Light Theme"
                else
                    "Switch to Dark Theme"
            ) {

                prefs.edit()
                    .putBoolean(
                        "dark",
                        !dark
                    )
                    .apply()

                recreate()
            }
        )
    }

    // ---------------------------------------------------------
    // ABOUT
    // ---------------------------------------------------------

    private fun about() {

        content.removeAllViews()

        title.text =
            "About"

        add(
            button("← Back") {
                render()
            }
        )

        add(
            card(
                tv(
                    "Noor-e-Quran\n\n" +
                            "A privacy-friendly, offline-first Islamic companion.\n\n" +
                            "Quran source: Tanzil Project (Uthmani).\n\n" +
                            "Prayer calculations use established astronomical methods.\n\n" +
                            "This prototype is not a religious authority.",
                    16f,
                    GREEN,
                    true
                )
            )
        )
    }

    // ---------------------------------------------------------
    // NOTIFICATIONS
    // ---------------------------------------------------------

    private fun schedulePrayerNotifications() {

        if (
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifications()
            return
        }

        prayerTimes =
            PrayerCalc.times(
                Date(),
                lat,
                lon,
                2
            )

        val alarmManager =
            getSystemService(
                ALARM_SERVICE
            ) as AlarmManager

        val now =
            Calendar.getInstance()

        prayerTimes
            .filterKeys {
                it != "Sunrise"
            }
            .forEachIndexed { index, entry ->

                val name =
                    entry.key

                val time =
                    entry.value

                val parts =
                    time.substring(
                        0,
                        5
                    ).split(":")

                val hour12 =
                    parts[0].toInt()

                val minute =
                    parts[1].toInt()

                val pm =
                    time.endsWith(
                        "PM",
                        ignoreCase = true
                    )

                var hour24 =
                    hour12 % 12

                if (pm) {
                    hour24 += 12
                }

                val alarm =
                    Calendar.getInstance().apply {

                        set(
                            Calendar.HOUR_OF_DAY,
                            hour24
                        )

                        set(
                            Calendar.MINUTE,
                            minute
                        )

                        set(
                            Calendar.SECOND,
                            0
                        )

                        set(
                            Calendar.MILLISECOND,
                            0
                        )

                        if (
                            timeInMinutes(this) <=
                            timeInMinutes(now)
                        ) {
                            add(
                                Calendar.DAY_OF_YEAR,
                                1
                            )
                        }
                    }

                val adhanUri =
                    prefs.getString(
                        "adhan_${(index % 15) + 1}",
                        ""
                    ) ?: ""

                val intent =
                    Intent(
                        this,
                        PrayerAlarmReceiver::class.java
                    )
                        .putExtra(
                            "prayer",
                            name
                        )
                        .putExtra(
                            "adhan_uri",
                            adhanUri
                        )

                val pendingIntent =
                    PendingIntent.getBroadcast(
                        this,
                        300 + index,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or
                                PendingIntent.FLAG_IMMUTABLE
                    )

                try {

                    if (
                        Build.VERSION.SDK_INT >= 31
                    ) {

                        alarmManager
                            .setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                alarm.timeInMillis,
                                pendingIntent
                            )

                    } else {

                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            alarm.timeInMillis,
                            pendingIntent
                        )
                    }

                } catch (
                    e: SecurityException
                ) {

                    Toast.makeText(
                        this,
                        "Exact alarm permission may be required.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        Toast.makeText(
            this,
            "Prayer notifications scheduled",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun timeInMinutes(
        calendar: Calendar
    ): Int {

        return calendar.get(
            Calendar.HOUR_OF_DAY
        ) * 60 +
                calendar.get(
                    Calendar.MINUTE
                )
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            val manager =
                getSystemService(
                    NOTIFICATION_SERVICE
                ) as NotificationManager

            val channel =
                NotificationChannel(
                    "prayer",
                    "Prayer Times",
                    NotificationManager.IMPORTANCE_HIGH
                )

            channel.description =
                "Noor-e-Quran prayer notifications"

            manager.createNotificationChannel(
                channel
            )
        }
    }

    // ---------------------------------------------------------
    // PERMISSIONS
    // ---------------------------------------------------------

    private fun requestNotifications() {

        if (
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS
                ),
                11
            )
        }
    }

    private fun requestLocation() {

        if (
            Build.VERSION.SDK_INT >= 23 &&
            checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                10
            )
        }
    }

    private fun requestLastLocation() {

        try {

            if (
                checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val manager =
                getSystemService(
                    LOCATION_SERVICE
                ) as LocationManager

            val providers =
                manager.getProviders(true)

            var best: Location? = null

            for (provider in providers) {

                val location =
                    manager.getLastKnownLocation(
                        provider
                    ) ?: continue

                if (
                    best == null ||
                    location.accuracy <
                    best!!.accuracy
                ) {
                    best = location
                }
            }

            best?.let {

                lat =
                    it.latitude

                lon =
                    it.longitude
            }

        } catch (_: Exception) {
        }
    }

    // ---------------------------------------------------------
    // COMPASS
    // ---------------------------------------------------------

    private fun setupSensors() {

        sensorManager =
            getSystemService(
                SENSOR_SERVICE
            ) as SensorManager

        val sensor =
            sensorManager?.getDefaultSensor(
                Sensor.TYPE_ROTATION_VECTOR
            )

        if (sensor != null) {

            sensorManager?.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    override fun onSensorChanged(
        event: SensorEvent
    ) {

        if (
            event.sensor.type !=
            Sensor.TYPE_ROTATION_VECTOR
        ) {
            return
        }

        val matrix =
            FloatArray(9)

        val orientation =
            FloatArray(3)

        SensorManager
            .getRotationMatrixFromVector(
                matrix,
                event.values
            )

        SensorManager
            .getOrientation(
                matrix,
                orientation
            )

        var degrees =
            Math.toDegrees(
                orientation[0].toDouble()
            ).toFloat()

        if (degrees < 0f) {
            degrees += 360f
        }

        heading =
            degrees

        qiblaView?.text =
            "Qibla: ${qiblaBearing.roundToInt()}°\n" +
                    "Phone heading: ${heading.roundToInt()}°"
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode == 10 &&
            grantResults.isNotEmpty() &&
            grantResults[0] ==
            PackageManager.PERMISSION_GRANTED
        ) {

            requestLastLocation()

            if (::content.isInitialized) {
                render()
            }
        }
    }

    private fun Double.f2(): String {

        return String.format(
            Locale.US,
            "%.4f",
            this
        )
    }
}

// ---------------------------------------------------------
// DATA CLASSES
// ---------------------------------------------------------

data class Ayah(
    val number: Int,
    val text: String
)

data class Surah(
    val number: Int,
    val name: String,
    val arabic: String,
    val ayahs: List<Ayah>
)

data class Dua(
    val title: String,
    val arabic: String,
    val meaning: String,
    val source: String,
    val use: String
)

// ---------------------------------------------------------
// TEXT WATCHER
// ---------------------------------------------------------

class SimpleWatcher(
    private val callback: (String) -> Unit
) : TextWatcher {

    override fun beforeTextChanged(
        s: CharSequence?,
        start: Int,
        count: Int,
        after: Int
    ) {
    }

    override fun onTextChanged(
        s: CharSequence?,
        start: Int,
        before: Int,
        count: Int
    ) {

        callback(
            s?.toString() ?: ""
        )
    }

    override fun afterTextChanged(
        s: Editable?
    ) {
    }
}

// ---------------------------------------------------------
// PRAYER CALCULATOR
// ---------------------------------------------------------

object PrayerCalc {

    private const val D2R =
        Math.PI / 180.0

    private const val R2D =
        180.0 / Math.PI

    fun times(
        date: Date,
        latitude: Double,
        longitude: Double,
        asrFactor: Int
    ): LinkedHashMap<String, String> {

        val calendar =
            Calendar.getInstance()

        calendar.time =
            date

        val year =
            calendar.get(Calendar.YEAR)

        val month =
            calendar.get(Calendar.MONTH) + 1

        val day =
            calendar.get(Calendar.DAY_OF_MONTH)

        val jd =
            367 * year -
                    floor(
                        7.0 *
                                (
                                        year +
                                                floor(
                                                    (month + 9) / 12.0
                                                )
                                        ) / 4.0
                    ) +
                    floor(
                        275.0 *
                                month / 9.0
                    ) +
                    day +
                    1721013.5

        val timezone =
            TimeZone
                .getDefault()
                .getOffset(date.time) /
                    3600000.0

        val result =
            calculate(
                jd,
                latitude,
                longitude,
                timezone,
                asrFactor
            )

        return linkedMapOf(

            "Fajr" to format(result[0]),

            "Sunrise" to format(result[1]),

            "Dhuhr" to format(result[2]),

            "Asr" to format(result[3]),

            "Maghrib" to format(result[4]),

            "Isha" to format(result[5])
        )
    }

    private fun calculate(
        jd: Double,
        latitude: Double,
        longitude: Double,
        timezone: Double,
        asrFactor: Int
    ): DoubleArray {

        val d =
            jd - 2451545.0

        val g =
            fix(
                357.529 +
                        0.98560028 * d
            )

        val q =
            fix(
                280.459 +
                        0.98564736 * d
            )

        val l =
            fix(
                q +
                        1.915 *
                        sin(g * D2R) +
                        0.020 *
                        sin(2 * g * D2R)
            )

        val e =
            23.439 -
                    0.00000036 * d

        val decl =
            asin(
                sin(e * D2R) *
                        sin(l * D2R)
            ) * R2D

        val ra =
            atan2(
                cos(e * D2R) *
                        sin(l * D2R),
                cos(l * D2R)
            ) * R2D / 15.0

        val equation =
            q / 15.0 - ra

        val noon =
            12.0 +
                    timezone -
                    longitude / 15.0 -
                    equation

        fun hour(angle: Double): Double {

            val x =
                (
                    -sin(angle * D2R) -
                            sin(latitude * D2R) *
                            sin(decl * D2R)
                    ) /
                        (
                            cos(latitude * D2R) *
                                    cos(decl * D2R)
                            )

            return acos(
                x.coerceIn(
                    -1.0,
                    1.0
                )
            ) *
                    R2D /
                    15.0
        }

        val sunrise =
            hour(0.833)

        val fajr =
            hour(18.0)

        val isha =
            hour(18.0)

        val asrAngle =
            R2D *
                    acot(
                        asrFactor +
                                tan(
                                    abs(
                                        latitude - decl
                                    ) * D2R
                                )
                    )

        return doubleArrayOf(

            noon - fajr,

            noon - sunrise,

            noon,

            noon + hour(asrAngle),

            noon + sunrise,

            noon + isha
        )
    }

    private fun acot(
        value: Double
    ): Double {

        return atan(
            1.0 / value
        )
    }

    private fun fix(
        value: Double
    ): Double {

        var result =
            value % 360.0

        if (result < 0) {
            result += 360.0
        }

        return result
    }

    private fun format(
        value: Double
    ): String {

        var hour =
            floor(value).toInt()

        var minute =
            round(
                (value - hour) * 60.0
            ).toInt()

        if (minute >= 60) {

            hour++

            minute -= 60
        }

        hour =
            (hour % 24 + 24) % 24

        val amPm =
            if (hour >= 12)
                "PM"
            else
                "AM"

        val displayHour =
            if (hour % 12 == 0)
                12
            else
                hour % 12

        return String.format(
            Locale.US,
            "%02d:%02d %s",
            displayHour,
            minute,
            amPm
        )
    }

    fun next(
        times: Map<String, String>
    ): Pair<String, String> {

        val now =
            Calendar.getInstance()

        val current =
            now.get(
                Calendar.HOUR_OF_DAY
            ) * 60 +
                    now.get(
                        Calendar.MINUTE
                    )

        for ((name, value) in times) {

            val parts =
                value.substring(
                    0,
                    5
                ).split(":")

            val hour12 =
                parts[0].toInt()

            val minute =
                parts[1].toInt()

            val pm =
                value.endsWith(
                    "PM",
                    ignoreCase = true
                )

            var hour24 =
                hour12 % 12

            if (pm) {
                hour24 += 12
            }

            val prayerMinutes =
                hour24 * 60 + minute

            if (prayerMinutes > current) {

                return name to value
            }
        }

        return times.entries
            .firstOrNull()
            ?.let {
                it.key to it.value
            }
            ?: ("Fajr" to "--:--")
    }

    fun qibla(
        latitude: Double,
        longitude: Double
    ): Double {

        val kaabaLat =
            21.4225 * D2R

        val kaabaLon =
            39.8262 * D2R

        val latRad =
            latitude * D2R

        val deltaLon =
            kaabaLon -
                    longitude * D2R

        var bearing =
            atan2(
                sin(deltaLon),
                cos(latRad) *
                        tan(kaabaLat) -
                        sin(latRad) *
                        cos(deltaLon)
            ) * R2D

        if (bearing < 0) {
            bearing += 360.0
        }

        return bearing
    }
}

// ---------------------------------------------------------
// HIJRI
// ---------------------------------------------------------

data class HijriDate(
    val day: Int,
    val month: Int,
    val year: Int,
    val monthName: String
)

object Hijri {

    private val names =
        listOf(
            "Muharram",
            "Safar",
            "Rabi al-Awwal",
            "Rabi al-Thani",
            "Jumada al-Awwal",
            "Jumada al-Thani",
            "Rajab",
            "Shaban",
            "Ramadan",
            "Shawwal",
            "Dhul Qadah",
            "Dhul Hijjah"
        )

    fun adjust(
        date: HijriDate,
        delta: Int
    ): HijriDate {

        var day =
            date.day + delta

        var month =
            date.month

        var year =
            date.year

        while (day > 30) {

            day -= 30

            month++

            if (month > 12) {

                month = 1

                year++
            }
        }

        while (day < 1) {

            day += 30

            month--

            if (month < 1) {

                month = 12

                year--
            }
        }

        return HijriDate(
            day,
            month,
            year,
            names[
                (month - 1)
                    .coerceIn(0, 11)
            ]
        )
    }

    fun fromGregorian(
        date: Date
    ): HijriDate {

        val calendar =
            Calendar.getInstance()

        calendar.time =
            date

        val year =
            calendar.get(Calendar.YEAR)

        val month =
            calendar.get(Calendar.MONTH) + 1

        val day =
            calendar.get(
                Calendar.DAY_OF_MONTH
            )

        val jd =
            (
                367 * year -
                        floor(
                            7.0 *
                                    (
                                        year +
                                                floor(
                                                    (month + 9) /
                                                            12.0
                                                )
                                        ) / 4.0
                        ) +
                        floor(
                            275.0 *
                                    month / 9.0
                        ) +
                        day +
                        1721013.5
                ).toInt()

        var l =
            jd - 1948440 + 10632

        val n =
            (l - 1) / 10631

        l =
            l -
                    10631 * n +
                    354

        val j =
            ((10985 - l) / 5316) *
                    ((50 * l) / 17719) +
                    (l / 5670) *
                    ((43 * l) / 15238)

        l =
            l -
                    ((30 - j) / 15) *
                    ((17719 * j) / 50) -
                    (j / 16) *
                    ((15238 * j) / 43) +
                    29

        val monthNumber =
            (24 * l) / 709

        val dayNumber =
            l -
                    (709 * monthNumber) / 24

        val yearNumber =
            30 * n +
                    j -
                    30

        return HijriDate(
            dayNumber,
            monthNumber,
            yearNumber,
            names[
                (monthNumber - 1)
                    .coerceIn(0, 11)
            ]
        )
    }
}

// ---------------------------------------------------------
// ADHKAR
// ---------------------------------------------------------

val ADHKAR =
    listOf(

        Triple(
            "Morning Adhkar",
            "Ayat al-Kursi; Surah Al-Ikhlas, Al-Falaq and An-Nas; and authentic morning remembrance.",
            "Use a trusted verified collection for exact wording and counts."
        ),

        Triple(
            "Evening Adhkar",
            "Ayat al-Kursi; Surah Al-Ikhlas, Al-Falaq and An-Nas; and authentic evening remembrance.",
            "Use a trusted verified collection."
        ),

        Triple(
            "After Prayer",
            "Astaghfirullah and established post-prayer remembrances.",
            "Sahih Muslim and verified collections."
        ),

        Triple(
            "Before Sleep",
            "Ayat al-Kursi, the last two verses of Al-Baqarah, and the three Quls.",
            "Verified hadith collections."
        ),

        Triple(
            "Upon Waking",
            "Alhamdulillah and the established waking remembrance.",
            "Sahih al-Bukhari and Muslim."
        ),

        Triple(
            "Protection",
            "Seek refuge in Allah and recite established protective supplications.",
            "Quran and authentic hadith collections."
        ),

        Triple(
            "Istighfar",
            "Astaghfirullaha wa atubu ilayh.",
            "Authentic hadith collections."
        ),

        Triple(
            "Salawat",
            "Allahumma salli wa sallim ala Muhammad.",
            "Use authentic sources for exact forms."
        )
    )

// ---------------------------------------------------------
// DUAS
// ---------------------------------------------------------

object DUAS {

    val list =
        listOf(

            Dua(
                "Dua for Guidance",
                "رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِنْ لَدُنْكَ رَحْمَةً ۚ إِنَّكَ أَنتَ الْوَهَّابُ",
                "Our Lord, do not let our hearts deviate after You have guided us, and grant us mercy from You.",
                "Qur'an 3:8",
                "Guidance and steadfastness"
            ),

            Dua(
                "Dua for Forgiveness",
                "رَبَّنَا ظَلَمْنَا أَنفُسَنَا وَإِن لَّمْ تَغْفِرْ لَنَا وَتَرْحَمْنَا لَنَكُونَنَّ مِنَ الْخَاسِرِينَ",
                "Our Lord, we have wronged ourselves. If You do not forgive us and have mercy on us, we will surely be among the losers.",
                "Qur'an 7:23",
                "Repentance"
            ),

            Dua(
                "Dua for Parents",
                "رَبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا",
                "My Lord, have mercy upon them as they raised me when I was small.",
                "Qur'an 17:24",
                "Parents"
            ),

            Dua(
                "Dua for Knowledge",
                "رَبِّ زِدْنِي عِلْمًا",
                "My Lord, increase me in knowledge.",
                "Qur'an 20:114",
                "Study and learning"
            ),

            Dua(
                "Dua for Ease",
                "رَبِّ اشْرَحْ لِي صَدْرِي ۝ وَيَسِّرْ لِي أَمْرِي",
                "My Lord, expand my chest and make my task easy for me.",
                "Qur'an 20:25–26",
                "Difficulty and confidence"
            ),

            Dua(
                "Dua for Good in Both Worlds",
                "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
                "Our Lord, give us good in this world and good in the Hereafter and protect us from the punishment of the Fire.",
                "Qur'an 2:201",
                "Comprehensive supplication"
            ),

            Dua(
                "Dua for Patience",
                "رَبَّنَا أَفْرِغْ عَلَيْنَا صَبْرًا وَتَوَفَّنَا مُسْلِمِينَ",
                "Our Lord, pour upon us patience and let us die as Muslims.",
                "Qur'an 7:126",
                "Patience"
            ),

            Dua(
                "Dua for Acceptance",
                "رَبَّنَا تَقَبَّلْ مِنَّا ۖ إِنَّكَ أَنتَ السَّمِيعُ الْعَلِيمُ",
                "Our Lord, accept from us; indeed You are the Hearing, the Knowing.",
                "Qur'an 2:127",
                "Good deeds"
            ),

            Dua(
                "Dua for Righteous Family",
                "رَبَّنَا هَبْ لَنَا مِنْ أَزْوَاجِنَا وَذُرِّيَّاتِنَا قُرَّةَ أَعْيُنٍ وَاجْعَلْنَا لِلْمُتَّقِينَ إِمَامًا",
                "Our Lord, grant us from our spouses and offspring comfort to our eyes and make us an example for the righteous.",
                "Qur'an 25:74",
                "Family"
            ),

            Dua(
                "Dua for Provision",
                "رَبِّ إِنِّي لِمَا أَنزَلْتَ إِلَيَّ مِنْ خَيْرٍ فَقِيرٌ",
                "My Lord, indeed I am in need of whatever good You send me.",
                "Qur'an 28:24",
                "Provision"
            ),

            Dua(
                "Dua of Prophet Yunus",
                "لَا إِلَٰهَ إِلَّا أَنتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ",
                "There is no deity except You; glory be to You. Indeed, I have been of the wrongdoers.",
                "Qur'an 21:87",
                "Distress"
            ),

            Dua(
                "Dua for Healing",
                "وَإِذَا مَرِضْتُ فَهُوَ يَشْفِينِ",
                "And when I am ill, it is He who cures me.",
                "Qur'an 26:80",
                "Illness"
            ),

            Dua(
                "Dua for Safety",
                "رَبِّ نَجِّنِي مِنَ الْقَوْمِ الظَّالِمِينَ",
                "My Lord, save me from the wrongdoing people.",
                "Qur'an 28:21",
                "Protection"
            ),

            Dua(
                "Dua for Gratitude",
                "رَبِّ أَوْزِعْنِي أَنْ أَشْكُرَ نِعْمَتَكَ الَّتِي أَنْعَمْتَ عَلَيَّ",
                "My Lord, enable me to be grateful for Your favor upon me.",
                "Qur'an 27:19",
                "Gratitude"
            ),

            Dua(
                "Dua for Light",
                "رَبَّنَا أَتْمِمْ لَنَا نُورَنَا وَاغْفِرْ لَنَا",
                "Our Lord, perfect for us our light and forgive us.",
                "Qur'an 66:8",
                "Faith"
            ),

            Dua(
                "Dua for Mercy",
                "رَبَّنَا آتِنَا مِن لَّدُنكَ رَحْمَةً وَهَيِّئْ لَنَا مِنْ أَمْرِنَا رَشَدًا",
                "Our Lord, grant us mercy from Yourself and guide our affair to what is right.",
                "Qur'an 18:10",
                "Guidance"
            ),

            Dua(
                "Dua for Straight Path",
                "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ",
                "Guide us to the straight path.",
                "Qur'an 1:6",
                "Every prayer"
            ),

            Dua(
                "Dua for Mercy and Forgiveness",
                "رَبِّ اغْفِرْ وَارْحَمْ وَأَنتَ خَيْرُ الرَّاحِمِينَ",
                "My Lord, forgive and have mercy, and You are the best of the merciful.",
                "Qur'an 23:118",
                "Forgiveness"
            ),

            Dua(
                "Dua for Wisdom",
                "رَبِّ هَبْ لِي حُكْمًا وَأَلْحِقْنِي بِالصَّالِحِينَ",
                "My Lord, grant me wisdom and join me with the righteous.",
                "Qur'an 26:83",
                "Wisdom"
            ),

            Dua(
                "Dua for Paradise",
                "وَاجْعَلْنِي مِن وَرَثَةِ جَنَّةِ النَّعِيمِ",
                "Make me among the inheritors of the Garden of Bliss.",
                "Qur'an 26:85",
                "Hereafter"
            ),

            Dua(
                "Dua for Trust",
                "حَسْبِيَ اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ ۖ عَلَيْهِ تَوَكَّلْتُ",
                "Allah is sufficient for me; there is no deity except Him. Upon Him I rely.",
                "Qur'an 9:129",
                "Reliance on Allah"
            )
        )
}
