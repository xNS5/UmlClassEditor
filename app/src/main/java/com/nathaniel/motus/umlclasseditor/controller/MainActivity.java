package com.nathaniel.motus.umlclasseditor.controller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.nathaniel.motus.umlclasseditor.R;
import com.nathaniel.motus.umlclasseditor.model.TypeNameComparator;
import com.nathaniel.motus.umlclasseditor.model.UmlClass;
import com.nathaniel.motus.umlclasseditor.model.UmlProject;
import com.nathaniel.motus.umlclasseditor.model.UmlRelation;
import com.nathaniel.motus.umlclasseditor.model.UmlType;
import com.nathaniel.motus.umlclasseditor.view.AttributeEditorFragment;
import com.nathaniel.motus.umlclasseditor.view.ClassEditorFragment;
import com.nathaniel.motus.umlclasseditor.view.GraphFragment;
import com.nathaniel.motus.umlclasseditor.view.GraphView;
import com.nathaniel.motus.umlclasseditor.view.MethodEditorFragment;
import com.nathaniel.motus.umlclasseditor.view.ParameterEditorFragment;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements FragmentObserver,
        GraphView.GraphViewObserver/*,
        NavigationView.OnNavigationItemSelectedListener*/{

    private UmlProject mProject;
    private DrawerLayout mDrawerLayout;
    private Toolbar mToolbar;
    private NavigationView mNavigationView;
//    private TextView mMenuHeaderProjectNameText;

    private static boolean sWriteExternalStoragePermission =true;
    private static boolean sReadExternalStoragePermission=true;
    private static final int WRITE_EXTERNAL_STORAGE_INDEX=0;
    private static final int READ_EXTERNAL_STORAGE_INDEX=1;

    private long mFirstBackPressedTime =0;
    private OnBackPressedCallback mOnBackPressedCallback;

//    **********************************************************************************************
//    Fragments declaration
//    **********************************************************************************************
    private GraphFragment mGraphFragment;
    private ClassEditorFragment mClassEditorFragment;
    private AttributeEditorFragment mAttributeEditorFragment;
    private MethodEditorFragment mMethodEditorFragment;
    private ParameterEditorFragment mParameterEditorFragment;

    private static final String GRAPH_FRAGMENT_TAG="graphFragment";
    private static final String CLASS_EDITOR_FRAGMENT_TAG="classEditorFragment";
    private static final String ATTRIBUTE_EDITOR_FRAGMENT_TAG="attributeEditorFragment";
    private static final String METHOD_EDITOR_FRAGMENT_TAG="methodEditorFragment";
    private static final String PARAMETER_EDITOR_FRAGMENT_TAG="parameterEditorFragment";

    private static final String SHARED_PREFERENCES_PROJECT_NAME="sharedPreferencesProjectName";

    private static final int INTENT_CREATE_DOCUMENT_EXPORT_PROJECT =1000;
    private static final int INTENT_OPEN_DOCUMENT_IMPORT_PROJECT =2000;
    private static final int INTENT_CREATE_DOCUMENT_EXPORT_CUSTOM_TYPES=3000;
    private static final int INTENT_OPEN_DOCUMENT_IMPORT_CUSTOM_TYPES=4000;

    private static final int REQUEST_PERMISSION=5000;

    private GraphView mGraphView;
    private ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        try{
                            Intent intent = result.getData();
                            assert intent != null;
                            Uri fileNameUri= intent.getData();
                            Context context = getBaseContext();
                            int subIntent = intent.getIntExtra("intent", -1);
                            switch(subIntent){
                                case INTENT_CREATE_DOCUMENT_EXPORT_PROJECT:
                                    mProject.exportProject(context,fileNameUri);
                                    break;
                                case INTENT_OPEN_DOCUMENT_IMPORT_PROJECT:
                                    UmlType.clearProjectUmlTypes();
                                    mProject=UmlProject.importProject(context,fileNameUri);
                                    mGraphView.setUmlProject(mProject);
                                    break;
                                case INTENT_CREATE_DOCUMENT_EXPORT_CUSTOM_TYPES:
                                    UmlType.exportCustomUmlTypes(context,fileNameUri);
                                    break;
                                case INTENT_OPEN_DOCUMENT_IMPORT_CUSTOM_TYPES:
                                    UmlType.importCustomUmlTypes(context,fileNameUri);
                                    break;
                            }

                        }catch(NullPointerException e){
                            Log.i("TEST", "ActivityResult is Null");
                        }

                    }
                }
            });




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Instantiate views
        //    **********************************************************************************************
        //    Views declaration
        //    **********************************************************************************************
        FrameLayout mMainActivityFrame = findViewById(R.id.activity_main_frame);

        UmlType.clearUmlTypes();
        UmlType.initializePrimitiveUmlTypes(this);
        UmlType.initializeCustomUmlTypes(this);
        getPreferences();
        configureToolbar();

/*
        configureDrawerLayout();
        configureNavigationView();
*/
        configureAndDisplayGraphFragment(R.id.activity_main_frame);
        createOnBackPressedCallback();
        setOnBackPressedCallback();
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_toolbar_menu,menu);
        MenuCompat.setGroupDividerEnabled(menu,true);
        //    Allows to display the menu icons
        if(menu instanceof MenuBuilder){
            MenuBuilder m = (MenuBuilder) menu;
            m.setOptionalIconsVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGraphView=findViewById(R.id.graphview);
        mGraphView.setUmlProject(mProject);
        Log.i("TEST","onStart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mProject.save(getApplicationContext());
        Log.i("TEST","save : project");
        savePreferences();
        Log.i("TEST", "save : preferences");
        UmlType.saveCustomUmlTypes(this);
        Log.i("TEST","save : custom types");
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
    }


//    **********************************************************************************************
//    Configuration methods
//    **********************************************************************************************

//  android:label="@string/app_name"
    private void configureToolbar() {
        mToolbar = findViewById(R.id.main_activity_toolbar);
        Button title_button = (Button)mToolbar.findViewById(R.id.title_button);
        title_button.setText(mProject.getName());
        mToolbar.findViewById(R.id.title_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                toolbarMenuSaveAs();
            }
        });
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    /*
    * Commented out the functions regarding the nav drawer.
    * */
/*     private void configureDrawerLayout() {
        mDrawerLayout=findViewById(R.id.activity_main_drawer);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }*/

/*    private void configureNavigationView() {
        mNavigationView=findViewById(R.id.activity_main_navigation_view);
        mMenuHeaderProjectNameText= mNavigationView.getHeaderView(0).findViewById(R.id.activity_main_navigation_view_header_project_name_text);
        updateNavigationView();
        mNavigationView.setNavigationItemSelectedListener(this);
    }*/

/*
    private void updateNavigationView() {
        mMenuHeaderProjectNameText.setText(mProject.getName());
    }
*/

    private void savePreferences() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor=preferences.edit();
        editor.putString(SHARED_PREFERENCES_PROJECT_NAME,mProject.getName());
        editor.apply();
    }

    private void getPreferences() {
        SharedPreferences preferences=getPreferences(MODE_PRIVATE);
        String projectName=preferences.getString(SHARED_PREFERENCES_PROJECT_NAME,null);
        Log.i("TEST","Loaded preferences");
        if (projectName != null) {
            mProject = UmlProject.load(getApplicationContext(), projectName);
        } else {
            mProject=new UmlProject("NewProject",getApplicationContext());
        }
    }

    private void createOnBackPressedCallback() {
        mOnBackPressedCallback=new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onBackButtonPressed();
            }
        };
    }

    private void setOnBackPressedCallback() {
        this.getOnBackPressedDispatcher().addCallback(this,mOnBackPressedCallback);
    }

    // TODO: Make double tap/back customizable
    private void onBackButtonPressed() {
        long DOUBLE_BACK_PRESSED_DELAY = 2000;
        if (Calendar.getInstance().getTimeInMillis() - mFirstBackPressedTime > DOUBLE_BACK_PRESSED_DELAY) {
            mFirstBackPressedTime=Calendar.getInstance().getTimeInMillis();
            Toast.makeText(this,"Press back again to leave",Toast.LENGTH_SHORT).show();
        }else
        finish();
    }

//    **********************************************************************************************
//    Fragment management
//    **********************************************************************************************
    private void configureAndDisplayGraphFragment(int viewContainerId){
        //handle graph fragment

//        mGraphFragment=new GraphFragment();
        mGraphFragment=GraphFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(viewContainerId,mGraphFragment,GRAPH_FRAGMENT_TAG)
                .commitNow();
    }

    private void configureAndDisplayClassEditorFragment(int viewContainerId,float xLocation,float yLocation,int classOrder) {
        //handle class editor fragment
        if (mClassEditorFragment==null) {
            mClassEditorFragment = ClassEditorFragment.newInstance(xLocation, yLocation, classOrder);
            getSupportFragmentManager().beginTransaction()
                    .hide(mGraphFragment)
                    .add(viewContainerId, mClassEditorFragment, CLASS_EDITOR_FRAGMENT_TAG)
                    .commitNow();
        }
        else {
            mClassEditorFragment.updateClassEditorFragment(xLocation, yLocation, classOrder);
            getSupportFragmentManager().beginTransaction()
                    .hide(mGraphFragment)
                    .show(mClassEditorFragment)
                    .commitNow();
        }
    }

    private void configureAndDisplayAttributeEditorFragment(int viewContainerId,int attributeOrder,int classOrder) {

        if (mAttributeEditorFragment == null) {
            mAttributeEditorFragment = AttributeEditorFragment.newInstance(mClassEditorFragment.getTag(), attributeOrder, classOrder);
            getSupportFragmentManager().beginTransaction()
                    .hide(mClassEditorFragment)
                    .add(viewContainerId, mAttributeEditorFragment, ATTRIBUTE_EDITOR_FRAGMENT_TAG)
                    .commitNow();
        } else {
            mAttributeEditorFragment.updateAttributeEditorFragment(attributeOrder,classOrder);
            getSupportFragmentManager().beginTransaction()
                    .hide(mClassEditorFragment)
                    .show(mAttributeEditorFragment)
                  .commitNow();
        }
    }

    private void configureAndDisplayMethodEditorFragment(int viewContainerId, int methodOrder,int classOrder) {
        if (mMethodEditorFragment == null) {
            mMethodEditorFragment = MethodEditorFragment.newInstance(mClassEditorFragment.getTag(), methodOrder, classOrder);
            getSupportFragmentManager().beginTransaction()
                    .hide(mClassEditorFragment)
                    .add(viewContainerId, mMethodEditorFragment, METHOD_EDITOR_FRAGMENT_TAG)
                    .commitNow();
        } else {
            mMethodEditorFragment.updateMethodEditorFragment(methodOrder,classOrder);
            getSupportFragmentManager().beginTransaction()
                    .hide(mClassEditorFragment)
                    .show(mMethodEditorFragment)
                    .commitNow();
        }
    }

    private void configureAndDisplayParameterEditorFragment(int viewContainerId, int parameterOrder,int methodOrder,int classOrder) {
        if (mParameterEditorFragment == null) {
            mParameterEditorFragment = ParameterEditorFragment.newInstance(mMethodEditorFragment.getTag(), parameterOrder, methodOrder, classOrder);
            getSupportFragmentManager().beginTransaction()
                    .hide(mMethodEditorFragment)
                    .add(viewContainerId, mParameterEditorFragment, PARAMETER_EDITOR_FRAGMENT_TAG)
                    .commitNow();
        } else {
            mParameterEditorFragment.updateParameterEditorFragment(parameterOrder,methodOrder,classOrder);
            getSupportFragmentManager().beginTransaction()
                    .hide(mMethodEditorFragment)
                    .show(mParameterEditorFragment)
                    .commitNow();
        }
    }

//    **********************************************************************************************
//    Getters and setters
//    **********************************************************************************************

    public void setProject(UmlProject project) {
        mProject = project;
    }

//    **********************************************************************************************
//    Callback methods
//    **********************************************************************************************

//    GraphFragmentObserver

    @Override
    public void setPurpose(Purpose purpose) {
    }

    @Override
    public void closeClassEditorFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .hide(fragment)
                .show(mGraphFragment)
                .commitNow();
        mGraphView.invalidate();
    }

    @Override
    public void closeAttributeEditorFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .hide(fragment)
                .show(mClassEditorFragment)
                .commit();
        mClassEditorFragment.updateLists();
    }

    @Override
    public void closeMethodEditorFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .hide(fragment)
                .show(mClassEditorFragment)
                .commitNow();
        mClassEditorFragment.updateLists();
    }

    @Override
    public void closeParameterEditorFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .hide(fragment)
                .show(mMethodEditorFragment)
                .commitNow();
        mMethodEditorFragment.updateLists();
    }

    @Override
    public void openAttributeEditorFragment(int attributeOrder,int classOrder) {
        configureAndDisplayAttributeEditorFragment(R.id.activity_main_frame,attributeOrder,classOrder);
    }

    @Override
    public void openMethodEditorFragment(int methodOrder,int classOrder) {
        configureAndDisplayMethodEditorFragment(R.id.activity_main_frame,methodOrder,classOrder);
    }

    @Override
    public void openParameterEditorFragment(int parameterOrder,int methodOrder,int classOrder) {
        configureAndDisplayParameterEditorFragment(R.id.activity_main_frame,parameterOrder,methodOrder,classOrder);
    }

    @Override
    public UmlProject getProject() {
        return this.mProject;
    }

//    GraphViewObserver

    @Override
    public boolean isExpectingTouchLocation() {
        boolean mExpectingTouchLocation = false;
        return mExpectingTouchLocation;
    }

    @Override
    public void createClass(float xLocation, float yLocation) {
        configureAndDisplayClassEditorFragment(R.id.activity_main_frame,xLocation,yLocation,-1);
    }

    @Override
    public void editClass(UmlClass umlClass) {
        configureAndDisplayClassEditorFragment(R.id.activity_main_frame,0,0,umlClass.getClassOrder());
    }

    @Override
    public void createRelation(UmlClass startClass, UmlClass endClass, UmlRelation.UmlRelationType relationType) {
        if (!mProject.relationAlreadyExistsBetween(startClass,endClass))
            mProject.addUmlRelation(new UmlRelation(startClass,endClass,relationType));
    }

//    **********************************************************************************************
//    Navigation view events
//    **********************************************************************************************
//    @Override
//    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
//        int menuId=item.getItemId();
//        if (menuId == R.id.toolbar_menu_new_project) {
//            toolbarMenuNewProject();
//        } else if (menuId == R.id.toolbar_menu_load_project) {
//            toolbarMenuLoadProject();
//        } else if (menuId == R.id.toolbar_menu_save_as) {
//            toolbarMenuSaveAs();
//        } else if (menuId == R.id.toolbar_menu_merge_project) {
//            toolbarMenuMerge();
//        } else if (menuId == R.id.toolbar_menu_delete_project) {
//            toolbarMenuDeleteProject();
//        }
//        return true;
//    }

//    **********************************************************************************************
//    Navigation view called methods
//    **********************************************************************************************

    private void toolbarMenuSaveAs() {
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        final EditText editText = new EditText(this);
        editText.setText(mProject.getName());
        builder.setTitle("Save Project")
                .setView(editText)
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {}
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        saveAs(editText.getText().toString());
                    }
                })
                .create()
                .show();
    }

    private void toolbarMenuNewProject() {
        mProject.save(this);
        UmlType.clearProjectUmlTypes();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText editText = new EditText(this);
        editText.setPadding(50, 25, 25, 25);
        editText.setHint("Project Name");
        builder.setTitle("New Project")
                .setView(editText)
                .setNegativeButton("CANCEL", (dialogInterface, i) -> {})
                .setPositiveButton("OK", (dialogInterface, i) -> mProject = new UmlProject(editText.getText().toString(), mProject.getContext()))
                .create()
                .show();
        mGraphView.setUmlProject(mProject);
//        updateNavigationView();
    }

    private void toolbarMenuLoadProject() {
        final Context context=this;
        final Spinner spinner=new Spinner(context);
        ArrayAdapter<String> adapter = projectDirectoryAdapter();
        /*
         * Gets the adapter and checks to see if it contains any files to load. If there are none, a toast message pops up alerting the user.
         * */
        if(adapter.getCount() > 0){
            spinner.setAdapter(adapter);
            AlertDialog.Builder builder=new AlertDialog.Builder(this);
            builder.setTitle("Load project")
                    .setMessage("Choose project to load :")
                    .setView(spinner)
                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    })
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String fileName=spinner.getSelectedItem().toString();
                            if (fileName != null) {
                                UmlType.clearProjectUmlTypes();
                                mProject = UmlProject.load(getApplicationContext(), fileName);
                                mGraphView.setUmlProject(mProject);
//                                updateNavigationView();
                            }
                        }
                    })
                    .create()
                    .show();
        } else {
            Toast.makeText(context,"No files to load",Toast.LENGTH_SHORT).show();
        }
    }

    private void toolbarMenuDeleteProject() {
        final Context context=this;
        final Spinner spinner=new Spinner(context);
        ArrayAdapter<String> adapter = projectDirectoryAdapter();
        /*
        * Gets the adapter and checks to see if it contains any files to delete. If there are none, a toast message pops up alerting the user.
        * */
        if(adapter.getCount() > 0){
            spinner.setAdapter(adapter);
            AlertDialog.Builder builder=new AlertDialog.Builder(this);
            builder.setTitle("Delete project")
                    .setMessage("Choose project to delete :")
                    .setView(spinner)
                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    })
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String fileName=spinner.getSelectedItem().toString();
                            if (fileName!=null) {
                                File pathName=new File(getFilesDir(),UmlProject.PROJECT_DIRECTORY);
                                final File file=new File(pathName,fileName);
                                AlertDialog.Builder alert=new AlertDialog.Builder(context);
                                alert.setTitle("Delete Project")
                                        .setMessage("Are you sure you want to delete "+fileName+" ?")
                                        .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {

                                            }
                                        })
                                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                file.delete();
                                            }
                                        })
                                        .create()
                                        .show();
                            }
                        }
                    })
                    .create()
                    .show();
        } else {
            Toast.makeText(context,"No projects to delete",Toast.LENGTH_SHORT).show();
        }
    }

    private void toolbarMenuMerge() {
        final Context context =this;
        final Spinner spinner=new Spinner(context);
        ArrayAdapter<String> adapter = projectDirectoryAdapter();
        /*
         * Gets the adapter and checks to see if it contains any files to merge. If there are none, a toast message pops up alerting the user.
         * */
        if(adapter.getCount() > 0){
            spinner.setAdapter(adapter);
            AlertDialog.Builder builder=new AlertDialog.Builder(this);
            builder.setTitle("Merge project")
                    .setMessage("Choose project to merge")
                    .setView(spinner)
                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    })
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String fileName=spinner.getSelectedItem().toString();
                            if (fileName!=null) {
                                UmlProject project = UmlProject.load(getApplicationContext(), fileName);
                                assert project != null;
                                mProject.mergeWith(project);
                                mGraphView.invalidate();
                            }

                        }
                    })
                    .create()
                    .show();
        } else {
            Toast.makeText(context,"No projects to merge",Toast.LENGTH_SHORT).show();
        }
    }

    private ArrayAdapter<String> projectDirectoryAdapter() {
        /*
        * Create an array adapter to set a spinner with all project file names
        *
        * Added try...catch blocks to catch a null pointer exception that occurs when the user tries to delete a file when there are none saved.
        * */
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        File[] file_list = new File(getFilesDir(), UmlProject.PROJECT_DIRECTORY).listFiles();
        try {
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, IOUtils.sortedFiles(file_list));
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        } catch(NullPointerException e){
            e.printStackTrace();
        }
       return adapter;
    }

//    **********************************************************************************************
//    Option menu events
//    **********************************************************************************************

    @SuppressLint("NonConstantResourceId")
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if((itemId != R.id.project_menu || itemId != R.id.type_menu) && sReadExternalStoragePermission){
            switch(itemId) {
                case R.id.toolbar_menu_new_project:
                    toolbarMenuNewProject();
                    break;
                case R.id.toolbar_menu_load_project:
                    toolbarMenuLoadProject();
                    break;
                case R.id.toolbar_menu_save_as:
                    toolbarMenuSaveAs();
                    break;
                case R.id.toolbar_menu_merge_project:
                    toolbarMenuMerge();
                    break;
                case R.id.toolbar_menu_delete_project:
                    toolbarMenuDeleteProject();
                    break;
                case R.id.toolbar_menu_export:
                    menuItemImport();
                    break;
                case R.id.toolbar_menu_import:
                    menuItemExport();
                    break;
                case R.id.toolbar_menu_create_custom_type:
                    menuCreateCustomType();
                    break;
                case R.id.toolbar_menu_delete_custom_types:
                    menuDeleteCustomTypes();
                    break;
                case R.id.toolbar_menu_export_custom_types:
                    menuExportCustomTypes();
                    break;
                case R.id.toolbar_menu_import_custom_types:
                    menuImportCustomTypes();
                    break;
                case R.id.toolbar_menu_help:
                    menuHelp();
                    break;
            }
            return true;
        }
        return false;
    }

//    **********************************************************************************************
//    Menu item called methods
//    TODO: Remove deprecated API + find alternatives
//    **********************************************************************************************
    private void menuItemExport() {
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/*");
//        startActivityForResult(intent, INTENT_CREATE_DOCUMENT_EXPORT_PROJECT);
        intent.putExtra("intent", INTENT_CREATE_DOCUMENT_EXPORT_PROJECT);
        mStartForResult.launch(intent);
    }

    private void menuItemImport() {
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
//        startActivityForResult(intent, INTENT_OPEN_DOCUMENT_IMPORT_PROJECT);
        intent.putExtra("intent", INTENT_OPEN_DOCUMENT_IMPORT_PROJECT);
        mStartForResult.launch(intent);
    }

    private void menuCreateCustomType() {
        final EditText editText=new EditText(this);
        final Context context=getApplicationContext();
        AlertDialog.Builder adb=new AlertDialog.Builder(this);
        adb.setTitle("Create custom type")
                .setMessage("Enter custom type name :")
                .setView(editText)
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String typeName=editText.getText().toString();
                        if (typeName.equals(""))
                            Toast.makeText(context,"Failed : name cannot be blank",Toast.LENGTH_SHORT).show();
                        else if (UmlType.containsUmlTypeNamed(typeName))
                            Toast.makeText(context,"Failed : this name is already used",Toast.LENGTH_SHORT).show();
                        else{
                            UmlType.createUmlType(typeName, UmlType.TypeLevel.CUSTOM);
                            Toast.makeText(context,"Custom type created",Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .create()
                .show();
    }

    private void menuDeleteCustomTypes() {
        final ListView listView=new ListView(this);
        List<String> listArray=new ArrayList<>();
        for (UmlType t:UmlType.getUmlTypes())
            if (t.isCustomUmlType()) listArray.add(t.getName());
        Collections.sort(listArray,new TypeNameComparator());
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice,listArray);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        listView.setAdapter(adapter);

        AlertDialog.Builder adb=new AlertDialog.Builder(this);
        adb.setTitle("Delete custom types")
                .setMessage("Check custom types to delete")
                .setView(listView)
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        SparseBooleanArray checkMapping=listView.getCheckedItemPositions();
                        UmlType t;
                        for (int j = 0; j < checkMapping.size(); j++) {
                            if (checkMapping.valueAt(j)) {
                                t=UmlType.valueOf(listView.getItemAtPosition(checkMapping.keyAt(j)).toString(),UmlType.getUmlTypes());
                                UmlType.removeUmlType(t);
                                mProject.removeParametersOfType(t);
                                mProject.removeMethodsOfType(t);
                                mProject.removeAttributesOfType(t);
                                mGraphView.invalidate();
                            }
                        }
                    }
                })
                .create()
                .show();
    }

    private void menuExportCustomTypes() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/*");
//        startActivityForResult(intent,INTENT_CREATE_DOCUMENT_EXPORT_CUSTOM_TYPES);
        intent.putExtra("intent", INTENT_CREATE_DOCUMENT_EXPORT_CUSTOM_TYPES);
        mStartForResult.launch(intent);
    }

    private void menuImportCustomTypes() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
//        startActivityForResult(intent,INTENT_OPEN_DOCUMENT_IMPORT_CUSTOM_TYPES);
        intent.putExtra("intent", INTENT_OPEN_DOCUMENT_IMPORT_CUSTOM_TYPES);
        mStartForResult.launch(intent);
    }

    private void menuHelp() {
        AlertDialog.Builder adb=new AlertDialog.Builder(this);
        adb.setTitle("Help")
                .setMessage(Html.fromHtml(IOUtils.readRawHtmlFile(this,R.raw.help_html)))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .create()
                .show();
    }

//    **********************************************************************************************
//    Intents
//    **********************************************************************************************

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        if (requestCode == INTENT_CREATE_DOCUMENT_EXPORT_PROJECT && resultCode == RESULT_OK) {
//            Uri fileNameUri=data.getData();
//            mProject.exportProject(this,fileNameUri);
//        } else if (requestCode == INTENT_OPEN_DOCUMENT_IMPORT_PROJECT && resultCode == RESULT_OK) {
//            Uri fileNameUri=data.getData();
//            UmlType.clearProjectUmlTypes();
//            mProject=UmlProject.importProject(this,fileNameUri);
//            mGraphView.setUmlProject(mProject);
//        } else if (requestCode == INTENT_CREATE_DOCUMENT_EXPORT_CUSTOM_TYPES && resultCode == RESULT_OK) {
//            Uri fileNameUri=data.getData();
//            UmlType.exportCustomUmlTypes(this,fileNameUri);
//        } else if (requestCode == INTENT_OPEN_DOCUMENT_IMPORT_CUSTOM_TYPES && resultCode == RESULT_OK) {
//            Uri fileNameUri=data.getData();
//            UmlType.importCustomUmlTypes(this,fileNameUri);
//        }
//        super.onActivityResult(requestCode, resultCode, data);
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION && grantResults[WRITE_EXTERNAL_STORAGE_INDEX]==PackageManager.PERMISSION_GRANTED)
            sWriteExternalStoragePermission =true;
        else
            sWriteExternalStoragePermission =false;

        if (requestCode == REQUEST_PERMISSION && grantResults[READ_EXTERNAL_STORAGE_INDEX]==PackageManager.PERMISSION_GRANTED)
            sReadExternalStoragePermission =true;
        else
            sReadExternalStoragePermission =false;
    }

//    **********************************************************************************************
//    Project management methods
//    **********************************************************************************************

    private void saveAs(String projectName) {
        mProject.setName(projectName);
//        updateNavigationView();
        mProject.save(getApplicationContext());
    }

//    **********************************************************************************************
//    Check permissions
//    **********************************************************************************************

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            String[] permissionString={Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE};

            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)
                requestPermissions(permissionString, REQUEST_PERMISSION);
        }
    }
}
