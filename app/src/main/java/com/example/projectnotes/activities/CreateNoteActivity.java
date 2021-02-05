package com.example.projectnotes.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Layout;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.request.RequestOptions;
import com.example.projectnotes.R;
import com.example.projectnotes.database.NotesDatabase;
import com.example.projectnotes.entities.Note;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CreateNoteActivity extends AppCompatActivity{

    private EditText inputNoteTitle, inputNoteSubtitle, inputNoteText;
    private TextView textDateTime;

    private String selectedNoteColor;   // for note color
    private View subtitleIndicator;

    private ImageView imageNote;  // for image
    private  String selectedImagePath;

    private TextView textWebURL;  // for URL
    private  LinearLayout layoutWebURL;

    private AlertDialog dialogAddURL;
    private  AlertDialog dialogDelete; // for delete for note

    private Note alreadyAvailableNote;

    private  static  final  int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CODE_SELECT_IMAGE = 2;

    private static boolean isBottomSheetExpanded=false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);

        ImageView imageBack = findViewById(R.id.imageBack);
        imageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        inputNoteTitle = findViewById(R.id.inputNoteTitle);
        inputNoteSubtitle = findViewById(R.id.inputNoteSubtitle);
        inputNoteText = findViewById(R.id.inputNote);
        textDateTime = findViewById(R.id.textDateTime);
        subtitleIndicator=findViewById(R.id.viewSubtitleIndicator);
        imageNote = findViewById(R.id.imageNote);

        textWebURL=findViewById(R.id.textWebURL);
        layoutWebURL=findViewById(R.id.layoutWebURL);

        textDateTime.setText(
                new SimpleDateFormat("EEEE,dd MMMM yyyy HH:mm a", Locale.getDefault())
                .format(new Date())
        );

        ImageView imageSave = findViewById(R.id.imageSave);
        imageSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveNote();
            }
        });

        selectedNoteColor = "#333333";
        selectedImagePath = "null";

        if(getIntent().getBooleanExtra("isViewOrUpdate",false)){
            alreadyAvailableNote = (Note) getIntent().getSerializableExtra("note");
            setViewOrUpdateNote();
        }

        //seting onclick listener on WebURL DeleteButton
        findViewById(R.id.imageRemoveWebURL).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textWebURL.setText(null);
                layoutWebURL.setVisibility(View.GONE);
            }
        });

        // on click listner for ImageDelete BUtton
        findViewById(R.id.imageRemoveImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageNote.setImageBitmap(null);
                imageNote.setVisibility(View.GONE);
                findViewById(R.id.imageRemoveImage).setVisibility(View.GONE);
                selectedImagePath="null";
            }
        });

        //chceking for quick ActionImage
        if (getIntent().getBooleanExtra("isFromQuickActions",false)){
            String type=getIntent().getStringExtra("quickActionType");
            if (type!=null){
                if (type.equals("image")){
                    selectedImagePath=getIntent().getStringExtra("imagePath");
                    Glide.with(this).load(selectedImagePath).into(imageNote);
                    imageNote.setVisibility(View.VISIBLE);
                    findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
                }else if (type.equals("URL")){
                    textWebURL.setText(getIntent().getStringExtra("URL"));
                    layoutWebURL.setVisibility(View.VISIBLE);
                }
            }
        }
        // end

        initMiscellaneous();
        setsubtitleIndicatorColor();
    }

   // for View Notes
    private  void setViewOrUpdateNote(){

        inputNoteTitle.setText(alreadyAvailableNote.getTitle());
        inputNoteSubtitle.setText(alreadyAvailableNote.getSubtitle());
        inputNoteText.setText(alreadyAvailableNote.getNoteText());
        textDateTime.setText(alreadyAvailableNote.getDateTime());
        selectedNoteColor=alreadyAvailableNote.getColor();
        setsubtitleIndicatorColor();

        if( ! alreadyAvailableNote.getImagePath().equals("null") ){
            Glide.with(CreateNoteActivity.this).load(alreadyAvailableNote.getImagePath()).into(imageNote);
            imageNote.setVisibility(View.VISIBLE);
            findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
            selectedImagePath=alreadyAvailableNote.getImagePath();
        }

        if(alreadyAvailableNote.getWebLink()!=null && !alreadyAvailableNote.getWebLink().trim().isEmpty()){
            textWebURL.setText(alreadyAvailableNote.getWebLink());
            layoutWebURL.setVisibility(View.VISIBLE);
        }
    }

    private  void saveNote()
    {
        if(inputNoteTitle.getText().toString().trim().isEmpty())
        {
            Toast.makeText(this, "Note title can't be empty!", Toast.LENGTH_SHORT).show();
            return;
        }
        else if( inputNoteSubtitle.getText().toString().trim().isEmpty()
                 && inputNoteText.getText().toString().trim().isEmpty() )
        {
            Toast.makeText(this, "Subtitle  can't be empty!", Toast.LENGTH_SHORT).show();
             return;
        }

        final Note note=new Note();
        note.setTitle(inputNoteTitle.getText().toString());
        note.setSubtitle(inputNoteSubtitle.getText().toString());
        note.setNoteText(inputNoteText.getText().toString());
        note.setDateTime(textDateTime.getText().toString());
        note.setColor(selectedNoteColor);
        note.setImagePath(selectedImagePath);

        // checking if the Dialog is visible..if true then fetch data from it
        if(layoutWebURL.getVisibility() == View.VISIBLE){
            note.setWebLink(textWebURL.getText().toString());
        }

        if (alreadyAvailableNote != null)
        {
            note.setId(alreadyAvailableNote.getId());
        }

        // Clearing cache
      /*  new Thread(new Runnable() {
            @Override
            public void run() {
                Glide.get(CreateNoteActivity.this).clearDiskCache();
                Log.d("MYLOG","GlideCache Cleared successfully");
              //  Toast.makeText(CreateNoteActivity.this, "Clearing cache", Toast.LENGTH_SHORT).show();
            }
        }).start();*/
        // // //

        @SuppressLint("StaticFieldLeak")
        class SaveNoteTask extends AsyncTask<Void,Void,Void>{

            @Override
            protected Void doInBackground(Void... voids) {
                NotesDatabase.getDatabase(getApplicationContext()).noteDao().insertNote(note);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Intent intent = new Intent();
                setResult(RESULT_OK,intent);
                finish();
            }
        }

        new SaveNoteTask().execute();
        selectedImagePath="null";

    }

    public void initMiscellaneous()
    {   //Bottom Sheet Operation
        final LinearLayout layoutMicellaneous = findViewById(R.id.layoutMiscellaneous);
        final BottomSheetBehavior<LinearLayout> bottomSheetBehavior=BottomSheetBehavior.from(layoutMicellaneous);
        layoutMicellaneous.findViewById(R.id.textMiscellaneous).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    isBottomSheetExpanded=true;
                }else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    isBottomSheetExpanded=false;
                }
            }

        });
        // end operation

         final ImageView imageColor1 = layoutMicellaneous.findViewById(R.id.imageColor1);
         final ImageView imageColor2 = layoutMicellaneous.findViewById(R.id.imageColor2);
         final ImageView imageColor3 = layoutMicellaneous.findViewById(R.id.imageColor3);
         final ImageView imageColor4 = layoutMicellaneous.findViewById(R.id.imageColor4);
         final ImageView imageColor5 = layoutMicellaneous.findViewById(R.id.imageColor5);

         //Grey color = Defalut
         layoutMicellaneous.findViewById(R.id.imageColor1).setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {

                 selectedNoteColor="#333333";
                 imageColor1.setImageResource(R.drawable.ic_done);
                 imageColor2.setImageResource(0);
                 imageColor3.setImageResource(0);
                 imageColor4.setImageResource(0);
                 imageColor5.setImageResource(0);
                 setsubtitleIndicatorColor();
             }
         });

        //Yellow Color
        layoutMicellaneous.findViewById(R.id.imageColor2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                selectedNoteColor="#FDBE3B";
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(R.drawable.ic_done);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);
                setsubtitleIndicatorColor();
            }
        });

        //Red Color
        layoutMicellaneous.findViewById(R.id.imageColor3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                selectedNoteColor="#FF4842";
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(R.drawable.ic_done);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);
                setsubtitleIndicatorColor();
            }
        });

        //Blue Color
        layoutMicellaneous.findViewById(R.id.imageColor4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                selectedNoteColor="#3A52Fc";
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(R.drawable.ic_done);
                imageColor5.setImageResource(0);
                setsubtitleIndicatorColor();
            }
        });

        //Black Color
        layoutMicellaneous.findViewById(R.id.imageColor5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                selectedNoteColor="#000000";
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(R.drawable.ic_done);
                setsubtitleIndicatorColor();
            }
        });


        if(alreadyAvailableNote != null && alreadyAvailableNote.getColor() != null && !alreadyAvailableNote.getColor().trim().isEmpty()){
            switch (alreadyAvailableNote.getColor()){
                case "#FDBE3B":
                    layoutMicellaneous.findViewById(R.id.viewColor2).performClick();
                    break;
                case "#FF4842":
                    layoutMicellaneous.findViewById(R.id.viewColor3).performClick();
                    break;
                case "#3A52Fc":
                    layoutMicellaneous.findViewById(R.id.viewColor4).performClick();
                    break;
                case "#000000":
                    layoutMicellaneous.findViewById(R.id.viewColor5).performClick();
                    break;
            }
        }




        // Add image
        layoutMicellaneous.findViewById(R.id.layoutAddImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                isBottomSheetExpanded=false;

                if(ContextCompat.
                        checkSelfPermission( getApplicationContext(),
                                              Manifest.permission.READ_EXTERNAL_STORAGE ) != PackageManager.PERMISSION_GRANTED  )
                {
                    ActivityCompat.requestPermissions(
                            CreateNoteActivity.this,
                            new String []{ Manifest.permission.READ_EXTERNAL_STORAGE },
                            REQUEST_CODE_STORAGE_PERMISSION  );
                } else
                {
                    selectImage();
                }

            }
        });

        // Add URL
        layoutMicellaneous.findViewById(R.id.layoutAddUrl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showAddURLDialog();
                isBottomSheetExpanded=false;
            }
        });

        // Deletion Note
        if(alreadyAvailableNote != null)
        {
            layoutMicellaneous.findViewById(R.id.layoutDeleteNote).setVisibility(View.VISIBLE);
            layoutMicellaneous.findViewById(R.id.layoutDeleteNote).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    showDeleteNoteDialog();

                }
            });
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////////////

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////


    }

    private  void showDeleteNoteDialog(){
        if (dialogDelete == null)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_delete_note,
                    (ViewGroup) findViewById(R.id.layoutDeleteNoteContainer)
            );

            builder.setView(view);
            dialogDelete=builder.create();
            if(dialogDelete.getWindow()!=null){
                dialogDelete.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            view.findViewById(R.id.textDeleteNote).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    @SuppressLint("StaticFieldLeak")
                    class  DeleteNoteTask extends AsyncTask<Void,Void,Void>{
                        @Override
                        protected Void doInBackground(Void... voids) {
                            NotesDatabase.getDatabase(getApplicationContext())
                                    .noteDao()
                                    .deleteNote(alreadyAvailableNote);

                            return null;
                        } // end of this Function

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);

                            Intent intent=new Intent();
                            intent.putExtra("isNoteDeleted",true);
                            setResult(RESULT_OK,intent);
                            finish();
                        } // end of this funtion
                    } // end of Class

                     new DeleteNoteTask().execute();
                }
                //end of onClick
            });
            // end of DeleteNote clickListenerr

            view.findViewById(R.id.textCancelDelete).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialogDelete.dismiss();
                }
            });
            //end of onClickListener
        }

        dialogDelete.show();
    }

    private  void setsubtitleIndicatorColor()
    {
        GradientDrawable gradientDrawable = (GradientDrawable) subtitleIndicator.getBackground();
        gradientDrawable.setColor(Color.parseColor(selectedNoteColor));
    }

    private  void selectImage()
    {      Log.d("MYLOG","Starting Select-Image ");

          Intent intent= new Intent(Intent.ACTION_PICK);
         intent.setType("image/*");
         startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
         /*
        Intent intent=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
          if(intent.resolveActivity(getPackageManager()) != null)
          {
              Log.d("MYLOG","Start Activity For Result");

              startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
              Log.d("MYLOG","End Activity For Result =========");

          }*/
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0 )
        {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                selectImage();
            }else{
                Toast.makeText(this, " Permission Denied !!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        Log.d("MYLOG","Entered onActivity ===");
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("MYLOG","Inside On ActivityResult");
        if(requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK){
            if( data != null)
            {
                Uri selectedImageUri = data.getData();
                Log.d("MYLOG","Selected Image Uri : "+selectedImageUri);
                if( selectedImageUri != null)
                {
                    /* InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                     Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                     imageNote.setImageBitmap(bitmap);*/
                    Log.d("MYLOG","Glide Is Starting");
                    Glide.with(this).
                            load(selectedImageUri).error(R.drawable.ic_cross_add).
                             into(imageNote);

                    //Picasso.get().load(selectedImagePath).fit().into(imageNote);
                    Log.d("MYLOG","Glide Ending");

                    imageNote.setVisibility(View.VISIBLE);

                    findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
                    selectedImagePath = getPathFromUri(selectedImageUri);

                } else{
                    Log.d("MYLOG","Selected Image uri is null ==");
                    Toast.makeText(this, "Selected Image Uri Null", Toast.LENGTH_SHORT).show();
                }
            }
            else
            {   Log.d("MYLOG","Data is null ==");
                Toast.makeText(this, "Wrong Request Code == ", Toast.LENGTH_SHORT).show();
            }

        }
        else
        {   Log.d("MYLOG","Password Encountered");
            //Toast.makeText(this, "Image Too Large ! !", Toast.LENGTH_SHORT).show();
        }

    }

    private  String getPathFromUri(Uri contentUri)
    {
        String filePath;
        Cursor cursor = getContentResolver()
                         .query(contentUri,null,null,null,null);

        if (cursor == null)
        {
            filePath = contentUri.getPath();
            Log.d("MYLOG","Cursor NULL thi");
        }
        else{
            Log.d("MYLOG","Cursor Moving To First");
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        return  filePath;
    }

    private void showAddURLDialog() {
        if (dialogAddURL == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_add_url,
                    (ViewGroup) findViewById(R.id.layoutAddUrlContainer)
            );
            builder.setView(view);

            dialogAddURL = builder.create();
            if (dialogAddURL.getWindow() != null) {
                dialogAddURL.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText inputURL = view.findViewById(R.id.inputURL);
            inputURL.requestFocus();

            view.findViewById(R.id.textAdd).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (inputURL.getText().toString().trim().isEmpty()) {
                        Toast.makeText(CreateNoteActivity.this, "Enter URL", Toast.LENGTH_SHORT).show();
                    } else if (! Patterns.WEB_URL.matcher(inputURL.getText().toString()).matches()) {
                        Toast.makeText(CreateNoteActivity.this, "Enter Valid URL", Toast.LENGTH_SHORT).show();
                    } else {
                        textWebURL.setText(inputURL.getText().toString());
                        layoutWebURL.setVisibility(View.VISIBLE);
                        dialogAddURL.dismiss();
                    }

                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialogAddURL.dismiss();
                }
            });


        }

        dialogAddURL.show();
    }

    @Override
    public void onBackPressed() {
        if(isBottomSheetExpanded)
        {
            final LinearLayout layoutMicellaneous = findViewById(R.id.layoutMiscellaneous);
            final BottomSheetBehavior<LinearLayout> bottomSheetBehavior=BottomSheetBehavior.from(layoutMicellaneous);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            isBottomSheetExpanded=false;
        }else {
            super.onBackPressed();
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////


}
