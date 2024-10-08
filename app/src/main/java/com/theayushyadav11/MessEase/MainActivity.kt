package com.theayushyadav11.MessEase


import android.Manifest
import android.app.AlarmManager
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.theayushyadav11.MessEase.Models.User
import com.theayushyadav11.MessEase.databinding.ActivityMainBinding
import com.theayushyadav11.MessEase.notifications.AlarmReceiver
import com.theayushyadav11.MessEase.ui.MessCommittee.activities.MessCommitteeMain
import com.theayushyadav11.MessEase.ui.more.PaymentActivity
import com.theayushyadav11.MessEase.ui.more.ReviewActivity
import com.theayushyadav11.MessEase.ui.more.SettingsActivity
import com.theayushyadav11.MessEase.ui.splash.fragments.LoginAndSignUpActivity
import com.theayushyadav11.MessEase.utils.Constants.Companion.COORDINATOR
import com.theayushyadav11.MessEase.utils.Constants.Companion.DEVELOPER
import com.theayushyadav11.MessEase.utils.Constants.Companion.USERS
import com.theayushyadav11.MessEase.utils.Constants.Companion.auth
import com.theayushyadav11.MessEase.utils.Constants.Companion.fireBase
import com.theayushyadav11.MessEase.utils.Constants.Companion.firestoreReference
import com.theayushyadav11.MessEase.utils.FireBase
import com.theayushyadav11.MessEase.utils.Mess
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var supabase: SupabaseClient
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var mess: Mess
    private lateinit var alarmManager: AlarmManager
    private val REQUEST_CODE_POST_NOTIFICATIONS = 1
    private val REQUEST_CODE_SCHEDULE_EXACT_ALARM = 2
    private lateinit var analytics: FirebaseAnalytics

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initialise()
        setUpNav()
        setUpHeader()
        createNotificationChannel()
        askForNotificationPermission()
        fireBase.getToken()
        //setSupaBase()


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initialise() {
        mess = Mess(this)
        mess.setIsLoggedIn(true)
        analytics = FirebaseAnalytics.getInstance(this)
        registerReceiver(
            onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            RECEIVER_NOT_EXPORTED
        )
    }
    private fun setSupaBase() {
        supabase = createSupabaseClient(
            supabaseUrl = "https://yrkrbtqdyknfawyjnuhs.supabase.co/",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inlya3JidHFkeWtuZmF3eWpudWhzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MjYzMzM2MDIsImV4cCI6MjA0MTkwOTYwMn0.psr8wK4USbjkdoW0uyNoK0RY0tN3fEHQ7JYjMfIpggI"
        ) {
            install(Postgrest)
            install(Realtime)

        }
        addusers()
    }
    fun addusers()
    {
        firestoreReference.collection(USERS).get().addOnSuccessListener {
            for(document in it.documents)
            {
                val user = document.toObject(User::class.java)
                lifecycleScope.launch {
                    if (user != null) {
                        try {
                            supabase.postgrest["User"].insert(
                                user
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

            }

        }
    }


    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            Toast.makeText(context, "Download completed!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }

            R.id.action_payment -> {
                startActivity(Intent(this, PaymentActivity::class.java))
                true
            }

            R.id.action_review -> {
                startActivity(Intent(this, ReviewActivity::class.java))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setDrawerClickListener(
        navView: NavigationView, drawerLayout: DrawerLayout, navController: NavController
    ) {
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    firestoreReference.collection("Users")
                        .document(auth.currentUser?.uid.toString()).update("token", "")
                    signOut()
                    val intent = Intent(this@MainActivity, LoginAndSignUpActivity::class.java)
                    mess.setIsLoggedIn(false)
                    mess.setUser(User())
                    startActivity(intent)
                    finish()
                    true
                }

                R.id.nav_messCommitteeActivity -> {
                     val user=mess.getUser()
                        if (user.member && auth.currentUser != null) {
                            val intent = Intent(this, MessCommitteeMain::class.java)
                            startActivity(intent)
                        } else {


                            if (!isFinishing && !isDestroyed) {
                                mess.showAlertDialog(
                                    "Alert!", "You are not a member of Mess Committee!", "Ok", ""
                                ) {}
                            }
                        }

                    true
                }

                R.id.nav_download -> {


                    firestoreReference.collection("MainMenu").document("url").get()
                        .addOnSuccessListener { value ->
                            val url = value?.get("url").toString()
                            mess.downloadFile(url)

                        }.addOnFailureListener { error ->
                            mess.toast("Failed to download menu")
                        }
                    true
                }

                else -> {
                    navController.navigate(menuItem.itemId)
                    drawerLayout.closeDrawers()
                    true
                }
            }
        }
    }

    private fun setUpNav() {
        setSupportActionBar(binding.appBarMain.toolbar)
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_messCommitteeActivity, R.id.nav_admin, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        setDrawerClickListener(navView, drawerLayout, navController)

        val menu: Menu = navView.menu
        val admin = menu.findItem(R.id.nav_admin)
        val user=mess.getUser()
            if (!(user.designation == COORDINATOR || user.designation == DEVELOPER)) {
                admin.isVisible = false
            }
    }

    private fun setUpHeader() {
        val headerView: View = binding.navView.getHeaderView(0)
        val layout: LinearLayout = headerView.findViewById(R.id.navMain)

            if (!isFinishing && !isDestroyed) {
                val user=mess.getUser()
                mess.loadCircleImage(user.photoUrl, layout.findViewById(R.id.propic))
                layout.findViewById<TextView>(R.id.mname).text = user.name
                layout.findViewById<TextView>(R.id.email).text = user.email
            }
         }

    private fun signOut() {
        var mAuth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()
        val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        mAuth.signOut()

        mGoogleSignInClient.signOut().addOnCompleteListener(this) {
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()


        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "getString(R.string.channel_name)"
            val descriptionText = "getString(R.string.channel_description)"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("DailyNotification", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            cancelAllAlarms(this@MainActivity)
            setAlarm()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onRestart() {
        super.onRestart()
        try {
            cancelAllAlarms(this@MainActivity)
            setAlarm()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setAlarm() {
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this@MainActivity, AlarmReceiver::class.java)

        val times = listOf(
            mess.get("bt", "7:30"),
            mess.get("lt", "12:0"),
            mess.get("st", "16:30"),
            mess.get("dt", "19:0")
        )
        //val times = listOf("20:14","20:15","20:11","20:12")
        mess.log(
            "Ayush" + mess.get("bt", "7:30") + mess.get("lt", "12:0") + mess.get(
                "st", "16:30"
            ) + mess.get("dt", "19:0")
        )
        for (i in times.indices) {

            val calendar = Calendar.getInstance()
            val timeParts = times[i].split(":")
            calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
            calendar.set(Calendar.MINUTE, timeParts[1].toInt())
            calendar.set(Calendar.SECOND, 0)
            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            intent.putExtra("type", i)
            val pendingIntent = PendingIntent.getBroadcast(
                this@MainActivity,
                times[i].hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
            )
        }
    }
    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_POST_NOTIFICATIONS
                )
            } else {
                askForExactAlarmPermission()
            }
        } else {
            askForExactAlarmPermission()
        }
    }
    private fun askForExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    data = Uri.parse("package:$packageName")
                }
                startActivityForResult(intent, REQUEST_CODE_SCHEDULE_EXACT_ALARM)
            } else {
                cancelAllAlarms(this@MainActivity)
                setAlarm()
            }
        } else {
            cancelAllAlarms(this@MainActivity)
            setAlarm()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_POST_NOTIFICATIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    askForExactAlarmPermission()
                }
            }
        }
    }

    private fun cancelAllAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }


    @RequiresApi(Build.VERSION_CODES.S)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCHEDULE_EXACT_ALARM) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (alarmManager.canScheduleExactAlarms()) {
                cancelAllAlarms(this@MainActivity)
                setAlarm()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadComplete)
    }

}