package com.example.gymbuddy_tabatatimer.view

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.DialogInterface
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat.IntentBuilder
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gymbuddy_tabatatimer.*
import com.example.gymbuddy_tabatatimer.databinding.ActivityMainBinding
import com.example.gymbuddy_tabatatimer.helpers.Constants
import com.example.gymbuddy_tabatatimer.helpers.Constants.CREATE_FILE
import com.example.gymbuddy_tabatatimer.helpers.Constants.FILE_SHARING
import com.example.gymbuddy_tabatatimer.helpers.Constants.MODE_ADD
import com.example.gymbuddy_tabatatimer.helpers.Constants.MODE_DELETE
import com.example.gymbuddy_tabatatimer.helpers.Constants.MODE_SAVE_TO_CLOUD
import com.example.gymbuddy_tabatatimer.helpers.Constants.MODE_SAVE_TO_FILE
import com.example.gymbuddy_tabatatimer.helpers.Constants.MODE_SHARE
import com.example.gymbuddy_tabatatimer.helpers.Constants.PICK_FILE
import com.example.gymbuddy_tabatatimer.helpers.Helpers
import com.example.gymbuddy_tabatatimer.helpers.Utils
import com.example.gymbuddy_tabatatimer.model.Tabata
import com.example.gymbuddy_tabatatimer.recyclerViewAdapters.TabatasRVAdapter
import com.example.gymbuddy_tabatatimer.viewModel.MainViewModel
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {

    private var tabatas = ArrayList<Tabata>()
    private var tabatasToSave = ArrayList<Tabata>()
    private var tabatasToLoad = ArrayList<Tabata>()
    private lateinit var adapter: TabatasRVAdapter
    private var doubleBackToExitPressedOnce = false
    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    private var authMenuItem: MenuItem? = null
    private val TAG = "MainActivity"
    private var db = Firebase.firestore
    private var gson: Gson = Gson()
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private val signInLauncher = registerForActivityResult(FirebaseAuthUIActivityResultContract()) { res -> onSignInResult(res) }
    private var userRef: DocumentReference? = null
    private var tryingToSave = false
    private var tryingToLoad = false
    private var tryingToDelete = false
    private var directoryFile: File? = null
    private var mRewardedAd: RewardedAd? = null

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        authMenuItem = menu?.findItem(R.id.auth)
        if (viewModel.user.value != null) menu?.findItem(R.id.auth)?.title = resources.getString(R.string.signOut)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.submenuShare -> handlePickToExport(MODE_SHARE)
            R.id.importFromFile -> openFile()
            R.id.exportToFile -> handlePickToExport(MODE_SAVE_TO_FILE)
            R.id.auth -> handleAuth()
            R.id.loadFromCloud -> {
                Firebase.auth.currentUser?.let {
                    handleLoadFromCloud(MODE_ADD)
                } ?: run {
                    handleAuth()
                    tryingToLoad = true
                }
            }
            R.id.saveToCloud -> {
                Firebase.auth.currentUser?.let {
                    handlePickToExport(MODE_SAVE_TO_CLOUD)
                } ?: run {
                    handleAuth()
                    tryingToSave = true
                }
            }
            R.id.deleteFromCloud -> {
                Firebase.auth.currentUser?.let {
                    handleLoadFromCloud(MODE_DELETE)
                } ?: run {
                    handleAuth()
                    tryingToDelete = true
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handlePickToExport(mode: String) {
        val areChecked = BooleanArray(tabatas.size)
        val workoutNames = arrayOfNulls<String>(tabatas.size)
        for (i in tabatas.indices) {
            workoutNames[i] = tabatas[i].name
            areChecked[i] = false
        }
        AlertDialog.Builder(this, R.style.DefaultAlertDialogTheme)
            .setTitle(
                when (mode) {
                    MODE_SHARE -> R.string.selectWorkoutsToShare
                    else -> R.string.selectWorkoutsToSave
                }
            )
            .setIcon(
                when (mode) {
                    MODE_SHARE -> R.drawable.ic_share
                    else -> R.drawable.ic_save
                }
            )
            .setMultiChoiceItems(workoutNames, areChecked) { _: DialogInterface?, which: Int, isChecked: Boolean -> areChecked[which] = isChecked }
            .setPositiveButton(
                when (mode) {
                    MODE_SHARE -> R.string.share
                    else -> R.string.save
                }
            ) { _, _ ->
                val exportedTabatas: ArrayList<Tabata> = ArrayList()
                val size: Int = tabatas.size
                for (i in 0 until size) {
                    if (areChecked[i]) {
                        exportedTabatas.add(tabatas[i])
                    }
                }
                if (exportedTabatas.isEmpty()) {
                    Toast.makeText(this@MainActivity, R.string.noWorkoutsSelected, Toast.LENGTH_SHORT).show()
                } else {
                    when (mode) {
                        MODE_SHARE -> handleShare(exportedTabatas)
                        MODE_SAVE_TO_FILE -> handleSaveToFile(exportedTabatas)
                        MODE_SAVE_TO_CLOUD -> handleSaveToCloud(exportedTabatas)
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun handleLoadFromCloud(mode: String) {
        Firebase.auth.currentUser?.let { user ->
            val usersRef = db.collection("users")
            val query = usersRef.whereEqualTo("id", user.uid)
            query.get().addOnCompleteListener { task ->
                task.result?.let {
                    if (task.result.size() == 0) {
                        Toast.makeText(this@MainActivity, R.string.noWorkoutsToLoad, Toast.LENGTH_SHORT).show()
                    } else {
                        userRef = task.result.documents[0].reference
                        val tabatasRef = userRef?.collection("tabatas")
                        tabatasRef?.orderBy("timestamp")?.get()?.addOnCompleteListener { task ->
                            val queriedTabatas = ArrayList<Tabata>()
                            for (r in task.result) {
                                queriedTabatas.add(r.toObject(Tabata::class.java))
                            }
                            when (mode) {
                                MODE_ADD -> handlePickFromCloud(queriedTabatas, null, MODE_ADD)
                                MODE_DELETE -> handlePickFromCloud(queriedTabatas, tabatasRef, MODE_DELETE)
                            }

                        }
                    }
                }
            }
        }
    }

    private fun handlePickFromCloud(queriedTabatas: ArrayList<Tabata>, tabatasRef: CollectionReference?, mode: String) {
        if (queriedTabatas.isEmpty()) {
            Toast.makeText(this@MainActivity, R.string.noWorkoutsToLoad, Toast.LENGTH_SHORT).show()
        } else {
            val areChecked = BooleanArray(queriedTabatas.size)
            val workoutNames = arrayOfNulls<String>(queriedTabatas.size)
            for (i in queriedTabatas.indices) {
                workoutNames[i] = queriedTabatas[i].name
                areChecked[i] = false
            }
            AlertDialog.Builder(this@MainActivity, R.style.DefaultAlertDialogTheme)
                .setTitle(
                    when (mode) {
                        MODE_DELETE -> R.string.selectWorkoutsToDelete
                        else -> R.string.selectWorkoutsToLoad
                    }
                )
                .setIcon(
                    when (mode) {
                        MODE_DELETE -> R.drawable.ic_delete
                        else -> R.drawable.ic_load
                    }
                )
                .setMultiChoiceItems(workoutNames, areChecked) { _: DialogInterface?, which12: Int, isChecked: Boolean -> areChecked[which12] = isChecked }
                .setPositiveButton(
                    when (mode) {
                        MODE_DELETE -> R.string.delete
                        else -> R.string.load
                    }
                ) { _, _ ->
                    when (mode) {
                        MODE_DELETE -> handleDeleteFromCloud(queriedTabatas, areChecked, tabatasRef)
                        else -> handleAddFromCloud(queriedTabatas, areChecked)
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun handleDeleteFromCloud(queriedTabatas: ArrayList<Tabata>, areChecked: BooleanArray, tabatasRef: CollectionReference?) {
        for (i in queriedTabatas.indices) {
            if (areChecked[i]) {
                val tabataRef = tabatasRef?.document(queriedTabatas[i].cloudId)
                tabataRef?.delete()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "${queriedTabatas[i].name} - ${resources.getString(R.string.deleted)}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "${queriedTabatas[i].name} - ${resources.getString(R.string.unableToDelete)}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun handleAddFromCloud(queriedTabatas: ArrayList<Tabata>, areChecked: BooleanArray) {
        var removed = 0
        val size: Int = queriedTabatas.size
        for (i in 0 until size) {
            if (areChecked[i]) {
                queriedTabatas[i - removed].let {
                    it.id = Utils.getInstance(this).getTabatasID()
                    tabatas.add(it)
                    Toast.makeText(this, "${it.durationTotal}", Toast.LENGTH_SHORT).show()
                }
                queriedTabatas.removeAt(i - removed)
                removed++
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun handleSaveToCloud(tabatas: ArrayList<Tabata>) {
        Firebase.auth.currentUser?.let { user ->
            val usersRef = db.collection("users")
            val query = usersRef.whereEqualTo("id", user.uid)
            query.get().addOnCompleteListener { task ->
                if (task.result.size() == 0) {
                    addNewUser(tabatas)
                } else {
                    userRef = task.result.documents[0].reference
                    addTabatasToUser(tabatas)
                }

            }
        }
    }

    private fun addTabatasToUser(tabatas: ArrayList<Tabata>) {
        for (t in tabatas) {
            if (t.cloudId == "") {
                insertTabataToCloud(t)
            } else {
                userRef?.collection("tabatas")?.whereEqualTo("cloudId", t.cloudId)?.get()?.addOnCompleteListener { task ->
                    if (task.result.isEmpty) {
                        insertTabataToCloud(t)
                    } else {
                        updateTabataInCloud(task.result.documents[0].reference, t)
                    }
                }
            }
        }
    }

    private fun updateTabataInCloud(tabataRef: DocumentReference, tabata: Tabata) {
        tabataRef.update(
            "name", tabata.name,
            "defPrep", tabata.defPrep,
            "defRounds", tabata.defRounds,
            "parts", tabata.parts,
            "state", tabata.state,
            "durationTotal", tabata.durationTotal,
            "timestamp", FieldValue.serverTimestamp()
        )
            .addOnSuccessListener { Toast.makeText(this, "${tabata.name} - ${resources.getString(R.string.updated)}", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(this, "${tabata.name} - ${resources.getString(R.string.unableToUpdate)}", Toast.LENGTH_SHORT).show() }
    }

    private fun insertTabataToCloud(t: Tabata) {
        userRef?.collection("tabatas")?.add(tabataToHashmap(t))
            ?.addOnSuccessListener { tabataRef ->
                Toast.makeText(this, "${t.name} - ${resources.getString(R.string.saved)}", Toast.LENGTH_SHORT).show()
                tabataRef.update("cloudId", tabataRef.id)
                t.cloudId = tabataRef.id
                tabataRef.update("timestamp", FieldValue.serverTimestamp())
            }
            ?.addOnFailureListener { _ ->
                Toast.makeText(this, "${t.name} - ${resources.getString(R.string.unableToSave)}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun tabataToHashmap(tabata: Tabata): HashMap<String, Any?> {
        return hashMapOf(
            "id" to tabata.id,
            "name" to tabata.name,
            "defPrep" to tabata.defPrep,
            "defRounds" to tabata.defRounds,
            "parts" to tabata.parts,
            "state" to tabata.state,
            "duration" to tabata.durationTotal,
            "timestamp" to null
        )
    }

    private fun addNewUser(tabatas: ArrayList<Tabata>) {
        val newUser = hashMapOf(
            "id" to Firebase.auth.currentUser!!.uid
        )
        db.collection("users").add(newUser).addOnSuccessListener { documentReference ->
            Log.d(TAG, "added: ${documentReference.id}")
            userRef = documentReference
            addTabatasToUser(tabatas)
        }
            .addOnFailureListener { e -> Log.d(TAG, "failed to add: $e") }
    }

    private fun handleAuth() {
        if (viewModel.user.value == null) {
            val providers = arrayListOf(
                AuthUI.IdpConfig.EmailBuilder().build(),
                AuthUI.IdpConfig.PhoneBuilder().build(),
                AuthUI.IdpConfig.GoogleBuilder().build()
            )

            val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build()
            signInLauncher.launch(signInIntent)
        } else {
            AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener {
                    Toast.makeText(this, resources.getString(R.string.signedOut), Toast.LENGTH_LONG).show()
                    viewModel.setUser(null)
                }
        }
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            viewModel.setUser(FirebaseAuth.getInstance().currentUser)
            when (tryingToSave || tryingToLoad || tryingToDelete) {
                tryingToLoad -> {
                    tryingToLoad = false
                    handleLoadFromCloud(MODE_ADD)
                }
                tryingToSave -> {
                    tryingToSave = false
                    handlePickToExport(MODE_SAVE_TO_CLOUD)
                }
                else -> {
                    tryingToDelete = false
                    handleLoadFromCloud(MODE_DELETE)
                }
            }
        } else {
            viewModel.setUser(null)
            Toast.makeText(this, resources.getString(R.string.errorSigningIn), Toast.LENGTH_LONG).show()
            Log.d(TAG, "onSignInResult: $response")
        }
    }

    private fun createFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "myTabata.txt")
        }
        startActivityForResult(intent, CREATE_FILE)
    }

    private fun saveToFile(uri: Uri, contentResolver: ContentResolver) {
        try {
            contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { file ->
                    file.write((gson.toJson(tabatasToSave)).toByteArray())
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            Log.d(TAG, "onActivityResult: $e")
        } catch (e: IOException) {
            e.printStackTrace()
            Log.d(TAG, "onActivityResult: $e")
        }
    }

    private fun loadFromFile(uri: Uri, contentResolver: ContentResolver) {
        val stringBuilder = StringBuilder()
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
            }
            val type = object : TypeToken<ArrayList<Tabata?>?>() {}.type
            tabatasToLoad = gson.fromJson(stringBuilder.toString(), type)
            handlePickToImport(tabatasToLoad)
        }
    }

    private fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/*"
        }
        startActivityForResult(intent, PICK_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if (requestCode == FILE_SHARING) {
            if (directoryFile?.delete() == true) {
                Log.d(TAG, "onActivityResult: shared file deleted")
                directoryFile = null
            } else Log.d(TAG, "onActivityResult: failed to delete")
        }

        if (resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                val contentResolver = applicationContext.contentResolver
                when (requestCode) {
                    CREATE_FILE -> saveToFile(uri, contentResolver)
                    PICK_FILE -> loadFromFile(uri, contentResolver)
                }
            }
        }
    }

    private fun handleShare(tabatas: ArrayList<Tabata>) {

        val directory = File(filesDir.toString())
        if (!directory.exists()) {
            directory.mkdirs()
        }
        directoryFile = File("$directory/gbttShare.txt")
        directoryFile?.let {
            try {
                val shareIntent = IntentBuilder(this).setType("text/plain")
                val uri = FileProvider.getUriForFile(this, "com.example.gymbuddy_tabatatimer.fileprovider", it)
                val contentResolver = applicationContext.contentResolver
                contentResolver.openFileDescriptor(uri, "w")?.use {
                    FileOutputStream(it.fileDescriptor).use { file ->
                        file.write((gson.toJson(tabatas)).toByteArray())
                    }
                }
                shareIntent.addStream(uri)
                val intent = shareIntent.intent
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivityForResult(Intent.createChooser(intent, getString(R.string.shareWorkoutsVia)), FILE_SHARING)
//            directory.delete()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                Log.d(TAG, "onActivityResult: $e")
            } catch (e: IOException) {
                e.printStackTrace()
                Log.d(TAG, "onActivityResult: $e")
            }
        }
    }

    private fun handlePickToImport(returnList: ArrayList<Tabata>) {
        if (returnList.isEmpty()) {
            Toast.makeText(this@MainActivity, R.string.noWorkoutsToLoad, Toast.LENGTH_SHORT).show()
        } else {
            val areChecked = BooleanArray(returnList.size)
            val workoutNames = arrayOfNulls<String>(returnList.size)
            for (i in returnList.indices) {
                workoutNames[i] = returnList[i].name
                areChecked[i] = false
            }
            AlertDialog.Builder(this@MainActivity, R.style.DefaultAlertDialogTheme)
                .setTitle(R.string.selectWorkoutsToLoad)
                .setIcon(R.drawable.ic_load)
                .setMultiChoiceItems(workoutNames, areChecked) { _: DialogInterface?, which12: Int, isChecked: Boolean -> areChecked[which12] = isChecked }
                .setPositiveButton(R.string.load) { _, _ ->
                    var removed = 0
                    val size: Int = returnList.size
                    for (i in 0 until size) {
                        if (areChecked[i]) {
                            returnList[i - removed].let {
                                it.id = Utils.getInstance(this).getTabatasID()
                                tabatas.add(it)
                            }
                            returnList.removeAt(i - removed)
                            removed++
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun handleSaveToFile(tabatas: ArrayList<Tabata>) {
        tabatasToSave.clear()
        tabatasToSave = tabatas
        createFile()
    }

    override fun onResume() {
        if (Helpers.modifiedTabata != null) {
            for (i in tabatas.indices) {
                if (tabatas[i].id == Helpers.modifiedTabata?.id) {
                    tabatas[i] = Helpers.modifiedTabata!!
                    adapter.notifyItemChanged(i)
                    break
                }
            }
        }
        Helpers.modifiedTabata = null
        super.onResume()
    }

    override fun onPause() {
        Utils.getInstance(this).updateTabatas(tabatas)
        super.onPause()
    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }
        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, resources.getString(R.string.pressBackToExit), Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_GymBuddyTabataTimer)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        Utils.getInstance(this).setLastAdShown(0)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        viewModel.setUser(Firebase.auth.currentUser)

        viewModel.rewardGranted.observe(this, { rewardGranted ->
            binding.mainAdContainer.visibility = if (rewardGranted) View.GONE else View.VISIBLE
            if (rewardGranted) binding.rewardedAdButton.visibility = View.GONE
        })

        viewModel.setRewardGranted(Helpers.rewardGrantedThisSession)

        viewModel.user.observe(this, {
            if (it != null) {
                authMenuItem?.title = resources.getText(R.string.signOut)
                Toast.makeText(this, "${resources.getString(R.string.signedInAs)} ${Firebase.auth.currentUser?.phoneNumber ?: ""}${Firebase.auth.currentUser?.email ?: ""}", Toast.LENGTH_LONG).show()
            } else {
                authMenuItem?.title = resources.getText(R.string.signIn)
            }
        })

        MobileAds.initialize(this) { initializationStatus: InitializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            for (adapterClass in statusMap.keys) {
                val status = statusMap[adapterClass]
                Log.d(
                    "GB", String.format(
                        "Adapter name: %s, Description: %s, Latency: %d",
                        adapterClass, status!!.description, status.latency
                    )
                )
            }
            Helpers.handleNativeAds(mainAdTemplate, this, Constants.AD_ID_MAIN_NATIVE, null, Helpers.isRewardGranted(Utils.getInstance(this).getRewardGranted()))
            if (!Helpers.rewardGrantedThisSession) loadRewardedAd()
        }

        supportActionBar?.let { Helpers.setupActionBar(resources.getString(R.string.tabataTimer), "", it, this) }
        Helpers.showRatingUserInterface(this)

        tabatas = initSampleTabatas()
        tabatasRV.layoutManager = LinearLayoutManager(this)
        adapter = TabatasRVAdapter(tabatas = tabatas, listener = { onItemClick(it) }, doubleTapListener = { onItemDoubleTap(it) })
        tabatasRV.adapter = adapter
        val itemTouchHelper = ItemTouchHelper(simpleCallback)
        itemTouchHelper.attachToRecyclerView(tabatasRV)

        btnAddTabata.setOnClickListener {
            handleAddTabata()
        }

        binding.rewardedAdButton.setOnClickListener {
            AlertDialog.Builder(this, R.style.DefaultAlertDialogTheme)
                .setTitle(R.string.disableAds)
                .setMessage(R.string.disableAdsInfo)
                .setIcon(R.drawable.ic_info)
                .setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
                    if (mRewardedAd != null) {
                        mRewardedAd?.show(this) {
                            grantReward()
                        }
                    } else {
                        AlertDialog.Builder(this, R.style.DefaultAlertDialogTheme)
                            .setTitle(R.string.failedToLoad)
                            .setMessage(R.string.failedToLoadInfo)
                            .setIcon(R.drawable.ic_info)
                            .setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
                                grantReward()
                            }
                            .show()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }


    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, Constants.AD_ID_REWARDED, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
//                Toast.makeText(this@MainActivity, "failed to load $adError", Toast.LENGTH_LONG).show()
                mRewardedAd = null
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                binding.rewardedAdButton.visibility = View.VISIBLE
                mRewardedAd = rewardedAd
            }
        })
    }

    private fun grantReward() {
        viewModel.setRewardGranted(true)
        Utils.getInstance(this).setRewardGranted(System.currentTimeMillis())
        Helpers.rewardGrantedThisSession = true
    }

    private fun handleAddTabata(name: String = "", edited: Boolean = false, position: Int = 0) {
        val dialogView = layoutInflater.inflate(R.layout.til_dialog, null)
        val tabataName = dialogView.findViewById<EditText>(R.id.edtDialog)
        val tabataTil: TextInputLayout = dialogView.findViewById(R.id.tilDialog)
        tabataTil.setHint(R.string.name)
        tabataName.setText(name)
        tabataName.setSelection(tabataName.text.length)
        tabataName.requestFocus()
        val title = if (name == "") resources.getString(R.string.addNewTabata) else resources.getString(R.string.editName)
        AlertDialog.Builder(this@MainActivity, R.style.DefaultAlertDialogTheme)
            .setTitle(title)
            .setView(dialogView)
            .setIcon(R.drawable.ic_add_tabata)
            .setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                if (tabataName.text.isEmpty()) {
                    Toast.makeText(this, resources.getString(R.string.insertName), Toast.LENGTH_SHORT).show()
                    handleAddTabata(name, edited, position)
                } else {
                    if (!edited) {
                        val newTabata = Tabata(
                            Utils.getInstance(this).getTabatasID(),
                            tabataName.text.toString()
                        )
                        Utils.getInstance(this).addTabata(newTabata)
                        tabatas.add(newTabata)
                        adapter.notifyItemInserted(tabatas.lastIndex)
//                        addToDb(newTabata)
                    } else {
                        tabatas[position].name = tabataName.text.toString()
                        adapter.notifyItemChanged(position)
                        Utils.getInstance(this).updateTabatas(tabatas)
                    }
                    Helpers.hideKeyboard(applicationContext, tabataName)
                }
            }
            .setNegativeButton(resources.getString(R.string.cancel), null)
            .show()
        tabataName.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    private fun onItemClick(position: Int) {
        val intent = Intent(this, PartsActivity::class.java)
        intent.putExtra(Constants.TABATA_KEY, tabatas[position])
        startActivity(intent)
    }

    private fun onItemDoubleTap(position: Int) {
        handleAddTabata(tabatas[position].name, true, position)
    }

    private var simpleCallback: ItemTouchHelper.SimpleCallback = object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN or
                ItemTouchHelper.START or ItemTouchHelper.END, ItemTouchHelper.RIGHT
    ) {

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            if (viewHolder != null) {
                viewHolder.itemView.isPressed = true
            }
            super.onSelectedChanged(viewHolder, actionState)
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            viewHolder.itemView.isPressed = false
            super.clearView(recyclerView, viewHolder)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val fromPosition = viewHolder.adapterPosition
            val toPosition = target.adapterPosition
            tabatas = Utils.getInstance(this@MainActivity).getAllTabatas()!!
            Collections.swap(tabatas, fromPosition, toPosition)
            adapter.notifyItemMoved(fromPosition, toPosition)
            Utils.getInstance(this@MainActivity).updateTabatas(tabatas)
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
//            Utils.getInstance(this@MainActivity).deleteTabata(tabatas[position])
            val deletedTabata = tabatas[position]
            tabatas.removeAt(position)
            adapter.notifyItemRemoved(position)
            adapter.notifyItemRangeChanged(position, 1)
            Snackbar.make(tabatasRV, resources.getString(R.string.tabataDeleted), Snackbar.LENGTH_LONG)
                .setAction(resources.getString(R.string.undo)) {
                    tabatas.add(position, deletedTabata)
                    adapter.notifyItemInserted(position)
//                    Utils.getInstance(this@MainActivity).updateTabatas(tabatas)
                }
                .setActionTextColor(ContextCompat.getColor(applicationContext, R.color.purple_500))
                .show()
        }

        override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                val itemView = viewHolder.itemView
                val itemHeight = itemView.bottom - itemView.top
                val isCanceled = dX == 0f && !isCurrentlyActive
                val deleteIcon =
                    ContextCompat.getDrawable(applicationContext, R.drawable.ic_delete_swipe)
                val intrinsicWidth = deleteIcon!!.intrinsicWidth
                val intrinsicHeight = deleteIcon.intrinsicHeight
                val background =
                    ContextCompat.getDrawable(applicationContext, R.drawable.ic_background)

                if (isCanceled) {
                    clearCanvas(c, itemView.left + dX, itemView.top.toFloat(), itemView.right + dX, itemView.bottom.toFloat())
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    return
                }

                background!!.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt() + 30, itemView.bottom)
                background.draw(c)

                val deleteIconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
                val deleteIconMargin = (itemHeight - intrinsicHeight) / 2
                val deleteIconLeft = itemView.left + deleteIconMargin
                val deleteIconRight = itemView.left + deleteIconMargin + intrinsicWidth
                val deleteIconBottom = deleteIconTop + intrinsicHeight

                deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
                deleteIcon.draw(c)

            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }

        fun clearCanvas(c: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
            val clearPaint = Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
            c?.drawRect(left, top, right, bottom, clearPaint)
        }
    }

    private fun initSampleTabatas(): ArrayList<Tabata> {
        val sampleTabatas: ArrayList<Tabata> = Utils.getInstance(this).getAllTabatas()!!
        if (sampleTabatas.isNotEmpty()) {
            for (t in sampleTabatas) {
                if (t.parts.isNotEmpty()) {
                    if (t.parts[0].imgID != R.drawable.ic_exercise
                        && t.parts[0].imgID != R.drawable.ic_break
                        && t.parts[0].imgID != R.drawable.ic_set_marker
                    ) {
                        for (p in t.parts) {
                            p.imgID = when (p.type) {
                                "exercise" -> R.drawable.ic_exercise
                                "break" -> R.drawable.ic_break
                                "set marker" -> R.drawable.ic_set_marker
                                else -> R.drawable.ic_exercise
                            }
                        }

                    }
                }
            }
        }
        return sampleTabatas
    }

}